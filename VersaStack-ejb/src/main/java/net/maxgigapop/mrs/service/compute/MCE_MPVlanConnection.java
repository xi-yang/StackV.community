/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
public class MCE_MPVlanConnection implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_MPVlanConnection.class.getName());
    /*
     ** Simple L2 connection will create new SwitchingSubnet on every transit switching node.
     */

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        try {
            log.log(Level.FINE, "\n>>>MCE_MPVlanConnection--DeltaAddModel Input=\n" + ModelUtil.marshalModel(annotatedDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
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
                throw new EJBException(String.format("%s::process cannot find paths for %s", this.getClass().getName(), policyAction));
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
            log.log(Level.FINE, "\n>>>MCE_MPVlanConnection--DeltaAddModel Output=\n" + ModelUtil.marshalModel(outputDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new AsyncResult(outputDelta);
    }

    private Map<String, MCETools.Path> doMultiPathFinding(OntModel systemModel, OntModel spaModel, Resource policyAction, Map connDataMap) {
        // transform network graph
        // filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2NetworkModel(systemModel);
        try {
            log.log(Level.FINE, "\n>>>MCE_MPVlanConnection--SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
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
                    throw new EJBException(String.format("%s::process doMultiPathFinding cannot parse json string %s", this.getClass().getName(), (String) entry.get("value")));
                }
            } else {
                throw new EJBException(String.format("%s::process doMultiPathFinding does not import policyData of %s type", entry.get("type").toString()));
            }
        }
        if (jsonConnReqs == null || jsonConnReqs.isEmpty()) {
            throw new EJBException(String.format("%s::process doMultiPathFinding receive none connection request for <%s>", this.getClass().getName(), policyAction));
        }
        //@TODO: verify that all connList elements have been covered by jsonConnReqs
        for (Object connReq: jsonConnReqs.keySet()) {
            String connId = (String) connReq;
            List<Resource> terminals = new ArrayList<>();
            JSONObject jsonConnReq = (JSONObject)jsonConnReqs.get(connReq);
            if (jsonConnReq.size() != 2) {
                throw new EJBException(String.format("%s::process cannot doMultiPathFinding for connection '%s' should have exactly 2 terminals.", this.getClass().getName(), connId));
            }
            for (Object key : jsonConnReq.keySet()) {
                Resource terminal = systemModel.getResource((String) key);
                if (!systemModel.contains(terminal, null)) {
                    throw new EJBException(String.format("%s::process doMultiPathFinding cannot identify terminal <%s> in JSON data", this.getClass().getName(), key));
                }
                terminals.add(terminal);
            }
            Resource nodeA = terminals.get(0);
            Resource nodeZ = terminals.get(1);
            // KSP-MP path computation on the connected graph model (point2point for now - will do MP in future)
            Property[] filterProperties = {Nml.connectsTo};
            Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
            List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);
            if (KSP == null || KSP.isEmpty()) {
                throw new EJBException(String.format("%s::process doMultiPathFinding cannot find feasible path for connection '%s'", MCE_MPVlanConnection.class.getName(), connId));
            }
            // Verify TE constraints (switching label and ?adaptation?), 
            Iterator<MCETools.Path> itP = KSP.iterator();
            while (itP.hasNext()) {
                MCETools.Path candidatePath = itP.next();
                // verify path
                //@TODO: TE constraint 
                if (!MCETools.verifyL2Path(transformedModel, candidatePath)) {
                    itP.remove();
                } else {
                    // generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                    //@TODO: TE constraint
                    OntModel l2PathModel = MCETools.createL2PathVlanSubnets(transformedModel, candidatePath, jsonConnReq);
                    if (l2PathModel == null) {
                        itP.remove();
                    } else {
                        candidatePath.setOntModel(l2PathModel);
                    }
                }
            }
            if (KSP.size() == 0) {
                throw new EJBException(String.format("%s::process doMultiPathFinding cannot find feasible path for connection '%s'", MCE_MPVlanConnection.class.getName(), connId));
            }
            // pick the shortest path from remaining/feasible paths in KSP
            MCETools.Path connPath = MCETools.getLeastCostPath(KSP);
            transformedModel.add(connPath.getOntModel());
            mapConnPaths.put(connId, connPath);
        }
        return mapConnPaths;
    }

    private void exportPolicyData(OntModel spaModel, Resource resPolicy, String connId, MCETools.Path l2Path) {
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
            List<QuerySolution> terminalVlanSolutions = getTerminalVlanLabels(l2Path);
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

    private List<QuerySolution> getTerminalVlanLabels(MCETools.Path l2path) {
        OntModel model = l2path.getOntModel();
        String sparql = String.format("SELECT ?bp ?vlan ?tag WHERE {"
                + " ?bp a nml:BidirectionalPort. "
                + " ?bp nml:hasLabel ?vlan."
                + " ?vlan nml:value ?tag."
                + " ?vlan nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>}");
        ResultSet r = ModelUtil.sparqlQuery(model, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        return solutions;
    }
}