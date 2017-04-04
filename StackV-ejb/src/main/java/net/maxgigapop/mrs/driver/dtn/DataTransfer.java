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
class DataTransfer implements Serializable {

    private final String source;
    private final String destination;
    private final String task_id;
    private long bytes_transferred;
    private long files_transferred;
    private int parallelism;
    private int concurrency;
    private long request_time;
    private long completion_time;
    private double mbits_sec;
    private String protocol;
    private long backgroud_transfers;
    private String backgroud_traffic;
    
    public DataTransfer(String task_id, String source, String destination) {
        this.task_id = task_id;
        this.source = source;
        this.destination = destination;
    }

    public void setDetails(long bt, long ft, int p, int cc, long rt, long ct, double mbits) {
        this.bytes_transferred = bt;
        this.files_transferred = ft;
        this.parallelism = p;
        this.concurrency = cc;        
        this.request_time = rt;
        this.completion_time = ct;
        this.mbits_sec = mbits;        
    }
    
    public void setBackgroud(long at, String traffic){       
        this.backgroud_transfers = at;
        this.backgroud_traffic = traffic;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSource() {
        return this.source;
    }

    public String getDestination() {
        return this.destination;
    }

    public String getTaskID() {
        return this.task_id;
    }

    public String getProtocol() {
        return this.protocol;
    }

    @Override
    public String toString() {
        String tmp = this.source + ";"
                    + this.destination + ";"
                    + this.task_id + ";"
                    + this.bytes_transferred + ";"
                    + this.files_transferred + ";"
                    + this.parallelism + ";"
                    + this.concurrency + ";"
                    + this.request_time + ";"
                    + this.completion_time + ";"
                    + this.mbits_sec + ";"
                    + this.backgroud_transfers + ";"
                    + this.backgroud_traffic;
        return tmp;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataTransfer)) {
            return false;
        }
        DataTransfer other = (DataTransfer) obj;
        return !((this.task_id == null && other.task_id != null) || (this.task_id != null && !this.task_id.equals(other.task_id)));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.task_id);
        return hash;
    }

}
