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
public class MCE_AWSDirectConnectStitch implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_AWSDirectConnectStitch.class.getName());

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(ModelBase systemModel, ServiceDelta annotatedDelta) {
        log.log(Level.INFO, "MCE_AWSDirectConnectStitch::process {0}", annotatedDelta);
        try {
            log.log(Level.INFO, "\n>>>MCE_AWSDirectConnectStitch--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_AWSDirectConnectStitch.class.getName()).log(Level.SEVERE, null, ex);
        }

        // importPolicyData : Interface->Stitching->List<PolicyData>
        String sparql = "SELECT ?res ?policy ?actionValue ?data ?dataType ?dataValue WHERE {"
                + "?res spa:dependOn ?policy . "
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_AWSDirectConnectStitch'. "
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
                List connList = new ArrayList<>();
                stitchMap.put("stitches", connList);
                List dataList = new ArrayList<>();
                stitchMap.put("imports", dataList);
                stitchPolicyMap.put(resPolicy, stitchMap);
            }
            Resource res = querySolution.get("res").asResource();
            if (!((List)stitchPolicyMap.get(resPolicy).get("stitches")).contains(res))
                ((List)stitchPolicyMap.get(resPolicy).get("stitches")).add(res);
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
        List<Resource> stitchList = (List<Resource>)stitchPolicyData.get("stitches");
        //@TODO: common logic
        List<Map> dataMapList = (List<Map>)stitchPolicyData.get("imports");
        JSONObject jsonStitchReqs = null;
        for (Map entry : dataMapList) {
            if (!entry.containsKey("data") || !entry.containsKey("type") || !entry.containsKey("value")) {
                continue;
            }
            if (entry.get("type").toString().equalsIgnoreCase("JSON")) {
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonObj = (JSONObject) parser.parse((String) entry.get("value"));
                    if (jsonStitchReqs == null) {
                        jsonStitchReqs = jsonObj;
                    } else { // merge
                        for (Object key: jsonObj.keySet()) {
                            jsonStitchReqs.put(key, jsonObj.get(key));
                        }
                    }
                } catch (ParseException e) {
                    throw new EJBException(String.format("%s::process doStitching cannot parse json string %s", this.getClass().getName(), (String) entry.get("value")));
                }
            } else {
                throw new EJBException(String.format("%s::process doStitching does not import policyData of %s type", entry.get("type").toString()));
            }
        }
        
        if (jsonStitchReqs == null || jsonStitchReqs.isEmpty()) {
            throw new EJBException(String.format("%s::process doStitching receive none connection request for <%s>", this.getClass().getName(), policyAction));
        }

        OntModel stitchModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Model unionSysModel = spaModel.union(systemModel);
        try {
            log.log(Level.FINE, "\n>>>MCE_AWSDirectConnectStitch--unionSysModel=\n" + ModelUtil.marshalModel(unionSysModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_AWSDirectConnectStitch.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for (Object stitchReq: jsonStitchReqs.keySet()) {
            String stitchId = (String) stitchReq;
            JSONObject jsonStitchReq = (JSONObject)jsonStitchReqs.get(stitchReq);
            if (!jsonStitchReq.containsKey("parent") || !jsonStitchReq.containsKey("stitch_from") || 
                    (!jsonStitchReq.containsKey("to_dxvif") && !jsonStitchReq.containsKey("to_l2path"))) {
                throw new EJBException(String.format("%s::process doStitching imports incomplete JSON data for '%s'", this.getClass().getName(), stitchId));
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
                    + "?swsvc mrs:providesSubnet ?subnet"
                    + "?vpc nml:hasBidirectionalPort ?vgw."
                    + "?vgw mrs:type \"vpn-gateway\""
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
                String stitchToUri =  (String) jsonStitchReq.get("to_dxvif");
                if (unionSysModel.contains(unionSysModel.getResource(stitchToUri), RdfOwl.type, "direct-connect-vif")) {
                    resDxvif = unionSysModel.getResource(stitchToUri);
                } else {
                    throw new EJBException(String.format("%s::process cannot find resource for '%s: must be uri a DirectConnect vif", this.getClass().getName(), stitchToUri));
                }
            } else  { //if (jsonStitchReq.containsKey("to_l2path"))
                String stitchToPath = (String) jsonStitchReq.get("to_l2path");
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonObj = (JSONObject) parser.parse(stitchToPath);
                    Object[] objs = jsonObj.values().toArray();
                    JSONArray pathHops = (JSONArray) objs[0];
                    if (pathHops.isEmpty()) {
                        throw new EJBException(String.format("%s::process cannot parse JSON data 'to_l2path': %s", this.getClass().getName(), stitchToPath));
                    }
                    for (Object obj: pathHops) {
                        jsonObj = (JSONObject) obj;
                        if (!jsonObj.containsKey("uri")) {
                            throw new EJBException(String.format("%s::process cannot parse JSON data 'to_l2path': %s - invalid hop: %s", this.getClass().getName(), stitchToPath, jsonObj.toJSONString()));
                        }
                        String hopUri = (String) jsonObj.get("uri");
                        sparql = "SELECT ?vgw WHERE {"
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
                } catch (ParseException e) {
                    throw new EJBException(String.format("%s::process cannot parse JSON data 'to_l2path': %s", this.getClass().getName(), stitchToPath));
                }           
            } 
            if (resDxvif == null) {
                throw new EJBException(String.format("%s::process cannot find DxVif resource to stitch to (in %s)", this.getClass().getName(), jsonStitchReq.containsKey("to_dxvif")  ? (String) jsonStitchReq.get("to_l2path"): (String) jsonStitchReq.get("to_dxvif")));
            }
            stitchModel.add(stitchModel.createStatement(resVgw, Nml.isAlias, resDxvif));
            stitchModel.add(stitchModel.createStatement(resDxvif, Nml.isAlias, resVgw));
            stitchModel.add(stitchModel.createStatement(resDxvif, Mrs.type, "direct-connect-vif"));            
        }
        
        return stitchModel;
    }
}
