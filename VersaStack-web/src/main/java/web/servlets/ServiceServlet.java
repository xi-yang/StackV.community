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
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import web.async.AppAsyncListener;
import web.async.DNCWorker;
import web.async.FL2PWorker;
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
            front_connectionProps.put("user", "front_view");
            front_connectionProps.put("password", "frontuser");

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            // Select the correct service.
            if (request.getParameter("driverID") != null) { // Driver
                serviceString = "driver";
            } else if (request.getParameter("driverType") != null) { // VM
                serviceString = "vmadd";
            } else if (request.getParameter("netCreate") != null) { // Network Creation
                serviceString = "netcreate";
            } else if (request.getParameter("dncCreate") != null) {
                serviceString = "dnc";
            } else if(request.getParameter("fl2pCreate") != null){
                serviceString ="fl2p";
            }
            else {
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
            prep = front_conn.prepareStatement("INSERT INTO frontend.service_instance "
                    + "(`service_id`, `user_id`, `creation_time`, `referenceUUID`, `service_state_id`) VALUES (?, ?, ?, ?, ?)");
            prep.setInt(1, serviceID);
            prep.setString(2, request.getParameter("userID"));
            prep.setTimestamp(3, timeStamp);
            prep.setString(4, refUuid);
            prep.setInt(5, 1);
            prep.executeUpdate();
            
            int instanceID = servBean.getInstanceID(refUuid);
            
            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_history` "
                    + "(`service_history_id`, `service_state_id`, `service_instance_id`) VALUES (1, 1, ?)");
            prep.setInt(1, instanceID);
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
            } else if (serviceString.equals("dnc")) {
                //System.out.println("Im inside dnc");
                response.sendRedirect(createConnection(request, paraMap));
            } else if (serviceString.equals("fl2p")){
               response.sendRedirect(createFlow(request, paraMap)); 
            }
            else {
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

    public String createFullNetwork(HttpServletRequest request, HashMap<String, String> paraMap) throws SQLException {
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
            paraMap.put("topoUri", "urn:ogf:network:aws.amazon.com:aws-cloud");
            paraMap.put("netCidr", "10.1.0.0/16");
            paraMap.put("driverType","aws");
            paraMap.put("subnet1", "name+ &cidr+10.1.0.0/24&routesto+206.196.0.0/16,nextHop+internet\r\nfrom+vpn,to+0.0.0.0/0,nextHop+vpn\r\nto+72.24.24.0/24,nextHop+vpn");
            paraMap.put("subnet2", "name+ &cidr+10.1.1.0/24");

            paraMap.remove("netCreate");
            paraMap.remove("template1");

            // Async setup
            request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            AsyncContext asyncCtx = request.startAsync();
            asyncCtx.addListener(new AppAsyncListener());
            asyncCtx.setTimeout(60000);

            ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

            executor.execute(new NetCreateWorker(asyncCtx, paraMap));
        }
        else if(paraMap.containsKey("template2")){ // AWS with VMs and direct connect
            // Add template data.
            paraMap.put("topoUri", "urn:ogf:network:aws.amazon.com:aws-cloud");
            paraMap.put("netCidr", "10.1.0.0/16");
            paraMap.put("driverType","aws");
            paraMap.put("subnet1", "name+ &cidr+10.1.0.0/24&routesto+206.196.0.0/16,nextHop+internet\r\nfrom+vpn,to+0.0.0.0/0,nextHop+vpn\r\nto+72.24.24.0/24,nextHop+vpn");
            paraMap.put("subnet2", "name+ &cidr+10.1.1.0/24");
            paraMap.put("vm1", "vm1&1& & & & ");  //value format: "vm_name&subnet_index_number&image_type&instance_type&keypair_name&security_group_name"
            paraMap.put("vm2", "vm2&2& & & & ");  //put space if not mentioned
            paraMap.put("directConn", "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*?vlan=3023");
            //if not specified the vlan range, replace 3023 with any
            
            paraMap.remove("netCreate");
            paraMap.remove("template2");

            // Async setup
            request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            AsyncContext asyncCtx = request.startAsync();
            asyncCtx.addListener(new AppAsyncListener());
            asyncCtx.setTimeout(300000);

            ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

            executor.execute(new NetCreateWorker(asyncCtx, paraMap));
            
        }
        else if(paraMap.containsKey("template3")){ // AWS with VMs specified VM types
            // Add template data.
            paraMap.put("topoUri", "urn:ogf:network:aws.amazon.com:aws-cloud");
            paraMap.put("netCidr", "10.1.0.0/16");
            paraMap.put("driverType","aws");
            paraMap.put("subnet1", "name+ &cidr+10.1.0.0/24&routesto+206.196.0.0/16,nextHop+internet\r\nfrom+vpn,to+0.0.0.0/0,nextHop+vpn\r\nto+72.24.24.0/24,nextHop+vpn");
            paraMap.put("subnet2", "name+ &cidr+10.1.1.0/24");
            paraMap.put("vm1", "test_with_vm_types_1&1&ami-08111162&t2.micro& & ");  //value format: "vm_name&subnet_index_number&image_type&instance_type&keypair_name&security_group_name"
            paraMap.put("vm2", "test_with_vm_type_2&2&ami-fce3c696&t2.small&xi-aws-max-dev-key&geni");  //put space if not mentioned
            
            paraMap.remove("netCreate");
            paraMap.remove("template3");

            // Async setup
            request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            AsyncContext asyncCtx = request.startAsync();
            asyncCtx.addListener(new AppAsyncListener());
            asyncCtx.setTimeout(300000);

            ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

            executor.execute(new NetCreateWorker(asyncCtx, paraMap));
            
        }
        else if(paraMap.containsKey("template4")){ //OpenStack with 2 subnets and 1 VM
            // Add template data.
            paraMap.put("topoUri", "urn:ogf:network:openstack.com:openstack-cloud");
            paraMap.put("netCidr", "10.1.0.0/16");
            paraMap.put("driverType","ops");
            paraMap.put("subnet1", "name+ &cidr+10.0.0.0/24&routesto+0.0.0.0/0,nextHop+internet");
            paraMap.put("subnet2", "name+ &cidr+10.1.1.0/24");
            paraMap.put("vm1", "vm_OPS&1& &m1.medium&icecube_key&rains&msx1& & & & & ");
            //value format: "vm_name&subnet_index_number&image_type&instance_type&keypair_name&security_group_name&host&floating_IP&sriov_destination&sriov_mac_address&sriov_ip_address&sriov_routes"
            
            paraMap.remove("netCreate");
            paraMap.remove("template4");

            // Async setup
            request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            AsyncContext asyncCtx = request.startAsync();
            asyncCtx.addListener(new AppAsyncListener());
            asyncCtx.setTimeout(300000);

            ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

            executor.execute(new NetCreateWorker(asyncCtx, paraMap));
            
        }
        else if(paraMap.containsKey("template5")){ //OpenStack with 1 VM with sriov connection
            // Add template data.
            paraMap.put("topoUri", "urn:ogf:network:openstack.com:openstack-cloud");
            paraMap.put("netCidr", "10.1.0.0/16");
            paraMap.put("driverType","ops");
            paraMap.put("subnet1", "name+ &cidr+10.0.0.0/24&routesto+0.0.0.0/0,nextHop+internet");
            paraMap.put("subnet2", "name+ &cidr+10.1.1.0/24");
            paraMap.put("vm1", "vm_OPS&1& &m1.medium&icecube_key&rains&msx1&206.196.180.148&urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*&aa:bb:cc:00:00:12&10.10.0.1/30&to+192.168.0.0/24,next_hop+10.10.0.2\r\nto+206.196.179.0/24,next_hop+10.10.0.2");
            //value format: "vm_name&subnet_index_number&image_type&instance_type&keypair_name&security_group_name&host&floating_IP&sriov_destination&sriov_mac_address&sriov_ip_address&sriov_routes"
            
            paraMap.remove("netCreate");
            paraMap.remove("template4");

            // Async setup
            request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            AsyncContext asyncCtx = request.startAsync();
            asyncCtx.addListener(new AppAsyncListener());
            asyncCtx.setTimeout(300000);

            ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

            executor.execute(new NetCreateWorker(asyncCtx, paraMap));
            
        }
        else { // Custom Form Handling
            PreparedStatement prep = rains_conn.prepareStatement("SELECT driverEjbPath"
                    + " FROM driver_instance WHERE topologyUri = ?");
            prep.setString(1, paraMap.get("topoUri"));
            ResultSet rs1 = prep.executeQuery();
            rs1.next();
            String driverPath = rs1.getString(1);
            if (driverPath.contains("Aws") || driverPath.contains("aws")) {
                paraMap.put("driverType", "aws");
            } else if (driverPath.contains("OpenStack") || driverPath.contains("os")) {
                paraMap.put("driverType", "ops");
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
                        // Check for subroute existence.
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
                    } 
                    else if (paraMap.containsKey("subnet" + i + "-route-default")) {
                        if (!subnetString.contains("routes")) {
                            subnetString += "routes";
                        }
                        subnetString += "to+0.0.0.0/0,nextHop+internet";
                    } else {
                        if (subnetString.contains("routes")) {
                            subnetString = subnetString.substring(0, (subnetString.length() - 2));
                        }
                    }
                    paraMap.remove("subnet" + i + "-route-prop");
                    paraMap.remove("subnet" + i + "-route-default");

                    // Process VMs.
                    for (int j = 1; j < 10; j++) {
                        if (paraMap.get("driverType").equalsIgnoreCase("aws")) {
                            //value format: "vm_name&subnet_index_number&image_type&instance_type&keypair_name&security_group_name"
                            if (paraMap.containsKey("subnet" + i + "-vm" + j)) {
                                String VMString = "";

                                VMString += paraMap.get("subnet" + i + "-vm" + j);
                                VMString += "&" + i;

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-image")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-image");
                                } else {
                                    VMString += "& ";
                                }

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-instance")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-instance");
                                } else {
                                    VMString += "& ";
                                }

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-keypair")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-keypair");
                                } else {
                                    VMString += "& ";
                                }

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "security")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "security");
                                } else {
                                    VMString += "& ";
                                }

                                paraMap.put("vm" + j, VMString);
                            }
                        } else if (paraMap.get("driverType").equalsIgnoreCase("ops")) {
                            //value format: "vm_name&subnet_index_number&image_type&instance_type&keypair_name&security_group_name&host&floating_IP&sriov_destination&sriov_mac_address&sriov_ip_address&sriov_routes"
                            if (paraMap.containsKey("subnet" + i + "-vm" + j)) {
                                String VMString = "";

                                VMString += paraMap.get("subnet" + i + "-vm" + j);
                                VMString += "&" + i;

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-image")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-image");
                                } else {
                                    VMString += "& ";
                                }

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-instance")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-instance");
                                } else {
                                    VMString += "& ";
                                }

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-keypair")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-keypair");
                                } else {
                                    VMString += "& ";
                                }

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-security")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-security");
                                } else {
                                    VMString += "& ";
                                }

                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-host")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-host");
                                } else {
                                    VMString += "& ";
                                }
                                
                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-floating")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-floating");
                                } else {
                                    VMString += "& ";
                                }
                                
                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-sriov-dest")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-sriov-dest");
                                } else {
                                    VMString += "& ";
                                }
                                
                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-sriov-mac")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-sriov-mac");
                                } else {
                                    VMString += "& ";
                                }
                                
                                if (paraMap.containsKey("subnet" + i + "-vm" + j + "-sriov-ip")) {
                                    VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-sriov-ip");
                                } else {
                                    VMString += "& ";
                                }
                                
                                String vmRouteString = "";
                                for (int k = 1; k < 5; k++) {

//                                    if (paraMap.containsKey("subnet" + i + "-vm" + j + "-route" + k)) {
//                                        VMString += "&" + paraMap.get("subnet" + i + "-vm" + j + "-");
//                                    } else {
//                                        VMString += "& ";
//                                    }

                                    if (paraMap.containsKey("subnet" + i + "-vm" + j + "-route" + k + "-to")) {
                                        vmRouteString += "to+" + paraMap.get("subnet" + i + "-vm" + j + "-route" + k + "-to") + ",";

                                        if (paraMap.containsKey("subnet" + i + "-vm" + j + "-route" + k + "-from")) {
                                            vmRouteString += "from+" + paraMap.get("subnet" + i + "-vm" + j + "-route" + k + "-from") + ",";
                                        }
                                        if (paraMap.containsKey("subnet" + i + "-vm" + j + "-route" + k + "-next")) {
                                            vmRouteString += "next_hop+" + paraMap.get("subnet" + i + "-vm" + j + "-route" + k + "-next");
                                        }
                                        vmRouteString += "\r\n";
                                    }

                                    paraMap.remove("subnet" + i + "-vm" + j + "-route" + k + "-to");
                                    paraMap.remove("subnet" + i + "-vm" + j + "-route" + k + "-from");
                                    paraMap.remove("subnet" + i + "-vm" + j + "-route" + k + "-next");
                                }
                                
                                if (!vmRouteString.isEmpty()) {
                                    VMString += "&" + vmRouteString;
                                } else {
                                    VMString += "& ";
                                }

                                paraMap.put("vm" + j, VMString);
                                
                            }
                        }

                        paraMap.remove("subnet" + i + "-vm" + j);
                        paraMap.remove("subnet" + i + "-vm" + j + "-image");
                        paraMap.remove("subnet" + i + "-vm" + j + "-instance");
                        paraMap.remove("subnet" + i + "-vm" + j + "-keypair");
                        paraMap.remove("subnet" + i + "-vm" + j + "-security");
                        paraMap.remove("subnet" + i + "-vm" + j + "-host");
                        paraMap.remove("subnet" + i + "-vm" + j + "-floating");
                        paraMap.remove("subnet" + i + "-vm" + j + "-sriov-dest");
                        paraMap.remove("subnet" + i + "-vm" + j + "-sriov-ip");
                        paraMap.remove("subnet" + i + "-vm" + j + "-sriov-mac");
                    }

                    paraMap.remove("subnet" + i + "-cidr");
                    paraMap.remove("subnet" + i + "-name");

                    paraMap.put("subnet" + i, subnetString);
                }
            }
            paraMap.put("netRoutes", "to+0.0.0.0/0,nextHop+internet");

            // Parse direct connect.
            if (paraMap.containsKey("conn-dest")) {
                String connString = paraMap.get("conn-dest");
                if (paraMap.containsKey("conn-vlan")) {
                    connString += "?vlan=" + paraMap.get("conn-vlan");
                } else {
                    connString += "?vlan=any";
                }            
                paraMap.put("directConn", connString);
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
    
    public String createFlow(HttpServletRequest request,HashMap<String, String> paraMap) throws SQLException {
        for (Object Key : paraMap.keySet().toArray()){
            if (paraMap.get((String) Key).isEmpty()){
                paraMap.remove((String) Key);
            }
        }
        
        Connection rains_conn;
        Properties rains_connectionProps = new Properties();
        rains_connectionProps.put("user","root");
        rains_connectionProps.put("password","root");
        
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

        } 
                else {
                paraMap.remove("userID");
                paraMap.remove("custom");
                paraMap.remove("fl2pCreate");
            //Process each link
                
                for(Map.Entry<String,String>entry : paraMap.entrySet())
                {
                    System.out.println(entry.getKey()+entry.getValue());
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

    public String createConnection(HttpServletRequest request, HashMap<String, String> paraMap) throws SQLException {
        for (Object key : paraMap.keySet().toArray()) {
            if (paraMap.get((String) key).isEmpty()) {
                paraMap.remove((String) key);
            }
        }
        
        Connection rains_conn;
        Properties rains_connectionProps = new Properties();
        rains_connectionProps.put("user","root");
        rains_connectionProps.put("password","root");
        
        rains_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb",
                rains_connectionProps);
        
        if (paraMap.containsKey("template1")) {
            //paraMap.put("driverType", "aws");
            paraMap.put("linkUri1", "urn:ogf:network:vo1.maxgigapop.net:link=conn1");
            paraMap.put("conn1", "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*&vlan_tag+3021-3029\r\nurn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*&vlan_tag+3021-3029");

            paraMap.remove("template1");
            paraMap.remove("dncCreate");
            
            servBean.createConnection(paraMap);

        } else if (paraMap.containsKey("template2")) {
            //paraMap.put("driverType", "aws");
            paraMap.put("linkUri1", "urn:ogf:network:vo1.maxgigapop.net:link=conn1");
            paraMap.put("conn1", "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*& vlan_tag+3021-3029\r\nc&vlan_tag+3021-3029");
            paraMap.put("linkUri2", "urn:ogf:network:vo1.maxgigapop.net:link=conn2");
            paraMap.put("conn2", "urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*& vlan_tag+3021-3029\r\nurn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*&vlan_tag+3021-3029");

            paraMap.remove("template2");
            paraMap.remove("dncCreate");
            
            servBean.createConnection(paraMap);

        } 
        else {
            //Process each link
                for (int i = 1; i < 10; i++) {
                    //if(paraMap.containsKey("linkUri"+i))
                    if(paraMap.containsKey("link"+ i +"-src")){
                    String linkString = "";
                    if (paraMap.containsKey("link" + i + "-src")) {
                        linkString = paraMap.get("link" + i + "-src") + "&";
                     }
                    if (paraMap.containsKey("link" + i + "-src-vlan")) {
                        linkString += "vlan_tag+"+paraMap.get("link" + i + "-src-vlan");
                    }
                    if (paraMap.containsKey("link" + i + "-des")) {
                        linkString += "\r\n" + paraMap.get("link" + i + "-des") + "&";
                    }
                    if (paraMap.containsKey("link" + i + "-des-vlan")) {
                        linkString += "vlan_tag+"+paraMap.get("link" + i + "-des-vlan");
                    }

                    paraMap.remove("link" + i + "-src");
                    paraMap.remove("link" + i + "-src-vlan");
                    paraMap.remove("link" + i + "-des");
                    paraMap.remove("link" + i + "-des-vlan");

                    paraMap.put("conn" + i, linkString);
                }
                }
        
                paraMap.remove("userID");
                paraMap.remove("custom");
                paraMap.remove("dncCreate");
                
                for(Map.Entry<String,String>entry : paraMap.entrySet())
                {
                    System.out.println(entry.getKey()+entry.getValue());
                }

        // Async setup 
                request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
                AsyncContext asyncCtx = request.startAsync();
                asyncCtx.addListener(new AppAsyncListener());
                asyncCtx.setTimeout(60000);

                ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");

                executor.execute(new DNCWorker(asyncCtx, paraMap));
        }
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
