/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import net.maxgigapop.mrs.common.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

//import com.google.api.services.compute.Compute;
/**
 *
 * @author raymonddsmith
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
        model.add(model.createStatement(gcpTopology, Nml.hasService, vpcService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, computeService));
        model.add(model.createStatement(gcpTopology, Nml.hasService, storageService));
        
        //JSONArray subnetlist = GoogleCloudGet.combineAggregatedList( (JSONObject) computeGet.getSubnets().get("items"), "subnetworks");
        
        //Add networks to the model
        JSONObject vpcInfo = computeGet.getVPCs();
        if (vpcInfo != null) {
            JSONArray vpcs = (JSONArray) vpcInfo.get("items");
            for (Object o : vpcs) {
                JSONObject network = (JSONObject) o;
                String name = network.get("name").toString();
                
                Resource vpc = RdfOwl.createResource(model, ResourceTool.getResourceUri("", GoogleCloudPrefix.vpc, name), Nml.Topology);
                //System.out.println("create vpc: "+vpc.getURI());
                model.add(model.createStatement(gcpTopology, Nml.hasTopology, vpc));
                Resource switchingService = RdfOwl.createResource(model, vpc.getURI()+":switchingservice", Mrs.SwitchingService);
                model.add(model.createStatement(vpc, Nml.hasService, switchingService));
                
                JSONArray subnetworks = (JSONArray) network.get("subnetworks");
                for (Object o2 : subnetworks) {
                    String subnetName = GoogleCloudGet.getSubnetName(o2.toString());
                    String subnetRegion = GoogleCloudGet.getSubnetRegion(o2.toString());
                    //System.out.println("Region: "+subnetRegion);
                    JSONObject subnetInfo = computeGet.getSubnet(subnetRegion, subnetName);
                    String cidr = subnetInfo.get("ipCidrRange").toString();
                    String gateway = subnetInfo.get("gatewayAddress").toString();
                    //System.out.println(subnetName+", "+subnetRegion+" -> "+subnetInfo);
                    Resource subnet = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetName, GoogleCloudPrefix.subnet, name, subnetRegion, subnetName), Mrs.SwitchingSubnet);
                    //System.out.println(subnet);
                    model.add(model.createStatement(switchingService, Mrs.providesSubnet, subnet));
                    
                    Resource subnetGateway = RdfOwl.createResource(model, subnet.getURI()+":gateway", Nml.BidirectionalPort);
                    model.add(model.createStatement(subnet, Nml.hasBidirectionalPort, subnetGateway));

                    Resource gatewayIP = RdfOwl.createResource(model, subnetGateway.getURI()+":gatewayIP", Mrs.NetworkAddress);
                    model.createStatement(subnetGateway, Mrs.hasNetworkAddress, gatewayIP);
                    model.createStatement(gatewayIP, Mrs.type, "ipv4-address");
                    model.createStatement(gatewayIP, Mrs.value, gateway);
                    
                    Resource subnetCIDR = RdfOwl.createResource(model, subnet.getURI()+":cidr", Mrs.NetworkAddress);
                    model.createStatement(subnet, Mrs.hasNetworkAddress, subnetCIDR);
                    model.createStatement(subnetCIDR, Mrs.type, "ipv4-prefix-list");
                    model.createStatement(subnetCIDR, Mrs.value, cidr);
                }
            }
        } else {
            System.out.println("Get VPCs failed.");
        }
        
        //Add VMs to model
        JSONObject vmInstances = computeGet.getVmInstances();
        if (vmInstances != null) {
            JSONArray vms = (JSONArray) vmInstances.get("items");
            for (Object o : vms) {
                JSONObject vm = (JSONObject) o;
                String instanceName = vm.get("name").toString();
                String instanceType = vm.get("machineType").toString();
                JSONArray netifaces = (JSONArray) vm.get("networkInterfaces");
                String vpcName = "none";
                String subnetName = "none";
                String networkIP = "none";
                String natIP = "none";
                
                if (netifaces != null) {
                    JSONObject netiface = (JSONObject) netifaces.get(0);
                    vpcName = GoogleCloudGet.getVPCName(netiface.get("network").toString());
                    subnetName = GoogleCloudGet.getSubnetName(netiface.get("subnetwork").toString());
                    networkIP = netiface.get("networkIP").toString();
                    natIP = GoogleCloudGet.getInstancePublicIP(netiface);
                }
                
                Resource instance = RdfOwl.createResource(model, ResourceTool.getResourceUri(instanceName, GoogleCloudPrefix.instance, vpcName, subnetName, instanceName), Nml.Node);
                Resource vpc = model.getResource(ResourceTool.getResourceUri("", GoogleCloudPrefix.vpc, vpcName));
                //System.out.println("get vpc:    "+vpc.getURI());
                
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
                JSONArray disks = (JSONArray) vm.get("disks");
                for (Object o2 : disks) {
                    JSONObject disk = (JSONObject) o2;
                    String diskName = disk.get("deviceName").toString();
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
