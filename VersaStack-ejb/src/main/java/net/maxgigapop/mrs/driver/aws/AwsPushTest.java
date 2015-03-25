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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;

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
    private Regions region = null;
    static final Logger logger = Logger.getLogger(AwsPush.class.getName());
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

    public static void main(String[] args) throws Exception {
        String modelAdditionStr = "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
                + "\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-123456>\n"
                + "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                + "        mrs:hasTag            <urn:ogf:network:aws.amazon.com:aws-cloud:portTag> ;\n"
                + "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.6> .\n"
                + "\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.6>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:type   \"ipv4:private\" ;\n"
                + "        mrs:value  \"10.0.0.6\" .\n"
                + "\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:subnet-2cd6ad16>\n"
                + "	nml:hasBidirectionalPort <urn:ogf:network:aws.amazon.com:aws-cloud:eni-123456> .";

        String modelReductionStr = "";
        OntModel model = AwsModelBuilder.createOntology("", "", Regions.US_EAST_1, "urn:ogf:network:aws.amazon.com:aws-cloud");
        StringWriter out = new StringWriter();
        try {
            model.write(out, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
        }
        String ttl = out.toString();
        System.out.println(ttl);

        AwsPushTest push = new AwsPushTest("", "", Regions.US_EAST_1, "urn:ogf:network:aws.amazon.com:aws-cloud");
        String request = push.pushPropagate(ttl, modelAdditionStr, modelReductionStr);
        System.out.println(request);
        push.pushCommit(request);
    }

    public AwsPushTest(String access_key_id, String secret_access_key, Regions region, String topologyUri) {
        //have all the information regarding the topology
        ec2Client = new AwsEC2Get(access_key_id, secret_access_key, region);
        client = ec2Client.getClient();
        this.region = region;

        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
    }

    /**
     * ***********************************************
     * function to propagate all the requests
     * ************************************************
     */
    public String pushPropagate(String modelTtl, String modelAddTtl, String modelReductTtl) throws Exception {
        String requests = "";

        OntModel modelReduct = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        OntModel modelAdd = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        try {
            modelAdd.read(new ByteArrayInputStream(modelAddTtl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall model addition, due to %s", e.getMessage()));
        }

        try {
            model.read(new ByteArrayInputStream(modelTtl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall reference model, due to %s", e.getMessage()));
        }

        try {
            modelReduct.read(new ByteArrayInputStream(modelReductTtl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall model reduction, due to %s", e.getMessage()));
        }
        //deatch volumes that need to be detached
        requests += detachVolumeRequests(model, modelReduct);

        //Delete a volume if a volume needs to be created
        requests += deleteVolumesRequests(model, modelReduct);

        //delete all the instances that need to be created
        requests += deleteInstancesRequests(model, modelReduct);

        //disassociate an address from a network interface
        requests += disassociateAddressRequest(model, modelReduct);

        //Delete the network interfaces that need to be deleted
        requests += deletePortsRequests(model, modelReduct);

        //Delete routes that need to be deleted
        requests += deleteRouteRequests(model, modelReduct);

        //deatch gateways that need to be detached
        requests += detachGatewayRequests(model, modelReduct);

        //delete gateways that need to be deleated
        requests += deleteGatewayRequests(model, modelReduct);

        //disassociate route tabes from subnets
        requests += disassociateTableRequests(model, modelReduct);

        //delete a route table 
        requests += deleteRouteTableRequests(model, modelReduct);

        //delete subnets that need to deleted
        requests += deleteSubnetsRequests(model, modelReduct);

        //delete the Vpcs that need to be deleted
        requests += deleteVpcsRequests(model, modelReduct);

        //create all the vpcs that need to be created
        requests += createVpcsRequests(model, modelAdd);

        //create all the subnets that need to be created
        requests += createSubnetsRequests(model, modelAdd);

        //create all the routeTables that need to be created
        requests += createRouteTableRequests(model, modelAdd);

        //create the associations of route tables
        requests += associateTableRequest(model, modelAdd);

        //create gateways request 
        requests += createGatewayRequests(model, modelAdd);

        //attach the gateways
        requests += attachGatewayRequests(model, modelAdd);

        //create the new routes requests
        requests += createRouteRequest(model, modelAdd);

        //create a volume if a volume needs to be created
        requests += createVolumesRequests(model, modelAdd);

        //create network interface if it needs to be created
        requests += createPortsRequests(model, modelAdd);

        //Associate an address with a  interface
        requests += associateAddressRequest(model, modelAdd);

        //create all the nodes that need to be created 
        requests += createInstancesRequests(model, modelAdd);

        //attach volumes that need to be atatched to existing instances
        requests += attachVolumeRequests(model, modelAdd);
        return requests;
    }

    /**
     * **********************************************************************
     * Function to do execute all the requests provided by the propagate method
     * **********************************************************************
     */
    public void pushCommit(String r) {
        String[] requests = r.split("[\\n]");

        for (String request : requests) {
            if (request.contains("TerminateInstancesRequest")) {
                String[] parameters = request.split("\\s+");

                String instanceId = getInstanceId(parameters[1]);
                TerminateInstancesRequest del = new TerminateInstancesRequest();
                del.withInstanceIds(instanceId);
                DeleteTagsRequest tagRequest = new DeleteTagsRequest();
                client.terminateInstances(del);
                ec2Client.getEc2Instances().remove(ec2Client.getInstance(instanceId));

            } else if (request.contains("DetachVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                DetachVolumeRequest volumeRequest = new DetachVolumeRequest();

                volumeRequest.withVolumeId(parameters[1])
                        .withInstanceId(parameters[2]);
                client.detachVolume(volumeRequest);

            } else if (request.contains("DisassociateAddressRequest")) {
                String[] parameters = request.split("\\s+");

                Address publicIp = ec2Client.getElasticIp(parameters[1]);
                DisassociateAddressRequest disassociateAddressRequest = new DisassociateAddressRequest();
                disassociateAddressRequest.withAssociationId(publicIp.getAssociationId())
                        .withPublicIp(publicIp.toString());
                client.disassociateAddress(disassociateAddressRequest);

            } else if (request.contains("DeleteNetworkInterfaceRequest")) {
                String[] parameters = request.split("\\s+");

                DeleteNetworkInterfaceRequest portRequest = new DeleteNetworkInterfaceRequest();
                portRequest.withNetworkInterfaceId(parameters[1]);
                client.deleteNetworkInterface(portRequest);
                ec2Client.getNetworkInterfaces().remove(ec2Client.getNetworkInterface(parameters[1]));

            } else if (request.contains("DeleteVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                DeleteVolumeRequest volumeRequest = new DeleteVolumeRequest();
                volumeRequest.withVolumeId(parameters[1]);
                client.deleteVolume(volumeRequest);
                ec2Client.getVolumes().remove(ec2Client.getVolume(parameters[1]));

            } else if (request.contains("DeleteRouteRequest")) {
                String[] parameters = request.split("\\s+");

                String tableIdTag = parameters[1];
                String target = getResourceId(parameters[3]);
                RouteTable t = ec2Client.getRoutingTable(getTableId(tableIdTag));
                Vpc v = ec2Client.getVpc(t.getVpcId());

                Route route = new Route();
                route.withDestinationCidrBlock(parameters[2])
                        .withGatewayId(target)
                        .withState(RouteState.Active)
                        .withOrigin(RouteOrigin.CreateRoute);

                if (t.getRoutes().contains(route) || parameters[2].equals(v.getCidrBlock())) {
                } else {
                    DeleteRouteRequest routeRequest = new DeleteRouteRequest();
                    routeRequest.withRouteTableId(t.getRouteTableId())
                            .withDestinationCidrBlock(parameters[2]);
                    client.deleteRoute(routeRequest);
                }

            } else if (request.contains("DeleteSubnetRequest")) {
                String[] parameters = request.split("\\s+");

                Subnet subnet = ec2Client.getSubnet(parameters[1]);
                DeleteSubnetRequest subnetRequest = new DeleteSubnetRequest();
                subnetRequest.withSubnetId(subnet.getSubnetId());
                client.deleteSubnet(subnetRequest);

                ec2Client.getSubnets().remove(subnet);

            } else if (request.contains("DeleteVpcRequest")) {
                String[] parameters = request.split("\\s+");

                Vpc vpc = ec2Client.getVpc(parameters[1]);
                DeleteVpcRequest vpcRequest = new DeleteVpcRequest();
                vpcRequest.withVpcId(vpc.getVpcId());
                client.deleteVpc(vpcRequest);

                ec2Client.getVpcs().remove(vpc);

            } else if (request.contains("DettachVpnGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                VpnGateway vpn = ec2Client.getVirtualPrivateGateway(getResourceId(parameters[1]));
                Vpc v = ec2Client.getVpc(getResourceId(parameters[2]));

                if (vpn.equals(ec2Client.getVirtualPrivateGateway(v))) {
                    DetachVpnGatewayRequest gwRequest = new DetachVpnGatewayRequest();
                    gwRequest.withVpnGatewayId(vpn.getVpnGatewayId())
                            .withVpcId(v.getVpcId());

                    client.detachVpnGateway(gwRequest);
                }

            } else if (request.contains("DettachInternetGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                InternetGateway igw = ec2Client.getInternetGateway(getResourceId(parameters[1]));
                Vpc v = ec2Client.getVpc(getResourceId(parameters[2]));

                if (igw.equals(ec2Client.getInternetGateway(v))) {
                    DetachInternetGatewayRequest gwRequest = new DetachInternetGatewayRequest();
                    gwRequest.withInternetGatewayId(getResourceId(parameters[1]))
                            .withVpcId(getResourceId(parameters[2]));

                    client.detachInternetGateway(gwRequest);
                }

            } else if (request.contains("DeleteInternetGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                InternetGateway gateway = ec2Client.getInternetGateway(parameters[1]);
                DeleteInternetGatewayRequest gatewayRequest = new DeleteInternetGatewayRequest();
                gatewayRequest.withInternetGatewayId(gateway.getInternetGatewayId());
                client.deleteInternetGateway(gatewayRequest);

                ec2Client.getInternetGateways().remove(gateway);

            } else if (request.contains("DeleteVpnGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                VpnGateway gateway = ec2Client.getVirtualPrivateGateway(parameters[1]);
                DeleteVpnGatewayRequest gatewayRequest = new DeleteVpnGatewayRequest();
                gatewayRequest.withVpnGatewayId(gateway.getVpnGatewayId());
                client.deleteVpnGateway(gatewayRequest);

                ec2Client.getVirtualPrivateGateways().remove(gateway);

            } else if (request.contains("DisassociateTableRequest")) {
                String[] parameters = request.split("\\s+");

                DisassociateRouteTableRequest tableRequest = new DisassociateRouteTableRequest();
                tableRequest.withAssociationId(parameters[1]);
                client.disassociateRouteTable(tableRequest);

            } else if (request.contains("DeleteRouteTableRequest")) {
                String[] parameters = request.split("\\s+");

                RouteTable table = ec2Client.getRoutingTable(parameters[1]);
                DeleteRouteTableRequest tableRequest = new DeleteRouteTableRequest();
                tableRequest.withRouteTableId(table.getRouteTableId());
                client.deleteRouteTable(tableRequest);

                ec2Client.getRoutingTables().remove(table);

            } else if (request.contains("CreateVpcRequest")) {
                String[] parameters = request.split("\\s+");

                CreateVpcRequest vpcRequest = new CreateVpcRequest();
                vpcRequest.withCidrBlock(parameters[1]);
                CreateVpcResult vpcResult = client.createVpc(vpcRequest);
                String vpcId = vpcResult.getVpc().getVpcId();

                //create the tag for the vpc
                CreateTagsRequest vpcTagRequest = new CreateTagsRequest();
                vpcTagRequest.withTags(new Tag("id", parameters[2]));
                vpcTagRequest.withResources(vpcId);
                ec2Client.getVpcs().add(vpcResult.getVpc());
                //wait a little bit to tag the vpc

                //tag the routing table of the 
                DescribeRouteTablesResult tablesResult = this.client.describeRouteTables();
                List<RouteTable> routeTables = tablesResult.getRouteTables();
                routeTables.removeAll(ec2Client.getRoutingTables()); //get the new routing table
                RouteTable mainTable = routeTables.get(0);
                CreateTagsRequest tableTagRequest = new CreateTagsRequest();
                tableTagRequest.withTags(new Tag("id", parameters[3]));
                tableTagRequest.withResources(mainTable.getRouteTableId());
                client.createTags(vpcTagRequest);
                client.createTags(tableTagRequest);
                ec2Client.getRoutingTables().add(mainTable);
            } else if (request.contains("CreateSubnetRequest")) {
                String[] parameters = request.split("\\s+");

                CreateSubnetRequest subnetRequest = new CreateSubnetRequest();
                subnetRequest.withVpcId(getResourceId(parameters[1]))
                        .withCidrBlock(parameters[2])
                        .withAvailabilityZone(Regions.US_EAST_1.getName() + "e");

                CreateSubnetResult subnetResult = client.createSubnet(subnetRequest);
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[3]));
                tagRequest.withResources(subnetResult.getSubnet().getSubnetId());

                client.createTags(tagRequest);
                ec2Client.getSubnets().add(subnetResult.getSubnet());
            } else if (request.contains("CreateRouteTableReques")) {
                String[] parameters = request.split("\\s+");

                CreateRouteTableRequest tableRequest = new CreateRouteTableRequest();
                tableRequest.withVpcId(getResourceId(parameters[1]));
                CreateRouteTableResult tableResult = client.createRouteTable(tableRequest);
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[2]));
                tagRequest.withResources(tableResult.getRouteTable().getRouteTableId());
                client.createTags(tagRequest);
                ec2Client.getRoutingTables().add(tableResult.getRouteTable());

            } else if (request.contains("AssociateTableRequest")) {
                String[] parameters = request.split("\\s+");

                String routeTableId = getTableId(parameters[1]);
                String subnetId = getResourceId(parameters[2]);

                AssociateRouteTableRequest associateRequest = new AssociateRouteTableRequest();
                associateRequest.withRouteTableId(getResourceId(routeTableId))
                        .withSubnetId(getResourceId(subnetId));
                AssociateRouteTableResult associateResult = client.associateRouteTable(associateRequest);

            } else if (request.contains("CreateInternetGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                CreateInternetGatewayResult igwResult = client.createInternetGateway();
                InternetGateway igw = igwResult.getInternetGateway();

                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[1]));
                tagRequest.withResources(igw.getInternetGatewayId());
                client.createTags(tagRequest);
                ec2Client.getInternetGateways().add(igw);

            } else if (request.contains("CreateVpnGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                CreateVpnGatewayRequest vpngwRequest = new CreateVpnGatewayRequest();
                vpngwRequest.withType(GatewayType.Ipsec1);
                CreateVpnGatewayResult vpngwResult = client.createVpnGateway(vpngwRequest);
                VpnGateway vpngw = vpngwResult.getVpnGateway();

                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[1]));
                tagRequest.withResources(vpngw.getVpnGatewayId());
                client.createTags(tagRequest);
                ec2Client.getVirtualPrivateGateways().add(vpngw);

            } else if (request.contains("CreateRouteRequest")) {
                String[] parameters = request.split("\\s+");

                String tableIdTag = parameters[1];
                String target = getResourceId(parameters[3]);
                RouteTable t = ec2Client.getRoutingTable(getTableId(tableIdTag));
                Vpc v = ec2Client.getVpc(t.getVpcId());

                Route route = new Route();
                route.withDestinationCidrBlock(parameters[2])
                        .withGatewayId(target)
                        .withState(RouteState.Active)
                        .withOrigin(RouteOrigin.CreateRoute);

                if (t.getRoutes().contains(route) || parameters[2].equals(v.getCidrBlock())) {
                } else {
                    CreateRouteRequest routeRequest = new CreateRouteRequest();
                    routeRequest.withRouteTableId(t.getRouteTableId())
                            .withGatewayId(target)
                            .withDestinationCidrBlock(parameters[2]);
                    client.createRoute(routeRequest);
                }

            } else if (request.contains("AttachInternetGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                InternetGateway igw = ec2Client.getInternetGateway(getResourceId(parameters[1]));
                Vpc v = ec2Client.getVpc(getResourceId(parameters[2]));

                if (!igw.equals(ec2Client.getInternetGateway(v))) {
                    AttachInternetGatewayRequest gwRequest = new AttachInternetGatewayRequest();
                    gwRequest.withInternetGatewayId(getResourceId(parameters[1]))
                            .withVpcId(getResourceId(parameters[2]));

                    client.attachInternetGateway(gwRequest);
                }

            } else if (request.contains("AttachVpnGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                VpnGateway vpn = ec2Client.getVirtualPrivateGateway(getResourceId(parameters[1]));
                Vpc v = ec2Client.getVpc(getResourceId(parameters[2]));

                if (!vpn.equals(ec2Client.getVirtualPrivateGateway(v))) {
                    AttachVpnGatewayRequest gwRequest = new AttachVpnGatewayRequest();
                    gwRequest.withVpnGatewayId(vpn.getVpnGatewayId())
                            .withVpcId(v.getVpcId());

                    client.attachVpnGateway(gwRequest);
                }

            } else if (request.contains("CreateVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                CreateVolumeRequest volumeRequest = new CreateVolumeRequest();
                volumeRequest.withVolumeType(parameters[1])
                        .withSize(Integer.parseInt(parameters[2]))
                        .withAvailabilityZone(parameters[3]);

                CreateVolumeResult result = client.createVolume(volumeRequest);

                Volume volume = result.getVolume();
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[4]));
                tagRequest.withResources(volume.getVolumeId());
                client.createTags(tagRequest);
                ec2Client.getVolumes().add(volume);

            } else if (request.contains("CreateNetworkInterfaceRequest")) {
                String[] parameters = request.split("\\s+");

                CreateNetworkInterfaceRequest portRequest = new CreateNetworkInterfaceRequest();
                portRequest.withPrivateIpAddress(parameters[1])
                        .withSubnetId(getResourceId(parameters[2]));
                CreateNetworkInterfaceResult portResult = client.createNetworkInterface(portRequest);

                NetworkInterface port = portResult.getNetworkInterface();
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[3]));
                tagRequest.withResources(port.getNetworkInterfaceId());
                client.createTags(tagRequest);
                ec2Client.getNetworkInterfaces().add(port);

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
                Instance instance = result.getReservation().getInstances().get(0);
                CreateTagsRequest tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", parameters[3]));
                tagRequest.withResources(instance.getInstanceId());
                client.createTags(tagRequest);
                ec2Client.getEc2Instances().add(instance);

                //tag the root volume
                tagRequest = new CreateTagsRequest();
                tagRequest.withTags(new Tag("id", volumeTag));
                //String volumeId = m.getEbs().getVolumeId();
                //tagRequest.withResources(volumeId);
                //client.createTags(tagRequest);
            } else if (request.contains("AttachVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                AttachVolumeRequest volumeRequest = new AttachVolumeRequest();
                volumeRequest.withInstanceId(getInstanceId(parameters[1]))
                        .withVolumeId(getVolumeId(parameters[2]))
                        .withDevice(parameters[3]);

                client.attachVolume(volumeRequest);
            }
        }
    }

    /**
     * ****************************************************************
     * function that executes a query using a model addition/subtraction and a
     * reference model, returns the result of the query
     * ****************************************************************
     */
    private ResultSet executeQuery(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet r = qexec.execSelect();

        //check on reference model if the statement is not in the model addition,
        //or model subtraction
        if (!r.hasNext()) {
            qexec = QueryExecutionFactory.create(query, refModel);
            r = qexec.execSelect();
        }
        return r;
    }

    /**
     * ****************************************************************
     * Detach a volume to an existing instance AWS
     * ****************************************************************
     */
    private String detachVolumeRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";

        //check if the volume is new therefore it should be in the model additiom
        String query = "SELECT  ?node ?volume  WHERE {?node  mrs:hasVolume  ?volume}";

        ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode node = querySolution1.get("node");
            String nodeTagId = node.asResource().toString().replace(topologyUri, "");
            RDFNode volume = querySolution1.get("volume");
            String volumeTagId = volume.asResource().toString().replace(topologyUri, "");
            String volumeId = getVolumeId(volumeTagId);

            query = "SELECT ?deviceName WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName}";
            ResultSet r2 = executeQuery(query, model, modelReduct);
            if (!r2.hasNext()) {
                throw new Exception(String.format("volume device name is not specified for volume %s", volume));
            }

            QuerySolution querySolution2 = r2.next();

            RDFNode deviceName = querySolution2.get("deviceName");
            String device = deviceName.asLiteral().toString();

            if (!device.equals("/dev/sda1") && !device.equals("/dev/xvda")) {
                String nodeId = getInstanceId(nodeTagId);

                Instance ins = ec2Client.getInstance(nodeId);
                Volume vol = ec2Client.getVolume(volumeId);
                if (vol == null) {
                    throw new Exception(String.format("The volume %s to be deleted does not exist", volume));
                }
                if (ins == null) {
                    throw new Exception(String.format("The node %s where the volume %s "
                            + "wiil be deattached does not exist", node, volume));
                }
                if (!vol.getAttachments().isEmpty()) {
                    requests += String.format("DetachVolumeRequest %s %s \n", volumeId, nodeId);
                }
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to disassociate an address from a network interface
     * ****************************************************************
     */
    private String disassociateAddressRequest(OntModel model, OntModel modelReduct) throws Exception {
        //to get the public ip address of the network interface if any
        String requests = "";
        String query = "SELECT  ?port ?address WHERE {?port  mrs:hasNetworkAddress ?address ."
                + "?address mrs:type \"ipv4:public\"}";
        ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode address = querySolution1.get("address");
            RDFNode port = querySolution1.get(("port"));
            String portIdTagValue = port.asResource().toString().replace(topologyUri, "");
            query = "SELECT ?value WHERE {<" + address.asResource() + ">  mrs:value  ?value}";
            ResultSet r2 = executeQuery(query, model, modelReduct);
            if (!r2.hasNext()) {
                throw new Exception(String.format("model Reduction  network addres  %s for port $s"
                        + "is not found in the reference model", address, port));
            }
            QuerySolution querySolution2 = r2.next();
            RDFNode value = querySolution2.get("value");
            String publicAddress = value.asLiteral().toString().replace(topologyUri, "");
            requests += "DisassociateAddressRequest " + publicAddress + " " + portIdTagValue
                    + " \n";
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to delete a network interface
     * ****************************************************************
     */
    private String deletePortsRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query;

        //get the tag resource from the reference model that indicates 
        //that this is a network  interface 
        query = "SELECT ?tag WHERE {?tag mrs:type \"interface\" ."
                + "?tag mrs:value \"network\"}";
        ResultSet r = executeQuery(query, model, emptyModel);
        if (!r.hasNext()) {
            throw new Exception(String.format("Reference model has no tags for network"
                    + "interfaces"));
        }
        QuerySolution q = r.next();
        RDFNode tag = q.get("tag");
        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort ."
                + "?port  mrs:hasTag <" + tag.asResource() + ">}";
        r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portIdTagValue = port.asResource().toString().replace(topologyUri, "");
            String portId = getResourceId(portIdTagValue);

            NetworkInterface p = ec2Client.getNetworkInterface(portId);
            if (p == null) //network interface does not exist, need to create a network interface
            {
                throw new Exception(String.format("The port %s to be deleted"
                        + "does not exists", port));
            } else {
                //check to see the network interface has no attachments before
                //deleting it, if they do and the instance is not in the deleting
                //part, there will be an error
                query = "SELECT ?node WHERE {?node nml:hasBidirectionalPort <" + port.asResource() + "> ."
                        + "?node a nml:Node}";
                ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
                if (p.getAttachment() != null && !r1.hasNext()) {
                    throw new Exception(String.format("The port %s to be deleted"
                            + " has attachments, delete dependant resource first", port));
                }
                //to get the private ip of the network interface
                query = "SELECT ?address WHERE {<" + port.asResource() + ">  mrs:privateIpAddress  ?address}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model Reduction does not specify privat ip address of port: %s", port));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");
                String privateAddress = address.asResource().toString().replace(topologyUri, "");

                //find the subnet that has the port previously found
                query = "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model Reduction does not specify network subnet of port: %s", port));
                }
                String subnetId = null;
                while (r1.hasNext()) {
                    querySolution1 = r1.next();
                    RDFNode subnet = querySolution1.get("subnet");
                    query = "SELECT ?subnet WHERE {<" + subnet.asResource() + ">  a  mrs:SwitchingSubnet}";
                    ResultSet r3 = executeQuery(query, model, modelReduct);
                    while (r3.hasNext()) //search in the model to see if subnet existed before
                    {
                        subnetId = subnet.asResource().toString().replace(topologyUri, "");
                        subnetId = getResourceId(subnetId);
                        break;
                    }
                }
                if (subnetId == null) {
                    throw new Exception(String.format("model additions subnet for port %s"
                            + "is not found in the reference model", subnetId));
                }
                //create the network interface 
                requests += String.format("DeleteNetworkInterfaceRequest %s \n", portId);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * function to delete an instance from a model
     * ****************************************************************
     */
    private String deleteInstancesRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");

            query = "SELECT ?service ?volume ?port WHERE {<" + node.asResource() + "> mrs:providedByService ?service ."
                    + "<" + node.asResource() + "> mrs:hasVolume ?volume ."
                    + "<" + node.asResource() + "> nml:hasBidirectionalPort ?port}";
            ResultSet r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new Exception(String.format("model reduction is malformed for node: %s", node));
            }
            QuerySolution querySolution1 = r1.next();

            RDFNode service = querySolution1.get("service");
            RDFNode volume = querySolution1.get("volume");
            RDFNode port = querySolution1.get("port");
            if (service == null) {
                throw new Exception(String.format("node to delete does not specify service for node: %s", node));
            }
            if (volume == null) {
                throw new Exception(String.format("node to delete does not specify volume for node: %s", node));
            }
            if (port == null) {
                throw new Exception(String.format("node to delete does not specify port for node: %s", node));
            }
            //check that one of the volumes is the main device 
            query = "SELECT ?volume ?deviceName WHERE {<" + node.asResource() + "> mrs:hasVolume ?volume ."
                    + "?volume mrs:target_device ?deviceName}";
            r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new Exception(String.format("model reduction does not specify root device name for volume"
                        + " attached to instance: %s", node));
            }
            boolean found = false;
            while (r1.hasNext()) {
                QuerySolution q1 = r1.next();
                RDFNode deviceName = q1.get("deviceName");
                if (deviceName.toString().equals("/dev/sda1") || deviceName.toString().equals("/dev/xvda")) {
                    found = true;
                }
            }
            if (found == false) {
                throw new Exception(String.format("model reduction does not specify root volume"
                        + " attached to instance: %s", node));
            }

            query = "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() + ">}";
            r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new Exception(String.format("model reduction does not specify vpc of node to be deleted: %s", node));
            }

            query = "SELECT ?service WHERE {?service  mrs:providesVM <" + node.asResource() + ">}";
            r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new Exception(String.format("model reduction does not specify service of node: %s", node));
            }

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

    /**
     * ****************************************************************
     * Function to delete volumes from a model
     * ****************************************************************
     */
    private String deleteVolumesRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeIdTagValue = volume.asResource().toString().replace(topologyUri, "");
            String volumeId = getVolumeId(volumeIdTagValue);

            Volume v = ec2Client.getVolume(volumeId);

            if (v == null) {
                throw new Exception(String.format("volume does not exist: %s", volume));
            } else //volume exists so it has to be deleted
            {
                //check what service is providing the volume
                query = "SELECT ?type WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model reduction does not specify service that provides volume: %s", volume));
                }

                //find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model reduction does not specify new type of volume: %s", volume));
                }
                //find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model reduction does not specify new size of volume: %s", volume));
                }
                //dont create the volumes with no target device and that are attached to an instance
                //since this volumes are the root devices
                query = "SELECT ?deviceName WHERE {<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                r1 = executeQuery(query, model, modelReduct);
                if (r1.hasNext()) {
                    QuerySolution querySolution1 = r1.next();
                    String deviceName = querySolution1.get("deviceName").asLiteral().toString();

                    if (!deviceName.equals("/dev/sda1") && !deviceName.equals("/dev/xvda")) {
                        requests += String.format("DeleteVolumeRequest %s \n", volumeId);
                    }
                } else {
                    requests += String.format("DeleteVolumeRequest %s \n", volumeId);
                }
            }
        }

        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a volumes from a model
     * ****************************************************************
     */
    private String deleteRouteRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query = "";
        String tempRequest = "";

        query = "SELECT ?route ?table WHERE {?route a mrs:Route ."
                + "?table mrs:hasRoute ?route}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode table = q.get("table");
            RDFNode route = q.get("route");

            String tableIdTag = table.asResource().toString().replace(topologyUri, "");
            RouteTable rTable = ec2Client.getRoutingTable(getTableId(tableIdTag));
            if (rTable == null) {
                throw new Exception(String.format("Route Table %s where"
                        + " route %s will be deleted does not exist ", tableIdTag, route));
            }

            //make sure the new route will have all the source subnets as the table already has
            List<String> tableSubnets = new ArrayList();
            List<String> subnets = new ArrayList();
            for (RouteTableAssociation as : rTable.getAssociations()) {
                String subnetId = as.getSubnetId();
                String subnetIdTag = ec2Client.getIdTag(subnetId);
                tableSubnets.add(subnetIdTag);
            }
            query = "SELECT ?value WHERE {<" + route.asResource() + "> mrs:routeFrom ?routeFrom ."
                    + "?routeFrom mrs:type \"subnet\" ."
                    + "?routeFrom mrs:value ?value}";
            ResultSet r2 = executeQuery(query, emptyModel, modelReduct);
            if (!r2.hasNext()) {
                continue;
            }
            while (r2.hasNext()) {
                QuerySolution q2 = r2.next();
                RDFNode value = q2.get("value");
                subnets.add(value.asLiteral().toString());
            }
            Collections.sort(subnets);
            Collections.sort(tableSubnets);
            if (!subnets.equals(tableSubnets)) {
                throw new Exception(String.format("Discrepancy between source destinations between"
                        + " reference model and old route %s", route));
            }
            query = "SELECT  ?nextHop ?value WHERE {<" + route.asResource() + "> mrs:routeTo ?routeTo ."
                    + "<" + route.asResource() + "> mrs:nextHop ?nextHop ."
                    + "?routeFrom mrs:type \"subnet\" ."
                    + "?routeTo mrs:value ?value}";
            r2 = executeQuery(query, emptyModel, modelReduct);
            while (r2.hasNext()) {
                QuerySolution q2 = r2.next();
                RDFNode value = q2.get("value");
                RDFNode nextHop = q2.get("nextHop");
                String destination = value.asLiteral().toString();
                String target;
                String gatewayId;
                if (nextHop.isLiteral()) {
                    target = nextHop.asLiteral().toString();
                    gatewayId = target;
                } else {
                    target = nextHop.asResource().toString().replace(topologyUri, "");
                    gatewayId = getResourceId(target);
                }

                Route rou = new Route();
                rou.withDestinationCidrBlock(destination)
                        .withGatewayId(gatewayId)
                        .withState(RouteState.Active)
                        .withOrigin(RouteOrigin.CreateRoute);

                if (rTable.getRoutes().contains(rou)) {
                    tempRequest = String.format("DeleteRouteRequest %s %s %s \n", tableIdTag, destination, target);
                } else {
                    throw new Exception(String.format("Route %s to be deleted does not exist in the route table %s", route, table));
                }

                if (!requests.contains(tempRequest)) {
                    requests += tempRequest;
                }

            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to disassociate Route table with a subnet
     * ****************************************************************
     */
    private String disassociateTableRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String tempRequests = "";
        String query;

        query = "SELECT ?route ?value WHERE {?route mrs:routeFrom ?routeFrom ."
                + "?routeFrom mrs:type \"subnet\" ."
                + "?routeFrom mrs:value ?value}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            boolean createRequest = true;
            QuerySolution querySolution = r.next();
            RDFNode value = querySolution.get("value");
            RDFNode route = querySolution.get("route");

            query = "SELECT ?table WHERE {?table mrs:hasRoute <" + route.asResource() + "> ."
                    + "?service mrs:providesRoute <" + route.asResource() + ">}";
            ResultSet r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new Exception(String.format("Route  %s"
                        + " in model reduction does not have a route table or is not"
                        + " being provided by a routing service", route));
            }
            QuerySolution querySolution1 = r1.next();
            RDFNode table = querySolution1.get("table");

            String subnetIdTag = value.asLiteral().toString();
            String subnetId = getResourceId(subnetIdTag);
            String tableIdTag = table.asResource().toString().replace(topologyUri, "");

            if (ec2Client.getSubnet(subnetId) == null) {
                throw new Exception(String.format("subnet %s to disassociate from"
                        + "route table %s does not exist", subnetIdTag, tableIdTag));
            }

            RouteTable rt = ec2Client.getRoutingTable(getTableId(tableIdTag));
            if (rt == null) {
                throw new Exception(String.format("Route table %s to disassociate"
                        + " does not exist", tableIdTag));
            } else {
                for (RouteTableAssociation as : rt.getAssociations()) {
                    if (as.getSubnetId().equals(getResourceId(subnetIdTag))) {
                        createRequest = true;
                    }
                }
            }

            String querySubnetIdTag = "\"" + subnetIdTag + "\"";
            if (createRequest == true) {
                //check that older routes in the table now include this new route
                //from network address
                query = "SELECT ?route WHERE {<" + table.asResource() + "> mrs:hasRoute ?route}";
                ResultSet r2 = executeQuery(query, model, emptyModel);
                while (r2.hasNext()) {
                    QuerySolution q2 = r2.next();
                    RDFNode ro = q2.get("route");
                    query = "SELECT ?routeFrom WHERE {<" + ro.asResource() + "> mrs:routeFrom ?routeFrom ."
                            + "?routeFrom mrs:value " + querySubnetIdTag + "}";
                    ResultSet r3 = executeQuery(query, emptyModel, modelReduct);
                    if (!r3.hasNext()) {
                        throw new Exception(String.format("Route  %s does state"
                                + "association with subnet %s", ro, subnetIdTag));
                    }
                }
                //get the association id of the route table association to delete
                String associationId = "";
                for (RouteTableAssociation as : rt.getAssociations()) {
                    if (as.getSubnetId().equals(subnetId)) {
                        associationId = as.getRouteTableAssociationId();
                        break;
                    }
                }
                tempRequests = String.format("DisassociateTableRequest %s \n", associationId);
                if (!requests.contains(tempRequests))//dont include duplicate requests
                {
                    requests += tempRequests;
                }
            }
        }
        return requests;
    }

    /*
     * ****************************************************************
     * Function to create a volumes from a model
     *****************************************************************
     */
    private String detachGatewayRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query = "";

        //fin all the vpcs that have a bidirectional port
        query = "SELECT ?vpc ?port  WHERE {?vpc nml:hasBidirectionalPort ?port}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode gateway = q.get("port");
            RDFNode vpc = q.get("vpc");

            //check that vpc is the correct type
            query = "SELECT ?vpc WHERE {<" + vpc.asResource() + "> a nml:Topology}";
            ResultSet r1 = executeQuery(query, model, modelReduct);
            while (r1.hasNext()) {
                r1.next();
                query = "SELECT ?tag WHERE {<" + gateway.asResource() + "> mrs:hasTag ?tag}";
                ResultSet r2 = executeQuery(query, emptyModel, modelReduct);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("Tag for Internet gateway %s i"
                            + "s not specified in model addition", gateway));
                }
                QuerySolution q1 = r2.next();
                RDFNode tag = q1.get("tag");

                //look for the lable in the reference model
                query = "SELECT ?value WHERE {<" + tag.asResource() + "> mrs:type \"gateway\" ."
                        + "<" + tag.asResource() + "> mrs:value  ?value}";
                r2 = executeQuery(query, model, emptyModel);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("Label %s for gateway %s is"
                            + " an invalid label", tag, gateway));
                }
                q1 = r2.next();
                String value = q1.get("value").asLiteral().toString();
                String gatewayIdTag = gateway.asResource().toString().replace(topologyUri, "");
                String gatewayId = getResourceId(gatewayIdTag);
                String vpcIdTag = vpc.asResource().toString().replace(topologyUri, "");
                String vpcId = getResourceId(vpcIdTag);
                if (ec2Client.getVpc(vpcId) == null) {
                    throw new Exception(String.format("Vpc with vpc id %s from where"
                            + "the gateway will be detacched does not exist", vpcId));
                }
                if (value.equals("internet")) {
                    if (ec2Client.getInternetGateway(gatewayId) == null) {
                        throw new Exception(String.format("Gatway %s to be detacched does"
                                + "not exist", gatewayId));
                    }
                    requests += String.format("DettachInternetGatewayRequest %s %s \n", gatewayId, vpcId);
                } else if (value.equals("vpn")) {
                    if (ec2Client.getVirtualPrivateGateway(gatewayId) == null) {
                        throw new Exception(String.format("Gatway %s to be detacched does"
                                + "not exist", gatewayId));
                    }
                    requests += String.format("DettachVpnGatewayRequest %s %s \n", gatewayId, vpcId);
                }

            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to delete gateway (Internet and VPN)
     * ****************************************************************
     */
    private String deleteGatewayRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?igw WHERE {?igw a nml:BidirectionalPort ."
                + "?igw mrs:hasTag ?tag ."
                + "?tag mrs:type \"gateway\"}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode igw = q.get("igw");
            String idTag = igw.asResource().toString().replace(topologyUri, "");

            query = "SELECT ?tag WHERE {<" + igw.asResource() + "> mrs:hasTag ?tag}";
            ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
            if (!r1.hasNext()) {
                throw new Exception(String.format("Tag for Internet gateway %s i"
                        + "s not specified in model reduction", igw));
            }
            QuerySolution q1 = r1.next();
            RDFNode tag = q1.get("tag");

            //look for the lable in the reference model
            query = "SELECT ?value WHERE {<" + tag.asResource() + "> mrs:type \"gateway\" ."
                    + "<" + tag.asResource() + "> mrs:value ?value}";
            r1 = executeQuery(query, model, emptyModel);
            if (!r1.hasNext()) {
                throw new Exception(String.format("Tag %s for gateway %s is"
                        + " an invalid tag", tag, igw));
            }
            q1 = r1.next();
            String value = q1.get("value").asLiteral().toString();
            if (value.equals("internet")) {
                InternetGateway gateway = ec2Client.getInternetGateway(getResourceId(idTag));
                if (gateway == null) {
                    throw new Exception(String.format("Internet gateway %s to de deleted"
                            + "does not exist", idTag));
                }
                requests += String.format("DeleteInternetGatewayRequest %s \n", gateway.getInternetGatewayId());
            } else if (value.equals("gateway")) {
                VpnGateway gateway = ec2Client.getVirtualPrivateGateway(getResourceId(idTag));
                if (gateway == null) {
                    throw new Exception(String.format("Internet gateway %s to de deleted"
                            + "does not exist", idTag));
                }
                requests += String.format("DeleteVpnGatewayRequest %s \n", gateway.getVpnGatewayId());
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to delete the route table
     * ****************************************************************
     */
    private String deleteRouteTableRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query = "";

        query = "SELECT ?table WHERE {?table a mrs:RoutingTable ."
                + "?table mrs:type \"local\"}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode table = querySolution.get("table");
            String tableIdTagValue = table.asResource().toString().replace(topologyUri, "");
            String tableId = getTableId(tableIdTagValue);
            if (ec2Client.getRoutingTable(tableId) != null) {
                //check route table is modeled 
                query = "SELECT ?type WHERE{<" + table.asResource() + "> mrs:type ?type}";
                ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model Reduction for route table %s"
                            + " is not well specified", table));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //check the service that provides the routing table
                query = "SELECT ?service WHERE {?service   mrs:providesRoutingTable <" + table.asResource() + ">}";
                r1 = executeQuery(query, emptyModel, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Route table to be deleted %s is not provided by any service", table));
                }
                querySolution1 = r1.next();
                RDFNode service = querySolution1.get("service");

                //get the address of the vpc of the route table
                query = "SELECT ?vpc ?address WHERE {?vpc nml:hasService <" + service.asResource() + "> ."
                        + "?vpc mrs:hasNetworkAddress ?networkAddress ."
                        + "?networkAddress mrs:value ?address}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("vpc of route table %s in model reduction "
                            + "has no network address", table));
                }
                querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");
                RDFNode vpc = querySolution1.get("vpc");
                String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri, "");

                //if the table was a main table, it was created with
                //the vpc
                if (!type.asLiteral().toString().equals("main")) {
                    //check the main route to the vpc is there
                    boolean found = false;
                    query = "SELECT ?route ?from ?routeTo WHERE{<" + table.asResource() + "> mrs:hasRoute ?route ."
                            + "?route mrs:nextHop \"local\" ."
                            + "?to mrs:type \"ipv4-prefix\" ."
                            + "?to mrs:value ?routeTo}";
                    r1 = executeQuery(query, emptyModel, modelReduct);
                    if (!r1.hasNext()) {
                        throw new Exception(String.format("model reduction for route table %s"
                                + " does not have any main  route", table));
                    }
                    while (r1.hasNext()) {
                        QuerySolution querySolution2 = r1.next();
                        RDFNode addressTable = querySolution2.get("routeTo");
                        //compare to check if it is in fact the address of the vpc
                        if (addressTable.asLiteral().toString().equals(address.asLiteral().toString())) {
                            found = true;
                        }
                    }
                    if (found == false) {
                        throw new Exception(String.format("route table %s"
                                + " does not have the main route", table));
                    }
                }
                requests += String.format("DeleteRouteTableRequest  %s \n", tableId);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a Vpc from a model
     * /*****************************************************************
     */
    private String deleteVpcsRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?vpc WHERE {?service mrs:providesVPC  ?vpc ."
                + "?vpc a nml:Topology}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode vpc = querySolution.get("vpc");
            String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri, "");
            String vpcId = getResourceId(vpcIdTagValue);

            //double check vpc does not exist in the cloud
            Vpc v = ec2Client.getVpc(vpcId);
            if (v == null) // vpc does not exist, has to be created
            {
                throw new Exception(String.format("vpc to be deleted $s does not exist", vpc));
            } else {
                query = "SELECT ?cloud WHERE {?cloud nml:hasTopology <" + vpc.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model reduction does not specify the aws-cloud that"
                            + "provides VPC : %s", vpc));
                }

                query = "SELECT ?address WHERE {<" + vpc.asResource() + "> mrs:hasNetworkAddress ?address}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model reduction does not specify the "
                            + "newtowk address of vpc: %s", vpc));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");

                query = "SELECT ?type ?value WHERE {<" + address.asResource() + "> mrs:type ?type ."
                        + "<" + address.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model reduction does not specify the "
                            + "type or value of network address: %s", address));
                }
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();

                //check taht vpc offers switching and routing Services and vpc services
                query = "SELECT ?service  WHERE {<" + vpc.asResource() + "> nml:hasService  ?service ."
                        + "?service a  mrs:SwitchingService}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Vpc %s does not speicfy Switching Service in the model reduction", vpc));
                }
                query = "SELECT ?service  WHERE {<" + vpc.asResource() + "> nml:hasService  ?service ."
                        + "?service a mrs:RoutingService}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Vpc %s does not speicfy Routing Service in the "
                            + "model reduction", vpc));
                }
                querySolution1 = r1.next();
                RDFNode routingService = querySolution1.get("service");

                //incorporate main routing table and routes into the vpc
                query = "SELECT ?routingTable ?type  WHERE {<" + routingService.asResource() + "> mrs:providesRoutingTable ?routingTable ."
                        + "?routingTable mrs:type  \"main\"}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Routing service  %s does not speicfy main Routing table in the model "
                            + "reduction", routingService));
                }

                querySolution1 = r1.next();
                RDFNode routingTable = querySolution1.get("routingTable");
                String routeTableIdTagValue = routingTable.asResource().toString().replace(topologyUri, "");

                //add routeTable id tag to the request to tatg the main route Table later
                String vpcIp = cidrBlock;
                cidrBlock = "\"" + cidrBlock + "\"";

                //check for  the local route is in the route table 
                query = "SELECT ?route ?to  WHERE {<" + routingTable.asResource() + "> mrs:hasRoute ?route ."
                        + "<" + routingService.asResource() + "> mrs:providesRoute ?route ."
                        + "?route mrs:nextHop \"local\" ."
                        + "?route mrs:routeTo  ?to ."
                        + "?to mrs:value " + cidrBlock + "}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Routing service has no route for main table in the model"
                            + " reduction", routingService));
                }
                requests += String.format("DeleteVpcRequest %s \n", vpcId);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a subnets from a model
     * ****************************************************************
     */
    private String deleteSubnetsRequests(OntModel model, OntModel modelReduct) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetIdTagValue = subnet.asResource().toString().replace(topologyUri, "");
            String subnetId = getResourceId(subnetIdTagValue);

            Subnet s = ec2Client.getSubnet(subnetId);

            if (s == null) //subnet does not exist, need to create subnet
            {
                throw new Exception(String.format("subnet %s to be deleted does not exist ", subnet));
            } else {
                query = "SELECT ?service {?service mrs:providesSubnet <" + subnet.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("No service has subnet %s", subnet));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode service = querySolution1.get("service");
                query = "SELECT ?vpc {?vpc nml:hasService <" + service.asResource() + "> ."
                        + "<" + service.asResource() + "> a mrs:SwitchingService}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    r1 = executeQuery(query, model, model);
                }
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Subnet %s does not have a vpc in the model reduction", subnet));
                }
                querySolution1 = r1.next();
                RDFNode vpc = querySolution1.get("vpc");
                String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri, "");

                query = "SELECT ?subnet ?address ?value WHERE {?subnet mrs:hasNetworkAddress ?address ."
                        + "?address a mrs:NetworkAddress ."
                        + "?address mrs:type \"ipv4-prefix\" ."
                        + "?address mrs:value ?value}";
                r1 = executeQuery(query, model, modelReduct);
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();

                requests += String.format("DeleteSubnetRequest %s \n", subnetId);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a Vpc from a model
     * /*****************************************************************
     */
    private String createVpcsRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?vpc WHERE {?service mrs:providesVPC  ?vpc ."
                + "?vpc a nml:Topology}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode vpc = querySolution.get("vpc");
            String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri, "");
            String vpcId = getResourceId(vpcIdTagValue);

            //double check vpc does not exist in the cloud
            Vpc v = ec2Client.getVpc(vpcId);
            if (v != null) // vpc  exists, has to be created
            {
                throw new Exception(String.format("VPC %s already exists", vpcIdTagValue));
            } else {
                query = "SELECT ?cloud WHERE {?cloud nml:hasTopology <" + vpc.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the aws-cloud that"
                            + "provides VPC : %s", vpc));
                }

                query = "SELECT ?address WHERE {<" + vpc.asResource() + "> mrs:hasNetworkAddress ?address}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the "
                            + "newtowk address of vpc: %s", vpc));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");

                query = "SELECT ?type ?value WHERE {<" + address.asResource() + "> mrs:type ?type ."
                        + "<" + address.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the "
                            + "type or value of network address: %s", address));
                }
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();

                //check taht vpc offers switching and routing Services and vpc services
                query = "SELECT ?service  WHERE {<" + vpc.asResource() + "> nml:hasService  ?service ."
                        + "?service a  mrs:SwitchingService}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("New Vpc %s does not speicfy Switching Service", vpc));
                }

                query = "SELECT ?service  WHERE {<" + vpc.asResource() + "> nml:hasService  ?service ."
                        + "?service a mrs:RoutingService}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("New Vpc %s does not speicfy Routing Service", vpc));
                }
                querySolution1 = r1.next();
                RDFNode routingService = querySolution1.get("service");

                //incorporate main routing table and routes into the vpc
                query = "SELECT ?routingTable ?type  WHERE {<" + routingService.asResource() + "> mrs:providesRoutingTable ?routingTable ."
                        + "?routingTable mrs:type  \"main\"}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Routing service  %s does not speicfy main Routing table in the model addition", routingService));
                }

                querySolution1 = r1.next();
                RDFNode routingTable = querySolution1.get("routingTable");
                String routeTableIdTagValue = routingTable.asResource().toString().replace(topologyUri, "");

                String vpcIp = cidrBlock;
                cidrBlock = "\"" + cidrBlock + "\"";

                //check for  the local route is in the route table 
                query = "SELECT ?route ?to  WHERE {<" + routingTable.asResource() + "> mrs:hasRoute ?route ."
                        + "<" + routingService.asResource() + "> mrs:providesRoute ?route ."
                        + "?route mrs:nextHop \"local\" ."
                        + "?route mrs:routeTo  ?to ."
                        + "?to mrs:value " + cidrBlock + "}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Routing service has no route for main table in the model"
                            + " addition", routingService));
                }
                requests += String.format("CreateVpcRequest %s %s %s \n", vpcIp, vpcIdTagValue, routeTableIdTagValue);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a subnets from a model
     * ****************************************************************
     */
    private String createSubnetsRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetIdTagValue = subnet.asResource().toString().replace(topologyUri, "");
            String subnetId = getResourceId(subnetIdTagValue);

            Subnet s = ec2Client.getSubnet(subnetId);

            if (s != null) //subnet  exists,does not need to create one
            {
                throw new Exception(String.format("Subnet %s already exists", subnetIdTagValue));
            } else {
                query = "SELECT ?service {?service mrs:providesSubnet <" + subnet.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("No service has subnet %s", subnet));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode service = querySolution1.get("service");
                query = "SELECT ?vpc {?vpc nml:hasService <" + service.asResource() + "> ."
                        + "<" + service.asResource() + "> a mrs:SwitchingService}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    r1 = executeQuery(query, model, model);
                }
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Subnet %s does not have a vpc", subnet));
                }
                querySolution1 = r1.next();
                RDFNode vpc = querySolution1.get("vpc");
                String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri, "");

                query = "SELECT ?subnet ?address ?value WHERE {?subnet mrs:hasNetworkAddress ?address ."
                        + "?address a mrs:NetworkAddress ."
                        + "?address mrs:type \"ipv4-prefix\" ."
                        + "?address mrs:value ?value}";
                r1 = executeQuery(query, model, modelAdd);
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();

                requests += String.format("CreateSubnetRequest %s %s %s \n", vpcIdTagValue, cidrBlock, subnetIdTagValue);

            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to createRoutetable
     * ****************************************************************
     */
    private String createRouteTableRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query = "";

        query = "SELECT ?table WHERE {?table a mrs:RoutingTable ."
                + "?table mrs:type \"local\"}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode table = querySolution.get("table");
            String tableIdTagValue = table.asResource().toString().replace(topologyUri, "");
            String tableId = getTableId(tableIdTagValue);
            if (ec2Client.getRoutingTable(tableId) != null) //routing table already exists
            {
                throw new Exception(String.format("Routing Table %s already exists, does not need"
                        + "to create one", tableIdTagValue));
            } else {
                //check route table is modeled 
                query = "SELECT ?type WHERE{<" + table.asResource() + "> mrs:type ?type}";
                ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition for route table %s"
                            + " is not well specified", table));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //check the service that provides the routing table
                query = "SELECT ?service WHERE {?service   mrs:providesRoutingTable <" + table.asResource() + ">}";
                r1 = executeQuery(query, emptyModel, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("Route table %s is not provided by any service in the model addition", table));
                }
                querySolution1 = r1.next();
                RDFNode service = querySolution1.get("service");

                //get the address of the vpc of the route table
                query = "SELECT ?vpc ?address WHERE {?vpc nml:hasService <" + service.asResource() + "> ."
                        + "?vpc mrs:hasNetworkAddress ?networkAddress ."
                        + "?networkAddress mrs:value ?address}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("vpc of route table %s has no network address", table));
                }
                querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");
                RDFNode vpc = querySolution1.get("vpc");
                String vpcIdTagValue = vpc.asResource().toString().replace(topologyUri, "");

                //if the table was a main table, it was created with
                //the vpc
                if (!type.asLiteral().toString().equals("main")) {
                    //check the main route to the vpc is there
                    boolean found = false;
                    query = "SELECT ?route ?routeTo WHERE{<" + table.asResource() + "> mrs:hasRoute ?route ."
                            + "?route mrs:nextHop \"local\" ."
                            + "?to mrs:type \"ipv4-prefix\" ."
                            + "?to mrs:value ?routeTo}";
                    r1 = executeQuery(query, emptyModel, modelAdd);
                    if (!r1.hasNext()) {
                        throw new Exception(String.format("model addition for route table %s"
                                + "does not have any routes", table));
                    }
                    while (r1.hasNext()) {
                        QuerySolution querySolution2 = r1.next();
                        RDFNode addressTable = querySolution2.get("routeTo");
                        //compare to check if it is in fact the address of the vpc
                        if (addressTable.asLiteral().toString().equals(address.asLiteral().toString())) {
                            found = true;
                        }
                    }
                    if (found == false) {
                        throw new Exception(String.format("route table %s"
                                + "does not specify the main route in the model addition", table));
                    }
                }
                requests += String.format("CreateRouteTableRequest %s %s \n", vpcIdTagValue, tableIdTagValue);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to associate Route table with a subnet
     * ****************************************************************
     */
    private String associateTableRequest(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String tempRequests = "";
        String query;

        query = "SELECT ?route ?value WHERE {?route mrs:routeFrom ?routeFrom ."
                + "?routeFrom mrs:type \"subnet\" ."
                + "?routeFrom mrs:value ?value}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            boolean createRequest = true;
            QuerySolution querySolution = r.next();
            RDFNode value = querySolution.get("value");
            RDFNode route = querySolution.get("route");

            query = "SELECT ?table WHERE {?table mrs:hasRoute <" + route.asResource() + "> ."
                    + "?service mrs:providesRoute <" + route.asResource() + ">}";
            ResultSet r1 = executeQuery(query, model, modelAdd);
            if (!r1.hasNext()) {
                throw new Exception(String.format("Route  %s"
                        + "does not have a route table or is not"
                        + "being provided by a routing service in the model addition", route));
            }
            QuerySolution querySolution1 = r1.next();
            RDFNode table = querySolution1.get("table");

            String subnetIdTag = value.asLiteral().toString();
            String tableIdTag = table.asResource().toString().replace(topologyUri, "");

            RouteTable rt = ec2Client.getRoutingTable(getTableId(tableIdTag));
            if (rt == null) {
            } else {
                for (RouteTableAssociation as : rt.getAssociations()) {
                    if (as.getSubnetId().equals(getResourceId(subnetIdTag))) {
                        createRequest = false;
                    }
                }
            }

            String querySubnetIdTag = "\"" + subnetIdTag + "\"";
            if (createRequest == true) {
                //check that older routes in the table now include this new route
                //from network address
                query = "SELECT ?route WHERE {<" + table.asResource() + "> mrs:hasRoute ?route}";
                ResultSet r2 = executeQuery(query, model, emptyModel);
                while (r2.hasNext()) {
                    QuerySolution q2 = r2.next();
                    RDFNode ro = q2.get("route");
                    query = "SELECT ?routeFrom WHERE {<" + ro.asResource() + "> mrs:routeFrom ?routeFrom ."
                            + "?routeFrom mrs:value " + querySubnetIdTag + "}";
                    ResultSet r3 = executeQuery(query, emptyModel, modelAdd);
                    if (!r3.hasNext()) {
                        throw new Exception(String.format("Route  %s does state"
                                + "new association with subnet %s in the model addition", ro, subnetIdTag));
                    }
                }
                tempRequests = String.format("AssociateTableRequest %s %s \n", tableIdTag, subnetIdTag);
                if (!requests.contains(tempRequests))//dont include duplicate requests
                {
                    requests += tempRequests;
                }
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create an internet gateway and attach it to a vpc
     * ****************************************************************
     */
    private String createGatewayRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?igw WHERE {?igw a nml:BidirectionalPort ."
                + "?igw mrs:hasTag ?tag ."
                + "?tag mrs:type \"gateway\"}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode igw = q.get("igw");
            String idTag = igw.asResource().toString().replace(topologyUri, "");

            query = "SELECT ?tag WHERE {<" + igw.asResource() + "> mrs:hasTag ?tag}";
            ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
            if (!r1.hasNext()) {
                throw new Exception(String.format("Label for Internet gateway %s i"
                        + "s not specified in model addition", igw));
            }
            QuerySolution q1 = r1.next();
            RDFNode tag = q1.get("tag");

            //look for the lable in the reference model
            query = "SELECT ?value WHERE {<" + tag.asResource() + "> mrs:type \"gateway\" ."
                    + "<" + tag.asResource() + "> mrs:value ?value}";
            r1 = executeQuery(query, model, emptyModel);
            if (!r1.hasNext()) {
                throw new Exception(String.format("Tag %s for gateway %s is"
                        + " an invalid Tag", tag, igw));
            }
            q1 = r1.next();
            String value = q1.get("value").asLiteral().toString();
            if (value.equals("internet")) {
                if (ec2Client.getInternetGateway(getResourceId(idTag)) != null) {
                    throw new Exception(String.format("Internet gateway %s already exists", idTag));
                } else {
                    requests += String.format("CreateInternetGatewayRequest %s \n", idTag);
                }
            } else if (value.equals("vpn")) {
                if (ec2Client.getVirtualPrivateGateway(getResourceId(idTag)) != null) {
                    throw new Exception(String.format("VPN gateway %s already exists", idTag));
                } else {
                    requests += String.format("CreateVpnGatewayRequest %s \n", idTag);
                }
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a volumes from a model
     * ****************************************************************
     */
    private String attachGatewayRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query = "";

        //fin all the vpcs that have a bidirectional port
        query = "SELECT ?vpc ?port  WHERE {?vpc nml:hasBidirectionalPort ?port}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode gateway = q.get("port");
            RDFNode vpc = q.get("vpc");

            //check that vpc is the correct type
            query = "SELECT ?vpc WHERE {<" + vpc.asResource() + "> a nml:Topology}";
            ResultSet r1 = executeQuery(query, model, modelAdd);
            while (r1.hasNext()) {
                r1.next();
                query = "SELECT ?tag WHERE {<" + gateway.asResource() + "> mrs:hasTag ?tag}";
                ResultSet r2 = executeQuery(query, emptyModel, modelAdd);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("Tag for Internet gateway %s i"
                            + "s not specified in model addition", gateway));
                }
                QuerySolution q1 = r2.next();
                RDFNode tag = q1.get("tag");

                //look for the lable in the reference model
                query = "SELECT ?value WHERE {<" + tag.asResource() + "> mrs:type \"gateway\" ."
                        + "<" + tag.asResource() + "> mrs:value  ?value}";
                r2 = executeQuery(query, model, emptyModel);
                if (!r2.hasNext()) {
                    throw new Exception(String.format("Tag %s for gateway %s is"
                            + " an invalid tag", tag, gateway));
                }
                q1 = r2.next();
                String value = q1.get("value").asLiteral().toString();
                String gatewayIdTag = gateway.asResource().toString().replace(topologyUri, "");
                String vpcIdTag = vpc.asResource().toString().replace(topologyUri, "");
                if (value.equals("internet")) {
                    requests += String.format("AttachInternetGatewayRequest %s %s \n", gatewayIdTag, vpcIdTag);
                } else if (value.equals("vpn")) {
                    requests += String.format("AttachVpnGatewayRequest %s %s \n", gatewayIdTag, vpcIdTag);
                }

            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a volumes from a model
     * ****************************************************************
     */
    private String createRouteRequest(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String tempRequest = "";
        String query = "";

        query = "SELECT ?route ?table WHERE {?route a mrs:Route ."
                + "?table mrs:hasRoute ?route}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode table = q.get("table");
            RDFNode route = q.get("route");

            //check that is not main route
            //if it is the main route dont 
            query = "SELECT ?value WHERE {<" + route.asResource() + "> mrs:routeFrom ?routeFrom ."
                    + "?routeFrom mrs:type \"subnet\" ."
                    + "?routeFrom mrs:value ?value}";
            ResultSet r2 = executeQuery(query, emptyModel, modelAdd);
            if (!r2.hasNext()) {
                continue;
            }

            String tableIdTag = table.asResource().toString().replace(topologyUri, "");
            RouteTable rTable = ec2Client.getRoutingTable(getTableId(tableIdTag));
            if (rTable == null) {
                throw new Exception(String.format("Route Table %s where"
                        + " route %s will be inserted does not exist ", tableIdTag, route));
            }

            //make sure the new route will have all the source subnets as the table already has
            List<String> tableSubnets = new ArrayList();
            List<String> subnets = new ArrayList();
            for (RouteTableAssociation as : rTable.getAssociations()) {
                String subnetId = as.getSubnetId();
                String subnetIdTag = ec2Client.getIdTag(subnetId);
                tableSubnets.add(subnetIdTag);
            }

            while (r2.hasNext()) {
                QuerySolution q2 = r2.next();
                RDFNode value = q2.get("value");
                subnets.add(value.asLiteral().toString());
            }
            Collections.sort(subnets);
            Collections.sort(tableSubnets);
            if (!subnets.equals(tableSubnets)) {
                throw new Exception(String.format("Discrepancy between source destinations between"
                        + " reference model and new route %s", route));
            }
            query = "SELECT  ?nextHop ?value WHERE {<" + route.asResource() + "> mrs:routeTo ?routeTo ."
                    + "<" + route.asResource() + "> mrs:nextHop ?nextHop ."
                    + "?routeFrom mrs:type \"subnet\" ."
                    + "?routeTo mrs:value ?value}";
            r2 = executeQuery(query, emptyModel, modelAdd);
            while (r2.hasNext()) {
                QuerySolution q2 = r2.next();
                RDFNode value = q2.get("value");
                RDFNode nextHop = q2.get("nextHop");
                String destination = value.asLiteral().toString();
                String target;
                String gatewayId;
                if (nextHop.isLiteral()) {
                    target = nextHop.asLiteral().toString();
                    gatewayId = target;
                } else {
                    target = nextHop.asResource().toString().replace(topologyUri, "");
                    gatewayId = getResourceId(target);
                }

                Route rou = new Route();
                rou.withDestinationCidrBlock(destination)
                        .withGatewayId(gatewayId)
                        .withState(RouteState.Active)
                        .withOrigin(RouteOrigin.CreateRoute);

                if (!rTable.getRoutes().contains(rou)) {
                    tempRequest = String.format("CreateRouteRequest %s %s %s \n", tableIdTag, destination, target);
                } else {
                    throw new Exception(String.format("Route %s already exist in the route table %s", route, table));
                }

                if (!requests.contains(tempRequest)) {
                    requests += tempRequest;
                }

            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a volumes from a model
     * ****************************************************************
     */
    private String createVolumesRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeIdTagValue = volume.asResource().toString().replace(topologyUri, "");
            String volumeId = getVolumeId(volumeIdTagValue);

            Volume v = ec2Client.getVolume(volumeId);

            if (v != null) //volume exists, no need to create a volume
            {
                throw new Exception(String.format("Volume %s already exists", volumeIdTagValue));
            } else {
                //check what service is providing the volume
                query = "SELECT ?type WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify service that provides volume: %s", volume));
                }

                //find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new type of volume: %s", volume));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify new size of volume: %s", volume));
                }
                querySolution1 = r1.next();
                RDFNode size = querySolution1.get("size");

                //dont create the volumes with no target device and that are attached to an instance
                //since this volumes are the root devices
                query = "SELECT ?deviceName WHERE {<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                r1 = executeQuery(query, model, modelAdd);
                if (r1.hasNext()) {
                    querySolution1 = r1.next();
                    String deviceName = querySolution1.get("deviceName").asLiteral().toString();

                    if (!deviceName.equals("/dev/sda1") && !deviceName.equals("/dev/xvda")) {
                        requests += String.format("CreateVolumeRequest %s %s %s  %s  \n", type.asLiteral().getString(),
                                size.asLiteral().getString(), region.getName() + "e", volumeIdTagValue);
                    }
                } else {
                    requests += String.format("CreateVolumeRequest %s %s %s %s  \n", type.asLiteral().getString(),
                            size.asLiteral().getString(), Regions.US_EAST_1.getName() + "e", volumeIdTagValue);
                }
            }
        }

        return requests;
    }

    /**
     * ****************************************************************
     * Function to create network interfaces from a model
     * ****************************************************************
     */
    private String createPortsRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        //get the tag resource from the reference model that indicates 
        //that this is a network  interface 
        query = "SELECT ?tag WHERE {?tag mrs:type \"interface\" ."
                + "?tag mrs:value \"network\"}";
        ResultSet r = executeQuery(query, model, emptyModel);
        if (!r.hasNext()) {
            throw new Exception(String.format("Reference model has no tags for network"
                    + "interfaces"));
        }
        QuerySolution q = r.next();
        RDFNode tag = q.get("tag");

        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort ."
                + "?port  mrs:hasTag <" + tag.asResource() + ">}";
        r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portIdTagValue = port.asResource().toString().replace(topologyUri, "");
            String portId = getResourceId(portIdTagValue);

            NetworkInterface p = ec2Client.getNetworkInterface(portId);

            if (p != null) //network interface  exists, no need to create a network interface
            {
                throw new Exception(String.format("Network interface %s already exists", portIdTagValue));
            } else {
                //to get the private ip of the network interface
                query = "SELECT ?address WHERE {<" + port.asResource() + ">  mrs:privateIpAddress  ?address}";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify privat ip address of port: %s", port));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");
                String privateAddress = address.asResource().toString().replace(topologyUri, "");

                //find the subnet that has the port previously found
                query = "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify network interface subnet of port: %s", port));
                }
                String subnetId = null;
                while (r1.hasNext()) {
                    querySolution1 = r1.next();
                    RDFNode subnet = querySolution1.get("subnet");
                    query = "SELECT ?subnet WHERE {<" + subnet.asResource() + ">  a  mrs:SwitchingSubnet}";
                    ResultSet r3 = executeQuery(query, model, modelAdd);
                    while (r3.hasNext()) //search in the model to see if subnet existed before
                    {
                        subnetId = subnet.asResource().toString().replace(topologyUri, "");
                        subnetId = getResourceId(subnetId);
                        break;
                    }
                }
                if (subnetId == null) {
                    throw new Exception(String.format("model additions subnet for port %s"
                            + "is not found in the reference model", subnetId));
                }
                //create the network interface 
                requests += String.format("CreateNetworkInterfaceRequest  %s %s %s \n", privateAddress, subnetId, portIdTagValue);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to associate an address with a network interface
     * ****************************************************************
     */
    private String associateAddressRequest(OntModel model, OntModel modelAdd) throws Exception {
        //to get the public ip address of the network interface if any
        String requests = "";
        String query = "SELECT  ?port ?address WHERE {?port  mrs:hasNetworkAddress ?address ."
                + "?address mrs:type \"ipv4:public\"}";
        ResultSet r1 = executeQuery(query, model, modelAdd);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode address = querySolution1.get("address");
            RDFNode port = querySolution1.get(("port"));
            String portIdTagValue = port.asResource().toString().replace(topologyUri, "");
            query = "SELECT ?value WHERE {<" + address.asResource() + ">  mrs:value  ?value}";
            ResultSet r2 = executeQuery(query, model, modelAdd);
            if (!r2.hasNext()) {
                throw new Exception(String.format("model additions network addres  %s for port $s"
                        + "is not found in the reference mode", address, port));
            }
            QuerySolution querySolution2 = r2.next();
            RDFNode value = querySolution2.get("value");
            String publicAddress = value.asLiteral().toString().replace(topologyUri, "");
            requests += "AssociateAddressRequest " + publicAddress + " " + portIdTagValue
                    + " \n";
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create Instances
     * ****************************************************************
     */
    private String createInstancesRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";
        String query;

        query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");
            String nodeIdTagValue = node.asResource().toString().replace(topologyUri, "");
            String nodeId = getInstanceId(nodeIdTagValue);

            Instance instance = ec2Client.getInstance(nodeId);
            List<Instance> i = ec2Client.getEc2Instances();
            if (instance != null) //instance does not be to be created
            {
                throw new Exception(String.format("Instance %s already exists", nodeIdTagValue));
            } else {
                //check what service is providing the instance
                query = "SELECT ?service WHERE {?service mrs:providesVM <" + node.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify service that provides Instance: %s", node));
                }

                //find the Vpc that the node will be in
                query = "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() + ">}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new Exception(String.format("model addition does not specify the Vpc of the node: %s", node));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode vpc = querySolution1.get("vpc");
                String vpcId = vpc.asResource().toString().replace(topologyUri, "");
                vpcId = getResourceId(vpcId);

                //to find the subnet the node is in first  find the port the node uses
                query = "SELECT ?port WHERE {<" + node.asResource() + "> nml:hasBidirectionalPort ?port}";
                ResultSet r2 = executeQuery(query, model, modelAdd);
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
                ResultSet r4 = executeQuery(query, model, modelAdd);
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
                r4 = executeQuery(query, model, modelAdd);
                boolean hasRootVolume = false;
                while (r4.hasNext()) {
                    QuerySolution querySolution4 = r4.next();
                    RDFNode volume = querySolution4.get("volume");
                    String volumeTag = volume.asResource().toString().replace(topologyUri, "");
                    String type = querySolution4.get("type").asLiteral().toString();
                    String size = querySolution4.get("size").asLiteral().toString();
                    String deviceName = querySolution4.get("deviceName").asLiteral().toString();
                    if (deviceName.equals("/dev/sda1") || deviceName.equals("/dev/xvda")) {
                        hasRootVolume = true;
                        requests += String.format("%s %s %s %s ", type, size, deviceName, volumeTag);
                    }
                }
                if (hasRootVolume == false) {
                    throw new Exception(String.format("model addition does not specify root volume for node: %s", node));
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

    /**
     * ****************************************************************
     * Attach a volume to an existing instance AWS
     * ****************************************************************
     */
    private String attachVolumeRequests(OntModel model, OntModel modelAdd) throws Exception {
        String requests = "";

        //check if the volume is new therefore it should be in the model additiom
        String query = "SELECT  ?node ?volume  WHERE {?node  mrs:hasVolume  ?volume}";

        ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode node = querySolution1.get("node");
            String nodeTagId = node.asResource().toString().replace(topologyUri, "");
            RDFNode volume = querySolution1.get("volume");
            String volumeTagId = volume.asResource().toString().replace(topologyUri, "");
            String volumeId = getVolumeId(volumeTagId);

            query = "SELECT ?deviceName WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName}";
            ResultSet r2 = executeQuery(query, model, modelAdd);
            if (!r2.hasNext()) {
                throw new Exception(String.format("volume device name is not specified for volume %s in the model addition", volume));
            }

            QuerySolution querySolution2 = r2.next();

            RDFNode deviceName = querySolution2.get("deviceName");
            String device = deviceName.asLiteral().toString();

            if (!device.equals("/dev/sda1") && !device.equals("/dev/xvda")) {
                String nodeId = getInstanceId(nodeTagId);

                Instance ins = ec2Client.getInstance(nodeId);
                Volume vol = ec2Client.getVolume(volumeId);
                if (vol == null) {
                    query = "SELECT ?deviceName ?size ?type WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                    r2 = executeQuery(query, model, modelAdd);
                    if (!r2.hasNext()) {
                        throw new Exception(String.format("volume %s is not well specified in volume addition", volume));
                    }
                }
                if (ins != null) {
                    List< InstanceBlockDeviceMapping> map = ins.getBlockDeviceMappings();
                    boolean attach = true;
                    if (vol == null) {
                        requests += String.format("AttachVolumeRequest %s %s %s \n", nodeId, volumeTagId, device);
                    } else {
                        for (InstanceBlockDeviceMapping m : map) {
                            String instanceVolumeId = m.getEbs().getVolumeId();
                            if (instanceVolumeId.equals(volumeId)) {
                                attach = false;
                                break;
                            }
                        }
                        if (attach == true) {
                            requests += String.format("AttachVolumeRequest %s %s %s \n", nodeId, volumeTagId, device);
                        }
                    }
                } else if (ins == null) {
                    requests += String.format("AttachVolumeRequest %s %s %s \n", nodeTagId, volumeTagId, device);
                }
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * //function that checks if the id provided is a tag; if it is, it will
     * return //the resource id, otherwise it will return the input parameter
     * which is the //id of the resource //NOTE: does not work with volumes and
     * instances because sometimes the tags // persist for a while in this
     * resources, therefore we need to check if the // instance or volume
     * actually exists
     * ****************************************************************
     */
    private String getResourceId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            return descriptions.get(descriptions.size() - 1).getResourceId(); //get the last resource tagged with this id 
        }

        return tag;
    }

    /**
     * ****************************************************************
     * //function to get the Id from and instance tag
     * ****************************************************************
     */
    private String getInstanceId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            for (TagDescription des : descriptions) {
                Instance i = ec2Client.getInstance(des.getResourceId());
                if (i != null && i.getState().getCode() != 48) //check if the instance was not deleted
                {
                    return des.getResourceId();
                }
            }
        }
        return tag;
    }

    /**
     * ****************************************************************
     * //function to get the Id from and volume tag
     * ****************************************************************
     */
    private String getVolumeId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            for (TagDescription des : descriptions) {
                Volume vol = ec2Client.getVolume(des.getResourceId());
                if (vol != null && !vol.getState().equals("deleted")) {
                    return des.getResourceId();
                }
            }
        }
        return tag;
    }

    /**
     * ****************************************************************
     * //function to get the Id from a volume tag
     * ****************************************************************
     */
    private String getTableId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = ec2Client.getClient().describeTags(tagRequest).getTags();
        if (!descriptions.isEmpty()) {
            for (TagDescription des : descriptions) {
                RouteTable table = ec2Client.getRoutingTable(des.getResourceId());
                if (table != null) {
                    return des.getResourceId();
                }
            }
        }
        return tag;
    }
}
