 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        Resource ec2Service = RdfOwl.createResource(model, ResourceTool.getResourceUri("",AwsPrefix.ec2Service , region.getName()), hypervisorService);
  
        Resource vpcService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", AwsPrefix.vpcService , region.getName()), virtualCloudService);
        Resource s3Service = RdfOwl.createResource(model, ResourceTool.getResourceUri("", AwsPrefix.s3Service , region.getName()), objectStorageService);
        Resource ebsService = RdfOwl.createResource(model, ResourceTool.getResourceUri("", AwsPrefix.ebsService , region.getName()), blockStorageService);
        Resource directConnect = RdfOwl.createResource(model, ResourceTool.getResourceUri("", AwsPrefix.directConnectService,region.getName()), biPort);

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
                Resource INTERNETGATEWAY = RdfOwl.createResource(model, ResourceTool.getResourceUri(internetGatewayId,AwsPrefix.gateway,internetGatewayId), biPort);
                model.add(model.createStatement(INTERNETGATEWAY, Mrs.type, "internet-gateway"));
            }
        }

        //put all the Vpn gateways into the model
        for (VpnGateway g : ec2Client.getVirtualPrivateGateways()) {
            String vpnGatewayId = ec2Client.getIdTag(g.getVpnGatewayId());
            Resource VPNGATEWAY = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpnGatewayId,AwsPrefix.gateway,vpnGatewayId), biPort);
            model.add(model.createStatement(VPNGATEWAY, Mrs.type, "vpn-gateway"));
            model.add(model.createStatement(awsTopology, hasBidirectionalPort, VPNGATEWAY));
        }

        //get a list of all the virtual interfaces that do not bellong to a VPN gateway
        //as this Virtual interfaces could be accepted or denied to be a part of a dc connection
        //in the push part
        for (VirtualInterface vi : dcClient.getVirtualInterfaces()) {
            String vlanNum = Integer.toString(vi.getVlan());

            String virtualInterfaceState =  vi.getVirtualInterfaceState();
            String[] invalidStates = {VirtualInterfaceState.Deleted.toString(), VirtualInterfaceState.Deleting.toString()};
            if ((Arrays.asList(invalidStates).contains(virtualInterfaceState))) {
                continue;
            }
            Resource VLAN_LABEL_GROUP = RdfOwl.createResource(model, ResourceTool.getResourceUri(vlanNum, AwsPrefix.labelGroup,vi.getVirtualInterfaceId(),vlanNum ), Nml.LabelGroup);
            model.add(model.createStatement(VLAN_LABEL_GROUP, Nml.values, vlanNum));
            
            Resource VIRTUAL_INTERFACE = RdfOwl.createResource(model, ResourceTool.getResourceUri(vi.getVirtualInterfaceId(),AwsPrefix.vif,vi.getVirtualInterfaceId()), biPort);
            model.add(model.createStatement(VIRTUAL_INTERFACE, Nml.name, vi.getVirtualInterfaceId()));
            model.add(model.createStatement(VIRTUAL_INTERFACE, Mrs.type, "direct-connect-vif"));
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
            String[] acceptedStates = {VirtualInterfaceState.Available.toString(), 
                VirtualInterfaceState.Confirming.toString(), VirtualInterfaceState.Deleting.toString(), 
                VirtualInterfaceState.Pending.toString(), VirtualInterfaceState.Verifying.toString(), "down"};
            if(virtualGatewayId != null && (Arrays.asList(acceptedStates).contains(virtualInterfaceState)))
            {
                virtualGatewayId = ec2Client.getIdTag(virtualGatewayId);
                Resource VLAN_LABEL = RdfOwl.createResource(model, ResourceTool.getResourceUri(vlanNum, AwsPrefix.vlan,vi.getVirtualInterfaceId(),vlanNum), Nml.Label);
                Resource VPNGATEWAY = model.getResource(ResourceTool.getResourceUri(virtualGatewayId,AwsPrefix.gateway,virtualGatewayId));
                model.add(model.createStatement(VLAN_LABEL, Nml.labeltype, vlan));
                model.add(model.createStatement(VLAN_LABEL, Nml.value, vlanNum));
                model.add(model.createStatement(VIRTUAL_INTERFACE, Nml.hasLabel, VLAN_LABEL));
                model.add(model.createStatement(VPNGATEWAY, Nml.isAlias, VIRTUAL_INTERFACE));
                model.add(model.createStatement(VIRTUAL_INTERFACE, Nml.isAlias, VPNGATEWAY));
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
            Resource SUBNET = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId,AwsPrefix.subnet,vpcId,subnetId), switchingSubnet);
            Resource SUBNET_NETWORK_ADDRESS
                    = RdfOwl.createResource(model, ResourceTool.getResourceUri(subnetId,AwsPrefix.subnetNetworkAddress,vpcId,subnetId), networkAddress);
            model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, type, "ipv4-prefix"));
            model.add(model.createStatement(SUBNET_NETWORK_ADDRESS, value, p.getCidrBlock()));
            model.add(model.createStatement(SUBNET, hasNetworkAddress, SUBNET_NETWORK_ADDRESS));
        }

        //put all the network interfaces into the model
        for (NetworkInterface n : ec2Client.getNetworkInterfaces()) {
            String portId = ec2Client.getIdTag(n.getNetworkInterfaceId());
            String subnetId = ec2Client.getIdTag(n.getSubnetId());
            String vpcId = ec2Client.getIdTag(ec2Client.getSubnet(n.getSubnetId()).getVpcId());
            Resource PORT = RdfOwl.createResource(model, ResourceTool.getResourceUri(portId,AwsPrefix.nic,vpcId,subnetId,portId), biPort);

            //specify the addresses of the network interfaces 
            //put the private ip (if any) of the network interface in the model
            for (NetworkInterfacePrivateIpAddress q : n.getPrivateIpAddresses()) {
                if (q.getPrivateIpAddress() != null) {
                    Resource PRIVATE_ADDRESS = RdfOwl.createResource(model, ResourceTool.getResourceUri(PORT.toString()+":ip+"+q.getPrivateIpAddress(),AwsPrefix.nicNetworkAddress,vpcId,subnetId,portId,q.getPrivateIpAddress()), networkAddress);
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
            Resource SUBNET = model.getResource(ResourceTool.getResourceUri(subnetId,AwsPrefix.subnet,vpcId,subnetId));
            model.add(model.createStatement(SUBNET, hasBidirectionalPort, PORT));
        }

        //put all the vpcs and their information into the model
        for (Vpc v : ec2Client.getVpcs()) {
            String vpcId = ec2Client.getIdTag(v.getVpcId());
            Resource VPC = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpcId,AwsPrefix.vpc,vpcId), topology);
            Resource VPC_NETWORK_ADDRESS
                    = RdfOwl.createResource(model, ResourceTool.getResourceUri(vpcId,AwsPrefix.vpcNetworkAddress,vpcId ), networkAddress);
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
                String resourceUri = ResourceTool.getResourceUri(gatewayId,AwsPrefix.gateway,gatewayId);
                Resource GATEWAY = model.getResource(resourceUri);
                model.add(model.createStatement(VPC, hasBidirectionalPort, GATEWAY));
            }
            if (vpngw != null) {
                String gatewayId =ec2Client.getIdTag(vpngw.getVpnGatewayId());
                String resourceUri = ResourceTool.getResourceUri(gatewayId,AwsPrefix.gateway,gatewayId);
                Resource GATEWAY = model.getResource(resourceUri);
                model.add(model.createStatement(VPC, hasBidirectionalPort, GATEWAY));
            }

            //Specify the subnets within the vpc
            String switchingServiceURI = vpcId + ":switchingservice";
            Resource SWITCHINGSERVICE = RdfOwl.createResource(model, ResourceTool.getResourceUri(switchingServiceURI,AwsPrefix.switchingService,vpcId), switchingService);
            model.add(model.createStatement(VPC, hasService, SWITCHINGSERVICE));
            for (Subnet p : ec2Client.getSubnets(v.getVpcId())) {
                String subnetId = ec2Client.getIdTag(p.getSubnetId());
                Resource SUBNET = model.getResource(ResourceTool.getResourceUri(subnetId,AwsPrefix.subnet,vpcId,subnetId));
                model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));

                //put all the intances inside this subnet into the model if there are any
                List<Instance> instances = ec2Client.getInstances(p.getSubnetId());
                if (!instances.isEmpty()) {
                    for (Instance i : instances) {
                        String instanceId = ec2Client.getIdTag(i.getInstanceId());
                        Resource INSTANCE = RdfOwl.createResource(model, ResourceTool.getResourceUri(instanceId,AwsPrefix.instance,vpcId,subnetId,instanceId), node);
                        model.add(model.createStatement(VPC, hasNode, INSTANCE));
                        model.add(model.createStatement(ec2Service, providesVM, INSTANCE));
                        model.add(model.createStatement(INSTANCE, providedByService, ec2Service));

                        //put all the voumes attached to this instance into the modle
                        for (Volume vol : ec2Client.getVolumesWithAttachement(i)) {
                            String volumeId = ec2Client.getIdTag(vol.getVolumeId());
                            Resource VOLUME = RdfOwl.createResource(model, ResourceTool.getResourceUri(volumeId,AwsPrefix.volume,volumeId), volume);
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
                            Resource PORT = model.getResource(ResourceTool.getResourceUri(portId,AwsPrefix.nic,vpcId,subnetId,portId));
                            model.add(model.createStatement(INSTANCE, hasBidirectionalPort, PORT));
                        }
                    }
                }
            }

            //Make the L3 routing model for this VPC
            String routingServiceUri = vpcId + ":routingservice";
            Resource ROUTINGSERVICE = RdfOwl.createResource(model, ResourceTool.getResourceUri(routingServiceUri , AwsPrefix.routingService,vpcId), routingService);
            model.add(model.createStatement(VPC, hasService, ROUTINGSERVICE));

            //add the internet and vpn gateway off this vpc
            for (RouteTable t : ec2Client.getRoutingTables(v.getVpcId())) {
                List<RouteTableAssociation> associations = t.getAssociations();
                String routeTableId = ec2Client.getIdTag(t.getRouteTableId());
                Resource ROUTINGTABLE = RdfOwl.createResource(model, ResourceTool.getResourceUri(routeTableId,AwsPrefix.routingTable,vpcId,routeTableId), routingTable);
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
                    String routeId = r.getDestinationCidrBlock().replace("/", "");
                    Resource ROUTE = RdfOwl.createResource(model, ResourceTool.getResourceUri(routeId,AwsPrefix.route,vpcId,routeTableId,routeId), route);
                    model.add(model.createStatement(ROUTINGSERVICE, providesRoute, ROUTE));
                    String target = r.getGatewayId();

                    if (target != null) //in case is not a vpc peering connection
                    {
                        InternetGateway internetGateway = ec2Client.getInternetGateway(target);
                        VpnGateway vpnGateway = ec2Client.getVirtualPrivateGateway(target);
                        model.add(model.createStatement(ROUTINGTABLE, hasRoute, ROUTE));

                        if (internetGateway != null) {
                            String gatewayId = ec2Client.getIdTag(internetGateway.getInternetGatewayId());
                            Resource resource = model.getResource(ResourceTool.getResourceUri(gatewayId,AwsPrefix.gateway,gatewayId));
                            model.add(model.createStatement(ROUTE, nextHop, resource));
                        } else if (vpnGateway != null) {
                            String gatewayId = ec2Client.getIdTag(vpnGateway.getVpnGatewayId());
                            Resource resource = model.getResource(ResourceTool.getResourceUri(gatewayId,AwsPrefix.gateway,gatewayId));
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
                        Resource resource = model.getResource(ResourceTool.getResourceUri(target,AwsPrefix.vpc,target));
                        model.add(model.createStatement(ROUTE, nextHop, resource));
                    }

                    ROUTE_TO = RdfOwl.createResource(model, ResourceTool.getResourceUri(routeId,AwsPrefix.routeTo,vpcId,routeTableId,routeId), networkAddress);
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
                        ROUTE_FROM = model.getResource(ResourceTool.getResourceUri(complementId,AwsPrefix.subnet,vpcId,complementId));
                        model.add(model.createStatement(ROUTE, routeFrom, ROUTE_FROM));

                        i++; //increment the association index
                    }

                }
            }
        }

        //put the volumes of the ebsService into the model
        for (Volume v : ec2Client.getVolumesWithoutAttachment()) {
            String volumeId = ec2Client.getIdTag(v.getVolumeId());
            Resource VOLUME = RdfOwl.createResource(model, ResourceTool.getResourceUri(volumeId,AwsPrefix.volume,volumeId), volume);
            model.add(model.createStatement(ebsService, providesVolume, VOLUME));
            model.add(model.createStatement(VOLUME, value, v.getVolumeType()));
            model.add(model.createStatement(VOLUME, Mrs.disk_gb, Integer.toString(v.getSize())));
        }

        //put all the buckets of the s3Service into the model
        for (Bucket b : s3Client.getBuckets()) {
            Resource BUCKET = RdfOwl.createResource(model, ResourceTool.getResourceUri(b.getName(),AwsPrefix.bucket,b.getName()), bucket);
            model.add(model.createStatement(s3Service, providesBucket, BUCKET));
            model.add(model.createStatement(awsTopology, hasBucket, BUCKET));
        }

        //create abstraction for batch resources
        AwsBatchResourcesTool batchTool = new AwsBatchResourcesTool(access_key_id,secret_access_key, region);
        model = batchTool.contractVMbatch(model);
        return model;
    }
}
