/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver;

import com.hp.hpl.jena.ontology.OntModel;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.URL;
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
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */

//use properties: driverSystemPath which translates to calls in driverSystemEjbPath.HandleSystemPushCall and driverSystemEjbPath.HandleSystemCall

@Stateless
public class GenericRESTDriver implements IHandleDriverSystemCall{       
    private static final Logger logger = Logger.getLogger(GenericRESTDriver.class.getName());

    //@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
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

    //@Override
    @Asynchronous
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(aDelta.getDriverInstance().getId());
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
                sleep(30000L); // poll every 30 seconds -> ? make configurable
                // pull model from REST API
                URL url = new URL(String.format("%s/delta/%s/%d", subsystemBaseUrl, aDelta.getReferenceVersionItem().getReferenceUUID(), aDelta.getId()));
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
    
    @Override
    @Asynchronous
    @SuppressWarnings("empty-statement")
    public Future<String> pullModel(Long driverInstanceId) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInance(id=%d)", driverInstanceId));
        }
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null 
                || !DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().containsKey(driverInstance.getTopologyUri())) {
            return new AsyncResult<>("INITIALIZING");
        }
        // sync on cached DriverInstance object = once per driverInstance to avoid write multiple vi of same version 
        DriverInstance syncOnDriverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(driverInstance.getTopologyUri());
        synchronized (syncOnDriverInstance) {
            String version = null;
            String ttlModel = null;
            String creationTimestamp = null;
            try {
                if (driverInstance.getHeadVersionItem() != null) {
                    URL url = new URL(subsystemBaseUrl + "/model/"+driverInstance.getHeadVersionItem().getReferenceUUID());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String status = this.executeHttpMethod(url, conn, "GET", null);
                    if (status.toUpperCase().equals("LATEST")) {
                        return new AsyncResult<>("SUCCESS");
                    }
                }
                // pull model from REST API
                URL url = new URL(subsystemBaseUrl + "/model");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                String responseStr = this.executeHttpMethod(url, conn, "GET", null);
                JSONObject responseJSON = (JSONObject) JSONValue.parseWithException(responseStr);
                version = responseJSON.get("version").toString();
                if (version == null || version.isEmpty()) {
                    throw new EJBException(String.format("%s pulled model from subsystem with null/empty version", driverInstance));
                }
                ttlModel = responseJSON.get("ttlModel").toString();
                if (ttlModel == null || ttlModel.isEmpty()) {
                    throw new EJBException(String.format("%s pulled model from subsystem with null/empty ttlModel content", driverInstance));
                }
                creationTimestamp = responseJSON.get("creationTime").toString();
                if (creationTimestamp == null || creationTimestamp.isEmpty()) {
                    throw new EJBException(String.format("%s pulled model from subsystem with null/empty creationTime", driverInstance));
                }
            } catch (IOException ex) {
                throw new EJBException(String.format("%s failed to connect to subsystem with exception (%s)", driverInstance, ex));
            } catch (ParseException ex) {
                throw new EJBException(String.format("%s failed to parse pulled information from subsystem with exception (%s)", driverInstance, ex));
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
        }
        return new AsyncResult<>("SUCCESS");
    }
    
    private String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }
        logger.log(Level.FINEST, "Sending {0} request to URL : {1}", new Object[]{method, url});
        int responseCode = conn.getResponseCode();
        logger.log(Level.FINEST, "Response Code : {0}", responseCode);

        StringBuilder responseStr;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }
        return responseStr.toString();
    }

}
