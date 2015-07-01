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
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.www.rains.ontmodel.Spa;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_InterfaceVlanStitching implements IModelComputationElement {
    private static final Logger log = Logger.getLogger(MCE_InterfaceVlanStitching.class.getName());
    
    @Override
    @Asynchronous
    public Future<DeltaBase> process(ModelBase systemModel, DeltaBase annotatedDelta) {
        log.log(Level.FINE, "MCE_InterfaceVlanStitching::process {0}", annotatedDelta);
        try {
            log.log(Level.FINE, "\n>>>MCE_InterfaceVlanStitching--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_InterfaceVlanStitching.class.getName()).log(Level.SEVERE, null, ex);
        }
        DeltaBase outputDelta = annotatedDelta.clone();

        // importPolicyData : Interface->Stitching->List<PolicyData>
        String sparql =  "SELECT ?netif ?policy ?data ?type ?value WHERE {"
                + "?netif a nml:BidirectionalPort ."
                + "?netif spa:dependOn ?policy . "
                + "?policy a spa:Stitching. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?type. ?data spa:value ?value. "
                + "FILTER not exists {?policy spa:dependOn ?other} "
                + "}";        


        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        Map<Resource, List> stitchPolicyMap = new HashMap<>();
        while(r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resNetIf = querySolution.get("netif").asResource();
            if (!stitchPolicyMap.containsKey(resNetIf)) {
                List policyList = new ArrayList<>();
                stitchPolicyMap.put(resNetIf, policyList);
            }
            Resource resPolicy = querySolution.get("policy").asResource();
            Resource resData = querySolution.get("data").asResource();
            RDFNode nodeDataType = querySolution.get("type");
            RDFNode nodeDataValue = querySolution.get("value");
            Map policyData = new HashMap<>();
            policyData.put("policy", resPolicy);
            policyData.put("data", resData);
            policyData.put("type", nodeDataType.toString());
            policyData.put("value", nodeDataValue.toString());
            stitchPolicyMap.get(resNetIf).add(policyData);
        }        
        for (Resource resNetIf : stitchPolicyMap.keySet()) {
            OntModel stitchModel = this.doStitching(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), resNetIf, stitchPolicyMap.get(resNetIf));
            // merge the placement satements into spaModel
            if (stitchModel != null)
                outputDelta.getModelAddition().getOntModel().add(stitchModel.getBaseModel());
            // remove policy dependency
            this.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), resNetIf);   
        }
        return new AsyncResult(outputDelta);
    }

    // General logic: 1. find the "terminal / end" containing resource (eg. Host Node or Topology)
    // 2. identify the "attach-point" resource (eg. VLAN port) along with a stitching path
    // 3. add statements to the stitching path to connect the terminal to the attach-point (if applicable)
    private OntModel doStitching (OntModel systemModel, OntModel spaModel, Resource netIf, List<Map> stitchPolicyData) {
        String policyDataType = "SIMPLE_HOST";
        Resource endSite = null;
        List<Resource> vlanPorts = new ArrayList<>();
        for (Map entry: stitchPolicyData) {
            if (!entry.containsKey("data") || !entry.containsKey("type") || !entry.containsKey("value")) 
                continue;
            if (((String)entry.get("type")).equals("InterfaceVlanStitching:StitchingType")) {
                policyDataType = (String)entry.get("value");
            } else if (((String)entry.get("type")).equals("VMFilterPlacement:HostSite")) {
                endSite = systemModel.getResource((String)entry.get("value"));
            } else if (((String)entry.get("type")).equals("MPVlanConnection:VlanPorts")) {
                Resource vlanPort = systemModel.getResource((String)entry.get("value"));
                vlanPorts.add(vlanPort);
            }
        }
        if (endSite == null || vlanPorts.isEmpty())
            throw new EJBException(String.format("%s::doStitching on <%s> miss policy data.", this, netIf));
        switch (policyDataType) {
            case "SIMPLE_HOST":
                return stitchWithSimpleHost(systemModel, spaModel, netIf, endSite, vlanPorts);
            case "OPENSTACK_HOST":
                return stitchWithOpenstackHost(systemModel, spaModel, netIf, endSite, vlanPorts);
            case "AWS_VPC":
                return stitchWithAwsVpc(systemModel, spaModel, netIf, endSite, vlanPorts);
            default:
                throw new EJBException(String.format("%s::doStitching on <%s> has unrecognized StitchingType='%s'.", this, netIf, policyDataType));
        }
    }
    
    private OntModel stitchWithSimpleHost(OntModel systemModel, OntModel spaModel, Resource netIf, Resource endSite, List<Resource> vlanPorts) {
        // do nothing
        return null;
    }
    
    private OntModel stitchWithAwsVpc(OntModel systemModel, OntModel spaModel, Resource netIf, Resource endSite, List<Resource> vlanPorts) {
        OntModel stitchModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //?? find BiPort that AWS (top level topology) has that is parent to one of the vlanPorts
        /*
        String sparql = "SELECT ?dcPort WHERE {"
                + "{?awsTopo nml:hasBidirectionalPort ?dcPort ."
                + String.format("?awsTopo nml:hasTopology <%s>.", endSite.getURI())
                + "?awsTopo a nml:Topology."
                + String.format("<%s> a nml:Topology.", endSite.getURI())
                + "?dcPort a nml:BidirectionalPort."
                + String.format("?dcPort nml:hasBidirectionalPort <%s>.", netIf.getURI())
                + "} UNION {"
                + String.format("{<%s> nml:hasBidirectionalPort ?dcPort .", endSite.getURI())
                + String.format("<%s> a nml:Topology.", endSite.getURI())
                + "?dcPort a nml:BidirectionalPort."
                + String.format("?dcPort nml:hasBidirectionalPort <%s>.", netIf.getURI())
                + "} }";
        */
        Model unionSysModel = spaModel.union(systemModel);
        try {
            log.log(Level.FINE, "\n>>>MCE_InterfaceVlanStitching--unionSysModel=\n" + ModelUtil.marshalModel(unionSysModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_InterfaceVlanStitching.class.getName()).log(Level.SEVERE, null, ex);
        }
        // select a VPC based on endSite
        List<QuerySolution> solutions = new ArrayList<>();
        for (Resource resVlanPort: vlanPorts) {
            String sparql = "SELECT ?vpc WHERE {"
                    + "?aws nml:hasBidirectionalPort ?dcPort ."
                    + "?aws nml:hasTopology ?vpc."
                    + "?aws a nml:Topology."
                    + "?vpc a nml:Topology."
                    + "?dcPort a nml:BidirectionalPort."
                    + String.format("?dcPort nml:hasBidirectionalPort <%s>.", resVlanPort.getURI())
                    + String.format("FILTER (?aws = <%s> || ?vpc = <%s>)", endSite.getURI(), endSite.getURI())
                    + "}";
            ResultSet r = ModelUtil.sparqlQuery(unionSysModel, sparql);
            while (r.hasNext()) {
                solutions.add(r.next());
            }
        }
        if (solutions.isEmpty())
            return null;
        Resource resAws = solutions.get(0).getResource("aws");
        Resource resVpc = solutions.get(0).getResource("vpc");
        // create VGW and attach to VPC (endSite), and peer (isAlias) it with the vlanPort
        Resource VPNGW_TAG = RdfOwl.createResource(stitchModel, resAws + ":vpngwTag", Mrs.Tag);
        stitchModel.add(stitchModel.createStatement(VPNGW_TAG, Mrs.type, "gateway"));
        stitchModel.add(stitchModel.createStatement(VPNGW_TAG, Mrs.value, "vpn"));
        Resource VIRTUAL_INTERFACE_TAG = RdfOwl.createResource(stitchModel, resAws + ":virtualinterfaceTag", Mrs.Tag);
        stitchModel.add(stitchModel.createStatement(VIRTUAL_INTERFACE_TAG, Mrs.type, "interface"));
        stitchModel.add(stitchModel.createStatement(VIRTUAL_INTERFACE_TAG, Mrs.value, "virtual"));
        String vpnGatewayId = "vgw+" + UUID.randomUUID();
        Resource resVgw = RdfOwl.createResource(stitchModel, resAws + ":" + vpnGatewayId, Nml.BidirectionalPort);
        stitchModel.add(stitchModel.createStatement(resVgw, Mrs.hasTag, VPNGW_TAG));
        stitchModel.add(stitchModel.createStatement(netIf, Mrs.hasTag, VIRTUAL_INTERFACE_TAG));
        stitchModel.add(stitchModel.createStatement(resAws, Nml.hasBidirectionalPort, resVgw));
        stitchModel.add(stitchModel.createStatement(resVgw, Nml.isAlias, netIf));
        stitchModel.add(stitchModel.createStatement(netIf, Nml.isAlias, resVgw));
        return stitchModel; 
    }
    
    private OntModel stitchWithOpenstackHost(OntModel systemModel, OntModel spaModel, Resource netIf, Resource endSite, List<Resource> vlanPorts) {
        // do nothing
        return null; 
    }
    
    private void removeResolvedAnnotation(OntModel spaModel, Resource netIf) {
        String sparql = "SELECT ?anyOther ?policyAction WHERE {"
                + String.format("<%s> spa:dependOn ?policyAction .", netIf.getURI())
                + "?policyAction a spa:Stitching."
                + "?anyOther spa:dependOn ?policyAction . "
                + "?policyData a spa:PolicyData . "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resAnyOther = querySolution.get("anyOther").asResource();
            Resource resPolicy = querySolution.get("policyAction").asResource();
            spaModel.remove(resAnyOther, Spa.dependOn, resPolicy);
        }
    }
}
