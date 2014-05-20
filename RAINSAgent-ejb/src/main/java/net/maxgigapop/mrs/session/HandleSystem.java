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
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import net.maxgigapop.mrs.bean.*;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.SystemInstancePersistenceManager;

/**
 *
 * @author xyang
 */

@Stateless
public class HandleSystem implements HandleSystemLocal {
 
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
        if (systemInstance == null)
            systemInstance = this.createInstance();
        //EJBExeption will be thrown upon fault
        systemInstance.addSystemDelta(aDelta);5
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
        
        //$$ update systemInstance with versioning handling (and model commited?)
    }
    
    @Asynchronous
    public Future<String> provisionDelta(SystemInstance systemInstance, SystemDelta aDelta) {
        String status = "";
        String transactionUUID = UUID.randomUUID().toString();
        //$$ TODO: start async trasaction to wait for updates for driverModels
        //$$ return async transaction ID
        return new AsyncResult<String>(status);
    }
    
}
