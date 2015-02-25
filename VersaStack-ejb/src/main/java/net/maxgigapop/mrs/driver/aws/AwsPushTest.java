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
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import static java.lang.Thread.sleep;
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
import net.maxgigapop.mrs.common.Mrs;

/**
 *
 * @author max
 */

// TODO attach network interfaces and volumes to existing instances,tag root device
// change the address type in network interfaces , recognize network interface by 
// bidirectional port and the lable that says NetworkInterface
//availability zone problems in volumes 
public class AwsPushTest {

    private AmazonEC2Client client = null;
    private AwsEC2Get ec2Client = null;
    private String topologyUri = null;
    private Regions region =null;
    static final Logger logger = Logger.getLogger(AwsPush.class.getName());

    public static void main(String[] args) throws Exception {
        String modelAdditionStr = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60>\n"
                + "        a                         nml:Node , owl:NamedIndividual ;\n"
                + "        mrs:hasVolume             <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> , <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be> ;\n"
                + "        mrs:providedByService     <urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1> ;\n"
                + "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> , <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e9>\n"
                + "        a               nml:Topology , owl:NamedIndividual ;\n"
                + "        nml:hasNode     <urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60> ;\n"
                + "        nml:hasService  <urn:ogf:network:aws.amazon.com:aws-cloud:rtb-864b05e3> , <urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e910.0.0.0/16> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1>\n"
                + "        a               mrs:HypervisorService , owl:NamedIndividual ;\n"
                + "        mrs:providesVM  <urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:subnet-2cd6ad16>\n"
                + "        a               mrs:SwitchingSubnet , owl:NamedIndividual ;\n"
                + "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> , <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> \n"
                + "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                + "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> \n"
                + "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                + "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.231> ;\n"
                + "        mrs:publicIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:54.152.72.205> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:ebsservice-us-east-1>\n"
                + "        a                   mrs:BlockStorageService , owl:NamedIndividual ;\n"
                + "        mrs:providesVolume  <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> , <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf>\n"
                + "        a          mrs:Volume , owl:NamedIndividual ;\n"
                + "        mrs:value  \"gp2\" ;\n"
                + "        mrs:target_device \"/dev/xvdba\" ;\n"
                + "        mrs:disk_gb \"8\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be>\n"
                + "        a          mrs:Volume , owl:NamedIndividual ;\n"
                + "        mrs:value  \"standard\" ;\n"
                + "        mrs:target_device \"/dev/xvda\" ;\n"
                + "        mrs:disk_gb \"8\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:value  \"10.0.0.230\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.231>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:value  \"10.0.0.231\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:54.152.72.205>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:value  \"54.152.72.205\" .\n";

        String modelReductionStr = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60>\n"
                + "        a                         nml:Node , owl:NamedIndividual ;\n"
                + "        mrs:hasVolume             <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> ;\n"
                + "        mrs:providedByService     <urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1> ;\n"
                + "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> , <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> .";

        AwsPushTest push = new AwsPushTest("", "", Regions.US_EAST_1, "urn:ogf:network:aws.amazon.com:aws-cloud");
        String request = push.pushPropagate(modelAdditionStr, "");
        System.out.println(request);
        push.pushCommit(request);
        
        
        
       /* OntModel model= AwsModelBuilder.createOntology("","",Regions.US_EAST_1, "urn:ogf:network:aws.amazon.com:aws-cloud");
         StringWriter out = new StringWriter();
         try {
         model.write(out, "TURTLE");
         } catch (Exception e) {
         throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
         }
         String ttl = out.toString();
         System.out.println(ttl);*/
        
    }

    public AwsPushTest(String access_key_id, String secret_access_key, Regions region, String topologyUri) {
        //have all the information regarding the topology
        ec2Client = new AwsEC2Get(access_key_id, secret_access_key, region);
        client = ec2Client.getClient();
        this.region =region;

        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
    }


    /*
     function to propagate all the requests 
     */
    public String pushPropagate(String modelAddTtl, String modelReductTtl) throws Exception {
        String requests = "";

        OntModel modelReduct = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        OntModel modelAdd = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        try {
            modelAdd.read(new ByteArrayInputStream(modelAddTtl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall model addition, due to %s", e.getMessage()));
        }

        /*try {
         modelReduct.read(new ByteArrayInputStream(modelReductTtl.getBytes()), null, "TURTLE");
         } catch (Exception e) {
         throw new Exception(String.format("failure to unmarshall model reduction, due to %s", e.getMessage()));
         }*/
        //delete all the instances that need to be created
        requests += deleteInstancesRequests(modelReduct);

        //create all the vpcs that need to be created
        requests += createVpcsRequests(modelAdd);

        //create all the subnets that need to be created
        requests += createSubnetsRequests(modelAdd);
        
        //create a volume if a volume needs to be created
        requests += createVolumesRequests(modelAdd);
        
        //create network interface if it needs to be created
        requests += createPortsRequests(modelAdd);
        
        //create all the nodes that need to be created 
        requests += createInstancesRequests(modelAdd);
        
        //attach volumes that need to be atatched to existing instances
        requests+= attachVolumeRequests(modelAdd);

        return requests;
    }

    /*
     Function to do execute all the requests provided by the propagate method
     */
    public void pushCommit(String r) {
        String[] requests = r.split("[\\n]");

        for (String request : requests) {
            if (request.contains("TerminateInstancesRequest")) {
                String[] parameters = request.split("\\s+");

                TerminateInstancesRequest del = new TerminateInstancesRequest();
                del.withInstanceIds(getInstanceId(parameters[1]));
                del.toString();
                client.terminateInstances(del);
            } else if (request.contains("CreateVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                CreateVolumeRequest volumeRequest = new CreateVolumeRequest();
                volumeRequest.withVolumeType(parameters[1])
                        .withSize(Integer.parseInt(parameters[2]))
                        .withAvailabilityZone(parameters[3]);

                
                CreateVolumeResult result = client.createVolume(volumeRequest);

                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[4]));
                tagRequest.withResources(result.getVolume().getVolumeId());
                client.createTags(tagRequest);
            } else if (request.contains("CreateNetworkInterfaceRequest")) {
                String[] parameters = request.split("\\s+");

                CreateNetworkInterfaceRequest portRequest = new CreateNetworkInterfaceRequest();
                portRequest.withPrivateIpAddress(parameters[1])
                        .withSubnetId(getResourceId(parameters[2]));
                CreateNetworkInterfaceResult portResult = client.createNetworkInterface(portRequest);

                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[3]));
                tagRequest.withResources(portResult.getNetworkInterface().getNetworkInterfaceId());
                client.createTags(tagRequest);

            } else if (request.contains("AssociateAddressRequest")) {
                String[] parameters = request.split("\\s+");

                Address publicIp = ec2Client.getElasticIp(parameters[1]);
                AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest();
                associateAddressRequest.withAllocationId(publicIp.getAllocationId())
                        .withNetworkInterfaceId(getResourceId(parameters[2]));
                client.associateAddress(associateAddressRequest);
            } else if (request.contains("RunInstancesRequest")) {
               //requests += String.format("RunInstancesRequest ami-146e2a7c t2.micro ") 
                //requests+=String.format("InstanceNetworkInterfaceSpecification %s %d",id,index)
                String[] parameters = request.split("\\s+");

                RunInstancesRequest runInstance = new RunInstancesRequest();
                runInstance.withImageId(parameters[1]);
                runInstance.withInstanceType(parameters[2]);
                runInstance.withMaxCount(1);
                runInstance.withMinCount(1);

                //integrate the root device
                EbsBlockDevice device = new EbsBlockDevice();
                device.withVolumeType(parameters[4]);
                device.withVolumeSize(Integer.parseInt(parameters[5]));
                BlockDeviceMapping mapping = new BlockDeviceMapping();
                mapping.withDeviceName(parameters[6]);
                mapping.withEbs(device);
                String volumeTag = parameters[7];
                runInstance.withBlockDeviceMappings(mapping);

                List<InstanceNetworkInterfaceSpecification> portSpecification = new ArrayList();
                for (int i = 9; i < parameters.length; i++) {
                    InstanceNetworkInterfaceSpecification s = new InstanceNetworkInterfaceSpecification();
                    s.withNetworkInterfaceId(getResourceId(parameters[i]));
                    i++;
                    s.withDeviceIndex(Integer.parseInt(parameters[i]));
                    portSpecification.add(s);
                }
                runInstance.withNetworkInterfaces(portSpecification);
                RunInstancesResult result = client.runInstances(runInstance);
                
                //tag the new instance
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[3]));
                tagRequest.withResources(result.getReservation().getInstances().get(0).getInstanceId());
                client.createTags(tagRequest);
                
                //tag the root volume
                tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", volumeTag));
                //String volumeId = m.getEbs().getVolumeId();
                //tagRequest.withResources(volumeId);
                //client.createTags(tagRequest);
            }
            
             else if (request.contains("AttachVolumeRequest")) 
             {
               String[] parameters = request.split("\\s+");  
               
               AttachVolumeRequest volumeRequest = new AttachVolumeRequest();
               volumeRequest.withInstanceId(getInstanceId(parameters[1]))
                       .withVolumeId(getVolumeId(parameters[2]))
                       .withDevice(parameters[3]);
 
               client.attachVolume(volumeRequest);
             }
        }
    }

//function that execustes a query and returns the result
    private ResultSet executeQuery(String queryString, Model model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        return (ResultSet) qexec.execSelect();
    }


    /*
     function to delete an instance from a model
     */
    private String deleteInstancesRequests(OntModel modelReduct) throws Exception {
        String requests = "";
        String query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");
            String nodeIdTagValue = node.asResource().toString().replace(topologyUri, "");
            String nodeId = getInstanceId(nodeIdTagValue);

            Instance instance = ec2Client.getInstance(nodeId);
            if (instance == null) //instance does not exists
            {
                throw new Exception(String.format("Node to delete: %s does not exist", node));
            } else {
                requests += String.format("TerminateInstancesRequest %s \n", nodeId);
            }
        }
        return requests;
    }

    /*
     Function to create a Vpc from a model
     */
    private String createVpcsRequests(OntModel modelAdd) {
        String requests = "";
        String query;

        query = "SELECT ?vpc WHERE {?service mrs:providesVPC  ?vpc}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode vpc = querySolution.get("vpc");
            String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri, "");
            String vpcId = getResourceId(vpcIdTagValue);

            Vpc v = ec2Client.getVpc(vpcId);

            if (v == null) // vpc does not exist, has to be created
            {
                System.out.println("needs to create vpc with id: " + vpcId);
            }
        }
        return requests;
    }

    /*
     Function to create a subnets from a model
     */
    private String createSubnetsRequests(OntModel modelAdd) {
        String requests = "";
        String query;

        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetIdTagValue = subnet.asResource().toString().replace(topologyUri, "");
            String subnetId = getResourceId(subnetIdTagValue);

            Subnet s = ec2Client.getSubnet(subnetId);

            if (s == null) //subnet does not exist, need to create subnet
            {
                System.out.println("needs to create a subnet with subnet id:" + subnetId);
            }
        }
        return requests;
    }

    /*
     Function to create a volumes from a model
     */
    private String createVolumesRequests(OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeIdTagValue = volume.asResource().toString().replace(topologyUri, "");
            String volumeId = getVolumeId(volumeIdTagValue);

            Volume v = ec2Client.getVolume(volumeId);

            if (v == null) //volume does not exist, need to create a volume
            {
                //check what service is providing the volume
                query = "SELECT ?type WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify service that provides volume: %s", volume));
                }

                //find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new type of volume: %s", volume));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new size of volume: %s", volume));
                }
                querySolution1 = r1.next();
                RDFNode size = querySolution1.get("size");

                //dont create the volumes with no target device and that are attached to an instance
                //since this volumes are the root devices
                query = "SELECT ?deviceName WHERE {<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                r1 = executeQuery(query, modelAdd);
                if (r1.hasNext()) {
                    querySolution1 = r1.next();
                    String deviceName = querySolution1.get("deviceName").asLiteral().toString();

                    if (!deviceName.equals("/dev/sda1") && !deviceName.equals("/dev/xvda")) {
                        requests += String.format("CreateVolumeRequest %s %s %s  %s  \n", type.asLiteral().getString(),
                                size.asLiteral().getString(), region.getName() + "e", volumeIdTagValue);
                    }
                }
                else
                {
                    requests += String.format("CreateVolumeRequest %s %s %s %s  \n", type.asLiteral().getString(),
                                size.asLiteral().getString(), Regions.US_EAST_1.getName() + "e", volumeIdTagValue);
                }
            }
        }

        return requests;
    }

    /*
     Function to create network interfaces from a model
     */
    private String createPortsRequests(OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort }";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portIdTagValue = port.asResource().toString().replace(topologyUri, "");
            String portId = getResourceId(portIdTagValue);

            NetworkInterface p = ec2Client.getNetworkInterface(portId);

            if (p == null) //network interface does not exist, need to create a network interface
            {
                //to get the private ip of the network interface
                query = "SELECT ?address WHERE {<" + port.asResource() + ">  mrs:privateIpAddress  ?address}";
                ResultSet r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify privat ip address of port: %s", port));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");
                String privateAddress = address.asResource().toString().replace(topologyUri, "");

                //find the subnet that has the port previously found
                query = "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + "> ."
                        + "?subnet a mrs:SwitchingSubnet}";
                r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not network subnet of port: %s", port));
                }
                querySolution1 = r1.next();
                RDFNode subnet = querySolution1.get("subnet");
                String subnetId = subnet.asResource().toString().replace(topologyUri, "");
                subnetId = getResourceId(subnetId);

                //create the network interface 
                requests += String.format("CreateNetworkInterfaceRequest  %s %s %s \n", privateAddress, subnetId, portIdTagValue);

                //to get the public ip address of the network interface if any
                query = "SELECT ?address WHERE {<" + port.asResource() + ">  mrs:publicIpAddress  ?address}";
                r1 = executeQuery(query, modelAdd);
                if (r1.hasNext()) {
                    querySolution1 = r1.next();
                    address = querySolution1.get("address");
                    String publicAddress = address.asResource().toString().replace(topologyUri, "");
                    requests += "AssociateAddressRequest " + publicAddress + " " + portIdTagValue
                            + " \n";
                }
            }
        }
        return requests;
    }

    /*
     Function to create Instances
     */
    private String createInstancesRequests(OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");
            String nodeIdTagValue = node.asResource().toString().replace(topologyUri, "");
            String nodeId = getInstanceId(nodeIdTagValue);

            Instance instance = ec2Client.getInstance(nodeId);
            List<Instance> i =ec2Client.getEc2Instances();
            if (instance == null) //instance needs to be created
            {
                //check what service is providing the instance
                query = "SELECT ?type WHERE {?service mrs:providesVM <" + node.asResource() + ">}";
                ResultSet r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify service that provides Instance: %s", node));
                }

                //find the Vpc that the node will be in
                query = "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() + ">}";
                r1 = executeQuery(query, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the Vpc of the node: %s", node));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode vpc = querySolution1.get("vpc");
                String vpcId = vpc.asResource().toString().replace(topologyUri, "");
                vpcId = getResourceId(vpcId);

                //to find the subnet the node is in first  find the port the node uses
                query = "SELECT ?port WHERE {<" + node.asResource() + "> nml:hasBidirectionalPort ?port}";
                ResultSet r2 = executeQuery(query, modelAdd);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the subnet that the node is: %s", node));
                }
                List<String> portsId = new ArrayList();
                RDFNode lastPort = null;
                while (r2.hasNext())//there could be multiple network interfaces attached to the instance
                {
                    QuerySolution querySolution2 = r2.next();
                    RDFNode port = querySolution2.get("port");
                    String id = port.asResource().toString().replace(topologyUri, "");
                    portsId.add(getResourceId(id));
                    lastPort = port;
                }

                //find the EBS volumes that the instance uses
                query = "SELECT ?volume WHERE {<" + node.asResource() + ">  mrs:hasVolume  ?volume}";
                ResultSet r4 = executeQuery(query, modelAdd);
                if (!r4.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the volume of the new node: %s", node));
                }
                List<String> volumesId = new ArrayList();
                while (r4.hasNext())//there could be multiple volumes attached to the instance
                {
                    QuerySolution querySolution4 = r4.next();
                    RDFNode volume = querySolution4.get("volume");
                    String id = volume.asResource().toString().replace(topologyUri, "");
                    volumesId.add(getVolumeId(id));
                }

                //put request for new instance
                requests += String.format("RunInstancesRequest ami-146e2a7c t2.micro %s ", nodeIdTagValue);

                //put the root device of the instance
                query = "SELECT ?volume ?deviceName ?size ?type  WHERE {"
                        + "<" + node.asResource() + ">  mrs:hasVolume  ?volume ."
                        + "?volume mrs:target_device ?deviceName ."
                        + "?volume mrs:disk_gb ?size ."
                        + "?volume mrs:value ?type}";
                r4 = executeQuery(query, modelAdd);
                if (r4.hasNext()) {
                    QuerySolution querySolution4 = r4.next();
                    RDFNode volume = querySolution4.get("volume");
                    String volumeTag = volume.asResource().toString().replace(topologyUri, "");
                    String type = querySolution4.get("type").asLiteral().toString();
                    String size = querySolution4.get("size").asLiteral().toString();
                    String deviceName = querySolution4.get("deviceName").asLiteral().toString();
                    boolean hasRootVolume=false;
                    if (deviceName.equals("/dev/sda1") || deviceName.equals("/dev/xvda"))
                    {
                        hasRootVolume=true;
                        requests += String.format("%s %s %s %s ", type, size, deviceName, volumeTag);
                    }
                    if(hasRootVolume==false)
                        throw new Exception(String.format("model addition does not specify root volume for node: %s",node)); 
                }

                int index = 0;
                //put the networ interfaces 
                requests += "NetworkInterfaceSpecification ";
                for (String id : portsId) {
                    requests += String.format("%s %d ", id, index);
                    index++; //increment the device index
                }
                requests += "\n";
            }
        }
        return requests;
    }
    
    
    /*
        Attach a volume to an existing instance AWS
    */
    private String attachVolumeRequests(OntModel modelAdd)
    {
        String requests="";
        
        String query = "SELECT  ?node ?volume ?deviceName ?size ?type  WHERE {"
          + "?node  mrs:hasVolume  ?volume ."
          + "?volume mrs:target_device ?deviceName ."
          + "?volume mrs:disk_gb ?size ."
          + "?volume mrs:value ?type}";
        
        ResultSet r1= executeQuery(query,modelAdd);
        while(r1.hasNext())
        {
           QuerySolution querySolution1 = r1.next();
           RDFNode node = querySolution1.get("node");
           RDFNode volume = querySolution1.get("volume");
           RDFNode deviceName = querySolution1.get("deviceName");
           
           String nodeTagId= node.asResource().toString().replace(topologyUri,"");
           String volumeTagId= volume.asResource().toString().replace(topologyUri,"");
           String device= deviceName.asLiteral().toString();
           
           if (!device.equals("/dev/sda1") && !device.equals("/dev/xvda"))
           {
                String nodeId= getInstanceId(nodeTagId);
                String volumeId = getVolumeId(volumeTagId);


                Instance ins = ec2Client.getInstance(nodeId);
                Volume vol = ec2Client.getVolume(volumeId);
                if(ins!=null)
                {
                    List< InstanceBlockDeviceMapping> map =ins.getBlockDeviceMappings();
                    boolean attach =true;
                    if(vol==null)
                    {
                     requests+=String.format("AttachVolumeRequest %s %s %s \n",nodeId,volumeTagId,device);
                    }
                    else 
                    {
                        for(InstanceBlockDeviceMapping m :map)
                        {
                            String instanceVolumeId = m.getEbs().getVolumeId();
                            if(instanceVolumeId.equals(volumeId))
                            {
                                attach=false;
                                break;
                            }
                        }
                        if(attach == true)
                        {
                            requests+=String.format("AttachVolumeRequest %s %s %s \n",nodeId,volumeTagId,device);
                        }
                    }
                }
                else if (ins == null)
                {
                   requests+=String.format("AttachVolumeRequest %s %s %s \n",nodeTagId,volumeTagId,device);
                }
           }
        }
        return requests;
    }
    
    //function that checks if the id provided is a tag; if it is, it will return
    //the resource id, otherwise it will return the input parameter which is the 
    //id of the resource 
    //NOTE: does not work with volumes and instances because sometimes the tags
    //      persist for a while in this resources, therefore we need to check if the 
    //      instance or volume actually exists
    private String getResourceId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            return descriptions.get(descriptions.size()-1).getResourceId(); //get the last resource tagged with this id 
        }

        return tag;
    }
    
    //function to get the Id from and instance tag
    private String getInstanceId(String tag)
    {   
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);
        
        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            for(TagDescription des : descriptions)
            {
                Instance i = ec2Client.getInstance(des.getResourceId());
                if(i !=null && i.getState().getCode()!=48) //check if the instance was not deleted
                    return des.getResourceId();
            }
        }
        return tag;
    }
    
    //function to get the Id from and instance tag
    private String getVolumeId(String tag)
    {   
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);
        
        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            for(TagDescription des : descriptions)
            {
                Volume vol = ec2Client.getVolume(des.getResourceId());
                if(vol !=null  && !vol.getState().equals("deleted") )
                return des.getResourceId();
            }
        }
        return tag;
    }
}

