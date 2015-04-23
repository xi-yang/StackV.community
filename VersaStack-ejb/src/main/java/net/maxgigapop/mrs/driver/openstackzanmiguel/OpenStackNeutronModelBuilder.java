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
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.storage.block.Volume;

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
        Resource OpenstackTopology = RdfOwl.createResource(model, topologyURI + ":topology", topology);
        //Resource Neutron = model.createResource("urn:ogf:network:dragon.maxgigapop.net:openstack-neutron");
        Resource networkService = RdfOwl.createResource(model, topologyURI + ":openstack-neutron", Nml.NetworkObject);
        Resource directConnect = RdfOwl.createResource(model, topologyURI + ":directconnect", biPort);
        Resource routingService = RdfOwl.createResource(model, topologyURI + "routing_service", Mrs.RoutingService);
        Resource cinderService = RdfOwl.createResource(model, topologyURI + "cinder-service", blockStorageService);
        
        //port tag
        Resource PORT_TAG = RdfOwl.createResource(model, topologyURI + ":portTag", Mrs.Tag);
        model.add(model.createStatement(PORT_TAG, type, "interface"));
        model.add(model.createStatement(PORT_TAG, value, "network"));

        OpenStackGet openstackget = new OpenStackGet(url, user_name, password, tenantName);

  
        model.add(model.createStatement(OpenstackTopology, hasService, routingService));
       
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
                if(hostID == null || hostID.isEmpty()){
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
                
                for (Port port :  openstackget.getServerPorts(server)){
                    Resource Port = model.getResource(topologyURI+":"+ openstackget.getResourceName(port));  //use function
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

            for (Subnet s : openstackget.getSubnets(n.getId())) {
                String subnetId = openstackget.getResourceName(s);
                Resource SUBNET = RdfOwl.createResource(model, topologyURI + ":" + subnetId, switchingSubnet);
                model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
                Resource SUBNET_NETWORK_ADDRESS
                        = RdfOwl.createResource(model, topologyURI + ":subnetnetworkaddress-" + s.getId(), networkAddress);
                model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
                model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, s.getCidr()));
                model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
                for (Port port : openstackget.getPorts()){
                    for(String subID: openstackget.getPortSubnetID(port)){
                        if (subnetId.equals(subID)){
                        Resource Port = model.getResource(topologyURI + ":" + port.getId());
                        model.add(model.createStatement(SUBNET, hasBidirectionalPort, Port)); 
                        model.add(model.createStatement(Port, hasTag, PORT_TAG));
                        }
                        
                    }
                }
            }
        for (Volume v : openstackget.getVolumes()) {
            String volumeName = openstackget.getVolumeName(v);
            Resource VOLUME = RdfOwl.createResource(model, topologyURI + ":" + volumeName, volume);
            model.add(model.createStatement(cinderService, providesVolume, VOLUME));
            model.add(model.createStatement(VOLUME, value, v.getVolumeType()));
            model.add(model.createStatement(VOLUME, Mrs.disk_gb, Integer.toString(v.getSize())));
        }

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
