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
import com.google.api.services.storage.model.Bucket;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.HashSet;
import net.maxgigapop.mrs.common.StackLogger;

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
    public static final StackLogger logger = GcpDriver.logger;
    
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
        
        requests.addAll(gcq.deleteBucketsRequest());
        requests.addAll(gcq.deleteInstanceRequests());
        requests.addAll(gcq.deleteSubnetRequests());
        requests.addAll(gcq.deleteVpcRequests());
        
        requests.addAll(gcq.createVpcRequests());
        requests.addAll(gcq.createSubnetRequests());
        requests.addAll(gcq.createInstanceRequests());
        requests.addAll(gcq.createBucketsRequest());
        
        return requests;
    }
    
    //When deleting an instance, remember to remove it's URI from the lookup table.
    public void commit(JSONArray requests) throws InterruptedException {
        String method = "commit";
        HttpRequest request;
        
        for (Object o : requests) {
            JSONObject requestInfo = (JSONObject) o;
            System.out.printf("COMMIT START: %s info: %s \n", requestInfo.get("type"), requestInfo);
            String type = requestInfo.containsKey("type") ? requestInfo.get("type").toString() : "null";
            
            switch (type) {
            case "create_vpc":
                createVpc(requestInfo);
            break;
            case "delete_vpc":
                deleteVpc(requestInfo);
            break;
            case "create_subnet":
                createSubnet(requestInfo);
            break;
            case "delete_subnet":
                deleteSubnet(requestInfo);
            break;
            case "create_instance":
                createInstance(requestInfo);
            break;
            case "delete_instance":
                deleteInstance(requestInfo);
            break;
            case "create_bucket":
                createBucket(requestInfo);
            break;
            case "delete_bucket":
                //deleteBucket(requestInfo);
            break;
            case "null":
                logger.warning(method, "encountered request without type");
            break;
            default:
                logger.warning(method, "encountered request of unknown type: "+ type);
            break;
            }
            System.out.printf("COMMIT END: %s\n", requestInfo.get("type"));
        }
    }
    
    private void createVpc(JSONObject requestInfo) {
        String method = "createVpc";
        String missingArgs = checkArgs(requestInfo, "name", "uri");
        if (missingArgs != null) {
            logger.warning(method, missingArgs);
            return;
        }
        
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
            System.out.printf("COMMIT ERROR\n");
        }
    }
    
    private void deleteVpc(JSONObject requestInfo) {
        String method = "deleteVpc";
        String missingArgs = checkArgs(requestInfo, "name");
        if (missingArgs != null) {
            logger.warning(method, missingArgs);
            return;
        }
        
        String name = requestInfo.get("name").toString();
        JSONObject result;
        
        try {
            gcpGet.getComputeClient().networks().delete(projectID, name).execute();
            //HttpRequest request = gcpGet.getComputeClient().networks().delete(projectID, name).buildHttpRequest();
            //result = gcpGet.makeRequest(request);
            //System.out.printf("result: %s\n", result);
            //remove the metadata entry
            gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("vpc", name), null);
            
        } catch (IOException e) {
            System.out.printf("COMMIT ERROR\n");
        }
    }
    
    private void createSubnet(JSONObject requestInfo) {
        String method = "createSubnet";
        String missingArgs = checkArgs(requestInfo, "vpcName", "uri", "cidr", "region", "name");
        if (missingArgs != null) {
            logger.warning(method, missingArgs);
            return;
        }
        
        //vpcUri is not a MAX but a google URI
        String vpcName = requestInfo.get("vpcName").toString();
        String vpcFormat = "https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s";
        String vpcUri = String.format(vpcFormat, projectID, vpcName);
        String subnetUri = requestInfo.get("uri").toString();
        String cidr = requestInfo.get("cidr").toString();
        String subnetRegion = requestInfo.get("region").toString();
        String subnetName = requestInfo.get("name").toString();
        
        Subnetwork subnet = new Subnetwork();
        subnet.setIpCidrRange(cidr).setName(subnetName).setRegion(subnetRegion).setNetwork(vpcUri);
        
        try {
            HttpRequest request = gcpGet.getComputeClient().subnetworks().insert(projectID, subnetRegion, subnet).buildHttpRequest();
            if (waitRequest(request)) {
                gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("subnet", vpcName, subnetRegion, subnetName), subnetUri);
            } else {
                System.out.printf("an error occurred\n");
            }
            
        } catch (IOException e) {
            System.out.printf("COMMIT ERROR\n");
        }
    }
    
    private void deleteSubnet(JSONObject requestInfo) {
        String method = "deleteSubnet";
        String missingArgs = checkArgs(requestInfo, "name", "vpcName", "region");
        if (missingArgs != null) {
            logger.warning(method, missingArgs);
            return;
        }
        
        String name = requestInfo.get("name").toString();
        String vpcName = requestInfo.get("vpcName").toString();
        String region = requestInfo.get("region").toString();
        
        String subnetFormat = "projects/%s/global/networks/%s/regions/%s/subnets/%s";
        String subnetUri = String.format(subnetFormat, projectID, vpcName, region, name);
        
        try {
            HttpRequest request = gcpGet.getComputeClient().subnetworks().delete(projectID, region, name).buildHttpRequest();
            JSONObject result = gcpGet.makeRequest(request);
            //remove the metadata entry
            gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("subnet", vpcName, region, name), null);
            
        } catch (IOException e) {
            System.out.printf("COMMIT ERROR %s\n", e);
        }
    }
    
    private void createInstance(JSONObject requestInfo) {
        String method = "createInstance";
        String missingArgs = checkArgs(requestInfo, "name", "uri", "zone", "machineType", "sourceImage", "diskType", "diskSize");
        if (missingArgs != null) {
            logger.warning(method, missingArgs);
            return;
        }
        
        String name = requestInfo.get("name").toString();
        String uri = requestInfo.get("uri").toString();
        String zone = requestInfo.get("zone").toString();
        String machineFormat = "zones/%s/machineTypes/%s";
        String machineType = String.format(machineFormat, zone, requestInfo.get("machineType"));
        String sourceImage = requestInfo.get("sourceImage").toString();
        String diskFormat = "projects/%s/zones/us-central1-c/diskTypes/%s";
        String diskType = String.format(diskFormat, projectID, requestInfo.get("diskType"));
        long diskSizeGb = Integer.parseInt(requestInfo.get("diskSize").toString());
        
        //Used by NICs
        String vpcFormat = "projects/%s/global/networks/%s";
        String subnetFormat = "projects/%s/regions/%s/subnetworks/%s";
        String vpcName, subnetRegion, subnetName, subnetIP;
        
        Instance instance = new Instance()
            .setName(name)
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
                
                AccessConfig access = new AccessConfig()
                    .setName("External Nat").setType("ONE_TO_ONE_NAT");
                if (nicInfo.containsKey("ip")) {
                    //External ip
                    access.setNatIP(nicInfo.get("ip").toString());
                }
                
                accessList = new ArrayList<>();
                accessList.add(access);
            
                NetworkInterface netiface = new NetworkInterface()
                    .setNetwork(String.format(vpcFormat, projectID, vpcName))
                    .setSubnetwork(String.format(subnetFormat, projectID, subnetRegion, subnetName))
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
            
            if (waitRequest(request)) {
                gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("vm", zone, name), uri);
            } else {
                System.out.printf("an error occurred\n");
            }
        } catch (IOException e) {
            //TODO
            System.out.printf("COMMIT ERROR: %s\n", e);
        }
    }
    
    private void deleteInstance(JSONObject requestInfo) {
        String method = "deleteInstance";
        String missingArgs = checkArgs(requestInfo, "name", "zone");
        if (missingArgs != null) {
            logger.warning(method, missingArgs);
            return;
        }
        
        String name = requestInfo.get("name").toString();
        String zone = requestInfo.get("zone").toString();
        JSONObject result;
        
        try {
            HttpRequest request = gcpGet.getComputeClient().instances().delete(projectID, zone, name).buildHttpRequest();
            result = gcpGet.makeRequest(request);
            //remove the metadata entry
            gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("vm", zone, name), null);
            
        } catch (IOException e) {
            System.out.printf("COMMIT ERROR: %s\n", e);
        }
    }
    
    private void createBucket(JSONObject requestInfo) {
        String method = "createBucket";
        String missingArgs = checkArgs(requestInfo, "name", "uri");
        if (missingArgs != null) {
            logger.warning(method, missingArgs);
            return;
        }
        
        String name = requestInfo.get("name").toString();
        String uri = requestInfo.get("uri").toString();
        Bucket bucket = new Bucket();
        bucket.setName(name);
        
        try {
            HttpRequest request = gcpGet.getStorageClient().buckets().insert(projectID, bucket).buildHttpRequest();
            JSONObject result = gcpGet.makeRequest(request);
            gcpGet.modifyCommonMetadata(GcpModelBuilder.getResourceKey("bucket", name), uri);
        } catch (IOException e) {
            System.out.printf("COMMIT ERROR: %s\n", e);
        }
    }
    
    String checkArgs(JSONObject request, String ...reqArgs) {
        HashSet<String> missingArgs = new HashSet<>();
        
        for (String arg : reqArgs) {
            if (!request.containsKey(arg)) {
                missingArgs.add(arg);
            }
        }
        
        if (missingArgs.isEmpty()) {
            return null;
        } else {
            String type, args;
            type = request.get("type").toString();
            args = missingArgs.toString();
            return String.format("%s request is missing these args: %s", type, args);
        }
    }

    JSONObject parseJSONException(GoogleJsonResponseException e) {
        String errorJson = e.getDetails().toString();
        JSONObject error;
        JSONArray errors;
        try {
            error = (JSONObject) parser.parse(errorJson);
            errors = (JSONArray) error.get("errors");
            return (JSONObject) errors.get(0);
        } catch (ParseException pe) {
            //It's not even JSON?
            System.out.printf("Expected JSON, but got %s\n", errorJson);
        }
        return null;
    }
    
    boolean waitRequest(HttpRequest request) throws IOException {
        //wait 5 seconds between requests, and send up to 5 requests
        return waitRequest(request, 5000, 5);
    }
    
    boolean waitRequest(HttpRequest request, long requestInterval, int maxRequests) throws IOException {
        /*
        this function retries a request multiple times if resourceNotReady is thrown as a response
        the funtion stops attempting after maxRequests has been reached
        */
        long start = System.currentTimeMillis(), max = 60000;
        int numRequests = 0;
        /*
        This set defines errors which may disappear if the request is retried
        resourceInUse may or may not disappear
        */
        HashSet<String> reasons = new HashSet<>();
        reasons.add("resourceInUseByAnotherResource");
        reasons.add("resourceNotReady");
        
        if (requestInterval > max) {
            //This prevents long waits for a single resource
            requestInterval = max;
        }
        
        do {
            if (System.currentTimeMillis() - start > max) {
                //resource is taking too long
                System.out.println("Resource timeout");
                return false;
            }
            
            if (numRequests >= maxRequests) {
                //too many failures!
                return false;
            }
            
            try {
                numRequests++;
                gcpGet.makeRequest(request);
                return true;
            } catch (GoogleJsonResponseException e) {
                JSONObject errorJson = parseJSONException(e);
                
                if (reasons.contains(errorJson.get("reason"))) {
                    //wait for resource to become ready
                    try {
                        Thread.sleep(requestInterval);
                    } catch (InterruptedException ie) {
                        //TODO
                    }
                } else {
                    System.out.println(errorJson);
                    return false;
                }
            }
        } while (true);
    }
    
}
