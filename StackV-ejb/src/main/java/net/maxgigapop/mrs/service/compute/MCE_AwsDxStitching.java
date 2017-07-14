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

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_AwsDxStitching extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_AwsDxStitching.class.getName(), "MCE_AwsDxStitching");

    private static final String OSpec_Template
            = "{\n"
            + "\"vlan\": \"?vlan?\",\n"
            + "\"dxvif_name\": \"?dxvif_name?\",\n"
            + "\"vgw_name\": \"?vgw_name?\",\n"
            + "\"amazon_ip\": \"?amazon_ip?\",\n"
            + "\"customer_ip\": \"?customer_ip?\",\n"
            + "\"authkey\": \"?authkey?\",\n"
            + "\"customer_asn\": \"?customer_asn?\",\n"
            + "\"#sparql\": \"SELECT ?dxvif ?vgw WHERE {?dxvif mrs:type \\\"direct-connect-vif\\\". ?dxvif nml:isAlias ?vgw. }\",\n"
            + "\"#required\": \"true\",\n"
            + "\"#sparql-ext\": \"SELECT ?dxvif_name ?vgw_name ?customer_asn ?vlan ?amazon_ip ?customer_ip ?authkey "
            + "WHERE {"
            + "?dxvif mrs:hasNetworkAddress ?netaddr_asn. "
            + "?netaddr_asn mrs:type \\\"bgp-asn\\\". "
            + "?netaddr_asn mrs:value ?customer_asn. "
            + "?dxvif nml:hasLabelGroup ?lg_vlan. "
            + "?lg_vlan nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. "
            + "?lg_vlan nml:values ?vlan. "
            + "OPTIONAL { ?dxvif nml:name ?dxvif_name. } "
            + "OPTIONAL {?dxvif mrs:hasNetworkAddress ?netaddr_amazon_ip. ?netaddr_amazon_ip mrs:type \\\"ipv4-address:amazon\\\". "
            + "?netaddr_amazon_ip mrs:value ?amazon_ip. ?dxvif mrs:hasNetworkAddress ?netaddr_customer_ip. "
            + "?netaddr_customer_ip mrs:type \\\"ipv4-address:customer\\\". ?netaddr_customer_ip mrs:value ?customer_ip. }"
            + "OPTIONAL {?dxvif mrs:hasNetworkAddress ?netaddr_authkey. ?netaddr_authkey mrs:type \\\"bgp-authkey\\\". ?netaddr_authkey mrs:value ?authkey. }"
            + "}\""
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

        if (!jsonStitchReq.containsKey("parent") || !jsonStitchReq.containsKey("stitch_from")
                || (!jsonStitchReq.containsKey("to_dxvif") && !jsonStitchReq.containsKey("to_l2path"))) {
            throw logger.error_throwing(method, "imported incomplete JSON data");
        }
        String awsUri = (String) jsonStitchReq.get("parent");
        String stitchFromUri = (String) jsonStitchReq.get("stitch_from");
        if (stitchFromUri.isEmpty() || (!stitchFromUri.startsWith("ur") && stitchFromUri.startsWith("x-"))) {
            throw logger.error_throwing(method, "imported invalid 'stitch_from' value");
        }
        // 1. get VGW resources
        String sparql = "SELECT ?vgw WHERE {{"
                + "?aws nml:hasTopology ?vpc."
                + "?aws a nml:Topology."
                + "?vpc a nml:Topology."
                + "?vpc nml:hasBidirectionalPort ?vgw."
                + "?vgw mrs:type \"vpn-gateway\""
                + String.format("FILTER ((?aws = <%s> && ?vgw = <%s>) )", awsUri, stitchFromUri)
                + "} UNION {"
                + "?aws nml:hasTopology ?vpc."
                + "?aws a nml:Topology."
                + "?vpc a nml:Topology."
                + "?vpc nml:hasBidirectionalPort ?vgw."
                + "?vgw mrs:type \"vpn-gateway\""
                + String.format("FILTER ((?aws = <%s> && ?vpc = <%s>) )", awsUri, stitchFromUri)
                + "} UNION {"
                + "?aws nml:hasTopology ?vpc."
                + "?aws a nml:Topology."
                + "?vpc a nml:Topology."
                + "?vpc nml:hasService ?swsvc."
                + "?swsvc mrs:providesSubnet ?subnet."
                + "?vpc nml:hasBidirectionalPort ?vgw."
                + "?vgw mrs:type \"vpn-gateway\". "
                + String.format("FILTER ((?aws = <%s> && ?subnet = <%s>) )", awsUri, stitchFromUri)
                + "}}";

        ResultSet r = ModelUtil.sparqlQuery(unionSysModel, sparql);
        Resource resVgw = null;
        if (r.hasNext()) {
            QuerySolution solution = r.nextSolution();
            resVgw = solution.getResource("vgw");
        } else {
            throw logger.error_throwing(method, String.format("cannot find resource for '%s: must be uri for a VPC or VGW or Subnet", stitchFromUri));
        }
        Resource resDxvif = null;
        if (jsonStitchReq.containsKey("to_dxvif")) {
            String stitchToUri = (String) jsonStitchReq.get("to_dxvif");
            if (unionSysModel.contains(unionSysModel.getResource(stitchToUri), Mrs.type, "direct-connect-vif")) {
                resDxvif = unionSysModel.getResource(stitchToUri);
            } else {
            throw logger.error_throwing(method, String.format("cannot find resource for '%s: must be uri a DirectConnect vif", stitchToUri));
            }
        } else { //if (jsonStitchReq.containsKey("to_l2path"))
            JSONArray stitchToPath = (JSONArray) jsonStitchReq.get("to_l2path");
            if (stitchToPath.isEmpty()) {
                throw logger.error_throwing(method, String.format("cannot parse JSON data 'to_l2path': %s", stitchToPath));
            }
            for (Object obj : stitchToPath) {
                JSONObject jsonObj = (JSONObject) obj;
                if (!jsonObj.containsKey("hop")) {
                    throw logger.error_throwing(method, String.format("cannot parse JSON data 'to_l2path': %s - invalid hop: %s", stitchToPath, jsonObj));
                }
                String hopUri = (String) jsonObj.get("hop");
                sparql = "SELECT ?dxvif WHERE {"
                        + "?aws nml:hasBidirectionalPort ?dxport."
                        + "?aws a nml:Topology."
                        + "?dxport nml:hasBidirectionalPort ?dxvif."
                        + String.format("FILTER ((?aws = <%s> && ?dxvif = <%s>) )", awsUri, hopUri)
                        + "}";
                r = ModelUtil.sparqlQuery(unionSysModel, sparql);
                if (r.hasNext()) {
                    QuerySolution solution = r.nextSolution();
                    resDxvif = solution.getResource("dxvif");
                }
            }
        }
        if (resDxvif == null) {
            throw logger.error_throwing(method, String.format("cannot find DxVif resource to stitch to (in %s)", jsonStitchReq.containsKey("to_dxvif") ? (String) jsonStitchReq.get("to_l2path") : (String) jsonStitchReq.get("to_dxvif")));
        }
        stitchModel.add(stitchModel.createStatement(resVgw, Nml.isAlias, resDxvif));
        stitchModel.add(stitchModel.createStatement(resDxvif, Nml.isAlias, resVgw));
        stitchModel.add(stitchModel.createStatement(resDxvif, Mrs.type, "direct-connect-vif"));

        return stitchModel;
    }
}
