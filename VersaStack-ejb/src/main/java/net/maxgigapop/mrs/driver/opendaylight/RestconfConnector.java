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

import net.maxgigapop.mrs.driver.onosystem.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import net.maxgigapop.mrs.common.DriverUtil;
import org.json.simple.JSONArray;

/**
 *
 * @author diogo
 */
public class RestconfConnector {
    private static final Logger logger = Logger.getLogger(OnosRESTDriver.class.getName());

    //pull network topology
    public JSONObject getNetworkTopology(String subsystemBaseUrl, String username, String password) {
        try  {
            URL url = new URL(subsystemBaseUrl + "/operational/network-topology:network-topology"); 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String[] response = DriverUtil.executeHttpMethod(username, password, conn, "GET", null);
            if (!response[1].equals("200")) {
                throw new EJBException("RestconfConnector:getNetworkTopology failed with HTTP return code:"+response[1]);
            }
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response[0]);
            return jsonObject;
        } catch (Exception ex) {
            throw new EJBException("RestconfConnector:getNetworkTopology failed.", ex);
        }
    }
    
    //pull configured flows
    public JSONObject getConfigFlows(String subsystemBaseUrl, String username, String password) {
        try  {
            URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes"); 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String[] response = DriverUtil.executeHttpMethod(username, password, conn, "GET", null);
            if (!response[1].equals("200")) {
                throw new EJBException("RestconfConnector:getNetworkTopology failed with HTTP return code:"+response[1]);
            }
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response[0]);
            return jsonObject;
        } catch (Exception ex) {
            throw new EJBException("RestconfConnector:getConfigFlows failed.", ex);
        }
    }
    
    //push to add flow
    public void pushAddFlow(String subsystemBaseUrl, String username, String password, String nodeId, String tableId, String flowId, List<String> matches, List<String> actions) {
        try  {
            URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes/node/"+nodeId+"/flow-node-inventory:table/"+tableId); 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String data = this.makeFlowData(flowId, tableId, matches, actions);
            String[] response = DriverUtil.executeHttpMethod(username, password, conn, "POST", data);
            if (!response[1].equals("200") && !response[1].equals("204")) {
                throw new EJBException("RestconfConnector:pushAddFlow failed with HTTP return code:"+response[1]);
            }
        } catch (Exception ex) {
            throw new EJBException("RestconfConnector:pushAddFlow failed.", ex);
        }
    }

    //push to modify flow
    public void pushModFlow(String subsystemBaseUrl, String username, String password, String nodeId, String tableId, String flowId, List<String> matches, List<String> actions) {
        try  {
            URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes"+nodeId+"/flow-node-inventory:table/"+tableId+"/flow/"+flowId); 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String data = this.makeFlowData(flowId, tableId, matches, actions);
            String[] response = DriverUtil.executeHttpMethod(username, password, conn, "GET", data);
            if (!response[1].equals("200")) {
                throw new EJBException("RestconfConnector:pushModFlow failed with HTTP return code:"+response[1]);
            }
        } catch (Exception ex) {
            throw new EJBException("RestconfConnector:pushModFlow failed.", ex);
        }
    }
    
    //push to delete flow
    public void pushDeleteFlow(String subsystemBaseUrl, String username, String password, String nodeId, String tableId, String flowId) {
        try  {
            URL url = new URL(subsystemBaseUrl + "/config/opendaylight-inventory:nodes"+nodeId+"/flow-node-inventory:table/"+tableId+"/flow/"+flowId); 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String[] response = DriverUtil.executeHttpMethod(username, password, conn, "DELETE", null);
            if (response[1].equals("404")) {
                logger.warning("RestconfConnector:pushDeleteFlow with unfound flow: "+flowId);
            }
            if (!response[1].equals("200")) {
                throw new EJBException("RestconfConnector:pushDeleteFlow failed with HTTP return code:"+response[1]);
            }
        } catch (Exception ex) {
            throw new EJBException("RestconfConnector:pushDeleteFlow failed.", ex);
        }
    }

    private String makeFlowData(String id, String table, List<String> matches, List<String> actions) {
        JSONObject jData = new JSONObject();
        jData.put("id", id);
        jData.put("table_id", table);
        if (matches != null && !matches.isEmpty()) {
            JSONObject jMatch = new JSONObject();
            jData.put("match", jMatch);
            for (String match: matches) {
                String[] tv = match.split(":");
                if (tv[0].equals("in_port") && tv.length == 2) {
                    jMatch.put("in-port", tv[1]);
                } else if (tv[0].startsWith("dl_") && !tv[0].equals("dl_vlan") && tv.length == 2) {
                    if (!jMatch.containsKey("ethernet-match")) {
                        jMatch.put("ethernet-match", new JSONObject());
                    }
                    JSONObject jMatchEther = (JSONObject)jMatch.get("ethernet-match");
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
                String[] tv = actions.get(order).split(":");
                JSONObject jAction = new JSONObject();
                jAction.put("order", Integer.toString(order));
                if (tv[0].equalsIgnoreCase("drop")) {
                    break;
                } else if (tv[0].equalsIgnoreCase("output")) {
                    JSONObject jOutputActoin = new JSONObject();
                    jAction.put("output-action", jOutputActoin);
                    jOutputActoin.put("output-node-connector", tv[1]);
                    jOutputActoin.put("max-length", 65535); // hardcoded
                } // more action types ?
                jActions.add(jAction);
            }
        }
        return jData.toJSONString();
    }
}
