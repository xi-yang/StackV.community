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
public class SystemInstancePersistenceManager extends PersistenceManager {

    private static Map<String, SystemInstance> systemInstanceByUuidMap = null;

    public static SystemInstance findById(Long id) {
        return PersistenceManager.find(SystemInstance.class, id);
    }

    public static SystemInstance findByReferenceUUID(String uuid) {
        if (systemInstanceByUuidMap != null && systemInstanceByUuidMap.containsKey(uuid)) {
            return systemInstanceByUuidMap.get(uuid);
        }
        try {
            Query q = createQuery(String.format("FROM %s WHERE referenceUUID='%s'", SystemInstance.class.getSimpleName(), uuid));
            List<SystemInstance> listSI = (List<SystemInstance>) q.getResultList();
            if (listSI == null || listSI.isEmpty()) {
                return null;
            }
            return listSI.get(0);
        } catch (Exception e) {
            if (e.getMessage().contains("No entity found")) {
                return null;
            }
            throw new EJBException(String.format("SystemInstancePersistenceManager::findByReferenceUUID raised exception: %s", e.getMessage()));
        }
    }

    public static SystemInstance findBySystemDelta(SystemDelta delta) {
        try {
            Query q = createQuery(String.format("FROM %s WHERE systemDelta=%d", SystemInstance.class.getSimpleName(), delta.getId()));
            List<SystemInstance> listSI = (List<SystemInstance>) q.getResultList();
            if (listSI == null || listSI.isEmpty()) {
                return null;
            }
            return listSI.get(0);
        } catch (Exception e) {
            if (e.getMessage().contains("No entity found")) {
                return null;
            }
            throw new EJBException(String.format("SystemInstancePersistenceManager::findBySystemDelta raised exception: %s", e.getMessage()));
        }
    }

    public static void save(SystemInstance si) {
        PersistenceManager.save(si);
        if (systemInstanceByUuidMap == null) {
            systemInstanceByUuidMap = new HashMap<String, SystemInstance>();
        }
        if (si.getReferenceUUID() != null && !si.getReferenceUUID().isEmpty()
                && !systemInstanceByUuidMap.containsKey(si.getReferenceUUID())) {
            systemInstanceByUuidMap.put(si.getReferenceUUID(), si);
        }
    }

    public static void delete(SystemInstance si) {
        PersistenceManager.delete(si);
        if (si.getReferenceUUID() != null && !si.getReferenceUUID().isEmpty()
                && systemInstanceByUuidMap != null
                && systemInstanceByUuidMap.containsKey(si.getReferenceUUID())) {
            systemInstanceByUuidMap.remove(si.getReferenceUUID());
        }
    }
}
