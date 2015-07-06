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
import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.*;
import org.openstack4j.model.storage.block.*;
import org.openstack4j.openstack.compute.domain.NovaInterfaceAttachment;
import org.openstack4j.openstack.compute.domain.NovaServer;
import org.openstack4j.openstack.compute.internal.ServerServiceImpl;
import org.openstack4j.openstack.compute.internal.ext.InterfaceServiceImpl;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;
import org.openstack4j.openstack.networking.internal.PortServiceImpl;
import org.openstack4j.openstack.storage.block.domain.CinderVolume;

/**
 *
 * @author muzcategui
 */
/**
 * **********************************************************
 *
 * TODO 1) figure out how the root devices work in openStack
 * in order to create/delete a volume
 *      2) Add and delete an object to the reference of OS client
 *         for local reference
 * **********************************************************
 */
public class OpenStackPush {

    //global variables
    private OpenStackGet client = null;
    private OSClient osClient = null;
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    private String topologyUri;

    /*public static void main(String[] args) {
     OpenStackPush test = new OpenStackPush();

     }*/
    public OpenStackPush(String url,String NATServer, String username, String password, String tenantName, String topologyUri) {
        client = new OpenStackGet(url, NATServer, username, password, tenantName);
        osClient = client.getClient();

        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
    }

    /**
     * ***********************************************
     * Method to get the requests provided in the model addition and model
     * reduction ************************************************
     */
    public List<JSONObject> propagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) throws Exception {
        List<JSONObject> requests = new ArrayList();

        //get all the requests
        requests.addAll(portAttachmentRequests(modelRef, modelReduct, false));
        requests.addAll(volumesAttachmentRequests(modelRef, modelReduct, false));
        requests.addAll(volumeRequests(modelRef, modelReduct, false));
        requests.addAll((portRequests(modelRef, modelReduct, false)));
        requests.addAll(subnetRequests(modelRef, modelReduct, false));
        requests.addAll(subnetRequests(modelRef, modelAdd, true));
        requests.addAll(volumeRequests(modelRef, modelAdd, true));
        requests.addAll((portRequests(modelRef, modelAdd, true)));
        requests.addAll(volumesAttachmentRequests(modelRef, modelAdd, true));
        requests.addAll(portAttachmentRequests(modelRef, modelAdd, true));
        return requests;
    }

    /**
     * **********************************************************************
     * Function to do execute all the requests provided by the propagate method
     * **********************************************************************
     */
    public void pushCommit(List<JSONObject> requests) {
        for (JSONObject o : requests) {
            if (o.get("request").toString().equals("CreatePortRequest")) {
                Port port = new NeutronPort();
                Subnet net = client.getSubnet(o.get("subnet name").toString());
                port.toBuilder().name(o.get("name").toString())
                        .fixedIp(o.get("private address").toString(), net.getId());

                osClient.networking().port().create(port);
            } else if (o.get("request").toString().equals("DeletePortRequest")) {
                Port port = client.getPort(o.get("port name").toString());
                osClient.networking().port().delete(port.getId());

            } else if (o.get("request").toString().equals("CreateVolumeRequest")) {
                Volume volume = new CinderVolume();
                volume.toBuilder().size(Integer.parseInt(o.get("size").toString()))
                        .volumeType(o.get("type").toString())
                        .name(o.get("name").toString());

                osClient.blockStorage().volumes().create(volume);

            } else if (o.get("request").toString().equals("DeleteVolumeRequest")) {
                Volume volume = client.getVolume(o.get("volume name").toString());
                osClient.blockStorage().volumes().delete(volume.getId());

            } else if (o.get("request").toString().equals("CreateSubnetRequest")) {
                Subnet subnet = new NeutronSubnet();
                subnet.toBuilder().cidr(o.get("cidr block").toString())
                        .network(client.getNetwork(o.get("network name").toString()))
                        .name(o.get("name").toString());
                String gatewayIp = o.get("gateway ip").toString();
                if (!gatewayIp.isEmpty()) {
                    subnet.toBuilder().gateway(gatewayIp);
                }

                osClient.networking().subnet().create(subnet);

            } else if (o.get("request").toString().equals("DeleteSubnetRequest")) {
                Subnet net = client.getSubnet(o.get("subnet name").toString());
                osClient.networking().subnet().delete(net.getId());
            } else if (o.get("request").toString().equals("RunInstanceRequest")) {
                ServerCreateBuilder builder = Builders.server()
                        .name("name")
                        .image(o.get("image").toString())
                        .flavor(o.get("flavor").toString());

                int index = 0;
                while (true) {
                    String key = "port" + Integer.toString(index);
                    if (o.containsKey(key)) {
                        builder.addNetworkPort(o.get(key).toString());
                    } else {
                        break;
                    }
                }

                Server server = (Server) builder.build();
                server = osClient.compute().servers().boot((ServerCreate) server);

            } else if (o.get("request").toString().equals("TerminateInstanceRequest")) {
                Server server = client.getServer(o.get("server name").toString());
                osClient.compute().servers().delete(server.getId());
            } else if (o.get("request").toString().equals("AttachVolumeRequest")) {
                ServerServiceImpl serverService = new ServerServiceImpl();
                String volumeId = client.getVolume(o.get("volume name").toString()).getId();
                String serverId = client.getServer(o.get("server name").toString()).getId();

                serverService.attachVolume(serverId, volumeId, o.get("device name").toString());
            } else if (o.get("request").toString().equals("DetachVolumeRequest")) {
                ServerServiceImpl serverService = new ServerServiceImpl();
                String serverId = client.getServer(o.get("server name").toString()).getId();
                String attachmentId = o.get("attachment id").toString();

                serverService.detachVolume(serverId, attachmentId);
            } else if (o.get("request").toString().equals("AttachPortRequest")) {
                InterfaceServiceImpl portService = new InterfaceServiceImpl();
                String serverId = client.getServer(o.get("server name").toString()).getId();
                String portId = client.getPort(o.get("port name").toString()).getId();

                portService.create(serverId, portId);
            } else if (o.get("request").toString().equals("DetachPortRequest")) {
                InterfaceServiceImpl portService = new InterfaceServiceImpl();
                String serverId = client.getServer(o.get("server name").toString()).getId();
                String portId = client.getPort(o.get("port name").toString()).getId();

                portService.detach(serverId, portId);
            }

        }

    }

    /**
     * *****************************************************************
     * Function to create a Vpc from a modelRef
     * /*****************************************************************
     */
    private List<JSONObject> networkRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check to see if any operations involves network creation/deletion
        query = "SELECT ?network WHERE {?service mrs:providesNetwork  ?network ."
                + "?network a nml:Topology}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode network = querySolution.get("network");
            String networkName = network.asResource().toString().replace(topologyUri, "");
            Network net = client.getNetwork(networkName);

            //1.1 see if the operation desired is valid
            if (net != null ^ creation) // if network  exists, no need to create it
            {
                if (creation == true) {
                    throw new Exception(String.format("Network %s already exists", network));
                } else {
                    throw new Exception(String.format("Network %s does not exists", network));
                }
            } else {
                //1.1 make sure root topology has the newtork
                query = "SELECT ?cloud WHERE {?cloud nml:hasTopology <" + network.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the openStack-cloud that"
                            + "provides network : %s", network));
                }
                //1.2 find the tag of the network
                query = "SELECT ?tag {<" + network.asResource() + "> mrs:hasTag ?tag}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("network %s does  ot have a tag", network));
                }
                QuerySolution q1 = r1.next();
                RDFNode networkTag = q1.get("tag");
                //1.2.1 check that tag is of the appropiate type and the the value
                query = "SELECT ?value WHERE {<" + networkTag.asResource() + "> mrs:type \"network\" ."
                        + "<" + networkTag.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, modelRef, emptyModel);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("network %s has improper type of tag", network));
                }
                q1 = r1.next();
                String networkTagValue = q1.get("value").asLiteral().toString();

                //1.3 check taht network offers switching service
                query = "SELECT ?service  WHERE {<" + network.asResource() + "> nml:hasService  ?service ."
                        + "?service a  mrs:SwitchingService}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("New network %s does not speicfy Switching Service", network));
                }
                
                //1.4 TODO if the netwrk is external, make sure it has the route to connect

                JSONObject o = new JSONObject();
                if (creation == true) {
                    o.put("request", "CreateNetworkRequests");
                } else {
                    o.put("request", "DeleteNetworkRequests");
                }
                o.put("name", networkName);
            }
        }
        return requests;
    }

    /**
     * *****************************************************************
     * Function to create a subnets from a modelRef
     * ***************************************************************
     */
    private List<JSONObject> subnetRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check if there is any subnet to create or delete
        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode subnet = q.get("subnet");
            String subnetName = subnet.asResource().toString().replace(topologyUri, "");
            Subnet s = client.getSubnet(subnetName);

            //1.1 make sure that the operation that wants to be done is valid
            if (s == null ^ creation) //subnet  exists,does not need to create one
            {
                if (creation == true) {
                    throw new Exception(String.format("Subnet %s already exists", subnet));
                } else {
                    throw new Exception(String.format("Subnet %s does not exist, cannot be deleted", subnet));
                }
            } else {
                //1.2 check the subnet is being provided a service and get the service
                String subnetId = s.getId();
                query = "SELECT ?service {?service mrs:providesSubnet <" + subnet.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("No service has subnet %s", subnet));
                }
                QuerySolution q1 = r1.next();
                RDFNode service = q1.get("service");

                //1.3 check that the service is part of a network and get the network
                query = "SELECT ?network {?network nml:hasService <" + service.asResource() + "> ."
                        + "<" + service.asResource() + "> a mrs:SwitchingService}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Subnet %s does not belong a network", subnet));
                }
                q1 = r1.next();
                RDFNode network = q1.get("network");
                String networkName = network.asResource().toString().replace(topologyUri, "");
                //1.3.1 gte the tag of the network 
                query = "SELECT ?tag {<" + network.asResource() + "> mrs:hasTag ?tag}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("network %s does  not have a tag", subnet));
                }
                q1 = r1.next();
                RDFNode networkTag = q1.get("tag");
                //1.3.1.1 check that tag is of the appropiate type and the the value
                query = "SELECT ?value WHERE {<" + networkTag.asResource() + "> mrs:type \"network\" ."
                        + "<" + networkTag.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, modelRef, emptyModel);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("network %s for subnet  %s has improper type of tag", network, subnet));
                }
                q1 = r1.next();
                String networkTagValue = q1.get("value").asLiteral().toString();

                //1.4 check the subnet has a tag and get the tag
                query = "SELECT ?tag {<" + subnet.asResource() + "> mrs:hasTag ?tag}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Subnet %s does  ot have a tag", subnet));
                }
                q1 = r1.next();
                RDFNode tag = q1.get("tag");
                //1.4.1 check that tag is of the appropiate type
                query = "SELECT ?value WHERE {<" + tag.asResource() + "> mrs:type \"subnet\" ."
                        + "<" + tag.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, modelRef, emptyModel);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Subnet %s has improper type of tag", subnet));
                }
                q1 = r1.next();
                String subnetTagValue = q1.get("value").asLiteral().toString();

                //1.5 check that a public subnet is not being created in a private network or viceversa
                if (networkTagValue.equals("tenant") && subnetTagValue.equals("public")) {
                    throw new Exception(String.format("public subnet %s cannot be in tenant network "
                            + "network %s", subnet, network));
                }
                if ((networkTagValue.equals("external") && subnetTagValue.equals("private"))) {
                    throw new Exception(String.format("private subnet %s cannot be in external network "
                            + "network %s", subnet, network));
                }

                //get the netwokr addresses of the subnet
                query = "SELECT ?subnet ?address ?value WHERE {<" + subnet.asResource() + "> mrs:hasNetworkAddress ?address ."
                        + "?address a mrs:NetworkAddress ."
                        + "?address mrs:type \"ipv4-prefix\" ."
                        + "?address mrs:value ?value}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Subnet %s does nto specify network address", subnet));
                }
                q1 = r1.next();
                RDFNode value = q1.get("value");
                String cidrBlock = value.asLiteral().toString();
                //check if the subnet has a gateway ip, it is an optional parameter]
                String gatewayIp = "";
                query = "SELECT ?subnet ?address ?value WHERE {<" + subnet.asResource() + "> mrs:hasNetworkAddress ?address ."
                        + "?address a mrs:NetworkAddress ."
                        + "?address mrs:type \"gateway-ip\" ."
                        + "?address mrs:value ?value}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (r1.hasNext()) {
                    q1 = r1.next();
                    value = q1.get("value");
                    gatewayIp = value.asLiteral().toString();
                }

                JSONObject o = new JSONObject();
                if (creation == true) {
                    o.put("request", "CreateSubnetRequest");
                } else {
                    o.put("request", "DeleteSubnetRequest");
                }
                o.put("network name", networkName);
                o.put("cidr block", cidrBlock);
                o.put("name", subnetName);
                o.put("gateway ip", gatewayIp);
                requests.add(o);
            }
        }
        return requests;
    }

    /**
     * *****************************************************************
     * Function to create/delete a volumes from a modelRef
     * ***************************************************************
     */
    private List<JSONObject> volumeRequests(OntModel modelRef, OntModel model, boolean creation) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check if any operation needs to be done with a volume
        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        ResultSet r = executeQuery(query, emptyModel, model);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeName = volume.asResource().toString().replace(topologyUri, "");

            Volume v = client.getVolume(volumeName);

            //1.1 check if desired operagtion can be done
            if (v == null ^ creation) //volume exists, no need to create a volume
            {
                if (creation == true) {
                    throw new Exception(String.format("Volume %s already exists", v));
                } else {
                    throw new Exception(String.format("Volume %s does not exist, cannot be deleted", v));
                }
            } else {
                //1.2 check what service is providing the volume
                query = "SELECT ?service WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model does not specify service that provides volume: %s", volume));
                }
                QuerySolution q1 = r1.next();
                RDFNode service = q1.get("service");

                //1.3 check that service is a block sotrage service
                query = "SELECT ?type WHERE {<" + service.asResource() + ">  a mrs:BlockStorageService}";
                r1 = executeQuery(query, modelRef, emptyModel);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Service %s is not a block storage service", service));
                }

                //1.4 find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new type of volume: %s", volume));
                }
                q1 = r1.next();
                RDFNode type = q1.get("type");

                //1.5 find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new size of volume: %s", volume));
                }
                q1 = r1.next();
                RDFNode size = q1.get("size");

                //1.6 create the request
                JSONObject o = new JSONObject();
                if (creation == true) {
                    o.put("request", "CreateVolumeRequest");
                } else {
                    o.put("request", "DeleteVolumeRequest");
                }
                o.put("type", type.asLiteral().toString());
                o.put("size", size.asLiteral().toString());
                o.put("availabilty zone", "nova");
                o.put("name", volumeName);
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
    private List<JSONObject> portRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 get the tag resource from the reference model that indicates 
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

        //2 select all the ports in the reference model that have that tag
        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort ."
                + "?port  mrs:hasTag <" + tag.asResource() + ">}";
        r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portName = port.asResource().toString().replace(topologyUri, "");

            Port p = client.getPort(portName);

            //2.1 make sure that the desired operation is valid
            if (p == null ^ creation) //network interface  exists, no need to create a network interface
            {
                if (creation == true) {
                    throw new Exception(String.format("Network interface %s already exists", portName));
                } else {
                    throw new Exception(String.format("Network interface %s does not exist, cannot be deleted", portName));
                }
            } else {
                //2.2to get the private ip of the network interface
                query = "SELECT ?address ?value WHERE {<" + port.asResource() + ">  mrs:hasNetworkAddress  ?address ."
                        + "?address mrs:type \"ipv4:private\" ."
                        + "?address mrs:value ?value }";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Delta Model does not specify privat ip address of port: %s", port));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String privateAddress = value.asLiteral().toString();

                //2.3 find the subnet that has the port previously found
                query = "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Delta model does not specify network interface subnet of port: %s", port));
                }
                String subnetName = "";
                while (r1.hasNext()) {
                    querySolution1 = r1.next();
                    RDFNode subnet = querySolution1.get("subnet");
                    query = "SELECT ?subnet WHERE {<" + subnet.asResource() + ">  a  mrs:SwitchingSubnet}";
                    ResultSet r3 = executeQuery(query, modelRef, modelDelta);
                    if (r3.hasNext()) //search in the model to see if subnet existed before
                    {
                        subnetName = subnet.asResource().toString().replace(topologyUri, "");
                    } else {
                        throw new Exception(String.format("Subnet  for port %s"
                                + "is not found in any model", port.asResource()));
                    }
                }

                //2.4 make the request
                JSONObject o = new JSONObject();
                if (creation == true) {
                    o.put("request", "CreateNetworkInterfaceRequest");
                } else {
                    o.put("request", "DeleteNetworkInterfaceRequest");
                }

                o.put("private address", privateAddress);
                o.put("subnet name", subnetName);
                o.put("port name", portName);
                requests.add(o);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to attach (if not on server creation) or detach a port from a
     * server ****************************************************************
     */
    private List<JSONObject> portAttachmentRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check for any addition of a port into a device or subnet
        query = "SELECT ?node ?port WHERE {?node nml:hasBidirectionalPort ?port}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode port = q.get("port");
            RDFNode server = q.get("node");
            String serverName = server.asResource().toString().replace(topologyUri, "");

            //1.1 get the server name, if no server is found, it means the port is not being attached to a server
            //so we will just skip this iteration
            //we are also checking for the existance of the server, if the server does not exist
            //the ports will be attached during creation, not done by this method
            query = "SELECT ?node WHERE {<" + server.asResource() + "> a nml:Node}";
            ResultSet r1 = executeQuery(query, modelRef, emptyModel);
            Server s = null;
            if (r1.hasNext()) {
                s = client.getServer(serverName);
                r1.next();
                String portName = port.asResource().toString().replace(topologyUri, "");

                //1.2 check that the port has a tag
                query = "SELECT ?tag WHERE {<" + port.asResource() + "> mrs:hasTag ?tag}";
                ResultSet r2 = executeQuery(query, modelRef, modelDelta);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("bidirectional port %s to be attached to intsnace does not specify a tag", port));
                }
                QuerySolution q2 = r2.next();
                RDFNode tag = q2.get("tag");

                //1.3 check that the port has the correct tag
                query = "SELECT ?tag WHERE {<" + tag.asResource() + "> mrs:type \"interface\". "
                        + "<" + tag.asResource() + "> mrs:value \"network\"}";
                r2 = executeQuery(query, modelRef, emptyModel);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("bidirectional port %s to be attached to instance is not a net"
                            + "work interface", port));
                }

                //1.4 create the request
                JSONObject o = new JSONObject();
                Port p = client.getPort(portName);
                //1.4.1 port attachment will be added
                if (creation == true) {
                    //1.4.1.1 see if the network interface is already atatched
                    if (p.getDeviceOwner() != null || !p.getDeviceOwner().isEmpty()) {
                        throw new Exception(String.format("bidirectional port %s to be attached to instance %s is already"
                                + " attached to an instance", port, serverName));
                    }

                    o.put("request", "AttachPortRequest");
                    o.put("port name", portName);
                    o.put("server name", serverName);
                    requests.add(o);
                } //1.4.2 port attachment will be deleted
                else {
                    if (p.getDeviceOwner() != null || !p.getDeviceOwner().isEmpty()) {
                        throw new Exception(String.format("bidirectional port %s to be detached from instance %s is not"
                                + " attached", port, serverName));
                    }
                    o.put("request", "DetachPortRequest");
                    o.put("port name", portName);
                    o.put("server name", serverName);
                    requests.add(o);
                }
            }

        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to request or delete an instance
     * ****************************************************************
     */
    private List<JSONObject> serverRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check for any operation involving a server
        query = "SELECT ?server WHERE {?server a nml:Node}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode vm = q.get("server");
            String serverName = vm.asResource().toString().replace(topologyUri, "");
            Server server = client.getServer(serverName);

            //1.1 check if the desired operation is a valid operation
            if (server == null ^ creation) //check if server needs to be created or deleted
            {
                if (creation == true) {
                    throw new Exception(String.format("Server %s already exists", serverName));
                } else {
                    throw new Exception(String.format("Server %s does not exist, cannot be deleted", serverName));
                }
            } else {
                //1.2 check what service is providing the instance
                query = "SELECT ?service WHERE {?service mrs:providesVM <" + vm.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Dleta model does not specify service that provides the VM: %s", vm));
                }
                QuerySolution q1 = r1.next();
                RDFNode hypervisorService = q1.get("service");
                String hyperVisorServiceName = hypervisorService.asResource().toString().replace(topologyUri, "");

                //1.3 check that service is a hypervisor service
                query = "SELECT ?type WHERE {<" + hypervisorService.asResource() + "> a mrs:BlockStorageService}";
                r1 = executeQuery(query, modelRef, emptyModel);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Service %s is not a hypervisor service", hypervisorService));
                }

                //1.4 find the host of the VM
                query = "SELECT ?node WHERE {?node nml:hasService <" + hypervisorService.asResource() + ">}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Delta model does not specify host that provides service %s", hypervisorService));
                }
                q1 = r1.next();
                RDFNode host = q1.get("host");
                String hostName = host.asResource().toString().replace(topologyUri, "");

                //1.5 make sure that the host is a node
                query = "SELECT ?node WHERE {<" + host.asResource() + "> a nml:Node}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Host %s to host node %s is not of type nml:Node", host, vm));
                }

                //1.6 find the network that the server will be in
                query = "SELECT ?network WHERE {?network nml:hasNode <" + host.asResource() + ">}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("VM %s does not specify network", vm));
                }
                q1 = r1.next();
                RDFNode network = q1.get("network");
                String networkName = network.asResource().toString().replace(topologyUri, "");

                //1.7 to find the subnet the server is in first  find the port the server uses
                query = "SELECT ?port WHERE {<" + vm.asResource() + "> nml:hasBidirectionalPort ?port}";
                ResultSet r2 = executeQuery(query, modelRef, modelDelta);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("Vm %s does not specify the attached network interface", vm));
                }
                List<String> portNames = new ArrayList();
                while (r2.hasNext())//there could be multiple network interfaces attached to the instance
                {
                    QuerySolution q2 = r2.next();
                    RDFNode port = q2.get("port");
                    String name = port.asResource().toString().replace(topologyUri, "");
                    portNames.add(name);
                }

                //1.8 find the EBS volumes that the instance uses
                query = "SELECT ?volume WHERE {<" + vm.asResource() + ">  mrs:hasVolume  ?volume}";
                ResultSet r4 = executeQuery(query, emptyModel, modelDelta);
                if (!r4.hasNext()) {
                    throw new Exception(String.format("Delta model does not specify the volume of the new vm: %s", vm));
                }
                List<String> volumeNames = new ArrayList();
                while (r4.hasNext())//there could be multiple volumes attached to the instance
                {
                    QuerySolution q4 = r4.next();
                    RDFNode volume = q4.get("volume");
                    String name = volume.asResource().toString().replace(topologyUri, "");
                    volumeNames.add(name);
                }

                //1.9 put the root device of the instance
                query = "SELECT ?volume ?deviceName ?size ?type  WHERE {"
                        + "<" + vm.asResource() + ">  mrs:hasVolume  ?volume ."
                        + "?volume mrs:target_device ?deviceName ."
                        + "?volume mrs:disk_gb ?size ."
                        + "?volume mrs:value ?type}";
                r4 = executeQuery(query, modelRef, modelDelta);
                boolean hasRootVolume = false;
                String volumeName = "";
                while (r4.hasNext()) {
                    QuerySolution q4 = r4.next();
                    RDFNode volume = q4.get("volume");
                    volumeName = volume.asResource().toString().replace(topologyUri, "");
                    String deviceName = q4.get("deviceName").asLiteral().toString();
                    if (deviceName.equals("/dev/")) {
                        hasRootVolume = true;
                    }
                }
                if (hasRootVolume == false) {
                    throw new Exception(String.format("model addition does not specify root volume for node: %s", vm));
                }

                //1.10 create the request
                JSONObject o = new JSONObject();
                if (creation == true) {
                    o.put("request", "RunInstanceRequest");
                } else {
                    o.put("request", "TerminateInstanceRequest");
                }

                o.put("server name", serverName);
                o.put("image", "54f6673b-f39f-461b-886e-dbe4f4497fd5");
                o.put("flavor", volumeName);

                //1.10.1 put all the ports in the request
                int index = 0;
                for (String port : portNames) {
                    String key = "port" + Integer.toString(index);
                    o.put(key, port);
                    index++; //increment the device index
                }
                requests.add(o);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to attach or detach volume to an instance
     * ****************************************************************
     */
    private List<JSONObject> volumesAttachmentRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check for new association between intsnce and volume
        query = "SELECT  ?node ?volume  WHERE {?node  mrs:hasVolume  ?volume}";

        ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode server = querySolution1.get("node");
            String serverName = server.asResource().toString().replace(topologyUri, "");
            RDFNode volume = querySolution1.get("volume");
            String volumeName = volume.asResource().toString().replace(topologyUri, "");

            //1.1 find the device name of the volume
            query = "SELECT ?deviceName WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName}";
            ResultSet r2 = executeQuery(query, modelRef, modelDelta);
            if (!r2.hasNext()) {
                throw new Exception(String.format("volume device name is not specified for volume %s in the model delta", volume));
            }
            QuerySolution querySolution2 = r2.next();
            RDFNode deviceName = querySolution2.get("deviceName");
            String device = deviceName.asLiteral().toString();

            //1.2 make sure is not the root device
            if (!device.equals("/dev/")) {
                Server s = client.getServer(serverName);

                Volume vol = client.getVolume(volumeName);
                //1.3 if the volume does not exist already, make sure all parameters are wll specified
                if (vol == null) {
                    query = "SELECT ?deviceName ?size ?type WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName ."
                            + "<" + volume.asResource() + "> mrs:type ?type ."
                            + "<" + volume.asResource() + "> mrs:size ?size }";
                    r2 = executeQuery(query, modelRef, modelDelta);
                    if (!r2.hasNext()) {
                        throw new Exception(String.format("volume %s is not well specified in volume delta", volume));
                    }
                }
                //1.4 if s is not null 
                if (s != null) {
                    List<String> map = s.getOsExtendedVolumesAttached();
                    if (vol == null) {
                        if (creation == false) {
                            throw new Exception(String.format("volume %s to be detached does not exist", volumeName));
                        } else {
                            JSONObject o = new JSONObject();
                            o.put("request", "AttachVolumeRequest");
                            o.put("server name", serverName);
                            o.put("volume name", volumeName);
                            o.put("device name", device);
                            requests.add(o);
                        }
                    } else {
                        if (creation == true) {
                            if (!map.contains(vol.getId())) {
                                JSONObject o = new JSONObject();
                                o.put("request", "AttachVolumeRequest");
                                o.put("server name", serverName);
                                o.put("volume name", volumeName);
                                o.put("device name", device);
                                requests.add(o);
                            } else {
                                throw new Exception(String.format("volume %s is already attached to"
                                        + " server %s", volumeName, serverName));
                            }
                        } else if (creation == false) {
                            if (map.contains(vol.getId())) {
                                JSONObject o = new JSONObject();
                                o.put("request", "DetachVolumeRequest");
                                o.put("server name", serverName);
                                List<? extends VolumeAttachment> att = vol.getAttachments();
                                for (VolumeAttachment a : att) {
                                    if (a.getId().equals(s.getId())) {
                                        o.put("attachment id", a.getId());
                                    }
                                }
                                s.getOsExtendedVolumesAttached();
                                requests.add(o);
                            } else {
                                throw new Exception(String.format("volume %s is not attached to"
                                        + " server %s", volumeName, serverName));
                            }
                        }
                    }

                } else if (s == null) {
                    if (creation == true) {
                        JSONObject o = new JSONObject();
                        o.put("request", "AttachVolumeRequest");
                        o.put("server name", serverName);
                        o.put("volume name", volumeName);
                        o.put("device name", device);
                        requests.add(o);
                    } else {
                        throw new Exception(String.format("server %s where the volume %s will be"
                                + "detached does not exists", serverName, volumeName));
                    }
                }
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to add and delete routes to the model. This function might
     * attach/detach a port to a router,set/clear gateway routers create/delete
     * subnet/router host routes. Depending on what route it is, it will do any
     * of the previous commits. If the
     * ****************************************************************
     */
    private List<JSONObject> layer3Requests(OntModel modelRef, OntModel modelDelta, boolean creation) throws Exception {
        List<JSONObject> requests = new ArrayList();
        String query = "";

        //1 find out if any new routes are being add to the model
        query = "SELECT ?route ?nextHop ?routeTo WHERE {?route a mrs:Route ."
                + "?route mrs:nextHop ?nextHop ."
                + "?route mrs:routeTo ?routeTo}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode routeResource = q.get("route");
            String nextHop = q.get("nextHop").toString();
            RDFNode routeToResource = q.get("routeTo");

            //1.1 check that the route was model correctly
            //1.1.1 make sure that service provides the route
            query = "SELECT ?service WHERE {?service mrs:providesRoute <" + routeResource.asResource() + "}";
            ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                throw new Exception(String.format("route %s is not provided"
                        + "by any service", routeResource));
            }
            QuerySolution q1 = r1.next();
            RDFNode service = q1.get("type");
            //1.1.2 make sure service is well specified in the model
            query = "SELECT ?x WHERE{<"+service.asResource()+"> a mrs:RoutingService}";
            r1 = executeQuery(query,modelRef,emptyModel);
            if (!r1.hasNext()) {
                throw new Exception(String.format("Sercive %s is not a routing service",service));
            }
            //1.1.3 get the route Table of the route
            //TODO make sure to skip the loop if the route is the external network route
            query = "SELECT ?table WHERE {?table mrs:hasRoute <" + routeResource.asResource() + "}";
            r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                throw new Exception(String.format("route %S is not in any route table",routeResource));
            }
            q1 = r1.next();
            RDFNode tableResource = q1.get("table");
            //1.1.4 make sure the table is a routing table 
            query = "SELECT ?x WHERE{<"+tableResource.asResource()+"> a mrs:RoutingTable}";
            r1 = executeQuery(query,modelRef,modelDelta);
            if (!r1.hasNext()) {
                throw new Exception(String.format("tbale %s is not a routing table",tableResource));
            }
            
            //1.2make sure routeTo is well formed
            query = "SELECT ?type ?value WHERE {<" + routeToResource + "> a mrs:NetworkAddress ."
                    + "<" + routeToResource + "> mrs:type ?type ."
                    + "<" + routeToResource + "> mrs:value ?value}";
            r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                throw new Exception(String.format("routeTo %s for route %s is "
                        + "malformed", routeToResource, routeResource));
            }
            q1 = r1.next();
            RDFNode routeToType = q1.get("type");
            RDFNode routeToValue = q1.get("value");

            //2 find if there is a routeFrom statement in the route 
            query = "SELECT ?routeFrom WHERE{<" + routeResource.asResource() + "> mrs:routeFrom ?routeFrom}";
            r1 = executeQuery(query, emptyModel, modelDelta);
            //2.1 if there is, it means that the route is a router or subnet
            //host route 
            while (r1.hasNext()) {
                //2.1.1 make sure the routeFrom statement is well formed
                q1 = r1.next();
                RDFNode routeFromResource = q1.get("routeFrom");
                query = "SELECT ?type ?value WHERE {<" + routeFromResource + "> a mrs:NetworkAddress ."
                        + "<" + routeFromResource + "> mrs:type ?type ."
                        + "<" + routeFromResource + "> mrs:value ?value}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("routeTo %s for route %s is "
                            + "malformed", routeToResource, routeResource));
                }
                q1 = r1.next();
                RDFNode routeFromType = q1.get("type");
                RDFNode routeFromcvalue = q1.get("value");

                //2.1.2 TODO differentiate between subnet host route or router host route
            }
            
            //3.1 because the route did not have a route to, it means that an 
            //interface will be attached or dettached
            String toValue = routeToValue.asLiteral().toString();
            String toType = routeToType.asLiteral().toString();
            //if type equals subnet, then a port will be atatched or detached
            //from subnet
            if(toType.equals("subnet"))
            {
                //3.1.1 
                
                
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
