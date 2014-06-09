/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean.persist;

import java.util.List;
import net.maxgigapop.mrs.bean.DeltaBase;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;

/**
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class DeltaPersistenceManager extends PersistenceManager {
 
    public static DeltaBase findById(Long id) {
        return PersistenceManager.find(DeltaBase.class, id);
    }
    public static List<DeltaBase> retrieveAll() {
        return createQuery("FROM " + DeltaBase.class.getSimpleName()).getResultList();
    }
}
