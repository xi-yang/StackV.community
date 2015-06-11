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
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.www.rains.ontmodel.Spa;

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
    public Future<DeltaBase> process(ModelBase systemModel, DeltaBase annotatedDelta) {        
        try {
            log.log(Level.INFO, "\n>>>MCE_MPVlanConnection--DeltaAddModel=\n" + ModelUtil.marshalModel(annotatedDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // importPolicyData : Link->Connection->List<PolicyData> of terminal Node/Topology
        String sparql =  "SELECT ?link ?policy ?data ?type ?value WHERE {"
                + "?link a nml:Link ."
                + "?link spa:dependOn ?policy . "
                + "?policy a spa:Connection. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?type. ?data spa:value ?value. "
                + "FILTER not exists {?policy spa:dependOn ?other} "
                + "}";        

        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        Map<Resource, List> connPolicyMap = new HashMap<>();
        while(r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resLink = querySolution.get("link").asResource();
            if (!connPolicyMap.containsKey(resLink)) {
                List policyList = new ArrayList<>();
                connPolicyMap.put(resLink, policyList);
            }
            Resource resPolicy = querySolution.get("policy").asResource();
            Resource resData = querySolution.get("data").asResource();
            RDFNode nodeDataType = querySolution.get("type");
            RDFNode nodeDataValue = querySolution.get("value");
            Map policyData = new HashMap<>();
            policyData.put("policy", resPolicy);
            policyData.put("data", resData);
            policyData.put("type", nodeDataType.toString());
            policyData.put("value", nodeDataValue.toString());
            connPolicyMap.get(resLink).add(policyData);
        }
        
        DeltaBase outputDelta = annotatedDelta.clone();

        // compute a List<Model> of MPVlan connections
        for (Resource resLink : connPolicyMap.keySet()) {
            OntModel connModel = this.doPathFinding(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), resLink, connPolicyMap.get(resLink));
            if (connModel == null)
                throw new EJBException(String.format("%s::process cannot find a path for %s", this.getClass().getName(), resLink));
                        
            //2. merge the placement satements into spaModel
            outputDelta.getModelAddition().getOntModel().add(connModel.getBaseModel());

            //3. update policyData this action exportTo 
            this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), resLink, connModel);
            
            //4. remove policy and all related SPA statements receursively under link from spaModel
            //   and also remove all statements that say dependOn this 'policy'
            this.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), resLink);   
        }        
        return new AsyncResult(outputDelta);
    }
    
    private OntModel doPathFinding(OntModel systemModel, OntModel spaModel, Resource resLink, List<Map> connTerminalData) {
        // transform network graph
        // filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2NetworkModel(systemModel);
        try {
            log.log(Level.INFO, "\n>>>MCE_MPVlanConnection--SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        // get source and destination nodes (nodeA, nodeZ) -- only picks fist two terminals for now 
        List<Resource> terminals = new ArrayList<>();
        for (Map entry: connTerminalData) {
            if (!entry.containsKey("data") || !entry.containsKey("type") || !entry.containsKey("value")) 
                continue;
            Resource terminal = systemModel.getResource((String)entry.get("value"));
            if (terminal == null){
                throw new EJBException(String.format("%s::process doPathFinding cannot identify terminal <%s>", (String)entry.get("value")));
            }
            terminals.add(terminal);
        }
        if (terminals.size() < 2) {
            throw new EJBException(String.format("%s::process cannot doPathFinding for %s which provides fewer than 2 terminals", this.getClass().getName(), resLink));
        }
        Resource nodeA = terminals.get(0);
        Resource nodeZ = terminals.get(1);
        // KSP-MP path computation on the connected graph model (point2point for now - will do MP in future)
        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
        List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);
        if (KSP == null || KSP.isEmpty()) {
            throw new EJBException(String.format("%s::process doPathFinding cannot find feasible path for <%s>", resLink));
        }
        // Verify TE constraints (switching label and ?adaptation?), 
        OntModel connectionModel = null;
        Iterator<MCETools.Path> itP = KSP.iterator();
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();
            // verify path
            if(!MCETools.verifyL2Path(transformedModel, candidatePath)) {
                itP.remove();
            }
            // generating connection subnets (statements added to candidatePath) while verifying VLAN availability
            //if(!MCETools.allcocateSubnetsOnL2Path(transformedModel, candidatePath)) {
            //    itP.remove();
            //}

        }
        if (KSP.size() == 0)
            return null;

        // pick the shortest path from remaining/feasible paths in KSP and add to connectionModel
        connectionModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //@@ add subnet intefaces and xconn's
            //$$ extract vlan's
        connectionModel.add(MCETools.getLeastCostPath(KSP));
        return connectionModel;
    }
    
    private void exportPolicyData(OntModel spaModel, Resource resLink, OntModel connModel) {
        // find Placement policy -> exportTo -> policyData
        String sparql = "SELECT ?link ?policyAction ?policyData WHERE {"
                + String.format("<%s> spa:dependOn ?policyAction .", resLink.getURI())
                + "?policyAction a spa:Connection."
                + "?policyAction spa:exportTo ?policyData . "
                + "?policyData a spa:PolicyData . "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        if (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution: solutions) {
            Resource resPolicy = querySolution.get("policyAction").asResource();
            Resource resData = querySolution.get("policyData").asResource();
            //@@ add VLANs to export data (vlan's from connModel)
            //
            // remove VM->exportTo statement so the exportData can be kept in spaModel during receurive removal
            spaModel.remove(resPolicy, Spa.exportTo, resData);
        }
    }

    private void removeResolvedAnnotation(OntModel spaModel, Resource resLink) {
        List<Statement> listStmtsToRemove = new ArrayList<>();
        Resource resVm = spaModel.getResource(resLink.getURI());
        ModelUtil.listRecursiveDownTree(resVm, Spa.getURI(), listStmtsToRemove);
        if (listStmtsToRemove.isEmpty()) {
            throw new EJBException(String.format("%s::process cannot remove SPA statements under %s", this.getClass().getName(), resLink));
        }

        String sparql = "SELECT ?anyOther ?policyAction WHERE {"
                + String.format("<%s> spa:dependOn ?policyAction .", resLink.getURI())
                + "?policyAction a spa:Connection."
                + "?anyOther spa:dependOn ?policyAction . "
                + "?policyData a spa:PolicyData . "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        if (r.hasNext()) {
            solutions.add(r.next());
        }

        for (QuerySolution querySolution : solutions) {
            Resource resAnyOther = querySolution.get("anyOther").asResource();
            Resource resPolicy = querySolution.get("policyAction").asResource();
            spaModel.remove(resAnyOther, Spa.dependOn, resPolicy);
        }
        spaModel.remove(listStmtsToRemove);
    }

}
