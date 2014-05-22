/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.session;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import net.maxgigapop.mrs.bean.*;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.SystemInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;

/**
 *
 * @author xyang
 */
@LocalBean
@Stateless
public class HandleSystemCall {

    public SystemInstance createInstance() {
        SystemInstance systemInstance = new SystemInstance();
        SystemInstancePersistenceManager.save(systemInstance);
        return systemInstance;
    }

    public void terminateInstance(SystemInstance systemInstance) {
        if (systemInstance != null) {
            for (SystemDelta aDelta : systemInstance.getSystemDeltas()) {
                ModelPersistenceManager.delete(aDelta);
            }
            SystemInstancePersistenceManager.delete(systemInstance);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(SystemInstance systemInstance, SystemDelta aDelta) {
        if (systemInstance == null) {
            throw new EJBException(String.format("null systemInstance"));
        }
        // use VG from systemInstance.versionGroup or based on aDelta.versionReferenceId
        // Note 1: a defaut VG (#1) must exist the first time the system starts.
        // Note 2: the VG below must contain versionItems for committed models only.
        if (aDelta.getReferenceVersionGroup() != null) {
            systemInstance.setVersionGroup(aDelta.getReferenceVersionGroup());
        }
        if (systemInstance.getVersionGroup() == null) {
            throw new EJBException(String.format("%s has no reference versionGroup to work with", systemInstance));
        }

        //EJBExeption may be thrown upon fault from subroutine of each step below
        
        //## Step 1. create reference model and target model 
        systemInstance.updateReferenceModel();
        OntModel referenceOntModel = systemInstance.getReferenceModel().getOntModel();
        OntModel targetOntModel = systemInstance.getReferenceModel().dryrunDelta(aDelta);
        systemInstance.addSystemDelta(aDelta);

        //## Step 2. verify model change
        // 2.1. get head/lastest VG based on the current versionGroup 
        VersionGroup headVG = VersionGroupPersistenceManager.refreshToHead(systemInstance.getVersionGroup());
        // 2.2. if the head VG is newer than the current/reference VG
        if (headVG != null && !headVG.equals(systemInstance.getVersionGroup())) {
            //          create headOntModel. get D12=headModel.diffFromModel(refeneceOntModel)
            ModelBase headSystemModel = headVG.createUnionModel();
            // Note: The head model has committed (driverDeltas) or propagated (driverSystemDeltas if available) to reduce contention.
            // We must persist target model VG after both propagated and committed.
            DeltaBase D12 = headSystemModel.diffFromModel(referenceOntModel);
            //          verify D12.getModelAddition().getOntModel().intersection(aDelta.getModelReduction().getOntModel()) == empty
            //          verify D12.getModelReduction().getOntModel().intersection(aDelta.getModelAddiction().getOntModel()) == empty
            com.hp.hpl.jena.rdf.model.Model reductionConflict = D12.getModelAddition().getOntModel().intersection(aDelta.getModelReduction().getOntModel());
            com.hp.hpl.jena.rdf.model.Model additionConflict = D12.getModelReduction().getOntModel().intersection(aDelta.getModelAddition().getOntModel());
            //          if either verification fails throw EJBException("version conflict");
            if (!ModelUtil.isEmptyModel(reductionConflict) || !ModelUtil.isEmptyModel(additionConflict)) {
                throw new EJBException(String.format("%s %s based on %s conflicts with current head %s", systemInstance, aDelta, systemInstance.getVersionGroup(), headVG));
            }
            // Note: no need to update current VG to head as the targetDSD will be based the current VG and driverSystem will verify contention on its own.
        }

        //## Step 3. decompose aDelta into driverSystemDeltas by <Topology>
        // 3.1. split targetOntModel to otain list of target driver topologies
        Map<String, OntModel> targetDriverSystemModels = ModelUtil.splitOntModelByTopology(targetOntModel);
        // 3.2. split referenceOntModel to otain list of reference driver topologies 
        Map<String, OntModel> referenceDriverSystemModels = ModelUtil.splitOntModelByTopology(referenceOntModel);
        // 3.3. create list of non-empty driverSystemDeltas by diff referenceOntModel components to targetOntModel
        List<DriverSystemDelta> targetDriverSystemDeltas = new ArrayList<DriverSystemDelta>();
        VersionGroup targetVG = new VersionGroup();
        targetVG.setStatus("INIT");
        VersionGroupPersistenceManager.save(targetVG);
        for (String driverSystemTopoUri : targetDriverSystemModels.keySet()) {
            if (!referenceDriverSystemModels.containsKey(driverSystemTopoUri)) {
                throw new EJBException(String.format("%s cannot decompose %s due to unexpected target topology [uri=%s]", systemInstance, aDelta, driverSystemTopoUri));
            }
            DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(driverSystemTopoUri);
            if (driverInstance == null) {
                throw new EJBException(String.format("%s cannot find driverInstance for target topology [uri=%s]", systemInstance, driverSystemTopoUri));
            }
            //get old versionItem for reference model
            VersionItem oldVI = systemInstance.getVersionGroup().getVersionItemByDriverInstance(driverInstance);
            OntModel tom = targetDriverSystemModels.get(driverSystemTopoUri);
            OntModel rom = referenceDriverSystemModels.get(driverSystemTopoUri);
            ModelBase referenceDSM = new ModelBase();
            referenceDSM.setOntModel(rom);
            // check diff from refrence model (tom) to target model (rom)
            DeltaBase delta = referenceDSM.diffToModel(tom);
            if (ModelUtil.isEmptyModel(delta.getModelAddition().getOntModel()) && ModelUtil.isEmptyModel(delta.getModelReduction().getOntModel())) {
                // no diff, use existing verionItem
                targetVG.addVersionItem(oldVI);
                continue;
            }
            // create targetDSM and targetDSD only if there is a change. 
            ModelBase targetDSM = new ModelBase();
            targetDSM.setOntModel(tom);
            // new versionItem using targetDSM as temporary modelRef
            VersionItem newVI = new VersionItem();
            newVI.setDriverInstance(driverInstance);
            newVI.setModelRef(targetDSM);
            newVI.addVersionGroup(targetVG);
            targetVG.addVersionItem(newVI);
            VersionGroupPersistenceManager.save(newVI);
            // target model has a new version ID
            targetDSM.setCxtVersion(newVI.getId());
            ModelPersistenceManager.save(targetDSM);
            // create targetDSD 
            DriverSystemDelta targetDSD = new DriverSystemDelta();
            targetDSD.setPersistent(true);
            targetDSD.setModelAddition(delta.getModelAddition());
            targetDSD.setModelReduction(delta.getModelReduction());
            targetDSD.setSystemDelta(null);
            // target delta uses version reference ID of committed model that corresponds to a known version in driverSystem.
            targetDSD.setReferenceVersionItem(oldVI);
            targetDSD.setTargetVersionItem(newVI);
            if (driverInstance == null) {
                throw new EJBException(String.format("%s cannot find a dirverInstance for topology: %s", systemInstance, driverSystemTopoUri));
            }
            // prepare to dispatch to driverInstance
            targetDSD.setDriverInstance(driverInstance);
        }

        //## Step 4. propagate driverSystemDeltas 
        for (DriverSystemDelta targetDSD : targetDriverSystemDeltas) {
            // 4.1. save driverSystemDeltas
            DeltaPersistenceManager.save(targetDSD);
            // 4.2. push driverSystemDeltas to driverInstances
            DriverInstance driverInstance = targetDSD.getDriverInstance();
            String driverEjbPath = driverInstance.getDriverEjbPath();
            // make driverSystem propagateDelta call with targetDSD
            try {
                Context ctx = new InitialContext();
                HandleDriverSystemCall = (HandleDriverSystemCall) ctx.lookup(driverEjbPath);
                HandleDriverSystemCall.propagateDelta(driverInstance, targetDSD);
            } catch (NamingException e) {
                throw new EJBException(e);
            }
        }
        // 4.3 save systemInstance and VG
        SystemInstancePersistenceManager.save(systemInstance);
        targetVG.setStatus("PROPAGATED");
        VersionGroupPersistenceManager.save(targetVG);
        // Note: driverSystem will save the targetDSD to driverInstance and extract refrerence version ID 

        //## End of propagtion
    }

    @Asynchronous
    public Future<String> commitDelta(SystemInstance systemInstance) {
        String status = "INIT";

        // 1. Get target VG
        // 2. Get list of driverInstances
        // 3. Call Async commitDelta to each driverInstance based on versionItems in VG.
        // 4. Qury for status in a loop bounded by timeout.
        // 5. return status
        //$$ catch exception to create abnormal FAILED status
        return new AsyncResult<String>(status);
    }

    //public VersionGroup retrieveHeadVersionGroup()
    //public ModelBase retrieveHeadModel()

    //public VersionGroup retrieveVersionGroup(Long vgId)
    //public ModelBase retrieveVersionModel(Long vgId)
}
