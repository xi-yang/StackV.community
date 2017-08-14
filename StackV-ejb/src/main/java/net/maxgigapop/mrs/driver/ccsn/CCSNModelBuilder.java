 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.ccsn;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.maxgigapop.mrs.common.*;
import net.maxgigapop.mrs.driver.ccsn.NIC.RateUnit;
//import net.maxgigapop.mrs.driver.dtn.FileSystem;

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
public class CCSNModelBuilder {

    @SuppressWarnings("empty-statement")
    public static OntModel createOntology(String topologyURI, String transferCmdPattern, Map<String, List<Object>> endpointIDConfigurationMap)
                                    throws IOException {

        //create model object
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        //set all the model prefixes
        model.setNsPrefix("rdfs", RdfOwl.getRdfsURI());
        model.setNsPrefix("rdf", RdfOwl.getRdfURI());
        model.setNsPrefix("xsd", RdfOwl.getXsdURI());
        model.setNsPrefix("owl", RdfOwl.getOwlURI());
        model.setNsPrefix("nml", Nml.getURI());
        model.setNsPrefix("mrs", Mrs.getURI());

        // Global properties
        Property hasNode = Nml.hasNode;
        Property hasBidirectionalPort = Nml.hasBidirectionalPort;
        Property hasService = Nml.hasService;
        Property providesVolume = Mrs.providesVolume;
        Property providesBucket = Mrs.providesBucket;
        Property providesJobQueueSrvc = Mrs.providesJobQueueService;
        Property hasBucket = Mrs.hasBucket;
        Property hasVolume = Mrs.hasVolume;
        Property hasFileSystem = Mrs.hasFileSystem;

        // Global data properties
        Property cpu = Mrs.num_core;
        Property cpu_avail = Mrs.num_core_avail;
        Property memory_mb = Mrs.memory_mb;
        Property type = Mrs.type;
//        Property value = Nml.value;
//        Property values = Nml.values;
        Property link_capacity_Mbps = Mrs.capacity;
        Property disk_cap = Mrs.disk_gb;
        Property disk_avail = model.createProperty(model.getNsPrefixURI("mrs") + "disk_free_gb");
        Property mount = model.createProperty(model.getNsPrefixURI("mrs") + "mount_point");
//        Property cpu_usage = model.createProperty(model.getNsPrefixURI("mrs") + "measurement");
        Property cnodes_tot = Mrs.cluster_nodes_total;
        Property cnodes_avail = Mrs.cluster_nodes_available;
//        Property queue_name = Mrs.queue_name;
//        Property wall_time = Mrs.queue_wall_time;
        Property assigned_nodes = Mrs.assigned_nodes;
        Property activeJobs = Mrs.active_jobs;
        Property queuedJobs = Mrs.queued_jobs;
        Property stoppedJobs = Mrs.stopped_jobs;

        // Global resources
        Resource blockStorageService = Mrs.BlockStorageService;
        Resource objectStorageService = Mrs.ObjectStorageService;
        Resource bucket = Mrs.Bucket;
        Resource volume = Mrs.Volume;
        Resource topology = Nml.Topology;
        Resource computeClusterService = Mrs.ComputeClusterService;
        Resource jobQueueService = Mrs.JobQueueService;
//        Resource jobNode = Mrs.Job;
        Resource fileSystem = Mrs.FileSystem;
        Resource node = Nml.Node;
        Resource schedulerService = Mrs.SchedulerService;
        Resource biPort = Nml.BidirectionalPort;

        Resource ccTopology = RdfOwl.createResource(model, topologyURI, topology);

//	  Resource INTERCONNECTION = RdfOwl.createResource(model,topologyURI+":interconnection", switchingService);
//	  model.add(model.createStatement(ccTopology, hasService, INTERCONNECTION));

        Logger logger = Logger.getLogger(CCSNDriver.class.getName());

        final int
                NAME = 1,
                PULL_PARAMS = 2;
        for (String endpoint : endpointIDConfigurationMap.keySet()) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> pullUtilParams = (HashMap<String, String>) endpointIDConfigurationMap.get(endpoint).get(PULL_PARAMS);

            CCSNPull conf = new CCSNPull(transferCmdPattern, pullUtilParams);
            if (conf.getCCSNode() != null) {
                ComputeResource arch = conf.getCCSNode().getComputeResource();

            // Create the outer layer of the CCS model
            logger.log(Level.INFO, "Modeling endpoint " + endpoint);
            Resource CCS_NODE = RdfOwl.createResource(model, topologyURI + ":" + endpointIDConfigurationMap.get(endpoint).get(NAME), node);
            model.add(model.createStatement(ccTopology, hasNode, CCS_NODE));

            List<FileSystem> pfslist = new ArrayList<>();

            // Create login CPU and memory information
            model.addLiteral(CCS_NODE, cpu, conf.getCCSNode().getNumCPU());
//            model.addLiteral(CCS_NODE, cpu_usage, ResourceFactory.createTypedLiteral("cpu_usage=" + conf.getCPUload(), XSDDatatype.XSDstring));
            model.addLiteral(CCS_NODE, memory_mb, conf.getCCSNode().getMemorySize());

            //create NIC information
            if (!conf.getCCSNode().getNICs().isEmpty()) {
                for (NIC nic : conf.getCCSNode().getNICs()) {
                    String nic_id = nic.getNICid();
                    Resource NIC = RdfOwl.createResource(model, CCS_NODE.getURI() + ":nic-" + nic_id, biPort);
                    model.add(model.createStatement(CCS_NODE, hasBidirectionalPort, NIC));
                    model.addLiteral(NIC, type, ResourceFactory.createTypedLiteral(nic.getLinkType(), XSDDatatype.XSDstring));
                    if (nic.getLinkCapacity() != 0L)
                        model.addLiteral(NIC, link_capacity_Mbps, nic.getLinkCapacity());
                    if (!nic.isPrivate()) {
                        Property inbound_load = Mrs.inbound_load_kbps,
                                 outbound_load = Mrs.outbound_load_kbps;
                        model.addLiteral(NIC, inbound_load, nic.getRXUsage(RateUnit.Kbps));
                        model.addLiteral(NIC, outbound_load, nic.getTXUsage(RateUnit.Kbps));
                    }
                }
            }

            // Create compute cluster service description
            String debugStr = "Cluster summary:";
            debugStr += " total nodes=" + arch.getTotalNodes();
            debugStr += " avail nodes=" + arch.getAvailNodes();
            debugStr += " total cpus=" + arch.getCoresPerNode() * arch.getTotalNodes();
            debugStr += " avail cpus=" + arch.getCoresPerNode() * arch.getAvailNodes(); 
            logger.log(Level.INFO, debugStr);

            model = arch.createOntology(model, CCS_NODE, topologyURI);

            // Create storage information
//                if (conf.getFileSystems() != null && !conf.getFileSystems().isEmpty()) {
//                	for (FileSystem f : conf.getFileSystems()) {
//                		if (!f.isParallel()) {
//                    		String fs_id = f.getDeviceName();
//                    		Resource LOCALFS = RdfOwl.createResource(model, CCS_NODE.getURI() + ":localfilesystem-" + fs_id, fileSystem);
//                    		model.add(model.createStatement(CCS_NODE, hasFileSystem, LOCALFS));
//                    		model.addLiteral(LOCALFS, type, ResourceFactory.createTypedLiteral(f.getType(), XSDDatatype.XSDstring));
//                    		model.addLiteral(LOCALFS, mount, ResourceFactory.createTypedLiteral(f.getMountPoint(), XSDDatatype.XSDstring));
//                    		if (f.isBlockStorage()) {
//                    			Resource BLOCKSERVICE = RdfOwl.createResource(model, LOCALFS.getURI() + ":blockstorageservice", blockStorageService);
//                    			model.add(model.createStatement(LOCALFS, hasService, BLOCKSERVICE));
//                    			model.add(model.createStatement(CCS_NODE, hasService, BLOCKSERVICE));
//                    			Resource LOCALVOLUME = RdfOwl.createResource(model, LOCALFS.getURI() + ":localvolume", volume);
//                    			model.add(model.createStatement(BLOCKSERVICE, providesVolume, LOCALVOLUME));
//                    			model.add(model.createStatement(CCS_NODE, hasVolume, LOCALVOLUME));
//                    			model.addLiteral(LOCALVOLUME, disk_cap, f.getSize());
//                    			model.addLiteral(LOCALVOLUME, disk_avail, f.getAvailableSize());
//                    		} else {
//                    			Resource OBJECTSERVICE = RdfOwl.createResource(model, LOCALFS.getURI() + ":objectstorageservice", objectStorageService);
//                    			model.add(model.createStatement(LOCALFS, hasService, OBJECTSERVICE));
//                    			model.add(model.createStatement(CCS_NODE, hasService, OBJECTSERVICE));
//                    			Resource LOCALBUCKET = RdfOwl.createResource(model, LOCALFS.getURI() + ":localbucket", bucket);
//                    			model.add(model.createStatement(OBJECTSERVICE, providesBucket, LOCALBUCKET));
//                    			model.add(model.createStatement(CCS_NODE, hasBucket, LOCALBUCKET));
//                    			model.addLiteral(LOCALBUCKET, disk_cap, f.getSize());
//                    			model.addLiteral(LOCALBUCKET, disk_avail, f.getAvailableSize());
//                    		}
//                		} else {
//                    		if (!pfslist.contains(f)) {
//                        			pfslist.add(f);
//                    		}
//                		}
//                	}
//                }

            // For parallel file systems
//			    if (!pfslist.isEmpty()) {
//			        Resource STORAGE_NODE = RdfOwl.createResource(model, topologyURI + ":storagenode", node);
//			        model.add(model.createStatement(ccTopology, hasNode, STORAGE_NODE));
//			        for (FileSystem pf : pfslist) {
//			            String fs_id = pf.getMountPoint();
//			            Resource PARALLELEFS = RdfOwl.createResource(model, topologyURI + ":parallelfilesystem-" + fs_id, node);
//			            model.add(model.createStatement(STORAGE_NODE, hasFileSystem, PARALLELEFS));
//			            model.addLiteral(PARALLELEFS, type, ResourceFactory.createTypedLiteral(pf.getType(), XSDDatatype.XSDstring));
//			            model.addLiteral(PARALLELEFS, mount, ResourceFactory.createTypedLiteral(pf.getMountPoint(), XSDDatatype.XSDstring));
//			            if (pf.isBlockStorage()) {
//			                Resource BLOCKSERVICE = RdfOwl.createResource(model, PARALLELEFS.getURI() + ":blockstorageservice", blockStorageService);
//			                model.add(model.createStatement(PARALLELEFS, hasService, BLOCKSERVICE));
//			                model.add(model.createStatement(STORAGE_NODE, hasService, BLOCKSERVICE));
//			                Resource VOLUME = RdfOwl.createResource(model, PARALLELEFS.getURI() + ":volume", volume);
//			                model.add(model.createStatement(BLOCKSERVICE, providesVolume, VOLUME));
//			                model.add(model.createStatement(STORAGE_NODE, hasVolume, VOLUME));
//			                model.addLiteral(VOLUME, disk_cap, pf.getSize());
//			                model.addLiteral(VOLUME, disk_avail, pf.getAvailableSize());
//			            } else {
//			                Resource OBJECTSERVICE = RdfOwl.createResource(model, PARALLELEFS.getURI() + ":objectstorageservice", objectStorageService);
//			                model.add(model.createStatement(PARALLELEFS, hasService, OBJECTSERVICE));
//			                model.add(model.createStatement(STORAGE_NODE, hasService, OBJECTSERVICE));
//			                Resource BUCKET = RdfOwl.createResource(model, PARALLELEFS.getURI() + ":bucket", bucket);
//			                model.add(model.createStatement(OBJECTSERVICE, providesBucket, BUCKET));
//			                model.add(model.createStatement(STORAGE_NODE, hasVolume, BUCKET));
//			                model.addLiteral(BUCKET, disk_cap, pf.getSize());
//			                model.addLiteral(BUCKET, disk_avail, pf.getAvailableSize());
//			            }
//			    	}
//			    }
            }
        }

        return model;
    }
}
