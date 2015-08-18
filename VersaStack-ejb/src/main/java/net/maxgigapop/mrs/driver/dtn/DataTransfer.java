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
class DataTransfer implements Serializable{
    private final String source;
    private final String destination;
    private final String task_id;
    private String parallelism;
    private String concurrency;
    private String pipeline;
    private String protocol;
    
    public DataTransfer(String task_id, String source, String destination){
        this.task_id = task_id;
        this.source = source;
        this.destination = destination;
    }
    
    public void setParallelism(String parallelism){
        this.parallelism = parallelism;
    }
    
    public void setConcurrency(String concur){
        this.concurrency = concur;
    }
    
    public void setPipeline(String pipe){
        this.pipeline = pipe;
    }
    
    public void setProtocol(String protocol){
        this.protocol = protocol;
    }
    
    public String getSource(){
        return this.source;
    }
    
    public String getDestination(){
        return this.destination;
    }
    
    public String getTaskID(){
        return this.task_id;
    }
    
    public String getParallelism(){
        return this.parallelism;
    }
    
    public String getConcurrency(){
        return this.concurrency;
    }
    
    public String getPipeline(){
        return this.pipeline;
    }
    
    public String getProtocol(){
        return this.protocol;
    }
    
    @Override
    public String toString(){
        String tmp = null;
        tmp += "Source="+this.source;
        tmp += "|Destination="+this.destination;
        tmp += "|Protocol="+this.protocol;
        return tmp;
    }
    
    @Override
    public boolean equals(Object obj){
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
