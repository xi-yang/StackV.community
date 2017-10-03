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
import org.json.simple.JSONObject;

/**
 *
 * @author rikenavadur
 */
class ServiceEngine {

    private final static StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "ServiceEngine");
    private final static String host = "http://127.0.0.1:8080/StackV-web/restapi";
    private final static String front_db_user = "front_view";
    private final static String front_db_pass = "frontuser";
    private final static String rains_db_user = "root";
    private final static String rains_db_pass = "root";

    // OPERATION FUNCTIONS    
    static void orchestrateInstance(String refUUID, String svcDelta, String deltaUUID, TokenHandler token, boolean autoProceed) {
        String method = "orchestrateInstance";
        String result;
        String lastState = "INIT";
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
            logger.trace(method, "Initialized");
            cacheSystemDelta(instanceID, result);

            if (autoProceed) {
                logger.trace(method, "Proceeding automatically");

                propagateInstance(refUUID, token.auth());
                lastState = "PROPAGATED";
                logger.trace(method, "Propagated");

                result = commitInstance(refUUID, token.auth());
                lastState = "COMMITTING";
                logger.trace(method, "Committing");

                URL url = new URL(String.format("%s/service/%s/status", host, refUUID));
                while (!result.equals("COMMITTED") && !result.equals("FAILED")) {
                    logger.trace(method, "Waiting on instance: " + result);
                    sleep(5000);//wait for 5 seconds and check again later        
                    HttpURLConnection status = (HttpURLConnection) url.openConnection();
                    result = WebResource.executeHttpMethod(url, status, "GET", null, token.auth());
                }
                lastState = "COMMITTED";
                logger.trace(method, "Committed");

                if (!result.equals("FAILED")) {
                    VerificationHandler verify = new VerificationHandler(refUUID, token);
                    verify.startVerification();
                } else {
                    logger.trace(method, "Automatic verification skipped due to FAILED state");
                }
            }

            logger.end(method);
        } catch (EJBException | IOException | InterruptedException ex) {
            logger.catching(method, ex);
        } finally {
            logger.trace_start("updateLastState", lastState);

            Connection front_conn = null;
            PreparedStatement prep = null;
            ResultSet rs = null;
            try {
                Properties front_connectionProps = new Properties();
                front_connectionProps.put("user", front_db_user);
                front_connectionProps.put("password", front_db_pass);

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

    /*static String verify(String refUuid, TokenHandler token) throws MalformedURLException, IOException, InterruptedException, SQLException {
        ResultSet rs;
        String method = "verify";
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        ThreadContext.put("refUUID", refUuid);
        logger.start(method);

        PreparedStatement prep = front_conn.prepareStatement("SELECT service_instance_id FROM service_instance WHERE referenceUUID = ?");
        prep.setString(1, refUuid);
        rs = prep.executeQuery();
        rs.next();
        int instanceID = rs.getInt("service_instance_id");

        prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = 0, `verification_run` = '0', `delta_uuid` = NULL, `creation_time` = NULL, `verified_addition` = NULL, `unverified_addition` = NULL, `addition` = NULL WHERE `service_verification`.`service_instance_id` = ?");
        prep.setInt(1, instanceID);
        prep.executeUpdate();

        for (int run = 1; run <= 30; run++) {
            prep = front_conn.prepareStatement("SELECT V.enabled"
                    + " FROM service_instance I, service_verification V"
                    + " WHERE referenceUUID = ? AND I.service_instance_id = V.service_instance_id");
            prep.setString(1, refUuid);
            rs = prep.executeQuery();
            rs.next();
            boolean enabled = rs.getBoolean("enabled");
            if (!enabled) {
                logger.end(method, "Disabled");
                WebResource.commonsClose(front_conn, prep, rs);
                return "READY";
            }

            logger.trace(method, "Verification Attempt: " + run + "/30");

            boolean redVerified = true, addVerified = true;
            URL url = new URL(String.format("%s/service/verify/%s", host, refUuid));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String result = WebResource.executeHttpMethod(url, conn, "GET", null, token.auth());

            // Pull data from JSON.
            JSONParser parser = new JSONParser();
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
            prep.setInt(9, run);
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
                WebResource.commonsClose(front_conn, prep, rs);
                return "READY";
            }

            prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '0' WHERE `service_verification`.`service_instance_id` = ?");
            prep.setInt(1, instanceID);
            prep.executeUpdate();

            Thread.sleep(10000);
        }

        prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '-1' WHERE `service_verification`.`service_instance_id` = ?");
        prep.setInt(1, instanceID);
        prep.executeUpdate();

        logger.end(method, "Failure");
        WebResource.commonsClose(front_conn, prep, rs);
        return "READY";
    }*/
 /*static void toggleVerify(boolean enabled, String refUuid, TokenHandler token) throws MalformedURLException, IOException, InterruptedException, SQLException {
        ResultSet rs;
        String method = "cancelVerify";
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        ThreadContext.put("refUUID", refUuid);
        logger.trace_start(method);

        PreparedStatement prep = front_conn.prepareStatement("SELECT service_instance_id FROM service_instance WHERE referenceUUID = ?");
        prep.setString(1, refUuid);
        rs = prep.executeQuery();
        rs.next();
        int instanceID = rs.getInt("service_instance_id");

        prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `enabled` = ? WHERE `service_verification`.`service_instance_id` = ?");
        prep.setBoolean(1, enabled);
        prep.setInt(2, instanceID);
        prep.executeUpdate();

        logger.trace_end(method);
        WebResource.commonsClose(front_conn, prep, rs);
    }*/
    // UTILITY FUNCTIONS    
    private static int cacheServiceDelta(String refUuid, String svcDelta, String deltaUUID) {
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
        } finally {
            WebResource.commonsClose(front_conn, prep, rs);
        }

        logger.end(method);
        return instanceID;
    }

    private static void cacheSystemDelta(int instanceID, String result) {
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
        } finally {
            WebResource.commonsClose(front_conn, prep, rs);
        }
    }

    private static String networkAddressFromJson(JSONObject jsonAddr) {
        if (!jsonAddr.containsKey("value")) {
            return "";
        }
        String type = "ipv4-address";
        if (jsonAddr.containsKey("type")) {
            type = jsonAddr.get("type").toString();
        }
        return String.format("[a    mrs:NetworkAddress; mrs:type    \"%s\"; mrs:value   \"%s\"]", type, jsonAddr.get("value").toString());
    }

    static String initInstance(String refUuid, String svcDelta, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s", host, refUuid));
        HttpURLConnection compile = (HttpURLConnection) url.openConnection();
        String result = WebResource.executeHttpMethod(url, compile, "POST", svcDelta, auth);
        if (!result.contains("referenceVersion")) {
            throw new EJBException("Service Delta Failed!");
        }
        return result;
    }

    static String propagateInstance(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = WebResource.executeHttpMethod(url, propagate, "PUT", null, auth);
        if (!result.equals("PROPAGATED")) {
            throw new EJBException("Propagate Failed!");
        }
        return result;
    }

    static String commitInstance(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit", host, refUuid));
        HttpURLConnection commit = (HttpURLConnection) url.openConnection();
        String result = WebResource.executeHttpMethod(url, commit, "PUT", null, auth);
        if (!result.equals("COMMITTING")) {
            throw new EJBException("Commit Failed!");
        }
        return result;
    }

    // -------------------------- SERVICE FUNCTIONS --------------------------------    
    static int createOperationModelModification(Map<String, String> paraMap, TokenHandler token) {
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
            URL url = new URL(String.format("%s/service/%s", host, refUuid));
            HttpURLConnection compile = (HttpURLConnection) url.openConnection();
            result = WebResource.executeHttpMethod(url, compile, "POST", delta, token.auth());
            if (!result.contains("referenceVersion")) {
                return 2;//Error occurs when interacting with back-end system
            }

            // Cache System Delta
            cacheSystemDelta(instanceID, result);

            url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
            HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
            result = WebResource.executeHttpMethod(url, propagate, "PUT", null, token.auth());
            if (!result.equals("PROPAGATED")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/commit", host, refUuid));
            HttpURLConnection commit = (HttpURLConnection) url.openConnection();
            result = WebResource.executeHttpMethod(url, commit, "PUT", null, token.auth());
            if (!result.equals("COMMITTED")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/status", host, refUuid));
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
            return 1;//connection error
        }
    }
}
