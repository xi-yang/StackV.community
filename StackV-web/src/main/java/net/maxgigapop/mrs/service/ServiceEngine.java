/*
 * Copyright (c) 2013-2017 University of Maryland
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
package net.maxgigapop.mrs.service;

import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.common.TokenHandler;
import net.maxgigapop.mrs.rest.api.WebResource;
import static net.maxgigapop.mrs.rest.api.WebResource.commonsClose;
import static net.maxgigapop.mrs.rest.api.WebResource.executeHttpMethod;
import org.json.simple.JSONObject;

/**
 *
 * @author rikenavadur
 */
public class ServiceEngine {

    private final static StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "ServiceEngine");
    private final static String HOST = "http://127.0.0.1:8080/StackV-web/restapi";
    private final static String FRONT_DB_USER = "front_view";
    private final static String FRONT_DB_PASS = "frontuser";   

    // OPERATION FUNCTIONS    
    static void orchestrateInstance(String refUUID, JSONObject inputJSON, String deltaUUID, TokenHandler token, boolean autoProceed) throws EJBException, IOException, InterruptedException, SQLException {
        String method = "orchestrateInstance";
        String result;
        String lastState = "INIT";
        String svcDelta = (String) inputJSON.get("data");
        logger.start(method, svcDelta);

        int start = svcDelta.indexOf("<modelAddition>") + 15;
        int end = svcDelta.indexOf("</modelAddition>");
        String model = svcDelta.substring(start, end);
        String newModel = model.replace("<", "&lt;");
        newModel = newModel.replace(">", "&gt;");
        svcDelta = svcDelta.replace(model, newModel);
        svcDelta = svcDelta.replace("\\/", "/");

        try {
            // Cache serviceDelta.
            int results = cacheServiceDelta(refUUID, svcDelta, deltaUUID);
            int instanceID = results;

            result = initInstance(refUUID, svcDelta, token.auth());
            lastState = "COMPILED";
            logger.trace(method, "Initialized");
            cacheSystemDelta(instanceID, result);

            if (inputJSON.containsKey("host")) {
                pushProperty(refUUID, "host", (String) inputJSON.get("host"), token.auth());
            }

            if (autoProceed) {
                logger.trace(method, "Proceeding automatically");

                propagateInstance(refUUID, token.auth());
                lastState = "PROPAGATED";
                logger.trace(method, "Propagated");

                result = commitInstance(refUUID, token.auth());
                lastState = "COMMITTING";
                logger.trace(method, "Committing");

                URL url = new URL(String.format("%s/service/%s/status", HOST, refUUID));
                while (!result.equals("COMMITTED") && !result.equals("FAILED")) {
                    logger.trace(method, "Waiting on instance: " + result);
                    sleep(5000);//wait for 5 seconds and check again later        
                    HttpURLConnection status = (HttpURLConnection) url.openConnection();
                    result = WebResource.executeHttpMethod(url, status, "GET", null, token.auth());
                }

                if (result.equals("FAILED")) {
                    logger.trace(method, "Automatic verification skipped due to FAILED state");
                } else {
                    lastState = "COMMITTED";
                    logger.trace(method, "Committed");
                    VerificationHandler verify = new VerificationHandler(refUUID, token, 30, 10, false);
                    verify.startVerification();
                }
            }

            logger.end(method);
        } catch (EJBException | IOException | InterruptedException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            logger.trace_start("updateLastState", lastState);

            Connection front_conn = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            try {
                Properties front_connectionProps = new Properties();
                front_connectionProps.put("user", FRONT_DB_USER);
                front_connectionProps.put("password", FRONT_DB_PASS);

                front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                        front_connectionProps);

                prep = front_conn.prepareStatement("UPDATE service_instance SET last_state = ? WHERE referenceUUID = ?");
                prep.setString(1, lastState);
                prep.setString(2, refUUID);
                prep.executeUpdate();

                logger.trace_end("updateLastState");
            } catch (SQLException ex) {
                logger.catching(method, ex);
            } finally {
                commonsClose(front_conn, prep, rs);
            }
        }
    }

    // UTILITY FUNCTIONS    
    private static int cacheServiceDelta(String refUuid, String svcDelta, String deltaUUID) throws SQLException {
        String method = "cacheServiceDelta";
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;

        logger.trace_start(method);
        // Cache serviceDelta.
        int instanceID = -1;
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

            String formatDelta = svcDelta.replaceAll("<", "&lt;");
            formatDelta = formatDelta.replaceAll(">", "&gt;");

            prep = front_conn.prepareStatement("INSERT INTO frontend.service_delta "
                    + "(`service_instance_id`, `super_state`, `type`, `referenceUUID`, `delta`) "
                    + "VALUES (?, 'CREATE', 'Service', ?, ?)");
            prep.setInt(1, instanceID);
            prep.setString(2, deltaUUID);
            prep.setString(3, formatDelta);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            WebResource.commonsClose(front_conn, prep, rs);
        }

        logger.end(method);
        return instanceID;
    }

    private static void cacheSystemDelta(int instanceID, String result) throws SQLException {
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
                    + "(`service_instance_id`, `super_state`, `type`, `delta`) "
                    + "VALUES (?, 'CREATE', 'System', ?)");
            prep.setInt(1, instanceID);
            prep.setString(2, formatDelta);
            prep.executeUpdate();

        } catch (SQLException ex) {
            logger.catching("cacheSystemDelta", ex);
            throw ex;
        } finally {
            WebResource.commonsClose(front_conn, prep, rs);
        }
    }

    static String initInstance(String refUuid, String svcDelta, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s", HOST, refUuid));
        HttpURLConnection compile = (HttpURLConnection) url.openConnection();
        String result = WebResource.executeHttpMethod(url, compile, "POST", svcDelta, auth);
        if (!result.contains("referenceVersion")) {
            throw new EJBException("Service Delta Failed!");
        }
        return result;
    }

    static String propagateInstance(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate", HOST, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = WebResource.executeHttpMethod(url, propagate, "PUT", null, auth);
        if (!result.equals("PROPAGATED")) {
            throw new EJBException("Propagate Failed!");
        }
        return result;
    }

    static String commitInstance(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit", HOST, refUuid));
        HttpURLConnection commit = (HttpURLConnection) url.openConnection();
        String result = WebResource.executeHttpMethod(url, commit, "PUT", null, auth);
        if (!result.equals("COMMITTING")) {
            throw new EJBException("Commit Failed!");
        }
        return result;
    }

    public static String verifyInstance(String refUUID, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/verify/%s", HOST, refUUID));
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        String result = WebResource.executeHttpMethod(url, urlConn, "GET", null, auth);

        return result;
    }

    // -------------------------- SERVICE FUNCTIONS --------------------------------    
    static int createOperationModelModification(Map<String, String> paraMap, TokenHandler token) throws IOException, InterruptedException, SQLException {
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
        int results = cacheServiceDelta(refUuid, delta, deltaUUID);
        int instanceID = results;

        try {
            URL url = new URL(String.format("%s/service/%s", HOST, refUuid));
            HttpURLConnection compile = (HttpURLConnection) url.openConnection();
            result = WebResource.executeHttpMethod(url, compile, "POST", delta, token.auth());
            if (!result.contains("referenceVersion")) {
                return 2;//Error occurs when interacting with back-end system
            }

            // Cache System Delta
            cacheSystemDelta(instanceID, result);

            url = new URL(String.format("%s/service/%s/propagate", HOST, refUuid));
            HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
            result = WebResource.executeHttpMethod(url, propagate, "PUT", null, token.auth());
            if (!result.equals("PROPAGATED")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/commit", HOST, refUuid));
            HttpURLConnection commit = (HttpURLConnection) url.openConnection();
            result = WebResource.executeHttpMethod(url, commit, "PUT", null, token.auth());
            if (!result.equals("COMMITTED")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/status", HOST, refUuid));
            while (!result.equals("READY")) {
                sleep(5000);//wait for 5 seconds and check again later
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                result = WebResource.executeHttpMethod(url, status, "GET", null, token.auth());
                if (!result.equals("COMMITTED")) {
                    return 3;//Fail to create network
                }
            }

            return 0;
        } catch (IOException | InterruptedException ex) {
            logger.catching(method, ex);
            throw ex;
        }
    }

    static void pushProperty(String refUUID, String key, String value, String auth) throws IOException {
        URL url = new URL(String.format("%s/service/property/%s/%s", HOST, refUUID, key));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String result = executeHttpMethod(url, conn, "POST", value, auth);
    }
    
    

    public static String getCachedSystemDelta(String refUuid)  {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
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
            int instanceID = rs1.getInt(1);

            
            prep = front_conn.prepareStatement("SELECT delta FROM frontend.service_delta "
                    + "WHERE service_instance_id = ? AND type='System'");
            prep.setInt(1, instanceID);
            ResultSet rs2 = prep.executeQuery();
            rs2.next();
            return rs2.getString(1);
        } catch (SQLException ex) {
            logger.catching("getCachedSystemDelta", ex);
        } finally {
            WebResource.commonsClose(front_conn, prep, rs);
        }
        return null;
    }
}
