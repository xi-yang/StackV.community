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

import static java.lang.Thread.sleep;
import static net.maxgigapop.mrs.rest.api.WebResource.commonsClose;
import static net.maxgigapop.mrs.rest.api.WebResource.executeHttpMethod;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ejb.EJBException;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.json.simple.JSONObject;

import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.common.TokenHandler;
import net.maxgigapop.mrs.rest.api.JNDIFactory;
import net.maxgigapop.mrs.rest.api.WebResource;

/**
 *
 * @author rikenavadur
 */
public class ServiceEngine {

    private final static StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "ServiceEngine");
    private final static String HOST = "http://127.0.0.1:8080/StackV-web/restapi";
    private final static JNDIFactory factory = new JNDIFactory();
    private static final OkHttpClient client = new OkHttpClient();

    // OPERATION FUNCTIONS
    public static void orchestrateInstance(String refUUID, JSONObject inputJSON, String deltaUUID, TokenHandler token,
            boolean autoProceed) throws EJBException, IOException, InterruptedException, SQLException {
        String method = "orchestrateInstance";
        String result;
        String lastState = "INIT";
        String svcDelta = (String) inputJSON.get("data");
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
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

            // Check for license deduction.
            if (inputJSON.containsKey("profileID")) {
                front_conn = factory.getConnection("frontend");
                prep = front_conn.prepareStatement(
                        "SELECT L.remaining, L.type FROM service_wizard W, service_wizard_licenses L WHERE W.service_wizard_id = ? AND W.service_wizard_id = L.service_wizard_id AND L.username = ?");
                prep.setString(1, (String) inputJSON.get("profileID"));
                prep.setString(2, (String) inputJSON.get("username"));
                rs = prep.executeQuery();
                while (rs.next()) {
                    if (rs.getString("type").equals("ticket")) {
                        int remaining = rs.getInt("remaining");
                        if (remaining > 0) {
                            prep = front_conn.prepareStatement(
                                    "UPDATE service_wizard_licenses SET remaining = ? WHERE username = ? AND service_wizard_id = ?");
                            prep.setInt(1, --remaining);
                            prep.setString(2, (String) inputJSON.get("username"));
                            prep.setString(3, (String) inputJSON.get("profileID"));
                            prep.executeUpdate();
                            logger.trace(method, "License deducted, now at " + remaining + " uses remaining.");
                        }

                        if (remaining <= 0) {
                            prep = front_conn.prepareStatement(
                                    "DELETE FROM service_wizard_licenses WHERE username = ? AND service_wizard_id = ?");
                            prep.setString(1, (String) inputJSON.get("username"));
                            prep.setString(2, (String) inputJSON.get("profileID"));
                            prep.executeUpdate();
                            logger.trace(method, "License fully used.");
                        }
                    }
                }
            }

            // REGISTER
            URL regURL = new URL(String.format("%s/md2/register/services/%s/", HOST, refUUID));
            Request request = new Request.Builder().url(regURL).post(null).header("Authorization", token.auth())
                    .build();
            client.newCall(request).execute();

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
                    sleep(5000);// wait for 5 seconds and check again later
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
            Connection front_conn2 = null;
            PreparedStatement prep2 = null;
            try {
                front_conn2 = factory.getConnection("frontend");

                prep2 = front_conn2
                        .prepareStatement("UPDATE service_instance SET last_state = ? WHERE referenceUUID = ?");
                prep2.setString(1, lastState);
                prep2.setString(2, refUUID);
                prep2.executeUpdate();

                logger.trace_end("updateLastState");
            } catch (SQLException ex) {
                logger.catching(method, ex);
            } finally {
                logger.trace(method, "Connection closing!");
                commonsClose(front_conn, prep, rs, logger);
                commonsClose(front_conn2, prep2, null, logger);
            }
        }
    }

    // UTILITY FUNCTIONS
    private static int cacheServiceDelta(String refUuid, String svcDelta, String deltaUUID) throws SQLException {
        String method = "cacheServiceDelta";
        logger.trace_start(method);
        // Cache serviceDelta.
        int instanceID = -1;
        try (Connection front_conn = factory.getConnection("frontend");) {
            PreparedStatement prep = front_conn
                    .prepareStatement("SELECT service_instance_id" + " FROM service_instance WHERE referenceUUID = ?");
            prep.setString(1, refUuid);
            ResultSet rs = prep.executeQuery();
            rs.next();
            instanceID = rs.getInt(1);
            rs.close();
            prep.close();

            String formatDelta = svcDelta.replaceAll("<", "&lt;");
            formatDelta = formatDelta.replaceAll(">", "&gt;");

            prep = front_conn.prepareStatement("INSERT INTO frontend.service_delta "
                    + "(`service_instance_id`, `super_state`, `type`, `referenceUUID`, `delta`) "
                    + "VALUES (?, 'CREATE', 'Service', ?, ?)");
            prep.setInt(1, instanceID);
            prep.setString(2, deltaUUID);
            prep.setString(3, formatDelta);
            prep.executeUpdate();
            prep.close();
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        }

        logger.end(method);
        return instanceID;
    }

    private static void cacheSystemDelta(int instanceID, String result) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            String formatDelta = result.replaceAll("<", "&lt;");
            formatDelta = formatDelta.replaceAll(">", "&gt;");

            prep = front_conn.prepareStatement("INSERT INTO frontend.service_delta "
                    + "(`service_instance_id`, `super_state`, `type`, `delta`) " + "VALUES (?, 'CREATE', 'System', ?)");
            prep.setInt(1, instanceID);
            prep.setString(2, formatDelta);
            prep.executeUpdate();

        } catch (SQLException ex) {
            logger.catching("cacheSystemDelta", ex);
            throw ex;
        } finally {
            WebResource.commonsClose(front_conn, prep, rs, logger);
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

    static String revertInstance(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert", HOST, refUuid));
        HttpURLConnection revert = (HttpURLConnection) url.openConnection();
        String result = WebResource.executeHttpMethod(url, revert, "PUT", null, auth);
        if (!result.contains("-PARTIAL")) {
            throw new EJBException("Revert Failed!");
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
    static void pushProperty(String refUUID, String key, String value, String auth) throws IOException {
        URL url = new URL(String.format("%s/service/property/%s/%s", HOST, refUUID, key));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        executeHttpMethod(url, conn, "POST", value, auth);
    }

    public static String getCachedSystemDelta(String refUuid) {
        try (Connection front_conn = factory.getConnection("frontend");) {
            int instanceID;
            try (PreparedStatement prep = front_conn
                    .prepareStatement("SELECT service_instance_id FROM service_instance WHERE referenceUUID = ?");) {
                prep.setString(1, refUuid);
                try (ResultSet rs = prep.executeQuery();) {
                    rs.next();
                    instanceID = rs.getInt(1);
                }
            }
            try (PreparedStatement prep = front_conn.prepareStatement(
                    "SELECT delta FROM frontend.service_delta WHERE service_instance_id = ? AND type='System' ORDER BY service_delta_id DESC");) {
                prep.setInt(1, instanceID);
                try (ResultSet rs = prep.executeQuery();) {
                    rs.next();
                    return rs.getString(1);
                }
            }
        } catch (SQLException ex) {
            logger.catching("getCachedSystemDelta", ex);
        }
        return null;
    }
}
