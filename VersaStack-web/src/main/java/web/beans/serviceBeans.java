package web.beans;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.common.ModelUtil;

public class serviceBeans {

    private static final Logger logger = Logger.getLogger(serviceBeans.class.getName());
    String login_db_user = "login_view";
    String login_db_pass = "loginuser";
    String front_db_user = "front_view";
    String front_db_pass = "frontuser";
    String host = "http://localhost:8080/VersaStack-web/restapi/";

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
     * 
     * @param parameters An array of the required parameters:<br />
     * [0] - Version Group UUID<br />
     * [1] - topology URI<br />
     * [2] - OS Type<br />
     * [3] - Instance Type(AWS)/Flavor(OPS)<br />
     * [4] - Name of the VMs<br />
     * [5] - Number of the VM want to created<br />
     * [6] - VPC Id<br />
     * [7] - Number of network interfaces want to attached to the VM<br />
     * [8] - Number of volume want to create in the VM<br />
     * @return
     * 0 - success.<br />
     * 1 - Requesting System Instance UUID error.<br />
     * 2 - unplug error.<br />
     * 3 - connection error.<br />
     * 4 - model building error<br />
     */
    public int vmInstall(String[] parameters){
        String siUuid ;        
        try {
            URL url = new URL(String.format("%s/model/systeminstance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            siUuid = this.executeHttpMethod(url, connection, "GET", null);
            if(siUuid.length()!=36)
                return 1;//not returning System Instance UUID. error occurs
        } catch (Exception e) {
            return 3;//connection error
        }

        String delta = "<delta>\n<id>1</id>\n" +
                       "<creationTime>2015-03-11T13:07:23.116-04:00</creationTime>\n" +
                       "<referenceVersion>"+ parameters[0] + "</referenceVersion>\n" +
                       "<modelReduction></modelReduction>\n\n" +
                       "<modelAddition>\n" +
                       "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n" +
                       "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n" +
                       "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n" +
                       "@prefix rdf:   &lt;http://schemas.ogf.org/nml/2013/03/base##&gt; .\n" +
                       "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n" +
                       "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .";
        
        
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        
        //transform the model into turtle format
        String ttlModel;
        try {
            ttlModel = ModelUtil.marshalOntModel(model);
        } catch (Exception ex) {
            Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
            return 4;//model building error
        }
        //replace the brackets so that the api won't misinterprete
        ttlModel = ttlModel.replaceAll("<", "&lt;");
        ttlModel = ttlModel.replaceAll(">", "&gt;");
        delta += ttlModel + "\n</modelAddition>\n</delta>";
        
        //push to the system api and get response
        try {
            //propagate the delta
            URL url = new URL(String.format("%s/delta/%s/propagate", host,siUuid));
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
            if (!result.equalsIgnoreCase("commit successfully")) //plugin error
            {
                return 2;
            }
            
        } catch (Exception e) {
            return 3;//connection error
        }
        
        
        return 0;
    }
    
    // Given a Topology, return list of VM's that can be added under it.
    //TODO Fill skeleton and JavaDoc as appropriate
    public ArrayList<String> VMSearchByTopology(String topoUri) {
        return null;
    }
    
    // Given a VM type, return list of topologies that can support it.
    //TODO Fill skeleton and JavaDoc as appropriate
    public ArrayList<String> VMSearchByType(String VMString) {
        return null;
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