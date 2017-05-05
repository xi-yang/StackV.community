/*
* Copyright (c) 2013-2016 University of Maryland
* Created by: Alberto Jimenez 2015
* Modified by: Tao-Hung Yang 2016
* Modified by: Xi Yang 2016
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and/or hardware specification (the “Work”) to deal in the
* Work without restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
* the Work, and to permit persons to whom the Work is furnished to do so,
* subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Work.
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
* IN THE WORK.
 */
package web.beans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.ejb.EJBException;
import javax.net.ssl.HttpsURLConnection;
import net.maxgigapop.mrs.common.StackLogger;
import org.apache.commons.dbutils.DbUtils;
import org.apache.logging.log4j.ThreadContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class serviceBeans {

    private final StackLogger logger = new StackLogger(serviceBeans.class.getName(), "serviceBeans");

    private final String kc_url = System.getProperty("kc_url");

    JSONParser parser = new JSONParser();

    String login_db_user = "login_view";
    String login_db_pass = "loginuser";
    String front_db_user = "front_view";
    String front_db_pass = "frontuser";
    String rains_db_user = "root";
    String rains_db_pass = "root";
    String host = "http://localhost:8080/StackV-web/restapi";

    public serviceBeans() {

    }

    /**
     * Installs driver with the user defined properties via the system API
     *
     * @param paraMap a key-value pair contains all the properties defined by
     * user. It should contains at least the driver ID and the topology uri.
     * @return error code:<br />
     * 0 - success.<br />
     * 2 - plugin error.<br />
     * 3 - connection error.<br />
     */
    public int driverInstall(Map<String, String> paraMap, String auth) {
        String driver = "<driverInstance><properties>";
        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            //if the key indicates what kind of driver it is, put the corresponding ejb path
            if (entry.getKey().equalsIgnoreCase("driverID")) {
                driver += "<entry><key>driverEjbPath</key>";
                switch (entry.getValue()) {
                    case "stubdriver":
                        driver += "<value>java:module/StubSystemDriver</value></entry>";
                        break;
                    case "awsdriver":
                        driver += "<value>java:module/AwsDriver</value></entry>";
                        break;
                    case "versaNSDriver":
                        driver += "<value>java:module/GenericRESTDriver</value></entry>";
                        break;
                    case "openStackDriver":
                        driver += "<value>java:module/OpenStackDriver</value></entry>";
                        break;
                    case "StackDriver":
                        driver += "<value>java:module/StackSystemDriver</value></entry>";
                        break;
                    default:
                        break;
                }
            } //if it is ttl model, modify the format so that the system can recognize the brackets
            else if (entry.getKey().equalsIgnoreCase("ttlmodel")) {
                String ttlModel = entry.getValue().replaceAll("<", "&lt;");
                ttlModel = ttlModel.replaceAll(">", "&gt;");
                driver += "<entry><key>stubModelTtl</key><value>" + ttlModel + "</value></entry>";

            } //if it indicates it's a natserver in openstack, add this entry
            else if (entry.getKey().equalsIgnoreCase("NATServer") && entry.getValue().equalsIgnoreCase("yes")) {
                driver += "<entry><key>NATServer</key><value></value></entry>";
            } //simply put the key value pair into the string
            else {
                driver += "<entry><key>" + entry.getKey() + "</key><value>"
                        + entry.getValue() + "</value></entry>";
            }
        }
        driver += "</properties></driverInstance>";

        //push to the system api and get response
        try {
            URL url = new URL(String.format("%s/driver", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = this.executeHttpMethod(url, connection, "POST", driver, auth);
            if (!result.equalsIgnoreCase("plug successfully")) //plugin error
            {
                return 2;
            }
        } catch (IOException ex) {
            logger.catching("driverInstall", ex);
        }

        return 0;
    }

    /**
     * Uninstalls driver via the system API
     *
     * @param topoUri an unique string represents each driver topology
     * @return error code:<br />
     * 0 - success.<br />
     * 2 - unplug error.<br />
     * 3 - connection error.<br />
     */
    public int driverUninstall(String topoUri, String auth) {
        try {
            URL url = new URL(String.format("%s/driver/%s", host, topoUri));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = this.executeHttpMethod(url, connection, "DELETE", null, auth);
            if (!result.equalsIgnoreCase("unplug successfully")) //unplug error
            {
                return 2;
            }
        } catch (IOException ex) {
            logger.catching("driverUninstall", ex);
        }
        return 0;
    }

// -------------------------- SERVICE FUNCTIONS --------------------------------
    /*
    public int createflow(Map<String, String> paraMap) {
    String topUri = null;
    String refUuid = null;
    String eth_src = null;
    String eth_des = null;
    
    for (Map.Entry<String, String> entry : paraMap.entrySet()) {
    if (entry.getKey().contains("topUri")) {
    topUri = entry.getValue();
    } else if (entry.getKey().equalsIgnoreCase("instanceUUID")) {
    refUuid = entry.getValue();
    } else if (entry.getKey().equalsIgnoreCase("eth-src")) {
    eth_src = entry.getValue();
    } else if (entry.getKey().equalsIgnoreCase("eth-des")) {
    eth_des = entry.getValue();
    }
    }
    
    String deltaUUID = UUID.randomUUID().toString();
    
    String delta = "<serviceDelta>\n<uuid>" + deltaUUID
    + "</uuid>\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"
    + "\n\n<modelAddition>\n\n"
    + "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
    + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
    + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
    + "@prefix rdf:   &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .\n"
    + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
    + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n"
    + "@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .\n"
    + "@prefix sna:   &lt;http://schemas.ogf.org/mrs/2015/08/network#&gt; .\n\n\n"
    + "&lt;" + topUri + "&gt; \n"
    + "        a                         owl:NamedIndividual , nml:Link ;\n"
    + "        spa:type                  spa:Abstraction;\n"
    + "        nml:name                  \"l2-connetion-test\" ;\n"
    + "        spa:dependOn &lt;x-policy-annotation:action:connect-path1&gt; .\n\n"
    + "&lt;x-policy-annotation:action:connect-path1&gt;\n"
    + "        a            spa:PolicyAction ;\n"
    + "        spa:type     \"MCE_L2OpenflowPath\" ;\n"
    + "        spa:value    \"SRRG Path Pair\" ;\n"
    + "        spa:importFrom &lt;x-policy-annotation:data:left-location&gt;, &lt;x-policy-annotation:data:right-location&gt; ;\n"
    + "        spa:exportTo &lt;x-policy-annotation:data:path1-vlan&gt; .\n\n"
    + "&lt;x-policy-annotation:data:left-location&gt;\n"
    + "        a            spa:PolicyData ;\n"
    + "        spa:type     \"port\";\n"
    + "        spa:value    \" " + eth_src + "\"  .\n\n"
    + "&lt;x-policy-annotation:data:right-location&gt;\n"
    + "        a            spa:PolicyData ;\n"
    + "        spa:type     \"port\";\n"
    + "        spa:value    \" " + eth_des + "\" .\n\n"
    + "&lt;x-policy-annotation:data:path1-vlan&gt;\n"
    + "        a            spa:PolicyData.\n\n"
    + "</modelAddition>\n\n"
    + "</serviceDelta>";
    
    String result;
    
    try {
    Connection front_conn;
    Properties front_connectionProps = new Properties();
    front_connectionProps.put("user", "root");
    front_connectionProps.put("password", "root");
    
    front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
    front_connectionProps);
    
    PreparedStatement prep = front_conn.prepareStatement("SELECT service_instance_id, service_state_id"
    + " FROM service_instance WHERE referenceUUID = ?");
    prep.setString(1, refUuid);
    ResultSet rs1 = prep.executeQuery();
    rs1.next();
    int instanceId = rs1.getInt(1);
    int stateId = rs1.getInt(2);
    
    } catch (SQLException ex) {
    Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
    }
    
    try {
    URL url = new URL(String.format("%s/service/%s", host, refUuid));
    HttpURLConnection compile = (HttpURLConnection) url.openConnection();
    result = this.executeHttpMethod(url, compile, "POST", delta);
    if (!result.contains("referenceVersion")) {
    return 2;//Error occurs when interacting with back-end system
    }
    url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
    HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
    result = this.executeHttpMethod(url, propagate, "PUT", null);
    if (!result.equals("PROPAGATED")) {
    return 2;//Error occurs when interacting with back-end system
    }
    url = new URL(String.format("%s/service/%s/commit", host, refUuid));
    HttpURLConnection commit = (HttpURLConnection) url.openConnection();
    result = this.executeHttpMethod(url, commit, "PUT", null);
    if (!result.equals("COMMITTED")) {
    return 2;//Error occurs when interacting with back-end system
    }
    url = new URL(String.format("%s/%s/status", host, refUuid));
    while (true) {
    HttpURLConnection status = (HttpURLConnection) url.openConnection();
    result = this.executeHttpMethod(url, status, "GET", null);
    if (result.equals("READY")) {
    return 0;//create network successfully
    } else if (!result.equals("COMMITTED")) {
    return 3;//Fail to create network
    }
    sleep(5000);//wait for 5 seconds and check again later
    }
    } catch (Exception e) {
    return 1;//connection error
    }
    
    }
     */
    public String getLinks(JSONObject JSONinput) {
        ArrayList<String> retList = new ArrayList<>();
        JSONArray tempArray = (JSONArray) JSONinput.get("connections");
        JSONObject retJSON = new JSONObject();
        String retString = "{";

        if (tempArray != null) {
            for (int i = 0; i < tempArray.size(); i++) {
                JSONObject tempJSON = (JSONObject) tempArray.get(i);
                JSONArray innerArray = (JSONArray) tempJSON.get("terminals");

                if (!retString.equals("{")) {
                    retString += ",";
                }
                retString += "\n\"" + tempJSON.get("name") + "\": {\n\t\""
                        + ((JSONObject) innerArray.get(0)).get("uri")
                        + "\":{\"vlan_tag\":\""
                        + ((JSONObject) innerArray.get(0)).get("vlan_tag")
                        + "\"},\n\t\""
                        + ((JSONObject) innerArray.get(1)).get("uri")
                        + "\":{\"vlan_tag\":\""
                        + ((JSONObject) innerArray.get(1)).get("vlan_tag")
                        + "\"}\n\t}\n";
            }
        }
        return retString + "}";
    }

    public int createDNC(JSONObject JSONinput, String auth, String refresh, String refUuid) {

        String deltaJSON = getLinks(JSONinput);

        String svcDelta = "<serviceDelta>\n<uuid>" + refUuid + "</uuid>\n\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"
                + "\n\n<modelAddition>\n"
                + "\n@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
                + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
                + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
                + "@prefix rdf:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n"
                + "@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .\n\n"
                + "&lt;urn:ogf:network:vo1.maxgigapop.net:link=abstract&gt;\n"
                + "\ta            nml:Link ;\n"
                + "\tspa:type            spa:Abstraction ;\n"
                + "\tspa:dependOn &lt;x-policy-annotation:action:create-path&gt;.\n\n"
                + "&lt;x-policy-annotation:action:create-path&gt;\n"
                + "\ta            spa:PolicyAction ;\n"
                + "\tspa:type     \"MCE_MPVlanConnection\" ;\n"
                + "\tspa:importFrom &lt;x-policy-annotation:data:conn-criteria&gt; ;\n"
                + "\tspa:exportTo &lt;x-policy-annotation:data:conn-criteriaexport&gt; .\n\n"
                + "&lt;x-policy-annotation:data:conn-criteria&gt;\n"
                + "\ta            spa:PolicyData;\n"
                + "\tspa:type     \"JSON\";\n"
                + "\tspa:value    \"\"\"";

        svcDelta += deltaJSON;

        svcDelta += "\"\"\".\n\n"
                + "&lt;x-policy-annotation:data:conn-criteriaexport&gt;\n"
                + "\ta            spa:PolicyData.</modelAddition></serviceDelta>";

        orchestrateInstance(refUuid, svcDelta, refUuid, refresh);
        return 0;
    }

    public int createNetwork(Map<String, String> paraMap, String auth, String refresh) {
        Connection rains_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "createNetwork";

        String topoUri = null;
        String driverType = null;
        String netCidr = null;
        String refUuid = null;
        String directConn = null;
        List<String> subnets = new ArrayList<>();
        List<String> vmList = new ArrayList<>();
        JSONParser jsonParser = new JSONParser();
        JSONArray gateArr = null;
        boolean gwVpn = false;

        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("topoUri")) {
                topoUri = entry.getValue();

                Properties rains_connectionProps = new Properties();
                rains_connectionProps.put("user", rains_db_user);
                rains_connectionProps.put("password", rains_db_pass);
                try {
                    rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb", rains_connectionProps);
                    prep = rains_conn.prepareStatement("SELECT driverEjbPath"
                            + " FROM driver_instance WHERE topologyUri = ?");
                    prep.setString(1, topoUri);
                    ResultSet rs1 = prep.executeQuery();
                    rs1.next();
                    String driverPath = rs1.getString(1);
                    if (driverPath.contains("Aws")) {
                        driverType = "aws";
                    } else if (driverPath.contains("OpenStack")) {
                        driverType = "ops";
                    }

                } catch (SQLException ex) {
                    logger.catching(method, ex);
                } finally {
                    try {
                        DbUtils.close(rs);
                        DbUtils.close(prep);
                        DbUtils.close(rains_conn);
                    } catch (SQLException ex) {
                        logger.catching("DBUtils", ex);
                    }
                }
            } else if (entry.getKey().equalsIgnoreCase("netCidr")) {
                netCidr = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("instanceUUID")) {
                refUuid = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("gateways")) {
                try {
                    gateArr = (JSONArray) jsonParser.parse(entry.getValue());
                    for (Object gateEle : gateArr) {
                        JSONObject gateJSON = (JSONObject) gateEle;
                        if (((String) gateJSON.get("type")).equals("aws_direct_connect")) {
                            JSONArray destArr = (JSONArray) gateJSON.get("to");
                            if (destArr != null) {
                                JSONObject destJSON = (JSONObject) destArr.get(0);
                                directConn = (String) destJSON.get("value");
                            }
                            gateArr.remove(gateEle);
                            break;
                        }
                    }

                } catch (ParseException ex) {
                    logger.catching(method, ex);
                }
            } else if (entry.getKey().contains("subnet")) {
                subnets.add(entry.getValue());
            } else if (entry.getKey().contains("vm")) {
                vmList.add(entry.getValue());
            }
            //example for vm : vm1&0
        }

        try {
            URL url = new URL(String.format("%s/service/property/%s/host/", host, refUuid));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            executeHttpMethod(url, connection, "POST", driverType, auth);
        } catch (IOException ex) {
            logger.catching(method, ex);
        }

        JSONObject network = new JSONObject();
        network.put("type", "internal");
        network.put("cidr", netCidr);
        network.put("parent", topoUri);

        JSONArray subnetsJson = new JSONArray();
        //routing problem solved. need testing.
        for (String net : subnets) {
            String[] netPara = net.split("&");
            JSONObject subnetValue = new JSONObject();
            for (String para : netPara) {
                if (para.startsWith("routes")) {
                    String[] route = para.substring(6).split("\r\n");
                    JSONArray routesArray = new JSONArray();
                    for (String r : route) {
                        String[] routePara = r.split(",");
                        JSONObject jsonRoute = new JSONObject();
                        for (String rp : routePara) {
                            String[] keyValue = rp.split("\\+");
                            jsonRoute.put(keyValue[0], keyValue[1]);
                            if (keyValue[1].contains("vpn")) {
                                gwVpn = true;
                            }
                        }
                        routesArray.add(jsonRoute);
                    }
                    subnetValue.put("routes", routesArray);
                } else {
                    String[] keyValue = para.split("\\+");
                    subnetValue.put(keyValue[0], keyValue[1]);
                }
            }
            subnetsJson.add(subnetValue);
        }
        network.put("subnets", subnetsJson);

        JSONArray routesJson = new JSONArray();
        JSONObject routesValue = new JSONObject();
        routesValue.put("to", "0.0.0.0/0");
        routesValue.put("nextHop", "internet");
        routesJson.add(routesValue);
        network.put("routes", routesJson);

        JSONArray gatewaysJson = new JSONArray();
        JSONObject temp = new JSONObject();
        temp.put("type", "internet");
        gatewaysJson.add(temp);
        if (gwVpn || ((driverType != null && driverType.equals("aws")) && directConn != null)) {
            JSONObject gatewayValue = new JSONObject();
            gatewayValue.put("type", "vpn");
            gatewaysJson.add(gatewayValue);
        }
        network.put("gateways", gatewaysJson);

        String deltaUUID = UUID.randomUUID().toString();

        String svcDelta = "<serviceDelta>\n<uuid>" + deltaUUID
                + "</uuid>\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"
                + "\n\n<modelAddition>\n"
                + "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
                + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
                + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
                + "@prefix rdf:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n"
                + "@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .\n\n"
                + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_clouds:tag+vpc1&gt;\n"
                + "    a                         nml:Topology ;\n";

        String exportTo = "";
        if ((driverType != null && driverType.equals("aws")) && directConn != null) {
            String dest = directConn.contains("?vlan") ? directConn.substring(0, directConn.indexOf("?vlan")) : directConn;
            String vlan = directConn.contains("?vlan") ? directConn.substring(directConn.indexOf("?vlan") + 6) : "any";
            exportTo += "&lt;x-policy-annotation:data:vpc-export&gt;, ";
            svcDelta += "    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt;"
                    + ", &lt;x-policy-annotation:action:create-mce_dc1&gt; .\n\n"
                    + "&lt;urn:ogf:network:vo1_maxgigapop_net:link=conn1&gt;\n"
                    + "    a            mrs:SwitchingSubnet;\n"
                    + "    spa:type     spa:Abstraction;\n"
                    + "    spa:dependOn &lt;x-policy-annotation:action:create-path&gt;.\n\n"
                    + "&lt;x-policy-annotation:action:create-path&gt;\n"
                    + "    a            spa:PolicyAction ;\n"
                    + "    spa:type     \"MCE_MPVlanConnection\" ;\n"
                    + "    spa:importFrom &lt;x-policy-annotation:data:conn-criteria1&gt; ;\n"
                    + "    spa:exportTo &lt;x-policy-annotation:data:conn-export&gt; .\n\n"
                    + "&lt;x-policy-annotation:action:create-mce_dc1&gt;\n"
                    + "    a            spa:PolicyAction ;\n"
                    + "    spa:type     \"MCE_AwsDxStitching\" ;\n"
                    + "    spa:importFrom &lt;x-policy-annotation:data:vpc-export&gt;, &lt;x-policy-annotation:data:conn-export&gt; ;\n"
                    + "    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt;, &lt;x-policy-annotation:action:create-path&gt;.\n\n"
                    + "&lt;x-policy-annotation:data:vpc-export&gt;\n"
                    + "    a            spa:PolicyData ;\n"
                    + "    spa:type     \"JSON\" ;\n"
                    + "    spa:format   \"\"\"{\n"
                    + "       \"parent\":\"" + topoUri + "\",\n"
                    + "       \"stitch_from\": \"%$.gateways[?(@.type=='vpn-gateway')].uri%\",\n"
                    + "    }\"\"\" .\n\n"
                    + "&lt;x-policy-annotation:data:conn-export&gt;\n"
                    + "    a            spa:PolicyData;\n"
                    + "    spa:type     \"JSON\" ;\n"
                    + "    spa:format   \"\"\"{\n"
                    + "       \"to_l2path\": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn1%\n"
                    + "    }\"\"\" .\n\n"
                    + "&lt;x-policy-annotation:data:conn-criteria1&gt;\n"
                    + "    a            spa:PolicyData;\n"
                    + "    spa:type     \"JSON\";\n"
                    + "    spa:value    \"\"\"{\n"
                    + "        \"urn:ogf:network:vo1_maxgigapop_net:link=conn1\":"
                    + "{ \"" + dest + "\":{\"vlan_tag\":\"" + vlan + "\"},\n"
                    + "        \"" + topoUri + "\":{\"vlan_tag\":\"" + vlan + "\"}\n"
                    + "        }\n"
                    + "    }\"\"\".\n\n";
        } else {
            svcDelta += "    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt; .\n\n";
        }

        if (!vmList.isEmpty()) {
            if (driverType != null && driverType.equals("aws")) {
                for (String vm : vmList) {
                    String[] vmPara = vm.split("&");
                    //0:vm name.
                    //1:subnet #
                    //2:types: image, instance, key pair, security group
                    //4:eth0
                    String addressString = null;
                    if (!vmPara[4].equals(" ")) {
                        try {
                            JSONArray interfaceArr = (JSONArray) jsonParser.parse(vmPara[4]);
                            for (Object obj : interfaceArr) {
                                JSONObject interfaceJSON = (JSONObject) obj;
                                String typeString = (String) interfaceJSON.get("type");
                                if (typeString.equalsIgnoreCase("Ethernet")) {
                                    addressString = (String) interfaceJSON.get("address");
                                    addressString = addressString.contains("ipv") ? addressString.substring(addressString.indexOf("ipv") + 5) : addressString;
                                    addressString = addressString.contains("/") ? addressString.substring(0, addressString.indexOf("/")) : addressString;
                                }
                            }
                            if (addressString != null) {
                                addressString = ";\n    mrs:hasNetworkAddress   "
                                        + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0:floating&gt;.\n\n"
                                        + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0:floating&gt;\n"
                                        + "    a            mrs:NetworkAddress;\n    mrs:type     \"floating-ip\";\n"
                                        + "    mrs:value     \"" + addressString + "\" .\n\n";

                            }
                        } catch (ParseException ex) {
                            logger.catching(method, ex);
                        }
                    }
                    svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "&gt;\n"
                            + "    a                         nml:Node ;\n"
                            + "    nml:name         \"" + vmPara[0] + "\";\n"
                            + (vmPara[2].equals(" ") ? "" : "    mrs:type       \"" + vmPara[2] + "\";\n")
                            + "    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0&gt; ;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;.\n\n"
                            + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0&gt;\n"
                            + "    a            nml:BidirectionalPort;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;"
                            + ((addressString == null) ? " .\n\n" : addressString)
                            + "&lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;\n"
                            + "    a            spa:PolicyAction ;\n"
                            + "    spa:type     \"MCE_VMFilterPlacement\" ;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt; ;\n"
                            + "    spa:importFrom ";
                    String subnetCriteria = "&lt;x-policy-annotation:data:vpc-subnet-" + vmPara[0] + "-criteria&gt;";
                    exportTo += subnetCriteria + ", ";
                    int sub = Integer.valueOf(vmPara[1]) - 1;
                    svcDelta += subnetCriteria + ".\n\n"
                            + subnetCriteria + "\n    a            spa:PolicyData;\n"
                            + "    spa:type     \"JSON\";\n    spa:format    \"\"\"{ "
                            + "\"place_into\": \"%$.subnets[" + sub + "].uri%\"}\"\"\" .\n\n";
                }
            } else if (driverType != null && driverType.equals("ops")) {
                String createPathExportTo = "";
                String dependOn = "";
                JSONObject connCriteriaValue = new JSONObject();
                String providesVolume = "";
                String svcDeltaCeph = "";
                String svcDeltaEndPoints = "";

                for (String vm : vmList) {
                    String[] vmPara = vm.split("&");
                    //0:vm name.
                    //1:subnet #
                    //2:types: image, instance, key pair, security group
                    //3:vm host
                    //4:Interfaces: floating IP, SRIOV
                    //5:vm routes
                    //6:ceph
                    //7-8:globus, nfs endpoints
                    svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "&gt;\n"
                            + "    a                         nml:Node ;\n"
                            + "    nml:name         \"" + vmPara[0] + "\";\n"
                            + (vmPara[2].equals(" ") ? "" : "    mrs:type       \"" + vmPara[2] + "\";\n")
                            + "    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0&gt; ;\n";
                    JSONArray vmRouteArr = null;
                    if (!vmPara[5].equals(" ")) {
                        try {
                            vmRouteArr = (JSONArray) jsonParser.parse(vmPara[5]);
                            svcDelta += "    nml:hasService  &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":routingservice&gt; ;\n";
                        } catch (ParseException ex) {
                            logger.catching(method, ex);
                        }
                    }
                    svcDelta += "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;.\n\n"
                            + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0&gt;\n"
                            + "    a            nml:BidirectionalPort;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "-eth0&gt;";
                    if (!vmPara[4].equals(" ")) {
                        try {
                            JSONArray interfaceArr = (JSONArray) jsonParser.parse(vmPara[4]);
                            String addressString = null;
                            ArrayList<JSONObject> sriovList = new ArrayList<>();
                            for (Object obj : interfaceArr) {
                                JSONObject interfaceJSON = (JSONObject) obj;
                                String typeString = (String) interfaceJSON.get("type");
                                if (typeString.equalsIgnoreCase("Ethernet")) {
                                    addressString = (String) interfaceJSON.get("address");
                                    addressString = addressString.contains("ipv") ? addressString.substring(addressString.indexOf("ipv") + 5) : addressString;
                                    addressString = addressString.contains("/") ? addressString.substring(0, addressString.indexOf("/")) : addressString;
                                } else if (typeString.equalsIgnoreCase("SRIOV")) {
                                    sriovList.add(interfaceJSON);
                                }
                            }

                            if (addressString != null) {
                                svcDelta += ";\n    mrs:hasNetworkAddress   "
                                        + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0:floating&gt;.\n\n"
                                        + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0:floating&gt;\n"
                                        + "    a            mrs:NetworkAddress;\n    mrs:type     \"floating-ip\";\n"
                                        + "    mrs:value     \"" + addressString + "\".\n\n";

                                for (int i = 0; i < sriovList.size(); i++) {
                                    JSONObject sriov = sriovList.get(i);

                                    //Parse sriov ip and mac address.
                                    String address = (String) sriov.get("address");
                                    String[] addArr = address.split(",");
                                    String ip = null;
                                    String mac = null;
                                    for (String str : addArr) {
                                        ip = str.contains("ipv4") ? str.substring(str.indexOf("ipv4") + 5) : ip;
                                        mac = str.contains("mac") ? str.substring(str.indexOf("mac") + 4) : mac;
                                    }

                                    //Find sriov parameter from Gateways.
                                    if (gateArr == null) {
                                        return -1;
                                    }
                                    for (Object gwEle : gateArr) {
                                        JSONObject gwJSON = (JSONObject) gwEle;
                                        if (gwJSON.get("name").equals(sriov.get("gateway"))) {
                                            //parse sriov routes
                                            JSONArray routeArr = new JSONArray();
                                            if (sriov.containsKey("routes")) {
                                                JSONArray srRouteArr = (JSONArray) sriov.get("routes");
                                                for (Object rtEle : srRouteArr) {
                                                    JSONObject rtObject = (JSONObject) rtEle;
                                                    JSONObject rt = new JSONObject();
                                                    for (Object key : rtObject.keySet()) {
                                                        JSONObject rtPara = (JSONObject) rtObject.get(key);
                                                        rt.put(key, rtPara.get("value"));
                                                    }
                                                    routeArr.add(rt);
                                                }
                                            }
                                            // add VM level routes - noop
                                            /*
                                            if (vmRouteArr != null && !vmRouteArr.isEmpty()) {
                                                if (routeArr == null) {
                                                    routeArr = vmRouteArr;
                                                } else {
                                                    routeArr.addAll(vmRouteArr);
                                                }
                                                vmRouteArr = null;
                                            }
                                             */
                                            // sriov port_profile
                                            if (gwJSON.containsKey("from")) {
                                                JSONArray fromArr = (JSONArray) gwJSON.get("from");
                                                JSONObject fromJSON = (JSONObject) fromArr.get(0);
                                                if (fromJSON.get("type").equals("port_profile")) {
                                                    //construct models;
                                                    dependOn += "&lt;x-policy-annotation:action:ucs-sriov-stitch-external-" + vmPara[0] + "-sriov" + i + "&gt;, ";
                                                    svcDelta += "&lt;x-policy-annotation:action:ucs-sriov-stitch-external-" + vmPara[0] + "-sriov" + i + "&gt;\n"
                                                            + "    a            spa:PolicyAction ;\n"
                                                            + "    spa:type     \"MCE_UcsSriovStitching\" ;\n"
                                                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;, &lt;x-policy-annotation:action:create-" + vmPara[0] + "-eth0&gt;;\n"
                                                            + "    spa:importFrom &lt;x-policy-annotation:data:sriov-criteria-external-" + vmPara[0] + "-sriov" + i + "&gt;.\n"
                                                            + "\n"
                                                            + "&lt;x-policy-annotation:data:sriov-criteria-external-" + vmPara[0] + "-sriov" + i + "&gt;\n"
                                                            + "    a            spa:PolicyData;\n"
                                                            + "    spa:type     \"JSON\";\n"
                                                            + "    spa:value    \"\"\"{\n"
                                                            + "       \"stitch_from\": \"urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "\",\n"
                                                            + "       \"to_port_profile\": \"" + (String) fromJSON.get("value") + "\",\n"
                                                            + "       \"mac_address\": \"" + mac + "\""
                                                            + (ip == null ? "" : ",\n       \"ip_address\": \"" + ip + "\"")
                                                            + (routeArr.isEmpty() ? "" : ",\n       \"routes\": " + routeArr.toString().replace("\\", ""))
                                                            + "\n    }\"\"\" .\n\n";
                                                }
                                            }

                                            //sriov stitch_port
                                            if (gwJSON.containsKey("to")) {
                                                JSONArray toArr = (JSONArray) gwJSON.get("to");
                                                JSONObject toJSON = (JSONObject) toArr.get(0);
                                                if (toJSON.get("type").equals("stitch_port")) {
                                                    //construct models;
                                                    dependOn += "&lt;x-policy-annotation:action:ucs-" + vmPara[0] + "-sriov" + i + "-stitch&gt;, ";
                                                    svcDelta += "&lt;x-policy-annotation:action:ucs-" + vmPara[0] + "-sriov" + i + "-stitch&gt;\n"
                                                            + "    a            spa:PolicyAction ;\n"
                                                            + "    spa:type     \"MCE_UcsSriovStitching\" ;\n"
                                                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;, &lt;x-policy-annotation:action:create-path&gt;;\n"
                                                            + "    spa:importFrom &lt;x-policy-annotation:data:" + vmPara[0] + "-sriov" + i + "-criteria&gt; .\n"
                                                            + "\n"
                                                            + "&lt;x-policy-annotation:data:" + vmPara[0] + "-sriov" + i + "-criteria&gt;\n"
                                                            + "    a            spa:PolicyData;\n"
                                                            + "    spa:type     \"JSON\";\n"
                                                            + "    spa:format    \"\"\"{\n"
                                                            + "       \"stitch_from\": \"urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "\",\n"
                                                            + "       \"to_l2path\": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn" + (String) gwJSON.get("name") + "%,\n"
                                                            + "       \"mac_address\": \"" + mac + "\""
                                                            + (ip == null ? "" : ",\n       \"ip_address\": \"" + ip + "\"")
                                                            + (routeArr.isEmpty() ? "" : ",\n       \"routes\": " + routeArr.toString().replace("\\", ""))
                                                            + "\n    }\"\"\" .\n\n";
                                                    createPathExportTo += "&lt;x-policy-annotation:data:" + vmPara[0] + "-sriov" + i + "-criteria&gt;, ";
                                                    if (!connCriteriaValue.containsKey("urn:ogf:network:vo1_maxgigapop_net:link=conn" + (String) gwJSON.get("name"))) {
                                                        JSONObject vlanTag = new JSONObject();
                                                        String dest = (String) toJSON.get("value");
                                                        String vlan = "any";
                                                        if (dest.contains("?vlan=")) {
                                                            dest = dest.substring(0, dest.indexOf("?vlan"));
                                                            vlan = dest.substring(dest.indexOf("?vlan=") + 6);
                                                        }
                                                        vlanTag.put("vlan_tag", vlan);
                                                        JSONObject path = new JSONObject();
                                                        path.put(topoUri, vlanTag);
                                                        path.put(dest.replace("\\", ""), vlanTag);
                                                        connCriteriaValue.put("urn:ogf:network:vo1_maxgigapop_net:link=conn" + (String) gwJSON.get("name"), path);
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }

                            } else {
                                svcDelta += ".\n\n";
                            }
                        } catch (ParseException ex) {
                            logger.catching(method, ex);
                        }
                    } else {
                        svcDelta += ".\n\n";
                    }
                    if (vmRouteArr != null) {
                        String vmRoutes = "";
                        svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":routingservice&gt;\n"
                                + "     a   mrs:RoutingService;\n"
                                + "     mrs:providesRoutingTable     " + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":routingservice:routingtable+linux&gt; .\n";
                        svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":routingservice:routingtable+linux&gt;\n"
                                + "     a   mrs:RoutingTable;\n"
                                + "     mrs:type   \"linux\";\n"
                                + "     mrs:hasRoute    \n";
                        int routeCt = 1;
                        for (Object r : vmRouteArr) {
                            JSONObject route = (JSONObject) r;
                            if (routeCt > 1) {
                                svcDelta += ",\n";
                            }
                            svcDelta += "            &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":routingservice:routingtable+linux:route+" + routeCt + "&gt;\n";
                            vmRoutes += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":routingservice:routingtable+linux:route+" + routeCt + "&gt;\n"
                                    + "      a  mrs:Route;\n";
                            if (route.containsKey("to")) {
                                vmRoutes += "      mrs:routeTo " + networkAddressFromJson((JSONObject) route.get("to")) + ";";
                            }
                            if (route.containsKey("from")) {
                                vmRoutes += "      mrs:routeFrom " + networkAddressFromJson((JSONObject) route.get("from")) + ";";
                            }
                            if (route.containsKey("next_hop")) {
                                vmRoutes += "      mrs:nextHop " + networkAddressFromJson((JSONObject) route.get("next_hop")) + ";";
                            }
                            vmRoutes = vmRoutes.trim();
                            vmRoutes += ".\n\n";
                            routeCt++;
                        }
                        svcDelta += ". \n\n" + vmRoutes;
                    }
                    // Ceph RBD
                    if (!vmPara[6].equals(" ")) {
                        String nodeHasVolume = "";
                        try {
                            JSONArray cephRbdArr = (JSONArray) jsonParser.parse(vmPara[6]);
                            int volNum = 0;
                            for (Object obj : cephRbdArr) {
                                JSONObject rbdJSON = (JSONObject) obj;
                                nodeHasVolume += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":volume+ceph" + volNum + "&gt;, ";
                                svcDeltaCeph += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":volume+ceph" + volNum + "&gt;\n"
                                        + "   a  mrs:Volume;\n"
                                        + "   mrs:disk_gb \"" + (String) rbdJSON.get("disk_gb") + "\";\n"
                                        + "   mrs:mount_point \"" + (String) rbdJSON.get("mount_point") + "\".\n\n";
                                volNum++;
                            }
                            providesVolume += nodeHasVolume;
                        } catch (ParseException ex) {
                            logger.catching(method, ex);
                        }
                        if (!nodeHasVolume.isEmpty()) {
                            svcDeltaCeph += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "&gt;\n" + "    mrs:hasVolume       " + nodeHasVolume.substring(0, nodeHasVolume.length() - 2) + ".\n\n";
                        }
                    }
                    // Globus Connect
                    if (!vmPara[7].equals(" ")) {
                        String globusUri = "urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":service+globus";
                        String netAdresses = "\n";
                        try {
                            JSONObject globusJSON = (JSONObject) jsonParser.parse(vmPara[7]);
                            svcDeltaEndPoints += "&lt;" + globusUri + "&gt;\n"
                                    + "   a  mrs:EndPoint ;\n"
                                    + "   mrs:type \"globus:connect\" ;\n";
                            if (globusJSON.containsKey("username")) {

                                String naUri = globusUri + ":username";
                                svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; ;\n";
                                netAdresses += "&lt;" + naUri + "&gt;\n"
                                        + "   a mrs:NetworkAddress ;\n"
                                        + "   mrs:type \"globus:username\";\n"
                                        + "   mrs:value \"" + (String) globusJSON.get("username") + "\" .\n";
                            }
                            if (globusJSON.containsKey("password")) {
                                String naUri = globusUri + ":password";
                                svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; ;\n";
                                netAdresses += "&lt;" + naUri + "&gt;\n"
                                        + "   a mrs:NetworkAddress ;\n"
                                        + "   mrs:type \"globus:password\";\n"
                                        + "   mrs:value \"" + (String) globusJSON.get("password") + "\" .\n";
                            }
                            if (globusJSON.containsKey("default_directory")) {
                                String naUri = globusUri + ":directory";
                                svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; ;\n";
                                netAdresses += "&lt;" + naUri + "&gt;\n"
                                        + "   a mrs:NetworkAddress ;\n"
                                        + "   mrs:type \"globus:directory\";\n"
                                        + "   mrs:value \"" + (String) globusJSON.get("default_directory") + "\" .\n";
                            }
                            if (globusJSON.containsKey("data_interface")) {
                                String naUri = globusUri + ":interface";
                                svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; ;\n";
                                netAdresses += "&lt;" + naUri + "&gt;\n"
                                        + "   a mrs:NetworkAddress ;\n"
                                        + "   mrs:type \"globus:interface\";\n"
                                        + "   mrs:value \"" + (String) globusJSON.get("data_interface") + "\" .\n";
                            }
                            svcDeltaEndPoints += "   nml:name \"" + (String) globusJSON.get("short_name") + "\" .\n\n";
                        } catch (ParseException ex) {
                            logger.catching(method, ex);
                        }
                        svcDeltaEndPoints += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "&gt;\n" + "    nml:hasService       &lt;" + globusUri + "&gt;. \n";
                        svcDeltaEndPoints += netAdresses;
                    }
                    // NFS Service
                    if (!vmPara[8].equals(" ")) {
                        String nfsUri = "urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":service+nfs";
                        String netAdresses = "\n";
                        try {
                            JSONObject nfsJSON = (JSONObject) jsonParser.parse(vmPara[8]);
                            svcDeltaEndPoints += "&lt;" + nfsUri + "&gt;\n"
                                    + "   a  mrs:EndPoint ;\n"
                                    + "   mrs:type \"nfs\" ;\n";
                            if (nfsJSON.containsKey("exports")) {
                                String naUri = nfsUri + ":expots";
                                svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; .\n";
                                netAdresses += "&lt;" + naUri + "&gt;\n"
                                        + "   a mrs:NetworkAddress ;\n"
                                        + "   mrs:type \"nfs:exports\";\n"
                                        + "   mrs:value \"" + (String) nfsJSON.get("exports") + "\" .\n";
                            }
                        } catch (ParseException ex) {
                            logger.catching(method, ex);
                        }
                        svcDeltaEndPoints += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "&gt;\n" + "    nml:hasService       &lt;" + nfsUri + "&gt;. \n";
                        svcDeltaEndPoints += netAdresses;
                    }
                    svcDelta += "&lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;\n"
                            + "    a            spa:PolicyAction ;\n"
                            + "    spa:type     \"MCE_VMFilterPlacement\" ;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt; ;\n"
                            + "    spa:importFrom &lt;x-policy-annotation:data:" + vmPara[0] + "-host-criteria&gt;.\n\n"
                            + "&lt;x-policy-annotation:data:" + vmPara[0] + "-host-criteria&gt;\n"
                            + "    a            spa:PolicyData;\n    spa:type     \"JSON\";\n"
                            + "    spa:value    \"\"\"{\n"
                            + "       \"place_into\": \"" + topoUri + ":host+" + vmPara[3] + "\"\n"
                            + "    }\"\"\" .\n\n"
                            + "&lt;x-policy-annotation:action:create-" + vmPara[0] + "-eth0&gt;\n"
                            + "    a            spa:PolicyAction ;\n"
                            + "    spa:type     \"MCE_VMFilterPlacement\" ;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt; ;\n"
                            + "    spa:importFrom ";
                    String subnetCriteria = "&lt;x-policy-annotation:data:vpc-subnet-" + vmPara[0] + "-criteria&gt;";
                    exportTo += subnetCriteria + ", ";
                    int sub = Integer.valueOf(vmPara[1]) - 1;
                    svcDelta += subnetCriteria + ".\n\n"
                            + subnetCriteria + "\n    a            spa:PolicyData;\n"
                            + "    spa:type     \"JSON\";\n    spa:format    \"\"\"{ "
                            + "\"place_into\": \"%$.subnets[" + sub + "].uri%\"}\"\"\" .\n\n";
                }

                if (!createPathExportTo.isEmpty()) {
                    svcDelta += "&lt;x-policy-annotation:action:create-path&gt;\n"
                            + "    a            spa:PolicyAction ;\n"
                            + "    spa:type     \"MCE_MPVlanConnection\" ;\n"
                            + "    spa:importFrom &lt;x-policy-annotation:data:conn-criteria&gt; ;\n"
                            + "    spa:exportTo " + createPathExportTo.substring(0, (createPathExportTo.length() - 2)) + ".\n"
                            + "\n"
                            + "&lt;x-policy-annotation:data:conn-criteria&gt;\n"
                            + "    a            spa:PolicyData;\n"
                            + "    spa:type     \"JSON\";\n"
                            + "    spa:value    \"\"\"" + connCriteriaValue.toString().replace("\\", "") + "\"\"\".\n\n";
                }

                if (!providesVolume.isEmpty()) {
                    svcDeltaCeph += "&lt;urn:ogf:network:openstack.com:openstack-cloud:ceph-rbd&gt;\n"
                            + "   mrs:providesVolume " + providesVolume.substring(0, providesVolume.length() - 2) + " .\n\n";
                    svcDelta += svcDeltaCeph;
                }
                if (!svcDeltaEndPoints.isEmpty()) {
                    svcDelta += svcDeltaEndPoints;
                }
                if (!dependOn.isEmpty()) {
                    svcDelta += "&lt;" + topoUri + ":vt&gt;\n"
                            + "   a  nml:Topology;\n"
                            + "   spa:type spa:Abstraction;\n"
                            + "   spa:dependOn  " + dependOn.substring(0, (dependOn.length() - 2)) + ".\n\n";
                }

            }
        }
        svcDelta += "&lt;x-policy-annotation:action:create-vpc&gt;\n"
                + "    a           spa:PolicyAction ;\n"
                + "    spa:type     \"MCE_VirtualNetworkCreation\" ;\n"
                + "    spa:importFrom &lt;x-policy-annotation:data:vpc-criteria&gt; ";
        svcDelta += exportTo.isEmpty() ? ".\n\n" : ";\n    spa:exportTo " + exportTo.substring(0, (exportTo.length() - 2)) + " .\n\n";

        svcDelta += "&lt;x-policy-annotation:data:vpc-criteria&gt;\n"
                + "    a            spa:PolicyData;\n"
                + "    spa:type     nml:Topology;\n"
                + "    spa:value    \"\"\"" + network.toString().replace("\\", "")
                + "\"\"\".\n\n"
                + "</modelAddition>\n\n"
                + "</serviceDelta>";

        orchestrateInstance(refUuid, svcDelta, refUuid, refresh);
        return 0;
    }

    public int createHybridCloud(Map<String, String> paraMap, String auth, String refresh) {
        Connection rains_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "createHybridCloud";

        String refUuid = null;
        JSONParser jsonParser = new JSONParser();
        JSONArray vcnArr = null;
        String creatPathExportTo = "";
        //Mapping from paraMap to local variables
        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("instanceUUID")) {
                refUuid = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("virtual_clouds")) {
                try {
                    vcnArr = (JSONArray) jsonParser.parse(entry.getValue());
                } catch (ParseException ex) {
                    logger.catching(method, ex);
                }
            }
        }
        if (vcnArr == null) {
            return -1;
        }

        String deltaUuid = UUID.randomUUID().toString();
        String awsExportTo = "";
        String awsDxStitching = "";
        String svcDelta = "<serviceDelta>\n<uuid>" + deltaUuid
                + "</uuid>\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"
                + "\n\n<modelAddition>\n"
                + "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
                + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
                + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
                + "@prefix rdf:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n"
                + "@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .\n\n";

        for (Object obj : vcnArr) {
            JSONObject vcnJson = (JSONObject) obj;
            String topoUri = (String) vcnJson.get("parent");
            String driverType = "";
            String vcnName = (String) vcnJson.get("name");
            vcnJson.remove("name");

            JSONArray subArr = (JSONArray) vcnJson.get("subnets");
            ArrayList<JSONArray> vmList = new ArrayList<>();
            for (int i = 0; i < subArr.size(); i++) {
                JSONObject subObj = (JSONObject) subArr.get(i);

                //store the vm value in the vmList and remove from the subnet Json Array
                JSONArray vmArr = (JSONArray) subObj.get("virtual_machines");
                vmList.add(i, vmArr);
                subObj.remove("virtual_machines");

                //modify the subnet routes info to be directly use in ttl model
                JSONArray subRoutesArr = (JSONArray) subObj.get("routes");
                for (Object r : subRoutesArr) {
                    JSONObject route = (JSONObject) r;
                    if (route.containsKey("to")) {
                        JSONObject value = (JSONObject) route.get("to");
                        route.put("to", value.get("value"));
                    }
                    if (route.containsKey("from")) {
                        JSONObject value = (JSONObject) route.get("from");
                        route.put("from", value.get("value"));
                    }
                    if (route.containsKey("next_hop")) {
                        JSONObject value = (JSONObject) route.get("next_hop");
                        route.put("nextHop", value.get("value"));
                    }
                }
            }

            //modify the network routes info to be directly use in ttl model
            JSONArray netRoutesArr = (JSONArray) vcnJson.get("routes");
            for (Object r : netRoutesArr) {
                JSONObject route = (JSONObject) r;
                if (route.containsKey("to")) {
                    JSONObject value = (JSONObject) route.get("to");
                    route.put("to", value.get("value"));
                }
                if (route.containsKey("from")) {
                    JSONObject value = (JSONObject) route.get("from");
                    route.put("from", value.get("value"));
                }
                if (route.containsKey("next_hop")) {
                    JSONObject value = (JSONObject) route.get("next_hop");
                    route.put("nextHop", value.get("value"));
                }
            }

            //absence check ?
            JSONArray gatewayArr = (JSONArray) vcnJson.get("gateways");

            //find driver type
            Properties rains_connectionProps = new Properties();
            rains_connectionProps.put("user", rains_db_user);
            rains_connectionProps.put("password", rains_db_pass);
            try {
                rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb", rains_connectionProps);
                prep = rains_conn.prepareStatement("SELECT driverEjbPath"
                        + " FROM driver_instance WHERE topologyUri = ?");
                prep.setString(1, topoUri);
                ResultSet rs1 = prep.executeQuery();
                rs1.next();
                String driverPath = rs1.getString(1);
                if (driverPath.contains("Aws")) {
                    driverType = "aws";
                } else if (driverPath.contains("OpenStack")) {
                    driverType = "ops";
                }

            } catch (SQLException ex) {
                logger.catching(method, ex);
            } finally {
                try {
                    DbUtils.close(rs);
                    DbUtils.close(prep);
                    DbUtils.close(rains_conn);
                } catch (SQLException ex) {
                    logger.catching("DBUtils", ex);
                }
            }

            if (driverType.equals("aws")) {
                //add gateway for aws cloud
                JSONArray gatewaysJson = new JSONArray();
                JSONObject temp = new JSONObject();
                temp.put("type", "internet");
                gatewaysJson.add(temp);
                temp = new JSONObject();
                temp.put("type", "vpn");
                gatewaysJson.add(temp);
                vcnJson.put("gateways", gatewaysJson);

                svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_clouds:tag+" + vcnName + "&gt;\n"
                        + "    a                         nml:Topology ;\n"
                        + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vcnName + "&gt;,"
                        + " &lt;x-policy-annotation:action:create-dc1&gt;.\n\n"
                        + "&lt;x-policy-annotation:data:" + vcnName + "-export&gt;\n"
                        + "    a            spa:PolicyData ;\n"
                        + "    spa:type     \"JSON\" ;\n"
                        + "    spa:format   \"\"\"{\n"
                        + "       \"parent\":\"" + topoUri + "\",\n"
                        + "       \"stitch_from\": \"%$.gateways[?(@.type=='vpn-gateway')].uri%\",\n"
                        + "    }\"\"\" .\n\n";
                awsDxStitching += "&lt;x-policy-annotation:action:create-dc1&gt;\n"
                        + "    a            spa:PolicyAction ;\n"
                        + "    spa:type     \"MCE_AwsDxStitching\" ;\n"
                        + "    spa:importFrom &lt;x-policy-annotation:data:" + vcnName + "-export&gt;,"
                        + " &lt;x-policy-annotation:data:aws-ops-criteriaexport&gt; ;\n"
                        + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vcnName + "&gt;,"
                        + " &lt;x-policy-annotation:action:create-aws-ops-path&gt;";

                String vncExportTo = "";
                for (int i = 0; i < vmList.size(); i++) {
                    String subnetCriteria = "&lt;x-policy-annotation:data:" + vcnName + "-subnet" + i + "-vm-criteria&gt;";
                    vncExportTo += subnetCriteria + ", ";
                    svcDelta += subnetCriteria + "\n    a            spa:PolicyData;\n"
                            + "    spa:type     \"JSON\";\n"
                            + "    spa:format    \"\"\"{\n"
                            + "       \"place_into\": \"%$.subnets[" + i + "].uri%\"\n"
                            + "    }\"\"\" .\n\n";

                    JSONArray vmArr = vmList.get(i);
                    for (Object vmObj : vmArr) {
                        JSONObject vmJson = (JSONObject) vmObj;
                        String vmName = (String) vmJson.get("name");
                        String vmType = (String) vmJson.get("type");
                        svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "&gt;\n"
                                + "    a                         nml:Node ;\n"
                                + "    nml:name         \"" + vmName + "\";\n"
                                + (vmType == null ? "" : "    mrs:type       \"" + vmType + "\";\n")
                                + "    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0&gt; ;\n"
                                + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;.\n\n"
                                + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0&gt;\n"
                                + "    a            nml:BidirectionalPort;\n"
                                + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;";
                        if (!vmJson.containsKey("interfaces")) {
                            svcDelta += ".\n\n";
                        } else {
                            String addressString = null;
                            JSONArray interArr = (JSONArray) vmJson.get("interfaces");
                            for (Object interObj : interArr) {
                                JSONObject interJson = (JSONObject) interObj;
                                String typeString = (String) interJson.get("type");
                                if (typeString.equalsIgnoreCase("Ethernet")) {
                                    addressString = (String) interJson.get("address");
                                    addressString = addressString.contains("ipv") ? addressString.substring(addressString.indexOf("ipv") + 5) : addressString;
                                    addressString = addressString.contains("/") ? addressString.substring(0, addressString.indexOf("/")) : addressString;
                                    break;
                                }
                            }
                            if (addressString != null) {
                                svcDelta += ";\n    mrs:hasNetworkAddress          "
                                        + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0:floatingip&gt; .\n\n"
                                        + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0:floatingip&gt;\n"
                                        + "    a            mrs:NetworkAddress;\n"
                                        + "    mrs:type     \"floating-ip\";\n"
                                        + "    mrs:value     \"" + addressString + "\".\n\n";
                            } else {
                                svcDelta += ".\n\n";
                            }
                        }
                        svcDelta += "&lt;x-policy-annotation:action:create-" + vmName + "&gt;\n"
                                + "    a            spa:PolicyAction ;\n"
                                + "    spa:type     \"MCE_VMFilterPlacement\" ;\n"
                                + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vcnName + "&gt; ;\n"
                                + "    spa:importFrom " + subnetCriteria + " .\n\n";
                    }
                }

                svcDelta += "&lt;x-policy-annotation:action:create-" + vcnName + "&gt;\n"
                        + "    a            spa:PolicyAction ;\n"
                        + "    spa:type     \"MCE_VirtualNetworkCreation\" ;\n"
                        + "    spa:importFrom &lt;x-policy-annotation:data:" + vcnName + "-criteria&gt; ;\n"
                        + "    spa:exportTo &lt;x-policy-annotation:data:" + vcnName + "-export&gt;"
                        + (vncExportTo.isEmpty() ? ".\n\n" : "," + vncExportTo.substring(0, (vncExportTo.length() - 2)) + " .\n\n");
            } else if (driverType.equals("ops")) {
                int sriovCounter = 1;
                String dependOn = "";
                String createPathExportTo = "";
                JSONObject connCriteriaValue = new JSONObject();
                String providesVolume = "";
                String svcDeltaCeph = "";
                String svcDeltaEndPoints = "";
                svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_clouds:tag+" + vcnName + "&gt;\n"
                        + "    a                         nml:Topology ;\n"
                        + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vcnName + "&gt;.\n\n";

                String vncExportTo = "";
                for (int i = 0; i < vmList.size(); i++) {
                    String subnetCriteria = "&lt;x-policy-annotation:data:" + vcnName + "-subnet" + i + "-vm-criteria&gt;";
                    vncExportTo += subnetCriteria + ", ";
                    svcDelta += subnetCriteria + "\n    a            spa:PolicyData;\n"
                            + "    spa:type     \"JSON\";\n"
                            + "    spa:format    \"\"\"{\n"
                            + "       \"place_into\": \"%$.subnets[" + i + "].uri%\"\n"
                            + "    }\"\"\" .\n\n";

                    JSONArray vmArr = vmList.get(i);
                    for (Object vmObj : vmArr) {
                        JSONObject vmJson = (JSONObject) vmObj;
                        String vmName = (String) vmJson.get("name");
                        String vmType = (String) vmJson.get("type");
                        String vmHost = (String) vmJson.get("host");
                        if (!vmHost.startsWith("any") && !vmHost.startsWith("urn:")) {
                            vmHost = topoUri + ":host+" + vmHost;
                        }
                        String nodeHasVolume = "";
                        if (vmJson.containsKey("ceph_rbd")) {
                            JSONArray cephArr = (JSONArray) vmJson.get("ceph_rbd");
                            for (int ceph = 0; ceph < cephArr.size(); ceph++) {
                                JSONObject cephJson = (JSONObject) cephArr.get(ceph);
                                nodeHasVolume += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":volume+ceph" + ceph + "&gt;, ";
                                svcDeltaCeph += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":volume+ceph" + ceph + "&gt;\n"
                                        + "   a  mrs:Volume;\n"
                                        + "   mrs:disk_gb \"" + (String) cephJson.get("disk_gb") + "\";\n"
                                        + "   mrs:mount_point \"" + (String) cephJson.get("mount_point") + "\".\n\n";
                            }
                            providesVolume += nodeHasVolume;
                        }
                        // Globus Connect
                        if (vmJson.containsKey("globus_connect")) {
                            String globusUri = "urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":service+globus";
                            String netAdresses = "\n";
                            try {
                                JSONObject globusJSON = (JSONObject) vmJson.get("globus_connect");
                                svcDeltaEndPoints += "&lt;" + globusUri + "&gt;\n"
                                        + "   a  mrs:EndPoint ;\n"
                                        + "   mrs:type \"globus:connect\" ;\n";
                                if (globusJSON.containsKey("username")) {
                                    String naUri = globusUri + ":username";
                                    svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; ;\n";
                                    netAdresses += "&lt;" + naUri + "&gt;\n"
                                            + "   a mrs:NetworkAddress ;\n"
                                            + "   mrs:type \"globus:username\";\n"
                                            + "   mrs:value \"" + (String) globusJSON.get("username") + "\" .\n";
                                }
                                if (globusJSON.containsKey("password")) {
                                    String naUri = globusUri + ":password";
                                    svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; ;\n";
                                    netAdresses += "&lt;" + naUri + "&gt;\n"
                                            + "   a mrs:NetworkAddress ;\n"
                                            + "   mrs:type \"globus:password\";\n"
                                            + "   mrs:value \"" + (String) globusJSON.get("password") + "\" .\n";
                                }
                                if (globusJSON.containsKey("default_directory")) {
                                    String naUri = globusUri + ":directory";
                                    svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; ;\n";
                                    netAdresses += "&lt;" + naUri + "&gt;\n"
                                            + "   a mrs:NetworkAddress ;\n"
                                            + "   mrs:type \"globus:directory\";\n"
                                            + "   mrs:value \"" + (String) globusJSON.get("default_directory") + "\" .\n";
                                }
                                if (globusJSON.containsKey("data_interface")) {
                                    String naUri = globusUri + ":interface";
                                    svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; ;\n";
                                    netAdresses += "&lt;" + naUri + "&gt;\n"
                                            + "   a mrs:NetworkAddress ;\n"
                                            + "   mrs:type \"globus:interface\";\n"
                                            + "   mrs:value \"" + (String) globusJSON.get("data_interface") + "\" .\n";
                                }
                                svcDeltaEndPoints += "   nml:name \"" + (String) globusJSON.get("short_name") + "\" .\n\n";
                            } catch (Exception ex) {
                                logger.catching(method, ex);
                            }
                            svcDeltaEndPoints += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "&gt;\n" + "    nml:hasService       &lt;" + globusUri + "&gt;. \n";
                            svcDeltaEndPoints += netAdresses;
                        }
                        // NFS Service
                        if (vmJson.containsKey("nfs")) {
                            String nfsUri = "urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":service+nfs";
                            String netAdresses = "\n";
                            try {
                                JSONObject nfsJSON = (JSONObject) vmJson.get("nfs");
                                svcDeltaEndPoints += "&lt;" + nfsUri + "&gt;\n"
                                        + "   a  mrs:EndPoint ;\n"
                                        + "   mrs:type \"nfs\" ;\n";
                                if (nfsJSON.containsKey("exports")) {
                                    String naUri = nfsUri + ":expots";
                                    svcDeltaEndPoints += "mrs:hasNetworkAddress &lt;" + naUri + "&gt; .\n";
                                    netAdresses += "&lt;" + naUri + "&gt;\n"
                                            + "   a mrs:NetworkAddress ;\n"
                                            + "   mrs:type \"nfs:exports\";\n"
                                            + "   mrs:value \"" + (String) nfsJSON.get("exports") + "\" .\n";
                                }
                            } catch (Exception ex) {
                                logger.catching(method, ex);
                            }
                            svcDeltaEndPoints += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "&gt;\n" + "    nml:hasService       &lt;" + nfsUri + "&gt;. \n";
                            svcDeltaEndPoints += netAdresses;
                        }

                        svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "&gt;\n"
                                + "    a                         nml:Node ;\n"
                                + "    nml:name         \"" + vmName + "\";\n"
                                + (vmType == null ? "" : "    mrs:type       \"" + vmType + "\";\n")
                                + (nodeHasVolume.isEmpty() ? "" : "    mrs:hasVolume       " + nodeHasVolume.substring(0, nodeHasVolume.length() - 2) + ";\n")
                                + "    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0&gt; ;\n";
                        // VM level routes
                        JSONArray vmRouteArr = null;
                        if (vmJson.containsKey("routes")) {
                            vmRouteArr = (JSONArray) vmJson.get("routes");
                            svcDelta += "    nml:hasService  &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":routingservice&gt; ;\n";
                        }
                        svcDelta += "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;.\n\n"
                                + "&lt;x-policy-annotation:action:create-" + vmName + "&gt;\n"
                                + "    a            spa:PolicyAction ;\n"
                                + "    spa:type     \"MCE_VMFilterPlacement\" ;\n"
                                + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vcnName + "&gt; ;\n"
                                + "    spa:importFrom &lt;x-policy-annotation:data:" + vcnName + "-" + vmName + "-host-criteria&gt;.\n\n"
                                + "&lt;x-policy-annotation:action:create-" + vmName + "-eth0&gt;\n"
                                + "    a            spa:PolicyAction ;\n"
                                + "    spa:type     \"MCE_VMFilterPlacement\" ;\n"
                                + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vcnName + "&gt; ;\n"
                                + "    spa:importFrom " + subnetCriteria + ".\n\n"
                                + "&lt;x-policy-annotation:data:" + vcnName + "-" + vmName + "-host-criteria&gt;\n"
                                + "    a            spa:PolicyData;\n"
                                + "    spa:type     \"JSON\";\n"
                                + "    spa:value    \"\"\"{\n"
                                + "       \"place_into\": \"" + vmHost + "\"\n"
                                + "    }\"\"\" .\n\n"
                                + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0&gt;\n"
                                + "    a            nml:BidirectionalPort ;\n"
                                + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "-eth0&gt; ";
                        // interfaces
                        if (!vmJson.containsKey("interfaces")) {
                            svcDelta += ".\n\n";
                        } else {
                            String addressString = null;
                            JSONArray interArr = (JSONArray) vmJson.get("interfaces");
                            for (Object interObj : interArr) {
                                JSONObject interJson = (JSONObject) interObj;
                                String typeString = (String) interJson.get("type");
                                if (typeString.equalsIgnoreCase("Ethernet")) {
                                    addressString = (String) interJson.get("address");
                                    addressString = addressString.contains("ipv") ? addressString.substring(addressString.indexOf("ipv") + 5) : addressString;
                                    addressString = addressString.contains("/") ? addressString.substring(0, addressString.indexOf("/")) : addressString;
                                    break;
                                }
                            }
                            if (addressString != null) {
                                svcDelta += ";\n    mrs:hasNetworkAddress          "
                                        + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0:floatingip&gt; .\n\n"
                                        + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0:floatingip&gt;\n"
                                        + "    a            mrs:NetworkAddress;\n"
                                        + "    mrs:type     \"floating-ip\";\n"
                                        + "    mrs:value     \"" + addressString + "\".\n\n";
                                for (Object interObj : interArr) {
                                    JSONObject interJson = (JSONObject) interObj;
                                    String typeString = (String) interJson.get("type");
                                    String gateway = (String) interJson.get("gateway");
                                    if (typeString.equalsIgnoreCase("SRIOV")) {
                                        //Parse sriov ip, mac, and routes.
                                        String address = (String) interJson.get("address");
                                        String[] addArr = address.split(",");
                                        String ip = null;
                                        String mac = null;
                                        for (String str : addArr) {
                                            ip = str.contains("ipv4") ? str.substring(str.indexOf("ipv4") + 5) : ip;
                                            mac = str.contains("mac") ? str.substring(str.indexOf("mac") + 4) : mac;
                                        }
                                        JSONArray routeArr = (JSONArray) interJson.get("routes");
                                        /*
                                        if (vmRouteArr != null && !vmRouteArr.isEmpty()) {
                                            if (routeArr == null) {
                                                routeArr = vmRouteArr;
                                            } else {
                                                routeArr.addAll(vmRouteArr);
                                            }
                                            vmRouteArr = null;
                                        }
                                         */
                                        //Find sriov parameter from Gateways.
                                        for (Object gwEle : gatewayArr) {
                                            JSONObject gwJSON = (JSONObject) gwEle;
                                            if (gwJSON.get("name").equals(gateway)) {
                                                if (gwJSON.get("type").equals("inter_cloud_network")) {
                                                    svcDelta += "&lt;x-policy-annotation:action:ucs-sriov-stitch" + sriovCounter + "&gt;\n"
                                                            + "    a            spa:PolicyAction ;\n"
                                                            + "    spa:type     \"MCE_UcsSriovStitching\" ;\n"
                                                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;, "
                                                            + "&lt;x-policy-annotation:action:create-" + vmName + "-eth0&gt;, "
                                                            + "&lt;x-policy-annotation:action:create-aws-ops-path&gt;;\n"
                                                            + "    spa:importFrom &lt;x-policy-annotation:data:sriov-criteria" + sriovCounter + "&gt;, "
                                                            + "&lt;x-policy-annotation:data:aws-ops-criteriaexport&gt; .\n\n"
                                                            + "&lt;x-policy-annotation:data:sriov-criteria" + sriovCounter + "&gt;\n"
                                                            + "    a            spa:PolicyData;\n"
                                                            + "    spa:type     \"JSON\";\n"
                                                            + "    spa:format    \"\"\"{\n"
                                                            + "       \"stitch_from\": \"urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "\",\n"
                                                            + "       \"to_l2path\": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn1%\n"
                                                            + "       \"mac_address\": \"" + mac + "\""
                                                            + (ip == null ? "" : ",\n       \"ip_address\": \"" + ip + "\"");

                                                    if (routeArr != null) {
                                                        for (Object r : routeArr) {
                                                            JSONObject route = (JSONObject) r;
                                                            if (route.containsKey("to")) {
                                                                JSONObject value = (JSONObject) route.get("to");
                                                                route.put("to", value.get("value"));
                                                            }
                                                            if (route.containsKey("from")) {
                                                                JSONObject value = (JSONObject) route.get("from");
                                                                route.put("from", value.get("value"));
                                                            }
                                                            if (route.containsKey("next_hop")) {
                                                                JSONObject value = (JSONObject) route.get("next_hop");
                                                                route.put("next_hop", value.get("value"));
                                                            }
                                                        }
                                                        svcDelta += ",\n       \"routes\": " + routeArr.toString().replace("\\", "");
                                                    }
                                                    svcDelta += "\n    }\"\"\" .\n\n";
                                                    if (gwJSON.containsKey("to") && ((JSONArray) gwJSON.get("to")).size() == 1 && ((JSONObject) ((JSONArray) gwJSON.get("to")).get(0)).containsKey("type")
                                                            && ((JSONObject) ((JSONArray) gwJSON.get("to")).get(0)).get("type").equals("peer_cloud")) {
                                                        String peerCloudNet = ((JSONObject) ((JSONArray) gwJSON.get("to")).get(0)).get("value").toString();
                                                        if (peerCloudNet.contains("?vlan=")) {
                                                            peerCloudNet = peerCloudNet.replace("?vlan=", "\":{\"vlan_tag\":\"");
                                                            peerCloudNet += "\"}\n";
                                                        } else {
                                                            peerCloudNet += "\":{\"vlan_tag\":\"any\"}\n";
                                                        }
                                                        svcDelta += "&lt;x-policy-annotation:data:aws-ops-criteria&gt;\n"
                                                                + "    a            spa:PolicyData;\n"
                                                                + "    spa:type     \"JSON\";\n"
                                                                + "    spa:value    \"\"\"{\n"
                                                                + "        \"urn:ogf:network:vo1_maxgigapop_net:link=conn1\": {\n"
                                                                + "            \"" + topoUri + "\":{\"vlan_tag\":\"any\"},\n"
                                                                + "            \"" + peerCloudNet
                                                                + "        }\n"
                                                                + "    }\"\"\".\n\n";
                                                    } else {
                                                        //@TODO: throw exception for format error!
                                                    }
                                                    if (vmJson.containsKey("quagga_bgp")) {
                                                        JSONObject quaggaJson = (JSONObject) vmJson.get("quagga_bgp");
                                                        if (!quaggaJson.containsKey("as_number")) {
                                                            quaggaJson.put("as_number", "%$..customer_asn%");
                                                        }
                                                        if (!quaggaJson.containsKey("router_id")) {
                                                            quaggaJson.put("router_id", "%$..customer_ip%");
                                                        }
                                                        JSONArray neighborArr = (JSONArray) quaggaJson.get("neighbors");
                                                        for (Object neighborObj : neighborArr) {
                                                            JSONObject neighborJson = (JSONObject) neighborObj;
                                                            if (!neighborJson.containsKey("remote_ip")) {
                                                                neighborJson.put("remote_ip", "%$..amazon_ip%");
                                                            }
                                                            if (!neighborJson.containsKey("local_ip")) {
                                                                neighborJson.put("local_ip", "%$..customer_ip%");
                                                            }
                                                        }
                                                        JSONArray quaggaNetworks = (JSONArray) quaggaJson.get("networks");
                                                        quaggaJson.remove("networks");
                                                        svcDelta += "&lt;x-policy-annotation:action:nfv-quagga-bgp" + sriovCounter + "&gt;\n"
                                                                + "    a            spa:PolicyAction ;\n"
                                                                + "    spa:type     \"MCE_NfvBgpRouting\";\n"
                                                                + "    spa:dependOn &lt;x-policy-annotation:action:create-dc1&gt;, "
                                                                + "&lt;x-policy-annotation:action:ucs-sriov-stitch" + sriovCounter + "&gt;;\n"
                                                                + "    spa:importFrom &lt;x-policy-annotation:data:quagga-bgp" + sriovCounter + "-remote&gt;, "
                                                                + "&lt;x-policy-annotation:data:quagga-bgp" + sriovCounter + "-local&gt;.\n\n"
                                                                + "&lt;x-policy-annotation:data:quagga-bgp" + sriovCounter + "-remote&gt;\n"
                                                                + "    a            spa:PolicyData ;\n"
                                                                + "    spa:type     \"JSON\" ;\n"
                                                                + "    spa:format   \"\"\"" + quaggaJson.toString() + "\"\"\" .\n\n"
                                                                + "&lt;x-policy-annotation:data:quagga-bgp" + sriovCounter + "-local&gt;\n"
                                                                + "    a            spa:PolicyData ;\n"
                                                                + "    spa:type     \"JSON\" ;\n"
                                                                + "    spa:value   \"\"\"{\n"
                                                                + "       \"parent\": \"urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "\",\n"
                                                                + "       \"networks\":" + quaggaNetworks.toString().replace("\\", "") + "\n"
                                                                + "    }\"\"\" .\n\n";
                                                        dependOn += "&lt;x-policy-annotation:action:nfv-quagga-bgp" + sriovCounter + "&gt;, ";
                                                        awsExportTo += "&lt;x-policy-annotation:data:quagga-bgp" + sriovCounter + "-remote&gt;, ";
                                                    } else {
                                                        dependOn += "&lt;x-policy-annotation:action:ucs-sriov-stitch" + sriovCounter + "&gt;, ";
                                                    }
                                                    dependOn += "&lt;x-policy-annotation:action:ucs-sriov-stitch" + sriovCounter + "&gt;, ";
                                                    creatPathExportTo += "&lt;x-policy-annotation:data:sriov-criteria" + sriovCounter + "&gt;, ";
                                                } else if (gwJSON.get("type").equals("ucs_port_profile")) {
                                                    //sriov port_profile
                                                    if (gwJSON.containsKey("from")) {
                                                        JSONArray fromArr = (JSONArray) gwJSON.get("from");
                                                        JSONObject fromJSON = (JSONObject) fromArr.get(0);
                                                        if (fromJSON.get("type").equals("port_profile")) {
                                                            //construct models;
                                                            dependOn += "&lt;x-policy-annotation:action:ucs-sriov-stitch-external-" + vmName + "-sriov" + sriovCounter + "&gt;, ";
                                                            svcDelta += "&lt;x-policy-annotation:action:ucs-sriov-stitch-external-" + vmName + "-sriov" + sriovCounter + "&gt;\n"
                                                                    + "    a            spa:PolicyAction ;\n"
                                                                    + "    spa:type     \"MCE_UcsSriovStitching\" ;\n"
                                                                    + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;, &lt;x-policy-annotation:action:create-" + vmName + "-eth0&gt;;\n"
                                                                    + "    spa:importFrom &lt;x-policy-annotation:data:sriov-criteria-external-" + vmName + "-sriov" + sriovCounter + "&gt;.\n"
                                                                    + "\n"
                                                                    + "&lt;x-policy-annotation:data:sriov-criteria-external-" + vmName + "-sriov" + sriovCounter + "&gt;\n"
                                                                    + "    a            spa:PolicyData;\n"
                                                                    + "    spa:type     \"JSON\";\n"
                                                                    + "    spa:value    \"\"\"{\n"
                                                                    + "       \"stitch_from\": \"urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "\",\n"
                                                                    + "       \"to_port_profile\": \"" + (String) fromJSON.get("value") + "\",\n"
                                                                    + "       \"mac_address\": \"" + mac + "\""
                                                                    + (ip == null ? "" : ",\n       \"ip_address\": \"" + ip + "\"")
                                                                    + ((routeArr == null || routeArr.isEmpty()) ? "" : ",\n       \"routes\": " + routeArr.toString().replace("\\", ""))
                                                                    + "\n    }\"\"\" .\n\n";
                                                        }
                                                    }
                                                    //sriov stitch_port
                                                    if (gwJSON.containsKey("to")) {
                                                        JSONArray toArr = (JSONArray) gwJSON.get("to");
                                                        JSONObject toJSON = (JSONObject) toArr.get(0);
                                                        if (toJSON.get("type").equals("stitch_port")) {
                                                            //construct models;
                                                            dependOn += "&lt;x-policy-annotation:action:ucs-" + vmName + "-sriov" + sriovCounter + "-stitch&gt;, ";
                                                            svcDelta += "&lt;x-policy-annotation:action:ucs-" + vmName + "-sriov" + sriovCounter + "-stitch&gt;\n"
                                                                    + "    a            spa:PolicyAction ;\n"
                                                                    + "    spa:type     \"MCE_UcsSriovStitching\" ;\n"
                                                                    + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;, &lt;x-policy-annotation:action:create-path&gt;;\n"
                                                                    + "    spa:importFrom &lt;x-policy-annotation:data:" + vmName + "-sriov" + sriovCounter + "-criteria&gt; .\n"
                                                                    + "\n"
                                                                    + "&lt;x-policy-annotation:data:" + vmName + "-sriov" + sriovCounter + "-criteria&gt;\n"
                                                                    + "    a            spa:PolicyData;\n"
                                                                    + "    spa:type     \"JSON\";\n"
                                                                    + "    spa:format    \"\"\"{\n"
                                                                    + "       \"stitch_from\": \"urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "\",\n"
                                                                    + "       \"to_l2path\": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn" + (String) gwJSON.get("name") + "%\n"
                                                                    + "       \"mac_address\": \"" + mac + "\""
                                                                    + (ip == null ? "" : ",\n       \"ip_address\": \"" + ip + "\"")
                                                                    + ((routeArr == null || routeArr.isEmpty()) ? "" : ",\n       \"routes\": " + routeArr.toString().replace("\\", ""))
                                                                    + "\n    }\"\"\" .\n\n";
                                                            createPathExportTo += "&lt;x-policy-annotation:data:" + vmName + "-sriov" + sriovCounter + "-criteria&gt;, ";
                                                            if (!connCriteriaValue.containsKey("urn:ogf:network:vo1_maxgigapop_net:link=conn" + (String) gwJSON.get("name"))) {
                                                                JSONObject vlanTag = new JSONObject();
                                                                String dest = (String) toJSON.get("value");
                                                                String vlan = "any";
                                                                if (dest.contains("?vlan=")) {
                                                                    dest = dest.substring(0, dest.indexOf("?vlan"));
                                                                    vlan = dest.substring(dest.indexOf("?vlan=") + 6);
                                                                }
                                                                vlanTag.put("vlan_tag", vlan);
                                                                JSONObject path = new JSONObject();
                                                                path.put(topoUri, vlanTag);
                                                                path.put(dest.replace("\\", ""), vlanTag);
                                                                connCriteriaValue.put("urn:ogf:network:vo1_maxgigapop_net:link=conn" + (String) gwJSON.get("name"), path);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    // @error handling !
                                                }
                                            }
                                        }
                                        sriovCounter++;
                                    }
                                }
                            } else {
                                svcDelta += ".\n\n";
                            }
                        }
                        if (vmRouteArr != null) {
                            svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":routingservice&gt;\n"
                                    + "     a   mrs:RoutingService;\n"
                                    + "     mrs:providesRoutingTable     " + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":routingservice:routingtable+linux&gt; .\n";
                            svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":routingservice:routingtable+linux&gt;\n"
                                    + "     a   mrs:RoutingTable;\n"
                                    + "     mrs:type   \"linux\";\n"
                                    + "     mrs:hasRoute    \n";
                            String vmRoutes = "";
                            int routeCt = 1;
                            for (Object r : vmRouteArr) {
                                JSONObject route = (JSONObject) r;
                                if (routeCt > 1) {
                                    svcDelta += ",\n";
                                }
                                svcDelta += "            &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":routingservice:routingtable+linux:route+" + routeCt + "&gt;\n";
                                vmRoutes += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":routingservice:routingtable+linux:route+" + routeCt + "&gt;\n"
                                        + "      a  mrs:Route;\n";
                                if (route.containsKey("to")) {
                                    vmRoutes += "      mrs:routeTo " + networkAddressFromJson((JSONObject) route.get("to")) + ";";
                                }
                                if (route.containsKey("from")) {
                                    vmRoutes += "      mrs:routeFrom " + networkAddressFromJson((JSONObject) route.get("from")) + ";";
                                }
                                if (route.containsKey("next_hop")) {
                                    vmRoutes += "      mrs:nextHop " + networkAddressFromJson((JSONObject) route.get("next_hop")) + ";";
                                }
                                vmRoutes = vmRoutes.trim();
                                vmRoutes += ".\n\n";
                                routeCt++;
                            }
                            svcDelta += ". \n\n" + vmRoutes;
                        }
                    }
                }

                if (!providesVolume.isEmpty()) {
                    svcDeltaCeph += "&lt;urn:ogf:network:openstack.com:openstack-cloud:ceph-rbd&gt;\n"
                            + "   mrs:providesVolume " + providesVolume.substring(0, providesVolume.length() - 2) + " .\n\n";
                }

                svcDelta += svcDeltaCeph
                        + svcDeltaEndPoints
                        + "&lt;x-policy-annotation:action:create-" + vcnName + "&gt;\n"
                        + "    a            spa:PolicyAction ;\n"
                        + "    spa:type     \"MCE_VirtualNetworkCreation\" ;\n"
                        + "    spa:importFrom &lt;x-policy-annotation:data:" + vcnName + "-criteria&gt; "
                        + (vncExportTo.isEmpty() ? "" : ";\n    spa:exportTo " + vncExportTo.substring(0, (vncExportTo.length() - 2)))
                        + ".\n\n"
                        + "&lt;" + topoUri + ":vt&gt;\n"
                        + "   a  nml:Topology;\n"
                        + "   spa:type spa:Abstraction;\n"
                        + "   spa:dependOn  " + dependOn.substring(0, dependOn.length() - 2) + ".\n\n";

                if (!createPathExportTo.isEmpty()) {
                    svcDelta += "&lt;x-policy-annotation:action:create-path&gt;\n"
                            + "    a            spa:PolicyAction ;\n"
                            + "    spa:type     \"MCE_MPVlanConnection\" ;\n"
                            + "    spa:importFrom &lt;x-policy-annotation:data:conn-criteria&gt; ;\n"
                            + "    spa:exportTo " + createPathExportTo.substring(0, (createPathExportTo.length() - 2)) + ".\n"
                            + "\n"
                            + "&lt;x-policy-annotation:data:conn-criteria&gt;\n"
                            + "    a            spa:PolicyData;\n"
                            + "    spa:type     \"JSON\";\n"
                            + "    spa:value    \"\"\"" + connCriteriaValue.toString() + "\"\"\".\n\n";
                }
            }

            svcDelta += "&lt;x-policy-annotation:data:" + vcnName + "-criteria&gt;\n"
                    + "    a            spa:PolicyData;\n"
                    + "    spa:type     nml:Topology;\n"
                    + "    spa:value    \"\"\"" + vcnJson.toString().replace("\\", "")
                    + "\"\"\".\n\n";
        }

        awsDxStitching += awsExportTo.isEmpty() ? ".\n\n"
                : ";\n    spa:exportTo " + awsExportTo.substring(0, awsExportTo.length() - 2) + ".\n\n";

        svcDelta += awsDxStitching
                + "&lt;x-policy-annotation:action:create-aws-ops-path&gt;\n"
                + "    a            spa:PolicyAction ;\n"
                + "    spa:type     \"MCE_MPVlanConnection\" ;\n"
                + "    spa:importFrom &lt;x-policy-annotation:data:aws-ops-criteria&gt; ;\n"
                + "    spa:exportTo &lt;x-policy-annotation:data:aws-ops-criteriaexport&gt;, "
                + creatPathExportTo.substring(0, creatPathExportTo.length() - 2) + " .\n\n"
                + "&lt;x-policy-annotation:data:aws-ops-criteriaexport&gt;\n"
                + "    a            spa:PolicyData;\n"
                + "    spa:type     \"JSON\" ;\n"
                + "    spa:format   \"\"\"{\n"
                + "       \"to_l2path\": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn1%\n"
                + "    }\"\"\" .\n\n"
                + "</modelAddition>\n\n"
                + "</serviceDelta>";
        orchestrateInstance(refUuid, svcDelta, deltaUuid, refresh);
        return 0;
    }

    public int createOperationModelModification(Map<String, String> paraMap, String auth) {
        String method = "createOperationModelModification";
        String refUuid = paraMap.get("instanceUUID");
        String deltaUUID = UUID.randomUUID().toString();
        String delta = "<serviceDelta>\n<uuid>" + deltaUUID
                + "</uuid>\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"
                + "\n\n<modelReduction>\n"
                + "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
                + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
                + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
                + "@prefix rdf:   &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .\n"
                + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n"
                + "@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .\n\n";

        delta += "&lt;x-policy-annotation:action:apply-modifications&gt;\n"
                + "    a            spa:PolicyAction ;\n"
                + "    spa:type     \"MCE_OperationalModelModification\" ;\n"
                + "    spa:importFrom &lt;x-policy-annotation:data:modification-map&gt; ;\n .\n\n"
                + "&lt;x-policy-annotation:data:modification-map&gt;\n"
                + "    a            spa:PolicyData;\n"
                + "    spa:type     \"JSON\";\n"
                + "    spa:value    \"\"\"" + paraMap.get("removeResource").replace("\\", "") + "\"\"\".\n\n";

        // need this for compilation
        delta += "&lt;urn:off:network:omm-abs&gt;\n"
                + "   a  nml:Topology;\n"
                + "   spa:type spa:Abstraction;\n"
                + "   spa:dependOn  &lt;x-policy-annotation:action:apply-modifications&gt;.\n\n";

        delta += "</modelReduction>\n\n"
                + "</serviceDelta>";

        String result;
        // Cache serviceDelta.
        int[] results = cacheServiceDelta(refUuid, delta, deltaUUID);
        int instanceID = results[0];
        int historyID = results[1];

        try {
            URL url = new URL(String.format("%s/service/%s", host, refUuid));
            HttpURLConnection compile = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, compile, "POST", delta, auth);
            if (!result.contains("referenceVersion")) {
                return 2;//Error occurs when interacting with back-end system
            }

            // Cache System Delta
            cacheSystemDelta(instanceID, historyID, result);

            url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
            HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, propagate, "PUT", null, auth);
            if (!result.equals("PROPAGATED")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/commit", host, refUuid));
            HttpURLConnection commit = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, commit, "PUT", null, auth);
            if (!result.equals("COMMITTED")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/status", host, refUuid));
            while (!result.equals("READY")) {
                sleep(5000);//wait for 5 seconds and check again later
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                result = this.executeHttpMethod(url, status, "GET", null, auth);
                if (!result.equals("COMMITTED")) {
                    return 3;//Fail to create network
                }
            }

            return 0;
        } catch (IOException | InterruptedException ex) {
            logger.catching(method, ex);
            return 1;//connection error
        }
    }

    // ------ Operations ------
// --------------------------- UTILITY FUNCTIONS -------------------------------
    /**
     * Executes HTTP Request.
     *
     * @param url destination url
     * @param conn connection object
     * @param method request method
     * @param body request body
     * @param authHeader authorization header
     * @return response string.
     * @throws IOException
     */
    public String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body, String authHeader) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-type", "application/xml");
        conn.setRequestProperty("Accept", "application/json");

        if (authHeader != null) {
            conn.setRequestProperty("Authorization", authHeader);
            //logger.log(Level.INFO, "{0} header added", authHeader);
        }

        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }

        logger.trace("executeHttpMethod", "Sending " + method + " request to URL: " + url);
        int responseCode = conn.getResponseCode();
        logger.trace("executeHttpMethod", "Response Code: " + responseCode);
        StringBuilder responseStr;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }

        String retStr = responseStr.substring(0, Math.min(responseStr.length(), 20));
        if (retStr.length() == 20) {
            retStr += "...";
        }

        logger.trace("executeHttpMethod", "Response: " + retStr);
        return responseStr.toString();
    }

    public String detailsStatus(String instanceUUID) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        try {
            URL url = new URL(String.format("%s/service/%s/status", host, instanceUUID));
            HttpURLConnection status = (HttpURLConnection) url.openConnection();
            return this.executeHttpMethod(url, status, "GET", null, null);
        } catch (IOException ex) {
            return "Error retrieving backend status!";
        }
    }

    public void cleanInstances() throws SQLException {
        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);

        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("DELETE FROM frontend.service_instance");
        prep.executeUpdate();
    }

    public int getInstanceID(String referenceUUID) throws SQLException {
        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);

        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT service_instance_id FROM service_instance WHERE referenceUUID = ?");
        prep.setString(1, referenceUUID);
        ResultSet rs1 = prep.executeQuery();
        rs1.next();
        return rs1.getInt("service_instance_id");
    }

    public int currentHistoryID(int instanceID) throws SQLException {
        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", "root");
        front_connectionProps.put("password", "root");

        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT service_history_id FROM service_history WHERE `service_instance_id` = ? ORDER BY service_history_id DESC LIMIT 0, 1");
        prep.setInt(1, instanceID);
        ResultSet rs1 = prep.executeQuery();
        rs1.next();
        return rs1.getInt(1);
    }

    /**
     *
     * @param serviceType filename of the service
     * @param name user-supplied tag
     * @param refUuid instance UUID
     * @return formatted URN.
     */
    public String urnBuilder(String serviceType, String name, String refUuid) {
        switch (serviceType) {
            case "dnc":
                return "urn:ogf:network:service+" + refUuid + ":resource+links:tag+" + name;
            case "netcreate":
                return "urn:ogf:network:service+" + refUuid + ":resource+virtual_clouds:tag+" + name;
            default:
                return "ERROR";
        }
    }

    public int[] cacheServiceDelta(String refUuid, String svcDelta, String deltaUUID) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        // Cache serviceDelta.
        int instanceID = -1;
        int historyID = -1;
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", "root");
            front_connectionProps.put("password", "root");

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT service_instance_id"
                    + " FROM service_instance WHERE referenceUUID = ?");
            prep.setString(1, refUuid);
            ResultSet rs1 = prep.executeQuery();
            rs1.next();
            instanceID = rs1.getInt(1);

            historyID = currentHistoryID(instanceID);

            String formatDelta = svcDelta.replaceAll("<", "&lt;");
            formatDelta = formatDelta.replaceAll(">", "&gt;");

            prep = front_conn.prepareStatement("INSERT INTO frontend.service_delta "
                    + "(`service_instance_id`, `service_history_id`, `type`, `referenceUUID`, `delta`) "
                    + "VALUES (?, ?, 'Service', ?, ?)");
            prep.setInt(1, instanceID);
            prep.setInt(2, historyID);
            prep.setString(3, deltaUUID);
            prep.setString(4, formatDelta);
            prep.executeUpdate();

        } catch (SQLException ex) {
            logger.catching("cacheServiceDelta", ex);
        } finally {
            try {
                DbUtils.close(rs);
                DbUtils.close(prep);
                DbUtils.close(front_conn);
            } catch (SQLException ex) {
                logger.catching("DBUtils", ex);
            }
        }

        return new int[]{instanceID, historyID};
    }

    private void cacheSystemDelta(int instanceID, int historyID, String result) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", "root");
            front_connectionProps.put("password", "root");

            // Retrieve UUID from delta
            /*
            
             */
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            String formatDelta = result.replaceAll("<", "&lt;");
            formatDelta = formatDelta.replaceAll(">", "&gt;");

            prep = front_conn.prepareStatement("INSERT INTO frontend.service_delta "
                    + "(`service_instance_id`, `service_history_id`, `type`, `delta`) "
                    + "VALUES (?, ?, 'System', ?)");
            prep.setInt(1, instanceID);
            prep.setInt(2, historyID);
            prep.setString(3, formatDelta);
            prep.executeUpdate();

        } catch (SQLException ex) {
            logger.catching("cacheSystemDelta", ex);
        } finally {
            try {
                DbUtils.close(rs);
                DbUtils.close(prep);
                DbUtils.close(front_conn);
            } catch (SQLException ex) {
                logger.catching("DBUtils", ex);
            }
        }
    }

    public int getServiceID(String filename) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT service_id FROM service WHERE filename = ?");
            prep.setString(1, filename);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                return rs1.getInt(1);
            }

            return -1;
        } catch (SQLException ex) {
            logger.catching("getServiceID", ex);
            return -1;
        } finally {
            try {
                DbUtils.close(rs);
                DbUtils.close(prep);
                DbUtils.close(front_conn);
            } catch (SQLException ex) {
                logger.catching("DBUtils", ex);
            }
        }
    }

    private String networkAddressFromJson(JSONObject jsonAddr) {
        if (!jsonAddr.containsKey("value")) {
            return "";
        }
        String type = "ipv4-address";
        if (jsonAddr.containsKey("type")) {
            type = jsonAddr.get("type").toString();
        }
        return String.format("[a    mrs:NetworkAddress; mrs:type    \"%s\"; mrs:value   \"%s\"]", type, jsonAddr.get("value").toString());
    }

    private void orchestrateInstance(String refUuid, String svcDelta, String deltaUUID, String refresh) {
        String method = "orchestrateInstance";
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String result;
        logger.trace_start(method);
        try {
            String token = refreshToken(refresh);
            result = initInstance(refUuid, svcDelta, token);
            logger.trace(method, "Initialized");

            // Cache serviceDelta.
            int[] results = cacheServiceDelta(refUuid, svcDelta, deltaUUID);
            int instanceID = results[0];
            int historyID = results[1];
            cacheSystemDelta(instanceID, historyID, result);

            token = refreshToken(refresh);
            propagateInstance(refUuid, svcDelta, token);
            logger.trace(method, "Propagated");

            token = refreshToken(refresh);
            result = commitInstance(refUuid, svcDelta, token);
            logger.trace(method, "Committed");

            verifyInstance(refUuid, result, refresh);
            logger.trace_end(method);
        } catch (EJBException | IOException | InterruptedException | SQLException ex) {
            try {
                Properties front_connectionProps = new Properties();
                front_connectionProps.put("user", front_db_user);
                front_connectionProps.put("password", front_db_pass);
                front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                        front_connectionProps);
                prep = front_conn.prepareStatement("UPDATE service_verification V INNER JOIN service_instance I SET V.verification_state = '-1' WHERE V.service_instance_id = I.service_instance_id AND I.referenceUUID = ?");
                prep.setString(1, refUuid);
                prep.executeUpdate();
            } catch (SQLException ex2) {
                logger.catching(method, ex2);
            }
            logger.catching(method, ex);
        } finally {
            try {
                DbUtils.close(rs);
                DbUtils.close(prep);
                DbUtils.close(front_conn);
            } catch (SQLException ex) {
                logger.catching("DBUtils", ex);
            }
        }
    }

    private String initInstance(String refUuid, String svcDelta, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s", host, refUuid));
        HttpURLConnection compile = (HttpURLConnection) url.openConnection();
        String result = this.executeHttpMethod(url, compile, "POST", svcDelta, auth);
        if (!result.contains("referenceVersion")) {
            throw new EJBException("Service Delta Failed!");
        }
        return result;
    }

    private String propagateInstance(String refUuid, String svcDelta, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = this.executeHttpMethod(url, propagate, "PUT", null, auth);
        if (!result.equals("PROPAGATED")) {
            throw new EJBException("Propagate Failed!");
        }
        return result;
    }

    private String commitInstance(String refUuid, String svcDelta, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit", host, refUuid));
        HttpURLConnection commit = (HttpURLConnection) url.openConnection();
        String result = this.executeHttpMethod(url, commit, "PUT", null, auth);
        if (!result.equals("COMMITTED")) {
            throw new EJBException("Commit Failed!");
        }
        return result;
    }

    private void verifyInstance(String refUuid, String result, String refresh) throws MalformedURLException, IOException, InterruptedException, SQLException {
        String method = "verifyInstance";
        logger.trace_start(method);
        String token = refreshToken(refresh);
        URL url = new URL(String.format("%s/service/%s/status", host, refUuid));
        int i = 1;
        while (!result.equals("READY") && !result.equals("FAILED")) {
            logger.trace(method, "Waiting on instance: " + result);
            if (i == 10) {
                token = refreshToken(refresh);
                i = 1;
            }
            sleep(5000);//wait for 5 seconds and check again later
            i++;
            HttpURLConnection status = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, status, "GET", null, token);
            /*if (!(result.equals("COMMITTED") || result.equals("FAILED"))) {
            throw new EJBException("Ready Check Failed!");
            }*/
        }
        verify(refUuid, refresh);
        logger.trace_end(method);
    }

    public boolean verify(String refUuid, String refresh) throws MalformedURLException, IOException, InterruptedException, SQLException {
        int instanceID = getInstanceID(refUuid);
        ResultSet rs = null;
        String method = "verify";
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        ThreadContext.put("refUUID", refUuid);
        logger.start(method);

        PreparedStatement prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = 0, `verification_run` = '0', `delta_uuid` = NULL, `creation_time` = NULL, `verified_addition` = NULL, `unverified_addition` = NULL, `addition` = NULL WHERE `service_verification`.`service_instance_id` = ?");
        prep.setInt(1, instanceID);
        prep.executeUpdate();

        for (int i = 1; i <= 5; i++) {
            logger.trace(method, "Start verification Run " + i + "/5");
            String auth = refreshToken(refresh);

            boolean redVerified = true, addVerified = true;
            URL url = new URL(String.format("%s/service/verify/%s", host, refUuid));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String result = executeHttpMethod(url, conn, "GET", null, auth);

            // Pull data from JSON.
            JSONObject verifyJSON = new JSONObject();
            try {
                Object obj = parser.parse(result);
                verifyJSON = (JSONObject) obj;
            } catch (ParseException ex) {
                throw new IOException("Parse Error within Verification: " + ex.getMessage());
            }

            // Update verification results cache.
            prep = front_conn.prepareStatement("UPDATE `service_verification` SET `delta_uuid`=?,`creation_time`=?,`verified_reduction`=?,`verified_addition`=?,`unverified_reduction`=?,`unverified_addition`=?,`reduction`=?,`addition`=?, `verification_run`=? WHERE `service_instance_id`=?");
            prep.setString(1, (String) verifyJSON.get("referenceUUID"));
            prep.setString(2, (String) verifyJSON.get("creationTime"));
            prep.setString(3, (String) verifyJSON.get("verifiedModelReduction"));
            prep.setString(4, (String) verifyJSON.get("verifiedModelAddition"));
            prep.setString(5, (String) verifyJSON.get("unverifiedModelReduction"));
            prep.setString(6, (String) verifyJSON.get("unverifiedModelAddition"));
            prep.setString(7, (String) verifyJSON.get("reductionVerified"));
            prep.setString(8, (String) verifyJSON.get("additionVerified"));
            prep.setInt(9, i);
            prep.setInt(10, instanceID);
            prep.executeUpdate();

            if (verifyJSON.containsKey("reductionVerified") && (verifyJSON.get("reductionVerified") != null) && ((String) verifyJSON.get("reductionVerified")).equals("false")) {
                redVerified = false;
            }
            if (verifyJSON.containsKey("additionVerified") && (verifyJSON.get("additionVerified") != null) && ((String) verifyJSON.get("additionVerified")).equals("false")) {
                addVerified = false;
            }

            if (redVerified && addVerified) {
                prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '1' WHERE `service_verification`.`service_instance_id` = ?");
                prep.setInt(1, instanceID);
                prep.executeUpdate();

                logger.end(method, "Success");
                try {
                    DbUtils.close(rs);
                    DbUtils.close(prep);
                    DbUtils.close(front_conn);
                } catch (SQLException ex) {
                    logger.catching("DBUtils", ex);
                }
                return true;
            }

            prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '0' WHERE `service_verification`.`service_instance_id` = ?");
            prep.setInt(1, instanceID);
            prep.executeUpdate();

            Thread.sleep(60000);
        }

        prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '-1' WHERE `service_verification`.`service_instance_id` = ?");
        prep.setInt(1, instanceID);
        prep.executeUpdate();

        logger.end(method, "Failure");
        try {
            DbUtils.close(rs);
            DbUtils.close(prep);
            DbUtils.close(front_conn);
        } catch (SQLException ex) {
            logger.catching("DBUtils", ex);
        }
        return false;
    }

    public String refreshToken(String refresh) {
        try {
            logger.trace_start("refreshToken");
            URL url = new URL(kc_url + "/realms/StackV/protocol/openid-connect/token");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // restapi
            //String encode = "cmVzdGFwaTpjMTZkMjRjMS0yNjJmLTQ3ZTgtYmY1NC1hZGE5YmQ4ZjdhY2E=";
            // StackV
            String encode = "U3RhY2tWOjQ4OTdlOGMzLWI4MzctNDIxMS1hOGYyLWFmM2Q2ZTM2M2RmMg==";

            conn.setRequestProperty("Authorization", "Basic " + encode);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            String data = "grant_type=refresh_token&refresh_token=" + refresh;
            try (OutputStream os = conn.getOutputStream(); BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"))) {
                writer.write(data);
                writer.flush();
            }

            conn.connect();
            StringBuilder responseStr;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
            }
            Object obj = parser.parse(responseStr.toString());
            JSONObject result = (JSONObject) obj;

            logger.trace_end("refreshToken");
            return "bearer " + (String) result.get("access_token");
        } catch (ParseException | IOException ex) {
            logger.catching("refreshToken", ex);
        }
        return null;
    }
}
