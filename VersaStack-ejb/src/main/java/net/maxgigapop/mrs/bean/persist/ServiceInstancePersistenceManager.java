/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean.persist;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJBException;
import javax.persistence.Query;
import net.maxgigapop.mrs.bean.*;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;
/**
 *
 * @author xyang
 */

@SuppressWarnings("unchecked")
public class ServiceInstancePersistenceManager extends PersistenceManager {
    private static Map<String, ServiceInstance> serviceInstanceByUuidMap = null;

    public static ServiceInstance findById(Long id) {
        return PersistenceManager.find(ServiceInstance.class, id);
    }
    
    public static ServiceInstance findByReferenceUUID(String uuid) {
        if (serviceInstanceByUuidMap != null && serviceInstanceByUuidMap.containsKey(uuid)) {
            return serviceInstanceByUuidMap.get(uuid);
        }
		try {
			Query q = createQuery(String.format("FROM %s WHERE referenceUUID='%s'", ServiceInstance.class.getSimpleName(), uuid));
            List<ServiceInstance> listSI = (List<ServiceInstance>)q.getResultList(); 
            if (listSI == null || listSI.isEmpty()) {
                return null;
            }
            return listSI.get(0);
		} catch (Exception e) {
            if (e.getMessage().contains("No entity found"))
                return null;
            throw new EJBException(String.format("ServiceInstancePersistenceManager::findByReferenceUUID raised exception: %s", e.getMessage()));
		}
    }
    
    public static void save(ServiceInstance si) {
        PersistenceManager.save(si);
        if (serviceInstanceByUuidMap == null) {
            serviceInstanceByUuidMap = new HashMap<String, ServiceInstance>();
        }
        if (si.getReferenceUUID() != null && !si.getReferenceUUID().isEmpty() 
                && !serviceInstanceByUuidMap.containsKey(si.getReferenceUUID())) {
            serviceInstanceByUuidMap.put(si.getReferenceUUID(), si);
        }
    }
    
    public static void delete(ServiceInstance si) {
        PersistenceManager.delete(si);
        if (si.getReferenceUUID() != null && !si.getReferenceUUID().isEmpty() 
                && serviceInstanceByUuidMap != null
                && serviceInstanceByUuidMap.containsKey(si.getReferenceUUID())) {
            serviceInstanceByUuidMap.remove(si.getReferenceUUID());
        }
    }
}
