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
public class DriverInstancePersistenceManager extends PersistenceManager {

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
        for (DriverInstance di : listDriverInstances) {
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
        if (driverInstanceByTopologyMap != null && driverInstanceByTopologyMap.containsKey(uri)) {
            return driverInstanceByTopologyMap.get(uri);
        }
        Query q = PersistenceManager.createQuery(String.format("FROM %s WHERE topologyUri='%s'", DriverInstance.class.getSimpleName(), uri));
        if (q.getResultList().isEmpty()) {
            return null;
        }
        DriverInstance di = (DriverInstance) q.getSingleResult();
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
                && !driverInstanceByTopologyMap.containsKey(di.getTopologyUri())) {
            driverInstanceByTopologyMap.put(di.getTopologyUri(), di);
        }
    }
}
