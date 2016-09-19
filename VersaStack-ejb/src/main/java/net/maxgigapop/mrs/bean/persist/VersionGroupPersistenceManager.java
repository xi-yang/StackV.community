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
import javax.ejb.EJBException;
import javax.persistence.Query;
import net.maxgigapop.mrs.bean.*;
import net.maxgigapop.mrs.bean.SystemInstance;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;

/**
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class VersionGroupPersistenceManager extends PersistenceManager {

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
        if (ditMap.isEmpty()) {
            throw new EJBException(String.format("VersionGroupPersistenceManager::refreshToHead canont find driverInstance in the system"));
        }
        VersionGroup newVG = new VersionGroup();
        newVG.setRefUuid(vg.getRefUuid());
        boolean needToUpdate = false;
        List<DriverInstance> listDI = new ArrayList<>();
        for (VersionItem vi : vg.getVersionItems()) {
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
                newVG.addVersionItem(newVi);
                if (!newVi.getVersionGroups().contains(vg)) {
                    newVi.addVersionGroup(vg);
                }
            }
        }
        for (DriverInstance di : ditMap.values()) {
            if (!listDI.contains(di)) {
                synchronized (di) {
                    VersionItem newVi = di.getHeadVersionItem();
                    if (newVi == null) {
                        throw new EJBException(String.format("refreshToHead encounters null head versionItem in %s", di));
                    }
                    newVi.addVersionGroup(vg);
                    newVG.addVersionItem(newVi);
                }
            }
        }
        if (!doUpdatePersist) {
            return newVG;
        }
        if (needToUpdate) {
            vg = findByReferenceId(vg.getRefUuid());
            vg.setVersionItems(newVG.getVersionItems());
            vg.setUpdateTime(new java.util.Date());
            VersionGroupPersistenceManager.save(vg);
        }
        return vg;
    }

    public static void cleanupAll() {
        try {
            // remove all VGs that has no dependency (by systemDelta)
            Query q = createQuery(String.format("FROM %s vg WHERE NOT EXISTS (FROM %s as delta WHERE delta.referenceVersionGroup = vg)", VersionGroup.class.getSimpleName(), SystemDelta.class.getSimpleName()));
            List<VersionGroup> listVG = (List<VersionGroup>) q.getResultList();
            if (listVG != null) {
                Iterator<VersionGroup> it = listVG.iterator();
                while (it.hasNext()) {
                    VersionGroup vg = it.next();
                    VersionGroupPersistenceManager.delete(vg);
                }
            }
            // remove all empty VGs
            q = createQuery(String.format("FROM %s", VersionGroup.class.getSimpleName()));
            listVG = (List<VersionGroup>) q.getResultList();
            if (listVG != null) {
                Iterator<VersionGroup> it = listVG.iterator();
                while (it.hasNext()) {
                    VersionGroup vg = it.next();
                    if (vg.getVersionItems() == null || vg.getVersionItems().isEmpty()) {
                        VersionGroupPersistenceManager.delete(vg);
                    }
                }
            }
        } catch (Exception e) {
            ;
        }
    }
    
    public static void cleanupAndUpdateAll() {
        try {
            // remove all VGs that has no dependency (by systemDelta)
            Query q = createQuery(String.format("FROM %s vg WHERE NOT EXISTS (FROM %s as delta WHERE delta.referenceVersionGroup = vg)", VersionGroup.class.getSimpleName(), SystemDelta.class.getSimpleName()));
            List<VersionGroup> listVG = (List<VersionGroup>) q.getResultList();
            if (listVG != null) {
                Iterator<VersionGroup> it = listVG.iterator();
                while (it.hasNext()) {
                    VersionGroup vg = it.next();
                    VersionGroupPersistenceManager.delete(vg);
                }
            }
            // remove all empty VGs and update the non-empty
            q = createQuery(String.format("FROM %s", VersionGroup.class.getSimpleName()));
            listVG = (List<VersionGroup>) q.getResultList();
            if (listVG == null) {
                return;
            }
            Iterator<VersionGroup> it = listVG.iterator();
            while (it.hasNext()) {
                VersionGroup vg = it.next();
                if (vg.getVersionItems() == null || vg.getVersionItems().isEmpty()) {
                    VersionGroupPersistenceManager.delete(vg);
                } else {
                    //@TODO: probe further: the update may create empty VGs
                    VersionGroupPersistenceManager.refreshToHead(vg, true);
                }
                
            }
        } catch (Exception e) {
            ;
        }
    }

}
