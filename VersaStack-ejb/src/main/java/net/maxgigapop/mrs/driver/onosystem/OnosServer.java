/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.onosystem;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import net.maxgigapop.mrs.driver.OnosRESTDriver;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 *
 * @author muzcategui
 */
public class OnosServer {
    public int qtyDevices=0;
    public int qtyLinks=0;
    public int qtyHosts=0;
    public int qtyPorts=0;
    private static final Logger logger = Logger.getLogger(OnosRESTDriver.class.getName());

    //pull Devices data
    public String[][] getOnosDevices(String subsystemBaseUrl) throws MalformedURLException, IOException, ParseException {
      
        URL url = new URL(subsystemBaseUrl+"/devices");
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String responseStr = this.executeHttpMethod(url, conn, "GET", null);
        responseStr=responseStr.replaceAll("(\\[|\\]|,|\\{|\\})","$1\n");
        responseStr=responseStr.replaceAll("\"\\}", "\"\n\\}");
        responseStr=responseStr.replaceAll("\\}\n,","\\},");
        //System.out.println(responseStr); 
        int realSize=responseStr.split("\n").length;
        String devicesArray[]=new String[realSize];
        devicesArray=responseStr.split("\n");
        
       
        for(int i=0;i<realSize;i++){
            
            if(devicesArray[i].matches("(.*)\"id\":(.*)")){
            qtyDevices++; 
         
            }
        }
        String device[][]=new String[3][qtyDevices];
        
        int j=0;
        for(int i=0;i<realSize;i++){
            
            if(devicesArray[i].matches("(.*)\"id\":(.*)")){
                
                device[0][j]=devicesArray[i].split("^\"id\":\"")[1];
                device[0][j]=device[0][j].split("\",")[0];
                
            }
            else if(devicesArray[i].matches("(.*)\"type\":(.*)")){
                
                device[1][j]=devicesArray[i].split("\"type\":\"")[1];
                device[1][j]=device[1][j].split("\",")[0];
                
            }
            else if(devicesArray[i].matches("(.*)\"available\":(.*)")){
                
                device[2][j]=devicesArray[i].split("\"available\":")[1];
                device[2][j]=device[2][j].split(",")[0];
               
                j++;
            }
            
        }
         return(device);        
    }   
    
    //pull links data
    public String[][] getOnosLinks(String subsystemBaseUrl) throws MalformedURLException, IOException, ParseException {
        
        URL url = new URL(subsystemBaseUrl+"/links");
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String responseStr = this.executeHttpMethod(url, conn, "GET", null);
        responseStr=responseStr.replaceAll("(\\[|\\]|,|\\{|\\})","$1\n");
        responseStr=responseStr.replaceAll("\"\\}", "\"\n\\}");
        responseStr=responseStr.replaceAll("\\}\n,","\\},");
        int realSize=responseStr.split("\n").length;
        String linksArray[]=new String[realSize];
        linksArray=responseStr.split("\n");
        
       
        for(int i=0;i<realSize;i++){
            
            if(linksArray[i].matches("(.*)\"src\":(.*)")){
            qtyLinks++; 
         
            }
        }
        String links[][]=new String[8][qtyLinks];
        
        int j=0;
        for(int i=0;i<realSize;i++){
            
            if(linksArray[i].matches("(.*)\"port\":(.*)")){

                if(linksArray[i-1].matches("(.*)\"src\":(.*)")){
                    links[0][j]=linksArray[i].split("\"port\":\"")[1];
                    links[0][j]=links[0][j].split("\"")[0];
                    links[1][j]=linksArray[i+1].split("\"device\":\"")[1];
                    links[1][j]=links[1][j].split("\"")[0];
                }
            }
            if(linksArray[i].matches("(.*)\"port\":(.*)")){
                if(linksArray[i-1].matches("(.*)\"dst\":(.*)")){
                    links[3][j]=linksArray[i].split("\"port\":\"")[1];
                    links[4][j]=linksArray[i+1].split("\"device\":\"")[1];
                    links[3][j]=links[3][j].split("\"")[0];
                    links[4][j]=links[4][j].split("\"")[0];
                }
                
            }
            
            if(linksArray[i].matches("(.*)\"type\":(.*)")){
                
                links[6][j]=linksArray[i].split("\"type\":\"")[1];
                links[6][j]=links[6][j].split("\"")[0];
            }
            if(linksArray[i].matches("(.*)\"state\":(.*)")){
                
                links[7][j]=linksArray[i].split("\"state\":\"")[1];
                links[7][j]=links[7][j].split("\"")[0];
                j++;
            }
            
        }
         return(links);        
    }   
    
    //pull Device Ports Data
    public String[][] getOnosDevicePorts(String subsystemBaseUrl, String devId) throws MalformedURLException, IOException, ParseException {
        qtyPorts=0;
        URL urlDevPort = new URL(subsystemBaseUrl+"/devices/"+devId+"/ports");
        HttpURLConnection connDevPort = (HttpURLConnection) urlDevPort.openConnection();
        String responseStrDevPort = this.executeHttpMethod(urlDevPort, connDevPort, "GET", null);
        responseStrDevPort=responseStrDevPort.replaceAll("(\\[|\\]|\\{|\\}|\\},)","$1\n");
        responseStrDevPort=responseStrDevPort.replaceAll(",\"",",\n\"");
        responseStrDevPort=responseStrDevPort.replaceAll("\"\\}", "\"\n\\}");
        responseStrDevPort=responseStrDevPort.replaceAll("\\}\n,","\\},");
        responseStrDevPort=responseStrDevPort.replaceAll("\\},\\{","\\},\n\\{");
        int realSize=responseStrDevPort.split("\n").length;
        String devicePortsArray[]=new String[realSize];
        devicePortsArray=responseStrDevPort.split("\n");
        
        for(int i=0;i<realSize;i++){
            
            if(devicePortsArray[i].matches("(.*)\"port\":(.*)")){
            qtyPorts++; 
         
            }
        }
        String devicePorts[][]=new String[5][qtyPorts];
        
        int j=0;
        for(int i=0;i<realSize;i++){
            
            if(devicePortsArray[i].matches("(.*)\"port\":(.*)")){
                
                devicePorts[0][j]=devicePortsArray[i].split("\"port\":\"")[1];
                devicePorts[0][j]=devicePorts[0][j].split("\"")[0];
            }
            else if(devicePortsArray[i].matches("(.*)\"isEnabled\":(.*)")){
                
                devicePorts[1][j]=devicePortsArray[i].split("\"isEnabled\":")[1];
                devicePorts[1][j]=devicePorts[1][j].split(",")[0];
            }
            else if(devicePortsArray[i].matches("(.*)\"type\":(.*)")){
                
                devicePorts[2][j]=devicePortsArray[i].split("\"type\":\"")[1];
                devicePorts[2][j]=devicePorts[2][j].split("\"")[0];
            }
            else if(devicePortsArray[i].matches("(.*)\"portSpeed\":(.*)")){
                
                devicePorts[3][j]=devicePortsArray[i].split("\"portSpeed\":")[1];
                devicePorts[3][j]=devicePorts[3][j].split(",")[0];
            }   
            else if(devicePortsArray[i].matches("(.*)\"portName\":(.*)")){
                
                devicePorts[4][j]=devicePortsArray[i].split("\"portName\":\"")[1];
                devicePorts[4][j]=devicePorts[4][j].split("\"")[0];
                j++;
            }
            
        }
         return(devicePorts);
    }   

        //pull Hosts Data
    public String[][] getOnosHosts(String subsystemBaseUrl) throws MalformedURLException, IOException, ParseException {
        qtyHosts=0;
        URL urlHosts = new URL(subsystemBaseUrl+"/hosts");
        HttpURLConnection connHosts = (HttpURLConnection) urlHosts.openConnection();
        String responseStrHosts = this.executeHttpMethod(urlHosts, connHosts, "GET", null);
        responseStrHosts=responseStrHosts.replaceAll("(\\[|\\]|\\{|\\}|,)","$1\n");
        responseStrHosts=responseStrHosts.replaceAll("\"\\}", "\"\n\\}");
        responseStrHosts=responseStrHosts.replaceAll("\"\\]", "\"\n\\]");
        responseStrHosts=responseStrHosts.replaceAll("\\]\n,","\\],");
        responseStrHosts=responseStrHosts.replaceAll("\\}\n,","\\},");
        responseStrHosts=responseStrHosts.replaceAll("\\},\\{","\\},\n\\{");
        int realSize=responseStrHosts.split("\n").length;
        String hostsArray[]=new String[realSize];
        hostsArray=responseStrHosts.split("\n");
        for(int i=0;i<realSize;i++){
            if(hostsArray[i].matches("(.*)\"id\":(.*)")){
            qtyHosts++; 
         
            }
        }
        String hosts[][]=new String[6][qtyHosts];
        
        int j=0;
        for(int i=0;i<realSize;i++){
            
            if(hostsArray[i].matches("(.*)\"id\":(.*)")){
                
                hosts[0][j]=hostsArray[i].split("\"id\":\"")[1];
                hosts[0][j]=hosts[0][j].split("\"")[0];
            }
            else if(hostsArray[i].matches("(.*)\"mac\":(.*)")){
                
                hosts[1][j]=hostsArray[i].split("\"mac\":\"")[1];
                hosts[1][j]=hosts[1][j].split("\"")[0];
            }
            else if(hostsArray[i].matches("(.*)\"vlan\":(.*)")){
                
                hosts[2][j]=hostsArray[i].split("\"vlan\":\"")[1];
                hosts[2][j]=hosts[2][j].split("\"")[0];
            }
            else if(hostsArray[i].matches("(.*)\"ipAddress\":(.*)")){
                
                hosts[3][j]=hostsArray[i+1].split("\"")[1];
                hosts[3][j]=hosts[3][j].split("\"")[0];
            }   
            else if(hostsArray[i].matches("(.*)\"elementId\":(.*)")){
                
                hosts[4][j]=hostsArray[i].split("\"elementId\":\"")[1];
                hosts[4][j]=hosts[4][j].split("\"")[0];
            }
            else if(hostsArray[i].matches("(.*)\"port\":(.*)")){
                
                hosts[5][j]=hostsArray[i].split("\"port\":\"")[1];
                hosts[5][j]=hosts[5][j].split("\"")[0];
                j++;
            }
            
        }
         return(hosts);
    }   

    //send GET to HTTP server and retrieve response
    public String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }
        logger.log(Level.INFO, "Sending {0} request to URL : {1}", new Object[]{method, url});
        int responseCode = conn.getResponseCode();
        logger.log(Level.INFO, "Response Code : {0}", responseCode);

        StringBuilder responseStr;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }
        return responseStr.toString();
    }

    
    
    
    
}
