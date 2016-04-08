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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_VMFilterPlacement implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_VMFilterPlacement.class.getName());

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        // $$ MCE_VMFilterPlacement deals with add model only for now.
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw new EJBException(String.format("%s::process ", this.getClass().getName()));
        }
        try {
            log.log(Level.FINE, "\n>>>MCE_VMFilterPlacement--DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        // importPolicyData
        String sparql = "SELECT ?res ?policy ?data ?dataType ?dataValue WHERE {"
                + "?res spa:dependOn ?policy . "
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_VMFilterPlacement'. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?dataType. ?data spa:value ?dataValue. "
                + String.format("FILTER (not exists {?policy spa:dependOn ?other} && not exists {?res a spa:PolicyAction} && ?policy = <%s>)", policy.getURI())
                + "}";
        Map<Resource, List> policyMap = new HashMap<>();
        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource res = querySolution.get("res").asResource();
            if (!policyMap.containsKey(res)) {
                List policyList = new ArrayList<>();
                policyMap.put(res, policyList);
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
            policyMap.get(res).add(policyData);
        }
        OntModel combinedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        combinedModel.add(systemModel.getOntModel());
        combinedModel.add(annotatedDelta.getModelAddition().getOntModel());
        
        ServiceDelta outputDelta = annotatedDelta.clone();
        
        for (Resource res : policyMap.keySet()) {
            //1. compute placement based on filter/match criteria *policyData*
            // returned placementModel contains the VM as well as hosting Node/Topology and HypervisorService from systemModel
            //$$ TODO: virtual node should be named and tagged using URI and/or polocy/criteria data in spaModel  
            OntModel placementModel = this.doPlacement(combinedModel, res, policyMap.get(res));
            if (placementModel == null) {
                throw new EJBException(String.format("%s::process cannot resolve any policy to place %s", this.getClass().getName(), res));
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
            this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), res);

            //4. remove policy and all related SPA statements receursively under the res from spaModel
            //   and also remove all statements that say dependOn this 'policy'
            MCETools.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), res);

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
    private OntModel doPlacement(OntModel model, Resource vm, List<Map> placementCriteria) {
        OntModel placementModel = null;
        for (Map filterCriterion : placementCriteria) {
            if (!filterCriterion.containsKey("data") || !filterCriterion.containsKey("type") || !filterCriterion.containsKey("value")) {
                continue;
            }
            String placeToUri = null;
            if (((String) filterCriterion.get("type")).equalsIgnoreCase(Nml.Topology.getURI())
                    || ((String) filterCriterion.get("type")).equalsIgnoreCase(Nml.Node.getURI())) {
                placeToUri = (String) filterCriterion.get("value");
            } else if (((String) filterCriterion.get("type")).equalsIgnoreCase("JSON")) {
                //$$ merge JSON for multi-filter ?
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonObj = (JSONObject) parser.parse((String) filterCriterion.get("value"));
                    if (jsonObj.containsKey("place_into")) {
                        placeToUri = (String) jsonObj.get("place_into");
                    }
                } catch (ParseException e) {
                    throw new EJBException(String.format("%s::process  cannot parse json string %s", this.getClass().getName(), (String) filterCriterion.get("value")));
                }  
            } 
            if (placeToUri == null || !model.contains(model.getResource(placeToUri), null)) {
                throw new EJBException(String.format("%s::process  cannot import data from %s", this.getClass().getName(), (String) filterCriterion.get("value")));
            }
            OntModel hostModel = filterTopologyNode(model, vm, placeToUri);
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
            // ? place to a specific Node ?
            //$$ Other types of filter methods have yet to be implemented.
        }
        return placementModel;
    }

    private OntModel filterTopologyNode(OntModel model, Resource resPlace, String placeToUri) {
        OntModel placeModel = null;
        // place VM and subnet to AWS VPC - Subnet
        String sparqlString = "SELECT ?vpc ?hvservice ?subnet WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasTopology ?vpc ."
                + "?vpc a nml:Topology . "
                + "?topology nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + "?vpc nml:hasService ?vpcsw . "
                + "?vpcsw mrs:providesSubnet ?subnet . "
                + "?res a nml:Node ."
                + String.format("FILTER (?subnet = <%s> && ?res = <%s>) ", placeToUri, resPlace.getURI())
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(model, sparqlString);
        if (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resHostOrVpc = querySolution.get("vpc").asResource();
            Resource resHvService = querySolution.get("hvservice").asResource();
            Resource resSubnet = querySolution.get("subnet").asResource();
            if (placeModel == null) {
                placeModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            }
            placeModel.add(resHostOrVpc, Nml.hasNode, resPlace);
            placeModel.add(resHvService, Mrs.providesVM, resPlace);
            return placeModel;
        }
        // place vm to topology or host node that has hypervisor
        //@TODO: randomization for 'any'
        sparqlString = "SELECT ?hosttopo ?hvservice WHERE {"
                + "?hosttopo nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + "?res a nml:Node ."
                + String.format("FILTER (?hosttopo = <%s> && ?res = <%s>) ", placeToUri, resPlace.getURI())
                + "}";
        r = ModelUtil.sparqlQuery(model, sparqlString);
        if (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resHostTopology = querySolution.get("hosttopo").asResource();
            Resource resHvService = querySolution.get("hvservice").asResource();
            if (placeModel == null) {
                placeModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            }
            placeModel.add(resHostTopology, Nml.hasNode, resPlace);
            placeModel.add(resHvService, Mrs.providesVM, resPlace);
            return placeModel;
        }
        // Place interface to subnet 
        sparqlString = "SELECT ?subnet WHERE {"
                + "?subnet a mrs:SwitchingSubnet ."
                + "?res a nml:BidirectionalPort ."
                + String.format("FILTER (?subnet = <%s> && ?res = <%s>) ", placeToUri, resPlace.getURI())
                + "}";
        r = ModelUtil.sparqlQuery(model, sparqlString);
        if (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resSubnet = querySolution.get("subnet").asResource();
            if (placeModel == null) {
                placeModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            }
            placeModel.add(resSubnet, Nml.hasBidirectionalPort, resPlace);
            return placeModel;
        }
        return null;
    }

    //@TODO: JSON export
    private void exportPolicyData(OntModel spaModel, Resource res) {
        // find Placement policy -> exportTo -> policyData
        String sparql = "SELECT ?hostPlace ?policyData WHERE {"
                + String.format("?hostPlace nml:hasNode <%s> .", res.getURI()) 
                + "?hvservice a mrs:HypervisorService . "
                + String.format("?hvservice mrs:providesVM <%s> .", res.getURI())
                + String.format("<%s> spa:dependOn ?policyAction .", res.getURI())
                + "?policyAction a spa:PolicyAction. "
                + "?policyAction spa:type 'MCE_VMFilterPlacement'. "
                + "?policyAction spa:exportTo ?policyData . "
                + "?policyData a spa:PolicyData . "
                + "OPTIONAL {?policyData spa:format ?format.}"
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resHost = querySolution.get("hostPlace").asResource();
            Resource resData = querySolution.get("policyData").asResource();
            spaModel.add(resData, Spa.type, "JSON");
            // add export data
            JSONObject output = new JSONObject();
            output.put("uri", res.getURI());
            output.put("place_into", resHost.getURI());
            //add output as spa:value of the export resrouce
            String exportValue = output.toJSONString();
            if (querySolution.contains("format")) {
                String exportFormat = querySolution.get("format").toString();
                exportValue = MCETools.formatJsonExport(exportValue, exportFormat);
            }
            spaModel.add(resData, Spa.value, exportValue);
        }
    }

    //@TODO: matchingNetwork (VPC or TenantNetwork)
    //@TODO: matchingSunbet
    //$$ regExURIFilter
    //$$ hostCapabilityFilter(s)
    //$$ placeMatchingRegExURI
    //$$ placeWithMultiFilter
}
