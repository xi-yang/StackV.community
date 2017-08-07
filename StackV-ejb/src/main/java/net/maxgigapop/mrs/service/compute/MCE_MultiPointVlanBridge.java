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
public class MCE_MultiPointVlanBridge implements IModelComputationElement {
    
    private static final StackLogger logger = new StackLogger(MCE_MultiPointVlanBridge.class.getName(), "MCE_MultiPointVlanBridge");

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        logger.cleanup();
        String method = "process";
        logger.refuuid(annotatedDelta.getReferenceUUID());
        logger.start(method);
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw logger.error_throwing(method, "target:ServiceDelta has null addition model");
        }
        try {
            logger.trace(method, "DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(annotatedDelta.additionModel) -exception-"+ex);
        }
        // importPolicyData : Link->Connection->List<PolicyData> of terminal Node/Topology
        String sparql = "SELECT ?conn ?policy ?data ?type ?value WHERE {"
                + "?conn spa:dependOn ?policy . "
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_MultiPointVlanBridge'. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?type. ?data spa:value ?value. "
                + String.format("FILTER (not exists {?policy spa:dependOn ?other} && ?policy = <%s>)", policy.getURI())
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        Map<Resource, Map> connPolicyMap = new HashMap<>();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resPolicy = querySolution.get("policy").asResource();
            if (!connPolicyMap.containsKey(resPolicy)) {
                Map<String, List> connMap = new HashMap<>();
                List connList = new ArrayList<>();
                connMap.put("connections", connList);
                List dataList = new ArrayList<>();
                connMap.put("imports", dataList);
                connPolicyMap.put(resPolicy, connMap);
            }
            Resource resConn = querySolution.get("conn").asResource();
            if (!((List)connPolicyMap.get(resPolicy).get("connections")).contains(resConn))
                ((List)connPolicyMap.get(resPolicy).get("connections")).add(resConn);
            Resource resData = querySolution.get("data").asResource();
            RDFNode nodeDataType = querySolution.get("type");
            RDFNode nodeDataValue = querySolution.get("value");
            boolean existed =false;
            for (Map dataMap: (List<Map>)connPolicyMap.get(resPolicy).get("imports")) {
                if(dataMap.get("data").equals(resData)) {
                    existed = true;
                    break;
                }
            }
            if (!existed) {
                Map policyData = new HashMap<>();
                policyData.put("data", resData);
                policyData.put("type", nodeDataType.toString());
                policyData.put("value", nodeDataValue.toString());
                ((List)connPolicyMap.get(resPolicy).get("imports")).add(policyData);
            }
        }
        
        ServiceDelta outputDelta = annotatedDelta.clone();

        // compute a List<Model> of multi-point vlan bridge (MPVB) connections
        for (Resource policyAction : connPolicyMap.keySet()) {
            Map<String, MCETools.Path> l2pathMap = this.doMultiPathFinding(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), policyAction, connPolicyMap.get(policyAction));
            if (l2pathMap == null) {
                throw logger.error_throwing(method, "cannot find multi-point bridge paths for policy=" + policyAction);
            }

            //2. merge the placement satements into spaModel
            //3. update policyData this action exportTo 
            for (String connId: l2pathMap.keySet()) {
                outputDelta.getModelAddition().getOntModel().add(l2pathMap.get(connId).getOntModel().getBaseModel());
                this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), policyAction, connId, l2pathMap.get(connId));
            }
    
            //4. remove policy and all related SPA statements receursively under conn from spaModel
            //   and also remove all statements that say dependOn this 'policy'
            MCETools.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), policyAction);

            //5. mark the Link as an Abstraction
            /*
            for (Resource resConn: (List<Resource>)((Map)connPolicyMap.get(policyAction)).get("connections")) {
                outputDelta.getModelAddition().getOntModel().add(outputDelta.getModelAddition().getOntModel().createStatement(resConn, Spa.type, Spa.Abstraction));
            }
            */
        }
        try {
            logger.trace(method, "DeltaAddModel Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(outputDelta.additionModel) -exception-"+ex);
        }
        logger.end(method);
        return new AsyncResult(outputDelta);
    }

    private Map<String, MCETools.Path> doMultiPathFinding(OntModel systemModel, OntModel spaModel, Resource policyAction, Map connDataMap) {
        String method = "doMultiPathFinding";
        // transform network graph
        // filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2NetworkModel(systemModel);
        try {
            logger.trace(method, "SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            logger.trace(method, "marshalModel(marshalModel(transformedModel) failed -- "+ex);
        }
        Map<String, MCETools.Path> mapConnPaths = new HashMap<>();
        //List<Resource> connList = (List<Resource>)connDataMap.get("connections");
        List<Map> dataMapList = (List<Map>)connDataMap.get("imports");
        // get source and destination nodes
        JSONObject jsonConnReqs = null;
        for (Map entry : dataMapList) {
            if (!entry.containsKey("data") || !entry.containsKey("type") || !entry.containsKey("value")) {
                continue;
            }
            if (entry.get("type").toString().equalsIgnoreCase("JSON")) {
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonObj = (JSONObject) parser.parse((String) entry.get("value"));
                    if (jsonConnReqs == null) {
                        jsonConnReqs = jsonObj;
                    } else { // merge
                        for (Object key: jsonObj.keySet()) {
                            jsonConnReqs.put(key, jsonObj.get(key));
                        }
                    }
                } catch (ParseException e) {                    
                    throw logger.throwing(method, String.format("cannot parse json string %s", entry.get("value")), e);
                }
            } else {
                throw logger.error_throwing(method, String.format("cannot import policyData of %s type", entry.get("type")));
            }
        }
        if (jsonConnReqs == null || jsonConnReqs.isEmpty()) {
            throw logger.error_throwing(method, String.format("received none connection request for policy <%s>", policyAction));
        }
        //Loop through multi-path requests (each for one multi-point vlan bridge (MPVB) connection)
        for (Object connReq: jsonConnReqs.keySet()) {
            String connId = (String) connReq;
            List<Resource> terminals = new ArrayList<>();
            JSONObject jsonConnReq = (JSONObject)jsonConnReqs.get(connReq);
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
            // For 3rd through Tth terminals, connect them to one of openflow nodes in the path
            for (Resource terminalX : terminals) {
                MCETools.Path bridgePath = connectTerminalToPath(transformedModel, mpvbPath, terminalX, jsonConnReq);
                if (bridgePath == null) {
                    throw logger.error_throwing(method, String.format("cannot find bridging path in connection '%s' for terminal '%s'", connId, terminalX));
                }
                mpvbPath.addAll(bridgePath);
                mpvbPath.getOntModel().add(bridgePath.getOntModel().getBaseModel());
            }

            transformedModel.add(mpvbPath.getOntModel());
            mapConnPaths.put(connId, mpvbPath);
        }
        return mapConnPaths;
    }

    private MCETools.Path connectTerminalToPath(OntModel transformedModel, MCETools.Path mpvbPath, Resource terminalX, JSONObject jsonConnReq) {
        String method = "connectTerminalToPath";
        Resource bridgeOpenflowService = checkTerminalOnPath(transformedModel, mpvbPath, terminalX);
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
                bridgeVlanTag = Integer.toOctalString(vlanRange.getRandom());
            }
            MCETools.Path bridgePath = new MCETools.Path();
            OntModel bridgePathModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            Statement bridgeHop = bridgePathModel.createStatement(bridgeOpenflowService, Nml.connectsTo, terminalX);
            bridgePathModel.add(bridgeHop);
            bridgePath.add(bridgeHop);
            bridgePathModel = createBridgePathFlows(transformedModel, mpvbPath, bridgePathModel, bridgeOpenflowService, bridgePort, bridgeVlanTag);
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
                            bridgePathModel = createBridgePathFlows(transformedModel, mpvbPath, bridgePathModel, bridgeOpenflowService, bridgePort, bridgeVlanTag);
                            if (bridgePathModel == null) {
                                continue;
                            }
                            // return the first feasible bridge path @TODO: picking with optimization criteria
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

    //@TODO: Create separate ARP and IPv4 (0x806 | 0x800) flows for static mac forwarding.
    // The ARP flows will broadcast to VLAN, while IPv4 flows will do src / dst mac matching.
    private OntModel createBridgePathFlows(OntModel transformedModel, MCETools.Path mpvbPath, OntModel bridgePathModel,
            Resource bridgeOpenflowService, Resource bridgePort, String bridgeVlanTag) {
        // create VLAN bridging flows with bridgeOpenflowService and bridgePort and add to l2PathModel
        OntModel mpvbModel = mpvbPath.getOntModel();
        Map<Resource, Map> mpvpFlowMap = new HashMap();
        Resource resFlowTable = null;
        // find existing ports and flows from mpvbModel
        String sparql_flowin = "SELECT ?table ?flow ?port_in ?vlan_in WHERE {"
                + String.format("<%s> mrs:providesFlow ?flow. ", bridgeOpenflowService)
                + "?table mrs:hasFlow ?flow. "
                + "?flow mrs:flowMatch ?match_port. "
                + "?match_port mrs:type \"in_port\". "
                + "?match_port mrs:value ?port_in. "
                + "?flow mrs:flowMatch ?match_vlan. "
                + "?match_vlan mrs:type \"dl_vlan\". "
                + "?match_vlan mrs:value ?vlan_in. "
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
        Character bridgePortActionOrder = 'A'; // order of actions in bridgeFlow will be A-B-C-D-D
        Resource resBridgeFlowAction = RdfOwl.createResource(bridgePathModel, URI_action(resBridgeFlow.getURI(), (bridgePortActionOrder++).toString()), Mrs.FlowRule);
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowAction, resBridgeFlowAction));
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlowAction, Mrs.type, "strip_vlan"));
        bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlowAction, Mrs.value, "strip_vlan"));
        for (Resource mpvbFlow : mpvpFlowMap.keySet()) {
            Map flowParams = mpvpFlowMap.get(mpvbFlow);
            String flowInPort = (String) flowParams.get("port_in");
            String flowInVlan = (String) flowParams.get("vlan_in");
            String[] flowOutPorts = ((String) flowParams.get("port_out")).split(",");
            String[] flowOutVlans = ((String) flowParams.get("vlan_out")).split(",");
            // order of actions in existing mpvbFlow: A-B-C-(D-E-...)->this = 'A' + flowOutPorts.length*2+1 
            Character flowActionOrder = (char) ('A' + flowOutPorts.length * 2 + 1);
            // add to mpvbFlow: actions A: set vlan id to bridgeVlanTag B: output to bridgePortName
            Resource resFlowActionA = RdfOwl.createResource(bridgePathModel, URI_action(mpvbFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(mpvbFlow, Mrs.flowAction, resFlowActionA));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.type, "mod_vlan_vid"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.value, bridgeVlanTag));
            Resource resFlowActionB = RdfOwl.createResource(bridgePathModel, URI_action(mpvbFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(mpvbFlow, Mrs.flowAction, resFlowActionB));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.type, "output"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.value, bridgePortName));
            // add to bridgeFlow: actions A: to set vlan id to flowInVlan B: output to flowInPort
            resFlowActionA = RdfOwl.createResource(bridgePathModel, URI_action(resBridgeFlow.getURI(), (bridgePortActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowAction, resFlowActionA));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.type, "mod_vlan_vid"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionA, Mrs.value, flowInVlan));
            resFlowActionB = RdfOwl.createResource(bridgePathModel, URI_action(resBridgeFlow.getURI(), (bridgePortActionOrder++).toString()), Mrs.FlowRule);
            bridgePathModel.add(bridgePathModel.createStatement(resBridgeFlow, Mrs.flowAction, resFlowActionB));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.type, "output"));
            bridgePathModel.add(bridgePathModel.createStatement(resFlowActionB, Mrs.value, flowInPort));
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

    private void exportPolicyData(OntModel spaModel, Resource resPolicy, String connId, MCETools.Path l2Path) {
        String method = "exportPolicyData";
        // find Connection policy -> exportTo -> policyData
        String sparql = "SELECT ?data ?type ?value ?format WHERE {"
                + String.format("<%s> a spa:PolicyAction. ", resPolicy.getURI())
                + String.format("<%s> spa:type 'MCE_MultiPointVlanBridge'. ", resPolicy.getURI())
                + String.format("<%s> spa:exportTo ?data . ", resPolicy.getURI())
                + "OPTIONAL {?data spa:type ?type.} "
                + "OPTIONAL {?data spa:value ?value.} "
                + "OPTIONAL {?data spa:format ?format.} "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resData = querySolution.get("data").asResource();
            RDFNode dataType = querySolution.get("type");
            RDFNode dataValue = querySolution.get("value");
            // add to export data with references to (terminal (src/dst) vlan labels from l2Path
            List<QuerySolution> terminalVlanSolutions = MCETools.getTerminalVlanLabels(l2Path);
            // require two terminal vlan ports and labels.
            if (solutions.isEmpty()) {
                logger.error_throwing(method, "failed to find '2' terminal Vlan tags for " + l2Path);
            }
            if (dataType == null) {
                spaModel.add(resData, Spa.type, "JSON");
            } else if (!dataType.toString().equalsIgnoreCase("JSON")) {
                continue;
            }
            JSONObject jsonValue = new JSONObject();
            if (dataValue != null) {
                JSONParser parser = new JSONParser();
                try {
                    jsonValue = (JSONObject)parser.parse(dataValue.toString());
                } catch (ParseException e) {
                    throw logger.throwing(method, String.format("cannot parse json string %s", dataValue), e);
                }
            }
            JSONArray jsonHops = new JSONArray();
            for (QuerySolution aSolution : terminalVlanSolutions) {
                JSONObject hop = new JSONObject();
                Resource bidrPort = aSolution.getResource("bp");
                Resource vlanTag = aSolution.getResource("vlan");
                hop.put("uri", bidrPort.toString());
                hop.put("vlan_tag", vlanTag.toString());
                jsonHops.add(hop);
            }
            jsonValue.put(connId, jsonHops);
            //add output as spa:value of the export resrouce
            String exportValue = jsonValue.toJSONString();
            if (querySolution.contains("format")) {
                String exportFormat = querySolution.get("format").toString();
                try {
                    exportValue = MCETools.formatJsonExport(exportValue, exportFormat);
                } catch (Exception ex) {
                    logger.warning(method, "formatJsonExport exception and ignored: "+ ex);
                    continue;
                }
            }
            //@TODO: common logic
            if (dataValue != null) {
                spaModel.remove(resData, Spa.value, dataValue);
            }
            spaModel.add(resData, Spa.value, exportValue);
        }
    }
}