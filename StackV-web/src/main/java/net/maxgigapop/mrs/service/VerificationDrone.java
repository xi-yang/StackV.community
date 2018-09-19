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
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.common.TokenHandler;
import net.maxgigapop.mrs.rest.api.JNDIFactory;
import net.maxgigapop.mrs.rest.api.WebResource;
import static net.maxgigapop.mrs.rest.api.WebResource.executeHttpMethod;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author rikenavadur
 */
public class VerificationDrone implements Runnable {

    private final static String HOST = "http://127.0.0.1:8080/StackV-web/restapi";
    private final StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "VerificationDrone");
    private final TokenHandler token;
    Connection conn;
    PreparedStatement prep;
    ResultSet rs;
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    String state, instanceUUID, instanceSubstate, pending, lastResult;
    int currentRun;
    Instant start;
    Timestamp timestamp;

    public VerificationDrone(String _instanceUUID, TokenHandler _token, Connection _conn) {
        instanceUUID = _instanceUUID;
        token = _token;
        conn = _conn;
        start = Instant.now();
    }

    public String getResult() {
        return lastResult;
    }

    @Override
    public void run() {
        // Double check for INIT state.
        logger.refuuid(instanceUUID);
        initData();
        executor.scheduleAtFixedRate(new DroneCheckIn(instanceUUID, start), 3, 1, TimeUnit.SECONDS);
        switch (state) {
            case "FINISHED":
                resetVerification();
                verify();
                break;
            case "INIT":
                verify();
                break;
            case "RUNNING":
                logger.status("run", "Verification attempted to start, but already started");
        }

        WebResource.commonsClose(conn, prep, rs, logger);
        executor.shutdown();
    }

    private void verify() {
        String method = "verify";
        logger.start(method, "Verification drone starting");
        state = "RUNNING";
        currentRun++;
        int IOErrors = 0;
        try {
            while (currentRun <= 50) {
                try {
                    // Step 1: Update data
                    updateData();
                    if (state.equals("FINISHED")) {
                        logger.status(method, "Drone terminated prematurely.");
                        return;
                    }

                    logger.trace(method, "Run " + currentRun + "/50 | Instance in " + instanceSubstate);

                    // Step 2: Update state
                    boolean redVerified = true, addVerified = true;

                    URL url = new URL(String.format("%s/service/verify/%s", HOST, instanceUUID));
                    HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                    String result = WebResource.executeHttpMethod(url, urlConn, "GET", null, token.auth());
                    lastResult = result;

                    // Pull data from JSON.
                    JSONParser parser = new JSONParser();
                    Object obj = parser.parse(result);
                    JSONObject verifyJSON = (JSONObject) obj;

                    if (verifyJSON.containsKey("reductionVerified") && (verifyJSON.get("reductionVerified") != null)
                            && ((String) verifyJSON.get("reductionVerified")).equals("false")) {
                        redVerified = false;
                    }
                    if (verifyJSON.containsKey("additionVerified") && (verifyJSON.get("additionVerified") != null)
                            && ((String) verifyJSON.get("additionVerified")).equals("false")) {
                        addVerified = false;
                    }

                    // Step 3: Update verification data
                    prep = conn.prepareStatement("UPDATE `service_verification` SET `verification_state` = '0', `state`='RUNNING',`delta_uuid`=?,`creation_time`=?,`verified_reduction`=?,`verified_addition`=?,"
                            + "`unverified_reduction`=?,`unverified_addition`=?,`reduction`=?,`addition`=?, `verification_run`=?, `timestamp`=?"
                            + "WHERE `instanceUUID`= ? ");
                    prep.setString(1, (String) verifyJSON.get("referenceUUID"));
                    prep.setString(2, (String) verifyJSON.get("creationTime"));
                    prep.setString(3, (String) verifyJSON.get("verifiedModelReduction"));
                    prep.setString(4, (String) verifyJSON.get("verifiedModelAddition"));
                    prep.setString(5, (String) verifyJSON.get("unverifiedModelReduction"));
                    prep.setString(6, (String) verifyJSON.get("unverifiedModelAddition"));
                    prep.setString(7, (String) verifyJSON.get("reductionVerified"));
                    prep.setString(8, (String) verifyJSON.get("additionVerified"));
                    prep.setInt(9, currentRun);
                    prep.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
                    prep.setString(11, instanceUUID);
                    prep.executeUpdate();

                    // Step 4: Check for success
                    if (redVerified && addVerified) {
                        state = "FINISHED";
                        prep = conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '1', `state` = 'FINISHED', `timestamp` = ?  "
                                + "WHERE `instanceUUID` = ? ");
                        prep.setTimestamp(1, null);
                        prep.setString(2, instanceUUID);
                        prep.executeUpdate();

                        logger.end(method, "Verification success");
                        return;
                    }
                } catch (IOException ex) {
                    prep = conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `state` = ?, `timestamp` = ?"
                            + "WHERE `instanceUUID` = ? ");
                    prep.setString(1, state);
                    prep.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    prep.setString(3, instanceUUID);
                    prep.executeUpdate();
                    logger.error(method, "Run " + currentRun + "/50 | Verification received IOException from backend.");

                    if (++IOErrors >= 5) {
                        state = "FINISHED";
                        prep = conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_run` = '0', `verification_state` = '-1', `state` = 'FINISHED', `timestamp` = ? "
                                + "WHERE `instanceUUID` = ? ");
                        prep.setTimestamp(1, null);
                        prep.setString(2, instanceUUID);
                        prep.executeUpdate();

                        logger.end(method, "Verification failed due to IOException.");
                        return;
                    }
                }

                // Step 5: Delay until next run
                if (currentRun <= 30) {
                    Thread.sleep(10000);
                } else {
                    Thread.sleep(30000);
                }
                currentRun++;
            }
            // Step 4.1 If hit max runs without success, verification has failed
            state = "FINISHED";
            prep = conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_run` = '0', `verification_state` = '-1', `state` = 'FINISHED', `timestamp` = ? "
                    + "WHERE `instanceUUID` = ? ");
            prep.setTimestamp(1, null);
            prep.setString(2, instanceUUID);
            prep.executeUpdate();

            logger.end(method, "Verification failed");
        } catch (ParseException | SQLException | InterruptedException ex) {
            logger.catching(method, ex);
        }
    }

    // UTILITY
    private void initData() {
        try {
            prep = conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `elapsed_time` = ? "
                    + "WHERE `instanceUUID` = ? ");
            prep.setString(1, "00:00:00");
            prep.setString(2, instanceUUID);
            prep.executeUpdate();

            prep = conn.prepareStatement("SELECT state, verification_run, timestamp FROM service_verification "
                    + "WHERE instanceUUID = ?");
            prep.setString(1, instanceUUID);
            rs = prep.executeQuery();
            if (rs.next()) {
                state = rs.getString("state");
                currentRun = rs.getInt("verification_run");
                timestamp = rs.getTimestamp("timestamp");
                logger.trace("initData", "Drone initialized");
            }
        } catch (SQLException ex) {
            logger.catching("initData", ex);
        }
    }

    private void updateData() {
        try {
            URL url = new URL(String.format("%s/service/%s/status", HOST, instanceUUID));
            HttpURLConnection status = (HttpURLConnection) url.openConnection();
            instanceSubstate = executeHttpMethod(url, status, "GET", null, token.auth());
        } catch (IOException ex) {
            logger.catching("updateData", ex);
        }
    }

    private void resetVerification() {
        try {
            int instanceID = -1;
            prep = conn.prepareStatement("SELECT service_instance_id FROM service_verification "
                    + "WHERE instanceUUID = ?");
            prep.setString(1, instanceUUID);
            rs = prep.executeQuery();
            if (rs.next()) {
                instanceID = rs.getInt("service_instance_id");
            }

            prep = conn.prepareStatement("DELETE FROM service_verification WHERE instanceUUID = ?");
            prep.setString(1, instanceUUID);
            prep.executeUpdate();

            prep = conn.prepareStatement("INSERT INTO `frontend`.`service_verification` "
                    + "(`service_instance_id`, `instanceUUID`, `state`) VALUES (?,?,'INIT')");
            prep.setInt(1, instanceID);
            prep.setString(2, instanceUUID);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("resetVerification", ex);
        }
        currentRun = 0;
    }

    private class DroneCheckIn implements Runnable {

        String uuid;
        Instant start;
        String method = "DroneCheckIn";
        Connection conn;
        PreparedStatement prep;
        private final JNDIFactory factory = new JNDIFactory();

        public DroneCheckIn(String uuid, Instant start) {
            this.uuid = uuid;
            this.start = start;
        }

        @Override
        public void run() {
            conn = factory.getConnection("frontend");
            try {
                prep = conn.prepareStatement("SELECT pending_action FROM service_verification "
                        + "WHERE instanceUUID = ?");
                prep.setString(1, instanceUUID);
                rs = prep.executeQuery();
                if (rs.next()) {
                    pending = rs.getString("pending_action");
                }

                if (!pending.isEmpty()) {
                    switch (pending) {
                        case "PAUSE":
                            logger.trace(method, "Pause signal received. Drone ending operation");
                            prep = conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `pending_action` = '', `state` = 'PAUSED' "
                                    + "WHERE `instanceUUID` = ? ");
                            prep.setString(1, uuid);
                            prep.executeUpdate();
                            break;
                        case "STOP":
                            logger.status(method, "Stop signal received. Drone ending operation");
                            state = "FINISHED";
                            prep = conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `pending_action` = '', `verification_run` = '0', `verification_state` = '-1', `state` = 'FINISHED', `timestamp` = NULL, `elapsed_time` = NULL "
                                    + "WHERE `instanceUUID` = ?");

                            prep.setString(1, uuid);
                            prep.executeUpdate();
                            break;
                    }                    
                } else if (state.equals("RUNNING")) {
                    Duration duration = Duration.between(start, Instant.now());
                    long absSeconds = Math.abs(duration.getSeconds());
                    String durationStr = String.format("%d:%02d:%02d",
                            absSeconds / 3600,
                            (absSeconds % 3600) / 60,
                            absSeconds % 60);

                    prep = conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `timestamp` = ?, `elapsed_time` = ? "
                            + "WHERE `instanceUUID` = ? ");
                    prep.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    prep.setString(2, durationStr);
                    prep.setString(3, uuid);
                    prep.executeUpdate();
                }
            } catch (SQLException ex) {
                logger.catching(method, ex);
            } finally {
                WebResource.commonsClose(conn, prep, rs, logger);
            }
        }
    }
}
