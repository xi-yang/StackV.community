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
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;
/**
 *
 * @author xyang
 */

@SuppressWarnings("unchecked")
public class SystemInstancePersistenceManager extends PersistenceManager {
    public static SystemInstance findById(Long id) {
        return PersistenceManager.find(SystemInstance.class, id);
    }
    
    public static SystemInstance findByReferenceUUID(String uuid) {
		try {
			Query q = createQuery(String.format("FROM %s WHERE referenceUUID='%s'", SystemInstance.class.getSimpleName(), uuid));
            List<SystemInstance> listSI = (List<SystemInstance>)q.getResultList(); 
            if (listSI == null || listSI.isEmpty()) {
                return null;
            }
            return listSI.get(0);
		} catch (Exception e) {
            if (e.getMessage().contains("No entity found"))
                return null;
            throw new EJBException(String.format("SystemInstancePersistenceManager::findByReferenceUUID raised exception: %s", e.getMessage()));
		}
    }
}
