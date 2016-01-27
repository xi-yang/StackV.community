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
import java.util.concurrent.ThreadPoolExecutor;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import web.async.AppAsyncListener;
import web.async.DriverWorker;
import web.async.NetCreateWorker;

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
        long startTime = System.currentTimeMillis();
        System.out.println("Service Servlet Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());

        try {
            // Instance Creation
            String serviceString = "";
            HashMap<String, String> paraMap = new HashMap<>();
            Enumeration paramNames = request.getParameterNames();
            String host = "http://localhost:8080/VersaStack-web/restapi";

            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String refUuid = servBean.executeHttpMethod(url, connection, "GET", null);
            paraMap.put("instanceUUID", refUuid);

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

            // Create paraMap.
            while (paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                String[] paramValues = request.getParameterValues(paramName);
                if (paramValues.length == 1) {
                    String paramValue = paramValues[0];
                    paraMap.put(paramName, paramValue);
                } else if (paramValues.length > 1) {
                    String fullValue = "";
                    for (String paramValue : paramValues) {
                        fullValue += paramValue + "\r\n";
                    }
                    fullValue = fullValue.substring(0, fullValue.length() - 4);
                    paraMap.put(paramName, fullValue);
                }
            }

            // Replicate properties into DB.
            for (String key : paraMap.keySet()) {
                if (!paraMap.get(key).isEmpty()) {
                    url = new URL(String.format("%s/service/property/%s/%s/", host, refUuid, key));
                    connection = (HttpURLConnection) url.openConnection();
                    servBean.executeHttpMethod(url, connection, "POST", paraMap.get(key));
                }
            }

            // Execute service Creation.
            if (serviceString.equals("driver")) { // Driver
                response.sendRedirect(createDriverInstance(request, paraMap));
            } else if (serviceString.equals("vmadd")) { // VM
                response.sendRedirect(createVMInstance(paraMap));
            } else if (serviceString.equals("netcreate")) { // Network Creation
                response.sendRedirect(createFullNetwork(request, paraMap));
            } else {
                response.sendRedirect("/VersaStack-web/errorPage.jsp");
            }
        } catch (SQLException ex) {
            Logger.getLogger(ServiceServlet.class.getName()).log(Level.SEVERE, null, ex);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Service Servlet End::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId() + "::Time Taken="
                + (endTime - startTime) + " ms.");
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

    private String createDriverInstance(HttpServletRequest request, HashMap<String, String> paraMap) {

        // Handles templates, in this order:
        // OpenStack, Stack Driver, Stub Driver, Generic Driver, AWS Driver 
        if (paraMap.containsKey("template1")) {
            paraMap.put("driverID", "openStackDriver");
            paraMap.put("url", "http://max-vlsr2.dragon.maxgigapop.net:35357/v2.0");
            paraMap.put("NATServer", "");
            paraMap.put("driverEjbPath", "java:module/OpenStackDriver");
            paraMap.put("username", "admin");
            paraMap.put("password", "1234");
            paraMap.put("topologyUri", "urn:ogf:network:openstack.com:openstack-cloud");
            paraMap.put("tenant", "admin");
            paraMap.put("install", "Install");
        } else if (paraMap.containsKey("template3")) {
            paraMap.put("driverID", "stubdriver");
            paraMap.put("topologyUri", "urn:ogf:network:rains.maxgigapop.net:wan:2015:topology");
            paraMap.put("driverEjbPath", "java:module/StubSystemDriver");

            // Reads large stubModelTtl property from file.
            String stubModelTTL = "", nextLine;
            String testingPath = "/Users/max/NetBeansProjects/FrontVis/VersaStack/VersaStack-web/"
                    + "src/main/webapp/tools/testing/";
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

            paraMap.put("stubModelTtl", stubModelTTL);
            paraMap.put("install", "Install");
        } else if (paraMap.containsKey("template4")) {
            paraMap.put("driverID", "versaNSDriver");
            paraMap.put("topologyUri", "urn:ogf:network:sdn.maxgigapop.net:network");
            paraMap.put("driverEjbPath", "java:module/GenericRESTDriver");
            paraMap.put("subsystemBaseUrl", "http://charon.dragon.maxgigapop.net:8080/VersaNS-0.0.1-SNAPSHOT");
            paraMap.put("install", "Install");
        }

        // Connect dynamically generated elements
        for (int i = 1; i <= 5; i++) {
            if (paraMap.containsKey("apropname" + i)) {
                paraMap.put(paraMap.get("apropname" + i), paraMap.get("apropval" + i));

                paraMap.remove("apropname" + i);
                paraMap.remove("apropval" + i);
            }
        }

        /*
         int retCode = -1;
         // Call appropriate driver control method
         if (paraMap.containsKey("install")) {
         paraMap.remove("install");
         retCode = servBean.driverInstall(paraMap);
         } else if (paraMap.containsKey("uninstall")) {
         retCode = servBean.driverUninstall(paraMap.get("topologyUri"));
         }*/
        // Async setup
        request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext asyncCtx = request.startAsync();
        asyncCtx.addListener(new AppAsyncListener());
        asyncCtx.setTimeout(60000);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

        executor.execute(new DriverWorker(asyncCtx, paraMap));

        return ("/VersaStack-web/ops/srvc/driver.jsp?ret=0");
    }

    private String createVMInstance(HashMap<String, String> paraMap) {
        int retCode = -1;

        // Handle templates 
        // AWS
        if (paraMap.containsKey("template1")) {
            paraMap.put("topologyUri", "urn:ogf:network:aws.amazon.com:aws-cloud");
            paraMap.put("vmType", "aws");
            paraMap.put("region", "us-east-1");
            paraMap.put("ostype", "windows");
            paraMap.put("vmQuantity", "1");
            paraMap.put("instanceType", "instance1");
            paraMap.put("vpcID", "urn:ogf:network:aws.amazon.com:aws-cloud:vpc-45143020");
            paraMap.put("subnets", "urn:ogf:network:aws.amazon.com:aws-cloud:subnet-a8a632f1,10.0.1.0");
            paraMap.put("volumes", "8,standard,/dev/xvda,snapshot\\r\\n");
            paraMap.put("driverType", "aws");
            paraMap.put("graphTopo", "none");
        }

        if (paraMap.get("driverType").equals("aws")) {
            if (!paraMap.get("graphTopo").equalsIgnoreCase("none")) {
                paraMap.put("topologyUri", paraMap.get("graphTopo"));
            }

            // Format volumes
            String volString = "";

            // Include root
            volString += paraMap.get("root-size") + ",";
            volString += paraMap.get("root-type") + ",";
            volString += paraMap.get("root-path") + ",";
            volString += paraMap.get("root-snapshot") + "\r\n";
            paraMap.remove("root-size");
            paraMap.remove("root-type");
            paraMap.remove("root-path");
            paraMap.remove("root-snapshot");

            for (int i = 1; i <= 10; i++) {
                if (paraMap.containsKey(i + "-path")) {
                    volString += paraMap.get(i + "-size") + ",";
                    volString += paraMap.get(i + "-type") + ",";
                    volString += paraMap.get(i + "-path") + ",";
                    volString += paraMap.get(i + "-snapshot") + "\r\n";
                    paraMap.remove(i + "-size");
                    paraMap.remove(i + "-type");
                    paraMap.remove(i + "-path");
                    paraMap.remove(i + "-snapshot");
                }
            }
            paraMap.put("volumes", volString);

            paraMap.remove("install");

            for (int i = 0; i < Integer.parseInt(paraMap.get("vmQuantity")); i++) {
                retCode = servBean.vmInstall(paraMap);
            }
        }
        return ("/VersaStack-web/ops/srvc/vmadd.jsp?ret=" + retCode);
    }

    private String createFullNetwork(HttpServletRequest request, HashMap<String, String> paraMap) throws SQLException {
        int retCode;

        for (Object key : paraMap.keySet().toArray()) {
            if (paraMap.get((String) key).isEmpty()) {
                paraMap.remove((String) key);
            }
        }

        Connection rains_conn;
        Properties rains_connectionProps = new Properties();
        rains_connectionProps.put("user", "root");
        rains_connectionProps.put("password", "root");

        rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb",
                rains_connectionProps);

        if (paraMap.containsKey("template1")) { // Basic Template
            // Add template data.
            paraMap.put("driverType", "aws");
            paraMap.put("topoUri", "urn:ogf:network:aws.amazon.com:aws-cloud");
            paraMap.put("netType", "internal");
            paraMap.put("netCidr", "10.1.0.0/16");
            paraMap.put("subnet1", "name+ &cidr+10.1.0.0/24&routesto+206.196.0.0/16,nextHop+internet\r\nfrom+vpn,to+0.0.0.0/0,nextHop+vpn\r\nto+72.24.24.0/24,nextHop+vpn");
            paraMap.put("subnet2", "name+ &cidr+10.1.1.0/24");
            paraMap.put("netRoutes", "to+0.0.0.0/0,nextHop+internet");
            paraMap.put("vm1", "1&imageType&instanceType&volumeSize&batch");
            paraMap.put("vm2", "1&imageType&instanceType&volumeSize&batch");

            paraMap.remove("netCreate");
            paraMap.remove("template1");

            // Async setup
            /*request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            AsyncContext asyncCtx = request.startAsync();
            asyncCtx.addListener(new AppAsyncListener());
            asyncCtx.setTimeout(60000);

            ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

            executor.execute(new NetCreateWorker(asyncCtx, paraMap));*/
            servBean.createNetwork(paraMap);
        } else { // Custom Form Handling
            PreparedStatement prep = rains_conn.prepareStatement("SELECT driverEjbPath"
                    + " FROM driver_instance WHERE topologyUri = ?");
            prep.setString(1, paraMap.get("topoUri"));
            ResultSet rs1 = prep.executeQuery();
            rs1.next();
            String driverPath = rs1.getString(1);
            if (driverPath.contains("Aws") || driverPath.contains("aws")) {
                paraMap.put("driverType", "aws");
            } else if (driverPath.contains("Os") || driverPath.contains("os")) {
                paraMap.put("driverType", "os");
            }

            // Process each subnet.
            for (int i = 1; i < 10; i++) {
                if (paraMap.containsKey("subnet" + i + "-cidr")) {
                    if (!paraMap.containsKey("name")) {
                        paraMap.put("subnet" + i + "-name", " ");
                    }

                    String subnetString = "name+" + paraMap.get("subnet" + i + "-name") + "&cidr+" + paraMap.get("subnet" + i + "-cidr") + "&";

                    // Check for route existence.
                    if (paraMap.containsKey("subnet" + i + "-route1-to")) {
                        subnetString += "routes";
                    }

                    // Process each routes.
                    for (int j = 1; j < 10; j++) {
                        // Check for subnet existence.
                        if (paraMap.containsKey("subnet" + i + "-route" + j + "-to")) {
                            subnetString += "to+" + paraMap.get("subnet" + i + "-route" + j + "-to") + ",";

                            if (paraMap.containsKey("subnet" + i + "-route" + j + "-from")) {
                                subnetString += "from+" + paraMap.get("subnet" + i + "-route" + j + "-from") + ",";
                            }
                            if (paraMap.containsKey("subnet" + i + "-route" + j + "-next")) {
                                subnetString += "nextHop+" + paraMap.get("subnet" + i + "-route" + j + "-next");
                            }
                            subnetString += "\r\n";
                        }

                        paraMap.remove("subnet" + i + "-route" + j + "-to");
                        paraMap.remove("subnet" + i + "-route" + j + "-from");
                        paraMap.remove("subnet" + i + "-route" + j + "-next");
                    }

                    // Apply route propagation
                    if (paraMap.containsKey("subnet" + i + "-route-prop")) {
                        if (!subnetString.contains("routes")) {
                            subnetString += "routes";
                        }
                        subnetString += "from+vpn,to+0.0.0.0/0,nextHop+vpn";
                    } else {
                        if (subnetString.contains("routes")) {
                            subnetString = subnetString.substring(0, (subnetString.length() - 2));
                        }
                    }
                    paraMap.remove("subnet" + i + "-route-prop");

                    paraMap.remove("subnet" + i + "-cidr");
                    paraMap.remove("subnet" + i + "-name");

                    paraMap.put("subnet" + i, subnetString);
                }
            }

            paraMap.remove("userID");
            paraMap.remove("custom");
            paraMap.remove("netCreate");

            // Async setup
            request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            AsyncContext asyncCtx = request.startAsync();
            asyncCtx.addListener(new AppAsyncListener());
            asyncCtx.setTimeout(60000);

            ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

            executor.execute(new NetCreateWorker(asyncCtx, paraMap));
        }

        return ("/VersaStack-web/ops/srvc/netcreate.jsp?ret=0");

    }
    
    private String createConnection(HttpServletRequest request, HashMap<String, String> paraMap) {        
        for (Object key : paraMap.keySet().toArray()) {
            if (paraMap.get((String) key).isEmpty()) {
                paraMap.remove((String) key);
            }
        }
        
        // ParaMap processing




        /* // Async setup 
        request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext asyncCtx = request.startAsync();
        asyncCtx.addListener(new AppAsyncListener());
        asyncCtx.setTimeout(60000);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

        executor.execute(new DNCWorker(asyncCtx, paraMap)); */
        servBean.createConnection(paraMap);

        return ("/VersaStack-web/ops/srvc/dnc.jsp?ret=0");
    }
}

/*

TEMPLATE SERVICE METHOD - REPLACE ___ PREFIXED NAMES
    private String [___servicename](HttpServletRequest request, HashMap<String, String> paraMap) {        
        for (Object key : paraMap.keySet().toArray()) {
            if (paraMap.get((String) key).isEmpty()) {
                paraMap.remove((String) key);
            }
        }
        
        // ParaMap processing




        // Async setup
        request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext asyncCtx = request.startAsync();
        asyncCtx.addListener(new AppAsyncListener());
        asyncCtx.setTimeout(60000);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

        executor.execute(new [___serviceworker](asyncCtx, paraMap));

        return ("/VersaStack-web/ops/srvc/[___servicejsp]?ret=0");
    }

*/