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
import java.util.logging.Level;
import java.util.logging.Logger;
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
import net.maxgigapop.mrs.common.TagSet;
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
            = "{\n" +
"	\"connections\": [\n" +
"		{\n" +
"			\"id\": \"?dxconn_id?\",\n" +
"			\"virtual_interfaces\": [\n" +
"				{\n" +
"					\"uri\": \"?dxvif?\",\n" +
"					\"vlan\": \"?dxvif_vlan?\",\n" +
"					\"#sparsql\": \"SELECT DISTINCT ?dxvif ?dxvif_vlan WHERE {?dxconn nml:hasBidirectionalPort ?dxvif. ?dxvif mrs:type \\\"direct-connect-vif\\\". ?dxvif nml:hasLabelGroup ?lg . ?lg nml:values ?dxvif_vlan. }\"\n" +
"				}\n" +
"			],\n" +
"			\"#sparsql\": \"SELECT DISTINCT ?dxconn WHERE { ?dxconn nml:hasBidirectionalPort ?dxvif. ?dxvif mrs:type \\\"direct-connect-vif\\\". }\",\n" +
"			\"#sparsql-ext\": \"SELECT  ?dxconn_id WHERE { ?dxconn nml:name  ?dxconn_id. }\"\n" +
"		}\n" +
"	]\n" +
"}";

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

        // find dxConn URI if given namd/ID
        if (!dxConn.startsWith("urn:")) {
            String sparql = "SELECT DISTINCT ?dxconn WHERE {"
                    + "?dxconn a nml:BidirectionalPort . "
                    + String.format("?dxconn nml:name \"%s\".", dxConn)
                    + "}";
            ResultSet r = ModelUtil.executeQuery(sparql, null, systemModel);
            if (r.hasNext()) {
                QuerySolution q = r.next();
                dxConn = q.get("dxconn").asResource().getURI();
            }
        }
        Resource resDC = dxvifModel.createResource(dxConn);

        // sparql for VLAN range of resDC
        String sparql = "SELECT ?vlan_range WHERE {"
                + String.format("<%s> nml:hasLabelGroup ?lg. ", dxConn)
                + "?lg nml:values ?vlan_range. "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(systemModel, sparql);
        if (!r.hasNext()) {
            throw logger.error_throwing(method, "Cannot get VLAN range for DirectConnect: " + dxConn);
        }
        QuerySolution q = r.next();
        String dxConnVlanRange = q.get("vlan_range").asLiteral().getString();
        TagSet vlanRange;
        try {
            vlanRange = new TagSet(dxConnVlanRange);
        } catch (TagSet.InvalidVlanRangeExeption ex) {
            throw logger.error_throwing(method, "Malformed VLAN range [" + dxConnVlanRange + "] for DirectConnect:" + dxConn);
        }

        Model unionSysModel = spaModel.union(systemModel);

        // sparql for allocated VLANs
        sparql = "SELECT DISTINCT ?dxvif ?vlan WHERE {"
                + String.format("<%s> nml:hasBidirectionalPort ?dxvif. ", dxConn)
                + "?dxvif mrs:type \"direct-connect-vif\" . "
                + "?dxvif nml:hasLabelGroup ?lg . "
                + "?lg nml:values ?vlan. "
                + "}";
        r = ModelUtil.sparqlQuery(unionSysModel, sparql);
        // get availalbe range (with intersection of given range in dxVifVlan)
        while (!r.hasNext()) {
            q = r.next();
            String dxvifUri = q.get("dxvif").asResource().getURI();
            String vlanTag = q.get("vlan").asLiteral().getString();
            Integer vlan;
            try {
                vlan = Integer.parseInt(vlanTag);
            } catch (NumberFormatException ex) {
                logger.warning(method, String.format("Malformed VLAN '%s' for DC virtual-interface: %s", vlanTag, dxvifUri));
                continue;
            }
            vlanRange.removeTag(vlan);
        }
        if (vlanRange.isEmpty()) {
            throw logger.error_throwing(method, "No more VLAN available for DirectConnect:" + dxConn);
        }
        // compute VLAN tag if given 'any' or a range.
        if (dxVifVlan.equalsIgnoreCase("any")) {
            dxVifVlan = Integer.toString(vlanRange.getRandom());
        } else if (dxVifVlan.matches("^\\d+-\\d+$")) {
            TagSet vlanRangeNarrowed;
            try {
                vlanRangeNarrowed = new TagSet(dxVifVlan);
            } catch (TagSet.InvalidVlanRangeExeption ex) {
                throw logger.error_throwing(method, "Malformed VLAN range [" + dxVifVlan + "] as provided input to this MCE");
            }
            vlanRange.intersect(vlanRangeNarrowed);
            if (vlanRange.isEmpty()) {
                throw logger.error_throwing(method, "No more VLAN available for DirectConnect:" + dxConn);
            }
            // pick random to rewrite dxVifVlan
            dxVifVlan = Integer.toString(vlanRange.getRandom());
        } else { // verify availability of the assumed single given VLAN 
            Integer vlan;
            try {
                vlan = Integer.parseInt(dxVifVlan);
            } catch (NumberFormatException ex) {
                throw logger.error_throwing(method, "Malformed VLAN [" + dxVifVlan + "] as provided input to this MCE");
            }
            if (!vlanRange.hasTag(vlan)) {
                throw logger.error_throwing(method, "VLAN " + dxVifVlan + " is not available for DirectConnect:" + dxConn);
            }
        }

        Resource resDxvif = RdfOwl.createResource(dxvifModel, String.format("%s:dxvif+vlan%s", dxConn, dxVifVlan), Nml.BidirectionalPort);
        dxvifModel.add(dxvifModel.createStatement(resDC, Nml.hasBidirectionalPort, resDxvif));
        dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.type, "direct-connect-vif"));
        dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.value, "direct-connect-vif+private"));
        Resource resVirtualIfVG = RdfOwl.createResource(dxvifModel, String.format("%s:labelgroup+%s", resDxvif.getURI(), dxVifVlan), Nml.LabelGroup);
        dxvifModel.add(dxvifModel.createStatement(resVirtualIfVG, Nml.labeltype, RdfOwl.labelTypeVLAN));
        dxvifModel.add(dxvifModel.createStatement(resVirtualIfVG, Nml.values, dxVifVlan));
        dxvifModel.add(dxvifModel.createStatement(resDxvif, Nml.hasLabelGroup, resVirtualIfVG));

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
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.type, "bgp-asn"));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.value, (String) jsonStitchReq.get("customer_asn")));            
        }
        if (jsonStitchReq.containsKey("authkey")) {
            vifAttr = RdfOwl.createResource(dxvifModel, resDxvif.getURI() + ":authkey", Mrs.NetworkAddress);
            dxvifModel.add(dxvifModel.createStatement(resDxvif, Mrs.hasNetworkAddress, vifAttr));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.type, "bgp-authkey"));
            dxvifModel.add(dxvifModel.createStatement(vifAttr, Mrs.value, (String) jsonStitchReq.get("authkey")));
        }

        return dxvifModel;
    }
}
