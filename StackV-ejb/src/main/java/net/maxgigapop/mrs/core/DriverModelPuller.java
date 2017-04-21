/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.maxgigapop.mrs.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.ejb.AsyncResult;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
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
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
@Startup
public class DriverModelPuller {

    private @PersistenceContext(unitName = "RAINSAgentPU")
    EntityManager entityManager;

    private static final StackLogger logger = new StackLogger(DriverModelPuller.class.getName(), "DriverModelPuller");

    private Map<DriverInstance, Future<String>> pullResultMap = new HashMap<DriverInstance, Future<String>>();

    @PostConstruct
    public void init() {
        if (PersistenceManager.getEntityManager() == null) {
            PersistenceManager.initialize(entityManager);
        }
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null) {
            DriverInstancePersistenceManager.refreshAll();
        }
        logger.init();
    }

    @Lock(LockType.WRITE)
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void run() {
        String method = "run";
        logger.trace_start(method);
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null
                || DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().isEmpty()) {
            DriverInstancePersistenceManager.refreshAll();
        }
        Context ejbCxt = null;
        for (String topoUri : DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().keySet()) {
            DriverInstance driverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(topoUri);
            Future<String> previousResult = pullResultMap.get(driverInstance);
            if (previousResult != null) {
                if (previousResult.isDone()) {
                    try {
                        String status = previousResult.get();
                        logger.trace(method, "model pulling - previousResult ready for topologyURI="+topoUri+" with status="+status);
                    } catch (Exception ex) {
                        logger.catching(method, ex);
                        //@TODO: retry couple of times, then exception if still failed
                    }
                } else {
                    logger.trace(method, "model pulling - previousResult not ready for topologyURI="+topoUri);
                    //@TODO: timeout handling: skip and check after one more cycle, then previousResult.cancel(true); 
                }
            }
            try {
                if (ejbCxt == null) {
                    ejbCxt = new InitialContext();
                }
                String driverEjbPath = driverInstance.getDriverEjbPath();
                IHandleDriverSystemCall driverSystemHandler = (IHandleDriverSystemCall) ejbCxt.lookup(driverEjbPath);
                // Call async pullModel -> the driverInstance persistence session will be invalid in another session bean / thread
                Future<String> result = driverSystemHandler.pullModel(driverInstance.getId());
                pullResultMap.put(driverInstance, result);
                logger.trace(method, "model pulling - putting async result for topologyURI="+topoUri);
            } catch (Exception ex) {
                logger.catching(method, ex);
            }
        }
        logger.trace_end(method);
    }
}
