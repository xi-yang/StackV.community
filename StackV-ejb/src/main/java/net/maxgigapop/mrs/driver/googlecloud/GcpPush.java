/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import java.util.ArrayList;
import java.util.Map;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.Subnetwork;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import com.hp.hpl.jena.ontology.OntModel;

/**
 *
 * @author raymonddsmith
 */
public class GcpPush {
    private GcpGet gcpGet;
    private Map<String, String> properties;
    private JSONParser parser;
    private String topologyUri, jsonAuth, projectID;
    //Default zone is used by instances and disks, and should contained within default region
    //Default region is currently used by subnetss
    private String defaultImage = null, defaultInstanceType, defaultRegion, defaultZone;
    
    public GcpPush(Map<String, String> properties) {
        this.properties = properties;
        jsonAuth = properties.get("jsonAuth");
        projectID = properties.get("projectID");
        gcpGet = new GcpGet(jsonAuth, projectID);
        topologyUri = properties.get("topologyUri") + ":";
        defaultImage = properties.get("defaultImage");
        defaultInstanceType = properties.get("defaultInstanceType");
        defaultRegion = properties.get("defaultRegion");
        defaultZone = properties.get("defaultZone");
        parser = new JSONParser();
    }
    
    public JSONArray propagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) {
        JSONArray requests = new JSONArray();
        GcpQuery gcq = new GcpQuery(modelRef, modelAdd, modelReduct, properties);
        //Instances, VPCs, subnets
        
        requests.addAll(gcq.deleteInstanceRequests());
        requests.addAll(gcq.deleteSubnetRequests());
        requests.addAll(gcq.deleteVpcRequests());
        
        requests.addAll(gcq.createVpcRequests());
        requests.addAll(gcq.createSubnetRequests());
        requests.addAll(gcq.createInstanceRequests());
        
        return requests;
    }
    
    //When deleting an instance, remember to remove it's URI from the lookup table.
    public void commit(JSONArray requests) throws InterruptedException {
        String method = "commit";
        HttpRequest request;
        
        for (Object o : requests) {
            JSONObject requestInfo = (JSONObject) o;
            System.out.printf("recieved request: %s info: %s \n", requestInfo.get("type"), requestInfo);
            switch (requestInfo.get("type").toString()) {
            case "create_vpc":
                String name = requestInfo.get("name").toString();
                String uri =  requestInfo.get("uri").toString();
                Network network = new Network();
                network.setName(name).setAutoCreateSubnetworks(false);
                
                try {
                    JSONObject result;
                    request = gcpGet.getComputeClient().networks().insert(projectID, network).buildHttpRequest();
                    result = gcpGet.makeRequest(request);
                    System.out.printf("create vpc result: %s\n", result);
                    //Add this vpc's uri to the metadata table
                    gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("vpc", name), uri);
                } catch (IOException e) {
                    //TODO log error
                    e.printStackTrace();
                }
            break;
            case "delete_vpc":
                //TODO
            break;
            case "create_subnet":
                //vpcUri is not a MAX but a google URI
                String vpcName = requestInfo.get("vpc_name").toString();
                String vpcUri = "https://www.googleapis.com/compute/v1/"
                        + "projects/elegant-works-176420/global/networks/"+vpcName;
                String subnetUri = requestInfo.get("subnet_uri").toString();
                String cidr = requestInfo.get("subnet_cidr").toString();
                String subnetRegion = requestInfo.get("subnet_region").toString();
                String subnetName = requestInfo.get("subnet_name").toString();
                Subnetwork subnet = new Subnetwork();
                subnet.setIpCidrRange(cidr).setName(subnetName).setRegion(subnetRegion).setNetwork(vpcUri);
                        //.setNetwork(vpcUri);
                
                //This is temporary code until a better solution is reached.
                //The code loops back if and only if the server returns an error
                //indicating that the resource is not ready.
                
                boolean repeat = true;
                String errorMessage = null;
                do {
                    try {
                        System.out.println("COMMMIT TEST: "+vpcName);
                    
                        JSONObject result;
                        request = gcpGet.getComputeClient().subnetworks().insert(projectID, subnetRegion, subnet).buildHttpRequest();
                        result = gcpGet.makeRequest(request);
                        System.out.printf("result: %s", result);
                        
                        gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("subnet", vpcName, subnetRegion, subnetName), subnetUri);
                        repeat = false;
                    } catch (GoogleJsonResponseException e) {
                        String errorJson = e.getDetails().toString();
                        
                        try {
                            
                            JSONObject content = (JSONObject) parser.parse(errorJson);
                            JSONObject error = (JSONObject) ((JSONArray) content.get("errors")).get(0);
                            String reason = error.get("reason").toString();
                            
                            if ("resourceNotReady".equals(reason)) {
                                //this is ok.
                                System.out.println("Waiting for vpc status to become ready");
                            } else {
                                //something went wrong
                                errorMessage = errorJson;
                                repeat = false;
                            }
                        } catch (ParseException pe) {
                            //It's not even JSON?
                            repeat = false;
                            errorMessage = String.format("Expected JSON, but got %s", errorJson);
                        }
                    } catch (IOException e) {
                        //TODO log
                        repeat = false;
                        errorMessage = e.toString();
                    }
                } while (repeat);
                
                if (errorMessage == null) {
                    System.out.println("OK!");
                } else {
                    System.out.printf("COMMIT ERROR: %s\n", errorMessage);
                }
            break;
            case "delete_subnet":
                //TODO
            break;
            case "create_instance":
                String vmName, zone, machineType, diskSize;
                vmName = requestInfo.get("name").toString();
                zone = requestInfo.get("zone").toString();
                String region = requestInfo.get("region").toString();
                String subnetIP = requestInfo.get("subnet_ip").toString();
                machineType = String.format("zones/%s/machineTypes/%s", zone, requestInfo.get("machineType"));
                long diskSizeGb = Integer.parseInt(requestInfo.get("diskSizeGb").toString());
                String diskType = requestInfo.get("disk_type").toString();
                
                //Add NICs
                ArrayList<NetworkInterface> netifaces = new ArrayList<>();
                NetworkInterface netiface;
                ArrayList<AccessConfig> accessList;
                AccessConfig access;
                int i = 0;
                String ip, key = "nic"+i;
                
                while (requestInfo.containsKey(key)) {
                    ip = requestInfo.get(key).toString();
                    access = new AccessConfig()
                        .setName("External Nat")
                        .setNatIP(ip)
                        .setType("ONE_TO_ONE_NAT");
                    accessList = new ArrayList<>();
                    accessList.add(access);
                    netiface = new NetworkInterface()
                        .setNetwork("projects/elegant-works-176420/global/networks/default")
                        .setSubnetwork("projects/elegant-works-176420/regions/us-central1/subnetworks/default")
                        .setNetworkIP(subnetIP)
                        .setAccessConfigs(accessList);
                    netifaces.add(netiface);
                    key = "key" + (++i);
                }
                
                AttachedDiskInitializeParams init = new AttachedDiskInitializeParams()
                    .setDiskSizeGb(diskSizeGb)
                    .setSourceImage("projects/debian-cloud/global/images/debian-9-stretch-v20170717")
                    .setDiskType("projects/elegant-works-176420/zones/us-central1-c/diskTypes/pd-standard");
                AttachedDisk disk = new AttachedDisk()
                    .setBoot(true)
                    .setAutoDelete(true)
                    .setInitializeParams(init);
                ArrayList<AttachedDisk> disks = new ArrayList<>();
                disks.add(disk);
                Instance instance = new Instance()
                    .setName(vmName)
                    .setMachineType(machineType)
                    .setNetworkInterfaces(netifaces)
                    .setDisks(disks);
                    
                try {
                    request = gcpGet.getComputeClient().instances().insert(projectID, region, instance).buildHttpRequest();
                    request.execute();
                } catch (IOException ex) {
                    //todo
                }
            break;
            case "delete_instance":
                name = "placeholder";
                region = "placeholder";
                try {
                    request = gcpGet.getComputeClient().instances().delete(projectID, region, name).buildHttpRequest();
                    gcpGet.makeRequest(request);
                } catch (IOException e) {
                    //TODO log error
                }
            break;
            }
        }
    }
}
