/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;


import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.AmazonEC2Client;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author muzcategui
 */
public class AwsEC2Get 
{
    private AmazonEC2Client client=null;
    private List<Vpc> vpcs=null;
    private List<Instance> instances=null;
    private List<Subnet> subnets=null;
    private List<SecurityGroup> securityGroups=null;
    private List<NetworkAcl> acls=null;
    private List<RouteTable> routeTables=null;
    private List<Address> elasticIps=null;
    private List<CustomerGateway> customerGateways=null;
    private List<Volume> volumes=null;
    private List<NetworkInterface> networkInterfaces=null;
    
    public AwsEC2Get(String access_key_id, String secret_access_key,Regions region)
    {
        AwsAuthenticateService authenticate=new AwsAuthenticateService(access_key_id,secret_access_key);
        this.client = authenticate.AwsAuthenticateEC2Service(Region.getRegion(region));
        
        //get all the vpcs of the account
        DescribeVpcsResult VpcsResult=this.client.describeVpcs();
        this.vpcs=VpcsResult.getVpcs();
        
        //get all the instances of the account
        DescribeInstancesResult instancesResult=this.client.describeInstances();
        List<Reservation> reservation=instancesResult.getReservations();
        instances=new ArrayList();
        if(reservation.size()>=1)
            for(Reservation t : reservation)
            {
               instances.add(t.getInstances().get(0));
            }
      
        //get all the subnets in the account
        DescribeSubnetsResult subnetsResult=this.client.describeSubnets();
        this.subnets=subnetsResult.getSubnets();
 
        //get all the security groups in the account
        DescribeSecurityGroupsResult securityGroupsResult=this.client.describeSecurityGroups();
        this.securityGroups=securityGroupsResult.getSecurityGroups();
        
        //get all the Acls under the account
        DescribeNetworkAclsResult aclsResult=this.client.describeNetworkAcls();
        acls=aclsResult.getNetworkAcls();
        
        //get all the routeTables of the account 
        DescribeRouteTablesResult tablesResult=this.client.describeRouteTables();
        routeTables=tablesResult.getRouteTables();
        
        //get all the elastic Ip's under the account
        DescribeAddressesResult elasticIpsResult=this.client.describeAddresses();
        elasticIps=elasticIpsResult.getAddresses();
        
        //get all the network interfaces under the account
        DescribeNetworkInterfacesResult networkInterfacesResult=this.client.describeNetworkInterfaces();
        networkInterfaces=networkInterfacesResult.getNetworkInterfaces();
        
        //get all the customer gatewyas under the account
        DescribeCustomerGatewaysResult gatewaysResult=this.client.describeCustomerGateways();
        customerGateways=gatewaysResult.getCustomerGateways();
        
        //get all the volumes under the account
        DescribeVolumesResult volumesResult=this.client.describeVolumes();
        volumes=volumesResult.getVolumes();
    }
    
    //get the client of this EC2 Account account
    public AmazonEC2Client getClient()
    {
        return client;
    }
    //get the list of all the VPCs of the client
    public  List<Vpc>  getVpcs()
    {
       return vpcs;
    }
    
    //get a single Vpc based on its ID  from a list of Vpcs
    public  static  Vpc  getVpc(List<Vpc> vpcs,String id)
    {
        for (Vpc vpc : vpcs) {
            if(vpc.getVpcId().equals(id))
                return vpc;
        }
        return null;
    }
    
    //get all the instances under the aws account
    public List<Instance> getEc2Instances()
    {
        return instances;
    }
    
    //get all the instances associated with a Vpc  or subnet in the account
    public List<Instance> getInstances(String id )
    {
        List<Instance> ins= new ArrayList();
        for(Instance i: instances)
        {
            if(i.getVpcId()!=null && i.getVpcId().equals(id))
                ins.add(i);
            if(i.getSubnetId()!=null && i.getSubnetId().equals(id))
                ins.add(i);
        }
        return ins;
    }
    
        
    //get all the attached network interfaces for an instance
    public static List<InstanceNetworkInterface> getInstanceInterfaces(Instance i)
    {
        return i.getNetworkInterfaces();
    }
    
    //get all the network interfaces under the account
    public List<NetworkInterface> getNetworkInterfaces()
    {
        return networkInterfaces;
    }

    
    //get the list of all the subnets associated with an account
    public  List<Subnet>  getSubnets()
    {
        return subnets;
    }
    
    //get the subnet under a vpc or a single subnet  based on its Id from a list
    //of subnets
    public static List<Subnet> getSubnets(List<Subnet> subnets,String id)
    {
        List<Subnet> subnetList=new ArrayList();
        for(Subnet sub : subnets)
        {
            if(sub.getVpcId().equals(id))
                subnetList.add(sub);
            else if(sub.getSubnetId().equals(id))
            {
                subnetList.add(sub);
                return subnetList;
            }
        }
        return subnetList;
    }
    
    
    //get all the security groups from an AWS account
    public List<SecurityGroup> getSecurityGroups()
    {
       return securityGroups;
    }
    
    //get all the security groups from a speicific VPC or a single group based 
    //on either a vpc Id or a group Id  from a list of security groups
    public static List<SecurityGroup> getSecurityGroups(List<SecurityGroup> securityGroup,String id)
    {
        List<SecurityGroup> group=new ArrayList();
        for(SecurityGroup gr : securityGroup )
        {
           if(gr.getVpcId().equals(id))
               group.add(gr);
           else if(gr.getGroupId().equals(id))
           {
               group.add(gr);
               return group;
           }
        }
        return group;
    }
    
    
    //get all the ACLs withinan AWS account
    public  List<NetworkAcl> getACLs()
    {
        return acls;
    }
    
    //get all the ACLs within a VPC or an ACl based on its id
    public static List<NetworkAcl> getACLs(List<NetworkAcl> aclList, String id)
    {
       List<NetworkAcl> acls=new ArrayList();
       for(NetworkAcl t : aclList)
       {
           if(t.getVpcId().equals(id))
               acls.add(t);
           else if(t.getNetworkAclId().equals(id))
           {
               acls.add(t);
               return acls;
           }
       }
       return acls;
    }
    
    
    //get all the routing tables under a vpc
    public  List<RouteTable> getRoutingTables()
    {
        return routeTables;
    }
    
    //get all the routing tables under a vpc or a single route table based on id
    public static List<RouteTable> getRoutingTables(List<RouteTable> tables,String id)
    {
        List<RouteTable> routeTables=new ArrayList();
        for(RouteTable t : tables)
        {
            if (t.getVpcId().equals(id))
                routeTables.add(t);
            else if(t.getRouteTableId().equals(id))
            {
                routeTables.add(t);
                return routeTables;
            }
        }
        return routeTables;
    }
    
    
    //get all the  elastic Ips under the account
    public List<Address> getElasticIps()
    {
        return elasticIps;
    }
    
    //get all the  elastic Ips under an 
    public static List<Address> getElasticIps(List<Address> ips,String id)
    {
        List<Address> elasticIps=new ArrayList();
        for(Address t : ips)
        {
            if(t.getInstanceId().equals(id))
            {
                elasticIps.add(t);
                return elasticIps;
            }
        }
        return null;
    }
    
    
    //get all the customer gateways under the aws account
    public  List<CustomerGateway> getCustomerGateways()
    {
        return customerGateways;
    }
    
    //get a specific customer gateway based on its id 
    public static List<CustomerGateway> getCustomerGateways(List<CustomerGateway> gateways,String id)
    {
        List<CustomerGateway> customerGateways=new ArrayList();
        for(CustomerGateway t : gateways)
        {
            if(t.getCustomerGatewayId().equals(id))
            {
                customerGateways.add(t);
                return customerGateways;
            }
        }
        return null;
    }
    
    //get all the volumes under the aws account
    public List<Volume> getVolumes()
    {
        return this.volumes;
    }
    
    //get a  volume with a particular Id from a list of volumes
    public static List<Volume> getVolumes(List<Volume> vol, String id)
    {
        List<Volume> volumesList=new ArrayList();
        for(Volume v : vol )
        {
            if(v.getVolumeId().equals(id))
            {
               volumesList.add(v);
               return volumesList;
            }
        }
        return null;
    }
}
