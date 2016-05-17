/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

/*
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class VersionItemPersistenceManager extends PersistenceManager {

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
            if (e.getMessage().contains("No entity found")) {
                return null;
            }
            throw new EJBException(String.format("VersionItemPersistenceManager::findByReferenceId raised exception: %s", e.getMessage()));
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
                return null;
            }
            throw new EJBException(String.format("VersionItemPersistenceManager::getHeadByDriverInstance raised exception: %s", e.getMessage()));
        }
    }

    public static VersionItem getHeadByVersionItem(VersionItem vi) {
        if (vi == null) {
            throw new EJBException(String.format("VersionItemPersistenceManager::refreshToHead encounters null VG"));
        }
        DriverInstance di = vi.getDriverInstance();
        if (di == null) {
            throw new EJBException(String.format("VersionItemPersistenceManager::refreshToHead has null dirverInstance in %s", vi));
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
            throw new EJBException(String.format("VersionItemPersistenceManager::getHeadByDriverInstance raised exception: %s", e.getMessage()));
        }
    }
    
    public static void cleanupAllBefore(Date before) {
        try {
            Query q = createQuery(String.format("FROM %s vi WHERE vi.modelRef.creationTime < :before AND "
                    + "NOT EXISTS (FROM %s as delta WHERE delta.referenceVersionItem = vi)", 
                    VersionItem.class.getSimpleName(), DriverSystemDelta.class.getSimpleName()));
            q.setParameter("before", before, TemporalType.TIMESTAMP);
            List<VersionItem> listVI = (List<VersionItem>) q.getResultList();
            if (listVI == null) {
                return;
            }
            Iterator<VersionItem> it = listVI.iterator();
            while (it.hasNext()) {
                VersionItem vi = it.next();
                if (vi.getVersionGroups() == null || vi.getVersionGroups().isEmpty()) {
                    VersionItemPersistenceManager.delete(vi);
                }
            }
        } catch (Exception e) {
            throw new EJBException(String.format("VersionItemPersistenceManager::getHeadByDriverInstance raised exception: %s", e.getMessage()));
        }
    }
}
