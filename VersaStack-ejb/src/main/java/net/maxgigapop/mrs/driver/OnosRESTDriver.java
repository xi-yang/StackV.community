/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver;

import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.driver.onosystem.OnosModelBuilder;
import net.maxgigapop.mrs.driver.onosystem.OnosPush;
import net.maxgigapop.mrs.driver.onosystem.OnosDriver;
import com.hp.hpl.jena.ontology.OntModel;
import static com.hp.hpl.jena.sparql.lang.SPARQLParserRegistry.parser;
import static com.hp.hpl.jena.sparql.lang.UpdateParserRegistry.parser;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
import net.maxgigapop.mrs.common.ModelUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



import net.maxgigapop.mrs.driver.aws.AwsModelBuilder;
import net.maxgigapop.mrs.driver.aws.AwsPush;
import net.maxgigapop.mrs.driver.aws.AwsDriver;
import com.amazonaws.regions.Regions;
import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;
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
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;
/**
 *
 * @author xyang
 */

//use properties: driverSystemPath which translates to calls in driverSystemEjbPath.HandleSystemPushCall and driverSystemEjbPath.HandleSystemCall

@Stateless
public class OnosRESTDriver implements IHandleDriverSystemCall{       
    private static final Logger logger = Logger.getLogger(OnosRESTDriver.class.getName());

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    /*public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        //driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        aDelta = (DriverSystemDelta)DeltaPersistenceManager.findById(aDelta.getId()); // refresh
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }
        VersionItem refVI = aDelta.getReferenceVersionItem();
        if (refVI == null) {
            throw new EJBException(String.format("%s has no referenceVersionItem", aDelta));
        }
        try {
            // compose string body (delta) using JSONObject
            JSONObject deltaJSON = new JSONObject();
            deltaJSON.put("id", Long.toString(aDelta.getId()));
            deltaJSON.put("referenceVersion", refVI.getReferenceUUID());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");         
            deltaJSON.put("creationTime", dateFormat.format(new Date()).toString());
            if (aDelta.getModelAddition() != null && aDelta.getModelAddition().getOntModel() != null) {
                String ttlModelAddition = ModelUtil.marshalOntModel(aDelta.getModelAddition().getOntModel());
                deltaJSON.put("modelAddition", ttlModelAddition);
            }
            if (aDelta.getModelReduction() != null && aDelta.getModelReduction().getOntModel() != null) {
                String ttlModelReduction = ModelUtil.marshalOntModel(aDelta.getModelReduction().getOntModel());
                deltaJSON.put("modelReduction", ttlModelReduction);
            }
            // push via REST POST
            URL url = new URL(String.format("%s/delta", subsystemBaseUrl));
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            String status = this.executeHttpMethod(url, conn, "POST", deltaJSON.toString());
            if (!status.toUpperCase().equals("CONFIRMED")) {
                throw new EJBException(String.format("%s failed to push %s into CONFIRMED status", driverInstance, aDelta));
            }
        } catch (Exception e) {
            throw new EJBException(String.format("propagateDelta failed for %s with %s due to exception (%s)", driverInstance, aDelta, e.getMessage()));
        }
    }

    @Override
    @Asynchronous
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw new EJBException(String.format("commitDelta see null driverInance for %s", aDelta));
        }
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }
        // commit through PUT
        try {
            URL url = new URL(String.format("%s/delta/%s/%d/commit", subsystemBaseUrl, aDelta.getReferenceVersionItem().getReferenceUUID(), aDelta.getId()));
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            String status = this.executeHttpMethod(url, conn, "PUT", null);
            //$$  if status == FAILED and raise exception
        } catch (IOException ex) {
            throw new EJBException(String.format("%s failed to connect to subsystem with exception (%s)", driverInstance, ex));
        }
        // query through GET
        boolean doPoll = true;
        int maxNumPolls = 10; // timeout after 5 minutes -> ? make configurable
        while (doPoll && (maxNumPolls--) > 0) {
            try {
                sleep(30000L); // poll every 30 minutes -> ? make configurable
                // pull model from REST API
                URL url = new URL(String.format("%s/delta/%s/%d", subsystemBaseUrl, aDelta.getReferenceVersionItem().getReferenceUUID(), aDelta.getId()));
                //URL url = new URL(String.format("%s/devices", subsystemBaseUrl));
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                String status = this.executeHttpMethod(url, conn, "GET", null);
                if (status.toUpperCase().equals("ACTIVE")) {
                    doPoll = false; // committed successfully
                } else if (status.toUpperCase().contains("FAILED")) {
                    throw new EJBException(String.format("%s failed to commit %s with status=%s", driverInstance, aDelta, status));
                }
            } catch (InterruptedException ex) {
                throw new EJBException(String.format("%s poll for commit status is interrupted", driverInstance));
            } catch (IOException ex) {
                throw new EJBException(String.format("%s failed to communicate with subsystem with exception (%s)", driverInstance, ex));
            }
        }        
        return new AsyncResult<>("SUCCESS");
    }
    
    @Override*/
    @Asynchronous
    @SuppressWarnings("empty-statement")
    public Future<String> pullModel(Long driverInstanceId) {
        
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInance(id=%d)", driverInstanceId));
        }
  
        try {
        //    String r = driverInstance.getProperty("region");
            //Searches for topologyURI in Driver Instance
            String topologyURI = driverInstance.getProperty("topologyUri");
            //Searches for subsystemBaseUrl in Driver Instance
            String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        //    Regions region = Regions.fromName(r);
            //Creates an Ontology Model for ONOS Server
            OntModel ontModel = OnosModelBuilder.createOntology(topologyURI,subsystemBaseUrl);
            
            
            
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
        } catch (IOException e) {
            throw new EJBException(String.format("pullModel on %s raised exception[%s]", driverInstance, e.getMessage()));
        } catch (Exception ex) {
            Logger.getLogger(OnosRESTDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }
        /*if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null 
                || !DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().containsKey(driverInstance.getTopologyUri())) {
            return new AsyncResult<>("INITIALIZING");
        }*/
        // sync on cached DriverInstance object = once per driverInstance to avoid write multiple vi of same version 
        //DriverInstance syncOnDriverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(driverInstance.getTopologyUri());
        /*synchronized (syncOnDriverInstance) {
            String version = null;
            String ttlModel = null;
            String creationTimestamp = null;
            try {
                if (driverInstance.getHeadVersionItem() != null) {
                    URL url = new URL(subsystemBaseUrl + "/model/"+driverInstance.getHeadVersionItem().getReferenceUUID());
                    //URL url = new URL(subsystemBaseUrl + "/devices");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String status = this.executeHttpMethod(url, conn, "GET", null);
                    if (status.toUpperCase().equals("LATEST")) {
                        return new AsyncResult<>("SUCCESS");
                    }
                }
                // pull model from REST API
                //URL url = new URL(subsystemBaseUrl + "/model");
                URL url = new URL(subsystemBaseUrl + "/topology");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                String responseStr = this.executeHttpMethod(url, conn, "GET", null);
                responseStr = readFile("/Users/diogonunes/Downloads/file.json");
                JSONObject responseJSON = (JSONObject) JSONValue.parseWithException(responseStr);
                
              
                
              
                version = responseJSON.get("version").toString();
                
                if (version == null || version.isEmpty()) {
                    throw new EJBException(String.format("%s pulled model from subsystem with null/empty version", driverInstance));
                }
                //ttlModel = responseJSON.get("ttlModel").toString();
                //if (ttlModel == null || ttlModel.isEmpty()) {
                //     System.out.println("8"+responseStr);
                //    throw new EJBException(String.format("%s pulled model from subsystem with null/empty ttlModel content", driverInstance));
                    
                //}
                //creationTimestamp = responseJSON.get("creationTime").toString();
                //if (creationTimestamp == null || creationTimestamp.isEmpty()) {
                //    throw new EJBException(String.format("%s pulled model from subsystem with null/empty creationTime", driverInstance));
                //}
            } catch (IOException ex) {
                throw new EJBException(String.format("%s failed to connect to subsystem with exception (%s)", driverInstance, ex));
            
            } catch (ParseException ex) {
                Logger.getLogger(OnosRESTDriver.class.getName()).log(Level.SEVERE, null, ex);
            } 
            VersionItem vi = null;
            DriverModel dm = null;
            try {
                // check if this version has been pulled before
                vi = VersionItemPersistenceManager.findByReferenceUUID(version);
                if (vi != null) {
                    return new AsyncResult<>("SUCCESS");
                }
                // create new driverDelta and versioItem
                OntModel ontModel = ModelUtil.unmarshalOntModel(ttlModel);
                dm = new DriverModel();
                dm.setCommitted(true);
                dm.setOntModel(ontModel);
                Date creationTime = new Date(Long.parseLong(creationTimestamp));
                dm.setCreationTime(creationTime);
                ModelPersistenceManager.save(dm);
                vi = new VersionItem();
                vi.setModelRef(dm);
                vi.setReferenceUUID(version);
                vi.setDriverInstance(driverInstance);
                VersionItemPersistenceManager.save(vi);
                driverInstance.setHeadVersionItem(vi);
                VersionItemPersistenceManager.save(vi);
                logger.info(String.format("persisted %s", vi));
            } catch (Exception e) {
                try {
                    if (dm != null) {
                        ModelPersistenceManager.delete(dm);
                    }
                    if (vi != null) {
                        VersionItemPersistenceManager.delete(vi);
                    }
                } catch (Exception ex) {
                    ; // do nothing (logging?)
                }
                throw new EJBException(String.format("pullModel on %s raised exception[%s]", driverInstance, e.getMessage()));
            }
        }*/

        return new AsyncResult<>("SUCCESS");
    }
}
