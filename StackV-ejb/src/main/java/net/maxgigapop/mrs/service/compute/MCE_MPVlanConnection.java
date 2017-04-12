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
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
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
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_MPVlanConnection implements IModelComputationElement {

    private static final StackLogger logger = new StackLogger(MCE_MPVlanConnection.class.getName(), "MCE_MPVlanConnection");
    /*
     ** Simple L2 connection will create new SwitchingSubnet on every transit switching node.
     */

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        String method = "process";
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
                + "?policy spa:type 'MCE_MPVlanConnection'. "
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

        // compute a List<Model> of MPVlan connections
        for (Resource policyAction : connPolicyMap.keySet()) {
            Map<String, MCETools.Path> l2pathMap = this.doMultiPathFinding(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), policyAction, connPolicyMap.get(policyAction));
            if (l2pathMap == null) {
                throw logger.error_throwing(method, String.format("cannot find paths for %s", policyAction));
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
            logger.trace(method, "\n>>>MCE_MPVlanConnection--SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            logger.trace(method, "marshalModel(transformedModel) failed -- "+ex);
        }
        Map<String, MCETools.Path> mapConnPaths = new HashMap<>();
        //List<Resource> connList = (List<Resource>)connDataMap.get("connections");
        List<Map> dataMapList = (List<Map>)connDataMap.get("imports");
        // get source and destination nodes (nodeA, nodeZ) -- only picks fist two terminals for now 
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
        //@TODO: verify that all connList elements have been covered by jsonConnReqs
        for (Object connReq: jsonConnReqs.keySet()) {
            String connId = (String) connReq;
            List<Resource> terminals = new ArrayList<>();
            JSONObject jsonConnReq = (JSONObject)jsonConnReqs.get(connReq);
            if (jsonConnReq.size() != 2) {
                throw logger.error_throwing(method, String.format("cannot find path for connection '%s' - request must have exactly 2 terminals", connId));
            }
            for (Object key : jsonConnReq.keySet()) {
                Resource terminal = systemModel.getResource((String) key);
                if (!systemModel.contains(terminal, null)) {
                    throw logger.error_throwing(method, String.format("cannot identify terminal <%s> in JSON data", key));
                }
                terminals.add(terminal);
            }
            Resource nodeA = terminals.get(0);
            Resource nodeZ = terminals.get(1);
            // KSP-MP path computation on the connected graph model (point2point for now - will do MP in future)
            List<MCETools.Path> KSP;
            try {
                KSP = MCETools.computeFeasibleL2KSP(transformedModel, nodeA, nodeZ, jsonConnReq);
            } catch (Exception ex) {
                throw logger.throwing(method, String.format("connectionId=%s computeFeasibleL2KSP(nodeA=%s, nodeZ=%s, jsonConnReq=%s) exception -- ", connId, nodeA, nodeZ, jsonConnReq), ex);
            }
            if (KSP == null || KSP.size() == 0) {
                throw logger.error_throwing(method, String.format("cannot find feasible path for connection '%s'", connId));
            }
            // pick the shortest path from remaining/feasible paths in KSP
            MCETools.Path connPath = MCETools.getLeastCostPath(KSP);
            transformedModel.add(connPath.getOntModel());
            mapConnPaths.put(connId, connPath);
        }
        return mapConnPaths;
    }

    private void exportPolicyData(OntModel spaModel, Resource resPolicy, String connId, MCETools.Path l2Path) {
        String method = "exportPolicyData";
        // find Connection policy -> exportTo -> policyData
        String sparql = "SELECT ?data ?type ?value ?format WHERE {"
                + String.format("<%s> a spa:PolicyAction. ", resPolicy.getURI())
                + String.format("<%s> spa:type 'MCE_MPVlanConnection'. ", resPolicy.getURI())
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
                throw logger.error_throwing(method, "failed to find '2' terminal Vlan tags for " + l2Path);
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