/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Zan Wang 2015
 * Modified by: Xi Yang 2015-2016
 * Modified by: Adam Smith 2017

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
package net.maxgigapop.mrs.driver.openstack;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.ResourceTool;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.network.HostRoute;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Pool;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.openstack.compute.domain.NovaFloatingIP;

/**
 *
 * @author max
 */
public class OpenStackNeutronModelBuilder {
    
    public static final StackLogger logger = OpenStackDriver.logger;

    public static OntModel createOntology(String url, String NATServer, String topologyURI, String user_name, String password, String tenantName,
            String adminUsername, String adminPassword, String adminTenant, OntModel modelExt) throws IOException, Exception {
        String method = "createOntology";
        ArrayList<String> fips = new ArrayList();

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
        Property batch = Mrs.batch;
        Property providedByService = Mrs.providedByService;
        Property providesBucket = Mrs.providesBucket;
        Property providesRoute = Mrs.providesRoute;
        Property hasRoute = Mrs.hasRoute;
        Property order = Mrs.order;

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
        Resource OpenstackTopology = RdfOwl.createResource(model, topologyURI, topology);
        Resource networkService = RdfOwl.createResource(model, ResourceTool.getResourceUri(topologyURI + ":networkservice", OpenstackPrefix.networkService, ""), Mrs.VirtualCloudService);
        Resource routingService = RdfOwl.createResource(model, ResourceTool.getResourceUri(topologyURI + ":routingservice", OpenstackPrefix.routingService, ""), RoutingService);
        Resource cinderService = RdfOwl.createResource(model, ResourceTool.getResourceUri(topologyURI + ":cinderservice", OpenstackPrefix.cinderService, ""), blockStorageService);

        //port tag
        OpenStackGet openstackget = new OpenStackGet(url, NATServer, user_name, password, tenantName);

        if (adminUsername != null && adminPassword != null && adminTenant != null) {
            Authenticate authenticate = new Authenticate();
            OSClient adminClient = authenticate.openStackAuthenticate(url, NATServer, adminUsername, adminPassword, adminTenant);
            openstackget.fetchAddResources(adminClient);
        }

        model.add(model.createStatement(OpenstackTopology, hasService, routingService));
        model.add(model.createStatement(OpenstackTopology, hasService, networkService));
        model.add(model.createStatement(OpenstackTopology, hasService, cinderService));

        for (Hypervisor hv : openstackget.getHypervisors()) {
            String hypervisorName = hv.getHypervisorHostname(); 
            String hostName = hypervisorName.split("\\.")[0]; 
            Resource HOST = RdfOwl.createResource(model, topologyURI + ":" + "host+" + hostName, Nml.Node);
            Resource HYPERVISOR = RdfOwl.createResource(model, topologyURI + ":" + "hypervisor+" + hypervisorName, Mrs.HypervisorService);
            model.add(model.createStatement(OpenstackTopology, hasNode, HOST));
            model.add(model.createStatement(HOST, hasService, HYPERVISOR));
            // hypervisor / host available resources 
            model.add(model.createStatement(HOST, Mrs.num_core, Integer.toString(hv.getVirtualCPU())));
            model.add(model.createStatement(HOST, Mrs.memory_mb, Integer.toString(hv.getLocalMemory())));
            model.add(model.createStatement(HOST, Mrs.disk_gb, Integer.toString(hv.getLocalDisk())));
            model.add(model.createStatement(HYPERVISOR, Mrs.num_core, Integer.toString(hv.getVirtualUsedCPU())));
            model.add(model.createStatement(HYPERVISOR, Mrs.memory_mb, Integer.toString(hv.getLocalMemoryUsed())));
            model.add(model.createStatement(HYPERVISOR, Mrs.disk_gb, Integer.toString(hv.getLocalDiskUsed())));
        }
        
        for (Port p : openstackget.getPorts()) {
            String PortID = openstackget.getResourceName(p);
            Resource PORT = RdfOwl.createResource(model, ResourceTool.getResourceUri(PortID, OpenstackPrefix.PORT, PortID), biPort);
            for (IP q : p.getFixedIps()) {
                if (q.getIpAddress() != null || !q.getIpAddress().isEmpty()) {
                    String s = q.getSubnetId();
                    Subnet sub = openstackget.getSubnet(s);
                    Resource FIXEDIP = RdfOwl.createResource(model, topologyURI + ":subnet+" + openstackget.getResourceName(sub) + ":fixedip+" + q.getIpAddress(), networkAddress);
                    model.add(model.createStatement(PORT, hasNetworkAddress, FIXEDIP));
                    model.add(model.createStatement(FIXEDIP, value, q.getIpAddress()));
                    if (openstackget.getNetwork(p.getNetworkId()).isRouterExternal()) {
                        model.add(model.createStatement(FIXEDIP, type, "ipv4:public"));
                    } else {
                        model.add(model.createStatement(FIXEDIP, type, "ipv4:private"));
                    }
                }
            }
        }

        for (Server server : openstackget.getServers()) {

            String hostID = server.getHost();
            if (hostID == null || hostID.isEmpty()) {
                hostID = server.getHostId();
            }

            String hypervisorname = server.getHypervisorHostname();
            String imageid = server.getImageId();
            String flavorid = server.getFlavorId();
            String keypair = server.getKeyName();
            String server_name = openstackget.getServerName(server);

            Resource HOST = RdfOwl.createResource(model, topologyURI + ":" + "host+" + hostID, node);

            Resource HYPERVISOR = RdfOwl.createResource(model, topologyURI + ":" + "hypervisor+" + hypervisorname, hypervisorService);
            Resource VM = RdfOwl.createResource(model, ResourceTool.getResourceUri(server_name, OpenstackPrefix.vm, server_name), node);

            model.add(model.createStatement(OpenstackTopology, hasNode, HOST));

            model.add(model.createStatement(HOST, hasService, HYPERVISOR));
            model.add(model.createStatement(HYPERVISOR, providesVM, VM));
            model.add(model.createStatement(HOST, hasNode, VM));
            model.add(model.createStatement(VM, type, "image+" + imageid));
            model.add(model.createStatement(VM, type, "flavor+" + flavorid));
            model.add(model.createStatement(VM, type, "keypair+" + keypair));
            model.add(model.createStatement(VM, Mrs.num_core, Integer.toString(server.getFlavor().getVcpus())));
            model.add(model.createStatement(VM, Mrs.memory_mb, Integer.toString(server.getFlavor().getRam())));
            model.add(model.createStatement(VM, Mrs.disk_gb, Integer.toString(server.getFlavor().getDisk())));
                        
            
           //check for batch creation and add the batch property
           Map<String,String> serverMetaData = server.getMetadata();
           if(serverMetaData.containsKey("has batch"))
           {model.add(model.createStatement(VM, batch, serverMetaData.get("has batch")));}
            
            for (Port port : openstackget.getServerPorts(server)) {
                String PortName = openstackget.getResourceName(port);
                Resource Port = RdfOwl.createResource(model, ResourceTool.getResourceUri(PortName, OpenstackPrefix.PORT, PortName), biPort);
                model.add(model.createStatement(VM, hasBidirectionalPort, Port));
            }
            Map<String, String> metadata = openstackget.getMetadata(server);
            //Linux and Quagga BGP routing tables  
            Resource vmRoutingSvc = null;
            Resource vmLinuxRtTable = null;
            int linuxRouteNum = 1;
            while (metadata != null && metadata.containsKey("linux:route:" + linuxRouteNum)) {
                if (vmRoutingSvc == null) {
                    vmRoutingSvc = RdfOwl.createResource(model, ResourceTool.getResourceUri(server_name + ":routingservice", OpenstackPrefix.routingService, server_name), Mrs.RoutingService);
                    model.add(model.createStatement(VM, Nml.hasService, vmRoutingSvc));
                    vmLinuxRtTable = RdfOwl.createResource(model, vmRoutingSvc.getURI() + ":routingtable+linux", Mrs.RoutingTable);
                    model.add(model.createStatement(vmRoutingSvc, Mrs.providesRoutingTable, vmLinuxRtTable));
                }
                JSONParser parser = new JSONParser();
                try {
                    String metaJson = (String) metadata.get("linux:route:" + linuxRouteNum);
                    metaJson = metaJson.replaceAll("\'", "\"");
                    JSONObject linuxRoute = (JSONObject) parser.parse(metaJson);
                    if (!linuxRoute.containsKey("status") || !linuxRoute.get("status").toString().equals("up")) {
                        linuxRouteNum++;
                        continue;
                    }
                    String routeUri = null;
                    if (linuxRoute.containsKey("uri")) {
                        routeUri = linuxRoute.get("uri").toString();
                    } else {
                        routeUri = vmLinuxRtTable.getURI() + ":route+" + linuxRouteNum;
                    }
                    Resource resLinuxRoute = RdfOwl.createResource(model, routeUri, Mrs.Route);
                    model.add(model.createStatement(vmLinuxRtTable, Mrs.hasRoute, resLinuxRoute));
                    if (linuxRoute.containsKey("to")) {
                        String netAddr = linuxRoute.get("to").toString();
                        Resource resNetAddr = RdfOwl.createResource(model, resLinuxRoute.getURI() + ":route_to", Mrs.NetworkAddress);
                        model.add(model.createStatement(resLinuxRoute, Mrs.routeTo, resNetAddr));
                        model.add(model.createStatement(resNetAddr, Mrs.type, "ipv4-prefix"));
                        model.add(model.createStatement(resNetAddr, Mrs.value, netAddr));
                    }
                    if (linuxRoute.containsKey("from")) {
                        String netAddr = linuxRoute.get("from").toString();
                        Resource resNetAddr = RdfOwl.createResource(model, resLinuxRoute.getURI() + ":route_from", Mrs.NetworkAddress);
                        model.add(model.createStatement(resLinuxRoute, Mrs.routeTo, resNetAddr));
                        model.add(model.createStatement(resNetAddr, Mrs.type, "ipv4-prefix-list"));
                        model.add(model.createStatement(resNetAddr, Mrs.value, netAddr));
                    }
                    if (linuxRoute.containsKey("via")) {
                        String netAddr = linuxRoute.get("via").toString();
                        Resource resNetAddr = RdfOwl.createResource(model, resLinuxRoute.getURI() + ":next_hop", Mrs.NetworkAddress);
                        model.add(model.createStatement(resLinuxRoute, Mrs.routeTo, resNetAddr));
                        model.add(model.createStatement(resNetAddr, Mrs.type, "ipv4-address"));
                        model.add(model.createStatement(resNetAddr, Mrs.value, netAddr));
                    }
                    if (linuxRoute.containsKey("dev")) {
                        String netAddr = linuxRoute.get("dev").toString();
                        Resource resNetAddr = RdfOwl.createResource(model, resLinuxRoute.getURI() + ":next_hop_dev", Mrs.NetworkAddress);
                        model.add(model.createStatement(resLinuxRoute, Mrs.routeTo, resNetAddr));
                        model.add(model.createStatement(resNetAddr, Mrs.type, "device"));
                        model.add(model.createStatement(resNetAddr, Mrs.value, netAddr));
                    }
                } catch (ParseException e) {
                    logger.catching(method, e);
                }
                linuxRouteNum++;
            }
            
            if (metadata != null && metadata.containsKey("quagga:bgp:info")) {
                if (vmRoutingSvc == null) {
                    vmRoutingSvc = RdfOwl.createResource(model, ResourceTool.getResourceUri(server_name + ":routingservice", OpenstackPrefix.routingService, server_name), Mrs.RoutingService);
                    model.add(model.createStatement(VM, Nml.hasService, vmRoutingSvc));
                }
                Resource vmQuaggaBgp = RdfOwl.createResource(model, vmRoutingSvc.getURI() + ":routingtable+quagga_bgp", Mrs.RoutingTable);
                model.add(model.createStatement(vmRoutingSvc, Mrs.providesRoutingTable, vmQuaggaBgp));
                int neighborNum = 1;
                while (metadata.containsKey("quagga:bgp:neighbor:"+neighborNum)) {
                    JSONParser parser = new JSONParser();
                    try {
                        String metaJson = (String) metadata.get("quagga:bgp:neighbor:" + neighborNum);
                        metaJson = metaJson.replaceAll("\'", "\"");
                        JSONObject bgpNeighbor = (JSONObject) parser.parse(metaJson);
                        if (!bgpNeighbor.containsKey("remote_ip") || !bgpNeighbor.containsKey("as_number")) {
                            continue;
                        }
                        String remoteIp = (String) bgpNeighbor.get("remote_ip");
                        String remoteAsn = (String) bgpNeighbor.get("as_number");
                        if (remoteIp.contains("/")) {
                            remoteIp = remoteIp.split("/")[0];
                        }
                        Resource resRouteToNeighbor = RdfOwl.createResource(model, vmQuaggaBgp.getURI() + ":neighbor+" + remoteIp.replaceAll("[.\\/]", "_"), Mrs.Route);
                        model.add(model.createStatement(vmQuaggaBgp, Mrs.hasRoute, resRouteToNeighbor));
                        // route NetAddresses
                        Resource resNetAddrRemoteAsn = RdfOwl.createResource(model, resRouteToNeighbor.getURI() + ":remote_asn", Mrs.NetworkAddress);
                        model.add(model.createStatement(resRouteToNeighbor, Mrs.hasNetworkAddress, resNetAddrRemoteAsn));
                        model.add(model.createStatement(resNetAddrRemoteAsn, Mrs.type, "bgp-asn"));
                        model.add(model.createStatement(resNetAddrRemoteAsn, Mrs.value, remoteAsn));
                        if (bgpNeighbor.containsKey("local_ip")) {
                            String localIp = (String) bgpNeighbor.get("local_ip");
                            if (localIp.contains("/")) {
                                localIp = localIp.split("/")[0];
                            }
                            Resource resNetAddrLocalIp = RdfOwl.createResource(model, resRouteToNeighbor.getURI() + ":local_ip", Mrs.NetworkAddress);
                            model.add(model.createStatement(resRouteToNeighbor, Mrs.hasNetworkAddress, resNetAddrLocalIp));
                            model.add(model.createStatement(resNetAddrLocalIp, Mrs.type, "ipv4-local")); // ? ipv4-address
                            model.add(model.createStatement(resNetAddrLocalIp, Mrs.value, localIp));
                        }
                        if (bgpNeighbor.containsKey("bgp_authkey")) {
                            String authkey = (String) bgpNeighbor.get("bgp_authkey");
                            Resource resNetAddrAuthkey = RdfOwl.createResource(model, resRouteToNeighbor.getURI() + ":bgp_authkey", Mrs.NetworkAddress);
                            model.add(model.createStatement(resRouteToNeighbor, Mrs.hasNetworkAddress, resNetAddrAuthkey));
                            model.add(model.createStatement(resNetAddrAuthkey, Mrs.type, "bgp-authkey"));
                            model.add(model.createStatement(resNetAddrAuthkey, Mrs.value, authkey));
                        }
                        // nextHop
                        Resource resNetAddrRemoteIp = RdfOwl.createResource(model, resRouteToNeighbor.getURI() + ":remote_ip", Mrs.NetworkAddress);
                        model.add(model.createStatement(resRouteToNeighbor, Mrs.nextHop, resNetAddrRemoteIp));
                        model.add(model.createStatement(resNetAddrRemoteIp, Mrs.type, "ipv4-remote")); // ? ipv4-address
                        model.add(model.createStatement(resNetAddrRemoteIp, Mrs.value, remoteIp));
                        // routeFrom
                        if (bgpNeighbor.containsKey("export_prefixes")) {
                            JSONArray jsonPrefixList = (JSONArray) bgpNeighbor.get("export_prefixes");
                            String prefixList = "";
                            for (Object obj: jsonPrefixList) {
                                if (!prefixList.isEmpty()) {
                                    prefixList += ",";
                                }
                                prefixList += obj.toString();
                             }
                            Resource resNetAddrLocalPrefixes = RdfOwl.createResource(model, vmQuaggaBgp.getURI() + ":local_prefix_list", Mrs.NetworkAddress);
                            model.add(model.createStatement(resNetAddrLocalPrefixes, Mrs.type, "ipv4-prefix-list")); 
                            model.add(model.createStatement(resNetAddrLocalPrefixes, Mrs.value, prefixList));
                            model.add(model.createStatement(resRouteToNeighbor, Mrs.routeFrom, resNetAddrLocalPrefixes));
                        }
                    } catch (ParseException e) {
                        logger.catching(method, e);
                    }
                    neighborNum++;
                }
            }
            //UCS STIOV special handling
            if (metadata != null && metadata.containsKey("sriov_vnic:status") && metadata.get("sriov_vnic:status").equals("up")) {
                if (vmRoutingSvc == null) {
                    vmRoutingSvc = RdfOwl.createResource(model, ResourceTool.getResourceUri(server_name + ":routingservice", OpenstackPrefix.routingService, server_name), Mrs.RoutingService);
                    model.add(model.createStatement(VM, Nml.hasService, vmRoutingSvc));
                }
                int sriovVnicNum = 1;
                while (modelExt != null) {
                    String sriovVnicKey = String.format("sriov_vnic:%d", sriovVnicNum);
                    if (!metadata.containsKey(sriovVnicKey)) {
                        break;
                    }
                    String sriovVnicJson = metadata.get(sriovVnicKey);
                    JSONParser parser = new JSONParser();
                    try {
                        sriovVnicJson = sriovVnicJson.replaceAll("'", "\""); // tolerate single quotes
                        JSONObject jsonObj = (JSONObject) parser.parse(sriovVnicJson);
                        // interface
                        if (!jsonObj.containsKey("interface") || !jsonObj.containsKey("profile")) {
                            logger.warning(method, String.format("modeling server '%s' SRIOV interface without both 'interface' and 'profile' parameters in metadata ''%s'", server_name, sriovVnicKey));
                            sriovVnicNum++;
                            continue;
                        }
                        String vnicName = (String) jsonObj.get("interface");
                        Resource sriovPort = RdfOwl.createResource(model, ResourceTool.getResourceUri(vnicName, OpenstackPrefix.PORT, vnicName), Nml.BidirectionalPort);
                        model.add(model.createStatement(VM, Nml.hasBidirectionalPort, sriovPort));
                        String sparql = "SELECT ?vmfex WHERE {"
                                + String.format("<%s> nml:hasService ?vmfex . ", HOST)
                                + "?vmfex a mrs:HypervisorBypassInterfaceService . "
                                + "}";
                        ResultSet r = ModelUtil.sparqlQuery(modelExt, sparql);
                        Resource vmfexSvc;
                        if (r.hasNext()) {
                            vmfexSvc = r.next().getResource("vmfex");
                        } else {
                            vmfexSvc = RdfOwl.createResource(model, HOST.getURI() + ":vmfex", Mrs.HypervisorBypassInterfaceService);
                        }
                        model.add(model.createStatement(vmfexSvc, Mrs.providesVNic, sriovPort));
                        //profile
                        String portProfile = (String) jsonObj.get("profile");
                        sparql = "SELECT ?port_profile WHERE {"
                                + "?port_profile a mrs:SwitchingSubnet . "
                                + "?port_profile mrs:type \"Cisco_UCS_Port_Profile\" . "
                                + String.format("?port_profile mrs:value \"Cisco_UCS_Port_Profile+%s\". ", portProfile)
                                + "}";
                        r = ModelUtil.sparqlQuery(modelExt, sparql);
                        if (!r.hasNext()) {
                            logger.warning(method, String.format("modeling server '%s' SRIOV interface without 'profile'='%s' already being defined in modelExtention", server_name, portProfile));
                            sriovVnicNum++;
                            continue;
                        }
                        Resource profileSwSubnet = r.next().getResource("port_profile");
                        model.add(model.createStatement(profileSwSubnet, Nml.hasBidirectionalPort, sriovPort));
                        // ipaddr
                        if (jsonObj.containsKey("ipaddr") && !((String) jsonObj.get("ipaddr")).isEmpty()) {
                            String ip = ((String) jsonObj.get("ipaddr")).replaceAll("/", "_");
                            Resource vnicIP = RdfOwl.createResource(model, ResourceTool.getResourceUri(ip, OpenstackPrefix.public_address, server_name + ":" + vnicName, ip), Mrs.NetworkAddress);
                            model.add(model.createStatement(vnicIP, Mrs.type, "ipv4-address"));
                            model.add(model.createStatement(vnicIP, Mrs.value, (String) jsonObj.get("ipaddr")));
                            model.add(model.createStatement(sriovPort, Mrs.hasNetworkAddress, vnicIP));
                        }
                        // macaddr
                        if (jsonObj.containsKey("macaddr") && !((String) jsonObj.get("macaddr")).isEmpty()) {
                            String mac = ((String) jsonObj.get("macaddr")).replaceAll(":", "_");
                            Resource vnicMAC = RdfOwl.createResource(model, ResourceTool.getResourceUri(mac, OpenstackPrefix.mac_address, server_name + ":" + vnicName, mac), Mrs.NetworkAddress);
                            model.add(model.createStatement(vnicMAC, Mrs.type, "mac-address"));
                            model.add(model.createStatement(vnicMAC, Mrs.value, (String) jsonObj.get("macaddr")));
                            model.add(model.createStatement(sriovPort, Mrs.hasNetworkAddress, vnicMAC));
                        }

                        // routes from RoutingService for VM
                        if (jsonObj.containsKey("routes")) {
                            JSONArray jsonRoutes = (JSONArray) jsonObj.get("routes");
                            for (Object obj : jsonRoutes) {
                                JSONObject jsonRoute = (JSONObject) obj;
                                if (!jsonRoute.containsKey("to") || !jsonRoute.containsKey("via")) {
                                    continue;
                                }
                                String strRouteTo = ((String) jsonRoute.get("to")).replaceAll("/", "");
                                String strRouteVia = ((String) jsonRoute.get("via")).replaceAll("/", "");
                                Resource vnicRoute = RdfOwl.createResource(model, sriovPort.getURI() + ":route+to-" + strRouteTo + "-via-" + strRouteVia, Mrs.Route);
                                model.add(model.createStatement(vmRoutingSvc, Mrs.providesRoute, vnicRoute));
                                Resource vnicRouteTo = RdfOwl.createResource(model, sriovPort.getURI() + ":routeto+" + strRouteTo, Mrs.NetworkAddress);
                                model.add(model.createStatement(vnicRouteTo, Mrs.type, "ipv4-prefix"));
                                model.add(model.createStatement(vnicRouteTo, Mrs.value, strRouteTo));
                                model.add(model.createStatement(vnicRoute, Mrs.routeTo, vnicRouteTo));
                                Resource vnicRouteVia = RdfOwl.createResource(model, sriovPort.getURI() + ":next+" + strRouteVia, Mrs.NetworkAddress);
                                model.add(model.createStatement(vnicRouteVia, Mrs.type, "ipv4-address"));
                                model.add(model.createStatement(vnicRouteVia, Mrs.value, strRouteVia));
                                model.add(model.createStatement(vnicRoute, Mrs.nextHop, vnicRouteVia));
                            }
                        }
                    } catch (ParseException e) {
                        logger.warning(method,  String.format("cannot parse server '%s' metadata '%s' for '%s' ", server.getName(), sriovVnicJson, sriovVnicKey));
                    }
                    sriovVnicNum++;
                }
            }
            // Ceph RBD block storage / volumes
            int cephRbdNum = 1;
            while (metadata != null && metadata.containsKey("ceph_rbd:" + cephRbdNum)) {
                String cephRbdKey = "ceph_rbd:" + cephRbdNum;
                cephRbdNum++;
                String cephRbdJson = metadata.get(cephRbdKey);
                JSONParser parser = new JSONParser();
                try {
                    cephRbdJson = cephRbdJson.replaceAll("'", "\""); // tolerate single quotes
                    JSONObject jsonObj = (JSONObject) parser.parse(cephRbdJson);
                    if (!jsonObj.containsKey("volume") || !jsonObj.containsKey("size") || !jsonObj.containsKey("status")) {
                        logger.warning(method, String.format("modeling server '%s' Ceph RBD requires both 'volume', 'size' and 'status' parameters in metadata ''%s'", server_name, cephRbdKey));
                        continue;
                    }
                    if (!jsonObj.get("status").equals("up")) {
                        continue;
                    }
                    // the VM server hasVolume
                    String volumeName = (String) jsonObj.get("volume");
                    String diskSize =  (String) jsonObj.get("size");
                    Long longDiskSize = Long.parseLong(diskSize)/1024;
                    diskSize = longDiskSize.toString();
                    String mountPoint =  (String) jsonObj.get("mount");
                    Resource resVolume = RdfOwl.createResource(model, ResourceTool.getResourceUri(volumeName, OpenstackPrefix.volume, volumeName), Mrs.Volume);
                    model.add(model.createStatement(VM, Mrs.hasVolume, resVolume));
                    if (diskSize != null) {
                        model.add(model.createStatement(resVolume, Mrs.disk_gb, diskSize));
                    }
                    if (mountPoint != null) {
                        model.add(model.createStatement(resVolume, Mrs.mount_point, mountPoint));
                    }
                    // find the ceph blockstorage service that providesVolume
                    String sparql = "SELECT ?cephrbd WHERE {"
                            + "?cephrbd a mrs:BlockStorageService. "
                            + "?cephrbd mrs:type  \"ceph-rbd\". "
                            + "}";
                    ResultSet r = ModelUtil.sparqlQuery(modelExt, sparql);
                    if (r.hasNext()) {
                        Resource resCephRbd = r.next().getResource("cephrbd");
                        model.add(model.createStatement(resCephRbd, Mrs.providesVolume, resVolume));
                    }
                } catch (ParseException e) {
                    logger.warning(method, String.format("cannot parse server '%s' metadata '%s' for '%s' ", server.getName(), cephRbdJson, cephRbdKey));
                }
            }
            // Ceph FS Service
            if (metadata != null && metadata.containsKey("ceph_fs")) {
                String cephfsJson = metadata.get("ceph_fs");
                JSONParser parser = new JSONParser();
                try {
                    cephfsJson = cephfsJson.replaceAll("'", "\""); // single quotes into double quotes
                    JSONObject jsonObj = (JSONObject) parser.parse(cephfsJson);
                    if (!jsonObj.get("status").equals("up")) {
                        continue;
                    }
                    String volumeName = (String) jsonObj.get("volume");
                    String mountPoint =  (String) jsonObj.get("mount");
                    Resource resVolume = RdfOwl.createResource(model, ResourceTool.getResourceUri(volumeName, OpenstackPrefix.volume, volumeName), Mrs.Volume);
                    model.add(model.createStatement(VM, Mrs.hasVolume, resVolume));
                    if (mountPoint != null) {
                        model.add(model.createStatement(resVolume, Mrs.mount_point, mountPoint));
                    }
                    String cephfsSubdir =  (String) jsonObj.get("subdir");
                    if (cephfsSubdir != null) {
                        Resource subdirAddr = RdfOwl.createResource(model, resVolume.getURI()+":cephfs_subdir", Mrs.NetworkAddress);
                        model.add(model.createStatement(resVolume, Mrs.hasNetworkAddress, subdirAddr));
                        model.add(model.createStatement(subdirAddr, Mrs.type, "cephfs-subdir"));
                        model.add(model.createStatement(subdirAddr, Mrs.value, cephfsSubdir));
                    }
                    String cephfsClient =  (String) jsonObj.get("client");
                    if (cephfsSubdir != null) {
                        Resource clientAddr = RdfOwl.createResource(model, resVolume.getURI()+":cephfs_client", Mrs.NetworkAddress);
                        model.add(model.createStatement(resVolume, Mrs.hasNetworkAddress, clientAddr));
                        model.add(model.createStatement(clientAddr, Mrs.type, "cephfs-client"));
                        model.add(model.createStatement(clientAddr, Mrs.value, cephfsClient));
                    }
                    // find the ceph blockstorage service that providesVolume
                    String sparql = "SELECT ?cephfs WHERE {"
                            + "?cephfs a mrs:BlockStorageService. "
                            + "?cephfs mrs:type  \"ceph-fs\". "
                            + "}";
                    ResultSet r = ModelUtil.sparqlQuery(modelExt, sparql);
                    if (r.hasNext()) {
                        Resource resCephRbd = r.next().getResource("cephfs");
                        model.add(model.createStatement(resCephRbd, Mrs.providesVolume, resVolume));
                    }
                } catch (ParseException e) {
                    logger.warning(method, String.format("cannot parse server '%s' metadata '%s' for '%s' ", server.getName(), cephfsJson, "cephfs:info"));
                }
            }
            //Strongswan ipsec vpn
            if (metadata != null && metadata.containsKey("ipsec")) {
                String input = metadata.get("ipsec");
                String endpointUri = metadata.get("ipsec:uri");
                JSONParser j = new JSONParser();
                
                try {
                    input = input.replaceAll("'", "\"");
                    endpointUri = endpointUri.replaceAll("'", "\"");
                    
                    JSONObject jdata = (JSONObject) j.parse(input);
                    if (!jdata.get("status").equals("up")) {
                        continue;
                    }
                    
                    //JSONObject jUri = (JSONObject) j.parse(uri);
                    //String endpointUri = jUri.get("uri").toString();
                    //provide a default uri if ipsec:uri is empty
                    if (endpointUri == null) endpointUri = VM+":vpn";
                    Resource strongswan = RdfOwl.createResource(model, endpointUri, Mrs.EndPoint);
                    model.add(model.createStatement(VM, Nml.hasService, strongswan));
                    
                    /*
                    Get the following values:
                    local ip            -> "local-ip"
                    local subnet cidr   -> "local-subnet"
                    remote subnet cidr  -> "remote-subnet"
                    t1,t2 remote ip     -> "remote-ip-n"
                    t1,t2 secret        -> not included in model
                    */
                    String localIPStr = jdata.get("local_ip").toString();
                    String localSubnetCIDR = jdata.get("local_subnet").toString();
                    String remoteSubnetCIDR = jdata.get("remote_subnet").toString();
                    
                    if (localIPStr == null) localIPStr = "null";
                    if (localSubnetCIDR == null) localSubnetCIDR = "null";
                    if (remoteSubnetCIDR == null) remoteSubnetCIDR = "null";
                    
                    //add local ip to model
                    Resource localIp = RdfOwl.createResource(model, endpointUri+":local-ip", Mrs.NetworkAddress);
                    model.add(model.createStatement(strongswan, Mrs.hasNetworkAddress, localIp));
                    model.add(model.createStatement(localIp, Mrs.type, "ipv4-address"));
                    model.add(model.createStatement(localIp, Mrs.value, localIPStr));
                    //add local subnet
                    Resource localSubnet = RdfOwl.createResource(model, endpointUri+":local-subnet", Mrs.NetworkAddress);
                    model.add(model.createStatement(strongswan, Mrs.hasNetworkAddress, localSubnet));
                    model.add(model.createStatement(localSubnet, Mrs.type, "ipv4-prefix-list"));
                    model.add(model.createStatement(localSubnet, Mrs.value, localSubnetCIDR));
                    //add remote subnet
                    Resource remoteSubnet = RdfOwl.createResource(model, endpointUri+":remote-subnet", Mrs.NetworkAddress);
                    model.add(model.createStatement(strongswan, Mrs.hasNetworkAddress, remoteSubnet));
                    model.add(model.createStatement(remoteSubnet, Mrs.type, "ipv4-prefix-list"));
                    model.add(model.createStatement(remoteSubnet, Mrs.value, remoteSubnetCIDR));
                    
                    //add tunnels
                    int i = 1;
                    String tunnelStr = "remote_ip_1";
                    ArrayList <String> tunnelIPs = new ArrayList<>();
                    
                    while (jdata.containsKey(tunnelStr)) {
                        tunnelIPs.add( (String) jdata.get(tunnelStr));
                        tunnelStr = "remote_ip_" + (++i);
                    }
                    
                    i = 0;
                    tunnelStr = endpointUri+":tunnel";
                    for (String ip : tunnelIPs) {
                        tunnelStr = endpointUri + ":tunnel" + (++i);
                        Resource tunnel = RdfOwl.createResource(model, tunnelStr, Nml.BidirectionalPort);
                        model.add(model.createStatement(strongswan, Nml.hasBidirectionalPort, tunnel));
                        Resource remoteIp = RdfOwl.createResource(model, tunnelStr+":remote-ip", Mrs.NetworkAddress);
                        model.add(model.createStatement(tunnel, Mrs.hasNetworkAddress, remoteIp));
                        model.add(model.createStatement(remoteIp, Mrs.type, "ipv4-address"));
                        model.add(model.createStatement(remoteIp, Mrs.value, ip));
                        Resource secret = RdfOwl.createResource(model,tunnelStr+":secret", Mrs.NetworkAddress);
                        model.add(model.createStatement(tunnel, Mrs.hasNetworkAddress, secret));
                        model.add(model.createStatement(secret, Mrs.type, "secret"));
                        model.add(model.createStatement(secret, Mrs.value, "####"));
                    }
                } catch (Exception e) {
                    logger.warning(method, String.format("cannot parse server '%s' metadata '%s' for '%s' ", server.getName(), input, "ipsec"));
                }
            }
            
            // Globus Connect Service
            if (metadata != null && metadata.containsKey("globus:info")) {
                String globusJson = metadata.get("globus:info");
                JSONParser parser = new JSONParser();
                try {
                    globusJson = globusJson.replaceAll("'", "\""); // single quotes into double quotes
                    JSONObject jsonObj = (JSONObject) parser.parse(globusJson);
                    if (!jsonObj.get("status").equals("up")) {
                        continue;
                    }
                    String shortName = jsonObj.get("shortname").toString();
                    String userName = jsonObj.get("user").toString();
                    //## Do not expose password
                    String defautDir = jsonObj.get("directory").toString();
                    String dataInterface = jsonObj.get("interface").toString();
                    String endpointUri = "";
                    if (metadata.containsKey("globus:info:uri")) {
                        endpointUri = metadata.get("globus:info:uri");
                    } else {
                        endpointUri = VM.getURI() + ":globus+" + shortName;
                    }
                    Resource resGlobus = RdfOwl.createResource(model, endpointUri, Mrs.EndPoint);
                    model.add(model.createStatement(VM, Nml.hasService, resGlobus));
                    model.add(model.createStatement(resGlobus, Nml.name, shortName));
                    model.add(model.createStatement(resGlobus, Mrs.type, "globus:connect"));
                    if (!userName.isEmpty()) {
                        Resource resNA = RdfOwl.createResource(model, endpointUri+":username", Mrs.NetworkAddress);
                        model.add(model.createStatement(resNA, Mrs.type, "globus:username"));
                        model.add(model.createStatement(resNA, Mrs.value, userName));
                        model.add(model.createStatement(resGlobus, Mrs.hasNetworkAddress, resNA));
                    }
                    if (!defautDir.isEmpty()) {
                        Resource resNA = RdfOwl.createResource(model, endpointUri+":directory", Mrs.NetworkAddress);
                        model.add(model.createStatement(resNA, Mrs.type, "globus:directory"));
                        model.add(model.createStatement(resNA, Mrs.value, defautDir));
                        model.add(model.createStatement(resGlobus, Mrs.hasNetworkAddress, resNA));
                    }
                    if (!dataInterface.isEmpty()) {
                        Resource resNA = RdfOwl.createResource(model, endpointUri+":interface", Mrs.NetworkAddress);
                        model.add(model.createStatement(resNA, Mrs.type, "globus:interface"));
                        model.add(model.createStatement(resNA, Mrs.value, dataInterface));
                        model.add(model.createStatement(resGlobus, Mrs.hasNetworkAddress, resNA));
                    }
                } catch (ParseException e) {
                    logger.warning(method, String.format("cannot parse server '%s' metadata '%s' for '%s' ", server.getName(), globusJson, "globus:info"));
                }
            }
            // NFS Service
            if (metadata != null && metadata.containsKey("nfs:info")) {
                String nfsJson = metadata.get("nfs:info");
                JSONParser parser = new JSONParser();
                try {
                    nfsJson = nfsJson.replaceAll("'", "\""); // single quotes into double quotes
                    JSONObject jsonObj = (JSONObject) parser.parse(nfsJson);
                    if (!jsonObj.get("status").equals("up")) {
                        continue;
                    }
                    String exports = jsonObj.get("exports").toString();
                    String endpointUri = "";
                    if (metadata.containsKey("nfs:info:uri")) {
                        endpointUri = metadata.get("nfs:info:uri");
                    } else {
                        endpointUri = VM.getURI() + ":service+nfs";
                    }
                    Resource resNfs = RdfOwl.createResource(model, endpointUri, Mrs.EndPoint);
                    model.add(model.createStatement(VM, Nml.hasService, resNfs));
                    model.add(model.createStatement(resNfs, Mrs.type, "nfs"));
                    if (!exports.isEmpty()) {
                        Resource resNA = RdfOwl.createResource(model, endpointUri+":exports", Mrs.NetworkAddress);
                        model.add(model.createStatement(resNA, Mrs.type, "nfs:exports"));
                        model.add(model.createStatement(resNA, Mrs.value, exports));
                        model.add(model.createStatement(resNfs, Mrs.hasNetworkAddress, resNA));
                    }
                } catch (ParseException e) {
                    logger.warning(method, String.format("cannot parse server '%s' metadata '%s' for '%s' ", server.getName(), nfsJson, "nfs:info"));
                }
            }
        }
        
        
        for (NovaFloatingIP f : openstackget.getNovaFloatingIP()) {
            String ipAddr = f.getFloatingIpAddress();
            if (!fips.contains(ipAddr)) {
                fips.add(ipAddr);
            }
        }

        //Right subnet part
        for (Network n : openstackget.getNetworks()) {
            String networkID = openstackget.getResourceName(n);
            Resource NETWORK = RdfOwl.createResource(model, ResourceTool.getResourceUri(networkID, OpenstackPrefix.NETWORK, networkID), topology);
            model.add(model.createStatement(OpenstackTopology, hasTopology, NETWORK));
            model.add(model.createStatement(networkService, providesVPC, NETWORK));
            Resource SWITCHINGSERVICE = null;
            SWITCHINGSERVICE = RdfOwl.createResource(model, ResourceTool.getResourceUri(networkID + ":switchingservice", OpenstackPrefix.switching_service, networkID), switchingService);

            model.add(model.createStatement(NETWORK, hasService, SWITCHINGSERVICE));

            //TO FIND THE EXTERNAL OR INTERNAL NETWORK
            if (n.isRouterExternal()) {
                model.add(model.createStatement(NETWORK, type, "external"));
                for (Subnet s : n.getNeutronSubnets()) {
                    //@xyang hack to work around a openstack4j flaw
                    if (s == null) {
                        continue;
                    }

                    for (Pool ap : s.getAllocationPools()) {
                        String START = ap.getStart();

                        String END = ap.getEnd();
                        String subnetId = openstackget.getResourceName(s);

                        Resource SUBNET = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.subnet, networkID, subnetId), switchingSubnet);
                        model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
                        Resource SUBNET_NETWORK_ADDRESS = null;
                        SUBNET_NETWORK_ADDRESS = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId + ":subnetnetworkaddress", OpenstackPrefix.subnet_network_address, networkID, subnetId), networkAddress);

                        Resource FLOATING_IP_ALLOC
                                = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.floating_ip_alloc, networkID, subnetId), networkAddress);
                        Resource FLOATING_IP_POOL = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.floating_ip_pool, networkID, subnetId), networkAddress);
                        if (s.getGateway() != null) {
                            String gateway = s.getGateway();
                            Resource GATEWAY = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId + "gateway", OpenstackPrefix.gateway, networkID, subnetId, gateway), networkAddress);
                            model.add(model.createStatement(SUBNET, hasNetworkAddress, GATEWAY));
                            model.add(model.createStatement(GATEWAY, type, "gateway"));
                            model.add(model.createStatement(GATEWAY, value, gateway));
                        }
                        model.add(model.createStatement(OpenstackTopology, hasNetworkAddress, FLOATING_IP_ALLOC));
                        model.add(model.createStatement(FLOATING_IP_ALLOC, type, "ipv4-floatingip"));
                        for (String fip: fips) {
                            model.add(model.createStatement(FLOATING_IP_ALLOC, value, fip));
                        }
                        model.add(model.createStatement(OpenstackTopology, hasNetworkAddress, FLOATING_IP_POOL));
                        model.add(model.createStatement(FLOATING_IP_POOL, type, "ipv4-floatingip-pool"));
                        model.add(model.createStatement(FLOATING_IP_POOL, value, START + "-" + END));
                        model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
                        model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
                        model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, s.getCidr()));

                        for (Port port : openstackget.getPorts()) {
                            for (String subID : openstackget.getPortSubnetID(port)) {
                                String subName = openstackget.getResourceName(openstackget.getSubnet(subID));
                                if (subnetId.equals(subName)) {
                                    String PortName = openstackget.getResourceName(port);
                                    Resource Port = RdfOwl.createResource(model, ResourceTool.getResourceUri(PortName, OpenstackPrefix.PORT, PortName), biPort);
                                    model.add(model.createStatement(SUBNET, hasBidirectionalPort, Port));
                                    //model.add(model.createStatement(Port, hasTag, PORT_TAG));

                                }

                            }
                        }

                        //external routing table
                        String dest = "0.0.0.00";
                        String nextHOP = s.getGateway();
                        Resource ROUTINGSERVICE = null;
                        ROUTINGSERVICE = RdfOwl.createResource(model, ResourceTool.getResourceUri(networkID + ":routingservice", OpenstackPrefix.network_routing_service, networkID), RoutingService);

                        Resource EXROUTINGTABLE = null;
                        EXROUTINGTABLE = RdfOwl.createResource(model, ResourceTool.getResourceUri(networkID + ":routingtable", OpenstackPrefix.network_routing_table, networkID), Mrs.RoutingTable);
                        Resource EXROUTE = RdfOwl.createResource(model, ResourceTool.getResourceUri(dest, OpenstackPrefix.network_route, networkID, dest, subnetId), route);
                        Resource EXNEXTHOP = RdfOwl.createResource(model, ResourceTool.getResourceUri(nextHOP, OpenstackPrefix.network_route_next_hop, networkID, nextHOP), networkAddress);
                        Resource DEST_EXTERNAL = RdfOwl.createResource(model, ResourceTool.getResourceUri(dest, OpenstackPrefix.network_route_dest, networkID, dest), networkAddress);
                        Resource LOCAL_ROUTE = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.local_route, networkID, subnetId), Mrs.Route);
                        model.add(model.createStatement(NETWORK, hasService, ROUTINGSERVICE));
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoutingTable, EXROUTINGTABLE));
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoute, EXROUTE));
                        model.add(model.createStatement(ROUTINGSERVICE, providesRoute, LOCAL_ROUTE));
                        model.add(model.createStatement(EXROUTINGTABLE, hasRoute, EXROUTE));
                        model.add(model.createStatement(EXROUTINGTABLE, hasRoute, LOCAL_ROUTE));
                        model.add(model.createStatement(EXROUTE, routeFrom, SUBNET));

                        model.add(model.createStatement(EXROUTE, routeTo, DEST_EXTERNAL));
                        model.add(model.createStatement(DEST_EXTERNAL, type, "ipv4-prefix"));
                        model.add(model.createStatement(DEST_EXTERNAL, value, dest));

                        model.add(model.createStatement(EXROUTE, nextHop, EXNEXTHOP));
                        model.add(model.createStatement(EXNEXTHOP, type, "ipv4-address"));
                        model.add(model.createStatement(EXNEXTHOP, value, nextHOP));

                        model.add(model.createStatement(LOCAL_ROUTE, routeFrom, SUBNET));
                        model.add(model.createStatement(LOCAL_ROUTE, routeTo, SUBNET_NETWORK_ADDRESS));
                        model.add(model.createStatement(LOCAL_ROUTE, nextHop, "local"));

                        for (HostRoute hr : s.getHostRoutes()) {
                            String destIp = hr.getDestination();
                            String destIP = destIp;
                            String nextHOPEXT = hr.getNexthop();
                            //@TODO naming convention not good
                            Resource EXHOSTROUTE = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.host_route, networkID, destIP, subnetId), route);

                            Resource EXHOSTROUTETO = RdfOwl.createResource(model, ResourceTool.getResourceUri(destIP, OpenstackPrefix.host_route_to, networkID, subnetId, destIP), networkAddress);
                            Resource EXHOSTNEXTHOP = RdfOwl.createResource(model, ResourceTool.getResourceUri(nextHOPEXT, OpenstackPrefix.host_route_next_hop, networkID, destIP, subnetId, nextHOPEXT), networkAddress);
                            Resource EXHOSTROUTINGTABLE = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.host_route_routing_table, networkID, destIP, subnetId), Mrs.RoutingTable);

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
                if (n.getSubnets().size() != 0) {

                    for (Subnet s : n.getNeutronSubnets()) {
                        if (s == null) {
                            continue;
                        }
                        String subnetId = openstackget.getResourceName(s);
                        Resource SUBNET = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.subnet, networkID, subnetId), switchingSubnet);
                        model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
                        Resource SUBNET_NETWORK_ADDRESS = null;
                        SUBNET_NETWORK_ADDRESS = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId + ":cidr", OpenstackPrefix.subnet_network_address, networkID, subnetId), networkAddress);

                        Resource ROUTINGSERVICE = null;
                        ROUTINGSERVICE = RdfOwl.createResource(model, ResourceTool.getResourceUri(networkID + ":routingservice", OpenstackPrefix.network_routing_service, networkID), RoutingService);
                        Resource ROUTINGTABLEPERNET = null;
                        ROUTINGTABLEPERNET = RdfOwl.createResource(model, ResourceTool.getResourceUri(networkID + ":routingtable", OpenstackPrefix.network_routing_table, networkID)+"-main", Mrs.RoutingTable);
                        if (s.getGateway() != null) {
                            String gateway = s.getGateway();
                            Resource GATEWAY = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId + ":gateway", OpenstackPrefix.gateway, networkID, subnetId, gateway), networkAddress);
                            model.add(model.createStatement(SUBNET, hasNetworkAddress, GATEWAY));
                            model.add(model.createStatement(GATEWAY, type, "gateway"));
                            model.add(model.createStatement(GATEWAY, value, gateway));
                        }
                        Resource LOCAL_ROUTE = null;
                        LOCAL_ROUTE = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId + ":route-local", OpenstackPrefix.local_route, networkID, subnetId), Mrs.Route);

                        //Resource TENANT_SUBNET_TAG = RdfOwl.createResource(model, topologyURI + ":subnet_tag_private", Mrs.Tag);
                        model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
                        model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
                        model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, s.getCidr()));

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
                            String destIP = destIp;
                            String nextHOP = hr.getNexthop();
                            Resource INROUTE = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.host_route, networkID, destIP, subnetId), route);
                            Resource INROUTEFROM = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.host_route_route_from, networkID, subnetId), switchingSubnet);
                            Resource INROUTETO = RdfOwl.createResource(model, ResourceTool.getResourceUri(destIP, OpenstackPrefix.host_route_to, networkID, subnetId, destIP), networkAddress);
                            Resource INNEXTHOP = RdfOwl.createResource(model, ResourceTool.getResourceUri(nextHOP, OpenstackPrefix.host_route_next_hop, networkID, destIP, subnetId, nextHOP), networkAddress);
                            Resource HOSTROUTINGTABLE = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.host_route_routing_table, networkID, destIP, subnetId), Mrs.RoutingTable);
                            model.add(model.createStatement(NETWORK, hasService, ROUTINGSERVICE));
                            model.add(model.createStatement(ROUTINGSERVICE, providesRoutingTable, HOSTROUTINGTABLE));
                            model.add(model.createStatement(ROUTINGSERVICE, providesRoute, INROUTE));
                            model.add(model.createStatement(HOSTROUTINGTABLE, hasRoute, INROUTE));
                            model.add(model.createStatement(INROUTE, routeFrom, INROUTEFROM));
                            model.add(model.createStatement(INROUTE, routeTo, INROUTETO));
                            model.add(model.createStatement(INROUTE, nextHop, INNEXTHOP));

                            model.add(model.createStatement(INROUTEFROM, type, "subnet"));
                            model.add(model.createStatement(INROUTEFROM, value, subnetId));

                            model.add(model.createStatement(INNEXTHOP, type, "ipv4-address"));
                            model.add(model.createStatement(INNEXTHOP, value, nextHOP));

                            model.add(model.createStatement(INROUTETO, type, "ipv4-prefix"));
                            model.add(model.createStatement(INROUTETO, value, destIp));

                        }

                        for (Port port : openstackget.getPorts()) {
                            for (String subID : openstackget.getPortSubnetID(port)) {
                                String subName = openstackget.getResourceName(openstackget.getSubnet(subID));
                                if (subnetId.equals(subName) || subID.equals(s.getId())) { //not enter the if  
                                    String PortName = openstackget.getResourceName(port);
                                    Resource Port = RdfOwl.createResource(model, ResourceTool.getResourceUri(PortName, OpenstackPrefix.PORT, PortName), biPort);
                                    model.add(model.createStatement(SUBNET, hasBidirectionalPort, Port));

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
            String routerShortName = routername;
            if (begain_with_uri(routerShortName, ResourceTool.versaStackPrefix)) {
                routerShortName = routerShortName.substring(ResourceTool.versaStackPrefix.length());
            }
            for (Port port : openstackget.getPorts()) {
                if (port.getDeviceId().equals(r.getId())) {

                    for (String DES_SUB : openstackget.getPortSubnetID(port)) {
                        Subnet s = openstackget.getSubnet(DES_SUB);
                        DES_SUB = openstackget.getResourceName(s);
                        String subnetShortName = DES_SUB; 
                        if (begain_with_uri(subnetShortName, ResourceTool.versaStackPrefix)){
                            subnetShortName = subnetShortName.substring(ResourceTool.versaStackPrefix.length());
                        }
                        String net_ID = s.getNetworkId();
                        String NET_ID = openstackget.getResourceName(openstackget.getNetwork(net_ID));
                        String networkShortName = NET_ID; 
                        if (begain_with_uri(networkShortName, ResourceTool.versaStackPrefix)){
                            networkShortName = networkShortName.substring(ResourceTool.versaStackPrefix.length());
                        }
                        Resource SUBNET = RdfOwl.createResource(model, ResourceTool.getResourceUri(DES_SUB, OpenstackPrefix.subnet, networkShortName, DES_SUB), switchingSubnet);
                        for (IP ip2 : port.getFixedIps()) {
                            String INTERFACE_IP = ip2.getIpAddress();
                            Resource ROUTER_INTERFACE_ROUTE_NEXTHOP = RdfOwl.createResource(model, ResourceTool.getResourceUri(INTERFACE_IP, OpenstackPrefix.router_interface_next_hop, routerShortName, INTERFACE_IP), networkAddress);
                            Resource ROUTER_INTERFACE_ROUTINGTABLE = null;
                            ROUTER_INTERFACE_ROUTINGTABLE = RdfOwl.createResource(model, ResourceTool.getResourceUri(routername, OpenstackPrefix.router_interface_routingtable, routername), Mrs.RoutingTable);
                            Resource ROUTER_INTERFACE_ROUTE = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetShortName, OpenstackPrefix.router_interface_route, routerShortName, subnetShortName), route);
                            model.add(model.createStatement(routingService, providesRoutingTable, ROUTER_INTERFACE_ROUTINGTABLE));
                            model.add(model.createStatement(routingService, providesRoute, ROUTER_INTERFACE_ROUTE));
                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTINGTABLE, hasRoute, ROUTER_INTERFACE_ROUTE));
                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTE, routeTo, SUBNET));
                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTE, nextHop, ROUTER_INTERFACE_ROUTE_NEXTHOP));

                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTE_NEXTHOP, type, "ipv4-address"));
                            model.add(model.createStatement(ROUTER_INTERFACE_ROUTE_NEXTHOP, value, INTERFACE_IP));
                        }
                    }
                    
                }
            }
            //router host route
            for (HostRoute hr : r.getRoutes()) {
                String RHOSTTO = hr.getDestination();
                String RHOSTNEXTHOP = hr.getNexthop();
                String host_dest = hr.getDestination();

                Resource RHOSTROUTINGTABLE = RdfOwl.createResource(model, ResourceTool.getResourceUri(routername, OpenstackPrefix.router_host_route, routername), Mrs.RoutingTable);
                Resource RHOSTROUTE = RdfOwl.createResource(model, ResourceTool.getResourceUri(RHOSTNEXTHOP, OpenstackPrefix.router_host_route, routername, host_dest, RHOSTNEXTHOP), route);//maybe need to modify
                Resource RHOSTROUTETO = RdfOwl.createResource(model, ResourceTool.getResourceUri(host_dest, OpenstackPrefix.router_host_routeto, routername, host_dest), networkAddress);
                Resource RHOSTROUTENEXTHOP = RdfOwl.createResource(model, ResourceTool.getResourceUri(RHOSTNEXTHOP, OpenstackPrefix.router_host_route_nexthop, routername, RHOSTNEXTHOP), networkAddress);

                model.add(model.createStatement(routingService, providesRoutingTable, RHOSTROUTINGTABLE));
                model.add(model.createStatement(routingService, providesRoute, RHOSTROUTE));
                model.add(model.createStatement(RHOSTROUTINGTABLE, hasRoute, RHOSTROUTE));
                model.add(model.createStatement(RHOSTROUTE, routeTo, RHOSTROUTETO));
                model.add(model.createStatement(RHOSTROUTE, nextHop, RHOSTROUTENEXTHOP));

                model.add(model.createStatement(RHOSTROUTETO, type, "subnet"));
                model.add(model.createStatement(RHOSTROUTETO, value, RHOSTTO));

                model.add(model.createStatement(RHOSTROUTENEXTHOP, type, "ipv4-address"));
                model.add(model.createStatement(RHOSTROUTENEXTHOP, value, RHOSTNEXTHOP));
            }

        }

        for (Volume v : openstackget.getVolumes()) {
            String volumeName = openstackget.getVolumeName(v);
            Resource VOLUME = RdfOwl.createResource(model, ResourceTool.getResourceUri(volumeName, OpenstackPrefix.volume, volumeName), volume);
            model.add(model.createStatement(cinderService, providesVolume, VOLUME));
            model.add(model.createStatement(VOLUME, value, v.getVolumeType()));
            model.add(model.createStatement(VOLUME, Mrs.disk_gb, Integer.toString(v.getSize())));
        }

        for (NetFloatingIP f : openstackget.getFloatingIp()) {
            Resource FLOATADD = null;
            Resource FIXEDADD = null;
            if (f.getFixedIpAddress() != null && !f.getFixedIpAddress().isEmpty() && f.getFloatingIpAddress() != null && !f.getFloatingIpAddress().isEmpty()) {
                for (Port po : openstackget.getPorts()) {
                    for (IP ips : po.getFixedIps()) {

                        if (ips.getIpAddress().equals(f.getFloatingIpAddress())) {
                            String s = ips.getSubnetId();
                            Subnet sub = openstackget.getSubnet(s);
                            Resource SUBNET = model.getResource(ResourceTool.getResourceUri(openstackget.getResourceName(sub), OpenstackPrefix.subnet, openstackget.getResourceName(openstackget.getNetwork(sub.getNetworkId())) , openstackget.getResourceName(sub)));
                            FLOATADD = RdfOwl.createResource(model, topologyURI + ":subnet+" + openstackget.getResourceName(sub) + ":floatingip+" + f.getFloatingIpAddress(), networkAddress);
                            model.add(model.createStatement(SUBNET, hasNetworkAddress, FLOATADD));
                            model.add(model.createStatement(FLOATADD, type, "floating-ip"));
                            model.add(model.createStatement(FLOATADD, value, f.getFloatingIpAddress()));
                        } else if (ips.getIpAddress().equals(f.getFixedIpAddress())) {
                            String s = ips.getSubnetId();
                            Subnet sub = openstackget.getSubnet(s);
                            String subnetId = openstackget.getResourceName(sub);
                            String n = sub.getNetworkId();
                            Network net = openstackget.getNetwork(n);
                            String networkId = openstackget.getResourceName(net);
                            Resource SUBNET = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId, OpenstackPrefix.subnet, networkId, subnetId), switchingSubnet);
                            FIXEDADD = RdfOwl.createResource(model, topologyURI + ":subnet+" + openstackget.getResourceName(sub) + ":fixedip+" + f.getFixedIpAddress(), networkAddress);
                            model.add(model.createStatement(SUBNET, hasNetworkAddress, FIXEDADD));
                            model.add(model.createStatement(FIXEDADD, type, "fixed-ip"));
                            model.add(model.createStatement(FIXEDADD, value, f.getFixedIpAddress()));
                        }
                    }
                }
                try {
                    if (FLOATADD != null && FIXEDADD != null) {
                        model.add(model.createStatement(FIXEDADD, isAlias, FLOATADD));
                        // add assocaited floating ip to VM
                        for (Server server : openstackget.getServers()) {
                            Port pt = openstackget.getPort(f.getPortId());
                            if (server.getId().equals(pt.getDeviceId())) {
                                String PortID = openstackget.getResourceName(pt);
                                Resource PORT = RdfOwl.createResource(model, ResourceTool.getResourceUri(PortID, OpenstackPrefix.PORT, PortID), biPort);
                                model.add(model.createStatement(PORT, hasNetworkAddress, FLOATADD));
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new Exception(e.toString());
                }

            }
        }
        /*
         StringWriter out = new StringWriter();
         try {
         model.write(out, "TURTLE");
         } catch (Exception e) {
         throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
         }
         String ttl = out.toString();
         System.out.println(ttl);
         System.out.println(ttl);
         */

        // combine extra model (static injection)
        if (modelExt != null) {
            model.add(modelExt.getBaseModel());
        }
        return model;
    }

    private static boolean begain_with_uri(String name, String uri) {
        if (name.startsWith(uri)) {
            return true;
        } else {
            return false;
        }
    }
    
}
