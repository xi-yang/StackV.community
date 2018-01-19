package net.maxgigapop.mrs.driver.ccsn;

import org.w3c.dom.Element;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.RdfOwl;

class ComputeCluster extends ComputeResource {

	public ComputeCluster(String name, String scheduler, int totalNodes, int availNodes, int coresPerNode,
			String memPerNode) {
		super(name, scheduler, totalNodes, availNodes, coresPerNode, memPerNode);
	}

	@Override
	void parseHardwareConfigsXml(Element compute) {
		// Nothing to do here
	}

	@Override
	protected QueueService parseQueueServiceConfigsXml(Element queueNode) {
		return new ClusterQueueService(
				queueNode.getAttribute("name"),
				Integer.parseInt(queueNode.getAttribute("assigned-nodes"))
				);
	}

	@Override
	protected void addHardwareLayer(OntModel model, Resource COMPUTE_RESOURCE) {
		// Nothing to do here
	}

	@Override
	protected void addServiceLayer(OntModel model, Resource SCHEDULERSRVC) {
	    Property providesJobQueueSrvc = Mrs.providesJobQueueService;
	    Property activeJobs = Mrs.active_jobs;
	    Property queuedJobs = Mrs.queued_jobs;
	    Property stoppedJobs = Mrs.stopped_jobs;
	    Property assigned_nodes = Mrs.assigned_nodes;
		Resource jobQueueService = Mrs.JobQueueService;

		if (!super.getJobQueues().isEmpty()) {
        	for (QueueService queue : super.getJobQueues()) {
        		Resource JOBQUEUESRVC = RdfOwl.createResource(model, SCHEDULERSRVC.getURI() + ":jobqueue-" + queue.getName(), jobQueueService);
        		model.add(model.createStatement(SCHEDULERSRVC, providesJobQueueSrvc, JOBQUEUESRVC));
        		model.addLiteral(JOBQUEUESRVC, assigned_nodes, ((ClusterQueueService) queue).getAssignedNodes());
        		model.addLiteral(JOBQUEUESRVC, activeJobs, queue.getActiveJobs());
        		model.addLiteral(JOBQUEUESRVC, queuedJobs, queue.getPendingJobs());
        		model.addLiteral(JOBQUEUESRVC, stoppedJobs, queue.getSuspendedJobs());
        	}
        }
	}

}
