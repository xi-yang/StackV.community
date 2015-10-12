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

            // ? place to a specific Node ?
            //$$ Other types of filter methods have yet to be implemented.
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
        if (type == null || !type.equalsIgnoreCase("internal") || !type.equalsIgnoreCase("external")) {
            type = "internal";
        }
        if (networkCIDR == null) {
            networkCIDR = "10.0.0.0/16";
        }
        if (topologyUri == null) {
            throw new EJBException(String.format("%s::process network %s does not have a parent topology", this.getClass().getName(), value));
        }

        spaModel = modelDefaultNetwork(systemModel, spaModel, resNetwork, type, networkCIDR, topologyUri, routes);
        spaModel = modelGateways(systemModel, spaModel, resNetwork, gateways, topologyUri);
        spaModel = modelSubnets(systemModel, spaModel, resNetwork, networkCIDR, topologyUri, subnets);

        return spaModel;
    }

    /**
     * *************************************
     * Create a default network with all the elements and services needed
     *
     ****************************************
     */
    private OntModel modelDefaultNetwork(OntModel systemModel, OntModel spaModel, Resource resNetwork, String type, String networkCIDR, String topologyUri, JSONArray routes) {
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
        if (routes != null) {
            ListIterator routesIt = routes.listIterator();
            while (routesIt.hasNext()) {
                JSONObject o = (JSONObject) routesIt.next();
                String to = (String) o.get("to");
                String next = (String) o.get("nextHop");

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
                Resource routeTo = spaModel.createResource(routingTable.toString() + "-" + "routeto" + to.replace("/", ""));
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
                spaModel.add(routingTable, Mrs.hasRoute, route);
                spaModel.add(routingService, Mrs.providesRoute, route);

            }
        }

        return spaModel;
    }

    /**
     * ********************************************************
     * Method to add subnet to model if specified
     *
     * @param spaModel
     * @param vm ********************************************************
     */
    private OntModel modelSubnets(OntModel systemModel, OntModel spaModel, Resource resNetwork, String networCIDR, String topologyUri, JSONArray subnets) {
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
            Resource subnet = spaModel.createResource(resNetwork.toString() + "-subnet" + Integer.toString(i));
            spaModel.add(subnet, RdfOwl.type, Mrs.SwitchingSubnet);
            spaModel.add(switchingService, Mrs.providesSubnet, subnet);
            Resource networkAddress = spaModel.createResource(subnet.toString() + "networkAddress");
            spaModel.add(subnet, Mrs.hasNetworkAddress, networkAddress);
            spaModel.add(networkAddress, RdfOwl.type, Mrs.NetworkAddress);
            spaModel.add(networkAddress, Mrs.type, "ipv4-prefix");
            spaModel.add(networkAddress, Mrs.value, subnetCIDR);

            if (o.containsKey("routes")) {
                Resource routingService = spaModel.getResource(resNetwork.toString() + "routingService");
                Resource routingTable = spaModel.createResource(resNetwork + "rtb-" + "subnet" + Integer.toString(i));
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
                    Resource routeTo = spaModel.createResource(routingTable.toString() + "-" + "routeto" + to.replace("/", ""));
                    spaModel.add(routeTo, RdfOwl.type, Mrs.NetworkAddress);
                    spaModel.add(routeTo, Mrs.type, "ipv4-prefix-list");
                    spaModel.add(routeTo, Mrs.value, to);
                    Resource route = spaModel.createResource(routingTable.toString() + "route" + to.replace("/", ""));
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
                Resource gateway = spaModel.createResource(resNetwork.toString() + "-igw");
                spaModel.add(gateway, RdfOwl.type, Nml.BidirectionalPort);
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
                Resource tag = spaModel.createResource(topoUri + "vpngwTag");
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
                output.put("gateways",gateways);
            }
            //export subnet results
            sparql = String.format("SELECT ?subnet WHERE {<%s>  nml:hasService ?service .", resNetwork)
                    + "?service a nml:SwitchingService ."
                    + "?service mrs:providesSubnet ?subnet}";
            r = ModelUtil.sparqlQuery(spaModel, sparql);
            JSONArray subnets = new JSONArray();
            while (r.hasNext()) {
                JSONObject subnetObject = new JSONObject();
                QuerySolution q1 = r.next();
                Resource subnet = q1.get("subnet").asResource();
                spaModel.add(resData, Spa.value, subnet);
                subnetObject.put("uri", subnet);
                subnets.add(subnetObject);
            }
            //add subnets to output
            if (!subnets.isEmpty()) {
                output.put("subnets",subnets);
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
}
