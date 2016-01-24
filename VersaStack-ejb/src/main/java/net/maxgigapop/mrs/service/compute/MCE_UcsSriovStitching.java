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
import net.maxgigapop.mrs.common.ResourceTool;
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
public class MCE_UcsSriovStitching implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_UcsSriovStitching.class.getName());

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(ModelBase systemModel, ServiceDelta annotatedDelta) {
        log.log(Level.FINE, "MCE_UcsSriovStitching::process {0}", annotatedDelta);
        try {
            log.log(Level.FINE, "\n>>>MCE_UcsSriovStitching--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_UcsSriovStitching.class.getName()).log(Level.SEVERE, null, ex);
        }
        //@TODO: make the initial data imports a common function
        // importPolicyData : Interface->Stitching->List<PolicyData>
        String sparql = "SELECT ?policy ?data ?type ?value WHERE {"
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_UcsSriovStitching'. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?type. ?data spa:value ?value. "
                + "FILTER not exists {?policy spa:dependOn ?other} "
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        Map<Resource, Map> stitchPolicyMap = new HashMap<>();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resPolicy = querySolution.get("policy").asResource();
            if (!stitchPolicyMap.containsKey(resPolicy)) {
                Map<String, List> stitchMap = new HashMap<>();
                List dataList = new ArrayList<>();
                stitchMap.put("imports", dataList);
                stitchPolicyMap.put(resPolicy, stitchMap);
            }
            Resource resData = querySolution.get("data").asResource();
            RDFNode nodeDataType = querySolution.get("type");
            RDFNode nodeDataValue = querySolution.get("value");
            boolean existed =false;
            for (Map dataMap: (List<Map>)stitchPolicyMap.get(resPolicy).get("imports")) {
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
                ((List)stitchPolicyMap.get(resPolicy).get("imports")).add(policyData);
            }
        }
        ServiceDelta outputDelta = annotatedDelta.clone();

        for (Resource policyAction : stitchPolicyMap.keySet()) {
            OntModel stitchModel = this.doStitching(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), policyAction, stitchPolicyMap.get(policyAction));
            // merge the placement satements into spaModel
            if (stitchModel != null) {
                outputDelta.getModelAddition().getOntModel().add(stitchModel.getBaseModel());
            }

            // no export ?
            //this.exportPolicyData()

            // remove policy dependency
            MCETools.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), policyAction);
        }
        return new AsyncResult(outputDelta);
    }

    //@TODO: Stitch ( VGW | VPC | Subnet? )to ( DcVx | L2Path )

    // General logic: 1. find the "terminal / end" containing resource (eg. Host Node or Topology)
    // 2. identify the "attach-point" resource (eg. VLAN port) along with a stitching path
    // 3. add statements to the stitching path to connect the terminal to the attach-point (if applicable)
    private OntModel doStitching(OntModel systemModel, OntModel spaModel, Resource policyAction, Map stitchPolicyData) {
        //@TODO: common logic
        List<Map> dataMapList = (List<Map>)stitchPolicyData.get("imports");
        JSONObject jsonStitchReq = null;
        for (Map entry : dataMapList) {
            if (!entry.containsKey("data") || !entry.containsKey("type") || !entry.containsKey("value")) {
                continue;
            }
            if (entry.get("type").toString().equalsIgnoreCase("JSON")) {
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonObj = (JSONObject) parser.parse((String) entry.get("value"));
                    if (jsonStitchReq == null) {
                        jsonStitchReq = jsonObj;
                    } else { // merge
                        for (Object key: jsonObj.keySet()) {
                            jsonStitchReq.put(key, jsonObj.get(key));
                        }
                    }
                } catch (ParseException e) {
                    throw new EJBException(String.format("%s::process doStitching cannot parse json string %s", this.getClass().getName(), (String) entry.get("value")));
                }
            } else {
                throw new EJBException(String.format("%s::process doStitching does not import policyData of %s type", entry.get("type").toString()));
            }
        }
        
        if (jsonStitchReq == null || jsonStitchReq.isEmpty()) {
            throw new EJBException(String.format("%s::process doStitching receive none request for <%s>", this.getClass().getName(), policyAction));
        }

        OntModel stitchModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Model unionSysModel = spaModel.union(systemModel);
        try {
            log.log(Level.INFO, "\n>>>MCE_UcsSriovStitching--unionSysModel=\n" + ModelUtil.marshalModel(unionSysModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_UcsSriovStitching.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!jsonStitchReq.containsKey("stitch_from") || (!jsonStitchReq.containsKey("to_port_profile") && !jsonStitchReq.containsKey("to_l2path"))) {
            throw new EJBException(String.format("%s::process doStitching imports incomplete JSON data", this.getClass().getName()));
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
            throw new EJBException(String.format("%s::process cannot find resource for '%s': must be URI for a VM with SRIOV", this.getClass().getName(), stitchFromUri));
        }
        Resource resPortProfile = null;
        if (jsonStitchReq.containsKey("to_port_profile")) {
            String stitchToProfile = (String) jsonStitchReq.get("to_port_profile");
            sparql = "SELECT ?profile WHERE {"
                    + "?profile a mrs:SwitchingSubnet. "
                    + "?profile mrs:type \"Cisco_UCS_Port_Profile\" . "
                    + String.format("?profile mrs:value \"%s\" . ", stitchToProfile)
                    + "}";

            r = ModelUtil.sparqlQuery(unionSysModel, sparql);
            if (!r.hasNext()) {
                throw new EJBException(String.format("%s::process cannot find resource for '%s': must be value for a SwitchingSubnet of 'UCS_Port_Profile' type", this.getClass().getName(), stitchToProfile));
            }
            resPortProfile = r.next().getResource("profile");
        } else { //if (jsonStitchReq.containsKey("to_l2path"))
            JSONArray stitchToPath = (JSONArray) jsonStitchReq.get("to_l2path");
            if (stitchToPath.isEmpty()) {
                throw new EJBException(String.format("%s::process cannot parse JSON data 'to_l2path': %s", this.getClass().getName(), stitchToPath));
            }
            for (Object obj : stitchToPath) {
                JSONObject jsonObj = (JSONObject) obj;
                if (!jsonObj.containsKey("uri")) {
                    throw new EJBException(String.format("%s::process cannot parse JSON data 'to_l2path': %s - invalid hop: %s", this.getClass().getName(), stitchToPath, jsonObj.toJSONString()));
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
            throw new EJBException(String.format("%s::process cannot find a SwitchingSubnet of 'UCS_Port_Profile' type to stitch to (in %s)", this.getClass().getName(), jsonStitchReq.containsKey("to_port_profile") ? (String) jsonStitchReq.get("to_l2path") : (String) jsonStitchReq.get("to_port_profile")));
        }
        int ethNum = 1; 
        while (unionSysModel.contains(unionSysModel.getResource(resVm.getURI()+String.format(":port+eth%d",ethNum)), RdfOwl.type, Nml.BidirectionalPort)) {
            ethNum++;
        }
        Resource resVnic = RdfOwl.createResource(spaModel, resVm.getURI() + String.format(":port+eth%d", ethNum), Nml.BidirectionalPort);

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
            resRoutingSvc = RdfOwl.createResource(spaModel, resVm.getURI()+":linuxrouting", Mrs.RoutingService);
            spaModel.add(spaModel.createStatement(resVm, Nml.hasService, resRoutingSvc));
        }
        JSONArray routes = (JSONArray) jsonStitchReq.get("routes");
            for (Object obj: routes) {
                JSONObject route = (JSONObject) obj;
                String strRouteTo = (String) route.get("to");
                String strRouteVia = (String) route.get("next_hop");
                Resource vnicRoute = RdfOwl.createResource(stitchModel, resVnic.getURI() + ":route+to-" + strRouteTo + "-via-" + strRouteVia, Mrs.Route);
                stitchModel.add(stitchModel.createStatement(resRoutingSvc, Mrs.providesRoute, vnicRoute));
                Resource vnicRouteTo = RdfOwl.createResource(stitchModel, resVnic.getURI() + ":routeto+" + strRouteTo, Mrs.NetworkAddress);
                stitchModel.add(stitchModel.createStatement(vnicRouteTo, Mrs.type, "ipv4-prefix"));
                stitchModel.add(stitchModel.createStatement(vnicRouteTo, Mrs.value, strRouteTo));
                stitchModel.add(stitchModel.createStatement(vnicRoute, Mrs.routeTo, vnicRouteTo));
                Resource vnicRouteVia = RdfOwl.createResource(stitchModel, resVnic.getURI() + ":next+" + strRouteVia, Mrs.NetworkAddress);
                stitchModel.add(stitchModel.createStatement(vnicRouteVia, Mrs.type, "ipv4-address"));
                stitchModel.add(stitchModel.createStatement(vnicRouteVia, Mrs.value, strRouteVia));
                stitchModel.add(stitchModel.createStatement(vnicRoute, Mrs.nextHop, vnicRouteVia));
            }
        }
        return stitchModel;
    }
}
