/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import net.maxgigapop.mrs.common.StackLogger;

public class GoogleCloudAuthenticate {
    /*
    This class creates and stores a GoogleCredential from a String or JSON object.
    */
    public static final StackLogger logger = GoogleCloudDriver.logger;
    private GoogleCredential credential = null;
    
    public GoogleCloudAuthenticate (JSONObject jsonInput) {
        this(jsonInput.toString());
    }
    
    public GoogleCloudAuthenticate (String jsonString) {
        String method = "GoogleCloudAuthenticate";
        try {
            InputStream resourceAsStream = new ByteArrayInputStream(jsonString.getBytes());
            credential = GoogleCredential.fromStream(resourceAsStream);
            ArrayList<String> scopes = new ArrayList<>();
            scopes.add("https://www.googleapis.com/auth/cloud-platform");
            credential.createScoped(scopes);
        } catch (IOException e) {
            logger.error(method, "error while authenticating: "+e.getMessage());
        }
    }
    
    public GoogleCredential getCredentials () {
        return credential;
    }
}