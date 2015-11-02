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
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author max
 */
@Stateless
public class MCE_VirtualNetworkCreation implements IModelComputationElement {

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
                + "?policy spa:type 'MCE_VirtualNetworkCreation'. "
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
            OntModel placementModel = this.doCreation(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), network, networkPolicyMap.get(network));
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
    private OntModel doCreation(OntModel systemModel, OntModel spaModel, Resource resNetwork, List<Map> placementCriteria) {
        OntModel placementModel = null;
        for (Map filterCriterion : placementCriteria) {
            if (!filterCriterion.containsKey("data") || !filterCriterion.containsKey("type") || !filterCriterion.containsKey("value")) {
                continue;
            }
            OntModel hostModel = this.modelNetwork(systemModel, spaModel, resNetwork, filterCriterion.get("value").toString());
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
        }
        System.out.println(placementModel);
        return placementModel;
    }

    private OntModel modelNetwork(OntModel systemModel, OntModel spaModel, Resource resNetwork, String value) {
        //the String value will be a JSON Object 
        JSONParser parser = new JSONParser();
        Object obj = new Object();
        try {
            obj = parser.parse(value);
        } catch (ParseException e) {
            throw new EJBException(String.format("%s::process  cannot parse json string %s", this.getClass().getName(), value));
        }
        JSONObject topoDescription = (JSONObject) obj;

        //1 get all the info in the array that matters for this MCE
        //1.1 get topology info it should be Stirngs
        String type = (String) topoDescription.get("type");
        String networkCIDR = (String) topoDescription.get("cidr");
        String topologyUri = (String) topoDescription.get("parent");
        //1.2 get the subnets , gateways and routes they should be JSONArrays
        JSONArray subnets = (JSONArray) topoDescription.get("subnets");
        JSONArray gateways = (JSONArray) topoDescription.get("gateways");
        JSONArray routes = (JSONArray) topoDescription.get("routes");

        //2 create the basic topology model
        //2.1 check for type and networkCIDR
        if (type == null || !type.equalsIgnoreCase("tenant") || !type.equalsIgnoreCase("external")) {
            type = "tenant";
        }
        if (networkCIDR == null) {
            networkCIDR = "10.0.0.0/16";
        }
        if (topologyUri == null) {
            throw new EJBException(String.format("%s::process network %s does not have a parent topology", this.getClass().getName(), value));
        }

        //2.3 check if it is openstack or AWS
        boolean aws = false;
        boolean ops = false;
        String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasService ?hypervisorService ."
                + "?hypervisorService a mrs:HypervisorService ."
                + String.format("FILTER (?topology = <%s>)}", topologyUri);
        Query query = QueryFactory.create(sparql);
        QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if (r.hasNext())//this is aws
        {
            aws = true;
        } else {
            ops = true;
        }
        spaModel = modelDefaultNetwork(systemModel, spaModel,ops, resNetwork, type, networkCIDR, topologyUri, routes);
        if (aws == true) {
            spaModel = modelGateways(systemModel, spaModel, resNetwork, gateways, topologyUri);
            spaModel = modelAwsSubnets(systemModel, spaModel, resNetwork, networkCIDR, type, topologyUri, subnets);
        } else if (ops = true) {
            spaModel = modelOpsSubnets(systemModel, spaModel, resNetwork, networkCIDR, type, topologyUri, subnets);
        }

        return spaModel;
    }

    /**
     * *************************************
     * Create a default network with all the elements and services needed
     *
     ****************************************
     */
    private OntModel modelDefaultNetwork(OntModel systemModel, OntModel spaModel,boolean ops, Resource resNetwork, String type, String networkCIDR, String topologyUri, JSONArray routes) {
        String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology ?virtualCloudService WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasService ?virtualCloudService ."
                + "?virtualCloudService a mrs:VirtualCloudService ."
                + String.format("FILTER (?topology = <%s>)}", topologyUri);
        Query query = QueryFactory.create(sparql);
        QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if (!r.hasNext()) {
            throw new EJBException(String.format("%s::could not find a virtual cloud service in the "
                    + "system model associated with  %s", this.getClass().getName(), topologyUri));
        }
        QuerySolution querySolution = r.next();
        Resource resTopology = querySolution.get("topology").asResource();
        Resource resVirtualCloudService = querySolution.get("virtualCloudService").asResource();

        //get basic network elements and relations
        spaModel.add(resTopology, RdfOwl.type, Nml.Topology);
        spaModel.add(resTopology, Nml.hasService, resVirtualCloudService);
        spaModel.add(resTopology, Nml.hasTopology, resNetwork);
        spaModel.add(resVirtualCloudService, Mrs.providesVPC, resNetwork);
        spaModel.add(resVirtualCloudService, RdfOwl.type, Mrs.VirtualCloudService);
        spaModel.add(resNetwork, RdfOwl.type, Nml.Topology);
        Resource switchingService = spaModel.createResource(resNetwork.toString() + "switchingService");
        Resource routingService = spaModel.createResource(resNetwork.toString() + "routingService");
        spaModel.add(resNetwork, Nml.hasService, switchingService);
        spaModel.add(switchingService, RdfOwl.type, Mrs.SwitchingService);
        spaModel.add(resNetwork, Nml.hasService, routingService);
        spaModel.add(routingService, RdfOwl.type, Mrs.RoutingService);

        //add network type to the model
        Resource networkTag = spaModel.getResource(topologyUri + ":network_tag_" + type);
        spaModel.add(networkTag, RdfOwl.type, Mrs.Tag);
        spaModel.add(networkTag, Mrs.type, "network-type");
        spaModel.add(networkTag, Mrs.value, type);
        spaModel.add(resNetwork, Mrs.hasTag, networkTag);

        //add routing info
        Resource networkAddress = spaModel.createResource(resNetwork.toString() + "networkAddress");
        spaModel.add(resNetwork, Mrs.hasNetworkAddress, networkAddress);
        spaModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
        spaModel.add(networkAddress, Mrs.type, "ipv4-prefix");
        spaModel.add(networkAddress, Mrs.value, networkCIDR);
        Resource routingTable = spaModel.createResource(resNetwork + "rtb-" + Integer.toString(resNetwork.hashCode()));
        Resource route = spaModel.createResource(resNetwork + "rtb-" + Integer.toString(resNetwork.hashCode()) + networkCIDR.replace("/", ""));
        spaModel.add(routingService, Mrs.providesRoutingTable, routingTable);
        spaModel.add(routingService, Mrs.providesRoute, route);
        spaModel.add(routingTable, RdfOwl.type, Mrs.RoutingTable);
        spaModel.add(routingTable, Mrs.hasRoute, route);
        spaModel.add(routingTable, Mrs.type, "main");
        spaModel.add(route, RdfOwl.type, Mrs.Route);
        spaModel.add(route, Mrs.nextHop, "local");
        spaModel.add(route, Mrs.routeTo, networkAddress);

        //add the routes to the main routing table
        if (routes != null && !ops ==true) { //openStack does not have this
            ListIterator routesIt = routes.listIterator();
            while (routesIt.hasNext()) {
                JSONObject o = (JSONObject) routesIt.next();
                String to = (String) o.get("to");
                String next = (String) o.get("nextHop");
                String from = (String) o.get("from");

                //accomodate for different kinds of nextHops
                if (next.equalsIgnoreCase("local")) {
                    next = next; //basically do nothing it is local
                } else if (next.equalsIgnoreCase("internet")) {
                    next = resNetwork.toString() + "-igw"; //the network internet gateway
                } else if (next.equalsIgnoreCase("vpn")) {
                    next = resNetwork.toString() + "-vpngw"; //the network vpn gateway
                } else {
                    next = topologyUri + ":" + next; //a radom resource
                }

                //in case we want to propagate from a vpn on the main route table
                route = spaModel.createResource(routingTable.toString() + "route" + to.replace("/", ""));
                if (from != null && from.equalsIgnoreCase("vpn")) {
                    from = resNetwork.toString() + "-vpngw"; //the network vpn gateway
                    Resource routeFrom = spaModel.getResource(from);
                    spaModel.add(route, Mrs.routeFrom, routeFrom);
                }

                //add modeling
                Resource routeTo = spaModel.createResource(routingTable.toString() + "-" + "routeto" + to.replace("/", ""));
                spaModel.add(routeTo, RdfOwl.type, Mrs.NetworkAddress);
                spaModel.add(routeTo, Mrs.type, "ipv4-prefix-list");
                spaModel.add(routeTo, Mrs.value, to);
                spaModel.add(route, RdfOwl.type, Mrs.Route);
                spaModel.add(route, Mrs.routeTo, routeTo);
                if (next.equalsIgnoreCase("local")) {
                    spaModel.add(route, Mrs.nextHop, next);
                } else {
                    Resource nextHop = spaModel.createResource(next);
                    spaModel.add(route, Mrs.nextHop, nextHop);
                }
                spaModel.add(routingTable, Mrs.hasRoute, route);
                spaModel.add(routingService, Mrs.providesRoute, route);

            }
        }

        return spaModel;
    }

    /**
     * ********************************************************
     * Method to add subnet to AWS model if specified
     *
     * @param systemModel
     * @param spaModel
     * @param resNetwork
     * @param networkCIDR
     * @param networkType
     * @param topologyUri
     * @param subnets
     * @param vm *******************************************************
     */
    private OntModel modelAwsSubnets(OntModel systemModel, OntModel spaModel, Resource resNetwork, String networkCIDR, String networkType, String topologyUri, JSONArray subnets) {
        //return if no subnets specified
        if (subnets == null) {
            return spaModel;
        }

        //add basic service for subnets
        Resource switchingService = spaModel.createResource(resNetwork.toString() + "switchingService");
        spaModel.add(switchingService, RdfOwl.type, Mrs.SwitchingService);

        int i = 0;
        ListIterator subnetsIt = subnets.listIterator();
        while (subnetsIt.hasNext()) {
            JSONObject o = (JSONObject) subnetsIt.next();
            if (!o.containsKey("cidr")) {
                throw new EJBException(String.format("%s::process JSON subnet does not have key cidr", this.getClass().getName()));
            }
            String subnetCIDR = (String) o.get("cidr");

            //check for validity of subnet cidr
            String subnetUri = resNetwork.toString() + "-subnet" + Integer.toString(i);
            String subnetName = subnetUri.substring(subnetUri.lastIndexOf(":") + 1);
            Resource subnet = spaModel.createResource(subnetUri);
            spaModel.add(subnet, RdfOwl.type, Mrs.SwitchingSubnet);
            spaModel.add(switchingService, Mrs.providesSubnet, subnet);
            Resource networkAddress = spaModel.createResource(subnet.toString() + "networkAddress");
            spaModel.add(subnet, Mrs.hasNetworkAddress, networkAddress);
            spaModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
            spaModel.add(networkAddress, Mrs.type, "ipv4-prefix");
            spaModel.add(networkAddress, Mrs.value, subnetCIDR);

            //add the subnet tag
            if (networkType.equalsIgnoreCase("external")) {
                networkType = "public";
            } else {
                networkType = "private";
            }
            Resource subnetTag = spaModel.getResource(topologyUri + ":subnet_tag_" + networkType);
            spaModel.add(subnetTag, RdfOwl.type, Mrs.Tag);
            spaModel.add(subnetTag, Mrs.type, "network-type");
            spaModel.add(subnetTag, Mrs.value, networkType);
            spaModel.add(subnet, Mrs.hasTag, subnetTag);

            if (o.containsKey("routes")) {
                Resource routingService = spaModel.getResource(resNetwork.toString() + "routingService");
                Resource routingTable = spaModel.createResource(resNetwork + "rtb-" + "subnet" + Integer.toString(i));
                spaModel.add(routingTable, RdfOwl.type, Mrs.RoutingTable);
                spaModel.add(routingTable, Mrs.type, "local");
                spaModel.add(routingService, Mrs.providesRoutingTable, routingTable);

                //add basic modeling for routing Table
                Resource routeTo = spaModel.createResource(routingTable.toString() + "-" + "routeto" + networkCIDR.replace("/", ""));
                spaModel.add(routeTo, RdfOwl.type, Mrs.NetworkAddress);
                spaModel.add(routeTo, Mrs.type, "ipv4-prefix-list");
                spaModel.add(routeTo, Mrs.value, networkCIDR);
                Resource route = spaModel.createResource(routingTable.toString() + "route" + networkCIDR.replace("/", ""));
                spaModel.add(route, RdfOwl.type, Mrs.Route);
                spaModel.add(route, Mrs.routeTo, routeTo);
                spaModel.add(route, Mrs.nextHop, "local");
                spaModel.add(route, Mrs.routeFrom, subnet);
                spaModel.add(routingTable, Mrs.hasRoute, route);
                spaModel.add(routingService, Mrs.providesRoute, route);

                JSONArray routes = (JSONArray) o.get("routes");
                ListIterator routesIt = routes.listIterator();
                while (routesIt.hasNext()) {
                    JSONObject routeObj = (JSONObject) routesIt.next();
                    if (!routeObj.containsKey("to") || !routeObj.containsKey("nextHop")) {
                        throw new EJBException(String.format("%s::process JSON subnet %s does not have proper routes "
                                + "formed", this.getClass().getName(), subnet));
                    }
                    String to = (String) routeObj.get("to");
                    String next = (String) routeObj.get("nextHop");
                    String from = (String) routeObj.get("from");

                    //accomodate for different kinds of nextHops
                    if (next.equalsIgnoreCase("local")) {
                        next = next; //basically do nothing it is local
                    } else if (next.equalsIgnoreCase("internet")) {
                        next = resNetwork.toString() + "-igw"; //the network internet gateway
                    } else if (next.equalsIgnoreCase("vpn")) {
                        next = resNetwork.toString() + "-vpngw"; //the network vpn gateway
                    } else {
                        next = topologyUri + ":" + next; //a radom resource
                    }

                    //add modeling
                    routeTo = spaModel.createResource(routingTable.toString() + "-" + "routeto" + to.replace("/", ""));
                    spaModel.add(routeTo, RdfOwl.type, Mrs.NetworkAddress);
                    spaModel.add(routeTo, Mrs.type, "ipv4-prefix-list");
                    spaModel.add(routeTo, Mrs.value, to);
                    route = spaModel.createResource(routingTable.toString() + "route" + to.replace("/", ""));
                    spaModel.add(route, RdfOwl.type, Mrs.Route);
                    spaModel.add(route, Mrs.routeTo, routeTo);
                    if (next.equalsIgnoreCase("local")) {
                        spaModel.add(route, Mrs.nextHop, next);
                    } else {
                        Resource nextHop = spaModel.createResource(next);
                        spaModel.add(route, Mrs.nextHop, nextHop);
                    }
                    //decide id the routeFrom should be a VPN gateway or a subent

                    if (from != null && from.equalsIgnoreCase("vpn")) {
                        from = resNetwork.toString() + "-vpngw"; //the network vpn gateway
                        Resource routeFrom = spaModel.getResource(from);
                        spaModel.add(route, Mrs.routeFrom, routeFrom);
                    } else {
                        spaModel.add(route, Mrs.routeFrom, subnet);
                    }

                    spaModel.add(routingTable, Mrs.hasRoute, route);
                    spaModel.add(routingService, Mrs.providesRoute, route);

                }
            }
            i++;
        }
        return spaModel;
    }

    /**
     * ********************************************************
     * Method to add subnet to OpenStack model if specified
     *
     * @param systemModel
     * @param spaModel
     * @param resNetwork
     * @param networkCIDR
     * @param networkType
     * @param topologyUri
     * @param subnets
     * @param vm *******************************************************
     */
    private OntModel modelOpsSubnets(OntModel systemModel, OntModel spaModel, Resource resNetwork, String networkCIDR, String networkType, String topologyUri, JSONArray subnets) {
        //return if no subnets specified
        if (subnets == null) {
            return spaModel;
        }

        //add basic service for subnets
        Resource switchingService = spaModel.createResource(resNetwork.toString() + "switchingService");
        spaModel.add(switchingService, RdfOwl.type, Mrs.SwitchingService);

        int i = 0;
        ListIterator subnetsIt = subnets.listIterator();
        while (subnetsIt.hasNext()) {
            JSONObject o = (JSONObject) subnetsIt.next();
            if (!o.containsKey("cidr")) {
                throw new EJBException(String.format("%s::process JSON subnet does not have key cidr", this.getClass().getName()));
            }
            String subnetCIDR = (String) o.get("cidr");

            //check for validity of subnet cidr
            String subnetUri = resNetwork.toString() + "-subnet" + Integer.toString(i);
            String subnetName = subnetUri.substring(subnetUri.lastIndexOf(":") + 1);
            Resource subnet = spaModel.createResource(subnetUri);
            spaModel.add(subnet, RdfOwl.type, Mrs.SwitchingSubnet);
            spaModel.add(switchingService, Mrs.providesSubnet, subnet);
            Resource networkAddress = spaModel.createResource(subnet.toString() + "networkAddress");
            spaModel.add(subnet, Mrs.hasNetworkAddress, networkAddress);
            spaModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
            spaModel.add(networkAddress, Mrs.type, "ipv4-prefix");
            spaModel.add(networkAddress, Mrs.value, subnetCIDR);

            //add the subnet tag
            if (networkType.equalsIgnoreCase("external")) {
                networkType = "public";
            } else {
                networkType = "private";
            }
            Resource subnetTag = spaModel.getResource(topologyUri + ":subnet_tag_" + networkType);
            spaModel.add(subnetTag, RdfOwl.type, Mrs.Tag);
            spaModel.add(subnetTag, Mrs.type, "network-type");
            spaModel.add(subnetTag, Mrs.value, networkType);
            spaModel.add(subnet, Mrs.hasTag, subnetTag);

            //add basic routing for the subnet
            Resource routingService = spaModel.getResource(resNetwork.toString() + "routingService");
            Resource routingTable = spaModel.createResource(resNetwork + "rtb-" + Integer.toString(resNetwork.hashCode()));
            spaModel.add(routingService, Mrs.providesRoutingTable, routingTable);
            Resource route = spaModel.createResource(routingTable.toString() + "route" + subnet + "local-route");
            spaModel.add(routingService, Mrs.providesRoute, route);
            spaModel.add(routingTable, Mrs.hasRoute, route);
            spaModel.add(route, RdfOwl.type, Mrs.Route);
            spaModel.add(route, Mrs.routeFrom, subnet);
            spaModel.add(route, Mrs.routeTo, networkAddress);
            spaModel.add(route, Mrs.nextHop, "local");

            if (o.containsKey("routes")) {
                JSONArray routes = (JSONArray) o.get("routes");
                ListIterator routesIt = routes.listIterator();
                while (routesIt.hasNext()) {
                    JSONObject routeObj = (JSONObject) routesIt.next();
                    if (!routeObj.containsKey("to") || !routeObj.containsKey("nextHop")) {
                        throw new EJBException(String.format("%s::process JSON subnet %s does not have proper routes "
                                + "formed", this.getClass().getName(), subnet));
                    }
                    String to = (String) routeObj.get("to");
                    String next = (String) routeObj.get("nextHop");
                    String from = (String) routeObj.get("from");

                    if (next != null && to != null && next.contains("internet") && to.equalsIgnoreCase("0.0.0.0/0")) { //create routing to internet
                        //get the routing service for the whole openstack
                        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                                + "SELECT ?service WHERE {?topology nml:hasService ?service ."
                                + "?service a mrs:RoutingService ."
                                + String.format("FILTER (?topology = <%s>)", topologyUri)
                                + "}";
                        Query query = QueryFactory.create(sparqlString);
                        QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
                        ResultSet r = (ResultSet) qexec.execSelect();
                        if (!r.hasNext()) {
                            throw new EJBException(String.format("%s::main topology %s does not have a routing service ", this.getClass().getName(), topologyUri));
                        }
                        routingService = r.next().getResource("service").asResource();
                        routingTable = spaModel.createResource(resNetwork + "rtb-" + "subnet" + Integer.toString(i));
                        spaModel.add(routingTable, RdfOwl.type, Mrs.RoutingTable);
                        spaModel.add(routingService, Mrs.providesRoutingTable, routingTable);

                        //create the port on the subnet
                        Resource nextHopAddress = RdfOwl.createResource(spaModel, topologyUri + ":port-" + subnetName + to.replace("/", "") + i + "networkAddress", Mrs.NetworkAddress);
                        spaModel.add(nextHopAddress, Mrs.type, "ipv4-network-address");
                        spaModel.add(nextHopAddress, Mrs.value, "any");

                        //create route from subnet to router
                        route = RdfOwl.createResource(spaModel, topologyUri + ":route" + subnetName + to.replace("/", ""), Mrs.Route);
                        spaModel.add(routingTable, Mrs.hasRoute, route);
                        spaModel.add(routingService, Mrs.providesRoute, route);
                        spaModel.add(route, Mrs.routeTo, subnet);
                        spaModel.add(route, Mrs.nextHop, nextHopAddress);

                        //we have the route completed from subnet to router 
                        //now we need the part that goes from router to public subnet 
                        //first we need find the public subnet
                        if (!next.contains("internet:")) {
                            sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                                    + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                                    + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                                    + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                                    + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                                    + "SELECT ?subnet WHERE {?topology nml:hasTopology ?network ."
                                    + "?network mrs:hasTag ?networkTag ."
                                    + "?networkTag mrs:value \"external\" ."
                                    + "?network nml:hasService ?service ."
                                    + "?service a mrs:SwitchingService ."
                                    + "?service mrs:providesSubnet ?subnet ."
                                    + "?subnet mrs:hasTag ?subnetTag ."
                                    + "?subnetTag mrs:value \"public\" ."
                                    + String.format("FILTER (?topology = <%s>)", topologyUri)
                                    + "}";
                        } else {
                            //get the network name
                            String tmpNext = next.replace("internet:","");
                            tmpNext = topologyUri+":network+"+tmpNext;
                            sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                                    + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                                    + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                                    + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                                    + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                                    + "SELECT ?subnet WHERE {?topology nml:hasTopology ?network ."
                                    + "?network mrs:hasTag ?networkTag ."
                                    + "?networkTag mrs:value \"external\" ."
                                    + "?network nml:hasService ?service ."
                                    + "?service a mrs:SwitchingService ."
                                    + "?service mrs:providesSubnet ?subnet ."
                                    + "?subnet mrs:hasTag ?subnetTag ."
                                    + "?subnetTag mrs:value \"public\" ."
                                    + String.format("FILTER (?topology = <%s>)", topologyUri)
                                    + String.format("FILTER (?network = <%s>)", tmpNext)
                                    + "}";
                        }

                        r = executeQuery(sparqlString,systemModel,spaModel);
                        if (!r.hasNext()) {
                            throw new EJBException(String.format("%s::main topology %s does not have a public subnet to route to the internet ", this.getClass().getName(), topologyUri));
                        }
                        Resource publicSubnet = r.next().getResource("subnet").asResource();
                        String publicSubnetName = publicSubnet.toString().replace(topologyUri + ":", "");

                        nextHopAddress = RdfOwl.createResource(spaModel, topologyUri + ":port-" + publicSubnetName + subnetName + to.replace("/", "") + i + "networkAddress", Mrs.NetworkAddress);
                        spaModel.add(nextHopAddress, Mrs.type, "ipv4-network-address");
                        spaModel.add(nextHopAddress, Mrs.value, "any");

                        //create route from subnet to router
                        route = RdfOwl.createResource(spaModel, topologyUri + ":route" + publicSubnetName + subnetName + to.replace("/", ""), Mrs.Route);
                        spaModel.add(routingTable, Mrs.hasRoute, route);
                        spaModel.add(routingService, Mrs.providesRoute, route);
                        spaModel.add(route, Mrs.routeTo, publicSubnet);
                        spaModel.add(route, Mrs.nextHop, nextHopAddress);
                    }

                }
            }
            i++;
        }
        return spaModel;
    }

    /**
     * ********************************************************
     * Method to add a gateway to VPC if specified, this Gateway could be either
     * an internet gateway vpc or a VPN gateway
     *
     * @param spaModel
     * @param vm *******************************************************
     */
    private OntModel modelGateways(OntModel systemModel, OntModel spaModel, Resource resNetwork, JSONArray gateways, String topoUri) {
        //return if no gateways specified
        if (gateways == null) {
            return spaModel;
        }

        ListIterator gatewaysIt = gateways.listIterator();
        while (gatewaysIt.hasNext()) {
            JSONObject o = (JSONObject) gatewaysIt.next();
            if (!o.containsKey("type")) {
                throw new EJBException(String.format("%s::process JSON gateway does not have key type", this.getClass().getName()));
            }
            String value = (String) o.get("type");
            if (value.equalsIgnoreCase("internet")) {
                Resource gateway = RdfOwl.createResource(spaModel,resNetwork.toString() + "-igw",Nml.BidirectionalPort);
                spaModel.add(resNetwork, Nml.hasBidirectionalPort, gateway);
                Resource tag = spaModel.createResource(topoUri + "igwTag");
                spaModel.add(gateway, Mrs.hasTag, tag);
                spaModel.add(tag, RdfOwl.type, Mrs.Tag);
                spaModel.add(tag, Mrs.type, "gateway");
                spaModel.add(tag, Mrs.value, "internet");
            } else if (value.equalsIgnoreCase("vpn")) {
                Resource gateway = spaModel.createResource(resNetwork.toString() + "-vpngw");
                spaModel.add(gateway, RdfOwl.type, Nml.BidirectionalPort);
                spaModel.add(resNetwork, Nml.hasBidirectionalPort, gateway);
                Resource tag = spaModel.createResource(topoUri + ":vpngwTag");
                spaModel.add(gateway, Mrs.hasTag, tag);
                spaModel.add(tag, RdfOwl.type, Mrs.Tag);
                spaModel.add(tag, Mrs.type, "gateway");
                spaModel.add(tag, Mrs.value, "vpn");
            }
        }
        return spaModel;
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
                + "?policyAction spa:type 'MCE_VirtualNetworkCreation' ."
                + "?policyAction spa:exportTo ?policyData . "
                + "?policyData a spa:PolicyData}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resData = querySolution.get("policyData").asResource();
            spaModel.add(resData, Spa.type, Nml.Topology);

            JSONObject output = new JSONObject();
            output.put("uri", resNetwork);

            sparql = String.format("SELECT ?gateway ?value WHERE {<%s> nml:hasBidirectionalPort ?gateway .", resNetwork.toString())
                    + "?gateway mrs:hasTag ?tag ."
                    + "?tag mrs:value ?value}";
            r = ModelUtil.sparqlQuery(spaModel, sparql);
            JSONArray gateways = new JSONArray();
            while (r.hasNext()) {
                JSONObject gatewayObject = new JSONObject();
                QuerySolution q1 = r.next();
                Literal value = q1.get("value").asLiteral();
                Resource gateway = q1.getResource("gateway");
                gatewayObject.put("uri", gateway);
                gatewayObject.put("type", value);
                gateways.add(gatewayObject);
            }
            //add gateways to output JSON
            if (!gateways.isEmpty()) {
                output.put("gateways", gateways);
            }
            //export subnet results
            sparql = String.format("SELECT ?subnet WHERE {<%s>  nml:hasService ?service .", resNetwork)
                    + "?service a mrs:SwitchingService ."
                    + "?service mrs:providesSubnet ?subnet}";
            r = ModelUtil.sparqlQuery(spaModel, sparql);
            JSONArray subnets = new JSONArray();
            while (r.hasNext()) {
                JSONObject subnetObject = new JSONObject();
                QuerySolution q1 = r.next();
                Resource subnet = q1.get("subnet").asResource();
                subnetObject.put("uri", subnet);
                subnets.add(subnetObject);
            }
            //add subnets to output
            if (!subnets.isEmpty()) {
                output.put("subnets", subnets);
            }

            //add output as spa:value of the export resrouce
            spaModel.add(resData, Spa.value, output.toJSONString());
        }
        try {
            String ttl = ModelUtil.marshalOntModel(spaModel);
            System.out.println();
        } catch (Exception ex) {
            Logger.getLogger(MCE_VirtualNetworkCreation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * ****************************************************************
     * function that executes a query using a model addition/subtraction and a
     * reference model, returns the result of the query
     * ****************************************************************
     */
    private ResultSet executeQuery(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet r = qexec.execSelect();

        //check on reference model if the statement is not in the model addition,
        //or model subtraction
        if (!r.hasNext()) {
            qexec = QueryExecutionFactory.create(query, refModel);
            r = qexec.execSelect();
        }
        return r;
    }
}
