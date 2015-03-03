/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.driver.openstack.OpenStackModelBuilder;
import net.maxgigapop.www.rains.ontmodel.Spa;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_VMFilterPlacement implements IModelComputationElement {
    private static final Logger log = Logger.getLogger(MCE_VMFilterPlacement.class.getName());
    
    @Override
    @Asynchronous
    public Future<DeltaBase> process(ModelBase systemModel, DeltaBase annotatedDelta) {
        // $$ MCE_VMFilterPlacement deals with add model only for now.
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw new EJBException(String.format("%s::process ", this.getClass().getName()));
        }
        try {
            log.log(Level.INFO, "\n>>>MCE_VMFilterPlacement--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n" +
                "SELECT ?vm ?policy ?data ?type ?value WHERE {"
                + "?vm a nml:Node ."
                + "?vm spa:dependOn ?policy . "
                + "?policy a spa:Placement. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?type. ?data spa:value ?value. "
                + "FILTER not exists {?policy spa:dependOn ?other} "
                + "}";        
        Map<Resource, List> vmPolicyMap = new HashMap<>();
        Query query = QueryFactory.create(sparqlString);
        QueryExecution qexec = QueryExecutionFactory.create(query, annotatedDelta.getModelAddition().getOntModel());
        ResultSet r = (ResultSet) qexec.execSelect();
        while(r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resVM = querySolution.get("vm").asResource();
            if (!vmPolicyMap.containsKey(resVM)) {
                List policyList = new ArrayList<>();
                vmPolicyMap.put(resVM, policyList);
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
            vmPolicyMap.get(resVM).add(policyData);
        }
        
        DeltaBase outputDelta = annotatedDelta.clone();
        
        for (Resource vm: vmPolicyMap.keySet()) {
            //1. compute placement based on filter/match criteria *data*
            // returned placementModel contains the VM as well as hosting Node/Topology and HypervisorService from systemModel
            //$$ TODO: virtual node should be named and tagged using URI and/or polocy/criteria data in spaModel  
            OntModel placementModel = this.doPlacement(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), vm, vmPolicyMap.get(vm));
            if (placementModel == null)
                throw new EJBException(String.format("%s::process cannot resolve any policy to place %s", this.getClass().getName(), vm));
                        
            //2. merge the placement satements into spaModel
            outputDelta.getModelAddition().getOntModel().add(placementModel.getBaseModel());
            try {
                log.log(Level.INFO, "\n>>>MCE_VMFilterPlacement--outputDelta(stage 2)=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
            } catch (Exception ex) {
                Logger.getLogger(MCE_VMFilterPlacement.class.getName()).log(Level.SEVERE, null, ex);
            }

            //$$ 3. update export data ??
            //this.updateExportData(outputDelta.getModelAddition().getOntModel(), placementModel, vm);
            
            //4. remove policy and all related SPA statements receursively under vm from spaModel
            List<Statement> listStmtsToRemove = new ArrayList<>();
            Resource resVm = outputDelta.getModelAddition().getOntModel().getResource(vm.getURI());
            ModelUtil.listRecursiveDownTree(resVm, Spa.getURI(), listStmtsToRemove);
            if (listStmtsToRemove.isEmpty())
                throw new EJBException(String.format("%s::process cannot remove SPA statements under %s", this.getClass().getName(), vm));
            outputDelta.getModelAddition().getOntModel().remove(listStmtsToRemove);
            
            //$$ TODO: change VM URI (and all other virtual resources) into a unique string either during compile or in stitching action
            //$$ TODO: Add dependOn->Abstraction annotation to root level spaModel and add a generic Action to remvoe that abstract nml:Topology
        }
        try {
            log.log(Level.INFO, "\n>>>MCE_VMFilterPlacement--outputDelta(stage 3)=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_VMFilterPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new AsyncResult(outputDelta);
    }
    
    //?? Use current containing abstract Topology ?
    // ignore if dependOn 'Abstraction'
    private OntModel doPlacement(OntModel systemModel, OntModel spaModel, Resource vm, List<Map> placementCriteria) {
        OntModel placementModel = null;
        for (Map filterCriterion: placementCriteria) {
            if (!filterCriterion.containsKey("data") || !filterCriterion.containsKey("type") || !filterCriterion.containsKey("value")) 
                continue;
            if (((String) filterCriterion.get("type")).equalsIgnoreCase(Nml.Topology.getURI())) {
                OntModel hostModel = filterTopologyNode(systemModel, vm, (String) filterCriterion.get("value"));
                if (hostModel == null)
                  throw new EJBException(String.format("%s::process cannot place %s based on polocy %s", this.getClass().getName(), vm, filterCriterion.get("policy")));
                //$$ create VM resource and relation
                //$$ assemble placementModel;
                if (placementModel == null) {
                    placementModel = hostModel;
                } else {
                    placementModel.add(hostModel.getBaseModel());
                }
            }
            // ? place to a specific Node ?
            //$$ Other types of filter methods have yet to be implemented.
        }
        return placementModel;
    }

    private OntModel filterTopologyNode(OntModel systemModel, Resource resVm, String topologyUri) {
        OntModel hostModel = null;
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "SELECT ?topology ?nodeorvpc ?hvservice WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasNode ?nodeorvpc ."
                + "?nodeorvpc a nml:Node . "
                + "?nodeorvpc nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + String.format("FILTER (?topology = <%s>) ", topologyUri)
                + "}";        
        Query query = QueryFactory.create(sparqlString);
        QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resTopology = querySolution.get("topology").asResource();
            Resource resHostOrVpc = querySolution.get("nodeorvpc").asResource();
            Resource resHvService = querySolution.get("hvservice").asResource();
            //$$ for now, return the first found node/topology
            //$$ TODO: in future, matching capability critria and (randomize or return list of candidates)
            if (hostModel == null) {
                hostModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            }
            hostModel.add(resTopology, RdfOwl.type, Nml.Topology);
            hostModel.add(resTopology, Nml.hasNode, resHostOrVpc);
            hostModel.add(resHostOrVpc, RdfOwl.type, Nml.Node);
            hostModel.add(resHostOrVpc, Nml.hasService, resHvService);
            hostModel.add(resHvService, RdfOwl.type, Mrs.HypervisorService);
            hostModel.add(resHostOrVpc, Nml.hasNode, resVm);
            hostModel.add(resHvService, Mrs.providesVM, resVm);
            return hostModel;
        }
        // if no host node found, try topology level hypervisor (vpc)
        sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "SELECT ?topology ?nodeorvpc ?hvservice WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasNode ?nodeorvpc ."
                + "?nodeorvpc a nml:Node . "
                + "?nodeorvpc nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + String.format("FILTER (?topology = <%s>) ", topologyUri)
                + "}";   
        query = QueryFactory.create(sparqlString);
        qexec = QueryExecutionFactory.create(query, systemModel);
        r = (ResultSet) qexec.execSelect();
        if (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resTopology = querySolution.get("topology").asResource();
            Resource resHostOrVpc = querySolution.get("nodeorvpc").asResource();
            Resource resHvService = querySolution.get("hvservice").asResource();
            if (hostModel == null) {
                hostModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            }
            hostModel.add(resTopology, RdfOwl.type, Nml.Topology);
            hostModel.add(resTopology, Nml.hasTopology, resHostOrVpc);
            hostModel.add(resTopology, Nml.hasService, Mrs.VirtualCloudService);
            hostModel.add(resHostOrVpc, RdfOwl.type, Nml.Topology);
            hostModel.add(resHostOrVpc, Nml.hasService, resHvService);
            hostModel.add(resHvService, RdfOwl.type, Mrs.HypervisorService);
            hostModel.add(resHostOrVpc, Nml.hasNode, resVm);
            hostModel.add(resHvService, Mrs.providesVM, resVm);
            return hostModel;
        }
        return null;
    }
    
    private void exportPolicyData(OntModel spaModel, Resource vm) {
        //$$ find Placement policy -> exportTo -> policyData
        //$$ export type (Nml:Topology / Nml:Node): value (host *URI*)
    }

    //$$ TODO: matchingRegExURIFilter
    //$$ TODO: hostCapabilityFilter's
    //$$ placeMatchingRegExURI
    //$$ placeWithMultiFilter ??
}
