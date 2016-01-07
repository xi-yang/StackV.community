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
import java.io.*;
import javax.servlet.annotation.WebServlet;

@WebServlet(asyncSupported = true, value = "/ServiceServlet")
public class ServiceServlet extends HttpServlet {

    serviceBeans servBean = new serviceBeans();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            HashMap<String, String> jobs = servBean.getJobStatuses();
            request.setAttribute("jobs", jobs);

            response.sendError(200);
        } catch (SQLException ex) {
            Logger.getLogger(ServiceServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // Instance Creation
            String serviceString = "";
            HashMap<String, String> paramMap = new HashMap<>();
            Enumeration paramNames = request.getParameterNames();
            String host = "http://localhost:8080/VersaStack-web/restapi";

            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String refUuid = servBean.executeHttpMethod(url, connection, "GET", null);
            paramMap.put("instanceUUID", refUuid);

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", "root");
            front_connectionProps.put("password", "root");

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                    front_connectionProps);

            // Select the correct service.
            if (request.getParameter("driverID") != null) { // Driver
                serviceString = "driver";
            } else if (request.getParameter("driverType") != null) { // VM
                serviceString = "vmadd";
            } else if (request.getParameter("netCreate") != null) { // Network Creation
                serviceString = "netcreate";
            } else {
                response.sendRedirect("/VersaStack-web/errorPage.jsp");
            }

            PreparedStatement prep = front_conn.prepareStatement("SELECT service_id"
                    + " FROM service WHERE filename = ?");
            prep.setString(1, serviceString);
            ResultSet rs1 = prep.executeQuery();
            rs1.next();
            int serviceID = rs1.getInt(1);
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            // Install Instance into DB.
            prep = front_conn.prepareStatement("INSERT INTO Frontend.service_instance "
                    + "(`service_id`, `user_id`, `creation_time`, `referenceUUID`) VALUES (?, ?, ?, ?)");
            prep.setInt(1, serviceID);
            prep.setString(2, request.getParameter("userID"));
            prep.setTimestamp(3, timeStamp);
            prep.setString(4, refUuid);
            prep.executeUpdate();

            // Create paramMap.
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

            // Replicate properties into DB.
            for (String key : paramMap.keySet()) {
                if (!paramMap.get(key).isEmpty()) {
                    url = new URL(String.format("%s/service/property/%s/%s/", host, refUuid, key));
                    connection = (HttpURLConnection) url.openConnection();
                    servBean.executeHttpMethod(url, connection, "POST", paramMap.get(key));
                }
            }

            // Execute service Creation.
            if (serviceString.equals("driver")) { // Driver
                response.sendRedirect(createDriverInstance(paramMap));
            } else if (serviceString.equals("vmadd")) { // VM
                response.sendRedirect(createVMInstance(paramMap));
            } else if (serviceString.equals("netcreate")) { // Network Creation
                response.sendRedirect(createFullNetwork(paramMap));
            } else {
                response.sendRedirect("/VersaStack-web/errorPage.jsp");
            }
        } catch (SQLException ex) {
            Logger.getLogger(ServiceServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "";
    }

    private String createDriverInstance(HashMap<String, String> paramMap) {     
        // Handles templates, in this order:
        // OpenStack, Stack Driver, Stub Driver, Generic Driver, AWS Driver 
        if (paramMap.containsKey("template1")) { 
            paramMap.put("driverID", "openStackDriver");
            paramMap.put("url", "http://max-vlsr2.dragon.maxgigapop.net:35357/v2.0");
            paramMap.put("NATServer", "");         
            paramMap.put("driverEjbPath", "java:module/OpenStackDriver");
            paramMap.put("username", "admin");
            paramMap.put("password", "1234");
            paramMap.put("topologyUri", "urn:ogf:network:openstack.com:openstack-cloud");
            paramMap.put("tenant", "admin");
            paramMap.put("install", "Install");
        } else if (paramMap.containsKey("template3")) {
            paramMap.put("driverID", "stubdriver");
            paramMap.put("topologyUri", "urn:ogf:network:rains.maxgigapop.net:wan:2015:topology");
            paramMap.put("driverEjbPath", "java:module/StubSystemDriver");     
            
            
            // Reads large stubModelTtl property from file.
            String stubModelTTL = "", nextLine;
            String testingPath = "/Users/max/NetBeansProjects/FrontVis/VersaStack/VersaStack-web/" + 
                                 "src/main/webapp/tools/testing/";
            String ttlFilename = "stub_driver_stubModelTtl";
            try {
                FileReader fr = new FileReader(testingPath + ttlFilename);
                BufferedReader br = new BufferedReader(fr);
                while ((nextLine = br.readLine()) != null) {
                    stubModelTTL += nextLine;
                }
                br.close();
            } catch (FileNotFoundException ex) {
                System.out.println(ttlFilename + " not found.");
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.println("Error reading " + ttlFilename + ".");
                ex.printStackTrace();
            }
            
            paramMap.put("stubModelTtl", stubModelTTL);
            paramMap.put("install", "Install");
        } else if (paramMap.containsKey("template4")) {
            paramMap.put("driverID", "versaNSDriver");
            paramMap.put("topologyUri", "urn:ogf:network:sdn.maxgigapop.net:network");
            paramMap.put("driverEjbPath", "java:module/GenericRESTDriver");     
            paramMap.put("subsystemBaseUrl", "http://charon.dragon.maxgigapop.net:8080/VersaNS-0.0.1-SNAPSHOT");    
            paramMap.put("install", "Install");
        }
        
        // Connect dynamically generated elements
        for (int i = 1; i <= 5; i++) {
            if (paramMap.containsKey("apropname" + i)) {
                paramMap.put(paramMap.get("apropname" + i), paramMap.get("apropval" + i));

                paramMap.remove("apropname" + i);
                paramMap.remove("apropval" + i);
            }
        }

        int retCode = -1;
        // Call appropriate driver control method
        if (paramMap.containsKey("install")) {
            paramMap.remove("install");
            retCode = servBean.driverInstall(paramMap);
        } else if (paramMap.containsKey("uninstall")) {
            retCode = servBean.driverUninstall(paramMap.get("topologyUri"));
        }

        return ("/VersaStack-web/ops/srvc/driver.jsp?ret=" + retCode);
    }

    private String createVMInstance(HashMap<String, String> paramMap) {
        int retCode = -1;
        
        // Handle templates 
        // AWS
        if (paramMap.containsKey("template1")) {
            paramMap.put("topologyUri", "urn:ogf:network:aws.amazon.com:aws-cloud");
            paramMap.put("vmType", "aws");
            paramMap.put("region", "us-east-1");
            paramMap.put("ostype", "windows");
            paramMap.put("vmQuantity", "1");
            paramMap.put("instanceType", "instance1");
            paramMap.put("vpcID", "urn:ogf:network:aws.amazon.com:aws-cloud:vpc-45143020");
            paramMap.put("subnets", "urn:ogf:network:aws.amazon.com:aws-cloud:subnet-a8a632f1,10.0.1.0");
            paramMap.put("volumes", "8,standard,/dev/xvda,snapshot\\r\\n");    
            paramMap.put("driverType", "aws");
            paramMap.put("graphTopo", "none");
        } 
        
        if (paramMap.get("driverType").equals("aws")) {
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

            for (int i = 0; i < Integer.parseInt(paramMap.get("vmQuantity")); i++) {
                retCode = servBean.vmInstall(paramMap);
            }
        }
        return ("/VersaStack-web/ops/srvc/vmadd.jsp?ret=" + retCode);
    }


    private String createFullNetwork(HashMap<String, String> paramMap) throws SQLException {
        int retCode;
        
        for ( Object key : paramMap.keySet().toArray()) {
            if (paramMap.get((String) key).isEmpty()) {
                paramMap.remove((String) key);
            }
        }

        Connection rains_conn;
        Properties rains_connectionProps = new Properties();
        rains_connectionProps.put("user", "root");
        rains_connectionProps.put("password", "root");

        rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb",
                rains_connectionProps);

        if (paramMap.containsKey("template1")) { // Basic Template
            // Add template data.
            paramMap.put("driverType", "aws");
            paramMap.put("topoUri", "urn:ogf:network:aws.amazon.com:aws-cloud");
            paramMap.put("netType", "internal");
            paramMap.put("netCidr", "10.1.0.0/16");
            paramMap.put("subnet1", "name+ &cidr+10.1.0.0/24&routesto+206.196.0.0/16,nextHop+internet\r\nfrom+vpn,to+0.0.0.0/0,nextHop+vpn\r\nto+72.24.24.0/24,nextHop+vpn");
            paramMap.put("subnet2", "name+ &cidr+10.1.1.0/24");
            paramMap.put("netRoutes", "to+0.0.0.0/0,nextHop+internet");
            paramMap.put("vm1", "1&imageType&instanceType&volumeSize&batch");
            paramMap.put("vm2", "1&imageType&instanceType&volumeSize&batch");

            paramMap.remove("netCreate");
            paramMap.remove("template1");

            retCode = servBean.createNetwork(paramMap);
        } else { // Custom Form Handling
            PreparedStatement prep = rains_conn.prepareStatement("SELECT driverEjbPath"
                    + " FROM driver_instance WHERE topologyUri = ?");
            prep.setString(1, paramMap.get("topoUri"));
            ResultSet rs1 = prep.executeQuery();
            rs1.next();
            String driverPath = rs1.getString(1);
            if (driverPath.contains("Aws") || driverPath.contains("aws")) {
                paramMap.put("driverType", "aws");
            } else if (driverPath.contains("Os") || driverPath.contains("os")) {
                paramMap.put("driverType", "os");
            }

            // Process each subnet.
            for (int i = 1; i < 10; i++) {
                if (paramMap.containsKey("subnet" + i + "-cidr")) {
                    if (!paramMap.containsKey("name")) {
                        paramMap.put("subnet" + i + "-name", " ");
                    }
                    
                    String subnetString = "name+" + paramMap.get("subnet" + i + "-name") + "&cidr+" + paramMap.get("subnet" + i + "-cidr") + "&";

                    // Check for route existence.
                    if (paramMap.containsKey("subnet" + i + "-route1-to")) {
                        subnetString += "routes";
                    }

                    // Process each routes.
                    for (int j = 1; j < 10; j++) {
                        // Check for subnet existence.
                        if (paramMap.containsKey("subnet" + i + "-route" + j + "-to")) {
                            subnetString += "to+" + paramMap.get("subnet" + i + "-route" + j + "-to") + ",";

                            if (paramMap.containsKey("subnet" + i + "-route" + j + "-from")) {
                                subnetString += "from+" + paramMap.get("subnet" + i + "-route" + j + "-from") + ",";
                            }
                            if (paramMap.containsKey("subnet" + i + "-route" + j + "-next")) {
                                subnetString += "nextHop+" + paramMap.get("subnet" + i + "-route" + j + "-next");
                            }
                            subnetString += "\r\n";
                        }
                        
                        paramMap.remove("subnet" + i + "-route" + j + "-to");
                        paramMap.remove("subnet" + i + "-route" + j + "-from");
                        paramMap.remove("subnet" + i + "-route" + j + "-next");
                    }                    

                    // Apply route propagation
                    if (paramMap.containsKey("subnet" + i + "-route-prop")) {
                        if (!subnetString.contains("routes")) {
                            subnetString += "routes";
                        }
                        subnetString += "from+vpn,to+0.0.0.0/0,nextHop+vpn";
                    } else {
                        if (subnetString.contains("routes"))  {
                            subnetString = subnetString.substring(0, (subnetString.length() - 2));
                        }
                    }
                    paramMap.remove("subnet" + i + "-route-prop");
                    
                    paramMap.remove("subnet" + i + "-cidr");
                    paramMap.remove("subnet" + i + "-name");
                    
                    paramMap.put("subnet" + i, subnetString);
                }
            }

            paramMap.remove("userID");                                              
            paramMap.remove("custom");
            paramMap.remove("netCreate");

            retCode = servBean.createNetwork(paramMap);
        }

        return ("/VersaStack-web/ops/srvc/netcreate.jsp?ret=" + retCode);

    }
}
