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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author xin
 */
@SuppressWarnings("serial")
public class NIC implements Serializable {

    private final String nic_id;
    private final String link_type;
    private final String ip_addr;
    private final String uid;
    private static final String txKey = "tx-bytes", rxKey = "rx-bytes",
    							epochKey = "epoch", globalKey = "global";
    private static final long defaultInterval = 60L;
    private long link_capacity_Mbps;

    public static
    enum RateUnit {
    	Kbps, Mbps;
    }

	public
	NIC(String id, String link_type, String ip_addr, long link_capacity_Mbps) {
        this.nic_id = id;
        this.link_type = link_type;
        this.ip_addr = ip_addr;
        this.link_capacity_Mbps = link_capacity_Mbps;
        this.uid = null;
	}
	
    @SuppressWarnings("unchecked")
	public
	NIC(String id, String link_type, String ip_addr, long link_capacity_Mbps, long rxBytes, long txBytes, String epoch) {
    	Logger.getLogger(CCSNode.class.getName()).log(Level.INFO, "Creating public interface:" + id);
        this.nic_id = id;
        this.link_type = link_type;
        this.ip_addr = ip_addr;
        this.link_capacity_Mbps = link_capacity_Mbps;
        this.uid = "login-" + this.ip_addr;
        
        // Send NIC usage stats to persistent storage
        GlobalPersistentStore store = GlobalPersistentStore.get();
        if (!store.exists(uid))
        	store.create(uid);
        
        List<Long> rxb,
        		   txb,
        		   epochList;
        if (!store.contains(uid, rxKey)) {
        	rxb = new ArrayList<>();
        	store.add(uid, rxKey, rxb);
        }
        else {
        	rxb = (List<Long>) store.get(uid, rxKey);
        	if (rxb.size() > 119)
        		rxb.remove(0);
        }
        rxb.add(rxBytes);
        
        // Add up to 120 tx-byte entries
        if (!store.contains(uid, txKey)) {
        	txb = new ArrayList<>();
        	store.add(uid, txKey, txb);
        }
        else {
        	txb = (List<Long>) store.get(uid, txKey);
        	if (txb.size() > 119)
        		txb.remove(0);
        }
        txb.add(txBytes);
        
        if (!store.contains(uid, epochKey)) {
        	epochList = new ArrayList<>();
        	store.add(uid, epochKey, epochList);
        }
        else {
        	epochList = (List<Long>) store.get(uid, epochKey);
        	if (epochList.size() > 119)
        		epochList.remove(0);
        }
        epochList.add( (epoch != null) ? Long.parseLong(epoch) : 0L );
    }

    public
    void setLinkCapacity(long link_cap) {
        this.link_capacity_Mbps = link_cap;
    }

    public
    String getNICid() {
        return this.nic_id;
    }

    public
    String getLinkType() {
        return this.link_type;
    }

    public
    String getIPAddress() {
        return this.ip_addr;
    }

    public
    long getLinkCapacity() {
        return this.link_capacity_Mbps;
    }
    
    public
    boolean isPrivate() {
    	return (uid == null);
    }
    
    @SuppressWarnings("unchecked")
	public
    long getRXUsage(RateUnit ru) {
    	GlobalPersistentStore store = GlobalPersistentStore.get();
    	long retval = -1L;
    	if (store.contains(uid, rxKey)) {
    		List<Long> rxBytes = (List<Long>) store.get(uid, rxKey);
    		retval = calcCurrentLoad(rxBytes, ru);
    	}
    	else {
    		if (!store.exists(uid))
    			generateWarningLog("Storage for uid:" + uid + " not found", null);
    		else if (!store.contains(uid, rxKey))
    			generateWarningLog("Key:" + rxKey + " not found", null);
    	}
    	return retval;
    }
    
    @SuppressWarnings("unchecked")
	public
    long getTXUsage(RateUnit ru) {
    	GlobalPersistentStore store = GlobalPersistentStore.get();
    	long retval = -1L;
    	if (store.exists(uid) && store.contains(uid, txKey)) {
    		List<Long> txBytes = (List<Long>) store.get(uid, txKey);
    		retval = calcCurrentLoad(txBytes, ru);
    	}
    	return retval;
    }
    
    private static
    void generateWarningLog(String message, List<Long> loadHistory) {
    	Logger logger = Logger.getLogger(CCSNDriver.class.getName());
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		String methodName = stackTraceElements[1].getMethodName();
		StringBuilder sb = new StringBuilder();
		sb.append(methodName + "::" + message + "\n");
		logger.log(Level.WARNING, sb.toString());
    }
    
    @SuppressWarnings("unchecked")
	private static
    long calcCurrentLoad(List<Long> bytes, RateUnit ru) {
    	List<Long> epoch = (List<Long>) GlobalPersistentStore.get().get(globalKey, epochKey);
    	long retval = -1L;
		int len = bytes.size();
		if (len > 1) {
	    	long seconds = epoch.get(epoch.size() - 1) - epoch.get(epoch.size() - 2);
	    	seconds = (seconds > 0) ? seconds : defaultInterval;	// If epoch isn't being serialized by agent or is not changing (stale config file)
	    															//	then set to default interval (best guess)
    		retval = (bytes.get(len - 1) * 8 - bytes.get(len - 2) * 8) / seconds;
    		
    		switch(ru) {
    		case Kbps:
    			retval /= 1000L;
    			break;
    		case Mbps:
    			retval /= 1000000L;
    		}
		}
		return retval;
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
