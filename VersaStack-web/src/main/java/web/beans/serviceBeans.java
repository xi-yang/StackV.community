/* 
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Alberto Jimenez
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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class serviceBeans {

    private static final Logger logger = Logger.getLogger(serviceBeans.class.getName());
    String login_db_user = "login_view";
    String login_db_pass = "loginuser";
    String front_db_user = "front_view";
    String front_db_pass = "frontuser";
    String rains_db_user = "root";
    String rains_db_pass = "root";
    String host = "http://localhost:8080/VersaStack-web/restapi";

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
    public int driverInstall(Map<String, String> paraMap) {
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
            String result = this.executeHttpMethod(url, connection, "POST", driver);
            if (!result.equalsIgnoreCase("plug successfully")) //plugin error
            {
                return 2;
            }
        } catch (Exception e) {
            return 3;//connection error
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
    public int driverUninstall(String topoUri) {
        try {
            URL url = new URL(String.format("%s/driver/%s", host, topoUri));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = this.executeHttpMethod(url, connection, "DELETE", null);
            if (!result.equalsIgnoreCase("unplug successfully")) //unplug error
            {
                return 2;
            }
        } catch (Exception e) {
            return 3;//connection error
        }
        return 0;
    }

// -------------------------- SERVICE FUNCTIONS --------------------------------
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

    public int createConnection(Map<String, String> paraMap) {

        String refUuid = paraMap.get("instanceUUID");
        List<String> linkUri = new ArrayList<>();

        JSONObject connectJSON = new JSONObject();
        for (int i = 1; i <= 10; i++) {
            if (paraMap.containsKey("src-conn" + i)) {
                JSONObject exteriorJSON = new JSONObject();

                // Construct interior connection JSON Object
                JSONObject interiorJSON = new JSONObject();
                interiorJSON.put("vlan_tag", paraMap.get("src-vlan" + i));
                exteriorJSON.put(paraMap.get("src-conn" + i), interiorJSON);

                interiorJSON = new JSONObject();
                interiorJSON.put("vlan_tag", paraMap.get("des-vlan" + i));
                exteriorJSON.put(paraMap.get("des-conn" + i), interiorJSON);

                // Insert into carrier object
                connectJSON.put(paraMap.get("linkUri" + i), exteriorJSON);
            }
        }

        String deltaUUID = UUID.randomUUID().toString();

        String delta = "<serviceDelta>\n<uuid>" + deltaUUID
                + "</uuid>\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"
                + "\n\n<modelAddition>\n"
                + "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
                + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
                + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
                + "@prefix rdf:   &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .\n"
                + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n"
                + "@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .\n\n";

        for (String linkName : linkUri) {
            delta += "&lt;" + linkName + "&gt;\n"
                    + "      a            mrs:SwitchingSubnet ;\n"
                    + "spa:dependOn &lt;x-policy-annotation:action:create-path&gt;.\n\n";
        }

        delta += "&lt;urn:ogf:network:openstack.com:openstack-cloud:vlan&gt;\n"
                + "a mrs:SwitchingSubnet; spa:type spa:Abstraction;\n"
                + "spa:dependOn &lt;x-policy-annotation:action:create-path&gt; .\n\n";

        delta += "&lt;x-policy-annotation:action:create-path&gt;\n"
                + "    a            spa:PolicyAction ;\n"
                + "    spa:type     \"MCE_MPVlanConnection\" ;\n"
                + "    spa:importFrom &lt;x-policy-annotation:data:conn-criteria&gt; ;\n"
                + "    spa:exportTo &lt;x-policy-annotation:data:conn-criteriaexport&gt; .\n\n"
                + "&lt;x-policy-annotation:data:conn-criteria&gt;\n"
                + "    a            spa:PolicyData;\n"
                + "    spa:type     \"JSON\";\n"
                + "    spa:value    \"\"\"" + connectJSON.toString().replace("\\", "")
                + "    \"\"\".\n\n&lt;x-policy-annotation:data:conn-criteriaexport&gt;\n"
                + "    a            spa:PolicyData;\n"
                + "    spa:type     \"JSON\" .\n\n"
                + "</modelAddition>\n\n"
                + "</serviceDelta>";

        String result;

        // Cache serviceDelta.
        int[] results = cacheServiceDelta(refUuid, deltaUUID, delta);
        int instanceID = results[0];
        int historyID = results[1];

        try {
            URL url = new URL(String.format("%s/service/%s", host, refUuid));
            HttpURLConnection compile = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, compile, "POST", delta);
            if (!result.contains("referenceVersion")) {
                return 2;//Error occurs when interacting with back-end system
            }

            // Cache System Delta
            cacheSystemDelta(instanceID, historyID, result);

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
            url = new URL(String.format("%s/service/%s/status", host, refUuid));
            while (!result.equals("READY")) {
                sleep(5000);//wait for 5 seconds and check again later
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                result = this.executeHttpMethod(url, status, "GET", null);
                if (!result.equals("COMMITTED")) {
                    return 3;//Fail to create network
                }
            }

            return 0;
        } catch (Exception e) {
            return 1;//connection error
        }

    }

    public int createNetwork(Map<String, String> paraMap) {
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
                Connection rains_conn;
                try {
                    rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb", rains_connectionProps);
                    PreparedStatement prep = rains_conn.prepareStatement("SELECT driverEjbPath"
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
                    Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (entry.getKey().contains("subnet")) {
                subnets.add(entry.getValue());
            } else if (entry.getKey().contains("vm")) {
                vmList.add(entry.getValue());
            }
            //example for vm : vm1&0
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
        if (gwVpn || (driverType.equals("aws") && directConn != null)) {
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
        if (driverType.equals("aws") && directConn != null) {
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
            if (driverType.equals("aws")) {
                for (String vm : vmList) {
                    String[] vmPara = vm.split("&");
                    //0:vm name.
                    //1:subnet #
                    //2:types: image, instance, key pair, security group
                    svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "&gt;\n"
                            + "    a                         nml:Node ;\n"
                            + (vmPara[2].equals(" ") ? "" : "    mrs:type       \"" + vmPara[2] + "\";\n")
                            + "    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0&gt; ;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;.\n\n"
                            + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0&gt;\n"
                            + "    a            nml:BidirectionalPort;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;.\n\n"
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
            } else if (driverType.equals("ops")) {
                String createPathExportTo = "";
                String dependOn = "";
                JSONObject connCriteriaValue = new JSONObject();

                for (String vm : vmList) {
                    String[] vmPara = vm.split("&");
                    //0:vm name.
                    //1:subnet #
                    //2:types: image, instance, key pair, security group
                    //3:vm host
                    //4:Interfaces: floating IP, SRIOV
                    svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + "&gt;\n"
                            + "    a                         nml:Node ;\n"
                            + (vmPara[2].equals(" ") ? "" : "    mrs:type       \"" + vmPara[2] + "\";\n")
                            + "    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmPara[0] + ":eth0&gt; ;\n"
                            + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmPara[0] + "&gt;.\n\n"
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
                                        ip = str.contains("ip") ? str.substring(5) : ip;
                                        mac = str.contains("mac") ? str.substring(4) : mac;
                                    }

                                    //Find sriov parameter from Gateways.
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

                                            //sriov port_profile
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
                                                            + "       \"to_l2path\": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn" + (String) gwJSON.get("name") + "%\n"
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
                        } catch (Exception ex) {
                            Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        svcDelta += ".\n\n";
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
                            + "    spa:value    \"\"\"" + connCriteriaValue.toString() + "\"\"\".\n\n";
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

        //System.out.println(svcDelta);
        // Cache serviceDelta.
        int[] results = cacheServiceDelta(refUuid, deltaUUID, svcDelta);
        int instanceID = results[0];
        int historyID = results[1];

//        String siUuid;
        String result;
        try {
            URL url = new URL(String.format("%s/service/%s", host, refUuid));
            HttpURLConnection compile = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, compile, "POST", svcDelta);
            if (!result.contains("referenceVersion")) {
                return 2;//Error occurs when interacting with back-end system
            }

            // Cache System Delta
            cacheSystemDelta(instanceID, historyID, result);

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
            url = new URL(String.format("%s/service/%s/status", host, refUuid));
            while (!result.equals("READY")) {
                sleep(5000);//wait for 5 seconds and check again later
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                result = this.executeHttpMethod(url, status, "GET", null);
                if (!result.equals("COMMITTED")) {
                    return 3;//Fail to create network
                }
            }

            return 0;

        } catch (IOException | InterruptedException e) {
            return 1;//connection error
        }
    }

    public int createHybridCloud(Map<String, String> paraMap) {
        String refUuid = null;
        JSONParser jsonParser = new JSONParser();
        JSONArray vcnArr = null;
        ArrayList<String> topoUriList = new ArrayList<>();
        String creatPathExportTo = "";
        //Mapping from paraMap to local variables
        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("instanceUUID")) {
                refUuid = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("virtual_clouds")) {
                try {
                    vcnArr = (JSONArray) jsonParser.parse(entry.getValue());
                } catch (ParseException ex) {
                    Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        String deltaUUID = UUID.randomUUID().toString();
        String awsExportTo = "";
        String awsDxStitching = "";
        String svcDelta = "<serviceDelta>\n<uuid>" + deltaUUID
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
            topoUriList.add(topoUri);
            String driverType = "";
            String vcnName = (String) vcnJson.get("name");
            vcnJson.remove("name");

            JSONArray subArr = (JSONArray) vcnJson.get("subnets");
            ArrayList<JSONArray> vmList = new ArrayList<JSONArray>();
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

            //find driver type
            Properties rains_connectionProps = new Properties();
            rains_connectionProps.put("user", rains_db_user);
            rains_connectionProps.put("password", rains_db_pass);
            Connection rains_conn;

            /*
             try {
             rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb", rains_connectionProps);
             PreparedStatement prep = rains_conn.prepareStatement("SELECT driverEjbPath"
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
             Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
             }
             */
            if (topoUri.contains("amazon") || topoUri.contains("aws")) {
                driverType = "aws";
            } else if (topoUri.contains("openstack")) {
                driverType = "ops";
            }

            switch (driverType) {
                case "aws": {
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
                                    + (vmType == null ? "" : "    mrs:type       \"" + vmType + "\";\n")
                                    + "    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0&gt; ;\n"
                                    + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;.\n\n"
                                    + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0&gt;\n"
                                    + "    a            nml:BidirectionalPort;\n"
                                    + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;.\n\n"
                                    + "&lt;x-policy-annotation:action:create-" + vmName + "&gt;\n"
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
                    break;
                }
                case "ops": {
                    int sriovCounter = 1;
                    String dependOn = "";
                    String providesVolume = "";
                    String svcDeltaCeph = "";
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
                            String nodeHasVolume = "";
                            if (vmJson.containsKey("ceph_rbd")) {
                                JSONArray cephArr = (JSONArray) vmJson.get("ceph_rbd");
                                for (int ceph = 0; ceph < cephArr.size(); ceph++) {
                                    JSONObject cephJson = (JSONObject) cephArr.get(i);
                                    nodeHasVolume += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":volume+ceph" + ceph + "&gt;, ";
                                    svcDeltaCeph += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":volume+ceph" + ceph + "&gt;\n"
                                            + "   a  mrs:Volume;\n"
                                            + "   mrs:disk_gb \"" + (String) cephJson.get("disk_gb") + "\";\n"
                                            + "   mrs:mount_point \"" + (String) cephJson.get("mount_point") + "\".\n\n";
                                }
                                providesVolume += nodeHasVolume;
                            }

                            svcDelta += "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + "&gt;\n"
                                    + "    a                         nml:Node ;\n"
                                    + (vmType == null ? "" : "    mrs:type       \"" + vmType + "\";\n")
                                    + (nodeHasVolume.isEmpty() ? "" : "    mrs:hasVolume       " + nodeHasVolume.substring(0, nodeHasVolume.length() - 2) + ";\n")
                                    + "    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0&gt; ;\n"
                                    + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "&gt;.\n\n"
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
                                    + "       \"place_into\": \"" + topoUri + ":host+" + vmHost + "\"\n"
                                    + "    }\"\"\" .\n\n"
                                    + "&lt;urn:ogf:network:service+" + refUuid + ":resource+virtual_machines:tag+" + vmName + ":eth0&gt;\n"
                                    + "    a            nml:BidirectionalPort ;\n"
                                    + "    spa:dependOn &lt;x-policy-annotation:action:create-" + vmName + "-eth0&gt; ";

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
                                                ip = str.contains("ip") ? str.substring(5) : ip;
                                                mac = str.contains("mac") ? str.substring(4) : mac;
                                            }
                                            JSONArray routeArr = (JSONArray) interJson.get("routes");

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
                                            sriovCounter++;
                                            break;
                                        }
                                    }
                                } else {
                                    svcDelta += ".\n\n";
                                }
                            }
                        }
                    }
                    if (!providesVolume.isEmpty()) {
                        svcDeltaCeph += "&lt;urn:ogf:network:openstack.com:openstack-cloud:ceph-rbd&gt;\n"
                                + "   mrs:providesVolume " + providesVolume.substring(0, providesVolume.length() - 2) + " .\n\n";
                    }
                    svcDelta += svcDeltaCeph
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
                    break;
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

        // creatPathExportTo is required.
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
                + "&lt;x-policy-annotation:data:aws-ops-criteria&gt;\n"
                + "    a            spa:PolicyData;\n"
                + "    spa:type     \"JSON\";\n"
                + "    spa:value    \"\"\"{\n"
                + "        \"urn:ogf:network:vo1_maxgigapop_net:link=conn1\": {\n"
                + "            \"" + topoUriList.get(0) + "\":{\"vlan_tag\":\"any\"},\n"
                + "            \"" + topoUriList.get(1) + "\":{\"vlan_tag\":\"any\"}\n"
                + "        }\n"
                + "    }\"\"\".\n\n"
                + "</modelAddition>\n\n"
                + "</serviceDelta>";

        //System.out.println(svcDelta);
        // Cache serviceDelta.
        int[] results = cacheServiceDelta(refUuid, deltaUUID, svcDelta);
        int instanceID = results[0];
        int historyID = results[1];

        String result;
        try {
            URL url = new URL(String.format("%s/service/%s", host, refUuid));
            HttpURLConnection compile = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, compile, "POST", svcDelta);
            if (!result.contains("referenceVersion")) {
                return 2;//Error occurs when interacting with back-end system
            }

            // Cache System Delta
            cacheSystemDelta(instanceID, historyID, result);

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
            url = new URL(String.format("%s/service/%s/status", host, refUuid));
            while (!result.equals("READY")) {
                sleep(5000);//wait for 5 seconds and check again later
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                result = this.executeHttpMethod(url, status, "GET", null);
                if (!result.equals("COMMITTED")) {
                    return 3;//Fail to create network
                }
            }

            return 0;

        } catch (Exception e) {
            return 1;//connection error
        }
    }

// --------------------------- UTILITY FUNCTIONS -------------------------------    
    public HashMap<String, String> getJobStatuses() throws SQLException {
        HashMap<String, String> retMap = new HashMap<>();

        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        ArrayList<String> service_list = new ArrayList<>();
        PreparedStatement prep = front_conn.prepareStatement("SELECT S.name, I.referenceUUID FROM service S, service_instance I WHERE I.service_id = S.service_id");
        ResultSet rs1 = prep.executeQuery();
        while (rs1.next()) {
            String name = rs1.getString("name");
            String refId = rs1.getString("referenceUUID");

            String status;
            try {
                URL url = new URL(String.format("%s/service/%s/status", host, refId));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                status = this.executeHttpMethod(url, connection, "GET", null);

                retMap.put(name, status);
            } catch (Exception e) {
                System.out.println(e.toString());//query error
            }
        }

        return retMap;
    }

    private HashMap<String, ArrayList<String>> getJobProperties() throws SQLException {
        HashMap<String, ArrayList<String>> retMap = new HashMap<>();

        return retMap;
    }

    /**
     * Executes HTTP Request.
     *
     * @param url destination url
     * @param conn connection object
     * @param method request method
     * @param body request body
     * @return response string.
     * @throws IOException
     */
    public String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-type", "application/xml");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }
        //logger.log(Level.INFO, "Sending {0} request to URL : {1}", new Object[]{method, url});
        int responseCode = conn.getResponseCode();
        //logger.log(Level.INFO, "Response Code : {0}", responseCode);

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

    public ArrayList<ArrayList<String>> instanceStatusCheck() throws SQLException {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();
        ArrayList<String> banList = new ArrayList<>();

        banList.add("Driver Management");

        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);

        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT S.name, I.referenceUUID, X.super_state, I.alias_name FROM"
                + " service S, service_instance I, service_state X WHERE S.service_id = I.service_id AND I.service_state_id = X.service_state_id");
        ResultSet rs1 = prep.executeQuery();
        while (rs1.next()) {
            ArrayList<String> instanceList = new ArrayList<>();

            String instanceName = rs1.getString("name");
            String instanceUUID = rs1.getString("referenceUUID");
            String instanceSuperState = rs1.getString("super_state");
            String instanceAlias = rs1.getString("alias_name");
            if (!banList.contains(instanceName)) {
                try {
                    URL url = new URL(String.format("%s/service/%s/status", host, instanceUUID));
                    HttpURLConnection status = (HttpURLConnection) url.openConnection();

                    String instanceState = instanceSuperState + " - " + this.executeHttpMethod(url, status, "GET", null);

                    instanceList.add(instanceName);
                    instanceList.add(instanceUUID);
                    instanceList.add(instanceState);
                    instanceList.add(instanceAlias);

                    retList.add(instanceList);
                } catch (IOException ex) {
                    logger.log(Level.INFO, "Instance Status Check Failed on UUID = {0}", instanceUUID);
                }
            }
        }

        return retList;
    }

    public ArrayList<String> instanceStatusCheck(String instanceUUID) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        ArrayList<String> retList = new ArrayList<>();

        Connection front_conn;
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);

        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT S.name, X.super_state FROM"
                + " service S, service_instance I, service_state X WHERE I.referenceUUID = ? AND S.service_id = I.service_id AND I.service_state_id = X.service_state_id");
        prep.setString(1, instanceUUID);
        ResultSet rs1 = prep.executeQuery();
        while (rs1.next()) {
            String instanceName = rs1.getString("name");
            String instanceSuperState = rs1.getString("super_state");
            try {
                URL url = new URL(String.format("%s/service/%s/status", host, instanceUUID));
                HttpURLConnection status = (HttpURLConnection) url.openConnection();

                String instanceState = instanceSuperState + " - " + this.executeHttpMethod(url, status, "GET", null);

                retList.add(instanceName);
                retList.add(instanceUUID);
                retList.add(instanceState);
            } catch (IOException ex) {
            }
        }

        return retList;
    }

    public String detailsStatus(String instanceUUID) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        try {
            URL url = new URL(String.format("%s/service/%s/status", host, instanceUUID));
            HttpURLConnection status = (HttpURLConnection) url.openConnection();
            return this.executeHttpMethod(url, status, "GET", null);
        } catch (IOException ex) {
            return "Error retrieving backend status!";
        }
    }

    public ArrayList<ArrayList<String>> catalogPull(int usergroup_id, int user_id) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);

        try {
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT DISTINCT S.name, S.filename, S.description FROM service S JOIN acl A, acl_entry_group G, acl_entry_user U "
                    + "WHERE S.atomic = 0 AND A.service_id = S.service_id AND ((A.acl_id = G.acl_id AND G.usergroup_id = ?) OR (A.acl_id = U.acl_id AND U.user_id = ?))");
            prep.setInt(1, usergroup_id);
            prep.setInt(2, user_id);
            ResultSet rs1 = prep.executeQuery();

            while (rs1.next()) {
                ArrayList<String> instanceList = new ArrayList<>();

                instanceList.add(rs1.getString("name"));
                instanceList.add(rs1.getString("description"));
                instanceList.add(rs1.getString("filename"));

                retList.add(instanceList);
            }
        } catch (SQLException e) {
            System.out.println("THIS IS A NEW BUILD.");
            System.out.println("Exception: " + e);
        }

        return retList;
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

    private int[] cacheServiceDelta(String refUuid, String deltaUUID, String svcDelta) {
        // Cache serviceDelta.
        int instanceID = -1;
        int historyID = -1;
        try {
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", "root");
            front_connectionProps.put("password", "root");

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT service_instance_id"
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
            Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new int[]{instanceID, historyID};
    }

    private void cacheSystemDelta(int instanceID, int historyID, String result) {
        try {
            Connection front_conn;
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

            PreparedStatement prep = front_conn.prepareStatement("INSERT INTO frontend.service_delta "
                    + "(`service_instance_id`, `service_history_id`, `type`, `delta`) "
                    + "VALUES (?, ?, 'System', ?)");
            prep.setInt(1, instanceID);
            prep.setInt(2, historyID);
            prep.setString(3, formatDelta);
            prep.executeUpdate();

        } catch (SQLException ex) {
            Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

// ------------------------- DEPRECATED SERVICES -------------------------------
    /**
     *
     * Create a virtual machine. Compose the ttl model according to the parsing
     * parameters. Put the ttl model and the VersionGroup UUID in the parsing
     * parameter into the modelAddition part and referenceVersion respectively
     * in the delta. Request an UUID for system instance and use the UUID to
     * propagate and commit the delta via the system API.
     *
     * @param paraMap a key-value pair contains all the required information,
     * either selected by the user or assigned by the system, to build the
     * request virtual machine.
     * @return 0 - success.<br />
     * 1 - Requesting System Instance UUID error.<br />
     * 2 - plugin error.<br />
     * 3 - connection error.<br />
     * 4 - parsing parameter error<br />
     */
    public int vmInstall(Map<String, String> paraMap) {
        String vgUuid = null;
        String driverType = null;
        String hypervisor = null;
        String topoUri = null;
        String region = null;
        String vpcUri = null;
        String osType = null;
        String instanceType = null;
        String name = null;
        String[] subnets = null;
        String[] volumes = null;
        int quantity;

        //Map the parsing parameters into each variable
        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("driverType")) {
                driverType = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("hypervisor")) {
                hypervisor = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("topologyUri")) {
                topoUri = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("region")) {
                region = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("vpcID")) {
                vpcUri = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("osType")) {
                osType = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("instanceType")) {
                instanceType = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("vmQuantity")) {
                quantity = Integer.valueOf(entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("vmName")) {
                name = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("subnets")) {
                subnets = entry.getValue().split("\r\n");
            } else if (entry.getKey().equalsIgnoreCase("volumes")) {
                volumes = entry.getValue().split("\r\n");
            }
        }

        try {
            URL url = new URL(String.format("%s/model/", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            this.executeHttpMethod(url, connection, "GET", null);
        } catch (Exception e) {
            return 3;//connection error
        }

        Connection rains_conn;
        // Database Connection
        try {

            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Properties rains_connectionProps = new Properties();
            rains_connectionProps.put("user", rains_db_user);
            rains_connectionProps.put("password", rains_db_pass);

            rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb",
                    rains_connectionProps);

            PreparedStatement prep = rains_conn.prepareStatement("SELECT * FROM version_group ORDER BY id DESC LIMIT 1");
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                vgUuid = rs1.getString("refUuid");
            }
        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
        }

        //create a system instance and get an UUID for this system instance from the API
        String siUuid;

        try {
            URL url = new URL(String.format("%s/model/systeminstance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            siUuid = this.executeHttpMethod(url, connection, "GET", null);
            if (siUuid.length() != 36) {
                return 1;//not returning System Instance UUID. error occurs
            }
        } catch (Exception e) {
            return 3;//connection error
        }
        //building ttl model
        String delta = "<delta>\n<id>1</id>\n"
                + "<creationTime>2015-03-11T13:07:23.116-04:00</creationTime>\n"
                + "<referenceVersion>" + vgUuid + "</referenceVersion>\n"
                + "<modelReduction></modelReduction>\n\n"
                + "<modelAddition>\n"
                + "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
                + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
                + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
                + "@prefix rdf:   &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .\n"
                + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n\n";

        //check if driver the client choose is of AWS
        if (driverType.equalsIgnoreCase("awsdriver")) {
            String nodeTag = "&lt;" + topoUri + ":i-" + UUID.randomUUID().toString() + "&gt;";
            String model = "&lt;" + vpcUri + "&gt;\n"
                    + "        nml:hasNode        " + nodeTag + ".\n\n"
                    + "&lt;" + topoUri + ":ec2service-" + region + "&gt;\n"
                    + "        mrs:providesVM  " + nodeTag + ".\n\n";

            //building all the volumes 
            String allVolUri = "";
            for (String vol : volumes) {
                String volUri = "&lt;" + topoUri + ":vol-" + UUID.randomUUID().toString() + "&gt;";
                String[] parameters = vol.split(",");
                model += volUri + "\n        a                  mrs:Volume , owl:NamedIndividual ;\n"
                        + "        mrs:disk_gb        \"" + parameters[0] + "\" ;\n"
                        + "        mrs:target_device  \"" + parameters[2] + "\" ;\n"
                        + "        mrs:value          \"" + parameters[1] + "\" .\n\n";
                allVolUri += volUri + " , ";
            }
            model += "&lt;" + topoUri + ":ebsservice-" + region + "&gt;\n"
                    + "        mrs:providesVolume  " + allVolUri.substring(0, (allVolUri.length() - 2)) + ".\n\n";

            //building all the network interfaces
            String allSubnets = "";
            for (String net : subnets) {
                String portUri = "&lt;" + topoUri + ":eni-" + UUID.randomUUID().toString() + "&gt;";
                model += "&lt;" + net + "&gt;\n        nml:hasBidirectionalPort " + portUri + " .\n\n"
                        + portUri + "\n        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                        + "        mrs:hasTag            &lt;" + topoUri + ":portTag&gt; .\n\n";

                allSubnets += portUri + " , ";
            }

            //building the node
            model += nodeTag + "\n        a                         nml:Node , owl:NamedIndividual ;\n"
                    + "        mrs:providedByService     &lt;" + topoUri + ":ec2service-" + region + "&gt; ;\n"
                    + "        mrs:hasVolume             "
                    + allVolUri.substring(0, (allVolUri.length() - 2)) + ";\n"
                    + "        nml:hasBidirectionalPort  "
                    + allSubnets.substring(0, (allSubnets.length() - 2)) + ".\n\n";

            delta += model + "</modelAddition>\n</delta>";
        } //
        else if (driverType.equalsIgnoreCase("openStackDriver")) {
            String nodeTag = "&lt;" + topoUri + ":server-name+" + UUID.randomUUID().toString() + "&gt;";
            String model = "&lt;" + vpcUri + "&gt;\n"
                    + "        nml:hasNode        " + nodeTag + ".\n\n"
                    + "&lt;" + hypervisor + "&gt;\n"
                    + "        mrs:providesVM  " + nodeTag + ".\n\n";
            //building all the subnets connected
            String allSubnets = "";
            for (String net : subnets) {
                String portUri = "&lt;" + topoUri + ":port+" + UUID.randomUUID().toString() + "&gt;";
                model += "&lt;" + net + "&gt;\n       a       mrs:SwitchingSubnet , owl:NamedIndividual ;\n"
                        + "        nml:hasBidirectionalPort " + portUri + " .\n\n"
                        + portUri + "\n        a       nml:BidirectionalPort , owl:NamedIndividual ;\n"
                        + "        mrs:hasTag             &lt;" + topoUri + ":portTag&gt; .\n\n";
                allSubnets += portUri + " , ";
            }
            //building the node
            model += nodeTag + "\n        a       nml:Node , owl:NamedIndividual ;\n"
                    + "        nml:hasBidirectionalPort                "
                    + allSubnets.substring(0, (allSubnets.length() - 2)) + ".\n\n";

            delta += model + "</modelAddition>\n</delta>";
        }

        //push to the system api and get response
        try {
            //propagate the delta
            URL url = new URL(String.format("%s/delta/%s/propagate", host, siUuid));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = this.executeHttpMethod(url, connection, "POST", delta);
            if (!result.equalsIgnoreCase("propagate successfully")) //plugin error
            {
                return 2;
            }
            //commit the delta
            url = new URL(String.format("%s/delta/%s/commit", host, siUuid));
            connection = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, connection, "PUT", "");
            if (!result.equalsIgnoreCase("PROCESSING")) //plugin error
            {
                return 2;
            }

        } catch (Exception e) {
            return 3;//connection error
        }

        return 0;

    }

    /**
     * Create a customize model view based on the criteria user specifies.
     *
     * @param filters A string array. Each string contains SPARQL description,
     * inclusive flag, subtreeRecursive flag, and suptreeRecursive flag,
     * concatenated by "\r\n".<br /><br />
     * For example: CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o. ?s a
     * nml:Topology.}\r\ntrue\r\nfalse\r\nfalse
     * @return A string contains the filtered model in json format if creating
     * successfully, otherwise, a string contains the error message.
     */
    public String createModelView(String[] filters) {
        String vgUuid = null;
        //create a new version group.
        try {
            //URL url = new URL(String.format("http://localhost:8080/VersaStack-web/data/json/umd-anl-all.json"));
            URL url = new URL(String.format("%s/model/", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            this.executeHttpMethod(url, connection, "GET", null);
        } catch (Exception e) {
            System.out.println(e.toString());//connection error
            return null;
        }

        //retrieve the version group UUID from the database.
        Connection rains_conn;
        try {

            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Properties rains_connectionProps = new Properties();
            rains_connectionProps.put("user", rains_db_user);
            rains_connectionProps.put("password", rains_db_pass);

            rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb",
                    rains_connectionProps);

            PreparedStatement prep = rains_conn.prepareStatement("SELECT * FROM version_group ORDER BY id DESC LIMIT 1");
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                vgUuid = rs1.getString("refUuid");
            }
        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
        }

        //construct the queryModelView object.
        String view = "<view><filters>";
        for (String filter : filters) {
            String[] filterParam = filter.split("\r\n");
            view += "<filter><sparql>" + filterParam[0] + "</sparql>"
                    + "<inclusive>" + filterParam[1] + "</inclusive>"
                    + "<subtreeRecursive>" + filterParam[2] + "</subtreeRecursive>"
                    + "<suptreeRecursive>" + filterParam[3] + "</suptreeRecursive></filter>";
        }
        view += "</filters></view>";

        //Send the request though API to back-end system.
        String result;
        try {
            URL url = new URL(String.format("%s/model/view/%s", host, vgUuid));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, connection, "POST", view);
        } catch (Exception e) {
            System.out.println(e.toString());//query error
            return null;
        }

        return result;
    }

}
