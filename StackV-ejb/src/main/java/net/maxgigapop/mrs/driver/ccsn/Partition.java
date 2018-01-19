package net.maxgigapop.mrs.driver.ccsn;

class Partition {

	String name, state, geometry;
	int size;
	
	Partition(String name, int size, String state, String geometry) {
		this.name = name;
		this.size = size;
		this.state = state;
		this.geometry = geometry;
	}
	
	String getName() {
		return name;
	}
	
	int getSize() {
		return size;
	}
	
	String getState() {
		return state;
	}
	
	String getGeometry() {
		return geometry;
	}
	
}
