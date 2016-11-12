/* 
 * Copyright (c) 2013-2016 University of Maryland
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
package web.servlets;

import web.beans.serviceBeans;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DriverServlet extends HttpServlet {

    /**
     * Collects parameters from Driver forms and collates into HashMap, before
     * passing the new map into the serviceBean for model modification.
     * <br/>
     * Upon completion, servlet redirects to service page with error code.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            HashMap<String, String> paramMap = new HashMap<>();
            Enumeration paramNames = request.getParameterNames();
            serviceBeans servBean = new serviceBeans();
            String host = "http://localhost:8080/StackV-web/restapi";

            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String refUuid = servBean.executeHttpMethod(url, connection, "GET", null);

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", "root");
            front_connectionProps.put("password", "root");

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT service_id"
                    + " FROM service WHERE filename = ?");
            prep.setString(1, "driver");
            ResultSet rs1 = prep.executeQuery();
            rs1.next();
            int serviceID = rs1.getInt(1);

            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            prep = front_conn.prepareStatement("INSERT INTO frontend.service_instance "
                    + "(`service_id`, `user_id`, `creation_time`, `referenceUUID`) VALUES (?, ?, ?, ?)");
            prep.setInt(1, serviceID);
            prep.setString(2, request.getParameter("userID"));
            prep.setTimestamp(3, timeStamp);
            prep.setString(4, refUuid);
            prep.executeUpdate();

            // Collate named elements
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);
                String paramValue = paramValues[0];
                if (paramValue.length() != 0) {
                    paramMap.put(paramName, paramValue);
                }
            }

            // Connect dynamically generated elements
            for (int i = 1; i <= 5; i++) {
                if (paramMap.containsKey("apropname" + i)) {
                    paramMap.put(paramMap.get("apropname" + i), paramMap.get("apropval" + i));

                    paramMap.remove("apropname" + i);
                    paramMap.remove("apropval" + i);
                }
            }

            paramMap.remove("driver_id");
            paramMap.remove("form_install");

            for (String key : paramMap.keySet()) {
                if (!paramMap.get(key).isEmpty()) {
                    url = new URL(String.format("%s/service/property/%s/%s/", host, refUuid, key));
                    connection = (HttpURLConnection) url.openConnection();
                    servBean.executeHttpMethod(url, connection, "POST", paramMap.get(key));
                }
            }

            int retCode = -1;
            // Call appropriate driver control method
            if (paramMap.containsKey("install")) {
                paramMap.remove("install");
                retCode = servBean.driverInstall(paramMap);
            } else if (paramMap.containsKey("uninstall")) {
                retCode = servBean.driverUninstall(request.getParameter("topologyUri"));
            }

            response.sendRedirect("/StackV-web/ops/srvc/driver.jsp?ret=" + retCode);
        } catch (SQLException ex) {
            Logger.getLogger(DriverServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Collects parameters from Driver forms and collates into HashMap, "
                + "before passing the new map into the serviceBean for model modification. "
                + "Upon completion, servlet redirects to service page with error code.";
    }

}
