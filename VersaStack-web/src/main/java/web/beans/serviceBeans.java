package web.beans;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class serviceBeans {

    private static final Logger logger = Logger.getLogger(serviceBeans.class.getName());
    String login_db_user = "login_view";
    String login_db_pass = "loginuser";
    String front_db_user = "front_view";
    String front_db_pass = "frontuser";
    String rains_db_user = "root";
    String rains_db_pass = "root";
    String host = "http://localhost:8080/VersaStack-web/restapi";
    
    private Map<String,String> views = new HashMap<String,String>() {};
    
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
                        driver += "<value>java:module/StubSystemDriver</value></entry>";
                        break;                    
                    case "openStackDriver":
                        driver += "<value>java:module/GenericRESTDriver</value></entry>";
                        break;
                    case "StackDriver":
                        // to be filled
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
            if(entry.getKey().equalsIgnoreCase("versionGroup"))
                vgUuid = entry.getValue();
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
            //temporary code for assign IP
            String[] parameter = net.split(",");
            String assignedIp = parameter[1].substring(0, parameter[1].length()-1);
            Random rand = new Random();
            int i = rand.nextInt(251) + 4;
            
            //codes for assigning IP should be cleaned after AWS driver code being fixed
            String portUri = "&lt;" + topoUri + ":eni-" + UUID.randomUUID().toString() + "&gt;";
            model += "&lt;" + parameter[0] + "&gt;\n        nml:hasBidirectionalPort " + portUri + " .\n\n"
                    + portUri + "\n        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                    + "        mrs:hasTag            &lt;" + topoUri + ":portTag&gt; ;\n"
                    + "        mrs:hasNetworkAddress  &lt;"+ topoUri + ":" + assignedIp + i +"&gt; .\n\n"
                    + "&lt;"+ topoUri + ":" + assignedIp + i +"&gt;\n        "
                    + "a      mrs:NetworkAddress , owl:NamedIndividual ;\n"
                    + "        mrs:type  \"ipv4:private\" ;\n"
                    + "        mrs:value \""+ assignedIp + i +"\" .\n\n";
            
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
     * @param viewName The name of this filtered model
     * @param filters A string array. Each string contains SPARQL description, 
     * inclusive flag, subtreeRecursive flag, and suptreeRecursive flag, 
     * concatenated by "\r\n".<br /><br /> 
     * For example: CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o. ?s a nml:Topology.}\r\ntrue\r\nfalse\r\nfalse
     * @return
     * 0 - success.<br />
     * 1 - Query error.<br />
     */
    public int createModelView(String viewName, String[] filters) {
        String vgUuid = null;
        //create a new version group.
        try {
            URL url = new URL(String.format("%s/model/", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            this.executeHttpMethod(url, connection, "GET", null);           
        } catch (Exception e) {
            return 3;//connection error
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
            return 1;//query error
        }
        
        //Store the filtered view in the HashMap with the name of the view to be the key.
        views.put(viewName, result);
        return 0;
    }       

    
    // Utility Functions
    
    /**
     * Executes HTTP Request.
     * @param url destination url
     * @param conn connection object
     * @param method request method
     * @param body request body
     * @return response string.
     * @throws IOException 
     */
    private String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body) throws IOException {
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
