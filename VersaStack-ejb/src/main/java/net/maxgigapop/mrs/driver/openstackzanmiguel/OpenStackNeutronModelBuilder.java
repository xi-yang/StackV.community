/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.io.StringWriter;
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
import org.openstack4j.model.network.HostRoute;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.openstack.networking.domain.AddRouterInterfaceAction;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;

/**
 *
 * @author max
 */
public class OpenStackNeutronModelBuilder {

    public static OntModel createOntology(String url, String topologyURI, String user_name, String password, String tenantName) throws IOException, Exception {
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
        Property providesVM = Mrs.providesVM;
        Property type = Mrs.type;
        Property providedByService = Mrs.providedByService;
        Property providesBucket = Mrs.providesBucket;
        Property providesRoute = Mrs.providesRoute;

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
        Property hasRoute = Mrs.hasRoute;
        Property hasTag = Mrs.hasTag;
        Property hasNetworkAddress = Mrs.hasNetworkAddress;
        Property providesRoutingTable = model.createProperty(model.getNsPrefixURI("mrs") + "providesRoutingTable");

        //set the global resources
        Resource route = Mrs.Route;
        Resource hypervisorService = Mrs.HypervisorService;
        Resource virtualCloudService = Mrs.VirtualCloudService;

        Resource blockStorageService = Mrs.BlockStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology = Nml.Topology;
        Resource networkAddress = Mrs.NetworkAddress;
        Resource switchingSubnet = Mrs.SwitchingSubnet;
        Resource switchingService = Mrs.SwitchingService;
        Resource node = Nml.Node;
        Resource biPort = Nml.BidirectionalPort;

        Resource namedIndividual = model.createResource(model.getNsPrefixURI("mrs") + "NamedIndividual");
        Resource objectStorageService = Mrs.ObjectStorageService;
        //Resource OpenstackTopology = model.createResource("urn:ogf:network:dragon.maxgigapop.net:topology");
        Resource OpenstackTopology = RdfOwl.createResource(model, topologyURI, topology);
        //Resource Neutron = model.createResource("urn:ogf:network:dragon.maxgigapop.net:openstack-neutron");
        Resource networkService = RdfOwl.createResource(model, topologyURI + ":neutron-service", Nml.NetworkObject);
        Resource directConnect = RdfOwl.createResource(model, topologyURI + ":directconnect", biPort);
        Resource routingService = RdfOwl.createResource(model, topologyURI + "routing_service", Mrs.RoutingService);
        Resource cinderService = RdfOwl.createResource(model, topologyURI + "cinder-service", blockStorageService);
         
        //port tag
        Resource PORT_TAG = RdfOwl.createResource(model, topologyURI + ":portTag", Mrs.Tag);
        Resource EXTERNALNETWORK = RdfOwl.createResource(model, topologyURI+":external_network", Mrs.Tag);
        Resource TENANTNETWORK = RdfOwl.createResource(model, topologyURI+":tenant_network", Mrs.Tag);
        Resource PUBLICSUBNET = RdfOwl.createResource(model, topologyURI+":public_subnet", Mrs.Tag);
        Resource PRIVATESUBNET = RdfOwl.createResource(model, topologyURI+":private_subnet", Mrs.Tag);
        model.add(model.createStatement(PORT_TAG, type, "interface"));
        model.add(model.createStatement(PORT_TAG, value, "network"));

        OpenStackGet openstackget = new OpenStackGet(url, user_name, password, tenantName);

        model.add(model.createStatement(OpenstackTopology, hasService, routingService));
        model.add(model.createStatement(OpenstackTopology, hasService, cinderService));
        model.add(model.createStatement(OpenstackTopology, hasService, networkService));

        //Left part
        for (Port p : openstackget.getPorts()) {
            String PortID = openstackget.getResourceName(p);
            Resource PORT = RdfOwl.createResource(model, topologyURI + ":" + PortID, biPort);

            for (IP q : p.getFixedIps()) {
                if (q.getIpAddress() != null || !q.getIpAddress().isEmpty()) {
                    
                    Resource PRIVATE_ADDRESS = RdfOwl.createResource(model, topologyURI + ":" + q.getIpAddress(), networkAddress);
                    model.add(model.createStatement(PORT, hasNetworkAddress, PRIVATE_ADDRESS));
                    model.add(model.createStatement(PRIVATE_ADDRESS, type, "ipv4:private"));
                    model.add(model.createStatement(PRIVATE_ADDRESS, value, q.getIpAddress()));
                    model.add(model.createStatement(PORT, hasTag, PORT_TAG));
                }
            }
            for (Server server : openstackget.getServers()) {

                String hostID = server.getHost();
                if (hostID == null || hostID.isEmpty()) {
                    hostID = server.getHostId();
                }

                String hypervisorname = server.getHypervisorHostname();

                Resource HOST = RdfOwl.createResource(model, topologyURI + ":" + hostID, node);

                Resource HYPERVISOR = RdfOwl.createResource(model, topologyURI + ":" + hypervisorname, hypervisorService);
                Resource VM = RdfOwl.createResource(model, topologyURI + ":" + openstackget.getServereName(server), node);

                model.add(model.createStatement(OpenstackTopology, hasNode, HOST));

                model.add(model.createStatement(HOST, hasService, HYPERVISOR));
                model.add(model.createStatement(HYPERVISOR, providesVM, VM));
                model.add(model.createStatement(HOST, hasNode, VM));

                for (Port port : openstackget.getServerPorts(server)) {
                    Resource Port = model.getResource(topologyURI + ":" + openstackget.getResourceName(port));  //use function
                    model.add(model.createStatement(VM, hasBidirectionalPort, Port));
                    model.add(model.createStatement(Port, hasTag, PORT_TAG));
                }

            }
        }

        //Right subnet part
        for (Network n : openstackget.getNetworks()) {
            String networkID = openstackget.getResourceName(n);//not using the miguel function
            Resource NETWORK = RdfOwl.createResource(model, topologyURI + ":" + networkID, topology);

            model.add(model.createStatement(OpenstackTopology, hasTopology, NETWORK));

            Resource SWITCHINGSERVICE = RdfOwl.createResource(model, topologyURI + ":switchingservice-" + networkID, switchingService);

            model.add(model.createStatement(NETWORK, hasService, SWITCHINGSERVICE));

            //TO FIND THE EXTERNAL OR INTERNAL NETWORK
            if (n.isRouterExternal()) {
                model.add(model.createStatement(NETWORK, hasTag, EXTERNALNETWORK));
                for (Subnet s : n.getNeutronSubnets()) {
                    String subnetId = openstackget.getResourceName(s);
                    Resource SUBNET = RdfOwl.createResource(model, topologyURI + ":" + subnetId, switchingSubnet);
                    
                    model.add(model.createStatement(SUBNET, hasTag, PUBLICSUBNET));
                    model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
                    Resource SUBNET_NETWORK_ADDRESS
                            = RdfOwl.createResource(model, topologyURI + ":subnetnetworkaddress-" + subnetId, networkAddress);

                    model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
                    model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, s.getCidr()));
                    model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
                    
                    //FOR THE GATEWAY
                    if (s.getGateway() != null && !s.getGateway().isEmpty()) {
                        String GATEWAYADDRESS = s.getGateway();
                        Resource GATEWAY = RdfOwl.createResource(model, topologyURI + ":" + GATEWAYADDRESS, biPort);
                        model.add(model.createStatement(SUBNET, hasBidirectionalPort, GATEWAY));
                        model.add(model.createStatement(GATEWAY, hasNetworkAddress, GATEWAYADDRESS));//LEAVE FOR THE FUTHER DEVELOP FOR THE MANNER
                        model.add(model.createStatement(GATEWAY, type, "gateway"));
                    }
                    for (Port port : openstackget.getPorts()) {
                        for (String subID : openstackget.getPortSubnetID(port)) {
                            String subName = openstackget.getResourceName(openstackget.getSubnet(subID));
                            if (subnetId.equals(subName)) {
                                Resource Port = model.getResource(topologyURI + ":" + openstackget.getResourceName(port));
                                model.add(model.createStatement(SUBNET, hasBidirectionalPort, Port));
                                model.add(model.createStatement(Port, hasTag, PORT_TAG));
                                
                            }

                        }
                    }
                    
                     for (HostRoute hr : s.getHostRoutes()) {
                        String destIp = hr.getDestination();
                        String destIP = destIp.replace("/", "");
                        String nextHOP = hr.getNexthop();
                        //System.out.println("aaaa" + destIP);
                        Resource EXROUTE = RdfOwl.createResource(model, topologyURI + ":route-" + destIP + subnetId, route);
                        Resource EXROUTEFROM = RdfOwl.createResource(model, topologyURI + ":route-" + subnetId, routeFrom);
                        Resource EXROUTETO = RdfOwl.createResource(model, topologyURI + ":route-" + destIP, routeTo);
                        Resource EXNEXTHOP = RdfOwl.createResource(model, topologyURI + ":route-" + nextHOP, nextHop);

                }
              } 
            }
            
            else {
                model.add(model.createStatement(NETWORK, hasTag, TENANTNETWORK));
                for (Subnet s : n.getNeutronSubnets()) {
                    String subnetId = openstackget.getResourceName(s);
                    Resource SUBNET = RdfOwl.createResource(model, topologyURI + ":" + subnetId, switchingSubnet);
                    //Resource ROUTE = RdfOwl.createResource(model, topologyURI + ":" + s.get, switchingSubnet);
                    model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
                    Resource SUBNET_NETWORK_ADDRESS
                            = RdfOwl.createResource(model, topologyURI + ":subnetnetworkaddress-" + subnetId, networkAddress);
                    Resource ROUTINGSERVICE = RdfOwl.createResource(model, topologyURI + ":routingservice-" + subnetId, switchingService);
                    Resource ROUTE_LOCAL = RdfOwl.createResource(model, topologyURI + ":route-" + SUBNET_NETWORK_ADDRESS + subnetId, route);
                    
                    model.add(model.createStatement(SUBNET, hasTag, PRIVATESUBNET));
                    model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
                    model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, s.getCidr()));
                    model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));

                    if (s.getGateway() != null && !s.getGateway().isEmpty()) {
                        String GATEWAYADDRESS = s.getGateway();
                        Resource GATEWAY = RdfOwl.createResource(model, topologyURI + ": subnetgateway" + GATEWAYADDRESS, biPort);
                        model.add(model.createStatement(SUBNET, hasBidirectionalPort, GATEWAY));
                        model.add(model.createStatement(GATEWAY, hasNetworkAddress, GATEWAYADDRESS));//LEAVE FOR THE FUTHER DEVELOP FOR THE MANNER
                        model.add(model.createStatement(GATEWAY, type, "gateway"));
                       
                    }

                    //subnet route modeling 
                    model.add(model.createStatement(ROUTINGSERVICE, providesRoute, ROUTE_LOCAL));
                    model.add(model.createStatement(ROUTE_LOCAL, routeFrom, subnetId));
                    model.add(model.createStatement(ROUTE_LOCAL, routeTo, SUBNET_NETWORK_ADDRESS));
                    model.add(model.createStatement(ROUTE_LOCAL, nextHop, "local"));

                    //host route modeling
                    for (HostRoute hr : s.getHostRoutes()) {
                        String destIp = hr.getDestination();
                        String destIP = destIp.replace("/", "");
                        String nextHOP = hr.getNexthop();
                        //System.out.println("aaaa" + destIP);
                        Resource INROUTE = RdfOwl.createResource(model, topologyURI + ":route-" + destIP + subnetId, route);
                        Resource INROUTEFROM = RdfOwl.createResource(model, topologyURI + ":route-" + subnetId, routeFrom);
                        Resource INROUTETO = RdfOwl.createResource(model, topologyURI + ":route-" + destIP, routeTo);
                        Resource INNEXTHOP = RdfOwl.createResource(model, topologyURI + ":route-" + nextHOP, nextHop);
                        
                        /*
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoute, ROUTE));
                        model.add(model.createStatement(ROUTE, routeFrom, ROUTEFROM));
                        model.add(model.createStatement(ROUTE, routeTo, ROUTETO));
                        model.add(model.createStatement(ROUTE, nextHop, nextHOP));

                        model.add(model.createStatement(ROUTEFROM, type, "subnet"));
                        model.add(model.createStatement(ROUTEFROM, type, subnetId));

                        model.add(model.createStatement(ROUTETO, type, "ipv4-prefix"));
                        model.add(model.createStatement(ROUTETO, value, destIp));
                        */

                    }

                    for (Port port : openstackget.getPorts()) {
                        for (String subID : openstackget.getPortSubnetID(port)) {
                            String subName = openstackget.getResourceName(openstackget.getSubnet(subID));
                            if (subnetId.equals(subName)) { //not enter the if  
                                Resource Port = model.getResource(topologyURI + ":" + port.getId());
                                model.add(model.createStatement(SUBNET, hasBidirectionalPort, Port));
                                model.add(model.createStatement(Port, hasTag, PORT_TAG));
                                
                            }

                        }
                    }

                }
            }
        }
        
        //BUILDING THE ROUTING TABLE
        for(Router r : openstackget.getRouters()){
            String routerId = openstackget.getResourceName(r);
            
            
        }
        
        
        NeutronRouterInterface ri = new NeutronRouterInterface();
        
        
        
        for (Volume v : openstackget.getVolumes()) {
            String volumeName = openstackget.getVolumeName(v);
            Resource VOLUME = RdfOwl.createResource(model, topologyURI + ":" + volumeName, volume);
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
        
        
        
        
        
        
        
        
        
        
        
         */
        StringWriter out = new StringWriter();
        try {
            model.write(out, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
        }
        String ttl = out.toString();
        System.out.println(ttl);
        return model;

    }
}
