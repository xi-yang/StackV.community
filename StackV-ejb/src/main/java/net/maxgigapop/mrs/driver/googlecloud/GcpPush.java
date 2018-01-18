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
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Firewall.Allowed;
import com.google.api.services.compute.model.Tags;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        //Buckets, Instances, VPCs, Subnets
        
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
        logger.start(method);
        HttpRequest request;
        
        HashMap<String, String> metadata = gcpGet.getCommonMetadata();
        HashMap<String, String> tempAdd = null, add = new HashMap<>();
        HashSet<String> tempRemove = null, remove = new HashSet<>();
        
        for (Object o : requests) {
            JSONObject requestInfo = (JSONObject) o;
            String type = requestInfo.containsKey("type") ? requestInfo.get("type").toString() : "null";
            logger.trace_start(method+"."+type, requestInfo.toString());
            
            switch (type) {
            case "create_vpc":
                tempAdd = createVpc(requestInfo);
            break;
            case "delete_vpc":
                tempRemove = deleteVpc(requestInfo);
            break;
            case "create_subnet":
                tempAdd = createSubnet(requestInfo);
            break;
            case "delete_subnet":
                tempRemove = deleteSubnet(requestInfo);
            break;
            case "create_instance":
                tempAdd = createInstance(requestInfo);
            break;
            case "delete_instance":
                tempRemove = deleteInstance(requestInfo);
            break;
            case "create_bucket":
                tempAdd = createBucket(requestInfo);
            break;
            case "delete_bucket":
                tempRemove = deleteBucket(requestInfo);
            break;
            case "add_firewall_rule":
                tempAdd = addFirewallRule(requestInfo);
            break;
            case "remove_firewall_rule":
                tempRemove = removeFirewallRule(requestInfo);
            break;
            case "null":
                logger.warning(method, "COMMIT ERROR: encountered request without type");
            break;
            default:
                logger.warning(method, "COMMIT ERROR: encountered request of unknown type: "+ type);
            break;
            }
            logger.trace_end(method+"."+type);
            if (tempAdd != null) add.putAll(tempAdd);
            if (tempRemove != null) remove.addAll(tempRemove);
            
            
        }
        logger.trace(method, "updating metadata");
        //System.out.printf("old: %s\nadd: %s\nremove: %s\n", metadata, add, remove);
        gcpGet.modifyCommonMetadata(metadata, add, remove);
        logger.end(method);
    }
    
    private HashMap<String, String> createVpc(JSONObject requestInfo) {
        String method = "createVpc";
        HashMap<String, String> output = new HashMap<>();
        String missingArgs = checkArgs(requestInfo, "name", "uri");
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return output;
        }
        
        String name = requestInfo.get("name").toString();
        String uri =  requestInfo.get("uri").toString();
        Network network = new Network();
        network.setName(name).setAutoCreateSubnetworks(false);
        
        try {
            JSONObject result;
            HttpRequest request = gcpGet.getComputeClient().networks().insert(projectID, network).buildHttpRequest();
            result = gcpGet.makeRequest(request);
            logger.trace(method, result.toString());
            //Add this vpc's uri to the metadata table
            output.put(GcpModelBuilder.getResourceKey("vpc", name), uri);
            return output;
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        return null;
    }
    
    private HashSet<String> deleteVpc(JSONObject requestInfo) {
        String method = "deleteVpc";
        String missingArgs = checkArgs(requestInfo, "name");
        HashSet<String> output = new HashSet<>();
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return null;
        }
        
        String name = requestInfo.get("name").toString();
        JSONObject result;
        
        try {
            HttpRequest request = gcpGet.getComputeClient().networks().delete(projectID, name).buildHttpRequest();
            String error = waitRequest(request, 5000, 10, true);
            if (error == null) {
                output.add(GcpModelBuilder.getResourceKey("vpc", name));
                return output;
            } else {
                logger.warning(method, "COMMIT ERROR: " + error);
            }
            
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        return null;
    }
    
    //currently ingress only?
    private HashMap<String, String> addFirewallRule(JSONObject requestInfo) {
        String method = "addFirewallRule";
        String missingArgs = checkArgs(requestInfo, "name", "vpc", "sources", "allowed");
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return null;
        }
        
        //TODO add tags as an optional argument
        
        String name = requestInfo.get("name").toString();
        String vpcName = requestInfo.get("vpc").toString();
        String vpcUri = String.format("projects/%s/global/networks/%s", projectID, vpcName);
        String sourceString = requestInfo.get("sources").toString();
        String allowedString = requestInfo.get("allowed").toString();
        List<Allowed> allowedList = new ArrayList<>();
        
        //TODO regex
        for (String port : Arrays.asList(allowedString.split(","))) {
            Allowed allowed = new Allowed();
            allowed.setIPProtocol(port);
            allowedList.add(allowed);
        }
        
        //TODO regex to confirm sourcelist matches proper format
        List<String> sourceList = Arrays.asList(sourceString.split(","));
        List<String> tagList = new ArrayList<>();
        tagList.add(name);  //Every firewall's name is a tag
        
        Firewall fire = new Firewall();
        fire.setName(String.format("%s-%s", vpcName, name)); //This ensures that firewall names never collide between vpcs
        fire.setNetwork(vpcUri);
        fire.setSourceRanges(sourceList);
        fire.setTargetTags(tagList);
        fire.setAllowed(allowedList);
        
        try {
            HttpRequest request = gcpGet.getComputeClient().firewalls().insert(projectID, fire).buildHttpRequest();
            String error = waitRequest(request);
            if (error == null) {
                //possibility to add firewall uri to metadata here.
                return null;
            } else {
                logger.warning(method, "COMMIT ERROR: " + error);
            }
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        
        return null;
    }
    
    private HashSet<String> removeFirewallRule(JSONObject requestInfo) {
        String method = "removeFirewallRule";
        String missingArgs = checkArgs(requestInfo, "name");
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return null;
        }
        
        String name = requestInfo.get("name").toString();
        
        try {
            HttpRequest request = gcpGet.getComputeClient().firewalls().delete(projectID, name).buildHttpRequest();
            request.execute();
            
        } catch (IOException e) {
            
        }
        return null;
    }
    
    private HashMap<String, String> createSubnet(JSONObject requestInfo) {
        String method = "createSubnet";
        HashMap<String, String> output = new HashMap<>();
        String missingArgs = checkArgs(requestInfo, "vpcName", "uri", "cidr", "region", "name");
        if (missingArgs != null) {
            logger.warning(method, missingArgs);
            return null;
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
            String error = waitRequest(request);
            if (error == null) {
                output.put(GcpModelBuilder.getResourceKey("subnet", vpcName, subnetRegion, subnetName), subnetUri);
                return output;
            } else {
                logger.warning(method, "COMMIT ERROR: " + error);
            }
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        return null;
    }
    
    private HashSet<String> deleteSubnet(JSONObject requestInfo) {
        String method = "deleteSubnet";
        String missingArgs = checkArgs(requestInfo, "name", "vpcName", "region");
        HashSet<String> output = new HashSet<>();
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return null;
        }
        
        String name = requestInfo.get("name").toString();
        String vpcName = requestInfo.get("vpcName").toString();
        String region = requestInfo.get("region").toString();
        
        String subnetFormat = "projects/%s/global/networks/%s/regions/%s/subnets/%s";
        String subnetUri = String.format(subnetFormat, projectID, vpcName, region, name);
        
        try {
            HttpRequest request = gcpGet.getComputeClient().subnetworks().delete(projectID, region, name).buildHttpRequest();
            String error = waitRequest(request);
            if (error == null) {
                output.add(GcpModelBuilder.getResourceKey("subnet", vpcName, region, name));
                return output;
            } else {
                logger.warning(method, error);
            }
            
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        return null;
    }
    
    private HashMap<String, String> createInstance(JSONObject requestInfo) {
        String method = "createInstance";
        HashMap<String, String> output = new HashMap<>();
        String missingArgs = checkArgs(requestInfo, "name", "uri", "zone", "instance", "sourceImage", "diskType", "diskSize", "firewallTags");
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return null;
        }
        
        String name = requestInfo.get("name").toString();
        String uri = requestInfo.get("uri").toString();
        String zone = requestInfo.get("zone").toString();
        String machineFormat = "zones/%s/machineTypes/%s";
        String machineType = String.format(machineFormat, zone, requestInfo.get("instance"));
        String sourceImage = requestInfo.get("sourceImage").toString();
        String diskFormat = "projects/%s/zones/us-central1-c/diskTypes/%s";
        String diskType = String.format(diskFormat, projectID, requestInfo.get("diskType"));
        long diskSizeGb = Integer.parseInt(requestInfo.get("diskSize").toString());
        String firewallTags = requestInfo.get("firewallTags").toString();
        
        //Used by NICs
        String vpcFormat = "projects/%s/global/networks/%s";
        String subnetFormat = "projects/%s/regions/%s/subnetworks/%s";
        String vpcName, subnetRegion, subnetName, subnetIP, nicUri, nicName;
        
        Tags tags = new Tags();
        tags.setItems(Arrays.asList(firewallTags.split(",")));
        
        Instance instance = new Instance()
            .setName(name)
            .setMachineType(machineType)
            .setTags(tags);
        
        //Add NICs
        JSONArray nics = (JSONArray) requestInfo.get("nics");
        
        if (nics != null) {
            ArrayList<NetworkInterface> netifaces = new ArrayList<>();
            ArrayList<AccessConfig> accessList;
            int i = 0;

            for (Object o : nics) {
                JSONObject nicInfo = (JSONObject) o;
                
                missingArgs = checkArgs(nicInfo, "vpc", "region", "subnet", "uri");
                if (missingArgs != null) {
                    logger.warning(method, "COMMIT ERROR: " + missingArgs);
                    return null;
                }
                
                nicUri = nicInfo.get("uri").toString();
                nicName = "nic"+nicInfo.get("nic").toString();
                vpcName = nicInfo.get("vpc").toString();
                subnetRegion = nicInfo.get("region").toString();
                subnetName = nicInfo.get("subnet").toString();
                
                output.put(GcpModelBuilder.getResourceKey("nic", vpcName, name, nicName), nicUri);
                
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
            
            String error = waitRequest(request);
            if (error == null) {
                output.put(GcpModelBuilder.getResourceKey("vm", zone, name), uri);
                return output;
            } else {
                logger.warning(method, "COMMIT ERROR: " + error);
            }
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        return null;
    }
    
    private HashSet<String> deleteInstance(JSONObject requestInfo) {
        String method = "deleteInstance";
        String missingArgs = checkArgs(requestInfo, "name", "zone");
        HashSet<String> output = new HashSet<>();
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return null;
        }
        
        String name = requestInfo.get("name").toString();
        String zone = requestInfo.get("zone").toString();
        JSONObject result;
        
        try {
            HttpRequest request = gcpGet.getComputeClient().instances().delete(projectID, zone, name).buildHttpRequest();
            result = gcpGet.makeRequest(request);
            logger.trace(method, result.toString());
            //remove the metadata entry
            output.add(GcpModelBuilder.getResourceKey("vm", zone, name));
            return output;
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        return null;
    }
    
    private HashMap<String, String> createBucket(JSONObject requestInfo) {
        String method = "createBucket";
        HashMap<String, String> output = new HashMap<>();
        String missingArgs = checkArgs(requestInfo, "name", "uri");
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return null;
        }
        
        String name = requestInfo.get("name").toString();
        String uri = requestInfo.get("uri").toString();
        Bucket bucket = new Bucket();
        bucket.setName(name);
        
        try {
            HttpRequest request = gcpGet.getStorageClient().buckets().insert(projectID, bucket).buildHttpRequest();
            JSONObject result = gcpGet.makeRequest(request);
            logger.trace(method, result.toString());
            output.put(GcpModelBuilder.getResourceKey("bucket", name), uri);
            return output;
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        return null;
    }
    
    private HashSet<String> deleteBucket(JSONObject requestInfo) {
        String method = "createBucket";
        String missingArgs = checkArgs(requestInfo, "name");
        HashSet<String> output = new HashSet<>();
        if (missingArgs != null) {
            logger.warning(method, "COMMIT ERROR: " + missingArgs);
            return null;
        }
        
        String name = requestInfo.get("name").toString();
        
        try {
            HttpRequest request = gcpGet.getStorageClient().buckets().delete(name).buildHttpRequest();
            JSONObject result = gcpGet.makeRequest(request);
            logger.trace(method, result.toString());
            output.add(GcpModelBuilder.getResourceKey("bucket", name));
            return output;
        } catch (IOException e) {
            logger.warning(method, "COMMIT ERROR: " + e.toString());
        }
        return null;
    }
    
    private String checkArgs(JSONObject request, String ...reqArgs) {
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
            //System.out.printf("Expected JSON, but got %s\n", errorJson);
        }
        return null;
    }
    
    String waitRequest(HttpRequest request) throws IOException {
        //wait 5 seconds between requests, and send up to 5 requests
        return waitRequest(request, 5000, 10, false);
    }
    
    String waitRequest(HttpRequest request, long requestInterval, int maxRequests, boolean require404) throws IOException {
        /*
        this function retries a request multiple times if resourceNotReady is thrown as a response
        the funtion stops attempting after maxRequests has been reached
        
        If require404 is true, it means that the request is a delete request and
        the resource delete requests should be re-sent until the result is 404
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
        
        //This prevents long waits for a single resource
        if (requestInterval > max) requestInterval = max;
        
        
        do {
            if (System.currentTimeMillis() - start > max) {
                //resource is taking too long
                return "Resource timeout";
            }
            
            if (numRequests >= maxRequests) {
                //too many failures!
                return "Too many attempts";
            }
            
            try {
                numRequests++;
                gcpGet.makeRequest(request);
                if (!require404) {
                    return null;
                }
            } catch (GoogleJsonResponseException e) {
                JSONObject errorJson = parseJSONException(e);
                if (errorJson == null || !errorJson.containsKey("reason")) {
                    return "error while parsing google JSON: "+errorJson;
                }
                
                String reason = errorJson.get("reason").toString();
                if (require404 && reason.equals("notFound")) {
                    //resource was successfully deleted
                    return null;
                } if (reasons.contains(reason)) {
                    //wait for resource to become ready
                    try {
                        Thread.sleep(requestInterval);
                    } catch (InterruptedException ie) {
                        //TODO
                    }
                } else {
                    return errorJson.toString();
                }
            }
        } while (true);
    }

}
