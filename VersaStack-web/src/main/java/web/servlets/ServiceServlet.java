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
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import net.maxgigapop.mrs.rest.api.WebResource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import web.async.AppAsyncListener;
import web.async.DNCWorker;
import web.async.FL2PWorker;
import web.async.DriverWorker;
import web.async.NetCreateWorker;

@WebServlet(asyncSupported = true, value = "/ServiceServlet")
public class ServiceServlet extends HttpServlet {

    serviceBeans servBean = new serviceBeans();
    String host = "http://localhost:8080/VersaStack-web/restapi";

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

            // Select the correct service.
            if (request.getParameter("driverID") != null) { // Driver
                serviceString = "driver";
            } else if (request.getParameter("netCreate") != null) { // Network Creation
                serviceString = "netcreate";
            } else if (request.getParameter("dncCreate") != null) {
                serviceString = "dnc";
            } else if (request.getParameter("driverType") != null) { // VM
                serviceString = "vmadd";
            } else if (request.getParameter("fl2pCreate") != null) {
                serviceString = "fl2p";
            } else {
                response.sendRedirect("/VersaStack-web/errorPage.jsp");
            }

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

            // Parse Service.
            if (serviceString.equals("driver")) { // Driver
                response.sendRedirect(createDriverInstance(request, paraMap));
            } else if (serviceString.equals("vmadd")) { // VM
                response.sendRedirect(createVMInstance(paraMap));
            } else if (serviceString.equals("netcreate")) { // Network Creation
                response.sendRedirect(parseFullNetwork(request, paraMap));
            } else if (serviceString.equals("dnc")) {
                response.sendRedirect(parseConnection(request, paraMap));
            } else if (serviceString.equals("fl2p")) {
                response.sendRedirect(createFlow(request, paraMap));
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

    public String createDriverInstance(HttpServletRequest request, HashMap<String, String> paraMap) {

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
            paraMap.put("subsystemBaseUrl", "http://206.196.179.139:8080/VersaNS-0.0.1-SNAPSHOT");
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

    public String createVMInstance(HashMap<String, String> paraMap) {
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

    public String parseFullNetwork(HttpServletRequest request, HashMap<String, String> paraMap) throws SQLException {
        for (Object key : paraMap.keySet().toArray()) {
            if (paraMap.get((String) key).isEmpty()) {
                paraMap.remove((String) key);
            }
        }

        JSONObject inputJSON = new JSONObject();
        JSONObject dataJSON = new JSONObject();
        inputJSON.put("user", paraMap.get("username"));
        inputJSON.put("type", "netcreate");
        inputJSON.put("alias", paraMap.get("alias"));

        JSONArray cloudArr = new JSONArray();
        JSONObject cloudJSON = new JSONObject();
        cloudJSON.put("type", paraMap.get("netType"));
        cloudJSON.put("cidr", paraMap.get("netCidr"));
        cloudJSON.put("name", "vtn1");
        cloudJSON.put("parent", paraMap.get("topoUri"));

        // Parse gateways.
        // AWS
        JSONArray gatewayArr = new JSONArray();
        if (paraMap.containsKey("conn-dest")) {
            JSONObject gatewayJSON = new JSONObject();
            gatewayJSON.put("name", "aws_dx1");
            gatewayJSON.put("type", "aws_direct_connect");

            JSONArray gateToArr = new JSONArray();
            JSONObject gateToJSON = new JSONObject();

            String connString = paraMap.get("conn-dest");
            if (paraMap.containsKey("conn-vlan")) {
                connString += "?vlan=" + paraMap.get("conn-vlan");
            } else {
                connString += "?vlan=any";
            }
            gateToJSON.put("value", connString);
            gateToJSON.put("type", "stitch_port");

            gateToArr.add(gateToJSON);
            gatewayJSON.put("to", gateToArr);
            gatewayArr.add(gatewayJSON);
        }
        // OpenStack
        for (int i = 1; i <= 10; i++) {
            if (paraMap.containsKey("gateway" + i + "-name")) {
                JSONObject gatewayJSON = new JSONObject();
                gatewayJSON.put("name", paraMap.get("gateway" + i + "-name"));
                gatewayJSON.put("type", "ucs_port_profile");                                
                
                if (paraMap.containsKey("gateway" + i + "-from")) {
                    JSONArray fromArr = new JSONArray();
                    JSONObject fromJSON = new JSONObject();
                    fromJSON.put("value", paraMap.get("gateway" + i + "-from"));                    
                    fromJSON.put("type", paraMap.get("gateway" + i + "-type"));                    
                    
                    fromArr.add(fromJSON);
                    gatewayJSON.put("from", fromArr);                    
                }
                if (paraMap.containsKey("gateway" + i + "-to")) {
                    JSONArray toArr = new JSONArray();
                    JSONObject toJSON = new JSONObject();
                    toJSON.put("value", paraMap.get("gateway" + i + "-to"));
                    toJSON.put("type", paraMap.get("gateway" + i + "-type"));                    
                                        
                    toArr.add(toJSON);
                    gatewayJSON.put("to", toArr);
                }
                
                gatewayArr.add(gatewayJSON);
            }
        }
        cloudJSON.put("gateways", gatewayArr);

        // Process each subnet.
        JSONArray subnetArr = new JSONArray();
        for (int i = 1; i <= 10; i++) {
            if (paraMap.containsKey("subnet" + i + "-cidr")) {
                JSONObject subnetJSON = new JSONObject();
                subnetJSON.put("cidr", paraMap.get("subnet" + i + "-cidr"));

                subnetJSON.put("name", paraMap.containsKey("subnet" + i + "-name") ? paraMap.get("subnet" + i + "-name") : " ");

                // Process each routes.
                JSONArray routeArr = new JSONArray();
                for (int j = 1; j <= 10; j++) {
                    // Check for subroute existence.
                    JSONObject routeJSON = new JSONObject();
                    if (paraMap.containsKey("subnet" + i + "-route" + j + "-to")) {
                        JSONObject toJSON = new JSONObject();
                        toJSON.put("value", paraMap.get("subnet" + i + "-route" + j + "-to"));
                        toJSON.put("type", "ipv4-prefix");
                        routeJSON.put("to", toJSON);
                    }

                    if (paraMap.containsKey("subnet" + i + "-route" + j + "-from")) {
                        JSONObject fromJSON = new JSONObject();
                        fromJSON.put("value", paraMap.get("subnet" + i + "-route" + j + "-from"));
                        routeJSON.put("from", fromJSON);
                    }
                    if (paraMap.containsKey("subnet" + i + "-route" + j + "-next")) {
                        JSONObject nextJSON = new JSONObject();
                        nextJSON.put("value", paraMap.get("subnet" + i + "-route" + j + "-next"));
                        routeJSON.put("next_hop", nextJSON);
                    }

                    if (!routeJSON.isEmpty()) {
                        routeArr.add(routeJSON);
                    }
                }

                // Apply route propagation
                if (paraMap.containsKey("subnet" + i + "-route-prop")) {
                    JSONObject routeJSON = new JSONObject();

                    JSONObject fromJSON = new JSONObject();
                    fromJSON.put("value", "vpn");
                    routeJSON.put("from", fromJSON);

                    JSONObject toJSON = new JSONObject();
                    toJSON.put("value", "0.0.0.0/0");
                    toJSON.put("type", "ipv4-prefix");
                    routeJSON.put("to", toJSON);

                    JSONObject nextJSON = new JSONObject();
                    nextJSON.put("value", "vpn");
                    routeJSON.put("next_hop", nextJSON);

                    routeArr.add(routeJSON);
                } else if (paraMap.containsKey("subnet" + i + "-route-default")) {
                    JSONObject routeJSON = new JSONObject();

                    JSONObject toJSON = new JSONObject();
                    toJSON.put("value", "0.0.0.0/0");
                    toJSON.put("type", "ipv4-prefix");
                    routeJSON.put("to", toJSON);

                    JSONObject nextJSON = new JSONObject();
                    nextJSON.put("value", "internet");
                    routeJSON.put("next_hop", nextJSON);

                    routeArr.add(routeJSON);
                }
                subnetJSON.put("routes", routeArr);

                // Process VMs.
                JSONArray vmArr = new JSONArray();
                for (int j = 1; j <= 10; j++) {
                    if (paraMap.containsKey("vm" + j + "-subnet") && (Integer.parseInt(paraMap.get("vm" + j + "-subnet")) == i)) {
                        JSONObject vmJSON = new JSONObject();
                        if (paraMap.get("submit").equalsIgnoreCase("aws")) {
                            vmJSON.put("name", paraMap.get("vm" + j + "-name"));
                            vmJSON.put("host", paraMap.get("vm" + j + "-host"));

                            // Parse Types.
                            String vmString = "";
                            if (paraMap.containsKey("vm" + j + "-instance")) {
                                vmString += "instance+" + paraMap.get("vm" + j + "-instance");
                            }
                            if (paraMap.containsKey("vm" + j + "-security")) {
                                if (!vmString.isEmpty()) {
                                    vmString += ",";
                                }
                                vmString += "secgroup+" + paraMap.get("vm" + j + "-security");
                            }
                            if (paraMap.containsKey("vm" + j + "-image")) {
                                if (!vmString.isEmpty()) {
                                    vmString += ",";
                                }
                                vmString += "image+" + paraMap.get("vm" + j + "-image");
                            }
                            if (paraMap.containsKey("vm" + j + "-keypair")) {
                                if (!vmString.isEmpty()) {
                                    vmString += ",";
                                }
                                vmString += "keypair+" + paraMap.get("vm" + j + "-keypair");
                            }
                            vmJSON.put("type", vmString);

                            // not implemented yet
                            JSONArray interfaceArr = new JSONArray();
                            if (true) {
                                JSONObject interfaceJSON = new JSONObject();

                                interfaceArr.add(interfaceJSON);
                            }
                        } 
                        else if (paraMap.get("submit").equalsIgnoreCase("ops")) {
                            vmJSON.put("name", paraMap.get("vm" + j + "-name"));
                            vmJSON.put("host", paraMap.get("vm" + j + "-host"));

                            // Parse Types.
                            String vmString = "";
                            if (paraMap.containsKey("vm" + j + "-instance")) {
                                vmString += "instance+" + paraMap.get("vm" + j + "-instance");
                            }
                            if (paraMap.containsKey("vm" + j + "-security")) {
                                if (!vmString.isEmpty()) {
                                    vmString += ",";
                                }
                                vmString += "secgroup+" + paraMap.get("vm" + j + "-security");
                            }
                            if (paraMap.containsKey("vm" + j + "-image")) {
                                if (!vmString.isEmpty()) {
                                    vmString += ",";
                                }
                                vmString += "image+" + paraMap.get("vm" + j + "-image");
                            }
                            if (paraMap.containsKey("vm" + j + "-keypair")) {
                                if (!vmString.isEmpty()) {
                                    vmString += ",";
                                }
                                vmString += "keypair+" + paraMap.get("vm" + j + "-keypair");
                            }
                            vmJSON.put("type", vmString);

                            //Parse Interfaces: either floating IP or SRIOV connection
                            JSONArray interfaceArr = new JSONArray();
                            //check if assigning floating IP
                            if (paraMap.containsKey("vm" + j + "-floating")) {
                                JSONObject interfaceJSON = new JSONObject();
                                interfaceJSON.put("name", paraMap.get("vm" + j + "-name") + ":eth0");
                                interfaceJSON.put("type", "Ethernet");
                                interfaceJSON.put("address", "ipv4+" + paraMap.get("vm" + j + "-floating") + "/255.255.255.0");
                                interfaceArr.add(interfaceJSON);

                                //Process SRIOV only when a floating IP is assigned
                                for (int k = 1; k <= 10; k++) {
                                    if (paraMap.containsKey("SRIOV" + k + "-ip") && Integer.parseInt(paraMap.get("SRIOV" + k + "-vm")) == j) {
                                        JSONObject sriovJSON = new JSONObject();
                                        String addrString = "ipv4+" + paraMap.get("SRIOV" + k + "-ip") + "/255.255.255.0";
                                        addrString += ",mac+" + paraMap.get("SRIOV" + k + "-mac");
                                        sriovJSON.put("address", addrString);
                                        sriovJSON.put("name", paraMap.get("vm" + j + "-name") + ":eth" + k);
                                        sriovJSON.put("type", "SRIOV");
                                        sriovJSON.put("gateway", paraMap.get("gateway" + paraMap.get("SRIOV" + k + "-gateway") + "-name"));

                                        interfaceArr.add(sriovJSON);
                                    }

                                }
                            }
                            vmJSON.put("interfaces", interfaceArr);
                        }

                        // Process each routes.
                        JSONArray vmRouteArr = new JSONArray();
                        for (int k = 1; k <= 10; k++) {
                            // Check for subroute existence.
                            JSONObject routeJSON = new JSONObject();
                            if (paraMap.containsKey("vm" + j + "-route" + k + "-to")) {
                                JSONObject toJSON = new JSONObject();
                                toJSON.put("value", paraMap.get("vm" + j + "-route" + k + "-to"));
                                toJSON.put("type", "ipv4-prefix");
                                routeJSON.put("to", toJSON);
                            }

                            if (paraMap.containsKey("vm" + j + "-route" + k + "-from")) {
                                JSONObject fromJSON = new JSONObject();
                                fromJSON.put("value", paraMap.get("vm" + j + "-route" + k + "-from"));
                                routeJSON.put("from", fromJSON);
                            }
                            if (paraMap.containsKey("vm" + j + "-route" + k + "-next")) {
                                JSONObject nextJSON = new JSONObject();
                                nextJSON.put("value", paraMap.get("vm" + j + "-route" + k + "-next"));
                                routeJSON.put("next_hop", nextJSON);
                            }

                            if (!routeJSON.isEmpty()) {
                                vmRouteArr.add(routeJSON);
                            }
                        }
                        vmJSON.put("routes", vmRouteArr);

                        if (!vmJSON.isEmpty()) {
                            vmArr.add(vmJSON);
                        }
                    }
                }
                subnetJSON.put("virtual_machines", vmArr);
                subnetArr.add(subnetJSON);
            }
        }
        cloudJSON.put("subnets", subnetArr);

        // Parse network routes.
        JSONArray netRouteArr = new JSONArray();
        JSONObject netRouteJSON = new JSONObject();

        JSONObject toJSON = new JSONObject();
        toJSON.put("value", "0.0.0.0/0");
        toJSON.put("type", "ipv4-prefix");
        netRouteJSON.put("to", toJSON);

        JSONObject nextJSON = new JSONObject();
        nextJSON.put("value", "internet");
        netRouteJSON.put("next_hop", nextJSON);

        netRouteArr.add(netRouteJSON);
        cloudJSON.put("routes", netRouteArr);

        cloudArr.add(cloudJSON);
        dataJSON.put("virtual_clouds", cloudArr);
        inputJSON.put("data", dataJSON);

        request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext asyncCtx = request.startAsync();
        asyncCtx.addListener(new AppAsyncListener());
        asyncCtx.setTimeout(300000);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");
        executor.execute(new APIRunner(inputJSON));

        return ("/VersaStack-web/ops/catalog.jsp");

    }

    public String createFlow(HttpServletRequest request, HashMap<String, String> paraMap) throws SQLException {
        for (Object Key : paraMap.keySet().toArray()) {
            if (paraMap.get((String) Key).isEmpty()) {
                paraMap.remove((String) Key);
            }
        }

        Connection rains_conn;
        Properties rains_connectionProps = new Properties();
        rains_connectionProps.put("user", "root");
        rains_connectionProps.put("password", "root");

        rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb",
                rains_connectionProps);

        if (paraMap.containsKey("template1")) {
            //paraMap.put("driverType", "aws");
            paraMap.put("topUri", "urn:ogf:network:domain=vo1.versastack.org:link=link1");
            paraMap.put("eth_src", "urn:ogf:network:onos.maxgigapop.net:network1:of:0000000000000005:port-s5-eth1");
            paraMap.put("eth_des", "urn:ogf:network:onos.maxgigapop.net:network1:of:0000000000000002:port-s2-eth1");
            paraMap.remove("template1");
            paraMap.remove("fl2pCreate");

            servBean.createflow(paraMap);

        } else {
            paraMap.remove("userID");
            paraMap.remove("custom");
            paraMap.remove("fl2pCreate");
            //Process each link

            for (Map.Entry<String, String> entry : paraMap.entrySet()) {
                System.out.println(entry.getKey() + entry.getValue());
            }

            // Async setup 
            request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            AsyncContext asyncCtx = request.startAsync();
            asyncCtx.addListener(new AppAsyncListener());
            asyncCtx.setTimeout(60000);

            ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

            executor.execute(new FL2PWorker(asyncCtx, paraMap));
        }
        return ("/VersaStack-web/ops/srvc/fl2p.jsp?ret=0");

    }

    // @TODO: COMPLETELY INEFFICIENT BUT LOW PRIORITY - SEE WEBRESOURCE
    public String parseConnection(HttpServletRequest request, HashMap<String, String> paraMap) throws SQLException, IOException {
        for (Object key : paraMap.keySet().toArray()) {
            if (paraMap.get((String) key).isEmpty()) {
                paraMap.remove((String) key);
            }
        }

        JSONObject inputJSON = new JSONObject();
        JSONObject dataJSON = new JSONObject();
        inputJSON.put("user", paraMap.get("username"));
        inputJSON.put("type", "dnc");
        inputJSON.put("alias", paraMap.get("alias"));

        //Process each link
        JSONArray linkArr = new JSONArray();
        for (int i = 1; i < 10; i++) {
            if (paraMap.containsKey("linkUri" + i)) {
                JSONObject linkJSON = new JSONObject();
                linkJSON.put("name", paraMap.get("linkUri" + i));

                if (paraMap.containsKey("link" + i + "-src")) {
                    linkJSON.put("src", paraMap.get("link" + i + "-src"));
                }
                if (paraMap.containsKey("link" + i + "-src-vlan")) {
                    linkJSON.put("src-vlan", paraMap.get("link" + i + "-src-vlan"));
                }
                if (paraMap.containsKey("link" + i + "-des")) {
                    linkJSON.put("des", paraMap.get("link" + i + "-des"));
                }
                if (paraMap.containsKey("link" + i + "-des-vlan")) {
                    linkJSON.put("des-vlan", paraMap.get("link" + i + "-des-vlan"));
                }

                linkArr.add(linkJSON);
            }
        }

        dataJSON.put("links", linkArr);
        inputJSON.put("data", dataJSON);

        request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        AsyncContext asyncCtx = request.startAsync();
        asyncCtx.addListener(new AppAsyncListener());
        asyncCtx.setTimeout(300000);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");
        executor.execute(new APIRunner(inputJSON));

        return ("/VersaStack-web/ops/srvc/dnc.jsp?ret=0");

    }

}

class APIRunner implements Runnable {

    JSONObject inputJSON;
    serviceBeans servBean = new serviceBeans();
    String host = "http://localhost:8080/VersaStack-web/restapi";

    public APIRunner(JSONObject input) {
        inputJSON = input;
    }

    @Override
    public void run() {
        try {
            System.out.println("API Runner Engaged!");
            URL url = new URL(String.format("%s/app/service/", host));
            HttpURLConnection create = (HttpURLConnection) url.openConnection();
            String result = servBean.executeHttpMethod(url, create, "POST", inputJSON.toJSONString());
        } catch (IOException ex) {
            Logger.getLogger(ServiceServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
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
