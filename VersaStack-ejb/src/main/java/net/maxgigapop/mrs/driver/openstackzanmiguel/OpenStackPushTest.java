/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONObject;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.*;
import org.openstack4j.model.storage.block.*;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;

/**
 *
 * @author muzcategui
 */
/**
 * **********************************************************
 *
 * TODO 1) figure how routing tables work in OpenStack 2) use the routing tables
 * while doing the network requests addition and deletion 3) finish the network
 * propagate request method 4) figure out how the root devices work in openStack
 * in order to create/delete a volume
 * **********************************************************
 */
public class OpenStackPushTest {

    //global variables
    private OpenStackGet client = null;
    private OSClient osClient = null;
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    private String topologyUri;

    public OpenStackPushTest(String url, String username, String password, String tenantName, String topologyUri) {
        client = new OpenStackGet(url, username, password, tenantName);
        osClient = client.getClient();

        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
    }
    
     /**
     * ***********************************************
     * Method to get the requests provided in the model
     * addition and model reduction
     * ************************************************
     */
    public List<JSONObject> pushPropagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) throws Exception {
        List<JSONObject> requests = new ArrayList();

        //get all the requests
        requests.addAll(deleteSubnetsRequests(modelRef, modelReduct));
        requests.addAll(createNetworksRequests(modelRef, modelAdd));
        requests.addAll(createSubnetsRequests(modelRef, modelAdd));
        return requests;
    }

    /**
     * **********************************************************************
     * Function to do execute all the requests provided by the propagate method
     * **********************************************************************
     */
    public void pushCommit(List<JSONObject> requests) {
        for(JSONObject o: requests)
        {
            if(o.get("request").equals("CreateSubnetRequest"))
            {
              Subnet subnet= new NeutronSubnet();
              subnet.toBuilder().cidr(o.get("cidr block").toString())
                      .network(client.getNetwork(o.get("network name").toString()))
                      .name(o.get("name").toString());
                      
              subnet = osClient.networking().subnet().create(subnet);
              client.getSubnets().add(? extends Subnet subnet);
            }
        }

    }

    /**
     * *****************************************************************
     * Function to create a subnets from a modelRef
     * ***************************************************************
     */
    private List<JSONObject> deleteSubnetsRequests(OntModel modelRef, OntModel model) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, emptyModel, model);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetName = subnet.asResource().toString().replace(topologyUri, "");
            Subnet s = client.getSubnet(subnetName);

            if (s == null) //subnet  exists,does not need to create one
            {
                throw new Exception(String.format("Subnet %s does not exists", subnet));
            } else {
                String subnetId = s.getId();
                query = "SELECT ?service {?service mrs:providesSubnet <" + subnet.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("No service has subnet %s", subnet));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode service = querySolution1.get("service");
                query = "SELECT ?network {?network nml:hasService <" + service.asResource() + "> ."
                        + "<" + service.asResource() + "> a mrs:SwitchingService}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    r1 = executeQuery(query, modelRef, modelRef);
                }
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Subnet %s does not belong a network", subnet));
                }
                querySolution1 = r1.next();
                RDFNode network = querySolution1.get("network");
                String networkName = network.asResource().toString().replace(topologyUri, "");

                query = "SELECT ?subnet ?address ?value WHERE {?subnet mrs:hasNetworkAddress ?address ."
                        + "?address a mrs:NetworkAddress ."
                        + "?address mrs:type \"ipv4-prefix\" ."
                        + "?address mrs:value ?value}";
                r1 = executeQuery(query, modelRef, model);
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();

                JSONObject o = new JSONObject();
                o.put("request", "DeleteSubnetRequest");
                o.put("network name", networkName);
                o.put("cidr block", cidrBlock);
                o.put("subnet name", subnetName);
                requests.add(o);
            }
        }
        return requests;
    }

    /**
     * *****************************************************************
     * Function to create a Vpc from a modelRef
     * /*****************************************************************
     */
    private List<JSONObject> createNetworksRequests(OntModel modelRef, OntModel model) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        query = "SELECT ?network WHERE {?service mrs:providesVPC  ?network ."
                + "?network a nml:Topology}";
        ResultSet r = executeQuery(query, emptyModel, model);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode network = querySolution.get("network");
            String networkName = network.asResource().toString().replace(topologyUri, "");

            //double check vpc does not exist in the cloud
            Network net = client.getNetwork(networkName);

            if (net != null) // if network  exists, no need to create it
            {
                throw new Exception(String.format("Network %s already exists", network));
            } else {
                String networkId = net.getId();
                query = "SELECT ?cloud WHERE {?cloud nml:hasTopology <" + network.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the openStack-cloud that"
                            + "provides network : %s", network));
                }

                query = "SELECT ?address WHERE {<" + network.asResource() + "> mrs:hasNetworkAddress ?address}";
                r1 = executeQuery(query, emptyModel, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the "
                            + "newtowk address of the network: %s", network));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");

                query = "SELECT ?type ?value WHERE {<" + address.asResource() + "> mrs:type ?type ."
                        + "<" + address.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the "
                            + "type or value of network address: %s", address));
                }
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();

                //check taht vpc offers switching and routing Services and vpc services
                query = "SELECT ?service  WHERE {<" + network.asResource() + "> nml:hasService  ?service ."
                        + "?service a  mrs:SwitchingService}";
                r1 = executeQuery(query, emptyModel, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("New network %s does not speicfy Switching Service", network));
                }

                query = "SELECT ?service  WHERE {<" + network.asResource() + "> nml:hasService  ?service ."
                        + "?service a mrs:RoutingService}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("New network %s does not speicfy Routing Service", network));
                }
                querySolution1 = r1.next();
                RDFNode routingService = querySolution1.get("service");

                //incorporate main routing table and routes into the vpc
                query = "SELECT ?routingTable ?type  WHERE {<" + routingService.asResource() + "> mrs:providesRoutingTable ?routingTable ."
                        + "?routingTable mrs:type  \"main\"}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Routing service  %s does not speicfy main Routing table in the model addition", routingService));
                }

                querySolution1 = r1.next();
                RDFNode routingTable = querySolution1.get("routingTable");
                String routeTableIdTagValue = routingTable.asResource().toString().replace(topologyUri, "");

                String vpcIp = cidrBlock;
                cidrBlock = "\"" + cidrBlock + "\"";

                //check for  the local route is in the route table 
                query = "SELECT ?route ?to  WHERE {<" + routingTable.asResource() + "> mrs:hasRoute ?route ."
                        + "<" + routingService.asResource() + "> mrs:providesRoute ?route ."
                        + "?route mrs:nextHop \"local\" ."
                        + "?route mrs:routeTo  ?to ."
                        + "?to mrs:value " + cidrBlock + "}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Routing service has no route for main table in the model"
                            + " addition", routingService));
                }
                requests += String.format("CreateVpcRequest %s %s %s \n", vpcIp, vpcIdTagValue, routeTableIdTagValue);
            }
        }
        return requests;
    }

    /**
     * *****************************************************************
     * Function to create a subnets from a modelRef
     * ***************************************************************
     */
    private List<JSONObject> createSubnetsRequests(OntModel modelRef, OntModel model) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, emptyModel, model);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetName = subnet.asResource().toString().replace(topologyUri, "");
            Subnet s = client.getSubnet(subnetName);

            if (s != null) //subnet  exists,does not need to create one
            {
                throw new Exception(String.format("Subnet %s already exists", subnet));
            } else {
                String subnetId = s.getId();
                query = "SELECT ?service {?service mrs:providesSubnet <" + subnet.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("No service has subnet %s", subnet));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode service = querySolution1.get("service");
                query = "SELECT ?network {?network nml:hasService <" + service.asResource() + "> ."
                        + "<" + service.asResource() + "> a mrs:SwitchingService}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    r1 = executeQuery(query, modelRef, modelRef);
                }
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Subnet %s does not belong a network", subnet));
                }
                querySolution1 = r1.next();
                RDFNode network = querySolution1.get("network");
                String networkName = network.asResource().toString().replace(topologyUri, "");

                query = "SELECT ?subnet ?address ?value WHERE {?subnet mrs:hasNetworkAddress ?address ."
                        + "?address a mrs:NetworkAddress ."
                        + "?address mrs:type \"ipv4-prefix\" ."
                        + "?address mrs:value ?value}";
                r1 = executeQuery(query, modelRef, model);
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();

                JSONObject o = new JSONObject();
                o.put("request", "CreateSubnetRequest");
                o.put("network name", networkName);
                o.put("cidr block", cidrBlock);
                o.put("subnet name", subnetName);
                requests.add(o);
            }
        }
        return requests;
    }

    /**
     * *****************************************************************
     * Function to create a volumes from a modelRef
     * ***************************************************************
     */
    private String createVolumesRequests(OntModel modelRef, OntModel model) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        ResultSet r = executeQuery(query, emptyModel, model);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeName = volume.asResource().toString().replace(topologyUri, "");

            Volume v = client.getVolume(volumeName);

            if (v != null) //volume exists, no need to create a volume
            {
                throw new Exception(String.format("Volume %s already exists", v));
            } else {
                String volumeId = v.getId();
                //check what service is providing the volume
                query = "SELECT ?type WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify service that provides volume: %s", volume));
                }

                //find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new type of volume: %s", volume));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new size of volume: %s", volume));
                }
                querySolution1 = r1.next();
                RDFNode size = querySolution1.get("size");

                //dont create the volumes with no target device and that are attached to an instance
                //since this volumes are the root devices
                query = "SELECT ?deviceName WHERE {<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                r1 = executeQuery(query, modelRef, model);
                if (r1.hasNext()) {
                    querySolution1 = r1.next();
                    String deviceName = querySolution1.get("deviceName").asLiteral().toString();

                    if (!deviceName.equals("/dev/sda1") && !deviceName.equals("/dev/xvda")) {
                        requests += String.format("CreateVolumeRequest %s %s %s  %s  \n", type.asLiteral().getString(),
                                size.asLiteral().getString(), region.getName() + "e", volumeIdTagValue);
                    }
                } else {
                    requests += String.format("CreateVolumeRequest %s %s %s %s  \n", type.asLiteral().getString(),
                            size.asLiteral().getString(), Regions.US_EAST_1.getName() + "e", volumeIdTagValue);
                }
            }
        }

        return requests;
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
