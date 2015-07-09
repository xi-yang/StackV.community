/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.core;

import static java.lang.Math.log;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.service.compute.MCE_MPVlanConnection;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author xyang
 */

@Singleton
@LocalBean
@Startup
@AccessTimeout(value=10000) // 10 seconds
public class SystemModelCoordinator {
    @EJB
    HandleSystemCall systemCallHandler;
            
    // current VG with cached union ModelBase
    VersionGroup systemVersionGroup = null;

    public VersionGroup getSystemVersionGroup() {
        return systemVersionGroup;
    }
    
    @Lock(LockType.WRITE)
    @Schedule(minute = "*", hour = "*", persistent = false)
    public void autoUpdate() {
        //check driverInstances 
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null || ditMap.isEmpty()) {
            return;
        }
        for (DriverInstance di: ditMap.values()) {
            synchronized (di) {
                if (di.getHeadVersionItem() == null)
                    return;
            }
        }
        if (this.systemVersionGroup == null) {
            this.systemVersionGroup = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
            if (this.systemVersionGroup != null) {
                this.systemVersionGroup.createUnionModel();
            }
        } else {
            VersionGroup newVersionGroup = newVersionGroup = systemCallHandler.updateHeadVersionGroup(systemVersionGroup.getRefUuid());
            if (newVersionGroup != null && !newVersionGroup.equals(systemVersionGroup)) {
                this.systemVersionGroup = newVersionGroup;
                this.systemVersionGroup.createUnionModel();
            }
        }
    }
    
    @Lock(LockType.WRITE)
    public VersionGroup getLatest() {
        if (this.systemVersionGroup == null) {
            this.systemVersionGroup = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
            if (this.systemVersionGroup != null) {
                //$$ handle exception?
                this.systemVersionGroup.createUnionModel();
            }
        } else {
            //$$ handle exception?
            VersionGroup newVersionGroup = null;
            try {
                newVersionGroup = systemCallHandler.updateHeadVersionGroup(systemVersionGroup.getRefUuid());
            } catch (Exception ex) {
                newVersionGroup = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
            }
            if (newVersionGroup == null) {
                return null;
            }
            if (!newVersionGroup.equals(systemVersionGroup)) {
                this.systemVersionGroup = newVersionGroup;
                this.systemVersionGroup.createUnionModel();
            }
        }
        return this.systemVersionGroup;
    }
}
