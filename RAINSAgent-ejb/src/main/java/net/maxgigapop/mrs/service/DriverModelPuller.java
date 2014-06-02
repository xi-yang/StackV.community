/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.service;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import static javax.ejb.LockType.READ;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.PersistenceManager;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
@Startup
public class DriverModelPuller {
    private @PersistenceContext(unitName="RAINSAgentPU")    
    EntityManager entityManager;
    
    @PostConstruct
    public void init() {
        PersistenceManager.initialize(entityManager);
        DriverInstancePersistenceManager.refreshAll();
    }
    
    @Lock(READ)
    @Schedule(minute = "*/3", hour = "*", persistent = false)
    public void run() {
        // placeholder for puller logic
        //@TODO: should call Async HandleDriverSystem method for pulling
    }
}
