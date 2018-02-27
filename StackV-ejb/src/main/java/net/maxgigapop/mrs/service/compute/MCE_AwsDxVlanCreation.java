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
import net.maxgigapop.mrs.common.ResourceTool;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_AwsDxVlanCreation extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_AwsDxVlanCreation.class.getName(), "MCE_AwsDxVlanCreation");

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
            OntModel addModel = this.doCreation(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), res, policyResDataMap.get(res));
            if (addModel != null) {
                outputDelta.getModelAddition().getOntModel().add(addModel.getBaseModel());
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

    private OntModel doCreation(OntModel systemModel, OntModel spaModel, Resource res, JSONObject jsonStitchReq) {
        String method = "doCreation";
        logger.message(method, "@doCreation -> " + res);

        OntModel dxvifModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        if (!jsonStitchReq.containsKey("direct_connect") || !jsonStitchReq.containsKey("dxvif_vlan") || !jsonStitchReq.containsKey("owner_account")) {
            throw logger.error_throwing(method, "imported incomplete JSON data: require at least direct_connect uri/id, dxvif vlan and owner account number.");
        }
        String dxConn = (String) jsonStitchReq.get("direct_connect");
        String dxVifVlan = (String) jsonStitchReq.get("dxvif_vlan");

        //$$ find dxConn URI if given ID
        if (!dxConn.startsWith("urn:")) {
            
        }
        Resource resDC = dxvifModel.createResource(dxConn);
        //$$ compute VLAN tag if any.
        if (dxVifVlan.equalsIgnoreCase("any") || dxVifVlan.matches("^\\d+-\\d+$")) {
            Model unionSysModel = spaModel.union(systemModel);
            //$$ sparql for VLAN range of resDC
            
            //$$ sparql for VLAN labels
            
            //$$ get availalbe range (with intersection of given range in dxVifVlan)
            
            //$$ pick random to rewrite dxVifVlan
        }
        
        Resource resDxvif = RdfOwl.createResource(dxvifModel, String.format("%s:dxvif+vlan%s", dxConn, dxVifVlan), Nml.BidirectionalPort);
        dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.type, "direct-connect-vif"));
        dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.value, "direct-connect-vif+private"));
        Resource resVirtualIfVG = RdfOwl.createResource(dxvifModel, String.format("%s:labelgroup+%s", resDxvif.getURI(), dxVifVlan), Nml.LabelGroup);
        dxvifModel.add(dxvifModel.createStatement(resVirtualIfVG, Nml.labeltype, RdfOwl.labelTypeVLAN));
        dxvifModel.add(dxvifModel.createStatement(resVirtualIfVG, Nml.values, dxVifVlan));
        dxvifModel.add(dxvifModel.createStatement(resDC, Nml.hasLabelGroup, resVirtualIfVG));

        Resource vifAttr = RdfOwl.createResource(dxvifModel, resDxvif.getURI() + ":owner_account", Mrs.NetworkAddress);
        dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.hasNetworkAddress, vifAttr));
        dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.type, "owner-account"));
        dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.value, (String) jsonStitchReq.get("owner_account")));
        if (jsonStitchReq.containsKey("customer_ip")) {
            vifAttr = RdfOwl.createResource(dxvifModel, resDxvif.getURI() + ":customer_ip", Mrs.NetworkAddress);
            dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.hasNetworkAddress, vifAttr));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.type, "ipv4-address:customer"));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.value, (String) jsonStitchReq.get("customer_ip")));
        }
        if (jsonStitchReq.containsKey("amazon_ip")) {
            vifAttr = RdfOwl.createResource(dxvifModel, resDxvif.getURI() + ":amazon_ip", Mrs.NetworkAddress);
            dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.hasNetworkAddress, vifAttr));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.type, "ipv4-address:amazon"));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.value, (String) jsonStitchReq.get("amazon_ip")));
        }
        if (jsonStitchReq.containsKey("customer_asn")) {
            vifAttr = RdfOwl.createResource(dxvifModel, resDxvif.getURI() + ":asn", Mrs.NetworkAddress);
            dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.hasNetworkAddress, vifAttr));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.type, "bgp-authkey"));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.value, (String) jsonStitchReq.get("customer_asn")));            
        }
        if (jsonStitchReq.containsKey("authkey")) {
            vifAttr = RdfOwl.createResource(dxvifModel, resDxvif.getURI() + ":authkey", Mrs.NetworkAddress);
            dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.hasNetworkAddress, vifAttr));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.type, "authkey"));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.value, (String) jsonStitchReq.get("authkey")));
        }

        return dxvifModel;
    }
}
