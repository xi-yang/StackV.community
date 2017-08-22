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
        
        //set global vars here
        GoogleCloudGet computeGet = new GoogleCloudGet(jsonAuth, projectID, region);
        Resource project = RdfOwl.createResource(model, ResourceTool.getResourceUri(projectID, GoogleCloudPrefix.project, projectID), Mrs.VirtualCloudService);
        
        //Add networks to the model
        JSONArray vpcs = (JSONArray) computeGet.getVPCs().get("items");
        for (Object o : vpcs) {
            JSONObject network = (JSONObject) o;
            String name = network.get("name").toString();
            
            Resource vpc = RdfOwl.createResource(model, ResourceTool.getResourceUri(name, GoogleCloudPrefix.vpc), Mrs.VirtualCloudService);
            model.createStatement(project, Nml.hasService, vpc);
            
            
                    
            JSONArray subnetworks = (JSONArray) network.get("subnetworks");
            for (Object o2 : subnetworks) {
                JSONObject subnetwork = (JSONObject) o2;
                String subnetName = subnetwork.get("name").toString();
                Resource subnet = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetName, GoogleCloudPrefix.subnet, name, subnetName), Mrs.SwitchingSubnet);
                model.createStatement(vpc, Mrs.providesSubnet, subnet);
            }
        }
        
        //Add VMs to model
        JSONArray vms = (JSONArray) computeGet.getVmInstances().get("items");
        for (Object o : vms) {
            JSONObject vm = (JSONObject) o;
            String name = vm.get("name").toString();
            String instanceType = vm.get("machineType").toString();
            JSONObject netiface = (JSONObject) vm.get("NetworkInterface");
            String vpcName = netiface.get("network").toString();
            String subnetName = netiface.get("subnetwork").toString();
            
            Resource instance = RdfOwl.createResource(model, ResourceTool.getResourceUri(name, GoogleCloudPrefix.instance, vpcName, subnetName, name), Nml.Node);
            
            //add each disk within vm
            JSONArray disks = (JSONArray) vm.get("disks");
            for (Object o2 : disks) {
                JSONObject disk = (JSONObject) o2;
                
                
            }
            
            
        }
        return model;
    }
}
