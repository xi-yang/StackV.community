/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2013

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

package net.maxgigapop.mrs.bean.persist;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.persistence.Query;
import net.maxgigapop.mrs.bean.*;
import net.maxgigapop.mrs.bean.SystemInstance;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.service.HandleServiceCall;

/**
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class VersionGroupPersistenceManager extends PersistenceManager {
    private static final StackLogger logger = new StackLogger(VersionGroupPersistenceManager.class.getName(), "VersionGroupPersistenceManager");

    public static VersionGroup findById(Long id) {
        return PersistenceManager.find(VersionGroup.class, id);
    }

    public static VersionGroup findByReferenceId(String uuid) {
        try {
            Query q = createQuery(String.format("FROM %s WHERE refUuid = '%s'", VersionGroup.class.getSimpleName(), uuid));
            return (VersionGroup) q.getSingleResult();
        } catch (Exception e) {
            throw new EJBException(String.format("VersionGroupPersistenceManager::findByReferenceId raised exception: %s", e.getMessage()));
        }
    }

    public static VersionGroup refreshToHead(VersionGroup vg, boolean doUpdatePersist) {
        if (vg == null) {
            throw new EJBException(String.format("VersionGroupPersistenceManager::refreshToHead encounters null VG"));
        }
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null) {
            DriverInstancePersistenceManager.refreshAll();
            ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        }
        logger.targetid(vg.getRefUuid());
        if (ditMap.isEmpty()) {
            throw logger.error_throwing("refreshToHead", "target:VersionGroup canont find driverInstance in the system");
        }
        vg = findByReferenceId(vg.getRefUuid());
        List<VersionItem> listVI = new ArrayList();
        listVI.addAll(vg.getVersionItems());
        vg.getVersionItems().clear();
        boolean needToUpdate = false;
        List<DriverInstance> listDI = new ArrayList<>();
        for (VersionItem vi : listVI) {
            DriverInstance di = vi.getDriverInstance();
            if (di != null) {
                if (listDI.contains(di)) {
                    continue;
                }
                listDI.add(di);
                VersionItem newVi = di.getHeadVersionItem();
                if (!newVi.equals(vi)) {
                    needToUpdate = true;
                }
                vg.addVersionItem(newVi);
                if (!newVi.getVersionGroups().contains(vg)) {
                    newVi.addVersionGroup(vg);
                }
            }
        }
        for (DriverInstance di : ditMap.values()) {
            if (!listDI.contains(di)) {
                    VersionItem newVi = di.getHeadVersionItem();
                    if (newVi == null) {
                        logger.targetid(di.getId().toString());
                        throw logger.error_throwing("refreshToHead", "target:VersionGroup encounters null head versionItem in "+di);
                    }
                    if (!newVi.getVersionGroups().contains(vg)) {
                        newVi.addVersionGroup(vg);
                    }
                    if (!vg.getVersionItems().contains(newVi)) {
                        vg.addVersionItem(newVi);
                    }
                needToUpdate = true;
            }
        }
        if (needToUpdate) {
            logger.message("refreshToHead", "ref:VersionGroup updated");
            vg.setUpdateTime(new java.util.Date());
        }
        if (!doUpdatePersist) {
            return vg;
        }
        if (needToUpdate) {
            VersionGroupPersistenceManager.save(vg);
            logger.message("refreshToHead", "ref:VersionGroup persisted");
        }
        return vg;
    }

    public static void cleanupAll(VersionGroup butVG) {
        logger.start("cleanupAll");
        Integer count = 0;
        try {
            // remove all VGs that has no dependency (by systemDelta)
            Query q = createQuery(String.format("SELECT vg.id FROM %s vg WHERE NOT EXISTS (FROM %s as delta WHERE delta.referenceVersionGroup = vg)", VersionGroup.class.getSimpleName(), SystemDelta.class.getSimpleName()));
            List<Long> listVG = (List<Long>) q.getResultList();
            if (listVG != null) {
                Iterator<Long> it = listVG.iterator();
                while (it.hasNext()) {
                    VersionGroup vg = findById(it.next());
                    if (butVG != null && vg.getRefUuid().equals(butVG.getRefUuid())) {
                        continue;
                    }
                    VersionGroupPersistenceManager.delete(vg);
                    count++;
                    logger.trace("cleanupAll", vg + " deleted.");
                }
            }
            // remove all empty VGs
            q = createQuery(String.format("SELECT id FROM %s", VersionGroup.class.getSimpleName()));
            listVG = (List<Long>) q.getResultList();
            if (listVG != null) {
                Iterator<Long> it = listVG.iterator();
                while (it.hasNext()) {
                    VersionGroup vg = findById(it.next());
                    if (vg.getVersionItems() == null || vg.getVersionItems().isEmpty()) {
                        VersionGroupPersistenceManager.delete(vg);
                        count++;
                        logger.trace("cleanupAll", vg + " deleted.");
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("cleanupAll",  "exception raised and ignored: " + e);
        }
        logger.message("cleanupAll", count + " version groups have been deleted");
        logger.end("cleanupAll");
    }
    
    public static void cleanupAndUpdateAll(VersionGroup butVG) {
        logger.start("cleanupAndUpdateAll");
        Integer count = 0;
        try {
            // remove all VGs that has no dependency (by systemDelta)
            Query q = createQuery(String.format("SELECT vg.id FROM %s vg WHERE NOT EXISTS (FROM %s as delta WHERE delta.referenceVersionGroup = vg)", VersionGroup.class.getSimpleName(), SystemDelta.class.getSimpleName()));
            List listVGID = q.getResultList();
            if (listVGID != null) {
                Iterator<Long> it = listVGID.iterator();
                while (it.hasNext()) {
                    Long vgid = it.next();
                    VersionGroup vg = VersionGroupPersistenceManager.findById(vgid);
                    if (butVG != null && vg.getRefUuid().equals(butVG.getRefUuid())) {
                        continue;
                    }
                    VersionGroupPersistenceManager.delete(vg);
                    count++;
                    logger.trace("cleanupAndUpdateAll", vg + " deleted (no longer used).");
                }
            }
            // remove all empty VGs and update the non-empty
            q = createQuery(String.format("SELECT id FROM %s", VersionGroup.class.getSimpleName()));
            listVGID = q.getResultList();
            if (listVGID == null) {
                return;
            }
            Iterator it = listVGID.iterator();
            while (it.hasNext()) {
                Long vgid = (Long) it.next();
                VersionGroup vg = VersionGroupPersistenceManager.findById(vgid);
                if (vg.getVersionItems() == null || vg.getVersionItems().isEmpty()) {
                    //@TODO: probe -> exception here (deletion may not be needed any way)
                    VersionGroupPersistenceManager.delete(vg);
                    count++;
                    logger.trace("cleanupAndUpdateAll", vg + " deleted (empty VI list).");
                } else {
                    //@TODO: probe further: the update may create empty VGs
                    VersionGroupPersistenceManager.refreshToHead(vg, true);
                    logger.trace("cleanupAndUpdateAll", vg + " refreshed.");
                }
                
            }
        } catch (Exception e) {
            logger.warning("cleanupAndUpdateAll", "exception raised and ignored: " + e);
        }
        logger.message("cleanupAndUpdateAll", count + " version groups have been deleted");
        logger.end("cleanupAndUpdateAll");
    }

}
