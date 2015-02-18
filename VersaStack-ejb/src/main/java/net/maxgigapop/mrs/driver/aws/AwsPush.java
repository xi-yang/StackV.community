/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.AmazonWebServiceRequest;
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
import java.util.Collection;
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
    private AmazonEC2Client client=null;
    private  AwsEC2Get ec2Client =null; 
    private String topologyUri=null;
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
        "        a               mrs:SwitchingSubnet , owl:NamedIndividual ;\n" +
        "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> \n" +
        "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n" +
        "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:ebsservice-us-east-1>\n" +
        "        a                   mrs:BlockStorageService , owl:NamedIndividual ;\n" +
        "        mrs:providesVolume  <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf>\n" +
        "        a          mrs:Volume , owl:NamedIndividual ;\n" +
        "        mrs:value  \"gp2\" ;\n" +
        "        mrs:disk_gb \"8\" .\n" +
        "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230>\n" +
        "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n" +
        "        mrs:value  \"10.0.0.230\" .\n";
        
        AwsPush push =new AwsPush("","",Regions.US_EAST_1,"urn:ogf:network:aws.amazon.com:aws-cloud");
        String request=push.pushPropagate(modelAdditionStr,"");
        push.pushCommit(request);
    }
  
    
    public  AwsPush(String access_key_id, String secret_access_key,Regions region, String topologyUri) 
    {
        //have all the information regarding the topology
         ec2Client=new AwsEC2Get(access_key_id,secret_access_key,region);
         client= ec2Client.getClient();
        
        //do an adjustment to the topologyUri
        this.topologyUri= topologyUri + ":";
    }
    
//function to push into the cloud
public String  pushPropagate(String modelAddTtl, String modelReductTtl) throws Exception
{
    String  requests="";
    
    OntModel modelReduct = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    OntModel modelAdd = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

     try {
        modelAdd.read(new ByteArrayInputStream(modelAddTtl.getBytes()), null, "TURTLE");
        //modelReduct.read(new ByteArrayInputStream(modelAddTtl.getBytes()), null, "TURTLE");
    } catch (Exception e) {
        throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
    }

    //create all the vpcs that need to be created
     String query= "SELECT ?vpc WHERE {?service mrs:providesVPC  ?vpc}";
     ResultSet r= executeQuery(query,modelAdd);         
    while(r.hasNext())
    {
        QuerySolution querySolution = r.next();
        RDFNode vpc = querySolution.get("vpc");
        String vpcIdTagValue= vpc.asResource().toString().replace(topologyUri,"");
        String vpcId=getResourceId(vpcIdTagValue);

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
        String subnetIdTagValue= subnet.asResource().toString().replace(topologyUri,""); 
        String subnetId=getResourceId(subnetIdTagValue);

        Subnet s = AwsEC2Get.getSubnet(ec2Client.getSubnets(), subnetId);

        if(s == null) //subnet does not exist, need to create subnet
        {
            System.out.println("needs to create a subnet with subnet id:" + subnetId);
        }
    }

    //create a volume if a volume needs to be created
    query= "SELECT ?volume WHERE {?volume a mrs:Volume}";
    r= executeQuery(query,modelAdd);
    while(r.hasNext())
    {
        QuerySolution querySolution = r.next();
        RDFNode volume = querySolution.get("volume");
        String volumeIdTagValue= volume.asResource().toString().replace(topologyUri,""); 
        String volumeId= getResourceId(volumeIdTagValue);

        Volume v = AwsEC2Get.getVolume(ec2Client.getVolumes(), volumeId);

        if(v == null) //volume does not exist, need to create a volume
        {
            
            //check what service is providing the volume
            query= "SELECT ?type WHERE {?service mrs:providesVolume <"+ volume.asResource() + ">}";
            ResultSet r1 = executeQuery(query,modelAdd);
            if(!r1.hasNext())
            throw  new Exception(String.format("model addition does not specify service that provides volume: %s",volume));
            
            //find out the type of the volume
             query= "SELECT ?type WHERE {<" + volume.asResource() +"> mrs:value ?type}";
             r1 = executeQuery(query,modelAdd);
            if(!r1.hasNext())
            throw  new Exception(String.format("model addition does not specify new type of volume: %s",volume));
             QuerySolution querySolution1 = r1.next();
             RDFNode type = querySolution1.get("type");
             
             //find out the size of the volume
             query= "SELECT ?size WHERE {<" + volume.asResource() +"> mrs:disk_gb ?size}";
             r1 = executeQuery(query,modelAdd);
            if(!r1.hasNext())
            throw  new Exception(String.format("model addition does not specify new size of volume: %s",volume));
             querySolution1 = r1.next();
             RDFNode size = querySolution1.get("size");
             
             //put it in a request 
             requests +=String.format("CreateVolumeRequest %s %s %s %s  \n",type.asLiteral().getString(),
                     size.asLiteral().getString(),Regions.US_EAST_1.getName()+"a ", volumeIdTagValue);
        }
    }

    //create network interface if it needs to be created
    query= "SELECT ?port WHERE {?port a  nml:BidirectionalPort }";
    r= executeQuery(query,modelAdd);
    while(r.hasNext())
    {
        QuerySolution querySolution = r.next();
        RDFNode port = querySolution.get("port");
        String portIdTagValue= port.asResource().toString().replace(topologyUri,""); 
        String portId= getResourceId(portIdTagValue);

        NetworkInterface p = AwsEC2Get.getNetworkInterface(ec2Client.getNetworkInterfaces(), portId);

        if(p == null) //network interface does not exist, need to create a network interface
        {   
            //to get the private ip of the network interface
            query= "SELECT ?address WHERE {<" + port.asResource() +">  mrs:privateIpAddress  ?address}";
             ResultSet r1 = executeQuery(query,modelAdd);
            if(!r1.hasNext())
            throw  new Exception(String.format("model addition does not specify privat ip address of port: %s",port));
             QuerySolution querySolution1 = r1.next();
             RDFNode address = querySolution1.get("address");
             String privateAddress = address.asResource().toString().replace(topologyUri,"");

            //find the subnet that has the port previously found
            query= "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + "> ."
                    + "?subnet a mrs:SwitchingSubnet}";
            r1= executeQuery(query,modelAdd);         
            if(!r1.hasNext())
                throw  new Exception(String.format("model addition does not network subnet of port: %s",port));
            querySolution1 = r1.next();
            RDFNode subnet = querySolution1.get("subnet");
            String subnetId= subnet.asResource().toString().replace(topologyUri,"");
            subnetId=getResourceId(subnetId);

            //create the network interface 
            requests += String.format("CreateNetworkInterfaceRequest  %s %s %s \n" ,privateAddress , subnetId
                         ,portIdTagValue);
 
            //to get the public ip address of the network interface if any
            query= "SELECT ?address WHERE {<" + port.asResource() +">  mrs:publicIpAddress  ?address}";
            r1 = executeQuery(query,modelAdd);
            if(r1.hasNext())
            {
                querySolution1 = r1.next();
                address = querySolution1.get("address");
                String publicAddress = address.asResource().toString().replace(topologyUri,"");
                requests  += "AssociateAddressRequest " + publicAddress +" " + portIdTagValue
                        + " \n";
            }       
        }
    }


    //create all the nodes that need to be created 
    query= "SELECT ?node WHERE {?node a nml:Node}";
    r = executeQuery(query,modelAdd);
    while(r.hasNext())
    {
        QuerySolution querySolution = r.next();
        RDFNode node = querySolution.get("node");
        String nodeIdTagValue= node.asResource().toString().replace(topologyUri,"");
        String nodeId= getResourceId(nodeIdTagValue);
        
        Instance instance = ec2Client.getInstance(nodeId);
        if(instance==null) //instance needs to be created
        {
            System.out.println("needs to create node with id "+ nodeIdTagValue);
                    
            //check what service is providing the instance
            query= "SELECT ?type WHERE {?service mrs:providesVM <"+ node.asResource() + ">}";
            ResultSet r1 = executeQuery(query,modelAdd);
            if(!r1.hasNext())
            throw  new Exception(String.format("model addition does not specify service that provides Instance: %s",node));
            
            //find the Vpc that the node will be in
            query= "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() +">}";
            r1= executeQuery(query,modelAdd);         
            if(!r1.hasNext())
                throw  new Exception(String.format("model addition does not specify the Vpc of the node: %s",node));
            QuerySolution querySolution1 = r1.next();
            RDFNode vpc = querySolution1.get("vpc");
            String vpcId= vpc.asResource().toString().replace(topologyUri,"");
            System.out.println("node has Vpc :" + vpcId);
            vpcId=getResourceId(vpcId);

            //to find the subnet the node is in first  find the port the node uses
            query= "SELECT ?port WHERE {<" + node.asResource()+ "> nml:hasBidirectionalPort ?port}";
            ResultSet r2= executeQuery(query,modelAdd);         
            if(!r2.hasNext())
                throw  new Exception(String.format("model addition does not specify the subnet that the node is: %s",node));
            List<String> portsId =new ArrayList();
            RDFNode lastPort = null;
            while(r2.hasNext())//there could be multiple network interfaces attached to the instance
            {
                QuerySolution querySolution2 = r2.next();
                RDFNode port= querySolution2.get("port");
                String id= port.asResource().toString().replace(topologyUri,"");
                portsId.add(getResourceId(id));
                System.out.println(" node has port: " + id);
                lastPort=port;
            }


            //find the EBS volumes that the instance uses
            query = "SELECT ?volume WHERE {<" + node.asResource() + ">  mrs:hasVolume  ?volume}";
            ResultSet r4= executeQuery(query,modelAdd);   
            if(!r4.hasNext())
               throw  new Exception(String.format("model addition does not specify the volume of the new node: %s",node));
            List<String> volumesId =new ArrayList();
             while(r4.hasNext())//there could be multiple network interfaces attached to the instance
            {
                QuerySolution querySolution4 = r4.next();
                RDFNode volume= querySolution4.get("volume");
                String id= volume.asResource().toString().replace(topologyUri,"");
                System.out.println("node has volume :" + id);
                volumesId.add(getResourceId(id));
            }

            //put request for new instance
            requests += String.format("RunInstancesRequest ami-146e2a7c t2.micro 1 1 ");
            int index =0;
            for(String id : portsId)
            {
               requests+=String.format("InstanceNetworkInterfaceSpecification %s %d",id,index);
               index++; //increment the device index
            } 
            requests+="\n";
        }
    }

    return requests;
}

public void pushCommit(String r)
{
    String []requests=r.split("[\\n]");
    
        for (String request : requests) 
        {          
            if(request.contains("CreateVolumeRequest"))
            {                
                String []parameters = request.split("%s");
                
                CreateVolumeRequest volumeRequest= new CreateVolumeRequest();
                volumeRequest.withVolumeType(parameters[1]);
                volumeRequest.withSize(Integer.parseInt(parameters[2]));
                volumeRequest.withAvailabilityZone(parameters[3]);                
                CreateVolumeResult result = client.createVolume(volumeRequest);
                
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id",parameters[4]));
                tagRequest.withResources(result.getVolume().getVolumeId());
                client.createTags(tagRequest);
            }
            else if(request.contains("CreateNetworkInterfaceRequest"))
            {
                 
            }
            else if(request.contains("RunInstancesRequest"))
            {
                
            }
        }
}
    
 //function that execustes a query and returns the result
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

    
    //function that checks if the id provided is a tag; if it is, it will return
    //the resource id, otherwise it will return the input parameter which is the 
    //id of the resource
    private  String getResourceId(String id)
    {
        Filter filter= new Filter();
        filter.withName("value")
              .withValues(id);
        
        DescribeTagsRequest tagRequest= new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions= ec2Client.getClient().describeTags(tagRequest).getTags();
        if(!descriptions.isEmpty())
            return descriptions.get(0).getResourceId();

        return id;
    }
}
