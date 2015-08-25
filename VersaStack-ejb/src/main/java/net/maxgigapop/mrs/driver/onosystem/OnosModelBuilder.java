 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.onosystem;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
//import java.util.ArrayList;
//import java.util.List;
import net.maxgigapop.mrs.common.*;
import net.maxgigapop.mrs.driver.onosystem.OnosServer;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
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

        
        //add SRRG
        model.setNsPrefix("sna", Sna.getURI());

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
        Property hasFlow = Mrs.hasFlow;
        Property providesFlowTable = Mrs.providesFlowTable;
        Property providesFlow = Mrs.providesFlow;
        Property flowMatch = Mrs.flowMatch;
        Property flowAction = Mrs.flowAction;
        
        //set the global resources
        Resource route = Mrs.Route;
        Resource hypervisorService = Mrs.HypervisorService;
        Resource virtualCloudService = Mrs.VirtualCloudService;
        Resource routingService = Mrs.RoutingService;
        Resource blockStorageService = Mrs.BlockStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology = Nml.Topology;
        Resource vlan = model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
        Resource networkAddress = Mrs.NetworkAddress;
        Resource switchingSubnet = Mrs.SwitchingSubnet;
        Resource switchingService = Mrs.SwitchingService;
        Resource node = Nml.Node;
        Resource biPort = Nml.BidirectionalPort;
        Resource onosTopology = RdfOwl.createResource(model, topologyURI, topology);
        Resource objectStorageService = Mrs.ObjectStorageService;
        Resource routingTable = Mrs.RoutingTable;
        Resource openflowService = Mrs.OpenflowService;
        Resource flowTable = Mrs.FlowTable;
        Resource flow = Mrs.Flow;
        Resource flowRule = Mrs.FlowRule;
        
        
        //SRRG declare, only SRRG is included here currently
        Resource SRRG = Sna.SRRG;
        Property severity = Sna.severity;
        Property occurenceProbability = Sna.occurenceProbability;
        
        String SRRG_file = "/home/onos-versastack/NetBeansProjects/onos-driver/sample_srrg.json";
        
        OnosServer onos = new OnosServer();
        String device[][] = onos.getOnosDevices(subsystemBaseUrl);
        String hosts[][]=onos.getOnosHosts(subsystemBaseUrl);
        String links[][] = onos.getOnosLinks(subsystemBaseUrl);
        int qtyLinks=onos.qtyLinks;
        int qtyHosts=onos.qtyHosts;
        int qtyDevices=onos.qtyDevices;
        for(int i=0;i<qtyDevices;i++){
            Resource resNode = RdfOwl.createResource(model,topologyURI+":"+device[0][i],node);
            //model.add(model.createStatement(resNode, type, device[1][i]));
            model.add(model.createStatement(onosTopology,hasNode,resNode));
        }
        for(int i=0;i<qtyHosts;i++){
                Resource resNode = RdfOwl.createResource(model,topologyURI+":"+hosts[1][i],node);
                model.add(model.createStatement(onosTopology,hasNode,resNode));
        }
        
        for(int i=0;i<qtyDevices;i++){
            Resource resNode = RdfOwl.createResource(model,topologyURI+":"+device[0][i],node);
            if(device[1][i].equals("SWITCH") && device[2][i].equals("true")){
                Resource resOpenFlow = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service",openflowService);
                model.add(model.createStatement(resNode, hasService, topologyURI+":"+device[0][i]+":openflow-service"));
                
                String devicePorts[][]=onos.getOnosDevicePorts(subsystemBaseUrl,device[0][i]);
                int qtyPorts=onos.qtyPorts;
                String deviceFlows[][]=onos.getOnosDeviceFlows(subsystemBaseUrl,device[0][i]);
                int qtyFlows=onos.qtyFlows;
                for(int j=0;j<qtyPorts;j++){
                    if(devicePorts[1][j].equals("true")){
                        Resource resPort = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":port-"+devicePorts[4][j],biPort);
                        model.add(model.createStatement(resNode,hasBidirectionalPort,resPort));
                        model.add(model.createStatement(resOpenFlow,hasBidirectionalPort,resPort));
                        
                        for(int k=0;k<qtyLinks;k++){
                            if(device[0][i].equals(links[1][k]) && devicePorts[0][j].equals(links[0][k])){
                                links[2][k]=devicePorts[4][j];

                            }
                            else if(device[0][i].equals(links[4][k]) && devicePorts[0][j].equals(links[3][k])){
                                links[5][k]=devicePorts[4][j];
       
                            }
                        }
                    }
                }
                
                
                for(int j=0;j<qtyFlows;j++){
                        Resource resFlowTable = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service:flow-table-"+deviceFlows[1][j],flowTable);
                        model.add(model.createStatement(resOpenFlow,providesFlowTable,resFlowTable));
                        Resource resFlow = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service:flow-table-"+deviceFlows[1][j]+":flow-"+deviceFlows[0][j],flow);
                        model.add(model.createStatement(resFlowTable,providesFlow,resFlow));
                        Resource resFlowRule0 = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service:flow-table-"+deviceFlows[1][j]+":flow-"+deviceFlows[0][j]+":rule-match-0",flowRule);
                        Resource resFlowRule1 = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service:flow-table-"+deviceFlows[1][j]+":flow-"+deviceFlows[0][j]+":rule-match-1",flowRule);
                        Resource resFlowRule2 = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service:flow-table-"+deviceFlows[1][j]+":flow-"+deviceFlows[0][j]+":rule-match-2",flowRule);
                        Resource resFlowRule3 = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service:flow-table-"+deviceFlows[1][j]+":flow-"+deviceFlows[0][j]+":rule-match-3",flowRule);
                        Resource resFlowRule4 = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service:flow-table-"+deviceFlows[1][j]+":flow-"+deviceFlows[0][j]+":rule-match-4",flowRule);
                        Resource resFlowAction = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":openflow-service:flow-table-"+deviceFlows[1][j]+":flow-"+deviceFlows[0][j]+":rule-action-0",flowRule);
                        model.add(model.createStatement(resFlow,flowMatch,resFlowRule0));
                        model.add(model.createStatement(resFlowRule0,type,"IN_PORT"));
                        model.add(model.createStatement(resFlowRule0,value,deviceFlows[4][j]));
                        
                        model.add(model.createStatement(resFlow,flowMatch,resFlowRule1));
                        model.add(model.createStatement(resFlowRule1,type,"ETH_SRC_MAC"));
                        model.add(model.createStatement(resFlowRule1,value,deviceFlows[6][j]));
                        
                        model.add(model.createStatement(resFlow,flowMatch,resFlowRule2));
                        model.add(model.createStatement(resFlowRule2,type,"ETH_DST_MAC"));
                        model.add(model.createStatement(resFlowRule2,value,deviceFlows[5][j]));
                        
                        model.add(model.createStatement(resFlow,flowMatch,resFlowRule3));
                        model.add(model.createStatement(resFlowRule3,type,"ETH_SRC_VLAN"));
                        model.add(model.createStatement(resFlowRule3,value,deviceFlows[7][j]));
                        
                        model.add(model.createStatement(resFlow,flowMatch,resFlowRule4));
                        model.add(model.createStatement(resFlowRule4,type,"ETH_DST_VLAN"));
                        model.add(model.createStatement(resFlowRule4,value,deviceFlows[8][j]));
                        
                        model.add(model.createStatement(resFlow,flowAction,resFlowAction));
                        model.add(model.createStatement(resFlowAction,type,"OUT_PORT"));
                        model.add(model.createStatement(resFlowAction,value,deviceFlows[3][j]));
                        
                }
            }
            
        }
        
        for(int i=0;i<qtyLinks;i++){
            Resource resSrcPort=RdfOwl.createResource(model,topologyURI+":"+links[1][i]+":port-"+links[2][i],biPort);
            Resource resDstPort=RdfOwl.createResource(model,topologyURI+":"+links[4][i]+":port-"+links[5][i],biPort);
            model.add(model.createStatement(resSrcPort,Nml.isAlias,resDstPort));
        }
        
        
        //manully read from a SRRG json file 
        try{
            FileReader reader = new FileReader(SRRG_file);
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
            
            JSONArray f = (JSONArray) jsonObject.get("SRRG");
            int srrg_num = f.size();
            
            for(int i=0; i<srrg_num; i++){
                JSONObject t = (JSONObject) f.get(i);
                String id = t.get("id").toString();
                Resource resSRRG = RdfOwl.createResource(model, topologyURI+":"+id, SRRG);
                
                String severity_str = t.get("severity").toString();
                String occurenceProbability_str = t.get("occurenceProbability").toString();
                
                String devices_str = t.get("devices").toString();
                if(devices_str.equals("")){
                    Resource resNode = RdfOwl.createResource(model, "", node);
                    model.add(model.createStatement(resSRRG, hasNode, resNode));
                }
                else{
                    List<String> devices_list = Arrays.asList(devices_str.split(","));
                    int devices_list_size = devices_list.size();
                    
                    for(int j=0; j<devices_list_size; j++){
                        Resource resNode = RdfOwl.createResource(model,topologyURI+":"+devices_list.get(j).toString() , node);
                        model.add(model.createStatement(resSRRG, hasNode, resNode));
                    }
                }
                
                String ports_str = t.get("bidirectionalPorts").toString();
                if(ports_str.equals("")){
                    Resource resPort = RdfOwl.createResource(model, "", biPort);
                    model.add(model.createStatement(resSRRG, hasBidirectionalPort, resPort));
                }
                else{
                    List<String> ports_list = Arrays.asList(ports_str.split(","));
                    int ports_list_size = ports_list.size();
                    
                    for(int j=0; j<ports_list_size; j++){
                        Resource resPort = RdfOwl.createResource(model,topologyURI+":"+ports_list.get(j).toString() , biPort);
                        model.add(model.createStatement(resSRRG, hasBidirectionalPort, resPort));
                    }
                }
                
                model.add(model.createStatement(resSRRG, severity, severity_str));
                model.add(model.createStatement(resSRRG, occurenceProbability, occurenceProbability_str));
                
                
            }
            
        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
        }catch (IOException ex){
            ex.printStackTrace();
        }catch (NullPointerException ex){
            ex.printStackTrace();
        }
        
        
        //manually insert a SRRG object for testing in mininet
        /*
        Resource resSRRG = RdfOwl.createResource(model, topologyURI+":SRRG_sampleID", SRRG);
        for (int i=0; i<qtyDevices; i++){
            if(device[0][i].equals("of:0000000000000002")){
                Resource resNode = RdfOwl.createResource(model, topologyURI+":"+device[0][i],node);
                model.add(model.createStatement(resSRRG, hasNode, resNode));
                
                String devicePorts[][]=onos.getOnosDevicePorts(subsystemBaseUrl,device[0][i]);
                int qtyPorts=onos.qtyPorts;
                for(int j=0; j<qtyPorts; j++){
                    if(devicePorts[1][j].equals("true")){
                        Resource resPort = RdfOwl.createResource(model,topologyURI+":"+device[0][i]+":port-"+devicePorts[4][j],biPort);
                        model.add(model.createStatement(resSRRG, hasBidirectionalPort, resPort));
                    }
                }
            }
        }
        model.add(model.createStatement(resSRRG, severity, "5"));
        model.add(model.createStatement(resSRRG, occurenceProbability, "0.5"));
        */
        
        
        //String links[][] = onos.getOnosLinks(subsystemBaseUrl);
        //int qtyLinks=onos.qtyLinks;
            
        /*for(int i=0;i<qtyLinks;i++){
            //Resource srcLink = RdfOwl.createResource(model,topologyURI+":"+links[1][i]+"-"+device[0][i],node);
            //String links[][] = onos.getOnosLinks(subsystemBaseUrl);
            //int qtyLinks=onos.qtyLinks;
            for(int k=0;k<qtyPorts;k++){
                if(device[0][i].equals(links[1][k])){
                    for(int l=0;l<qtyDevices;l++){
                        if(links[3][k].equals(device[0][l])){
                            Resource dstNode = RdfOwl.createResource(model,topologyURI+":"+device[0][l],node);
                            model.add(model.createStatement(srcNode,Nml.isAlias,dstNode));
                        }
                    }
                }
            }
        }*/
        /*String hosts[][]=onos.getOnosHosts(subsystemBaseUrl);
            //int qtyHosts=onos.qtyHosts;
            for(int i=0;i<qtyHosts;i++){
                Resource resNode = RdfOwl.createResource(model,topologyURI+":"+hosts[0][i],node);
                //model.add(model.createStatement(onosTopology,hasNode,resNode));
                //model.add(model.createStatement(resNode,hasBidirectionalPort,biPort));
                for(int j=0;j<qtyDevices;j++){
                    if(hosts[4][i].equals(device[0][j])){
                        Resource resDev = RdfOwl.createResource(model,topologyURI+":"+device[1][j]+"-"+device[0][j],node);
                        model.add(model.createStatement(resDev,Nml.isAlias,resNode));
                    }
                }
                //System.out.println(resPort.toString());
            }*/
        
        
        
        return model;
    }
}
