package web.beans;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
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
     * @param paraMap a key-value pair contains all the properties defined by user.
     * It should contains at least the driver ID and the topology uri. 
     * @return error code:<br />
     * 0 - success.<br />
     * 2 - plugin error.<br />
     * 3 - connection error.<br />
     */
    public int driverInstall(Map<String, String> paraMap) {
        String driver = "<driverInstance><properties>";
        for(Map.Entry<String, String> entry : paraMap.entrySet()){
            //if the key indicates what kind of driver it is, put the corresponding ejb path
            if(entry.getKey().equalsIgnoreCase("driverID")){
                driver += "<entry><key>driverEjbPath</key>";
                switch(entry.getValue()){
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
            }
            
            //if it is ttl model, modify the format so that the system can recognize the brackets
            else if(entry.getKey().equalsIgnoreCase("ttlmodel")){
                String ttlModel = entry.getValue().replaceAll("<", "&lt;");
                ttlModel = ttlModel.replaceAll(">", "&gt;");
                driver += "<entry><key>stubModelTtl</key><value>" + ttlModel +"</value></entry>";
                
            }
            
            //if it indicates it's a natserver in openstack, add this entry
            else if(entry.getKey().equalsIgnoreCase("NATServer") && entry.getValue().equalsIgnoreCase("yes")){
                driver += "<entry><key>NATServer</key><value></value></entry>";
            }
            
            //simply put the key value pair into the string
            else{
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

    /**

     * Create a virtual machine. Compose the ttl model according to the parsing
     * parameters. Put the ttl model and the VersionGroup UUID in the parsing 
     * parameter into the modelAddition part and referenceVersion respectively 
     * in the delta. Request an UUID for system instance and use the UUID to 
     * propagate and commit the delta via the system API.
     * @param paraMap a key-value pair contains all the required information, 
     * either selected by the user or assigned by the system, to build the request
     * virtual machine.
     * @return
     * 0 - success.<br />
     * 1 - Requesting System Instance UUID error.<br />
     * 2 - plugin error.<br />
     * 3 - connection error.<br />
     * 4 - parsing parameter error<br />
     */
    public int vmInstall(Map<String, String> paraMap){
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
        for(Map.Entry<String, String> entry : paraMap.entrySet()){
            if(entry.getKey().equalsIgnoreCase("driverType"))
                driverType = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("hypervisor"))
                hypervisor = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("topologyUri"))
                topoUri = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("region"))
                region = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("vpcID"))
                vpcUri = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("osType"))
                osType = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("instanceType"))
                instanceType = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("vmQuantity"))
                quantity = Integer.valueOf(entry.getValue());
            else if(entry.getKey().equalsIgnoreCase("vmName"))
                name = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("subnets"))
                subnets = entry.getValue().split("\r\n");
            else if(entry.getKey().equalsIgnoreCase("volumes"))
                volumes = entry.getValue().split("\r\n");            
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
        if(driverType.equalsIgnoreCase("awsdriver")){
            String nodeTag = "&lt;" + topoUri + ":i-" + UUID.randomUUID().toString() + "&gt;";
            String model = "&lt;" + vpcUri + "&gt;\n"
                    + "        nml:hasNode        " + nodeTag + ".\n\n"
                    + "&lt;" + topoUri + ":ec2service-" + region + "&gt;\n"
                    + "        mrs:providesVM  " + nodeTag + ".\n\n";
        
            //building all the volumes 
            String allVolUri = "";
            for(String vol : volumes){
               String volUri = "&lt;" + topoUri + ":vol-" + UUID.randomUUID().toString() + "&gt;";
               String[] parameters = vol.split(",");
               model += volUri +"\n        a                  mrs:Volume , owl:NamedIndividual ;\n"
                    + "        mrs:disk_gb        \"" + parameters[0] + "\" ;\n" 
                    + "        mrs:target_device  \"" + parameters[2] + "\" ;\n"
                    + "        mrs:value          \"" + parameters[1] + "\" .\n\n";
                allVolUri += volUri + " , ";
            }        
            model += "&lt;" + topoUri + ":ebsservice-" + region + "&gt;\n"
                   + "        mrs:providesVolume  " + allVolUri.substring(0, (allVolUri.length()-2)) + ".\n\n";

            //building all the network interfaces
            String allSubnets = "";
            for(String net : subnets){
                String portUri = "&lt;" + topoUri + ":eni-" + UUID.randomUUID().toString() + "&gt;";
                model += "&lt;" + net + "&gt;\n        nml:hasBidirectionalPort " + portUri + " .\n\n"
                       + portUri + "\n        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                        + "        mrs:hasTag            &lt;" + topoUri + ":portTag&gt; .\n\n";
            
                allSubnets += portUri + " , "; 
            }
        
            //building the node
            model += nodeTag +"\n        a                         nml:Node , owl:NamedIndividual ;\n"
                    + "        mrs:providedByService     &lt;" + topoUri + ":ec2service-" + region + "&gt; ;\n"
                    + "        mrs:hasVolume             " 
                    + allVolUri.substring(0, (allVolUri.length()-2)) + ";\n"
                    + "        nml:hasBidirectionalPort  "
                    + allSubnets.substring(0, (allSubnets.length()-2)) + ".\n\n";
        
            delta += model + "</modelAddition>\n</delta>";
        }
        //
        else if (driverType.equalsIgnoreCase("openStackDriver")){
            String nodeTag = "&lt;" + topoUri + ":server-name+" + UUID.randomUUID().toString() + "&gt;";
            String model = "&lt;" + vpcUri + "&gt;\n"
                    + "        nml:hasNode        " + nodeTag + ".\n\n"
                    + "&lt;" + hypervisor + "&gt;\n"
                    + "        mrs:providesVM  " + nodeTag + ".\n\n";
            //building all the subnets connected
            String allSubnets = "";
            for(String net : subnets){
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
                    + allSubnets.substring(0, (allSubnets.length()-2)) + ".\n\n";
            
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
            url = new URL(String.format("%s/delta/%s/commit", host,siUuid));
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
     * @param filters A string array. Each string contains SPARQL description, 
     * inclusive flag, subtreeRecursive flag, and suptreeRecursive flag, 
     * concatenated by "\r\n".<br /><br /> 
     * For example: CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o. ?s a nml:Topology.}\r\ntrue\r\nfalse\r\nfalse
     * @return
     * A string contains the filtered model in json format if creating successfully,
     * otherwise, a string contains the error message.
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
        for (String filter : filters){
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
            URL url = new URL(String.format("%s/model/view/%s", host,vgUuid));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, connection, "POST", view);
        } catch (Exception e) {
            System.out.println(e.toString());//query error
            return null;
        }
        
        return result;
    }       

    
    // Utility Functions
    
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
    
    private HashMap<String,ArrayList<String>> getJobProperties() throws SQLException {
        HashMap<String, ArrayList<String>> retMap = new HashMap<>();
        
        return retMap;
    }
    
    public int createNetwork(Map<String, String> paraMap){
        String topoUri = null;
        String driverType = null;
        String netType = null;
        String netCidr = null;
        List<String> subnets = new ArrayList<>();
        String[] netRoutes = null;
        boolean gwInternet = false;
        boolean gwVpn = false;        
        
        for(Map.Entry<String, String> entry : paraMap.entrySet()){
            if(entry.getKey().equalsIgnoreCase("driverType"))
                driverType = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("topoUri"))
                topoUri = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("netType"))
                netType = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("netCidr"))
                netCidr = entry.getValue();
            else if(entry.getKey().contains("subnet"))
                subnets.add(entry.getValue());
            else if (entry.getKey().equalsIgnoreCase("netRoutes"))
                netRoutes = entry.getValue().split("\r\n");
        }
        
        JSONObject network = new JSONObject();
        network.put("type", netType);
        network.put("cidr", netCidr);
        network.put("parent", topoUri);
        
        JSONArray subnetsJson = new JSONArray();
        //routing problem solved. need testing.
        for(String net : subnets){
            String[] netPara = net.split("&");
            JSONObject subnetValue = new JSONObject();
            for(String para : netPara){
                if(para.startsWith("routes")){
                    String[] route = para.substring(6).split("\r\n");
                    JSONArray routesArray = new JSONArray();
                    for(String r : route){
                        String[] routePara = r.split(",");
                        JSONObject jsonRoute = new JSONObject();
                        for(String rp : routePara){
                            String[] keyValue = rp.split("\\+");
                            jsonRoute.put(keyValue[0], keyValue[1]);
                            if(keyValue[1].contains("internet"))
                                gwInternet = true;
                            if(keyValue[1].contains("vpn"))
                                gwVpn = true;
                        }
                        routesArray.add(jsonRoute);
                    }
                    subnetValue.put("routes",routesArray);
                }
                else{
                    String[] keyValue = para.split("\\+");
                    subnetValue.put(keyValue[0], keyValue[1]);
                }
            }
            subnetsJson.add(subnetValue);
        }
        network.put("subnets", subnetsJson);
        
        
        JSONArray routesJson = new JSONArray();
        for(String route : netRoutes){
            JSONObject routesValue = new JSONObject();
            String[] routeDetail = route.split(",");
            for(String r : routeDetail){
                String[] keyValue = r.split("\\+");
                routesValue.put(keyValue[0], keyValue[1]);
                if(keyValue[1].contains("internet"))
                    gwInternet = true;
                if(keyValue[1].contains("vpn"))
                    gwVpn = true;
            }
            routesJson.add(routesValue);
        }
        network.put("routes", routesJson);
        
        JSONArray gatewaysJson = new JSONArray();
        if(gwInternet){
            JSONObject gatewayValue = new JSONObject();
            gatewayValue.put("type", "internet");
            gatewaysJson.add(gatewayValue);
        }
        if(gwVpn){
            JSONObject gatewayValue = new JSONObject();
            gatewayValue.put("type", "vpn");
            gatewaysJson.add(gatewayValue);
        }
        network.put("gateways", gatewaysJson);
        
        String svcDelta = "<serviceDelta>\n<uuid>" + UUID.randomUUID().toString()+
                "</uuid>\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"+
                "\n\n<modelAddition>\n" +
                "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n" +
                "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n" +
                "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n" +
                "@prefix rdf:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n" +
                "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n" +
                "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n" +
                "@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .\n\n" +
                "&lt;" + topoUri + ":vpc=abstract&gt;\n" +
                "    a                         nml:Topology ;\n" +
                "    spa:dependOn &lt;x-policy-annotation:action:create-" + driverType + "-vpc&gt; .\n\n" +
                "&lt;x-policy-annotation:action:create-" + driverType + "-vpc&gt;\n" +
                "    a           spa:PolicyAction ;\n" +
                "    spa:type     \"MCE_VirtualNetworkCreation\" ;\n" +
                "    spa:importFrom &lt;x-policy-annotation:data:vpc-criteria&gt; ;\n" +
                "    spa:exportTo &lt;x-policy-annotation:data:vpc-criteriaexport&gt; .\n" +
                "\n" +
                "\n" +
                "&lt;x-policy-annotation:data:vpc-criteria&gt;\n" +
                "    a            spa:PolicyData;\n" +
                "    spa:type     nml:Topology;\n" +
                "    spa:value    \"\"\"" + network.toString().replace("\\", "")+
                "\"\"\".\n" +
                "\n" +
                "\n" +
                "&lt;x-policy-annotation:data:vpc-criteriaexport&gt;\n" +
                "    a            spa:PolicyData;\n" +
                "\n" +
                "</modelAddition>\n" +
                "\n" +
                "</serviceDelta>";
        
        String siUuid;
        String result;
        try {
            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            siUuid = this.executeHttpMethod(url, connection, "GET", null);
            if (siUuid.length() != 36)
                return 2;//Error occurs when interacting with back-end system
            url = new URL(String.format("%s/service/%s",host,siUuid));
            HttpURLConnection compile = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, compile, "POST", svcDelta);
            if(!result.contains("referenceVersion"))
                return 2;//Error occurs when interacting with back-end system
            url = new URL(String.format("%s/service/%s/propagate",host,siUuid));
            HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, propagate, "PUT", null);
            if(!result.equals("PROPAGATED"))
                return 2;//Error occurs when interacting with back-end system
            url = new URL(String.format("%s/service/%s/commit",host,siUuid));
            HttpURLConnection commit = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, commit, "PUT", null);
            if(!result.equals("COMMITTED"))
                return 2;//Error occurs when interacting with back-end system
            url = new URL(String.format("%s/service/%s/status",host,siUuid));
            while(true){
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                result = this.executeHttpMethod(url, status, "GET", null); 
                if(result.equals("READY"))
                    return 0;//create network successfully
                else if(!result.equals("COMMITTED"))
                    return 3;//Fail to create network
                sleep(5000);//wait for 5 seconds and check again later
            }
        } catch (Exception e) {
            return 1;//connection error
        }
    }
    
    /**
     * Executes HTTP Request.
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
