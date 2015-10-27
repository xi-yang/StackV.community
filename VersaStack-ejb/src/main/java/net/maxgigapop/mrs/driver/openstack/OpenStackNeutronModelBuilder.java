/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstack;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.common.Mrs;
import static net.maxgigapop.mrs.common.Mrs.hasNetworkAddress;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.networking.RouterService;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.AllowedAddressPair;
import org.openstack4j.model.network.HostRoute;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Pool;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.openstack.compute.domain.NovaFloatingIP;
import org.openstack4j.openstack.networking.domain.AddRouterInterfaceAction;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;

/**
 *
 * @author max
 */
public class OpenStackNeutronModelBuilder {

    public static OntModel createOntology(String url, String NATServer, String topologyURI, String user_name, String password, String tenantName) throws IOException, Exception {
        ArrayList fip = new ArrayList();
        String POOL = null;
        Logger logger = Logger.getLogger(OpenStackNeutronModelBuilder.class.getName());
        
        //create model object
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        //set all the model prefixes
        model.setNsPrefix("rdfs", RdfOwl.getRdfsURI());
        model.setNsPrefix("rdf", RdfOwl.getRdfURI());
        model.setNsPrefix("xsd", RdfOwl.getXsdURI());
        model.setNsPrefix("owl", RdfOwl.getOwlURI());
        model.setNsPrefix("nml", Nml.getURI());
        model.setNsPrefix("mrs", Mrs.getURI());

        //set the global properties
        Property hasNode = Nml.hasNode;
        Property hasBidirectionalPort = Nml.hasBidirectionalPort;
        Property hasService = Nml.hasService;
        Property providesVPC = Mrs.providesVPC;
        Property providesVM = Mrs.providesVM;
        Property type = Mrs.type;
        Property providedByService = Mrs.providedByService;
        Property providesBucket = Mrs.providesBucket;
        Property providesRoute = Mrs.providesRoute;
        Property hasRoute = Mrs.hasRoute;

        Property providesSubnet = Mrs.providesSubnet;

        Property providesVolume = Mrs.providesVolume;
        Property routeFrom = Mrs.routeFrom;
        Property routeTo = Mrs.routeTo;
        Property nextHop = Mrs.nextHop;
        Property value = Mrs.value;
        Property hasBucket = Mrs.hasBucket;
        Property hasVolume = Mrs.hasVolume;
        Property hasTopology = Nml.hasTopology;
        Property targetDevice = model.createProperty(model.getNsPrefixURI("mrs") + "target_device");
       
        Property hasTag = Mrs.hasTag;
        Property hasNetworkAddress = Mrs.hasNetworkAddress;
        Property providesRoutingTable = model.createProperty(model.getNsPrefixURI("mrs") + "providesRoutingTable");
        Property isAlias = Nml.isAlias;
        //set the global resources
        Resource route = Mrs.Route;
        Resource hypervisorService = Mrs.HypervisorService;
       

        Resource blockStorageService = Mrs.BlockStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology = Nml.Topology;

        Resource networkAddress = Mrs.NetworkAddress;
        Resource switchingSubnet = Mrs.SwitchingSubnet;
        Resource switchingService = Mrs.SwitchingService;
        Resource node = Nml.Node;
        Resource biPort = Nml.BidirectionalPort;
        Resource RoutingService = Mrs.RoutingService;
        Resource namedIndividual = model.createResource(model.getNsPrefixURI("mrs") + "NamedIndividual");
        Resource objectStorageService = Mrs.ObjectStorageService;
        //Resource OpenstackTopology = model.createResource("urn:ogf:network:dragon.maxgigapop.net:topology");
        Resource OpenstackTopology = RdfOwl.createResource(model, topologyURI, topology);
        //Resource Neutron = model.createResource("urn:ogf:network:dragon.maxgigapop.net:openstack-neutron");
        Resource networkService = RdfOwl.createResource(model, topologyURI + ":network-service", Mrs.VirtualCloudService);

        Resource routingService = RdfOwl.createResource(model, topologyURI + ":routing-service", RoutingService);
        Resource cinderService = RdfOwl.createResource(model, topologyURI + ":cinder-service", blockStorageService);

        
        //port tag
        Resource PORT_TAG = RdfOwl.createResource(model, topologyURI + ":portTag", Mrs.Tag);

        model.add(model.createStatement(PORT_TAG, type, "interface"));
        model.add(model.createStatement(PORT_TAG, value, "network"));

        //OpenStackGet openstackget = new OpenStackGet(url, NATServer, user_name, password, tenantName);
        OpenStackGet openstackget = new OpenStackGet(url, NATServer, user_name, password, tenantName);

        model.add(model.createStatement(OpenstackTopology, hasService, routingService));
        model.add(model.createStatement(OpenstackTopology, hasService, cinderService));
        model.add(model.createStatement(OpenstackTopology, hasService, networkService));
        model.add(model.createStatement(OpenstackTopology, hasService, cinderService));

        //Left part
        for (Port p : openstackget.getPorts()) {
            String PortID = openstackget.getResourceName(p);
            Resource PORT = RdfOwl.createResource(model, topologyURI + ":" + "port+" + openstackget.getResourceName(p), biPort);

            for (IP q : p.getFixedIps()) {
                if (q.getIpAddress() != null || !q.getIpAddress().isEmpty()) {
                    if (openstackget.getNetwork(p.getNetworkId()).isRouterExternal()) {
                        Resource PUBLIC_ADDRESS = RdfOwl.createResource(model, topologyURI + ":port+" + PortID + ":" + "public-ip-address+" + q.getIpAddress(), networkAddress);
                        model.add(model.createStatement(PORT, hasNetworkAddress, PUBLIC_ADDRESS));
                        model.add(model.createStatement(PUBLIC_ADDRESS, type, "ipv4:public"));
                        model.add(model.createStatement(PUBLIC_ADDRESS, value, q.getIpAddress()));
                        model.add(model.createStatement(PORT, hasTag, PORT_TAG));

                    } else {
                        Resource PRIVATE_ADDRESS = RdfOwl.createResource(model, topologyURI + ":port+" + PortID + ":" + "private-ip-address+" + q.getIpAddress(), networkAddress);
                        model.add(model.createStatement(PORT, hasNetworkAddress, PRIVATE_ADDRESS));
                        model.add(model.createStatement(PRIVATE_ADDRESS, type, "ipv4:private"));
                        model.add(model.createStatement(PRIVATE_ADDRESS, value, q.getIpAddress()));
                        model.add(model.createStatement(PORT, hasTag, PORT_TAG));
                    }
                }

            }

            for (Server server : openstackget.getServers()) {

                String hostID = server.getHost();
                if (hostID == null || hostID.isEmpty()) {
                    hostID = server.getHostId();
                }

                String hypervisorname = server.getHypervisorHostname();

                Resource HOST = RdfOwl.createResource(model, topologyURI + ":" + "hostID+" + hostID, node);

                Resource HYPERVISOR = RdfOwl.createResource(model, topologyURI + ":" + "hypersor-name+" + hypervisorname, hypervisorService);
                Resource VM = RdfOwl.createResource(model, topologyURI + ":" + "server-name+" + openstackget.getServereName(server), node);
            
                
                model.add(model.createStatement(OpenstackTopology, hasNode, HOST));

                model.add(model.createStatement(HOST, hasService, HYPERVISOR));
                model.add(model.createStatement(HYPERVISOR, providesVM, VM));
                model.add(model.createStatement(HOST, hasNode, VM));
                
                for (Port port : openstackget.getServerPorts(server)) {
                    Resource Port = model.getResource(topologyURI + ":" + "port+" + openstackget.getResourceName(port));  //use function

                    model.add(model.createStatement(VM, hasBidirectionalPort, Port));
                    model.add(model.createStatement(Port, hasTag, PORT_TAG));
                }

            }
        }

        //Right subnet part
        for (Network n : openstackget.getNetworks()) {
            String networkID = openstackget.getResourceName(n);
            Resource NETWORK = RdfOwl.createResource(model, topologyURI + ":network+" + networkID, topology);

            model.add(model.createStatement(OpenstackTopology, hasTopology, NETWORK));
            
            model.add(model.createStatement(networkService, providesVPC, NETWORK));
            Resource SWITCHINGSERVICE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":switchingservice", switchingService);

            model.add(model.createStatement(NETWORK, hasService, SWITCHINGSERVICE));

            //TO FIND THE EXTERNAL OR INTERNAL NETWORK
            if (n.isRouterExternal()) {
                Resource EXTERNALNETWORK_TAG = RdfOwl.createResource(model, topologyURI + "network_tag_external", Mrs.Tag);

                model.add(model.createStatement(NETWORK, hasTag, EXTERNALNETWORK_TAG));
                model.add(model.createStatement(EXTERNALNETWORK_TAG, type, "network-type"));
                model.add(model.createStatement(EXTERNALNETWORK_TAG, value, "external"));

                for (Subnet s : n.getNeutronSubnets()) {
                    for (NetFloatingIP f : openstackget.getFloatingIp()) {
                        fip.add(f.getFloatingIpAddress());

                    }

                    for (Pool ap : s.getAllocationPools()) {
                        String START = ap.getStart();
                        String END = ap.getEnd();
                        String FLOATING_IP_INUSE = fip.toString();
                        String subnetId = openstackget.getResourceName(s);

                        Resource SUBNET = RdfOwl.createResource(model, topologyURI + ":" + "network+" + networkID + ":subnet+" + subnetId, switchingSubnet);
                        Resource EXTERNALSUBNET_TAG = RdfOwl.createResource(model, topologyURI + ":subnet_tag_public", Mrs.Tag);

                        model.add(model.createStatement(SUBNET, hasTag, EXTERNALSUBNET_TAG));
                        model.add(model.createStatement(EXTERNALSUBNET_TAG, type, "subnet-type"));
                        model.add(model.createStatement(EXTERNALSUBNET_TAG, value, "public"));

                        model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
                        Resource SUBNET_NETWORK_ADDRESS
                                = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":subnetnetworkaddress", networkAddress);
                        Resource FLOATING_IP_INUSING
                                = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":floatingip-inuse", networkAddress);
                        Resource FLOATING_IP_POOL = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":floatingip-pool", networkAddress);

                        model.add(model.createStatement(OpenstackTopology, hasNetworkAddress, FLOATING_IP_INUSING));
                        model.add(model.createStatement(FLOATING_IP_INUSING, type, "ipv4-floatingip"));
                        model.add(model.createStatement(FLOATING_IP_INUSING, value, FLOATING_IP_INUSE));//need to modify here
                        model.add(model.createStatement(OpenstackTopology, hasNetworkAddress, FLOATING_IP_POOL));
                        model.add(model.createStatement(FLOATING_IP_POOL, type, "ipv4-floatingip-pool"));
                        model.add(model.createStatement(FLOATING_IP_POOL, value, START + "-" + END));
                        model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
                        model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
                        model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, s.getCidr()));
                        //FOR THE GATEWAY
                        if (s.getGateway() != null && !s.getGateway().isEmpty()) {
                            String gatewayadd = s.getGateway();
                            Resource GATEWAY = RdfOwl.createResource(model, topologyURI + ":" + "gateway-ip+" + gatewayadd, biPort);
                            Resource GATEWAYADDRESS = RdfOwl.createResource(model, topologyURI + ":external_subnet_gateway_address", networkAddress);
                            model.add(model.createStatement(SUBNET, hasBidirectionalPort, GATEWAY));
                            model.add(model.createStatement(GATEWAY, hasNetworkAddress, GATEWAYADDRESS));//LEAVE FOR THE FUTHER DEVELOP FOR THE MANNER
                            model.add(model.createStatement(GATEWAYADDRESS, value, gatewayadd));
                            model.add(model.createStatement(GATEWAY, type, "gateway"));
                        }
                        for (Port port : openstackget.getPorts()) {
                            for (String subID : openstackget.getPortSubnetID(port)) {
                                String subName = openstackget.getResourceName(openstackget.getSubnet(subID));
                                if (subnetId.equals(subName)) {
                                    Resource Port = model.getResource(topologyURI + ":port+" + openstackget.getResourceName(port));
                                    model.add(model.createStatement(SUBNET, hasBidirectionalPort, Port));
                                    model.add(model.createStatement(Port, hasTag, PORT_TAG));

                                }

                            }
                        }

                        //external routing table
                        String dest = "0.0.0.0/24";
                        //String destIP = destIp.replace("/", "");
                        String nextHOP = s.getGateway();

                        Resource ROUTINGSERVICE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":network-routingservice", RoutingService);
                        Resource EXROUTINGTABLE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":route-external-routingtable", Mrs.RoutingTable);
                        Resource EXROUTE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":dest_ip:" + dest + "+subnet:" + subnetId + ":route-external", route);
                        Resource EXNEXTHOP = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":next-hop+" + nextHOP + ":route-external", networkAddress);
                        Resource DEST_EXTERNAL = RdfOwl.createResource(model, topologyURI + ":dest_ip+" + dest + ":route-external-dest", networkAddress);
                        Resource LOCAL_ROUTE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":local-route", Mrs.Route);
                        model.add(model.createStatement(NETWORK, hasService, ROUTINGSERVICE));
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoutingTable, EXROUTINGTABLE));
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoute, EXROUTE));
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoute,LOCAL_ROUTE));
                        model.add(model.createStatement(EXROUTINGTABLE, hasRoute, EXROUTE));
                        model.add(model.createStatement(EXROUTINGTABLE, hasRoute, LOCAL_ROUTE));
                        model.add(model.createStatement(EXROUTE, routeFrom, SUBNET));

                        model.add(model.createStatement(EXROUTE, routeTo, DEST_EXTERNAL));
                        model.add(model.createStatement(DEST_EXTERNAL, type, "ipv4-prefix"));
                        model.add(model.createStatement(DEST_EXTERNAL, value, dest));

                        model.add(model.createStatement(EXROUTE, nextHop, EXNEXTHOP));
                        model.add(model.createStatement(EXNEXTHOP, type, "ipv4-network-address"));
                        model.add(model.createStatement(EXNEXTHOP, value, nextHOP));

                        model.add(model.createStatement(LOCAL_ROUTE, routeFrom, SUBNET));
                        model.add(model.createStatement(LOCAL_ROUTE, routeTo, SUBNET_NETWORK_ADDRESS));
                        model.add(model.createStatement(LOCAL_ROUTE, nextHop, "local"));

                        for (HostRoute hr : s.getHostRoutes()) {
                            String destIp = hr.getDestination();
                           // String destIP = destIp.replace("/", "");
                            String destIP = destIp;
                            String nextHOPEXT = hr.getNexthop();
                            //System.out.println("aaaa" + destIP);
                            Resource EXHOSTROUTE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":dest_ip+" + destIP + ":from_subenet+" + subnetId + ":hostroute-public", route);

                            Resource EXHOSTROUTETO = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":dest:" + destIP + ":route-public", networkAddress);
                            Resource EXHOSTNEXTHOP = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":next-hop:" + nextHOPEXT + ":route-public", networkAddress);
                            Resource EXHOSTROUTINGTABLE = RdfOwl.createResource(model, topologyURI + ";network+" + networkID + ":subnet+" + openstackget.getResourceName(s) + ":hostroutingtable-external", Mrs.RoutingTable);

                            model.add(model.createStatement(NETWORK, hasService, ROUTINGSERVICE));
                            model.add(model.createStatement(ROUTINGSERVICE, providesRoutingTable, EXHOSTROUTINGTABLE));
                            model.add(model.createStatement(ROUTINGSERVICE, providesRoute, EXHOSTROUTE));
                            model.add(model.createStatement(EXHOSTROUTINGTABLE, hasRoute, EXHOSTROUTE));

                            model.add(model.createStatement(EXHOSTROUTE, routeFrom, SUBNET));
                            model.add(model.createStatement(EXHOSTROUTE, routeTo, EXHOSTROUTETO));
                            model.add(model.createStatement(EXHOSTROUTE, nextHop, EXHOSTNEXTHOP));

                            model.add(model.createStatement(EXHOSTROUTETO, type, "ipv4-prefix"));
                            model.add(model.createStatement(EXHOSTROUTETO, value, destIp));

                            model.add(model.createStatement(EXHOSTNEXTHOP, type, "ipv4-netwrok-address"));
                            model.add(model.createStatement(EXHOSTNEXTHOP, value, nextHOPEXT));

                        }
                    }
                }
            } else {
                Resource TENANTNETWORK_TAG = RdfOwl.createResource(model, topologyURI + ":network_tag_tenant", Mrs.Tag);

                model.add(model.createStatement(NETWORK, hasTag, TENANTNETWORK_TAG));
                model.add(model.createStatement(TENANTNETWORK_TAG, type, "network-type"));
                model.add(model.createStatement(TENANTNETWORK_TAG, value, "tenant"));
                if(n.getSubnets().size() != 0){
                    
               
                for (Subnet s : n.getNeutronSubnets()) {

                    String subnetId = openstackget.getResourceName(s);
                    Resource SUBNET = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId, switchingSubnet);
                    //Resource ROUTE = RdfOwl.createResource(model, topologyURI + ":" + s.get, switchingSubnet);
                    model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
                    Resource SUBNET_NETWORK_ADDRESS
                            = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":subnetnetworkaddress", networkAddress);
                    Resource ROUTINGSERVICE = RdfOwl.createResource(model, topologyURI + ":netowrk+" + networkID + ":network-routingservice", RoutingService);
                    Resource ROUTINGTABLEPERNET = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":routingtable", Mrs.RoutingTable);

                    Resource TENANT_SUBNET_TAG = RdfOwl.createResource(model, topologyURI + ":subnet_tag_private", Mrs.Tag);
                    Resource LOCAL_ROUTE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":local-route", Mrs.Route);

                    model.add(model.createStatement(SUBNET, hasTag, TENANT_SUBNET_TAG));
                    model.add(model.createStatement(TENANT_SUBNET_TAG, type, "subnet-type"));
                    model.add(model.createStatement(TENANT_SUBNET_TAG, value, "private"));

                    model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
                    model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
                    model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, s.getCidr()));

                    if (s.getGateway() != null && !s.getGateway().isEmpty()) {
                        String gatewayadd = s.getGateway();
                        Resource GATEWAY = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":subnetgateway" + gatewayadd, biPort);
                        Resource GATEWAYADDRESS = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":tenant_subnet_gateway_address" + gatewayadd, networkAddress);
                        model.add(model.createStatement(SUBNET, hasBidirectionalPort, GATEWAY));
                        model.add(model.createStatement(GATEWAY, hasNetworkAddress, GATEWAYADDRESS));//LEAVE FOR THE FUTHER DEVELOP FOR THE MANNER
                        model.add(model.createStatement(GATEWAYADDRESS, value, gatewayadd));
                        model.add(model.createStatement(GATEWAY, type, "gateway"));

                    }

                    //subnet route modeling 
                    model.add(model.createStatement(NETWORK, hasService, ROUTINGSERVICE));
                    model.add(model.createStatement(ROUTINGSERVICE, providesRoutingTable, ROUTINGTABLEPERNET));
                    model.add(model.createStatement(ROUTINGSERVICE, providesRoute, LOCAL_ROUTE));
                    model.add(model.createStatement(ROUTINGTABLEPERNET, hasRoute, LOCAL_ROUTE));

                    model.add(model.createStatement(LOCAL_ROUTE, routeFrom, SUBNET));
                    model.add(model.createStatement(LOCAL_ROUTE, routeTo, SUBNET_NETWORK_ADDRESS));
                    model.add(model.createStatement(LOCAL_ROUTE, nextHop, "local"));

                    //host route modeling
                    for (HostRoute hr : s.getHostRoutes()) {
                        String destIp = hr.getDestination();
                        //String destIP = destIp.replace("/", "");
                        String destIP = destIp;
                        String nextHOP = hr.getNexthop();
                        //System.out.println("aaaa" + destIP);
                        Resource INROUTE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":dest_ip+" + destIP + ":route_from+" + subnetId + ":hostroute-tenant", route);
                        Resource INROUTEFROM = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":route_from", switchingSubnet);
                        Resource INROUTETO = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":dest_ip+" + destIP, networkAddress);
                        Resource INNEXTHOP = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":next-hop+" + nextHOP + ":route-tenant", networkAddress);
                        Resource HOSTROUTINGTABLE = RdfOwl.createResource(model, topologyURI + ":network+" + networkID + ":subnet+" + subnetId + ":hostroutingtable", Mrs.RoutingTable);

                        model.add(model.createStatement(NETWORK, hasService, ROUTINGSERVICE));
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoutingTable, HOSTROUTINGTABLE));
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoute, INROUTE));
                        model.add(model.createStatement(HOSTROUTINGTABLE, hasRoute, INROUTE));
                        model.add(model.createStatement(INROUTE, routeFrom, INROUTEFROM));
                        model.add(model.createStatement(INROUTE, routeTo, INROUTETO));
                        model.add(model.createStatement(INROUTE, nextHop, INNEXTHOP));

                        model.add(model.createStatement(INROUTEFROM, type, "subnet"));
                        model.add(model.createStatement(INROUTEFROM, value, subnetId));

                        model.add(model.createStatement(INNEXTHOP, type, "ipv4-network-address"));
                        model.add(model.createStatement(INNEXTHOP, value, nextHOP));

                        model.add(model.createStatement(INROUTETO, type, "ipv4-prefix"));
                        model.add(model.createStatement(INROUTETO, value, destIp));

                    }

                    for (Port port : openstackget.getPorts()) {
                        for (String subID : openstackget.getPortSubnetID(port)) {
                            String subName = openstackget.getResourceName(openstackget.getSubnet(subID));
                            if (subnetId.equals(subName) || subID.equals(s.getId())) { //not enter the if  
                                Resource Port = model.getResource(topologyURI + ":port+" + openstackget.getResourceName(port));
                                model.add(model.createStatement(SUBNET, hasBidirectionalPort, Port));
                                model.add(model.createStatement(Port, hasTag, PORT_TAG));

                            }

                        }
                    }

                }
            }
            }
        }

        //BUILDING THE ROUTING TABLE
        for (Router r : openstackget.getRouters()) {
            String routername = openstackget.getResourceName(r);
            routername = routername.replaceAll("[^A-Za-z0-9()_-]", "");
            for (Port port : openstackget.getPorts()) {

                if (port.getDeviceId().equals(r.getId())) {

                    for (String DES_SUB : openstackget.getPortSubnetID(port)) {
                        String DES_SUB_NAME = openstackget.getResourceName(openstackget.getSubnet(DES_SUB));
                        Subnet s = openstackget.getSubnet(DES_SUB);
                        String net_ID = s.getNetworkId();
                        String NET_ID = openstackget.getResourceName(openstackget.getNetwork(net_ID));
                        Resource SUBNET = RdfOwl.createResource(model, topologyURI + ":network+" + NET_ID + ":subnet+" + DES_SUB_NAME, switchingSubnet);
                        for (IP ip2 : port.getFixedIps()) {
                            String INTERFACE_IP = ip2.getIpAddress();
                            Resource ROUTER_INTERFACE_ROUTE_NEXTHOP = RdfOwl.createResource(model, topologyURI + ":router+" + routername + ":router-interface-nexthop" + INTERFACE_IP + ":-router-interface-route-nexthop", networkAddress);
                            Resource ROUTER_INTERFACE_ROUTINGTABLE = RdfOwl.createResource(model, topologyURI + ":router+" + routername + ":router-interface-routingtable", Mrs.RoutingTable);
                            Resource ROUTER_INTERFACE_ROUTE = RdfOwl.createResource(model, topologyURI + ":router+" + routername + ":" + "interfaceip+" + INTERFACE_IP + ":router-interface-route", route);
                            //Resource ROUTER_INTERFACE_ROUTE_TO = RdfOwl.createResource(model, topologyURI + ":-router-interface-route-to "+":" + SUBNET, switchingSubnet);
                            model.add(model.createStatement(routingService, providesRoutingTable, ROUTER_INTERFACE_ROUTINGTABLE));
                            model.add(model.createStatement(routingService, providesRoute, ROUTER_INTERFACE_ROUTE));
                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTINGTABLE, hasRoute, ROUTER_INTERFACE_ROUTE));
                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTE, routeTo, SUBNET));
                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTE, nextHop, ROUTER_INTERFACE_ROUTE_NEXTHOP));

                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTE_NEXTHOP, type, "ipv4-network-address"));
                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTE_NEXTHOP, value, INTERFACE_IP));
                        }
                        /*
                         //external gateway route part
                         if (openstackget.getNetwork(openstackget.getSubnet(DES_SUB).getNetworkId()).isRouterExternal()) {
                         Resource EXTERNAL_GATEWAY_ROUTINTABLE = RdfOwl.createResource(model, topologyURI + ":-external-gateway-routingtable" + openstackget.getResourceName(r), Mrs.RoutingTable);
                         Resource EXTERNAL_GATEWAY_ROUTE = RdfOwl.createResource(model, topologyURI + ":-external-gateway-route" + openstackget.getResourceName(r), route);
                         String EXTERNAL_GATEWAY_ROUTE_TO = "0.0.0.0/0";

                         Resource EXTERNAL_GATEWAY_ROUTE_NEXTHOP = RdfOwl.createResource(model, topologyURI + ": -external-gateway-route-nexthop" + DES_SUB_NAME, nextHop);
                         //need to modify here
                         model.add(model.createStatement(routingService, providesRoutingTable, EXTERNAL_GATEWAY_ROUTINTABLE));
                         model.add(model.createStatement(EXTERNAL_GATEWAY_ROUTINTABLE, providesRoute, EXTERNAL_GATEWAY_ROUTE));
                         model.add(model.createStatement(EXTERNAL_GATEWAY_ROUTE, routeTo, EXTERNAL_GATEWAY_ROUTE_TO));
                         model.add(model.createStatement(EXTERNAL_GATEWAY_ROUTE, nextHop, EXTERNAL_GATEWAY_ROUTE_NEXTHOP));
                         }
                         */
                    }

                }
            }
            //router host route
            for (HostRoute hr : r.getRoutes()) {
                String RHOSTTO = hr.getDestination();
                String RHOSTNEXTHOP = hr.getNexthop();

                Resource RHOSTROUTINGTABLE = RdfOwl.createResource(model, topologyURI + ":routername+" + routername + ":router-host-routing-table", Mrs.RoutingTable);
                Resource RHOSTROUTE = RdfOwl.createResource(model, topologyURI + ":routername+" + routername + ":hostroute-to+" + hr.getDestination() + ":hostroute-nexthop+" + hr.getNexthop() + ":router-host-route", route);//maybe need to modify
                Resource RHOSTROUTETO = RdfOwl.createResource(model, topologyURI + ":rouerhost-route-to+" + RHOSTTO, networkAddress);
                Resource RHOSTROUTENEXTHOP = RdfOwl.createResource(model, topologyURI + ":router-host-route-nexthop+" + RHOSTNEXTHOP, networkAddress);

                model.add(model.createStatement(routingService, providesRoutingTable, RHOSTROUTINGTABLE));
                model.add(model.createStatement(routingService, providesRoute, RHOSTROUTE));
                model.add(model.createStatement(RHOSTROUTINGTABLE, hasRoute, RHOSTROUTE));
                model.add(model.createStatement(RHOSTROUTE, routeTo, RHOSTROUTETO));
                model.add(model.createStatement(RHOSTROUTE, nextHop, RHOSTROUTENEXTHOP));

                model.add(model.createStatement(RHOSTROUTETO, type, "subnet"));
                model.add(model.createStatement(RHOSTROUTETO, value, RHOSTTO));

                model.add(model.createStatement(RHOSTROUTENEXTHOP, type, "ipv4-network-address"));
                model.add(model.createStatement(RHOSTROUTENEXTHOP, value, RHOSTNEXTHOP));
            }

        }

        for (Volume v : openstackget.getVolumes()) {
            String volumeName = openstackget.getVolumeName(v);
            Resource VOLUME = RdfOwl.createResource(model, topologyURI + ":volume+" + volumeName, volume);
            model.add(model.createStatement(cinderService, providesVolume, VOLUME));
            model.add(model.createStatement(VOLUME, value, v.getVolumeType()));
            model.add(model.createStatement(VOLUME, Mrs.disk_gb, Integer.toString(v.getSize())));
        }


        /*for(Subnet s : openstackget.getSubnets()){
         String subnetId = s.getId();
         Resource SUBNET = RdfOwl.createResource(model, topologyURI + ":" + subnetId, switchingSubnet);
         Resource SUBNET_NETWORK_ADDRESS
         = RdfOwl.createResource(model, topologyURI + ":subnetnetworkaddress-" + s.getId(), networkAddress);
         model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
         //model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, s.getCidrBlock()));
         model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
        
         }
        
         
        
         //Layer 3 routing info 
         for (Router r :openstackget.getRouters()){
         String routerID = openstackget.getResourceName(r);
         Resource ROUTINGENTRIES = RdfOwl.createResource(model, topologyURI +":"+routerID+ "routinginfo" , route);
         String status = r.getStatus().toString();
         //String egNetworkID = r.getExternalGatewayInfo().getNetworkId();
         System.out.println("123123" + r.getExternalGatewayInfo());
            
         for (HostRoute h : r.getRoutes()){
         String dest = h.getDestination();
                
         System.out.println("123123" + h);
                
              
         }
            
         }

        
      
         s.getGateway() != null && !s.getGateway().isEmpty()

         */
        for (NetFloatingIP f : openstackget.getFloatingIp()) {
            Resource FLOATADD = null;
            Resource FIXEDADD = null;
            if (f.getFixedIpAddress() != null && !f.getFixedIpAddress().isEmpty() && f.getFloatingIpAddress() != null && !f.getFloatingIpAddress().isEmpty()) {
                for (Port po : openstackget.getPorts()) {
                    for (IP ips : po.getFixedIps()) {

                        if (ips.getIpAddress().equals(f.getFloatingIpAddress())) {
                            String s = ips.getSubnetId();
                            Subnet sub = openstackget.getSubnet(s);
                            Resource Subnet = model.getResource(topologyURI + ":network+"+ openstackget.getResourceName(openstackget.getNetwork(sub.getNetworkId()))+ ":subnet+" + openstackget.getResourceName(sub));

                            FLOATADD = RdfOwl.createResource(model, topologyURI +":subnet+" + openstackget.getResourceName(sub) + ":floatingip+" + f.getFloatingIpAddress(), networkAddress);
                            model.add(model.createStatement(Subnet, hasNetworkAddress, FLOATADD));
                            model.add(model.createStatement(FLOATADD, type, "floating-ip"));
                            model.add(model.createStatement(FLOATADD, value, f.getFloatingIpAddress()));
                        } else if (ips.getIpAddress().equals(f.getFixedIpAddress())) {
                            
                            String s = ips.getSubnetId();
                            Subnet sub = openstackget.getSubnet(s);
                            Resource Subnet = model.getResource(topologyURI + ":network+"+ openstackget.getResourceName(openstackget.getNetwork(sub.getNetworkId()))+ ":subnet+" + openstackget.getResourceName(sub));
                            

                            FIXEDADD = RdfOwl.createResource(model, topologyURI + ":subnet+" + openstackget.getResourceName(sub) + ":fixedip+" + f.getFixedIpAddress(), networkAddress);
                            
                            for(Server servers : openstackget.getServers()){
                                Port pt = openstackget.getPort(f.getPortId());
                                if(servers.getId().equals(pt.getDeviceId())){
                                Resource VM = RdfOwl.createResource(model, topologyURI + ":" + "server-name+" + openstackget.getServereName(servers), node);
                                model.add(model.createStatement(VM, hasNetworkAddress, FIXEDADD));
                                }
                            }
                            model.add(model.createStatement(Subnet, hasNetworkAddress, FIXEDADD));
                            
                            model.add(model.createStatement(FIXEDADD, type, "fixed-ip"));
                            model.add(model.createStatement(FIXEDADD, value, f.getFixedIpAddress()));
                        }

                    }
                }
                       // FIXEDADD = RdfOwl.createResource(model, topologyURI + ":port+" + openstackget.getResourceName(po) + ":fixedip+" + f.getFixedIpAddress(), networkAddress);
                //Resource FIXEDADD = RdfOwl.createResource(model, topologyURI + ":port+" + openstackget.getResourceName(openstackget.getPort(f.getPortId())) + ":fixedip+" + f.getFixedIpAddress(), networkAddress);
                try {
                    if (FLOATADD != null && FIXEDADD != null) {
                        model.add(model.createStatement(FIXEDADD, isAlias, FLOATADD));
                    }
                } catch (Exception e) {
                    throw new Exception(e.toString());
                }

            }
        }
       
        StringWriter out = new StringWriter();
        try {
            model.write(out, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
        }
        String ttl = out.toString();

        //System.out.println(ttl);
        

        //System.out.println(ttl);

        return model;

    }
}
