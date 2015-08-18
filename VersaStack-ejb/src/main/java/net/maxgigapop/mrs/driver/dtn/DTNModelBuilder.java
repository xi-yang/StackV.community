 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
public class DTNModelBuilder {

    @SuppressWarnings("empty-statement")
    public static OntModel createOntology(String user_account, String access_key, String addresses, String topologyURI, 
            String endpoint) throws IOException {

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
        Property isAlias = Nml.isAlias;
        Property hasBidirectionalPort = Nml.hasBidirectionalPort;
        Property hasService = Nml.hasService;
        Property providesVolume = Mrs.providesVolume;
        Property providesBucket = Mrs.providesBucket;
        Property hasBucket = Mrs.hasBucket;
        Property hasVolume = Mrs.hasVolume;
        Property hasFileSystem = Mrs.hasFileSystem;
        Property hasTransfer = Mrs.hasTransfer;
        
        //set the global data properties
        Property cpu = Mrs.num_core;
        Property memory_mb = Mrs.memory_mb;
        Property link_type = model.createProperty(model.getNsPrefixURI("mrs") + "link_type");
        Property link_capacity_Mbps = model.createProperty(model.getNsPrefixURI("mrs") + "link_capacity_Mbps");
        Property fs_type = model.createProperty(model.getNsPrefixURI("mrs") + "file_system_type");
        Property disk_cap = Mrs.disk_gb;
        Property disk_avail = model.createProperty(model.getNsPrefixURI("mrs") + "disk_free_gb");
        Property mount = model.createProperty(model.getNsPrefixURI("mrs") + "mount_point");
        Property active_transfers = model.createProperty(model.getNsPrefixURI("mrs") + "active_transfers");
        Property cpu_usage  = model.createProperty(model.getNsPrefixURI("mrs") + "cpu_usage");

        //set the global resources
        Resource storageService = Mrs.StorageService;
        Resource blockStorageService = Mrs.BlockStorageService;
        Resource objectStorageService = Mrs.ObjectStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology = Nml.Topology;
        Resource clusterService = Mrs.DataTransferClusterService;
        Resource dataTransferService = Mrs.DataTransferService;
        Resource dataTransfer = Mrs.DataTransfer;
        Resource fileSystem = Mrs.FileSystem;
        Resource node = Nml.Node;
        Resource biPort = Nml.BidirectionalPort;

        Resource dtnTopology = RdfOwl.createResource(model, topologyURI, topology);
        
        Resource CLUSTERSERVICE = null;
        if (endpoint.length() > 0){
            CLUSTERSERVICE = RdfOwl.createResource(model, topologyURI+":clusterservice-"+endpoint, clusterService);
        }
        Resource INTERCONNECTION = RdfOwl.createResource(model,topologyURI+":interconnection", topology);

        String[] ips = addresses.split("[\\(\\)]");
        List<FileSystem> pfslist = new ArrayList<>();
        for (String ip : ips) {
            if(ip != null && ip.length() > 0) {
                DTNGet conf = new DTNGet(user_account, access_key, ip);               
                if (conf.getDTNNode()!=null){
                    //create the outer layer of the DTN model
                    Resource DTN_NODE = RdfOwl.createResource(model, topologyURI+":"+conf.getDTNNode().getHostName(), node);
                    model.add(model.createStatement(dtnTopology, hasNode, DTN_NODE));
                    //create CPU and memory information
                    model.addLiteral(DTN_NODE, cpu, conf.getDTNNode().getNumCPU());
                    model.addLiteral(DTN_NODE, cpu_usage, conf.getCPUload());
                    model.addLiteral(DTN_NODE, memory_mb, conf.getDTNNode().getMemorySize());
                    
                    //create NIC information
                    if (!conf.getDTNNode().getNICs().isEmpty()) {
                        for (NIC i : conf.getDTNNode().getNICs()) {
                            String nic_id = i.getNICid();
                            Resource NIC = RdfOwl.createResource(model, DTN_NODE.getURI()+":nic-"+nic_id, biPort);
                            model.add(model.createStatement(DTN_NODE, hasBidirectionalPort, NIC));
                            model.addLiteral(NIC, link_type, ResourceFactory.createTypedLiteral(i.getLinkType(),XSDDatatype.XSDstring));
                            if (i.getLinkCapacity()!= 0L){
                                model.addLiteral(NIC, link_capacity_Mbps, i.getLinkCapacity());
                            }
                            //create connections
                            if (!i.getIPAddress().equals(conf.getDTNNode().getIP())){
                                Resource counterNIC = RdfOwl.createResource(model, INTERCONNECTION.getURI()+":"+conf.getDTNNode().getHostName()+"-"+nic_id, biPort);
                                model.add(model.createStatement(INTERCONNECTION, hasBidirectionalPort, counterNIC));
                                model.add(model.createStatement(NIC,isAlias,counterNIC));
                                model.add(model.createStatement(counterNIC,isAlias,NIC));
                            }
                        }
                    }
                    
                    //create storage informatioin
                    if (conf.getFileSystems()!=null){
                        if (!conf.getFileSystems().isEmpty()) {
                            for (FileSystem f : conf.getFileSystems()){
                                if(!f.isParallel()){
                                    String fs_id = f.getDeviceName();
                                    Resource LOCALFS = RdfOwl.createResource(model, DTN_NODE.getURI()+":localfilesystem-"+fs_id, fileSystem);
                                    model.add(model.createStatement(DTN_NODE, hasFileSystem, LOCALFS));
                                    model.addLiteral(LOCALFS, fs_type, ResourceFactory.createTypedLiteral(f.getType(),XSDDatatype.XSDstring));
                                    model.addLiteral(LOCALFS, mount, ResourceFactory.createTypedLiteral(f.getMountPoint(),XSDDatatype.XSDstring));
                                    if (f.isBlockStorage()){
                                        Resource BLOCKSERVICE = RdfOwl.createResource(model, LOCALFS.getURI()+":blockstorageservice", blockStorageService);
                                        model.add(model.createStatement(LOCALFS, hasService, BLOCKSERVICE));
                                        model.add(model.createStatement(DTN_NODE, hasService, BLOCKSERVICE));
                                        Resource LOCALVOLUME = RdfOwl.createResource(model, LOCALFS.getURI()+":localvolume", volume);
                                        model.add(model.createStatement(BLOCKSERVICE, providesVolume, LOCALVOLUME));
                                        model.add(model.createStatement(DTN_NODE, hasVolume, LOCALVOLUME));
                                        model.addLiteral(LOCALVOLUME, disk_cap, f.getSize());
                                        model.addLiteral(LOCALVOLUME, disk_avail, f.getAvailableSize());
                                    }else{
                                        Resource OBJECTSERVICE = RdfOwl.createResource(model, LOCALFS.getURI()+":objectstorageservice", objectStorageService);
                                        model.add(model.createStatement(LOCALFS, hasService, OBJECTSERVICE));
                                        model.add(model.createStatement(DTN_NODE, hasService, OBJECTSERVICE));
                                        Resource LOCALBUCKET = RdfOwl.createResource(model, LOCALFS.getURI()+":localbucket", bucket);
                                        model.add(model.createStatement(OBJECTSERVICE, providesBucket, LOCALBUCKET));
                                        model.add(model.createStatement(DTN_NODE, hasBucket, LOCALBUCKET));
                                        model.addLiteral(LOCALBUCKET, disk_cap, f.getSize());
                                        model.addLiteral(LOCALBUCKET, disk_avail, f.getAvailableSize());
                                    }
                                } else{
                                    if (!pfslist.contains(f)){
                                        pfslist.add(f);
                                    }  
                                }
                            }
                        }  
                    }
                    
                    //create data transfer service informatioin
                    if (conf.getTransferServiceType()!=null){
                        Resource TRANSFERSERVICE = RdfOwl.createResource(model, DTN_NODE.getURI()+":datatransferservice-"+conf.getTransferServiceType(), dataTransferService);
                        model.add(model.createStatement(DTN_NODE, hasService, TRANSFERSERVICE));
                        for(Map.Entry<String, String> entry : conf.getTransferConf().entrySet()){
                            Property key = model.createProperty(model.getNsPrefixURI("mrs") + entry.getKey());
                            String value = entry.getValue();
                            model.addLiteral(TRANSFERSERVICE, key, ResourceFactory.createTypedLiteral(value,XSDDatatype.XSDstring));
                        }
                        //add dynamic information
                        model.addLiteral(DTN_NODE, active_transfers, conf.getActiveTransfers());

                        if (endpoint.length() > 0){
                            model.add(model.createStatement(TRANSFERSERVICE, hasService, CLUSTERSERVICE));
                        }
                    }
                    
                    
                    
                }
            }
        }
        
        //for parallel file systems
        if(!pfslist.isEmpty()){
            Resource STORAGE_NODE = RdfOwl.createResource(model, topologyURI+":storagenode", node);
            model.add(model.createStatement(dtnTopology, hasNode, STORAGE_NODE));
            for (FileSystem pf : pfslist){
                String fs_id = pf.getMountPoint();
                Resource PARALLELEFS = RdfOwl.createResource(model, topologyURI+":parallelfilesystem-"+fs_id, node);
                model.add(model.createStatement(STORAGE_NODE, hasFileSystem, PARALLELEFS));
                model.addLiteral(PARALLELEFS, fs_type, ResourceFactory.createTypedLiteral(pf.getType(),XSDDatatype.XSDstring));
                model.addLiteral(PARALLELEFS, mount, ResourceFactory.createTypedLiteral(pf.getMountPoint(),XSDDatatype.XSDstring));
                if (pf.isBlockStorage()){
                    Resource BLOCKSERVICE = RdfOwl.createResource(model, PARALLELEFS.getURI()+":blockstorageservice", blockStorageService);
                    model.add(model.createStatement(PARALLELEFS, hasService, BLOCKSERVICE));
                    model.add(model.createStatement(STORAGE_NODE, hasService, BLOCKSERVICE));
                    Resource VOLUME = RdfOwl.createResource(model, PARALLELEFS.getURI()+":volume", volume);
                    model.add(model.createStatement(BLOCKSERVICE, providesVolume, VOLUME));
                    model.add(model.createStatement(STORAGE_NODE, hasVolume, VOLUME));
                    model.addLiteral(VOLUME, disk_cap, pf.getSize());
                    model.addLiteral(VOLUME, disk_avail, pf.getAvailableSize());
                }else{
                    Resource OBJECTSERVICE = RdfOwl.createResource(model, PARALLELEFS.getURI()+":objectstorageservice", objectStorageService);
                    model.add(model.createStatement(PARALLELEFS, hasService, OBJECTSERVICE));
                    model.add(model.createStatement(STORAGE_NODE, hasService, OBJECTSERVICE));
                    Resource BUCKET = RdfOwl.createResource(model, PARALLELEFS.getURI()+":bucket", bucket);
                    model.add(model.createStatement(OBJECTSERVICE, providesBucket, BUCKET));
                    model.add(model.createStatement(STORAGE_NODE, hasVolume, BUCKET));
                    model.addLiteral(BUCKET, disk_cap, pf.getSize());
                    model.addLiteral(BUCKET, disk_avail, pf.getAvailableSize());
                }
            }
            //create connections
            Resource fs_port = RdfOwl.createResource(model, STORAGE_NODE.getURI()+":biport", biPort);
            model.add(model.createStatement(STORAGE_NODE, hasBidirectionalPort, fs_port));
            Resource counter_port = RdfOwl.createResource(model, INTERCONNECTION.getURI()+":storage-biport", biPort);
            model.add(model.createStatement(INTERCONNECTION, hasBidirectionalPort, counter_port));
            model.add(model.createStatement(fs_port,isAlias,counter_port));
            model.add(model.createStatement(counter_port,isAlias,fs_port));
        }

        return model;
    }
}
