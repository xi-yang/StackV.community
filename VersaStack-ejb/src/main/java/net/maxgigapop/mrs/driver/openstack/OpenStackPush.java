/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor. ....
 */
package net.maxgigapop.mrs.driver.openstack;

import com.amazonaws.services.ec2.model.Instance;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.service.compute.MCE_InterfaceVlanStitching;
import org.json.simple.JSONObject;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.*;
import org.openstack4j.model.network.builder.RouterBuilder;
import org.openstack4j.model.storage.block.*;
import org.openstack4j.openstack.compute.domain.NovaInterfaceAttachment;
import org.openstack4j.openstack.compute.domain.NovaServer;
import org.openstack4j.openstack.compute.internal.ServerServiceImpl;
import org.openstack4j.openstack.compute.internal.ext.InterfaceServiceImpl;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;
import org.openstack4j.openstack.networking.internal.PortServiceImpl;
import org.openstack4j.openstack.networking.internal.RouterServiceImpl;
import org.openstack4j.openstack.storage.block.domain.CinderVolume;

/**
 *
 * @author muzcategui zwang126
 */
/**
 * **********************************************************
 *
 * TODO 1) figure out how the root devices work in openStack in order to
 * create/delete a volume 2) Add and delete an object to the reference of OS
 * client for local reference
 * **********************************************************
 */
public class OpenStackPush {

    private static final Logger log = Logger.getLogger(OpenStackPush.class.getName());

    //global variables
    private OpenStackGet client = null;
    private OpenStackGet client1 = null;
    private OSClient osClient = null;
    private OSClient osClient1 = null;
    public OpenStackGet openstackget = null;
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    private String topologyUri;

    /*public static void main(String[] args) {
     OpenStackPush test = new OpenStackPush();

     }*/
    public OpenStackPush(String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        client = new OpenStackGet(url, NATServer, username, password, tenantName);
        openstackget = new OpenStackGet(url, NATServer, username, password, tenantName);
        osClient = client.getClient();

        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
        topologyUri = topologyUri.replaceAll("[^A-Za-z0-9()_-]", "_");
    }

    private void OpenStackPushupdate(String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        client1 = new OpenStackGet(url, NATServer, username, password, tenantName);
        openstackget = new OpenStackGet(url, NATServer, username, password, tenantName);
        osClient1 = client1.getClient();
    }

    /**
     * ***********************************************
     * Method to get the requests provided in the model addition and model
     * reduction ************************************************
     */
    public List<JSONObject> propagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) throws EJBException {
        List<JSONObject> requests = new ArrayList();

        //get all the requests
        requests.addAll(portAttachmentRequests(modelRef, modelReduct, false));
        requests.addAll(volumesAttachmentRequests(modelRef, modelReduct, false));
        requests.addAll(volumeRequests(modelRef, modelReduct, false));
        requests.addAll((portRequests(modelRef, modelReduct, false)));
        requests.addAll(subnetRequests(modelRef, modelReduct, false));
        requests.addAll(networkRequests(modelRef, modelReduct, false));
        requests.addAll(serverRequests(modelRef, modelReduct, false));
        requests.addAll(layer3Requests(modelRef, modelReduct, false));
        requests.addAll(networkRequests(modelRef, modelAdd, true));
        requests.addAll(subnetRequests(modelRef, modelAdd, true));
        requests.addAll(volumeRequests(modelRef, modelAdd, true));
        requests.addAll((portRequests(modelRef, modelAdd, true)));
        requests.addAll(volumesAttachmentRequests(modelRef, modelAdd, true));
        requests.addAll(portAttachmentRequests(modelRef, modelAdd, true));
        requests.addAll(serverRequests(modelRef, modelAdd, true));
        requests.addAll(layer3Requests(modelRef, modelAdd, true));
        return requests;
    }

    /**
     * **********************************************************************
     * Function to do execute all the requests provided by the propagate method
     * **********************************************************************
     */
    public void pushCommit(List<JSONObject> requests, String url, String NATServer, String username, String password, String tenantName, String topologyUri) throws InterruptedException {
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

            } else if (o.get("request").toString().equals("CreateNetworkRequests")) {

                Network network = new NeutronNetwork();
                network.toBuilder().name(o.get("name").toString())
                        .tenantId("3cf2d992f604479dbcb1a6c679c6697a")
                        .adminStateUp(true);//hard code here
                osClient.networking().network().create(network);

            } else if (o.get("request").toString().equals("CreateSubnetRequest")) {
                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                String netid = null;
                Subnet subnet = new NeutronSubnet();
                /*
                 if (client1.getNetworks().) {
                 Network network = new NeutronNetwork();
                 network.toBuilder().name(o.get("network name").toString())
                 .tenantId("3cf2d992f604479dbcb1a6c679c6697a")
                 .adminStateUp(true);
                 netid = network.getId();
                 osClient.networking().network().create(network);

                 }
                 */
                //System.out.println(openstackget.getNetwork(o.get("network name").toString()).toString());

                subnet.toBuilder().cidr(o.get("cidr block").toString())
                        //.network(client.getNetwork(o.get("network name").toString()))
                        .network(client1.getNetwork(o.get("network name").toString()))
                        .name(o.get("name").toString())
                        .ipVersion(IPVersionType.V4);
                String gatewayIp = o.get("gateway ip").toString();
                if (!gatewayIp.isEmpty()) {
                    subnet.toBuilder().gateway(gatewayIp);
                }

                osClient.networking().subnet().create(subnet);

            } else if (o.get("request").toString().equals("DeleteSubnetRequest")) {
                Subnet net = client.getSubnet(o.get("name").toString());

                osClient.networking().subnet().delete(net.getId());
            } else if (o.get("request").toString().equals("DeleteNetworkRequests")) {
                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                Network network = client1.getNetwork(o.get("name").toString());
                for (Port p : client1.getPorts()) {
                    if (p.getNetworkId().equals(network.getId())) {
                        throw new EJBException(("port" + p.getId() + "is still attached to the network, so network" + network.getName() + "cannot be deleted"));
                    }
                }
                osClient.networking().network().delete(network.getId());
            } else if (o.get("request").toString().equals("RunInstanceRequest")) {
                ServerCreateBuilder builder = Builders.server()
                        .name(o.get("server name").toString())
                        .image("c9cc8be0-82de-490d-a5b4-a094a66e9b11")
                        .flavor("42");

                int index = 0;
                String portid = "";
                while (true) {
                    String key = "port" + Integer.toString(index);
                    if (o.containsKey(key)) {
                        OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                        for (Port p : client1.getPorts()) {  //here need to be careful

                            if (client1.getResourceName(p).equals(o.get(key).toString())) {
                                portid = p.getId();
                            }
                        }
                        builder.addNetworkPort(portid);
                        index++;
                    } else {
                        break;
                    }
                }

                ServerCreate server = (ServerCreate) builder.build();
                Server s = osClient.compute().servers().boot(server);

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
            } else if (o.get("request").toString().equals("CreateRotingInfoRequest")) {
                String routerName = "";
                String routerid = "";
                String netid = "";
                String subnet_name = "";
                String router_name = "";
                RouterServiceImpl rsi = new RouterServiceImpl();
                HashMap<String, HashMap<String, String>> routing_info_for_router = new HashMap<String, HashMap<String, String>>();
                router_name = "";
                //routerName = o.get("router name").toString();
                int k = 0;
                int i = 0;
                int x = 0;
                int j = 0;
                String key_routinginfo = "routing_info";
                routing_info_for_router = (HashMap<String, HashMap<String, String>>) o.get(key_routinginfo);
                //check the multiple routers condition, enter the while loop
                while (true) {
                    String key_router = "router" + Integer.toString(x);
                    if (o.containsKey(key_router)) {
                        k = 0;
                        i = 0;
                        j = 0;
                        boolean created = false;
                        //if the router is not in the openstack, create one
                        if (!client.getRouters().contains(client.getRouter(o.get(key_router).toString()))) {

                            Router router = osClient.networking().router().create((Builders.router()
                                    .name(o.get(key_router).toString())
                                    .adminStateUp(true)
                                    .build()));
                            OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);

                            //update the client
                            //multiple subnet and nexthop create once a time, same concept of the router one
                            while (true) {

                                String key_ip = "nexthop" + Integer.toString(j);

                                HashMap<String, String> routing_info1 = new HashMap<String, String>();

                                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);

                                if (o.containsKey(key_ip)) {
                                    for (Router router1 : client1.getRouters()) {

                                        if (routing_info_for_router.containsKey(router1.getName()) || routing_info_for_router.containsKey(router1.getId())) {
                                            router_name = router1.getName();
                                            routing_info1 = routing_info_for_router.get(router1.getName());
                                            if (routing_info1.containsKey(o.get(key_ip).toString())) {
                                                String sub_router = routing_info1.get(o.get(key_ip).toString());

                                                String[] sub_route1 = sub_router.split(",");
                                                subnet_name = sub_route1[0];
                                                router_name = sub_route1[1];
                                                if (router_name.equals(router1.getName())) {
                                                    Subnet s = client1.getSubnet(subnet_name);
                                                    Router r = client1.getRouter(router_name);

                                                    if (!o.get(key_ip).toString().contains("any")) {

                                                        String nexthop = o.get(key_ip).toString();
                                                        String router_id = r.getId();
                                                        Port port = new NeutronPort();
                                                        netid = s.getNetworkId();
                                                        String subnetid = s.getId();

                                                        port.toBuilder().networkId(netid)
                                                                .fixedIp(nexthop, subnetid)
                                                                .name("router_name" + router_name + "test_use" + i)
                                                                .adminState(true);

                                                        osClient1.networking().port().create(port);
                                                        OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                                                        String portid = client1.getPort("router_name" + router_name + "test_use" + i).getId();
                                                        rsi.attachInterface(router_id, AttachInterfaceType.PORT, portid);
                                                        i++;
                                                        j++;
                                                        key_ip = "nexthop" + Integer.toString(j);
                                                    } else {
                                                        String nexthop = o.get(key_ip).toString();
                                                        String router_id = r.getId();
                                                        Port port = new NeutronPort();
                                                        netid = s.getNetworkId();
                                                        String subnetid = s.getId();

                                                        port.toBuilder().networkId(netid)
                                                                .name("router_name" + router_name + "test_use" + i)
                                                                .adminState(true);

                                                        osClient1.networking().port().create(port);
                                                        OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                                                        String portid = client1.getPort("router_name" + router_name + "test_use" + i).getId();
                                                        rsi.attachInterface(router_id, AttachInterfaceType.PORT, portid);
                                                        i++;
                                                        j++;
                                                        key_ip = "nexthop" + Integer.toString(j);
                                                    }

                                                }
                                            } else {
                                                j++;
                                                key_ip = "nexthop" + Integer.toString(j);
                                            }

                                        }
                                    }

                                } else {
                                    break;
                                }

                            }

                        } else {
                            OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);

                            //update the client
                            //multiple subnet and nexthop create once a time, same concept of the router one
                            while (true) {

                                String key_ip = "nexthop" + Integer.toString(j);

                                if (o.containsKey(key_routinginfo)) {
                                    HashMap<String, String> routing_info1 = new HashMap<String, String>();

                                    OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                                    if (o.containsKey(key_ip)) {
                                        for (Router router1 : client1.getRouters()) {
                                            if (routing_info_for_router.containsKey(router1.getName()) || routing_info_for_router.containsKey(router1.getId())) {

                                                routing_info1 = routing_info_for_router.get(router1.getName());

                                                String sub_router = routing_info1.get(o.get(key_ip).toString());

                                                String[] sub_route1 = sub_router.split(",");
                                                subnet_name = sub_route1[0];
                                                router_name = sub_route1[1];

                                                Subnet s = client1.getSubnet(subnet_name);
                                                Router r = client1.getRouter(router_name);
                                                String nexthop = o.get(key_ip).toString();
                                                String router_id = r.getId();
                                                Port port = new NeutronPort();
                                                netid = s.getNetworkId();
                                                String subnetid = s.getId();

                                                port.toBuilder().networkId(netid)
                                                        .fixedIp(nexthop, subnetid)
                                                        .name("router_name" + routerName + "test_use" + i)
                                                        .adminState(true);

                                                osClient1.networking().port().create(port);
                                                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                                                String portid = client1.getPort("router_name" + routerName + "test_use" + i).getId();
                                                rsi.attachInterface(router_id, AttachInterfaceType.PORT, portid);

                                            }
                                        }
                                        j++;
                                    }

                                    k++;

                                } else {
                                    break;
                                }
                            }
                        }
                        routing_info_for_router.remove(router_name);
                        x++;
                    } else {
                        break;
                    }

                }

            } else if (o.get("request").toString().equals("CreateNetworkInterfaceRequest")) {
                Port port = new NeutronPort();
                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);

                Subnet subnet = null;
                for (Subnet sn : client1.getSubnets()) {
                    if (sn.getName().equals(o.get("subnet name").toString())) {
                        subnet = sn;
                    }

                }
                if (subnet == null) {
                    throw new EJBException("unknown subnet:" + o.get("subnet name"));
                }
                if (o.get("private address").toString().equals("any")) {
                    port.toBuilder().name(o.get("port name").toString())
                            .networkId(subnet.getNetworkId());
                } else {
                    port.toBuilder().name(o.get("port name").toString())
                            .fixedIp(o.get("private address").toString(), subnet.getId())
                            .networkId(subnet.getNetworkId());
                }
                osClient.networking().port().create(port);
            } else if (o.get("request").toString().equals("DeleteNetworkInterfaceRequest")) {
                Port port = client.getPort(o.get("port name").toString());
                osClient.networking().port().delete(port.getId());

            } else if (o.get("request").toString().equals("DeleteRotingInfoRequest")) {
                HashMap<String, HashMap<String, String>> routing_info_for_router = new HashMap<String, HashMap<String, String>>();
                String key_routinginfo = "routing_info";
                routing_info_for_router = (HashMap<String, HashMap<String, String>>) o.get(key_routinginfo);
                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                int i = 0;
                int j = 0;
                int x = 0;
                String routerid = "";

                while (true) {
                    String key_router = "router" + Integer.toString(x);
                    String key_ip = "nexthop" + Integer.toString(j);
                    if (o.containsKey(key_router)) {
                        j = 0;
                        key_ip = "nexthop" + Integer.toString(j);
                        while (o.containsKey(key_ip)) {
                            Router r = client1.getRouter(o.get(key_router).toString());
                            HashMap<String, String> routing_info1 = new HashMap<String, String>();
                            routing_info1 = routing_info_for_router.get(client1.getResourceName(r));
                            if (routing_info1.containsKey(o.get(key_ip).toString())) {
                                String sub_router = routing_info1.get(o.get(key_ip).toString());

                                String[] sub_route1 = sub_router.split(",");
                                String subnet_name = sub_route1[0];
                                String router_name = sub_route1[1];

                                Subnet s = client1.getSubnet(subnet_name);
                                Router r1 = client1.getRouter(router_name);
                                routerid = r1.getId();
                                String subid = s.getId();

                                osClient.networking().router().detachInterface(routerid, subid, null);
                                j++;
                                key_ip = "nexthop" + Integer.toString(j);

                                ArrayList<Boolean> arr = new ArrayList<Boolean>();
                                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                                for (Port p : client1.getPorts()) {

                                    if (p.getDeviceId().equals(routerid)) {
                                        arr.add(Boolean.TRUE);
                                    } else {
                                        arr.add(Boolean.FALSE);
                                    }
                                }
                                if (!arr.contains(Boolean.TRUE)) {
                                    osClient.networking().router().delete(routerid);

                                }
                            } else {
                                j++;
                                key_ip = "nexthop" + Integer.toString(j);
                            }

                            //os.networking().router()
                            //.detachInterface("routerId", "subnetId", null);
                        }
                        x++;

                    } else {
                        break;
                    }

                }

            } else if (o.get("request").toString().equals("CreateHostInfoRequest")) {
                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                String subnetname = o.get("subnetname").toString();
                String nexthop = o.get("nexthop").toString();
                String routeto = o.get("routeto").toString();
                Subnet s = client1.getSubnet(subnetname);
                s.toBuilder().addHostRoute(nexthop, routeto);
                osClient.networking().subnet().update(s);
            } else if (o.get("request").toString().equals("DeleteHostInfoRequest")) {
                OpenStackPushupdate(url, NATServer, username, password, tenantName, topologyUri);
                String subnetname = o.get("subnetname").toString();
                String nexthop = o.get("nexthop").toString();
                String routeto = o.get("routeto").toString();
                Subnet s = client1.getSubnet(subnetname);
                System.out.println("There is currently no way to delete the host route through api");
            }

        }

    }

    /**
     * *****************************************************************
     * Function to create a Vpc from a modelRef
     * /*****************************************************************
     */
    private List<JSONObject> networkRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check to see if any operations involves network creation/deletion
        int index = topologyUri.lastIndexOf(":");
        String topologyuri = topologyUri.substring(0, index);

        query = "SELECT ?network WHERE {?openstack nml:hasTopology ?network ."
                + "?network a nml:Topology "
                + String.format("FILTER(?openstack = <%s>) }", topologyuri);
        ResultSet r = executeQuery(query, emptyModel, modelDelta);

        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode network = querySolution.get("network");
            String NetworkName = network.asResource().toString();
            String networkName = getresourcename(NetworkName, "+", "");
            Network net = client.getNetwork(networkName);

            //1.1 see if the operation desired is valid
            if (net == null ^ creation) // if network  exists, no need to create it .......some error here neeed to be fixed
            {
                if (creation == true) {
                    throw new EJBException(String.format("Network %s already exists", network));
                } else {
                    throw new EJBException(String.format("Network %s does not exists", network));
                }
            } else {
                //1.1 make sure root topology has the newtork

                query = "SELECT ?cloud WHERE {?cloud nml:hasTopology <" + network.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify the openStack-cloud that"
                            + "provides network : %s", network));
                }

                //1.2 find the tag of the network
                query = "SELECT ?tag {<" + network.asResource() + "> mrs:hasTag ?tag}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("network %s does  ot have a tag", network));
                }
                QuerySolution q1 = r1.next();
                RDFNode networkTag = q1.get("tag");
                //1.2.1 check that tag is of the appropiate type and the the value
                query = "SELECT ?value WHERE {<" + networkTag.asResource() + "> mrs:type \"network-type\" ."
                        + "<" + networkTag.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("network %s has improper type of tag", network));
                }
                q1 = r1.next();
                String networkTagValue = q1.get("value").asLiteral().toString();

                //1.3 check taht network offers switching service
                query = "SELECT ?service  WHERE {<" + network.asResource() + "> nml:hasService  ?service ."
                        + "?service a  mrs:SwitchingService}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("New network %s does not speicfy Switching Service", network));
                }

                //1.4 TODO if the netwrk is external, make sure it has the route to connect
                JSONObject o = new JSONObject();
                if (creation == true) {
                    o.put("request", "CreateNetworkRequests");
                } else {
                    o.put("request", "DeleteNetworkRequests");
                }
                o.put("name", networkName);
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
    private List<JSONObject> subnetRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check if there is any subnet to create or delete
        query = "SELECT ?service ?subnet WHERE {?service a mrs:SwitchingService ."
                + "?service mrs:providesSubnet ?subnet}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        // System.out.println(modelDelta.toString());
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode subnet = q.get("subnet");
            String subnetname = subnet.asResource().toString();
            String subnetName = getresourcename(subnetname, "+", "");
            Subnet s = client.getSubnet(subnetName);

            //1.1 make sure that the operation that wants to be done is valid
            if (s == null ^ creation) //subnet  exists,does not need to create one
            {
                if (creation == true) {
                    throw new EJBException(String.format("Subnet %s already exists", subnet));
                } else {
                    throw new EJBException(String.format("Subnet %s does not exist, cannot be deleted", subnet));
                }
            } else {
                //1.2 check the subnet is being provided a service and get the service
                //String subnetId = s.getId();
                query = "SELECT ?service {?service mrs:providesSubnet <" + subnet.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("No service has subnet %s", subnet));
                }
                QuerySolution q1 = r1.next();
                RDFNode service = q1.get("service");

                //1.3 check that the service is part of a network and get the network
                query = "SELECT ?network {?network nml:hasService <" + service.asResource() + "> ."
                        + "<" + service.asResource() + "> a mrs:SwitchingService}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Subnet %s does not belong a network", subnet));
                }
                q1 = r1.next();
                RDFNode network = q1.get("network");
                String networkname = network.asResource().toString();
                String networkName = getresourcename(networkname, "+", "");
                //1.3.1 gte the tag of the network 
                query = "SELECT ?tag {<" + network.asResource() + "> mrs:hasTag ?tag}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("network %s does  not have a tag", subnet));
                }
                q1 = r1.next();
                RDFNode networkTag = q1.get("tag");
                //1.3.1.1 check that tag is of the appropiate type and the the value
                query = "SELECT ?value WHERE {<" + networkTag.asResource() + "> mrs:type \"network-type\" ."
                        + "<" + networkTag.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("network %s for subnet  %s has improper type of tag", network, subnet));
                }
                q1 = r1.next();
                String networkTagValue = q1.get("value").asLiteral().toString();

                //1.4 check the subnet has a tag and get the tag
                query = "SELECT ?tag {<" + subnet.asResource() + "> mrs:hasTag ?tag}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Subnet %s does  ot have a tag", subnet));
                }
                q1 = r1.next();
                RDFNode tag = q1.get("tag");
                //1.4.1 check that tag is of the appropiate type
                query = "SELECT ?value WHERE {<" + tag.asResource() + "> mrs:type \"subnet-type\" ."
                        + "<" + tag.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Subnet %s has improper type of tag", subnet));
                }
                q1 = r1.next();
                String subnetTagValue = q1.get("value").asLiteral().toString();

                //1.5 check that a public subnet is not being created in a private network or viceversa
                if (networkTagValue.equals("tenant") && subnetTagValue.equals("public")) {
                    throw new EJBException(String.format("public subnet %s cannot be in tenant network "
                            + "network %s", subnet, network));
                }
                if ((networkTagValue.equals("external") && subnetTagValue.equals("private"))) {
                    throw new EJBException(String.format("private subnet %s cannot be in external network "
                            + "network %s", subnet, network));
                }

                //get the netwokr addresses of the subnet
                query = "SELECT ?subnet ?address ?value WHERE {<" + subnet.asResource() + "> mrs:hasNetworkAddress ?address ."
                        + "?address a mrs:NetworkAddress ."
                        + "?address mrs:type \"ipv4-prefix\" ."
                        + "?address mrs:value ?value}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Subnet %s does nto specify network address", subnet));
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
    private List<JSONObject> volumeRequests(OntModel modelRef, OntModel model, boolean creation) throws EJBException {
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
                    throw new EJBException(String.format("Volume %s already exists", v));
                } else {
                    throw new EJBException(String.format("Volume %s does not exist, cannot be deleted", v));
                }
            } else {
                //1.2 check what service is providing the volume
                query = "SELECT ?service WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model does not specify service that provides volume: %s", volume));
                }
                QuerySolution q1 = r1.next();
                RDFNode service = q1.get("service");

                //1.3 check that service is a block sotrage service
                query = "SELECT ?type WHERE {<" + service.asResource() + ">  a mrs:BlockStorageService}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Service %s is not a block storage service", service));
                }

                //1.4 find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify new type of volume: %s", volume));
                }
                q1 = r1.next();
                RDFNode type = q1.get("type");

                //1.5 find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, modelRef, model);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify new size of volume: %s", volume));
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
    private List<JSONObject> portRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 get the tag resource from the reference model that indicates 
        //that this is a network  interface 
        query = "SELECT ?tag WHERE {?tag mrs:type \"interface\" ."
                + "?tag mrs:value \"network\"}";
        ResultSet r = executeQuery(query, modelRef, modelDelta);
        if (!r.hasNext()) {
            throw new EJBException(String.format("Reference model has no tags for network"
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
            String portname = port.asResource().toString();
            String portName = getresourcename(portname, "+", "");
            Port p = client.getPort(portName);

            //2.1 make sure that the desired operation is valid
            if (p == null ^ creation) //network interface  exists, no need to create a network interface
            {
                if (creation == true) {
                    throw new EJBException(String.format("Network interface %s already exists", portName));
                } else {
                    throw new EJBException(String.format("Network interface %s does not exist, cannot be deleted", portName));
                }
            } else {
                //2.2to get the private ip of the network interface
                query = "SELECT ?address ?value WHERE {<" + port.asResource() + ">  mrs:hasNetworkAddress  ?address ."
                        + "?address mrs:type \"ipv4:private\" ."
                        + "?address mrs:value ?value }";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                String privateAddress = "any";
                if (r1.hasNext()) {

                    QuerySolution querySolution1 = r1.next();
                    RDFNode value = querySolution1.get("value");
                    privateAddress = value.asLiteral().toString();
                }
                //2.3 find the subnet that has the port previously found
                //query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort ."
                // + "?port  mrs:hasTag <" + tag.asResource() + ">}";
                query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet. ?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">}";
                r1 = executeQueryUnion(query, modelDelta, modelRef);//
                //System.out.println(modelDelta.toString());
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Delta model does not specify network interface subnet of port: %s", port));
                }
                String subnetName = "";
                String subnetname = "";
                while (r1.hasNext()) {
                    QuerySolution querySolution1 = r1.next();
                    RDFNode subnet = querySolution1.get("subnet");

                    /*
                     query = "SELECT ?subnet WHERE {?subnet  a  mrs:SwitchingSubnet}"
                     + String.format("FILTER (?subnet = <%s>)", subnet.asResource())
                     + "}";
                     */
                    //query = "SELECT ?subnet WHERE {?subnet  a  mrs:SwitchingSubnet}";
                    //ResultSet r3 = executeQuery(query, modelRef, modelDelta);
                    subnetname = subnet.asResource().toString();
                    subnetName = getresourcename(subnetname, "+", "");
                    break;
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
    private List<JSONObject> portAttachmentRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check for any addition of a port into a device or subnet
        //some error here
        query = "SELECT ?node ?port WHERE {?node nml:hasBidirectionalPort ?port}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode port = q.get("port");
            RDFNode server = q.get("node");
            String servername = server.asResource().toString();
            String serverName = getresourcename(servername, "+", "");

            //1.1 get the server name, if no server is found, it means the port is not being attached to a server
            //so we will just skip this iteration
            //we are also checking for the existance of the server, if the server does not exist
            //the ports will be attached during creation, not done by this method
            //here is an error
            query = "SELECT ?node WHERE {?node a nml:Node. FILTER(?node = <" + server.asResource() + ">)}";
            // System.out.println(query.toString());
            ResultSet r1 = executeQuery(query, modelRef, modelDelta);
            Server s = null;
            if (r1.hasNext()) {
                s = client.getServer(serverName);
                r1.next();
                String portname = port.asResource().toString();
                String portName = getresourcename(portname, "+", "");

                //1.2 check that the port has a tag
                query = "SELECT ?tag WHERE {<" + port.asResource() + "> mrs:hasTag ?tag}";
                ResultSet r2 = executeQuery(query, modelRef, modelDelta);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("bidirectional port %s to be attached to intsnace does not specify a tag", port));
                }
                QuerySolution q2 = r2.next();
                RDFNode tag = q2.get("tag");

                //1.3 check that the port has the correct tag
                query = "SELECT ?tag WHERE {<" + tag.asResource() + "> mrs:type \"interface\". "
                        + "<" + tag.asResource() + "> mrs:value \"network\"}";
                r2 = executeQuery(query, modelRef, modelDelta);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("bidirectional port %s to be attached to instance is not a net"
                            + "work interface", port));
                }

                //1.4 create the request
                JSONObject o = new JSONObject();
                Port p = client.getPort(portName);
                //1.4.1 port attachment will be added
                if (creation == true) {
                    //1.4.1.1 see if the network interface is already atatched
                    if (p.getDeviceOwner() != null || !p.getDeviceOwner().isEmpty()) {
                        throw new EJBException(String.format("bidirectional port %s to be attached to instance %s is already"
                                + " attached to an instance", port, serverName));
                    }

                    o.put("request", "AttachPortRequest");
                    o.put("port name", portName);
                    o.put("server name", serverName);
                    requests.add(o);
                } //1.4.2 port attachment will be deleted
                else {
                    if (p.getDeviceOwner() == null || p.getDeviceOwner().isEmpty()) {
                        throw new EJBException(String.format("bidirectional port %s to be detached from instance %s is not"
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
    //query has error
    private List<JSONObject> serverRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check for any operation involving a server
        query = "SELECT ?server ?port WHERE {?server a nml:Node .}";
        ResultSet r = executeQuery(query, modelDelta, emptyModel);//here modified 

        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode vm = q.get("server");
            String servername = vm.asResource().toString();
            String serverName = getresourcename(servername, "+", "");
            Server server = client.getServer(serverName);

            //1.1 check if the desired operation is a valid operation
            if (server == null ^ creation) //check if server needs to be created or deleted
            {
                if (creation == true) {
                    throw new EJBException(String.format("Server %s already exists", serverName));
                } else {
                    throw new EJBException(String.format("Server %s does not exist, cannot be deleted", serverName));
                }
            } else {
                //1.2 check what service is providing the instance
                query = "SELECT ?service WHERE {?service mrs:providesVM <" + vm.asResource() + ">}";
                ResultSet r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Dleta model does not specify service that provides the VM: %s", vm));
                }
                QuerySolution q1 = r1.next();
                RDFNode hypervisorService = q1.get("service");
                String hyperVisorServiceName = hypervisorService.asResource().toString().replace(topologyUri, "");//need to change here

                //1.3 check that service is a hypervisor service
                query = "SELECT ?type WHERE { ?type a mrs:HypervisorService}";//modified here
                r1 = executeQuery(query, modelRef, modelDelta);//here may error
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Service %s is not a hypervisor service", hypervisorService));
                }

                //1.4 find the host of the VM
                query = "SELECT ?host WHERE {?host nml:hasService <" + hypervisorService.asResource() + ">}";//the deltamodel
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Delta model does not specify host that provides service %s", hypervisorService));
                }
                q1 = r1.next();
                RDFNode host = q1.get("host");
                //String hostName = host.asResource().toString().replace(topologyUri, "");

                //1.5 make sure that the host is a node
                //query = "SELECT ?node WHERE {?node a nml:Node. FILTER(?node = <" + server.asResource() + ">)}";
                query = "SELECT ?node WHERE {?node a nml:Node. FILTER(?node = <" + host.asResource() + ">)}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Host %s to host node %s is not of type nml:Node", host, vm));
                }

                //1.6 find the network that the server will be in
                //query = "SELECT ?node WHERE {?node a nml:Node. FILTER(?node = <" + server.asResource() + ">)}";
                query = "SELECT ?subnet ?port WHERE {?subnet a mrs:SwitchingSubnet ."
                        + "?subnet nml:hasBidirectionalPort ?port}";
                r1 = executeQuery(query, modelRef, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("VM %s does not specify network", vm));
                }
                q1 = r1.next();
                RDFNode subnet = q1.get("subnet");
                String subnetName = subnet.asResource().toString().replace(topologyUri, "");
                //find the port
                query = "SELECT ?port WHERE {<" + subnet.asResource() + "> nml:hasBidirectionalPort ?port}";
                ResultSet r5 = executeQuery(query, modelRef, modelDelta);
                if (!r5.hasNext()) {
                    throw new EJBException(String.format("Vm %s does not specify the attached network interface", vm));
                }

                //1.7 to find the subnet the server is in first  find the port the server uses
                query = "SELECT ?port WHERE {<" + vm.asResource() + "> nml:hasBidirectionalPort ?port}";
                ResultSet r2 = executeQuery(query, modelRef, modelDelta);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("Vm %s does not specify the attached network interface", vm));
                }
                List<String> portNames = new ArrayList();
                while (r2.hasNext())//there could be multiple network interfaces attached to the instance
                {
                    QuerySolution q2 = r2.next();
                    RDFNode port = q2.get("port");
                    String Name = port.asResource().toString();
                    String name = getresourcename(Name, "+", "");
                    portNames.add(name);
                }
                /*
                 //1.8 find the EBS volumes that the instance uses
                 query = "SELECT ?volume WHERE {<" + vm.asResource() + ">  mrs:hasVolume  ?volume}";
                 ResultSet r4 = executeQuery(query, emptyModel, modelDelta);
                 if (!r4.hasNext()) {
                 throw new EJBException(String.format("Delta model does not specify the volume of the new vm: %s", vm));
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
                 String volumename = "";
                 while (r4.hasNext()) {
                 QuerySolution q4 = r4.next();
                 RDFNode volume = q4.get("volume");
                 volumename = volume.asResource().toString();
                 volumeName = getresourcename(volumename, "+", "");
                 String deviceName = q4.get("deviceName").asLiteral().toString();
                 if (deviceName.equals("/dev/")) {
                 hasRootVolume = true;
                 }
                 }
                 if (hasRootVolume == false) {
                 throw new EJBException(String.format("model addition does not specify root volume for node: %s", vm));
                 }
                 */
                //1.10 create the request
                JSONObject o = new JSONObject();
                if (creation == true) {
                    o.put("request", "RunInstanceRequest");
                } else {
                    o.put("request", "TerminateInstanceRequest");
                }

                o.put("server name", serverName);
                o.put("image", "c9cc8be0-82de-490d-a5b4-a094a66e9b11");
                o.put("flavor", "42");

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
    private List<JSONObject> volumesAttachmentRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check for new association between intsnce and volume
        query = "SELECT  ?node ?volume  WHERE {?node  mrs:hasVolume  ?volume}";

        ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode server = querySolution1.get("node");
            String servername = server.asResource().toString();
            String serverName = getresourcename(servername, "+", "");
            RDFNode volume = querySolution1.get("volume");
            String volumeName = volume.asResource().toString().replace(topologyUri, "");

            //1.1 find the device name of the volume
            query = "SELECT ?deviceName WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName}";
            ResultSet r2 = executeQuery(query, modelRef, modelDelta);
            if (!r2.hasNext()) {
                throw new EJBException(String.format("volume device name is not specified for volume %s in the model delta", volume));
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
                        throw new EJBException(String.format("volume %s is not well specified in volume delta", volume));
                    }
                }
                //1.4 if s is not null 
                if (s != null) {
                    List<String> map = s.getOsExtendedVolumesAttached();
                    if (vol == null) {
                        if (creation == false) {
                            throw new EJBException(String.format("volume %s to be detached does not exist", volumeName));
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
                                throw new EJBException(String.format("volume %s is already attached to"
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
                                throw new EJBException(String.format("volume %s is not attached to"
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
                        throw new EJBException(String.format("server %s where the volume %s will be"
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
    private List<JSONObject> layer3Requests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        int x = 0;
        int z = 0;
        String query = "";
        String routername = "";
        List<String> nextHopV = new ArrayList<String>();
        List<String> routetoName = new ArrayList<String>();
        List<String> Router = new ArrayList<String>();
        List<String> lr = new ArrayList<String>();
        ArrayList<HashMap<String, String>> routing_info = new ArrayList<HashMap<String, String>>();
        HashMap<String, ArrayList<HashMap<String, String>>> routing_info_for_router = new HashMap<String, ArrayList<HashMap<String, String>>>();
        HashMap<String, HashMap<String, String>> routing_info_for_router1 = new HashMap<String, HashMap<String, String>>();
        HashMap<String, String> routinginfo = new HashMap<String, String>();
        JSONObject o = new JSONObject();
        //1 find out if any new routes are being add to the model
        query = "SELECT ?route ?nextHop ?routeTo WHERE {?route a mrs:Route ."
                + "?route mrs:nextHop ?nextHop ."
                + "?route mrs:routeTo ?routeTo}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode routeResource = q.get("route");
            RDFNode nextHopResource = q.get("nextHop");
            RDFNode routeToResource = q.get("routeTo");
            if (!nextHopResource.toString().equals("local")) {

                //1.1 check that the route was model correctly
                //1.1.1 make sure that service provides the route
                query = "SELECT ?routingtable WHERE {?routingtable mrs:hasRoute <" + routeResource.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);

                if (!r1.hasNext()) {
                    throw new EJBException(String.format("route %s is not provided"
                            + "by any routingtable", routeResource));
                }

                QuerySolution q1 = r1.next();

                RDFNode routingtable = q1.get("routingtable");
                String routingtablename = routingtable.toString();
                routername = getroutername(topologyUri, routingtablename);

                Router.add(routername);

                query = "SELECT ?service WHERE {?service mrs:providesRoutingTable <" + routingtable.asResource() + ">}";
                ResultSet r2 = executeQueryUnion(query, modelRef, modelDelta);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("routingtalbe %s is not provided"
                            + "by any service", routingtable));
                }
                QuerySolution q2 = r2.next();
                RDFNode service = q2.get("service");

                String routename = routeResource.toString();

                lr.add(routename);

                Map<String, List<String>> hp = new HashMap<>();
                hp.put(routername, lr);

                String subnet = getresourcename(routeToResource.toString(), "+", "");
                String subnet_routername = subnet + "," + routername;
                /*
                 query = "SELECT ?service WHERE {?service mrs:providesRoute <" + routeResource.asResource() + ">}";
                 r2 = executeQuery(query, emptyModel, modelDelta);
                 if (!r2.hasNext()) {
                 throw new EJBException(String.format("route %s is not provided"
                 + "by any service", routeResource));
                 }
                 */
                query = "SELECT ?type ?value WHERE {<" + nextHopResource.asResource() + "> a mrs:NetworkAddress ."
                        + "<" + nextHopResource.asResource() + "> mrs:type ?type ."
                        + "<" + nextHopResource.asResource() + "> mrs:value ?value}";

                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("nexthop %s for route %s is "
                            + "malformed", nextHopResource, routeResource));
                }

                q1 = r1.next();
                RDFNode nextHoptype = q1.get("type");
                String nextHopvalue = q1.get("value").toString();

                if (nextHopvalue.equals("any")) {
                    nextHopvalue = nextHopvalue + String.valueOf(z);
                    z++;
                }

                nextHopV.add(nextHopvalue);

                //String subnet_routername_nexthop = subnet + "," + routername + "," + nextHopvalue ;
                routinginfo.put(nextHopvalue, subnet_routername);

                routing_info.add(routinginfo);

                //routing_info_for_router.put(routername, routing_info);
                routing_info_for_router1.put(routername, routinginfo);
                //1.1.2 make sure service is well specified in the model
                //1.1.3 get the route Table of the route
                //TODO make sure to skip the loop if the route is the external network route
            /*
                 
                 */
                //1.2make sure routeTo is well formed
                query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet FILTER(?subnet = <" + routeToResource + ">).}";

                r1 = executeQuery(query, modelRef, modelDelta);

                if (!r1.hasNext()) {
                    throw new EJBException(String.format("routeTo %s for route %s is "
                            + "malformed", routeToResource, routeResource));
                }
                //while (r1.hasNext()) {
                q1 = r1.next();
                RDFNode routetosubnet = q1.get("subnet");
                String routeTosubnet = routetosubnet.toString();
                String subnetname = getresourcename(routeTosubnet, "+", "");
                routetoName.add(subnetname);
                //}
                //next hop information

                //}
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
                        throw new EJBException(String.format("routeTo %s for route %s is "
                                + "malformed", routeToResource, routeResource));
                    }
                    q1 = r1.next();
                    RDFNode routeFromType = q1.get("type");
                    RDFNode routeFromcvalue = q1.get("value");

                    //2.1.2 TODO differentiate between subnet host route or router host route
                }

                /*
                 
                 */
                //3.1.1 
                Router ro = client.getRouter(routername);
                if (creation == true) {
                    o.put("request", "CreateRotingInfoRequest");

                } else {
                    o.put("request", "DeleteRotingInfoRequest");

                }

                int index = 0;
                for (String subnet1 : routetoName) {
                    String key = "subnet" + Integer.toString(index);
                    o.put(key, subnet1);

                    index++;
                }
                int index1 = 0;
                for (String nexthop : nextHopV) {
                    String key = "nexthop" + Integer.toString(index1);
                    o.put(key, nexthop);
                    index1++;
                }
                int index2 = 0;
                //Because the routers name is find in the routing table, here need to remove the duplicates
                LinkedHashSet<String> routers = new LinkedHashSet<String>(Router);
                ArrayList<String> Routers = new ArrayList<String>(routers);
                for (String router : Routers) {

                    String key1 = "router" + Integer.toString(index2);
                    String key2 = "routing_info" + Integer.toString(index2);
                    //routing_info_for_router.put(router, routing_info);
                    /*
                     for(HashMap<String,String> rm :routing_info_for_router.get(router)){
                     for(String nexth : nextHopV){
                     String rou_name = rm.get(nexth).split(",")[1];
                     if(rou_name.equals(router)){
                     ....
                     }
                     }
                     }
                     */
                    o.put(key1, router);

                    index2++;

                }

            }
        }
        if (Router.size() != 0) {
            LinkedHashSet<String> routers = new LinkedHashSet<String>(Router);
            ArrayList<String> Routers = new ArrayList<String>(routers);
            /*
             for (String rou : Routers) {
             ArrayList<HashMap<String, String>> routing_info1 = new ArrayList<HashMap<String, String>>();
             ArrayList<HashMap<String, String>> routing_info2 = new ArrayList<HashMap<String, String>>();
             routing_info1 = routing_info_for_router.get(rou);
             for (HashMap<String, String> hp3 : routing_info1) {
             for (String n : nextHopV) {
             if (hp3.containsKey(n)) {
             String router_name_1 = hp3.get(n).split(",")[1];
             if (!router_name_1.equals(rou)) {
             //routing_info_for_router.remove(rou, routing_info1);
             hp3.remove(n);
             }
             } else {
             break;
             }
             }
             routing_info2.add(hp3);

             }
             routing_info_for_router.put(rou, routing_info2);

             }
             */

            HashMap<String, HashMap<String, String>> routing_info_for_router2 = new HashMap<String, HashMap<String, String>>();
            for (String rou : Routers) {
                HashMap<String, String> routing_info1 = new HashMap<String, String>(routing_info_for_router1.get(rou));

                HashMap<String, String> routing_info2 = new HashMap<String, String>(routing_info1);

                for (String n : nextHopV) {
                    if (routing_info1.containsKey(n)) {
                        String router_name_1 = routing_info1.get(n).split(",")[1];
                        if (!router_name_1.equals(rou)) {
                            //routing_info_for_router.remove(rou, routing_info1);
                            routing_info2.remove(n);
                        }
                    } else {
                        break;
                    }
                }
                //routing_info2.add(hp3);

                routing_info_for_router2.put(rou, routing_info2);

            }

            String key = "routing_info";
            o.put(key, routing_info_for_router2);

        }
        requests.add(o);
        if (o.size() == 0) {
            requests.remove(o);
        }
        return requests;
    }

    private List<JSONObject> hostRouteRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        JSONObject o = new JSONObject();
        String query = "";
        query = "SELECT ?route ?routeFrom ?nextHop ?routeTo WHERE {?route a mrs:Route ."
                + "?route mrs:nextHop ?nextHop ."
                + "?route mrs:routeTo ?routeTo ."
                + "?route mrs:routeFrom ?routeFrom}";

        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        QuerySolution q = r.next();
        while (r.hasNext()) {
            RDFNode routeResource = q.get("route");
            RDFNode nextHopResource = q.get("nextHop");
            RDFNode routeToResource = q.get("routeTo");
            RDFNode routeFromResource = q.get("routeFrom");
            if (!nextHopResource.toString().equals("local")) {
                query = "SELECT ?routingtable WHERE {?routingtable mrs:hasRoute <" + routeResource.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("route %s is not provided"
                            + "by any service", routeResource));
                }

                QuerySolution q1 = r1.next();

                RDFNode routingtable = q1.get("routingtable");
                String routingtablename = routingtable.toString();
                String subnetname = getHostRouteSubname(topologyUri, routingtablename);
                query = "SELECT ?service WHERE {?service mrs:providesRoutingTable <" + routingtable.asResource() + ">}";
                ResultSet r2 = executeQuery(query, emptyModel, modelDelta);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("routingtalbe %s is not provided"
                            + "by any service", routingtable));
                }
                QuerySolution q2 = r2.next();
                RDFNode service = q2.get("service");

                query = "SELECT ?type ?value WHERE {<" + nextHopResource.asResource() + "> a mrs:NetworkAddress ."
                        + "<" + nextHopResource.asResource() + "> mrs:type ?type ."
                        + "<" + nextHopResource.asResource() + "> mrs:value ?value}";

                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("fixip  %s  is "
                            + "malformed", nextHopResource));

                }
                QuerySolution q4 = r1.next();
                RDFNode valUe = q4.get("value");
                String nexthopValue = valUe.toString();

                query = "SELECT ?type ?value WHERE {<" + routeToResource.asResource() + "> a mrs:NetworkAddress ."
                        + "<" + routeToResource.asResource() + "> mrs:type ?type ."
                        + "<" + routeToResource.asResource() + "> mrs:value ?value}";

                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("routeto resource  %s  is "
                            + "malformed", routeToResource));

                }
                QuerySolution q3 = r1.next();
                RDFNode value = q3.get("value");
                String routetoValue = value.toString();

                if (creation == true) {
                    o.put("request", "CreateHostInfoRequest");

                } else {
                    o.put("request", "DeleteHostInfoRequest");

                }
                o.put("subnetname", subnetname);
                o.put("nethop", nexthopValue);
                o.put("routeto", routetoValue);
                requests.add(o);
            }
        }

        return requests;
    }

    private List<JSONObject> isAliasRequest(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query = "";
        query = "SELECT ?fixip ?floatingip WHERE{?fixedip nml:isAlias ?floatingip}";
        ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
        //find the fixip and floatingip
        while (r1.hasNext()) {
            QuerySolution q = r1.next();
            RDFNode fixip = q.get("fixip");
            RDFNode floatingip = q.get("floatingip");
            //find the subnet

            query = "SELECT ?subnet WHERE{?subnet mrs:hasNetworkAddress <" + fixip.asResource() + ">}";
            r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("routeTo %s  is "
                        + "malformed", fixip));

            }
            QuerySolution q1 = r1.next();

            RDFNode subNetfix = q1.get("subnet");
            String subnetNamefix = subNetfix.toString();
            String subnetnamefix = getresourcename(subnetNamefix, "+", "");

            //query subnet for the floating ip
            query = "SELECT ?subnet WHERE{?subnet mrs:hasNetworkAddress <" + floatingip.asResource() + ">}";
            r1 = executeQuery(query, emptyModel, modelDelta);
            while (!r1.hasNext()) {
                throw new EJBException(String.format("routeTo %s  is "
                        + "malformed", fixip));

            }
            QuerySolution q2 = r1.next();

            RDFNode subNetfloat = q2.get("subnet");
            String subnetNamefloat = subNetfloat.toString();
            String subnetnamefloat = getresourcename(subnetNamefloat, "+", "");

            //query for the server
            query = "SELECT ?server WHERE{?subnet mrs:hasNetworkAddress <" + fixip.asResource() + ">}";
            r1 = executeQuery(query, emptyModel, modelDelta);
            while (!r1.hasNext()) {
                throw new EJBException(String.format("routeTo %s  is "
                        + "malformed", fixip));

            }
            QuerySolution q3 = r1.next();
            RDFNode serVer = q3.get("server");
            String serverName = serVer.toString();
            String servername = getresourcename(serverName, "+", "");

            query = "SELECT ?type ?value WHERE {<" + fixip.asResource() + "> a mrs:NetworkAddress ."
                    + "<" + fixip.asResource() + "> mrs:type ?type ."
                    + "<" + fixip.asResource() + "> mrs:value ?value}";

            r1 = executeQuery(query, emptyModel, modelDelta);
            while (!r1.hasNext()) {
                throw new EJBException(String.format("fixip  %s  is "
                        + "malformed", fixip));

            }
            QuerySolution q4 = r1.next();
            RDFNode valUe = q4.get("value");
            String fixvalue = valUe.toString();

            query = "SELECT ?type ?value WHERE {<" + floatingip.asResource() + "> a mrs:NetworkAddress ."
                    + "<" + floatingip.asResource() + "> mrs:type ?type ."
                    + "<" + floatingip.asResource() + "> mrs:value ?value}";

            r1 = executeQuery(query, emptyModel, modelDelta);
            while (!r1.hasNext()) {
                throw new EJBException(String.format("floatingip  %s  is "
                        + "malformed", floatingip));

            }
            QuerySolution q5 = r1.next();
            RDFNode floatvalUe = q5.get("value");
            String floatvalue = floatvalUe.toString();

            JSONObject o = new JSONObject();
            if (creation == true) {
                o.put("request", "CreateisAliaseRequest");
            } else {
                o.put("request", "DeleteisAliaseRequest");
            }
            o.put("subnet name fixip", subnetnamefix);
            o.put("subnet name floatip", subnetnamefloat);
            o.put("server name fixip", servername);
            o.put("fixed ip", fixvalue);
            o.put("float ip", floatvalue);
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

    private static String getresourcename(String resourceName, String character1, String character2) {
        if (resourceName.contains(character1)) {
            if (!character2.isEmpty() || character2 != null) {
                int last1 = resourceName.lastIndexOf(character1);
                int last2 = resourceName.lastIndexOf(character2);
                String name = resourceName.substring(last1, last2).replace(character1, "");

                return name;
            } else {
                int last1 = resourceName.lastIndexOf(character1);
                String name = resourceName.substring(last1);
                String Name = name.replace(character1, "");
                return Name;
            }
        } else {
            return resourceName;
        }
    }

    private String getroutername(String topologyUri, String resourcename) {
        String topologyuri = topologyUri + "router+";
        String resource = resourcename.replace(topologyuri, "");
        int index = resource.indexOf(":");
        return resource.substring(0, index);
    }

    private static String getHostRouteSubname(String topologyUri, String resourcename) {

        String resource = resourcename.replace(topologyUri, "");
        int index = resource.indexOf(":subnet+");
        String middle = resource.substring(index);
        int num = middle.indexOf(":hostroutingtable");

        String res = middle.substring(0, num);
        int num1 = res.indexOf(":subnet+");
        String res_fin = res.substring(num1 + 8);
        return res_fin;
    }

    private ResultSet executeQueryUnion(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        Model unionModel = ModelFactory.createUnion(refModel, model);

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, unionModel);
        ResultSet r = qexec.execSelect();
        return r;
    }

    private boolean isBelongtoSameRouter(Port port1, Port port2, String routerid) {
        Router router = client1.getRouter(routerid);
        if (port1.getDeviceId().equals(router.getId()) && port2.getDeviceId().equals(router.getId())) {
            return true;
        }
        return false;
    }

}
