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
    public static OntModel createOntology(String jsonAuth, String projectID, String region, String topologyURI) throws IOException {
        //create model object
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
        
        //Add VPCs to the model
        JSONObject vpcsInfo = gcpGet.getVPCs();
        if (vpcsInfo != null) {
            JSONArray vpcs = (JSONArray) vpcsInfo.get("items");
            for (Object o : vpcs) {
                JSONObject vpcInfo = (JSONObject) o;
                String name = vpcInfo.get("name").toString();
                String vpcUri = vpcInfo.get("description").toString();
                
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
                    String subnetUri = recoverGcpUri(subnetInfo);
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
                    String routeUri = recoverGcpUri(routeInfo);
                    String destRange = routeInfo.get("destRange").toString();
                    //Resource nextHop = null;
                    String nextHop = "none";
                    String vpcName = GoogleCloudGet.parseGoogleURI(routeInfo.get("network").toString(), "networks");
                    if (routeInfo.get("nextHopNetwork") != null) {
                        //nextHop is a vpc
                        nextHop = "subnet";
                    } else {
                        //nextHop is the internet gateway of a vpc
                        nextHop = "internetGateway";
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
            System.out.println("Get VPCs failed.");
        }
        
        //Add VMs to model
        JSONObject instancesInfo = gcpGet.getVmInstances(); 
        if (instancesInfo != null) {
            JSONArray vms = (JSONArray) instancesInfo.get("items");
            for (Object o : vms) {
                JSONObject vmInfo = (JSONObject) o;
                String instanceName = vmInfo.get("name").toString();
                String instanceType = GoogleCloudGet.parseGoogleURI(vmInfo.get("machineType").toString(), "machineTypes");
                String instanceUri = recoverGcpUri(vmInfo);
                
                String zone = GoogleCloudGet.parseGoogleURI(vmInfo.get("zone").toString(), "zones");
                JSONArray netifaces = (JSONArray) vmInfo.get("networkInterfaces");
                String vpcName = "none";
                String subnetName = "none";
                String networkIP = "none";
                String natIP = "none";
                
                if (netifaces != null) {
                    //For now, we assume there is only one network interface
                    JSONObject netiface = (JSONObject) netifaces.get(0);
                    vpcName = GoogleCloudGet.parseGoogleURI(netiface.get("network").toString(), "networks");
                    subnetName = GoogleCloudGet.parseGoogleURI(netiface.get("subnetwork").toString(), "subnetworks");
                    networkIP = netiface.get("networkIP").toString();
                    natIP = GoogleCloudGet.getInstancePublicIP(netiface);
                }
                
                Resource instance = RdfOwl.createResource(model, ResourceTool.getResourceUri(instanceUri, GoogleCloudPrefix.instance, vpcName, zone, instanceName), Nml.Node);
                Resource vpc = model.getResource(ResourceTool.getResourceUri("", GoogleCloudPrefix.vpc, vpcName));
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
                    String diskName = diskInfo.get("deviceName").toString();
                    String diskUri = recoverGcpUri(diskInfo);
                    JSONObject fullDiskInfo = gcpGet.getVolume(zone, instanceName);
                    
                    String size = fullDiskInfo.get("sizeGb").toString();
                    String type = GoogleCloudGet.parseGoogleURI(fullDiskInfo.get("type").toString(), "diskTypes");
                    
                    Resource volume = RdfOwl.createResource(model, ResourceTool.getResourceUri(diskUri, GoogleCloudPrefix.volume, vpcName, instanceName, diskName), Mrs.Volume);
                    model.add(model.createStatement(instance, Mrs.hasVolume, volume));
                    model.add(model.createStatement(blockStorageService, Mrs.providesVolume, volume));
                    model.add(model.createStatement(volume, Mrs.value, type));
                    model.add(model.createStatement(volume, Mrs.disk_gb, size));
                }
            }
        } else {
            System.out.println("Get Instances failed.");
        }
        
        //buckets
        JSONObject bucketsResponse = gcpGet.getBuckets();
        if (bucketsResponse != null) {
            JSONArray bucketsInfo = (JSONArray) bucketsResponse.get("items");
            for (Object o : bucketsInfo) {
                JSONObject bucketInfo = (JSONObject) o;
                String bucketName = bucketInfo.get("name").toString();
                String bucketUri = recoverGcpUri(bucketInfo);
                Resource bucket = RdfOwl.createResource(model, ResourceTool.getResourceUri(bucketUri, GoogleCloudPrefix.bucket, bucketName), Mrs.Bucket);
                model.add(model.createStatement(objectStorageService, Mrs.providesBucket, bucket));
                model.add(model.createStatement(gcpTopology, Mrs.hasBucket, bucket));
                model.add(model.createStatement(bucket, Nml.name, bucketName));
            }
        } else {
            System.out.println("Get Buckets failed.");
        }
        
        return model;
    }
    
    public static String getResourceKey(Resource type, String... args) {
        String output = "";
        
        if (Mrs.VirtualCloudService.equals(type)) {
            if (args.length == 1) {
                //VPCs are identified by name only
                output = String.format("vpc_%s", args);
            } else {
                //error
            }
        } else if (Mrs.SwitchingSubnet.equals(type)) {
            if (args.length == 3) {
                //Subnets are identified by vpc, name, and region
                //Subnets in different regions or different vpcs may have same name
                output = String.format("subnet_%s_%s_%s", args);
            } else {
                //error
            }
        } else if (Mrs.Route.equals(type)) {
            if (args.length == 2) {
                //Routes are identified by vpc and name
                output = String.format("route_%s_%s", args);
            } else {
                //error
            }
        } else if (Nml.Node.equals(type)) {
            if (args.length == 2) {
                //VM instance
                //identified by zone and name
                output = String.format("instance_%s_%s", args );
            }
        } else if (Mrs.Volume.equals(type)) {
            if (args.length == 2) {
                output = String.format("volume_%s_%s", args);
            }
        } else if (Mrs.Bucket.equals(type)) {
            if (args.length == 1) {
                output = String.format("bucket_%s", args);
            }
        } else {
            return null;
        }
        
        //this ensures that normal entries in the metadata table will never be mistaken for uri entries
        return "uri_"+output;
    }
    
    public static String makeUriGcpCompatible (String uri) {
        /*
        Google cloud labels only support alphanumeric chars, hyphen -, and underscore.
        GCP names only support alphanumeric and hyphen, so hyphen is used as an escape character,
        This allows URIs of 63 characters or less to appear in GCP resource names
        _ -> -u
        + -> -p
        : -> -c
        . -> -d
        - -> -h
        */
        return uri.replaceAll("-", "-h")
                  .replaceAll("_", "-u")
                  .replaceAll("[+]", "-p")
                  .replaceAll(":", "-c")
                  .replaceAll("[.]", "-d");
    }
    
    public static String convertGcpUri (String uri) {
        return uri.replaceAll("-p", "+")
                  .replaceAll("-c", ":")
                  .replaceAll("-d", ".")
                  .replaceAll("-u", "_")
                  .replaceAll("-h", "-");
    }
    
    public static String recoverGcpUri (JSONObject o) {
        /*
        The URI could be stored in the resource labels under URI,
        in the resource description, or in the resource name itself.
        This function checks these three locations in that order.
        */
        if (o == null) return "";
        if (o.containsKey("labels")) {
            JSONObject labels = (JSONObject) o.get("labels");
            if (labels.containsKey("uri")) {
                return convertGcpUri(labels.get("uri").toString());
            }
        }
        
        if (o.containsKey("description")) {
            return convertGcpUri(o.get("description").toString());
        } else if (o.containsKey("name")) {
            return convertGcpUri(o.get("name").toString());
        } else {
            return "";
        }
    }
    
}
