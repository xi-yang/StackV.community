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
        if (systemInstance.getVersionGroup() == null) {
            throw new EJBException(String.format("systemInstance{%s} has null versionGroup", systemInstance));
        }

        //EJBExeption may be thrown upon fault from subroutine of each step below
        
        //## Step 1. create reference model and target model 
        //@TODO: use aDelta.versionRef instead
        systemInstance.updateReferenceModel();
        OntModel referenceOntModel = systemInstance.getReferenceModel().getOntModel();
        OntModel targetOntModel = systemInstance.getReferenceModel().dryrunDelta(aDelta);
        systemInstance.addSystemDelta(aDelta);

        //## Step 2. verify model change
        // 2.1. get current head versionGroup 
        //@TODO: Note that a defaut VG (#1) must exist the first time the system starts!
        //@TODO: Use refreshToHeadGroup(systemInstance.getVersionGroup()) to get an updated one based on just-propagated versionItems
        VersionGroup headVG = VersionGroupPersistenceManager.getHeadGroup();
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
            //          otherwise update systemInstance.versionGroup to head VG and Transient referenceModel = headModel
            systemInstance.setVersionGroup(headVG);
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
        targetVG.setVersionItems(new ArrayList<VersionItem>());
        for (String driverSystemTopoUri : targetDriverSystemModels.keySet()) {
            if (!referenceDriverSystemModels.containsKey(driverSystemTopoUri)) {
                throw new EJBException(String.format("%s cannot decompose %s due to unexpected target topology: %s", systemInstance, aDelta, driverSystemTopoUri));
            }
            OntModel tom = targetDriverSystemModels.get(driverSystemTopoUri);
            DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(driverSystemTopoUri);
            // version items using targetDSM as temporary modelRef
            //@TODO: some VersionItems should reuse existing ones in ManyToMany relation to VersionGroup
            ModelBase targetDSM = new ModelBase();
            targetDSM.setOntModel(tom);
            VersionItem newVI = new VersionItem();
            newVI.setDriverInstance(driverInstance);
            newVI.setModelRef(targetDSM);
            newVI.setVersionGroup(targetVG);
            targetVG.getVersionItems().add(newVI);
            VersionGroupPersistenceManager.save(newVI);
            targetDSM.setCxtVersion(newVI.getId());
            ModelPersistenceManager.save(targetDSM);
            // create targetDSD only if there is a change. 
            OntModel rom = referenceDriverSystemModels.get(driverSystemTopoUri);
            ModelBase referenceDSM = new ModelBase();
            referenceDSM.setOntModel(rom);
            DeltaBase delta = referenceDSM.diffToModel(tom);
            if (ModelUtil.isEmptyModel(delta.getModelAddition().getOntModel()) && ModelUtil.isEmptyModel(delta.getModelReduction().getOntModel())) {
                continue;
            }
            DriverSystemDelta targetDSD = new DriverSystemDelta();
            targetDSD.setPersistent(true);
            targetDSD.setModelAddition(delta.getModelAddition());
            targetDSD.setModelReduction(delta.getModelReduction());
            targetDSD.setSystemDelta(null);
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
            String driverEjbPath = targetDSD.getDriverInstance().getDriverEjbPath();
            //@TODO make driverSystem propagateDelta call with reference DSM versionID and targetDSD
        }
        // 4.3 save systemInstance and VG
        SystemInstancePersistenceManager.save(systemInstance);
        targetVG.setStatus("PROPAGATED");
        VersionGroupPersistenceManager.save(targetVG);

        //## End of propagtion
    }

    @Asynchronous
    public Future<String> commitDelta(SystemInstance systemInstance, SystemDelta aDelta) {
        String status = "INIT";

        // 1. Get target VG
        // 2. Get list of driverInstances
        // 3. Call Async commitDelta to each driverInstance with the VersionItem number.
        // 4. Qury for status in a loop bounded by timeout.
        // 5. return status
        //$$ catch exception to create abnormal FAILED status
        return new AsyncResult<String>(status);
    }

    //public ModelBase retrieveHeadModel(String statusFilter)
    //public ModelBase retrieveVersionModel(Long vgId)
}
