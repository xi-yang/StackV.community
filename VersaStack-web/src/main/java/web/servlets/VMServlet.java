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

public class VMServlet extends HttpServlet {

    /**
     * Collects parameters from VM Installation form and collates into HashMap,
     * before passing the new map into the serviceBean for model modification.
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
        serviceBeans servBean = new serviceBeans();
        int retCode = -1;
        String host = "http://localhost:8080/VersaStack-web/restapi";

        // Bi-directional search function
        if (request.getParameter("search") != null) {

        } // VM installation
        else if (request.getParameter("install") != null) {
            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String refUuid = servBean.executeHttpMethod(url, connection, "GET", null);
            
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", "root");
            front_connectionProps.put("password", "root");
            try {
                front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                        front_connectionProps);

                PreparedStatement prep = front_conn.prepareStatement("SELECT service_id"
                        + " FROM service WHERE filename = ?");
                prep.setString(1, "vmadd");
                ResultSet rs1 = prep.executeQuery();
                rs1.next();
                int serviceID = rs1.getInt(1);

                Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
                
                prep = front_conn.prepareStatement("INSERT INTO Frontend.service_instance "
                        + "(`service_id`, `user_id`, `creation_time`, `referenceUUID`) VALUES (?, ?, ?, ?)");
                prep.setInt(1, serviceID);
                prep.setString(2, request.getParameter("userID"));
                prep.setTimestamp(3, timeStamp);
                prep.setString(4, refUuid);
                prep.executeUpdate();

                if (request.getParameter("vmType").equals("aws")) {
                    HashMap<String, String> paramMap = new HashMap<>();
                    Enumeration paramNames = request.getParameterNames();

                    // Collate named elements
                    while (paramNames.hasMoreElements()) {
                        String paramName = (String) paramNames.nextElement();
                        String[] paramValues = request.getParameterValues(paramName);
                        if (paramValues.length == 1) {
                            String paramValue = paramValues[0];
                            paramMap.put(paramName, paramValue);
                        } else if (paramValues.length > 1) {
                            String fullValue = "";
                            for (String paramValue : paramValues) {
                                fullValue += paramValue + "\r\n";
                            }
                            fullValue = fullValue.substring(0, fullValue.length() - 4);
                            paramMap.put(paramName, fullValue);
                        }
                    }

                    if (!paramMap.get("graphTopo").equalsIgnoreCase("none")) {
                        paramMap.put("topologyUri", paramMap.get("graphTopo"));
                    }

                    // Format volumes
                    String volString = "";

                    // Include root
                    volString += paramMap.get("root-size") + ",";
                    volString += paramMap.get("root-type") + ",";
                    volString += paramMap.get("root-path") + ",";
                    volString += paramMap.get("root-snapshot") + "\r\n";
                    paramMap.remove("root-size");
                    paramMap.remove("root-type");
                    paramMap.remove("root-path");
                    paramMap.remove("root-snapshot");

                    for (int i = 1; i <= 10; i++) {
                        if (paramMap.containsKey(i + "-path")) {
                            volString += paramMap.get(i + "-size") + ",";
                            volString += paramMap.get(i + "-type") + ",";
                            volString += paramMap.get(i + "-path") + ",";
                            volString += paramMap.get(i + "-snapshot") + "\r\n";
                            paramMap.remove(i + "-size");
                            paramMap.remove(i + "-type");
                            paramMap.remove(i + "-path");
                            paramMap.remove(i + "-snapshot");
                        }
                    }
                    paramMap.put("volumes", volString);

                    paramMap.remove("install");

                    for (String key : paramMap.keySet()) {
                        if (!paramMap.get(key).isEmpty()) {
                            url = new URL(String.format("%s/service/property/%s/%s/%s", host, refUuid, key, paramMap.get(key)));
                            connection = (HttpURLConnection) url.openConnection();
                            servBean.executeHttpMethod(url, connection, "PUT", null);
                        }
                    }

                    for (int i = 0; i < Integer.parseInt(paramMap.get("vmQuantity")); i++) {
                        retCode = servBean.vmInstall(paramMap);
                    }
                }

            } catch (SQLException ex) {
                Logger.getLogger(VMServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        response.sendRedirect("/VersaStack-web/ops/srvc/vmadd.jsp?ret=" + retCode);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Collects parameters from VM Installation form and collates into HashMap, "
                + "before passing the new map into the serviceBean for model modification. "
                + "Upon completion, servlet redirects to service page with error code.";
    }

}
