/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.client.http.HttpRequest;
import com.google.api.services.compute.model.AccessConfig;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 *
 * @author raymonddsmith
 */
public class GoogleCloudPush {
    private GoogleCloudGet gcpGet;
    private String topologyUri = null;
    private String region = null;
    private String projectID = null;
    String defaultImage = null;
    String defaultInstanceType = null;
    String defaultKeyPair = null;
    String defaultSecGroup = null;
    String defaultRegion = null; //Default region is used by subnets, currently
    String defaultZone = null; //Default zone is used by instances and disks, and should contained within default region
    
    public GoogleCloudPush(String jsonAuth, String projectID, String region, String topologyUri, 
            String defaultImage, String defaultInstanceType, String defaultKeyPair, String defaultSecGroup) {
        this.gcpGet = new GoogleCloudGet(jsonAuth, projectID, region);
        this.projectID = projectID;
        this.region = region;
        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
        this.defaultImage = defaultImage;
        this.defaultInstanceType = defaultInstanceType;
        this.defaultKeyPair = defaultKeyPair;
        this.defaultSecGroup = defaultSecGroup;
        this.defaultRegion = "us-central1";//this should be set by constructor, but the value has not yet been added to driver
        this.defaultZone = "us-central1-c";//same ^
        
    }
    
    public ArrayList<JSONObject> propagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) {
        ArrayList<JSONObject> requests = new ArrayList<>();
        GoogleCloudQuery gcq = new GoogleCloudQuery(modelRef, modelAdd, modelReduct);
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
    public void commit(ArrayList<JSONObject> requests) throws InterruptedException {
        String method = "commit";
        HttpRequest httpRequest;
        String name;
        
        for (JSONObject request : requests) {
            switch (request.get("type").toString()) {
            case "create_vpc":
                name = request.get("name").toString();
                
            break;
            case "delete_vpc":
                
            break;
            case "create_subnet":
                
            break;
            case "delete_subnet":
                
            break;
            case "create_instance":
                name = request.get("name").toString();
                String ip = request.get("ip").toString();
                String subnetIP = request.get("subnetIP").toString();
                String machineType = "zones/"+region+"/machineTypes/";
                machineType += request.get("machineType").toString();
                long diskSizeGb = Integer.parseInt(request.get("diskSizeGb").toString());
            
                AccessConfig access = new AccessConfig()
                    .setName("External Nat")
                    .setNatIP(ip)
                    .setType("ONE_TO_ONE_NAT");
                ArrayList<AccessConfig> accessList = new ArrayList<>();
                accessList.add(access);
                NetworkInterface netiface = new NetworkInterface()
                    .setNetwork("projects/elegant-works-176420/global/networks/default")
                    .setSubnetwork("projects/elegant-works-176420/regions/us-central1/subnetworks/default")
                    .setNetworkIP(subnetIP)
                    .setAccessConfigs(accessList);
                ArrayList<NetworkInterface> netifaces = new ArrayList<>();
                netifaces.add(netiface);
                    
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
                    .setName(name)
                    .setMachineType(machineType)
                    .setNetworkInterfaces(netifaces)
                    .setDisks(disks);
                    
                try {
                    httpRequest = gcpGet.getComputeClient().instances().insert(projectID, region, instance).buildHttpRequest();
                    httpRequest.execute();
                } catch (IOException ex) {
                    //todo
                }
            break;
            case "delete_instance":
                String instanceName = "placeholder";
                try {
                    httpRequest = gcpGet.getComputeClient().instances().delete(projectID, region, instanceName).buildHttpRequest();
                    gcpGet.makeRequest(httpRequest);
                } catch (IOException e) {
                    //TODO log error
                }
            break;
            }
        }
    }
}
