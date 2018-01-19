package net.maxgigapop.mrs.driver.ccsn;

public enum JobStatus {
	Running,
	Queued,
	Suspended,
	Indeterminate;
	
	public static
	JobStatus fromString(String status) throws IllegalArgumentException {
		JobStatus returned;
		switch (status) {
		case "running":
		case "RUNNING":
			returned = Running;
			break;
			
		case "queued":
		case "pending":
		case "QUEUED":
		case "PENDING":
			returned = Queued;
			break;
			
		case "suspended":
		case "stopped":
		case "SUSPENDED":
		case "STOPPED":
			returned = Suspended;
			break;
		default:
			returned = Indeterminate;
		}
		
		return returned;
	}
}