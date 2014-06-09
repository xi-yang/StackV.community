/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.system;

import java.util.List;
import java.util.Map;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;

/**
 *
 * @author xyang
 */
@Stateless
@LocalBean
public class HandleSystemCall {    
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
    
    public void plugDriverInstance(Map<String, String> properties) {
        if (!properties.containsKey("topologyUri") || !properties.containsKey("driverEjbPath")) {
           throw new EJBException(String.format("plugDriverInstance must provide both topologyUri and driverEjbPath properties"));
        }
        if (DriverInstancePersistenceManager.findByTopologyUri(properties.get("topologyUri")) != null) {
           throw new EJBException(String.format("A driverInstance has existed for topologyUri=%s", properties.get("topologyUri")));
        }
        DriverInstance newDI = new DriverInstance();
        newDI.setProperties(properties);
        newDI.setTopologyUri(properties.get("topologyUri"));
        newDI.setDriverEjbPath(properties.get("driverEjbPath"));
        DriverInstancePersistenceManager.save(newDI);
    }
    
    public void unplugDriverInstance(String topoUri) {
        DriverInstance di = DriverInstancePersistenceManager.findByTopologyUri(topoUri);
        if (di == null) {
           throw new EJBException(String.format("unplugDriverInstance cannot find the driverInstance for topologyUri=%s", topoUri));
        }
        // remove all related versionItems
        VersionItemPersistenceManager.deleteByDriverInstance(di);
        DriverInstancePersistenceManager.delete(di);
    }

}
