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
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.SystemInstancePersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;

/**
 *
 * @author xyang
 */

@LocalBean
@Stateless
public class HandleSystem {
 
    public SystemInstance createInstance() {
        SystemInstance systemInstance = new SystemInstance();
        SystemInstancePersistenceManager.save(systemInstance);
        return systemInstance;
    }

    public void terminateInstance(SystemInstance systemInstance) {
        if (systemInstance != null) {
            for (SystemDelta aDelta: systemInstance.getSystemDeltas()) {
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
        /*
        if (systemInstance.getSystemDeltas() != null && systemInstance.getSystemDeltas().size() > 0) {
            SystemDelta lastDelta = systemInstance.getSystemDeltas().get(systemInstance.getSystemDeltas().size()-1);
        }
        */
        //## Step 1. create reference model and target model 
        //EJBExeption will be thrown upon fault
        //@@@@ systemInstance.createReferenceModel();
        OntModel referenceOntModel = systemInstance.getReferenceModel().getOntModel();
        OntModel targetOntModel = systemInstance.getReferenceModel().dryrunDelta(aDelta);
        systemInstance.addSystemDelta(aDelta);
        
        //## Step 2. verify model change
        // 2.1. get current head versionGroup
        // 2.2. if the head VG is newer than the current/reference VG
        //          create headOntModel. get D12=headModel.diffFromModel(refeneceOntModel)
        // ?? Should head model use committed (driverDeltas) or propagated (driverSystemDeltas).
        //    The later could be newer. To use the latter, we must persist target model VG after propagated. (add VG status?)
        //          verify D12.getModelAddition().getOntModel().intersection(aDelta.getModelReduction().getOntModel()) == empty
        //          verify D12.getModelReduction().getOntModel().intersection(aDelta.getModelAddiction().getOntModel()) == empty
        //          if either verification fails throw EJBException("version conflict");
        //          otherwise update systemInstance.versionGroup to head VG and Transient referenceModel = headModel

        //## Step 3. decompose aDelta into driverSystemDeltas by <Topology>
        // 3.1. split targetOntModel to otain list of target driver topologies
        // 3.2. split referenceOntModel to otain list of reference driver topologies 
        // 3.3. create list of non-empty driverSystemDeltas by diff referenceOntModel components to targetOntModel
        // 3.4. create list of corresponding driverInstances by topology URI.

        //## Step 4. propagate driverSystemDeltas 
        // 4.1. save driverSystemDeltas, systemInstance and VG
        // 4.2. push driverSystemDeltas to driverInstances
        // ?? versioning ?
        //## Finish transaction

        
        /* below to be deleted
        //decompose systemDelta into driverSystemDeltas
        //alway create new
        Map<String, DriverInstance> driverInstanceMap = new HashMap<String, DriverInstance>();
        Map<String, List<DeltaModel>> driverSystemDeltaModelListMap = new HashMap<String, List<DeltaModel>>();
        for (DeltaModel systemDeltaModel: aDelta.getDeltaModels()) {
            try {
                Map<String, OntModel> driverSystemDeltaModelMap = ModelUtil.splitOntModelByTopology(systemDeltaModel.getOntModel());
                for (String topoUri: driverSystemDeltaModelMap.keySet()) {
                    OntModel om = driverSystemDeltaModelMap.get(topoUri);
                    if (!driverSystemDeltaModelListMap.keySet().contains(topoUri)) {
                        driverSystemDeltaModelListMap.put(topoUri, new ArrayList<DeltaModel>());
                    }
                    List<DeltaModel> deltaModels = driverSystemDeltaModelListMap.get(topoUri);
                    DeltaModel dm = new DeltaModel();
                    dm.setOntModel(om);
                    deltaModels.add(dm);
                    //$$$$ save dm
                }
            } catch (Exception e) {
                //Logger.getLogger(HandleSystem.class.getName()).log(Level.SEVERE, null, ex);
                throw new EJBException(String.format("systemInstance failed to prepare %s, due to %s", aDelta.toString(), e.getMessage()));
            }
        } 
        //enlist driver instances
        for (String topoUri: driverSystemDeltaModelListMap.keySet()) {
            DriverSystemDelta dsd = new DriverSystemDelta();
            dsd.setDeltaModels(driverSystemDeltaModelListMap.get(topoUri));
            dsd.setSystemDelta(aDelta);
            if (aDelta.getDriverSystemDeltas() == null) {
                aDelta.setDriverSystemDeltas(new ArrayList<DriverSystemDelta>());
            }
            aDelta.getDriverSystemDeltas().add(dsd);
            DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(topoUri);
            if (driverInstance == null) {
                throw new EJBException(String.format("%s could not find driverInstance for topologyUri=%s", systemInstance.toString(), topoUri));
            }
            driverInstanceMap.put(topoUri, driverInstance);
            dsd.setDriverInstance(driverInstance);
            //$$$$ save dsd, aDelta  
            dsd.saveAll();
            aDelta.saveAll();
            //    @EJB private HandleDriver driverHandler

            //$$$$ driverHandler.provisionDelta(driverInstance, dsd);
            //if (driverInstance.getDriverSystemDeltas() == null) {
            //    driverInstance.setDriverSystemDeltas(new ArrayList<DriverSystemDelta>());
            //}
            //driverInstance.getDriverSystemDeltas().add(dsd);
        }
        
        */
        //$$ update systemInstance with versioning handling (and model commited?)
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
    
}
