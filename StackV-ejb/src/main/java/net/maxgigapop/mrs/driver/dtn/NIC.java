/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author xin
 */
public class NIC implements Serializable {

    private final String nic_id;
    private final String link_type;
    private final String ip_addr;
    private long link_capacity_Mbps;
    private String link_duplex_type;

    public NIC(String id, String link_type, String ip_addr) {
        this.nic_id = id;
        this.link_type = link_type;
        this.ip_addr = ip_addr;
    }

    public void setLinkCapacity(long link_cap) {
        this.link_capacity_Mbps = link_cap;
    }

    public void setLinkDuplex(String duplex_type) {
        this.link_duplex_type = duplex_type;
    }
    
    public String getNICid() {
        return this.nic_id;
    }

    public String getLinkType() {
        return this.link_type;
    }

    public String getIPAddress() {
        return this.ip_addr;
    }

    public long getLinkCapacity() {
        return this.link_capacity_Mbps;
    }

    public String getLinkDuplex() {
        return this.link_duplex_type;
    }

    @Override
    public String toString() {
        String tmp = null;
        tmp += "NIC_ID=" + this.nic_id;
        tmp += "|Link_type=" + this.link_type;
        tmp += "|IP_address=" + this.ip_addr;
        tmp += "|Link_capacity=" + this.link_capacity_Mbps;
        return tmp;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileSystem)) {
            return false;
        }
        NIC other = (NIC) obj;
        return !((this.ip_addr == null && other.ip_addr != null) || (this.ip_addr != null && !this.ip_addr.equals(other.ip_addr)));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.nic_id);
        hash = 29 * hash + Objects.hashCode(this.ip_addr);
        return hash;
    }

}
