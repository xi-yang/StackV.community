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

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import javax.ejb.EJBException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.VersionItem;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;
import net.maxgigapop.mrs.common.StackLogger;

/*
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class VersionItemPersistenceManager extends PersistenceManager {
    
    private static final StackLogger logger = new StackLogger(VersionItemPersistenceManager.class.getName(), "VersionItemPersistenceManager");

    public static VersionItem findById(Long id) {
        return PersistenceManager.find(VersionItem.class, id);
    }

    public static VersionItem findByReferenceUUID(String uuid) {
        try {
            Query q = createQuery(String.format("FROM %s WHERE referenceUUID='%s'", VersionItem.class.getSimpleName(), uuid));
            List<VersionItem> listVI = (List<VersionItem>) q.getResultList();
            if (listVI == null || listVI.isEmpty()) {
                return null;
            }
            return listVI.get(0);
        } catch (Exception e) {
            logger.targetid(uuid);
            if (e.getMessage().contains("No entity found")) {
                logger.warning("findByReferenceUUID", "target:VersionItem - no entity found");
                return null;
            }
            throw logger.error_throwing("findByReferenceUUID", e.getMessage());
        }
    }

    public static VersionItem getHeadByDriverInstance(DriverInstance di) {
        try {
            Query q = createQuery(String.format("FROM %s vi WHERE vi.id=(SELECT MAX(rec.id) FROM %s rec WHERE rec.driverInstance=%d)",
                    VersionItem.class.getSimpleName(), VersionItem.class.getSimpleName(), di.getId()));
            Object viObj = q.getSingleResult();
            return (VersionItem) viObj;
        } catch (Exception e) {
            if (e.getMessage().contains("No entity found")) {
                logger.warning("getHeadByDriverInstance", "target:VersionItem - no entity found");
                return null;
            }
            throw logger.error_throwing("getHeadByDriverInstance", e.getMessage());
        }
    }

    public static VersionItem getHeadByVersionItem(VersionItem vi) {
        if (vi == null) {
            throw logger.error_throwing("getHeadByVersionItem", "input vi is null");
        }
        DriverInstance di = vi.getDriverInstance();
        if (di == null) {
            logger.targetid(vi.getReferenceUUID());
            throw logger.error_throwing("getHeadByVersionItem", "ref:VersionItem has null driverInstance");
        }
        return getHeadByDriverInstance(di);
    }

    public static void deleteByDriverInstance(DriverInstance di) {
        try {
            Query q = createQuery(String.format("FROM %s WHERE driverInstanceId=%d",
                    VersionItem.class.getSimpleName(), di.getId()));
            List<VersionItem> listVI = (List<VersionItem>) q.getResultList();
            Iterator<VersionItem> it = listVI.iterator();
            while (it.hasNext()) {
                VersionItem vi = it.next();
                VersionItemPersistenceManager.delete(vi);
            }
        } catch (Exception e) {
            logger.targetid(di.getId().toString());
            throw logger.error_throwing("deleteByDriverInstance", "target:DriverInstance "+e.getMessage());
        }
    }

    public static void cleanupAllBefore(Date before) {
        logger.start("cleanupAllBefore");
        Query q = createQuery(String.format("SELECT vi.id FROM %s vi WHERE vi.modelRef.creationTime < :before AND "
                + "NOT EXISTS (FROM %s as delta WHERE delta.referenceVersionItem = vi)",
                VersionItem.class.getSimpleName(), DriverSystemDelta.class.getSimpleName()));
        q.setParameter("before", before, TemporalType.TIMESTAMP);
        List listVID = q.getResultList();
        if (listVID == null) {
            return;
        }
        Iterator<Long> it = listVID.iterator();
        Integer count = 0;
        while (it.hasNext()) {
            Long vid = it.next();
            VersionItem vi = VersionItemPersistenceManager.findById(vid);
            if (vi.getVersionGroups() == null || vi.getVersionGroups().isEmpty()) {
                try {
                    VersionItemPersistenceManager.delete(vi);
                    count++;
                    logger.trace("cleanupAllBefore", vi + " deleted");
                } catch (Exception e) {
                    logger.targetid(vi.getReferenceUUID());
                    logger.warning("cleanupAllBefore", "target:VersionGroup deleting exception raised and ignored: "+e);
                }
            }
        }
        logger.message("cleanupAllBefore", count + " version items have been deleted");
        logger.end("cleanupAllBefore");
    }
}
