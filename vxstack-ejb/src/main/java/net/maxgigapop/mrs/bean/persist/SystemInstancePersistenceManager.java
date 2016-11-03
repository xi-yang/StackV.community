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
