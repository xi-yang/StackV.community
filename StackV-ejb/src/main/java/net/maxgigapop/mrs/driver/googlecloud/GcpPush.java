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
            System.out.printf("COMMIT START: %s info: %s \n", requestInfo.get("type"), requestInfo);
            switch (requestInfo.get("type").toString()) {
            case "create_vpc":
                createVpc(requestInfo);
            break;
            case "delete_vpc":
                //TODO
            break;
            case "create_subnet":
                createSubnet(requestInfo);
            break;
            case "delete_subnet":
                //TODO
            break;
            case "create_instance":
                createInstance(requestInfo);
            break;
            case "delete_instance":
                String name = "placeholder";
                String region = "placeholder";
                try {
                    request = gcpGet.getComputeClient().instances().delete(projectID, region, name).buildHttpRequest();
                    gcpGet.makeRequest(request);
                    //TODO remove metadata entry
                } catch (IOException e) {
                    //TODO log error
                }
            break;
            }
            System.out.printf("COMMIT END: %s\n", requestInfo.get("type"));
        }
    }
    
    private void createVpc(JSONObject requestInfo) {
        String name = requestInfo.get("name").toString();
        String uri =  requestInfo.get("uri").toString();
        Network network = new Network();
        network.setName(name).setAutoCreateSubnetworks(false);
        
        try {
            JSONObject result;
            HttpRequest request = gcpGet.getComputeClient().networks().insert(projectID, network).buildHttpRequest();
            result = gcpGet.makeRequest(request);
            System.out.printf("create vpc result: %s\n", result);
            //Add this vpc's uri to the metadata table
            gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("vpc", name), uri);
        } catch (IOException e) {
            //TODO log error
            e.printStackTrace();
        }
    }
    
    private void createSubnet(JSONObject requestInfo) {
        //vpcUri is not a MAX but a google URI
        String vpcName = requestInfo.get("vpc_name").toString();
        String vpcFormat = "https://www.googleapis.com/compute/v1/projects/elegant-works-176420/global/networks/%s";
        String vpcUri = String.format(vpcFormat, vpcName);
        String subnetUri = requestInfo.get("subnet_uri").toString();
        String cidr = requestInfo.get("subnet_cidr").toString();
        String subnetRegion = requestInfo.get("subnet_region").toString();
        String subnetName = requestInfo.get("subnet_name").toString();
        
        Subnetwork subnet = new Subnetwork();
        subnet.setIpCidrRange(cidr).setName(subnetName).setRegion(subnetRegion).setNetwork(vpcUri);
        
        //This is temporary code until a better solution is reached.
        //The code loops back if and only if the server returns an error
        //indicating that the resource is not ready.
        //If the server takes too long to prepare the resource, assume something went wrong.
        long start = System.currentTimeMillis(), max = 30000;
        boolean repeat;
        String errorMessage = null;
                
        do {
            if (System.currentTimeMillis() - start > max) {
                errorMessage = "The resource is taking long to prepare.";
                break;
            }
            
            repeat = false;
            try {
                HttpRequest request = gcpGet.getComputeClient().subnetworks().insert(projectID, subnetRegion, subnet).buildHttpRequest();
                gcpGet.makeRequest(request);
                gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("subnet", vpcName, subnetRegion, subnetName), subnetUri);
            } catch (GoogleJsonResponseException e) {
                String errorJson = e.getDetails().toString();
                
                
            } catch (IOException e) {
                //TODO log
                errorMessage = e.toString();
            }
        } while (repeat);
        
        if (errorMessage != null) {
            System.out.printf("COMMIT ERROR: %s\n", errorMessage);
        }
    }
    
    private void createInstance(JSONObject requestInfo) {
        String vmName = requestInfo.get("name").toString();
        String zone = requestInfo.get("zone").toString();
        String machineFormat = "zones/%s/machineTypes/%s";
        String machineType = String.format(machineFormat, zone, requestInfo.get("machine_type"));
        String sourceImage = requestInfo.get("source_image").toString();
        String diskFormat = "projects/elegant-works-176420/zones/us-central1-c/diskTypes/%s";
        String diskType = String.format(diskFormat, requestInfo.get("disk_type"));
        long diskSizeGb = Integer.parseInt(requestInfo.get("disk_size").toString());
        
        //Used by NICs
        String vpcFormat = "projects/elegant-works-176420/global/networks/%s";
        String subnetFormat = "projects/elegant-works-176420/regions/%s/subnetworks/%s";
        String vpcName, subnetRegion, subnetName, subnetIP;
        
        Instance instance = new Instance()
            .setName(vmName)
            .setMachineType(machineType);
                
        //Add NICs
        JSONArray nics = (JSONArray) requestInfo.get("nics");
        
        if (nics != null) {
            ArrayList<NetworkInterface> netifaces = new ArrayList<>();
            ArrayList<AccessConfig> accessList;
            int i = 0;

            for (Object o : nics) {
                JSONObject nicInfo = (JSONObject) o;
                
                vpcName = nicInfo.get("vpc").toString();
                subnetRegion = nicInfo.get("region").toString();
                subnetName = nicInfo.get("subnet").toString();
                subnetIP = nicInfo.get("ip").toString();
                
                AccessConfig access = new AccessConfig()
                    .setName("External Nat")
                    .setNatIP(subnetIP) //external ip?
                    .setType("ONE_TO_ONE_NAT");
                accessList = new ArrayList<>();
                accessList.add(access);
            
                NetworkInterface netiface = new NetworkInterface()
                    .setNetwork(String.format(vpcFormat, vpcName))
                    .setSubnetwork(String.format(subnetFormat, subnetRegion, subnetName))
                    //.setNetworkIP(subnetIP) //This is the internal ip
                    .setAccessConfigs(accessList);
                netifaces.add(netiface);
            }
            
            instance.setNetworkInterfaces(netifaces);
        }
        
        AttachedDiskInitializeParams init = new AttachedDiskInitializeParams()
            .setDiskSizeGb(diskSizeGb)
            .setSourceImage(sourceImage)
            .setDiskType(diskType);
        AttachedDisk disk = new AttachedDisk()
            .setBoot(true)
            .setAutoDelete(true)
            .setInitializeParams(init);
        ArrayList<AttachedDisk> disks = new ArrayList<>();
            disks.add(disk);
        instance.setDisks(disks);
        
        try {
            HttpRequest request = gcpGet.getComputeClient().instances().insert(projectID, zone, instance).buildHttpRequest();
            request.execute();
        } catch (IOException ex) {
            //TODO
            System.out.printf("ERROR: %s\n", ex);
        }
    }
    
    JSONObject parseJSONException(GoogleJsonResponseException e) {
        String errorJson = e.getDetails().toString();
        try {
            return (JSONObject) parser.parse(errorJson);
        } catch (ParseException pe) {
            //It's not even JSON?
            System.out.printf("Expected JSON, but got %s", errorJson);

        }
        return null;
    }
    
}
