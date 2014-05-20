/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean.persist;

import javax.persistence.Query;
import net.maxgigapop.mrs.bean.DriverInstance;

/**
 *
 * @author xyang
 */

@SuppressWarnings("unchecked")
public class DriverInstancePersistenceManager extends PersistenceManager{
    public static DriverInstance findById(Long id) {
        return PersistenceManager.find(DriverInstance.class, id);
    }    

    public static DriverInstance findByTopologyUri(String uri) {
        Query q = PersistenceManager.createQuery(String.format("FROM SystemInstance WHERE topologyUri='%s'", uri));
        return (DriverInstance)q.getSingleResult();
    }    
}
