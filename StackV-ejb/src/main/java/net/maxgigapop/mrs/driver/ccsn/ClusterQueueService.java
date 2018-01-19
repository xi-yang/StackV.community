package net.maxgigapop.mrs.driver.ccsn;

class ClusterQueueService extends QueueService {

	private int nodes;
	
	public ClusterQueueService(String name, int nodes) {
		super(name);
		this.nodes = nodes;
	}
	
	public
	int getAssignedNodes() {
		return nodes;
	}
}
