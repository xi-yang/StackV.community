/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2015-2016
 * Modified by: Adam smith 2017

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


import com.amazonaws.regions.Regions;
import com.amazonaws.services.directconnect.model.VirtualInterface;
import com.amazonaws.services.directconnect.model.VirtualInterfaceState;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.model.Bucket;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.maxgigapop.mrs.common.*;

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
public class AwsModelBuilder {

    public static OntModel createOntology(String access_key_id, String secret_access_key, Regions region, String topologyURI) throws IOException {
        //configure prefix util
        AwsPrefix awsPrefix = new AwsPrefix(topologyURI);
                
        //create model object
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        //set all the model prefixes
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
        Resource awsTopology = RdfOwl.createResource(model, topologyURI, topology);
        Resource objectStorageService = Mrs.ObjectStorageService;
        Resource routingTable = Mrs.RoutingTable;
        
        //add ":" to topologyUri for convenience 
        topologyURI = topologyURI.concat(":");

        //get the information from the AWS account
        AwsEC2Get ec2Client = new AwsEC2Get(access_key_id, secret_access_key, region);

        AwsS3Get s3Client = new AwsS3Get(access_key_id, secret_access_key, region);

        AwsDCGet dcClient = new AwsDCGet(access_key_id, secret_access_key, region);

        //create the outer layer of the aws model
        Resource ec2Service = RdfOwl.createResource(model, ResourceTool.getResourceUri("",awsPrefix.ec2Service(), region.getName()), hypervisorService);
  
        Resource vpcService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", awsPrefix.vpcService(), region.getName()), virtualCloudService);
        Resource s3Service = RdfOwl.createResource(model, ResourceTool.getResourceUri("", awsPrefix.s3Service(), region.getName()), objectStorageService);
        Resource ebsService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", awsPrefix.ebsService(), region.getName()), blockStorageService);
        Resource directConnect = RdfOwl.createResource(model, ResourceTool.getResourceUri("", awsPrefix.directConnectService(), region.getName()), biPort);
        
        model.add(model.createStatement(awsTopology, hasService, ec2Service));
        model.add(model.createStatement(awsTopology, hasService, vpcService));
        model.add(model.createStatement(awsTopology, hasService, s3Service));
        model.add(model.createStatement(awsTopology, hasService, ebsService));
        model.add(model.createStatement(awsTopology, hasBidirectionalPort, directConnect));
        
        //create resource for Vlan labels
        Resource vlan = model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan");

        //put all the Internet gateways into the model
        for (InternetGateway t : ec2Client.getInternetGateways()) {
            if (!t.getAttachments().isEmpty()) {
                String internetGatewayId = ec2Client.getIdTag(t.getInternetGatewayId());
                Resource INTERNETGATEWAY = RdfOwl.createResource(model, ResourceTool.getResourceUri(internetGatewayId, awsPrefix.gateway(),internetGatewayId), biPort);
                model.add(model.createStatement(INTERNETGATEWAY, Mrs.type, "internet-gateway"));
            }
        }

        //put all the Vpn gateways into the model
        for (VpnGateway g : ec2Client.getVirtualPrivateGateways()) {
            String vpnGatewayId = ec2Client.getIdTag(g.getVpnGatewayId());
            Resource VPNGATEWAY = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpnGatewayId,awsPrefix.gateway(),vpnGatewayId), biPort);
            model.add(model.createStatement(VPNGATEWAY, Mrs.type, "vpn-gateway"));
            model.add(model.createStatement(awsTopology, hasBidirectionalPort, VPNGATEWAY));
            
            /*
            String resourceName = ResourceTool.getResourceName(VPNGATEWAY.toString(), awsPrefix.instance());
            System.out.println("DEBUG: vgwID " + ec2Client.getVpnGatewayId(resourceName) + ", Resource name was "+VPNGATEWAY.toString());
            //*/

        }

        //get a list of all the virtual interfaces that do not belong to a VPN gateway
        //as this Virtual interfaces could be accepted or denied to be a part of a dc connection
        //in the push part
        for (VirtualInterface vi : dcClient.getVirtualInterfaces()) {
            String vlanNum = Integer.toString(vi.getVlan());
            String virtualInterfaceState =  vi.getVirtualInterfaceState();
            String[] invalidStates = {VirtualInterfaceState.Deleted.toString(), VirtualInterfaceState.Deleting.toString()};
            if ((Arrays.asList(invalidStates).contains(virtualInterfaceState))) {
                continue;
            }
            
            Resource VIRTUAL_INTERFACE = RdfOwl.createResource(model, ResourceTool.getResourceUri(vi.getVirtualInterfaceId(), awsPrefix.vif(), directConnect.getURI(), vi.getVlan().toString()), biPort);
            model.add(model.createStatement(VIRTUAL_INTERFACE, Nml.name, vi.getVirtualInterfaceId()));
            model.add(model.createStatement(VIRTUAL_INTERFACE, Mrs.type, "direct-connect-vif"));
            model.add(model.createStatement(VIRTUAL_INTERFACE, Mrs.value, "direct-connect-vif+"+vi.getVirtualInterfaceType()));
            Resource VLAN_LABEL_GROUP = RdfOwl.createResource(model, ResourceTool.getResourceUri(vlanNum, awsPrefix.labelGroup(), VIRTUAL_INTERFACE.getURI(),vlanNum), Nml.LabelGroup);
            model.add(model.createStatement(VLAN_LABEL_GROUP, Nml.values, vlanNum));
            model.add(model.createStatement(VIRTUAL_INTERFACE, Nml.hasLabelGroup, VLAN_LABEL_GROUP));
            model.add(model.createStatement(VLAN_LABEL_GROUP, Nml.labeltype, vlan));
            model.add(model.createStatement(directConnect, hasBidirectionalPort, VIRTUAL_INTERFACE));

            Resource vifAsn = RdfOwl.createResource(model, VIRTUAL_INTERFACE.getURI()+":asn", Mrs.NetworkAddress);
            model.add(model.createStatement(VIRTUAL_INTERFACE, Mrs.hasNetworkAddress, vifAsn));
            model.add(model.createStatement(vifAsn, Mrs.type, "bgp-asn"));
            model.add(model.createStatement(vifAsn, Mrs.value, vi.getAsn().toString()));
            if (vi.getAmazonAddress() != null && !vi.getAmazonAddress().isEmpty()) {
                Resource vifAmazonIp = RdfOwl.createResource(model, VIRTUAL_INTERFACE.getURI()+":amazon_ip", Mrs.NetworkAddress);
                model.add(model.createStatement(VIRTUAL_INTERFACE, Mrs.hasNetworkAddress, vifAmazonIp));
                model.add(model.createStatement(vifAmazonIp, Mrs.type, "ipv4-address:amazon"));
                model.add(model.createStatement(vifAmazonIp, Mrs.value, vi.getAmazonAddress()));
            }
            if (vi.getCustomerAddress() != null && !vi.getCustomerAddress().isEmpty()) {
                Resource vifCustomerIp = RdfOwl.createResource(model, VIRTUAL_INTERFACE.getURI()+":customer_ip", Mrs.NetworkAddress);
                model.add(model.createStatement(VIRTUAL_INTERFACE, Mrs.hasNetworkAddress, vifCustomerIp));
                model.add(model.createStatement(vifCustomerIp, Mrs.type, "ipv4-address:customer"));
                model.add(model.createStatement(vifCustomerIp, Mrs.value, vi.getCustomerAddress()));
            }
            if (vi.getAuthKey() != null && !vi.getAuthKey().isEmpty()) {
                Resource vifBgpAuthKey = RdfOwl.createResource(model, VIRTUAL_INTERFACE.getURI()+":bgp_authkey", Mrs.NetworkAddress);
                model.add(model.createStatement(VIRTUAL_INTERFACE, Mrs.hasNetworkAddress, vifBgpAuthKey));
                model.add(model.createStatement(vifBgpAuthKey, Mrs.type, "bgp-authkey"));
                model.add(model.createStatement(vifBgpAuthKey, Mrs.value, vi.getAuthKey()));
            }            
            //check if it has a gateway, meaning the virtual interface is being used
            String virtualGatewayId =  vi.getVirtualGatewayId();
            String[] acceptedStates = {VirtualInterfaceState.Available.toString(), VirtualInterfaceState.Deleting.toString(), 
                VirtualInterfaceState.Pending.toString(), VirtualInterfaceState.Verifying.toString(), "down"};
            if(virtualGatewayId != null && (Arrays.asList(acceptedStates).contains(virtualInterfaceState)))
            {
                Resource VLAN_LABEL = RdfOwl.createResource(model, ResourceTool.getResourceUri(vlanNum, awsPrefix.label(),VIRTUAL_INTERFACE.getURI(),vlanNum), Nml.Label);
                model.add(model.createStatement(VLAN_LABEL, Nml.labeltype, vlan));
                model.add(model.createStatement(VLAN_LABEL, Nml.value, vlanNum));
                model.add(model.createStatement(VIRTUAL_INTERFACE, Nml.hasLabel, VLAN_LABEL));
                if (!virtualGatewayId.isEmpty()) {
                    virtualGatewayId = ec2Client.getIdTag(virtualGatewayId);
                    Resource VPNGATEWAY = model.getResource(ResourceTool.getResourceUri(virtualGatewayId,awsPrefix.gateway(),virtualGatewayId));
                    model.add(model.createStatement(VPNGATEWAY, Nml.isAlias, VIRTUAL_INTERFACE));
                    model.add(model.createStatement(VIRTUAL_INTERFACE, Nml.isAlias, VPNGATEWAY));
                }
            }
        }

        /*
        Get a list of vpnConnections. A vpn essentially consists of a virtual
        gateway and a customer gateway connected by two ipsec tunnels.
        
        Here's a list of the vpn connection properties currently used in the model:
        
        vpn.getVpnGatewayId()
        vpn.getVpnConnectionId()
        vpn.getVgwTelemetry()
        vpn.getCustomerGatewayId()
        vpn.getRoutes()
        */
        for (VpnConnection vpn : ec2Client.getVpnConnections()) {
            String vpnState = vpn.getState();
            String vpnId = ec2Client.getIdTag(vpn.getVpnConnectionId());
            String vgwId = ec2Client.getIdTag(vpn.getVpnGatewayId());
            String cgwId = vpn.getCustomerGatewayId();
            
            String[] invalidStates = {VpnState.Deleted.toString(), VpnState.Deleting.toString()};
            if (Arrays.asList(invalidStates).contains(vpnState)) {
                continue;
            }
            
            //First, create the VPN Connection resource
            Resource VPNC = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpnId, awsPrefix.vpn(), vpnId), Nml.BidirectionalPort);
            //Resource VPNC = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpn.getVpnConnectionId(), awsPrefix.vpn(), vpn.getVpnConnectionId()), Nml.BidirectionalPort);
            //the vpn connection has type vpn-connection to prevent it from being mistaken for a createPortsRequest during modelAddition
            model.add(model.createStatement(VPNC, Mrs.type, "vpn-connection"));
            model.add(model.createStatement(awsTopology, Nml.hasBidirectionalPort, VPNC));
            //Next, find the vgw and add it as an alias
            Resource vgwResource = model.getResource(ResourceTool.getResourceUri(vgwId, awsPrefix.gateway(), vgwId));
            model.add(model.createStatement(VPNC, Nml.isAlias, vgwResource));
            model.add(model.createStatement(vgwResource, Nml.isAlias, VPNC));

            //Now, add the customer gateway to the model as a Mrs.NetworkAddress resource
            CustomerGateway cgw = ec2Client.getCustomerGateway(cgwId);
            Resource cgwIp = RdfOwl.createResource(model, ResourceTool.getResourceUri(cgwId, awsPrefix.cgw(), cgwId), Mrs.NetworkAddress);
            model.add(model.createStatement(cgwIp, Mrs.type, "ipv4-address:customer"));
            model.add(model.createStatement(cgwIp, Mrs.value, cgw.getIpAddress()));
            model.add(model.createStatement(VPNC, Mrs.hasNetworkAddress, cgwIp));
            //Add the static routes to the model
            List<VpnStaticRoute> routes = vpn.getRoutes();
            if (!routes.isEmpty()) {
                Resource customerCIDR = RdfOwl.createResource(model, VPNC.getURI()+":routes", Mrs.NetworkAddress);
                model.add(model.createStatement(customerCIDR, Mrs.type, "ipv4-prefix-list:customer"));
                model.add(model.createStatement(customerCIDR, Mrs.value, routes.get(0).getDestinationCidrBlock()));
                model.add(model.createStatement(VPNC, Mrs.hasNetworkAddress, customerCIDR));
            }
            /*
            Every vpnConnection has 2 tunnels.
            The vpn connection has a hasBidirectionalPort relation to each tunnel.
            Each tunnel then has a hasNetworkAddress relation to its ip address.
            technically each tunnel has 2 ip addresses, amazon and customer but we are 
            only interested in our side.
            
            Since the two tunnels are not used identically, one is referred to
            as tunnnel1 and the other as tunnel2. tunnel2 is a backup tunnel.
            */
            List <VgwTelemetry> telemetry = vpn.getVgwTelemetry();
            int i = 0;
            for (VgwTelemetry tunnel : telemetry) {
                i ++;
                Resource tunnelIpResource = RdfOwl.createResource(model, VPNC.getURI()+":tunnel"+i+"-ip", Mrs.NetworkAddress);
                model.add(model.createStatement(tunnelIpResource, Mrs.type, "ipv4-address:amazon"));
                model.add(model.createStatement(tunnelIpResource, Mrs.value, tunnel.getOutsideIpAddress()));
                
                Resource tunnelResource = RdfOwl.createResource(model, VPNC.getURI()+":tunnel"+i, Nml.BidirectionalPort);
                model.add(model.createStatement(VPNC, Nml.hasBidirectionalPort, tunnelResource));
                model.add(model.createStatement(tunnelResource, Mrs.hasNetworkAddress, tunnelIpResource));
                model.add(model.createStatement(tunnelResource, Mrs.type, "vpn-tunnel"));
            }
        }

        //to be used later, a list containing the elatic ips as strings
        List<String> elasticIps = new ArrayList();
        //put all the elastic ips under the account into the model
        /*for (Address ip : ec2Client.getElasticIps()) {
            Resource PUBLIC_ADDRESS = RdfOwl.createResource(model, ResourceTool.getResourceUri(topologyURI,ip.getPublicIp(), networkAddress);
            model.add(model.createStatement(PUBLIC_ADDRESS, type, "ipv4:public"));
            model.add(model.createStatement(PUBLIC_ADDRESS, value, ip.getPublicIp()));
            elasticIps.add(ip.getPublicIp());
        }*/

        //Put all the subnets into the model
        for (Subnet p : ec2Client.getSubnets()) {
            String subnetId = ec2Client.getIdTag(p.getSubnetId());
            String vpcId = ec2Client.getIdTag(p.getVpcId());
            Resource SUBNET = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId,awsPrefix.subnet(),vpcId,subnetId), switchingSubnet);
            String subnetNetAddress = ResourceTool.getResourceUri(subnetId, awsPrefix.subnetNetworkAddress(), vpcId, subnetId);
            if (subnetNetAddress.equals(subnetId)) {
                subnetNetAddress += ":networkaddress";
            }
            Resource SUBNET_NETWORK_ADDRESS = RdfOwl.createResource(model, subnetNetAddress, networkAddress);
            model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
            model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, p.getCidrBlock()));
            model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
        }

        //put all the network interfaces into the model
        for (NetworkInterface n : ec2Client.getNetworkInterfaces()) {
            String portId = ec2Client.getIdTag(n.getNetworkInterfaceId());
            String subnetId = ec2Client.getIdTag(n.getSubnetId());
            String vpcId = ec2Client.getIdTag(ec2Client.getSubnet(n.getSubnetId()).getVpcId());
            Resource PORT = RdfOwl.createResource(model, ResourceTool.getResourceUri(portId,awsPrefix.nic(),vpcId,subnetId,portId), biPort);

            //specify the addresses of the network interfaces 
            //put the private ip (if any) of the network interface in the model
            for (NetworkInterfacePrivateIpAddress q : n.getPrivateIpAddresses()) {
                if (q.getPrivateIpAddress() != null) {
                    Resource PRIVATE_ADDRESS = RdfOwl.createResource(model, ResourceTool.getResourceUri(PORT.toString()+":ip+"+q.getPrivateIpAddress(),awsPrefix.nicNetworkAddress(),vpcId,subnetId,portId,q.getPrivateIpAddress()), networkAddress);
                    model.add(model.createStatement(PORT, hasNetworkAddress, PRIVATE_ADDRESS));
                    model.add(model.createStatement(PRIVATE_ADDRESS, type, "ipv4:private"));
                    model.add(model.createStatement(PRIVATE_ADDRESS, value, q.getPrivateIpAddress()));  
                }
            }

            /*//put the public Ip (if any) of the network interface into the model3
            if (n.getAssociation() != null && n.getAssociation().getPublicIp() != null) {
                String publicIp = n.getAssociation().getPublicIp();
                Resource PUBLIC_ADDRESS;
                if (elasticIps.contains(publicIp)) {
                    PUBLIC_ADDRESS = model.getResource(ResourceTool.getResourceUri(topologyURI, n.getAssociation().getPublicIp());
                } else {
                    PUBLIC_ADDRESS = RdfOwl.createResource(model, ResourceTool.getResourceUri(topologyURI, publicIp, networkAddress);
                    model.add(model.createStatement(PUBLIC_ADDRESS, type, "ipv4:public"));
                    model.add(model.createStatement(PUBLIC_ADDRESS, value, publicIp));
                }
                model.add(model.createStatement(PORT, hasNetworkAddress, PUBLIC_ADDRESS));
            }*/

            //specify the subnet of the network interface 
            Resource SUBNET = model.getResource(ResourceTool.getResourceUri(subnetId,awsPrefix.subnet(),vpcId,subnetId));
            model.add(model.createStatement(SUBNET, hasBidirectionalPort, PORT));
        }

        //put all the vpcs and their information into the model
        for (Vpc v : ec2Client.getVpcs()) {
            String vpcId = ec2Client.getIdTag(v.getVpcId());
            Resource VPC = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpcId,awsPrefix.vpc(),vpcId), topology);
            String vpcNetAddress = ResourceTool.getResourceUri(vpcId,awsPrefix.vpcNetworkAddress(),vpcId);
            if (vpcNetAddress.equals(vpcId)) {
                vpcNetAddress += ":networkaddress";
            }
            Resource VPC_NETWORK_ADDRESS
                    = RdfOwl.createResource(model, vpcNetAddress, networkAddress);
            model.add(model.createStatement(vpcService, providesVPC, VPC));
            model.add(model.createStatement(awsTopology, hasTopology, VPC));
            model.add(model.createStatement(VPC_NETWORK_ADDRESS, type, "ipv4-prefix"));
            model.add(model.createStatement(VPC_NETWORK_ADDRESS, value, v.getCidrBlock()));
            model.add(model.createStatement(VPC, hasNetworkAddress, VPC_NETWORK_ADDRESS));

            //put the internet gateways and von gateways attache dto the vpc into the model
            InternetGateway igw = ec2Client.getInternetGateway(v);
            VpnGateway vpngw = ec2Client.getVirtualPrivateGateway(v);
            if (igw != null) {
                String gatewayId = ec2Client.getIdTag(igw.getInternetGatewayId());
                String resourceUri = ResourceTool.getResourceUri(gatewayId,awsPrefix.gateway(),gatewayId);
                Resource GATEWAY = model.getResource(resourceUri);
                model.add(model.createStatement(VPC, hasBidirectionalPort, GATEWAY));
            }
            if (vpngw != null) {
                String gatewayId =ec2Client.getIdTag(vpngw.getVpnGatewayId());
                String resourceUri = ResourceTool.getResourceUri(gatewayId,awsPrefix.gateway(),gatewayId);
                Resource GATEWAY = model.getResource(resourceUri);
                model.add(model.createStatement(VPC, hasBidirectionalPort, GATEWAY));
            }

            //Specify the subnets within the vpc
            String switchingServiceURI = vpcId + ":switchingservice";
            Resource SWITCHINGSERVICE = RdfOwl.createResource(model, ResourceTool.getResourceUri(switchingServiceURI,awsPrefix.switchingService(),vpcId), switchingService);
            model.add(model.createStatement(VPC, hasService, SWITCHINGSERVICE));
            for (Subnet p : ec2Client.getSubnets(v.getVpcId())) {
                String subnetId = ec2Client.getIdTag(p.getSubnetId());
                Resource SUBNET = model.getResource(ResourceTool.getResourceUri(subnetId,awsPrefix.subnet(),vpcId,subnetId));
                model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));

                //put all the intances inside this subnet into the model if there are any
                List<Instance> instances = ec2Client.getInstances(p.getSubnetId());
                if (!instances.isEmpty()) {
                    for (Instance i : instances) {
                        String instanceId = ec2Client.getIdTag(i.getInstanceId());
                        String instanceType = i.getInstanceType();
                        String imageName = i.getImageId();
                        String keyName = i.getKeyName();
                        Resource INSTANCE = RdfOwl.createResource(model, ResourceTool.getResourceUri(instanceId,awsPrefix.instance(),vpcId,subnetId,instanceId), node);
                        model.add(model.createStatement(VPC, hasNode, INSTANCE));
                        model.add(model.createStatement(ec2Service, providesVM, INSTANCE));
                        model.add(model.createStatement(INSTANCE, providedByService, ec2Service));
                        model.add(model.createStatement(INSTANCE, Mrs.type, "instance+"+instanceType));
                        model.add(model.createStatement(INSTANCE, Mrs.type, "image+"+imageName));
                        model.add(model.createStatement(INSTANCE, Mrs.type, "keypair+"+keyName));

                        //put all the voumes attached to this instance into the model
                        for (Volume vol : ec2Client.getVolumesWithAttachement(i)) {
                            String volumeId = ec2Client.getIdTag(vol.getVolumeId());
                            Resource VOLUME = RdfOwl.createResource(model, ResourceTool.getResourceUri(volumeId,awsPrefix.volume(),volumeId), volume);
                            model.add(model.createStatement(ebsService, providesVolume, VOLUME));
                            model.add(model.createStatement(INSTANCE, hasVolume, VOLUME));
                            model.add(model.createStatement(VOLUME, value, vol.getVolumeType()));
                            model.add(model.createStatement(VOLUME, Mrs.disk_gb, Integer.toString(vol.getSize())));
                            List<VolumeAttachment> volAttach = vol.getAttachments();
                            for (VolumeAttachment va : volAttach) {
                                if (va.getInstanceId().equals(i.getInstanceId())) {
                                    model.add(model.createStatement(VOLUME, targetDevice, va.getDevice()));
                                }
                            }
                        }

                        //put all the network interfaces of each instance into the model
                        for (InstanceNetworkInterface n : AwsEC2Get.getInstanceInterfaces(i)) {
                            String portId = ec2Client.getIdTag(n.getNetworkInterfaceId());
                            Resource PORT = model.getResource(ResourceTool.getResourceUri(portId,awsPrefix.nic(),vpcId,subnetId,portId));
                            model.add(model.createStatement(INSTANCE, hasBidirectionalPort, PORT));
                        }
                        
                        //put public ip as NetworkAddress of this instance into the model
                        String publicIp = i.getPublicIpAddress();
                        if (publicIp != null && !publicIp.isEmpty()) {
                            Resource PUBLIC_ADDRESS = RdfOwl.createResource(model, ResourceTool.getResourceUri(INSTANCE.toString()+":public-ip+"+publicIp,awsPrefix.publicAddress(),publicIp), Mrs.NetworkAddress);
                            model.add(model.createStatement(INSTANCE, hasNetworkAddress, PUBLIC_ADDRESS));
                            model.add(model.createStatement(PUBLIC_ADDRESS, type, "ipv4:public"));
                            model.add(model.createStatement(PUBLIC_ADDRESS, value, publicIp));  
                        }
                    }
                }
            }

            //Make the L3 routing model for this VPC
            String routingServiceUri = vpcId + ":routingservice";
            Resource ROUTINGSERVICE = RdfOwl.createResource(model, ResourceTool.getResourceUri(routingServiceUri , awsPrefix.routingService(),vpcId), routingService);
            model.add(model.createStatement(VPC, hasService, ROUTINGSERVICE));

            //add the internet and vpn gateway off this vpc
            for (RouteTable t : ec2Client.getRoutingTables(v.getVpcId())) {
                List<RouteTableAssociation> associations = t.getAssociations();
                String routeTableId = ec2Client.getIdTag(t.getRouteTableId());
                Resource ROUTINGTABLE = RdfOwl.createResource(model, ResourceTool.getResourceUri(routeTableId,awsPrefix.routingTable(),vpcId,routeTableId), routingTable);
                model.add(model.createStatement(ROUTINGSERVICE, providesRoutingTable, ROUTINGTABLE));
                boolean main = false;
                if (!associations.isEmpty()) {
                    main = t.getAssociations().get(0).getMain();
                }
                if (main == true) {
                    model.add(model.createStatement(ROUTINGTABLE, type, "main"));
                } else {
                    model.add(model.createStatement(ROUTINGTABLE, type, "local"));
                }
                List<Route> routes = t.getRoutes();
                for (Route r : routes) {
                    Resource ROUTE_TO = null;
                    Resource ROUTE_FROM = null;
                    int i = 0;
                    //@TODO: A workaround for unconventional routes such as ones for VPC EndPoints
                    if (r.getDestinationCidrBlock() == null) {
                        continue;
                    }
                    String routeId = r.getDestinationCidrBlock().replace("/", "");
                    Resource ROUTE = RdfOwl.createResource(model, ROUTINGTABLE.toString()+":route-"+routeId, route);
                    model.add(model.createStatement(ROUTINGSERVICE, providesRoute, ROUTE));
                    String target = r.getGatewayId();

                    if (target != null) //in case is not a vpc peering connection
                    {
                        InternetGateway internetGateway = ec2Client.getInternetGateway(target);
                        VpnGateway vpnGateway = ec2Client.getVirtualPrivateGateway(target);
                        model.add(model.createStatement(ROUTINGTABLE, hasRoute, ROUTE));

                        if (internetGateway != null) {
                            String gatewayId = ec2Client.getIdTag(internetGateway.getInternetGatewayId());
                            Resource resource = model.getResource(ResourceTool.getResourceUri(gatewayId,awsPrefix.gateway(),gatewayId));
                            model.add(model.createStatement(ROUTE, nextHop, resource));
                        } else if (vpnGateway != null) {
                            String gatewayId = ec2Client.getIdTag(vpnGateway.getVpnGatewayId());
                            Resource resource = model.getResource(ResourceTool.getResourceUri(gatewayId,awsPrefix.gateway(),gatewayId));
                            model.add(model.createStatement(ROUTE, nextHop, resource));
                        } else {
                            model.add(model.createStatement(ROUTE, nextHop, "local"));
                        }
                    } else //in case is a vpc peering connection
                    {
                        target = r.getVpcPeeringConnectionId();
                        target = ec2Client.getPeerVpc(target);
                        target = ec2Client.getIdTag(target);
                        model.add(model.createStatement(ROUTINGTABLE, hasRoute, ROUTE));
                        Resource resource = model.getResource(ResourceTool.getResourceUri(target,awsPrefix.vpc(),target));
                        model.add(model.createStatement(ROUTE, nextHop, resource));
                    }

                    ROUTE_TO = RdfOwl.createResource(model, ROUTE.toString()+":routeto", networkAddress);
                    if (target.equals("local")) {
                        model.add(model.createStatement(ROUTE_TO, type, "ipv4-prefix"));
                    } else {
                        model.add(model.createStatement(ROUTE_TO, type, "ipv4-prefix-list"));
                    }

                    model.add(model.createStatement(ROUTE_TO, value, r.getDestinationCidrBlock()));
                    model.add(model.createStatement(ROUTE, routeTo, ROUTE_TO));
                    while (i < associations.size() && !associations.isEmpty()) //get the routes from the amazon cloud to any destination
                    {
                        String complementId = ec2Client.getIdTag(associations.get(i).getSubnetId());

                        //if the association subnet is null just skip to the next one
                        if (complementId == null) {
                            i++;
                            continue;
                        }
                        ROUTE_FROM = model.getResource(ResourceTool.getResourceUri(complementId,awsPrefix.subnet(),vpcId,complementId));
                        model.add(model.createStatement(ROUTE, routeFrom, ROUTE_FROM));
                        // if this ROUTINGTABLE has VPN propagation=yes add 0.0.0.0/0 routeto with nexthop=routefrom=propagatingVgw
                        if (t.getPropagatingVgws() != null && !t.getPropagatingVgws().isEmpty()) {
                            PropagatingVgw vgw = t.getPropagatingVgws().get(0);
                            String vpnGatewayId =ec2Client.getIdTag(vgw.getGatewayId());
                            String resourceUri = ResourceTool.getResourceUri(vpnGatewayId,awsPrefix.gateway(),vpnGatewayId);
                            Resource propagatingVGW = model.getResource(resourceUri);
                            Resource propagatingRoute = RdfOwl.createResource(model, ROUTINGTABLE.getURI()+":route-0.0.0.00", Mrs.Route);
                            Resource propagatingRouteTo = RdfOwl.createResource(model, ROUTINGTABLE.getURI()+":route-0.0.0.00:routeto", Mrs.NetworkAddress);
                            model.add(model.createStatement(propagatingRouteTo, Mrs.type, "ipv4-prefix"));
                            model.add(model.createStatement(propagatingRouteTo, Mrs.value, "0.0.0.0/0"));
                            model.add(model.createStatement(propagatingRoute, Mrs.routeTo, propagatingRouteTo));
                            model.add(model.createStatement(propagatingRoute, Mrs.routeFrom, propagatingVGW));
                            model.add(model.createStatement(propagatingRoute, Mrs.nextHop, propagatingVGW));
                            model.add(model.createStatement(ROUTINGTABLE, Mrs.hasRoute, propagatingRoute));
                            model.add(model.createStatement(ROUTINGSERVICE, Mrs.providesRoute, propagatingRoute));
                        }
                        i++; //increment the association index
                    }
                }
            }
        }

        //put the volumes of the ebsService into the model
        for (Volume v : ec2Client.getVolumesWithoutAttachment()) {
            String volumeId = ec2Client.getIdTag(v.getVolumeId());
            Resource VOLUME = RdfOwl.createResource(model, ResourceTool.getResourceUri(volumeId,awsPrefix.volume(),volumeId), volume);
            model.add(model.createStatement(ebsService, providesVolume, VOLUME));
            model.add(model.createStatement(VOLUME, value, v.getVolumeType()));
            model.add(model.createStatement(VOLUME, Mrs.disk_gb, Integer.toString(v.getSize())));
        }

        //put all the buckets of the s3Service into the model
        for (Bucket b : s3Client.getBuckets()) {
            Resource BUCKET = RdfOwl.createResource(model, ResourceTool.getResourceUri(b.getName(),awsPrefix.bucket(),b.getName()), bucket);
            model.add(model.createStatement(s3Service, providesBucket, BUCKET));
            model.add(model.createStatement(awsTopology, hasBucket, BUCKET));
            model.add(model.createStatement(BUCKET, Nml.name, b.getName()));
        }

        //create abstraction for batch resources
        AwsBatchResourcesTool batchTool = new AwsBatchResourcesTool(access_key_id,secret_access_key, region, topologyURI);
        model = batchTool.contractVMbatch(model);
        return model;
    }
}
