/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver.openstack;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author james
 */
public class OpenStackModelBuilder {
    
    public static OntModel createOntology(String hostName, String tenantName, String tenantPasswd) throws IOException {
        
        String host = "charon.dragon.maxgigapop.net";
        String tenant = "admin";
        String tenantId;
        String token;
        
        Logger logger = Logger.getLogger(OpenStackModelBuilder.class.getName());
    
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF); 
        
        model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
        model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        model.setNsPrefix("nml", "http://schemas.ogf.org/nml/2013/03/base#");
        model.setNsPrefix("mrs", "http://schemas.ogf.org/mrs/2013/12/topology#");
        
        Property hasNode = model.createProperty( "http://schemas.ogf.org/nml/2013/03/base#hasNode" );
        Property hasService = model.createProperty( "http://schemas.ogf.org/nml/2013/03/base#hasService" );
        Property providesVM = model.createProperty( "http://schemas.ogf.org/mrs/2013/12/topology#providesVM" );
        Property type = model.createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Property memory_mb = model.createProperty( "http://schemas.ogf.org/mrs/2013/12/topology#memory_mb" );
        Property num_core = model.createProperty( "http://schemas.ogf.org/mrs/2013/12/topology#num_core" );
        Property disk_gb = model.createProperty( "http://schemas.ogf.org/mrs/2013/12/topology#disk_gb" );
        
        Resource HypervisorService = model.createResource( "http://schemas.ogf.org/mrs/2013/12/topology#HypervisorService" );
        Resource Node = model.createResource( "http://schemas.ogf.org/nml/2013/03/base#Node" );
        Resource Topology = model.createResource( "http://schemas.ogf.org/mrs/2013/12/topology#Topology" );
        Resource VirtualSwitchService = model.createResource( "http://schemas.ogf.org/mrs/2013/12/topology#VirtualSwitchService" );
        Resource NamedIndividual = model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual");
        Resource Nova = model.createResource("urn:ogf:network:dragon.maxgigapop.net:openstack-nova");
        Resource Neutron = model.createResource("urn:ogf:network:dragon.maxgigapop.net:openstack-neutron");
        Resource OpenstackTopology = model.createResource("urn:ogf:network:dragon.maxgigapop.net:topology");
        
        model.add(model.createStatement(OpenstackTopology, type, Topology));
        model.add(model.createStatement(OpenstackTopology, type, NamedIndividual));
        
        model.add(model.createStatement(Nova, type, HypervisorService));
        model.add(model.createStatement(Nova, type, NamedIndividual));
        
        model.add(model.createStatement(Neutron, type, VirtualSwitchService));
        model.add(model.createStatement(Neutron, type, NamedIndividual));
        
        token = OpenStackRESTClient.getToken(host, tenant, "admin", "admin");
        tenantId = OpenStackRESTClient.getTenantId(host, tenant, token);
        JSONArray novaDescription = OpenStackRESTClient.pullNovaConfig(host, tenantId, token); 
               
        for(Object o : novaDescription) {
            
            JSONArray node = (JSONArray) ((JSONObject) o).get("host");
            
            if(node != null) {
                
                JSONObject resource = (JSONObject) ((JSONObject)node.get(0)).get("resource");
                String nodeName = (String) resource.get("host");
                Long numCpu = (Long) resource.get("cpu");
                Long memMb = (Long) resource.get("memory_mb");
                Long diskGb = (Long) resource.get("disk_gb");
                
                Resource computeNode = model.createResource("urn:ogf:network:dragon.maxgigapop.net:" + nodeName);
                Literal cpu = model.createTypedLiteral(numCpu);
                Literal mem = model.createTypedLiteral(memMb);
                Literal disk = model.createTypedLiteral(diskGb);
                
                model.add(model.createStatement(OpenstackTopology, hasNode, computeNode));
                model.add(model.createStatement(computeNode, type, Node));
                model.add(model.createStatement(computeNode, type, NamedIndividual));
                model.add(model.createStatement(computeNode, hasService, Nova));
                
                model.add(model.createStatement(computeNode, memory_mb, mem));
                model.add(model.createStatement(computeNode, disk_gb, disk));
                model.add(model.createStatement(computeNode, num_core, cpu));                               
            }
            
            JSONObject networkHost = (JSONObject) ((JSONObject) o).get("network_host");
            if (networkHost != null) {
                
                Resource networkNode = model.createResource("urn:ogf:network:dragon.maxgigapop.net:" + (String) networkHost.get("host_name"));
                
                model.add(model.createStatement(OpenstackTopology, hasNode, networkNode));
                model.add(model.createStatement(networkNode, type, Node));
                model.add(model.createStatement(networkNode, type, NamedIndividual));
                model.add(model.createStatement(networkNode, hasService, Neutron));
            }
        }
        
        tenantId = OpenStackRESTClient.getTenantId(host, "demo", token);
        token = OpenStackRESTClient.getToken(host, "demo", "demo", "demo");
        JSONArray vms = OpenStackRESTClient.pullNovaVM(host, tenantId, token);
        
        for(Object o : vms) {
            
            String vmName = (String) ((JSONObject) o).get("name");
            Resource vm = model.createResource("urn:ogf:network:dragon.maxgigapop.net:" + vmName);
            
            model.add(model.createStatement(vm, type, Node));
            model.add(model.createStatement(vm, type, NamedIndividual));
            
            model.add(model.createStatement(Nova, providesVM, vm));

        }
        
        /* 
        JSONArray ports = (JSONArray) OpenStackRESTClient.pullNeutron(host, tenantId, token).get("ports");        
        for(Object o : ports) {
            
            
        } */
                
        
        logger.log(Level.INFO, "Ontology model for OpenStack driver rewritten");
        
        return model;       
    }
    
}
