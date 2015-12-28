/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.bean.persist;

import java.util.List;
import javax.ejb.EJBException;
import javax.persistence.Query;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ServiceInstance;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;

/**
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class ServiceDeltaPersistenceManager {

    public static ServiceDelta findById(Long id) {
        return PersistenceManager.find(ServiceDelta.class, id);
    }

    public static ServiceDelta findByReferenceUUID(String uuid) {
        try {
            Query q = createQuery(String.format("FROM %s WHERE referenceUUID='%s'", ServiceDelta.class.getSimpleName(), uuid));
            List<ServiceDelta> listSD = (List<ServiceDelta>) q.getResultList();
            if (listSD == null || listSD.isEmpty()) {
                return null;
            }
            return listSD.get(0);
        } catch (Exception e) {
            if (e.getMessage().contains("No entity found")) {
                return null;
            }
            throw new EJBException(String.format("ServiceDeltaPersistenceManager::findByReferenceUUID raised exception: %s", e.getMessage()));
        }
    }
}
