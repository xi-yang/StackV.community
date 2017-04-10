/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2016

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
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.net.util.SubnetUtils;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_NfvBgpRouting implements IModelComputationElement {

    private static final StackLogger logger = new StackLogger(MCE_MPVlanConnection.class.getName(), "MCE_MPVlanConnection");

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        String method = "process";
        if (annotatedDelta.getServiceInstance() != null) {
            logger.refuuid(annotatedDelta.getServiceInstance().getReferenceUUID());
            logger.targetid(annotatedDelta.getId());
        }
        logger.start(method);
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw logger.error_throwing(method, "target:ServiceDelta has null addition model");
        }
        try {
            logger.trace(method, "DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(annotatedDelta.additionModel) -exception-"+ex);
        }
        // importPolicyData : Interface->Stitching->List<PolicyData>
        String sparql = "SELECT ?policy ?data ?type ?value WHERE {"
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_NfvBgpRouting'. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?type. ?data spa:value ?value. "
                + String.format("FILTER (not exists {?policy spa:dependOn ?other} && ?policy = <%s>)", policy.getURI())
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        Map<Resource, Map> policyDataMap = new HashMap<>();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resPolicy = querySolution.get("policy").asResource();
            if (!policyDataMap.containsKey(resPolicy)) {
                Map<String, List> stitchMap = new HashMap<>();
                List dataList = new ArrayList<>();
                stitchMap.put("imports", dataList);
                policyDataMap.put(resPolicy, stitchMap);
            }
            Resource resData = querySolution.get("data").asResource();
            RDFNode nodeDataType = querySolution.get("type");
            RDFNode nodeDataValue = querySolution.get("value");
            boolean existed =false;
            for (Map dataMap: (List<Map>)policyDataMap.get(resPolicy).get("imports")) {
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
                ((List)policyDataMap.get(resPolicy).get("imports")).add(policyData);
            }
        }
        ServiceDelta outputDelta = annotatedDelta.clone();

        for (Resource policyAction : policyDataMap.keySet()) {
            OntModel routingModel = this.doRouting(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), policyAction, policyDataMap.get(policyAction));
            // merge the placement satements into spaModel
            if (routingModel != null) {
                outputDelta.getModelAddition().getOntModel().add(routingModel.getBaseModel());
            }

            exportPolicyData(outputDelta.getModelAddition().getOntModel(), policyAction, routingModel, systemModel.getOntModel());

            // remove policy dependency
            MCETools.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), policyAction);
        }
        
        try {
            logger.trace(method, "DeltaAddModel Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(outputDelta.additionModel) -exception-"+ex);
        }
        logger.end(method);        
        return new AsyncResult(outputDelta);
    }

    private OntModel doRouting(OntModel systemModel, OntModel spaModel, Resource policyAction, Map stitchPolicyData) {
        String method = "doMultiPathFinding";
        //@TODO: common logic
        List<Map> dataMapList = (List<Map>)stitchPolicyData.get("imports");
        JSONObject jsonReqData = null;
        for (Map entry : dataMapList) {
            if (!entry.containsKey("data") || !entry.containsKey("type") || !entry.containsKey("value")) {
                continue;
            }
            if (entry.get("type").toString().equalsIgnoreCase("JSON")) {
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonObj = (JSONObject) parser.parse((String) entry.get("value"));
                    if (jsonReqData == null) {
                        jsonReqData = jsonObj;
                    } else { // merge
                        for (Object key: jsonObj.keySet()) {
                            jsonReqData.put(key, jsonObj.get(key));
                        }
                    }
                } catch (ParseException e) {
                    throw logger.throwing(method, String.format("cannot parse json string %s", entry.get("value")), e);
                }
            } else {
                throw logger.error_throwing(method, String.format("cannot import policyData of %s type", entry.get("type")));
            }
        }
        
        if (jsonReqData == null || jsonReqData.isEmpty()) {
            throw logger.error_throwing(method, String.format("received none request for policy <%s>", policyAction));
        }

        OntModel routingModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Model unionSysModel = spaModel.union(systemModel);
        if (!jsonReqData.containsKey("parent") || !jsonReqData.containsKey("router_id") 
                || !jsonReqData.containsKey("as_number") || !jsonReqData.containsKey("neighbors")) {
            throw logger.error_throwing(method, "imported incomplete JSON data");
        }
        String parentVM = (String) jsonReqData.get("parent");
        String routerId = (String) jsonReqData.get("router_id");
        SubnetUtils utils1 = new SubnetUtils(routerId);
        routerId = utils1.getInfo().getAddress();
        String routerAsn = (String) jsonReqData.get("as_number");
        Resource resVM = null;
        Resource resRtSvc = null;
        Resource resBgpRtTable = null;
        // 1. get VGW resources
        String sparql = "SELECT ?vm ?rtSvc ?bgpRtTable WHERE {\n"
                + "?vm a nml:Node.\n"
                + String.format("FILTER (?vm = <%s>) \n", parentVM)
                + "OPTIONAL {"
                + "?vm nml:hasService ?rtSvc."
                + "?rtSvc a mrs:RoutingService."
                + "}"
                + "OPTIONAL {"
                + "?rtSvc mrs:providesRoutingTable ?bgpRtTable."
                + "?bgpRtTable mrs:type \"quagga-bgp\". "
                + "}"
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(unionSysModel, sparql);
        if (r.hasNext()) {
            QuerySolution solution = r.nextSolution();
            resVM = solution.getResource("vm");
            if (solution.contains("rtSvc")) {
                resRtSvc = solution.getResource("rtSvc");
            } else {
                resRtSvc = RdfOwl.createResource(spaModel, parentVM + ":routingservice", Mrs.RoutingService);
                routingModel.add(routingModel.createStatement(resVM, Nml.hasService, resRtSvc));
            }
            if (solution.contains("bgpRtTable")) {
                resBgpRtTable = solution.getResource("bgpRtTable");
            } 
        } else {
            throw logger.error_throwing(method, "cannot identify NFV parent VM: " + parentVM);        }
        if (resBgpRtTable == null) {
            resBgpRtTable = RdfOwl.createResource(spaModel, resRtSvc.getURI() + ":routingtable+quagga_bgp", Mrs.RoutingTable);
            routingModel.add(routingModel.createStatement(resRtSvc, Mrs.providesRoutingTable, resBgpRtTable));
            routingModel.add(routingModel.createStatement(resBgpRtTable, Mrs.type, "quagga-bgp"));
        }
        Resource resNetAddrRouterId = RdfOwl.createResource(spaModel, resBgpRtTable.getURI() + ":router_id", Mrs.NetworkAddress);
        routingModel.add(routingModel.createStatement(resBgpRtTable, Mrs.hasNetworkAddress, resNetAddrRouterId));
        routingModel.add(routingModel.createStatement(resNetAddrRouterId, Mrs.type, "ipv4")); // ? ipv4-address
        routingModel.add(routingModel.createStatement(resNetAddrRouterId, Mrs.value, routerId));        
        Resource resNetAddrRouterAsn = RdfOwl.createResource(spaModel, resBgpRtTable.getURI() + ":asn", Mrs.NetworkAddress);
        routingModel.add(routingModel.createStatement(resBgpRtTable, Mrs.hasNetworkAddress, resNetAddrRouterAsn));
        routingModel.add(routingModel.createStatement(resNetAddrRouterAsn, Mrs.type, "bgp-asn")); 
        routingModel.add(routingModel.createStatement(resNetAddrRouterAsn, Mrs.value, routerAsn));
        // create local_cluster NetworkAddress statements based on 'networks'
        String prefixList = routerId+"/32";
        if (jsonReqData.containsKey("networks")) {
            JSONArray networks = (JSONArray)jsonReqData.get("networks");
            for (Object obj: networks) {
                String netAddr = (String) obj;
                SubnetUtils utils2 = new SubnetUtils(netAddr);
                String prefix = utils2.getInfo().getNetworkAddress()+"/"+netAddr.split("/")[1];
                prefixList += (","+prefix);
            }
        } 
        Resource resNetAddrLocalPrefixes = RdfOwl.createResource(spaModel, resBgpRtTable.getURI() + ":local_prefix_list", Mrs.NetworkAddress);
        routingModel.add(routingModel.createStatement(resNetAddrLocalPrefixes, Mrs.type, "ipv4-prefix-list")); 
        routingModel.add(routingModel.createStatement(resNetAddrLocalPrefixes, Mrs.value, prefixList));
        JSONArray neighbors = (JSONArray)jsonReqData.get("neighbors");
        for (Object obj: neighbors) {
            JSONObject bgpNeighbor = (JSONObject) obj;
            if (!bgpNeighbor.containsKey("remote_ip") || !bgpNeighbor.containsKey("remote_asn")) {
                continue;
            }
            String remoteAsn = (String)bgpNeighbor.get("remote_asn");
            String remoteIp = (String)bgpNeighbor.get("remote_ip");
            if (remoteIp.contains("/")) {
                remoteIp = remoteIp.split("/")[0];
            }
            Resource resRouteToNeighbor = RdfOwl.createResource(spaModel, resBgpRtTable.getURI() + ":neighbor+"+remoteIp.replaceAll("[.\\/]", "_"), Mrs.Route);
            routingModel.add(routingModel.createStatement(resBgpRtTable, Mrs.hasRoute, resRouteToNeighbor));
            // route NetAddresses
            Resource resNetAddrRemoteAsn = RdfOwl.createResource(spaModel, resRouteToNeighbor.getURI() + ":remote_asn", Mrs.NetworkAddress);
            routingModel.add(routingModel.createStatement(resRouteToNeighbor, Mrs.hasNetworkAddress, resNetAddrRemoteAsn));
            routingModel.add(routingModel.createStatement(resNetAddrRemoteAsn, Mrs.type, "bgp-asn"));
            routingModel.add(routingModel.createStatement(resNetAddrRemoteAsn, Mrs.value, remoteAsn));
            if (bgpNeighbor.containsKey("local_ip")) {
                String localIp = (String)bgpNeighbor.get("local_ip");
                if (localIp.contains("/")) {
                    localIp = localIp.split("/")[0];
                }
                Resource resNetAddrLocalIp = RdfOwl.createResource(spaModel, resRouteToNeighbor.getURI() + ":local_ip", Mrs.NetworkAddress);
                routingModel.add(routingModel.createStatement(resRouteToNeighbor, Mrs.hasNetworkAddress, resNetAddrLocalIp));
                routingModel.add(routingModel.createStatement(resNetAddrLocalIp, Mrs.type, "ipv4-local")); // ? ipv4-address
                routingModel.add(routingModel.createStatement(resNetAddrLocalIp, Mrs.value, localIp));
            }
            if (bgpNeighbor.containsKey("bgp_authkey")) {
                String authkey = (String)bgpNeighbor.get("bgp_authkey");
                Resource resNetAddrAuthkey = RdfOwl.createResource(spaModel, resRouteToNeighbor.getURI() + ":bgp_authkey", Mrs.NetworkAddress);
                routingModel.add(routingModel.createStatement(resRouteToNeighbor, Mrs.hasNetworkAddress, resNetAddrAuthkey));
                routingModel.add(routingModel.createStatement(resNetAddrAuthkey, Mrs.type, "bgp-authkey"));
                routingModel.add(routingModel.createStatement(resNetAddrAuthkey, Mrs.value, authkey));
            }
            // nextHop
            Resource resNetAddrRemoteIp = RdfOwl.createResource(spaModel, resRouteToNeighbor.getURI() + ":remote_ip", Mrs.NetworkAddress);
            routingModel.add(routingModel.createStatement(resRouteToNeighbor, Mrs.nextHop, resNetAddrRemoteIp));
            routingModel.add(routingModel.createStatement(resNetAddrRemoteIp, Mrs.type, "ipv4-remote")); // ? ipv4-address
            routingModel.add(routingModel.createStatement(resNetAddrRemoteIp, Mrs.value, remoteIp));
            // routeFrom
            if (resNetAddrLocalPrefixes != null) {
                routingModel.add(routingModel.createStatement(resRouteToNeighbor, Mrs.routeFrom, resNetAddrLocalPrefixes));
            }
        }
        return routingModel;
    }
    
    private void exportPolicyData(OntModel spaModel, Resource resPolicy, OntModel routingModel, OntModel systemModel) {
        String method = "exportPolicyData";
        String sparql = "SELECT ?data ?type ?value ?format WHERE {"
                + String.format("<%s> a spa:PolicyAction. ", resPolicy.getURI())
                + String.format("<%s> spa:type 'MCE_NfvBgpRouting'. ", resPolicy.getURI())
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
            
            //@ get export data
            
            //@ put new data into jsonValue
            //jsonValue.put(name, data);
            
            //@TODO: common logic
            if (dataValue != null) {
                spaModel.remove(resData, Spa.value, dataValue);
            }
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
            spaModel.add(resData, Spa.value, exportValue);
        }
    }
}
