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
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;
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
    private static final Logger log = Logger.getLogger(MCE_MultiPointVlanBridge.class.getName());

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        try {
            log.log(Level.FINE, "\n>>>MCE_MultiPointVlanBridge--DeltaAddModel Input=\n" + ModelUtil.marshalModel(annotatedDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MultiPointVlanBridge.class.getName()).log(Level.SEVERE, null, ex);
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
                throw new EJBException(String.format("%s::process cannot find multi-point bridge paths for %s", this.getClass().getName(), policyAction));
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
            log.log(Level.FINE, "\n>>>MCE_MultiPointVlanBridge--DeltaAddModel Output=\n" + ModelUtil.marshalModel(outputDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MultiPointVlanBridge.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new AsyncResult(outputDelta);
    }

    private Map<String, MCETools.Path> doMultiPathFinding(OntModel systemModel, OntModel spaModel, Resource policyAction, Map connDataMap) {
        // transform network graph
        // filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2NetworkModel(systemModel);
        try {
            log.log(Level.FINE, "\n>>>MCE_MultiPointVlanBridge--SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MultiPointVlanBridge.class.getName()).log(Level.SEVERE, null, ex);
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
                    throw new EJBException(String.format("%s::process doMultiPathFinding cannot parse json string %s", this.getClass().getName(), (String) entry.get("value")));
                }
            } else {
                throw new EJBException(String.format("%s::process doMultiPathFinding does not import policyData of %s type", entry.get("type").toString()));
            }
        }
        if (jsonConnReqs == null || jsonConnReqs.isEmpty()) {
            throw new EJBException(String.format("%s::process doMultiPathFinding receive none connection request for <%s>", this.getClass().getName(), policyAction));
        }
        //Loop through multi-path requests (each for one multi-point vlan bridge (MPVB) connection)
        for (Object connReq: jsonConnReqs.keySet()) {
            String connId = (String) connReq;
            List<Resource> terminals = new ArrayList<>();
            JSONObject jsonConnReq = (JSONObject)jsonConnReqs.get(connReq);
            if (jsonConnReq.size() < 3) {
                throw new EJBException(String.format("%s::process cannot doMultiPathFinding for connection '%s' should have at least 3 terminals.", this.getClass().getName(), connId));
            }
            for (Object key : jsonConnReq.keySet()) {
                Resource terminal = systemModel.getResource((String) key);
                if (!systemModel.contains(terminal, null)) {
                    throw new EJBException(String.format("%s::process doMultiPathFinding cannot identify terminal <%s> in JSON data", this.getClass().getName(), key));
                }
                terminals.add(terminal);
            }
            
            // Create init path starting from root terminaR to terminalR2 (This order could be reshuffled to disturb search)
            Resource terminal1 = terminals.get(0);
            terminals.remove(0);
            Resource terminal2 = terminals.get(0);
            terminals.remove(0);
            List<MCETools.Path> feasibleKSP12 = MCETools.computeFeasibleL2KSP(transformedModel, terminal1, terminal2, jsonConnReq);
            if (feasibleKSP12 == null || feasibleKSP12.size() == 0) {
                throw new EJBException(String.format("%s::process doMultiPathFinding cannot find initial feasible path for connection '%s' between '%s' and '%s'", 
                        MCE_MPVlanConnection.class.getName(), connId, terminal1, terminal2));
            }
            MCETools.Path mpvbPath = MCETools.getLeastCostPath(feasibleKSP12); //(Could also be pick 2nd and 3rd for disturbing search)

            // For 3rd through Tth terminals, connect them to one of openflow nodes in the path
            for (Resource terminalX : terminals) {
                MCETools.Path bridgePath = connectTerminalToPath(transformedModel, mpvbPath, terminalX, jsonConnReq);
                //@TODO: merge bridgePath to connPath | exception if null
            }

            transformedModel.add(mpvbPath.getOntModel());
            mapConnPaths.put(connId, mpvbPath);
        }
        return mapConnPaths;
    }

    private MCETools.Path connectTerminalToPath(OntModel transformedModel, MCETools.Path mpvbPath, Resource terminalX, JSONObject jsonConnReq) {
        List<Resource> openflowPorts = listOpenflowPortsOnPath(transformedModel, mpvbPath);
        if (openflowPorts.isEmpty()) {
            throw new EJBException(String.format("%s::process connectTerminalToPath cannot getOpenflowNodeOnPath", 
                            MCE_MPVlanConnection.class.getName()));
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
                Resource bridgeOpenflowService = shortenBridgePath(transformedModel, mpvbPath, bridgePath);
                Resource bridgePort = bridgePath.get(0).getSubject();
                // initial vlanRange in of bridgePort could be full range but will eventually constrained by terminalX
                if (MCETools.verifyL2Path(transformedModel, bridgePath)) {
                    // generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                    OntModel bridgePathModel = MCETools.createL2PathVlanSubnets(transformedModel, bridgePath, jsonConnReq);
                    if (bridgePathModel != null) {
                        // create VLAN bridging flows with bridgeOpenflowService and bridgePort and add to l2PathModel
                        OntModel mpvbModel = mpvbPath.getOntModel();
                        Map mpvpFlowMap = new HashMap();
                        // find existing ports and flows from mpvbModel
                        String sparql_flowin = "SELECT ?table ?flow ?port_in ?vlan_in WHERE {"
                                + String.format("<%s> mrs:hasFlowTable ?table. ", bridgeOpenflowService)
                                + "?table mrs:hasFlow ?flow. "
                                + "?flow mrs:flowMatch ?match_port. "
                                + "?match_port mrs:type \"in_port\". "
                                + "?match_port mrs:value \"?port_in\". "
                                + "?flow mrs:flowMatch ?match_vlan. "
                                + "?match_vlan mrs:type \"dl_vlan\". "
                                + "?match_vlan mrs:value \"?vlan_in\". "
                                + "}";
                        ResultSet rs = ModelUtil.sparqlQuery(mpvbModel, sparql_flowin);
                        while (rs.hasNext()) {
                            QuerySolution qs = rs.next();
                            Resource resTable = qs.getResource("table");
                            Resource resFlow = qs.getResource("flow");
                            String strPort = qs.get("port_in").toString();
                            String strVlan = qs.get("vlan_in").toString();
                            Map flowParamMap = new HashMap();
                            mpvpFlowMap.put(resFlow, flowParamMap);
                            flowParamMap.put("table", resTable);
                            flowParamMap.put("port_in", strPort);
                            flowParamMap.put("vlan_in", strVlan);
                        }
                        String sparql_flowout = "SELECT ?table ?flow ?port_out ?vlan_out WHERE {"
                                + String.format("<%s> mrs:hasFlowTable ?table. ", bridgeOpenflowService)
                                + "?table mrs:hasFlow ?flow. "
                                + "?flow mrs:flowMatch ?action_port. "
                                + "?action_port mrs:type \"ourput\". "
                                + "?action_port mrs:value \"?port_out\". "
                                + "?flow mrs:flowMatch ?action_vlan. "
                                + "?action_vlan mrs:type \"mod_vlan_vid\". "
                                + "?action_vlan mrs:value \"?vlan_out\". "
                                + "}";
                        rs = ModelUtil.sparqlQuery(mpvbModel, sparql_flowout);
                        while (rs.hasNext()) {
                            QuerySolution qs = rs.next();
                            Resource resTable = qs.getResource("table");
                            Resource resFlow = qs.getResource("flow");
                            String strPort = qs.get("port_out").toString();
                            String strVlan = qs.get("vlan_out").toString();
                            Map flowParamMap = null;
                            if (mpvpFlowMap.containsKey(resFlow)) {
                                flowParamMap = (Map)mpvpFlowMap.get(resFlow);
                            } else {
                                flowParamMap = new HashMap();
                                flowParamMap.put("table", resTable);
                                mpvpFlowMap.put(resFlow, flowParamMap);
                            }
                            flowParamMap.put("port_out", strPort);
                            flowParamMap.put("vlan_out", strVlan);
                        }
                        if (mpvpFlowMap.size() < 2) {
                            //@ exception
                        }
                        // find bridge port name and vlan
                        String addPortName = MCETools.getNameForPort(transformedModel, bridgePort);
                        String sparql_bridge = "SELECT ?vlan_in WHERE {"
                                + "?flow mrs:flowMatch ?match_port. "
                                + "?match_port mrs:type \"in_port\". "
                                + String.format("?match_port mrs:value \"?%s\". ", addPortName)
                                + "?flow mrs:flowMatch ?match_vlan. "
                                + "?match_vlan mrs:type \"dl_vlan\". "
                                + "?match_vlan mrs:value \"?vlan_in\". "
                                + "}";
                        rs = ModelUtil.sparqlQuery(bridgePathModel, sparql_bridge);
                        if (!rs.hasNext()) {
                            //@ exception
                        }
                        String addVlanTag = rs.next().get("vlan_in").toString();
                        //@ add VLAN flows to bridgePort from other ports
                        //@ add VLAN flows from bridgePort to other ports
                        bridgePath.setOntModel(bridgePathModel);
                        return bridgePath; //@TODO: Use optimization criteria instead of taking the first feasible?
                    }
                }
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
        return null ;
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
                throw new EJBException("exportPolicyData failed to find '2' terminal Vlan tags for " + l2Path);
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
                    throw new EJBException(String.format("%s::exportPolicyData  cannot parse json string %s due to: %s", this.getClass().getName(), dataValue.toString(), e));
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
                } catch (EJBException ex) {
                    log.log(Level.WARNING, ex.getMessage());
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