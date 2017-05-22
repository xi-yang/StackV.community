/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author xin
 */
public class DTNNode implements Serializable {

    private final String ip;
    private final String hostname;
    private int cpu;
    private double mem;
    private List<NIC> nics;
    private String tcp_rbuff;
    private String tcp_wbuff;
    private String traffic;

    public DTNNode(String ip, String hostname) {
        this.ip = ip;
        this.hostname = hostname;
        nics = new ArrayList<>();
    }

    public void setCPU(int cpu) {
        this.cpu = cpu;
    }

    public void setMemory(double mem) {
        this.mem = mem;
    }

    public void setNICs(List<NIC> nics) {
        this.nics = nics;
    }

    public void setTCPReadBuffer(String tcp_rbuff) {
        this.tcp_rbuff = tcp_rbuff;
    }

    public void setTCPWriteBuffer(String tcp_wbuff) {
        this.tcp_wbuff = tcp_wbuff;
    }

    public void setTraffic(String traffic) {
        this.traffic = traffic;
    }

    public String getIP() {
        return this.ip;
    }

    public String getHostName() {
        return this.hostname;
    }

    public int getNumCPU() {
        return this.cpu;
    }

    public double getMemorySize() {
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
    
    public String gettTraffic() {
        return this.traffic;
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
        if (!(obj instanceof DTNNode)) {
            return false;
        }
        DTNNode other = (DTNNode) obj;
        return !((this.ip == null && other.ip != null) || (this.ip != null && !this.ip.equals(other.ip)));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.ip);
        return hash;
    }
}
