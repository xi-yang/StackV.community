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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.common.TokenHandler;

/**
 *
 * @author rikenavadur
 */
public class VerificationHandler {

    private final StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "VerificationHandler");
    private final TokenHandler token;
    Connection conn;
    PreparedStatement prep;
    ResultSet rs;

    String state;
    String instanceUUID;

    public VerificationHandler(String _instanceUUID, TokenHandler _token) {
        String method = "init";
        token = _token;
        instanceUUID = _instanceUUID;
        logger.refuuid(instanceUUID);
        try {
            String front_db_user = "front_view";
            String front_db_pass = "frontuser";
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            logger.trace_end(method);
        } catch (SQLException ex) {
            logger.catching(method, ex);
        }
    }

    public void startVerification() {
        new Thread(new VerificationDrone(instanceUUID, token, conn, 30, 10)).start();
    }

    public void pauseVerification() {
        sendAction("PAUSE");
    }

    public void stopVerification() {
        sendAction("STOP");
    }

    public void clearVerification() {
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
            logger.catching("clearVerification", ex);
        }
    }

    // UTILITY
    private void sendAction(String action) {
        String method = "sendAction";
        updateState();
        try {
            if (state.equals("RUNNING")) {
                prep = conn.prepareStatement("UPDATE service_verification SET pending_action = ? "
                        + "WHERE instanceUUID = ?");
                prep.setString(1, action);
                prep.setString(2, instanceUUID);
                prep.executeUpdate();

                logger.trace_end(method, action + " call sent");
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
        }
    }

    private void updateState() {
        String method = "updateState";
        try {
            prep = conn.prepareStatement("SELECT state FROM service_verification "
                    + "WHERE instanceUUID = ?");
            prep.setString(1, instanceUUID);
            rs = prep.executeQuery();
            if (rs.next()) {
                state = rs.getString(state);
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
        }
    }
}
