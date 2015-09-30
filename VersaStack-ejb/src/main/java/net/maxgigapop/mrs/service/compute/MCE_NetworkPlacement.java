/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
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
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;

/* TODO
Fix the fact that a subnet's IP needs to be specify by the spa model
*/

/**
 *
 * @author max
 */
@Stateless
public class MCE_NetworkPlacement implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_VMFilterPlacement.class.getName());

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(ModelBase systemModel, ServiceDelta annotatedDelta) {
        // $$ MCE_VMFilterPlacement deals with add model only for now.
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw new EJBException(String.format("%s::process ", this.getClass().getName()));
        }
        try {
            log.log(Level.INFO, "\n>>>MCE_NetworkPlacement--DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        // importPolicyData
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "SELECT ?network ?policy ?data ?dataType ?dataValue WHERE {"
                + "?network a nml:Topology ."
                + "?network spa:dependOn ?policy . "
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_VMFilterPlacement'. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?dataType. "
                + "?data spa:value ?dataValue. "
                + "FILTER (not exists {?policy spa:dependOn ?other}) "
                + "}";
        Map<Resource, List> networkPolicyMap = new HashMap<>();
        Query query = QueryFactory.create(sparqlString);
        QueryExecution qexec = QueryExecutionFactory.create(query, annotatedDelta.getModelAddition().getOntModel());
        ResultSet r = (ResultSet) qexec.execSelect();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resNetwork = querySolution.get("network").asResource();
            if (!networkPolicyMap.containsKey(resNetwork)) {
                List policyList = new ArrayList<>();
                networkPolicyMap.put(resNetwork, policyList);
            }
            Resource resPolicy = querySolution.get("policy").asResource();
            Resource resData = querySolution.get("data").asResource();
            RDFNode networkDataType = querySolution.get("dataType");
            RDFNode networkDataValue = querySolution.get("dataValue");
            Map policyData = new HashMap<>();
            policyData.put("policy", resPolicy);
            policyData.put("data", resData);
            policyData.put("type", networkDataType.toString());
            policyData.put("value", networkDataValue.toString());
            networkPolicyMap.get(resNetwork).add(policyData);
        }

        ServiceDelta outputDelta = annotatedDelta.clone();

        for (Resource network : networkPolicyMap.keySet()) {
            //1. compute placement based on filter/match criteria *policyData*
            // returned placementModel contains the VM as well as hosting Node/Topology and HypervisorService from systemModel
            //$$ TODO: virtual node should be named and tagged using URI and/or polocy/criteria data in spaModel  
            OntModel placementModel = this.doPlacement(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), network, networkPolicyMap.get(network));
            if (placementModel == null) {
                throw new EJBException(String.format("%s::process cannot resolve any policy to place %s", this.getClass().getName(), network));
            }

            //2. merge the placement satements into spaModel
            outputDelta.getModelAddition().getOntModel().add(placementModel.getBaseModel());
            /*
             try {
             log.log(Level.FINE, "\n>>>MCE_VMFilterPlacement--outputDelta(stage 2)=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
             } catch (Exception ex) {
             Logger.getLogger(MCE_VMFilterPlacement.class.getName()).log(Level.SEVERE, null, ex);
             }
             */
            //3. update policyData this action exportTo 
            this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), network);

            //4. remove policy and all related SPA statements receursively under vm from spaModel
            //   and also remove all statements that say dependOn this 'policy'
            MCETools.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), network);

            //$$ TODO: change VM URI (and all other virtual resources) into a unique string either during compile or in stitching action
            //$$ TODO: Add dependOn->Abstraction annotation to root level spaModel and add a generic Action to remvoe that abstract nml:Topology
        }
        try {
            log.log(Level.FINE, "\n>>>MCE_NetworkPlacement--outputDelta Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_VMFilterPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new AsyncResult(outputDelta);
    }

    //?? Use current containing abstract Topology ?
    // ignore if dependOn 'Abstraction'
    private OntModel doPlacement(OntModel systemModel, OntModel spaModel, Resource resNetwork, List<Map> placementCriteria) {
        OntModel placementModel = null;
        for (Map filterCriterion : placementCriteria) {
            if (!filterCriterion.containsKey("data") || !filterCriterion.containsKey("type") || !filterCriterion.containsKey("value")) {
                continue;
            }
            OntModel hostModel = filterTopologyNode(systemModel, resNetwork, filterCriterion);
            if (hostModel == null) {
                throw new EJBException(String.format("%s::process cannot place %s based on polocy %s", this.getClass().getName(), resNetwork, filterCriterion.get("policy")));
            }
            //$$ create VM resource and relation
            //$$ assemble placementModel;
            if (placementModel == null) {
                placementModel = hostModel;
            } else {
                placementModel.add(hostModel.getBaseModel());
            }

            // ? place to a specific Node ?
            //$$ Other types of filter methods have yet to be implemented.
        }
        return placementModel;
    }

    /**
     * *******************************************
     * Create a model depending on the type of criteria
     *
     * @param systemModel
     * @param resNetwork
     * @param filterCriterion
     * @return
     */
    private OntModel filterTopologyNode(OntModel systemModel, Resource resNetwork, Map filterCriterion) {
        OntModel networkModel = null;

        //get main topologyUri
        String topologyUri = resNetwork.toString().substring(0, resNetwork.toString().lastIndexOf(':'));

        if (((String) filterCriterion.get("type")).equalsIgnoreCase(Nml.Topology.getURI())) {
            networkModel = modelDefaultNetwork(systemModel, resNetwork, topologyUri);
        } else if (((String) filterCriterion.get("type")).equalsIgnoreCase(Mrs.SwitchingSubnet.getURI())) {

        }
        return networkModel;
    }

    /**
     * *************************************
     * Create a default network with all the elements and services needed
     *
     ****************************************
     */
    private OntModel modelDefaultNetwork(OntModel systemModel, Resource resNetwork, String topologyUri) {
        OntModel networkModel = null;

        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology ?virtualCloudService WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasService ?virtualCloudService ."
                + "?virtualCloudService a nml:VirtualCloudService ."
                + String.format("FILTER (?topology = <%s>)}", topologyUri);
        Query query = QueryFactory.create(sparqlString);
        QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if (!r.hasNext()) {
            throw new EJBException(String.format("%s::could not find a virtual cloud service in the "
                    + "system model associated with  %s", this.getClass().getName(), topologyUri));
        }
        QuerySolution querySolution = r.next();
        Resource resTopology = querySolution.get("topology").asResource();
        Resource resVirtualCloudService = querySolution.get("VirtualCloudService").asResource();
        if (networkModel == null) {
            networkModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        }

        //get basic network elements and relations
        networkModel.add(resTopology, RdfOwl.type, Nml.Topology);
        networkModel.add(resTopology, Nml.hasService, resVirtualCloudService);
        networkModel.add(resTopology, Nml.hasTopology, resNetwork);
        networkModel.add(resVirtualCloudService, Mrs.providesVPC, resNetwork);
        networkModel.add(resVirtualCloudService, RdfOwl.type, Mrs.VirtualCloudService);
        networkModel.add(resNetwork, RdfOwl.type, Nml.Topology);
        Resource switchingService = networkModel.createResource(resNetwork.toString() + "switchingService");
        Resource routingService = networkModel.createResource(resNetwork.toString() + "routingService");
        networkModel.add(resNetwork, Nml.hasService, switchingService);
        networkModel.add(resNetwork, RdfOwl.type, Mrs.SwitchingSubnet);
        networkModel.add(resNetwork, Nml.hasService, routingService);
        networkModel.add(resNetwork, RdfOwl.type, Mrs.RoutingService);

        sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?%s ?value WHERE {"
                + "?topology a nml:Topology ."
                + "?topology mrs:hasNetworkAddress ?address ."
                + "?address mrs:type \"ipv4-prefix\" ."
                + "?address mrs:value ?value ."
                + String.format("FILTER (?topology = <%s>)}", resNetwork.toString());

        String value = "";

        query = QueryFactory.create(sparqlString);
        qexec = QueryExecutionFactory.create(query, systemModel);
        r = (ResultSet) qexec.execSelect();

        if (!r.hasNext()) { //means that vpc does not have a network address nor route 
            querySolution = r.next();
            value = querySolution.get("VirtualCloudService").asLiteral().toString();
        } else {
            value = "10.0.0.0/16";
        }

        //add routing info
        Resource networkAddress = networkModel.createResource(resNetwork.toString() + "networkAddress");
        networkModel.add(resNetwork, Mrs.hasNetworkAddress, networkAddress);
        networkModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
        networkModel.add(networkAddress, Mrs.type, "ipv4-prefix");
        networkModel.add(networkAddress, Mrs.value, value);
        Resource routingTable = networkModel.createResource(topologyUri + "rtb-" + Integer.toString(resNetwork.hashCode()));
        Resource route = networkModel.createResource(topologyUri + "rtb-" + Integer.toString(resNetwork.hashCode()) + value.replace("/", ""));
        networkModel.add(routingService, Mrs.providesRoutingTable, routingTable);
        networkModel.add(routingService, Mrs.providesRoute, route);
        networkModel.add(routingTable, RdfOwl.type, Mrs.RoutingTable);
        networkModel.add(routingTable, Mrs.hasRoute, route);
        networkModel.add(routingTable, Mrs.type, "main");
        networkModel.add(route, RdfOwl.type, Mrs.Route);
        networkModel.add(route, Mrs.nextHop, "local");
        networkModel.add(route, Mrs.routeTo, networkAddress);

        return networkModel;
    }

    /**
     * ********************************************************
     * Method to add subnet to model if specified
     *
     * @param spaModel
     * @param vm 
     *********************************************************
     */
    private OntModel addSubnetToModel(OntModel systemModel, OntModel networkModel, Resource resNetwork, String value) {
        if (networkModel == null) {
            networkModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        }
        
        
        //get the newtork switching service
        Resource switchingService =networkModel.getResource(resNetwork.toString() + "switchingService");
        if(switchingService == null){
            networkModel.add(switchingService,RdfOwl.type,Mrs.SwitchingService);
        }
        
        Resource subnet = networkModel.createResource(value);
        networkModel.add(subnet,RdfOwl.type,Mrs.SwitchingSubnet);
        networkModel.add(switchingService,Mrs.providesSubnet,subnet);
        
        //TODO find a way to make Ips be abstract instead of specified
        //assing the address of the subnet to the network
        String sparql = "SELECT ?ip WHERE  {?a a spa:PolicyData ."
                        + "?a spa:type mrs:SwitchingSubnet ."
                        + String.format("?a spa:value <%s> .",value)
                        + "?a mrs:value ?ip}";
        Query query = QueryFactory.create(sparql);
        QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if (!r.hasNext()) {
            throw new EJBException(String.format("%s::could not find and ip for subnet"
                    + " %s", this.getClass().getName(), subnet));
        }
        QuerySolution q = r.next();
        String ip = q.get("ip").asLiteral().toString();
        
        //add subnet address to model
        Resource networkAddress = networkModel.createResource(subnet.toString() + "networkAddress");
        networkModel.add(subnet,Mrs.hasNetworkAddress,networkAddress);
        networkModel.add(networkAddress,RdfOwl.type,Mrs.NetworkAddress);
        networkModel.add(networkAddress,Mrs.type,"ipv4-prefix");
        networkModel.add(networkAddress,Mrs.type,ip);
        
        return networkModel;
    }
    

    private void exportPolicyData(OntModel spaModel, Resource vm) {
        // find Placement policy -> exportTo -> policyData
        String sparql = "SELECT ?nodeorvpc ?policyAction ?policyData WHERE {"
                + "?nodeorvpc nml:hasService ?hvservice . "
                + "?hvservice a mrs:HypervisorService . "
                + String.format("?hvservice mrs:providesVM <%s> .", vm.getURI())
                + String.format("<%s> spa:dependOn ?policyAction .", vm.getURI())
                + "?policyAction a spa:PolicyAction. "
                + "?policyAction spa:type 'MCE_VMFilterPlacement'. "
                + "?policyAction spa:exportTo ?policyData . "
                + "?policyData a spa:PolicyData . "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resHost = querySolution.get("nodeorvpc").asResource();
            Resource resPolicy = querySolution.get("policyAction").asResource();
            Resource resData = querySolution.get("policyData").asResource();
            // add export data
            /*
             if (spaModel.listStatements(resHost, RdfOwl.type, Nml.Topology).hasNext()) {
             spaModel.add(resData, Spa.type, Nml.Topology);
             } else if (spaModel.listStatements(resHost, RdfOwl.type, Nml.Node).hasNext()) {
             spaModel.add(resData, Spa.type, Nml.Node);
             }
             */
            spaModel.add(resData, Spa.type, "VMFilterPlacement:HostSite");
            spaModel.add(resData, Spa.value, resHost);
            // remove Placement->exportTo statement so the exportData can be kept in spaModel during receurive removal
            //spaModel.remove(resPolicy, Spa.exportTo, resData);
        }
    }

    //@TODO: matchingNetwork (VPC or TenantNetwork)
    //@TODO: matchingSunbet
    //$$ regExURIFilter
    //$$ hostCapabilityFilter(s)
    //$$ placeMatchingRegExURI
    //$$ placeWithMultiFilter
}
