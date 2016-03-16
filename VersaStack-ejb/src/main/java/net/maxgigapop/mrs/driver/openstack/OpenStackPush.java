/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor. ....
 */
package net.maxgigapop.mrs.driver.openstack;

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
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.ResourceTool;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.identity.TenantService;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.identity.Tenant;
import org.openstack4j.model.network.*;
import org.openstack4j.model.network.builder.RouterBuilder;
import org.openstack4j.model.storage.block.*;
import org.openstack4j.openstack.compute.internal.ServerServiceImpl;
import org.openstack4j.openstack.compute.internal.ext.InterfaceServiceImpl;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;
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
    private OSClient osClient = null;
    private OpenStackGet adminClient = null;
    private OSClient osAdminClient = null;
    private String adminUsername = null;
    private String adminPassword = null;
    private String adminTenant = null;
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    private String topologyUri;
    private String defaultImage;

    private String defaultFlavor;

    /*public static void main(String[] args) {
     OpenStackPush test = new OpenStackPush();

     }*/
    public OpenStackPush(String url, String NATServer, String username, String password, String tenantName, 
        String adminUsername, String adminPassword, String adminTenant, String topologyUri, String defaultImage, String defaultFlavor) {
        client = new OpenStackGet(url, NATServer, username, password, tenantName);
        osClient = client.getClient();
        adminClient = new OpenStackGet(url, NATServer, adminUsername, adminPassword, adminTenant);
        osAdminClient = adminClient.getClient();
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminTenant = adminTenant;
        this.defaultImage = defaultImage;
        this.defaultFlavor = defaultFlavor;
        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
        topologyUri = topologyUri.replaceAll("[^A-Za-z0-9()_-]", "_");
    }

    private void OpenStackGetUpdate(String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        if (adminUsername != null && adminUsername.equals(username)) {
            adminClient = new OpenStackGet(url, NATServer, adminUsername, adminPassword, adminTenant);
            osAdminClient = adminClient.getClient();
        } else {
            client = new OpenStackGet(url, NATServer, username, password, tenantName);
            osClient = client.getClient();            
        }
    }

    /**
     * ***********************************************6
     * Method to get the requests provided in the model addition and model
     * reduction ************************************************
     */
    public List<JSONObject> propagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) throws EJBException {
        List<JSONObject> requests = new ArrayList();

        //get all the requests
        requests.addAll(virtualRouterRequests(modelRef, modelReduct, false));
        requests.addAll(sriovRequests(modelRef, modelReduct, false));
        requests.addAll(portAttachmentRequests(modelRef, modelReduct, false));
        requests.addAll(volumesAttachmentRequests(modelRef, modelReduct, false));
        requests.addAll(volumeRequests(modelRef, modelReduct, false));
        requests.addAll(portRequests(modelRef, modelReduct, false));
        requests.addAll(serverRequests(modelRef, modelReduct, false));
        requests.addAll(layer3Requests(modelRef, modelReduct, false));
        requests.addAll(isAliasRequest(modelRef, modelReduct, false));
        requests.addAll(subnetRequests(modelRef, modelReduct, false));
        requests.addAll(networkRequests(modelRef, modelReduct, false));

        requests.addAll(networkRequests(modelRef, modelAdd, true));
        requests.addAll(subnetRequests(modelRef, modelAdd, true));
        requests.addAll(volumeRequests(modelRef, modelAdd, true));
        requests.addAll(portRequests(modelRef, modelAdd, true));
        requests.addAll(serverRequests(modelRef, modelAdd, true));
        requests.addAll(volumesAttachmentRequests(modelRef, modelAdd, true));
        requests.addAll(portAttachmentRequests(modelRef, modelAdd, true));
        requests.addAll(layer3Requests(modelRef, modelAdd, true));
        requests.addAll(floatingIpRequests(modelRef, modelAdd, true));
        requests.addAll(isAliasRequest(modelRef, modelAdd, true));
        requests.addAll(sriovRequests(modelRef, modelAdd, true));
        requests.addAll(virtualRouterRequests(modelRef, modelAdd, true));

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
                PortCreationCheck(port.getId(), url, NATServer, username, password, tenantName, topologyUri);
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
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                Tenant tenant = osClient.identity().tenants().getByName(tenantName);
                String tenantid = tenant.getId();
                Network network = new NeutronNetwork();
                String network_name = o.get("name").toString();
                network.toBuilder().name(network_name)
                        .tenantId(tenantid)
                        .adminStateUp(true); //hard code here
                osClient.networking().network().create(network);
                NetworkCreationCheck(network_name, url, NATServer, username, password, tenantName, topologyUri);
                if (osClient.networking().network().list().contains(network)) {
                    System.out.println("find it");
                }
            } else if (o.get("request").toString().equals("CreateSubnetRequest")) {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                String netid = null;
                Subnet subnet = new NeutronSubnet();
                String subnet_name = o.get("name").toString();
                Network network1 = client.getNetwork(o.get("network name").toString());
                if (network1 == null) {
                    throw new EJBException("CreateSubnetRequest for" + subnet_name + "failed without network =" + o.get("network name"));
                }
                subnet.toBuilder().cidr(o.get("cidr block").toString())
                        //.network(client.getNetwork(o.get("network name").toString()))
                        .network(network1)
                        .name(subnet_name)
                        .ipVersion(IPVersionType.V4)
                        .enableDHCP(true);
                String gatewayIp = o.get("gateway ip").toString();
                if (!gatewayIp.isEmpty()) {
                    subnet.toBuilder().gateway(gatewayIp);
                }

                osClient.networking().subnet().create(subnet);
                SubnetCreationCheck(subnet_name, url, NATServer, username, password, tenantName, topologyUri);
            } else if (o.get("request").toString().equals("DeleteSubnetRequest")) {
                Subnet net = client.getSubnet(o.get("name").toString());
                String id = net.getId();
                osClient.networking().subnet().delete(net.getId());
                SubnetDeletionCheck(id, url, NATServer, username, password, tenantName, topologyUri);
            } else if (o.get("request").toString().equals("DeleteNetworkRequests")) {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                Network network = client.getNetwork(o.get("name").toString());
                String id = network.getId();
                for (Port p : client.getPorts()) {
                    if (p.getNetworkId().equals(network.getId())) {
                        throw new EJBException(("port" + p.getId() + "is still attached to the network, so network" + network.getName() + "cannot be deleted"));
                    }
                }
                osClient.networking().network().delete(network.getId());
                NetworkDeletionCheck(id, url, NATServer, username, password, tenantName, topologyUri);
            } else if (o.get("request").toString().equals("RunInstanceRequest")) {
                ServerCreateBuilder builder = Builders.server();
                // determine image and flavor
                if (o.get("image").toString().equals("any") && o.get("image").toString().equals("any")) {
                    builder.name(o.get("server name").toString())
                            .image(client.getImages().get(0).getId())
                            .flavor(client.getFlavors().get(0).getId());
                } else if (o.get("image").toString().equals("any")) {

                    builder.name(o.get("server name").toString())
                            .image(client.getImages().get(0).getId())
                            .flavor(o.get("flavor").toString());
                } else if (o.get("flavor").toString().equals("any")) {

                    builder.name(o.get("server name").toString())
                            .image(o.get("image").toString())
                            .flavor(client.getFlavors().get(0).getId());
                } else {
                    builder.name(o.get("server name").toString())
                            .image(o.get("image").toString())
                            .flavor(o.get("flavor").toString());
                }
                // optional keypair 
                if (o.containsKey("keypair") && !o.get("keypair").toString().isEmpty()) {
                    builder.keypairName(o.get("keypair").toString());
                } 
                
                int index = 0;
                while (true) {
                    String key = "port" + Integer.toString(index);
                    if (o.containsKey(key)) {
                        OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                        for (Port p : client.getPorts()) {  //here need to be careful
                            if (client.getResourceName(p).equals(o.get(key).toString())) {
                                builder.addNetworkPort(p.getId());
                                break;
                            }
                        }
                        index++;
                    } else {
                        break;
                    }
                }
                ServerCreate server = (ServerCreate) builder.build();
                Server s = osClient.compute().servers().boot(server);
                String servername = o.get("server name").toString();
                VmCreationCheck(servername, url, NATServer, username, password, tenantName, topologyUri);
                // optional secgroups 
                if (o.containsKey("secgroup") && !o.get("secgroup").toString().isEmpty()) {
                    String[] sgs = o.get("secgroup").toString().split(",|;|:");
                    for (String secgroup : sgs) {
                        SecurityGroupAddCheck(s.getId(), secgroup);
                    }
                }
            } else if (o.get("request").toString().equals("TerminateInstanceRequest")) {
                Server server = client.getServer(o.get("server name").toString());
                osClient.compute().servers().delete(server.getId());
                VmDeletionCheck(server.getId(), url, NATServer, username, password, tenantName, topologyUri);
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
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);

                InterfaceServiceImpl portService = new InterfaceServiceImpl();
                String serverId = client.getServer(o.get("server name").toString()).getId();
                String portId = client.getPort(o.get("port name").toString()).getId();

                //portService.create(serverId, portId);
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

                            if (osClient.networking().network().list().contains(o.get(key_router).toString())) {
                                System.out.println("find it");
                            }

                            OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);

                            //update the client
                            //multiple subnet and nexthop create once a time, same concept of the router one
                            while (true) {

                                String key_ip = "nexthop" + Integer.toString(j);

                                HashMap<String, String> routing_info1 = new HashMap<String, String>();

                                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);

                                if (o.containsKey(key_ip)) {
                                    for (Router router1 : client.getRouters()) {
                                        if (routing_info_for_router.containsKey(router1.getName()) || routing_info_for_router.containsKey(router1.getId())) {
                                            router_name = router1.getName();
                                            routing_info1 = routing_info_for_router.get(router1.getName());
                                            if (routing_info1.containsKey(o.get(key_ip).toString())) {
                                                String sub_router = routing_info1.get(o.get(key_ip).toString());

                                                String[] sub_route1 = sub_router.split(",");
                                                subnet_name = sub_route1[0];
                                                router_name = sub_route1[1];
                                                if (router_name.equals(router1.getName())) {
                                                    Subnet s = client.getSubnet(subnet_name);
                                                    if (s == null && adminClient != null) {
                                                        s = adminClient.getSubnet(subnet_name);
                                                    }
                                                    Router r = client.getRouter(router_name);

                                                    if (!o.get(key_ip).toString().contains("any")) {

                                                        String nexthop = o.get(key_ip).toString();
                                                        String router_id = r.getId();
                                                        Port port = new NeutronPort();
                                                        netid = s.getNetworkId();
                                                        String subnetid = s.getId();
                                                        boolean isNetworkExternal = osClient.networking().network().get(netid).isRouterExternal();
                                                        if (isNetworkExternal == true) {
                                                            osClient.networking().router().update(router.toBuilder().id(router_id).externalGateway(netid).build());
                                                            OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                                                        } else {
                                                            if (client.getPort(router_name + "port" + i) != null) {
                                                                i++;
                                                            }
                                                            try {
                                                                port.toBuilder().networkId(netid)
                                                                        .fixedIp(nexthop, subnetid)
                                                                        .name(router_name + "port" + i)
                                                                        .adminState(true);

                                                                osClient.networking().port().create(port);
                                                                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                                                                String portid = client.getPort(router_name + "port" + i).getId();
                                                                rsi.attachInterface(router_id, AttachInterfaceType.PORT, portid);
                                                                PortCreationCheck(portid, url, NATServer, username, password, tenantName, topologyUri);
                                                            } catch (Exception e) {
                                                                e.toString();
                                                            }
                                                        }
                                                        i++;
                                                        j++;
                                                        key_ip = "nexthop" + Integer.toString(j);
                                                    } else {
                                                        OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                                                        String nexthop = o.get(key_ip).toString();
                                                        String router_id = r.getId();
                                                        netid = s.getNetworkId();
                                                        boolean isNetworkExternal = osClient.networking().network().get(netid).isRouterExternal();
                                                        if (isNetworkExternal == true) {
                                                            osClient.networking().router().update(router.toBuilder().id(router_id).externalGateway(netid).build());
                                                            OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                                                        } else {
                                                            Boolean portCreated = false;
                                                            SubnetUtils info = new SubnetUtils(s.getCidr());
                                                            for (String ip : info.getInfo().getAllAddresses()) {
                                                                try {
                                                                    Port port = new NeutronPort();
                                                                    if (client.getPort(router_name + "port" + i) != null) {
                                                                        i++;
                                                                    }
                                                                    port.toBuilder().name(router_name + "port" + i)
                                                                            .adminState(true)
                                                                            .fixedIp(ip, s.getId())
                                                                            .networkId(netid);
                                                                    client.getClient().networking().port().create(port);
                                                                    PortCreationCheck(port.getName(), url, NATServer, username, password, tenantName, topologyUri);
                                                                    OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                                                                    portCreated = true;
                                                                    break;
                                                                } catch (Exception e) {
                                                                    //do nothing try again with different ip
                                                                    e.toString();
                                                                }
                                                            }
                                                            if (portCreated == false) {
                                                                throw new EJBException(String.format("could not create port %s", router_name + "port" + i));
                                                            }

                                                            String portid = client.getPort(router_name + "port" + i).getId();
                                                            rsi.attachInterface(router_id, AttachInterfaceType.PORT, portid);
                                                        }
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
                            OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                            //update the client
                            //multiple subnet and nexthop create once a time, same concept of the router one
                            while (true) {

                                String key_ip = "nexthop" + Integer.toString(j);

                                HashMap<String, String> routing_info1 = new HashMap<String, String>();

                                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);

                                if (o.containsKey(key_ip)) {
                                    for (Router router1 : client.getRouters()) {

                                        if (routing_info_for_router.containsKey(router1.getName()) || routing_info_for_router.containsKey(router1.getId())) {
                                            router_name = router1.getName();
                                            routing_info1 = routing_info_for_router.get(router1.getName());
                                            if (routing_info1.containsKey(o.get(key_ip).toString())) {
                                                String sub_router = routing_info1.get(o.get(key_ip).toString());

                                                String[] sub_route1 = sub_router.split(",");
                                                subnet_name = sub_route1[0];
                                                router_name = sub_route1[1];
                                                if (router_name.equals(router1.getName())) {
                                                    Subnet s = client.getSubnet(subnet_name);
                                                    Router r = client.getRouter(router_name);
                                                    if (!o.get(key_ip).toString().contains("any")) {

                                                        String nexthop = o.get(key_ip).toString();
                                                        String router_id = r.getId();
                                                        Port port = new NeutronPort();
                                                        netid = s.getNetworkId();
                                                        String subnetid = s.getId();
                                                        if (client.getPort(router_name + "port" + i) != null) {
                                                            i++;
                                                        }
                                                        port.toBuilder().networkId(netid)
                                                                .fixedIp(nexthop, subnetid)
                                                                .name(router_name + "port" + i)
                                                                .adminState(true);
                                                        osClient.networking().port().create(port);
                                                        OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                                                        String portid = client.getPort(router_name + "port" + i).getId();
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
                                                        if (client.getPort(router_name + "port" + i) != null) {
                                                            i++;
                                                        }
                                                        port.toBuilder().networkId(netid)
                                                                .name(router_name + "port" + i)
                                                                .adminState(true);

                                                        osClient.networking().port().create(port);
                                                        OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                                                        String portid = client.getPort(router_name + "port" + i).getId();
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
                        }
                        routing_info_for_router.remove(router_name);
                        x++;
                    } else {
                        break;
                    }
                }
            } else if (o.get("request").toString().equals("CreateNetworkInterfaceRequest")) {
                Port port = new NeutronPort();
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                String portname = o.get("port name").toString();
                Subnet subnet = null;
                for (Subnet sn : client.getSubnets()) {
                    if (sn.getName().equals(o.get("subnet name").toString())) {
                        subnet = sn;
                        break;
                    }

                }
                if (subnet == null) {
                    throw new EJBException("unknown subnet:" + o.get("subnet name"));
                }
                if (o.get("private address").toString().equals("any")) {
                    port.toBuilder().name(o.get("port name").toString())
                            .fixedIp(null, subnet.getId())
                            .networkId(subnet.getNetworkId());
                } else {
                    port.toBuilder().name(o.get("port name").toString())
                            .fixedIp(o.get("private address").toString(), subnet.getId())
                            .networkId(subnet.getNetworkId());
                }
                osClient.networking().port().create(port);

                PortCreationCheck(portname, url, NATServer, username, password, tenantName, topologyUri);
            } else if (o.get("request").toString().equals("DeleteNetworkInterfaceRequest")) {
                Port port = client.getPort(o.get("port name").toString());
                if (port.getDeviceOwner().equals("")) { //this is for delete port have no device owner, if the port has a device owner, it cannot delete it at here besides it will delete at elsewhere.
                    osClient.networking().port().delete(port.getId());
                }

            } else if (o.get("request").toString().equals("DeleteRotingInfoRequest")) {
                HashMap<String, HashMap<String, String>> routing_info_for_router = new HashMap<String, HashMap<String, String>>();
                String key_routinginfo = "routing_info";
                routing_info_for_router = (HashMap<String, HashMap<String, String>>) o.get(key_routinginfo);
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                int x = 0;
                int j = 0;
                String routerid = "";

                while (true) {
                    String key_router = "router" + Integer.toString(x);
                    String key_ip = "nexthop" + Integer.toString(j);
                    if (o.containsKey(key_router)) {
                        j = 0;
                        key_ip = "nexthop" + Integer.toString(j);
                        while (o.containsKey(key_ip)) {
                            Router r = client.getRouter(o.get(key_router).toString());
                            if (r == null) {
                                break;
                            }
                            HashMap<String, String> routing_info1 = routing_info_for_router.get(client.getResourceName(r));
                            if (routing_info1.containsKey(o.get(key_ip).toString())) {
                                String sub_router = routing_info1.get(o.get(key_ip).toString());
                                String portid = null;
                                String[] sub_route1 = sub_router.split(",");
                                String subnet_name = sub_route1[0];
                                String router_name = sub_route1[1];

                                Subnet s = client.getSubnet(subnet_name);
                                if (s == null && adminClient != null) {
                                    s = adminClient.getSubnet(subnet_name);
                                }
                                Router r1 = client.getRouter(router_name);
                                routerid = r1.getId();
                                String subid = s.getId();

                                if (client.getNetwork(s.getNetworkId()).isRouterExternal()) {
                                    for (Port p : client.getPorts()) {
                                        if (p.getDeviceId().equals(r1.getId()) && p.getNetworkId().equals(s.getNetworkId())) {
                                            portid = p.getId();
                                            break;
                                        }
                                    }
                                    r1.toBuilder().clearExternalGateway();
                                    osClient.networking().router().update(r1);
                                    PortDeletionCheck(portid, url, NATServer, username, password, tenantName, topologyUri);
                                } else {
                                    for (Port p : client.getPorts()) {
                                        if (p.getDeviceId().equals(r1.getId()) && p.getNetworkId().equals(s.getNetworkId())) {
                                            portid = p.getId();
                                            break;
                                        }
                                    }
                                    osClient.networking().router().detachInterface(routerid, subid, null);
                                    PortDeletionCheck(portid, url, NATServer, username, password, tenantName, topologyUri);
                                }
                                ArrayList<Boolean> arr = new ArrayList<Boolean>();
                                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                                //@TODO: should getPorts from adminClient
                                for (Port p : client.getPorts()) {
                                    if (p.getDeviceId().equals(routerid)) {
                                        arr.add(Boolean.TRUE);
                                    } else {
                                        arr.add(Boolean.FALSE);
                                    }
                                }
                                if (!arr.contains(Boolean.TRUE)) {
                                    osClient.networking().router().delete(routerid);
                                }
                            } 
                            j++;
                            key_ip = "nexthop" + Integer.toString(j);
                        }
                        x++;
                    } else {
                        break;
                    }
                }
            } else if (o.get("request").toString().equals("CreateHostInfoRequest")) {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                String subnetname = o.get("subnetname").toString();
                String nexthop = o.get("nexthop").toString();
                String routeto = o.get("routeto").toString();
                Subnet s = client.getSubnet(subnetname);
                s.toBuilder().addHostRoute(nexthop, routeto);
                osClient.networking().subnet().update(s);
            } else if (o.get("request").toString().equals("DeleteHostInfoRequest")) {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                String subnetname = o.get("subnetname").toString();
                String nexthop = o.get("nexthop").toString();
                String routeto = o.get("routeto").toString();
                Subnet s = client.getSubnet(subnetname);
                System.out.println("There is currently no way to delete the host route through api");
            } else if (o.get("request").toString().equals("AssociateFloatingIpRequest")) {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                String servername = o.get("server name").toString();
                String portname = o.get("port name").toString();
                String floatip = o.get("floating ip").toString();
                Server s = client.getServer(servername);
                Port p = client.getPort(portname);
                ActionResponse ar = osClient.compute().floatingIps().addFloatingIP(s, ((IP)p.getFixedIps().toArray()[0]).getIpAddress(), floatip);
            } else if (o.get("request").toString().equals("CreateisAliaseRequest")) {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                String servername = o.get("server name").toString();
                String subnetnamefloat = o.get("subnet name floatip").toString();
                String subnetnamefix = o.get("subnet name fixip").toString();
                String fixip = o.get("fixed ip").toString();
                String floatip = o.get("float ip").toString();
                Server s = client.getServer(servername);
                ActionResponse ar = osClient.compute().floatingIps().addFloatingIP(s, fixip, floatip);
            } else if (o.get("request").toString().equals("DeleteAliaseRequest")) {
                String servername = o.get("server name").toString();
                String subnetnamefloat = o.get("subnet name floatip").toString();
                String subnetnamefix = o.get("subnet name fixip").toString();
                String fixip = o.get("fixed ip").toString();
                String floatip = o.get("float ip").toString();
                Server s = client.getServer(servername);
                ActionResponse ar = osClient.compute().floatingIps().removeFloatingIP(s, floatip);
            } else if (o.get("request").toString().equals("AttachSriovRequest")) {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                int sriovNum = 1;
                JSONObject allMetaObj = new JSONObject();
                while (o.containsKey(String.format("sriov_vnic:%d", sriovNum))) {
                    Map o2 = (Map)o.get(String.format("sriov_vnic:%d", sriovNum));
                    String servername = o2.get("server name").toString();
                    JSONArray metaObjArray = null; 
                    if (allMetaObj.containsKey(servername)) {
                        metaObjArray = (JSONArray)allMetaObj.get(servername);
                    } else {
                        metaObjArray = new JSONArray();
                        allMetaObj.put(servername, metaObjArray);
                    }
                    JSONObject metaObj = new JSONObject();
                    metaObjArray.add(metaObj);
                    metaObj.put("interface", o2.get("vnic name").toString());
                    metaObj.put("profile", o2.get("port profile").toString());                
                    if (o2.containsKey("mac address")) {
                        metaObj.put("macaddr", o2.get("mac address").toString());
                    }
                    if (o2.containsKey("ip address")) {
                        metaObj.put("ipaddr", o2.get("ip address").toString());
                    }
                    JSONArray routes = new JSONArray();
                    int routeNum = 0;
                    while (o2.containsKey(String.format("routeto %d", routeNum)) && o2.containsKey(String.format("nexthop %d", routeNum))) {
                        JSONObject route = new JSONObject();
                        String routeTo = o2.get(String.format("routeto %d", routeNum)).toString();
                        String nextHop = o2.get(String.format("nexthop %d", routeNum)).toString();
                        route.put("to", routeTo);
                        route.put("via", nextHop);
                        routes.add(route);
                        routeNum++;
                    }
                    // add routes even is empty
                    metaObj.put("routes", routes);
                    String data = metaObj.toJSONString().replaceAll("\"", "'").replaceAll("\\\\/", "/");
                    // set metadata: "sriov_vnic:#": data
                    client.setMetadata(servername, String.format("sriov_vnic:%d", metaObjArray.size()), data);
                    sriovNum++;
                }
                for  (Object obj: allMetaObj.keySet()) {
                    // set metadata: "sriov_vnic:status": "do_attach"
                    String servername = (String)obj;
                    client.setMetadata(servername, "sriov_vnic:status", "do_attach");
                }
            } else if (o.get("request").toString().equals("CreateVirtualRouterRequest") && o.get("routing table").equals("quagga-bgp")) {
                // OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                // handling only Quagga BGP for now
                String servername = o.get("server name").toString();
                // 1. routing table (BGP root) level parameters as "network address #" 
                JSONObject jsonBgpInfo = new JSONObject();
                jsonBgpInfo.put("status", "up");
                int netAddrNum = 1;
                while (o.containsKey(String.format("network address %d", netAddrNum))) {
                    String addrValue = (String)o.get(String.format("network address %d", netAddrNum));
                    String[] typeValue = addrValue.split("=");
                    if (typeValue.length != 2) {
                        continue;
                    }
                    switch (typeValue[0]) {
                        case "ipv4":
                        case "router-id":
                            jsonBgpInfo.put("router_id", typeValue[1]);
                            break;
                        case "bgp-asn":
                            jsonBgpInfo.put("as_number", typeValue[1]);
                            break;
                    }
                    netAddrNum++;
                }
                client.setMetadata(servername, "quagga:bgp:info", jsonBgpInfo.toJSONString().replaceAll("\"", "'").replaceAll("\\\\/", "/"));
                // 2. route (BGP neighbor) level parameters as "route #"
                int neighborNum = 1;
                while (o.containsKey(String.format("route %d", neighborNum))) {
                    Map o2 = (Map) o.get(String.format("route %d", neighborNum));
                    // 2.1 per neighbor "network address #"
                    JSONObject jsonBgpNeighbor = new JSONObject();
                    netAddrNum = 1;
                    while (o2.containsKey(String.format("network address %d", netAddrNum))) {
                        String addrValue = (String) o2.get(String.format("network address %d", netAddrNum));
                        String[] typeValue = addrValue.split("=");
                        if (typeValue.length != 2) {
                            continue;
                        }
                        switch (typeValue[0]) {
                            case "bgp-asn":
                                jsonBgpNeighbor.put("as_number", typeValue[1]);
                                break;
                            case "bgp-authkey":
                                jsonBgpNeighbor.put("bgp_authkey", typeValue[1]);
                                break;
                            case "ipv4-local":
                            case "local-ip":
                                jsonBgpNeighbor.put("local_ip", typeValue[1]);
                                break;
                            case "ipv4-remote": // alternative to nextHop 
                            case "remote-ip":
                                jsonBgpNeighbor.put("remote_ip", typeValue[1]);
                                break;
                            case "ipv4-prefix-list": // alternative to routeFrom 
                                String[] prefixes = typeValue[1].split("[,;\\s]");
                                JSONArray jsonPrefixList = new JSONArray();
                                for (String prefix : prefixes) {
                                    jsonPrefixList.add(prefix);
                                }
                                jsonBgpNeighbor.put("export_prefixes", jsonPrefixList);
                                break;
                        }
                        netAddrNum++;
                    }

                    // 2.2 per neighbor "route to", "route from" and "next hop"
                    if (o2.containsKey("next hop")) {
                        String addrValue = (String) o2.get("next hop");
                        String[] typeValue = addrValue.split("=");
                        if (typeValue.length == 2) {
                            if (typeValue[0].equals("ipv4-remote") || typeValue[0].equals("remote_ip")) {
                                jsonBgpNeighbor.put("remote_ip", typeValue[1]);
                            }
                        }           
                    }
                    if (o2.containsKey("route from")) {
                        String addrValue = (String) o2.get("route from");
                        String[] typeValue = addrValue.split("=");
                        if (typeValue.length == 2) {
                            if (typeValue[0].equals("ipv4-prefix-list")) {
                                String[] prefixes = typeValue[1].split("[,;\\s]");
                                JSONArray jsonPrefixList = new JSONArray();
                                for (String prefix : prefixes) {
                                    jsonPrefixList.add(prefix);
                                }
                                jsonBgpNeighbor.put("export_prefixes", jsonPrefixList);
                            }
                        }
                    }
                    if (!jsonBgpNeighbor.containsKey("export_prefixes")) {
                        jsonBgpNeighbor.put("export_prefixes", new JSONArray());
                    }
                    // no need from "route to" (default == any)
                    // set metadata for quagga:bgp:neighbor:#
                    client.setMetadata(servername, String.format("quagga:bgp:neighbor:%d", neighborNum), jsonBgpNeighbor.toJSONString().replaceAll("\"", "'").replaceAll("\\\\/", "/"));
                    neighborNum++;
                }
            } else if (o.get("request").toString().equals("DeleteVirtualRouterRequest") && o.get("routing table").equals("quagga-bgp")) {
                String servername = o.get("server name").toString();
                Server s = client.getServer(servername);                
                String bgpInfoStr = client.getMetadata(s, "quagga:bgp:info");
                if (bgpInfoStr == null || bgpInfoStr.isEmpty()) {
                    continue;
                }
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonObj = (JSONObject) parser.parse(bgpInfoStr);
                    if (jsonObj.containsKey("status")) {
                        jsonObj.put("status", "down");
                        client.setMetadata(servername, "quagga:bgp:info", jsonObj.toJSONString().replaceAll("\"", "'").replaceAll("\\\\/", "/"));
                    }
                } catch (ParseException e) {
                    throw new EJBException(String.format("%s:DeleteVirtualRouterRequest cannot parse json string %s", this.getClass().getName(),bgpInfoStr));
                }
                // set status=down
            } else if (o.get("request").toString().equals("DetachSriovRequest")) {
                int sriovNum = 1;
                List<String> serversToDetachSriov = new ArrayList();
                while (o.containsKey(String.format("sriov_vnic:%d", sriovNum))) {
                    Map o2 = (Map)o.get(String.format("sriov_vnic:%d", sriovNum));
                    String servername = o2.get("server name").toString();
                    if (!serversToDetachSriov.contains(servername)) {
                        serversToDetachSriov.add(servername);
                    }
                    //@TODO: Add per VNic detach logic here once underlying detach supports finer control
                    sriovNum++;
                }
                for  (String servername: serversToDetachSriov) {
                    // set metadata: "sriov_vnic:status": "do_detach"
                    client.setMetadata(servername, "sriov_vnic:status", "do_detach");
                }
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
            String networkName = ResourceTool.getResourceName(NetworkName, OpenstackPrefix.NETWORK);
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
                /*
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
                 */
                //1.3 check taht network offers switching service
                query = "SELECT ?service  WHERE {<" + network.asResource() + "> nml:hasService  ?service ."
                        + "?service a  mrs:SwitchingService}";
                r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("New network %s does not speicfy Switching Service", network));
                }
                JSONObject o = new JSONObject();
                query = "SELECT ?exnetwork WHERE {?exnetwork mrs:Type \"external-network\"}";
                ResultSet r2 = executeQuery(query, emptyModel, modelDelta);

                if (r2.hasNext()) {
                    QuerySolution q = r2.next();
                    RDFNode network_ex = q.get("exnetwork");

                    String exnetworkname = network_ex.asResource().toString();
                    String exnetworkName = ResourceTool.getResourceName(exnetworkname, OpenstackPrefix.NETWORK);

                    Network n = client.getNetwork(exnetworkName);
                    o.put("exteral-network", client.getResourceName(n));
                }
                //1.4 TODO if the netwrk is external, make sure it has the route to connect

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
            String subnetName = ResourceTool.getResourceName(subnetname, OpenstackPrefix.subnet);
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
                String networkName = ResourceTool.getResourceName(networkname, OpenstackPrefix.NETWORK);
                //1.3.1 gte the tag of the network 
                /*
                 query = "SELECT ?tag {<" + network.asResource() + "> mrs:hasTag ?tag}";
                 r1 = executeQuery(query, modelRef, modelDelta);
                 if (!r1.hasNext()) {
                 throw new EJBException(String.format("network %s does  not have a tag", network));
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
                 */

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
            String volumeName = volume.asResource().toString();
            volumeName = ResourceTool.getResourceName(volumeName, OpenstackPrefix.volume);
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
        //2 select all the ports in the reference model that have that tag
        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort .}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portname = port.asResource().toString();
            String portName = ResourceTool.getResourceName(portname, OpenstackPrefix.PORT);
            Port p = client.getPort(portName);

            //2.1 make sure that the desired operation is valid
            if (p == null ^ creation) //network interface  exists, no need to create a network interface
            {
                if (creation == true) {
                    throw new EJBException(String.format("Network interface %s already exists", portName));
                } else {
                    //throw new EJBException(String.format("Network interface %s does not exist, cannot be deleted", portName));
                    continue;
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
                query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet. ?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">"
                        + "}";
                r1 = executeQueryUnion(query, modelDelta, modelRef);//
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Delta model does not specify network interface subnet of port: %s", port));
                }
                query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet. ?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">"
                        + " FILTER (not exists {$subnet mrs:type \"Cisco_UCS_Port_Profile\"})"
                        + "}";
                r1 = executeQueryUnion(query, modelDelta, modelRef);//
                if (!r1.hasNext()) {
                    continue;
                }
                r1 = executeQueryUnion(query, modelDelta, modelRef);//
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
                    subnetName = ResourceTool.getResourceName(subnetname, OpenstackPrefix.subnet);
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
        query = "SELECT ?node ?port WHERE {"
                + "?node nml:hasBidirectionalPort ?port ."
                + "?node a nml:Node. "
                + "FILTER (not exists {?vmfex mrs:providesVNic ?port})"
                + "}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode port = q.get("port");
            RDFNode server = q.get("node");
            String servername = server.asResource().toString();
            String serverName = ResourceTool.getResourceName(servername, OpenstackPrefix.vm);

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
                String portName = ResourceTool.getResourceName(portname, OpenstackPrefix.PORT);

                //1.2 check that the port has a tag
                /*
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
                 */
                //1.4 create the request
                JSONObject o = new JSONObject();
                /*
                 Port p = client.getPort(portName);
                 if (p == null) {
                 throw new EJBException(String.format("unknown port name '%s'", portName));
                 }
                 */
                //1.4.1 port attachment will be added
                if (creation == true) {
                    //1.4.1.1 see if the network interface is already atatched

                    /*
                    

                     if (p.getDeviceOwner() != null && !p.getDeviceOwner().isEmpty()) {

                     throw new EJBException(String.format("bidirectional port %s to be attached to instance %s is already"
                     + " attached to an instance", port, serverName));
                     }
                     */
                    o.put("request", "AttachPortRequest");
                    o.put("port name", portName);
                    o.put("server name", serverName);
                    requests.add(o);
                } //1.4.2 port attachment will be deleted
                else {

                    /*
                  

                     if (p.getDeviceOwner() == null && p.getDeviceOwner().isEmpty()) {

                     throw new EJBException(String.format("bidirectional port %s to be detached from instance %s is not"
                     + " attached", port, serverName));
                     }
                     */
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
     * Function to allocate floating ip to an interface that is attached to server
     * server ****************************************************************
     */
    private List<JSONObject> floatingIpRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query;

        //1 check for any addition of a port into a device or subnet
        //some error here
        query = "SELECT ?node ?port ?fip WHERE {"
                + "?node nml:hasBidirectionalPort ?port ."
                + "?node a nml:Node. "
                + "?port mrs:hasNetworkAddress ?addr. "
                + "?addr mrs:type \"floating-ip\". "
                + "?addr mrs:value ?fip. "
                + "}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode server = q.get("node");
            RDFNode port = q.get("port");
            RDFNode fip = q.get("fip");
            String servername = server.asResource().toString();
            String serverName = ResourceTool.getResourceName(servername, OpenstackPrefix.vm);
            String portname = port.asResource().toString();
            String portName = ResourceTool.getResourceName(portname, OpenstackPrefix.PORT);
            String floatingIp = fip.toString();
            JSONObject o = new JSONObject();

            if (creation == true) {
                o.put("request", "AssociateFloatingIpRequest");
                o.put("server name", serverName);
                o.put("port name", portName);
                o.put("floating ip", floatingIp);
                requests.add(o);
            } else {
                //@TODO: Add handling for DeassociateFloatingIpRequest in commit. 
                // Note that these are not handled for now as terminating VM will deassociate floating ip automatically
                o.put("request", "DeassociateFloatingIpRequest");
                o.put("server name", serverName);
                o.put("port name", portName);
                o.put("floating ip", floatingIp);
                requests.add(o);
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
        query = "SELECT ?server ?port WHERE {"
                + "?server a nml:Node"
                + "}";
        ResultSet r = executeQuery(query, modelDelta, emptyModel);//here modified 

        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode vm = q.get("server");
            String servername = vm.asResource().toString();
            String serverName = ResourceTool.getResourceName(servername, OpenstackPrefix.vm);
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

                //?? unused ?
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

                //find the port
                query = "SELECT ?port WHERE {<" + subnet.asResource() + "> nml:hasBidirectionalPort ?port}";
                ResultSet r5 = executeQuery(query, modelRef, modelDelta);
                if (!r5.hasNext()) {
                    throw new EJBException(String.format("Vm %s does not specify the attached network interface", vm));
                }
                query = "SELECT ?type WHERE {<" + vm.asResource() + "> mrs:type ?type}";
                r5 = executeQuery(query, emptyModel, modelDelta);
                String imageID = "any";
                String flavorID = "any";
                String keypairName = null;
                String secgroupName = null;
                while (r5.hasNext()) {
                    QuerySolution q2 = r5.next();
                    RDFNode type = q2.get("type");
                    String typename = type.toString();
                    if (typename.contains("imageUUID")) {
                        imageID = getresourcename(typename, "+", "");
                    } else if (typename.contains("flavorID")) {
                        flavorID = getresourcename(typename, "+", "");
                    } else if (typename.contains("keypairName")) {
                        keypairName = getresourcename(typename, "+", "");
                    } else if (typename.contains("secgroupName")) {
                        secgroupName = getresourcename(typename, "+", "");
                    }
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
                    String name = ResourceTool.getResourceName(Name, OpenstackPrefix.PORT);
                    portNames.add(name);
                }
                
                //1.10 create the request
                JSONObject o = new JSONObject();
                if (creation == true) {
                    o.put("request", "RunInstanceRequest");
                } else {
                    o.put("request", "TerminateInstanceRequest");
                }

                o.put("server name", serverName);
                String imageType = defaultImage;
                String flavorType = defaultFlavor;

                if ((imageType == null || imageType.isEmpty()) && imageID.equals("any")) {
                    throw new EJBException(String.format("Cannot determine server image type."));
                }
                if ((flavorType == null || flavorType.isEmpty()) && flavorID.equals("any")) {
                    throw new EJBException(String.format("Cannot determine server flavor type."));
                }
                if (imageID.equals("any")) {
                    o.put("image", imageType);
                } else {
                    o.put("image", imageID);
                }
                if (flavorID.equals("any")) {
                    o.put("flavor", flavorType);
                } else {
                    o.put("flavor", flavorID);
                }
                if (keypairName != null) {
                    o.put("keypair", keypairName);
                }
                if (secgroupName != null) {
                    o.put("secgroup", secgroupName);
                }

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
            String serverName = ResourceTool.getResourceName(servername, OpenstackPrefix.vm);
            RDFNode volume = querySolution1.get("volume");
            String volumeName = volume.asResource().toString();
            volumeName = ResourceTool.getResourceName(volumeName, OpenstackPrefix.volume);

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
        query = "SELECT ?rtservice ?rttable ?route ?nextHop ?routeTo WHERE {"
                + "?rtservice mrs:providesRoutingTable ?rttable. "
                + "?rttable mrs:hasRoute ?route. "
                + "?route a mrs:Route ."
                + "?route mrs:nextHop ?nextHop ."
                + "?route mrs:routeTo ?routeTo}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode routeResource = q.get("route");
            RDFNode nextHopResource = q.get("nextHop");
            RDFNode routeToResource = q.get("routeTo");
            RDFNode routingtable = q.get("rttable");
            if (!nextHopResource.toString().equals("local")) {
                String routingtablename = routingtable.asResource().toString();
                routername = getroutername(topologyUri, routingtablename);
                Router.add(routername);
                String routename = routeResource.toString();
                lr.add(routename);

                Map<String, List<String>> hp;
                hp = new HashMap<>();
                hp.put(routername, lr);

                String subnet = getresourcename(routeToResource.toString(), "+", "");
                String subnet_routername = subnet + "," + routername;
                query = "SELECT ?type ?value WHERE {<" + nextHopResource.asResource() + "> a mrs:NetworkAddress ."
                        + "<" + nextHopResource.asResource() + "> mrs:type ?type ."
                        + "<" + nextHopResource.asResource() + "> mrs:value ?value}";

                ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("nexthop %s for route %s is "
                            + "malformed", nextHopResource, routeResource));
                }

                QuerySolution q1 = r1.next();
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
                    o.put(key1, router);
                    index2++;

                }

            }
        }
        if (Router.size() != 0) {
            LinkedHashSet<String> routers = new LinkedHashSet<String>(Router);
            ArrayList<String> Routers = new ArrayList<String>(routers);

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
        query = "SELECT ?fixip ?floatingip WHERE{?fixedip a mrs:NetworkAddress ."
                + "?fixip nml:isAlias ?floatingip}";
        ResultSet r1 = executeQuery(query, emptyModel, modelDelta);
        //find the fixip and floatingip
        while (r1.hasNext()) {
            QuerySolution q = r1.next();
            RDFNode fixip = q.get("fixip");
            RDFNode floatingip = q.get("floatingip");
            //find the subnet
            //query = "SELECT ?subnet ?port WHERE {?subnet a mrs:SwitchingSubnet ."
            // + "?subnet nml:hasBidirectionalPort ?port}";
            //
            query = "SELECT ?subnet WHERE{?subnet a mrs:SwitchingSubnet ."
                    + "?subnet mrs:hasNetworkAddress <" + fixip.asResource() + "> }";
            r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                return requests; //? throw new EJBException
            }
            QuerySolution q1 = r1.next();

            RDFNode subNetfix = q1.get("subnet");
            String subnetNamefix = subNetfix.toString();
            String subnetnamefix = ResourceTool.getResourceName(subnetNamefix, OpenstackPrefix.subnet);

            //query subnet for the floating ip
            query = "SELECT ?subnet WHERE{?subnet a mrs:SwitchingSubnet ."
                    + "?subnet mrs:hasNetworkAddress <" + floatingip.asResource() + "> }";
            r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                return requests; //? throw new EJBException
            }
            QuerySolution q2 = r1.next();

            RDFNode subNetfloat = q2.get("subnet");
            String subnetNamefloat = subNetfloat.toString();
            String subnetnamefloat = ResourceTool.getResourceName(subnetNamefloat, OpenstackPrefix.subnet);

            //query for the server
            query = "SELECT ?server WHERE{?server a nml:Node ."
                    + "?server  mrs:hasNetworkAddress <" + fixip.asResource() + ">}";
            r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                return requests; //? throw new EJBException
            }
            QuerySolution q3 = r1.next();
            RDFNode serVer = q3.get("server");
            String serverName = serVer.toString();
            String servername = ResourceTool.getResourceName(serverName, OpenstackPrefix.vm);

            query = "SELECT ?type ?value WHERE {<" + fixip.asResource() + "> a mrs:NetworkAddress ."
                    + "<" + fixip.asResource() + "> mrs:type ?type ."
                    + "<" + fixip.asResource() + "> mrs:value ?value}";

            r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                return requests; //? throw new EJBException
            }
            QuerySolution q4 = r1.next();
            RDFNode valUe = q4.get("value");
            String fixvalue = valUe.toString();

            query = "SELECT ?type ?value WHERE {<" + floatingip.asResource() + "> a mrs:NetworkAddress ."
                    + "<" + floatingip.asResource() + "> mrs:type ?type ."
                    + "<" + floatingip.asResource() + "> mrs:value ?value}";

            r1 = executeQuery(query, emptyModel, modelDelta);
            if (!r1.hasNext()) {
                return requests; //? throw new EJBException
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
            requests.add(o);
        }

        return requests;
    }

    private List<JSONObject> sriovRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        JSONObject JO = new JSONObject();
        String query = "SELECT ?vmfex ?vnic WHERE {"
                + "?vmfex mrs:providesVNic ?vnic ."
                + "}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        int sriovNum = 1;
        while (r.hasNext()) {
            QuerySolution q = r.next();
            JSONObject o = new JSONObject();
            Resource vNic = q.getResource("vnic");
            query = "SELECT ?vm ?profile ?routing WHERE {"
                    + "?vm a nml:Node ."
                    + String.format("?vm nml:hasBidirectionalPort <%s> .", vNic.getURI())
                    + "?pp a mrs:SwitchingSubnet ."
                    + String.format("?pp nml:hasBidirectionalPort <%s>  .", vNic.getURI())
                    + "?pp mrs:type \"Cisco_UCS_Port_Profile\" ."
                    + "?pp mrs:value $profile ."
                    + "OPTIONAL {"
                    + " ?vm nml:hasService ?routing . "
                    + " ?routing a mrs:RoutingService. "
                    + " ?routing mrs:providesRoute ?route. }"
                    + "}";
            Resource VM = null;
            String portProfile = null;
            Resource routingSvc = null;
            ResultSet r1 = executeQueryUnion(query, modelRef, modelDelta);
            if (r1.hasNext()) {
                QuerySolution q1 = r1.next();
                VM = q1.getResource("vm");
                portProfile = q1.get("profile").toString();
                if (portProfile.contains("Cisco_UCS_Port_Profile+")) {
                    portProfile = this.getresourcename(portProfile, "+", "");
                }
                if (q1.contains("routing")) {
                    routingSvc = q1.getResource("routing");
                }
            } else {
                throw new EJBException("sriovRequests related resoruces for vNic='" + vNic.getURI() + "' cannot be resolved");
            }
            query = "SELECT ?ip WHERE {"
                    + String.format("<%s> mrs:hasNetworkAddress ?ipAddr . ", vNic)
                    + "?ipAddr a mrs:NetworkAddress . "
                    + "?ipAddr mrs:type \"ipv4-address\" . "
                    + "?ipAddr mrs:value ?ip . "
                    + "}";
            ResultSet r2 = executeQuery(query, emptyModel, modelDelta);
            String ip = null;
            if (r2.hasNext()) {
                ip = r2.next().get("ip").toString();
            }
            query = "SELECT ?mac WHERE {"
                    + String.format("<%s> mrs:hasNetworkAddress ?macAddr . ", vNic)
                    + "?macAddr a mrs:NetworkAddress . "
                    + "?macAddr mrs:type \"mac-address\" . "
                    + "?macAddr mrs:value ?mac . "
                    + "}";
            ResultSet r3 = executeQuery(query, emptyModel, modelDelta);
            String mac = null;
            if (r3.hasNext()) {
                mac = r3.next().get("mac").toString();
            }
            String serverName = ResourceTool.getResourceName(VM.toString(), OpenstackPrefix.vm);
            String vnicName = ResourceTool.getResourceName(vNic.toString(), OpenstackPrefix.PORT);
            o.put("server name", serverName);
            o.put("port profile", portProfile);
            o.put("vnic name", vnicName);
            if (ip != null) {
                o.put("ip address", ip);
            }
            if (mac != null) {
                o.put("mac address", mac);
            }
            if (routingSvc != null) {
                query = "SELECT ?routeto ?nexthop WHERE {"
                        + String.format("<%s> mrs:providesRoute ?route . ", routingSvc)
                        + "?route mrs:routeTo ?toAddr . "
                        + "?route mrs:nextHop ?viaAddr . "
                        + "?toAddr mrs:type \"ipv4-prefix\" . "
                        + "?toAddr mrs:value ?routeto . "
                        + "?viaAddr mrs:type \"ipv4-address\" . "
                        + "?viaAddr mrs:value ?nexthop . "
                        + "}";
                ResultSet r4 = executeQuery(query, emptyModel, modelDelta);
                int routeNum = 0;
                while (r4.hasNext()) {
                    QuerySolution q3 = r4.next();
                    String routeTo = q3.get("routeto").toString();
                    String nextHop = q3.get("nexthop").toString();
                    o.put(String.format("routeto %d", routeNum), routeTo);
                    o.put(String.format("nexthop %d", routeNum), nextHop);
                    routeNum++;
                }
            }
            JO.put(String.format("sriov_vnic:%d", sriovNum), o);
            sriovNum++;
        }
        if (creation == true) {
            JO.put("request", "AttachSriovRequest");
        } else {
            JO.put("request", "DetachSriovRequest");
        }
        requests.add(JO);
        return requests;
    }

    private List<JSONObject> virtualRouterRequests(OntModel modelRef, OntModel modelDelta, boolean creation) throws EJBException {
        List<JSONObject> requests = new ArrayList();
        String query = "SELECT ?rtsvc ?rtable ?rtable_type WHERE {"
                + "?rtsvc mrs:providesRoutingTable ?rtable ."
                + "?rtable mrs:type ?rtable_type ."
                + "}";
        ResultSet r = executeQuery(query, emptyModel, modelDelta);
        while (r.hasNext()) {
            JSONObject JO = new JSONObject();
            QuerySolution q = r.next();
            JSONObject o = new JSONObject();
            String rtType = q.get("rtable_type").toString();
            JO.put("routing table", rtType);
            if (!rtType.startsWith("quagga") && !rtType.equalsIgnoreCase("linux")) {
                continue;
            }
            Resource rtService = q.getResource("rtsvc");
            Resource rtTable = q.getResource("rtable");
            query = "SELECT ?vm WHERE {"
                    + String.format("?vm nml:hasService <%s> .", rtService.getURI())
                    + "?vm a nml:Node ."
                    + "}";
            Resource VM = null;
            ResultSet r1 = executeQueryUnion(query, modelRef, modelDelta);
            if (r1.hasNext()) {
                QuerySolution q1 = r1.next();
                VM = q1.getResource("vm");
            } else {
                throw new EJBException("virtualRouterRequests cannot find a VM hosting routingService='" + rtService.getURI());
            }
            JO.put("server name", VM.getURI());
            query = "SELECT ?netaddr ?netaddr_type ?netaddr_value WHERE {"
                    + String.format("<%s> mrs:hasNetworkAddress ?netaddr .", rtTable.getURI())
                    + "?netaddr mrs:type ?netaddr_type ."
                    + "?netaddr mrs:value ?netaddr_value ."
                    + "}";
            ResultSet r2 = executeQuery(query, emptyModel, modelDelta);
            int netAddrNum = 1;
            while (r2.hasNext()) {
                QuerySolution q2 = r2.next();
                Resource netAddr = q2.getResource("netaddr");
                String netAddrType = q2.get("netaddr_type").toString();
                String netAddrValue = q2.get("netaddr_value").toString();
                JO.put(String.format("network address %d", netAddrNum), netAddrType+"="+netAddrValue);
                netAddrNum++;                        
            }
            query = "SELECT ?route ?route_to ?route_to_type ?route_to_value "
                    + "?next_hop ?next_hop_type ?next_hop_value "
                    + "?route_from ?route_from_type ?route_from_value WHERE {"
                    + String.format("<%s> mrs:hasRoute ?route .", rtTable.getURI())
                    + "OPTIONAL {?route mrs:routeTo ?route_to. "
                    + "     ?route_to mrs:type ?route_to_type. "
                    + "     ?route_to mrs:value ?troute_to_value. } ."
                    + "OPTIONAL {?route mrs:routeFrom ?route_from. "
                    + "     ?route_from mrs:type ?route_from_type. "
                    + "     ?route_from mrs:value ?route_from_value. } ."
                    + "OPTIONAL {?route mrs:nextHop ?next_hop. "
                    + "     ?next_hop mrs:type ?next_hop_type. "
                    + "     ?next_hop mrs:value ?next_hop_value. } "
                    + "}";
            ResultSet r3 = executeQuery(query, emptyModel, modelDelta);
            int routeNum = 1;
            while (r3.hasNext()) {
                QuerySolution q3 = r3.next();
                JSONObject jsonRoute = new JSONObject();
                Resource route = q3.getResource("route");
                jsonRoute.put("name", route.getURI());
                Resource routeTo = q3.contains("route_to") ? q3.getResource("route_to") : null;
                if (routeTo != null) {
                    String routeToType = q3.get("route_to_type").toString();
                    String routeToValue = q3.get("route_to_value").toString();
                    jsonRoute.put("route to", routeToType+"="+routeToValue);
                }
                Resource routeFrom = q3.contains("route_from") ? q3.getResource("route_from") : null;
                if (routeFrom != null) {
                    String routeFromType = q3.get("route_from_type").toString();
                    String routeFromValue = q3.get("route_from_value").toString();
                    jsonRoute.put("route from", routeFromType+"="+routeFromValue);
                }
                Resource nextHop = q3.contains("next_hop") ? q3.getResource("next_hop") : null;
                if (nextHop != null) {
                    String nextHopType = q3.get("next_hop_type").toString();
                    String nextHopValue = q3.get("next_hop_value").toString();
                    jsonRoute.put("next hop", nextHopType+"="+nextHopValue);
                }
                // NetworkAddress elements for each Route
                query = "SELECT ?netaddr ?netaddr_type ?netaddr_value WHERE {"
                    + String.format("<%s> mrs:hasNetworkAddress ?netaddr .", route.getURI())
                    + "?netaddr mrs:type ?netaddr_type ."
                    + "?netaddr mrs:value ?netaddr_value ."
                    + "}";
                ResultSet r4 = executeQuery(query, emptyModel, modelDelta);
                int routeNetAddrNum = 1;
                while (r4.hasNext()) {
                    QuerySolution q4 = r4.next();
                    Resource netAddr = q4.getResource("netaddr");
                    String netAddrType = q4.get("netaddr_type").toString();
                    String netAddrValue = q4.get("netaddr_value").toString();
                    jsonRoute.put(String.format("network address %d", routeNetAddrNum), netAddrType + "=" + netAddrValue);
                    routeNetAddrNum++;
                }
                JO.put(String.format("route %d", routeNum), jsonRoute);
                routeNum++;
            }
            if (creation == true) {
                JO.put("request", "CreateVirtualRouterRequest");
            } else {
                JO.put("request", "DeleteVirtualRouterRequest");
            }
            requests.add(JO);
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
        if (resourcename.contains("router+")) {
            String topologyuri = topologyUri + "router+";
            String resource = resourcename.replace(topologyuri, "");
            int index = resource.indexOf(":");
            return resource.substring(0, index);
        } else {
            return resourcename;
        }
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
        Router router = client.getRouter(routerid);
        if (port1.getDeviceId().equals(router.getId()) && port2.getDeviceId().equals(router.getId())) {
            return true;
        }
        return false;
    }

    public void PortCreationCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Port resource = client.getPort(id);
                if (resource != null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void PortDeletionCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Port resource = client.getPort(id);
                if (resource == null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void SubnetDeletionCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Subnet resource = client.getSubnet(id);
                if (resource == null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void SubnetCreationCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Subnet resource = client.getSubnet(id);
                if (resource != null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void VmCreationCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Server resource = client.getServer(id);
                if (resource != null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void VmDeletionCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Server resource = client.getServer(id);
                if (resource == null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void NetworkDeletionCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Network resource = client.getNetwork(id);
                if (resource == null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void NetworkCreationCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Network resource = client.getNetwork(id);
                if (resource != null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void RouterCreationCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Router resource = client.getRouter(id);
                if (resource != null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }

    public void RouterDeletionCheck(String id, String url, String NATServer, String username, String password, String tenantName, String topologyUri) {
        /*DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
         request.withNetworkInterfaceIds(id);
         */
        //OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                OpenStackGetUpdate(url, NATServer, username, password, tenantName, topologyUri);
                //client.updateResources("Port");
                Router resource = client.getRouter(id);
                if (resource == null) {
                    break;
                }
            } catch (Exception e) {
            }
        }
    }
    
    public void SecurityGroupAddCheck(String serverId, String secgroupId) {
        int maxTries = 30;
        while ((maxTries--) > 0) {
            try {
                Server server = osClient.compute().servers().get(serverId);
                if (server != null && server.getStatus().equals(server.getStatus().ACTIVE)) {
                    // add
                    osClient.compute().servers().addSecurityGroup(serverId, secgroupId);
                    // check
                    List<? extends SecGroupExtension> listServerGroups = osClient.compute().securityGroups().listServerGroups(serverId);
                    if (listServerGroups != null && !listServerGroups.isEmpty()) {
                        for (SecGroupExtension secgroup : listServerGroups) {
                            if (secgroup.getName().equals(secgroupId)) {
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(10000);  // sleep 10 secs
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
