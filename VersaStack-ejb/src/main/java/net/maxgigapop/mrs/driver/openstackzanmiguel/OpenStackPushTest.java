/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import com.amazonaws.services.ec2.model.Instance;
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
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.*;
import org.openstack4j.model.storage.block.*;
import org.openstack4j.openstack.networking.domain.NeutronPort;
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

    public static void main(String[] args) {
        OpenStackPushTest test = new OpenStackPushTest("http://max-vlsr2.dragon.maxgigapop.net", "admin", "1234", "miguel's project", "jwjcjsd");

    }

    public OpenStackPushTest(String url, String username, String password, String tenantName, String topologyUri) {
        client = new OpenStackGet(url, username, password, tenantName);
        osClient = client.getClient();

        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";

    }

    /**
     * ***********************************************
     * Method to get the requests provided in the model addition and model
     * reduction ************************************************
     */
    public List<JSONObject> pushPropagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) throws Exception {
        List<JSONObject> requests = new ArrayList();

        //get all the requests
        requests.addAll(createVolumesRequests(modelRef, modelReduct));
        requests.addAll(deleteSubnetsRequests(modelRef, modelReduct));
        requests.addAll(createNetworksRequests(modelRef, modelAdd));
        requests.addAll(createSubnetsRequests(modelRef, modelAdd));
        requests.addAll(createVolumesRequests(modelRef, modelAdd));
        return requests;
    }

    /**
     * **********************************************************************
     * Function to do execute all the requests provided by the propagate method
     * **********************************************************************
     */
    public void pushCommit(List<JSONObject> requests) {
        for (JSONObject o : requests) {
            if (o.get("request").equals("CreateSubnetRequest")) {
                Subnet subnet = new NeutronSubnet();
                subnet.toBuilder().cidr(o.get("cidr block").toString())
                        .network(client.getNetwork(o.get("network name").toString()))
                        .name(o.get("name").toString());

                subnet = osClient.networking().subnet().create(subnet);
                // client.getSubnets().add(? extends Subnet subnet)
            }
        }

    }

    /**
     * *****************************************************************
     * Function to create a volumes from a modelRef
     * ***************************************************************
     */
    private List<JSONObject> DeleteVolumesRequests(OntModel modelRef, OntModel model) throws Exception {
        List<JSONObject> requests = new ArrayList();
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

                JSONObject o = new JSONObject();
                o.put("request", "DeleteVolumeRequest");
                o.put("type", type.asLiteral());
                o.put("size", size.asLiteral());
                o.put("availabiltyZone", "nova");
                o.put("name", volume);
                requests.add(o);

            }
        }
        return requests;
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
                o.put("name", subnetName);
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
//                requests += String.format("CreateVpcRequest %s %s %s \n", vpcIp, vpcIdTagValue, routeTableIdTagValue);
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
                o.put("name", subnetName);
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
    private List<JSONObject> createVolumesRequests(OntModel modelRef, OntModel model) throws Exception {
        List<JSONObject> requests = new ArrayList();
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

                JSONObject o = new JSONObject();
                o.put("request", "CreateVolumeRequest");
                o.put("type", type.asLiteral());
                o.put("size", size.asLiteral());
                o.put("availabiltyZone", "nova");
                o.put("name", volume);
                requests.add(o);

            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create network interfaces from a model
     * ****************************************************************
     */
    private String createPortsRequests(OntModel modelRef, OntModel modelDelta) throws Exception {
        String requests = "";
        String query;

        //get the tag resource from the reference model that indicates 
        //that this is a network  interface 
        query = "SELECT ?tag WHERE {?tag mrs:type \"interface\" ."
                + "?tag mrs:value \"network\"}";
        ResultSet r = executeQuery(query, modelRef, emptyModel);
        if (!r.hasNext()) {
            throw new Exception(String.format("Reference model has no tags for network"
                    + "interfaces"));
        }
        QuerySolution q = r.next();
        RDFNode tag = q.get("tag");

        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort ."
                + "?port  mrs:hasTag <" + tag.asResource() + ">}";
        r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portName = port.asResource().toString().replace(topologyUri, "");

            Port p = client.getPort(portName);

            if (p != null) //network interface  exists, no need to create a network interface
            {
                throw new Exception(String.format("Network interface %s already exists", portName));
            } else {
                //to get the private ip of the network interface
                query = "SELECT ?address ?value WHERE {<" + port.asResource() + ">  mrs:hasNetworkAddress  ?address ."
                        + "?address mrs:type \"ipv4:private\" ."
                        + "?address mrs:value ?value }";
                ResultSet r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify privat ip address of port: %s", port));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String privateAddress = value.asLiteral().toString();

                //find the subnet that has the port previously found
                query = "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify network interface subnet of port: %s", port));
                }
                String subnetId = null;
                while (r1.hasNext()) {
                    querySolution1 = r1.next();
                    RDFNode subnet = querySolution1.get("subnet");
                    query = "SELECT ?subnet WHERE {<" + subnet.asResource() + ">  a  mrs:SwitchingSubnet}";
                    ResultSet r3 = executeQuery(query, modelRef, modelDelta);
                    if (r3.hasNext()) //search in the model to see if subnet existed before
                    {
                        String subnetName = subnet.asResource().toString().replace(topologyUri, "");
                        subnetId = client.getSubnet(subnetName).getId();
                    }
                    if (subnetId==null || subnetId.isEmpty()) {
                        throw new Exception(String.format("model additions subnet for port %s"
                                + "is not found in the reference model", subnetId));
                    }
                }

                //create the network interface 
                requests += String.format("CreateNetworkInterfaceRequest  %s %s %s \n", privateAddress, subnetId, portName);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create Instances
     * ****************************************************************
     */
    /* private List<JSONObject> createServerRequests(OntModel model, OntModel modelAdd) throws Exception {
     List<JSONObject> requests = new ArrayList();
     String query;

     query = "SELECT ?server WHERE {?server a nml:vm}";
     ResultSet r = executeQuery(query, emptyModel, modelAdd);
     while (r.hasNext()) {
     QuerySolution querySolution = r.next();
     RDFNode node = querySolution.get("server");
     String serverName = node.asResource().toString().replace(topologyUri, "");
     Server server= client.getServer(serverName);

            
     if (server!= null) //instance does not be to be created
     {
     throw new Exception(String.format("Server %s already exists", serverName));
     } else {
     //check what service is providing the instance
     query = "SELECT ?service WHERE {?service mrs:providesVM <" + node.asResource() + ">}";
     ResultSet r1 = executeQuery(query, model, modelAdd);
     if (!r1.hasNext()) {
     throw new Exception(String.format("model addition does not specify service that provides Instance: %s", node));
     }

     //find the Vpc that the node will be in
     query = "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() + ">}";
     r1 = executeQuery(query, model, modelAdd);
     if (!r1.hasNext()) {
     throw new Exception(String.format("model addition does not specify the Vpc of the node: %s", node));
     }
     QuerySolution querySolution1 = r1.next();
     RDFNode vpc = querySolution1.get("vpc");
     String vpcId = vpc.asResource().toString().replace(topologyUri, "");
     vpcId = getVpcId(vpcId);

     //to find the subnet the node is in first  find the port the node uses
     query = "SELECT ?port WHERE {<" + node.asResource() + "> nml:hasBidirectionalPort ?port}";
     ResultSet r2 = executeQuery(query, model, modelAdd);
     if (!r2.hasNext()) {
     throw new Exception(String.format("model addition does not specify the subnet that the node is: %s", node));
     }
     List<String> portsId = new ArrayList();
     RDFNode lastPort = null;
     while (r2.hasNext())//there could be multiple network interfaces attached to the instance
     {
     QuerySolution querySolution2 = r2.next();
     RDFNode port = querySolution2.get("port");
     String id = port.asResource().toString().replace(topologyUri, "");
     portsId.add(getResourceId(id));
     lastPort = port;
     }

     //find the EBS volumes that the instance uses
     query = "SELECT ?volume WHERE {<" + node.asResource() + ">  mrs:hasVolume  ?volume}";
     ResultSet r4 = executeQuery(query, model, modelAdd);
     if (!r4.hasNext()) {
     throw new Exception(String.format("model addition does not specify the volume of the new node: %s", node));
     }
     List<String> volumesId = new ArrayList();
     while (r4.hasNext())//there could be multiple volumes attached to the instance
     {
     QuerySolution querySolution4 = r4.next();
     RDFNode volume = querySolution4.get("volume");
     String id = volume.asResource().toString().replace(topologyUri, "");
     volumesId.add(getVolumeId(id));
     }

     //put request for new instance
     requests += String.format("RunInstancesRequest ami-146e2a7c t2.micro %s ", nodeIdTagValue);

     //put the root device of the instance
     query = "SELECT ?volume ?deviceName ?size ?type  WHERE {"
     + "<" + node.asResource() + ">  mrs:hasVolume  ?volume ."
     + "?volume mrs:target_device ?deviceName ."
     + "?volume mrs:disk_gb ?size ."
     + "?volume mrs:value ?type}";
     r4 = executeQuery(query, model, modelAdd);
     boolean hasRootVolume = false;
     while (r4.hasNext()) {
     QuerySolution querySolution4 = r4.next();
     RDFNode volume = querySolution4.get("volume");
     String volumeTag = volume.asResource().toString().replace(topologyUri, "");
     String type = querySolution4.get("type").asLiteral().toString();
     String size = querySolution4.get("size").asLiteral().toString();
     String deviceName = querySolution4.get("deviceName").asLiteral().toString();
     if (deviceName.equals("/dev/sda1") || deviceName.equals("/dev/xvda")) {
     hasRootVolume = true;
     requests += String.format("%s %s %s %s ", type, size, deviceName, volumeTag);
     }
     }
     if (hasRootVolume == false) {
     throw new Exception(String.format("model addition does not specify root volume for node: %s", node));
     }

     int index = 0;
     //put the networ interfaces 
     requests += "NetworkInterfaceSpecification ";
     for (String id : portsId) {
     requests += String.format("%s %d ", id, index);
     index++; //increment the device index
     }
     requests += "\n";
     }
     }
     return requests;
     }/*

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
