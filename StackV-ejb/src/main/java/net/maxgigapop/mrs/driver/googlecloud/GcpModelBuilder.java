/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
//import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import net.maxgigapop.mrs.common.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Adam Smith
 */
public class GcpModelBuilder {
    
    public static final StackLogger logger = GcpDriver.logger;
    
    public static OntModel createOntology(Map<String, String> properties) throws IOException {
       
        String jsonAuth = properties.get("jsonAuth");
        String projectID = properties.get("projectID");
        String topologyUri = properties.get("topologyUri");
        String topologyRegion = "global";
        String method = "createOntology";
        logger.start(method);
        
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        
        //set all the model prefixes
        model.setNsPrefix("rdfs", RdfOwl.getRdfsURI());
        model.setNsPrefix("rdf", RdfOwl.getRdfURI());
        model.setNsPrefix("xsd", RdfOwl.getXsdURI());
        model.setNsPrefix("owl", RdfOwl.getOwlURI()); 
        model.setNsPrefix("nml", Nml.getURI());
        model.setNsPrefix("mrs", Mrs.getURI());
        
        GcpGet gcpGet = new GcpGet(jsonAuth, projectID);
        
        Resource gcpTopology = RdfOwl.createResource(model, topologyUri, Nml.Topology);
        Resource vpcService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GcpPrefix.vpcService, topologyRegion), Mrs.VirtualCloudService);
        Resource computeService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GcpPrefix.computeService, topologyRegion), Mrs.HypervisorService);
        Resource objectStorageService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GcpPrefix.objectStorageService, topologyRegion), Mrs.ObjectStorageService);
        Resource blockStorageService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GcpPrefix.blockStorageService, topologyRegion), Mrs.BlockStorageService);
        
        model.add(model.createStatement(gcpTopology, Nml.hasService, vpcService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, computeService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, blockStorageService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, objectStorageService));
        
        //get the commonInstanceMetadata, for uri retrieval
        HashMap<String, String> metadata = gcpGet.getCommonMetadata();
        if (metadata == null) {
            logger.error(method, "failed to get GCP metadata tables; URI will be constructed automatically");
        }
        
        HashMap<String, JSONObject> vpnRoutes = new HashMap<>();
        //The routes are requested here, and added to the model later
        JSONObject routeResult = gcpGet.getRoutes();
        JSONArray routesInfo = null;
        if (routeResult != null && routeResult.containsKey("items")) {
            routesInfo = (JSONArray) routeResult.get("items");
        } else {
            logger.error(method, "failed to get routes: "+routeResult);
        }
        
        //Add VPCs to the model
        JSONObject vpcsInfo = gcpGet.getVPCs();
        if (vpcsInfo != null) {
            JSONArray vpcs = (JSONArray) vpcsInfo.get("items");
            for (Object o : vpcs) {
                JSONObject vpcInfo = (JSONObject) o;
                //System.out.println(vpcInfo);
                String name = vpcInfo.get("name").toString();
                String vpcUri = lookupResourceUri(metadata, "vpc", name);
                //String vpcUri = vpcInfo.get("description").toString();
                
                //System.out.println("vpc info: "+vpcInfo);
                
                Resource vpc = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpcUri, GcpPrefix.vpc, name), Nml.Topology);
                model.add(model.createStatement(gcpTopology, Nml.hasTopology, vpc));
                Resource switchingService = RdfOwl.createResource(model, vpc.getURI()+":switchingservice", Mrs.SwitchingService);
                model.add(model.createStatement(vpc, Nml.hasService, switchingService));
                model.add(model.createStatement(vpcService, Mrs.providesVPC, vpc));
                
                Resource routingService = RdfOwl.createResource(model, vpc.getURI()+":routingservice", Mrs.RoutingService);
                model.add(model.createStatement(vpc, Nml.hasService, routingService));
                //google cloud does not use a routing table, so all routes for one vpc are placed in one routing table
                Resource routingTable = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GcpPrefix.routingTable, name), Mrs.RoutingTable);
                model.add(model.createStatement(routingService, Mrs.providesRoutingTable, routingTable));
                //every vpc has one built-in internet gateway
                Resource igw = RdfOwl.createResource(model, vpc.getURI()+"-igw", Nml.BidirectionalPort);
                model.add(model.createStatement(igw, Mrs.type, "internet-gateway"));
                model.add(model.createStatement(vpc, Nml.hasBidirectionalPort, igw));
                
                
                //subnets
                JSONArray subnetsInfo = (JSONArray) vpcInfo.get("subnetworks");
                if (subnetsInfo == null) {
                    logger.warning(method, "Null subnet response for VPC "+name);
                } else {
                    for (Object o2 : subnetsInfo) {
                        String subnetName = GcpGet.parseGoogleURI(o2.toString(), "subnetworks");
                        String subnetRegion = GcpGet.parseGoogleURI(o2.toString(), "regions");
                        JSONObject subnetInfo = gcpGet.getSubnet(subnetRegion, subnetName);
                        if (subnetInfo == null) {
                            //Skip this subnet
                            logger.warning(method, String.format("error while requesting details for subnet %s in  region %s", subnetName, subnetRegion));
                            continue;
                        }
                        
                        //System.out.printf("subnet info: %s\n", subnetInfo);
                        String subnetUri = lookupResourceUri(metadata, "subnet", name, subnetRegion, subnetName);
                        String cidr;
                        
                        if (subnetInfo.containsKey("ipCidrRange")) {
                            cidr = subnetInfo.get("ipCidrRange").toString();
                        } else {
                            cidr = "none";
                        }
                        String gateway = subnetInfo.get("gatewayAddress").toString();
                    
                        Resource subnet = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetUri, GcpPrefix.subnet, name, subnetRegion, subnetName), Mrs.SwitchingSubnet);
                        model.add(model.createStatement(switchingService, Mrs.providesSubnet, subnet));
                    
                        Resource subnetGateway = RdfOwl.createResource(model, subnet.getURI()+":gateway", Nml.BidirectionalPort);
                        model.add(model.createStatement(subnet, Nml.hasBidirectionalPort, subnetGateway));
                    
                        Resource gatewayIP = RdfOwl.createResource(model, subnetGateway.getURI()+":gatewayIP", Mrs.NetworkAddress);
                        model.add(model.createStatement(subnetGateway, Mrs.hasNetworkAddress, gatewayIP));
                        model.add(model.createStatement(gatewayIP, Mrs.type, "ipv4-address"));
                        model.add(model.createStatement(gatewayIP, Mrs.value, gateway));
                    
                        Resource subnetCIDR = RdfOwl.createResource(model, subnet.getURI()+":cidr", Mrs.NetworkAddress);
                        model.add(model.createStatement(subnet, Mrs.hasNetworkAddress, subnetCIDR));
                        model.add(model.createStatement(subnetCIDR, Mrs.type, "ipv4-prefix-list"));
                        model.add(model.createStatement(subnetCIDR, Mrs.value, cidr));
                    }
                }
                
                /*
                Add routes.
                Routes contain routeTo and nextHop info
                routeFrom info is not included.
                */
                if (routesInfo != null) {
                for (Object o2: routesInfo) {
                    JSONObject routeInfo = (JSONObject) o2;
                    String vpcName = GcpGet.parseGoogleURI(routeInfo.get("network").toString(), "networks");
                    //this route is for another vpc
                    if (!name.equals(vpcName)) continue;
                    String routeName = routeInfo.get("name").toString();
                    String routeUri = lookupResourceUri(metadata, "route", name, routeName);
                    String destRange = routeInfo.get("destRange").toString();
                    String nextHop = "unknown";
                    
                    Resource route = null;
                    
                    if (routeInfo.get("nextHopNetwork") != null) {
                        //nextHop is a vpc
                        nextHop = vpc.getURI()+":subnet";
                    } else if (routeInfo.get("nextHopGateway") != null) {
                        //nextHop is the internet gateway of a vpc
                        nextHop = vpc.getURI()+":internetGateway";
                    } else if (routeInfo.containsKey("nextHopVpnTunnel")) {
                        String googleUri = routeInfo.get("nextHopVpnTunnel").toString();
                        String tunnelRegion = GcpGet.parseGoogleURI(googleUri, "regions");
                        String tunnelName = GcpGet.parseGoogleURI(vpcUri, "vpnTunnels");
                        nextHop = ResourceTool.getResourceUri("", GcpPrefix.vpnTunnel, vpcName, tunnelRegion, tunnelName);
                    } else {
                        logger.warning(method, String.format("route %s's destination is not currently modeled. displaying json: %s", routeName, routeInfo));
                    }
                    
                    if (route == null) {
                        route = RdfOwl.createResource(model, ResourceTool.getResourceUri(routeUri, GcpPrefix.route, vpcName, routeName), Mrs.Route);
                    }
                    Resource routeTo = RdfOwl.createResource(model, route.getURI()+":route-to", Mrs.NetworkAddress);
                    model.add(model.createStatement(routeTo, Mrs.type, "ipv4-prefix-list"));
                    model.add(model.createStatement(routeTo, Mrs.value , destRange));
                    
                    model.add(model.createStatement(routingService, Mrs.providesRoute, route));
                    model.add(model.createStatement(routingTable, Mrs.hasRoute, route));
                    model.add(model.createStatement(route, Mrs.routeTo, routeTo));
                    model.add(model.createStatement(route, Mrs.nextHop, nextHop));
                }
                }
            }
        } else {
            logger.error(method, "failed to get VPCs due to null response");
        }
        
        //map all the vpn routes by vpn tunnel
        if (routesInfo != null) {
            for (Object o : routesInfo) {
                JSONObject route = (JSONObject) o;
                String key = "nextHopVpnTunnel";
            
                if (route.containsKey(key)) {
                    vpnRoutes.put(route.get(key).toString(), route);
                }
            }
        }
        
        //Add VPNs to the model
        JSONArray vgwsInfo = gcpGet.getAggregatedTargetVGWs();
        if (vgwsInfo != null) {
            for (Object o : vgwsInfo) {
                JSONObject vgwInfo = (JSONObject) o;
                //System.out.println("vgw: "+vgwInfo);
                String vgwName = vgwInfo.get("name").toString();
                String region = GcpGet.parseGoogleURI(vgwInfo.get("region").toString(), "regions");
                String vpcName = GcpGet.parseGoogleURI(vgwInfo.get("network").toString(), "networks");
                String vpcUri = lookupResourceUri(metadata, "vpc", vpcName);
                String vgwUri = lookupResourceUri(metadata, "vgw", region, vgwName);
                JSONArray tunnels = (JSONArray) vgwInfo.get("tunnels");
                JSONArray rules = (JSONArray) vgwInfo.get("forwardingRules");
                
                Resource vpc = model.getResource(ResourceTool.getResourceUri(vpcUri, GcpPrefix.vpc, vpcName));
                Resource vgw = RdfOwl.createResource(model, ResourceTool.getResourceUri(vgwUri, GcpPrefix.vpnGateway, vpcName, region, vgwName), Nml.BidirectionalPort);
                model.add(model.createStatement(vgw, Mrs.type, "vpn-gateway"));
                model.add(model.createStatement(vpc, Nml.hasBidirectionalPort, vgw));
                
                if (tunnels == null) {
                    logger.warning(method, "vgw "+vgwName+" has no tunnels");
                } else {
                    for (Object o2 : tunnels) {
                        String tunnelName = GcpGet.parseGoogleURI(o2.toString(), "vpnTunnels");
                        JSONObject tunnelInfo = gcpGet.getVpnTunnel(region, tunnelName);
                        JSONObject routeInfo = vpnRoutes.get(o2.toString());
                        if (routeInfo == null) {
                            System.out.println("null route: "+o2.toString());
                            continue;
                        }
                        
                        String tunnelRegion = GcpGet.parseGoogleURI(tunnelInfo.get("region").toString(),  "regions");
                        String tunnelUri = lookupResourceUri(metadata, "vpn", tunnelRegion, tunnelName);
                        
                        String remoteIp = tunnelInfo.get("peerIp").toString();
                        String remoteCIDR;
                        if (routeInfo.containsKey("destRange")) {
                            remoteCIDR = routeInfo.get("destRange").toString();
                        } else {
                            String routeName = routeInfo.get("name").toString();
                            remoteCIDR = "error";
                            logger.warning(method, "unable to find cidr for route "+routeName+". displaying JSON: "+routeInfo);
                        }
                        //String vpcName = GcpGet.parseGoogleURI(routeInfo.get("network").toString(), "networks");
                        String routeName = routeInfo.get("name").toString();
                        //System.out.println("tunnel: " + tunnelInfo);
                        //System.out.println("route: "+ routeInfo);
                    
                        Resource tunnel = RdfOwl.createResource(model, ResourceTool.getResourceUri(tunnelUri, GcpPrefix.vpnTunnel, vpcName, region, vgwName, tunnelName), Nml.BidirectionalPort);
                        model.add(model.createStatement(tunnel, Mrs.type, "vpn-tunnel"));
                        model.add(model.createStatement(vgw, Nml.hasBidirectionalPort, tunnel));
                    
                        Resource peerIp = RdfOwl.createResource(model, tunnel.getURI()+":ip", Mrs.NetworkAddress);
                        model.add(model.createStatement(peerIp, Mrs.type, "ipv4-address:customer"));
                        model.add(model.createStatement(peerIp, Mrs.value, remoteIp));
                        model.add(model.createStatement(tunnel, Mrs.hasNetworkAddress, peerIp));
                        
                        Resource peerCidr = RdfOwl.createResource(model, tunnel.getURI()+":customer-cidr", Mrs.NetworkAddress);
                        model.add(model.createStatement(peerCidr, Mrs.type, "ipv4-prefix-list:customer"));
                        model.add(model.createStatement(peerCidr, Mrs.value, remoteCIDR));
                        model.add(model.createStatement(tunnel, Mrs.hasNetworkAddress, peerCidr));
                        
                        Resource secret = RdfOwl.createResource(model, tunnel.getURI()+":secret", Mrs.NetworkAddress);
                        model.add(model.createStatement(secret, Mrs.type, "secret"));
                        model.add(model.createStatement(secret, Mrs.value, "####"));
                        model.add(model.createStatement(tunnel, Mrs.hasNetworkAddress, secret));
                    }
                }
                
                if (rules == null) {
                    logger.warning(method, "vgw "+vgwName+" has no rules");
                } else {
                    for (Object o2 : rules) {
                        String ruleName = GcpGet.parseGoogleURI(o2.toString(), "forwardingRules");
                        JSONObject ruleInfo = gcpGet.getForwardingRules(region, ruleName);
                        String ip = ruleInfo.get("IPAddress").toString();
                        String ruleRegion = GcpGet.parseGoogleURI(ruleInfo.get("region").toString(), "regions");
                        String protocol = GcpQuery.getOrDefault(ruleInfo, "IPProtocol", "null");
                        String portRange = GcpQuery.getOrDefault(ruleInfo, "portRange", "null");
                        HashMap<String, String> type = new HashMap<>();
                        type.put("protocol", protocol);
                        type.put("portRange", portRange);
                        String typeStr = GcpQuery.createTypeStr(type);
                        
                        //System.out.println("rule: " + ruleInfo);
                    
                        Resource forwardingRule = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GcpPrefix.rule, ruleRegion, ruleName), Nml.Link);
                        model.add(model.createStatement(vgw, Nml.hasLink, forwardingRule));
                        model.add(model.createStatement(forwardingRule, Mrs.type, typeStr));
                        
                        Resource ruleIp = RdfOwl.createResource(model, forwardingRule.getURI()+":ip", Mrs.NetworkAddress);
                        model.add(model.createStatement(forwardingRule, Mrs.hasNetworkAddress, ruleIp));
                        model.add(model.createStatement(ruleIp, Mrs.type, "ipv4-address"));
                        model.add(model.createStatement(ruleIp, Mrs.value, ip));
                    }
                }
            }
        }
        
        //Add VMs to model
        //JSONObject instancesInfo = gcpGet.getVmInstances(); 
        JSONArray instancesInfo = gcpGet.getAggregatedVmInstances();
        HashMap<String, String> instanceProperties = new HashMap<>();
        
        if (instancesInfo != null) {
            //JSONArray vms = (JSONArray) instancesInfo.get("items");
            for (Object o : instancesInfo) {
                JSONObject vmInfo = (JSONObject) o;
                String instanceName = vmInfo.get("name").toString();
                String machineType = GcpGet.parseGoogleURI(vmInfo.get("machineType").toString(), "machineTypes");
                String zone = GcpGet.parseGoogleURI(vmInfo.get("zone").toString(), "zones");
                instanceProperties.put("instance", machineType);
                instanceProperties.put("zone", zone);
                
                JSONArray netifaces = (JSONArray) vmInfo.get("networkInterfaces");
                String instanceUri = lookupResourceUri(metadata, "vm", zone, instanceName);
                Resource instance = RdfOwl.createResource(model, ResourceTool.getResourceUri(instanceUri, GcpPrefix.instance, zone, instanceName), Nml.Node);
                model.add(model.createStatement(instance, Mrs.type, GcpQuery.createTypeStr(instanceProperties)));
                
                if (netifaces == null) {
                    logger.warning(method, "unable to find any network interfaces for instance "+instanceName);
                } else {
                    //System.out.printf("netifaces:\n%s\n", netifaces);
                    
                    for (Object o2 : netifaces) {
                        JSONObject netiface = (JSONObject) o2;
                        
                        //Extract the vpcName from the google URI
                        String vpcName = GcpGet.parseGoogleURI(netiface.get("network").toString(), "networks");
                        //Try to find the VPC Uri in the metadata table. If absent, this returns the empty string
                        String vpcUri = lookupResourceUri(metadata, "vpc", vpcName);
                        Resource vpc = model.getResource(ResourceTool.getResourceUri(vpcUri, GcpPrefix.vpc, vpcName));
                        String nicName = netiface.get("name").toString();
                        String nicIP;
                        if (!netiface.containsKey("networkIP")) {
                            //System.out.printf("No network IP for instance %s, showing json %s\n", instanceName, netiface);
                            nicIP = "error";
                        } else {
                            nicIP = netiface.get("networkIP").toString();
                        }
                        String subnetName = GcpGet.parseGoogleURI(netiface.get("subnetwork").toString(), "subnetworks");
                        String subnetRegion = GcpGet.parseGoogleURI(netiface.get("subnetwork").toString(), "regions");
                        String nicUri = lookupResourceUri(metadata, "nic", instanceName, nicName);
                        String subnetUri = lookupResourceUri(metadata, "subnet", vpcName, subnetRegion, subnetName);
                        
                        //A instance is considered to be in the vpc used by nic0
                        //All instances must have nic0
                        if ("nic0".equals(netiface.get("name").toString())) {
                            String natIP = GcpGet.getInstancePublicIP(netiface);
                            if (!natIP.equals("none")) {
                                //An instance derives its puplic IP from nic0, but it doesn't have to have a public ip
                                Resource publicIP = RdfOwl.createResource(model, instance.getURI()+":publicIP", Mrs.NetworkAddress);
                                model.add(model.createStatement(publicIP, Mrs.type, "ipv4-address"));
                                model.add(model.createStatement(publicIP, Mrs.value, natIP));
                                model.add(model.createStatement(instance, Mrs.hasNetworkAddress, publicIP));
                            }
                            
                            //You cannot attach multiple network interfaces to the same VPC network.
                            //You can only configure a network interface when you create an instance.
                            model.add(model.createStatement(vpc, Nml.hasNode, instance));
                        }
                        
                        //Create a new biport resource for each netiface
                        Resource subnet = model.getResource(ResourceTool.getResourceUri(subnetUri, GcpPrefix.subnet, vpcName, subnetRegion, subnetName));
                        Resource biPort = RdfOwl.createResource(model, ResourceTool.getResourceUri(nicUri, GcpPrefix.nic, vpcName, instanceName, nicName), Nml.BidirectionalPort);
                        model.add(model.createStatement(instance, Nml.hasBidirectionalPort, biPort));
                        model.add(model.createStatement(subnet, Nml.hasBidirectionalPort, biPort));
                        Resource netiAddr = RdfOwl.createResource(model, ResourceTool.getResourceUri(biPort+":ip+"+nicIP, GcpPrefix.nicNetworkAddress, vpcName, subnetRegion, subnetName, nicName, nicIP), Mrs.NetworkAddress);
                        model.add(model.createStatement(biPort, Mrs.hasNetworkAddress, netiAddr));
                        model.add(model.createStatement(netiAddr, Mrs.type, "ipv4:private"));
                        model.add(model.createStatement(netiAddr, Mrs.value, nicIP));
                    }
                }
                
                model.add(model.createStatement(instance, Mrs.providedByService, computeService));
                model.add(model.createStatement(computeService, Mrs.providesVM, instance));
                
                //add each disk within vm
                JSONArray disksInfo = (JSONArray) vmInfo.get("disks");
                
                for (Object o2 : disksInfo) {
                    JSONObject diskInfo = (JSONObject) o2;
                    JSONObject fullDiskInfo = gcpGet.getVolume(zone, instanceName);
                    
                    String size = fullDiskInfo.get("sizeGb").toString();
                    String type = GcpGet.parseGoogleURI(fullDiskInfo.get("type").toString(), "diskTypes");
                    String diskName = fullDiskInfo.get("name").toString();
                    String diskUri = lookupResourceUri(metadata, "volume", zone, diskName);
                    
                    Resource volume = RdfOwl.createResource(model, ResourceTool.getResourceUri(diskUri, GcpPrefix.volume, zone, diskName), Mrs.Volume);
                    model.add(model.createStatement(instance, Mrs.hasVolume, volume));
                    model.add(model.createStatement(blockStorageService, Mrs.providesVolume, volume));
                    model.add(model.createStatement(volume, Mrs.value, type));
                    model.add(model.createStatement(volume, Mrs.disk_gb, size));
                }
            }
        } else {
            logger.error(method, "failed to get instances due to null response");
        }

        //buckets
        JSONObject bucketsResponse = gcpGet.getBuckets();
        if (bucketsResponse != null) {
            JSONArray bucketsInfo = (JSONArray) bucketsResponse.get("items");
            if (bucketsInfo != null) {
                for (Object o : bucketsInfo) {
                    JSONObject bucketInfo = (JSONObject) o;
                    String bucketName = bucketInfo.get("name").toString();
                    String bucketUri = lookupResourceUri(metadata, "bucket", bucketName);
                    //String bucketUri = recoverGcpUri(bucketInfo);
                    Resource bucket = RdfOwl.createResource(model, ResourceTool.getResourceUri(bucketUri, GcpPrefix.bucket, bucketName), Mrs.Bucket);
                    model.add(model.createStatement(objectStorageService, Mrs.providesBucket, bucket));
                    model.add(model.createStatement(gcpTopology, Mrs.hasBucket, bucket));
                    model.add(model.createStatement(bucket, Nml.name, bucketName));
                }
            } else {
                //this just means that there are no buckets on the gcp project
            }
        } else {
            logger.error(method, "failed to get buckets due to null response");
        }
        
        logger.end(method);
        return model;
    }
    
    public static String getResourceKey(String type, String...args) {
        //First argument to this function was changed from Resource to String, so that unique URIs could be assigned to unmodeled resources
        int requiredArgs = 0;
        String key = null, method = "getResourceKey";
        //uncommenting the following line results in uneccessary logging bloat
        //during model pull, but may be helpful during debugging
        //make sure to uncomment logger.end() as well
        //logger.start(method);
        
        switch (type) {
        case "vpc":
            //VPCs are identified by name only
            requiredArgs = 1;
        break;
        case "subnet":
            //Subnets are identified by vpc, name, and region, since subnets in different regions or vpcs may have same name
            requiredArgs = 3;
        break;
        case "route":
            //Routes are identified by vpc and name
            requiredArgs = 2;
        break;
        case "vm":
            //VM instance identified by zone and name
            requiredArgs = 2;
        break;
        case "volume":
            //identified by zone and name
            requiredArgs = 2;
        break;
        case "vpn":
            //VPN connection tunnel identified by region and name
            requiredArgs = 2;
        break;
        case "vgw":
            //VGW identified by region and name
            requiredArgs = 2;
        break;
        case "bucket":
            //buckets are identified by name only
            requiredArgs = 1;
        break;
        case "nic":
            //nics are identified by instance - name (nic0-7)
            requiredArgs = 2;
        break;
        default:
            logger.warning(method, "failed resource URI retrieval due to unknown resource: "+type);
            return "";
        }
        
        if (requiredArgs == args.length) {
            //this filters the input to avoid any weird injections and ensures that the key will be valid
            type = type.replaceAll("[^a-z]", "");
            //build the format string
            for (Object o : args) type += "_%s";
            key = String.format(type, args);
        } else {
            logger.warning(method, String.format("failed %s URI retrieval due to incorrect number of args", type));
        }
        
        //logger.end(method);
        
        //adding uri_ ensures that normal entries in the metadata table will never be mistaken for uri entries
        if (key == null) return "";
        else return ("uri_"+key).replaceAll("[^a-zA-Z0-9_\\-]", "");
    }
    
    public static String lookupResourceUri(HashMap<String, String> metadata, String type, String... args) {
        String key = getResourceKey(type, args);
        
        if (metadata != null && metadata.containsKey(key)) {
            return metadata.get(key);
        } else {
            return "";
        }
    }
    
    public static String makeUriGcpCompatible (String uri) {
        /*
        Google cloud labels and metadata keys only support alphanumeric chars, hyphen -, and underscore.
        GCP names only support alphanumeric and hyphen, so hyphen is used as an escape character,
        This allows URIs of 63 characters or less to appear in GCP resource names
        _ -> -u
        + -> -p
        : -> -c
        . -> -d
        - -> -h
        */
        if (uri == null) return null;
        return uri.replaceAll("-", "-h")
                  .replaceAll("_", "-u")
                  .replaceAll("[+]", "-p")
                  .replaceAll(":", "-c")
                  .replaceAll("[.]", "-d");
    }
    
    public static String convertGcpUri (String uri) {
        if (uri == null) return null;
        return uri.replaceAll("-p", "+")
                  .replaceAll("-c", ":")
                  .replaceAll("-d", ".")
                  .replaceAll("-u", "_")
                  .replaceAll("-h", "-");
    }
}
