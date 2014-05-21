/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean.persist;

import java.util.List;
import javax.persistence.Query;
import net.maxgigapop.mrs.bean.*;
import net.maxgigapop.mrs.bean.SystemInstance;
import static net.maxgigapop.mrs.bean.persist.PersistenceManager.createQuery;

/**
 *
 * @author xyang
 */

@SuppressWarnings("unchecked")
public class VersionGroupPersistenceManager extends PersistenceManager {
    public static VersionGroup findById(Long id) {
        return PersistenceManager.find(VersionGroup.class, id);
    }
    
    public static VersionGroup getHeadGroup() {
		try {
			Query q = createQuery(String.format("FROM %s WHERE id = (SELECT MAX(id) FROM %s)", VersionGroup.class.getSimpleName(), VersionGroup.class.getSimpleName()));
            return (VersionGroup)q.getSingleResult(); 
		} catch (Exception e) {
			return null;
		}
    }
    
    //@TODO:    public static VersionGroup getHeadGroupFresh()
    //@TODO:    public static VersionGroup refreshToHead(VersionGroup oldVG)

}
