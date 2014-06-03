/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import static javax.ejb.LockType.READ;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.PersistenceManager;
import net.maxgigapop.mrs.session.IHandleDriverSystemCall;

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
    
    private Map<DriverInstance, Future<String>> pullResultMap = new HashMap<DriverInstance, Future<String>>();
    
    @PostConstruct
    public void init() {
        PersistenceManager.initialize(entityManager);
        DriverInstancePersistenceManager.refreshAll();
    }
    
    @Lock(READ)
    @Schedule(minute = "*/3", hour = "*", persistent = false)
    public void run() {
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null
            || DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().isEmpty()) {
            DriverInstancePersistenceManager.refreshAll();
        }
        Context ejbCxt = null;
        for (String topoUri : DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().keySet()) {
            DriverInstance driverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(topoUri);
            Future<String> previousResult = pullResultMap.get(driverInstance);
            try {
                if (previousResult != null) {
                    if (previousResult.isDone()) {
                        String status = previousResult.get();
                        if (status.contains("FAILED")) {
                            //@TODO: pull error handling (retry in this current pull, then exception if still failed)
                        }
                    } else {
                        //@TODO: pull timeout handling (skip this current pull and allow one more cycle)
                        previousResult.cancel(true);
                    }
                }
                if (ejbCxt == null) {
                    ejbCxt = new InitialContext();
                }
                String driverEjbPath = driverInstance.getDriverEjbPath();
                IHandleDriverSystemCall driverSystemHandler = (IHandleDriverSystemCall) ejbCxt.lookup(driverEjbPath);
                // Call Async pullModel
                Future<String> result = driverSystemHandler.pullModel(driverInstance);
                pullResultMap.put(driverInstance, result);
            } catch (Exception e) {
                throw new EJBException(e);
            }
        }
    }
}
