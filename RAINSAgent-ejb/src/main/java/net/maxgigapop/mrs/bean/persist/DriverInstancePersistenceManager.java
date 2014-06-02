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
import net.maxgigapop.mrs.bean.DriverInstance;

/**
 *
 * @author xyang
 */

@SuppressWarnings("unchecked")
public class DriverInstancePersistenceManager extends PersistenceManager{
    private static Map<String, DriverInstance> driverInstanceByTopologyMap = null;

    public static Map<String, DriverInstance> getDriverInstanceByTopologyMap() {
        return driverInstanceByTopologyMap;
    }

    public static void setDriverInstanceByTopologyMap(Map<String, DriverInstance> driverInstanceByTopologyMap) {
        DriverInstancePersistenceManager.driverInstanceByTopologyMap = driverInstanceByTopologyMap;
    }
    
    public static void refreshAll() {
        driverInstanceByTopologyMap = new HashMap<String, DriverInstance>();
        List<DriverInstance> listDriverInstances = createQuery("FROM " + DriverInstance.class.getSimpleName()).getResultList();
        for (DriverInstance di: listDriverInstances) {
            if (di.getTopologyUri() == null || di.getTopologyUri().isEmpty()) {
                throw new EJBException(String.format("%s has null/empty topologyUri", di));
            }
            driverInstanceByTopologyMap.put(di.getTopologyUri(), di);
        }
    }    

    public static DriverInstance findById(Long id) {
        return PersistenceManager.find(DriverInstance.class, id);
    }    

    public static DriverInstance findByTopologyUri(String uri) {
        if (driverInstanceByTopologyMap.containsKey(uri)) {
            return driverInstanceByTopologyMap.get(uri);
        }
        Query q = PersistenceManager.createQuery(String.format("FROM SystemInstance WHERE topologyUri='%s'", uri));
        DriverInstance di = (DriverInstance)q.getSingleResult();
        if (driverInstanceByTopologyMap == null) {
            driverInstanceByTopologyMap = new HashMap<String, DriverInstance>();
        }
        driverInstanceByTopologyMap.put(uri, di);
        return di;
    }

    public static void delete(DriverInstance di) {
        PersistenceManager.delete(di);
        if (di.getTopologyUri() != null && !di.getTopologyUri().isEmpty() 
                && driverInstanceByTopologyMap != null
                && driverInstanceByTopologyMap.containsKey(di.getTopologyUri())) {
            driverInstanceByTopologyMap.remove(di.getTopologyUri());
        }
    }
    
    public static void save(DriverInstance di) {
        PersistenceManager.save(di);
        if (driverInstanceByTopologyMap == null) {
            driverInstanceByTopologyMap = new HashMap<String, DriverInstance>();
        }
        if (di.getTopologyUri() != null && !di.getTopologyUri().isEmpty() 
                && driverInstanceByTopologyMap.containsKey(di.getTopologyUri())) {
            driverInstanceByTopologyMap.put(di.getTopologyUri(), di);
        }
    }
}
