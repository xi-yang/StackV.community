/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean.persist;

import java.util.List;
import java.util.Map;
import javax.ejb.EJBException;
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

    public static VersionGroup findByReferenceId(String uuid) {
        try {
            Query q = createQuery(String.format("FROM %s WHERE refUuid = '%s'", VersionGroup.class.getSimpleName(), uuid));
            return (VersionGroup) q.getSingleResult();
        } catch (Exception e) {
            throw new EJBException(String.format("VersionGroupPersistenceManager::findByReferenceId raised exception: %s", e.getMessage()));
        }
    }

    public static VersionGroup refreshToHead(VersionGroup vg) {
        if (vg == null) {
            throw new EJBException(String.format("VersionGroupPersistenceManager::refreshToHead encounters null VG"));
        }
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null) {
            DriverInstancePersistenceManager.refreshAll();
            ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        }
        if (ditMap.isEmpty()) {
            throw new EJBException(String.format("VersionGroupPersistenceManager::refreshToHead canont find driverInstance in the system"));
        }
        VersionGroup vgNew = null;
        for (VersionItem vi : vg.getVersionItems()) {
            DriverInstance di = vi.getDriverInstance();
            VersionItem newVi = di.getHeadVersionItem();
            if (di != null && !newVi.equals(vi)) {
                if (vgNew == null) {
                    vgNew = new VersionGroup();
                }
                vgNew.addVersionItem(newVi);
                //newVi.addVersionGroup(vgNew);
            }
        }
        if (vgNew != null) {
            vgNew.setRefUuid(vg.getRefUuid());
            VersionGroupPersistenceManager.save(vgNew);
            VersionGroupPersistenceManager.delete(vg);
            return vgNew;
        }
        return vg;
    }
}
