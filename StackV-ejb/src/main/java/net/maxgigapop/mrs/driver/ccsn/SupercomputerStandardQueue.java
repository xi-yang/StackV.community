package net.maxgigapop.mrs.driver.ccsn;

import java.util.ArrayList;
import java.util.List;

class SupercomputerStandardQueue extends QueueService {
	List<String> partitions = new ArrayList<>();
	
	SupercomputerStandardQueue(String name) {
		super(name);
	}

	void addPartitionRef(String id) {
		partitions.add(id);
	}
	
	public
	List<String> getAssignedPartitions() {
		return partitions;
	}
}
