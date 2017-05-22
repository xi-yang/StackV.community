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
import net.maxgigapop.mrs.bean.*;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;
import net.maxgigapop.mrs.common.StackLogger;

/**
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class ServiceInstancePersistenceManager extends PersistenceManager {
    
    private static final StackLogger logger = new StackLogger(ServiceInstancePersistenceManager.class.getName(), "ServiceInstancePersistenceManager");

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
            List<ServiceInstance> listSI = (List<ServiceInstance>) q.getResultList();
            if (listSI == null || listSI.isEmpty()) {
                return null;
            }
            return listSI.get(0);
        } catch (Exception e) {
            logger.targetid(uuid);
            if (e.getMessage().contains("No entity found")) {
                logger.warning("findByReferenceUUID", "target:ServiceInstance - no entity found");
                return null;
            }
            throw logger.error_throwing("findByReferenceUUID", e.getMessage());
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
