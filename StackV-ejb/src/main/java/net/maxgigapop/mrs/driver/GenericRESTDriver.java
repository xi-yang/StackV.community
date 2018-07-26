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
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverSystemDeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
//use properties: driverSystemPath which translates to calls in driverSystemEjbPath.HandleSystemPushCall and driverSystemEjbPath.HandleSystemCall
@Stateless
public class GenericRESTDriver implements IHandleDriverSystemCall {

    private static final StackLogger logger = new StackLogger(GenericRESTDriver.class.getName(), "GenericRESTDriver");

    //@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
        }
        VersionItem refVI = aDelta.getReferenceVersionItem();
        if (refVI == null) {
            throw logger.error_throwing(method, "target:DriverSystemDelta has no reference VersionItem");
        }
        try {
            // compose string body (delta) using JSONObject
            JSONObject deltaJSON = new JSONObject();
            deltaJSON.put("id", aDelta.getReferenceUUID());
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
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String status = this.executeHttpMethod(url, conn, "POST", deltaJSON.toString());
            if (!status.toUpperCase().equals("CONFIRMED")) {
                throw logger.error_throwing(method, driverInstance + "failed to push target:DriverSystemDelta into CONFIRMED status");
            }
        } catch (Exception e) {
            throw logger.throwing(method, driverInstance + " failed to propagate", e);
        }
        logger.end(method);
    }

    //@Override
    @Asynchronous
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        logger.cleanup();
        String method = "commitDelta";
        aDelta = DriverSystemDeltaPersistenceManager.findById(aDelta.getId());
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
        }
        // commit through PUT
        try {
            URL url = new URL(String.format("%s/delta/%s/%s/commit", subsystemBaseUrl, aDelta.getReferenceVersionItem().getReferenceUUID(), aDelta.getReferenceUUID()));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String status = this.executeHttpMethod(url, conn, "PUT", null);
        } catch (IOException ex) {
            throw logger.throwing(method, driverInstance + " failed to communicate with subsystem ", ex);
        }
        // query through GET
        boolean doPoll = true;
        int maxNumPolls = 10; // timeout after 5 minutes -> ? make configurable
        while (doPoll && (maxNumPolls--) > 0) {
            try {
                sleep(30000L); // poll every 30 seconds -> ? make configurable
                // pull model from REST API
                URL url = new URL(String.format("%s/delta/%s/%s", subsystemBaseUrl, aDelta.getReferenceVersionItem().getReferenceUUID(), aDelta.getReferenceUUID()));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                String status = this.executeHttpMethod(url, conn, "GET", null);
                if (status.toUpperCase().equals("ACTIVE") || status.toUpperCase().equals("TERMINATED")) {
                    doPoll = false; // committed successfully
                } else if (status.toUpperCase().contains("FAILED")) {
                    throw logger.error_throwing(method, driverInstance + " failed to commit target:DriverSystemDelta with status=" + status);
                }
            } catch (InterruptedException ex) {
                throw logger.error_throwing(method, driverInstance + " polling commit status got interrupted");
            } catch (IOException ex) {
                if (ex instanceof java.io.FileNotFoundException) {
                    logger.warning(method, String.format("%s failed with exception (%s) - check the subsystem for expected resource change...", driverInstance, ex));
                } else {
                    throw logger.throwing(method, driverInstance + " failed to communicate with subsystem ", ex);
                }
            }
        }
        logger.end(method);
        return new AsyncResult<>("SUCCESS");
    }

    @Override
    @Asynchronous
    @SuppressWarnings("empty-statement")
    public Future<String> pullModel(Long driverInstanceId) {
        logger.cleanup();
        String method = "pullModel";
        logger.targetid(driverInstanceId.toString());
        logger.trace_start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw logger.error_throwing(method, "pullModel cannot find target:driverInance");
        }
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
        }
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null
                || !DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().containsKey(driverInstance.getTopologyUri())) {
            logger.warning(method, "driver instance is initializing");
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
                    URL url = new URL(subsystemBaseUrl + "/model/" + driverInstance.getHeadVersionItem().getReferenceUUID());
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
                        throw logger.error_throwing(method, driverInstance + "encounters null/empty version in pulled model from subsystem");
                }
                ttlModel = responseJSON.get("ttlModel").toString();
                if (ttlModel == null || ttlModel.isEmpty()) {
                    throw logger.error_throwing(method, driverInstance + "encounters null/empty ttlModel content");
                }
                creationTimestamp = responseJSON.get("creationTime").toString();
                if (creationTimestamp == null || creationTimestamp.isEmpty()) {
                    throw logger.error_throwing(method, driverInstance + "encounters null/empty creationTime");
                }
            } catch (IOException ex) {
                throw logger.throwing(method, driverInstance + " failed to connect to subsystem with ", ex);
            } catch (ParseException ex) {
                throw logger.throwing(method, driverInstance + " parse pulled information from subsystem ", ex);
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
                logger.trace(method, driverInstance + String.format(" persisted %s", vi));
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
                throw logger.throwing(method, driverInstance + " failed to pull model ", e);
            }
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }

    private String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setConnectTimeout(5*1000);
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
        logger.trace("executeHttpMethod", String.format("Sending %s request to URL : %s", method, url));
        int responseCode = conn.getResponseCode();
        logger.trace("executeHttpMethod", String.format("Response Code : %s", responseCode));

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
