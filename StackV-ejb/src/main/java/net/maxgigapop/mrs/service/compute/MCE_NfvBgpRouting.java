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
public class MCE_NfvBgpRouting extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_NfvBgpRouting.class.getName(), "MCE_NfvBgpRouting");
    
    private static final String OSpec_Template
            = "{ }";
    
    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        logger.cleanup();
        String method = "process";
        logger.refuuid(annotatedDelta.getReferenceUUID());
        logger.start(method);
        
        Map<Resource, JSONObject> policyResDataMap = this.preProcess(policy, systemModel, annotatedDelta);        

        // Specific MCE logic
        ServiceDelta outputDelta = annotatedDelta.clone();
        for (Resource res : policyResDataMap.keySet()) {
            OntModel routingModel = this.doRouting(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), res, policyResDataMap.get(res));
            if (routingModel != null) {
                outputDelta.getModelAddition().getOntModel().add(routingModel.getBaseModel());
            }
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

    private OntModel doRouting(OntModel systemModel, OntModel spaModel, Resource res, JSONObject jsonReqData) {
        String method = "doMultiPathFinding";

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
}
