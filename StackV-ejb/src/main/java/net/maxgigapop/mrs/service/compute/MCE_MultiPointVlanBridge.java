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
            + "			\"required\": \"false\",\n"
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
        try {
            logger.trace(method, "SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            logger.trace(method, "marshalModel(marshalModel(transformedModel) failed -- "+ex);
        }
        
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
            // For 3rd through Tth terminals, connect them to one of openflow nodes in the path
            for (Resource terminalX : terminals) {
                MCETools.Path bridgePath = connectTerminalToPath(transformedModel, mpvbPath, terminalX, jsonConnReq);
                if (bridgePath == null) {
                    throw logger.error_throwing(method, String.format("cannot find bridging path in connection '%s' for terminal '%s'", connId, terminalX));
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
}