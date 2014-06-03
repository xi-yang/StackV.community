/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.session;

import com.hp.hpl.jena.ontology.OntModel;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
@Stateful
@LocalBean
public class HandleSystemPushCall {
    private VersionGroup targetVG = null;
    
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
        VersionGroup referenceVG = aDelta.getReferenceVersionGroup();
        if (referenceVG == null) {
            throw new EJBException(String.format("%s has no reference versionGroup to work with", systemInstance));
        }

        //EJBExeption may be thrown upon fault from subroutine of each step below
        
        //## Step 1. create reference model and target model 
        ModelBase referenceModel = referenceVG.createUnionModel();
        OntModel referenceOntModel = referenceModel.getOntModel();
        OntModel targetOntModel = referenceModel.dryrunDelta(aDelta);
        systemInstance.addSystemDelta(aDelta);

        //## Step 2. verify model change
        // 2.1. get head/lastest VG based on the current versionGroup 
        VersionGroup headVG = VersionGroupPersistenceManager.refreshToHead(referenceVG);
        // 2.2. if the head VG is newer than the current/reference VG
        if (headVG != null && !headVG.equals(referenceVG)) {
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
                throw new EJBException(String.format("%s %s based on %s conflicts with current head %s", systemInstance, aDelta, referenceVG, headVG));
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
        this.targetVG = new VersionGroup();
        this.targetVG.setStatus("INIT");
        //transient - do not save
        //VersionGroupPersistenceManager.save(this.targetVG);
        for (String driverSystemTopoUri : targetDriverSystemModels.keySet()) {
            if (!referenceDriverSystemModels.containsKey(driverSystemTopoUri)) {
                throw new EJBException(String.format("%s cannot decompose %s due to unexpected target topology [uri=%s]", systemInstance, aDelta, driverSystemTopoUri));
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
                this.targetVG.addVersionItem(oldVI);
                continue;
            }
            // create targetDSM and targetDSD only if there is a change. 
            ModelBase targetDSM = new ModelBase();
            targetDSM.setOntModel(tom);
            // new versionItem using targetDSM as temporary modelRef
            VersionItem newVI = new VersionItem();
            newVI.setDriverInstance(driverInstance);
            newVI.setModelRef(targetDSM);
            newVI.addVersionGroup(this.targetVG);
            this.targetVG.addVersionItem(newVI);
            //transient - do not save
            //VersionGroupPersistenceManager.save(newVI);
            // target model has a new version ID
            targetDSM.setCxtVersion(newVI.getId());
            //transient - do not save
            //ModelPersistenceManager.save(targetDSM);
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
        Context ejbCxt = null;
        for (DriverSystemDelta targetDSD : targetDriverSystemDeltas) {
            // 4.1. save driverSystemDeltas
            DeltaPersistenceManager.save(targetDSD);
            // 4.2. push driverSystemDeltas to driverInstances
            DriverInstance driverInstance = targetDSD.getDriverInstance();
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
        // 4.3 save systemInstance and VG
        this.targetVG.setStatus("PROPAGATED");
        //transient - do not save
        //VersionGroupPersistenceManager.save(this.targetVG);
        SystemInstancePersistenceManager.save(systemInstance);

        //## End of propagtion
    }

    @Asynchronous
    public Future<String> commitDelta(SystemInstance systemInstance) {
        String status = "INIT";
        try {
            // 1. Get target VG from this stateful bean
            if (this.targetVG == null || this.targetVG.getVersionItems() == null || this.targetVG.getVersionItems().isEmpty()) {
                throw new EJBException(String.format("%s has null or empty versionGroup", systemInstance));
            }
            Context ejbCxt = null;
            // 2. Get list of versionItem, driverInstances and DSD
            Map<VersionItem, Future<String>> commitResultMap = new HashMap<VersionItem, Future<String>>();
            for (VersionItem targetVI : this.targetVG.getVersionItems()) {
                DriverInstance driverInstance = targetVI.getDriverInstance();
                if (driverInstance == null) {
                    throw new EJBException(String.format("%s in %s has null driverInstance ", targetVI, systemInstance));
                }
                try {
                    if (ejbCxt == null) {
                        ejbCxt = new InitialContext();
                    }
                    String driverEjbPath = driverInstance.getDriverEjbPath();
                    IHandleDriverSystemCall driverSystemHandler = (IHandleDriverSystemCall) ejbCxt.lookup(driverEjbPath);
                    // 3. Call Async commitDelta to each driverInstance based on versionItems in VG.
                    Future<String> result = driverSystemHandler.commitDelta(driverInstance, targetVI);
                    // 4. add AsyncResult to resultMap
                    commitResultMap.put(targetVI, result);
                } catch (NamingException e) {
                    throw new EJBException(e);
                }
            }
            // 4. Qury for status in a loop bounded by timeout.
            //@TODO: make timeout and interval values configurable
            int timeoutMinutes = 10; // 10 minutes 
            for (int minute = 0; minute < timeoutMinutes; minute++) {
                boolean doneSucessful = true;
                for (VersionItem targetVI : commitResultMap.keySet()) {
                    Future<String> asyncResult = commitResultMap.get(targetVI);
                    if (!asyncResult.isDone()) {
                        doneSucessful = false;
                        break;
                    }
                    String resultStatus = asyncResult.get();
                    // done but failed
                    if (!resultStatus.contains("SUCCESS")) {
                        status = "FAILED";
                        return new AsyncResult<String>(status);
                    }
                }
                if (doneSucessful) {
                    status = "SUCCESS";
                    return new AsyncResult<String>(status);
                }
            }
            status = "TIMEOUT";
            sleep(60000); // wait for 1 minute
        } catch (Exception e) {
            //@TODO: add error message to result
            status = "FALIED";
            // logging
        }
        if (status.equals("INIT")) {
        }
        return new AsyncResult<String>(status);
    }
}
