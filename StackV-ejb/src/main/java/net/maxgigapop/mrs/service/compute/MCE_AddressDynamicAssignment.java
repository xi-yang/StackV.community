/*
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
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.DriverUtil;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.AddressUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_AddressDynamicAssignment extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_AddressDynamicAssignment.class.getName(), "MCE_AddressDynamicAssignment");

    private static final String OSpec_Template
            = "{\n"
            + "	\"$$\": [\n"
            + "		{\n"
            + "			\"interface\": \"?vif?\",\n"
            + "			\"address\": \"?addr?\",\n"
            + "			\"address_type\": \"?type?\",\n"
            + "			\"#sparql\": \"SELECT DISTINCT ?vif ?addr ?type WHERE {?vif a nml:BidirectionalPort. "
            + "?vif mrs:hasNetworkAddress ?na. ?na mrs:value ?addr. ?na mrs:type ?type}\",\n"
            + "			\"#required\": \"false\"\n"
            + "		}\n"
            + "	]\n"
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
        ServiceDelta outputDelta = annotatedDelta.clone();
        for (Resource res : policyResDataMap.keySet()) {
            OntModel stitchModel = this.assignAddresses(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), res, policyResDataMap.get(res));
            if (stitchModel != null) {
                outputDelta.getModelAddition().getOntModel().add(stitchModel.getBaseModel());
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

    // General logic: 1. find the "terminal / end" containing resource (eg. Host Node or Topology)
    // 2. identify the "attach-point" resource (eg. VLAN port) along with a stitching path
    // 3. add statements to the stitching path to connect the terminal to the attach-point (if applicable)
    private OntModel assignAddresses(OntModel systemModel, OntModel spaModel, Resource res, JSONObject jsonAssignReq) {
        String method = "assignAddresses";
        logger.message(method, "@assignAddresses -> " + res);

        OntModel assignModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Model unionSysModel = spaModel.union(systemModel);

        //@TODO: with additional support from DNCAssignIpAddress.java, jsonAssignments array should only include terminals with "Assign IP" checked
        if (!jsonAssignReq.containsKey("assign_to")) {
            throw logger.error_throwing(method, "imported incomplete JSON data");
        }
        JSONArray jsonAssignments = (JSONArray) jsonAssignReq.get("assign_to");
        String addressType = jsonAssignReq.containsKey("address_type") ? (String) jsonAssignReq.get("address_type") : "ipv4-address";
        List<String> ipStaticAddressPool = jsonAssignReq.containsKey("address_pool") ? normalizeIpAddressList((JSONArray)jsonAssignReq.get("address_pool")) : null;
        for (Object obj: jsonAssignments) {
            JSONObject jsonAssignFilter = (JSONObject)obj;
            if (jsonAssignFilter.containsKey("l2path") && jsonAssignFilter.containsKey("terminals")) {
                JSONArray assignToPath = (JSONArray) jsonAssignFilter.get("l2path");
                JSONArray assignToTerminals = (JSONArray) jsonAssignFilter.get("terminals");
                List<Resource> assignToInterfaces = new ArrayList();
                Map<Resource, String> vifIpv4PoolMap = new HashMap();
                for (int i = 0; i < assignToTerminals.size(); i++) {
                    Resource resAssignToInterface = this.lookupL2PathTerminalInterfaceAndIpPool(unionSysModel, assignToPath, (String)assignToTerminals.get(i), vifIpv4PoolMap);
                    if (resAssignToInterface != null) {
                        assignToInterfaces.add(resAssignToInterface);
                    }
                }
                // intersect dynamic address pools
                List<String> ipDynamicAddressPool = null; 
                for (Resource vif: vifIpv4PoolMap.keySet()) {
                    String ipv4Pool = vifIpv4PoolMap.get(vif);
                    if (ipDynamicAddressPool == null) {
                        ipDynamicAddressPool = this.normalizeIpAddressList(ipv4Pool.split("[,;]"));
                    } else {
                        ipDynamicAddressPool = this.intersectIpAddressList(ipDynamicAddressPool, normalizeIpAddressList(ipv4Pool.split("[,;]")));
                    }
                }
                List<String> addresses;
                //TODO: make using and intersecting static pool explicit options 
                if (ipStaticAddressPool == null || ipStaticAddressPool.isEmpty()) {
                    if (ipDynamicAddressPool == null || ipDynamicAddressPool.isEmpty()) {
                        throw logger.error_throwing(method, "Address pools have none address available. Consider adding / changing static address assignment." );
                    } else {
                        addresses = this.checkoutUnassignedAddresses(unionSysModel, ipDynamicAddressPool, assignToInterfaces.size()); 
                    }
                } else {
                    if (ipDynamicAddressPool == null || ipDynamicAddressPool.isEmpty()) {
                        addresses = this.checkoutUnassignedAddresses(unionSysModel, ipStaticAddressPool, assignToInterfaces.size()); 
                    } else {
                        ipStaticAddressPool = this.intersectIpAddressList(ipDynamicAddressPool, ipStaticAddressPool);
                        if (ipStaticAddressPool.isEmpty()) {
                            // use dynamic pool if intersection with static pool has not result
                            //TODO: make this another explict option 
                            addresses = this.checkoutUnassignedAddresses(unionSysModel, ipDynamicAddressPool, assignToInterfaces.size()); 
                        } else {
                            addresses = this.checkoutUnassignedAddresses(unionSysModel, ipStaticAddressPool, assignToInterfaces.size()); 
                        }
                    }
                }
                
                // exception if running out of addresses
                if (addresses.size() < assignToTerminals.size()) {
                   throw logger.error_throwing(method, String.format("%d addresses are requested but address pool has %d remaining", assignToTerminals.size(), addresses.size()) );
                }
                for (int i = 0; i < assignToInterfaces.size(); i++) {
                    Resource resAssignToInterface = assignToInterfaces.get(i);
                    String addressUri = resAssignToInterface.getURI()+":"+addressType+"+"+DriverUtil.addressUriEscape(addresses.get(i));
                    Resource resAddress = RdfOwl.createResourceUnverifiable(assignModel, addressUri, Mrs.NetworkAddress); 
                    assignModel.add(assignModel.createStatement(resAddress, Mrs.type, addressType));
                    assignModel.add(assignModel.createStatement(resAddress, Mrs.value, addresses.get(i)));
                    assignModel.add(assignModel.createStatement(resAssignToInterface, Mrs.hasNetworkAddress, resAddress));
                }
            }
        }
        return assignModel;
    }
    
    //TODO: use range segements instead of individual addresses to save space to lift the mask < 16 restriction
    private List<String> normalizeIpAddressList(List poolAddressRanges) {
        List<String> listAddresses = new ArrayList();
        for (Object obj: poolAddressRanges) {
            String ipRange = (String) obj;
            if (AddressUtil.isIpAddressMaskRange(ipRange)) {
                List<Long> ipList = AddressUtil.ipMaskRangeToLongList(ipRange);
                Long mask = ipList.get(0);
                if (mask < 16) {
                    throw logger.error_throwing("normalizeIpAddressList", "cannot handle network range with mask < 16");
                }
                ipList.remove(0);
                for (Long ip: ipList) {
                    listAddresses.add(AddressUtil.ipToString(ip, mask));
                }
            } else if (AddressUtil.isIpAddressPrefixkRange(ipRange)) {
                List<Long> ipList = AddressUtil.ipPrefixRangeToLongList(ipRange);
                Long mask = ipList.get(0);
                if (mask < 16) {
                    throw logger.error_throwing("normalizeIpAddressList", "cannot handle network prefix with mask < 16");
                }
                ipList.remove(0);
                for (Long ip: ipList) {
                    listAddresses.add(AddressUtil.ipToString(ip, mask));
                }
            }
        }
        return listAddresses;
    }
    
    private List<String> normalizeIpAddressList(String[] poolAddressStringArray) {
        List poolAddressRanges = Arrays.asList(poolAddressStringArray);
        return normalizeIpAddressList(poolAddressRanges);
    }
    
    private List<String> intersectIpAddressList(List<String> poolAddressRanges1, List<String> poolAddressRanges2) {
        List<String> resultList = new ArrayList();
        if (poolAddressRanges2.isEmpty()) {
            return resultList;
        }
        for (String addr: poolAddressRanges1) {
            if (poolAddressRanges2.contains(addr)) {
                resultList.add(addr);
            }
        }
        return resultList;
    }
    
    private List<String> checkoutUnassignedAddresses(Model unionSysModel, List poolAddresses, int num) {
        List<String> listAddresses = new ArrayList();
        if (num > poolAddresses.size()) {
            return null;
        }
        int numAssigned = 0;
        int numPoolSize = poolAddresses.size();
        for (int i = 0; i < numPoolSize; i++) {
            // random? change get(0)/remove(0) into between 0 and listAddresses.size()-1 ?
            String sparql = String.format("SELECT ?na WHERE { ?na mrs:value \"%s\". } ", poolAddresses.get(0));
            if (ModelUtil.sparqlQuery(unionSysModel, sparql).hasNext()) {
                poolAddresses.remove(0);
            } else {
                listAddresses.add((String)poolAddresses.get(0));
                poolAddresses.remove(0);
                numAssigned++;
                if (numAssigned == num) {
                    break;
                }
            }
        }
        return listAddresses;
    }

    private Resource lookupL2PathTerminalInterfaceAndIpPool(Model unionSysModel, JSONArray l2pathHops, String terminalUri, Map vifIpv4PoolMap) {
        String method = "lookupL2PathTerminalInterface";
        for (Object obj : l2pathHops) {
            JSONObject jsonObj = (JSONObject) obj;
            if (!jsonObj.containsKey("hop")) {
                throw logger.error_throwing(method, String.format("l2path has invalid hop: %s", jsonObj));
            }
            String hopUri = (String) jsonObj.get("hop");
            if (hopUri.equals(terminalUri)) {
                return unionSysModel.getResource(hopUri);
            }
            String sparql = "SELECT ?vif ?ipv4pool WHERE { {"
                    + "?terminal nml:hasBidirectionalPort ?port. "
                    + "?port nml:hasBidirectionalPort ?vif. "
                    + "OPTIONAL{ ?port mrs:hasNetworkAddress ?naPool. ?naPool mrs:type \"ipv4-floatingip-pool\". ?naPool mrs:value ?ipv4pool. } "
                    + String.format("FILTER (?terminal = <%s> && ?vif = <%s>)", terminalUri, hopUri)
                    + "} UNION {"
                    + "?terminal nml:hasBidirectionalPort ?vif. "
                    + "OPTIONAL{ ?terminal mrs:hasNetworkAddress ?naPool. ?naPool mrs:type \"ipv4-floatingip-pool\". ?naPool mrs:value ?ipv4pool. } "
                    + String.format("FILTER (?terminal = <%s> && ?vif = <%s>)", terminalUri, hopUri)
                    + "} "
                    + "FILTER (NOT EXISTS { ?port mrs:type \"shared\". ?vif mrs:hasNetworkAddress ?na. ?na mrs:type \"ipv4-address\"}"
                    + " && NOT EXISTS { ?vif mrs:type \"shared\". ?vif mrs:hasNetworkAddress ?na. ?na mrs:type \"ipv4-address\"}) "
                    + "}";
            ResultSet r = ModelUtil.sparqlQuery(unionSysModel, sparql);
            if (r.hasNext()) {
                QuerySolution solution = r.nextSolution();
                Resource vif = solution.getResource("vif");
                if (solution.contains("ipv4pool")) {
                    String ipv4Pool = solution.getLiteral("ipv4pool").toString();
                    vifIpv4PoolMap.put(vif, ipv4Pool);
                }
                return vif;
            }
        }
        return null;
    }
}
