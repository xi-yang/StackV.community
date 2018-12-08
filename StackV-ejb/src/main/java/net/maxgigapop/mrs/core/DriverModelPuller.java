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
        //sexpr.hour("*").minute("*").second("30/15"); // every 30 seconds, starting at 15th
        sexpr.hour("*").minute("*").second("30"); // every minute, starting at 30th sec
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
        logger.trace_start(method);
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            logger.trace(method, "running on host="+hostname);
        } catch (UnknownHostException ex) {
            ;
        }
        boolean bootStrapped = GlobalPropertyPersistenceManager.getProperty("system.boot_strapped").equals("true");
        if (!bootStrapped) {
            logger.trace(method, "bootstrapping - bootStrapped==false");
        }

        boolean pullNormal = true;
        
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null) {
            DriverInstancePersistenceManager.refreshAll();
        }
        // more DriverInstancePersistenceManager.refreshAll calls are executed by SystemModelCoordinator (per node singleton)
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().isEmpty()) {
            logger.trace_end(method);
            return;
        }
        
        Context ejbCxt = null;
        for (String topoUri : DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().keySet()) {
            DriverInstance driverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(topoUri);
            // get driverInstance operational metadata / properties
            boolean driverInstanceDisabled = false;
            driverInstance = (DriverInstance) DriverInstancePersistenceManager.merge(driverInstance);
            String strDisabled = driverInstance.getProperty("disabled");
            if (strDisabled != null) {
                driverInstanceDisabled = Boolean.parseBoolean(strDisabled);
            }
            if (driverInstanceDisabled) {
                logger.trace(method, "model pulling skipped - driver instance ["+topoUri+"] disabled");
                continue;
            }
            int contErrors = 0;
            String strContErrors = driverInstance.getProperty("contErrors");
            if (strContErrors != null) {
                contErrors = Integer.parseInt(strContErrors);
            } else {
                strContErrors = "0";
            }
            int contDelays = 0;
            String strContDelays = driverInstance.getProperty("contDelays");
            if (strContDelays != null) {
                contDelays = Integer.parseInt(strContDelays);
            }
            Future<String> previousResult = pullResultMap.get(driverInstance);
            if (previousResult != null) {
                if (previousResult.isDone()) {
                    if (contDelays > 0) {
                        driverInstance.putProperty("contDelays", "0");
                    }
                    try {
                        String status = previousResult.get();
                        logger.trace(method, "model pulling - previousResult ready for topologyURI="+topoUri+" with status="+status);
                        if (contErrors > 0) {
                            driverInstance.putProperty("contErrors", "0");
                        }
                    } catch (Exception ex) {
                        pullNormal = false;
                        logger.catching(method, ex);
                        contErrors++;
                        strContErrors = Integer.toString(contErrors);
                        driverInstance.putProperty("contErrors", strContErrors);
                    } finally {
                        driverInstance = (DriverInstance)DriverInstancePersistenceManager.merge(driverInstance);
                    }
                } else {
                    pullNormal = false;
                    logger.trace(method, "model pulling - previousResult not ready for topologyURI="+topoUri);
                    // Assume the previous drvier instance will always finish or time out by itself
                    contDelays++;
                    strContDelays = Integer.toString(contDelays);
                    driverInstance.putProperty("contDelays", strContDelays);
                    driverInstance = (DriverInstance)DriverInstancePersistenceManager.merge(driverInstance);
                    continue;
                }
            } else {
                pullNormal = false;
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
                pullNormal = false;
            }
        }
        if (!bootStrapped && pullNormal) {
            // cleanning up from recovery
            logger.message(method, "cleanning up from recovery");
            GlobalPropertyPersistenceManager.setProperty("system.boot_strapped", "true");
            VersionGroupPersistenceManager.cleanupAndUpdateAll(null);
            Date before24h = new Date(System.currentTimeMillis()-24*60*60*1000);
            VersionItemPersistenceManager.cleanupAllBefore(before24h);
            logger.message(method, "Done! - bootStrapped changed to true");
        }
        logger.trace_end(method);
    }
}
