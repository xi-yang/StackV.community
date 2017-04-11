/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver.onosystem;

import com.hp.hpl.jena.ontology.OntModel;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;
import org.apache.commons.codec.binary.Base64;
/**
 *
 * @author diogon
 */

//use properties: driverSystemPath which translates to calls in driverSystemEjbPath.HandleSystemPushCall and driverSystemEjbPath.HandleSystemCall

@Stateless
public class OnosRESTDriver implements IHandleDriverSystemCall{       

    public static final StackLogger logger = new StackLogger(OnosRESTDriver.class.getName(), "OnosRESTDriver");

    String fakeMap="";
    //String requests="";
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)

    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        String method = "propagateDelta";
        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId());
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getId());
        logger.start(method);
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        String access_key_id = driverInstance.getProperty("onos_access_key_id");
        String secret_access_key = driverInstance.getProperty("onos_secret_access_key");
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String mappingId = driverInstance.getProperty("mappingId");

        String model = driverInstance.getHeadVersionItem().getModelRef().getTtlModel();
        
        String modelAdd = aDelta.getModelAddition().getTtlModel();
        String modelReduc = aDelta.getModelReduction().getTtlModel();
        OnosPush push = new OnosPush();
        
        String requests = null;
        try {
            requests = push.pushPropagate(access_key_id, secret_access_key, mappingId, model, modelAdd, modelReduc, topologyURI, subsystemBaseUrl);            
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }

        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        driverInstance.putProperty(requestId, requests);
        DriverInstancePersistenceManager.merge(driverInstance);
        logger.end(method);
    }

    @Override
    @Asynchronous
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        String method = "commitDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getId());
        logger.start(method);
        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }
        String access_key_id = driverInstance.getProperty("onos_access_key_id");
        String secret_access_key = driverInstance.getProperty("onos_secret_access_key");
        //Searches for topologyURI in Driver Instance
        String topologyURI = driverInstance.getProperty("topologyUri");
        
        
        String mappingId = driverInstance.getProperty("mappingId");
        
        //Searches for subsystemBaseUrl in Driver Instance
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null || access_key_id == null || secret_access_key ==null || topologyURI == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);
        
        OnosPush push = new OnosPush();
        
        try {
            push.pushCommit( access_key_id,  secret_access_key,requests, mappingId, topologyURI,  subsystemBaseUrl, aDelta);
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }

        driverInstance.getProperties().remove(requestId);
        //DriverInstancePersistenceManager.merge(driverInstance);
        Logger.getLogger(OnosRESTDriver.class.getName()).log(Level.INFO, "ONOS driver delta models succesfully commited");
        fakeMap=push.getFakeFlowId();
        driverInstance.putProperty("mappingId", fakeMap);
        DriverInstancePersistenceManager.merge(driverInstance);
        logger.end(method);
        return new AsyncResult<String>("SUCCESS");
    }
    
    @Override
    @Asynchronous
    @SuppressWarnings("empty-statement")
    public Future<String> pullModel(Long driverInstanceId) {
        String method = "pullModel";
        logger.targetid(driverInstanceId.toString());
        logger.start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInance(id=%d)", driverInstanceId));
        }
        try {
            String access_key_id = driverInstance.getProperty("onos_access_key_id");
            String secret_access_key = driverInstance.getProperty("onos_secret_access_key");
            //Searches for topologyURI in Driver Instance
            String topologyURI = driverInstance.getProperty("topologyUri");
            String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");

            String srrgFile = driverInstance.getProperty("srrg");
        
            
            String mappingId = driverInstance.getProperty("mappingId");
            
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }

        // Creates an Ontology Model for ONOS Server
        OntModel ontModel = OnosModelBuilder.createOntology(topologyURI,subsystemBaseUrl, srrgFile, mappingId, access_key_id, secret_access_key);
                       
        if (driverInstance.getHeadVersionItem() == null || !driverInstance.getHeadVersionItem().getModelRef().getOntModel().isIsomorphicWith(ontModel)) {
                DriverModel dm = new DriverModel();
                dm.setCommitted(true);
                dm.setOntModel(ontModel);
                ModelPersistenceManager.save(dm);

                VersionItem vi = new VersionItem();
                vi.setModelRef(dm);
                vi.setReferenceUUID(UUID.randomUUID().toString());
                vi.setDriverInstance(driverInstance);
                VersionItemPersistenceManager.save(vi);
                driverInstance.setHeadVersionItem(vi);
            } 
           
        } catch (Exception ex) {
            throw logger.throwing(method, driverInstance + " failed pull model", ex);
        }
        logger.end(method);
        return new AsyncResult<>("SUCCESS");
    }
    
      private int executeHttpMethod(String access_key_id,String secret_access_key,URL url, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        String username=access_key_id;
        String password=secret_access_key;
        String userPassword=username+":"+password;
        byte[] encoded=Base64.encodeBase64(userPassword.getBytes());
        String stringEncoded=new String(encoded);
        conn.setRequestProperty("Authorization", "Basic "+stringEncoded);
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }
        //logger.log(Level.INFO, "Sending {0} request to URL : {1}", new Object[]{method, url});
        int responseCode = conn.getResponseCode();
        //logger.log(Level.INFO, "Response Code : {0}", responseCode);

        return responseCode;
    }
}

