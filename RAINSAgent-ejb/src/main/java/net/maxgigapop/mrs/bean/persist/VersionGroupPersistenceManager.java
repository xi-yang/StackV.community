/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean.persist;

import java.util.List;
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

    public static VersionGroup findByReferenceId(Long refId) {
		try {
			Query q = createQuery(String.format("FROM %s WHERE referenceId = %d", VersionGroup.class.getSimpleName(), refId));
            return (VersionGroup)q.getSingleResult(); 
		} catch (Exception e) {
			return null;
		}
    }    
    
    public static VersionGroup getHeadGroup() {
		try {
			Query q = createQuery(String.format("FROM %s WHERE id = (SELECT MAX(id) FROM %s)", VersionGroup.class.getSimpleName(), VersionGroup.class.getSimpleName()));
            return (VersionGroup)q.getSingleResult(); 
		} catch (Exception e) {
			return null;
		}
    }
    
     public static VersionGroup refreshToHead(VersionGroup aVG) {
        VersionGroup newVG = new VersionGroup();
        for (VersionItem vi : aVG.getVersionItems()) {
            if (vi.getDriverInstance() == null) {
                throw new EJBException(String.format("%s falied to refresh on %s which has null driverInstance", aVG, vi));
            }
            try {
                //@TBD: max(id) includes populated VIs while max(referenceId) only includes commited/pulled VIs 
                //Query q = createQuery(String.format("FROM %s WHERE id = (SELECT MAX(id) FROM %s WHERE driverInstanceId = %d)", VersionItem.class.getSimpleName(), VersionItem.class.getSimpleName(), vi.getDriverInstance().getId()));
                Query q = createQuery(String.format("FROM %s WHERE referenceId = (SELECT MAX(referenceId) FROM %s WHERE driverInstanceId = %d)", VersionItem.class.getSimpleName(), VersionItem.class.getSimpleName(), vi.getDriverInstance().getId()));
                // refresh
                vi = (VersionItem) q.getSingleResult();
            } catch (Exception e) {
                throw new EJBException(String.format("%s falied to refresh on %s due to %s", aVG, vi, e.getMessage()));
            }
            newVG.getVersionItems().add(vi);
        }
        return newVG;
    }
}
