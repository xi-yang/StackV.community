/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean.persist;

import java.util.Map;
import java.util.List;
import javax.ejb.EJBException;
import javax.persistence.Query;
import net.maxgigapop.mrs.bean.DriverInstance;
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
			Query q = createQuery(String.format("FROM %s WHERE referenceUUID = %d", VersionItem.class.getSimpleName(), uuid));
            return (VersionItem)q.getSingleResult(); 
		} catch (Exception e) {
            throw new EJBException(String.format("VersionItemPersistenceManager::findByReferenceId raised exception: %s", e.getMessage()));
		}
    }    
    
    public static VersionItem getHeadByDriverInstance(DriverInstance di) {
		try {
			Query q = createQuery(String.format("FROM %s vi WHERE vi.id=(SELECT MAX(rec.id) FROM %s rec WHERE rec.driverInstance=%d)", 
                    VersionItem.class.getSimpleName(), VersionItem.class.getSimpleName(), di.getId()));
            Object viObj = q.getSingleResult();
            return (VersionItem)viObj;
		} catch (Exception e) {
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
			Query q = createQuery(String.format("DELETE FROM %s WHERE driverInstanceId=%d", 
                    VersionItem.class.getSimpleName(), di.getId()));
		} catch (Exception e) {
            throw new EJBException(String.format("VersionItemPersistenceManager::getHeadByDriverInstance raised exception: %s", e.getMessage()));
		}
    }
}
