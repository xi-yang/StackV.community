/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2015

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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_VMFilterPlacement extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_VMFilterPlacement.class.getName(), "MCE_VMFilterPlacement");

    private static final String OSpec_Template
            = "{\n"
            + " \"$$\": {\n"
            + "	\"uri\": \"$$\",\n"
            + "	\"host\": \"?host?\",\n"
            + "	\"#sparql\": \"SELECT ?host WHERE {?host nml:hasNode <$$>. <$$> a nml:Node.} "
            + " UNION {?host nml:hasBidirectionalPort <$$>. <$$> a nml:BidirectionalPort.} \"\n"
            + " }"
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

        // Specific MCE logic 
        OntModel combinedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        combinedModel.add(systemModel.getOntModel());
        combinedModel.add(annotatedDelta.getModelAddition().getOntModel());
        ServiceDelta outputDelta = annotatedDelta.clone();
        for (Resource res : policyResDataMap.keySet()) {
            //1. compute placement based on filter/match criteria *policyData*
            // returned placementModel contains the VM as well as hosting Node/Topology and HypervisorService from systemModel
            OntModel placementModel = this.doPlacement(combinedModel, res, policyResDataMap.get(res));
            if (placementModel == null) {
                throw logger.error_throwing(method, "cannot apply policy to place VM=" + res);
            }

            //2. merge the placement satements into spaModel
            outputDelta.getModelAddition().getOntModel().add(placementModel.getBaseModel());
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

    //?? Use current containing abstract Topology ?
    // ignore if dependOn 'Abstraction'
    private OntModel doPlacement(OntModel model, Resource res, JSONObject jsonData) {
        String method = "doPlacement";
        logger.message(method, "@doPlacement -> " + res);
        OntModel placementModel = null;
        String placeToUri = null;
        if (jsonData.containsKey("place_into")) {
            placeToUri = (String) jsonData.get("place_into");
            if (placeToUri.endsWith("+any")) {
                placeToUri = "any";
            }
        }
        if (placeToUri == null || (!placeToUri.startsWith("any") && !model.contains(model.getResource(placeToUri), null))) {
            throw logger.error_throwing(method, "json input misses placeToUri");
        }
        OntModel hostModel;
        if (placeToUri.startsWith("any")) {
            // place VM to any host (Topology / Node) that has more resources
            hostModel = selectAnyPlace(model, res);
        } else {
            // place VM to a specific subnet or host (Topology / Node) 
            // or place intreface to a specific subnet
            hostModel = filterAndPlace(model, res, placeToUri);
        }
        if (hostModel == null) {
            throw logger.error_throwing(method, String.format("cannot place '%s'", res));
        }
        // assemble placementModel;
        if (placementModel == null) {
            placementModel = hostModel;
        } else {
            placementModel.add(hostModel.getBaseModel());
        }
        return placementModel;
    }

    private OntModel selectAnyPlace(OntModel model, Resource resPlace) {
        String sparqlString = "SELECT ?hosttopo ?hvservice ?total_num_core ?total_memory_mb ?total_disk_gb "
                + "?used_num_core ?used_memory_mb ?used_disk_gb WHERE {"
                + "?hosttopo nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + "OPTIONAL { ?hosttopo mrs:num_core ?total_num_core . ?hvservice mrs:num_core ?used_num_core . } "
                + "OPTIONAL { ?hosttopo mrs:memory_mb ?total_memory_mb . ?hvservice mrs:memory_mb ?used_memory_mb . } "
                + "OPTIONAL { ?hosttopo mrs:disk_gb ?total_disk_gb . ?hvservice mrs:disk_gb ?used_disk_gb . } "
                + String.format("<%s> a nml:Node .", resPlace.getURI())
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(model, sparqlString);
        if (!r.hasNext()) {
            return null;
        }
        List<Map> rankArray = new ArrayList();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resHostTopology = querySolution.get("hosttopo").asResource();
            Resource resHvService = querySolution.get("hvservice").asResource();
            Map<String, Object> paramMap = new HashMap();
            paramMap.put("host", resHostTopology);
            paramMap.put("hypervisor", resHvService);
            Integer numCore = 0;
            if (querySolution.contains("total_num_core")) {
                numCore = Integer.parseInt(querySolution.get("total_num_core").toString()) - Integer.parseInt(querySolution.get("used_num_core").toString());
            }
            paramMap.put("num_core", numCore);
            Integer memoryMb =0;
            if (querySolution.contains("total_memory_mb")) {
                memoryMb = Integer.parseInt(querySolution.get("total_memory_mb").toString()) - Integer.parseInt(querySolution.get("used_memory_mb").toString());
            }
            paramMap.put("memory_mb", memoryMb);
            Integer diskGb = 0;
            if (querySolution.contains("total_disk_gb")) {
                diskGb = Integer.parseInt(querySolution.get("total_disk_gb").toString()) - Integer.parseInt(querySolution.get("used_disk_gb").toString());
            }
            paramMap.put("disk_gb", diskGb);
            rankArray.add(paramMap);
        }
        OntModel placeModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        // get top 3 after sorting #num_core 
        List<Map> topArray = new ArrayList();
        ListIterator<Map> itX = rankArray.listIterator(); 
        while (itX.hasNext()) {
            Map mapX = itX.next();
            if (topArray.isEmpty()) {
                topArray.add(mapX);
            } else {
                ListIterator<Map> itT = topArray.listIterator();
                while (itT.hasNext()) {
                    Map mapT = itT.next();
                    if ((Integer) mapX.get("num_core") > (Integer) mapT.get("num_core")) {
                        itT.previous();
                        itT.add(mapX);
                        break;
                    }
                }
                if (!itT.hasNext()) {
                    itT.add(mapX);
                }
            }
        }
        if (topArray.size() > 3) {
            topArray.subList(3, topArray.size()).clear();
        }
        // get best 2 out the 3 by #memory_mb
        if (topArray.size() == 3) {
            Map map1 = topArray.get(0);
            Map map2 = topArray.get(1);
            Map map3 = topArray.get(2);
            if ((Integer) map1.get("memory_mb") > (Integer) map2.get("memory_mb")) {
                if ((Integer) map2.get("memory_mb") > (Integer) map3.get("memory_mb")) {
                    topArray.remove(map3);
                } else {
                    topArray.remove(map2);
                }
            } else {
                if ((Integer) map1.get("memory_mb") > (Integer) map3.get("memory_mb")) {
                    topArray.remove(map3);
                } else {
                    topArray.remove(map1);
                }
            }
        }
        // get the better out of the 2 by #disk_gb
        if (topArray.size() == 2) {
            Map map1 = topArray.get(0);
            Map map2 = topArray.get(1);
            if ((Integer) map1.get("disk_gb") > (Integer) map2.get("disk_gb")) {
                    topArray.remove(map2);
            } else {
                    topArray.remove(map1);
            }
        }
        placeModel.add((Resource)topArray.get(0).get("host"), Nml.hasNode, resPlace);
        placeModel.add((Resource)topArray.get(0).get("hypervisor"), Mrs.providesVM, resPlace);
        return placeModel;
    }
    
    private OntModel filterAndPlace(OntModel model, Resource resPlace, String placeToUri) {
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
}
