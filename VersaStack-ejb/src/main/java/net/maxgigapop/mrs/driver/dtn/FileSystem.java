/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author xin
 */
public class FileSystem implements Serializable{
    private final String deviceName;
    private final String type;
    private long size_kB;
    private long avail_kB;
    private String mount_point;
    private long block_size;
    
    public FileSystem(String deviceName, String type){
        this.deviceName = deviceName;
        this.type = type;
    }
    
    public void setSize(long size) {
        this.size_kB = size;
    }
    
    public void setAvailableSize(long size){
        this.avail_kB = size;
    }
    
    public void setMountPoint(String mount_point){
        this.mount_point = mount_point;
    }
    
    public void setBlockSize(long block_size) {
        this.block_size = block_size;
    }
    
    public String getType(){
        return this.type;
    }
    
    public String getDeviceName(){
        return this.deviceName;
    }
    
    public long getSize(){
        return this.size_kB;
    }
    
    public long getAvailableSize(){
        return this.avail_kB;
    }
    
    public String getMountPoint(){
        return this.mount_point;
    }
    
    public long getBlockSize(){
        return this.block_size;
    }
    
    public boolean isParallel(){
        List<String> pfslist = Arrays.asList("ceph", "pvfs", "gpfs", "lustre", "nfs", "coda");
        return pfslist.contains(this.type);
    }
    
    //todo: block vs object storage
    public boolean isBlockStorage(){
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.deviceName);
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileSystem)) {
            return false;
        }
        FileSystem other = (FileSystem) obj;
        return !((this.mount_point == null && other.mount_point != null) || (this.mount_point != null && !this.mount_point.equals(other.mount_point)));
    }
    
    @Override
    public String toString(){
        String tmp = null;
        tmp += "Device="+this.deviceName;
        tmp += "|Type="+this.type;
        tmp += "|Mount_point="+this.mount_point;
        tmp += "|Disk_capacity="+this.size_kB;
        return tmp;
    }
}
