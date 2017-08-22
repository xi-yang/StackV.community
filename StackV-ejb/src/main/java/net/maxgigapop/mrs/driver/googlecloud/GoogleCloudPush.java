/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import org.json.simple.JSONObject;

import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
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
    private GoogleCloudGet computeGet = null;
    private String topologyUri = null;
    private String region = null;
    private String projectID = null;
    String defaultImage = null;
    String defaultInstanceType = null;
    String defaultKeyPair = null;
    String defaultSecGroup = null;
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    
    public GoogleCloudPush(String jsonAuth, String projectID, String region, String topologyUri, 
            String defaultImage, String defaultInstanceType, String defaultKeyPair, String defaultSecGroup) {
        computeGet = new GoogleCloudGet(jsonAuth, projectID, region); 
        this.projectID = projectID;
        this.region = region;
        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
        this.defaultImage = defaultImage;
        this.defaultInstanceType = defaultInstanceType;
        this.defaultKeyPair = defaultKeyPair;
        this.defaultSecGroup = defaultSecGroup;
    }
    
    public List<JSONObject> propagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) {
        
        return null;
    }
    
    public void commit(List<JSONObject> requests) throws InterruptedException {
        for (JSONObject request : requests) {
            switch (request.get("type").toString()) {
            case "vm":
                if (request.get("add") != null) {
                    String name = request.get("name").toString();
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
                    ArrayList<AttachedDisk> disks = new ArrayList<AttachedDisk>();
                    disks.add(disk);
                    Instance instance = new Instance()
                        .setName(name)
                        .setMachineType(machineType)
                        .setNetworkInterfaces(netifaces)
                        .setDisks(disks);
                    
                    try {
                        HttpRequest httpRequest = computeGet.getComputeClient().instances().insert(projectID, region, instance).buildHttpRequest();
                        httpRequest.execute();
                    } catch (IOException ex) {
                        //todo
                    }
                } else {
                    
                }
            break;
            }
        }
    }
}
