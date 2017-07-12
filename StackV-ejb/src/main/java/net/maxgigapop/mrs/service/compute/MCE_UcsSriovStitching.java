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
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.HashMap;
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
import net.maxgigapop.mrs.common.ResourceTool;
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.openstack.OpenstackPrefix;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_UcsSriovStitching extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_UcsSriovStitching.class.getName(), "MCE_UcsSriovStitching");

    private static final String OSpec_Template
            = "{\n"
            + "\"port_profile\": \"\",\n"
            + "\"ip_address\": \"\",\n"
            + "\"mac_address\": \"\",\n"
            + "\"#sparsql\": \"SELECT SELECT ?vm ?vnic WHERE { ?vm nml:hasBidirectionalPort ?vnic. ?vmfex mrs:providesVNic ?vnic. }\",\n"
            + "\"#required\": \"true\",\n"
            + "\"#sparsql-ext\": \"SELECT ?ip_address ?mac_address ?port_profile WHERE {"
            + "?vnic mrs:hasNetworkAddress ?netaddr_ip. \"?netaddr_ip mrs:type \\\"ipv4-address\\\". ?netaddr_ip mrs:value ?ip_address. "
            + "?vnic mrs:hasNetworkAddress ?netaddr_mac. ?netaddr_mac mrs:type \\\"mac-address\\\". ?netaddr_mac mrs:value ?mac_address. "
            + "?profile_subnet nml:hasBidirectionalPort ?vnic. ?profile_subnet a mrs:SwitchingSubnet. "
            + "?profile_subnet mrs:type \\\"Cisco_UCS_Port_Profile\\\". ?profile_subnet mrs:value ?port_profile. "
            + "}\"\n"
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
            OntModel stitchModel = this.doStitching(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), res, policyResDataMap.get(res));
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

    //@TODO: Stitch ( VGW | VPC | Subnet? )to ( DcVx | L2Path )

    // General logic: 1. find the "terminal / end" containing resource (eg. Host Node or Topology)
    // 2. identify the "attach-point" resource (eg. VLAN port) along with a stitching path
    // 3. add statements to the stitching path to connect the terminal to the attach-point (if applicable)
    private OntModel doStitching(OntModel systemModel, OntModel spaModel, Resource res, JSONObject jsonStitchReq) {
        String method = "doStitching";
        logger.message(method, "@doStitching -> " + res);

        OntModel stitchModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Model unionSysModel = spaModel.union(systemModel);

        if (!jsonStitchReq.containsKey("stitch_from") || (!jsonStitchReq.containsKey("to_port_profile") && !jsonStitchReq.containsKey("to_l2path"))) {
            throw logger.error_throwing(method, "imported incomplete JSON data");
        }
        String stitchFromUri = (String) jsonStitchReq.get("stitch_from");

        // 1. get VM and PortProfile resources
        String sparql = "SELECT ?vm ?vmfex WHERE {"
                + "?host nml:hasNode ?vm . "
                + "?vm a nml:Node. "
                + "?host nml:hasService ?vmfex. "
                + "?vmfex a mrs:HypervisorBypassInterfaceService. "
                + String.format("FILTER (?vm = <%s>)", stitchFromUri)
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(unionSysModel, sparql);
        Resource resVm = null;
        Resource resVmFex = null;
        if (r.hasNext()) {
            QuerySolution solution = r.nextSolution();
            resVm = solution.getResource("vm");
            resVmFex = solution.getResource("vmfex");
        } else {
            throw logger.error_throwing(method, String.format("cannot find resource for '%s': must be URI for a VM with SRIOV", stitchFromUri));
        }
        Resource resPortProfile = null;
        if (jsonStitchReq.containsKey("to_port_profile")) {
            String stitchToProfile = (String) jsonStitchReq.get("to_port_profile");
            sparql = "SELECT ?profile WHERE {"
                    + "?profile a mrs:SwitchingSubnet. "
                    + "?profile mrs:type \"Cisco_UCS_Port_Profile\" . "
                    + String.format("?profile mrs:value \"%s\" . ", "Cisco_UCS_Port_Profile+"+stitchToProfile)
                    + "}";

            r = ModelUtil.sparqlQuery(unionSysModel, sparql);
            if (!r.hasNext()) {
                throw logger.error_throwing(method, String.format("cannot find resource for '%s': must be value for a SwitchingSubnet of 'UCS_Port_Profile' type", stitchToProfile));
            }
            resPortProfile = r.next().getResource("profile");
        } else { //if (jsonStitchReq.containsKey("to_l2path"))
            JSONArray stitchToPath = (JSONArray) jsonStitchReq.get("to_l2path");
            if (stitchToPath.isEmpty()) {
                throw logger.error_throwing(method, String.format("cannot parse JSON data 'to_l2path': %s", stitchToPath));
            }
            for (Object obj : stitchToPath) {
                JSONObject jsonObj = (JSONObject) obj;
                if (!jsonObj.containsKey("uri")) {
                    throw logger.error_throwing(method, String.format("cannot parse JSON data 'to_l2path': %s - invalid hop: %s", stitchToPath, jsonObj.toJSONString()));
                }
                String hopUri = (String) jsonObj.get("uri");
                // find a port profile that the hop connects to via a VLAN
                sparql = "SELECT ?profile WHERE {"
                        + "?profile a mrs:SwitchingSubnet. "
                        + "?profile mrs:type \"Cisco_UCS_Port_Profile\". "
                        + String.format("?profile nml:hasBidirectionalPort <%s>. ", hopUri)
                        + "}";
                r = ModelUtil.sparqlQuery(unionSysModel, sparql);
                if (r.hasNext()) {
                    QuerySolution solution = r.nextSolution();
                    resPortProfile = solution.getResource("profile");
                    break;
                }
            }
        }
        if (resPortProfile == null) {
            throw logger.error_throwing(method, String.format("cannot find a SwitchingSubnet of 'UCS_Port_Profile' type to stitch to (in %s)", jsonStitchReq.containsKey("to_port_profile") ? (String) jsonStitchReq.get("to_l2path") : (String) jsonStitchReq.get("to_port_profile")));
        }
        String vnicName = "eth" + UUID.randomUUID().toString();
        Resource resVnic = RdfOwl.createResource(spaModel, ResourceTool.getResourceUri(vnicName, OpenstackPrefix.PORT, vnicName), Nml.BidirectionalPort);

        stitchModel.add(stitchModel.createStatement(resVm, Nml.hasBidirectionalPort, resVnic));
        stitchModel.add(stitchModel.createStatement(resVmFex, Mrs.providesVNic, resVnic));
        stitchModel.add(stitchModel.createStatement(resPortProfile, Nml.hasBidirectionalPort, resVnic));

        // Get addresses and routes parameters from jsonStitchReq
        if (jsonStitchReq.containsKey("ip_address")) {
            String ip = (String) jsonStitchReq.get("ip_address");
            Resource vnicIP = RdfOwl.createResource(stitchModel, resVnic.getURI() + ":ipv4-address+" + ip.replaceAll("/", "_"), Mrs.NetworkAddress);
            stitchModel.add(stitchModel.createStatement(vnicIP, Mrs.type, "ipv4-address"));
            stitchModel.add(stitchModel.createStatement(vnicIP, Mrs.value, ip));
            stitchModel.add(stitchModel.createStatement(resVnic, Mrs.hasNetworkAddress, vnicIP));
        }
        if (jsonStitchReq.containsKey("mac_address")) {
            String mac = (String) jsonStitchReq.get("mac_address");
            Resource vnicMac = RdfOwl.createResource(stitchModel, resVnic.getURI() + ":mac-address+" + mac.replaceAll(":", "_"), Mrs.NetworkAddress);
            stitchModel.add(stitchModel.createStatement(vnicMac, Mrs.type, "mac-address"));
            stitchModel.add(stitchModel.createStatement(vnicMac, Mrs.value, mac));
            stitchModel.add(stitchModel.createStatement(resVnic, Mrs.hasNetworkAddress, vnicMac));
        }
        if (jsonStitchReq.containsKey("routes")) {
            sparql = "SELECT ?routing WHERE {"
                    + String.format("<%s> nml:hasService ?routing . ", resVm)
                    + "?routing a mrs:RoutingService . "
                    + "}";
            r = ModelUtil.sparqlQuery(unionSysModel, sparql);
            Resource resRoutingSvc = null;
            if (r.hasNext()) {
                QuerySolution solution = r.nextSolution();
                resRoutingSvc = solution.getResource("routing");
            }
            if (resRoutingSvc == null) {
                resRoutingSvc = RdfOwl.createResource(stitchModel, resVm.getURI() + ":routingservice", Mrs.RoutingService);
                stitchModel.add(spaModel.createStatement(resVm, Nml.hasService, resRoutingSvc));
            }
            JSONArray routes = (JSONArray) jsonStitchReq.get("routes");
            for (Object obj : routes) {
                JSONObject route = (JSONObject) obj;
                String strRouteTo = null;
                if (route.get("to") instanceof JSONObject) {
                    strRouteTo = (String) ((JSONObject)route.get("to")).get("value");
                } else if (route.get("to") instanceof String) {
                    strRouteTo = ((String) route.get("to"));
                } else {
                    continue;
                }
                String strRouteVia = null;
                if (route.get("next_hop") instanceof JSONObject) {
                    strRouteVia = (String) ((JSONObject)route.get("next_hop")).get("value");
                } else if (route.get("next_hop") instanceof String) {
                    strRouteVia = ((String) route.get("next_hop"));
                }
                Resource vnicRoute = RdfOwl.createResource(stitchModel, resVnic.getURI() + ":route+to-" + strRouteTo.replaceAll("/", "") + "-via-" + strRouteVia.replaceAll("/", ""), Mrs.Route);
                stitchModel.add(stitchModel.createStatement(resRoutingSvc, Mrs.providesRoute, vnicRoute));
                Resource vnicRouteTo = RdfOwl.createResource(stitchModel, resVnic.getURI() + ":routeto+" + strRouteTo.replaceAll("/", ""), Mrs.NetworkAddress);
                stitchModel.add(stitchModel.createStatement(vnicRouteTo, Mrs.type, "ipv4-prefix"));
                stitchModel.add(stitchModel.createStatement(vnicRouteTo, Mrs.value, strRouteTo));
                stitchModel.add(stitchModel.createStatement(vnicRoute, Mrs.routeTo, vnicRouteTo));
                Resource vnicRouteVia = RdfOwl.createResource(stitchModel, resVnic.getURI() + ":next+" + strRouteVia.replaceAll("/", ""), Mrs.NetworkAddress);
                stitchModel.add(stitchModel.createStatement(vnicRouteVia, Mrs.type, "ipv4-address"));
                stitchModel.add(stitchModel.createStatement(vnicRouteVia, Mrs.value, strRouteVia));
                stitchModel.add(stitchModel.createStatement(vnicRoute, Mrs.nextHop, vnicRouteVia));
            }
        }
        return stitchModel;
    }
}
