/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.session;

import java.util.List;
import java.util.Map;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;

/**
 *
 * @author xyang
 */
@Stateless
@LocalBean
public class HandleSystemPullCall {    
    public VersionGroup createHeadVersionGroup(Long refId) {
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null) {
            DriverInstancePersistenceManager.refreshAll();
            ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        }
        if (ditMap.isEmpty()) {
           throw new EJBException(String.format("createHeadVersionGroup canont find driverInstance in the system"));
        }
        VersionGroup vg = new VersionGroup();
        for (String topoUri: ditMap.keySet()) {
            DriverInstance di = ditMap.get(topoUri);
            VersionItem vi = di.getHeadVersionItem();
            if (vi == null) {
                throw new EJBException(String.format("createHeadVersionGroup encounters null head versionItem in %s", di));
            }
            vg.addVersionItem(vi);
        }
        vg.setReferenceId(refId);
        VersionGroupPersistenceManager.save(vg);
        return vg;
    }

    
    public VersionGroup createHeadVersionGroup(Long refId, List<String> topoURIs) {
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null) {
            DriverInstancePersistenceManager.refreshAll();
            ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        }
        if (ditMap.isEmpty()) {
           throw new EJBException(String.format("createHeadVersionGroup canont find driverInstance in the system"));
        }
        VersionGroup vg = new VersionGroup();
        for (String topoUri: topoURIs) {
            DriverInstance di = ditMap.get(topoUri);
            if (di == null) {
                throw new EJBException(String.format("createHeadVersionGroup canont find driverInstance with topologyURI=%s", topoUri));
            }
            VersionItem vi = di.getHeadVersionItem();
            if (vi == null) {
                throw new EJBException(String.format("createHeadVersionGroup encounters null head versionItem in %s", di));
            }
            vg.addVersionItem(vi);
        }
        vg.setReferenceId(refId);
        VersionGroupPersistenceManager.save(vg);
        return vg;
    }

    public VersionGroup updateHeadVersionGroup(Long refId) {
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refId);
        return VersionGroupPersistenceManager.refreshToHead(vg);        
    }
    
    public ModelBase retrieveVersionGroupModel(Long refId) {
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refId);
        if (vg == null) {
           throw new EJBException(String.format("retrieveVersionModel cannot find a VG with referenceId=%d", refId));
        }
        return vg.createUnionModel();        
    }
    
    //@TODO: add driver instance management methods (plug, unplug etc.)}
}
