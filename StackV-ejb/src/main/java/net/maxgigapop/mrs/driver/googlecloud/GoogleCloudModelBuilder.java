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
import java.util.HashMap;
import net.maxgigapop.mrs.common.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Adam Smith
 */
public class GoogleCloudModelBuilder {
    
    public static final StackLogger logger = GoogleCloudDriver.logger;
    
    public static OntModel createOntology(String jsonAuth, String projectID, String region, String topologyURI) throws IOException {
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
        
        GoogleCloudGet gcpGet = new GoogleCloudGet(jsonAuth, projectID, region);
        
        Resource gcpTopology = RdfOwl.createResource(model, topologyURI, Nml.Topology);
        Resource vpcService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.vpcService, region), Mrs.VirtualCloudService);
        Resource computeService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.computeService, region), Mrs.HypervisorService);
        Resource objectStorageService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.objectStorageService, region), Mrs.ObjectStorageService);
        Resource blockStorageService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.blockStorageService, region), Mrs.BlockStorageService);
        
        model.add(model.createStatement(gcpTopology, Nml.hasService, vpcService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, computeService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, blockStorageService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, objectStorageService));
        
        //get the commonInstanceMetadata, for uri retrieval
        HashMap<String, String> metadata = gcpGet.getCommonMetadata();
        if (metadata == null) {
            logger.error(method, "failed to get GCP metadata tables; default values will be used for URIs");
        }
        
        //Add VPCs to the model
        JSONObject vpcsInfo = gcpGet.getVPCs();
        if (vpcsInfo != null) {
            JSONArray vpcs = (JSONArray) vpcsInfo.get("items");
            for (Object o : vpcs) {
                JSONObject vpcInfo = (JSONObject) o;
                String name = vpcInfo.get("name").toString();
                String vpcUri = lookupResourceUri(metadata, Mrs.VirtualCloudService, name);
                //String vpcUri = vpcInfo.get("description").toString();
                
                Resource vpc = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpcUri, GoogleCloudPrefix.vpc, name), Nml.Topology);
                model.add(model.createStatement(gcpTopology, Nml.hasTopology, vpc));
                Resource switchingService = RdfOwl.createResource(model, vpc.getURI()+":switchingservice", Mrs.SwitchingService);
                model.add(model.createStatement(vpc, Nml.hasService, switchingService));
                model.add(model.createStatement(vpcService, Mrs.providesVPC, vpc));
                
                Resource routingService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.routingService, name, region), Mrs.RoutingService);
                model.add(model.createStatement(vpc, Nml.hasService, routingService));
                //google cloud does not use a routing table, so all routes for one vpc are placed in one routing table
                Resource routingTable = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.routingTable, name), Mrs.RoutingTable);
                model.add(model.createStatement(routingService, Mrs.providesRoutingTable, routingTable));
                //every vpc has one built-in internet gateway
                Resource internetGateway = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.internetGateway, name), Nml.BidirectionalPort);
                model.add(model.createStatement(internetGateway, Mrs.type, "internet-gateway"));
                
                //subnets
                JSONArray subnetsInfo = (JSONArray) vpcInfo.get("subnetworks");
                for (Object o2 : subnetsInfo) {
                    String subnetName = GoogleCloudGet.parseGoogleURI(o2.toString(), "subnetworks");
                    String subnetRegion = GoogleCloudGet.parseGoogleURI(o2.toString(), "regions");
                    JSONObject subnetInfo = gcpGet.getSubnet(subnetRegion, subnetName);
                    String subnetUri = lookupResourceUri(metadata, Mrs.SwitchingSubnet, name, subnetRegion, subnetName);
                    //String subnetUri = recoverGcpUri(subnetInfo);
                    String cidr = subnetInfo.get("ipCidrRange").toString();
                    String gateway = subnetInfo.get("gatewayAddress").toString();
                    
                    Resource subnet = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetUri, GoogleCloudPrefix.subnet, name, subnetRegion, subnetName), Mrs.SwitchingSubnet);
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
                
                /*
                Add routes.
                Routes contain routeTo and nextHop info
                routeFrom info is not included.
                */
                JSONArray routesInfo = (JSONArray) gcpGet.getRoutes().get("items");
                
                for (Object o2: routesInfo) {
                    JSONObject routeInfo = (JSONObject) o2;
                    String routeName = routeInfo.get("name").toString();
                    String routeUri = lookupResourceUri(metadata, Mrs.Route, name, routeName);
                    String destRange = routeInfo.get("destRange").toString();
                    String nextHop = "unknown";
                    String vpcName = GoogleCloudGet.parseGoogleURI(routeInfo.get("network").toString(), "networks");
                    if (routeInfo.get("nextHopNetwork") != null) {
                        //nextHop is a vpc
                        nextHop = vpc.getURI()+":subnet";
                    } else if (routeInfo.get("nextHopGateway") != null) {
                        //nextHop is the internet gateway of a vpc
                        nextHop = vpc.getURI()+":internetGateway";
                    } else {
                        logger.warning(method, String.format("route %s's destination is not currently modeled."));
                    }
                    
                    Resource route = RdfOwl.createResource(model, ResourceTool.getResourceUri(routeUri, GoogleCloudPrefix.route, vpcName, routeName), Mrs.Route);
                    Resource routeTo = RdfOwl.createResource(model, route.getURI()+":route-to", Mrs.NetworkAddress);
                    model.add(model.createStatement(routeTo, Mrs.type, "ipv4-prefix-list"));
                    model.add(model.createStatement(routeTo, Mrs.value , destRange));
                    
                    model.add(model.createStatement(routingService, Mrs.providesRoute, route));
                    model.add(model.createStatement(routingTable, Mrs.hasRoute, route));
                    model.add(model.createStatement(route, Mrs.routeTo, routeTo));
                    model.add(model.createStatement(route, Mrs.nextHop, nextHop));
                }
            }
        } else {
            logger.error(method, "failed to get VPCs due to null response");
        }
        
        //Add VMs to model
        JSONObject instancesInfo = gcpGet.getVmInstances(); 
        if (instancesInfo != null) {
            JSONArray vms = (JSONArray) instancesInfo.get("items");
            for (Object o : vms) {
                JSONObject vmInfo = (JSONObject) o;
                String instanceName = vmInfo.get("name").toString();
                String instanceType = GoogleCloudGet.parseGoogleURI(vmInfo.get("machineType").toString(), "machineTypes");
                
                String zone = GoogleCloudGet.parseGoogleURI(vmInfo.get("zone").toString(), "zones");
                JSONArray netifaces = (JSONArray) vmInfo.get("networkInterfaces");
                String vpcName = "none";
                String subnetName = "none";
                String networkIP = "none";
                String natIP = "none";
                
                if (netifaces == null) {
                    logger.warning(method, "unable to find network interface for instance "+instanceName);
                } else {
                    if (netifaces.size() > 1) {
                        logger.warning(method, String.format("VM instance %s has %d network interfaces; only the first will be modeled.", instanceName, netifaces.size()));
                    }
                    //For now, we assume there is only one network interface
                    JSONObject netiface = (JSONObject) netifaces.get(0);
                    vpcName = GoogleCloudGet.parseGoogleURI(netiface.get("network").toString(), "networks");
                    subnetName = GoogleCloudGet.parseGoogleURI(netiface.get("subnetwork").toString(), "subnetworks");
                    networkIP = netiface.get("networkIP").toString();
                    natIP = GoogleCloudGet.getInstancePublicIP(netiface);
                }

                String vpcUri = lookupResourceUri(metadata, Mrs.VirtualCloudService, vpcName);
                String instanceUri = lookupResourceUri(metadata, Nml.Node, zone, instanceName);
                
                Resource instance = RdfOwl.createResource(model, ResourceTool.getResourceUri(instanceUri, GoogleCloudPrefix.instance, vpcName, zone, instanceName), Nml.Node);
                Resource vpc = model.getResource(ResourceTool.getResourceUri(vpcUri, GoogleCloudPrefix.vpc, vpcName));
                model.add(model.createStatement(vpc, Nml.hasNode, instance));
                model.add(model.createStatement(instance, Mrs.type, instanceType));
                
                if (!natIP.equals("none")) {
                    Resource publicIP = RdfOwl.createResource(model, instance.getURI()+":publicIP", Mrs.NetworkAddress);
                    model.add(model.createStatement(publicIP, Mrs.type, "ipv4-address"));
                    model.add(model.createStatement(publicIP, Mrs.value, networkIP));
                    model.add(model.createStatement(instance, Mrs.hasNetworkAddress, publicIP));
                }
                
                model.add(model.createStatement(instance, Mrs.providedByService, computeService));
                model.add(model.createStatement(computeService, Mrs.providesVM, instance));
                
                //add each disk within vm
                JSONArray disksInfo = (JSONArray) vmInfo.get("disks");
                
                for (Object o2 : disksInfo) {
                    JSONObject diskInfo = (JSONObject) o2;
                    JSONObject fullDiskInfo = gcpGet.getVolume(zone, instanceName);
                    
                    String size = fullDiskInfo.get("sizeGb").toString();
                    String type = GoogleCloudGet.parseGoogleURI(fullDiskInfo.get("type").toString(), "diskTypes");
                    String diskName = fullDiskInfo.get("name").toString();
                    String diskUri = lookupResourceUri(metadata, Mrs.Volume, zone, diskName);
                    
                    Resource volume = RdfOwl.createResource(model, ResourceTool.getResourceUri(diskUri, GoogleCloudPrefix.volume, zone, diskName), Mrs.Volume);
                    model.add(model.createStatement(instance, Mrs.hasVolume, volume));
                    model.add(model.createStatement(blockStorageService, Mrs.providesVolume, volume));
                    model.add(model.createStatement(volume, Mrs.value, type));
                    model.add(model.createStatement(volume, Mrs.disk_gb, size));
                }
            }
        } else {
            logger.error(method, "failed to get instancess due to null response");
        }
        
        //buckets
        JSONObject bucketsResponse = gcpGet.getBuckets();
        if (bucketsResponse != null) {
            JSONArray bucketsInfo = (JSONArray) bucketsResponse.get("items");
            for (Object o : bucketsInfo) {
                JSONObject bucketInfo = (JSONObject) o;
                String bucketName = bucketInfo.get("name").toString();
                String bucketUri = lookupResourceUri(metadata, Mrs.Bucket, bucketName);
                //String bucketUri = recoverGcpUri(bucketInfo);
                Resource bucket = RdfOwl.createResource(model, ResourceTool.getResourceUri(bucketUri, GoogleCloudPrefix.bucket, bucketName), Mrs.Bucket);
                model.add(model.createStatement(objectStorageService, Mrs.providesBucket, bucket));
                model.add(model.createStatement(gcpTopology, Mrs.hasBucket, bucket));
                model.add(model.createStatement(bucket, Nml.name, bucketName));
            }
        } else {
            logger.error(method, "failed to get buckets due to null response");
        }
        
        logger.end(method);
        return model;
    }
    
    public static String getResourceKey(Resource type, String... args) {
        String key = null, method = "getResourceKey";
        //logger.start(method);
        
        
        if (Mrs.VirtualCloudService.equals(type)) {
            if (args.length == 1) {
                //VPCs are identified by name only
                key = String.format("vpc_%s", args);
            } else {
                logger.warning(method, "failed VPC URI retrieval due to incorrect nuber of args");
            }
        } else if (Mrs.SwitchingSubnet.equals(type)) {
            if (args.length == 3) {
                //Subnets are identified by vpc, name, and region
                //Subnets in different regions or different vpcs may have same name
                key = String.format("subnet_%s_%s_%s", args);
            } else {
                logger.warning(method, "failed subnet URI retrieval due to incorrect nuber of args");
            }
        } else if (Mrs.Route.equals(type)) {
            if (args.length == 2) {
                //Routes are identified by vpc and name
                key = String.format("route_%s_%s", args);
            } else {
                logger.warning(method, "failed route URI retrieval due to incorrect nuber of args");
            }
        } else if (Nml.Node.equals(type)) {
            if (args.length == 2) {
                //VM instance
                //identified by zone and name
                key = String.format("instance_%s_%s", args );
            } else {
                logger.warning(method, "failed instance URI retrieval due to incorrect nuber of args");
            }
        } else if (Mrs.Volume.equals(type)) {
            if (args.length == 2) {
                //identified by zone and name
                key = String.format("volume_%s_%s", args);
            } else {
                logger.warning(method, "failed volume URI retrieval due to incorrect nuber of args");
            }
        } else if (Mrs.Bucket.equals(type)) {
            if (args.length == 1) {
                key = String.format("bucket_%s", args);
            } else {
                logger.warning(method, "failed bucket URI retrieval due to incorrect nuber of args");
            }
        } else {
            logger.warning(method, "failed resource URI retrieval due to unknown resource");
        }
        
        //logger.end(method);
        
        //adding uri_ ensures that normal entries in the metadata table will never be mistaken for uri entries
        if (key == null) return "";
        else return "uri_"+key;
    }
    
    public static String lookupResourceUri(HashMap<String, String> metadata, Resource type, String... args) {
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
