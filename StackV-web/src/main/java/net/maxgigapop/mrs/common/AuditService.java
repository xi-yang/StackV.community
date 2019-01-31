/*
 * Copyright (c) 2013-2018 University of Maryland
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
package net.maxgigapop.mrs.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.maxgigapop.mrs.rest.api.JNDIFactory;
import net.maxgigapop.mrs.rest.api.WebResource;

public class AuditService {

    private static final StackLogger logger = new StackLogger(WebResource.class.getName(), "AuditService");
    private static final JNDIFactory factory = new JNDIFactory();

    // Synchronize database instances.
    public static void cleanInstances(String mode) throws SQLException {
        String method = "cleanInstances";
        logger.trace_start(method);
        try (Connection frontConn = factory.getConnection("frontend");
                Connection backConn = factory.getConnection("rainsdb");
                PreparedStatement prep = frontConn
                        .prepareStatement("SELECT DISTINCT referenceUUID FROM service_instance");
                PreparedStatement prep2 = backConn
                        .prepareStatement("SELECT DISTINCT referenceUUID FROM service_instance");
                ResultSet rs = prep.executeQuery();
                ResultSet rs2 = prep2.executeQuery();) {
            ArrayList<String> frontList = new ArrayList<>();
            ArrayList<String> backList = new ArrayList<>();

            while (rs.next()) {
                frontList.add(rs.getString(1));
            }

            while (rs.next()) {
                backList.add(rs.getString(1));
            }

            if (mode.equals("frontend")) {
                // Sync frontend with backend.
                frontList.removeAll(backList);

                for (String uuid : frontList) {
                    PreparedStatement temp = frontConn
                            .prepareStatement("DELETE FROM service_instance WHERE referenceUUID = ?");
                    temp.setString(1, uuid);
                    temp.executeUpdate();
                    logger.status(method, "Service instance " + uuid + " removed from frontend database.");
                    temp.close();
                }
            } else if (mode.equals("backend")) {
                // Sync frontend with backend.
                backList.removeAll(frontList);

                for (String uuid : backList) {
                    PreparedStatement temp = backConn
                            .prepareStatement("DELETE FROM service_instance WHERE referenceUUID = ?");
                    temp.setString(1, uuid);
                    temp.executeUpdate();
                    logger.status(method, "Service instance " + uuid + " removed from backend database.");
                    temp.close();
                }
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
        }
    }
}
