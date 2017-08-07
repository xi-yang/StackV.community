/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2015-2016
 * Modified by: Adam Smith 2017

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

import net.maxgigapop.mrs.driver.aws.AwsAuthenticateService;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.AmazonEC2Client;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author muzcategui
 */
public class AwsEC2Get {

    private AmazonEC2AsyncClient client = null;

    private List<Vpc> vpcs = null;
    private List<Instance> instances = null;
    private List<Subnet> subnets = null;
    private List<SecurityGroup> securityGroups = null;
    private List<NetworkAcl> acls = null;
    private List<RouteTable> routeTables = null;
    private List<Address> elasticIps = null;
    private List<CustomerGateway> customerGateways = null;
    private List<Volume> volumes = null;
    private List<NetworkInterface> networkInterfaces = null;
    private List<InternetGateway> internetGateways = null;
    private List<VpnGateway> virtualPrivateGateways = null;
    private List<VpnConnection> vpnConnections = null;
    
    final private long delayMax = 32000L;

    public AwsEC2Get(String access_key_id, String secret_access_key, Regions region) {
        AwsAuthenticateService authenticate = new AwsAuthenticateService(access_key_id, secret_access_key);
        this.client = authenticate.AwsAuthenticateEC2ServiceAsync(Region.getRegion(region));
        

        //get all the vpcs of the account
        DescribeVpcsResult VpcsResult = this.client.describeVpcs();
        this.vpcs = VpcsResult.getVpcs();

        //get all the instances of the account
        DescribeInstancesResult instancesResult = this.client.describeInstances();
        List<Reservation> reservation = instancesResult.getReservations();
        instances = new ArrayList();
        if (reservation.size() >= 1) {
            for (Reservation t : reservation) {
                for (Instance i : t.getInstances()) {
                    if (i.getState().getCode() != 48) { //do not add terminated instances
                        instances.add(i);
                    }
                }
            }
        }
        
        //get all the subnets in the account
        DescribeSubnetsResult subnetsResult = this.client.describeSubnets();
        this.subnets = subnetsResult.getSubnets();

        //get all the security groups in the account
        DescribeSecurityGroupsResult securityGroupsResult = this.client.describeSecurityGroups();
        this.securityGroups = securityGroupsResult.getSecurityGroups();

        //get all the Acls under the account
        DescribeNetworkAclsResult aclsResult = this.client.describeNetworkAcls();
        acls = aclsResult.getNetworkAcls();

        //get all the routeTables of the account 
        DescribeRouteTablesResult tablesResult = this.client.describeRouteTables();
        routeTables = tablesResult.getRouteTables();

        //get all the elastic Ip's under the account
        DescribeAddressesResult elasticIpsResult = this.client.describeAddresses();
        elasticIps = elasticIpsResult.getAddresses();

        //get all the network interfaces under the account
        DescribeNetworkInterfacesResult networkInterfacesResult = this.client.describeNetworkInterfaces();
        networkInterfaces = networkInterfacesResult.getNetworkInterfaces();

        //get all the customer gatewyas under the account
        DescribeCustomerGatewaysResult gatewaysResult = this.client.describeCustomerGateways();
        customerGateways = gatewaysResult.getCustomerGateways();

        //get all the Internet gateways under the account
        DescribeInternetGatewaysResult internetGatewaysResult = this.client.describeInternetGateways();
        internetGateways = internetGatewaysResult.getInternetGateways();

        //get all the virtual private gateways under the account
        DescribeVpnGatewaysResult vpnGatewaysResult = this.client.describeVpnGateways();
        virtualPrivateGateways = vpnGatewaysResult.getVpnGateways();

        //Added 6/7
        //get all the vpnConnctions under the account
        DescribeVpnConnectionsResult vpnConnectionResult = this.client.describeVpnConnections();
        vpnConnections = vpnConnectionResult.getVpnConnections();
        
        //get all the volumes under the account
        DescribeVolumesResult volumesResult = this.client.describeVolumes();
        volumes = volumesResult.getVolumes();
    }

    //get the client of this EC2 Account account
    public AmazonEC2AsyncClient getClient() {
        return client;
    }


    //get the list of all the VPCs of the client
    public List<Vpc> getVpcs() {
        return vpcs;
    }

    //get a single Vpc based on its ID  from a list of Vpcs
    public Vpc getVpc(String id) {
        for (Vpc vpc : vpcs) {
            if (vpc.getVpcId().equals(id)) {
                return vpc;
            }
        }
        return null;
    }

    //get all the instances under the aws account
    public List<Instance> getEc2Instances() {
        return instances;
    }

    //get all the instances associated with a Vpc  or subnet in the account
    public List<Instance> getInstances(String id) {
        List<Instance> ins = new ArrayList();
        for (Instance i : instances) {
            if (i.getVpcId() != null && i.getVpcId().equals(id)) {
                ins.add(i);
            }
            if (i.getSubnetId() != null && i.getSubnetId().equals(id)) {
                ins.add(i);
            }
        }
        return ins;
    }

    //get all the instances associated with a Vpc  or subnet in the account
    public Instance getInstance(String id) {
        for (Instance i : instances) {
            if (i.getInstanceId().equals(id)) {
                return i;
            }
        }
        return null;
    }

    //get all the attached network interfaces for an instance
    public static List<InstanceNetworkInterface> getInstanceInterfaces(Instance i) {
        return i.getNetworkInterfaces();
    }

    //get all the network interfaces under the account
    public List<NetworkInterface> getNetworkInterfaces() {
        return networkInterfaces;
    }

    //get all the network interfaces under the account
    public NetworkInterface getNetworkInterface(String id) {
        for (NetworkInterface n : networkInterfaces) {
            if (n.getNetworkInterfaceId().equals(id)) {
                return n;
            }
        }
        return null;
    }

    //get the list of all the subnets associated with an account
    public List<Subnet> getSubnets() {
        return subnets;
    }

    //get the subnet under a vpc or a single subnet  based on its Id from a list
    //of subnets
    public List<Subnet> getSubnets(String id) {
        List<Subnet> subnetList = new ArrayList();
        for (Subnet sub : subnets) {
            if (sub.getVpcId().equals(id)) {
                subnetList.add(sub);
            } else if (sub.getSubnetId().equals(id)) {
                subnetList.add(sub);
                return subnetList;
            }
        }
        return subnetList;
    }

    //get a single subnet based on its id
    public Subnet getSubnet(String id) {
        for (Subnet sub : subnets) {
            if (sub.getSubnetId().equals(id)) {
                return sub;
            }
        }
        return null;
    }

    //get all the security groups from an AWS account
    public List<SecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    //get all the security groups from a speicific VPC or a single group based 
    //on either a vpc Id or a group Id  from a list of security groups
    public List<SecurityGroup> getSecurityGroups(String id) {
        List<SecurityGroup> group = new ArrayList();
        for (SecurityGroup gr : securityGroups) {
            if (gr.getGroupName().equals(id)) {
                group.add(gr);
                return group;                
            } else if (gr.getGroupId().equals(id)) {
                group.add(gr);
                return group;
            } else if (gr.getVpcId() != null && gr.getVpcId().equals(id)) {
                group.add(gr);
            } 
        }
        return group;
    }
    
    public SecurityGroup getSecurityGroup(String id) {
        for (SecurityGroup gr : securityGroups) {
            if (gr.getGroupName().equals(id)) {
                return gr;                
            } else if (gr.getGroupId().equals(id)) {
                return gr;                
            } 
        }
        return null;
    }


    //get all the ACLs withinan AWS account
    public List<NetworkAcl> getACLs() {
        return acls;
    }

    //get all the ACLs within a VPC or an ACl based on its id
    public List<NetworkAcl> getACLs(String id) {
        List<NetworkAcl> rules = new ArrayList();
        for (NetworkAcl t : acls) {
            if (t.getVpcId().equals(id)) {
                rules.add(t);
            } else if (t.getNetworkAclId().equals(id)) {
                rules.add(t);
                return rules;
            }
        }
        return rules;
    }

    //get all the routing tables under a vpc
    public List<RouteTable> getRoutingTables() {
        return routeTables;
    }

    //get a single routing table based on its id 
    public RouteTable getRoutingTable(String id) {
        for (RouteTable t : routeTables) {
            if (t.getRouteTableId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    //get all the routing tables under a vpc or a single route table based on id
    public List<RouteTable> getRoutingTables(String id) {
        List<RouteTable> rt = new ArrayList();
        for (RouteTable t : routeTables) {
            if (t.getVpcId().equals(id)) {
                rt.add(t);
            } else if (t.getRouteTableId().equals(id)) {
                rt.add(t);
                return rt;
            }
        }
        return rt;
    }

    //get all the  elastic Ips under the account
    public List<Address> getElasticIps() {
        return elasticIps;
    }

    //get all the  elastic Ips under an 
    public Address getElasticIp(String id) {
        for (Address t : elasticIps) {
            if (t.getPublicIp().equals(id)) {
                return t;
            }
        }
        return null;
    }

    //get all the customer gateways under the aws account
    public List<CustomerGateway> getCustomerGateways() {
        return customerGateways;
    }

    //get a specific customer gateway based on its id 
    public CustomerGateway getCustomerGateway(String id) {
        for (CustomerGateway t : customerGateways) {
            if (t.getCustomerGatewayId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    //get all the internet gateways under the aws account
    public List<InternetGateway> getInternetGateways() {
        return internetGateways;
    }

    //get a specific internet gateway based on its id or vpc id
    public InternetGateway getInternetGateway(String id) {
        for (InternetGateway t : internetGateways) {
            if (t.getInternetGatewayId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    //get a specificic internet gateway fro a vpc
    public InternetGateway getInternetGateway(Vpc v) {
        String vpcId = v.getVpcId();
        for (InternetGateway t : internetGateways) {
            for (InternetGatewayAttachment att : t.getAttachments()) {
                if (att.getVpcId().equals(vpcId)) {
                    return t;
                }
            }
        }
        return null;
    }

    //get the virtual private gateways
    public List<VpnGateway> getVirtualPrivateGateways() {
        return virtualPrivateGateways;
    }

    //get virtual private gateways associated with a specific vpc
    public VpnGateway getVirtualPrivateGateway(Vpc v) {
        String vpcId = v.getVpcId();
        for (VpnGateway vpn : virtualPrivateGateways) {
            for (VpcAttachment att : vpn.getVpcAttachments()) {
                if (att.getVpcId().equals(vpcId) && att.getState().equals(AttachmentStatus.Attached.toString())) {
                    return vpn;
                }
            }
        }
        return null;
    }

    //get a specific virtual private gateway
    public VpnGateway getVirtualPrivateGateway(String id) {
        for (VpnGateway t : virtualPrivateGateways) {
            if (t.getVpnGatewayId().equals(id)) {
                return t;
            }
        }
        return null;
    }
    
    //Added 6/7/17
    //get the VPN Connections
    public List<VpnConnection> getVpnConnections() {
        return vpnConnections;
    }

    public VpnConnection getVpnConnection(String id) {
        for (VpnConnection v : vpnConnections) {
            if (v.getVpnConnectionId().equals(id)) {
                return v;
            }
        }
        return null;
    }
    
    //get all the volumes under the aws account
    public List<Volume> getVolumes() {
        return this.volumes;
    }

    //get a  volume with a particular Id from a list of volumes
    public Volume getVolume(String id) {
        for (Volume v : volumes) {
            if (v.getVolumeId().equals(id)) {
                return v;
            }
        }
        return null;
    }

    //get a list of all the volumes that have an attachement to an instance
    public List<Volume> getVolumesWithAttachement(Instance i) {
        List<Volume> volume = new ArrayList();
        for (Volume v : this.volumes) {
            for (VolumeAttachment va : v.getAttachments()) {
                if (va.getInstanceId().equals(i.getInstanceId()) && !volume.contains(v)) {
                    volume.add(v);
                }
            }
        }
        return volume;
    }

    //get a List of all the volumes withouh an attachement
    public List<Volume> getVolumesWithoutAttachment() {
        List<Volume> volume = new ArrayList();

        for (Volume v : this.volumes) {
            if (v.getAttachments().isEmpty()) {
                volume.add(v);
            }
        }
        return volume;
    }

    //get Id tag returns the resource Id if no Id tags were found
    public String getIdTag(String resourceId) {
        Filter filter = new Filter();
        filter.withName("resource-id")
                .withValues(resourceId);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = this.describeTagsUnlimit(tagRequest);

        for (TagDescription td : descriptions) {
            if (td.getKey().equals("id")) {
                return td.getValue();
            }
        }
        return resourceId;
    }

    //from a peering connection get the vpc id
    public String getPeerVpc(String connectionId) {
        DescribeVpcPeeringConnectionsRequest request = new DescribeVpcPeeringConnectionsRequest();
        request.withVpcPeeringConnectionIds(connectionId);
        DescribeVpcPeeringConnectionsResult vpcConnect = client.describeVpcPeeringConnections(request);
        String vpcId = vpcConnect.getVpcPeeringConnections().get(0).getAccepterVpcInfo().getVpcId();

        return vpcId;
    }

    /**
     * ****************************************************************
     * function to wait for correct VPC status
     * ****************************************************************
     */
    public void vpcStatusCheck(String id, String status) {
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        request.withVpcIds(id);
        
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                Vpc resource = client.describeVpcs(request).getVpcs().get(0);
                if (resource.getState().toLowerCase().equals(status.toLowerCase())) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }


    /**
     * ****************************************************************
     * function to wait for correct subnet status
     * ****************************************************************
     */
    public void subnetCreationCheck(String id, String status) {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest();
        request.withSubnetIds(id);

        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                Subnet resource = client.describeSubnets(request).getSubnets().get(0);
                if (resource.getState().toLowerCase().equals(status.toLowerCase())) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for subnet deletion
     * ****************************************************************
     */
    public void subnetDeletionCheck(String id, String status) {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest();
        request.withSubnetIds(id);

        long delay = 1000L;
        while (true) {
            try {
                Subnet resource = client.describeSubnets(request).getSubnets().get(0);
                //break;
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for routing Table creation
     * ****************************************************************
     */
    public void RouteTableCreationCheck(String id) {
        DescribeRouteTablesRequest request = new DescribeRouteTablesRequest();
        request.withRouteTableIds(id);

        long delay = 1000L;
        while (true) {
            try {
                RouteTable resource = client.describeRouteTables(request).getRouteTables().get(0);
                break;
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for routing Table deletion
     * ****************************************************************
     */
    public void RouteTableDeletionCheck(String id) {
        DescribeRouteTablesRequest request = new DescribeRouteTablesRequest();
        request.withRouteTableIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                RouteTable resource = client.describeRouteTables(request).getRouteTables().get(0);
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Internet gateway addition
     * ****************************************************************
     */
    public void internetGatewayAdditionCheck(String id) {
        DescribeInternetGatewaysRequest request = new DescribeInternetGatewaysRequest();
        request.withInternetGatewayIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                InternetGateway resource = client.describeInternetGateways(request).getInternetGateways().get(0);
                break;
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Internet gateway deletion
     * ****************************************************************
     */
    public void internetGatewayDeletionCheck(String id) {
        DescribeInternetGatewaysRequest request = new DescribeInternetGatewaysRequest();
        request.withInternetGatewayIds(id);
        long delay = 0;
        while (true) {
            delay *= 2;
            try {
                InternetGateway resource = client.describeInternetGateways(request).getInternetGateways().get(0);
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Internet gateway attachment
     * ****************************************************************
     */
    public void internetGatewayAttachmentCheck(String id) {
        DescribeInternetGatewaysRequest request = new DescribeInternetGatewaysRequest();
        request.withInternetGatewayIds(id);

        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                InternetGateway resource = client.describeInternetGateways(request).getInternetGateways().get(0);
                if (!resource.getAttachments().isEmpty()) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                }
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Internet gateway attachment
     * ****************************************************************
     */
    public void internetGatewayDetachmentCheck(String id) {
        DescribeInternetGatewaysRequest request = new DescribeInternetGatewaysRequest();
        request.withInternetGatewayIds(id);

        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                InternetGateway resource = client.describeInternetGateways(request).getInternetGateways().get(0);
                if (resource.getAttachments().isEmpty()) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                }
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }
    
    //TODO: add wait functions for vpn connection addition and deletion. 6/9
    
    
    /**
     * ****************************************************************
     * function to wait for Vpn gateway addition
     * ****************************************************************
     */
    public void vpnGatewayAdditionCheck(String id) {
        DescribeVpnGatewaysRequest request = new DescribeVpnGatewaysRequest();
        request.withVpnGatewayIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                VpnGateway resource = client.describeVpnGateways(request).getVpnGateways().get(0);
                break;
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Vpn gateway deletion
     * ****************************************************************
     */
    public void vpnGatewayDeletionCheck(String id) {
        DescribeVpnGatewaysRequest request = new DescribeVpnGatewaysRequest();
        request.withVpnGatewayIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                VpnGateway resource = client.describeVpnGateways(request).getVpnGateways().get(0);
                if (resource.getState().toLowerCase().equals("deleted")) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Vpn gateway attachment
     * ****************************************************************
     */
    public void vpnGatewayAttachmentCheck(String id, String vpcId) {
        DescribeVpnGatewaysRequest request = new DescribeVpnGatewaysRequest();
        request.withVpnGatewayIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                VpnGateway resource = client.describeVpnGateways(request).getVpnGateways().get(0);
                VpcAttachment att = new VpcAttachment();
                att.withState(AttachmentStatus.Attached)
                        .withVpcId(vpcId);
                if (!resource.getVpcAttachments().isEmpty() && resource.getVpcAttachments().contains(att)) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                }
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Vpn gateway dettachment
     * ****************************************************************
     */
    public void vpnGatewayDetachmentCheck(Future<Void> asyncResult) throws ExecutionException {
        long delay = 1000L;
        while (true) {
            if (asyncResult.isDone()) {
                try {
                    asyncResult.get();
                    break;
                } catch (InterruptedException ex) {
                    Logger.getLogger(AwsEC2Get.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                sleep(delay);
            } catch (InterruptedException ex1) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Volume deletion
     * ****************************************************************
     */
    public void volumeAdditionCheck(String id, String status) {
        DescribeVolumesRequest request = new DescribeVolumesRequest();
        request.withVolumeIds(id);

        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                Volume resource = client.describeVolumes(request).getVolumes().get(0);
                if (resource.getState().toLowerCase().equals(status)) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Volume deletion
     * ****************************************************************
     */
    public void volumeDeletionCheck(String id, String status) {
        DescribeVolumesRequest request = new DescribeVolumesRequest();
        request.withVolumeIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                Volume resource = client.describeVolumes(request).getVolumes().get(0);
                if (resource.getState().toLowerCase().equals(status)) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Volume attachment
     * ****************************************************************
     */
    public void volumeAttachmentCheck(String id) {
        DescribeVolumesRequest request = new DescribeVolumesRequest();
        request.withVolumeIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                Volume resource = client.describeVolumes(request).getVolumes().get(0);
                if (!resource.getAttachments().isEmpty()) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for Volume detachment
     * ****************************************************************
     */
    public void volumeDetachmentCheck(String id) {
        DescribeVolumesRequest request = new DescribeVolumesRequest();
        request.withVolumeIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                Volume resource = client.describeVolumes(request).getVolumes().get(0);
                if (resource.getAttachments().isEmpty()) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for NetworkInterface addition
     * ****************************************************************
     */
    public void PortAdditionCheck(String id) {
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        request.withNetworkInterfaceIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                NetworkInterface resource = client.describeNetworkInterfaces(request).getNetworkInterfaces().get(0);
                if (resource != null) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for NetworkInterface deletion
     * ****************************************************************
     */
    public void PortDeletionCheck(String id) {
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        request.withNetworkInterfaceIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                NetworkInterface resource = client.describeNetworkInterfaces(request).getNetworkInterfaces().get(0);
                if (resource == null) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for NetworkInterface attachment
     * ****************************************************************
     */
    public void PortAttachmentCheck(String id) {
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        request.withNetworkInterfaceIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                NetworkInterface resource = client.describeNetworkInterfaces(request).getNetworkInterfaces().get(0);
                if (resource.getAttachment() != null) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for NetworkInterface attachment
     * ****************************************************************
     */
    public void PortDetachmentCheck(String id) {
        DescribeNetworkInterfacesRequest request = new DescribeNetworkInterfacesRequest();
        request.withNetworkInterfaceIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                NetworkInterface resource = client.describeNetworkInterfaces(request).getNetworkInterfaces().get(0);
                if (resource.getAttachment() == null) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
                break;
            } catch (NullPointerException ex2) {
                break;
            }
        }
    }

    /**
     * ****************************************************************
     * function to wait for the correct instance status
     * ****************************************************************
     */
    public void instanceStatusCheck(String id, String status) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.withInstanceIds(id);
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                Instance resource = client.describeInstances(request).getReservations().get(0).getInstances().get(0);
                if (resource.getState().getName().toLowerCase().equals(status)) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }

    //Method to wait for vpn Connection created status
    public void vpnCreationCheck(String id) {
        DescribeVpnConnectionsRequest request = new DescribeVpnConnectionsRequest();
        request.withVpnConnectionIds(id);
        
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                VpnConnection resource = client.describeVpnConnections(request).getVpnConnections().get(0);
                if (resource != null) {
                    break;
                }
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }
    
    //Method to wait for vpn Connection deleted status
    public void vpnDeletionCheck(String id) {
        DescribeVpnConnectionsRequest request = new DescribeVpnConnectionsRequest();
        request.withVpnConnectionIds(id);
        
        long delay = 1000L;
        while (true) {
            delay *= 2;
            try {
                VpnConnection resource = client.describeVpnConnections(request).getVpnConnections().get(0);
                if (resource == null || resource.getState().equals(VpnState.Deleted.toString()) ) {
                    break;
                }
                
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } 
            } catch (NullPointerException ex2) {
                ;
            }
        }
    }
    
    //9/21
    //returns a VpnConnection if vgw is attached to one, else returns null 
    public VpnConnection vgwGetVpn(String vgwId) {
        String deleting = VpnState.Deleting.toString();
        String deleted = VpnState.Deleted.toString();
        for (VpnConnection v : vpnConnections) {
            if (v.getVpnGatewayId().equals(vgwId)) {
                    if (v.getState().equals(deleted)) continue;
                    if (v.getState().equals(deleting)) continue;
                    return v;
                }
            }
        return null;
    }
    
    /*
     *******************************************************************
     * Method to find root volume of instance 
     * Note: This method should only be used after instance init
     *******************************************************************
     */
    public Volume getInstanceRootDevice(Instance i) {
        long delay = 1000L;
        while (true) {
            delay *= 2; // pause for 2 ~ 32 seconds
            try {
                List<Volume> vols = this.client.describeVolumes().getVolumes();
                for (Volume vol : vols) {
                    if (!vol.getAttachments().isEmpty() && vol.getAttachments().get(0).getInstanceId().equalsIgnoreCase(i.getInstanceId())) {
                        return vol;
                    }
                }
                return null;
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax) {
                    try {
                        sleep(delay);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } else {
                    throw ex;
                }
            }
        }
    }

    /**
     * ****************************************************************
     * function that checks if the id provided is a tag; if it is, it will
     * return the resource id, otherwise it will return the input parameter
     * which is the id of the resource NOTE: does not work with volumes and
     * instances because sometimes the tags persist for a while in this
     * resources, therefore we need to check if the instance or volume
     * actually exists
     * ****************************************************************
     */
    public String getResourceId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = this.describeTagsUnlimit(tagRequest);
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
    public String getInstanceId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = this.describeTagsUnlimit(tagRequest);
        if (!descriptions.isEmpty()) {
            for (TagDescription des : descriptions) {
                Instance i = getInstance(des.getResourceId());
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
    public String getVolumeId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = this.describeTagsUnlimit(tagRequest);
        if (!descriptions.isEmpty()) {
            for (TagDescription des : descriptions) {
                Volume vol = getVolume(des.getResourceId());
                if (vol != null && !vol.getState().equals("deleted")) {
                    return des.getResourceId();
                }
            }
        }
        return tag;
    }

    /**
     * ****************************************************************
     * function to get the Id from a volume tag
     * ****************************************************************
     */
    public String getTableId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = this.describeTagsUnlimit(tagRequest);
        if (!descriptions.isEmpty()) {
            for (TagDescription des : descriptions) {
                RouteTable table = getRoutingTable(des.getResourceId());
                if (table != null) {
                    return des.getResourceId();
                }
            }
        }
        return tag;
    }

    /**
     * ****************************************************************
     * function to get the Id from a volume tag
     * ****************************************************************
     */
    public String getVpcId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = this.describeTagsUnlimit(tagRequest);
        if (!descriptions.isEmpty()) {
            for (TagDescription des : descriptions) {
                Vpc vpc = getVpc(des.getResourceId());
                if (vpc != null) {
                    return des.getResourceId();
                }
            }
        }
        return tag;
    }

    /**
     * ****************************************************************
     * function to get the Id from a vpnGateway tag
     * ****************************************************************
     */
    public String getVpnGatewayId(String tag) {
        Filter filter = new Filter();
        filter.withName("value")
                .withValues(tag);

        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = this.describeTagsUnlimit(tagRequest);
        if (!descriptions.isEmpty()) {
            for (TagDescription des : descriptions) {
                VpnGateway vpn = getVirtualPrivateGateway(des.getResourceId());
                if (vpn != null) {
                    return des.getResourceId();
                }
            }
        }
        
        /* 9/16/17
        Sometimes, if the vgw was created manually the id tag will be absent but
        the vgwID can still be recovered from the URI. If the vgw was created manually,
        the URI should have the id appended near the end.
        
        the id has the form vgw-XXXXXXXX
        */
        int pos;
        pos = tag.indexOf("vgw-");
        if (pos != -1) {
            return tag.substring(pos, pos+12);
        }
        
        return tag;
    }

    /**
     * ****************************************************************
     * Function to get the Id from a vpnConnection tag - added 6/9
     * ****************************************************************
     */
    
    public String getVpnConnectionId(String tag) {
        Filter filter = new Filter();
        filter.withName("value").withValues(tag);
        
        DescribeTagsRequest tagRequest = new DescribeTagsRequest();
        tagRequest.withFilters(filter);
        List<TagDescription> descriptions = this.describeTagsUnlimit(tagRequest);
        if (!descriptions.isEmpty()) {
            for (TagDescription desc : descriptions) {
                VpnConnection vpnc = getVpnConnection(desc.getResourceId());
                if (vpnc != null) {
                    return desc.getResourceId();
                }

            }
        }
        
        int pos;
        pos = tag.indexOf("vpn-");
        if (pos != -1) {
            return tag.substring(pos, pos+12);
        }
        
        return tag;
    }
    
    
    
    /**
     * ****************************************************************
     * function to tag a resource
     * ****************************************************************
     */
    public void tagResource(String id, String tag) {
        CreateTagsRequest tagRequest = new CreateTagsRequest();
        tagRequest.withTags(new Tag("id", tag));
        tagRequest.withResources(id);
        while (true) {
            try {
                client.createTags(tagRequest);
                break;
            } catch (AmazonServiceException e) {
            }
        }
    }

    /**
     * ************************************************************
     * Method to find a tag form a resource given the resource id
     *
     * ************************************************************ @param tag
     * @param tagName
     * @param tags
     * @return String
     */
    public String getTagValue(String tagName, List<Tag> tags) {
        if (tags == null) {
            return null;
        }
        for (Tag tag : tags) {
            if (tag.getKey().equalsIgnoreCase(tagName)) {
                return tag.getValue();
            }
        }
        return null;
    }

    // use delayMax*2 = 32 secs (doubled total wait up to 2 minute)
    private List<TagDescription> describeTagsUnlimit(DescribeTagsRequest tagRequest) {
        long delay = 1000L;
        while (true) {
            delay *= 2; 
            try {
                List<TagDescription> descriptions = client.describeTags(tagRequest).getTags();
                return descriptions;
            } catch (com.amazonaws.AmazonServiceException ex) {
                if (ex.getErrorCode().equals("RequestLimitExceeded") && delay > 0 && delay <= delayMax*2) {
                    try {
                        sleep(delay); // pause for 2 ~ 64 seconds
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } else {
                    throw ex;
                }
            }
        }
    }
}
