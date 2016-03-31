/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    private static final Logger log = Logger.getLogger(MCE_NfvBgpRouting.class.getName());

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        log.log(Level.FINE, "MCE_NfvBgpRouting::process {0}", annotatedDelta);
        try {
            log.log(Level.FINE, "\n>>>MCE_NfvBgpRouting--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_NfvBgpRouting.class.getName()).log(Level.SEVERE, null, ex);
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
        return new AsyncResult(outputDelta);
    }

    private OntModel doRouting(OntModel systemModel, OntModel spaModel, Resource policyAction, Map stitchPolicyData) {
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
                    throw new EJBException(String.format("%s::process doRouting cannot parse json string %s", this.getClass().getName(), (String) entry.get("value")));
                }
            } else {
                throw new EJBException(String.format("%s::process doRouting does not import policyData of %s type", entry.get("type").toString()));
            }
        }
        
        if (jsonReqData == null || jsonReqData.isEmpty()) {
            throw new EJBException(String.format("%s::process doRouting receive none request for <%s>", this.getClass().getName(), policyAction));
        }

        OntModel routingModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Model unionSysModel = spaModel.union(systemModel);
        try {
            log.log(Level.FINE, "\n>>>MCE_AWSDirectConnectStitch--unionSysModel=\n" + ModelUtil.marshalModel(unionSysModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_NfvBgpRouting.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!jsonReqData.containsKey("parent") || !jsonReqData.containsKey("router_id") 
                || !jsonReqData.containsKey("as_number") || !jsonReqData.containsKey("neighbors")) {
            throw new EJBException(String.format("%s::process doRouting imports incomplete JSON data", this.getClass().getName()));
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
                resRtSvc = RdfOwl.createResource(spaModel, parentVM + ":routingservice+quagga", Mrs.RoutingService);
                routingModel.add(routingModel.createStatement(resVM, Nml.hasService, resRtSvc));
            }
            if (solution.contains("bgpRtTable")) {
                resBgpRtTable = solution.getResource("bgpRtTable");
            } 
        } else {
            throw new EJBException(String.format("%s::process doRouting cannot identify NFV parent VM: %s", this.getClass().getName(), parentVM));
        }
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
                    throw new EJBException(String.format("%s::exportPolicyData  cannot parse json string %s due to: %s", this.getClass().getName(), dataValue.toString(), e));
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
                } catch (EJBException ex) {
                    log.log(Level.WARNING, ex.getMessage());
                    continue;
                }
            }
            spaModel.add(resData, Spa.value, exportValue);
        }
    }
}
