/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

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

package net.maxgigapop.mrs.system;

import com.hp.hpl.jena.ontology.OntModel;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.bean.SystemInstance;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.SystemInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.core.SystemModelCoordinator;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;
import net.maxgigapop.mrs.service.HandleServiceCall;

/**
 *
 * @author xyang
 */
@Stateless
@LocalBean
public class HandleSystemCall {
    
    private static final StackLogger logger = new StackLogger(HandleSystemCall.class.getName(), "SystemIntegrationAPI");

    public VersionGroup createHeadVersionGroup(String refUuid) {
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null) {
            DriverInstancePersistenceManager.refreshAll();
            ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        }
        if (ditMap.isEmpty()) {
            throw new EJBException(String.format("createHeadVersionGroup canont find driverInstance in the system"));
        }
        VersionGroup vg = new VersionGroup();
        vg.setRefUuid(refUuid);
        for (String topoUri : ditMap.keySet()) {
            DriverInstance di = ditMap.get(topoUri);
            synchronized (di) {
                VersionItem vi = di.getHeadVersionItem();
                if (vi == null) {
                    throw new EJBException(String.format("createHeadVersionGroup encounters null head versionItem in %s", di));
                }
                //$$ TODO: remove duplicate references
                if (vi.getVersionGroups() == null || !vi.getVersionGroups().contains(vg)) {
                    vi.addVersionGroup(vg);
                }
                if (vg.getVersionItems() == null || !vg.getVersionItems().contains(vi)) {
                    vg.addVersionItem(vi);
                }
            }
        }
        VersionGroupPersistenceManager.save(vg);
        return vg;
    }

    public VersionGroup createHeadVersionGroup(String refUuid, List<String> topoURIs) {
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null) {
            DriverInstancePersistenceManager.refreshAll();
            ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        }
        if (ditMap.isEmpty()) {
            throw new EJBException(String.format("createHeadVersionGroup canont find driverInstance in the system"));
        }
        VersionGroup vg = new VersionGroup();
        vg.setRefUuid(refUuid);
        for (String topoUri : topoURIs) {
            DriverInstance di = ditMap.get(topoUri);
            if (di == null) {
                throw new EJBException(String.format("createHeadVersionGroup canont find driverInstance with topologyURI=%s", topoUri));
            }
            VersionItem vi = di.getHeadVersionItem();
            if (vi == null) {
                throw new EJBException(String.format("createHeadVersionGroup encounters null head versionItem in %s", di));
            }
            //$$ TODO: remove duplicate references
            if (vi.getVersionGroups() == null || !vi.getVersionGroups().contains(vg)) {
                vi.addVersionGroup(vg);
            }
            if (vg.getVersionItems() == null || !vg.getVersionItems().contains(vi)) {
                vg.addVersionItem(vi);
            }
        }
        VersionGroupPersistenceManager.save(vg);
        return vg;
    }

    public VersionGroup updateHeadVersionGroup(String refUuid) {
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUuid);
        return VersionGroupPersistenceManager.refreshToHead(vg, true);
    }

    public ModelBase retrieveVersionGroupModel(String refUuid) {
        if (refUuid.equals("default")) {
            try {
                Context ejbCxt = new InitialContext();
                SystemModelCoordinator systemModelCoordinator = (SystemModelCoordinator) ejbCxt.lookup("java:module/SystemModelCoordinator");
                VersionGroup vg = systemModelCoordinator.getSystemVersionGroup();
                return vg.getCachedModelBase();
            } catch (Exception ex) {
                throw new EJBException(this.getClass().getName() + ".retrieveVersionGroupModel('default') failed to lookup systemModelCoordinator", ex);
            }
        } else {
            VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUuid);
            if (vg == null) {
                throw new EJBException(String.format("retrieveVersionModel cannot find a VG with refUuid=%s", refUuid));
            }
            return vg.createUnionModel();
        }
    }

    public OntModel queryModelView(String refUuid, List<ModelUtil.ModelViewFilter> mvfs) {
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUuid);
        if (vg == null) {
            throw new EJBException(String.format("queryModelView cannot find a VG with refUuid=%s", refUuid));
        }
        ModelBase vgModel = vg.createUnionModel();
        OntModel resultModel = null;
        for (ModelUtil.ModelViewFilter mvf : mvfs) {
            if (!mvf.isInclusive()) {
                continue;
            }
            try {
                OntModel filteredModel = ModelUtil.queryViewFilter(vgModel.getOntModel(), mvf);
                if (resultModel == null) {
                    resultModel = filteredModel;
                } else {
                    resultModel.add(filteredModel);
                }
            } catch (Exception ex) {
                throw new EJBException(String.format("queryModelView cannot queryViewFilter for VG=%s", refUuid), ex);
            }
        }
        if (resultModel == null) {
            throw new EJBException(String.format("queryModelView has no inclusive filters for VG=%s", refUuid));
        }
        for (ModelUtil.ModelViewFilter mvf : mvfs) {
            if (mvf.isInclusive()) {
                continue;
            }
            try {
                OntModel filteredModel = ModelUtil.queryViewFilter(vgModel.getOntModel(), mvf);
                resultModel.remove(filteredModel);
            } catch (Exception ex) {
                throw new EJBException(String.format("queryModelView cannot queryViewFilter for VG=%s", refUuid), ex);
            }
        }
        return resultModel;
    }

    public SystemInstance createInstance() {
        SystemInstance systemInstance = new SystemInstance();
        systemInstance.setReferenceUUID(UUID.randomUUID().toString());
        SystemInstancePersistenceManager.save(systemInstance);
        return systemInstance;
    }

    public void terminateInstance(String refUUID) {
        SystemInstance systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(refUUID);
        systemInstance = SystemInstancePersistenceManager.findById(systemInstance.getId());
        if (systemInstance == null) {
            throw new EJBException(String.format("terminateInstance cannot find the SystemInstance with referenceUUID=%s", refUUID));
        }
        if (systemInstance.getSystemDelta() != null) {
            DeltaPersistenceManager.delete(systemInstance.getSystemDelta());
            if (systemInstance.getSystemDelta().getDriverSystemDeltas() != null) {
                for (Iterator<DriverSystemDelta> it = systemInstance.getSystemDelta().getDriverSystemDeltas().iterator(); it.hasNext();) {
                    DriverSystemDelta dsd = it.next();
                    //DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(dsd.getDriverInstance().getTopologyUri());
                    DriverInstance driverInstance = dsd.getDriverInstance();
                    driverInstance.getDriverSystemDeltas().remove(dsd);
                    DeltaPersistenceManager.delete(dsd);
                }
            }
        }
        if (systemInstance.getSystemDelta() != null) {
            DeltaPersistenceManager.delete(systemInstance.getSystemDelta());
        }
        SystemInstancePersistenceManager.delete(systemInstance);
    }

    // useCachedVG = true && useRefreshedVG = false : 'normal' or 'through' mode 
    // useCachedVG = false: 'forward' mode  |  useRefreshedVG = true: 'forced' mode
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(SystemInstance systemInstance, SystemDelta sysDelta, boolean useCachedVG, boolean refreshForced) {
        String method = "propagateDelta";
        // refresh systemInstance into current persistence context
        if (systemInstance.getId() != 0) {
            systemInstance = SystemInstancePersistenceManager.findById(systemInstance.getId());
        }
        logger.refuuid(systemInstance.getReferenceUUID());
        if (systemInstance.getSystemDelta() != null
                && systemInstance.getSystemDelta().getDriverSystemDeltas() != null
                && !systemInstance.getSystemDelta().getDriverSystemDeltas().isEmpty()) {
            throw new EJBException(String.format("Trying to propagateDelta for %s that has delta already progagated.", systemInstance));
        }
        if (sysDelta.getId() != null && !sysDelta.getId().isEmpty()) {
            sysDelta = (SystemDelta) DeltaPersistenceManager.findById(sysDelta.getId());
        }
        // Note 1: an initial VG (#1) must exist 
        VersionGroup referenceVG_cached = sysDelta.getReferenceVersionGroup();
        if (referenceVG_cached == null) {
            throw new EJBException(String.format("%s has no reference versionGroup to work with", systemInstance));
        }
        // refresh VG into current persistence context
        VersionGroup referenceVG = VersionGroupPersistenceManager.findByReferenceId(referenceVG_cached.getRefUuid());
        sysDelta.setReferenceVersionGroup(referenceVG);

        //EJBExeption may be thrown upon fault from subroutine of each step below
        //## Step 1. create reference model and target model 
        // referenceVG could have new VIs compared to referenceVG_cached. If referenceVG_cached == false, 
        // the reference model will use the latest VIs. The latest VIs can be from persistence if the VG
        // has been actively updated, or we can force refresh/update by setting useRefreshedVG = true.
        // When useCachedVG = false && refreshForced = true, delta will be "forced" and bypass merge check.
        VersionGroup headVG = VersionGroupPersistenceManager.refreshToHead(referenceVG, false);
        ModelBase referenceModel = (useCachedVG ? referenceVG_cached : (refreshForced ? headVG : referenceVG))
                .createUnionModel();
        OntModel referenceOntModel = referenceModel.getOntModel();
        OntModel targetOntModel = referenceModel.dryrunDelta(sysDelta);

        //## Step 2. verify model change if refreshForced = false.
        // Under 'forced', delta apply to refreshed headVG. No need to compare to referenceVG as referenceModel is from headVG.
        // Under other conditions, do compare their models to see if the head VG is newer than the referenceVG.
        if (!refreshForced && headVG != null && !headVG.equals(referenceVG)) {
            //          create headOntModel. get D12=headModel.diffFromModel(refeneceOntModel)
            ModelBase headSystemModel = headVG.createUnionModel();
            // Note: The head model has committed (driverDeltas) or propagated (driverSystemDeltas if available) to reduce contention.
            // We must persist target model VG after both propagated and committed.
            DeltaBase D12 = headSystemModel.diffFromModel(referenceOntModel);
            //          verify D12.getModelAddition().getOntModel().intersection(sysDelta.getModelReduction().getOntModel()) == empty
            if (sysDelta.getModelReduction() != null && sysDelta.getModelReduction().getOntModel() != null) {
                com.hp.hpl.jena.rdf.model.Model reductionConflict = D12.getModelAddition().getOntModel().intersection(sysDelta.getModelReduction().getOntModel());
                if (!ModelUtil.isEmptyModel(reductionConflict)) {
                    throw new EJBException(String.format("%s %s.modelReduction based on %s conflicts with current head %s", systemInstance, sysDelta, referenceVG, headVG));
                }
            }
            //          verify D12.getModelReduction().getOntModel().intersection(sysDelta.getModelAddiction().getOntModel()) == empty
            if (sysDelta.getModelAddition() != null && sysDelta.getModelAddition().getOntModel() != null) {
                com.hp.hpl.jena.rdf.model.Model additionConflict = D12.getModelReduction().getOntModel().intersection(sysDelta.getModelAddition().getOntModel());
                if (!ModelUtil.isEmptyModel(additionConflict)) {
                    throw new EJBException(String.format("%s %s.modelAddition based on %s conflicts with current head %s", systemInstance, sysDelta, referenceVG, headVG));
                }
            }
            // Note: no need to update current VG to head as the targetDSD will be based the current VG and driverSystem will verify contention on its own.
        }
        //## Step 3. decompose sysDelta into driverSystemDeltas by <Topology>
        // 3.1. split targetOntModel to otain list of target driver topologies
        Map<String, OntModel> targetDriverSystemModels;
        try {
            targetDriverSystemModels = ModelUtil.splitOntModelByTopology(targetOntModel, sysDelta);
        } catch (Exception ex) {
            throw logger.error_throwing(method, "failed to split targetOntModel -- " + ex);
        }
        // 3.2. split referenceOntModel to otain list of reference driver topologies 
        Map<String, OntModel> referenceDriverSystemModels;
        try {
            referenceDriverSystemModels = ModelUtil.splitOntModelByTopology(referenceOntModel, sysDelta);
        } catch (Exception ex) {
            throw logger.error_throwing(method, "failed to split referenceOntModel -- " + ex);
        }
        // 3.3. create list of non-empty driverSystemDeltas by diff referenceOntModel components to targetOntModel
        List<DriverSystemDelta> targetDriverSystemDeltas = new ArrayList<>();
        for (String driverSystemTopoUri : targetDriverSystemModels.keySet()) {
            if (!referenceDriverSystemModels.containsKey(driverSystemTopoUri)) {
                throw new EJBException(String.format("%s cannot decompose %s due to unexpected target topology [uri=%s]", systemInstance, sysDelta, driverSystemTopoUri));
            }
            DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(driverSystemTopoUri);
            if (driverInstance == null) {
                throw new EJBException(String.format("%s cannot find driverInstance for target topology [uri=%s]", systemInstance, driverSystemTopoUri));
            }
            //get old versionItem for reference model
            VersionItem oldVI = referenceVG.getVersionItemByDriverInstance(driverInstance);
            OntModel tom = targetDriverSystemModels.get(driverSystemTopoUri);
            OntModel rom = referenceDriverSystemModels.get(driverSystemTopoUri);
            ModelBase referenceDSM = new ModelBase();
            referenceDSM.setOntModel(rom);
            // check diff from refrence model (tom) to target model (rom)
            DeltaBase delta = referenceDSM.diffToModel(tom);
            if (ModelUtil.isEmptyModel(delta.getModelAddition().getOntModel()) && ModelUtil.isEmptyModel(delta.getModelReduction().getOntModel())) {
                // no diff, use existing verionItem
                continue;
            }
            // create  targetDSD only if there is a change. 
            DriverSystemDelta targetDSD = new DriverSystemDelta();
            // do not save delta as it is transient but delta.modelA and delta.modelR must be saved
            targetDSD.setModelAddition(delta.getModelAddition());
            targetDSD.setModelReduction(delta.getModelReduction());
            targetDSD.setSystemDelta(sysDelta);
            // target delta uses version reference ID of committed model that corresponds to a known version in driverSystem.
            targetDSD.setReferenceVersionItem(oldVI);
            if (driverInstance == null) {
                throw new EJBException(String.format("%s cannot find a dirverInstance for topology: %s", systemInstance, driverSystemTopoUri));
            }
            // prepare to dispatch to driverInstance
            driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
            targetDSD.setDriverInstance(driverInstance);
            targetDriverSystemDeltas.add(targetDSD);
            // Save targetDSD modelA and modelR.
            targetDSD.getModelAddition().setDelta(null);
            targetDSD.getModelReduction().setDelta(null);
            //ModelPersistenceManager.save(targetDSD.getModelAddition());
            //ModelPersistenceManager.save(targetDSD.getModelReduction());
        }
        // Save systemDelta
        sysDelta.setDriverSystemDeltas(targetDriverSystemDeltas);
        sysDelta.setPersistent(false);
        DeltaPersistenceManager.save(sysDelta); // propogate to save included targetDriverSystemDeltas and modelA and modelR

        //## Step 4. propagate driverSystemDeltas 
        Context ejbCxt = null;
        for (DriverSystemDelta targetDSD : targetDriverSystemDeltas) {
            // save targetDSD
            DeltaPersistenceManager.merge(targetDSD);
            targetDSD.getModelAddition().setDelta(targetDSD);
            targetDSD.getModelReduction().setDelta(targetDSD);
            ModelPersistenceManager.merge(targetDSD.getModelAddition());
            ModelPersistenceManager.merge(targetDSD.getModelReduction());
            // push driverSystemDeltas to driverInstances
            DriverInstance driverInstance = targetDSD.getDriverInstance();
            // remove other driverInstance.driverSystemDeltas that are not by the current systemDelta
            if (driverInstance.getDriverSystemDeltas() != null) {
                Iterator<DriverSystemDelta> itOtherDSD = driverInstance.getDriverSystemDeltas().iterator();
                while (itOtherDSD.hasNext()) {
                    DriverSystemDelta otherDSD = itOtherDSD.next();
                    if (!otherDSD.getSystemDelta().equals(sysDelta)) {
                        itOtherDSD.remove();
                    }
                }
            }
            driverInstance.addDriverSystemDelta(targetDSD);
            String driverEjbPath = driverInstance.getDriverEjbPath();
            // make driverSystem propagateDelta call with targetDSD
            try {
                if (ejbCxt == null) {
                    ejbCxt = new InitialContext();
                }
                IHandleDriverSystemCall driverSystemHandler = (IHandleDriverSystemCall) ejbCxt.lookup(driverEjbPath);
                driverSystemHandler.propagateDelta(driverInstance, targetDSD);
            } catch (NamingException e) {
                throw new EJBException(e);
            }
        }
        // save systemInstance
        systemInstance.setSystemDelta(sysDelta);
        SystemInstancePersistenceManager.save(systemInstance);
        //## End of propgation
    }

    @Asynchronous
    public Future<String> commitDelta(SystemInstance systemInstance) {
        // 1. refresh systemInstance
        if (systemInstance.getId() != 0) {
            systemInstance = SystemInstancePersistenceManager.findById(systemInstance.getId());
        }
        if (systemInstance.getSystemDelta() == null || systemInstance.getSystemDelta().getDriverSystemDeltas() == null
                || systemInstance.getSystemDelta().getDriverSystemDeltas().isEmpty()) {
            throw new EJBException(String.format("%s has no systemDelta or driverSystemDeltas to commit", systemInstance));
        }
        Context ejbCxt = null;
        // 2. dispatch commit to drivers
        Map<DriverSystemDelta, Future<String>> commitResultMap = new HashMap<>();
        for (DriverSystemDelta dsd : systemInstance.getSystemDelta().getDriverSystemDeltas()) {
            if (dsd.getStatus() != null && dsd.getStatus().equalsIgnoreCase("DELETED")) {
                continue;
            }
            DriverInstance driverInstance = dsd.getDriverInstance();
            if (driverInstance == null) {
                throw new EJBException(String.format("%s in %s has null driverInstance ", dsd, systemInstance));
            }
            try {
                if (ejbCxt == null) {
                    ejbCxt = new InitialContext();
                }
                String driverEjbPath = driverInstance.getDriverEjbPath();
                IHandleDriverSystemCall driverSystemHandler = (IHandleDriverSystemCall) ejbCxt.lookup(driverEjbPath);
                // 3. Call Async commitDelta to each driverInstance based on versionItems in VG.
                Future<String> result = driverSystemHandler.commitDelta(dsd);
                // 4. add AsyncResult to resultMap
                commitResultMap.put(dsd, result);
            } catch (NamingException e) {
                throw new EJBException(e);
            }
        }
        // 4. Qury for status in a loop bounded by timeout.
        //@TODO: make timeout and interval values configurable
        int timeoutMinutes = 20; // 20 minutes 
        for (int minute = 0; minute < timeoutMinutes; minute++) {
            boolean doneSucessful = true;
            for (DriverSystemDelta dsd : commitResultMap.keySet()) {
                Future<String> asyncResult = commitResultMap.get(dsd);
                if (!asyncResult.isDone()) {
                    doneSucessful = false;
                    break;
                }
                try {
                    String resultStatus = asyncResult.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new EJBException(String.format("commitDelta for (SI=%s, DI=%s) raised exception: %s", systemInstance, dsd.getDriverInstance(), e));
                }
            }
            if (doneSucessful) {
                return new AsyncResult<>("SUCCESS");
            }
            try {
                sleep(60000); // wait for 1 minute
            } catch (InterruptedException ex) {
                //Logger.getLogger(HandleSystemPushCall.class.getName()).log(Level.SEVERE, null, ex);
                throw new EJBException(String.format("commitDelta for %s is interrupted before timed out ", systemInstance));
            }
        }
        throw new EJBException(String.format("commitDelta for %s has timed out ", systemInstance));
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(String sysInstanceUUID, SystemDelta sysDelta, boolean useCachedVG, boolean refreshForced) {
        SystemInstance systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(sysInstanceUUID);
        if (systemInstance == null) {
            throw new EJBException("propagateDelta encounters unknown systemInstance with referenceUUID=" + sysInstanceUUID);
        }

        this.propagateDelta(systemInstance, sysDelta, useCachedVG, refreshForced);
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
        try {
            Context ejbCxt = new InitialContext();
            SystemModelCoordinator systemModelCoordinator = (SystemModelCoordinator) ejbCxt.lookup("java:module/SystemModelCoordinator");
            systemModelCoordinator.setBootStrapped(false);
        } catch (Exception ex) {
            throw new EJBException(this.getClass().getName() + " failed to re-bootstrap systemModelCoordinator", ex);
        }
    }

    public void unplugDriverInstance(String topoUri) {
        DriverInstance di = DriverInstancePersistenceManager.findByTopologyUri(topoUri);
        if (di == null) {
            throw new EJBException(String.format("unplugDriverInstance cannot find the driverInstance for topologyUri=%s", topoUri));
        }
        // remove all related versionItems
        VersionItemPersistenceManager.deleteByDriverInstance(di);
        // remove all empty versionGroups
        VersionGroupPersistenceManager.cleanupAll(null);
        // delete this driverInstance from db
        DriverInstancePersistenceManager.delete(di);
        // set system ready status to false and rebootstrap
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().isEmpty()) {
            try {
                Context ejbCxt = new InitialContext();
                SystemModelCoordinator systemModelCoordinator = (SystemModelCoordinator) ejbCxt.lookup("java:module/SystemModelCoordinator");
                systemModelCoordinator.setBootStrapped(false);
            } catch (Exception ex) {
                throw new EJBException(this.getClass().getName() + " failed to re-bootstrap systemModelCoordinator", ex);
            }
        } 
    }

    public DriverInstance retrieveDriverInstance(String topoUri) {
        DriverInstance di = DriverInstancePersistenceManager.findByTopologyUri(topoUri);
        if (di == null) {
            throw new EJBException(String.format("retrieveDriverInstance cannot find the driverInstance for topologyUri=%s", topoUri));
        }
        return di;
    }

    public Map<String, DriverInstance> retrieveAllDriverInstanceMap() {
        return DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
    }
}
