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

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_AwsDxStitching implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_AwsDxStitching.class.getName());

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(ModelBase systemModel, ServiceDelta annotatedDelta) {
        log.log(Level.FINE, "MCE_AWSDirectConnectStitch::process {0}", annotatedDelta);
        try {
            log.log(Level.FINE, "\n>>>MCE_AWSDirectConnectStitch--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_AwsDxStitching.class.getName()).log(Level.SEVERE, null, ex);
        }

        // importPolicyData : Interface->Stitching->List<PolicyData>
        String sparql = "SELECT ?policy ?data ?type ?value WHERE {"
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_AwsDxStitching'. "
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

            exportPolicyData(outputDelta.getModelAddition().getOntModel(), policyAction, stitchModel, systemModel.getOntModel());

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
            log.log(Level.FINE, "\n>>>MCE_AWSDirectConnectStitch--unionSysModel=\n" + ModelUtil.marshalModel(unionSysModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_AwsDxStitching.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!jsonStitchReq.containsKey("parent") || !jsonStitchReq.containsKey("stitch_from")
                || (!jsonStitchReq.containsKey("to_dxvif") && !jsonStitchReq.containsKey("to_l2path"))) {
            throw new EJBException(String.format("%s::process doStitching imports incomplete JSON data", this.getClass().getName()));
        }
        String awsUri = (String) jsonStitchReq.get("parent");
        String stitchFromUri = (String) jsonStitchReq.get("stitch_from");

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
            throw new EJBException(String.format("%s::process cannot find resource for '%s: must be uri for a VPC or VGW or Subnet", this.getClass().getName(), stitchFromUri));
        }
        Resource resDxvif = null;
        if (jsonStitchReq.containsKey("to_dxvif")) {
            String stitchToUri = (String) jsonStitchReq.get("to_dxvif");
            if (unionSysModel.contains(unionSysModel.getResource(stitchToUri), Mrs.type, "direct-connect-vif")) {
                resDxvif = unionSysModel.getResource(stitchToUri);
            } else {
                throw new EJBException(String.format("%s::process cannot find resource for '%s: must be uri a DirectConnect vif", this.getClass().getName(), stitchToUri));
            }
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
            throw new EJBException(String.format("%s::process cannot find DxVif resource to stitch to (in %s)", this.getClass().getName(), jsonStitchReq.containsKey("to_dxvif") ? (String) jsonStitchReq.get("to_l2path") : (String) jsonStitchReq.get("to_dxvif")));
        }
        stitchModel.add(stitchModel.createStatement(resVgw, Nml.isAlias, resDxvif));
        stitchModel.add(stitchModel.createStatement(resDxvif, Nml.isAlias, resVgw));
        stitchModel.add(stitchModel.createStatement(resDxvif, Mrs.type, "direct-connect-vif"));

        return stitchModel;
    }
    
    private void exportPolicyData(OntModel spaModel, Resource resPolicy, OntModel stitchModel, OntModel systemModel) {
        // find Connection policy -> exportTo -> policyData
        String sparql = "SELECT ?data ?type ?value ?format WHERE {"
                + String.format("<%s> a spa:PolicyAction. ", resPolicy.getURI())
                + String.format("<%s> spa:type 'MCE_AwsDxStitching'. ", resPolicy.getURI())
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
            
            // get export data
            sparql = "SELECT ?dxvif ?vgw WHERE {"
                + "?dxvif mrs:type \"direct-connect-vif\". "
                + "?dxvif nml:isAlias ?vgw. "
                + "}";
            r = ModelUtil.sparqlQuery(stitchModel, sparql);
            if (!r.hasNext()) {
                return;
            }
            QuerySolution solution = r.next();
            Resource resDxvif = solution.getResource("dxvif");
            Resource resVgw = solution.getResource("vgw");
            sparql = "SELECT ?dxvif_name ?asn ?vlan ?amazon_ip ?customer_ip ?authkey WHERE {"
                    + String.format("<%s> mrs:hasNetworkAddress ?netaddr_asn. ", resDxvif.getURI())
                    + "?netaddr_asn mrs:type \"bgp-asn\". "
                    + "?netaddr_asn mrs:value ?asn. "
                    + String.format("<%s> nml:hasLabelGroup ?lg_vlan. ", resDxvif.getURI())
                    + "?lg_vlan nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. "
                    + "?lg_vlan nml:values ?vlan. "
                    + "OPTIONAL {"
                    + String.format("<%s> nml:name ?dxvif_name. ", resDxvif.getURI())
                    + "}"
                    + "OPTIONAL {"
                    + String.format("<%s> mrs:hasNetworkAddress ?netaddr_amazon_ip. ", resDxvif.getURI())
                    + "?netaddr_amazon_ip mrs:type \"ipv4-address:amazon\". "
                    + "?netaddr_amazon_ip mrs:value ?amazon_ip. "
                    + String.format("<%s> mrs:hasNetworkAddress ?netaddr_customer_ip. ", resDxvif.getURI())
                    + "?netaddr_customer_ip mrs:type \"ipv4-address:customer\". "
                    + "?netaddr_customer_ip mrs:value ?customer_ip. "
                    + "}"
                    + "OPTIONAL {"
                    + String.format("<%s> mrs:hasNetworkAddress ?netaddr_authkey. ", resDxvif.getURI())
                    + "?netaddr_authkey mrs:type \"bgp-authkey\". "
                    + "?netaddr_authkey mrs:value ?authkey. "
                    + "}"
                    + "}";
            Model unionModel = ModelFactory.createUnion(systemModel, spaModel); // spaModel contains stitchModel
            r = ModelUtil.sparqlQuery(unionModel, sparql);
            if (!r.hasNext()) {
                return;
            }
            solution = r.next();
            JSONObject dxvifData = new JSONObject();
            dxvifData.put("customer_asn", solution.get("asn").toString());
            dxvifData.put("vlan", solution.get("vlan").toString());
            if (solution.contains("dxvif_name")) {
                dxvifData.put("name", solution.getLiteral("dxvif_name").getString());
            }
            if (solution.contains("amazon_ip")) {
                dxvifData.put("amazon_ip", solution.get("amazon_ip").toString());
            }
            if (solution.contains("customer_ip")) {
                dxvifData.put("customer_ip", solution.get("customer_ip").toString());
            }
            if (solution.contains("authkey")) {
                dxvifData.put("bgp_authkey", solution.get("authkey").toString());
            }
            // put new data into jsonValue
            jsonValue.put(resDxvif.getURI(), dxvifData);
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
