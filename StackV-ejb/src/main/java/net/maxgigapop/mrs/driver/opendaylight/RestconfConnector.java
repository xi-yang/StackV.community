/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */
package net.maxgigapop.mrs.driver.opendaylight;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import net.maxgigapop.mrs.common.DriverUtil;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;

/**
 *
 * @author diogo
 */
public class RestconfConnector {

    public static final StackLogger logger = OpenflowRestconfDriver.logger;

    //pull network topology
    public JSONObject getNetworkTopology(String subsystemBaseUrl, String username, String password) {
        String method = "getNetworkTopology";
        try {
            URL url = new URL(subsystemBaseUrl + "/operational/network-topology:network-topology");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String[] response = DriverUtil.executeHttpMethod(username, password, conn, "GET", null);
            if (!response[1].equals("200")) {
                throw logger.error_throwing(method, "failed with HTTP return code:" + response[1]);
            }
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response[0]);
            return jsonObject;
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
    }

    //pull configured flows
    public JSONObject getConfigFlows(String subsystemBaseUrl, String username, String password) throws Exception {
        String method = "getConfigFlows";
        URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String[] response = DriverUtil.executeHttpMethod(username, password, conn, "GET", null);
        if (!response[1].equals("200")) {
            throw logger.error_throwing(method, "failed with HTTP return code:" + response[1]);
        }
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(response[0]);
        return jsonObject;
    }

    //push to add flow
    public void pushAddFlow(String subsystemBaseUrl, String username, String password, String nodeId, String tableId,
            String flowId, List<String> matches, List<String> actions) throws Exception {
        String method = "pushAddFlow";
        URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes/node/" + nodeId + "/flow-node-inventory:table/" + tableId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String data = this.makeFlowData(flowId, tableId, matches, actions);
        String[] response = DriverUtil.executeHttpMethod(username, password, conn, "POST", data);
        if (!response[1].equals("200") && !response[1].equals("204")) {
            throw logger.error_throwing(method, "failed with HTTP return code:" + response[1]);
        }
    }

    //push to modify flow
    public void pushModFlow(String subsystemBaseUrl, String username, String password, String nodeId, String tableId,
            String flowId, List<String> matches, List<String> actions) throws Exception {
        String method = "pushModFlow";
        URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes/node/" + nodeId + "/flow-node-inventory:table/" + tableId + "/flow/" + flowId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String data = this.makeFlowData(flowId, tableId, matches, actions);
        String[] response = DriverUtil.executeHttpMethod(username, password, conn, "PUT", data);
        if (!response[1].equals("201")) {
            throw logger.error_throwing(method, "failed with HTTP return code:" + response[1]);
        }

    }

    //push to delete flow
    public void pushDeleteFlow(String subsystemBaseUrl, String username, String password, String nodeId, String tableId,
            String flowId) throws Exception {
        String method = "pushDeleteFlow";
        URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes/node/" + nodeId + "/flow-node-inventory:table/" + tableId + "/flow/" + flowId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String[] response = DriverUtil.executeHttpMethod(username, password, conn, "DELETE", null);
        if (response[1].equals("404")) {
            logger.warning(method, "failed with unfound flow: " + flowId);
        }
        if (!response[1].equals("200")) {
            throw logger.error_throwing(method, "failed with HTTP return code:" + response[1]);
        }
    }

    private String makeFlowData(String id, String table, List<String> matches, List<String> actions) {
        JSONObject jData = new JSONObject();
        jData.put("id", id);
        jData.put("table_id", table);
        if (matches != null && !matches.isEmpty()) {
            JSONObject jMatch = new JSONObject();
            jData.put("match", jMatch);
            for (String match : matches) {
                String[] tv = match.split("=");
                if (tv[0].equals("in_port") && tv.length == 2) {
                    jMatch.put("in-port", tv[1]);
                } else if (tv[0].startsWith("dl_") && !tv[0].equals("dl_vlan") && tv.length == 2) {
                    if (!jMatch.containsKey("ethernet-match")) {
                        jMatch.put("ethernet-match", new JSONObject());
                    }
                    JSONObject jMatchEther = (JSONObject) jMatch.get("ethernet-match");
                    JSONObject jMatchEtherData = new JSONObject();
                    if (tv[0].equals("dl_type")) {
                        jMatchEther.put("ethernet-type", jMatchEtherData);
                        jMatchEtherData.put("type", tv[1]);
                    } else if (tv[0].equals("dl_src")) {
                        jMatchEther.put("ethernet-source", jMatchEtherData);
                        jMatchEtherData.put("address", tv[1]);
                    } else if (tv[0].equals("dl_dst")) {
                        jMatchEther.put("ethernet-destination", jMatchEtherData);
                        jMatchEtherData.put("address", tv[1]);
                    }
                } else if (tv[0].equals("dl_vlan") && tv.length == 2) {
                    JSONObject jMatchVlan = new JSONObject();
                    jMatch.put("vlan-match", jMatchVlan);
                    JSONObject jMatchVlanId = new JSONObject();
                    jMatchVlan.put("vlan-id", jMatchVlanId);
                    jMatchVlanId.put("vlan-id-present", true);
                    if (!tv[1].equals("any")) {
                        jMatchVlanId.put("vlan-id", tv[1]);
                    }
                } else if (tv[0].equals("nw_prot") && tv.length == 2) {
                    JSONObject jMatchIp = new JSONObject();
                    jMatch.put("ip-match", jMatchIp);
                    jMatchIp.put("ip-protocol", tv[1]);
                } else if (tv[0].equals("nw_src") && tv.length == 2) {
                    // assuming ipv4 for now. Will add ipv6 after parsing the address
                    jMatch.put("ipv4-source", tv[1]);
                } else if (tv[0].equals("nw_dst") && tv.length == 2) {
                    // assuming ipv4 for now. Will add ipv6 after parsing the address
                    jMatch.put("ipv4-destination", tv[1]);
                } else if (tv[0].equals("tp_src") && tv.length == 2) {
                    // assuming  tcp  matching. Will diff after check ip protocol
                    jMatch.put("tcp-source-port", tv[1]);
                    //jMatch.put("udp-source-port", tv[1]);
                } else if (tv[0].equals("tp_dst") && tv.length == 2) {
                    // assuming  tcp  matching. Will diff after check ip protocol
                    jMatch.put("tcp-destination-port", tv[1]);
                    //jMatch.put("udp-destination-port", tv[1]);
                }
            }
        }
        if (actions != null && !actions.isEmpty()) {
            JSONObject jInstructions = new JSONObject();
            JSONObject jInstruction = new JSONObject();
            JSONObject jApplyActions = new JSONObject();
            JSONArray jActions = new JSONArray();
            jData.put("instructions", jInstructions);
            jInstructions.put("instruction", jInstruction);
            jInstruction.put("order", "0");
            jInstruction.put("apply-actions", jApplyActions);
            jApplyActions.put("action", jActions);
            for (int order = 0; order < actions.size(); order++) {
                String[] tv = actions.get(order).split("=");
                JSONObject jAction = new JSONObject();
                jAction.put("order", Integer.toString(order));
                if (tv[0].equalsIgnoreCase("drop")) {
                    break;
                } else if (tv[0].equalsIgnoreCase("output")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("output-action", jOutputActoin);
                    jOutputActoin.put("output-node-connector", tv[1]);
                    jOutputActoin.put("max-length", 65535); // hardcoded
                } else if (tv[0].equalsIgnoreCase("strip_vlan")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("pop-vlan-action", jOutputActoin);
                } else if (tv[0].equalsIgnoreCase("push_vlan")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("push-vlan-action", jOutputActoin);
                    jOutputActoin.put("ethernet-type", "33024"); //ethernet type (alwasy 0x88a8)
                } else if (tv[0].equalsIgnoreCase("mod_vlan_vid")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("set-field", jOutputActoin);
                    JSONObject jVlanMatch = new JSONObject();
                    jOutputActoin.put("vlan-match", jVlanMatch);
                    JSONObject jVlanId = new JSONObject();
                    jVlanMatch.put("vlan-id", jVlanId);
                    if (!tv[1].equalsIgnoreCase("any")) {
                        jVlanId.put("vlan-id", tv[1]);
                    }
                    jVlanId.put("vlan-id-present", "true");
                } else if (tv[0].equalsIgnoreCase("mod_vlan_pcp")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("set-field", jOutputActoin);
                    JSONObject jVlanMatch = new JSONObject();
                    jOutputActoin.put("vlan-match", jVlanMatch);
                    jVlanMatch.put("vlan-pcp", tv[1]);
                } else if (tv[0].startsWith("mod_dl")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("set-field", jOutputActoin);
                    JSONObject jMatchEther = new JSONObject();
                    jOutputActoin.put("ethernet-match", jMatchEther);
                    JSONObject jMatchEtherData = new JSONObject();
                    if (tv[0].equals("mod_dl_type")) {
                        jMatchEther.put("ethernet-type", jMatchEtherData);
                        jMatchEtherData.put("type", tv[1]);
                    } else if (tv[0].equals("mod_dl_src")) {
                        jMatchEther.put("ethernet-source", jMatchEtherData);
                        jMatchEtherData.put("address", tv[1]);
                    } else if (tv[0].equals("mod_dl_dst")) {
                        jMatchEther.put("ethernet-destination", jMatchEtherData);
                        jMatchEtherData.put("address", tv[1]);
                    }
                } else if (tv[0].equalsIgnoreCase("mod_nw_src")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("set-field", jOutputActoin);
                    jOutputActoin.put("ipv4-source", tv[1]);
                } else if (tv[0].equalsIgnoreCase("mod_nw_dst")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("set-field", jOutputActoin);
                    jOutputActoin.put("ipv4-destination", tv[1]);
                } else if (tv[0].equalsIgnoreCase("mod_tp_src")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("set-field", jOutputActoin);
                    jOutputActoin.put("tcp-source-port", tv[1]);  // only tcp for now
                } else if (tv[0].equalsIgnoreCase("mod_tp_dst")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("set-field", jOutputActoin);
                    jOutputActoin.put("tcp-destination-port", tv[1]);  //only tcp for now
                } // and more, e.g. MPLS label swap, dscp, ttl etc.
                // https://wiki.opendaylight.org/view/Editing_OpenDaylight_OpenFlow_Plugin:End_to_End_Flows:Example_Flows
                // http://www.brocade.com/content/html/en/sdn-controller-applications/bfm/3.1.0/GUID-3D7035AF-94DA-47BD-A595-A555B46FE037.html
                jActions.add(jAction);
            }
        }
        return "{\"flow\":[" + jData.toJSONString() + "]}";
    }
}
