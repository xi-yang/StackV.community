/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2015

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

import com.hp.hpl.jena.ontology.OntModel;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
@Startup
@AccessTimeout(value = 10000) // 10 seconds
public class SystemModelCoordinator {   
    
    @EJB
    HandleSystemCall systemCallHandler;
    
    private static final StackLogger logger = new StackLogger(SystemModelCoordinator.class.getName(), "SystemModelCoordinator");

    // indicator of system being ready for service
    boolean bootStrapped = false;
    // current VG with cached union ModelBase
    VersionGroup systemVersionGroup = null;

    @Lock(LockType.READ)
    public boolean isBootStrapped() {
        return bootStrapped;
    }
    
    @Lock(LockType.WRITE)
    public void setBootStrapped(boolean bl) {
        logger.message("setBootStrapped", String.format("set status from %b into %b", bootStrapped, bl));
        bootStrapped = bl;
        if (bootStrapped == false) {
            systemVersionGroup = null;
        }
    }
    
    @Lock(LockType.WRITE)
    public VersionGroup getSystemVersionGroup() {
        return systemVersionGroup;
    }

    @Lock(LockType.WRITE)
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void autoUpdate() {
        String method = "autoUpdate";
        logger.trace_start(method);
        if (!bootStrapped) {
            logger.message(method, "bootstrapping - bootStrapped==false");
        }
        //check driverInstances (catch: if someone unplug and plug a driver within a minute, we will have problem)
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null || ditMap.isEmpty()) {
            bootStrapped = false;
            systemVersionGroup = null;
            logger.warning(method, "ditMap == null or ditMap.isEmpty");
            logger.end(method);
            return;
        }
        for (DriverInstance di : ditMap.values()) {
            synchronized (di) { 
                if (di.getHeadVersionItem() == null) {
                    bootStrapped = false;
                    systemVersionGroup = null;
                    logger.warning(method, di + "has null headVersionItem");
                    logger.end(method);
                    return;
                }
            }
        }
        if (this.systemVersionGroup == null) {
            this.systemVersionGroup = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
            if (this.systemVersionGroup != null) {
                this.systemVersionGroup.createUnionModel();
            }
        } else {
            VersionGroup newVersionGroup = null;
            try {
                newVersionGroup = systemCallHandler.updateHeadVersionGroup(systemVersionGroup.getRefUuid());
            } catch (Exception ex) {
                this.systemVersionGroup = null;
                bootStrapped = false;
                logger.catching(method, ex);
                return;
            }
            if (newVersionGroup != null && !newVersionGroup.equals(systemVersionGroup)) {
                this.systemVersionGroup = newVersionGroup;
                this.systemVersionGroup.createUnionModel();
            }
        }
        if (!bootStrapped) {
            // cleanning up from recovery
            logger.message(method, "cleanning up from recovery");
            VersionGroupPersistenceManager.cleanupAndUpdateAll(systemVersionGroup);
            Date before24h = new Date(System.currentTimeMillis()-24*60*60*1000);
            VersionItemPersistenceManager.cleanupAllBefore(before24h);
            bootStrapped = true;
            logger.message(method, "Done! - bootStrapped changed to true");
        }
        logger.trace_end(method);
    }

    @Lock(LockType.WRITE)
    public VersionGroup getLatest() {
        String method = "getLatest";
        logger.trace_start(method);
        if (this.systemVersionGroup == null) {
            logger.trace(method, "this.systemVersionGroup == null");
            this.systemVersionGroup = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
            if (this.systemVersionGroup != null) {
                logger.trace(method, "created new ref:VersionGroup");
                this.systemVersionGroup.createUnionModel();
            }
        } else {
            VersionGroup newVersionGroup = null;
            try {
                logger.trace(method, "update head VersionGroup");
                newVersionGroup = systemCallHandler.updateHeadVersionGroup(systemVersionGroup.getRefUuid());
            } catch (Exception ex) {
                logger.catching(method, ex);
                newVersionGroup = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
                logger.trace(method, "re-created new ref:VersionGroup");
            }
            if (newVersionGroup == null) {
                logger.error(method, "failed to create new ref:VersionGroup");
                return null;
            }
            if (!newVersionGroup.equals(systemVersionGroup)) {
                logger.trace(method, "replace " + systemVersionGroup + " with " + newVersionGroup);
                this.systemVersionGroup = newVersionGroup;
                this.systemVersionGroup.createUnionModel();
            }
        }
        logger.trace_end(method);
        return this.systemVersionGroup;
    }
    
    @Lock(LockType.READ)
    public OntModel getCachedOntModel() {
        if (this.systemVersionGroup != null) {
            return this.systemVersionGroup.getCachedModelBase().getOntModel();
        }
        return null;
    }

    @Lock(LockType.READ)
    public OntModel getLatestOntModel() {
        VersionGroup vg = getLatest();
        return vg.getCachedModelBase().getOntModel();
    }
}
