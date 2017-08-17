/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import java.security.GeneralSecurityException;
import com.google.api.services.compute.Compute;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;

//import com.google.cloud.compute.*;

/**
 *
 * @author raymonddsmith
 */
public class GoogleCloudGet {
    
    private GoogleCloudAuthenticate authenticate = null;
    private Compute computeClient = null;
    private String projectID;
    private String region;
    
    public GoogleCloudGet(String jsonAuth, String projectID, String region) {
        authenticate = new GoogleCloudAuthenticate(jsonAuth);
        this.projectID = projectID;
        this.region = region;
        
        try {
            HttpTransport trans = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory j = new JacksonFactory();
            computeClient = new Compute(trans, j, authenticate.getCredentials());
        } catch (GeneralSecurityException e) {
           
        } catch (IOException ex) {
            
        }
        
    }
    
    public String getAllInstances() {
        String output = null;
        try {
            output = computeClient.instances().list(projectID, region).buildHttpRequest().execute().getContent().toString();
            System.out.print(output);
        } catch (IOException ex) {
            
        }
        
        return output;
    }
    
    public Compute getComputeClient() {
        return computeClient;
    }
    
    
}
