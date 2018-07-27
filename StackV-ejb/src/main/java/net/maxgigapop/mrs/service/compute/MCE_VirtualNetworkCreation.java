/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2016, 2017

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
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.ResourceTool;
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author max
 */
@Stateless
public class MCE_VirtualNetworkCreation extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_VirtualNetworkCreation.class.getName(), "MCE_VirtualNetworkCreation");

    private static final String OSpec_Template
            = "{\n"
            //+ "	\"name\": \"?vpc_name?\",\n"
            //+ "	\"cidr\": \"?vpc_cidr?\",\n"
            + "	\"subnets\": [\n"
            + "		{\n"
            //+ "			\"name\": \"?subnet_name?\",\n"
            //+ "			\"cidr\": \"?subnet_cidr?\",\n"
            + "			\"uri\": \"?subnet_uri?\",\n"
            + "			\"#required\": \"false\",\n"
            + "			\"#sparql\": \"SELECT DISTINCT ?subnet_uri WHERE {?vpc_uri nml:hasService ?service. ?service a mrs:SwitchingService. ?service mrs:providesSubnet ?subnet_uri}\"\n"
            + "		}\n"
            + "	],\n"
            + "	\"gateways\": [\n"
            + "		{\n"
            //+ "			\"name\": \"?gateway_name?\",\n"
            + "			\"type\": \"?gateway_type?\",\n"
            + "			\"uri\": \"?gateway_uri?\",\n"
            + "			\"#required\": \"false\",\n"
            + "			\"#sparql\": \"SELECT DISTINCT ?gateway_uri ?gateway_type WHERE {?vpc_uri nml:hasBidirectionalPort ?gateway_uri. ?gateway_uri mrs:type ?gateway_type. FILTER(?gateway_type = \\\"internet-gateway\\\" || ?gateway_type = \\\"vpn-gateway\\\")}\"\n"
            + "		}\n"
            + "	],\n"
            + "	\"uri\": \"?vpc_uri?\",\n"
            + "	\"#sparql\": \"SELECT DISTINCT ?vpc_uri WHERE {?vpc_uri a nml:Topology. ?cloud mrs:providesVPC ?vpc_uri.}\"\n"
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
        for (Resource network : policyResDataMap.keySet()) {
            //1. compute virtual network model based on  policyData
            OntModel placementModel = this.doCreation(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), network, policyResDataMap.get(network));
            if (placementModel == null) {
                throw logger.error_throwing(method, "cannot apply policy to create network=" + network);
            }

            //2. merge the network creation satements into spaModel
            outputDelta.getModelAddition().getOntModel().add(placementModel.getBaseModel());
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

    //?? Use current containing abstract Topology ?
    // ignore if dependOn 'Abstraction'
    private OntModel doCreation(OntModel systemModel, OntModel spaModel, Resource resNetwork, JSONObject topoDescription) {
        String method = "doCreation";
        logger.message(method, "@doVirtualNetworkCreation -> " + resNetwork);
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
        if (type == null) {
            type = "tenant";
        }
        else if(type.equalsIgnoreCase("external")){
            type = "external";
        }
        else
        {
            type = "tenant";
        }
        if (networkCIDR == null) {
            networkCIDR = "10.0.0.0/16";
        }
        if (topologyUri == null) {
            throw logger.error_throwing(method, String.format("network %s does not have a parent topology", resNetwork));
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
        OntModel vnetModel = modelDefaultNetwork(systemModel, spaModel, ops, resNetwork, type, networkCIDR, topologyUri, routes);
        if (aws == true) {
            vnetModel = modelGateways(systemModel, vnetModel, resNetwork, gateways, topologyUri);
            vnetModel = modelAwsSubnets(systemModel, vnetModel, resNetwork, networkCIDR, type, topologyUri, subnets);
        } else if (ops = true) {
            vnetModel = modelOpsSubnets(systemModel, vnetModel, resNetwork, networkCIDR, type, topologyUri, subnets);
        }

        return vnetModel;
    }

    /**
     * *************************************
     * Create a default network with all the elements and services needed
     *
     ****************************************
     */
    private OntModel modelDefaultNetwork(OntModel systemModel, OntModel spaModel, boolean ops, Resource resNetwork, String type, String networkCIDR, String topologyUri, JSONArray routes) {
        String method = "modelDefaultNetwork";
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
            throw logger.error_throwing(method, String.format("could not find a virtual cloud service in the "
                    + "system model associated with  %s", topologyUri));
        }
        QuerySolution querySolution = r.next();
        Resource resTopology = querySolution.get("topology").asResource();
        Resource resVirtualCloudService = querySolution.get("virtualCloudService").asResource();

        //get basic network elements and relations
        spaModel.add(resTopology, Nml.hasTopology, resNetwork);
        spaModel.add(resVirtualCloudService, Mrs.providesVPC, resNetwork);
        //
        spaModel.add(resVirtualCloudService, RdfOwl.type, Mrs.VirtualCloudService);
        spaModel.add(resNetwork, RdfOwl.type, Nml.Topology);
        Resource switchingService = spaModel.createResource(resNetwork.toString() + ":switchingservice");
        Resource routingService = spaModel.createResource(resNetwork.toString() + ":routingservice");
        spaModel.add(resNetwork, Nml.hasService, switchingService);
        spaModel.add(switchingService, RdfOwl.type, Mrs.SwitchingService);
        spaModel.add(resNetwork, Nml.hasService, routingService);
        spaModel.add(routingService, RdfOwl.type, Mrs.RoutingService);

        //add network type to the model
        spaModel.add(resNetwork, Mrs.type, type);

        //add routing info
        Resource networkAddress = spaModel.createResource(resNetwork.toString() + ":networkaddress");
        spaModel.add(resNetwork, Mrs.hasNetworkAddress, networkAddress);
        spaModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
        spaModel.add(networkAddress, Mrs.type, "ipv4-prefix");
        spaModel.add(networkAddress, Mrs.value, networkCIDR);
        Resource routingTable = spaModel.createResource(resNetwork + ":routingtable-main");
        Resource route = RdfOwl.createResourceUnverifiable(spaModel, routingTable.getURI()+ ":route-" + networkCIDR.replace("/", ""), Mrs.Route);
        spaModel.add(routingService, Mrs.providesRoutingTable, routingTable);
        spaModel.add(routingService, Mrs.providesRoute, route);
        spaModel.add(routingTable, RdfOwl.type, Mrs.RoutingTable);
        spaModel.add(routingTable, Mrs.hasRoute, route);
        spaModel.add(routingTable, Mrs.type, "main");
        spaModel.add(route, Mrs.nextHop, "local");
        spaModel.add(route, Mrs.routeTo, networkAddress);

        //add the routes to the main routing table
        if (routes != null && !ops == true) { //openStack does not have this
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
                    next = resNetwork.toString() + ":igw"; //the network internet gateway
                } else if (next.equalsIgnoreCase("vpn")) {
                    next = resNetwork.toString() + ":vpngw"; //the network vpn gateway
                } else {
                    next = ResourceTool.getResourceUri(topologyUri + ":" ,next); //a radom resource
                }

                //in case we want to propagate from a vpn on the main route table
                route = RdfOwl.createResourceUnverifiable(spaModel, routingTable.toString() + ":route-" + to.replace("/", ""), Mrs.Route);
                if (from != null && from.equalsIgnoreCase("vpn")) {
                    from = resNetwork.toString() + ":vpngw"; //the network vpn gateway
                    Resource routeFrom = spaModel.getResource(from);
                    spaModel.add(route, Mrs.routeFrom, routeFrom);
                }

                //add modeling
                Resource routeTo = spaModel.createResource(route.getURI() + ":routeto");
                spaModel.add(routeTo, RdfOwl.type, Mrs.NetworkAddress);
                spaModel.add(routeTo, Mrs.type, "ipv4-prefix-list");
                spaModel.add(routeTo, Mrs.value, to);
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
        String method = "modelAwsSubnets";
        //return if no subnets specified
        if (subnets == null) {
            return spaModel;
        }

        //add basic service for subnets
        Resource switchingService = spaModel.createResource(resNetwork.toString() + ":switchingservice");
        spaModel.add(switchingService, RdfOwl.type, Mrs.SwitchingService);

        int i = 0;
        ListIterator subnetsIt = subnets.listIterator();
        while (subnetsIt.hasNext()) {
            JSONObject o = (JSONObject) subnetsIt.next();
            if (!o.containsKey("cidr")) {
                throw logger.error_throwing(method, "JSON subnet does not have key cidr");
            }
            String subnetCIDR = (String) o.get("cidr");

            //check for validity of subnet cidr
            String subnetUri = resNetwork.toString() + "-subnet" + Integer.toString(i);
            String subnetName = subnetUri.substring(subnetUri.lastIndexOf(":") + 1);
            Resource subnet = spaModel.createResource(subnetUri);
            spaModel.add(subnet, RdfOwl.type, Mrs.SwitchingSubnet);
            spaModel.add(switchingService, Mrs.providesSubnet, subnet);
            Resource networkAddress = spaModel.createResource(subnet.toString() + ":networkaddress");
            spaModel.add(subnet, Mrs.hasNetworkAddress, networkAddress);
            spaModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
            spaModel.add(networkAddress, Mrs.type, "ipv4-prefix");
            spaModel.add(networkAddress, Mrs.value, subnetCIDR);

            if (o.containsKey("routes")) {
                Resource routingService = spaModel.getResource(resNetwork.toString() + ":routingservice");
                Resource routingTable = spaModel.createResource(resNetwork + ":routingtable-" + "subnet" + Integer.toString(i));
                spaModel.add(routingTable, RdfOwl.type, Mrs.RoutingTable);
                spaModel.add(routingTable, Mrs.type, "local");
                spaModel.add(routingService, Mrs.providesRoutingTable, routingTable);

                //add basic modeling for routing Table

                Resource route = RdfOwl.createResourceUnverifiable(spaModel, routingTable.toString() + ":route-" + networkCIDR.replace("/", ""), Mrs.Route);
                Resource routeTo = spaModel.createResource(route.toString() + ":routeto");
                spaModel.add(routeTo, RdfOwl.type, Mrs.NetworkAddress);
                spaModel.add(routeTo, Mrs.type, "ipv4-prefix-list");
                spaModel.add(routeTo, Mrs.value, networkCIDR);
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
                        throw logger.error_throwing(method, String.format("JSON subnet %s does not have proper routes "
                                + "formed", subnet));
                    }
                    String to = (String) routeObj.get("to");
                    String next = (String) routeObj.get("nextHop");
                    String from = (String) routeObj.get("from");

                    //accomodate for different kinds of nextHops
                    if (next.equalsIgnoreCase("local")) {
                        next = next; //basically do nothing it is local
                    } else if (next.equalsIgnoreCase("internet")) {
                        next = resNetwork.toString() + ":igw"; //the network internet gateway
                    } else if (next.equalsIgnoreCase("vpn")) {
                        next = resNetwork.toString() + ":vpngw"; //the network vpn gateway
                    } else {
                        next = ResourceTool.getResourceUri(topologyUri + ":" , next); //a radom resource
                    }

                    //add modeling
                    route = RdfOwl.createResourceUnverifiable(spaModel, routingTable.toString() + ":route-" + to.replace("/", ""), Mrs.Route);
                    routeTo = spaModel.createResource(route.toString() + ":routeto");
                    spaModel.add(routeTo, RdfOwl.type, Mrs.NetworkAddress);
                    spaModel.add(routeTo, Mrs.type, "ipv4-prefix-list");
                    spaModel.add(routeTo, Mrs.value, to);

                    spaModel.add(route, Mrs.routeTo, routeTo);
                    if (next.equalsIgnoreCase("local")) {
                        spaModel.add(route, Mrs.nextHop, next);
                    } else {
                        Resource nextHop = spaModel.createResource(next);
                        spaModel.add(route, Mrs.nextHop, nextHop);
                    }
                    //decide id the routeFrom should be a VPN gateway or a subent

                    if (from != null && from.equalsIgnoreCase("vpn")) {
                        from = resNetwork.toString() + ":vpngw"; //the network vpn gateway
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
        String method = "modelOpsSubnets";
        //return if no subnets specified
        if (subnets == null) {
            return spaModel;
        }

        //add basic service for subnets
        Resource switchingService = spaModel.createResource(resNetwork.toString() + ":switchingservice");
        spaModel.add(switchingService, RdfOwl.type, Mrs.SwitchingService);

        int i = 0;
        ListIterator subnetsIt = subnets.listIterator();
        while (subnetsIt.hasNext()) {
            JSONObject o = (JSONObject) subnetsIt.next();
            if (!o.containsKey("cidr")) {
                throw logger.error_throwing(method, "JSON subnet does not have key cidr");
            }
            String subnetCIDR = (String) o.get("cidr");

            //check for validity of subnet cidr
            String subnetUri = resNetwork.toString() + "-subnet" + Integer.toString(i);
            String subnetName = subnetUri.substring(subnetUri.lastIndexOf(":") + 1);
            Resource subnet = spaModel.createResource(subnetUri);
            spaModel.add(subnet, RdfOwl.type, Mrs.SwitchingSubnet);
            spaModel.add(switchingService, Mrs.providesSubnet, subnet);
            Resource networkAddress = spaModel.createResource(subnet.toString() + ":networkaddress");
            spaModel.add(subnet, Mrs.hasNetworkAddress, networkAddress);
            spaModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
            spaModel.add(networkAddress, Mrs.type, "ipv4-prefix");
            spaModel.add(networkAddress, Mrs.value, subnetCIDR);


            //add basic routing for the subnet
            Resource routingService = spaModel.getResource(resNetwork.toString() + ":routingservice");
            Resource routingTable = spaModel.createResource(resNetwork + ":routingtable-main");
            spaModel.add(routingService, Mrs.providesRoutingTable, routingTable);
            Resource route = RdfOwl.createResourceUnverifiable(spaModel, routingTable.toString() + ":route-" + subnetName, Mrs.Route);
            spaModel.add(routingService, Mrs.providesRoute, route);
            spaModel.add(routingTable, Mrs.hasRoute, route);
            spaModel.add(route, Mrs.routeFrom, subnet);
            spaModel.add(route, Mrs.routeTo, networkAddress);
            spaModel.add(route, Mrs.nextHop, "local");

            if (o.containsKey("routes")) {
                JSONArray routes = (JSONArray) o.get("routes");
                ListIterator routesIt = routes.listIterator();
                while (routesIt.hasNext()) {
                    JSONObject routeObj = (JSONObject) routesIt.next();
                    if (!routeObj.containsKey("to") || !routeObj.containsKey("nextHop")) {
                        throw logger.error_throwing(method, String.format("JSON subnet %s does not have proper routes "
                                + "formed", subnet));
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
                            throw logger.error_throwing(method, String.format("main topology %s does not have a routing service", topologyUri));
                        }
                        routingService = r.next().getResource("service").asResource();
                        routingTable = spaModel.createResource(resNetwork + ":routingtable-" + "subnet" + Integer.toString(i));
                        spaModel.add(routingTable, RdfOwl.type, Mrs.RoutingTable);
                        spaModel.add(routingService, Mrs.providesRoutingTable, routingTable);

                        //create the nextHop network address on the subnet
                        Resource nextHopAddress = RdfOwl.createResource(spaModel, subnetUri +"-port" + to.replace("/", "") + i + "networkAddress", Mrs.NetworkAddress);
                        spaModel.add(nextHopAddress, Mrs.type, "ipv4-network-address");
                        spaModel.add(nextHopAddress, Mrs.value, "any");

                        //create route from subnet to router
                        route = RdfOwl.createResourceUnverifiable(spaModel, subnetUri + ":route-" + to.replace("/", ""), Mrs.Route);
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
                                    + "?network mrs:type \"external\" ."
                                    + "?network nml:hasService ?service ."
                                    + "?service a mrs:SwitchingService ."
                                    + "?service mrs:providesSubnet ?subnet ."
                                    + String.format("FILTER (?topology = <%s>)", topologyUri)
                                    + "}";
                        } else {
                            //get the network name
                            String tmpNext = next.replace("internet:", "");
                            tmpNext = topologyUri + ":network+" + tmpNext;
                            sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                                    + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                                    + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                                    + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                                    + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                                    + "SELECT ?subnet WHERE {?topology nml:hasTopology ?network ."
                                    + "?network mrs:type \"external\" ."
                                    + "?network nml:hasService ?service ."
                                    + "?service a mrs:SwitchingService ."
                                    + "?service mrs:providesSubnet ?subnet ."
                                    + String.format("FILTER (?topology = <%s>)", topologyUri)
                                    + String.format("FILTER (?network = <%s>)", tmpNext)
                                    + "}";
                        }

                        r = ModelUtil.executeQuery(sparqlString, systemModel, spaModel);
                        if (!r.hasNext()) {
                            throw logger.error_throwing(method, String.format("main topology %s does not have a public subnet to route to the internet", topologyUri));
                        }
                        Resource publicSubnet = r.next().getResource("subnet").asResource();
                        String publicSubnetUri = ResourceTool.getResourceUri(topologyUri,publicSubnet.toString());

                        nextHopAddress = RdfOwl.createResource(spaModel, publicSubnetUri + ":port-"  + subnetName + to.replace("/", "") + i + "networkAddress", Mrs.NetworkAddress);
                        spaModel.add(nextHopAddress, Mrs.type, "ipv4-network-address");
                        spaModel.add(nextHopAddress, Mrs.value, "any");

                        //create route from subnet to router
                        route = RdfOwl.createResourceUnverifiable(spaModel, publicSubnetUri + ":route-" +  subnetName +"-"+ to.replace("/", ""), Mrs.Route);
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
        String method = "modelGateways";
        //return if no gateways specified
        if (gateways == null) {
            return spaModel;
        }

        ListIterator gatewaysIt = gateways.listIterator();
        while (gatewaysIt.hasNext()) {
            JSONObject o = (JSONObject) gatewaysIt.next();
            if (!o.containsKey("type")) {
                throw logger.error_throwing(method, "JSON gateway does not have key type");
            }
            String type = (String) o.get("type");
            JSONArray jConnects = (JSONArray) o.get("connects");
            if (type.equalsIgnoreCase("internet")) {
                Resource gateway = RdfOwl.createResource(spaModel, resNetwork.toString() + ":igw", Nml.BidirectionalPort);
                spaModel.add(resNetwork, Nml.hasBidirectionalPort, gateway);
                spaModel.add(gateway, Mrs.type, "internet-gateway");
            } else if (type.equalsIgnoreCase("vpn")) {
                Resource gateway = RdfOwl.createResource(spaModel, resNetwork.toString() + ":vpngw", Nml.BidirectionalPort);
                spaModel.add(resNetwork, Nml.hasBidirectionalPort, gateway);
                spaModel.add(gateway, Mrs.type, "vpn-gateway");
            } else if (type.equalsIgnoreCase("cloud_vpn") && jConnects != null && !jConnects.isEmpty()) {
                Resource vpn = RdfOwl.createResource(spaModel, resNetwork.toString() + ":vpn", Nml.BidirectionalPort);
                spaModel.add(spaModel.getResource(topoUri), Nml.hasBidirectionalPort, vpn);
                spaModel.add(vpn, Mrs.type, "vpn-connection");
                spaModel.add(vpn, Nml.isAlias, spaModel.getResource(resNetwork.toString() + ":vpngw"));
                for (int i = 0; i < jConnects.size(); i++) {
                    JSONObject jConn = (JSONObject) jConnects.get(i);
                    Resource vpnTunnel = RdfOwl.createResource(spaModel, resNetwork.toString() + ":vpn:tunnel-" + (i+1), Nml.BidirectionalPort);
                    Resource cgwAddress = RdfOwl.createResource(spaModel, vpnTunnel.getURI() + ":cgw-address", Mrs.NetworkAddress);
                    spaModel.add(cgwAddress, Mrs.type, "ipv4-address:customer");
                    spaModel.add(cgwAddress, Mrs.value, jConn.get("via").toString());
                    spaModel.add(vpnTunnel, Mrs.hasNetworkAddress, cgwAddress);
                    Resource cgwRoutes = RdfOwl.createResource(spaModel, vpnTunnel.getURI() + ":cgw-routes", Mrs.NetworkAddress);
                    spaModel.add(cgwRoutes, Mrs.type, "ipv4-prefix-list:customer");
                    spaModel.add(cgwRoutes, Mrs.value, jConn.get("to").toString());
                    spaModel.add(vpnTunnel, Mrs.hasNetworkAddress, cgwRoutes);
                    spaModel.add(vpn, Nml.hasBidirectionalPort, vpnTunnel);
                    if (jConn.containsKey("from")) {
                        Resource cloudAddress = RdfOwl.createResource(spaModel, vpnTunnel.getURI() + ":address", Mrs.NetworkAddress);
                        spaModel.add(cloudAddress, Mrs.type, "ipv4-address");
                        spaModel.add(cloudAddress, Mrs.value, jConn.get("from").toString());
                        spaModel.add(vpnTunnel, Mrs.hasNetworkAddress, cloudAddress);
                    }
                    if (jConn.containsKey("secret")) {
                        Resource sharedSecret = RdfOwl.createResource(spaModel, vpnTunnel.getURI() + ":secret", Mrs.NetworkAddress);
                        spaModel.add(sharedSecret, Mrs.type, "secret");
                        spaModel.add(sharedSecret, Mrs.value, jConn.get("secret").toString());
                        spaModel.add(vpnTunnel, Mrs.hasNetworkAddress, sharedSecret);
                    }
                }
            }
        }
        return spaModel;
    }

}
