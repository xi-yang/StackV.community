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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author muzcategui
 */
public class OnosServer {

    public int qtyDevices = 0;
    public int qtyLinks = 0;
    public int qtyHosts = 0;
    //public int qtyPorts = 0;
    public int qtyFlows = 0;

    private static final Logger logger = Logger.getLogger(OnosRESTDriver.class.getName());

    //pull Devices data
    public String[][] getOnosDevices(String subsystemBaseUrl, String access_key_id, String secret_access_key)
            throws MalformedURLException, IOException, ParseException, NullPointerException {

        URL url = new URL(subsystemBaseUrl + "/devices");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String responseStr = this.executeHttpMethod(url, conn, "GET", null, access_key_id, secret_access_key);
        
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(responseStr);
        JSONArray deviceArray = (JSONArray) jsonObject.get("devices");
        qtyDevices = deviceArray.size();

        String device[][] = new String[qtyDevices][3];
        //device[][0]: deviceID (e.x. of:0000000000000002)
        //device[][1]: deviceType (SWITCH)
        //device[][2]: deviceAvailable (true/false)
        for (int i = 0; i < qtyDevices; i++) {
            JSONObject deviceObj = (JSONObject) deviceArray.get(i);

            device[i][0] = (String) deviceObj.get("id");
            device[i][1] = (String) deviceObj.get("type");
            device[i][2] = String.valueOf(deviceObj.get("available"));
                        
        }
        return device;
    }

    //pull links data
    public String[][] getOnosLinks(String subsystemBaseUrl, String access_key_id, String secret_access_key)
            throws MalformedURLException, IOException, ParseException {

        URL url = new URL(subsystemBaseUrl + "/links");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String responseStr = this.executeHttpMethod(url, conn, "GET", null, access_key_id, secret_access_key);
        
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(responseStr);
        JSONArray linkArray = (JSONArray) jsonObject.get("links");
        qtyLinks = linkArray.size();
        
        String links[][] = new String[qtyLinks][8];
        //links[][0]: src_device; links[][1] src_port; 
        //links[][2]: dest_device; links[][3]: dest_port
        //links[][4]: type; links[][5]: state
        //links[][6]: src_portName
        //links[][7]: dst_portName
        
        for(int i=0; i<qtyLinks; i++){
            JSONObject linkObj = (JSONObject) linkArray.get(i);
            
            JSONObject srcObj = (JSONObject) linkObj.get("src");
            links[i][0] = (String) srcObj.get("device");
            links[i][1] = (String) srcObj.get("port");
            
            JSONObject dstObj = (JSONObject) linkObj.get("dst");
            links[i][2] = (String) dstObj.get("device");
            links[i][3] = (String) dstObj.get("port");
            
            links[i][4] = (String) linkObj.get("type");
            links[i][5] = (String) linkObj.get("state");
        }
 
        return links;
    }

    //pull Device Ports Data
    public String[][] getOnosDevicePorts(String subsystemBaseUrl, String devId, String access_key_id, String secret_access_key)
            throws MalformedURLException, IOException, ParseException {
        //qtyPorts = 0;
        URL urlDevPort = new URL(subsystemBaseUrl + "/devices/" + devId + "/ports");
        HttpURLConnection connDevPort = (HttpURLConnection) urlDevPort.openConnection();
        String responseStr = this.executeHttpMethod(urlDevPort, connDevPort, "GET", null, access_key_id, secret_access_key);

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(responseStr);
        JSONArray portArray = (JSONArray) jsonObject.get("ports");
        int qtyPorts = portArray.size();

        String devicePorts[][] = new String[qtyPorts][5];
        //ports[][0]: port_num; ports[][1]: isEnabled (true/false)
        //ports[][2]: port_type; ports[][3]: port_speed
        //ports[][4]: port_name
        
        for (int i = 0; i < qtyPorts; i++) {
            JSONObject portObj = (JSONObject) portArray.get(i);

            devicePorts[i][0] = (String) portObj.get("port");
            devicePorts[i][1] = String.valueOf(portObj.get("isEnabled"));
            devicePorts[i][2] = (String) portObj.get("type");
            devicePorts[i][3] = String.valueOf(portObj.get("portSpeed"));

            JSONObject annotations = (JSONObject) portObj.get("annotations");
            devicePorts[i][4] = (String) annotations.get("portName");
        }
        
        return devicePorts;
    }

    //pull Hosts Data
    public String[][] getOnosHosts(String subsystemBaseUrl, String access_key_id, String secret_access_key)
            throws MalformedURLException, IOException, ParseException {
        qtyHosts = 0;
        URL urlHosts = new URL(subsystemBaseUrl + "/hosts");
        HttpURLConnection connHosts = (HttpURLConnection) urlHosts.openConnection();
        String responseStrHosts = this.executeHttpMethod(urlHosts, connHosts, "GET", null, access_key_id, secret_access_key);
        responseStrHosts = responseStrHosts.replaceAll("(\\[|\\]|\\{|\\}|,)", "$1\n");
        responseStrHosts = responseStrHosts.replaceAll("\"\\}", "\"\n\\}");
        responseStrHosts = responseStrHosts.replaceAll("\"\\]", "\"\n\\]");
        responseStrHosts = responseStrHosts.replaceAll("\\]\n,", "\\],");
        responseStrHosts = responseStrHosts.replaceAll("\\}\n,", "\\},");
        responseStrHosts = responseStrHosts.replaceAll("\\},\\{", "\\},\n\\{");
        int realSize = responseStrHosts.split("\n").length;
        String hostsArray[] = new String[realSize];
        hostsArray = responseStrHosts.split("\n");
        for (int i = 0; i < realSize; i++) {
            if (hostsArray[i].matches("(.*)\"id\":(.*)")) {
                qtyHosts++;

            }
        }
        String hosts[][] = new String[6][qtyHosts];

        int j = 0;
        for (int i = 0; i < realSize; i++) {

            if (hostsArray[i].matches("(.*)\"id\":(.*)")) {

                hosts[0][j] = hostsArray[i].split("\"id\":\"")[1];
                hosts[0][j] = hosts[0][j].split("\"")[0];
            } else if (hostsArray[i].matches("(.*)\"mac\":(.*)")) {

                hosts[1][j] = hostsArray[i].split("\"mac\":\"")[1];
                hosts[1][j] = hosts[1][j].split("\"")[0];
            } else if (hostsArray[i].matches("(.*)\"vlan\":(.*)")) {

                hosts[2][j] = hostsArray[i].split("\"vlan\":\"")[1];
                hosts[2][j] = hosts[2][j].split("\"")[0];
            } else if (hostsArray[i].matches("(.*)\"ipAddress\":(.*)")) {

                hosts[3][j] = hostsArray[i + 1].split("\"")[1];
                hosts[3][j] = hosts[3][j].split("\"")[0];
            } else if (hostsArray[i].matches("(.*)\"elementId\":(.*)")) {

                hosts[4][j] = hostsArray[i].split("\"elementId\":\"")[1];
                hosts[4][j] = hosts[4][j].split("\"")[0];
            } else if (hostsArray[i].matches("(.*)\"port\":(.*)")) {

                hosts[5][j] = hostsArray[i].split("\"port\":\"")[1];
                hosts[5][j] = hosts[5][j].split("\"")[0];
                j++;
            }

        }
        return (hosts);
    }

    //pull Device Ports Data
    public String[][] getOnosDeviceFlows(String subsystemBaseUrl, String devId, String access_key_id, String secret_access_key)
            throws MalformedURLException, IOException, ParseException {
        qtyFlows = 0;

        URL urlDevFlow = new URL(subsystemBaseUrl + "/flows/" + devId);
        HttpURLConnection connDevFlow = (HttpURLConnection) urlDevFlow.openConnection();
        String responseStrDevFlow = this.executeHttpMethod(urlDevFlow, connDevFlow, "GET", null, access_key_id, secret_access_key);
        responseStrDevFlow = responseStrDevFlow.replaceAll("\\}", "\n\\}");
        responseStrDevFlow = responseStrDevFlow.replaceAll("(\\[|\\]|\\{|\\}|\\},)", "$1\n");
        responseStrDevFlow = responseStrDevFlow.replaceAll(",\"", ",\n\"");
        responseStrDevFlow = responseStrDevFlow.replaceAll("\"\\}", "\"\n\\}");
        responseStrDevFlow = responseStrDevFlow.replaceAll("\\}\n,", "\\},");
        responseStrDevFlow = responseStrDevFlow.replaceAll("\\]\n,", "\\],");
        responseStrDevFlow = responseStrDevFlow.replaceAll("\\},\\{", "\\},\n\\{");
        int auxcount = 0;

        int realSize = responseStrDevFlow.split("\n").length;

        String deviceFlowsArray[] = new String[realSize];
        deviceFlowsArray = responseStrDevFlow.split("\n");
        for (int i = 0; i < realSize; i++) {
            if (deviceFlowsArray[i].matches("(.*)\"id\":(.*)")) {
                qtyFlows++;
            }
        }
        String deviceFlows[][] = new String[9][qtyFlows];
        int j = 0;

        for (int i = 0; i < realSize; i++) {
            if (deviceFlowsArray[i].matches("(.*)\"id\":(.*)")) {
                auxcount = 1;
                deviceFlows[0][j] = "";
                deviceFlows[1][j] = "";
                deviceFlows[2][j] = "";
                deviceFlows[3][j] = "";
                deviceFlows[4][j] = "";
                deviceFlows[5][j] = "";
                deviceFlows[6][j] = "";
                deviceFlows[7][j] = "-1";
                deviceFlows[8][j] = "-1";
                deviceFlows[0][j] = deviceFlowsArray[i].split("\"id\":\"")[1];
                deviceFlows[0][j] = deviceFlows[0][j].split("\"")[0];
                j++;
            } else if (deviceFlowsArray[i].matches("(.*)\"groupId\":(.*)")) {
                deviceFlows[1][j - 1] = deviceFlowsArray[i].split("\"groupId\":")[1];
                deviceFlows[1][j - 1] = deviceFlows[1][j - 1].split(",")[0];
            } else if (deviceFlowsArray[i].matches("(.*)\"deviceId\":(.*)")) {
                deviceFlows[2][j - 1] = deviceFlowsArray[i].split("\"deviceId\":\"")[1];
                deviceFlows[2][j - 1] = deviceFlows[2][j - 1].split("\"")[0];
            } else if (deviceFlowsArray[i].matches("(.*)\"type\":\"OUTPUT\"(.*)")) {
                deviceFlows[3][j - 1] = deviceFlowsArray[i + 1].split("\"port\":")[1];
            } else if (deviceFlowsArray[i].matches("(.*)\"type\":\"IN_PORT\"(.*)")) {
                deviceFlows[4][j - 1] = deviceFlowsArray[i + 1].split("\"port\":")[1];
            } else if (deviceFlowsArray[i].matches("(.*)\"type\":\"ETH_DST\"(.*)")) {
                deviceFlows[5][j - 1] = deviceFlowsArray[i + 1].split("\"mac\":\"")[1];
                deviceFlows[5][j - 1] = deviceFlows[5][j - 1].split("\"")[0];
            } else if (deviceFlowsArray[i].matches("(.*)\"type\":\"ETH_SRC\"(.*)")) {
                deviceFlows[6][j - 1] = deviceFlowsArray[i + 1].split("\"mac\":\"")[1];
                deviceFlows[6][j - 1] = deviceFlows[6][j - 1].split("\"")[0];
            }

        }
        return (deviceFlows);
    }

    //send GET to HTTP server and retrieve response
    public String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body, String access_key_id, String secret_access_key)
            throws IOException {

        String username = access_key_id;
        String password = secret_access_key;
        String userPassword = username + ":" + password;
        byte[] encoded = Base64.encodeBase64(userPassword.getBytes());
        String stringEncoded = new String(encoded);

        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Basic " + stringEncoded);
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
