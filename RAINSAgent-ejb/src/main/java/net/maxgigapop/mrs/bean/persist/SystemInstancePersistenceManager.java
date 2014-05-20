/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean.persist;
import java.util.List;
import net.maxgigapop.mrs.bean.*;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;
/**
 *
 * @author xyang
 */

@SuppressWarnings("unchecked")
public class SystemInstancePersistenceManager extends PersistenceManager {
    public static SystemInstance findById(Long id) {
        return PersistenceManager.find(SystemInstance.class, id);
    }
}
