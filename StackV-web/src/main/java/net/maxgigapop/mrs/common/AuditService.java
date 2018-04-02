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
import static net.maxgigapop.mrs.rest.api.WebResource.commonsClose;

public class AuditService {

    private static final StackLogger logger = new StackLogger(WebResource.class.getName(), "WebResource");
    private static final JNDIFactory factory = new JNDIFactory();

    // Synchronize database instances.
    public static void cleanInstances(String mode) {
        String method = "cleanInstances";
        logger.trace_start(method);
        Connection frontConn = null;
        Connection backConn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<String> frontList = new ArrayList<>();
            ArrayList<String> backList = new ArrayList<>();
            
            frontConn = factory.getConnection("frontend");
            prep = frontConn.prepareStatement("SELECT DISTINCT referenceUUID FROM service_instance");
            rs = prep.executeQuery();
            while (rs.next()) {
                frontList.add(rs.getString(1));
            }
            
            backConn = factory.getConnection("rainsdb");
            prep = backConn.prepareStatement("SELECT DISTINCT referenceUUID FROM service_instance");
            rs = prep.executeQuery();
            while (rs.next()) {
                backList.add(rs.getString(1));
            }
            
            if (mode.equals("frontend")) {
                // Sync frontend with backend.
                frontList.removeAll(backList);
                
                for (String uuid : frontList) {
                    prep = frontConn.prepareStatement("DELETE FROM service_instance WHERE referenceUUID = ?");
                    prep.setString(1, uuid);
                    prep.executeUpdate();
                    logger.status(method, "Service instance " + uuid + " removed from frontend database.");
                }
            }
            else if (mode.equals("backend")) {
                // Sync frontend with backend.
                backList.removeAll(frontList);
                
                for (String uuid : backList) {
                    prep = backConn.prepareStatement("DELETE FROM service_instance WHERE referenceUUID = ?");
                    prep.setString(1, uuid);
                    prep.executeUpdate();
                    logger.status(method, "Service instance " + uuid + " removed from backend database.");
                }
            }            
        } catch (SQLException ex) {
            logger.catching(method, ex);
        } finally {
            commonsClose(frontConn, prep, rs);     
            commonsClose(backConn, prep, rs);     
        }
    }
}
