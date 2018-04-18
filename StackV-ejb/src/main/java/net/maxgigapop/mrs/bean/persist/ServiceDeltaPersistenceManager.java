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

import java.util.List;
import javax.ejb.EJBException;
import javax.persistence.Query;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ServiceInstance;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;
import net.maxgigapop.mrs.common.StackLogger;

/**
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class ServiceDeltaPersistenceManager {

    private static final StackLogger logger = new StackLogger(ServiceDeltaPersistenceManager.class.getName(), "ServiceDeltaPersistenceManager");

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
            logger.targetid(uuid);
            if (e.getMessage().contains("No entity found")) {
                logger.warning("findByReferenceUUID", "target:ServiceDelta - no entity found");
                return null;
            }
            throw logger.error_throwing("findByReferenceUUID", e.getMessage());
        }
    }

}
