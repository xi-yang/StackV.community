/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.onosystem;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author diogo
 */
public class OnosServer {

    public int qtyDevices = 0;
    public int qtyLinks = 0;
    public int qtyHosts = 0;
    //public int qtyPorts = 0;
    //public int qtyFlows = 0;

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

        for (int i = 0; i < qtyLinks; i++) {
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

        URL url = new URL(subsystemBaseUrl + "/hosts");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String responseStr = this.executeHttpMethod(url, conn, "GET", null, access_key_id, secret_access_key);

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(responseStr);
        JSONArray hostArray = (JSONArray) jsonObject.get("hosts");
        qtyHosts = hostArray.size();

        String hosts[][] = new String[qtyHosts][6];
        //hosts[][0]:id;  "mac/-1"
        //hosts[][1]:mac; 
        //hosts[][2]:vlan; "-1"
        //hosts[][3]:ipAddresses    local IP: "10.0.0.x"
        //hosts[][4]:elementid;     device ID "of:0000000000000002"
        //hosts[][5]:port           host connecting which port of device

        for (int i = 0; i < qtyHosts; i++) {
            JSONObject hostObj = (JSONObject) hostArray.get(i);

            hosts[i][0] = (String) hostObj.get("id");
            hosts[i][1] = (String) hostObj.get("mac");
            hosts[i][2] = (String) hostObj.get("vlan");

            JSONArray ipArray = (JSONArray) hostObj.get("ipAddresses");
            hosts[i][3] = String.valueOf(ipArray.get(0));

            JSONObject locObj = (JSONObject) hostObj.get("location");
            hosts[i][4] = (String) locObj.get("elementId");
            hosts[i][5] = (String) locObj.get("port");

        }
        return hosts;
    }

    //pull Device Ports Data
    public String[][] getOnosDeviceFlows(String topologyURI, String subsystemBaseUrl, String devId, String mappingIdMatrix[], int mappingIdSize, String access_key_id, String secret_access_key)
            throws MalformedURLException, IOException, ParseException {

        URL urlDevFlow = new URL(subsystemBaseUrl + "/flows/" + devId);
        HttpURLConnection connDevFlow = (HttpURLConnection) urlDevFlow.openConnection();
        String responseStrDevFlow = this.executeHttpMethod(urlDevFlow, connDevFlow, "GET", null, access_key_id, secret_access_key);

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(responseStrDevFlow);
        JSONArray flowArray = (JSONArray) jsonObject.get("flows");

        int qtyFlows = flowArray.size();

        String flows[][] = new String[qtyFlows][9];
        //flows[][0]: id; flows[][1]:groupid; flows[][2]:deviceId; 
        //flows[][3]: out_port, flows[][4]: in_port
        //flows[][5]: ETH_DST mac, flows[][6]: ETH_SRC mac
        //flows[][7]: ETH_SRC_VLAN, flows[][8]:ETH_DST_VLAN

        for (int i = 0; i < qtyFlows; i++) {
            JSONObject flowObj = (JSONObject) flowArray.get(i);

            String uri=String.format("%s:%s:openflow-service:flow-table-0:flow-", topologyURI,devId);
            flows[i][0] = (String) flowObj.get("id");
            if(mappingIdSize>0){
                for (int counter=0;counter<mappingIdSize;counter++){
                    if(mappingIdMatrix[counter].contains(uri+flows[i][0])){
                    flows[i][0]=mappingIdMatrix[counter].split("-->>")[0].split(uri)[1];
                    }
                }
                
            }
            flows[i][1] = String.valueOf(flowObj.get("groupId"));
            flows[i][2] = (String) flowObj.get("deviceId");

            JSONObject treatmentObj = (JSONObject) flowObj.get("treatment");
            JSONArray instruArray = (JSONArray) treatmentObj.get("instructions");

            for (Object instru : instruArray) {
                JSONObject instruObj = (JSONObject) instru;
                if (String.valueOf(instruObj.get("type")).equals("OUTPUT")) {
                    flows[i][3] = String.valueOf(instruObj.get("port"));
                }
            }

            //initialize for flows[][4]-flows[][6]
            flows[i][4] = "";
            flows[i][5] = "";
            flows[i][6] = "";

            JSONObject selectorObj = (JSONObject) flowObj.get("selector");
            JSONArray criteriaArray = (JSONArray) selectorObj.get("criteria");

            for (Object cri : criteriaArray) {
                JSONObject criObj = (JSONObject) cri;
                if (String.valueOf(criObj.get("type")).equals("IN_PORT")) {
                    flows[i][4] = String.valueOf(criObj.get("port"));
                } else if (String.valueOf(criObj.get("type")).equals("ETH_DST")) {
                    flows[i][5] = (String) criObj.get("mac");
                } else if (String.valueOf(criObj.get("type")).equals("ETH_SRC")) {
                    flows[i][6] = (String) criObj.get("mac");
                }
            }

            flows[i][7] = "-1";
            flows[i][8] = "-1";

        }
        return flows;
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
