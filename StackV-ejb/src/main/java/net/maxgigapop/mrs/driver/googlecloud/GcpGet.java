/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.api.services.compute.Compute;
import com.google.api.services.storage.Storage;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.model.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author raymonddsmith
 */
public class GcpGet {
    
    private GcpAuthenticate authenticate;
    private Compute computeClient;
    private Storage storageClient;
    private String projectID;
    private JSONParser parser = new JSONParser();
    
    public GcpGet(String jsonAuth, String projectID) {
        authenticate = new GcpAuthenticate(jsonAuth);
        this.projectID = projectID;
        
        try {
            HttpTransport trans = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory j = new JacksonFactory();
            
            Compute.Builder cb = new Compute.Builder(trans, j, authenticate.getCredentials());
            Storage.Builder sb = new Storage.Builder(trans, j, authenticate.getCredentials());
            cb.setApplicationName("gcp-cloud");
            sb.setApplicationName("gcp-cloud");
            computeClient = cb.build();
            storageClient = sb.build();
        } catch (GeneralSecurityException | IOException e) {
            //TODO log
        }
    }
    
    public Compute getComputeClient() { return computeClient; }

    public Storage getStorageClient() { return storageClient; }
    
    public JSONObject makeRequest(HttpRequest request) throws IOException {
        JSONObject output = null;
        try {
            return (JSONObject) parser.parse(request.execute().parseAsString());
        } catch (ParseException e) {
            //this should never occur unless google returns faulty JSON
            return null;
        }
    }
    
    public JSONObject getVPCs() {
        try {
            return makeRequest(computeClient.networks().list(projectID).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public JSONObject getVPC(String vpc) {
        try {
            return makeRequest(computeClient.networks().get(projectID, vpc).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public JSONObject getSubnet(String region, String subnet) {
        try {
            return makeRequest(computeClient.subnetworks().get(projectID, region, subnet).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public JSONObject getVmInstances(String region) {
        try {
            //returns a list of vm instances wiithin a region
            return makeRequest(computeClient.instances().list(projectID, region).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    //unlike most gcp get functions, this does not return the raw google response
    //which is always a JSONObject.
    public JSONArray getAggregatedVmInstances() {
        try {
            JSONObject result, temp;
            JSONArray zoneResult, output = new JSONArray();
            result = makeRequest(computeClient.instances().aggregatedList(projectID).buildHttpRequest());
            result = (JSONObject) result.get("items");
            
            for (Object key : result.keySet()) {
                //if the jsonobject contains the key "instances", then there are vms in that zone
                temp = (JSONObject) result.get(key);
                if (temp.containsKey("instances")) {
                    zoneResult = (JSONArray) temp.get("instances");
                    output.addAll(zoneResult);
                }
            }
            
            return output;
        } catch (IOException e) {
            return null;
        }
    }
    
    public JSONObject getRoutes() {
        try {
            return makeRequest(computeClient.routes().list(projectID).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public JSONObject getBuckets() {
        try {
            return makeRequest(storageClient.buckets().list(projectID).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public JSONObject getVolume(String zone, String name) {
        try {
            return makeRequest(computeClient.disks().get(projectID, zone, name).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public JSONObject getProject() {
        try {
            return makeRequest(computeClient.projects().get(projectID).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public HashMap getCommonMetadata() {
        /*
        this makes a get project request, and then extracts the commonInstanceMetadata
        from the result and places it in a hashmap.
        */
        JSONObject project = getProject();
        if (project == null || !project.containsKey("commonInstanceMetadata")) return null;
        JSONObject projectMetadata = (JSONObject) getProject().get("commonInstanceMetadata");
        JSONArray items = (JSONArray) projectMetadata.get("items");
        JSONObject item;
        HashMap<String, String> output = new HashMap();
        
        if (items == null) {
            //TODO log
            System.out.printf("found metadata, but items = null. Displaying metadata: %s\n", projectMetadata);
        } else {
            for (Object o : items) {
                item = (JSONObject) o;
                output.put(item.get("key").toString(), item.get("value").toString());
            }
        }
        return output;
    }
    
    public JSONObject modifyCommonMetadata(String key, String value) {
        /*
        This function is used to add or remove keys from common instance metadata
        When both key and value are set, the entry pair will be added to the metadata
        When value is null and key is present in metadata, the key will be removed.
        If key is null or an exception is thrown, or key cannot be deleted, null is returned.
        Otherwise, the result of the setCommonInstanceMetadata request is returned.
        */
        
        if (key == null) return null;
        
        String method = "modifyCommonMetadata";
        boolean deletedItem = false;
        Metadata meta = new Metadata();
        Metadata.Items newItem;
        ArrayList newItems = new ArrayList();
        JSONArray oldItems;
        JSONObject project = getProject();
        //project is a JSONObject with field commonInstanceMetadata, which in turn is a JSONObject with a JSONArray items
        if (project == null || !project.containsKey("commonInstanceMetadata")) {
            //olditems is empty
            oldItems = null;
        } else {
            oldItems = (JSONArray) ( (JSONObject) project.get("commonInstanceMetadata")).get("items");
        }
        
        String oldKey;
        
        if (value != null) {
            newItem = new Metadata.Items();
            newItem.setKey(key);
            newItem.setValue(value);
            newItems.add(newItem);
        }
        
        if (oldItems == null) {
            if (value == null) {
                return null;
            }
        } else {
            for (Object o: oldItems) {
                JSONObject oldItem = (JSONObject) o;
                newItem = new Metadata.Items();
                newItem.setKey(oldItem.get("key").toString());
                newItem.setValue(oldItem.get("value").toString());
                //if the value is null and key matches, exclude this entry
                if (value == null && newItem.getKey().equals(key)) {
                    deletedItem = true;
                } else {
                    newItems.add(newItem);
                }
            }
        }
        
        //return null, as key to be deleted was not found
        if (value == null && !deletedItem) return null;
        
        meta.setItems(newItems);
        
        try {
            return makeRequest(computeClient.projects().setCommonInstanceMetadata(projectID, meta).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public static String parseGoogleURI(String uri, String key) {
        /*
        retrieves strong content from a relevant section of a google URI
        for example, if key is "regions" and uri is:
        https://www.googleapis.com/compute/v1/projects/elegant-works-176420/regions/southamerica-east1/subnetworks/default
        the result would be "southamerica-east1"
        We do this by replacing everything up to and including "key/" and then clearing everything after the final "/"
        */
        return uri.replaceAll(".*"+key+"/", "").replaceAll("/.*", "");
    }

    public static String getInstancePublicIP(JSONObject netiface) {
        if (netiface == null) return "none";
        JSONArray accessConfigs = (JSONArray) netiface.get("accessConfigs");
        if (accessConfigs == null) return "none";
        JSONObject config = (JSONObject) accessConfigs.get(0);
        if (config == null || !config.containsKey("natIP")) return "none";
        return config.get("natIP").toString();
    }
}
