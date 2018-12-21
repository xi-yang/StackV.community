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
import net.maxgigapop.mrs.common.EJBExceptionNegotiable;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.core.DataConcurrencyPoster;
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
    
    private static final StackLogger logger = new StackLogger(HandleSystemCall.class.getName(), "HandleSystemCall");

    public VersionGroup createHeadVersionGroup_API(String refUuid) {
        logger.cleanup();
        return createHeadVersionGroup(refUuid);
    }
    
    public VersionGroup createHeadVersionGroup(String refUuid) {
        String method = "createHeadVersionGroup";
        logger.refuuid(refUuid);
        logger.start(method);
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null) {
            DriverInstancePersistenceManager.refreshAll();
            ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        }
        if (ditMap.isEmpty()) {
            throw logger.error_throwing(method, "encounters empty DriverInstancePersistenceManager.driverInstanceByTopologyMap");
        }
        VersionGroup vg = new VersionGroup();
        vg.setRefUuid(refUuid);
        for (String topoUri : ditMap.keySet()) {
            DriverInstance di = ditMap.get(topoUri);
            if (di == null) {
                throw logger.error_throwing(method, "canont find driverInstance with topologyURI="+topoUri);
            }
            String strDisabled = di.getProperty("disabled");
            boolean diDiasbled = false;
            if (strDisabled != null) {
                diDiasbled = Boolean.parseBoolean(strDisabled);
            }
            if (diDiasbled) {
                continue;
            }
            VersionItem vi = di.getHeadVersionItem();
            if (vi == null) {
                throw logger.error_throwing(method, "encounters null head versionItem in " + di);
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
        logger.end(method);
        return vg;
    }

    public VersionGroup createHeadVersionGroup(String refUuid, List<String> topoURIs) {
        String method = "createHeadVersionGroup";
        logger.refuuid(refUuid);
        logger.start(method);
        Map<String, DriverInstance> ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        if (ditMap == null) {
            DriverInstancePersistenceManager.refreshAll();
            ditMap = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
        }
        if (ditMap.isEmpty()) {
            throw logger.error_throwing(method, "encounters empty DriverInstancePersistenceManager.driverInstanceByTopologyMap");
        }
        VersionGroup vg = new VersionGroup();
        vg.setRefUuid(refUuid);
        for (String topoUri : topoURIs) {
            DriverInstance di = ditMap.get(topoUri);
            if (di == null) {
                throw logger.error_throwing(method, "canont find driverInstance with topologyURI=" + topoUri);
            }
            String strDisabled = di.getProperty("disabled");
            boolean diDiasbled = false;
            if (strDisabled != null) {
                diDiasbled = Boolean.parseBoolean(strDisabled);
            }
            if (diDiasbled) {
                continue;
            }
            VersionItem vi = di.getHeadVersionItem();
            if (vi == null) {
                    throw logger.error_throwing(method, "encounters null head versionItem in " + di);
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
        logger.end(method);
        return vg;
    }

    public VersionGroup updateHeadVersionGroup_API(String refUuid) {
        logger.cleanup();
        return updateHeadVersionGroup(refUuid);
    }
    
    public VersionGroup updateHeadVersionGroup(String refUuid) {
        String method = "updateHeadVersionGroup";
        logger.refuuid(refUuid);
        logger.trace_start(method);
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUuid);
        vg = VersionGroupPersistenceManager.refreshToHead(vg, true);
        logger.trace_end(method);
        return vg;
    }

    public ModelBase retrieveVersionGroupModel(String refUuid) {
        logger.cleanup();
        String method = "retrieveVersionGroupModel";
        logger.refuuid(refUuid);
        logger.trace_start(method);
        if (refUuid.equals("default")) {
            try {//@TODO: use cache model from DataConcurrencyPoster
                Context ejbCxt = new InitialContext();
                DataConcurrencyPoster dataConcurrencyPoster = (DataConcurrencyPoster) ejbCxt.lookup("java:module/DataConcurrencyPoster");
                ModelBase model = dataConcurrencyPoster.getSystemModelCoordinator_cachedModelBase();
                logger.trace_end(method);
                return model;
            } catch (Exception ex) {
                throw logger.throwing(method, ex);
            }
        } else {
            VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUuid);
            if (vg == null) {
                throw logger.error_throwing(method, "cannot find ref:VersionGroup");
            }
            ModelBase model = vg.createUnionModel();
            logger.trace_end(method);
            return model;
        }
    }

    public ModelBase retrieveLatestModelByDriver(String topologyUri) {
        logger.cleanup();
        String method = "retrieveLatestModelBase";
        logger.trace_start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(topologyUri);
        VersionItem headVI = VersionItemPersistenceManager.getHeadByDriverInstance(driverInstance);
        if (headVI == null) {
            return null;
        }
        logger.trace_end(method);
        return headVI.getModelRef();
    }

    public Map<String, ModelBase> retrieveAllLatestModels() {
        logger.cleanup();
        String method = "retrieveAllLatestModels";
        logger.trace_start(method);
        Map<String, ModelBase> retMap = new HashMap();
        for (String topoUri: DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().keySet()) {
            DriverInstance driverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(topoUri);
            if (driverInstance == null) {
                throw logger.error_throwing(method, "canont find driverInstance with topologyURI="+topoUri);
            }
            String strDisabled = driverInstance.getProperty("disabled");
            boolean diDiasbled = false;
            if (strDisabled != null) {
                diDiasbled = Boolean.parseBoolean(strDisabled);
            }
            if (diDiasbled) {
                continue;
            }
            VersionItem headVI = VersionItemPersistenceManager.getHeadByDriverInstance(driverInstance);
            if (headVI == null) {
                continue;
            }
            retMap.put(topoUri, headVI.getModelRef());
        }
        logger.trace_end(method);
        return retMap;
    }
    
    public OntModel queryModelView(String refUuid, List<ModelUtil.ModelViewFilter> mvfs) {
        logger.cleanup();
        String method = "queryModelView";
        logger.refuuid(refUuid);
        logger.start(method);
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUuid);
        if (vg == null) {
            throw logger.error_throwing(method, "cannot find ref:VersionGroup");
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
                throw logger.throwing(method, ex);
            }
        }
        if (resultModel == null) {
            throw logger.error_throwing(method, "has no inclusive filters for ref:VersionGroup");
        }
        for (ModelUtil.ModelViewFilter mvf : mvfs) {
            if (mvf.isInclusive()) {
                continue;
            }
            try {
                OntModel filteredModel = ModelUtil.queryViewFilter(vgModel.getOntModel(), mvf);
                resultModel.remove(filteredModel);
            } catch (Exception ex) {
                throw logger.throwing(method, ex);
            }
        }
        logger.end(method);
        return resultModel;
    }

    public SystemInstance createInstance_API() {
        logger.cleanup();
        return createInstance();
    }
    
    public SystemInstance createInstance() {
        String method = "createInstance";
        logger.start(method);
        SystemInstance systemInstance = new SystemInstance();
        systemInstance.setReferenceUUID(UUID.randomUUID().toString());
        SystemInstancePersistenceManager.save(systemInstance);
        logger.end(method);
        return systemInstance;
    }

    public void terminateInstance_API(String refUUID) {
        terminateInstance(refUUID);
    }
    
    public void terminateInstance(String refUUID) {
        String method = "terminateInstance";
        logger.start(method);
        SystemInstance systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(refUUID);
        systemInstance = SystemInstancePersistenceManager.findById(systemInstance.getId());
        if (systemInstance == null) {
            logger.refuuid(refUUID);
            throw logger.error_throwing(method, "cannot find ref:SystemInstance");
        }
        if (systemInstance.getSystemDelta() != null) {
            DeltaPersistenceManager.delete(systemInstance.getSystemDelta());
            if (systemInstance.getSystemDelta().getDriverSystemDeltas() != null) {
                for (Iterator<DriverSystemDelta> it = systemInstance.getSystemDelta().getDriverSystemDeltas().iterator(); it.hasNext();) {
                    DriverSystemDelta dsd = it.next();
                    it.remove();
                    DeltaPersistenceManager.delete(dsd);
                    logger.trace(method, "deleted "+dsd);
                }
            }
        }
        if (systemInstance.getSystemDelta() != null) {
            DeltaPersistenceManager.delete(systemInstance.getSystemDelta());
            logger.trace(method, "deleted "+systemInstance.getSystemDelta());
        }
        SystemInstancePersistenceManager.delete(systemInstance);
        logger.end(method);
    }

    // useCachedVG = true && useRefreshedVG = false : 'normal' or 'through' mode 
    // useCachedVG = false: 'forward' mode  |  useRefreshedVG = true: 'forced' mode
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(SystemInstance systemInstance, SystemDelta sysDelta, boolean useCachedVG, boolean refreshForced) {
        String method = "propagateDelta";
        logger.start(method);
        // refresh systemInstance into current persistence context
        if (systemInstance.getId() != 0) {
            systemInstance = SystemInstancePersistenceManager.findById(systemInstance.getId());
        }
        logger.refuuid(systemInstance.getReferenceUUID());
        if (systemInstance.getSystemDelta() != null) {
            if (systemInstance.getSystemDelta().getServiceDelta() != null && systemInstance.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
                logger.refuuid(systemInstance.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
            }
            if (systemInstance.getSystemDelta().getDriverSystemDeltas() != null
                    && !systemInstance.getSystemDelta().getDriverSystemDeltas().isEmpty()) {
                logger.targetid(systemInstance.getSystemDelta().getId());
                throw logger.error_throwing(method, "target:SystemDelta has already propagated.");
            }
        }
        if (sysDelta.getId() != null && !sysDelta.getId().isEmpty()) {
            sysDelta = (SystemDelta) DeltaPersistenceManager.findById(sysDelta.getId());
        } else {
            DeltaPersistenceManager.save(sysDelta);
        }
        logger.targetid(sysDelta.getId());
        // Note 1: an initial VG (#1) must exist 
        VersionGroup referenceVG_cached = sysDelta.getReferenceVersionGroup();
        if (referenceVG_cached == null) {
            throw logger.error_throwing(method, "has no reference VersionGroup to work with.");
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
            logger.trace(method, String.format("verify model change with refreshForced=false, headVG=%s, headVG != referenceVG", headVG.toString()));
            //          create headOntModel. get D12=headModel.diffFromModel(refeneceOntModel)
            ModelBase headSystemModel = headVG.createUnionModel();
            // Note: The head model has committed (driverDeltas) or propagated (driverSystemDeltas if available) to reduce contention.
            // We must persist target model VG after both propagated and committed.
            DeltaBase D12 = headSystemModel.diffFromModel(referenceOntModel);
            //          verify D12.getModelAddition().getOntModel().intersection(sysDelta.getModelReduction().getOntModel()) == empty
            if (sysDelta.getModelReduction() != null && sysDelta.getModelReduction().getOntModel() != null) {
                com.hp.hpl.jena.rdf.model.Model reductionConflict = D12.getModelAddition().getOntModel().intersection(sysDelta.getModelReduction().getOntModel());
                if (!ModelUtil.isEmptyModel(reductionConflict)) {
                    throw logger.error_throwing(method, String.format("target:SystemDelta(%s).modelReduction based on %s conflicts with current head %s", sysDelta, referenceVG, headVG));
                }
            }
            //          verify D12.getModelReduction().getOntModel().intersection(sysDelta.getModelAddiction().getOntModel()) == empty
            if (sysDelta.getModelAddition() != null && sysDelta.getModelAddition().getOntModel() != null) {
                com.hp.hpl.jena.rdf.model.Model additionConflict = D12.getModelReduction().getOntModel().intersection(sysDelta.getModelAddition().getOntModel());
                if (!ModelUtil.isEmptyModel(additionConflict)) {
                    throw logger.error_throwing(method, String.format("target:SystemDelta(%s).modelAddition based on %s conflicts with current head %s", sysDelta, referenceVG, headVG));
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
                throw logger.error_throwing(method, String.format("cannot decompose target:SystemDelta(%s) due to unexpected target topology [uri=%s]", sysDelta, driverSystemTopoUri));
            }
            DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(driverSystemTopoUri);
            if (driverInstance == null) {
                throw logger.error_throwing(method, String.format("cannot find driverInstance for target topology [uri=%s]", driverSystemTopoUri));
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
                logger.trace(method, "no diff between reference and target models for target topology="+driverSystemTopoUri);
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
                throw logger.error_throwing(method, String.format("cannot find driverInstance for target topology [uri=%s]", driverSystemTopoUri));
            }
            // prepare to dispatch to driverInstance
            driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
            targetDSD.setDriverInstance(driverInstance);
            targetDriverSystemDeltas.add(targetDSD);
            // Save targetDSD modelA and modelR.
            targetDSD.getModelAddition().setDelta(null);
            targetDSD.getModelReduction().setDelta(null);
            logger.trace(method, "created driver delta for target topology="+driverSystemTopoUri);
        }
        // Save systemDelta
        //sysDelta.setDriverSystemDeltas(targetDriverSystemDeltas);
        //sysDelta.setPersistent(false);

        //## Step 4. propagate driverSystemDeltas 
        Context ejbCxt = null;
        for (DriverSystemDelta targetDSD : targetDriverSystemDeltas) {
            targetDSD.setReferenceUUID(UUID.randomUUID().toString());
            DriverInstance driverInstance = targetDSD.getDriverInstance();
            String driverEjbPath = driverInstance.getDriverEjbPath();
            // make driverSystem propagateDelta call with targetDSD
            try {
                if (ejbCxt == null) {
                    ejbCxt = new InitialContext();
                }
                IHandleDriverSystemCall driverSystemHandler = (IHandleDriverSystemCall) ejbCxt.lookup(driverEjbPath);
                driverSystemHandler.propagateDelta(driverInstance, targetDSD);
            } catch (NamingException ex) {
                throw logger.throwing(method, ex);
            } catch (EJBException ex) {
                throw logger.throwing(method, ex);
            }
        }
        //## Step 5. save propagate data
        //DeltaPersistenceManager.merge(sysDelta); // propogate to save included targetDriverSystemDeltas and modelA and modelR
        for (DriverSystemDelta targetDSD : targetDriverSystemDeltas) {
            // save targetDSD
            DeltaPersistenceManager.save(targetDSD);
            targetDSD.getModelAddition().setDelta(targetDSD);
            targetDSD.getModelReduction().setDelta(targetDSD);
            ModelPersistenceManager.merge(targetDSD.getModelAddition());
            ModelPersistenceManager.merge(targetDSD.getModelReduction());
            logger.trace(method, "start propagating targetDSD="+targetDSD);
            // push driverSystemDeltas to driverInstances
            DriverInstance driverInstance = targetDSD.getDriverInstance();
            logger.trace(method, "targetDSD has driverInstance="+driverInstance);
            DriverInstancePersistenceManager.save(driverInstance);
        }
        //sysDelta.setDriverSystemDeltas(targetDriverSystemDeltas);
        // save systemInstance and cascade to save systemDelta
        systemInstance.setSystemDelta(sysDelta);
        SystemInstancePersistenceManager.save(systemInstance);
        logger.end(method);
        //## End of propgation
    }

    @Asynchronous
    public Future<String> commitDelta(SystemInstance systemInstance) {
        logger.cleanup();
        String method = "commitDelta";
        // 1. refresh systemInstance
        if (systemInstance.getId() != 0) {
            systemInstance = SystemInstancePersistenceManager.findById(systemInstance.getId());
        }
        logger.refuuid(systemInstance.getReferenceUUID());
        if (systemInstance.getSystemDelta() != null) {
            if (systemInstance.getSystemDelta().getServiceDelta() != null && systemInstance.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
                logger.refuuid(systemInstance.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
            }
            if (systemInstance.getSystemDelta().getDriverSystemDeltas() == null
                    || systemInstance.getSystemDelta().getDriverSystemDeltas().isEmpty()) {
                throw logger.error_throwing(method, "as systemDelta == null or systemDelta.driverSystemDeltas is empty");
            }
        }
        logger.targetid(systemInstance.getSystemDelta().getId());
        Context ejbCxt = null;
        // 2. dispatch commit to drivers
        Map<DriverSystemDelta, Future<String>> commitResultMap = new HashMap<>();
        for (DriverSystemDelta dsd : systemInstance.getSystemDelta().getDriverSystemDeltas()) {
            if (dsd.getStatus() != null && dsd.getStatus().equalsIgnoreCase("DELETED")) {
                continue;
            }
            DriverInstance driverInstance = dsd.getDriverInstance();
            driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
            if (driverInstance == null) {
                throw logger.error_throwing(method, String.format("%s has null driverInstance ", dsd, systemInstance));
            }
            try {
                if (ejbCxt == null) {
                    ejbCxt = new InitialContext();
                }
                String driverEjbPath = driverInstance.getDriverEjbPath();
                IHandleDriverSystemCall driverSystemHandler = (IHandleDriverSystemCall) ejbCxt.lookup(driverEjbPath);
                // 3. Call Async commitDelta to each driverInstance based on versionItems in VG.
                dsd.setDriverInstance(driverInstance);
                Future<String> result = driverSystemHandler.commitDelta(dsd);
                // 4. add AsyncResult to resultMap
                commitResultMap.put(dsd, result);
            } catch (NamingException ex) {
                throw logger.throwing(method, ex);
            }
        }
        // 4. Qury for status in a loop bounded by timeout.
        //@TODO: make timeout and interval values configurable
        int timeoutMinutes = 20; // 20 minutes 
        for (int minute = 0; minute < timeoutMinutes; minute++) {
            boolean doneSucessful = true;
            for (DriverSystemDelta dsd : commitResultMap.keySet()) {
                Future<String> asyncResult = commitResultMap.get(dsd);
                logger.trace(method, String.format("polling commited %s at minute %d", dsd, minute+1));
                if (!asyncResult.isDone()) {
                    doneSucessful = false;
                    logger.trace(method, String.format("%s pending", dsd));
                    break;
                }
                try {
                    String resultStatus = asyncResult.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw logger.throwing(method, String.format("commiting %s at minute %d -exception- %s", dsd, minute+1, e), e);
                } catch (EJBException ex) {
                    throw logger.throwing(method, ex);
                }
            }
            if (doneSucessful) {
                logger.trace(method, String.format("commit successful at minute %d", minute+1));
                return new AsyncResult<>("SUCCESS");
            }
            try {
                sleep(60000); // wait for 1 minute
            } catch (InterruptedException ex) {
                throw logger.error_throwing(method, "commit polling got interrupted prematurely");
            }
        }
        throw logger.error_throwing(method, "commit polling timed out - commit unsuccessful");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(String sysInstanceUUID, SystemDelta sysDelta, boolean useCachedVG, boolean refreshForced) {
        logger.cleanup();
        String method = "propagateDelta";
        logger.refuuid(sysInstanceUUID);
        logger.start(method);
        SystemInstance systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(sysInstanceUUID);
        if (systemInstance == null) {
            logger.error_throwing(method, "encounters unknown ref:SystemInstance.");
        }
        logger.message(method, String.format("call internal this.propagateDelta(%s, %s, useCachedVG=%b, useCachedVG=%b)",systemInstance, sysDelta, useCachedVG, refreshForced));
        this.propagateDelta(systemInstance, sysDelta, useCachedVG, refreshForced);
        logger.end(method);
    }
    
    public void plugDriverInstance(Map<String, String> properties) {
        logger.cleanup();
        String method = "plugDriverInstance";
        logger.start(method);
        if (!properties.containsKey("topologyUri") || !properties.containsKey("driverEjbPath")) {
            throw logger.error_throwing(method, "missing either topologyUri or driverEjbPath in properties");
        }
        if (DriverInstancePersistenceManager.findByTopologyUri(properties.get("topologyUri")) != null) {
            throw logger.error_throwing(method, String.format("DriverInstance with topologyURI='%s' has existed.", properties.get("topologyUri")));
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
            throw logger.throwing(method, ex);
        }
        logger.message(method, "plugged " + newDI);
        logger.end(method);
    }

    public List lookupDriverBoundServiceInstances(String topoUri) {
        logger.cleanup();
        String method = "lookupDriverServiceInstances";
        logger.start(method);
        DriverInstance di = DriverInstancePersistenceManager.findByTopologyUri(topoUri);
        if (di == null) {
            throw logger.error_throwing(method, String.format("canot find DriverInstance with topologyURI='%s'.", topoUri));
        }
        List<DriverSystemDelta> listDeltas = DeltaPersistenceManager.retrieveDriverInstanceDeltas(di.getId());
        if (listDeltas == null) {
            return null;
        } 
        List<String> listUUIDs = new ArrayList();
        for (DriverSystemDelta delta: listDeltas) {
            if (delta.getSystemDelta() == null || delta.getSystemDelta().getServiceDelta() == null
                    || delta.getSystemDelta().getServiceDelta().getServiceInstance() == null) {
                continue;
            }
            String svcUUID = delta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID();
            if (!listUUIDs.contains(svcUUID)) {
                listUUIDs.add(delta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
            }
        }
        logger.end(method);
        if (listUUIDs.isEmpty()) {
            return null;
        }
        return listUUIDs;
    }
    
    public void unplugDriverInstance(String topoUri) {
        logger.cleanup();
        String method = "unplugDriverInstance";
        logger.start(method);
        DriverInstance di = DriverInstancePersistenceManager.findByTopologyUri(topoUri);
        if (di == null) {
            throw logger.error_throwing(method, String.format("canot find DriverInstance with topologyURI='%s'.", topoUri));
        }
        di = DriverInstancePersistenceManager.findById(di.getId());
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
                throw logger.throwing(method, ex);
            }
        }
        logger.message(method, "unplugged " + di);
        logger.end(method);
    }

    public DriverInstance retrieveDriverInstance(String topoUri) {
        String method = "retrieveDriverInstance";
        logger.start(method);
        DriverInstance di = DriverInstancePersistenceManager.findByTopologyUri(topoUri);
        if (di == null) {
            throw logger.error_throwing(method, String.format("canot find DriverInstance with topologyURI='%s'.", topoUri));
        }
        logger.end(method);
        return di;
    }

    public Map<String, DriverInstance> retrieveAllDriverInstanceMap() {
        return DriverInstancePersistenceManager.getDriverInstanceByTopologyMap();
    }

    public void setDriverInstanceProperty(String diId, String property, String value) {
        logger.cleanup();
        String method = "setDriverInstanceProperty";
        logger.start(method);
        DriverInstance di;
        if (diId.startsWith("urn")) {
            di = DriverInstancePersistenceManager.findByTopologyUri(diId);
        } else {
            di = DriverInstancePersistenceManager.findById(Long.parseLong(diId));
        }
        if (di == null) {
            throw logger.error_throwing(method, String.format("canot find DriverInstance with ID='%d'.", diId));
        }
        di.getProperties().put(property, value);
        DriverInstancePersistenceManager.merge(di);
    }
    
    public void updateDriverInstance(String diId, Map<String, String> properties) {
        logger.cleanup();
        String method = "updateDriverInstance";
        logger.start(method);
        DriverInstance di;
        if (diId.startsWith("urn")) {
            di = DriverInstancePersistenceManager.findByTopologyUri(diId);
        } else {
            di = DriverInstancePersistenceManager.findById(Long.parseLong(diId));
        }
        if (di == null) {
            throw logger.error_throwing(method, String.format("canot find DriverInstance with ID='%d'.", diId));
        }
        for (String property : properties.keySet()) {
            String value = properties.get(property);
            di.putProperty(property, value);
        }
        DriverInstancePersistenceManager.merge(di);
    }
}
