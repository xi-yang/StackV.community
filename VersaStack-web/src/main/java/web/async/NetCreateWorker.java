package web.async;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.AsyncContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class NetCreateWorker implements Runnable {

    private AsyncContext asyncContext;
    private HashMap<String, String> paramMap;
    private final String host = "http://localhost:8080/VersaStack-web/restapi";

    public NetCreateWorker() {
    }

    public NetCreateWorker(AsyncContext asyncCtx, HashMap<String, String> paramMap) {
        this.asyncContext = asyncCtx;
        this.paramMap = paramMap;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        System.out.println("Network Creation Worker Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());

        System.out.println("Async Supported? " + asyncContext.getRequest().isAsyncSupported());

        createNetwork(paramMap);

        long endTime = System.currentTimeMillis();
        System.out.println("Network Creation Worker End::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId() + "::Time Taken="
                + (endTime - startTime) + " ms.");

        asyncContext.complete();
    }

    private int createNetwork(Map<String, String> paraMap) {
        String topoUri = null;
        String driverType = null;
        String netCidr = null;
        List<String> subnets = new ArrayList<>();
        List<String> vmList = new ArrayList<>();
        boolean gwVpn = false;

        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("driverType")) {
                driverType = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("topoUri")) {
                topoUri = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase("netCidr")) {
                netCidr = entry.getValue();
            } else if (entry.getKey().contains("subnet")) {
                subnets.add(entry.getValue());
            } else if (entry.getKey().contains("vm")) {
                vmList.add(entry.getValue());
            }
            //example for vm : 1&imageType&instanceType&volumeSize&batch
        }

        JSONObject network = new JSONObject();
        network.put("type", "internal");
        network.put("cidr", netCidr);
        network.put("parent", topoUri);

        JSONArray subnetsJson = new JSONArray();
        //routing problem solved. need testing.
        for (String net : subnets) {
            String[] netPara = net.split("&");
            JSONObject subnetValue = new JSONObject();
            for (String para : netPara) {
                if (para.startsWith("routes")) {
                    String[] route = para.substring(6).split("\r\n");
                    JSONArray routesArray = new JSONArray();
                    for (String r : route) {
                        String[] routePara = r.split(",");
                        JSONObject jsonRoute = new JSONObject();
                        for (String rp : routePara) {
                            String[] keyValue = rp.split("\\+");
                            jsonRoute.put(keyValue[0], keyValue[1]);
                            if (keyValue[1].contains("vpn")) {
                                gwVpn = true;
                            }
                        }
                        routesArray.add(jsonRoute);
                    }
                    subnetValue.put("routes", routesArray);
                } else {
                    String[] keyValue = para.split("\\+");
                    subnetValue.put(keyValue[0], keyValue[1]);
                }
            }
            subnetsJson.add(subnetValue);
        }
        network.put("subnets", subnetsJson);

        JSONArray routesJson = new JSONArray();
        JSONObject routesValue = new JSONObject();
        routesValue.put("to", "0.0.0.0/0");
        routesValue.put("nextHop", "internet");
        routesJson.add(routesValue);
        network.put("routes", routesJson);

        JSONArray gatewaysJson = new JSONArray();
        JSONObject temp = new JSONObject();
        temp.put("type", "internet");
        gatewaysJson.add(temp);
        if (gwVpn) {
            JSONObject gatewayValue = new JSONObject();
            gatewayValue.put("type", "vpn");
            gatewaysJson.add(gatewayValue);
        }
        network.put("gateways", gatewaysJson);

        String svcDelta = "<serviceDelta>\n<uuid>" + UUID.randomUUID().toString()
                + "</uuid>\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"
                + "\n\n<modelAddition>\n"
                + "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
                + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
                + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
                + "@prefix rdf:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n"
                + "@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .\n\n"
                + "&lt;" + topoUri + ":vpc=abstract&gt;\n"
                + "    a                         nml:Topology ;\n"
                + "    spa:dependOn &lt;x-policy-annotation:action:create-" + driverType + "-vpc&gt; .\n\n";

        if (vmList.isEmpty()) {
            svcDelta += "&lt;x-policy-annotation:action:create-" + driverType + "-vpc&gt;\n"
                    + "    a           spa:PolicyAction ;\n"
                    + "    spa:type     \"MCE_VirtualNetworkCreation\" ;\n"
                    + "    spa:importFrom &lt;x-policy-annotation:data:vpc-criteria&gt; ;\n"
                    + "    spa:exportTo &lt;x-policy-annotation:data:vpc-criteriaexport&gt; .\n\n"
                    + "&lt;x-policy-annotation:data:vpc-criteria&gt;\n"
                    + "    a            spa:PolicyData;\n"
                    + "    spa:type     nml:Topology;\n"
                    + "    spa:value    \"\"\"" + network.toString().replace("\\", "")
                    + "\"\"\".\n\n&lt;x-policy-annotation:data:vpc-criteriaexport&gt;\n"
                    + "    a            spa:PolicyData;\n\n"
                    + "</modelAddition>\n\n"
                    + "</serviceDelta>";
        } else {
            String exportTo = "";
            for (String vm : vmList) {
                String[] vmPara = vm.split("&");
                //1:subnet #
                //2:image Type
                //3:instance Type
                //4:volume size
                //5:batch
                svcDelta += "&lt;" + topoUri + ":vpc=abstract:vm" + vmPara[0] + "&gt;\n"
                        + "    a                         nml:Node ;\n"
                        + "    nml:hasBidirectionalPort   &lt;" + topoUri + ":vpc=abstract:vm" + vmPara[0] + ":eth0&gt; ;\n"
                        + "    spa:dependOn &lt;x-policy-annotation:action:create-vm" + vmPara[0] + "&gt;.\n\n"
                        + "&lt;" + topoUri + ":vpc=abstract:vm" + vmPara[0] + ":eth0&gt;\n"
                        + "    a            nml:BidirectionalPort;"
                        + "    spa:dependOn &lt;x-policy-annotation:action:create-vm" + vmPara[0] + "&gt;.\n\n"
                        + "&lt;x-policy-annotation:action:create-vm" + vmPara[0] + "&gt;\n"
                        + "    a            spa:PolicyAction ;\n"
                        + "    spa:type     \"MCE_VMFilterPlacement\" ;\n"
                        + "    spa:dependOn &lt;x-policy-annotation:action:create-" + driverType + "-vpc&gt;\n"
                        + "    spa:importFrom ";
                String subnetCriteria = "&lt;x-policy-annotation:data:vpc-subnet-vm" + vmPara[0] + "-criteria&gt;";
                exportTo += subnetCriteria + ", ";
                int sub = Integer.valueOf(vmPara[0]) - 1;
                svcDelta += subnetCriteria + ".\n\n"
                        + subnetCriteria + "\n    a            spa:PolicyData;\n"
                        + "    spa:type     \"JSON\";\n    spa:format    \"\"\"{ "
                        + "\"place_into\": \"%%$.subnets[" + sub + "].uri%%\"}\"\"\" .\n\n";
            }
            svcDelta += "&lt;x-policy-annotation:action:create-" + driverType + "-vpc&gt;\n"
                    + "    a           spa:PolicyAction ;\n"
                    + "    spa:type     \"MCE_VirtualNetworkCreation\" ;\n"
                    + "    spa:importFrom &lt;x-policy-annotation:data:vpc-criteria&gt; ;\n"
                    + "    spa:exportTo " + exportTo.substring(0, (exportTo.length() - 2)) + " .\n\n"
                    + "&lt;x-policy-annotation:data:vpc-criteria&gt;\n"
                    + "    a            spa:PolicyData;\n"
                    + "    spa:type     nml:Topology;\n"
                    + "    spa:value    \"\"\"" + network.toString().replace("\\", "")
                    + "\"\"\".\n\n&lt;x-policy-annotation:data:vpc-criteriaexport&gt;\n"
                    + "    a            spa:PolicyData;\n\n"
                    + "</modelAddition>\n\n"
                    + "</serviceDelta>";
        }

        String siUuid;
        String result;
        try {
            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            siUuid = this.executeHttpMethod(url, connection, "GET", null);
            if (siUuid.length() != 36) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s", host, siUuid));
            HttpURLConnection compile = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, compile, "POST", svcDelta);
            if (!result.contains("referenceVersion")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/propagate", host, siUuid));
            HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, propagate, "PUT", null);
            if (!result.equals("PROPAGATED")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/commit", host, siUuid));
            HttpURLConnection commit = (HttpURLConnection) url.openConnection();
            result = this.executeHttpMethod(url, commit, "PUT", null);
            if (!result.equals("COMMITTED")) {
                return 2;//Error occurs when interacting with back-end system
            }
            url = new URL(String.format("%s/service/%s/status", host, siUuid));
            while (true) {
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                result = this.executeHttpMethod(url, status, "GET", null);
                if (result.equals("READY")) {
                    return 0;//create network successfully
                } else if (!result.equals("COMMITTED")) {
                    return 3;//Fail to create network
                }
                sleep(5000);//wait for 5 seconds and check again later
            }
        } catch (Exception e) {
            return 1;//connection error
        }
    }

    /**
     * Executes HTTP Request.
     *
     * @param url destination url
     * @param conn connection object
     * @param method request method
     * @param body request body
     * @return response string.
     * @throws IOException
     */
    public String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-type", "application/xml");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }

        StringBuilder responseStr;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }
        return responseStr.toString();
    }
}
