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
            computeClient = new Compute(trans, j, authenticate.getCredentials());
            storageClient = new Storage(trans, j, authenticate.getCredentials());
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
    
    public JSONObject getSubnets() {
        try {
            //returns an aggregated list of subnets
            return makeRequest(computeClient.subnetworks().aggregatedList(projectID).buildHttpRequest());
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
            return makeRequest(computeClient.instances().list(projectID, region).buildHttpRequest());
        } catch (IOException e) {
            return null;
        }
    }
    
    public static String getVPCName (String vpc) {
        /*
        gets the name of a vpc from the url.
        the url should look like this
        https://www.googleapis.com/compute/v1/projects/elegant-works-176420/global/networks/default
        We want the part at the end.
        */
        return vpc.replaceAll(".*networks/", "");
        /*
        int index, length;
        String s = "global/networks/";
        index = vpc.indexOf(s);
        length = s.length();
        return vpc.substring(index+length);
        //*/
    }
    
    public static String getSubnetName (String subnet) {
        /*
        gets the name of a subnet from the url.
        the url should look like this
        https://www.googleapis.com/compute/v1/projects/elegant-works-176420/regions/us-central1/subnetworks/default
        We want the part at the end.
        */
        return subnet.replaceAll(".*subnetworks/", "");
        /*
        int index, length;
        String s = "subnetworks/";
        index = subnet.indexOf(s);
        length = s.length();
        return subnet.substring(index+length);
        //*/
    }
    
    public static String getSubnetRegion (String subnet) {
        
        return subnet.replaceAll(".*regions/", "").replaceAll("/subnetworks.*", "");
        /*
        int index, length;
        String s = "regions/";
        index = subnet.indexOf(s);
        length = s.length();
        return subnet.substring(index+length).replaceAll("/subnetworks.*", "");
        //*/
    }
    
    /*
    public static JSONArray combineAggregatedList(JSONObject input, String key) {
        //*
        An aggregated list gets resources from multiple regions, and puts them
        in separate lists keyed by region string, like so:
        region -> { key -> [list of results] }
        ///
        JSONArray output = new JSONArray();
        JSONObject temp;
        for (Object o : input.keySet()) {
            temp = (JSONObject) input.get(o);
            output.addAll( (JSONArray) temp.get(key));
        }
        return output;
    }
    */
    
    public static String getInstancePublicIP(JSONObject netiface) {
        if (netiface == null) return "none";
        JSONArray accessConfigs = (JSONArray) netiface.get("accessConfigs");
        if (accessConfigs == null) return "none";
        JSONObject config = (JSONObject) accessConfigs.get(0);
        return config.get("natIP").toString();
    }
}
