/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

/**
 *
 * @author xyang
 */
public class ActionState {
    static final public String IDLE = "IDLE";
    static final public String PROCESSING = "PROCESSING";
    static final public String CANCELLED = "CANCELLED";
    static final public String FINISHED = "FINISHED";
    static final public String MERGED = "MERGED";
    static final public String FAILED = "FAILED";
}
