package net.maxgigapop.mrs.driver.ccsn;

class Job {
	private int id, nodes;
	private long queued_time, run_time, wall_time;
	private JobStatus status;
	private String state;
	
	public
	Job(int id, int nodes, long queued_time, long run_time, long wall_time, JobStatus status, String state) {
		this.id = id;
		this.nodes = nodes;
		this.queued_time = queued_time;
		this.run_time = run_time;
		this.wall_time = wall_time;
		this.status = status;
		this.state = state;
	}
	
	public
	int getID() {
		return id;
	}
	
	public
	int getRequestedNodes() {
		return nodes;
	}
	
	public
	long getQueuedTime() {
		return queued_time;
	}
	
	public
	long getRunTime() {
		return run_time;
	}
	
	public
	long getWallTime() {
		return wall_time;
	}
	
	public
	JobStatus getStatus() {
		return status;
	}
	
	public
	String getState() {
		return state;
	}
	
	public static
	long convertTime(String time) {
		long result = 0;
		if (!time.equals("N/A")) {
			String timeArr[] = time.split(":");
			long factor = (long) Math.pow(60, timeArr.length - 1);
			for (String tmp : timeArr) {
				result += Long.parseLong(tmp) * factor;
				factor /= 60;
			}
		}
		
		return result;
	}
	
	public static
	String convertTime(long time) {
		StringBuilder result = new StringBuilder();
		long factor = 1;
		while (factor * 60 < time)
			factor *= 60;
		
		while (factor > 1) {
			result.append(time / factor);
			time %= factor;
			factor /= 60;
			if (factor > 1) result.append(':');
		}
		
		return result.toString();
	}
}
