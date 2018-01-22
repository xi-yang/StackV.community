/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.ccsn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author xin, jboley
 */
@SuppressWarnings("serial")
public class CCSNode implements Serializable {

	private final String ip, hostname;
	private int cpu;
	private long mem;
	private List<NIC> nics;
	private String tcp_rbuff, tcp_wbuff;
	private String scheduler, clusterType;
	private ComputeResource computeArch = null;

	public
	CCSNode(String ip, String hostname) {
		this.ip = ip;
		this.hostname = hostname;
		nics = new ArrayList<>();
	}

	public
	void setCPU(int cpu) {
		this.cpu = cpu;
	}

	public
	void setMemory(long mem) {
		this.mem = mem;
	}

	public
	void setNICs(List<NIC> nics) {
		this.nics = nics;
	}

	public
	void setTCPReadBuffer(String tcp_rbuff) {
		this.tcp_rbuff = tcp_rbuff;
	}

	public
	void setTCPWriteBuffer(String tcp_wbuff) {
		this.tcp_wbuff = tcp_wbuff;
	}
    
	public
	void setScheduler(String name) {
		scheduler = name;
	}
    
	public
	void setClusterType(String type) {
		clusterType = type;
	}
	
	public
	void setComputeResource(ComputeResource computeArch) {
		this.computeArch = computeArch;
	}
    
	public
	void setJobQueues(List<QueueService> queues) {
		queues.addAll(queues);
	}

	public
	String getIP() {
		return this.ip;
	}

	public
	String getHostName() {
		return this.hostname;
	}

	public
	int getNumCPU() {
		return this.cpu;
	}

	public
	long getMemorySize() {
		return this.mem;
	}

	public List<NIC> getNICs() {
		return this.nics;
	}

	public String getTCPReadBuffer() {
		return this.tcp_rbuff;
	}

	public String getTCPWriteBuffer() {
		return this.tcp_wbuff;
	}
	
	public
	ComputeResource getComputeResource() {
		return computeArch;
	}
    
	public String getScheduler() {
		return scheduler;
	}
    
	public String getClusterType() {
		return clusterType;
	}
    
	@Override
	public String toString() {
		String tmp = null;
		tmp += "IP: " + this.ip;
		tmp += ";Hostname: " + this.hostname;
		tmp += ";CPU(s): " + this.cpu;
		tmp += ";Memeory(kB): " + this.mem;
		tmp += ";NIC(s): " + this.nics.toString();
		tmp += ";TCP_Read_Buffer: " + this.tcp_rbuff;
		tmp += ";TCP_Write_Buffer: " + this.tcp_wbuff;
		return tmp;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CCSNode)) {
			return false;
		}
		CCSNode other = (CCSNode) obj;
		return !((this.ip == null && other.ip != null) || (this.ip != null && !this.ip.equals(other.ip)));
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + Objects.hashCode(this.ip);
		return hash;
	}
}
