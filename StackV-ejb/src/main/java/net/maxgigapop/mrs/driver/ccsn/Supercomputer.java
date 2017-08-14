package net.maxgigapop.mrs.driver.ccsn;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.RdfOwl;

import java.util.ArrayList;
import java.util.List;

class Supercomputer extends ComputeResource {

    private List<Partition> partitions = new ArrayList<>();

    Supercomputer(String name, String scheduler, int totalNodes, int availNodes, int coresPerNode, String memPerNode) {
        super(name, scheduler, totalNodes, availNodes, coresPerNode, memPerNode);
    }

    @Override
    void parseHardwareConfigsXml(Element compute) {
        if (compute.getElementsByTagName("partitions").getLength() != 0) {
            NodeList partitionList = compute.getElementsByTagName("partitions")
                    .item(0)
                    .getChildNodes();
            for (int i = 0; i < partitionList.getLength(); ++i) {
                if (partitionList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element partitionNode = (Element) partitionList.item(i);
                    Partition partition = new Partition(
                            partitionNode.getAttribute("id"),
                            Integer.parseInt( partitionNode.getAttribute("size") ),
                            partitionNode.getAttribute("state"),
                            partitionNode.getAttribute("geometry")
                            );
                    partitions.add(partition);
                }
            }
        }
    }

    @Override
    protected
    QueueService parseQueueServiceConfigsXml(Element queueNode) {
        String name = queueNode.getAttribute("name");
        SupercomputerStandardQueue queue = new SupercomputerStandardQueue(name);
        NodeList assignedPartitions = queueNode.getElementsByTagName("partitions")
                .item(0)
                .getChildNodes();
        for (int i = 0; i < assignedPartitions.getLength(); ++i) {
            if (assignedPartitions.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element partitionNode = (Element) assignedPartitions.item(i);
                queue.addPartitionRef(partitionNode.getAttribute("id"));
            }
        }
        return queue;
    }

    @Override
    protected
    void addHardwareLayer(OntModel model, Resource COMPUTE_RESOURCE) {
        Property hasPartition = Mrs.hasPartition;
        Property assignedNodes = Mrs.assigned_nodes;
//        Property geometry = Mrs.geometry;
        Resource mrsPartition = Mrs.PhysicalPartition;

        if (!partitions.isEmpty()) {
            for (Partition partition : partitions) {
                Resource PARTITION = RdfOwl.createResource(model, COMPUTE_RESOURCE.getURI() + ":partition-" + partition.getName(), mrsPartition);
                model.add(model.createStatement(COMPUTE_RESOURCE, hasPartition, PARTITION));
                model.addLiteral(PARTITION, assignedNodes, partition.getSize());
//                model.addLiteral(PARTITION, geometry, ResourceFactory.createTypedLiteral(partition.getGeometry(), XSDDatatype.XSDstring));
            }
        }
    }

    @Override
    protected
    void addServiceLayer(OntModel model, Resource SCHEDULERSRVC) {
        Property providesJobQueueSrvc = Mrs.providesJobQueueService;
        Property activeJobs = Mrs.active_jobs;
        Property queuedJobs = Mrs.queued_jobs;
        Property stoppedJobs = Mrs.stopped_jobs;
        Resource jobQueueService = Mrs.JobQueueService;

        if (!getJobQueues().isEmpty()) {
            for (QueueService queue : getJobQueues()) {
                Resource JOBQUEUESRVC = RdfOwl.createResource(model, SCHEDULERSRVC.getURI() + ":jobqueue-" + queue.getName(), jobQueueService);
                model.add(model.createStatement(SCHEDULERSRVC, providesJobQueueSrvc, JOBQUEUESRVC));
                model.addLiteral(JOBQUEUESRVC, activeJobs, queue.getActiveJobs());
                model.addLiteral(JOBQUEUESRVC, queuedJobs, queue.getPendingJobs());
                model.addLiteral(JOBQUEUESRVC, stoppedJobs, queue.getSuspendedJobs());
            }
        }
    }
}
