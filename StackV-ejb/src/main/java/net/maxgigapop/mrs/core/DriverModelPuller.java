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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.naming.Context;
import javax.naming.InitialContext;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.GlobalPropertyPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
public class DriverModelPuller {
    //timerService managed by MSC singleton service
    @Resource
    private TimerService timerService;

    private Map<DriverInstance, Future<String>> pullResultMap = new HashMap<DriverInstance, Future<String>>();

    private static final StackLogger logger = new StackLogger(DriverModelPuller.class.getName(), "DriverModelPuller");

    public void start() {
        // bootStrapped set to false
        GlobalPropertyPersistenceManager.setProperty("system.boot_strapped", "false");
        // schedule model pull timer
        ScheduleExpression sexpr = new ScheduleExpression();
        sexpr.hour("*").minute("*").second("0"); // every minute
        // persistent must be false because the timer is started by the HASingleton service
        timerService.createCalendarTimer(sexpr, new TimerConfig("", false));
    }
    
    public void stop() {
        for (Object obj : timerService.getTimers()) {
            Timer t = (Timer)obj;
            t.cancel();
        }
        timerService.getTimers().clear();
        pullResultMap.clear();
    }
    
    @Lock(LockType.WRITE)
    @Timeout
    public void run() {
        String method = "run";
        logger.start(method);
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            logger.message(method, "running on host="+hostname);
        } catch (UnknownHostException ex) {
            ;
        }
        boolean bootStrapped = GlobalPropertyPersistenceManager.getProperty("system.boot_strapped").equals("true");
        if (!bootStrapped) {
            logger.trace(method, "bootstrapping - bootStrapped==false");
        }

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
        if (!bootStrapped) {
            // cleanning up from recovery
            logger.message(method, "cleanning up from recovery");
            VersionGroupPersistenceManager.cleanupAndUpdateAll(null);
            Date before24h = new Date(System.currentTimeMillis()-24*60*60*1000);
            VersionItemPersistenceManager.cleanupAllBefore(before24h);
            bootStrapped = true;
            GlobalPropertyPersistenceManager.setProperty("system.boot_strapped", bootStrapped ? "true" : "false");
            logger.message(method, "Done! - bootStrapped changed to true");
        }
        logger.trace_end(method);
    }
}
