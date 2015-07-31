 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.onosystem;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.maxgigapop.mrs.common.*;
import net.maxgigapop.mrs.driver.onosystem.OnosServer;
import org.json.simple.parser.ParseException;
//TODO add the public ip address that an instance might have that is not an
//elastic ip

/*TODO: Intead of having separate routeFrom statements for routes in a route table 
associated with subnets. Include the routeFrom statement just once in the model, 
meaning that look just once for the associations of the route table, 
do not do a routeFrom statement for every route.*/

/*
 *
 * @author muzcategui
 */
public class OnosModelBuilder {

    //public static OntModel createOntology(String access_key_id, String secret_access_key, Regions region, String topologyURI) throws IOException {
    public static OntModel createOntology(String topologyURI, String subsystemBaseUrl) throws IOException, ParseException {

        //create model object
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        //set all the model prefixes"
        
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
        Property providesVPC = Mrs.providesVPC;
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
        Resource routingService = Mrs.RoutingService;
        Resource blockStorageService = Mrs.BlockStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology = Nml.Topology;

        Resource networkAddress = Mrs.NetworkAddress;
        Resource switchingSubnet = Mrs.SwitchingSubnet;
        Resource switchingService = Mrs.SwitchingService;
        Resource node = Nml.Node;
        Resource biPort = Nml.BidirectionalPort;
        Resource onosTopology = RdfOwl.createResource(model, topologyURI, topology);
        Resource objectStorageService = Mrs.ObjectStorageService;
        Resource routingTable = Mrs.RoutingTable;
        
        OnosServer onos = new OnosServer();
        String device[][] = onos.getOnosDevices(subsystemBaseUrl);
        int qtyDevices=onos.qtyDevices;
        for(int i=0;i<qtyDevices;i++){
            Resource resNode = RdfOwl.createResource(model,topologyURI+":"+device[1][i]+"-"+device[0][i],node);
            model.add(model.createStatement(resNode, type, device[1][i]));
            //System.out.println(resNode.toString());
            String devicePorts[][]=onos.getOnosDevicePorts(subsystemBaseUrl,device[0][i]);
            int qtyPorts=onos.qtyPorts;
            for(int j=0;j<qtyPorts;j++){
                Resource resPort = RdfOwl.createResource(model,topologyURI+":"+device[1][i]+"-"+device[0][i]+":port-"+devicePorts[4][j],biPort);
                model.add(model.createStatement(resNode,hasBidirectionalPort,resPort));
                //System.out.println(resPort.toString());
            }
            
        }
        
        for(int i=0;i<qtyDevices;i++){
            Resource srcNode = RdfOwl.createResource(model,topologyURI+":"+device[1][i]+"-"+device[0][i],node);
            String links[][] = onos.getOnosLinks(subsystemBaseUrl);
            int qtyLinks=onos.qtyLinks;
            for(int k=0;k<qtyLinks;k++){
                if(device[0][i].equals(links[1][k])){
                    for(int l=0;l<qtyDevices;l++){
                        if(links[3][k].equals(device[0][l])){
                            Resource dstNode = RdfOwl.createResource(model,topologyURI+":"+device[1][l]+"-"+device[0][l],node);
                            model.add(model.createStatement(srcNode,Nml.isAlias,dstNode));
                        }
                    }
                }
            }
        }
     
        return model;
    }
}
