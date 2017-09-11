/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.services.compute.Compute;
import com.google.api.services.storage.Storage;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author raymonddsmith
 */
public class GoogleCloudGet {
    
    private GoogleCloudAuthenticate authenticate = null;
    private Compute computeClient = null;
    private Storage storageClient = null;
    private String projectID;
    private String region;
    private JSONParser parser = new JSONParser();
    
    public GoogleCloudGet(String jsonAuth, String projectID, String region) {
        authenticate = new GoogleCloudAuthenticate(jsonAuth);
        this.projectID = projectID;
        this.region = region;
        
        try {
            HttpTransport trans = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory j = new JacksonFactory();
            
            Compute.Builder cb = new Compute.Builder(trans, j, authenticate.getCredentials());
            Storage.Builder sb = new Storage.Builder(trans, j, authenticate.getCredentials());
            cb.setApplicationName("gcp-cloud");
            sb.setApplicationName("gcp-cloud");
            computeClient = cb.build();
            storageClient = sb.build();
        } catch (GeneralSecurityException e) {
           
        } catch (IOException e) {
            
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
    
    public JSONObject getVmInstances() {
        try {
            //returns a list of vm instances wiithin a region
            return makeRequest(computeClient.instances().list(projectID, region).buildHttpRequest());
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
    
    
    //Static functions follow
    public static String getVPCName (String vpc) {
        /*
        gets the name of a vpc from the url.
        the url should look like this
        https://www.googleapis.com/compute/v1/projects/elegant-works-176420/global/networks/default
        We want the part at the end.
        */
        return vpc.replaceAll(".*networks/", "");
    }
    
    public static String getSubnetName (String subnet) {
        /*
        gets the name of a subnet from the url.
        the url should look like this
        https://www.googleapis.com/compute/v1/projects/elegant-works-176420/regions/us-central1/subnetworks/default
        We want the part at the end.
        */
        return subnet.replaceAll(".*subnetworks/", "");
    }
    
    public static String getSubnetRegion (String subnet) {
        return subnet.replaceAll(".*regions/", "").replaceAll("/subnetworks.*", "");
    }
    
    public static String getInstancePublicIP(JSONObject netiface) {
        if (netiface == null) return "none";
        JSONArray accessConfigs = (JSONArray) netiface.get("accessConfigs");
        if (accessConfigs == null) return "none";
        JSONObject config = (JSONObject) accessConfigs.get(0);
        return config.get("natIP").toString();
    }
}
