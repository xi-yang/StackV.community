 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;


import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.*;
import net.maxgigapop.mrs.driver.openstack.OpenStackModelBuilder;

/*
 *
 * @author muzcategui
 */
public class AwsModelBuilder 
{
    public static OntModel createOntology(String access_key_id,String secret_access_key, Regions region, String topologyURI) throws IOException
    {
        Logger logger = Logger.getLogger(AwsModelBuilder.class.getName());
        
        //create model object
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF); 
        
        //set all the model prefixes
        model.setNsPrefix("rdfs",RdfOwl.getRdfsURI());
        model.setNsPrefix("rdf", RdfOwl.getRdfURI());
        model.setNsPrefix("xsd", RdfOwl.getXsdURI());
        model.setNsPrefix("owl", RdfOwl.getOwlURI());
        model.setNsPrefix("nml", Nml.getURI());
        model.setNsPrefix("mrs", Mrs.getURI());
        
        //set the global properties
        Property hasNode =Nml.hasNode;
        Property hasBidirectionalPort=Nml.hasBidirectionalPort;
        Property hasService =Nml.hasService;
        Property providesVM =Mrs.providesVM;
        Property type = RdfOwl.type;
        Property providedByService =Mrs.providedByService;
        Property providesBucket =Mrs.providesBucket;
        Property providesRoute =Mrs.providesRoute;
        Property providesSubnet =Mrs.providesSubnet;
        Property providesVPC = Mrs.providesVPC;
        Property providesVolume = Mrs.providesVolume;
        Property routeFrom = Mrs.routeFrom;
        Property routeTo =Mrs.routeTo;
        Property nextHop=Mrs.nextHop;
        Property value =Mrs.value;
        Property name=Nml.name;
        Property hasBucket=Mrs.hasBucket;
        Property hasVolume=Mrs.hasVolume;
        Property hasTopology=Nml.hasTopology;
        Property publicIpAddress=model.createProperty(model.getNsPrefixURI("mrs")+"publicIpAddress");
        Property privateIpAddress=model.createProperty(model.getNsPrefixURI("mrs")+"privateIpAddress");
        
        //set the global resources
        Resource route=Mrs.Route;
        Resource hypervisorService=Mrs.HypervisorService;
        Resource virtualCloudService =Mrs.VirtualCloudService;
        Resource routingService = Mrs.RoutingService;
        Resource blockStorageService = Mrs.BlockStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology =Nml.Topology;
        Resource networkAddress=Mrs.NetworkAddress;
        Resource switchingSubnet=Mrs.SwitchingSubnet;
        Resource switchingService=Mrs.SwitchingService;
        Resource node =Nml.Node;
        Resource port = Nml.BidirectionalPort;
        Resource namedIndividual = model.createResource(model.getNsPrefixURI("mrs")+"NamedIndividual");
        Resource awsTopology = RdfOwl.createResource(model,topologyURI,topology);
        Resource objectStorageService=Mrs.ObjectStorageService;
        
        //get the information from the AWS account
        AwsEC2Get ec2Client=new AwsEC2Get(access_key_id,secret_access_key,region);
        AwsS3Get s3Client=new AwsS3Get(access_key_id,secret_access_key,region);
        AwsDCGet dcClient= new AwsDCGet(access_key_id,secret_access_key,region);
        
        
        //create the outer layer of the aws model
        Resource ec2Service=RdfOwl.createResource(model,topologyURI+  ":ec2service-"+region.getName(),hypervisorService);
        Resource vpcService=RdfOwl.createResource(model,topologyURI + ":vpcservice-"+region.getName(),virtualCloudService);
        Resource s3Service= RdfOwl.createResource(model,topologyURI + ":s3service-"+region.getName(),objectStorageService);
        Resource ebsService= RdfOwl.createResource(model,topologyURI + ":ebsservice-"+region.getName(),blockStorageService);
        
        model.add(model.createStatement(awsTopology,hasService,ec2Service));
        model.add(model.createStatement(awsTopology, hasService, vpcService));
        model.add(model.createStatement(awsTopology,hasService,s3Service));
        model.add(model.createStatement(awsTopology,hasService,ebsService));
        
        
        //put all the vpcs and their information into the model
        for(Vpc v : ec2Client.getVpcs())
        {
            String vpcId= ec2Client.getIdTag(v.getVpcId());
            Resource VPC = RdfOwl.createResource(model,topologyURI +":" + vpcId,topology);
            model.add(model.createStatement(vpcService,providesVPC,VPC));
            model.add(model.createStatement(awsTopology,hasTopology,VPC));
            
            //put all the subnets within the vpc
            Resource SWITCHINGSERVICE= RdfOwl.createResource(model,topologyURI + ":" +vpcId+v.getCidrBlock(),switchingService);
            for(Subnet p: AwsEC2Get.getSubnets(ec2Client.getSubnets(), v.getVpcId()))
            {
                String subnetId= ec2Client.getIdTag(p.getSubnetId());
                Resource SUBNET= RdfOwl.createResource(model,topologyURI + ":" +subnetId,switchingSubnet);
                model.add(model.createStatement(VPC, hasService,SWITCHINGSERVICE));
                model.add(model.createStatement(SWITCHINGSERVICE, providesSubnet, SUBNET));
            
                //put all the intances inside this subnet into the model if there are any
                List<Instance> instances = ec2Client.getInstances(p.getSubnetId());
                if(!instances.isEmpty())
                {
                    for(Instance i : instances)
                    {
                        String instanceId= ec2Client.getIdTag(i.getInstanceId());
                        Resource INSTANCE= RdfOwl.createResource(model,topologyURI + ":" + instanceId ,node);
                        model.add(model.createStatement(VPC,hasNode, INSTANCE));
                        model.add(model.createStatement(ec2Service,providesVM,INSTANCE));
                        model.add(model.createStatement(INSTANCE, providedByService,ec2Service));

                        //put all the network interfaces of each instance into the model
                        for(InstanceNetworkInterface n : AwsEC2Get.getInstanceInterfaces(i))
                        {
                            String portId= ec2Client.getIdTag(n.getNetworkInterfaceId());
                            Resource PORT = RdfOwl.createResource(model,topologyURI + ":" +portId,port);
                            model.add(model.createStatement(INSTANCE,hasBidirectionalPort,PORT));
                            model.add(model.createStatement(SUBNET,hasBidirectionalPort,PORT));
                            
                            //put all the voumes attached to this instance into the modle
                            for(Volume vol : ec2Client.getVolumesWithAttachement(i))
                            {
                               String volumeId= ec2Client.getIdTag(vol.getVolumeId());
                               Resource VOLUME= RdfOwl.createResource(model,topologyURI + ":" +volumeId,volume);
                               model.add(model.createStatement(ebsService,providesVolume, VOLUME));
                               model.add(model.createStatement(INSTANCE,hasVolume, VOLUME));
                               model.add(model.createStatement(VOLUME, value,vol.getVolumeType()));
                               model.add(model.createStatement(VOLUME, Mrs.disk_gb,Integer.toString(vol.getSize())));
                               
                            }
                            
                            //put the private ip (if any) of the network interface in the model
                            for(InstancePrivateIpAddress q : n.getPrivateIpAddresses())
                            {
                                if(q.getPrivateIpAddress()!=null)
                                {
                                    Resource PRIVATE_ADDRESS= RdfOwl.createResource(model,topologyURI + ":" +q.getPrivateIpAddress(),networkAddress);
                                    model.add(model.createStatement(PORT, privateIpAddress, PRIVATE_ADDRESS));
                                    model.add(model.createStatement(PRIVATE_ADDRESS,value,q.getPrivateIpAddress()));
                                }
                            }
                            
                            //put the public Ip (if any) of the network interface into the model
                            if(n.getAssociation() !=null && n.getAssociation().getPublicIp()!=null)
                            {
                                Resource PUBLIC_ADDRESS= RdfOwl.createResource(model,topologyURI + ":" +n.getAssociation().getPublicIp(),networkAddress);
                                model.add(model.createStatement(PORT, publicIpAddress, PUBLIC_ADDRESS));
                                model.add(model.createStatement(PUBLIC_ADDRESS,value,n.getAssociation().getPublicIp()));
                            }
                        }
                    }
                }
            }  
            
            //get all the routes inside this VPC
            for(RouteTable t : AwsEC2Get.getRoutingTables(ec2Client.getRoutingTables(),v.getVpcId()))
            {
                String routeTableId= ec2Client.getIdTag(t.getRouteTableId());
                Resource ROUTINGSERVICE=RdfOwl.createResource(model,topologyURI + ":" +routeTableId,routingService);
                model.add(model.createStatement(VPC, hasService,ROUTINGSERVICE));
                List<Route> routes= t.getRoutes();
                for(Route r: routes)
                {
                    Resource ROUTE= RdfOwl.createResource(model,topologyURI + ":" +routeTableId+r.getDestinationCidrBlock()+r.getState(),route);
                    model.add(model.createStatement(ROUTINGSERVICE,providesRoute,ROUTE));
                    //model.add(model.createStatement(ROUTE, routeFrom,r.getOrigin()));
                    //model.add(model.createStatement(ROUTE,routeTo,r.getDestinationCidrBlock()));
                }
            }
         }
        
        //put the volumes of the ebsService into the model
        for(Volume v : ec2Client.getVolumesWithoutAttachment())
        {
            String volumeId= ec2Client.getIdTag(v.getVolumeId());
            Resource VOLUME= RdfOwl.createResource(model,topologyURI + ":" +volumeId,volume);
            model.add(model.createStatement(ebsService,providesVolume, VOLUME));
            model.add(model.createStatement(VOLUME, value,v.getVolumeType()));
            model.add(model.createStatement(VOLUME, Mrs.disk_gb,Integer.toString(v.getSize())));
        }
        
        //put all the buckets of the s3Service into the model
        for(Bucket b: s3Client.getBuckets())
        {
            Resource BUCKET = RdfOwl.createResource(model,topologyURI + ":" + b.getName(),bucket);
            model.add(model.createStatement(s3Service, providesBucket,BUCKET));
            model.add(model.createStatement(awsTopology,hasBucket,BUCKET));
        }
        logger.log(Level.INFO, "Ontology model for AWS driver rewritten");
        return model;
    }
}
