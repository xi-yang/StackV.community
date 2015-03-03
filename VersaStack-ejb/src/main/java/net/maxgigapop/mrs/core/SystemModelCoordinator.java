/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.core;

import static java.lang.Math.log;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import net.maxgigapop.mrs.bean.VersionGroup;
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
    public VersionGroup getLatestVersionGroupWithUnionModel() {
        if (this.systemVersionGroup == null) {
            this.systemVersionGroup = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
            if (this.systemVersionGroup != null) {
                //$$ handle exception?
                this.systemVersionGroup.createUnionModel();
                /*
                try {
                    Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.INFO, "\n>>>SystemModelCoordinator--SystemModel=\n" + ModelUtil.marshalOntModel(systemVersionGroup.getCachedModelBase().getOntModel()));
                } catch (Exception ex) {
                    Logger.getLogger(SystemModelCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                }
                */
            }
        } else {
            //$$ handle exception?
            VersionGroup newVersionGroup = systemCallHandler.updateHeadVersionGroup(systemVersionGroup.getRefUuid());
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
