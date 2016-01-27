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
import java.net.InetAddress;
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
            log.log(Level.FINE, "\n>>>MCE_NetworkPlacement--DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
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
                + "?policy spa:type 'MCE_NetworkPlacement'. "
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
            OntModel hostModel = this.filterTopologyNode(systemModel, spaModel, resNetwork, filterCriterion);
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
        System.out.println(placementModel);
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
    private OntModel filterTopologyNode(OntModel systemModel, OntModel spaModel, Resource resNetwork, Map filterCriterion) {
        OntModel networkModel = null;

        if (((String) filterCriterion.get("type")).equalsIgnoreCase(Nml.Topology.getURI())) {
            networkModel = modelDefaultNetwork(systemModel, spaModel, resNetwork, (String) filterCriterion.get("value"));
        } else if (((String) filterCriterion.get("type")).equalsIgnoreCase(Mrs.SwitchingSubnet.getURI())) {
            networkModel = addSubnetToModel(systemModel, spaModel, resNetwork, (Resource) filterCriterion.get("data"));
        } else if (((String) filterCriterion.get("type")).equalsIgnoreCase(Nml.BidirectionalPort.getURI())) {
            networkModel = addGatewayToModel(systemModel, spaModel, resNetwork, (Resource) filterCriterion.get("data"));
        }
        return networkModel;
    }

    /**
     * *************************************
     * Create a default network with all the elements and services needed
     *
     ****************************************
     */
    private OntModel modelDefaultNetwork(OntModel systemModel, OntModel spaModel, Resource resNetwork, String topologyUri) {
        OntModel networkModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        //add to model type of resource 
        //check if resource does not exists already
        String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "SELECT ?network WHERE  {?network a nml:Topology ."
                + String.format("FILTER (?network = <%s>)}", resNetwork.asResource().toString());
        Query query = QueryFactory.create(sparql);
        QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if (r.hasNext()) {
            return networkModel; //return just what we have which is basically what we have in the model
        }

        sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology ?virtualCloudService WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasService ?virtualCloudService ."
                + "?virtualCloudService a mrs:VirtualCloudService ."
                + String.format("FILTER (?topology = <%s>)}", topologyUri);
        query = QueryFactory.create(sparql);
        qexec = QueryExecutionFactory.create(query, systemModel);
        r = (ResultSet) qexec.execSelect();
        if (!r.hasNext()) {
            throw new EJBException(String.format("%s::could not find a virtual cloud service in the "
                    + "system model associated with  %s", this.getClass().getName(), topologyUri));
        }
        QuerySolution querySolution = r.next();
        Resource resTopology = querySolution.get("topology").asResource();
        Resource resVirtualCloudService = querySolution.get("virtualCloudService").asResource();

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
        networkModel.add(switchingService, RdfOwl.type, Mrs.SwitchingService);
        networkModel.add(resNetwork, Nml.hasService, routingService);
        networkModel.add(routingService, RdfOwl.type, Mrs.RoutingService);

        sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?value WHERE {"
                + "?topology a nml:Topology ."
                + "?topology mrs:hasNetworkAddress ?address ."
                + "?address mrs:type \"ipv4-prefix\" ."
                + "?address mrs:value ?value ."
                + String.format("FILTER (?topology = <%s>)}", resNetwork.toString());

        String value = "";

        query = QueryFactory.create(sparql);
        qexec = QueryExecutionFactory.create(query, spaModel);
        r = (ResultSet) qexec.execSelect();

        if (r.hasNext()) { //means that vpc does not have a network address nor route 
            querySolution = r.next();
            value = querySolution.get("value").asLiteral().toString();
        } else {
            value = "10.0.0.0/16";
        }

        //add routing info
        Resource networkAddress = networkModel.createResource(resNetwork.toString() + "networkAddress");
        networkModel.add(resNetwork, Mrs.hasNetworkAddress, networkAddress);
        networkModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
        networkModel.add(networkAddress, Mrs.type, "ipv4-prefix");
        networkModel.add(networkAddress, Mrs.value, value);
        Resource routingTable = networkModel.createResource(resNetwork + "rtb-" + Integer.toString(resNetwork.hashCode()));
        Resource route = networkModel.createResource(resNetwork + "rtb-" + Integer.toString(resNetwork.hashCode()) + value.replace("/", ""));
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
     * @param vm ********************************************************
     */
    private OntModel addSubnetToModel(OntModel systemModel, OntModel spaModel, Resource resNetwork, Resource resData) {
        OntModel networkModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?value WHERE {"
                + "?topology a nml:Topology ."
                + "?topology mrs:hasNetworkAddress ?address ."
                + "?address mrs:type \"ipv4-prefix\" ."
                + "?address mrs:value ?value ."
                + String.format("FILTER (?topology = <%s>)}", resNetwork.toString());

        String vpcIp = "";

        Query query = QueryFactory.create(sparql);
        QueryExecution qexec = QueryExecutionFactory.create(query, spaModel);
        ResultSet r = (ResultSet) qexec.execSelect();

        if (r.hasNext()) { //means that vpc does not have a network address nor route 
            QuerySolution querySolution = r.next();
            vpcIp = querySolution.get("value").asLiteral().toString();
        } else {
            vpcIp = "10.0.0.0/16";
        }

        //TODO Ip validation
        //assing the address of the subnet to the network
        sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + String.format("SELECT ?ip WHERE  {<%s> spa:value ?ip}", resData);
        query = QueryFactory.create(sparql);
        qexec = QueryExecutionFactory.create(query, spaModel);
        r = (ResultSet) qexec.execSelect();
        if (!r.hasNext()) { //no ip was specify create a subnet with the first ip subnet available
            //TODO add capability to ad at least one subnet if ip not specified
            throw new EJBException(String.format("%s::could not find spa:value in"
                    + " %s", this.getClass().getName(), resData));
        }

        //start modeling each of the subnets 
        int i = 0;
        Resource switchingService = networkModel.createResource(resNetwork.toString() + "switchingService");
        networkModel.add(switchingService, RdfOwl.type, Mrs.SwitchingService);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            String ip = q.get("ip").asLiteral().toString();
            //TODO carefull not doing any checks in ips correctnes, needs to implement this

            //add subnet address to model
            Resource subnet = networkModel.createResource(resNetwork.toString() + "-subnet" + Integer.toString(i));
            networkModel.add(subnet, RdfOwl.type, Mrs.SwitchingSubnet);
            networkModel.add(switchingService, Mrs.providesSubnet, subnet);
            Resource networkAddress = networkModel.createResource(subnet.toString() + "networkAddress");
            networkModel.add(subnet, Mrs.hasNetworkAddress, networkAddress);
            networkModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
            networkModel.add(networkAddress, Mrs.type, "ipv4-prefix");
            networkModel.add(networkAddress, Mrs.value, ip);
            i++;
        }

        return networkModel;
    }

    /**
     * ********************************************************
     * Method to add a gateway to VPC if specified, this Gateway could be either
     * an internet gateway vpc or a VPN gateway
     *
     * @param spaModel
     * @param vm *******************************************************
     */
    private OntModel addGatewayToModel(OntModel systemModel, OntModel spaModel, Resource resNetwork, Resource resData) {
        OntModel networkModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        //find whihc type of gateway is 
        String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + String.format("SELECT ?type WHERE  {<%s> spa:value ?type}", resData);
        Query query = QueryFactory.create(sparql);
        QueryExecution qexec = QueryExecutionFactory.create(query, spaModel);
        ResultSet r = (ResultSet) qexec.execSelect();

        //assume is an internet gateway if type is not included
        String type = "";
        Resource tag;
        String topoUri = resNetwork.toString().substring(0, resNetwork.toString().lastIndexOf(":"));
        if (!r.hasNext()) {
            type = "internet";
            Resource gateway = networkModel.createResource(resNetwork.toString() + "-gateway" + type);
            networkModel.add(gateway, RdfOwl.type, Nml.BidirectionalPort);
            networkModel.add(resNetwork, Nml.hasBidirectionalPort, gateway);
            tag = networkModel.createResource(topoUri + "igwTag");
            networkModel.add(gateway, Mrs.hasTag, tag);
        } else {
            while (r.hasNext()) {
                //add basic statements of any gateway
                QuerySolution q = r.next();
                type = q.get("type").asLiteral().toString().toLowerCase();
                Resource gateway = networkModel.createResource(resNetwork.toString() + "-gateway" + type);
                networkModel.add(gateway, RdfOwl.type, Nml.BidirectionalPort);
                networkModel.add(resNetwork, Nml.hasBidirectionalPort, gateway);

                if (type.equals("internet")) {
                    tag = networkModel.createResource(topoUri + ":igwTag");
                    networkModel.add(tag, RdfOwl.type, Mrs.Tag);
                    networkModel.add(tag, Mrs.type, "gateway");
                    networkModel.add(tag, Mrs.value, "internet");
                } else {
                    tag = networkModel.createResource(topoUri + ":vpngwTag");
                    networkModel.add(tag, RdfOwl.type, Mrs.Tag);
                    networkModel.add(tag, Mrs.type, "gateway");
                    networkModel.add(tag, Mrs.value, "vpn");
                }

                //Add the propert tag to the gateway
                networkModel.add(gateway, Mrs.hasTag, tag);
            }
        }
        return networkModel;
    }

    private void exportPolicyData(OntModel spaModel, Resource resNetwork) {
        // find Placement policy -> exportTo -> policyData for vpc
        String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "SELECT ?vpc ?policyAction ?policyData ?type ?value WHERE {"
                + "?vpc nml:hasService ?vservice . "
                + "?vservice a mrs:VirtualCloudService . "
                + String.format("?vservice mrs:providesVPC <%s> .", resNetwork.toString())
                + String.format("<%s> spa:dependOn ?policyAction .", resNetwork.toString())
                + "?policyAction a spa:PolicyAction. "
                + "?policyAction spa:type 'MCE_NetworkPlacement' ."
                + "?policyAction spa:exportTo ?policyData . "
                + "?policyData a spa:PolicyData . "
                + "?policyData spa:type ?type}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resPolicy = querySolution.get("policyAction").asResource();
            Resource resData = querySolution.get("policyData").asResource();
            Resource type = querySolution.get("type").asResource();
            //look at type of the policyData to know what value to export
            //TODO check if resNetwork shoudl be a resource or string
            if (type.toString().equalsIgnoreCase(Nml.Topology.toString())) {
                spaModel.add(resData, Spa.value, resNetwork.toString());
            } //export gateway results
            //TODO what happens if policyAction did not create a gateway
            //TODO how to differencinate between gateway types
            else if (type.toString().equalsIgnoreCase(Nml.BidirectionalPort.toString())) {
                sparql = String.format("SELECT ?gateway ?value WHERE {<%s> nml:hasBidirectionalPort ?gateway .", resNetwork.toString())
                        + "?gateway mrs:hasTag ?tag ."
                        + "?tag mrs:value ?value}";
                r = ModelUtil.sparqlQuery(spaModel, sparql);
                if(r.hasNext()) {
                    QuerySolution q1 = r.next();
                    Literal value = q1.get("value").asLiteral();
                    Resource gateway = q1.getResource("gateway");
                    spaModel.add(resData, Spa.value, gateway);
                }
            } //export subnet results
            //TODO what happens if policyAction did not create a subnet
            else if (type.toString().equalsIgnoreCase(Mrs.SwitchingSubnet.toString())) {
                sparql = String.format("SELECT ?subnet WHERE {<%s>  nml:hasService ?service .", resNetwork)
                        +"?service a nml:SwitchingService ."
                        + "?service mrs:providesSubnet ?subnet}";
                r = ModelUtil.sparqlQuery(spaModel, sparql);
                if (r.hasNext()) {
                    QuerySolution q1 = r.next();
                    Literal subnet = q1.get("value").asLiteral();
                    spaModel.add(resData, Spa.value, subnet);
                }
            }
        }
        try {
            String ttl = ModelUtil.marshalOntModel(spaModel);
            System.out.println();
        } catch (Exception ex) {
            Logger.getLogger(MCE_NetworkPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
