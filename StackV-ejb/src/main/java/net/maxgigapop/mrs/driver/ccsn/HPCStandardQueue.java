package net.maxgigapop.mrs.driver.ccsn;

class HPCStandardQueue extends QueueService {
	private int assignedNodes;

	HPCStandardQueue(String name, int assignedNodes) {
		super(name);
		this.assignedNodes = assignedNodes;
	}
	
	public
	int getAssignedNodes() {
		return assignedNodes;
	}
}
