/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.bean.persist;

import java.util.List;
import net.maxgigapop.mrs.bean.ModelBase;

/**
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class ModelPersistenceManager extends PersistenceManager {

    public static ModelBase findById(Long id) {
        return PersistenceManager.find(ModelBase.class, id);
    }

    public static List<ModelBase> retrieveAll() {
        return createQuery("FROM " + ModelBase.class.getSimpleName()).getResultList();
    }
}
