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
        
        GoogleCloudGet computeGet = new GoogleCloudGet(jsonAuth, projectID, region);
        
        Resource gcpTopology = RdfOwl.createResource(model, topologyURI, Nml.Topology);
        Resource vpcService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.vpcService, region), Mrs.VirtualCloudService);
        Resource computeService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.computeService, region), Mrs.HypervisorService);
        Resource storageService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.storageService, region), Mrs.StorageService);
        Resource routingService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.routingService, region), Mrs.RoutingService);
        //google cloud does not use a routing table, so all routes are placed in one routing table
        Resource routingTable = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.routingTable, region), Mrs.RoutingTable);
        model.add(model.createStatement(routingTable, Mrs.type, "main"));
        model.add(model.createStatement(routingService, Mrs.providesRoutingTable, routingTable));
        model.add(model.createStatement(gcpTopology, Nml.hasService, vpcService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, computeService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, storageService));
        
        //Add networks to the model
        JSONObject vpcsInfo = computeGet.getVPCs();
        if (vpcsInfo != null) {
            JSONArray vpcs = (JSONArray) vpcsInfo.get("items");
            for (Object o : vpcs) {
                JSONObject vpcInfo = (JSONObject) o;
                String name = vpcInfo.get("name").toString();
                
                Resource vpc = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.vpc, name), Nml.Topology);
                model.add(model.createStatement(gcpTopology, Nml.hasTopology, vpc));
                Resource switchingService = RdfOwl.createResource(model, vpc.getURI()+":switchingservice", Mrs.SwitchingService);
                model.add(model.createStatement(vpc, Nml.hasService, switchingService));
                model.add(model.createStatement(vpc, Nml.hasService, routingService));
                
                Resource internetGateway = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.internetGateway, name), Nml.BidirectionalPort);
                model.add(model.createStatement(internetGateway, Mrs.type, "internet-gateway"));
                
                JSONArray subnetsInfo = (JSONArray) vpcInfo.get("subnetworks");
                
                for (Object o2 : subnetsInfo) {
                    String subnetName = GoogleCloudGet.getSubnetName(o2.toString());
                    String subnetRegion = GoogleCloudGet.getSubnetRegion(o2.toString());
                    JSONObject subnetInfo = computeGet.getSubnet(subnetRegion, subnetName);
                    String cidr = subnetInfo.get("ipCidrRange").toString();
                    String gateway = subnetInfo.get("gatewayAddress").toString();
                    Resource subnet = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetName, GoogleCloudPrefix.subnet, name, subnetRegion, subnetName), Mrs.SwitchingSubnet);
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
                Routes are added last.
                Routes contain routeTo and nextHop info
                routeFrom info is not included.
                */
                JSONArray routesInfo = (JSONArray) computeGet.getRoutes().get("items");
                
                for (Object o2: routesInfo) {
                    JSONObject routeInfo = (JSONObject) o2;
                    String routeName = routeInfo.get("name").toString();
                    String destRange = routeInfo.get("destRange").toString();
                    Resource nextHop = null;
                    String vpcName = GoogleCloudGet.getVPCName(routeInfo.get("network").toString());
                    if (routeInfo.get("nextHopNetwork") != null) {
                        //nextHop is a vpc
                        nextHop = model.getResource(ResourceTool.getResourceUri("", GoogleCloudPrefix.vpc, vpcName));
                    } else {
                        //nextHop is the internet gateway of a vpc
                        nextHop = model.getResource(ResourceTool.getResourceUri("", GoogleCloudPrefix.internetGateway, vpcName));
                    }
                    
                    Resource route = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.route, vpcName, routeName), Mrs.Route);
                    Resource routeTo = RdfOwl.createResource(model, route.getURI()+":route-to", Mrs.NetworkAddress);
                    model.add(model.createStatement(routeTo, Mrs.type, "ipv4-prefix-list"));
                    model.add(model.createStatement(routeTo, Mrs.value , destRange));
                    
                    model.add(model.createStatement(routingService, Mrs.providesRoute, route));
                    model.add(model.createStatement(routingTable, Mrs.hasRoute, route));
                    model.add(model.createStatement(route, Mrs.routeTo, routeTo));
                    
                    if (nextHop != null) {
                        model.add(model.createStatement(route, Mrs.nextHop, nextHop));
                    }
                }
            }
        } else {
            System.out.println("Get VPCs failed.");
        }
        
        //Add VMs to model
        JSONObject instancesInfo = computeGet.getVmInstances();
        if (instancesInfo != null) {
            JSONArray vms = (JSONArray) instancesInfo.get("items");
            for (Object o : vms) {
                JSONObject vmInfo = (JSONObject) o;
                String instanceName = vmInfo.get("name").toString();
                String instanceType = vmInfo.get("machineType").toString();
                JSONArray netifaces = (JSONArray) vmInfo.get("networkInterfaces");
                String vpcName = "none";
                String subnetName = "none";
                String networkIP = "none";
                String natIP = "none";
                
                if (netifaces != null) {
                    //For now, we assume there is only one network interface
                    JSONObject netiface = (JSONObject) netifaces.get(0);
                    vpcName = GoogleCloudGet.getVPCName(netiface.get("network").toString());
                    subnetName = GoogleCloudGet.getSubnetName(netiface.get("subnetwork").toString());
                    networkIP = netiface.get("networkIP").toString();
                    natIP = GoogleCloudGet.getInstancePublicIP(netiface);
                }
                
                Resource instance = RdfOwl.createResource(model, ResourceTool.getResourceUri(instanceName, GoogleCloudPrefix.instance, vpcName, subnetName, instanceName), Nml.Node);
                Resource vpc = model.getResource(ResourceTool.getResourceUri("", GoogleCloudPrefix.vpc, vpcName));
                model.add(model.createStatement(vpc, Nml.hasNode, instance));
                
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
                    Resource volume = RdfOwl.createResource(model, ResourceTool.getResourceUri(diskName, GoogleCloudPrefix.volume, vpcName, instanceName, diskName), Mrs.Volume);
                    model.add(model.createStatement(instance, Mrs.hasVolume, volume));
                }
            }
        } else {
            System.out.println("Get Instances failed.");
        }
        
        return model;
    }
}
