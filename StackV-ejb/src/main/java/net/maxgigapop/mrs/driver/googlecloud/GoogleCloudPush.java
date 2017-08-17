/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.List;
import org.json.simple.JSONObject;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.util.ArrayList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


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
                String name = request.get("name").toString();
                String subnetIP = request.get("subnetIP").toString();
                String machineType = "zones/"+region+"/machineTypes/";
                machineType += request.get("machineType").toString();
                long diskSizeGb = Integer.parseInt(request.get("diskSizeGb").toString());
                
                
                NetworkInterface netiface = new NetworkInterface()
                    .setNetwork("projects/elegant-works-176420/global/networks/default")
                    .setSubnetwork("projects/elegant-works-176420/regions/us-central1/subnetworks/default")
                    .setNetworkIP(subnetIP);
                    //.setAccessConfigs(null);
                ArrayList<NetworkInterface> netifaces = new ArrayList<>();
                netifaces.add(netiface);
                    
                AttachedDiskInitializeParams init = new AttachedDiskInitializeParams()
                    .setDiskSizeGb(diskSizeGb)
                    .setSourceImage("projects/debian-cloud/global/images/debian-9-stretch-v20170717")
                    .setDiskType("projects/elegant-works-176420/zones/us-central1-c/diskTypes/pd-standard");
                AttachedDisk disk = new AttachedDisk()
                    .setBoot(true)
                    .setInitializeParams(init);
                ArrayList<AttachedDisk> disks = new ArrayList<AttachedDisk>();
                disks.add(disk);
                Instance instance = new Instance()
                    .setName(name)
                    .setMachineType(machineType)
                    .setNetworkInterfaces(netifaces)
                    .setDisks(disks);
            
                HttpRequest httpRequest;
                try {
                    httpRequest = computeGet.getComputeClient().instances().insert(projectID, region, instance).buildHttpRequest();
                    httpRequest.execute();
                } catch (IOException ex) {
                    //todo
                }
            break;
            }
        }
    }
}
