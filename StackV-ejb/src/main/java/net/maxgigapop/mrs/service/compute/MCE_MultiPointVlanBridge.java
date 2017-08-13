/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

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

package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntTools;
import com.hp.hpl.jena.ontology.OntTools.PredicatesFilter;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.Filter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.common.TagSet;
import static net.maxgigapop.mrs.driver.opendaylight.OpenflowModelBuilder.URI_action;
import static net.maxgigapop.mrs.driver.opendaylight.OpenflowModelBuilder.URI_flow;
import static net.maxgigapop.mrs.driver.opendaylight.OpenflowModelBuilder.URI_match;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_MultiPointVlanBridge extends MCEBase {
    
    private static final StackLogger logger = new StackLogger(MCE_MultiPointVlanBridge.class.getName(), "MCE_MultiPointVlanBridge");

    private static final String OSpec_Template
            = "{\n"
            + "	\"$$\": [\n"
            + "		{\n"
            + "			\"hop\": \"?hop?\",\n"
            + "			\"vlan_tag\": \"?vid?\",\n"
            + "			\"#sparql\": \"SELECT DISTINCT ?hop ?vid WHERE {?hop a nml:BidirectionalPort. "
            + "?hop nml:hasLabel ?vlan. ?vlan nml:value ?vid. ?hop mrs:tag \\\"l2path+$$:%%\\\".}\",\n"
            + "			\"#required\": \"false\",\n"
            + "		}\n"
            + "	]\n"
            + "}";
    
    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        logger.cleanup();
        String method = "process";
        logger.refuuid(annotatedDelta.getReferenceUUID());
        logger.start(method);
        
        try {
            logger.trace(method, "DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(annotatedDelta.additionModel) -exception-"+ex);
        }
        
        Map<Resource, JSONObject> policyResDataMap = this.preProcess(policy, systemModel, annotatedDelta);        

        // Specific MCE logic - compute a List<Model> of MPVlan connections
        ServiceDelta outputDelta = annotatedDelta.clone();
        for (Resource res: policyResDataMap.keySet()) {
            Map<String, MCETools.Path> l2pathMap = this.doMPVBFinding(systemModel.getOntModel(), res, policyResDataMap.get(res));
            for (String connId : l2pathMap.keySet()) {
                outputDelta.getModelAddition().getOntModel().add(l2pathMap.get(connId).getOntModel().getBaseModel());
            }
        }
        
        this.postProcess(policy, outputDelta.getModelAddition().getOntModel(), systemModel.getOntModel(), OSpec_Template, policyResDataMap);

        try {
            logger.trace(method, "DeltaAddModel Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(outputDelta.additionModel) -exception-"+ex);
        }

        logger.end(method);
        return new AsyncResult(outputDelta);
    }

    private Map<String, MCETools.Path> doMPVBFinding(OntModel systemModel, Resource resConn,  Map<String, JSONObject> connDataMap) {
        String method = "doMPVBFinding";
        logger.message(method, "@doMPVBFinding -> " + resConn);
        // transform network graph
        // filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2NetworkModel(systemModel);
        /*
        try {
            logger.trace(method, "SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            logger.trace(method, "marshalModel(marshalModel(transformedModel) failed -- "+ex);
        }
        */
        Map<String, MCETools.Path> mapConnPaths = new HashMap<>();
        for (String connId: connDataMap.keySet()) {
            List<Resource> terminals = new ArrayList<>();
            JSONObject jsonConnReq = (JSONObject)connDataMap.get(connId);
            if (jsonConnReq.size() < 3) {
                throw logger.error_throwing(method, String.format("cannot find path for connection '%s' - request must have at least 3 terminals", connId));
            }
            for (Object key : jsonConnReq.keySet()) {
                Resource terminal = systemModel.getResource((String) key);
                if (!systemModel.contains(terminal, null)) {
                    throw logger.error_throwing(method, String.format("cannot identify terminal <%s> in JSON data", key));
                }
                terminals.add(terminal);
            }
            
            // Create init path starting from root terminaR to terminalR2 (This order could be reshuffled to disturb search)
            Resource terminal1 = terminals.get(0);
            terminals.remove(0);
            Resource terminal2 = terminals.get(0);
            terminals.remove(0);
            List<MCETools.Path> feasibleKSP12;
            try {
                feasibleKSP12 = MCETools.computeFeasibleL2KSP(transformedModel, terminal1, terminal2, jsonConnReq);
            } catch (Exception ex) {
                throw logger.error_throwing(method, String.format("connectionId=%s computeFeasibleL2KSP(nodeA=%s, nodeZ=%s, jsonConnReq=%s) exception -- ", connId, terminal1, terminal2, jsonConnReq) + ex);
            }
            if (feasibleKSP12 == null || feasibleKSP12.size() == 0) {
                throw logger.error_throwing(method, String.format("cannot find initial feasible path for connection '%s' between '%s' and '%s'", connId, terminal1, terminal2));
            }
            MCETools.Path mpvbPath = MCETools.getLeastCostPath(feasibleKSP12); //(Could also be pick 2nd and 3rd for disturbing search)
            if (((JSONObject)jsonConnReq.get(terminal1.getURI())).containsKey("mac_list") && ((JSONObject)jsonConnReq.get(terminal2.getURI())).containsKey("mac_list")) {
                lookupMacsForOpenflowPorts(mpvbPath, transformedModel, jsonConnReq, terminal1, terminal2);
            }
            // For 3rd through Tth terminals, connect them to one of openflow nodes in the path
            for (Resource terminalX : terminals) {
                MCETools.Path bridgePath = connectTerminalToPath(transformedModel, mpvbPath, terminalX, jsonConnReq);
                if (bridgePath == null) {
                    throw logger.error_throwing(method, String.format("cannot find bridging path in connection '%s' for terminal '%s'", connId, terminalX));
                }
                //mark mac_list for bridgePath from path(0).object to terminalX if (path.size > 1)
                if (bridgePath.size() > 1 && ((JSONObject)jsonConnReq.get(terminalX.getURI())).containsKey("mac_list") 
                        && ((JSONObject)jsonConnReq.get(bridgePath.get(0).getObject().toString())).containsKey("mac_list") ) {
                    lookupMacsForOpenflowPorts(bridgePath, transformedModel, jsonConnReq, bridgePath.get(0).getObject().asResource(), terminalX);
                }
                mpvbPath.addAll(bridgePath);
                mpvbPath.getOntModel().add(bridgePath.getOntModel().getBaseModel());
            }

            MCETools.tagPathHops(mpvbPath, "l2path+"+resConn.getURI()+":"+connId+"");
            transformedModel.add(mpvbPath.getOntModel());
            mapConnPaths.put(connId, mpvbPath);
        }
        return mapConnPaths;
    }

    private MCETools.Path connectTerminalToPath(OntModel transformedModel, MCETools.Path mpvbPath, Resource terminalX, JSONObject jsonConnReq) {
        String method = "connectTerminalToPath";
        Resource bridgeOpenflowService = checkTerminalOnPath(transformedModel, mpvbPath, terminalX);
        String bridgeMacList = null;
        if (((JSONObject)jsonConnReq.get(terminalX.getURI())).containsKey("mac_list")) {
            bridgeMacList = (String)((JSONObject)jsonConnReq.get(terminalX.getURI())).get("mac_list");
        }
        if (bridgeOpenflowService != null) {
            Resource bridgePort = terminalX;
            JSONObject jsonTe = (JSONObject) jsonConnReq.get(terminalX.getURI());
            String bridgeVlanTag = null;
            if (jsonTe != null && jsonTe.containsKey("vlan_tag")) {
                TagSet vlanRange;
                try {
                    vlanRange = new TagSet((String) jsonTe.get("vlan_tag"));
                } catch (TagSet.InvalidVlanRangeExeption ex) {
                    throw logger.throwing(method, String.format("terminal <%s> -exception- ", terminalX.getURI()), ex);
                }
                bridgeVlanTag = Integer.toString(vlanRange.getRandom());
            } else {
                throw logger.error_throwing(method, String.format("terminal '%s' has no 'vlan_tag' parameter in request data.", terminalX));
            }
            MCETools.Path bridgePath = new MCETools.Path();
            OntModel bridgePathModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            Statement bridgeHop = bridgePathModel.createStatement(bridgeOpenflowService, Nml.connectsTo, terminalX);
            bridgePathModel.add(bridgeHop);
            bridgePath.add(bridgeHop);
            if (bridgeMacList != null) {
                String bridgePortName = MCETools.getNameForPort(transformedModel, bridgePort);
                jsonConnReq.put(bridgePortName, new JSONObject());
                ((JSONObject)jsonConnReq.get(bridgePortName)).put("mac_list", bridgeMacList);
            }
            bridgePathModel = createBridgePathFlows(transformedModel, mpvbPath, bridgePathModel, bridgeOpenflowService, bridgePort, bridgeVlanTag, jsonConnReq);
            if (bridgePathModel == null) {
                throw logger.error_throwing(method, String.format("terminal '%s' is in path but cannot be bridged.", terminalX));
            }
            bridgePath.setOntModel(bridgePathModel);
            return bridgePath;
        } else {
            List<Resource> openflowPorts = listOpenflowPortsOnPath(transformedModel, mpvbPath);
            if (openflowPorts.isEmpty()) {
                throw logger.error_throwing(method, "listOpenflowPortsOnPath cannot find Openflow node on path");
            }
            for (Resource openflowPort : openflowPorts) {
                Property[] filterProperties = {Nml.connectsTo};
                Filter<Statement> connFilters = new OntTools.PredicatesFilter(filterProperties);
                List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, openflowPort, terminalX, MCETools.KSP_K_DEFAULT, connFilters);
                if (KSP == null || KSP.size() == 0) {
                    continue;
                }
                // loop through KSP, and shorten, verify and generate path model
                // Verify TE constraints (switching label and ?adaptation?), 
                Iterator<MCETools.Path> itP = KSP.iterator();
                while (itP.hasNext()) {
                    MCETools.Path bridgePath = itP.next();
                    // cut overalapping and loopback segments of the bridgePath and identify bridge OF service and Port 
                    bridgeOpenflowService = shortenBridgePath(transformedModel, mpvbPath, bridgePath);
                    Resource bridgePort = bridgePath.get(0).getSubject();
                    Resource bridgePortNext = bridgePath.get(0).getObject().asResource();
                    // get mac_list of bridgeOpenflowService to bridgePortNext
                    if (!jsonConnReq.containsKey(bridgeOpenflowService.getURI())) {
                        throw logger.error_throwing(method, "no mac_list for bridgeOpenflowService"+bridgeOpenflowService);
                    }
                    String bridgeOfMacList = (String)((JSONObject)jsonConnReq.get(bridgeOpenflowService.getURI())).get("mac_list");
                    if (!jsonConnReq.containsKey(bridgePortNext.getURI())) {
                        jsonConnReq.put(bridgePortNext.getURI(), new JSONObject());
                    }
                    ((JSONObject)jsonConnReq.get(bridgePortNext.getURI())).put("mac_list", bridgeOfMacList);
                    ((JSONObject)jsonConnReq.get(bridgeOpenflowService.getURI())).put("mac_list", bridgeOfMacList+bridgeMacList);
                    // initial vlanRange in of bridgePort could be full range but will eventually constrained by terminalX
                    boolean verified;
                    try {
                        verified = MCETools.verifyL2Path(transformedModel, bridgePath);
                    } catch (Exception ex) {
                        throw logger.throwing(method, "verifyL2Path -exception- ", ex);
                    }
                    if (verified) {
                        // generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                        OntModel bridgePathModel = MCETools.createL2PathVlanSubnets(transformedModel, bridgePath, jsonConnReq);
                        if (bridgePathModel != null) {
                            // bridgePath is always in port->port->ofSvc->port form. The first is bridge port 
                            // but VLAN flows created by createL2PathVlanSubnets start from second port.
                            String vlanPortName = MCETools.getNameForPort(transformedModel, bridgePortNext);
                            // find bridge port name and vlan
                            String sparql_bridge = "SELECT ?vlan_in WHERE {"
                                    + "?flow mrs:flowMatch ?match_port. "
                                    + "?match_port mrs:type \"in_port\". "
                                    + String.format("?match_port mrs:value \"%s\". ", vlanPortName)
                                    + "?flow mrs:flowMatch ?match_vlan. "
                                    + "?match_vlan mrs:type \"dl_vlan\". "
                                    + "?match_vlan mrs:value ?vlan_in. "
                                    + "}";
                            ResultSet rs = ModelUtil.sparqlQuery(bridgePathModel, sparql_bridge);
                            if (!rs.hasNext()) {
                                continue;
                            }
                            String bridgeVlanTag = rs.next().get("vlan_in").toString();
                            if (bridgeMacList != null) {
                                String bridgePortName = MCETools.getNameForPort(transformedModel, bridgePort);
                                jsonConnReq.put(bridgePortName, new JSONObject());
                                ((JSONObject)jsonConnReq.get(bridgePortName)).put("mac_list", bridgeMacList);
                            }
                            bridgePathModel = createBridgePathFlows(transformedModel, mpvbPath, bridgePathModel, bridgeOpenflowService, bridgePort, bridgeVlanTag, jsonConnReq);
                            if (bridgePathModel == null) {
                                continue;
                            }
                            // return the first feasible bridge path of KSP from the first feasible openflowPort of openflowPorts 
                            // @TODO: picking with optimization criteria OR random from all of the openflowPorts and their KSPs
                            bridgePath.setOntModel(bridgePathModel);
                            return bridgePath;
                        }
                    }
                }
            }
            return null;
        }
    }

    private Resource checkTerminalOnPath(OntModel transformedModel, MCETools.Path mpvbPath, Resource terminalX) {
        Iterator<Statement> itStmt = mpvbPath.iterator();
        itStmt.next();
        while (itStmt.hasNext()) {
            Statement stmt = itStmt.next();
            Resource resOriginal = stmt.getSubject();
            String sparql = "SELECT ?svc WHERE {"
                    + "?svc a mrs:OpenflowService. "
                    + String.format("?svc nml:hasBidirectionalPort <%s>. "
                            + "FILTER (?svc=<%s>)", terminalX, resOriginal)
                    + "}";
            ResultSet rs = ModelUtil.sparqlQuery(transformedModel, sparql);
            if (rs.hasNext()) {
                return rs.next().getResource("svc");
            }
        }
        return null;
    }
    
    Resource shortenBridgePath(OntModel transformedModel, MCETools.Path connPath, MCETools.Path bridgePath) {
        Resource bridgePort = bridgePath.get(0).getSubject();
        Resource bridgeSvc = bridgePath.get(0).getObject().asResource();
        Iterator<Statement> itStmt = bridgePath.iterator();
        itStmt.next();
        //find the fartherest bridgePort on bridgePath that diverges from connPath (share OfSvc with another port)
        while (itStmt.hasNext()) {
            Statement stmt = itStmt.next();
            Resource resDiverge = stmt.getSubject();
            Iterator<Statement> itStmt2 = connPath.iterator();
            while (itStmt2.hasNext()) {
                Statement stmt2 = itStmt2.next();
                Resource resOriginal = stmt2.getSubject();
                String sparql = "SELECT ?svc WHERE {"
                        + "?svc a mrs:OpenflowService. "
                        + String.format("?svc nml:hasBidirectionalPort <%s>. "
                                + "FILTER (?svc=<%s>)", resDiverge, resOriginal)
                        + "}";
                ResultSet rs = ModelUtil.sparqlQuery(transformedModel, sparql);
                if (rs.hasNext()) {
                    bridgePort = resDiverge;
                    bridgeSvc = rs.next().getResource("svc");
                    break;
                }
            }
        }
        //remove segment before the divergence (remaning segment still in port-port-ofsvc-... form) 
        itStmt = bridgePath.iterator();
        while (itStmt.hasNext()) {
            Statement stmt = itStmt.next();
            Resource resDiverge = stmt.getSubject();
            if (resDiverge.equals(bridgePort)) {
                break;
            } else {
                itStmt.remove();
            }
        }
        return bridgeSvc;
    }
    
    private OntModel createBridgePathFlows(OntModel transformedModel, MCETools.Path mpvbPath, OntModel bridgePathModel,
            Resource bridgeOpenflowService, Resource bridgePort, String bridgeVlanTag, JSONObject jsonConnReq) {
        String method="createBridgePathFlows";
        // create VLAN bridging flows with bridgeOpenflowService and bridgePort and add to l2PathModel
        OntModel mpvbModel = mpvbPath.getOntModel();
        Map<Resource, Map> mpvpFlowMap = new HashMap();
        Resource resFlowTable = null;
        // find existing ports and flows from mpvbModel
        String sparql_flowin = "SELECT DISTINCT ?table ?flow ?port_in ?vlan_in ?match_arp WHERE {"
                + String.format("<%s> mrs:providesFlow ?flow. ", bridgeOpenflowService)
                + "?table mrs:hasFlow ?flow. "
                + "?flow mrs:flowMatch ?match_port. "
                + "?match_port mrs:type \"in_port\". "
                + "?match_port mrs:value ?port_in. "
                + "?flow mrs:flowMatch ?match_vlan. "
                + "?match_vlan mrs:type \"dl_vlan\". "
                + "?match_vlan mrs:value ?vlan_in. "
                + "OPTIONAL {?flow mrs:flowMatch ?match_arp. ?match_arp mrs:type \"dl_type\". ?match_arp mrs:value \"2054\". } "
                + "FILTER (NOT EXISTS {?flow mrs:flowMatch ?match_src_mac. ?match_src_mac mrs:type \"dl_src\".} "
                + "&& NOT EXISTS {?flow mrs:flowMatch ?match_dst_mac. ?match_dst_mac mrs:type \"dl_dst\".} ) "
                + "}";
        ResultSet rs = ModelUtil.sparqlQuery(mpvbModel, sparql_flowin);
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            if (resFlowTable == null) {
                resFlowTable = qs.getResource("table");
            }
            Resource resFlow = qs.getResource("flow");
            String strPort = qs.get("port_in").toString();
            String strVlan = qs.get("vlan_in").toString();
            Map flowParamMap = new HashMap();
            mpvpFlowMap.put(resFlow, flowParamMap);
            flowParamMap.put("port_in", strPort);
            flowParamMap.put("vlan_in", strVlan);
            if (qs.contains("match_arp")) {
                flowParamMap.put("match_arp", "true");
            }
        }
        String sparql_flowout = "SELECT ?table ?flow ?port_out ?vlan_out WHERE {"
                + String.format("<%s> mrs:providesFlow ?flow. ", bridgeOpenflowService)
                + "?table mrs:hasFlow ?flow. "
                + "?flow mrs:flowAction ?action_port. "
                + "?action_port mrs:type \"output\". "
                + "?action_port mrs:value ?port_out. "
                + "?flow mrs:flowAction ?action_vlan. "
                + "?action_vlan mrs:type \"mod_vlan_vid\". "
                + "?action_vlan mrs:value ?vlan_out. "
                + "}";
        rs = ModelUtil.sparqlQuery(mpvbModel, sparql_flowout);
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            if (resFlowTable == null) {
                resFlowTable = qs.getResource("table");
            }
            Resource resFlow = qs.getResource("flow");
            String strPort = qs.get("port_out").toString();
            String strVlan = qs.get("vlan_out").toString();
            Map flowParamMap = null;
            if (mpvpFlowMap.containsKey(resFlow)) {
                flowParamMap = (Map) mpvpFlowMap.get(resFlow);
            } else {
                flowParamMap = new HashMap();
                mpvpFlowMap.put(resFlow, flowParamMap);
            }
            if (flowParamMap.containsKey("port_out")) {
                strPort = (String) flowParamMap.get("port_out") + "," + strPort;
            }
            flowParamMap.put("port_out", strPort);
            if (flowParamMap.containsKey("vlan_out")) {
                strVlan = (String) flowParamMap.get("vlan_out") + "," + strVlan;
            }
            flowParamMap.put("vlan_out", strVlan);
        }
        if (mpvpFlowMap.size() < 2) {
            return null;
        }
        // add new flow for match bridgePortName and bridgeVlanTag
        String bridgeFlowId = bridgePort.getURI() + ":flow=input_vlan" + bridgeVlanTag;
        String bridgePortName = MCETools.getNameForPort(transformedModel, bridgePort);
        String[] bridgePortMacList = null;
        if (jsonConnReq.containsKey(bridgePortName) && ((JSONObject)jsonConnReq.get(bridgePortName)).containsKey("mac_list")) {
            bridgePortMacList = ((JSONObject)jsonConnReq.get(bridgePortName)).get("mac_list").toString().split(",");
            if (bridgePortMacList.length == 0 || bridgePortMacList[0].length() != 17) {
                throw logger.error_throwing(method, "invalid mac_list format in request data for port: " +  bridgePortName);
            }
        }
        Resource resBridgeFlow = RdfOwl.createResource(bridgePathModel, URI_flow(resFlowTable.getURI(), bridgeFlowId), Mrs.Flow);
        bridgePathModel.add(bridgePathModel.createStatement(resFlowTable, Mrs.hasFlow, resBridgeFlow));
        bridgePathModel.add(bridgePathModel.createStatement(bridgeOpenflowService, Mrs.providesFlow, resBridgeFlow));
        Resource resBridgeFlowMatch1 = RdfOwl.createResource(bridgePathModel, URI_match(resBridgeFlow.getURI(), "in_port"), Mrs.FlowRule);
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowMatch, resBridgeFlowMatch1));
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlowMatch1, Mrs.type, "in_port"));
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlowMatch1, Mrs.value, bridgePortName));
        Resource resBridgeFlowMatch2 = RdfOwl.createResource(bridgePathModel, URI_match(resBridgeFlow.getURI(), "dl_vlan"), Mrs.FlowRule);
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowMatch, resBridgeFlowMatch2));
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlowMatch2, Mrs.type, "dl_vlan"));
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlowMatch2, Mrs.value, bridgeVlanTag));
        if (bridgePortMacList != null) {
            Resource resBridgeFlowMatch3 = RdfOwl.createResource(bridgePathModel, URI_match(resBridgeFlow.getURI(), "dl_type"), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowMatch, resBridgeFlowMatch3));
            bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlowMatch3, Mrs.type, "dl_type"));
            bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlowMatch3, Mrs.value, "2054"));            
        }
        List<Resource> flowsInPath = new ArrayList();
        List<String> portsInPath = new ArrayList();
        List<String> vlansInPath = new ArrayList();
        Character bridgePortActionOrder = 'A'; // order of actions in bridgeFlow will be A-B-C-D-D
        for (Resource mpvbFlow : mpvpFlowMap.keySet()) {
            Map flowParams = mpvpFlowMap.get(mpvbFlow);
            String flowInPort = (String) flowParams.get("port_in");
            String flowInVlan = (String) flowParams.get("vlan_in");
            String[] flowOutPorts = ((String) flowParams.get("port_out")).split(",");
            String[] flowOutVlans = ((String) flowParams.get("vlan_out")).split(",");
            // insert match for ARP
            if (bridgePortMacList != null && !flowParams.containsKey("match_arp")) {
                Resource mpvbFlowMatchArp = RdfOwl.createResource(bridgePathModel, URI_match(mpvbFlow.getURI(), "dl_type"), Mrs.FlowRule);
                bridgePathModel.add(bridgePathModel.createStatement(mpvbFlow, Mrs.flowMatch, mpvbFlowMatchArp));
                bridgePathModel.add(bridgePathModel.createStatement(mpvbFlowMatchArp, Mrs.type, "dl_type"));
                bridgePathModel.add(bridgePathModel.createStatement(mpvbFlowMatchArp, Mrs.value, "2054"));
                flowsInPath.add(mpvbFlow);
                portsInPath.add(flowInPort);
                vlansInPath.add(flowInVlan);
            }
            // order of actions in existing mpvbFlow: A-B-C-(D-E-...)->this = 'A' + flowOutPorts.length*2+1 
            Character flowActionOrder = (char) ('A' + flowOutPorts.length * 2 + 1);
            /*
            Resource resFlowAction0 = RdfOwl.createResource(bridgePathModel, URI_action(mpvbFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(mpvbFlow, Mrs.flowAction, resFlowAction0));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowAction0, Mrs.type, "strip_vlan"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowAction0, Mrs.value, "any"));
            */
            // add to mpvbFlow: actions A: set vlan id to bridgeVlanTag B: output to bridgePortName
            Resource resFlowActionA = RdfOwl.createResource(bridgePathModel, URI_action(mpvbFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(mpvbFlow, Mrs.flowAction, resFlowActionA));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.type, "mod_vlan_vid"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.value, bridgeVlanTag));
            Resource resFlowActionB = RdfOwl.createResource(bridgePathModel, URI_action(mpvbFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(mpvbFlow, Mrs.flowAction, resFlowActionB));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.type, "output"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.value, bridgePortName));
            /*
            Resource resFlowAction1 = RdfOwl.createResource(bridgePathModel, URI_action(resBridgeFlow.getURI(), (bridgePortActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowAction, resFlowAction1));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowAction1, Mrs.type, "strip_vlan"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowAction1, Mrs.value, "any"));
            */
            // add to bridgeFlow: actions C: to set vlan id to flowInVlan D: output to flowInPort
            Resource resFlowActionC = RdfOwl.createResource(bridgePathModel, URI_action(resBridgeFlow.getURI(), (bridgePortActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowAction, resFlowActionC));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionC, Mrs.type, "mod_vlan_vid"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionC, Mrs.value, flowInVlan));
            Resource resFlowActionD = RdfOwl.createResource(bridgePathModel, URI_action(resBridgeFlow.getURI(), (bridgePortActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowAction, resFlowActionD));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionD, Mrs.type, "output"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionD, Mrs.value, flowInPort));
            
            // Add MAC flows 
            if (bridgePortMacList != null && jsonConnReq.containsKey(flowInPort) && ((JSONObject)jsonConnReq.get(flowInPort)).containsKey("mac_list")) {
                String[] flowInPortMacList = ((JSONObject)jsonConnReq.get(flowInPort)).get("mac_list").toString().split(",");
                if (flowInPortMacList.length == 0 || flowInPortMacList[0].length() != 17) {
                    throw logger.error_throwing(method, "invalid mac_list format in request data for port: " + flowInPort);
                }
                for (String inPortMac: flowInPortMacList) {
                    for (String bridgePortMac: bridgePortMacList) {
                        // MAC flow from inPort to bridgePort
                        Resource resToBridgePortMacFlow = RdfOwl.createResource(bridgePathModel, URI_flow(resFlowTable.getURI(), bridgeFlowId+":"+inPortMac+":"+bridgePortMac), Mrs.Flow);
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowTable, Mrs.hasFlow, resToBridgePortMacFlow));
                        bridgePathModel.add(bridgePathModel.createStatement(bridgeOpenflowService, Mrs.providesFlow, resToBridgePortMacFlow));
                        Resource resToBridgePortMacFlowMatch1 = RdfOwl.createResource(bridgePathModel, URI_match(resToBridgePortMacFlow.getURI(), "in_port"), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlow, Mrs.flowMatch, resToBridgePortMacFlowMatch1));
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlowMatch1, Mrs.type, "in_port"));
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlowMatch1, Mrs.value, flowInPort));
                        Resource resToBridgePortMacFlowMatch2 = RdfOwl.createResource(bridgePathModel, URI_match(resToBridgePortMacFlow.getURI(), "dl_vlan"), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlow, Mrs.flowMatch, resToBridgePortMacFlowMatch2));
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlowMatch2, Mrs.type, "dl_vlan"));
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlowMatch2, Mrs.value, flowInVlan));                    
                        Resource resToBridgePortMacFlowMatch3 = RdfOwl.createResource(bridgePathModel, URI_match(resToBridgePortMacFlow.getURI(), "dl_src"), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlow, Mrs.flowMatch, resToBridgePortMacFlowMatch3));
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlowMatch3, Mrs.type, "dl_src"));
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlowMatch3, Mrs.value, inPortMac));
                        Resource resToBridgePortMacFlowMatch4 = RdfOwl.createResource(bridgePathModel, URI_match(resToBridgePortMacFlow.getURI(), "dl_dst"), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlow, Mrs.flowMatch, resToBridgePortMacFlowMatch4));
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlowMatch4, Mrs.type, "dl_dst"));
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlowMatch4, Mrs.value, bridgePortMac));
                        // actions A: set vlan id to bridgeVlanTag B: output to bridgePortName
                        flowActionOrder = 'A';
                        resFlowActionA = RdfOwl.createResource(bridgePathModel, URI_action(resToBridgePortMacFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlow, Mrs.flowAction, resFlowActionA));
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.type, "mod_vlan_vid"));
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.value, bridgeVlanTag));
                        resFlowActionB = RdfOwl.createResource(bridgePathModel, URI_action(resToBridgePortMacFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resToBridgePortMacFlow, Mrs.flowAction, resFlowActionB));
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.type, "output"));
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.value, bridgePortName));

                        // MAC flow from bridgePort to inPort (reverse)
                        Resource resFromBridgePortMacFlow = RdfOwl.createResource(bridgePathModel, URI_flow(resFlowTable.getURI(), bridgeFlowId+":"+bridgePortMac+":"+inPortMac), Mrs.Flow);
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowTable, Mrs.hasFlow, resFromBridgePortMacFlow));
                        bridgePathModel.add(bridgePathModel.createStatement(bridgeOpenflowService, Mrs.providesFlow, resFromBridgePortMacFlow));
                        Resource resFromBridgePortMacFlowMatch1 = RdfOwl.createResource(bridgePathModel, URI_match(resFromBridgePortMacFlow.getURI(), "in_port"), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlow, Mrs.flowMatch, resFromBridgePortMacFlowMatch1));
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlowMatch1, Mrs.type, "in_port"));
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlowMatch1, Mrs.value, bridgePortName));
                        Resource resFromBridgePortMacFlowMatch2 = RdfOwl.createResource(bridgePathModel, URI_match(resFromBridgePortMacFlow.getURI(), "dl_vlan"), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlow, Mrs.flowMatch, resFromBridgePortMacFlowMatch2));
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlowMatch2, Mrs.type, "dl_vlan"));
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlowMatch2, Mrs.value, bridgeVlanTag));                    
                        Resource resFromBridgePortMacFlowMatch3 = RdfOwl.createResource(bridgePathModel, URI_match(resFromBridgePortMacFlow.getURI(), "dl_src"), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlow, Mrs.flowMatch, resFromBridgePortMacFlowMatch3));
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlowMatch3, Mrs.type, "dl_src"));
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlowMatch3, Mrs.value, bridgePortMac));
                        Resource resFromBridgePortMacFlowMatch4 = RdfOwl.createResource(bridgePathModel, URI_match(resFromBridgePortMacFlow.getURI(), "dl_dst"), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlow, Mrs.flowMatch, resFromBridgePortMacFlowMatch4));
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlowMatch4, Mrs.type, "dl_dst"));
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlowMatch4, Mrs.value, inPortMac));
                        // actions C: to set vlan id to flowInVlan D: output to flowInPort
                        resFlowActionC = RdfOwl.createResource(bridgePathModel, URI_action(resFromBridgePortMacFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlow, Mrs.flowAction, resFlowActionC));
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowActionC, Mrs.type, "mod_vlan_vid"));
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowActionC, Mrs.value, flowInVlan));
                        resFlowActionD = RdfOwl.createResource(bridgePathModel, URI_action(resFromBridgePortMacFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
                        bridgePathModel.add(bridgePathModel.createStatement(resFromBridgePortMacFlow, Mrs.flowAction, resFlowActionD));
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowActionD, Mrs.type, "output"));
                        bridgePathModel.add(bridgePathModel.createStatement(resFlowActionD, Mrs.value, flowInPort));
                    }
                }
            }
        }
        // add MAC flows between two ports in the original path
        if (mpvpFlowMap.size() == 2 && portsInPath.size() == 2) {
            Resource inFlow = flowsInPath.get(0);
            String flowInPort = portsInPath.get(0);
            String flowInVlan = vlansInPath.get(0);
            Resource outFlow = flowsInPath.get(1);
            String flowOutPort = portsInPath.get(1);
            String flowOutVlan = vlansInPath.get(1);
            String[] flowInPortMacList = ((JSONObject) jsonConnReq.get(flowInPort)).get("mac_list").toString().split(",");
            String[] flowOutPortMacList = ((JSONObject) jsonConnReq.get(flowOutPort)).get("mac_list").toString().split(",");
            for (String inPortMac : flowInPortMacList) {
                for (String outPortMac : flowOutPortMacList) {
                    // MAC flow from inPort to outPort
                    Resource resToOutPortMacFlow = RdfOwl.createResource(bridgePathModel, URI_flow(resFlowTable.getURI(), inFlow + ":" + inPortMac + ":" + outPortMac), Mrs.Flow);
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowTable, Mrs.hasFlow, resToOutPortMacFlow));
                    bridgePathModel.add(bridgePathModel.createStatement(bridgeOpenflowService, Mrs.providesFlow, resToOutPortMacFlow));
                    Resource resToOutPortMacFlowMatch1 = RdfOwl.createResource(bridgePathModel, URI_match(resToOutPortMacFlow.getURI(), "in_port"), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlow, Mrs.flowMatch, resToOutPortMacFlowMatch1));
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlowMatch1, Mrs.type, "in_port"));
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlowMatch1, Mrs.value, flowInPort));
                    Resource resToOutPortMacFlowMatch2 = RdfOwl.createResource(bridgePathModel, URI_match(resToOutPortMacFlow.getURI(), "dl_vlan"), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlow, Mrs.flowMatch, resToOutPortMacFlowMatch2));
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlowMatch2, Mrs.type, "dl_vlan"));
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlowMatch2, Mrs.value, flowInVlan));
                    Resource resToOutPortMacFlowMatch3 = RdfOwl.createResource(bridgePathModel, URI_match(resToOutPortMacFlow.getURI(), "dl_src"), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlow, Mrs.flowMatch, resToOutPortMacFlowMatch3));
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlowMatch3, Mrs.type, "dl_src"));
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlowMatch3, Mrs.value, inPortMac));
                    Resource resToOutPortMacFlowMatch4 = RdfOwl.createResource(bridgePathModel, URI_match(resToOutPortMacFlow.getURI(), "dl_dst"), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlow, Mrs.flowMatch, resToOutPortMacFlowMatch4));
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlowMatch4, Mrs.type, "dl_dst"));
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlowMatch4, Mrs.value, outPortMac));
                    // actions A: set vlan id to flowOutVlan B: output to flowOutPort
                    Character flowActionOrder = 'A';
                    Resource resFlowActionA = RdfOwl.createResource(bridgePathModel, URI_action(resToOutPortMacFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlow, Mrs.flowAction, resFlowActionA));
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.type, "mod_vlan_vid"));
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.value, flowOutVlan));
                    Resource resFlowActionB = RdfOwl.createResource(bridgePathModel, URI_action(resToOutPortMacFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resToOutPortMacFlow, Mrs.flowAction, resFlowActionB));
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.type, "output"));
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.value, flowOutPort));

                    // MAC flow from outPort to inPort (reverse)
                    Resource resFromOutPortMacFlow = RdfOwl.createResource(bridgePathModel, URI_flow(resFlowTable.getURI(), outFlow + ":" + outPortMac + ":" + inPortMac), Mrs.Flow);
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowTable, Mrs.hasFlow, resFromOutPortMacFlow));
                    bridgePathModel.add(bridgePathModel.createStatement(bridgeOpenflowService, Mrs.providesFlow, resFromOutPortMacFlow));
                    Resource resFromOutPortMacFlowMatch1 = RdfOwl.createResource(bridgePathModel, URI_match(resFromOutPortMacFlow.getURI(), "in_port"), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlow, Mrs.flowMatch, resFromOutPortMacFlowMatch1));
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlowMatch1, Mrs.type, "in_port"));
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlowMatch1, Mrs.value, flowOutPort));
                    Resource resFromOutPortMacFlowMatch2 = RdfOwl.createResource(bridgePathModel, URI_match(resFromOutPortMacFlow.getURI(), "dl_vlan"), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlow, Mrs.flowMatch, resFromOutPortMacFlowMatch2));
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlowMatch2, Mrs.type, "dl_vlan"));
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlowMatch2, Mrs.value, flowOutVlan));
                    Resource resFromOutPortMacFlowMatch3 = RdfOwl.createResource(bridgePathModel, URI_match(resFromOutPortMacFlow.getURI(), "dl_src"), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlow, Mrs.flowMatch, resFromOutPortMacFlowMatch3));
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlowMatch3, Mrs.type, "dl_src"));
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlowMatch3, Mrs.value, outPortMac));
                    Resource resFromOutPortMacFlowMatch4 = RdfOwl.createResource(bridgePathModel, URI_match(resFromOutPortMacFlow.getURI(), "dl_dst"), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlow, Mrs.flowMatch, resFromOutPortMacFlowMatch4));
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlowMatch4, Mrs.type, "dl_dst"));
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlowMatch4, Mrs.value, inPortMac));
                    // actions C: to set vlan id to flowInVlan D: output to flowInPort
                    Resource resFlowActionC = RdfOwl.createResource(bridgePathModel, URI_action(resFromOutPortMacFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlow, Mrs.flowAction, resFlowActionC));
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowActionC, Mrs.type, "mod_vlan_vid"));
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowActionC, Mrs.value, flowInVlan));
                    Resource resFlowActionD = RdfOwl.createResource(bridgePathModel, URI_action(resFromOutPortMacFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
                    bridgePathModel.add(bridgePathModel.createStatement(resFromOutPortMacFlow, Mrs.flowAction, resFlowActionD));
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowActionD, Mrs.type, "output"));
                    bridgePathModel.add(bridgePathModel.createStatement(resFlowActionD, Mrs.value, flowInPort));
                }
            }
        }

        return bridgePathModel;
    }

    private Resource getOpenflowNodeOnPath(OntModel transformedModel, MCETools.Path connPath) {
        Iterator<Statement> itStmt = connPath.iterator();
        while (itStmt.hasNext()) {
            Statement stmt = itStmt.next();
            Resource resObj = stmt.getObject().asResource();
            String sparql = "SELECT ?node WHERE {"
                    + "?node a nml:Node. "
                    + String.format("?node nml:hasService <%s>. <%s> a mrs:OpenflowService. ",
                            resObj, resObj)// assume openflow devices are all modeled as nml:Node
                    + "}";
            ResultSet rs = ModelUtil.sparqlQuery(transformedModel, sparql);
            if (!rs.hasNext()) {
                continue;
            }
            Resource resNode = rs.next().getResource("node");
            return resNode;
        }
        return null;
    }

    private List<Resource> listOpenflowNodesOnPath(OntModel transformedModel, MCETools.Path connPath) {
        List<Resource> listNodes = new ArrayList();
        Iterator<Statement> itStmt = connPath.iterator();
        while (itStmt.hasNext()) {
            Statement stmt = itStmt.next();
            Resource resObj = stmt.getObject().asResource();
            String sparql = "SELECT ?node WHERE {"
                    + "?node a nml:Node. "
                    + String.format("?node nml:hasService <%s>. <%s> a mrs:OpenflowService. ",
                            resObj, resObj)// assume openflow devices are all modeled as nml:Node
                    + "}";
            ResultSet rs = ModelUtil.sparqlQuery(transformedModel, sparql);
            if (!rs.hasNext()) {
                continue;
            }
            Resource resNode = rs.next().getResource("node");
            listNodes.add(resNode);
        }
        return listNodes;
    }

    private List<Resource> listOpenflowPortsOnPath(OntModel transformedModel, MCETools.Path connPath) {
        List<Resource> listNodes = new ArrayList();
        Iterator<Statement> itStmt = connPath.iterator();
        while (itStmt.hasNext()) {
            Statement stmt = itStmt.next();
            Resource resObj = stmt.getSubject();
            String sparql = "SELECT ?svc WHERE {"
                    + "?svc a mrs:OpenflowService. "
                    + String.format("?svc nml:hasBidirectionalPort <%s>. ",
                            resObj)
                    + "}";
            ResultSet rs = ModelUtil.sparqlQuery(transformedModel, sparql);
            if (rs.hasNext()) {
                listNodes.add(resObj);
            }
            if (!itStmt.hasNext()) {
                resObj = stmt.getObject().asResource();
                sparql = "SELECT ?svc WHERE {"
                        + "?svc a mrs:OpenflowService. "
                        + String.format("?svc nml:hasBidirectionalPort <%s>. ",
                                resObj)
                        + "}";
                rs = ModelUtil.sparqlQuery(transformedModel, sparql);
                if (rs.hasNext()) {
                    listNodes.add(resObj);
                }
            }
        }
        return listNodes;
    }

    private void lookupMacsForOpenflowPorts(MCETools.Path mpvbPath, OntModel refModel, JSONObject jsonConnReq, Resource terminal1, Resource terminal2) {
        Iterator<Statement> itStmt = mpvbPath.iterator();
        String macList1 = (String)((JSONObject)jsonConnReq.get(terminal1.getURI())).get("mac_list");
        String macList2 = (String)((JSONObject)jsonConnReq.get(terminal2.getURI())).get("mac_list");
        while (itStmt.hasNext()) {
            Statement link = itStmt.next();
            Resource res1 = link.getSubject();
            Resource resX = link.getObject().asResource();
            if (!refModel.contains(res1, RdfOwl.type, Nml.BidirectionalPort)
                    || !refModel.contains(resX, RdfOwl.type, Mrs.OpenflowService)) {
                continue;
            }
            if (itStmt.hasNext()) {
                link = itStmt.next();
                Resource res2 = link.getObject().asResource();
                if (refModel.contains(res2, RdfOwl.type, Nml.BidirectionalPort) ) {
                    String port1 = MCETools.getNameForPort(refModel, res1);
                    String port2 = MCETools.getNameForPort(refModel, res2);
                    if (!jsonConnReq.containsKey(port1)) {
                        jsonConnReq.put(port1, new JSONObject());
                    }
                    ((JSONObject)jsonConnReq.get(port1)).put("mac_list", macList1);
                    //((JSONObject)jsonConnReq.get(port1)).put("openflow_service", resX);
                    if (!jsonConnReq.containsKey(port2)) {
                        jsonConnReq.put(port2, new JSONObject());
                    }
                    ((JSONObject)jsonConnReq.get(port2)).put("mac_list", macList2);
                    //((JSONObject)jsonConnReq.get(port2)).put("openflow_service", resX);
                    //combine mac addresses from two pors and put them to openflow_service
                    if (!jsonConnReq.containsKey(resX.getURI())) {
                        jsonConnReq.put(resX.getURI(), new JSONObject());
                    }
                    ((JSONObject)jsonConnReq.get(resX.getURI())).put("mac_list", macList1+macList2);
                }
            }

        }
    }
}