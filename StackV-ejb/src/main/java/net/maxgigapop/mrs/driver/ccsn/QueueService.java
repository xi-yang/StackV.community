package net.maxgigapop.mrs.driver.ccsn;

import java.util.ArrayList;
import java.util.List;

abstract
class QueueService {
	private String name, walltime;
	private int
		activeJobs = 0,
		pendingJobs = 0,
		stoppedJobs = 0;
	private List<Job> jobs = new ArrayList<>();
	private List<Reservation> reservations = new ArrayList<>();
	
	public QueueService(String name) {
		this.name = name;
	}
	
	public
	void setWalltime(String time) {
		this.walltime = time;
	}
	
	public
	void addJob(Job job) {
		jobs.add(job);
		switch (job.getStatus()) {
		case Running:
			activeJobs += 1;
			break;
		case Queued:
			pendingJobs += 1;
			break;
		case Suspended:
			stoppedJobs += 1;
			break;
		case Indeterminate:
			stoppedJobs += 1;
		}
	}

	public
	void addReservation(Reservation reservation) {
		reservations.add(reservation);
	}
	
	public
	List<Reservation> getReservations() {
		return reservations;
	}
	
	public
	void addJobs(List<Job> jobs) {
		for (Job job : jobs) addJob(job);
	}
	
	public
	String getName() {
		return name;
	}

	// TODO: Make this a DateTime object
	public
	String getWalltime() {
		return walltime;
	}
	
	public
	int getActiveJobs() {
		return activeJobs;
	}
	
	public
	int getPendingJobs() {
		return pendingJobs;
	}

	public
	int getSuspendedJobs() {
		return stoppedJobs;
	}
	
	public
	List<Job> getJobs() {
		return jobs;
	}
}
