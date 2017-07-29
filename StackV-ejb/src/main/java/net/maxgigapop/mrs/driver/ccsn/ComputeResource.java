package net.maxgigapop.mrs.driver.ccsn;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;

abstract
class ComputeResource {

	private String scheduler, identifier;
	private int totalNodes, availNodes;
	private int coresPerNode;
	private String memPerNode;
	private List<QueueService> queues = new ArrayList<>();
	
	ComputeResource(String name, String scheduler, int totalNodes, int availNodes, int coresPerNode, String memPerNode) {
		this.scheduler = scheduler;
		this.identifier = name;
		this.totalNodes = totalNodes;
		this.availNodes = availNodes;
		this.coresPerNode = coresPerNode;
		this.memPerNode = memPerNode;
	}
	
	public
	int getTotalNodes() {
		return totalNodes;
	}
	
	public
	int getAvailNodes() {
		return availNodes;
	}
	
	public
	int getCoresPerNode() {
		return coresPerNode;
	}
	
	public
	String getMemPerNode() {
		return memPerNode;
	}
	
	protected
	List<QueueService> getJobQueues() {
		return queues;
	}
	
	protected
	void setJobQueue(QueueService queue) {
		queues.add(queue);
	}
	
	void parseServiceConfigsXml(Element compute) {
		// Get queue, job and reservation info
		if (compute.getElementsByTagName("queues").getLength() != 0) {
			Node tmpNode = compute.getElementsByTagName("queues").item(0);
			if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
				NodeList queuesList = compute.getElementsByTagName("queue");
				for (int i = 0; i < queuesList.getLength(); ++i) {
					if (queuesList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						Element queueNode = (Element) queuesList.item(i);
						
						// Retrieve job queue information
						QueueService queue = this.parseQueueServiceConfigsXml(queueNode);
						
						// Retrieve job information
						NodeList jobs = queueNode.getElementsByTagName("jobs")
								.item(0)
								.getChildNodes();
						for (int j = 0; j < jobs.getLength(); ++j) {
							if (jobs.item(j).getNodeType() == Node.ELEMENT_NODE) {
								Element jobNode = (Element) jobs.item(j);
								Job job = new Job(
									Integer.parseInt(jobNode.getAttribute("id")),
									Integer.parseInt(jobNode.getAttribute("nodes")),
									DateTimeParser.timestampToSeconds(jobNode.hasAttribute("queued-time") ? jobNode.getAttribute("queued-time") : "N/A"),
									DateTimeParser.timestampToSeconds(jobNode.hasAttribute("run-time") ? jobNode.getAttribute("run-time") : "N/A"),
									DateTimeParser.timestampToSeconds(jobNode.hasAttribute("wall-time") ? jobNode.getAttribute("wall-time") : "N/A"),
									JobStatus.fromString(jobNode.hasAttribute("status") ? jobNode.getAttribute("status") : "Indeterminate"),
									jobNode.hasAttribute("state") ? jobNode.getAttribute("state") : "Indeterminate"
									);
								queue.addJob(job);
							}
						}
						
						// Retrieve reservation information
						NodeList reservations = queueNode.getElementsByTagName("reservations")
								.item(0)
								.getChildNodes();
						for (int j = 0; j < reservations.getLength(); ++j) {
							if (reservations.item(j).getNodeType() == Node.ELEMENT_NODE) {
								Element reservNode = (Element) reservations.item(j);
								Reservation reservation = new Reservation()
										.setTitle(reservNode.getAttribute("name"))
										.setUsers(reservNode.hasAttribute("users") ? reservNode.getAttribute("users") : reservNode.getAttribute("user"), ":")
										.setStartTime(reservNode.hasAttribute("start") ? reservNode.getAttribute("start") : "N/A")
										.setDuration(reservNode.hasAttribute("duration") ? reservNode.getAttribute("duration") : "N/A")
										.setRemaining(reservNode.hasAttribute("remaining") ? reservNode.getAttribute("remaining") : "N/A");
								queue.addReservation(reservation);
							}
						}
						
						queues.add(queue);
					}
				}
			}
		}
	}
	
	public
	OntModel createOntology(OntModel model, Resource CCS_NODE, String topologyURI) {
	    // Global properties
	    Property hasService = Nml.hasService;
	
	    // Global data properties
	    Property cpu = Mrs.num_core;
		Property cpu_avail = Mrs.num_core_avail;
	    Property type = Mrs.type;
	    Property cnodes_tot = Mrs.cluster_nodes_total;
	    Property cnodes_avail = Mrs.cluster_nodes_available;
	
	    // Global resources
	    Resource computeResource = Mrs.ComputeClusterService;
		Resource schedulerService = Mrs.SchedulerService;

		Resource COMPUTE_RESOURCE = RdfOwl.createResource(model, CCS_NODE.getURI() + ":compute-" + identifier, computeResource);
		model.add(model.createStatement(CCS_NODE, hasService, COMPUTE_RESOURCE));
		model.addLiteral(COMPUTE_RESOURCE, cnodes_tot, totalNodes);
		model.addLiteral(COMPUTE_RESOURCE, cnodes_avail, availNodes);
		model.addLiteral(COMPUTE_RESOURCE, cpu, coresPerNode * totalNodes);
		model.addLiteral(COMPUTE_RESOURCE, cpu_avail, coresPerNode * availNodes);
		
		// Generate hardware layer
		addHardwareLayer(model, COMPUTE_RESOURCE);

		// Generate service layer
		Resource SCHEDULERSRVC = RdfOwl.createResource(model, CCS_NODE.getURI() + ":scheduler", schedulerService);
		model.add(model.createStatement(CCS_NODE, hasService, SCHEDULERSRVC));
		model.addLiteral(SCHEDULERSRVC, type, ResourceFactory.createTypedLiteral(scheduler, XSDDatatype.XSDstring));
		addServiceLayer(model, SCHEDULERSRVC);
                
        return model;
	}
    
	abstract
	void parseHardwareConfigsXml(Element compute);
	
	protected abstract
	QueueService parseQueueServiceConfigsXml(Element queueNode);

	protected abstract
	void addHardwareLayer(OntModel model, Resource COMPUTE_RESOURCE);
	
	protected abstract
	void addServiceLayer(OntModel model, Resource SCHEDULERSRVC);
}
