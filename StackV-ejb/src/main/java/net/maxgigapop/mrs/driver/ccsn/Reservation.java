package net.maxgigapop.mrs.driver.ccsn;

import java.util.ArrayList;
import java.util.List;

class Reservation {
	private String
			reservationTitle,
			passthrough = null,
			startTime = null;
	private List<String>
			users = new ArrayList<>(),
			nodes = new ArrayList<>();
	private long
			duration = 0L,
			remaining = 0L,
			tminus = 0L;

	public
	Reservation setTitle(String title) {
		reservationTitle = title;
		return this;
	}

	public
	Reservation setPassthrough(String passthrough) {
		this.passthrough = passthrough;
		return this;
	}

	public
	Reservation setUsers(String users, String separator) {
		for (String user : users.split(separator))
			this.users.add(user);
		return this;
	}

	public
	Reservation setReservedNodes(String nodes, String separator) {
		for (String node : nodes.split(separator))
			this.nodes.add(node);
		return this;
	}

	public
	Reservation setDuration(String duration) {
		this.duration = DateTimeParser.timestampToSeconds(duration);
		return this;
	}

	public
	Reservation setRemaining(String remaining) {
		this.remaining = DateTimeParser.timestampToSeconds(remaining);
		return this;
	}

	public
	Reservation setTMinus(String tminus) {
		this.tminus = DateTimeParser.timestampToSeconds(tminus);
		return this;
	}

	public
	Reservation setStartTime(String startTime) {
		this.startTime = startTime;
		return this;
	}
	
	public
	String getReservationTitle() {
		return reservationTitle;
	}
	
	public
	String getPassthrough() {
		return passthrough;
	}
	
	public
	List<String> getUsers() {
		return users;
	}
	
	public
	List<String> getReservedNodes() {
		return nodes;
	}
	
	public
	String getStartTime() {
		return startTime;
	}
	
	public
	long getDuration() {
		return duration;
	}
	
	public
	long getRemaining() {
		return remaining;
	}
	
	public
	long getTMinus() {
		return tminus;
	}
}
