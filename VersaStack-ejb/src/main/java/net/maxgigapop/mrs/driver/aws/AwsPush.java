/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2015-2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */
package net.maxgigapop.mrs.driver.aws;

import net.maxgigapop.mrs.driver.aws.AwsDCGet;
import net.maxgigapop.mrs.driver.aws.AwsEC2Get;
import net.maxgigapop.mrs.driver.aws.AwsPush;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.directconnect.AmazonDirectConnectClient;
import com.amazonaws.services.directconnect.model.ConfirmPrivateVirtualInterfaceRequest;
import com.amazonaws.services.directconnect.model.ConfirmPrivateVirtualInterfaceResult;
import com.amazonaws.services.directconnect.model.DeleteVirtualInterfaceRequest;
import com.amazonaws.services.directconnect.model.DeleteVirtualInterfaceResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
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
import com.hp.hpl.jena.rdf.model.Resource;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.ResourceTool;

/**
 *
 * @author muzcategui
 */
//TODO availability zone problems in volumes and subnets and instancees.
//add a property in the model to speicfy availability zone.
//TODO associate and disassociate address methods do not do anything. Reason is
//elastic IPs are not linked in any way to the root topology, find a way to do this
//in the model to make the two methods work.
public class AwsPush {

    private AmazonEC2Client ec2 = null;
    private AmazonDirectConnectClient dc = null;
    private AwsEC2Get ec2Client = null;
    private AwsDCGet dcClient = null;
    private String topologyUri = null;
    private Regions region = null;
    private AwsBatchResourcesTool batchTool = new AwsBatchResourcesTool();
    static final Logger logger = Logger.getLogger(AwsPush.class.getName());
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

    public AwsPush(String access_key_id, String secret_access_key, Regions region, String topologyUri) {
        //have all the information regarding the topology
        ec2Client = new AwsEC2Get(access_key_id, secret_access_key, region);
        dcClient = new AwsDCGet(access_key_id, secret_access_key, region);
        ec2 = ec2Client.getClient();
        dc = dcClient.getClient();
        this.region = region;

        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
    }

    /**
     * ***********************************************
     * function to propagate all the requests
     * ************************************************
     */
    public String pushPropagate(String modelRefTtl, String modelAddTtl, String modelReductTtl) {
        String requests = "";

        OntModel modelRef;
        OntModel modelAdd;
        OntModel modelReduct;
        try {
            modelRef = ModelUtil.unmarshalOntModel(modelRefTtl);
            modelAdd = ModelUtil.unmarshalOntModel(modelAddTtl);
            modelReduct = ModelUtil.unmarshalOntModel(modelReductTtl);

            //add the batched resources
            modelRef = batchTool.expandBatchAbstraction(modelRef);
            modelAdd = batchTool.expandBatchAbstraction(modelAdd);
            modelReduct = batchTool.expandBatchAbstraction(modelReduct);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }

        //deatch volumes that need to be detached
        requests += detachVolumeRequests(modelRef, modelReduct);

        //Delete a volume if a volume needs to be created
        requests += deleteVolumesRequests(modelRef, modelReduct);

        //delete all the instances that need to be created
        requests += deleteInstancesRequests(modelRef, modelReduct);

        //detach a network interface from an existing instance
        requests += detachPortRequest(modelRef, modelReduct);

        //disassociate an address from a network interface
        //requests += disassociateAddressRequest(modelRef, modelReduct);
        //Delete the network interfaces that need to be deleted
        requests += deletePortsRequests(modelRef, modelReduct);

        //Delete routes that need to be deleted
        requests += deleteRouteRequests(modelRef, modelReduct);

        //delete a virtual interface from a gateway
        requests += deleteVirtualInterfaceRequests(modelRef, modelReduct);

        //detach vpn gateway to VPC
        requests += detachVPNGatewayRequests(modelRef, modelReduct);

        //delete gateways that need to be deleated
        requests += deleteGatewayRequests(modelRef, modelReduct);

        //disassociate route tabes from subnets
        requests += disassociateTableRequests(modelRef, modelReduct);

        //delete a route table 
        requests += deleteRouteTableRequests(modelRef, modelReduct);

        //delete subnets that need to deleted
        requests += deleteSubnetsRequests(modelRef, modelReduct);

        //delete the Vpcs that need to be deleted
        requests += deleteVpcsRequests(modelRef, modelReduct);

        //create all the vpcs that need to be created
        requests += createVpcsRequests(modelRef, modelAdd);

        //create all the subnets that need to be created
        requests += createSubnetsRequests(modelRef, modelAdd);

        //create all the routeTables that need to be created
        requests += createRouteTableRequests(modelRef, modelAdd);

        //create the associations of route tables
        requests += associateTableRequest(modelRef, modelAdd);

        //create gateways request 
        requests += createGatewayRequests(modelRef, modelAdd);

        //attach vpn gateway to VPC
        requests += attachVPNGatewayRequests(modelRef, modelAdd);

        //acccept/reject a virtual interface for direct connect
        requests += acceptRejectVirtualInterfaceRequests(modelRef, modelAdd);

        //create the new routes requests
        requests += createRouteRequests(modelRef, modelAdd);

        //create a volume if a volume needs to be created
        requests += createVolumesRequests(modelRef, modelAdd);

        //create network interface if it needs to be created
        requests += createPortsRequests(modelRef, modelAdd);

        //Associate an address with a  interface
        //requests += associateAddressRequest(modelRef, modelAdd);
        //attach ports to existing instances
        requests += attachPortRequest(modelRef, modelAdd);

        //create all the nodes that need to be created 
        requests += createInstancesRequests(modelRef, modelAdd);

        //attach volumes that need to be atatched to existing instances
        requests += attachVolumeRequests(modelRef, modelAdd);

        //associate elastic IP 
        requests += associateElasticIpRequests(modelRef, modelAdd);
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

                String instanceId = ec2Client.getInstanceId(parameters[1]);
                TerminateInstancesRequest del = new TerminateInstancesRequest();
                del.withInstanceIds(instanceId);
                DeleteTagsRequest tagRequest = new DeleteTagsRequest();
                ec2.terminateInstances(del);
                ec2Client.getEc2Instances().remove(ec2Client.getInstance(instanceId));
                ec2Client.instanceStatusCheck(parameters[1], "terminated");

            } else if (request.contains("DetachNetworkInterfaceRequest")) {
                String[] parameters = request.split("\\s+");

                String attachmentId = parameters[1];
                DetachNetworkInterfaceRequest portRequest = new DetachNetworkInterfaceRequest();
                portRequest.withAttachmentId(attachmentId);
                ec2.detachNetworkInterface(portRequest);
                ec2Client.PortDetachmentCheck(parameters[2]);

            } else if (request.contains("DetachVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                DetachVolumeRequest volumeRequest = new DetachVolumeRequest();

                volumeRequest.withVolumeId(parameters[1])
                        .withInstanceId(parameters[2]);
                ec2.detachVolume(volumeRequest);
                ec2Client.volumeDetachmentCheck(parameters[1]);

            } else if (request.contains("DisassociateAddressRequest")) {
                String[] parameters = request.split("\\s+");

                Address publicIp = ec2Client.getElasticIp(parameters[1]);
                DisassociateAddressRequest disassociateAddressRequest = new DisassociateAddressRequest();
                disassociateAddressRequest.withAssociationId(publicIp.getAssociationId())
                        .withPublicIp(publicIp.toString());
                ec2.disassociateAddress(disassociateAddressRequest);

            } else if (request.contains("DeleteNetworkInterfaceRequest")) {
                String[] parameters = request.split("\\s+");

                DeleteNetworkInterfaceRequest portRequest = new DeleteNetworkInterfaceRequest();
                portRequest.withNetworkInterfaceId(parameters[1]);
                ec2.deleteNetworkInterface(portRequest);
                ec2Client.getNetworkInterfaces().remove(ec2Client.getNetworkInterface(parameters[1]));
                ec2Client.PortDeletionCheck(parameters[1]);

            } else if (request.contains("DeleteVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                DeleteVolumeRequest volumeRequest = new DeleteVolumeRequest();
                volumeRequest.withVolumeId(parameters[1]);
                ec2.deleteVolume(volumeRequest);
                ec2Client.getVolumes().remove(ec2Client.getVolume(parameters[1]));
                ec2Client.volumeDeletionCheck(parameters[1], "deleted");

            } else if (request.contains("DeleteRouteRequest")) {
                String[] parameters = request.split("\\s+");

                String tableIdTag = parameters[1];
                String target = ec2Client.getResourceId(parameters[3]);
                RouteTable t = ec2Client.getRoutingTable(ec2Client.getTableId(tableIdTag));
                Vpc v = ec2Client.getVpc(t.getVpcId());

                Route route = new Route();
                route.withDestinationCidrBlock(parameters[2])
                        .withGatewayId(target)
                        .withState(RouteState.Active)
                        .withOrigin(RouteOrigin.CreateRoute);

                if (!t.getRoutes().contains(route) || parameters[2].equals(v.getCidrBlock())) {
                } else {
                    DeleteRouteRequest routeRequest = new DeleteRouteRequest();
                    routeRequest.withRouteTableId(t.getRouteTableId())
                            .withDestinationCidrBlock(parameters[2]);
                    ec2.deleteRoute(routeRequest);
                }

            } else if (request.contains("DeleteSubnetRequest")) {
                String[] parameters = request.split("\\s+");

                Subnet subnet = ec2Client.getSubnet(parameters[1]);
                DeleteSubnetRequest subnetRequest = new DeleteSubnetRequest();
                subnetRequest.withSubnetId(subnet.getSubnetId());
                ec2.deleteSubnet(subnetRequest);

                ec2Client.getSubnets().remove(subnet);
                ec2Client.subnetDeletionCheck(subnet.getSubnetId(), SubnetState.Available.name());

            } else if (request.contains("DeleteVpcRequest")) {
                String[] parameters = request.split("\\s+");

                Vpc vpc = ec2Client.getVpc(parameters[1]);
                DeleteVpcRequest vpcRequest = new DeleteVpcRequest();
                vpcRequest.withVpcId(vpc.getVpcId());
                int tries = 0;
                // delete added security groups in the VPC (all but 'default')
                DescribeSecurityGroupsResult securityGroupsResult = ec2.describeSecurityGroups();
                List<SecurityGroup> listSecGroups = securityGroupsResult.getSecurityGroups();
                for (SecurityGroup sg: listSecGroups){
                    if (sg.getVpcId() != null 
                            && sg.getVpcId().equals(vpcRequest.getVpcId()) 
                            && !sg.getGroupName().equals("default")) {
                        DeleteSecurityGroupRequest deleteSecGroupRequst = new DeleteSecurityGroupRequest()
                                .withGroupId(sg.getGroupId());
                        ec2.deleteSecurityGroup(deleteSecGroupRequst);
                    }
                }
                while (true) {
                    try {
                        ec2.deleteVpc(vpcRequest);
                    } catch (AmazonServiceException e) {
                        try {
                            Thread.sleep(60000);  // sleep 60 secs
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        tries++;
                        if (tries > 10) {
                            break;
                        }
                        continue;
                    }
                    break;
                }

                ec2Client.getVpcs().remove(vpc);

            } else if (request.contains("DeleteInternetGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                InternetGateway gateway = ec2Client.getInternetGateway(ec2Client.getResourceId(parameters[1]));
                Vpc v = ec2Client.getVpc(ec2Client.getVpcId(parameters[2]));
                DetachInternetGatewayRequest gwRequest = new DetachInternetGatewayRequest();
                gwRequest.withInternetGatewayId(ec2Client.getResourceId(parameters[1]))
                        .withVpcId(ec2Client.getVpcId(parameters[2]));

                ec2.detachInternetGateway(gwRequest);
                ec2Client.internetGatewayDetachmentCheck(ec2Client.getResourceId(parameters[1]));

                DeleteInternetGatewayRequest gatewayRequest = new DeleteInternetGatewayRequest();
                gatewayRequest.withInternetGatewayId(gateway.getInternetGatewayId());
                ec2.deleteInternetGateway(gatewayRequest);

                ec2Client.getInternetGateways().remove(gateway);
                ec2Client.internetGatewayDeletionCheck(gateway.getInternetGatewayId());

            } else if (request.contains("DeleteVirtualInterface")) {
                String[] parameters = request.split("\\s+");
                // if parameters[1] is an integer map it into dxvif id
                String virtualInterfaceId = parameters[1];
                if (Character.isDigit(virtualInterfaceId.charAt(0))) {
                    virtualInterfaceId = dcClient.getVirtualInterfaceByVlan(virtualInterfaceId);
                }
                if (virtualInterfaceId == null || !virtualInterfaceId.startsWith("dxvif")) {
                    throw new EJBException(String.format("unrecognized virtualInterface ID or VLAN %s", parameters[1])); 
                }
                DeleteVirtualInterfaceRequest interfaceRequest = new DeleteVirtualInterfaceRequest();
                interfaceRequest.withVirtualInterfaceId(virtualInterfaceId);

                DeleteVirtualInterfaceResult interfaceResult = dc.deleteVirtualInterface(interfaceRequest);
                dcClient.dxvifDeletionCheck(interfaceRequest.getVirtualInterfaceId());                
            } else if (request.contains("DeleteVpnGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                VpnGateway gateway = ec2Client.getVirtualPrivateGateway(ec2Client.getVpnGatewayId(parameters[1]));

                DeleteVpnGatewayRequest gatewayRequest = new DeleteVpnGatewayRequest();
                gatewayRequest.withVpnGatewayId(gateway.getVpnGatewayId());
                boolean deleteExecuted = false;
                for (int i = 0; i < 10; i++) {
                    try {
                        ec2.deleteVpnGateway(gatewayRequest);
                        deleteExecuted = true;
                        break;
                    } catch (AmazonServiceException | NullPointerException e) {
                        try {
                            Thread.sleep(30000L); // sleep 30 secs
                        } catch (InterruptedException ex) {
                            ;
                        }
                        continue;
                    }
                }
                if (deleteExecuted) {
                    ec2Client.getVirtualPrivateGateways().remove(gateway);
                    ec2Client.vpnGatewayDeletionCheck(gateway.getVpnGatewayId());
                }

            } else if (request.contains("detachVpnGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                VpnGateway gateway = ec2Client.getVirtualPrivateGateway(ec2Client.getVpnGatewayId(parameters[1]));
                Vpc v = ec2Client.getVpc(ec2Client.getVpcId(parameters[2]));
                DetachVpnGatewayRequest gwRequest = new DetachVpnGatewayRequest();
                gwRequest.withVpnGatewayId(gateway.getVpnGatewayId())
                        .withVpcId(v.getVpcId());
                
                // Somehow VGW may disappear from the API for a while after deleting dxvif. Trick is to detach a few more times.
                boolean detachExecuted = false;
                for (int i = 0; i < 10; i++) {
                    try {
                        ec2.detachVpnGateway(gwRequest);
                        detachExecuted = true;
                        break;
                    } catch (AmazonServiceException | NullPointerException e) {
                        try {
                            Thread.sleep(30000L); // sleep 30 secs
                        } catch (InterruptedException ex) {
                            ;
                        }
                        continue;
                    }
                }
                if (detachExecuted) {
                    ec2Client.vpnGatewayDetachmentCheck(gateway.getVpnGatewayId(), v.getVpcId());
                }
            } else if (request.contains("DisassociateTableRequest")) {
                String[] parameters = request.split("\\s+");

                DisassociateRouteTableRequest tableRequest = new DisassociateRouteTableRequest();
                tableRequest.withAssociationId(parameters[1]);
                ec2.disassociateRouteTable(tableRequest);

            } else if (request.contains("DeleteRouteTableRequest")) {
                String[] parameters = request.split("\\s+");

                RouteTable table = ec2Client.getRoutingTable(parameters[1]);
                DeleteRouteTableRequest tableRequest = new DeleteRouteTableRequest();
                tableRequest.withRouteTableId(table.getRouteTableId());
                ec2.deleteRouteTable(tableRequest);

                ec2Client.getRoutingTables().remove(table);
                ec2Client.RouteTableDeletionCheck(table.getRouteTableId());

            } else if (request.contains("CreateVpcRequest")) {
                String[] parameters = request.split("\\s+");

                CreateVpcRequest vpcRequest = new CreateVpcRequest();
                vpcRequest.withCidrBlock(parameters[1]);
                CreateVpcResult vpcResult = ec2.createVpc(vpcRequest);
                String vpcId = vpcResult.getVpc().getVpcId();
                ec2Client.getVpcs().add(vpcResult.getVpc());
                ec2Client.vpcStatusCheck(vpcId, VpcState.Available.name().toLowerCase());
                //tag the routing table of the 
                RouteTable mainTable = null;
                for (int retry = 0; retry < 12; retry++) {
                    DescribeRouteTablesResult tablesResult = this.ec2.describeRouteTables();
                    for (RouteTable tb : tablesResult.getRouteTables()) {
                        if (tb.getVpcId().equals(vpcId)) {
                            mainTable = tb;
                        }
                    }
                    if (mainTable != null) {
                        break;
                    }
                    try {
                        Thread.sleep(10000L);
                    } catch (InterruptedException ex) {
                        ;
                    }
                }
                ec2Client.getRoutingTables().add(mainTable);
                //create the tag for the vpc
                ec2Client.tagResource(vpcId, parameters[2]);
                ec2Client.tagResource(mainTable.getRouteTableId(), parameters[3]);
            } else if (request.contains("CreateSubnetRequest")) {
                String[] parameters = request.split("\\s+");

                CreateSubnetRequest subnetRequest = new CreateSubnetRequest();
                subnetRequest.withVpcId(ec2Client.getVpcId(parameters[1]))
                        .withCidrBlock(parameters[2])
                        .withAvailabilityZone(Regions.US_EAST_1.getName() + "e");

                CreateSubnetResult subnetResult = ec2.createSubnet(subnetRequest);
                ec2Client.getSubnets().add(subnetResult.getSubnet());
                ec2Client.subnetCreationCheck(subnetResult.getSubnet().getSubnetId(), SubnetState.Available.name().toLowerCase());
                ec2Client.tagResource(subnetResult.getSubnet().getSubnetId(), parameters[3]);
                
                if (parameters.length > 4 && parameters[4].equals("public")) {
                    ec2.modifySubnetAttribute(new ModifySubnetAttributeRequest()
                            .withSubnetId(subnetResult.getSubnet().getSubnetId())
                            .withMapPublicIpOnLaunch(true));
                }
            } else if (request.contains("CreateRouteTableReques")) {
                String[] parameters = request.split("\\s+");

                CreateRouteTableRequest tableRequest = new CreateRouteTableRequest();
                tableRequest.withVpcId(ec2Client.getVpcId(parameters[1]));
                CreateRouteTableResult tableResult = ec2.createRouteTable(tableRequest);

                ec2Client.getRoutingTables().add(tableResult.getRouteTable());
                ec2Client.RouteTableCreationCheck(tableResult.getRouteTable().getRouteTableId());
                ec2Client.tagResource(tableResult.getRouteTable().getRouteTableId(), parameters[2]);

            } else if (request.contains("AssociateTableRequest")) {
                String[] parameters = request.split("\\s+");

                String routeTableId = ec2Client.getTableId(parameters[1]);
                String subnetId = ec2Client.getResourceId(parameters[2]);
                for (int retry = 0; retry < 6; retry++) {
                    String resId = ec2Client.getResourceId(routeTableId);
                    if (routeTableId != resId) {
                        routeTableId = resId;
                        break;
                    }
                    try {
                        Thread.sleep(10000L);
                    } catch (InterruptedException ex) {
                        ;
                    }
                }
                AssociateRouteTableRequest associateRequest = new AssociateRouteTableRequest();
                associateRequest.withRouteTableId(routeTableId)
                        .withSubnetId(ec2Client.getResourceId(subnetId));
                AssociateRouteTableResult associateResult = ec2.associateRouteTable(associateRequest);

            } else if (request.contains("CreateInternetGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                CreateInternetGatewayResult igwResult = ec2.createInternetGateway();
                InternetGateway igw = igwResult.getInternetGateway();

                ec2Client.getInternetGateways().add(igw);
                ec2Client.internetGatewayAdditionCheck(igw.getInternetGatewayId());
                ec2Client.tagResource(igw.getInternetGatewayId(), parameters[1]);

                for (int retry = 0; retry < 3; retry++) {
                    try {
                        AttachInternetGatewayRequest gwRequest = new AttachInternetGatewayRequest();
                        gwRequest.withInternetGatewayId(ec2Client.getResourceId(parameters[1]))
                                .withVpcId(ec2Client.getVpcId(parameters[2]));

                        ec2.attachInternetGateway(gwRequest);
                        ec2Client.internetGatewayAttachmentCheck(ec2Client.getResourceId(parameters[1]));
                        break;
                    } catch (AmazonServiceException e) {
                        if (!e.getErrorCode().equals("InvalidInternetGatewayID.NotFound")) {
                            throw e;
                        }
                        try {
                            sleep(20000L); // pause for 20 seconds and retry
                        } catch (InterruptedException ex) {
                            ;
                        }
                    }
                }
            } else if (request.contains("CreateVpnGatewayRequest")) {
                String[] parameters = request.split("\\s+");

                CreateVpnGatewayRequest vpngwRequest = new CreateVpnGatewayRequest();
                vpngwRequest.withType(GatewayType.Ipsec1);
                CreateVpnGatewayResult vpngwResult = ec2.createVpnGateway(vpngwRequest);
                VpnGateway vpngw = vpngwResult.getVpnGateway();

                ec2Client.getVirtualPrivateGateways().add(vpngw);
                ec2Client.vpnGatewayAdditionCheck(vpngw.getVpnGatewayId());
                ec2Client.tagResource(vpngw.getVpnGatewayId(), parameters[1]);

            } else if (request.contains("AttachVpnGatewayRequest")) {
                String[] parameters = request.split("\\s+");
                for (int retry = 0; retry < 3; retry++) {
                    try {
                        VpnGateway vpn = ec2Client.getVirtualPrivateGateway(ec2Client.getVpnGatewayId(parameters[1]));
                        Vpc v = ec2Client.getVpc(ec2Client.getVpcId(parameters[2]));
                        if (!vpn.equals(ec2Client.getVirtualPrivateGateway(v))) {
                            AttachVpnGatewayRequest gwRequest = new AttachVpnGatewayRequest();
                            gwRequest.withVpnGatewayId(vpn.getVpnGatewayId())
                                    .withVpcId(v.getVpcId());

                            AttachVpnGatewayResult result = ec2.attachVpnGateway(gwRequest);
                            ec2Client.vpnGatewayAttachmentCheck(vpn.getVpnGatewayId(), v.getVpcId());
                        }
                        break;
                    } catch (NullPointerException nullEx) {
                        try {
                            sleep(20000L); // pause for 20 seconds and retry
                        } catch (InterruptedException ex) {
                            ;
                        }
                    }
                }
            } else if (request.contains("PropagateVpnRequest")) {
                String[] parameters = request.split("\\s+");
                String tableIdTag = parameters[1];
                VpnGateway vpn = ec2Client.getVirtualPrivateGateway(ec2Client.getVpnGatewayId(parameters[2]));
                RouteTable table = ec2Client.getRoutingTable(ec2Client.getTableId(tableIdTag));

                EnableVgwRoutePropagationRequest propagationRequest = new EnableVgwRoutePropagationRequest();
                propagationRequest.withGatewayId(vpn.getVpnGatewayId())
                        .withRouteTableId(table.getRouteTableId());
                ec2.enableVgwRoutePropagation(propagationRequest);

            } else if (request.contains("AcceptVirtualInterface")) {
                String[] parameters = request.split("\\s+");
                // if parameters[1] is an integer map it into dxvif id
                String virtualInterfaceId = parameters[1];
                if (Character.isDigit(virtualInterfaceId.charAt(0))) {
                    virtualInterfaceId = dcClient.getVirtualInterfaceByVlan(virtualInterfaceId);
                }
                if (virtualInterfaceId == null || !virtualInterfaceId.startsWith("dxvif")) {
                    throw new EJBException(String.format("unrecognized virtualInterface ID or VLAN %s", parameters[1])); 
                }
                ConfirmPrivateVirtualInterfaceRequest interfaceRequest = new ConfirmPrivateVirtualInterfaceRequest();
                interfaceRequest.withVirtualInterfaceId(virtualInterfaceId)
                        .withVirtualGatewayId(ec2Client.getVpnGatewayId(parameters[2]));

                ConfirmPrivateVirtualInterfaceResult interfaceResult = dc.confirmPrivateVirtualInterface(interfaceRequest);
            } else if (request.contains("CreateRouteRequest")) {
                String[] parameters = request.split("\\s+");

                String tableIdTag = parameters[1];
                String target = ec2Client.getResourceId(parameters[3]);
                RouteTable t = ec2Client.getRoutingTable(ec2Client.getTableId(tableIdTag));
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
                    try {
                        ec2.createRoute(routeRequest);
                    } catch (AmazonServiceException ex) {
                        if (ex.getErrorCode().equals("RouteAlreadyExists")) {
                            ; // never mind
                        }
                    }
                }

            } else if (request.contains("CreateVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                CreateVolumeRequest volumeRequest = new CreateVolumeRequest();
                volumeRequest.withVolumeType(parameters[1])
                        .withSize(Integer.parseInt(parameters[2]))
                        .withAvailabilityZone(parameters[3]);

                CreateVolumeResult result = ec2.createVolume(volumeRequest);

                Volume volume = result.getVolume();
                ec2Client.volumeAdditionCheck(volume.getVolumeId(), "available");
                ec2Client.tagResource(volume.getVolumeId(), parameters[4]);
                ec2Client.getVolumes().clear();
                ec2Client.getVolumes().addAll(ec2.describeVolumes().getVolumes());

                //suport for batch
                if (parameters[4].matches("(.*)(batch)(\\d{1,19}$)")) {
                    String batchTag = parameters[4].split("batch")[0] + "batch";
                    CreateTagsRequest tagRequest = new CreateTagsRequest();
                    tagRequest.withTags(new Tag("batch", batchTag));
                    tagRequest.withResources(volume.getVolumeId());
                    ec2.createTags(tagRequest);
                }

            } else if (request.contains("CreateNetworkInterfaceRequest")) {
                String[] parameters = request.split("\\s+");
                CreateNetworkInterfaceRequest portRequest = new CreateNetworkInterfaceRequest();
                if (parameters[1].equalsIgnoreCase("any")) {
                    portRequest.withSubnetId(ec2Client.getResourceId(parameters[2]));
                } else {
                    portRequest.withPrivateIpAddress(parameters[1])
                            .withSubnetId(ec2Client.getResourceId(parameters[2]));
                }
                CreateNetworkInterfaceResult portResult = ec2.createNetworkInterface(portRequest);

                NetworkInterface port = portResult.getNetworkInterface();
                ec2Client.getNetworkInterfaces().add(port);
                ec2Client.PortAdditionCheck(port.getNetworkInterfaceId());
                ec2Client.tagResource(port.getNetworkInterfaceId(), parameters[3]);

                //suport for batch
                if (parameters[3].matches("(.*)(batch)(\\d{1,19}$)")) {
                    String batchTag = parameters[3].split("batch")[0] + "batch";
                    CreateTagsRequest tagRequest = new CreateTagsRequest();
                    tagRequest.withTags(new Tag("batch", batchTag));
                    tagRequest.withResources(port.getNetworkInterfaceId());
                    ec2.createTags(tagRequest);
                }

            } else if (request.contains("AssociateAddressRequest")) {
                String[] parameters = request.split("\\s+");

                Address publicIp = ec2Client.getElasticIp(parameters[1]);
                AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest();
                associateAddressRequest.withAllocationId(publicIp.getAllocationId())
                        .withNetworkInterfaceId(ec2Client.getResourceId(parameters[2]));
                try {
                    ec2.associateAddress(associateAddressRequest);
                } catch (com.amazonaws.AmazonServiceException ex) {
                    if (ex.getErrorCode().startsWith("InvalidAddress")) {
                        logger.warning("Invalid Elastic IP address " + parameters[2] + " - not assocaited to Instance " + parameters[2]);
                    } // also drop other exception quietly
                }

            } else if (request.contains("AttachNetworkInterfaceRequest")) {
                String[] parameters = request.split("\\s+");

                String portId = ec2Client.getResourceId(parameters[1]);
                String nodeId = ec2Client.getInstanceId(parameters[2]);
                int index = Integer.parseInt(parameters[3]);
                AttachNetworkInterfaceRequest portRequest = new AttachNetworkInterfaceRequest();
                portRequest.withInstanceId(nodeId);
                portRequest.withNetworkInterfaceId(portId);
                portRequest.withDeviceIndex(index);
                ec2.attachNetworkInterface(portRequest);
                ec2Client.PortAttachmentCheck(portId);

            } else if (request.contains("RunInstancesRequest")) {
                //requests += String.format("RunInstancesRequest ami-146e2a7c t2.micro ") 
                //requests+=String.format("InstanceNetworkInterfaceSpecification %s %d",id,index)
                String[] parameters = request.split("\\s+");

                RunInstancesRequest runInstance = new RunInstancesRequest();
                runInstance.withImageId(parameters[1]);
                runInstance.withInstanceType(parameters[2]);
                runInstance.withKeyName(parameters[3]);
                runInstance.withMaxCount(1);
                runInstance.withMinCount(1);
                //runInstance.withSecurityGroupIds(parameters[4]);

                //integrate the root device
                if (!parameters[6].equalsIgnoreCase("any")) {
                    EbsBlockDevice device = new EbsBlockDevice();
                    device.withVolumeType(parameters[6]);
                    device.withVolumeSize(Integer.parseInt(parameters[7]));
                    BlockDeviceMapping mapping = new BlockDeviceMapping();
                    mapping.withDeviceName(parameters[8]);
                    mapping.withEbs(device);
                    String volumeTag = parameters[9];
                    runInstance.withBlockDeviceMappings(mapping);
                }

                List<InstanceNetworkInterfaceSpecification> portSpecification = new ArrayList();
                for (int i = 11; i < parameters.length; i++) {
                    InstanceNetworkInterfaceSpecification s = new InstanceNetworkInterfaceSpecification();
                    String netIfId = ec2Client.getResourceId(parameters[i]);
                    int retry = 0; // special handling for lagging interface name and tag
                    while (!netIfId.startsWith("eni-") && retry++ < 6) {
                        netIfId = ec2Client.getResourceId(parameters[i]);
                        try {
                            sleep(10000L);
                        } catch (InterruptedException ex) {
                            ;
                        }
                    }
                    s.withNetworkInterfaceId(netIfId);
                    i++;
                    s.withDeviceIndex(Integer.parseInt(parameters[i]));
                    portSpecification.add(s);
                }
                runInstance.withNetworkInterfaces(portSpecification);

                RunInstancesResult result = ec2.runInstances(runInstance);

                //tag the new instance
                Instance instance = result.getReservation().getInstances().get(0);
                ec2Client.getEc2Instances().add(instance);
                ec2Client.instanceStatusCheck(instance.getInstanceId(), "running");
                ec2Client.tagResource(instance.getInstanceId(), parameters[5]);
                //modify security group as it conflicts with specified newtork interface at launch
                SecurityGroup secGroup = ec2Client.getSecurityGroup(parameters[4]);
                if (secGroup != null && !secGroup.getGroupName().equals("default")) {
                    String secGroupName = instance.getVpcId() + '-' + secGroup.getGroupName();
                    String secGroupId = null;
                    DescribeSecurityGroupsResult securityGroupsResult = ec2.describeSecurityGroups();
                    List<SecurityGroup> listSecGroups = securityGroupsResult.getSecurityGroups();
                    for (SecurityGroup sg : listSecGroups) {
                        if (sg.getGroupName().equals(secGroupName)) {
                            secGroupId = sg.getGroupId();
                            break;
                        }
                    }
                    if (secGroupId == null) {
                        CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest()
                                .withGroupName(secGroupName)
                                .withVpcId(instance.getVpcId())
                                .withDescription(secGroup.getGroupName() + "-copy-to-" + instance.getVpcId());
                        CreateSecurityGroupResult csgResult = ec2.createSecurityGroup(csgr);
                        List<IpPermission> ingPermList = secGroup.getIpPermissions();
                        AuthorizeSecurityGroupIngressRequest asgir = new AuthorizeSecurityGroupIngressRequest()
                                .withGroupId(csgResult.getGroupId())
                                .withIpPermissions(ingPermList);
                        ec2.authorizeSecurityGroupIngress(asgir);
                        List<IpPermission> egrPermList = secGroup.getIpPermissionsEgress();
                        if (!egrPermList.isEmpty()) {
                            AuthorizeSecurityGroupEgressRequest asger = new AuthorizeSecurityGroupEgressRequest()
                                    .withGroupId(csgResult.getGroupId())
                                    .withIpPermissions(egrPermList);
                            try {
                                ec2.authorizeSecurityGroupEgress(asger);
                            } catch (com.amazonaws.AmazonServiceException ex) {
                                if (ex.getErrorCode().equals("InvalidPermission.Duplicate")) {
                                    ;
                                }
                            }
                        }
                        secGroupId = csgResult.getGroupId();
                    }
                    ec2.modifyInstanceAttribute((new ModifyInstanceAttributeRequest()
                            .withInstanceId(instance.getInstanceId())
                            .withGroups(secGroupId)));
                }
                //suport for batch
                if (parameters[5].matches("(.*)(batch)(\\d{1,19}$)")) {
                    String batchTag = parameters[5].split("batch")[0] + "batch";
                    CreateTagsRequest tagRequest = new CreateTagsRequest();
                    tagRequest.withTags(new Tag("batch", batchTag));
                    tagRequest.withResources(instance.getInstanceId());
                    ec2.createTags(tagRequest);
                }

                if (!parameters[6].equalsIgnoreCase("any")) {
                    String volumeTag = parameters[9];
                    Volume volume = ec2Client.getInstanceRootDevice(instance);
                    String volumeId = volume.getVolumeId();
                    ec2Client.tagResource(volumeId, volumeTag);
                    ec2Client.getVolumes().add(volume);

                    //suport for batch
                    if (volumeTag.matches("(.*)(batch)(\\d{1,19}$)")) {
                        String batchTag = volumeTag.split("batch")[0] + "batch";
                        CreateTagsRequest tagRequest = new CreateTagsRequest();
                        tagRequest.withTags(new Tag("batch", batchTag));
                        tagRequest.withResources(volumeId);
                        ec2.createTags(tagRequest);
                    }
                } else {
                    //there are two options here, either the volume is part of a batch or not
                    String volumeTag = "";
                    Volume volume = ec2Client.getInstanceRootDevice(instance);
                    String volumeId = volume.getVolumeId();
                    ec2Client.getVolumes().add(volume);

                    //case of batch instance
                    if (parameters[5].matches("(.*)(batch)(\\d{1,19}$)")) {
                        String split[] = parameters[5].split("batch");
                        volumeTag = split[0] + ":volume+rootbatch" + split[1];
                        ec2Client.tagResource(volumeId, volumeTag);
                        String batchTag = volumeTag.split("batch")[0] + "batch";
                        CreateTagsRequest tagRequest = new CreateTagsRequest();
                        tagRequest.withTags(new Tag("batch", batchTag));
                        tagRequest.withResources(volumeId);
                        ec2.createTags(tagRequest);
                    } else {
                        volumeTag = parameters[5] + ":volume+root";
                        ec2Client.tagResource(volumeId, volumeTag);
                    }
                }
            } else if (request.contains("AttachVolumeRequest")) {
                String[] parameters = request.split("\\s+");

                AttachVolumeRequest volumeRequest = new AttachVolumeRequest();
                volumeRequest.withInstanceId(ec2Client.getInstanceId(parameters[1]))
                        .withVolumeId(ec2Client.getVolumeId(parameters[2]))
                        .withDevice(parameters[3]);

                ec2.attachVolume(volumeRequest);
                ec2Client.volumeAttachmentCheck(ec2Client.getVolumeId(parameters[2]));
            } else if (request.contains("AssociateElasticIpRequest")) {
                String[] parameters = request.split("\\s+");

                AssociateAddressRequest elasticIpRequest = new AssociateAddressRequest();
                elasticIpRequest.withInstanceId(ec2Client.getInstanceId(parameters[1]))
                        .withPublicIp(parameters[2].split("/")[0]);
                ec2.associateAddress(elasticIpRequest);
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
     * function that executes a query using a model addition/subtraction and a
     * reference model, returns the result of the query
     * ****************************************************************
     */
    private ResultSet executeQueryUnion(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        Model unionModel = ModelFactory.createUnion(refModel, model);

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, unionModel);
        ResultSet r = qexec.execSelect();
        return r;
    }

    /**
     * ****************************************************************
     * Detach a volume to an existing instance AWS
     * ****************************************************************
     */
    private String detachVolumeRequests(OntModel model, OntModel modelReduct) {
        String requests = "";

        //check fornew association between intsnce and volume
        String query = "SELECT  ?node ?volume  WHERE {?node  mrs:hasVolume  ?volume}";

        ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode node = querySolution1.get("node");
            String nodeTagId = ResourceTool.getResourceName(node.asResource().toString(), AwsPrefix.instance);
            RDFNode volume = querySolution1.get("volume");
            String volumeTagId = ResourceTool.getResourceName(volume.asResource().toString(), AwsPrefix.volume);
            String volumeId = ec2Client.getVolumeId(volumeTagId);

            query = "SELECT ?deviceName WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName}";
            ResultSet r2 = executeQuery(query, model, modelReduct);
            if (!r2.hasNext()) {
                throw new EJBException(String.format("volume device name is not specified for volume %s", volume));
            }

            QuerySolution querySolution2 = r2.next();

            RDFNode deviceName = querySolution2.get("deviceName");
            String device = deviceName.asLiteral().toString();

            if (!device.equals("/dev/sda1") && !device.equals("/dev/xvda")) {
                String nodeId = ec2Client.getInstanceId(nodeTagId);

                Instance ins = ec2Client.getInstance(nodeId);
                Volume vol = ec2Client.getVolume(volumeId);
                if (vol == null) {
                    throw new EJBException(String.format("The volume %s to be deleted does not exist", volume));
                }
                if (ins == null) {
                    throw new EJBException(String.format("The node %s where the volume %s "
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
    private String disassociateAddressRequest(OntModel model, OntModel modelReduct) {
        //to get the public ip address of the network interface if any
        String requests = "";
        String query = "SELECT  ?port ?address WHERE {?port  mrs:hasNetworkAddress ?address}";
        ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode address = querySolution1.get("address");
            RDFNode port = querySolution1.get(("port"));
            String portIdTagValue = ResourceTool.getResourceName(port.asResource().toString(), AwsPrefix.nic);
            query = "SELECT  ?a WHERE {<" + address.asResource() + ">  mrs:type \"ipv4:public\"}";
            ResultSet r2 = executeQuery(query, model, modelReduct);
            if (r2.hasNext()) {
                query = "SELECT ?value WHERE {<" + address.asResource() + ">  mrs:value  ?value}";
                r2 = executeQuery(query, model, modelReduct);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("model additions network addres  %s for port $s"
                            + "is not found in the reference mode", address, port));
                }
                QuerySolution querySolution2 = r2.next();
                RDFNode value = querySolution2.get("value");
                String publicAddress = ResourceTool.getResourceName(value.asLiteral().toString(), "public-ip-");
                requests += "disassociateAddressRequest " + publicAddress + " " + portIdTagValue
                        + " \n";
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to delete a network interface
     * ****************************************************************
     */
    private String deletePortsRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query;

        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort ."
                + "FILTER (NOT EXISTS {?port mrs:type ?type})} ";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portIdTagValue = ResourceTool.getResourceName(port.asResource().toString(), AwsPrefix.nic);
            String portId = ec2Client.getResourceId(portIdTagValue);

            NetworkInterface p = ec2Client.getNetworkInterface(portId);
            if (p == null) //network interface does not exist, need to create a network interface
            {
                throw new EJBException(String.format("The port %s to be deleted"
                        + "does not exists", port));
            } else {
                //check to see the network interface has no attachments before
                //deleting it, if they do and the instance is not in the deleting
                //part, there will be an error (if the port is the eth0)
                query = "SELECT ?node WHERE {?node nml:hasBidirectionalPort <" + port.asResource() + "> ."
                        + "?node a nml:Node}";
                ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
                if (p.getAttachment() != null && !r1.hasNext() && p.getAttachment().getDeviceIndex() == 0) {
                    throw new EJBException(String.format("The port %s to be deleted"
                            + " has attachments, delete dependant resource first", port));
                }
                //find the subnet that has the port previously found
                query = "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model Reduction does not specify network subnet of port: %s", port));
                }
                String subnetId = null;
                while (r1.hasNext()) {
                    QuerySolution querySolution1 = r1.next();
                    RDFNode subnet = querySolution1.get("subnet");
                    query = "SELECT ?subnet WHERE {<" + subnet.asResource() + ">  a  mrs:SwitchingSubnet}";
                    ResultSet r3 = executeQuery(query, model, modelReduct);
                    while (r3.hasNext()) //search in the model to see if subnet existed before
                    {
                        subnetId = ResourceTool.getResourceName(subnet.asResource().toString(), AwsPrefix.subnet);
                        subnetId = ec2Client.getResourceId(subnetId);
                        break;
                    }
                }
                if (subnetId == null) {
                    throw new EJBException(String.format("model additions subnet for port %s"
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
    private String deleteInstancesRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");
            /* Xi: no need to check volume 
             query = "SELECT ?service ?volume ?port WHERE {<" + node.asResource() + "> mrs:providedByService ?service ."
             + "<" + node.asResource() + "> mrs:hasVolume ?volume ."
             + "<" + node.asResource() + "> nml:hasBidirectionalPort ?port}";
             ResultSet r1 = executeQuery(query, model, modelReduct);
             if (!r1.hasNext()) {
             throw new EJBException(String.format("model reduction is malformed for node: %s", node));
             }
             QuerySolution querySolution1 = r1.next();

             RDFNode service = querySolution1.get("service");
             RDFNode volume = querySolution1.get("volume");
             RDFNode port = querySolution1.get("port");
             if (service == null) {
             throw new EJBException(String.format("node to delete does not specify service for node: %s", node));
             }
             if (volume == null) {
             throw new EJBException(String.format("node to delete does not specify volume for node: %s", node));
             }
             if (port == null) {
             throw new EJBException(String.format("node to delete does not specify port for node: %s", node));
             }
             //check that one of the volumes is the main device 
             query = "SELECT ?volume ?deviceName WHERE {<" + node.asResource() + "> mrs:hasVolume ?volume ."
             + "?volume mrs:target_device ?deviceName}";
             r1 = executeQuery(query, model, modelReduct);
             if (!r1.hasNext()) {
             throw new EJBException(String.format("model reduction does not specify root device name for volume"
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
             throw new EJBException(String.format("model reduction does not specify root volume"
             + " attached to instance: %s", node));
             }
             */
            query = "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() + ">}";
            ResultSet r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("model reduction does not specify vpc of node to be deleted: %s", node));
            }

            query = "SELECT ?service WHERE {?service  mrs:providesVM <" + node.asResource() + ">}";
            r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("model reduction does not specify service of node: %s", node));
            }

            String nodeIdTagValue = ResourceTool.getResourceName(node.asResource().toString(), AwsPrefix.instance);
            String nodeId = ec2Client.getInstanceId(nodeIdTagValue);

            Instance instance = ec2Client.getInstance(nodeId);
            if (instance == null) //instance does not exists
            {
                throw new EJBException(String.format("Node to delete: %s does not exist", node));
            } else {
                requests += String.format("TerminateInstancesRequest %s \n", nodeId);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to detach a network interface to an existing instance
     * ****************************************************************
     */
    private String detachPortRequest(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query = "";

        query = "SELECT ?node ?port WHERE {?node nml:hasBidirectionalPort ?port}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode port = q.get("port");
            RDFNode node = q.get("node");
            String nodeIdTag = ResourceTool.getResourceName(node.asResource().toString(), AwsPrefix.instance);
            query = "SELECT ?node WHERE {<" + node.asResource() + "> a nml:Node}";
            ResultSet r1 = executeQuery(query, model, emptyModel);
            ResultSet r1_1 = executeQuery(query, emptyModel, modelReduct);
            Instance i = null;
            if (r1.hasNext()) {
                String nodeId = ec2Client.getInstanceId(nodeIdTag);
                i = ec2Client.getInstance(nodeId);
            }
            while (r1.hasNext() && !r1_1.hasNext()) {
                r1.next();
                String portIdTag = ResourceTool.getResourceName(port.asResource().toString(), AwsPrefix.nic);

                query = "SELECT ?tag WHERE {<" + port.asResource() + "> a  nml:BidirectionalPort ."
                        + "FILTER (NOT EXISTS {<" + port.asResource() + "> mrs:type ?type})} ";
                ResultSet r2 = executeQuery(query, model, modelReduct);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("bidirectional port %s to be detaches is not a  network-interface", port));
                }

                String attachmentId = "";
                int deviceIndex = 0;
                for (InstanceNetworkInterface eni : i.getNetworkInterfaces()) {
                    if (eni.getNetworkInterfaceId().equals(ec2Client.getResourceId(portIdTag))) {
                        attachmentId = eni.getAttachment().getAttachmentId();
                        break;
                    }

                }
                if (attachmentId.equals("")) {
                    throw new EJBException(String.format("bidirectional port %s to be detached has no attachments", port));
                }

                requests += String.format("DetachNetworkInterfaceRequest %s %s\n", attachmentId, ec2Client.getResourceId(portIdTag));
            }

        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to delete volumes from a model
     * ****************************************************************
     */
    private String deleteVolumesRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query;

        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeIdTagValue = ResourceTool.getResourceName(volume.asResource().toString(), AwsPrefix.volume);
            String volumeId = ec2Client.getVolumeId(volumeIdTagValue);

            Volume v = ec2Client.getVolume(volumeId);

            if (v == null) {
                throw new EJBException(String.format("volume does not exist: %s", volume));
            } else //volume exists so it has to be deleted
            {
                //check what service is providing the volume
                query = "SELECT ?type WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model reduction does not specify service that provides volume: %s", volume));
                }

                //find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, emptyModel, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model reduction does not specify new type of volume: %s", volume));
                }
                //find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, emptyModel, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model reduction does not specify new size of volume: %s", volume));
                }
                //dont create the volumes with no target device and that are attached to an instance
                //since this volumes are the root devices
                query = "SELECT ?deviceName WHERE {<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                r1 = executeQuery(query, emptyModel, modelReduct);
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
    private String deleteRouteRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String tempRequest = "";
        String query = "";

        query = "SELECT ?route ?table WHERE {?route a mrs:Route ."
                + "?table mrs:hasRoute ?route}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode table = q.get("table");
            RDFNode route = q.get("route");

            //check that is not main route
            //if it is the main route dont
            String vpcAddress;
            String routeAddress;
            query = "SELECT ?service WHERE {?service mrs:providesRoutingTable <" + table.asResource() + "> }";
            ResultSet r2 = executeQuery(query, model, modelReduct);
            RDFNode service = null;
            if (r2.hasNext()) {
                QuerySolution q1 = r2.next();
                service = q1.get("service");
            } else {
                throw new EJBException(String.format("No service provides the routing"
                        + " table %s", table.asResource()));
            }
            query = "SELECT ?service WHERE {<" + service.asResource() + "> a  mrs:RoutingService}";
            r2 = executeQuery(query, model, modelReduct);
            if (!r2.hasNext()) {
                throw new EJBException(String.format("Service %s is not a "
                        + "routing service", service));
            }

            query = "SELECT ?value WHERE {?vpc nml:hasService  <" + service.asResource() + "> ."
                    + "?vpc mrs:hasNetworkAddress ?address ."
                    + "?address mrs:value ?value}";
            r2 = executeQuery(query, model, modelReduct);
            if (r2.hasNext()) {
                QuerySolution q1 = r2.next();
                RDFNode val = q1.get("value");
                vpcAddress = val.asLiteral().toString();
            } else {
                throw new EJBException(String.format("Network address for Vpc of routing"
                        + "table %s could not be found ", table.asResource()));
            }
            query = "SELECT ?value WHERE {<" + route.asResource() + "> mrs:routeTo ?routeTo ."
                    + "?routeTo mrs:value ?value}";
            r2 = executeQuery(query, model, modelReduct);
            if (!r2.hasNext()) {
                throw new EJBException(String.format("RouteTo statement is not defined"
                        + "for route %s", route.asResource()));
            } else {
                QuerySolution q1 = r2.next();
                RDFNode val = q1.get("value");
                routeAddress = val.asLiteral().toString();
            }

            if (routeAddress.equals(vpcAddress)) {
                continue;
            }

            String tableIdTag = ResourceTool.getResourceName(table.asResource().toString(), AwsPrefix.routingTable);

            //make sure the new route will have all the source subnets as the table already has
            //take as reference the main route of the route table
            vpcAddress = "\"" + vpcAddress + "\"";
            query = "SELECT ?route WHERE {<" + table.asResource() + "> mrs:hasRoute ?route ."
                    + "?route mrs:routeTo ?routeTo ."
                    + "?routeTo mrs:value " + vpcAddress + "}";
            r2 = executeQuery(query, model, modelReduct);
            QuerySolution q1 = r2.next();
            RDFNode mainRoute = q1.get("route");
            boolean skipRoute = false;
            //get the subnets of the main route, that will tell the routeTable associations
            query = "SELECT ?routeFrom WHERE {<" + mainRoute.asResource() + "> mrs:routeFrom ?routeFrom ."
                    + "?routeFrom a mrs:SwitchingSubnet}";
            r2 = executeQuery(query, emptyModel, modelReduct);
            while (r2.hasNext()) {
                q1 = r2.next();
                String routeFrom = q1.get("routeFrom").asResource().toString();
                query = "SELECT ?value WHERE {<" + route.asResource() + "> mrs:routeFrom <" + routeFrom + ">}";
                ResultSet r3 = executeQuery(query, emptyModel, modelReduct);
                if (!r3.hasNext()) {
                    skipRoute = true;
                    //throw new EJBException(String.format("new route %s does not contain all the subnet"
                    //        + " associations of the route table", route.asResource()));
                }
            }
            query = "SELECT ?routeFrom WHERE {<" + mainRoute.asResource() + "> mrs:routeFrom ?routeFrom ."
                    + "?routeFrom a mrs:SwitchingSubnet}";
            r2 = executeQuery(query, model, emptyModel);
            while (r2.hasNext()) {
                q1 = r2.next();
                String routeFrom = q1.get("routeFrom").asResource().toString();
                query = "SELECT ?value WHERE {<" + route.asResource() + "> mrs:routeFrom <" + routeFrom + ">}";
                ResultSet r3 = executeQuery(query, emptyModel, modelReduct);
                if (!r3.hasNext()) {
                    skipRoute = true;
                    //throw new EJBException(String.format("new route %s does not contain all the subnet"
                    //        + "associations of the route table", route.asResource()));
                }
            }
            if (skipRoute) {
                continue;
            }
            //find the destination and nex hop
            query = "SELECT  ?nextHop ?value WHERE {<" + route.asResource() + "> mrs:routeTo ?routeTo ."
                    + "<" + route.asResource() + "> mrs:nextHop ?nextHop ."
                    + "?routeTo mrs:value ?value}";
            r2 = executeQuery(query, emptyModel, modelReduct);
            while (r2.hasNext()) {
                QuerySolution q2 = r2.next();
                RDFNode value = q2.get("value");
                RDFNode nextHop = q2.get("nextHop");
                String destination = value.asLiteral().toString();
                String target;
                String gatewayId;

                //check next hop
                if (nextHop.isLiteral()) {
                    target = nextHop.asLiteral().toString();
                    gatewayId = target;
                } else {
                    String nextHopResource = nextHop.asResource().toString();
                    target = ResourceTool.getResourceName(nextHop.asResource().toString(), AwsPrefix.gateway);
                    query = String.format("SELECT ?gateway WHERE{<%s> a nml:BidirectionalPort}", nextHopResource);
                    ResultSet r3 = executeQuery(query, model, modelReduct);
                    if (!r3.hasNext()) {
                        throw new EJBException(String.format("next hop %s does not exist in delta "
                                + "or system model", target));
                    }
                    gatewayId = ec2Client.getResourceId(target);
                }

                Route rou = new Route();
                rou.withDestinationCidrBlock(destination)
                        .withGatewayId(gatewayId)
                        .withState(RouteState.Active)
                        .withOrigin(RouteOrigin.CreateRoute);

                tempRequest = String.format("DeleteRouteRequest %s %s %s \n", tableIdTag, destination, target);

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
    private String disassociateTableRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String tempRequests = "";
        String query;

        query = "SELECT ?route ?routeFrom WHERE {?route mrs:routeFrom ?routeFrom}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            boolean continueFlag = false;
            boolean createRequest = true;
            QuerySolution querySolution = r.next();
            RDFNode value = querySolution.get("routeFrom");
            RDFNode route = querySolution.get("route");
            RDFNode routeFrom = querySolution.get("routeFrom");

            //routeFrom must be a subnet
            query = "SELECT ?a WHERE {<" + value.asResource().toString() + "> a mrs:SwitchingSubnet}";
            ResultSet r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                continue;
            }

            query = "SELECT ?table WHERE {?table mrs:hasRoute <" + route.asResource() + "> ."
                    + "?service mrs:providesRoute <" + route.asResource() + ">}";
            r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("Route  %s"
                        + " does not exist in a route table or is not being provided "
                        + "by a routing service", route));
            }
            QuerySolution querySolution1 = r1.next();
            RDFNode table = querySolution1.get("table");

            String subnetIdTag = ResourceTool.getResourceName(value.asResource().toString(), AwsPrefix.subnet);
            String subnetId = ec2Client.getResourceId(subnetIdTag);
            String tableIdTag = ResourceTool.getResourceName(table.asResource().toString(), AwsPrefix.routingTable);

            if (ec2Client.getSubnet(subnetId) == null) {
                throw new EJBException(String.format("subnet %s to disassociate from"
                        + "route table %s does not exist", subnetIdTag, tableIdTag));
            }

            RouteTable rt = ec2Client.getRoutingTable(ec2Client.getTableId(tableIdTag));
            if (rt == null) {
                throw new EJBException(String.format("Route table %s to disassociate"
                        + " does not exist", tableIdTag));
            } else {
                for (RouteTableAssociation as : rt.getAssociations()) {
                    if (as != null && as.getSubnetId() != null && as.getSubnetId().equals(ec2Client.getResourceId(subnetIdTag))) { //main routeTable may have implicit associations
                        createRequest = true;
                    }
                }
            }

            if (createRequest == true) {
                //check that older routes in the table also include this  routeFrom
                query = "SELECT ?route WHERE {<" + table.asResource() + "> mrs:hasRoute ?route}";
                ResultSet r2 = executeQuery(query, model, emptyModel);
                while (r2.hasNext()) {
                    QuerySolution q2 = r2.next();
                    RDFNode ro = q2.get("route");
                    query = "SELECT ?routeFrom WHERE {<" + routeFrom.asResource() + "> a mrs:SwitchingSubnet}";
                    ResultSet r3 = executeQuery(query, model, modelReduct);
                    if (!r3.hasNext()) {
                        continueFlag = true;
                        break;
                    } else {
                        continueFlag = false;
                    }
                }
                if (continueFlag == true) {
                    continue;
                }
                //get the association id of the route table association to delete
                String associationId = "";
                for (RouteTableAssociation as : rt.getAssociations()) {
                    if (as != null && as.getSubnetId() != null && as.getSubnetId().equals(subnetId)) {
                        associationId = as.getRouteTableAssociationId();
                        break;
                    }
                }
                if (associationId.isEmpty()) {
                    boolean main = false;
                    List<RouteTableAssociation> as = rt.getAssociations();
                    if (!as.isEmpty()) {
                        main = as.get(0).getMain();
                    }
                    if (main == true) //skip this as it is an implicit assco with the main table
                    {
                        continue;
                    } else {
                        throw new EJBException(String.format("The route table association id for subnet %s"
                                + " in route tabe %s does not exist", subnetIdTag, tableIdTag));
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

    /**
     * ****************************************************************
     * Function to delete gateway (Internet and VPN)
     * ****************************************************************
     */
    private String deleteGatewayRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query;

        query = "SELECT ?igw  WHERE {?igw a nml:BidirectionalPort}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode igw = q.get("igw");
            String idTag = ResourceTool.getResourceName(igw.asResource().toString(), AwsPrefix.gateway);

            //look too see if resource is a internet gateway or not
            query = "SELECT ?type WHERE {<" + igw.asResource() + "> mrs:type ?type ."
                    + "FILTER(?type In (\"internet-gateway\",\"vpn-gateway\"))}";
            ResultSet r1 = executeQuery(query, model, emptyModel);
            if (!r1.hasNext()) {
                continue; //not a gateway
            }
            QuerySolution q1 = r1.next();
            String type = q1.get("type").asLiteral().toString();

            //find the vpc of the gateway
            query = "SELECT ?vpc WHERE {?vpc nml:hasBidirectionalPort <" + igw + ">}";
            r1 = executeQuery(query, emptyModel, modelReduct);
            if (!r1.hasNext()) {
                continue; // the gateway might be detached, skip
                //throw new EJBException(String.format("Gateway %s does not specify topology", igw));
            }
            q = r1.next();
            RDFNode vpc = q.get("vpc");
            String vpcIdTag = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);

            //check that the vpc is of type topology
            query = "SELECT ?vpc WHERE {<" + vpc.asResource() + "> a nml:Topology}";
            r1 = executeQuery(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("VPC %s for gateway %s is not "
                        + "of type topology", vpc, igw));
            }
            if (type.equals("internet-gateway")) {

                //check that the topology for the internet gateway is not the main topology
                //as the internet gateway should be attached to a VPC not the main topology
                if (vpcIdTag.contains(topologyUri.replace(":", ""))) {
                    throw new EJBException(String.format("Internet gateway %s"
                            + " cannot be detached from root topology", igw));
                }
                if (ec2Client.getInternetGateway(ec2Client.getResourceId(idTag)) == null) {
                    throw new EJBException(String.format("Internet gateway %s  does not exists", idTag));
                } else {
                    requests += String.format("DeleteInternetGatewayRequest %s %s \n", idTag, vpcIdTag);
                }
            } else if (type.equals("vpn-gateway")) {
                if (ec2Client.getVirtualPrivateGateway(ec2Client.getVpnGatewayId(idTag)) == null) {
                    throw new EJBException(String.format("VPN gateway %s does not exists", idTag));
                } else {
                    requests += String.format("DeleteVpnGatewayRequest %s \n", idTag);
                }
            } else {
                throw new EJBException(String.format("Gateway %s has an invalid type", igw));
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to detach a VPN gateway to a VPC from a model
     * ****************************************************************
     */
    private String detachVPNGatewayRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query = "SELECT ?vpc ?port  WHERE {?vpc nml:hasBidirectionalPort ?port}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        // a new port has been added to delta
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode vpc = q.get("vpc");
            RDFNode gateway = q.get("port");
            // check if the new port is an VGW gateway for an VPC 
            query = "SELECT ?vpc ?port ?tag WHERE {?vpc nml:hasBidirectionalPort ?port . "
                    + "?vpc nml:hasService ?service . "
                    + "?service a  mrs:SwitchingService . "
                    + "?port mrs:type \"vpn-gateway\" ."
                    + String.format("FILTER(?vpc = <%s> && ?port = <%s>) ", vpc, gateway)
                    + "}";
            ResultSet r1 = executeQueryUnion(query, model, modelReduct);
            if (r1.hasNext()) {
                String gatewayIdTag = ResourceTool.getResourceName(gateway.asResource().toString(), AwsPrefix.gateway);
                String vpcIdTag = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);
                requests += String.format("detachVpnGatewayRequest %s %s \n", gatewayIdTag, vpcIdTag);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to delete the route table
     * ****************************************************************
     */
    private String deleteRouteTableRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query = "";

        query = "SELECT ?table WHERE {?table a mrs:RoutingTable ."
                + "?table mrs:type \"local\"}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode table = querySolution.get("table");
            String tableIdTagValue = ResourceTool.getResourceName(table.asResource().toString(), AwsPrefix.routingTable);
            String tableId = ec2Client.getTableId(tableIdTagValue);
            if (ec2Client.getRoutingTable(tableId) != null) {
                //check route table is modeled 
                query = "SELECT ?type WHERE{<" + table.asResource() + "> mrs:type ?type}";
                ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model Reduction for route table %s"
                            + " is not well specified", table));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //check the service that provides the routing table
                query = "SELECT ?service WHERE {?service   mrs:providesRoutingTable <" + table.asResource() + ">}";
                r1 = executeQuery(query, emptyModel, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Route table to be deleted %s is not provided by any service", table));
                }
                querySolution1 = r1.next();
                RDFNode service = querySolution1.get("service");

                //get the address of the vpc of the route table
                query = "SELECT ?vpc ?address WHERE {?vpc nml:hasService <" + service.asResource() + "> ."
                        + "?vpc mrs:hasNetworkAddress ?networkAddress ."
                        + "?networkAddress mrs:value ?address}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("vpc of route table %s in model reduction "
                            + "has no network address", table));
                }
                querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");
                RDFNode vpc = querySolution1.get("vpc");
                String vpcIdTagValue = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);

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
                        throw new EJBException(String.format("model reduction for route table %s"
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
                        throw new EJBException(String.format("route table %s"
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
    private String deleteVpcsRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query;

        query = "SELECT ?vpc WHERE {?service mrs:providesVPC  ?vpc ."
                + "?vpc a nml:Topology}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode vpc = querySolution.get("vpc");
            String vpcIdTagValue = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);
            String vpcId = ec2Client.getVpcId(vpcIdTagValue);

            //double check vpc does not exist in the cloud
            Vpc v = ec2Client.getVpc(vpcId);
            if (v == null) // vpc does not exist, has to be created
            {
                throw new EJBException(String.format("vpc to be deleted $s does not exist", vpc));
            } else {
                query = "SELECT ?cloud WHERE {?cloud nml:hasTopology <" + vpc.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model reduction does not specify the aws-cloud that"
                            + "provides VPC : %s", vpc));
                }

                query = "SELECT ?address WHERE {<" + vpc.asResource() + "> mrs:hasNetworkAddress ?address}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model reduction does not specify the "
                            + "newtowk address of vpc: %s", vpc));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");

                query = "SELECT ?type ?value WHERE {<" + address.asResource() + "> mrs:type ?type ."
                        + "<" + address.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model reduction does not specify the "
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
                    throw new EJBException(String.format("Vpc %s does not speicfy Switching Service in the model reduction", vpc));
                }
                query = "SELECT ?service  WHERE {<" + vpc.asResource() + "> nml:hasService  ?service ."
                        + "?service a mrs:RoutingService}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Vpc %s does not speicfy Routing Service in the "
                            + "model reduction", vpc));
                }
                querySolution1 = r1.next();
                RDFNode routingService = querySolution1.get("service");

                //incorporate main routing table and routes into the vpc
                query = "SELECT ?routingTable ?type  WHERE {<" + routingService.asResource() + "> mrs:providesRoutingTable ?routingTable ."
                        + "?routingTable mrs:type  \"main\"}";
                r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Routing service  %s does not speicfy main Routing table in the model "
                            + "reduction", routingService));
                }

                querySolution1 = r1.next();
                RDFNode routingTable = querySolution1.get("routingTable");
                String routeTableIdTagValue = ResourceTool.getResourceName(routingTable.asResource().toString(), AwsPrefix.routingTable);

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
                    throw new EJBException(String.format("Routing service has no route for main table in the model"
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
    private String deleteSubnetsRequests(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query;

        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetIdTagValue = ResourceTool.getResourceName(subnet.asResource().toString(), AwsPrefix.subnet);
            String subnetId = ec2Client.getResourceId(subnetIdTagValue);

            Subnet s = ec2Client.getSubnet(subnetId);

            if (s == null) //subnet does not exist, need to create subnet
            {
                throw new EJBException(String.format("subnet %s to be deleted does not exist ", subnet));
            } else {
                query = "SELECT ?service {?service mrs:providesSubnet <" + subnet.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelReduct);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("No service has subnet %s", subnet));
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
                    throw new EJBException(String.format("Subnet %s does not have a vpc in the model reduction", subnet));
                }
                querySolution1 = r1.next();
                RDFNode vpc = querySolution1.get("vpc");
                String vpcIdTagValue = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.subnet);

                query = "SELECT ?subnet ?address ?value WHERE {<" + subnet.asResource() + "> mrs:hasNetworkAddress ?address ."
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
    private String createVpcsRequests(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query;

        query = "SELECT ?vpc WHERE {?service mrs:providesVPC  ?vpc ."
                + "?vpc a nml:Topology}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode vpc = querySolution.get("vpc");
            String vpcIdTagValue = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);
            String vpcId = ec2Client.getVpcId(vpcIdTagValue);

            //double check vpc does not exist in the cloud
            Vpc v = ec2Client.getVpc(vpcId);
            if (v != null) // vpc  exists, has to be created
            {
                throw new EJBException(String.format("VPC %s already exists", vpcIdTagValue));
            } else {
                query = "SELECT ?cloud WHERE {?cloud nml:hasTopology <" + vpc.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify the aws-cloud that"
                            + "provides VPC : %s", vpc));
                }

                query = "SELECT ?address WHERE {<" + vpc.asResource() + "> mrs:hasNetworkAddress ?address}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify the "
                            + "newtowk address of vpc: %s", vpc));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");

                query = "SELECT ?type ?value WHERE {<" + address.asResource() + "> mrs:type ?type ."
                        + "<" + address.asResource() + "> mrs:value ?value}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify the "
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
                    throw new EJBException(String.format("New Vpc %s does not speicfy Switching Service", vpc));
                }

                query = "SELECT ?service  WHERE {<" + vpc.asResource() + "> nml:hasService  ?service ."
                        + "?service a mrs:RoutingService}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("New Vpc %s does not speicfy Routing Service", vpc));
                }
                querySolution1 = r1.next();
                RDFNode routingService = querySolution1.get("service");

                //incorporate main routing table and routes into the vpc
                query = "SELECT ?routingTable ?type  WHERE {<" + routingService.asResource() + "> mrs:providesRoutingTable ?routingTable ."
                        + "?routingTable mrs:type  \"main\"}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Routing service  %s does not speicfy main Routing table in the model addition", routingService));
                }

                querySolution1 = r1.next();
                RDFNode routingTable = querySolution1.get("routingTable");
                String routeTableIdTagValue = ResourceTool.getResourceName(routingTable.asResource().toString(), AwsPrefix.routingTable);

                String vpcIp = cidrBlock;
                cidrBlock = "\"" + cidrBlock + "\"";

                //check for  the local route is in the route table 
                query = "SELECT ?route ?to  WHERE {<" + routingTable.asResource().toString() + "> mrs:hasRoute ?route ."
                        + "<" + routingService.asResource() + "> mrs:providesRoute ?route ."
                        + "?route mrs:nextHop \"local\" ."
                        + "?route mrs:routeTo  ?to ."
                        + "?to mrs:value " + cidrBlock + "}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Routing service has no route for main table in the model"
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
    private String createSubnetsRequests(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query;

        query = "SELECT ?subnet WHERE {?subnet a mrs:SwitchingSubnet}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode subnet = querySolution.get("subnet");
            String subnetIdTagValue = ResourceTool.getResourceName(subnet.asResource().toString(), AwsPrefix.subnet);
            String subnetId = ec2Client.getResourceId(subnetIdTagValue);

            Subnet s = ec2Client.getSubnet(subnetId);

            if (s != null) //subnet  exists,does not need to create one
            {
                throw new EJBException(String.format("Subnet %s already exists", subnetIdTagValue));
            } else {
                query = "SELECT ?service {?service mrs:providesSubnet <" + subnet.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("No service has subnet %s", subnet));
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
                    throw new EJBException(String.format("Subnet %s does not have a vpc", subnet));
                }
                querySolution1 = r1.next();
                RDFNode vpc = querySolution1.get("vpc");
                String vpcIdTagValue = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);

                query = "SELECT ?address ?value ?igw_route WHERE {"
                        + String.format("<%s> mrs:hasNetworkAddress ?address .", subnet.asResource().getURI())
                        + "?address a mrs:NetworkAddress ."
                        + "?address mrs:type \"ipv4-prefix\" ."
                        + "?address mrs:value ?value. "
                        + "OPTIONAL {"
                        + String.format("?igw_route mrs:routeFrom <%s>.", subnet.asResource().getURI())
                        + "?igw_route mrs:nextHop ?igw."
                        + "?igw mrs:type \"internet-gateway\""
                        + "}"
                        + "}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Subnet %s does not have a valid CIDR", subnetIdTagValue));
                }
                querySolution1 = r1.next();
                RDFNode value = querySolution1.get("value");
                String cidrBlock = value.asLiteral().toString();
                String subnetType = querySolution1.contains("igw_route")?"public":"private";
                requests += String.format("CreateSubnetRequest %s %s %s %s \n", vpcIdTagValue, cidrBlock, subnetIdTagValue, subnetType);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to createRoutetable
     * ****************************************************************
     */
    private String createRouteTableRequests(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query = "";

        query = "SELECT ?table WHERE {?table a mrs:RoutingTable ."
                + "?table mrs:type \"local\"}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode table = querySolution.get("table");
            String tableIdTagValue = ResourceTool.getResourceName(table.asResource().toString(), AwsPrefix.routingTable);
            String tableId = ec2Client.getTableId(tableIdTagValue);
            if (ec2Client.getRoutingTable(tableId) != null) //routing table already exists
            {
                throw new EJBException(String.format("Routing Table %s already exists, does not need"
                        + "to create one", tableIdTagValue));
            } else {
                //check route table is modeled 
                query = "SELECT ?type WHERE{<" + table.asResource() + "> mrs:type ?type}";
                ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition for route table %s"
                            + " is not well specified", table));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //check the service that provides the routing table
                query = "SELECT ?service WHERE {?service   mrs:providesRoutingTable <" + table.asResource() + ">}";
                r1 = executeQuery(query, emptyModel, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("Route table %s is not provided by any service in the model addition", table));
                }
                querySolution1 = r1.next();
                RDFNode service = querySolution1.get("service");

                //get the address of the vpc of the route table
                query = "SELECT ?vpc ?address WHERE {?vpc nml:hasService <" + service.asResource() + "> ."
                        + "?vpc mrs:hasNetworkAddress ?networkAddress ."
                        + "?networkAddress mrs:value ?address}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("vpc of route table %s has no network address", table));
                }
                querySolution1 = r1.next();
                RDFNode address = querySolution1.get("address");
                RDFNode vpc = querySolution1.get("vpc");
                String vpcIdTagValue = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);

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
                        throw new EJBException(String.format("model addition for route table %s"
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
                        throw new EJBException(String.format("route table %s"
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
    private String associateTableRequest(OntModel model, OntModel modelAdd) {
        String requests = "";
        String tempRequests = "";
        String query;

        query = "SELECT ?route ?routeFrom WHERE {?route mrs:routeFrom ?routeFrom}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            boolean createRequest = true;
            QuerySolution querySolution = r.next();
            RDFNode value = querySolution.get("routeFrom");
            RDFNode route = querySolution.get("route");
            RDFNode routeFrom = querySolution.get("routeFrom");

            //routeFrom must be a subnet
            query = "SELECT ?a WHERE {<" + value.asResource().toString() + "> a mrs:SwitchingSubnet}";
            ResultSet r1 = executeQuery(query, model, modelAdd);
            if (!r1.hasNext()) {
                continue;
            }

            query = "SELECT ?table WHERE {?table mrs:hasRoute <" + route.asResource() + "> ."
                    + "?service mrs:providesRoute <" + route.asResource() + ">}";
            r1 = executeQuery(query, model, modelAdd);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("Route  %s"
                        + "does not have a route table or is not"
                        + "being provided by a routing service in the model addition", route));
            }
            QuerySolution querySolution1 = r1.next();
            RDFNode table = querySolution1.get("table");

            String subnetIdTag = ResourceTool.getResourceName(value.asResource().toString(), AwsPrefix.subnet);
            String tableIdTag = ResourceTool.getResourceName(table.asResource().toString(), AwsPrefix.routingTable);

            RouteTable rt = ec2Client.getRoutingTable(ec2Client.getTableId(tableIdTag));
            if (rt == null) {
            } else {
                for (RouteTableAssociation as : rt.getAssociations()) {
                    if (as.getSubnetId().equals(ec2Client.getResourceId(subnetIdTag))) {
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
                    query = "SELECT ?routeFrom WHERE {<" + routeFrom.asResource() + "> a mrs:SwitchingSubnet}";
                    ResultSet r3 = executeQuery(query, model, modelAdd);
                    if (!r3.hasNext()) {
                        throw new EJBException(String.format("Route  %s does state"
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
    private String createGatewayRequests(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query;

        query = "SELECT ?igw WHERE {?igw a nml:BidirectionalPort}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode igw = q.get("igw");
            String idTag = ResourceTool.getResourceName(igw.asResource().toString(), AwsPrefix.gateway);

            //look for the type in the reference model
            query = "SELECT ?type WHERE {<" + igw.asResource() + "> mrs:type ?type ."
                    + "FILTER(?type In (\"internet-gateway\",\"vpn-gateway\"))}";
            ResultSet r1 = executeQuery(query, model, modelAdd);
            if (!r1.hasNext()) {
                continue; //not a gateway
            }
            QuerySolution q1 = r1.next();
            String type = q1.get("type").asLiteral().toString();

            //find the vpc of the gateway
            query = "SELECT ?vpc ?port  WHERE {?vpc nml:hasBidirectionalPort <" + igw + ">}";
            r1 = executeQuery(query, emptyModel, modelAdd);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("Gateway %s does not specify vpc", igw));
            }
            q = r1.next();
            RDFNode vpc = q.get("vpc");
            String vpcIdTag = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);

            //check that the vpc is of type topology
            query = "SELECT ?vpc WHERE {<" + vpc.asResource() + "> a nml:Topology}";
            r1 = executeQuery(query, model, modelAdd);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("VPC %s for gateway %s is not "
                        + "of type topology", vpc, igw));
            }
            if (type.equals("internet-gateway")) {

                //check that the topology for the internet gateway is not the main topology
                //as the internet gateway should be attached to a VPC not the main topology
                String topology = topologyUri;
                if (vpcIdTag.contains(topology.replace(":", ""))) {
                    throw new EJBException(String.format("Internet gateway %s"
                            + " cannot be attached to root topology", igw));
                }
                if (ec2Client.getInternetGateway(ec2Client.getResourceId(idTag)) != null) {
                    throw new EJBException(String.format("Internet gateway %s already exists", idTag));
                } else {
                    requests += String.format("CreateInternetGatewayRequest %s %s \n", idTag, vpcIdTag);
                }
            } else if (type.equals("vpn-gateway")) {
                if (ec2Client.getVirtualPrivateGateway(ec2Client.getVpnGatewayId(idTag)) != null) {
                    throw new EJBException(String.format("VPN gateway %s already exists", idTag));
                } else {
                    requests += String.format("CreateVpnGatewayRequest %s \n", idTag);
                }
            } else {
                throw new EJBException(String.format("Gateway %s has an invalid type", igw));
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to attach a VPN gateway to a VPC from a model
     * ****************************************************************
     */
    private String attachVPNGatewayRequests(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query = "SELECT ?vpc ?port  WHERE {?vpc nml:hasBidirectionalPort ?port}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        // a new port has been added to delta
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode vpc = q.get("vpc");
            RDFNode gateway = q.get("port");
            // check if the new port is an VGW gateway for an VPC 
            query = "SELECT ?vpc ?port WHERE {?vpc nml:hasBidirectionalPort ?port . "
                    + "?vpc nml:hasService ?service . "
                    + "?service a  mrs:SwitchingService . "
                    + "?port mrs:type \"vpn-gateway\" ."
                    + String.format("FILTER(?vpc = <%s> && ?port = <%s>) ", vpc, gateway)
                    + "}";
            ResultSet r1 = executeQueryUnion(query, model, modelAdd);
            if (r1.hasNext()) {
                String gatewayIdTag = ResourceTool.getResourceName(gateway.asResource().toString(), AwsPrefix.gateway);
                String vpcIdTag = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);
                requests += String.format("AttachVpnGatewayRequest %s %s \n", gatewayIdTag, vpcIdTag);
            }
        }

        return requests;
    }

    /**
     * ****************************************************************
     * Function to create a routes from a model
     * ****************************************************************
     */
    private String createRouteRequests(OntModel model, OntModel modelAdd) {
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
            String vpcAddress;
            String routeAddress;
            query = "SELECT ?service WHERE {?service mrs:providesRoutingTable <" + table.asResource() + "> }";
            ResultSet r2 = executeQuery(query, model, modelAdd);
            RDFNode service = null;
            if (r2.hasNext()) {
                QuerySolution q1 = r2.next();
                service = q1.get("service");
            } else {
                throw new EJBException(String.format("No service provides the routing"
                        + " table %s", table.asResource()));
            }
            query = "SELECT ?service WHERE {<" + service.asResource() + "> a  mrs:RoutingService}";
            r2 = executeQuery(query, model, modelAdd);
            if (!r2.hasNext()) {
                throw new EJBException(String.format("Service %s is not a "
                        + "routing service", service));
            }

            query = "SELECT ?value WHERE {?vpc nml:hasService  <" + service.asResource() + "> ."
                    + "?vpc mrs:hasNetworkAddress ?address ."
                    + "?address mrs:value ?value}";
            r2 = executeQuery(query, model, modelAdd);
            if (r2.hasNext()) {
                QuerySolution q1 = r2.next();
                RDFNode val = q1.get("value");
                vpcAddress = val.asLiteral().toString();
            } else {
                throw new EJBException(String.format("Network address for Vpc of routing"
                        + "table %s could not be found ", table.asResource()));
            }
            query = "SELECT ?value WHERE {<" + route.asResource() + "> mrs:routeTo ?routeTo ."
                    + "?routeTo mrs:value ?value}";
            r2 = executeQuery(query, model, modelAdd);
            if (!r2.hasNext()) {
                throw new EJBException(String.format("RouteTo statement is not defined"
                        + "for route %s", route.asResource()));
            } else {
                QuerySolution q1 = r2.next();
                RDFNode val = q1.get("value");
                routeAddress = val.asLiteral().toString();
            }

            if (routeAddress.equals(vpcAddress)) {
                continue;
            }

            String tableIdTag = ResourceTool.getResourceName(table.asResource().toString(), AwsPrefix.routingTable);

            //make sure the new route will have all the source subnets as the table already has
            //take as reference the main route of the route table
            vpcAddress = "\"" + vpcAddress + "\"";
            query = "SELECT ?route WHERE {<" + table.asResource() + "> mrs:hasRoute ?route ."
                    + "?route mrs:routeTo ?routeTo ."
                    + "?routeTo mrs:value " + vpcAddress + "}";
            r2 = executeQuery(query, model, modelAdd);
            QuerySolution q1 = r2.next();
            RDFNode mainRoute = q1.get("route");

            //find if the routeFrom comes from a vpn gateway which indicates propagation
            String fromGateway = null;
            query = "SELECT ?routeFrom WHERE {<" + route.asResource() + "> mrs:routeFrom ?routeFrom}";
            ResultSet r3 = executeQuery(query, emptyModel, modelAdd);
            while (r3.hasNext()) {
                QuerySolution q3 = r3.next();
                String routeFrom = q3.get("routeFrom").asResource().toString();
                query = String.format("SELECT ?gateway WHERE{<%s> a nml:BidirectionalPort .", routeFrom)
                        + String.format("<%s> mrs:type \"vpn-gateway\"}", routeFrom);
                ResultSet r4 = executeQueryUnion(query, model, modelAdd);
                if (r4.hasNext()) {
                    fromGateway = routeFrom;
                }
            }

            if (fromGateway == null) { //skip this step as route came from gateway
                //get the subnets of the main route, that will tell the routeTable associations
                query = "SELECT ?routeFrom WHERE {<" + mainRoute.asResource() + "> mrs:routeFrom ?routeFrom}"; //to avoid processing this routeFrom statement
                r2 = executeQuery(query, emptyModel, modelAdd);
                while (r2.hasNext()) {
                    q1 = r2.next();
                    String routeFrom = q1.get("routeFrom").asResource().toString();
                    query = "SELECT ?value WHERE {<" + route.asResource() + "> mrs:routeFrom ?routeFrom ."
                            + String.format("FILTER (?routeFrom = <%s>)}", routeFrom);
                    r3 = executeQuery(query, emptyModel, modelAdd);
                    if (!r3.hasNext()) {
                        throw new EJBException(String.format("new route %s does not contain all the subnet"
                                + "associations od the route table", route.asResource()));
                    }
                }
                query = "SELECT ?routeFrom WHERE {<" + mainRoute.asResource() + "> mrs:routeFrom ?routeFrom}"; //to avoid processing this routeFrom statement
                r2 = executeQuery(query, model, emptyModel);
                while (r2.hasNext()) {
                    q1 = r2.next();
                    String routeFrom = q1.get("routeFrom").asResource().toString();
                    query = "SELECT ?value WHERE {<" + route.asResource() + "> mrs:routeFrom ?routeFrom ."
                            + String.format("FILTER (?routeFrom = <%s>)}", routeFrom);
                    r3 = executeQuery(query, emptyModel, modelAdd);
                    if (!r3.hasNext()) {
                        throw new EJBException(String.format("new route %s does not contain all the subnet"
                                + " associations of the route table", route.asResource()));
                    }
                }
            }

            //find the destination and nex hop
            query = "SELECT  ?nextHop ?value WHERE {<" + route.asResource() + "> mrs:routeTo ?routeTo ."
                    + "<" + route.asResource() + "> mrs:nextHop ?nextHop ."
                    + "?routeTo mrs:value ?value}";
            r2 = executeQuery(query, emptyModel, modelAdd);
            while (r2.hasNext()) {
                QuerySolution q2 = r2.next();
                RDFNode value = q2.get("value");
                RDFNode nextHop = q2.get("nextHop");
                String destination = value.asLiteral().toString();
                String target;
                String gatewayId;

                //check next Hop
                if (nextHop.isLiteral()) {
                    target = nextHop.asLiteral().toString();
                    gatewayId = target;
                    tempRequest = String.format("CreateRouteRequest %s %s %s \n", tableIdTag, destination, target);
                } else {
                    String targetResource = nextHop.asResource().toString();
                    target = ResourceTool.getResourceName(nextHop.asResource().toString(), AwsPrefix.gateway);
                    gatewayId = ec2Client.getResourceId(target);
                    //if the resource is a vpn gateway then just do a vpngateway propagation
                    //instead of route addition
                    query = String.format("SELECT ?gateway WHERE{<%s> a nml:BidirectionalPort}", targetResource);
                    r3 = executeQuery(query, model, modelAdd);
                    if (!r3.hasNext()) {
                        throw new EJBException(String.format("next hop %s does not exist in delta "
                                + "or system model", targetResource));
                    }
                    query = String.format("SELECT ?gateway WHERE{<%s> a nml:BidirectionalPort .", targetResource)
                            + String.format("<%s> mrs:type \"vpn-gateway\" }", targetResource);
                    r3 = executeQuery(query, model, modelAdd);
                    if (fromGateway != null && r3.hasNext()) {
                        tempRequest = String.format("PropagateVpnRequest %s %s \n", tableIdTag, target);
                    } else {
                        tempRequest = String.format("CreateRouteRequest %s %s %s \n", tableIdTag, destination, target);
                    }
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
    private String createVolumesRequests(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query;

        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode volume = querySolution.get("volume");
            String volumeIdTagValue = ResourceTool.getResourceName(volume.asResource().toString(), AwsPrefix.volume);
            String volumeId = ec2Client.getVolumeId(volumeIdTagValue);

            Volume v = ec2Client.getVolume(volumeId);

            if (v != null) //volume exists, no need to create a volume
            {
                throw new EJBException(String.format("Volume %s already exists", volumeIdTagValue));
            } else {
                //check what service is providing the volume
                query = "SELECT ?type WHERE {?service mrs:providesVolume <" + volume.asResource() + ">}";
                ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
                if (!r1.hasNext()) {
                    logger.warning(String.format("model addition does not specify service that provides volume: %s", volume));
                    continue;
                }

                //find out the type of the volume
                query = "SELECT ?type WHERE {<" + volume.asResource() + "> mrs:value ?type}";
                r1 = executeQuery(query, emptyModel, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify new type of volume: %s", volume));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode type = querySolution1.get("type");

                //find out the size of the volume
                query = "SELECT ?size WHERE {<" + volume.asResource() + "> mrs:disk_gb ?size}";
                r1 = executeQuery(query, emptyModel, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify new size of volume: %s", volume));
                }
                querySolution1 = r1.next();
                RDFNode size = querySolution1.get("size");

                //dont create the volumes with no target device and that are attached to an instance
                //since this volumes are the root devices
                query = "SELECT ?deviceName WHERE {<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                r1 = executeQuery(query, emptyModel, modelAdd);
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
    private String createPortsRequests(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query;

        //query for the port 
        query = "SELECT ?port WHERE {?port a  nml:BidirectionalPort ."
                + "FILTER (NOT EXISTS {?port mrs:type ?type})} ";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode port = querySolution.get("port");
            String portIdTagValue = ResourceTool.getResourceName(port.asResource().toString(), AwsPrefix.nic);
            String portId = ec2Client.getResourceId(portIdTagValue);

            NetworkInterface p = ec2Client.getNetworkInterface(portId);

            if (p != null) //network interface  exists, no need to create a network interface
            {
                throw new EJBException(String.format("Network interface %s already exists", portIdTagValue));
            } else {
                //to get the private ip of the network interface
                query = "SELECT ?address ?value WHERE {<" + port.asResource() + ">  mrs:hasNetworkAddress  ?address ."
                        + "?address mrs:type \"ipv4:private\" ."
                        + "?address mrs:value ?value }";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                String privateAddress = "any"; // "any" means unspecified and AWS EC2 will pick an IP from subnet
                if (r1.hasNext()) {
                    QuerySolution querySolution1 = r1.next();
                    RDFNode value = querySolution1.get("value");
                    privateAddress = value.asLiteral().toString();
                }

                //find the subnet that has the port previously found
                query = "SELECT ?subnet WHERE {?subnet  nml:hasBidirectionalPort <" + port.asResource() + ">}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify network interface subnet of port: %s", port));
                }
                String subnetId = null;
                while (r1.hasNext()) {
                    QuerySolution querySolution1 = r1.next();
                    RDFNode subnet = querySolution1.get("subnet");
                    query = "SELECT ?subnet WHERE {<" + subnet.asResource() + ">  a  mrs:SwitchingSubnet}";
                    ResultSet r3 = executeQuery(query, model, modelAdd);
                    while (r3.hasNext()) //search in the model to see if subnet existed before
                    {
                        subnetId = ResourceTool.getResourceName(subnet.asResource().toString(), AwsPrefix.subnet);
                        subnetId = ec2Client.getResourceId(subnetId);
                        break;
                    }
                }
                if (subnetId == null) {
                    throw new EJBException(String.format("model additions subnet for port %s"
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
    private String associateAddressRequest(OntModel model, OntModel modelAdd) {
        //to get the public ip address of the network interface if any
        String requests = "";
        String query = "SELECT  ?port ?address WHERE {?port  mrs:hasNetworkAddress ?address}";
        ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode address = querySolution1.get("address");
            RDFNode port = querySolution1.get("port");
            String portIdTagValue = ResourceTool.getResourceName(port.asResource().toString(), AwsPrefix.nic);
            query = "SELECT  ?a WHERE {<" + address.asResource() + ">  mrs:type \"ipv4:public\"}";
            ResultSet r2 = executeQuery(query, model, emptyModel);
            if (r2.hasNext()) {
                query = "SELECT ?value WHERE {<" + address.asResource() + ">  mrs:value  ?value}";
                r2 = executeQuery(query, model, modelAdd);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("model additions network addres  %s for port $s"
                            + "is not found in the reference mode", address, port));
                }
                QuerySolution querySolution2 = r2.next();
                RDFNode value = querySolution2.get("value");
                String publicAddress = ResourceTool.getResourceName(value.asLiteral().toString(), AwsPrefix.publicAddress);
                requests += "AssociateAddressRequest " + publicAddress + " " + portIdTagValue
                        + " \n";
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to attach a network interface to an existing instance
     * ****************************************************************
     */
    private String attachPortRequest(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query = "";

        query = "SELECT ?node ?port WHERE {?node nml:hasBidirectionalPort ?port}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode port = q.get("port");
            RDFNode node = q.get("node");
            String nodeIdTag = ResourceTool.getResourceName(node.asResource().toString(), AwsPrefix.instance);
            query = "SELECT ?node WHERE {<" + node.asResource() + "> a nml:Node}";
            ResultSet r1 = executeQuery(query, model, emptyModel);
            Instance i = null;
            int index = 0;
            if (r1.hasNext()) {
                String nodeId = ec2Client.getInstanceId(nodeIdTag);
                i = ec2Client.getInstance(nodeId);
                index = i.getNetworkInterfaces().size();
            }
            while (r1.hasNext()) {
                r1.next();
                String portIdTag = ResourceTool.getResourceName(port.asResource().toString(), AwsPrefix.nic);

                query = "SELECT ?tag WHERE {<" + port.asResource() + "> a nml:BidirectionalPort ."
                        + "FILTER (NOT EXISTS {<" + port.asResource() + ">  mrs:type ?type})}";
                ResultSet r2 = executeQuery(query, model, modelAdd);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("bidirectional port %s to be attached to intsnace does not specify a network interface type", port));
                }

                //see if the network interface is already atatched
                NetworkInterface eni = ec2Client.getNetworkInterface(ec2Client.getResourceId(portIdTag));
                if (eni != null) {
                    if (eni.getAttachment() != null) {
                        throw new EJBException(String.format("bidirectional port %s to be attached to instance is already"
                                + " attached to an instance", port));
                    }
                }
                requests += String.format("AttachNetworkInterfaceRequest %s %s %s \n", portIdTag, nodeIdTag, Integer.toString(index));
                index++;
            }

        }
        return requests;
    }

    /**
     * ****************************************************************
     * Function to create Instances
     * ****************************************************************
     */
    private String createInstancesRequests(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query;

        query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("node");
            String nodeIdTagValue = ResourceTool.getResourceName(node.asResource().toString(), AwsPrefix.instance);
            String nodeId = ec2Client.getInstanceId(nodeIdTagValue);

            Instance instance = ec2Client.getInstance(nodeId);
            List<Instance> i = ec2Client.getEc2Instances();
            if (instance != null) //instance does not be to be created
            {
                throw new EJBException(String.format("Instance %s already exists", nodeIdTagValue));
            } else {
                //check what service is providing the instance
                query = "SELECT ?service WHERE {?service mrs:providesVM <" + node.asResource() + ">}";
                ResultSet r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify service that provides Instance: %s", node));
                }

                //find the Vpc that the node will be in
                query = "SELECT ?vpc WHERE {?vpc nml:hasNode <" + node.asResource() + ">}";
                r1 = executeQuery(query, model, modelAdd);
                if (!r1.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify the Vpc of the node: %s", node));
                }
                QuerySolution querySolution1 = r1.next();
                RDFNode vpc = querySolution1.get("vpc");
                String vpcId = ResourceTool.getResourceName(vpc.asResource().toString(), AwsPrefix.vpc);

                //to find the subnet the node is in first  find the port the node uses
                query = "SELECT ?port WHERE {<" + node.asResource() + "> nml:hasBidirectionalPort ?port}";
                ResultSet r2 = executeQuery(query, model, modelAdd);
                if (!r2.hasNext()) {
                    throw new EJBException(String.format("model addition does not specify the subnet that the node is: %s", node));
                }
                List<String> portsId = new ArrayList();
                RDFNode lastPort = null;
                while (r2.hasNext())//there could be multiple network interfaces attached to the instance
                {
                    QuerySolution querySolution2 = r2.next();
                    RDFNode port = querySolution2.get("port");
                    String id = ResourceTool.getResourceName(port.asResource().toString(), AwsPrefix.nic);
                    portsId.add(ec2Client.getResourceId(id));
                    lastPort = port;
                }

                //find the EBS volumes that the instance uses
                query = "SELECT ?volume WHERE {<" + node.asResource() + ">  mrs:hasVolume  ?volume}";
                ResultSet r4 = executeQuery(query, model, modelAdd);
                List<String> volumesId = new ArrayList();
                while (r4.hasNext())//there could be multiple volumes attached to the instance
                {
                    QuerySolution querySolution4 = r4.next();
                    RDFNode volume = querySolution4.get("volume");
                    String id = ResourceTool.getResourceName(volume.asResource().toString(), AwsPrefix.volume);
                    volumesId.add(ec2Client.getVolumeId(id));
                }
                
                //get instance type, image, secgroup and keypair names
                query = "SELECT ?type WHERE {<" + node.asResource() + "> mrs:type ?type}";
                ResultSet r5 = executeQuery(query, model, modelAdd);
                String flavorID = "t2.micro";
                String imageID = "ami-146e2a7c";
                String keypairName = "driver_key";
                String secgroupName = "default";
                String instanceTypes = null;
                while (r5.hasNext()) {
                    QuerySolution q2 = r5.next();
                    RDFNode type = q2.get("type");
                    if (instanceTypes == null) {
                        instanceTypes = type.toString();
                    } else {
                        instanceTypes += "," + type.toString();
                    }
                }
                if (instanceTypes != null) {
                    String[] typeItems = instanceTypes.split(",|;|:");
                    for (String typename: typeItems) {
                        String value = null;
                        if (typename.contains("+") || typename.contains("=")) {
                            value = typename.split("\\+|=")[1];
                        } else {
                            continue;
                        }
                        if (typename.contains("flavor") || typename.contains("instance")) {
                            flavorID = value;
                        } else if (typename.startsWith("image")) {
                            imageID = value;
                        } else if (typename.contains("keypair")) {
                            keypairName = value;
                        } else if (typename.contains("secgroup")) {
                            secgroupName = value;
                        }
                    }
                }
                //put request for new instance
                requests += String.format("RunInstancesRequest %s %s %s %s %s ", imageID, flavorID, keypairName, secgroupName, nodeIdTagValue);

                //put the root device of the instance
                query = "SELECT ?volume ?deviceName ?size ?type  WHERE {"
                        + "<" + node.asResource() + ">  mrs:hasVolume  ?volume ."
                        + "?volume mrs:target_device ?deviceName ."
                        + "?volume mrs:disk_gb ?size ."
                        + "?volume mrs:value ?type}";
                ResultSet r6 = executeQuery(query, model, modelAdd);
                boolean hasRootVolume = false;
                while (r6.hasNext()) {
                    QuerySolution querySolution4 = r6.next();
                    RDFNode volume = querySolution4.get("volume");
                    String volumeTag = ResourceTool.getResourceName(volume.asResource().toString(), AwsPrefix.volume);
                    String type = querySolution4.get("type").asLiteral().toString();
                    String size = querySolution4.get("size").asLiteral().toString();
                    String deviceName = querySolution4.get("deviceName").asLiteral().toString();
                    if (deviceName.equals("/dev/sda1") || deviceName.equals("/dev/xvda")) {
                        hasRootVolume = true;
                        requests += String.format("%s %s %s %s ", type, size, deviceName, volumeTag);
                    }
                }
                if (hasRootVolume == false) {
                    requests += "any any any any ";
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
    private String attachVolumeRequests(OntModel model, OntModel modelAdd) {
        String requests = "";

        //check fornew association between intsnce and volume
        String query = "SELECT  ?node ?volume  WHERE {?node  mrs:hasVolume  ?volume}";

        ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            RDFNode node = querySolution1.get("node");
            String nodeTagId = ResourceTool.getResourceName(node.asResource().toString(), AwsPrefix.instance);
            RDFNode volume = querySolution1.get("volume");
            String volumeTagId = ResourceTool.getResourceName(volume.asResource().toString(), AwsPrefix.volume);
            String volumeId = ec2Client.getVolumeId(volumeTagId);

            query = "SELECT ?deviceName WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName}";
            ResultSet r2 = executeQuery(query, model, modelAdd);
            if (!r2.hasNext()) {
                throw new EJBException(String.format("volume device name is not specified for volume %s in the model addition", volume));
            }

            QuerySolution querySolution2 = r2.next();

            RDFNode deviceName = querySolution2.get("deviceName");
            String device = deviceName.asLiteral().toString();

            if (!device.equals("/dev/sda1") && !device.equals("/dev/xvda")) {
                String nodeId = ec2Client.getInstanceId(nodeTagId);

                Instance ins = ec2Client.getInstance(nodeId);
                Volume vol = ec2Client.getVolume(volumeId);
                if (vol == null) {
                    query = "SELECT ?deviceName ?size ?type WHERE{<" + volume.asResource() + "> mrs:target_device ?deviceName}";
                    r2 = executeQuery(query, model, modelAdd);
                    if (!r2.hasNext()) {
                        throw new EJBException(String.format("volume %s is not well specified in volume addition", volume));
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
     * Accept/reject a virtualInterface connection for direct connect
     * ****************************************************************
     */
    public String acceptRejectVirtualInterfaceRequests(OntModel model, OntModel modelAdd) {
        String requests = "";

        //@TODO: public VLAN with hasLabel in delta model will be intepreted as "Accept" request
        
        //check for aliasing of an interface
        String query = "SELECT  ?x ?y  WHERE {?x  nml:isAlias  ?y}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            //we dont know who is the gateway and who is the interface
            QuerySolution q = r.next();
            RDFNode x = q.get("x");
            RDFNode y = q.get("y");
            RDFNode gateway = null;
            RDFNode vInterface = null;

            //check to see if y is the vgw
            //check to see if x is the virtual interface
            query = "SELECT  ?tag WHERE {<" + x.asResource() + ">  a  nml:BidirectionalPort ."
                    + "<" + x.asResource() + "> mrs:type \"direct-connect-vif\"}";
            ResultSet r1 = executeQueryUnion(query, model, modelAdd);
            if (r1.hasNext()) {
                gateway = y;
                vInterface = x;
            }

            //check to see if x is the vgw
            //check to see if y is the virtual interface
            query = "SELECT  ?tag WHERE {<" + y.asResource() + ">  a  nml:BidirectionalPort ."
                    + "<" + y.asResource() + "> mrs:type \"direct-connect-vif\"}";
            r1 = executeQueryUnion(query, model, modelAdd);
            if (r1.hasNext()) {
                gateway = x;
                vInterface = y;
            } //the delta model might be used for something else, just skip this loop
            else {
                continue;
            }

            //one resource is aliased to the second resource, make sure that the 
            //reverse also happens in the delta model
            query = "SELECT  ?a  WHERE {<" + y.asResource() + ">  nml:isAlias  <" + x.asResource() + ">}";
            r1 = executeQuery(query, emptyModel, modelAdd);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("%s is aliased to %s but %s is not aliased to %s ", x, y, y, x));
            }

            //make sure that the gateway is a virtual private gateway
            query = "SELECT ?tag WHERE {<" + gateway.asResource() + "> mrs:type \"vpn-gateway\"}";
            r1 = executeQueryUnion(query, model, modelAdd);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("Gateway %s is not a vpn gateway", gateway));
            }

            //make sure the virtual private gateway has not been associated with other dx virtual interface
            query = "SELECT ?other ?name WHERE {"
                    + "<" + gateway.asResource() + "> nml:isAlias ?other. "
                    + "?other  a  nml:BidirectionalPort. "
                    + "?other mrs:type \"direct-connect-vif\" "
                    + "FILTER (?other != <" + vInterface.asResource() + ">)"
                    + "}";
            r1 = executeQuery(query, emptyModel, model);
            if (r1.hasNext()) {
                Resource otherDxvif = r1.next().getResource("other");
                throw new EJBException(String.format("Trying to accociate VGW %s with %s but it is already associated with %s",  gateway, vInterface, otherDxvif));
            }
            
            String gatewayIdTag = ResourceTool.getResourceName(gateway.asResource().toString(), AwsPrefix.gateway);
            String interfaceId = null;
            query = "SELECT ?name WHERE {"
                    + "<" + vInterface.asResource() + "> nml:name ?name. "
                    + "}";
            r1 = executeQuery(query, emptyModel, model);
            if (r1.hasNext()) {
                interfaceId = r1.nextSolution().get("name").toString();
            }
            if (interfaceId == null) {
                query = "SELECT ?vlan WHERE {"
                        + "<" + vInterface.asResource() + "> nml:hasLabel ?label. "
                        + "?label nml:value ?vlan. "
                        + "}";
                r1 = executeQuery(query, emptyModel, model);
                if (!r1.hasNext()) {
                    continue;
                }
                interfaceId = r1.nextSolution().get("vlan").toString();
            }
            requests += String.format("AcceptVirtualInterface %s %s \n", interfaceId, gatewayIdTag);
        }
        return requests;
    }

    /**
     * ****************************************************************
     * shut down and delete an available virtual interface
     * ****************************************************************
     */
    public String deleteVirtualInterfaceRequests(OntModel model, OntModel modelReduct) {
        String requests = "";

        //@TODO: public VLAN with hasLabel in delta model will be intepreted as "Delete" request
        
        //check for aliasing of an interface
        String query = "SELECT  ?x ?y  WHERE {?x  nml:isAlias  ?y}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            //we dont know who is the gateway and who is the interface
            QuerySolution q = r.next();
            RDFNode x = q.get("x");
            RDFNode y = q.get("y");
            RDFNode gateway = null;
            RDFNode vInterface = null;

            //check to see if x is the virtual interface
            query = "SELECT  ?tag WHERE {<" + x.asResource() + ">  a  nml:BidirectionalPort ."
                    + "<" + x.asResource() + "> mrs:type \"direct-connect-vif\"}";
            ResultSet r1 = executeQueryUnion(query, model, modelReduct);
            if (r1.hasNext()) {
                gateway = y;
                vInterface = x;
            }

            //check to see if y is the interface
            //check to see if x is the virtual interface
            query = "SELECT  ?tag WHERE {<" + y.asResource() + ">  a  nml:BidirectionalPort ."
                    + "<" + y.asResource() + "> mrs:type \"direct-connect-vif\"}";
            r1 = executeQueryUnion(query, model, modelReduct);
            if (r1.hasNext()) {
                gateway = x;
                vInterface = y;
            } //the delta model might be used for something else, just skip this loop
            else {
                continue;
            }

            //one resource is aliased to the second resource, make sure that the 
            //reverse also happens in the elta model
            query = "SELECT  ?a  WHERE {<" + y.asResource() + ">  nml:isAlias  <" + x.asResource() + ">}";
            r1 = executeQuery(query, emptyModel, modelReduct);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("%s is aliased to %s but %s is not aliased to %s ", x, y, y, x));
            }

            //make sure that the gateway is a virtual private gateway
            query = "SELECT ?tag WHERE {<" + gateway.asResource() + "> mrs:type \"vpn-gateway\"}";
            r1 = executeQueryUnion(query, model, modelReduct);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("Gateway %s is not a vpn gateway", gateway));
            }

            String gatewayIdTag = ResourceTool.getResourceName(gateway.asResource().toString(), AwsPrefix.gateway);
            String interfaceId = null;
            query = "SELECT ?name WHERE {"
                    + "<" + vInterface.asResource() + "> nml:name ?name. "
                    + "}";
            r1 = executeQuery(query, emptyModel, model);
            if (r1.hasNext()) {
                interfaceId = r1.nextSolution().get("name").toString();
            }
            if (interfaceId == null) {
                query = "SELECT ?vlan WHERE {"
                        + "<" + vInterface.asResource() + "> nml:hasLabel ?label. "
                        + "?label nml:value ?vlan. "
                        + "}";
                r1 = executeQuery(query, emptyModel, model);
                if (!r1.hasNext()) {
                    continue;
                }
                interfaceId = r1.nextSolution().get("vlan").toString();
            }
            
            requests += String.format("DeleteVirtualInterface %s %s \n", interfaceId, gatewayIdTag);
        }
        return requests;
    }

    public String associateElasticIpRequests(OntModel model, OntModel modelAdd) {
        String requests = "";

        // check if node/port has floating-ip or elastic-ip annotion 
        String query = "SELECT ?node ?port ?fip WHERE {"
                + "?node nml:hasBidirectionalPort ?port ."
                + "?node a nml:Node. "
                + "?port mrs:hasNetworkAddress ?addr. "
                + "?addr mrs:type ?fiptype. "
                + "?addr mrs:value ?fip. "
                + "FILTER (?fiptype = \"floating-ip\" || ?fiptype = \"elastic-ip\") "
                + "}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        if (r.hasNext()) {
            QuerySolution q = r.next();
            RDFNode server = q.get("node");
            RDFNode fip = q.get("fip");
            String nodeIdTag = ResourceTool.getResourceName(server.asResource().toString(), AwsPrefix.instance);
            String instanceId = ec2Client.getInstanceId(nodeIdTag);
            String floatingIp = fip.toString();
            requests  +=  String.format("AssociateElasticIpRequest %s %s \n", instanceId, floatingIp);
        }
        return requests;
    }    
}
