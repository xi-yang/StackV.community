package web.beans;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
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
            //connection error
            return 3;
        }

        return 0;
    }

    /**
     * Uninstalls driver via the sysmtem API
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