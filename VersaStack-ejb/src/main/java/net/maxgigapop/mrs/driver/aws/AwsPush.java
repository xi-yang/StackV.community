/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.*;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.common.ModelUtil;

/**
 *
 * @author max
 */
public class AwsPush 
{
    public AmazonEC2Client client=null;
    static final Logger logger = Logger.getLogger(AwsPush.class.getName());
    
    public static void main(String [ ] args) throws Exception
    {
       String modelAdditionStr = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
        "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
        "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
        "@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n" +
        "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n" +
        "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60>\n" +
        "        a                         nml:Node , owl:NamedIndividual ;\n" +
        "        mrs:hasVolume             <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> ;\n" +
        "        mrs:providedByService     <urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1> ;\n" +
        "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e9>\n" +
        "        a               nml:Topology , owl:NamedIndividual ;\n" +
        "        nml:hasNode     <urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60> ;\n" +
        "        nml:hasService  <urn:ogf:network:aws.amazon.com:aws-cloud:rtb-864b05e3> , <urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e910.0.0.0/16> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1>\n" +
        "        a               mrs:HypervisorService , owl:NamedIndividual ;\n" +
        "        mrs:providesVM  <urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:subnet-2cd6ad16>\n" +
        "        a                         mrs:SwitchingSubnet , owl:NamedIndividual ;\n" +
        "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> \n" +
        "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n" +
        "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:ebsservice-us-east-1>\n" +
        "        a                   mrs:BlockStorageService , owl:NamedIndividual ;\n" +
        "        mrs:providesVolume  <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf>\n" +
        "        a          mrs:Volume , owl:NamedIndividual ;\n" +
        "        mrs:value  \"gp2\" .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230>\n" +
        "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n" +
        "        mrs:value  \"10.0.0.230\" .\n";
        
        AwsPush push =new AwsPush("","",Regions.US_EAST_1,modelAdditionStr,"","urn:ogf:network:aws.amazon.com:aws-cloud");
    }
  
    
    public  AwsPush(String access_key_id, String secret_access_key,Regions region , String modelAddTtl, String modelReductTtl,String topologyUri) throws Exception 
    {
        String query="";
        
        //have all the information regarding the topology
         AwsEC2Get ec2Client=new AwsEC2Get(access_key_id,secret_access_key,region);
         client= ec2Client.getClient();
        
        //do an adjustment to the topologyUri
        topologyUri= topologyUri + ":";
        
        //AwsAuthenticateService authenticate=new AwsAuthenticateService(access_key_id,secret_access_key);
        //this.client = authenticate.AwsAuthenticateEC2Service(Region.getRegion(region));
        
        OntModel modelReduct = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        OntModel modelAdd = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        
         try {
            modelAdd.read(new ByteArrayInputStream(modelAddTtl.getBytes()), null, "TURTLE");
            modelReduct.read(new ByteArrayInputStream(modelAddTtl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
        }
        
        //create all the vpcs that need to be created
         query= "SELECT ?vpc WHERE {?service mrs:providesVPC  ?vpc}";
         ResultSet r= executeQuery(query,modelAdd);         
        while(r.hasNext())
        {
            QuerySolution querySolution = r.next();
            RDFNode vpc = querySolution.get("vpc");
            String vpcId= vpc.asResource().toString().replace(topologyUri,"");

            Vpc v = AwsEC2Get.getVpc(ec2Client.getVpcs(), vpcId);

            if(v == null) // vpc does not exist, has to be created
            {
                System.out.println("needs to create vpc with id: " + vpcId);
            }

        }
         
        //create all the subnets that need to be created
        query= "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        r= executeQuery(query,modelAdd);
        while(r.hasNext())
        {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetId= subnet.asResource().toString().replace(topologyUri,""); 
            
            Subnet s = AwsEC2Get.getSubnet(ec2Client.getSubnets(), subnetId);
            
            if(s == null) //subnet does not exist, need to create subnet
            {
                System.out.println("needs to create a subnet");
            }
        }
        
        //create a volume if a volume needs to be created
        query= "SELECT ?volume WHERE {?service mrs:providesVolume ?volume}";
        r= executeQuery(query,modelAdd);
        while(r.hasNext())
        {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeId= volume.asResource().toString().replace(topologyUri,""); 
            
            Volume v = AwsEC2Get.getVolume(ec2Client.getVolumes(), volumeId);
            
            if(v == null) //volume does not exist, need to create a volume
            {
                CreateVolumeRequest volumeRequest =  new CreateVolumeRequest();
                
                //find out the type of the volume
                 query= "SELECT ?type WHERE {<" + volume.asResource() +"> mrs:value ?type}";
                 ResultSet r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                throw  new Exception(String.format("model addition does not specify new volume type"));
                 QuerySolution querySolution1 = r1.next();
                 RDFNode type = querySolution1.get("type");
                 
                 System.out.println("create a volume with type: " + type.asLiteral().getString());
                 volumeRequest.withVolumeType(type.asLiteral().getString());
            }
        }
        
        //create network interface if it needs to be created
        query= "SELECT ?port WHERE {?port a  nml:BidirectionalPort }";
        r= executeQuery(query,modelAdd);
        while(r.hasNext())
        {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portId= port.asResource().toString().replace(topologyUri,""); 
            
            NetworkInterface p = AwsEC2Get.getNetworkInterface(ec2Client.getNetworkInterfaces(), portId);
            
            if(p == null) //network interface does not exist, need to create a network interface
            {
                //to get the private ip of the network interface
                query= "SELECT ?address WHERE {<" + port.asResource() +">  mrs:privateIpAddress  ?address}";
                ResultSet r1 = executeQuery(query,modelAdd);
                if(!r1.hasNext())
                throw  new Exception(String.format("model addition does not specify privat ip address of network interface"));
                 QuerySolution querySolution1 = r1.next();
                 RDFNode address = querySolution1.get("address");
                 String privateAddress = address.asResource().toString().replace(topologyUri,"");
                 System.out.println("Node has private address :" + privateAddress);
                 
                 //to get the public ip address of the network interface if any
                query= "SELECT ?address WHERE {<" + port.asResource() +">  mrs:publicIpAddress  ?address}";
                r1 = executeQuery(query,modelAdd);
                if(r1.hasNext())
                querySolution1 = r1.next();
                address = querySolution1.get("address");
                String publicAddress = address.asResource().toString().replace(topologyUri,"");
                System.out.println("node has public address: "+ publicAddress);
                
                /*
                CreateNetworkInterfaceRequest networkInterfaceRequest= new CreateNetworkInterfaceRequest();
                networkInterfaceRequest.withPrivateIpAddress(privateAddress);
                CreateNetworkInterfaceResult networkInterfaceResult=ec2Client.getClient().createNetworkInterface(networkInterfaceRequest);
                networkInterfaceResult.getNetworkInterface().setNetworkInterfaceId(portId);
                
                NetworkInterfaceAssociation networkInterfaceAssociation= new NetworkInterfaceAssociation();
                networkInterfaceAssociation.setPublicIp(publicAddress);
                networkInterfaceResult.getNetworkInterface().setAssociation(networkInterfaceAssociation);
                        */
            }
        }
         
         
        //create all the nodes that need to be created 
        query= "SELECT ?node WHERE {?node a nml:Node}";
        r = executeQuery(query,modelAdd);
        while(r.hasNext())
        {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");
            String nodeId= node.asResource().toString().replace(topologyUri,"");
            System.out.println("create node with id :"+ nodeId);
            
            //find the Vpc that the node will be in
            query= "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() +">}";
            ResultSet r1= executeQuery(query,modelAdd);         
            if(!r1.hasNext())
                throw  new Exception(String.format("model addition does not specify new node's vpc"));
            QuerySolution querySolution1 = r1.next();
            RDFNode vpc = querySolution1.get("vpc");
            String vpcId= vpc.asResource().toString().replace(topologyUri,"");
            System.out.println("node has Vpc :" + vpcId);
            
            //to find the subnet the node is in first  find the port the node uses
            query= "SELECT ?port WHERE {<" + node.asResource()+ "> nml:hasBidirectionalPort ?port}";
            ResultSet r2= executeQuery(query,modelAdd);         
            if(!r2.hasNext())
                throw  new Exception(String.format("model addition does not specify new node's port"));
            List<String> portsId =new ArrayList();
            RDFNode lastPort = null;
            while(r2.hasNext())//there could be multiple network interfaces attached to the instance
            {
                QuerySolution querySolution2 = r2.next();
                RDFNode port= querySolution2.get("port");
                portsId.add(port.asResource().toString().replace(topologyUri,""));
                String id= port.asResource().toString().replace(topologyUri,"");
                System.out.println(" node has port: " + id);
                lastPort=port;
            }
            
            //find the subnet that has the port previously found
            query= "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + lastPort.asResource() + ">}";
            ResultSet r3= executeQuery(query,modelAdd);         
            if(!r3.hasNext())
                throw  new Exception(String.format("model addition does not specify new node's subnet"));
            QuerySolution querySolution3 = r3.next();
            RDFNode subnet = querySolution3.get("subnet");
            String subnetId= subnet.asResource().toString().replace(topologyUri,"");
            System.out.println("node is in subnet : " + subnetId);
            
            //find the EBS volumes that the instance uses
            query = "SELECT ?volume WHERE {<" + node.asResource() + ">  nml:hasVolume  ?volume}";
            ResultSet r4= executeQuery(query,modelAdd);   
            if(!r4.hasNext())
                throw  new Exception(String.format("model addition does not specify new node's volumes"));
            List<String> volumesId =new ArrayList();
             while(r2.hasNext())//there could be multiple network interfaces attached to the instance
            {
                QuerySolution querySolution4 = r4.next();
                RDFNode volume= querySolution4.get("volume");
                volumesId.add( volume.asResource().toString().replace(topologyUri,""));
                String id= volume.asResource().toString().replace(topologyUri,"");
                System.out.println("node has volume :" + id);
            }
            
            
            //start creating instance
            RunInstancesRequest runInstances= new RunInstancesRequest();
            runInstances.withImageId("ami-146e2a7c")
                .withInstanceType("t2.micro")
                .withSubnetId(subnetId)
                .withMaxCount(1)
                .withMinCount(1);
                
            
            //attach the network interfaces to the instance         
            for(String id : portsId)
            {
               InstanceNetworkInterfaceSpecification networkInterfaceSpecification =
                       new InstanceNetworkInterfaceSpecification();
               networkInterfaceSpecification.setNetworkInterfaceId(id);
               runInstances.withNetworkInterfaces(networkInterfaceSpecification);
            } 
            
            //create instance
            RunInstancesResult  instanceResult=ec2Client.getClient().runInstances(runInstances);
            
            //set Id of the instance
            Instance i = instanceResult.getReservation().getInstances().get(0);
            i.setInstanceId(nodeId);
            
            //attach volumes to the instance
            for(String id : volumesId)
            {
                AttachVolumeRequest volumeRequest= new AttachVolumeRequest();
                volumeRequest.withVolumeId(id);
                volumeRequest.withInstanceId(nodeId);
                ec2Client.getClient().attachVolume(volumeRequest);
            }
            
            
        }
            
            
            
    }
    
  
private ResultSet executeQuery(String queryString, Model model)
{
  queryString=  "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                queryString;
  
   //get all the nodes that will be added
   Query query = QueryFactory.create(queryString);
   QueryExecution qexec = QueryExecutionFactory.create(query, model);
   return (ResultSet) qexec.execSelect();
}
    
    //function to delete one  EC2 instance, returns true if deleted succesfully
    public boolean deleteInstances(String id)
    {
         TerminateInstancesRequest terminateRequest= new TerminateInstancesRequest();
         terminateRequest.withInstanceIds(id);
         try
         {
            this.client.terminateInstances(terminateRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred during termination {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to delete one or more EC2 instances, returns true if deleted succesfully
    public boolean deleteInstances(List<String> id)
    {
         TerminateInstancesRequest terminateRequest= new TerminateInstancesRequest();
         terminateRequest.withInstanceIds(id);
         try
         {
            this.client.terminateInstances(terminateRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred during termination {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to stop one  EC2 Instance, returns true if stopped succesfuly
    public boolean stopInstances(List<String> id)
    {
         StopInstancesRequest stopRequest= new StopInstancesRequest();
         stopRequest.withInstanceIds(id);
         try
         {
            this.client.stopInstances(stopRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred while stopping instances: {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to stop one  EC2 Instance, returns true if stopped succesfuly
    public boolean stopInstances(String id)
    {
         StopInstancesRequest stopRequest= new StopInstancesRequest();
         stopRequest.withInstanceIds(id);
         try
         {
            this.client.stopInstances(stopRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred while stopping instances: {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to start one or more  EC2 Instances, returns true if stopped succesfuly
    public boolean startInstances(List<String> id)
    {
         StartInstancesRequest startRequest= new StartInstancesRequest();
         startRequest.withInstanceIds(id);
         try
         {
            this.client.startInstances(startRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred while starting instances: {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
    //function to start one  EC2 Instance, returns true if stopped succesfuly
    public boolean startInstances(String id)
    {
         StartInstancesRequest startRequest= new StartInstancesRequest();
         startRequest.withInstanceIds(id);
         try
         {
            this.client.startInstances(startRequest);
         }
         catch(Exception e)
         { 
             logger.log(Level.INFO, "Error ocurred while starting instances: {0}", e.getMessage());
             return false;
         }
        return true;
    }
    
}
