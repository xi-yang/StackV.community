package web.beans;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * Installs driver into model.
     *
     * @param driverID driver identifier;<br /> either 'stubdriver',
     * 'awsdriver', 'versaNSDriver', 'openStackDriver'<br />
     * @param topoName user defined name for the topology;
     * @param accountID the access id to the specified driver;
     * @param accountPW the access password to the specified driver;
     * @param awsRegion the geographic area for amazon web service;
     * @return error code:<br />
     * 0 - success.<br />
     * 1 - invalid driverID.<br />
     * 2 - plugin error.<br />
     * 3 - connection error.<br />
     */
    public int driverInstall(String driverID, String topoName, String accountID, String accountPW, String awsRegion) {
        String driver = "";
        if (driverID.equalsIgnoreCase("stubdriver")) {
            driver = "<driverInstance><properties><entry><key>topologyUri</key>"
                    + "<value>urn:ogf:network:rains.maxgigapop.net:2013:topology"
                    + topoName + "</value></entry><entry><key>driverEjbPath</key>"
                    + "<value>java:module/StubSystemDriver</value></entry>"
                    + "<entry><key>stubModelTtl</key>"
                    + "<value>@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#>.\n"
                    + "@prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>.\n"
                    + "@prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>.\n"
                    + "<http://www.maxgigapop.net/mrs/2013/topology#> a owl:Ontology;\n"
                    + "    rdfs:label \"NML-MRS Description of the MAX Research Infrastructure\".\n"
                    + "<urn:ogf:network:rains.maxgigapop.net:2013:topology>\n"
                    + "    a   nml:Topology,\n"
                    + "        owl:NamedIndividual;\n"
                    + "    nml:hasNode\n"
                    + "        <urn:ogf:network:rains.maxgigapop.net:2013:clpk-msx-1>,\n"
                    + "        <urn:ogf:network:rains.maxgigapop.net:2013:clpk-msx-4>."
                    + "</value></entry></properties></driverInstance>";

        } else if (driverID.equalsIgnoreCase("awsdriver")) {
            driver = "<driverInstance><properties><entry><key>topologyUri</key>"
                    + "<value>urn:ogf:network:aws.amazon.com:aws-cloud" + topoName + "</value></entry>"
                    + "<entry><key>driverEjbPath</key><value>java:module/AwsDriver</value></entry>"
                    + "<entry><key>aws_access_key_id</key><value>" + accountID + "</value></entry>"
                    + "<entry><key>aws_secret_access_key</key><value>" + accountPW + "</value></entry>"
                    + "<entry><key>region</key><value>" + awsRegion + "</value></entry></properties></driverInstance>";
        } else if (driverID.equalsIgnoreCase("versaNSDriver")) {
            driver = "<driverInstance><properties><entry><key>topologyUri</key>"
                    + "<value>urn:ogf:network:sdn.maxgigapop.net:network" + topoName + "</value></entry>"
                    + "<entry><key>driverEjbPath</key><value>java:module/GenericRESTDriver</value></entry>"
                    + "<entry><key>subsystemBaseUrl</key><value>http://localhost:8080/VersaNS-0.0.1-SNAPSHOT</value></entry>"
                    + "</properties></driverInstance>";
        } else if (driverID.equalsIgnoreCase("openStackDriver")) {
            driver =  "<driverInstance><properties><entry><key>url</key>"
                    + "<value>http://max-vlsr2.dragon.maxgigapop.net:35357/v2.0</value></entry>"
                    + "<entry><key>NATServer</key><value></value></entry>"
                    + "<entry><key>driverEjbPath</key><value>java:module/OpenStackDriver</value></entry>"
                    + "<entry><key>username</key><value>" + accountID + "</value></entry><entry>"
                    + "<key>password</key><value>" + accountPW + "</value></entry><entry><key>topologyUri</key>"
                    + "<value>urn:ogf:network:openstack.com:openstack-cloud" + topoName + "</value></entry>"
                    + "<entry><key>tenant</key><value>admin</value></entry></properties></driverInstance>";
        } else if(driverID.equalsIgnoreCase("StackDriver")){
            //for VersaStack
        } else //invalid driverID
        {
            return 1;
        }
        try {
            URL url = new URL(String.format("%s/driver", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = this.executeHttpMethod(url, connection, "POST", driver);
            if (!result.equalsIgnoreCase("plug successfully")) //plugin error
            {
                return 2;
            }
        } catch (Exception e) {
            //connection error
            return 3;
        }

        return 0;
    }

    /** TODO: UPDATE FOR NEW PARAMETERS<br />
     * Uninstalls driver from model.
     *
     * @param driverID driver identifier;<br /> either 'stubdriver',
     * 'awsdriver', 'versaNSDriver', 'openStackDriver'
     * @return error code:<br />
     * 0 - success.<br />
     * 1 - invalid driverID.<br />
     * 2 - unplug error.<br />
     * 3 - connection error.<br />
     */
    public int driverUninstall(String driverID, String par1, String par2, String par3) {
        String topoUri = "";
        if (driverID.equalsIgnoreCase("stubdriver")) {
            topoUri = "urn:ogf:network:rains.maxgigapop.net:2013:topology";
        } else if (driverID.equalsIgnoreCase("awsdriver")) {
            topoUri = "urn:ogf:network:aws.amazon.com:aws-cloud";
        } else if (driverID.equalsIgnoreCase("versaNSDriver")) {
            topoUri = "urn:ogf:network:sdn.maxgigapop.net:network";
        } else if (driverID.equalsIgnoreCase("openStackDriver")) {
            topoUri = "urn:ogf:network:openstack.com:openstack-cloud";
        } else if(driverID.equalsIgnoreCase("StackDriver")){
            //for VersaStack
        } else //invalid driverID
        {
            return 1;
        }

        try {
            URL url = new URL(String.format("%s/driver/%s", host, topoUri));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = this.executeHttpMethod(url, connection, "DELETE", null);
            if (!result.equalsIgnoreCase("unplug successfully")) //unplug error
            {
                return 2;
            }
        } catch (Exception e) {
            //connection error
            return 3;
        }
        return 0;
    }

    //@@TODO Fill out Javadoc
    public int vmInstall() {
        return -1;
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