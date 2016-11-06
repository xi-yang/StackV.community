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
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;
import net.maxgigapop.mrs.common.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import com.jayway.jsonpath.JsonPath;
import javax.ejb.EJBException;

//TODO Escape \:


public class OpenflowModelBuilder {
    private static final Logger logger = Logger.getLogger(OnosRESTDriver.class.getName());
    
    public static String URI_node(String prefix, String id) {
        return prefix+":node="+id;
    }
    
    public static String URI_port(String prefix, String id) {
        return prefix+":port="+id;
    }

    public static String URI_port(String prefix, String node, String id) {
        return prefix+":node="+node+":port="+id;
    }

    public static String URI_flow(String prefix, String id) {
        if (id.startsWith("urn:")) {
            return id;
        }
        return prefix+":flow="+id;
    }

    public static String URI_match(String prefix, String id) {
        return prefix+":match="+id;
    }

    public static String URI_match(String prefix, String flow, String id) {
        if (flow.startsWith("urn:")) {
            return flow+":match="+id;
        }
        return prefix+":flow="+flow+":match="+id;
    }
    
    public static String URI_action(String prefix, String id) {
        return prefix+":action="+id;
    }

    public static String URI_action(String prefix, String flow, String id) {
        if (flow.startsWith("urn:")) {
            return flow+":action="+id;
        }
        return prefix+":flow="+flow+":action="+id;
    }
    
    public static OntModel createOntology(String topologyURI, String subsystemBaseUrl, String username, String password) {
        //create model object
        OntModel model = ModelUtil.newMrsOntModel(topologyURI);
        Resource resTopo = RdfOwl.createResource(model, topologyURI, Nml.Topology);
        //retrieve ODL information into model
        RestconfConnector restconf = new RestconfConnector();
        JSONObject jsonTopology = restconf.getNetworkTopology(subsystemBaseUrl, username, password);
        try {
            Object r = JsonPath.parse(jsonTopology).read("$.network-topology..node");
            if (r != null && r instanceof net.minidev.json.JSONArray && !((net.minidev.json.JSONArray)r).isEmpty()) {
                r = ((net.minidev.json.JSONArray)r).get(0);
                for (Object o1: (JSONArray)r) {
                    JSONObject j1 = (JSONObject)o1;
                    if(!j1.containsKey("node-id")) {
                        continue;
                    }
                    String nodeId = (String)j1.get("node-id");
                    String nodeIdEsc = DriverUtil.addressUriEscape(nodeId);
                    Resource resNode = RdfOwl.createResource(model, URI_node(topologyURI, nodeIdEsc), Nml.Node);
                    model.add(model.createStatement(resTopo, Nml.hasNode, resNode));
                    model.add(model.createStatement(resNode, Nml.name, nodeId));
                    Resource resOpenFlow = RdfOwl.createResource(model, resNode.getURI() + ":openflow" , Mrs.OpenflowService);
                    model.add(model.createStatement(resNode, Nml.hasService, resOpenFlow));
                    Resource resFlowTable = RdfOwl.createResource(model, resOpenFlow.getURI()+":table=0", Mrs.FlowTable);
                    model.add(model.createStatement(resFlowTable, Nml.name, "0"));
                    model.add(model.createStatement(resOpenFlow, Mrs.providesFlowTable, resFlowTable));
                    if (j1.containsKey("termination-point")) {
                        for (Object o2 : (JSONArray) j1.get("termination-point")) {
                            JSONObject j2 = (JSONObject) o2;
                            if (!j2.containsKey("tp-id")) {
                                continue;
                            }
                            String portId = (String) j2.get("tp-id");
                            String portIdEsc = DriverUtil.addressUriEscape(portId);
                            Resource resPort = RdfOwl.createResource(model, URI_port(resNode.getURI(), portIdEsc), Nml.BidirectionalPort);
                            model.add(model.createStatement(resPort, Nml.name, portId));
                            model.add(model.createStatement(resNode, Nml.hasBidirectionalPort, resPort));
                            model.add(model.createStatement(resOpenFlow, Nml.hasBidirectionalPort, resPort));
                        }
                    }
                    if (j1.containsKey("host-tracker-service:addresses")) {
                        JSONObject jAddrs = (JSONObject) ((JSONArray) j1.get("host-tracker-service:addresses")).get(0);
                        if (jAddrs.containsKey("mac")) {
                            String macAddr = (String)jAddrs.get("mac");
                            String macAddrEsc = DriverUtil.addressUriEscape(macAddr);
                            Resource resNodeMac = RdfOwl.createResource(model, resNode.getURI()+":mac="+macAddrEsc, Mrs.NetworkAddress);
                            model.add(model.createStatement(resNode, Mrs.hasNetworkAddress, resNodeMac));
                            model.add(model.createStatement(resNodeMac, Mrs.type, "mac-address"));
                            model.add(model.createStatement(resNodeMac, Mrs.value, macAddr));
                        }
                        if (jAddrs.containsKey("ip")) {
                            String ipAddr = (String)jAddrs.get("ip");
                            String ipAddrEsc = DriverUtil.addressUriEscape(ipAddr);
                            Resource resNodeIp = RdfOwl.createResource(model, resNode.getURI()+":ip="+ipAddrEsc, Mrs.NetworkAddress);
                            model.add(model.createStatement(resNode, Mrs.hasNetworkAddress, resNodeIp));
                            model.add(model.createStatement(resNodeIp, Mrs.type, "ip-address"));
                            model.add(model.createStatement(resNodeIp, Mrs.value, ipAddr));
                        }
                    }
                }
            } else {
                throw new EJBException(String.format("OpenflowModelBuilder.createOntology failed to retrieve topology nodes from jsonTopology."));
            }
        } catch (Exception ex) {
            throw new EJBException(String.format("OpenflowModelBuilder.createOntology failed to parse the jsonTopology for nodes.", ex));
        }
        try {
            Object r = JsonPath.parse(jsonTopology).read("$.network-topology..link");
            if (r != null && r instanceof net.minidev.json.JSONArray && !((net.minidev.json.JSONArray)r).isEmpty()) {
                r = ((net.minidev.json.JSONArray)r).get(0);
                for (Object o1: (JSONArray)r) {
                    JSONObject j1 = (JSONObject)o1;
                    if(!j1.containsKey("source") || !j1.containsKey("destination")) {
                        continue;
                    }
                    String srcNode = DriverUtil.addressUriEscape((String)((JSONObject)j1.get("source")).get("source-node"));
                    String srcPort = DriverUtil.addressUriEscape((String)((JSONObject)j1.get("source")).get("source-tp"));
                    Resource resSrc = model.getResource(topologyURI + ":node=" + srcNode+":port="+srcPort);
                    String dstNode = DriverUtil.addressUriEscape((String)((JSONObject)j1.get("destination")).get("dest-node"));
                    String dstPort = DriverUtil.addressUriEscape((String)((JSONObject)j1.get("destination")).get("dest-tp"));
                    Resource resDst = model.getResource(topologyURI + ":node=" + dstNode+":port="+dstPort);
                    model.add(model.createStatement(resSrc, Nml.isAlias, resDst));
                }
            } else {
                logger.warning("OpenflowModelBuilder.createOntology failed to retrieve topology links from jsonTopology.");
            }
        } catch (Exception ex) {
            logger.warning(String.format("OpenflowModelBuilder.createOntology failed to parse the jsonTopology for links.", ex));
        }

        JSONObject jsonFlows = restconf.getConfigFlows(subsystemBaseUrl, username, password);
        try {
            Object r = JsonPath.parse(jsonFlows).read("$.nodes.node");
            for (Object o1: (JSONArray)r) {
                JSONObject j1 = (JSONObject)o1;
                String nodeId = (String)j1.get("id");
                String nodeIdEsc = DriverUtil.addressUriEscape(nodeId);
                // Through ODL, we assume all ops on table '0' for now. But this could be easily changed when multiple tables are required.
                net.minidev.json.JSONArray jTable0 = (net.minidev.json.JSONArray)JsonPath.parse(j1).read("$.flow-node-inventory:table[?(@.id=='0')]");
                if (jTable0.isEmpty()) {
                    continue;
                }
                Resource resOpenflow = model.getResource(URI_node(topologyURI, nodeIdEsc)+":openflow");
                Resource resFlowTable = model.getResource(resOpenflow.getURI()+":table=0");
                JSONArray jFlows = (JSONArray)((JSONObject)jTable0.get(0)).get("flow");
                for (Object o2: jFlows) {
                    JSONObject jFlow = (JSONObject)o2;
                    String flowId = (String)jFlow.get("id");
                    String flowName = null;
                    if (jFlow.containsKey("flow-name")) {
                        flowName = (String)jFlow.get("flow-name");
                        if (flowName.startsWith("urn:")) {
                            flowId = flowName;
                        }
                    }
                    Resource resFlow = RdfOwl.createResource(model, URI_flow(resFlowTable.getURI(), flowId), Mrs.Flow);
                    model.add(model.createStatement(resFlowTable, Mrs.hasFlow, resFlow));
                    model.add(model.createStatement(resOpenflow, Mrs.providesFlow, resFlow));
                    if (flowName != null) {
                        model.add(model.createStatement(resFlow, Nml.name, flowName));
                    }
                    if (!jFlow.containsKey("match")) {
                        continue;
                    }
                    JSONObject jFlowMatch = (JSONObject)jFlow.get("match");
                    if (jFlowMatch.isEmpty()) {
                        continue;
                    }
                    // flow match rules -> in_port, dl_type, dl_src, dl_dst, dl_vlan, nw_prot, nw_src, nw_dst, tcp_src, tcp dst
                    // restconf mapping: in-port, ethernet-match/ethernet-type/type, ethernet-match/ethernet-destination/address, 
                    // vlan-match/vlan-id/{vlan-id|vlan-id-present}, ip-match/ip-protocol, ipv4-destination, tcp-destination-port , udp-
                    // http://www.brocade.com/content/html/en/user-guide/Flow-Manager-2.0.0-User-Guide/GUID-7B6AF236-A11B-4A6A-B5D4-FAD5BDA2FED6.html
                    if (jFlowMatch.containsKey("in-port")) {
                        String inPort = jFlowMatch.get("in-port").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "in_port"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "in_port"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, inPort));
                    }
                    if (jFlowMatch.containsKey("ethernet-match")) {
                        JSONObject jEtherMatch = (JSONObject) jFlowMatch.get("ethernet-match");
                        if (jEtherMatch.containsKey("ethernet-type")) {
                            String dlType = ((JSONObject) jEtherMatch.get("ethernet-type")).get("type").toString();
                            Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "dl_type"), Mrs.FlowRule);
                            model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                            model.add(model.createStatement(resMatchRule, Mrs.type, "dl_type"));
                            model.add(model.createStatement(resMatchRule, Mrs.value, dlType));
                        } 
                        if (jEtherMatch.containsKey("ethernet-destination")) {
                            String dlDst = ((JSONObject) jEtherMatch.get("ethernet-destination")).get("address").toString();
                            Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "dl_dst"), Mrs.FlowRule);
                            model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                            model.add(model.createStatement(resMatchRule, Mrs.type, "dl_dst"));
                            model.add(model.createStatement(resMatchRule, Mrs.value, dlDst));                            
                        }
                        if (jEtherMatch.containsKey("ethernet-source")) {
                            String dlSrc = ((JSONObject) jEtherMatch.get("ethernet-source")).get("address").toString();
                            Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "dl_src"), Mrs.FlowRule);
                            model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                            model.add(model.createStatement(resMatchRule, Mrs.type, "dl_src"));
                            model.add(model.createStatement(resMatchRule, Mrs.value, dlSrc));                            
                        }
                    }
                    if (jFlowMatch.containsKey("vlan-match")) {
                        JSONObject jVlanMatch = (JSONObject) jFlowMatch.get("vlan-match");
                        if (jVlanMatch.containsKey("vlan-id")) {
                            JSONObject jVlanId = (JSONObject) jVlanMatch.get("vlan-id");
                            String dlVlan = "any";
                            if (jVlanId.containsKey("vlan-id")) {
                                dlVlan = jVlanId.get("vlan-id").toString();
                            }
                            Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "dl_vlan"), Mrs.FlowRule);
                            model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                            model.add(model.createStatement(resMatchRule, Mrs.type, "dl_vlan"));
                            model.add(model.createStatement(resMatchRule, Mrs.value, dlVlan));
                        } 
                    }
                    if (jFlowMatch.containsKey("ip-match")) {
                        JSONObject jIpMatch = (JSONObject) jFlowMatch.get("ip-match");
                        if (jIpMatch.containsKey("ip-protocol")) {
                            String ipProt = jIpMatch.get("ip-protocol").toString();
                            Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "nw_prot"), Mrs.FlowRule);
                            model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                            model.add(model.createStatement(resMatchRule, Mrs.type, "nw_prot"));
                            model.add(model.createStatement(resMatchRule, Mrs.value, ipProt));
                        }
                    }
                    if (jFlowMatch.containsKey("ipv4-source")) {
                        String ipv4 = jFlowMatch.get("ipv4-source").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "nw_src"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "nw_src"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, ipv4));
                    }
                    if (jFlowMatch.containsKey("ipv4-destination")) {
                        String ipv4 = jFlowMatch.get("ipv4-destination").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "nw_dst"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "nw_dst"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, ipv4));
                    }
                    if (jFlowMatch.containsKey("ipv6-source")) {
                        String ipv6 = jFlowMatch.get("ipv6-source").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "nw_src"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "nw_src"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, ipv6));
                    }
                    if (jFlowMatch.containsKey("ipv6-destination")) {
                        String ipv6 = jFlowMatch.get("ipv6-destination").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "nw_dst"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "nw_dst"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, ipv6));
                    }
                    if (jFlowMatch.containsKey("tcp-source-port")) {
                        String l4port = jFlowMatch.get("tcp-source-port").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "tp_src"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "tp_src"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, l4port));
                    }
                    if (jFlowMatch.containsKey("tcp-destination-port")) {
                        String l4port = jFlowMatch.get("tcp-destination-port").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "tp_dst"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "tp_dst"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, l4port));
                    }
                    if (jFlowMatch.containsKey("udp-source-port")) {
                        String l4port = jFlowMatch.get("udp-source-port").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "tp_src"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "tp_src"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, l4port));
                    }
                    if (jFlowMatch.containsKey("udp-destination-port")) {
                        String l4port = jFlowMatch.get("udp-destination-port").toString();
                        Resource resMatchRule = RdfOwl.createResource(model, URI_match(resFlow.getURI(), "tp_dst"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowMatch, resMatchRule));
                        model.add(model.createStatement(resMatchRule, Mrs.type, "tp_dst"));
                        model.add(model.createStatement(resMatchRule, Mrs.value, l4port));
                    }
                    //flow action rules
                    //--simplified 'instructions' (apply-actions only)
                    //@TODO: handle 'group'
                    try {
                        net.minidev.json.JSONArray j3 = (net.minidev.json.JSONArray)JsonPath.parse(jFlow).read("$.instructions..action");
                        JSONArray jActions = (JSONArray)j3.get(0);
                        for (Object o3: jActions) {
                            JSONObject jAction = (JSONObject) o3;
                            // flow action rules
                            String order = "0";
                            if (jAction.containsKey("order")) {
                                jAction.get("order").toString();
                            }
                            Resource resFlowAction = RdfOwl.createResource(model, URI_action(resFlow.getURI(), order), Mrs.FlowRule);
                            model.add(model.createStatement(resFlow, Mrs.flowAction, resFlowAction));
                            if (jAction.containsKey("output-action")) {
                                JSONObject jActionOutput = (JSONObject)jAction.get("output-action");
                                if (jActionOutput.containsKey("output-node-connector")) {
                                    String outPort = jActionOutput.get(("output-node-connector")).toString();
                                    model.add(model.createStatement(resFlowAction, Mrs.type, "output"));
                                    model.add(model.createStatement(resFlowAction, Mrs.value, outPort));
                                }
                            } else if (jAction.containsKey("pop-vlan-action")) {
                                model.add(model.createStatement(resFlowAction, Mrs.type, "strip_vlan"));
                                model.add(model.createStatement(resFlowAction, Mrs.value, "strip_vlan"));
                            } else if (jAction.containsKey("push-vlan-action")) {
                                model.add(model.createStatement(resFlowAction, Mrs.type, "push_vlan"));
                                model.add(model.createStatement(resFlowAction, Mrs.value, "33024"));
                            } else if (jAction.containsKey("set-field")) {
                                JSONObject jActionOutput = (JSONObject)jAction.get("set-field");
                                if (jActionOutput.containsKey("ethernet-match")) {
                                    JSONObject jMatchEther = (JSONObject)jActionOutput.get("ethernet-match");
                                    if (jMatchEther.containsKey("ethernet-type")) {
                                        JSONObject jMatchEtherData = (JSONObject)jMatchEther.get("ethernet-type");
                                        model.add(model.createStatement(resFlowAction, Mrs.type, "mod_dl_type"));
                                        model.add(model.createStatement(resFlowAction, Mrs.value, jMatchEtherData.get("type").toString()));
                                    }
                                    if (jMatchEther.containsKey("ethernet-source")) {
                                        JSONObject jMatchEtherData = (JSONObject)jMatchEther.get("ethernet-source");
                                        model.add(model.createStatement(resFlowAction, Mrs.type, "mod_dl_src"));
                                        model.add(model.createStatement(resFlowAction, Mrs.value, jMatchEtherData.get("address").toString()));
                                    } 
                                    if (jMatchEther.containsKey("ethernet-destination")) {
                                        JSONObject jMatchEtherData = (JSONObject)jMatchEther.get("ethernet-destination");
                                        model.add(model.createStatement(resFlowAction, Mrs.type, "mod_dl_dst"));
                                        model.add(model.createStatement(resFlowAction, Mrs.value, jMatchEtherData.get("address").toString()));
                                    }
                                } else if (jActionOutput.containsKey("vlan-match")) {
                                    JSONObject jMatchVlan = (JSONObject)jActionOutput.get("vlan-match");
                                    if (jMatchVlan.containsKey("vlan-id")) {
                                        JSONObject jVlanId = (JSONObject)jMatchVlan.get("vlan-id");
                                        model.add(model.createStatement(resFlowAction, Mrs.type, "mod_vlan_id"));
                                        String vlanId = "any";
                                        if (jVlanId.containsKey("vlan-id")) {
                                            vlanId = jVlanId.get("vlan-id").toString();
                                        }
                                        model.add(model.createStatement(resFlowAction, Mrs.value, vlanId));
                                    }
                                    if (jMatchVlan.containsKey("vlan-pcp")) {
                                        model.add(model.createStatement(resFlowAction, Mrs.type, "mod_vlan_pcp"));
                                        model.add(model.createStatement(resFlowAction, Mrs.value, jMatchVlan.get("vlan-pcp").toString()));
                                    }
                                } else if (jActionOutput.containsKey("ipv4-source")) {
                                    model.add(model.createStatement(resFlowAction, Mrs.type, "mod_nw_src"));
                                    model.add(model.createStatement(resFlowAction, Mrs.value, jActionOutput.get("ipv4-source").toString()));
                                } else if (jActionOutput.containsKey("ipv4-destination")) {
                                    model.add(model.createStatement(resFlowAction, Mrs.type, "mod_nw_dst"));
                                    model.add(model.createStatement(resFlowAction, Mrs.value, jActionOutput.get("ipv4-destination").toString()));
                                } else if (jActionOutput.containsKey("tcp-source-port")) {
                                    model.add(model.createStatement(resFlowAction, Mrs.type, "mod_tp_src"));
                                    model.add(model.createStatement(resFlowAction, Mrs.value, jActionOutput.get("tcp-source-port").toString()));
                                } else if (jActionOutput.containsKey("tcp-destination-port")) {
                                    model.add(model.createStatement(resFlowAction, Mrs.type, "mod_tp_dst"));
                                    model.add(model.createStatement(resFlowAction, Mrs.value, jActionOutput.get("tcp-destination-port").toString()));
                                }
                            } // and more, e.g. MPLS label swap, dscp, ttl etc.
                        }
                    } catch (Exception ex) {
                        Resource resFlowAction = RdfOwl.createResource(model, URI_action(resFlow.getURI(), "0"), Mrs.FlowRule);
                        model.add(model.createStatement(resFlow, Mrs.flowAction, resFlowAction));
                        model.add(model.createStatement(resFlowAction, Mrs.type, "drop"));
                        model.add(model.createStatement(resFlowAction, Mrs.value, "drop"));
                    }
                }
            }
        } catch (Exception ex) {
            logger.warning(String.format("OpenflowModelBuilder.createOntology failed to parse the jsonFlows for links.", ex));
        }
        return model;
    }
}
