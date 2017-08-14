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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Properties;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.common.TokenHandler;
import static net.maxgigapop.mrs.rest.api.WebResource.executeHttpMethod;
import static net.maxgigapop.mrs.rest.api.WebResource.SuperState;
import static net.maxgigapop.mrs.rest.api.WebResource.commonsClose;
import org.json.simple.JSONObject;

/**
 *
 * @author rikenavadur
 */
public class ServiceHandler {

    private final StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "ServiceHandler");
    private final String host = "http://127.0.0.1:8080/StackV-web/restapi";
    private final String front_db_user = "front_view";
    private final String front_db_pass = "frontuser";

    TokenHandler token;
    public String refUUID;
    public SuperState superState;
    String type;
    String owner;
    String alias;
    String lastState = "INIT";

    public ServiceHandler(JSONObject input, TokenHandler initToken) {
        token = initToken;

        createInstance(input);
    }

    public ServiceHandler(String refUUID, TokenHandler initToken) {
        this.refUUID = refUUID;
        logger.refuuid(refUUID);
        token = initToken;

        loadInstance(refUUID);
    }

    // INIT METHODS
    private void createInstance(JSONObject inputJSON) {
        String method = "createInstance";
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            logger.start(method);

            type = (String) inputJSON.get("service");
            alias = (String) inputJSON.get("alias");
            owner = (String) inputJSON.get("username");

            String delta = (String) inputJSON.get("data");
            String deltaUUID = (String) inputJSON.get("uuid");

            // Find user ID.
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();

            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                logger.catching(method, ex);
            }

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            // Instance Creation
            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            refUUID = executeHttpMethod(url, connection, "GET", null, token.auth());
            logger.refuuid(refUUID);
           
            // Initialize service parameters.
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            // Install Instance into DB.
            prep = front_conn.prepareStatement("INSERT INTO frontend.service_instance "
                    + "(`type`, `username`, `creation_time`, `referenceUUID`, `alias_name`, `super_state`, `last_state`) VALUES (?, ?, ?, ?, ?, ?, ?)");
            prep.setString(1, type);
            prep.setString(2, owner);
            prep.setTimestamp(3, timeStamp);
            prep.setString(4, refUUID);
            prep.setString(5, alias);
            prep.setString(6, "CREATE");
            prep.setString(7, lastState);
            prep.executeUpdate();

            superState = SuperState.CREATE;

            prep = front_conn.prepareStatement("SELECT service_instance_id FROM service_instance WHERE referenceUUID = ?");
            prep.setString(1, refUUID);
            rs = prep.executeQuery();
            rs.next();
            int instanceID = rs.getInt("service_instance_id");

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_verification` "
                    + "(`service_instance_id`) VALUES (?)");
            prep.setInt(1, instanceID);
            prep.executeUpdate();

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`acl` (`subject`, `is_group`, `object`) "
                    + "VALUES (?, '0', ?)");
            prep.setString(1, owner);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.init();

            // Execute service creation.
            ServiceEngine.orchestrateInstance(refUUID, delta, deltaUUID, token);
            /*switch (type) {
                case "netcreate":
                    ServiceEngine.createNetwork(paraMap, token);
                    break;
                case "hybridcloud":
                    ServiceEngine.createHybridCloud(paraMap, token);
                    break;
                case "omm":
                    ServiceEngine.createOperationModelModification(paraMap, token);
                    break;
                case "dnc":
                    ServiceEngine.createDNC(dataJSON, token, refUUID);
                    break;
                default:
            }*/

            // Return instance UUID
            logger.end(method);
        } catch (EJBException | SQLException | IOException ex) {
            logger.catching(method, ex);
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    private void loadInstance(String refUUID) {
        String method = "loadInstance";
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT * FROM service_instance WHERE referenceUUID = ?");
            prep.setString(1, refUUID);
            ResultSet rs1 = prep.executeQuery();
            if (rs1.next()) {
                owner = rs1.getString("username");
                alias = rs1.getString("alias_name");
                superState = SuperState.valueOf(rs1.getString("super_state"));
                type = rs1.getString("type");
                lastState = rs1.getString("last_state");
            }

            logger.trace_end(method);
        } catch (SQLException ex) {
            logger.catching(method, ex);
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    // OPERATION METHODS
    public void operate(String action) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "operate:" + action;

        logger.refuuid(refUUID);
        logger.start(method);
        try {
            clearVerification();
            ServiceEngine.toggleVerify(true, refUUID, token);
            switch (action) {
                case "cancel":
                    setSuperState(refUUID, SuperState.CANCEL);
                    cancelInstance(refUUID, token);
                    break;
                case "force_cancel":
                    setSuperState(refUUID, SuperState.CANCEL);
                    forceCancelInstance(refUUID, token);
                    break;

                case "reinstate":
                    setSuperState(refUUID, SuperState.REINSTATE);
                    cancelInstance(refUUID, token);
                    break;
                case "force_reinstate":
                    setSuperState(refUUID, SuperState.REINSTATE);
                    forceCancelInstance(refUUID, token);
                    break;

                case "force_retry":
                    forceRetryInstance(refUUID, token);
                    break;

                case "delete":
                case "force_delete":
                    deleteInstance(refUUID, token);
                    break;

                case "verify":
                    ServiceEngine.verify(refUUID, token);
                    break;
                case "unverify":
                    ServiceEngine.toggleVerify(false, refUUID, token);
                    break;

                default:
                    logger.warning(method, "Invalid action");
            }

            logger.end(method);
        } catch (IOException | SQLException | InterruptedException | EJBException ex) {
            try {
                Properties front_connectionProps = new Properties();
                front_connectionProps.put("user", front_db_user);
                front_connectionProps.put("password", front_db_pass);
                front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                        front_connectionProps);

                prep = front_conn.prepareStatement("UPDATE service_verification V INNER JOIN service_instance I SET V.verification_state = '-1' WHERE V.service_instance_id = I.service_instance_id AND I.referenceUUID = ?");
                prep.setString(1, refUUID);
                prep.executeUpdate();
            } catch (SQLException ex2) {
                logger.catching(method, ex2);
            }
            logger.catching(method, ex);
        } finally {
            commonsClose(front_conn, prep, rs);
            if (lastState != null) {
                updateLastState(lastState, refUUID);
            }
        }
    }

    public String status() {
        try {
            URL url = new URL(String.format("%s/service/%s/status", host, refUUID));
            HttpURLConnection status = (HttpURLConnection) url.openConnection();
            String result = executeHttpMethod(url, status, "GET", null, token.auth());

            return result;
        } catch (IOException ex) {
            logger.catching("status", ex);
            return null;
        }
    }

    // UTILITY METHODS
    /**
     * Deletes a service instance.
     *
     * @param refUuid instance UUID
     * @return error code |
     */
    private int deleteInstance(String refUuid, TokenHandler token) throws SQLException, IOException {
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("DELETE FROM `frontend`.`service_instance` WHERE `service_instance`.`referenceUUID` = ?");
        prep.setString(1, refUuid);
        prep.executeUpdate();

        prep = front_conn.prepareStatement("DELETE FROM `frontend`.`acl` WHERE `acl`.`object` = ?");
        prep.setString(1, refUuid);
        prep.executeUpdate();

        commonsClose(front_conn, prep, null);

        String result = delete(refUuid, token.auth());
        if (result.equalsIgnoreCase("Successfully terminated")) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Cancels a service instance. Requires instance to be in 'ready' substate.
     *
     * @param refUuid instance UUID
     * @return error code | -1: Exception thrown. 0: success. 1: stage 1 error
     * (Failed pre-condition). 2: stage 2 error (Failed revert). 3: stage 3
     * error (Failed propagate). 4: stage 4 error (Failed commit). 5: stage 5
     * error (Failed result check).
     */
    private int cancelInstance(String refUuid, TokenHandler token) throws EJBException, SQLException, IOException, MalformedURLException, InterruptedException {
        boolean result;
        String instanceState = status();
        if (!instanceState.equalsIgnoreCase("READY")) {
            return 1;
        }

        result = revert(refUuid, token.auth());
        if (!result) {
            return 2;
        }

        result = propagate(refUuid, token.auth());
        if (!result) {
            return 3;
        }

        result = commit(refUuid, token.auth());
        if (!result) {
            return 4;
        }

        while (true) {
            instanceState = status();
            if (instanceState.equals("COMMITTED")) {
                lastState = instanceState;
                lastState = ServiceEngine.verify(refUuid, token);
                return 0;
            } else if (!(instanceState.equals("COMMITTING"))) {
                return 5;
            }

            Thread.sleep(5000);
        }
    }

    private int forceCancelInstance(String refUuid, TokenHandler token) throws EJBException, SQLException, IOException, MalformedURLException, InterruptedException {
        forceRevert(refUuid, token.auth());
        forcePropagate(refUuid, token.auth());
        forceCommit(refUuid, token.auth());
        while (true) {
            String instanceState = status();
            logger.trace("forceCancelInstance", "Verification priming check - " + instanceState);
            if (instanceState.equals("COMMITTED")) {
                lastState = "COMMITTED";
                lastState = ServiceEngine.verify(refUuid, token);
                return 0;
            } else if (!(instanceState.equals("COMMITTING"))) {
                return 5;
            }
            Thread.sleep(5000);
        }
    }

    private int forceRetryInstance(String refUuid, TokenHandler token) throws SQLException, IOException, MalformedURLException, InterruptedException {
        forcePropagate(refUuid, token.auth());
        forceCommit(refUuid, token.auth());
        while (true) {
            logger.trace("forceRetryInstance", "Verification priming check");

            String instanceState = status();
            if (instanceState.equals("COMMITTED")) {
                ServiceEngine.verify(refUuid, token);

                return 0;
            } else if (!(instanceState.equals("COMMITTING"))) {
                return 5;
            }
            Thread.sleep(5000);
        }
    }

    // Utility Methods ---------------------------------------------------------
    private void setSuperState(String refUuid, SuperState superState) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("UPDATE service_instance SET super_state = ? "
                    + "WHERE referenceUUID = ?");
            prep.setString(1, superState.name());
            prep.setString(2, refUuid);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("setSuperState", ex);
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    private boolean propagate(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Propagate Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        lastState = result;
        return result.equalsIgnoreCase("PROPAGATED");
    }

    private boolean forcePropagate(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate_forcedretry", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Forced Propagate Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        lastState = result;
        return result.equalsIgnoreCase("PROPAGATED");
    }

    private boolean commit(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Commit Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        lastState = result;
        return result.equalsIgnoreCase("COMMITTING");
    }

    private boolean forceCommit(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit_forced", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Forced Commit Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        lastState = result;
        return result.equalsIgnoreCase("COMMITTING");
    }

    private boolean revert(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Revert Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        lastState = result;
        return true;
    }

    private boolean forceRevert(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert_forced", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Forced Revert Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        lastState = result;
        return true;
    }

    private String delete(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = executeHttpMethod(url, propagate, "DELETE", null, auth);
        //logger.log(Level.INFO, "Sending Delete Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        lastState = result;
        return result;
    }

    private boolean clearVerification() {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("UPDATE service_verification V "
                    + "INNER JOIN service_instance I "
                    + "ON V.service_instance_id = I.service_instance_id AND I.referenceUUID = ? "
                    + "SET V.verification_state = ?, V.verification_run = 0");
            prep.setString(1, refUUID);
            prep.setNull(2, java.sql.Types.INTEGER);
            prep.executeUpdate();

            return true;
        } catch (SQLException ex) {
            logger.catching("clearVerification", ex);
            return false;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    void updateLastState(String lastState, String refUUID) {
        String method = "updateLastState";
        logger.trace_start(method);

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
        } catch (SQLException ex) {
            logger.catching("cacheSystemDelta", ex);
        } finally {
            commonsClose(front_conn, prep, rs);
        }

        logger.trace_end(method);
    }
}
