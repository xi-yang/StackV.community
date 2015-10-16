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
import java.io.StringWriter;
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
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_VMFilterPlacement implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_VMFilterPlacement.class.getName());

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(ModelBase systemModel, ServiceDelta annotatedDelta) {
        // $$ MCE_VMFilterPlacement deals with add model only for now.
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw new EJBException(String.format("%s::process ", this.getClass().getName()));
        }
        try {
            log.log(Level.INFO, "\n>>>MCE_VMFilterPlacement--DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        // importPolicyData
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "SELECT ?vm ?policy ?data ?dataType ?dataValue WHERE {"
                + "?vm a nml:Node ."
                + "?vm spa:dependOn ?policy . "
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_VMFilterPlacement'. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?dataType. ?data spa:value ?dataValue. "
                + "FILTER (not exists {?policy spa:dependOn ?other}) "
                + "}";
        Map<Resource, List> vmPolicyMap = new HashMap<>();
        Query query = QueryFactory.create(sparqlString);
        QueryExecution qexec = QueryExecutionFactory.create(query, annotatedDelta.getModelAddition().getOntModel());
        ResultSet r = (ResultSet) qexec.execSelect();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resVM = querySolution.get("vm").asResource();
            if (!vmPolicyMap.containsKey(resVM)) {
                List policyList = new ArrayList<>();
                vmPolicyMap.put(resVM, policyList);
            }
            Resource resPolicy = querySolution.get("policy").asResource();
            Resource resData = querySolution.get("data").asResource();
            RDFNode nodeDataType = querySolution.get("dataType");
            RDFNode nodeDataValue = querySolution.get("dataValue");
            Map policyData = new HashMap<>();
            policyData.put("policy", resPolicy);
            policyData.put("data", resData);
            policyData.put("type", nodeDataType.toString());
            policyData.put("value", nodeDataValue.toString());
            vmPolicyMap.get(resVM).add(policyData);
        }

        ServiceDelta outputDelta = annotatedDelta.clone();

        for (Resource vm : vmPolicyMap.keySet()) {
            //1. compute placement based on filter/match criteria *policyData*
            // returned placementModel contains the VM as well as hosting Node/Topology and HypervisorService from systemModel
            //$$ TODO: virtual node should be named and tagged using URI and/or polocy/criteria data in spaModel  
            OntModel placementModel = this.doPlacement(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), vm, vmPolicyMap.get(vm));
            if (placementModel == null) {
                throw new EJBException(String.format("%s::process cannot resolve any policy to place %s", this.getClass().getName(), vm));
            }

            //2. merge the placement satements into spaModel
            outputDelta.getModelAddition().getOntModel().add(placementModel.getBaseModel());
            /*
             try {
             log.log(Level.FINE, "\n>>>MCE_VMFilterPlacement--outputDelta(stage 2)=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
             } catch (Exception ex) {
             Logger.getLogger(MCE_VMFilterPlacement.class.getName()).log(Level.SEVERE, null, ex);
             }
             */
            //3. update policyData this action exportTo 
            this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), vm);

            //4. remove policy and all related SPA statements receursively under vm from spaModel
            //   and also remove all statements that say dependOn this 'policy'
            MCETools.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), vm);

            //$$ TODO: change VM URI (and all other virtual resources) into a unique string either during compile or in stitching action
            //$$ TODO: Add dependOn->Abstraction annotation to root level spaModel and add a generic Action to remvoe that abstract nml:Topology
        }
        try {
            log.log(Level.FINE, "\n>>>MCE_VMFilterPlacement--outputDelta Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_VMFilterPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new AsyncResult(outputDelta);
    }

    //?? Use current containing abstract Topology ?
    // ignore if dependOn 'Abstraction'
    private OntModel doPlacement(OntModel systemModel, OntModel spaModel, Resource vm, List<Map> placementCriteria) {
        OntModel placementModel = null;
        for (Map filterCriterion : placementCriteria) {
            if (!filterCriterion.containsKey("data") || !filterCriterion.containsKey("type") || !filterCriterion.containsKey("value")) {
                continue;
            }
            if (((String) filterCriterion.get("type")).equalsIgnoreCase(Nml.Topology.getURI())
                    || ((String) filterCriterion.get("type")).equalsIgnoreCase(Nml.Node.getURI())) {
                OntModel hostModel = filterTopologyNode(systemModel, vm, (String) filterCriterion.get("value"));
                if (hostModel == null) {
                    throw new EJBException(String.format("%s::process cannot place %s based on polocy %s", this.getClass().getName(), vm, filterCriterion.get("policy")));
                }
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
        // host node or vpc
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology ?nodeorvpc ?hvservice WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasNode ?nodeorvpc ."
                + "?nodeorvpc a nml:Node . "
                + "?nodeorvpc nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + String.format("FILTER (?nodeorvpc = <%s>)", topologyUri)
                + "}";
        Query query = QueryFactory.create(sparqlString);
        QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resTopology = querySolution.get("topology").asResource();
            Resource resHostOrVpc = querySolution.get("nodeorvpc").asResource();
            Resource resHvService = querySolution.get("hvservice").asResource();
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
        sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology ?nodeorvpc ?hvservice WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasTopology ?nodeorvpc ."
                + "?nodeorvpc a nml:Topology . "
                + "?topology nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + String.format("FILTER (?nodeorvpc = <%s>) ", topologyUri)
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
            hostModel.add(resHostOrVpc, RdfOwl.type, Nml.Topology);
            hostModel.add(resTopology, Nml.hasService, resHvService);
            hostModel.add(resHvService, RdfOwl.type, Mrs.HypervisorService);
            hostModel.add(resHostOrVpc, Nml.hasNode, resVm);
            hostModel.add(resHvService, Mrs.providesVM, resVm);
            return hostModel;
        }
        // if no host node or vpc found, try flat topology with hypervisor
        sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology ?hvservice WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + String.format("FILTER (?topology = <%s>) ", topologyUri)
                + "}";
        query = QueryFactory.create(sparqlString);
        qexec = QueryExecutionFactory.create(query, systemModel);
        r = (ResultSet) qexec.execSelect();
        if (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resHostTopology = querySolution.get("topology").asResource();
            Resource resHvService = querySolution.get("hvservice").asResource();
            if (hostModel == null) {
                hostModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            }
            hostModel.add(resHostTopology, RdfOwl.type, Nml.Topology);
            hostModel.add(resHostTopology, Nml.hasService, resHvService);
            hostModel.add(resHvService, RdfOwl.type, Mrs.HypervisorService);
            hostModel.add(resHostTopology, Nml.hasNode, resVm);
            hostModel.add(resHvService, Mrs.providesVM, resVm);
            return hostModel;
        }
        return null;
    }

    private void exportPolicyData(OntModel spaModel, Resource vm) {
        // find Placement policy -> exportTo -> policyData
        String sparql = "SELECT ?nodeorvpc ?policyAction ?policyData WHERE {"
                + "?nodeorvpc nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + String.format("?hvservice mrs:providesVM <%s> .", vm.getURI())
                + String.format("<%s> spa:dependOn ?policyAction .", vm.getURI())
                + "?policyAction a spa:PolicyAction. "
                + "?policyAction spa:type 'MCE_VMFilterPlacement'. "
                + "?policyAction spa:exportTo ?policyData . "
                + "?policyData a spa:PolicyData . "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resHost = querySolution.get("nodeorvpc").asResource();
            Resource resPolicy = querySolution.get("policyAction").asResource();
            Resource resData = querySolution.get("policyData").asResource();
            // add export data
            /*
             if (spaModel.listStatements(resHost, RdfOwl.type, Nml.Topology).hasNext()) {
             spaModel.add(resData, Spa.type, Nml.Topology);
             } else if (spaModel.listStatements(resHost, RdfOwl.type, Nml.Node).hasNext()) {
             spaModel.add(resData, Spa.type, Nml.Node);
             }
             */
            spaModel.add(resData, Spa.type, "VMFilterPlacement:HostSite");
            spaModel.add(resData, Spa.value, resHost);
            // remove Placement->exportTo statement so the exportData can be kept in spaModel during receurive removal
            //spaModel.remove(resPolicy, Spa.exportTo, resData);
        }
    }

    //@TODO: matchingNetwork (VPC or TenantNetwork)
    //@TODO: matchingSunbet
    //$$ regExURIFilter
    //$$ hostCapabilityFilter(s)
    //$$ placeMatchingRegExURI
    //$$ placeWithMultiFilter
}
