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
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.datatype.XMLGregorianCalendar;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverSystemDeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.DateTimeUtil;
import net.maxgigapop.mrs.common.DriverUtil;
import net.maxgigapop.mrs.common.EJBExceptionNegotiable;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import org.apache.logging.log4j.core.net.Severity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
public class SenseRMDriver implements IHandleDriverSystemCall {

    private static final StackLogger logger = new StackLogger(SenseRMDriver.class.getName(), "SenseRMDriver");

    //@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getId());
        logger.start(method);
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
        }
        VersionItem refVI = aDelta.getReferenceVersionItem();
        if (refVI == null) {
            throw logger.error_throwing(method, "target:DriverSystemDelta has no reference VersionItem");
        }
        String sslClientCertAlias = driverInstance.getProperty("sslClientCertAlias");
        String sslKeytorePassword = driverInstance.getProperty("sslKeytorePassword");
        try {
            // compose string body (delta) using JSONObject
            JSONObject deltaJSON = new JSONObject();
            deltaJSON.put("id", aDelta.getReferenceUUID());
            deltaJSON.put("modelId", refVI.getReferenceUUID());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            deltaJSON.put("lastModified", dateFormat.format(new Date()).toString());
            if (aDelta.getModelAddition() != null && aDelta.getModelAddition().getOntModel() != null) {
                String ttlModelAddition = ModelUtil.marshalOntModel(aDelta.getModelAddition().getOntModel());
                deltaJSON.put("addition", ttlModelAddition);
            }
            if (aDelta.getModelReduction() != null && aDelta.getModelReduction().getOntModel() != null) {
                String ttlModelReduction = ModelUtil.marshalOntModel(aDelta.getModelReduction().getOntModel());
                deltaJSON.put("reduction", ttlModelReduction);
            }
            // push via REST POST
            URL url = new URL(String.format("%s/deltas", subsystemBaseUrl));
            HttpURLConnection conn;
            if (url.toString().startsWith("https:")) {
                conn = (HttpsURLConnection) url.openConnection();
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setConnectTimeout(5*1000);
            //conn.setReadTimeout(5*1000);
            conn.setRequestProperty("Content-Encoding", "gzip");
            String[] response;
            if (sslClientCertAlias == null) {
                response = DriverUtil.executeHttpMethod(conn, "POST", deltaJSON.toString());
            } else {
                response = DriverUtil.executeHttpMethodWithClientCert(conn, "POST", deltaJSON.toString(), sslClientCertAlias, sslKeytorePassword);
            }
            if (response[1].equals("201")) {
                aDelta.setStatus("PROPGATED");
            } else if (response[1].equals("409")) {
                String jsonData = response[0];
                EJBExceptionNegotiable ejbNegotiable = new EJBExceptionNegotiable();
                ejbNegotiable.setNegotitaionMessage(jsonData);
                throw logger.throwing(method, ejbNegotiable);
            } else if (response[1].equals("400")) {
                throw logger.error_throwing(method, driverInstance + "Bad Request - " + response[0]);
            } else if (response[1].equals("401")) {
                throw logger.error_throwing(method, driverInstance + " Unauthorized Request - " + response[0]);
            } else if (response[1].equals("404")) {
                throw logger.error_throwing(method, driverInstance + " Resource Unfound - " + response[0]);
            } else if (response[1].equals("406")) {
                throw logger.error_throwing(method, driverInstance + " Request Unacceptable - " + response[0]);                
            } else if (response[1].equals("500")) {
                throw logger.error_throwing(method, driverInstance + " RM Internal Error - " + response[0]);                
            } else {
                throw logger.error_throwing(method, driverInstance + " Unexpected HTTP return code: " + response[1]);
            }
        } catch (IOException e) {
            throw logger.throwing(method, driverInstance + " API failed to communicate with subsystem - ", e);
        } catch (Exception ex) {
            throw logger.throwing(method, driverInstance + " API failed to propagate - ", ex);
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
        logger.targetid(aDelta.getId());
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
        String sslClientCertAlias = driverInstance.getProperty("sslClientCertAlias");
        String sslKeytorePassword = driverInstance.getProperty("sslKeytorePassword");
        // commit through PUT
        try {
            URL url = new URL(String.format("%s/deltas/%s/actions/commit", subsystemBaseUrl, aDelta.getReferenceUUID()));
            HttpURLConnection conn;
            if (url.toString().startsWith("https:")) {
                conn = (HttpsURLConnection) url.openConnection();
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setConnectTimeout(5*1000);
            //conn.setReadTimeout(5*1000);
                String[] response;
                if (sslClientCertAlias == null) {
                    response = DriverUtil.executeHttpMethod(conn, "PUT", null);
                } else {
                    response = DriverUtil.executeHttpMethodWithClientCert(conn, "PUT", null, sslClientCertAlias, sslKeytorePassword);
                }
            if (response[1].equals("200") || response[1].equals("204")) {
                aDelta.setStatus("COMMITTING");
                DeltaPersistenceManager.merge(aDelta);
            } else if (response[1].equals("400")) {
                throw logger.error_throwing(method, driverInstance + "Bad Request - " + response[0]);
            } else if (response[1].equals("401")) {
                throw logger.error_throwing(method, driverInstance + " Unauthorized Request - " + response[0]);
            } else if (response[1].equals("404")) {
                throw logger.error_throwing(method, driverInstance + " Resource Unfound - " + response[0]);
            } else if (response[1].equals("406")) {
                throw logger.error_throwing(method, driverInstance + " Request Unacceptable - " + response[0]);
            } else if (response[1].equals("409")) {
                throw logger.error_throwing(method, driverInstance + " Resource Conflict - " + response[0]);
            } else if (response[1].equals("500")) {
                throw logger.error_throwing(method, driverInstance + " RM Internal Error - " + response[0]);
            } else { // handle other HTTP code
                throw logger.error_throwing(method, driverInstance + " Unexpected HTTP return code: " + response[1]);
            }
        } catch (IOException ex) {
            throw logger.throwing(method, driverInstance + " API failed to communicate with subsystem ", ex);
        }
        // query through GET
        boolean doPoll = true;
        int maxNumPolls = 10; // timeout after 5 minutes -> ? make configurable
        while (doPoll && (maxNumPolls--) > 0) {
            try {
                sleep(30000L); // poll every 30 seconds -> ? make configurable
                // pull model from REST API
                URL url = new URL(String.format("%s/deltas/%s?summary=true", subsystemBaseUrl, aDelta.getReferenceUUID()));
                HttpURLConnection conn;
                if (url.toString().startsWith("https:")) {
                    conn = (HttpsURLConnection) url.openConnection();
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                String[] response;
                if (sslClientCertAlias == null) {
                    response = DriverUtil.executeHttpMethod(conn, "GET", null);
                } else {
                    response = DriverUtil.executeHttpMethodWithClientCert(conn, "GET", null, sslClientCertAlias, sslKeytorePassword);
                }
                if (response[1].equals("200")) { // committed successfully
                    JSONObject responseJSON;
                    if (response[0].startsWith("[")) {
                        responseJSON = (JSONObject) ((JSONArray) JSONValue.parseWithException(response[0])).get(0);
                    } else {
                        responseJSON = (JSONObject) JSONValue.parseWithException(response[0]);
                    }
                    if (!responseJSON.containsKey("state") || responseJSON.get("state") == null) {
                        throw logger.error_throwing(method, driverInstance + "RM return none / null 'state' - " + response[0]);
                    }
                    aDelta.setStatus(((String) responseJSON.get("state")).toUpperCase());
                    DeltaPersistenceManager.merge(aDelta);
                    if (aDelta.getStatus().equalsIgnoreCase("COMMITTED") || aDelta.getStatus().equalsIgnoreCase("ACTIVATING") || aDelta.getStatus().equalsIgnoreCase("ACTIVATED")) {
                        doPoll = false;
                    } else if (aDelta.getStatus().equalsIgnoreCase("COMMITTING")) {
                        doPoll = true;
                    } else if (aDelta.getStatus().equalsIgnoreCase("FAILED") ) {
                        //@TODO: responseJSON.error
                        throw logger.error_throwing(method, driverInstance + "RM Internal Error - " + response[0]);
                    }
                } else if (response[1].equals("400")) {
                    throw logger.error_throwing(method, driverInstance + "Bad Request - " + response[0]);
                } else if (response[1].equals("401")) {
                    throw logger.error_throwing(method, driverInstance + " Unauthorized Request - " + response[0]);
                } else if (response[1].equals("404")) {
                    throw logger.error_throwing(method, driverInstance + " Resource Unfound - " + response[0]);
                } else if (response[1].equals("406")) {
                    throw logger.error_throwing(method, driverInstance + " Request Unacceptable - " + response[0]);
                } else if (response[1].equals("409")) {
                    throw logger.error_throwing(method, driverInstance + " Resource Conflict - " + response[0]);
                } else if (response[1].equals("500")) {
                    throw logger.error_throwing(method, driverInstance + " RM Internal Error - " + response[0]);
                } else { // handle other HTTP code
                    throw logger.error_throwing(method, driverInstance + " Unexpected HTTP return code: " + response[1]);
                }
            } catch (InterruptedException e) {
                throw logger.error_throwing(method, driverInstance + " polling commit status got interrupted");
            } catch (IOException ex) {
                throw logger.throwing(method, driverInstance + " failed to communicate with subsystem - ", ex);
            } catch (ParseException ex) {
                throw logger.throwing(method, driverInstance + " failed to parse delta information - ", ex);
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
            return new AsyncResult<>("INIT");
        }
        String sslClientCertAlias = driverInstance.getProperty("sslClientCertAlias");
        String sslKeytorePassword = driverInstance.getProperty("sslKeytorePassword");
        // sync on cached DriverInstance object = once per driverInstance to avoid write multiple vi of same version 
        DriverInstance syncOnDriverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(driverInstance.getTopologyUri());
        synchronized (syncOnDriverInstance) {
            String version = null;
            String ttlModel = null;
            String creationTimeStr = null;
            SimpleDateFormat r1123Formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            r1123Formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                String lastModified = null;
                if (driverInstance.getHeadVersionItem() != null) {
                    lastModified = r1123Formatter.format(driverInstance.getHeadVersionItem().getModelRef().getCreationTime());
                }
                // pull model from REST API
                URL url = new URL(subsystemBaseUrl + "/models?current=true&summary=false&encode=false");
                HttpURLConnection conn;
                if (url.toString().startsWith("https:")) {
                    conn = (HttpsURLConnection) url.openConnection();
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                if (lastModified != null) {
                    conn.addRequestProperty("If-Modified-Since", lastModified);
                }
                conn.setConnectTimeout(5*1000);
                conn.setReadTimeout(30*1000);
                conn.addRequestProperty("Content-Encoding", "gzip");
                String[] response;
                if (sslClientCertAlias == null) {
                    response = DriverUtil.executeHttpMethod(conn, "GET", null);
                } else {
                    response = DriverUtil.executeHttpMethodWithClientCert(conn, "GET", null, sslClientCertAlias, sslKeytorePassword);
                }
                if (response[1].equals("304")) {
                    return new AsyncResult<>("SUCCESS");
                } else if (response[1].equals("400")) {
                    throw logger.error_throwing(method, driverInstance + "Bad Request - " + response[0]);
                } else if (response[1].equals("401")) {
                    throw logger.error_throwing(method, driverInstance + " Unauthorized Request - " + response[0]);
                } else if (response[1].equals("404")) {
                    throw logger.error_throwing(method, driverInstance + " Resource Unfound - " + response[0]);
                } else if (response[1].equals("406")) {
                    throw logger.error_throwing(method, driverInstance + " Request Unacceptable - " + response[0]);
                } else if (response[1].equals("500")) {
                    throw logger.error_throwing(method, driverInstance + " RM Internal Error - " + response[0]);
                } else if (!response[1].equals("200")) { // handle other HTTP code
                    throw logger.error_throwing(method, driverInstance + " Unexpected HTTP return code: " + response[1]);
                }
                JSONObject responseJSON = (JSONObject) ((JSONArray) JSONValue.parseWithException(response[0])).get(0);
                if (responseJSON.containsKey("id")) {
                    version = responseJSON.get("id").toString();
                }
                if (version == null || version.isEmpty()) {
                    throw logger.error_throwing(method, driverInstance + "encounters null/empty id in pulled model from SENSE-RM");
                }
                if (responseJSON.containsKey("model")) {
                    ttlModel = responseJSON.get("model").toString();
                }
                if (ttlModel == null || ttlModel.isEmpty()) {
                    throw logger.error_throwing(method, driverInstance + "encounters null/empty model content from SENSE-RM");
                }
                if (responseJSON.containsKey("creationTime")) {
                    creationTimeStr = responseJSON.get("creationTime").toString();
                }
                if (creationTimeStr == null || creationTimeStr.isEmpty()) {
                    throw logger.error_throwing(method, driverInstance + "encounters null/empty creationTime from SENSE-RM");
                }
            } catch (IOException ex) {
                throw logger.throwing(method, driverInstance + " API failed to connect to subsystem with ", ex);
            } catch (ParseException ex) {
                throw logger.throwing(method, driverInstance + " failed to parse pulled information from subsystem ", ex);
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
                XMLGregorianCalendar cal = DateTimeUtil.xmlGregorianCalendar(creationTimeStr);
                Date creationTime = DateTimeUtil.xmlGregorianCalendarToDate(cal);
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
                    logger.error(method, "failed to clean up (" + ex + ") for exception: " + e);
                }
                throw logger.throwing(method, driverInstance + " API failed to pull model ", e);
            }
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }
}
