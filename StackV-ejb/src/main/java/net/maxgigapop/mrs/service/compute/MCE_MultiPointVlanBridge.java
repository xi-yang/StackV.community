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
            Resource terminalR = terminals.get(0);
            Resource terminalR2 = terminals.get(1);
            terminals.remove(0);
            terminals.remove(1);
            List<MCETools.Path> KSP = MCETools.computeFeasibleL2KSP(transformedModel, terminalR, terminalR2, jsonConnReq);
            if (KSP == null || KSP.size() == 0) {
                throw new EJBException(String.format("%s::process doMultiPathFinding cannot find initial feasible path for connection '%s' between '%s' and '%s'", 
                        MCE_MPVlanConnection.class.getName(), connId, terminalR, terminalR2));
            }
            MCETools.Path mpvbPath = MCETools.getLeastCostPath(KSP); //(Could also be pick 2nd and 3rd for disturbing search)

            //$$ For 3rd through Tth terminals, connect them to one of openflow nodes in the path
            for (Resource terminalX: terminals) {
                connectTerminalToPath(transformedModel, mpvbPath, terminalX, jsonConnReq);
            }
            //@TODO: add VLAN constraints in openflow computation
            
            transformedModel.add(mpvbPath.getOntModel());
            mapConnPaths.put(connId, mpvbPath);
        }
        return mapConnPaths;
    }

    private void connectTerminalToPath(OntModel transformedModel, MCETools.Path connPath, Resource terminalX, JSONObject jsonConnReq) {
        Resource openflowNode = getOpenflowNodeOnPath(transformedModel, connPath);
        //@TODO: use listOpenflowNodesOnPath to loop through the openflow nodes for feasible addedPath?
        if (openflowNode == null) {
            throw new EJBException(String.format("%s::process connectTerminalToPath cannot getOpenflowNodeOnPath", 
                            MCE_MPVlanConnection.class.getName()));
        }

        //@TODO: modify jsonConnReq to add TE spec for openflowNode? (init vlanRange in TE Params)

        List<MCETools.Path> KSP = MCETools.computeFeasibleL2KSP(transformedModel, openflowNode, terminalX, jsonConnReq);
        if (KSP == null || KSP.size() == 0) {
            throw new EJBException(String.format("%s::process doMultiPathFinding cannot find feasible path for add connection to '%s'",
                    MCE_MPVlanConnection.class.getName(), terminalX));
        }
        // pick the added path as shortest of KSP
        MCETools.Path addedPath = MCETools.getLeastCostPath(KSP); 
        //$$ look for last common OpenFlow 'hop' in addedPath and remove all preceding hops 
            //$$ make sure to remove the statements in addedPath.ontModel

        //$$ identify the 'bridging" OpenFlow node
                        
        //$$ create VLAN bridging flows
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