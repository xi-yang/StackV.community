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
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.SystemInstance;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.StackLogger;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
public class StackSystemDriver implements IHandleDriverSystemCall {

    private static final StackLogger logger = new StackLogger(StackSystemDriver.class.getName(), "StackSystemDriver");
    private static Map<DriverSystemDelta, SystemInstance> driverSystemSessionMap = new HashMap<DriverSystemDelta, SystemInstance>();

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
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
        }
        VersionItem refVI = aDelta.getReferenceVersionItem();
        if (refVI == null) {
            throw logger.error_throwing(method, "target:DriverSystemDelta has no reference VersionItem");
        }
        String authServer = driverInstance.getProperty("authServer");
        String credential = driverInstance.getProperty("credential");
        try {
            // Step 1. create systemInstance
            String url = String.format("%s/model/systeminstance", subsystemBaseUrl);
            String[] response = this.executeHttpMethod(url, "GET", null, authServer, credential);
            String systemInstanceUUID = response[2];
            // Step 2. propagate delta to systemInstance
            // compose string body (delta) using JSONObject
            JSONObject deltaJSON = new JSONObject();
            deltaJSON.put("id", aDelta.getId());
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
            url = String.format("%s/delta/%s/propagate", subsystemBaseUrl, systemInstanceUUID);
            response = this.executeHttpMethod(url, "POST", deltaJSON.toString(), authServer, credential);
            String status = response[2];
            if (!status.toUpperCase().contains("SUCCESS")) {
                throw logger.error_throwing(method, "target:DriverSystemDelta has no reference VersionItem");
            }
            driverInstance.putProperty("stackSystemInstanceUUID:" + driverInstance.getId().toString() + aDelta.getId().toString(), systemInstanceUUID);
            DriverInstancePersistenceManager.merge(driverInstance);
        } catch (Exception e) {
            throw logger.throwing(method, driverInstance + " failed to propagate", e);
        }
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
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(aDelta.getDriverInstance().getId());
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
        }
        String stackSystemInstanceUUID = driverInstance.getProperty("stackSystemInstanceUUID:" + driverInstance.getId().toString() + aDelta.getId().toString());
        if (stackSystemInstanceUUID == null) {
            throw logger.error_throwing(method, driverInstance + " has no property key=systemInstanceUUID as required for commit");
        }
        driverInstance.getProperties().remove("stackSystemInstanceUUID:" + driverInstance.getId().toString() + aDelta.getId().toString());
        DriverInstancePersistenceManager.merge(driverInstance);
        String authServer = driverInstance.getProperty("authServer");
        String credential = driverInstance.getProperty("credential");
        // Step 1. commit to systemInstance
        try {
            String url = String.format("%s/delta/%s/commit", subsystemBaseUrl, stackSystemInstanceUUID);
            String[] response = this.executeHttpMethod(url, "PUT", null, authServer, credential);
            String status = response[2];
            if (status.toUpperCase().contains("FAILED")) {
                throw logger.error_throwing(method, driverInstance + " failed to commit target:DriverSystemDelta");
            }
        } catch (IOException ex) {
            throw logger.throwing(method, driverInstance + " failed to connect to subsystem" ,ex);
        }
        // Step 2. query systemInstance
        boolean doPoll = true;
        int maxNumPolls = 20; // timeout after 10 minutes -> ? make configurable
        while (doPoll && (maxNumPolls--) > 0) {
            try {
                sleep(30000L); // poll every 30 seconds -> ? make configurable
                // pull model from REST API
                String url = String.format("%s/delta/%s/checkstatus", subsystemBaseUrl, stackSystemInstanceUUID);
                String[] response = this.executeHttpMethod(url, "GET", null, authServer, credential);
                String status = response[2];
                if (status.toUpperCase().equals("SUCCESS")) {
                    doPoll = false; // committed successfully
                } else if (status.toUpperCase().contains("FAILED")) {
                    throw logger.error_throwing(method, driverInstance + " failed to commit target:DriverSystemDelta with status=" + status);
                }
            } catch (InterruptedException ex) {
                throw logger.error_throwing(method, driverInstance + " polling commit status got interrupted");
            } catch (IOException ex) {
                throw logger.throwing(method, driverInstance + " failed to communicate with subsystem ", ex);
            }
        }
        // Step 3. delete systemInstance
        try {
            String url = String.format("%s/model/systeminstance/%s", subsystemBaseUrl, stackSystemInstanceUUID);
            String[] response = this.executeHttpMethod(url, "DELETE", null, authServer, credential);
            String status = response[2];
            if (!status.toUpperCase().contains("SUCCESS")) {
                throw logger.error_throwing(method, driverInstance + " failed to delete subsystem SystemInstance="+stackSystemInstanceUUID);
            }
        } catch (IOException ex) {
            throw logger.throwing(method, driverInstance + " failed to communicate with subsystem ", ex);
        }
        logger.end(method);
        return new AsyncResult<>("SUCCESS");
    }

    @Override
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId) {
        String method = "pullModel";
        logger.targetid(driverInstanceId.toString());
        logger.trace_start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw logger.error_throwing(method, "pullModel cannot find target:driverInance");
        }
        String stackTopologyUri = driverInstance.getProperty("topologyUri");
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
        }
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null
                || !DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().containsKey(driverInstance.getTopologyUri())) {
            logger.warning(method, "driver instance is initializing");
            return new AsyncResult<>("INITIALIZING");
        }
        String authServer = driverInstance.getProperty("authServer");
        String credential = driverInstance.getProperty("credential");
        DriverInstance syncOnDriverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(driverInstance.getTopologyUri());
        synchronized (syncOnDriverInstance) {
            String creationTime = null;
            String version = null;
            String jsonModel = null;
            VersionItem headVI = driverInstance.getHeadVersionItem();
            try {
                if (headVI != null) {
                    String url = subsystemBaseUrl + "/model/" + headVI.getReferenceUUID();
                    String[] response = this.executeHttpMethod(url, "PUT", null, authServer, credential);
                    JSONObject responseJSON = (JSONObject) JSONValue.parseWithException(response[2]);
                    creationTime = responseJSON.get("creationTime").toString();
                    // if creationTime remains the same, skip update
                    Date dateCreationTime = ModelUtil.modelDateFromString(creationTime);
                    Date dateCreationTimeLast = headVI.getModelRef().getCreationTime();
                    if (dateCreationTime.equals(dateCreationTimeLast)) {
                        return new AsyncResult<String>("SUCCESS");
                    }
                    version = responseJSON.get("version").toString();
                    if (version == null || version.isEmpty()) {
                        throw logger.error_throwing(method, driverInstance + "encounters null/empty version in pulled model from subsystem");
                    }
                    jsonModel = responseJSON.get("ttlModel").toString();
                } 
            } catch (IOException ex) {
                if (ex.toString().contains("response code: 500")) {
                    headVI = null;
                } else {
                    throw logger.throwing(method, driverInstance + "failed to connect to subsystem", ex);
                }
            } catch (org.json.simple.parser.ParseException ex) {
                throw logger.throwing(method, driverInstance + "failed to parse pulled model in JSON format", ex);
            } catch (java.text.ParseException ex) {
                throw logger.throwing(method, driverInstance + "failed to parse datetime=" + creationTime, ex);
            }
            try {
                if (headVI == null) {
                    // pull model from REST API
                    String url = subsystemBaseUrl + "/model";
                    String[] response = this.executeHttpMethod(url, "GET", null, authServer, credential);
                    JSONObject responseJSON = (JSONObject) JSONValue.parseWithException(response[2]);
                    creationTime = responseJSON.get("creationTime").toString();
                    version = responseJSON.get("version").toString();
                    if (version == null || version.isEmpty()) {
                        throw logger.error_throwing(method, driverInstance + "encounters null/empty version in pulled model from subsystem");
                    }
                    jsonModel = responseJSON.get("ttlModel").toString();
                }
            } catch (Exception ex) {
                throw logger.throwing(method, driverInstance + "failed to pull model from " + subsystemBaseUrl, ex);
            }
            VersionItem vi = null;
            DriverModel dm = null;
            try {
                // create new driverDelta and versioItem
                OntModel ontModel = ModelUtil.unmarshalOntModelJson(jsonModel);
                // Alter the model to add stack root topology to contain sub-level driver topologies.
                List<RDFNode> listTopo = ModelUtil.getTopologyList(ontModel);
                for (RDFNode subRootTopo : listTopo) {
                    if (subRootTopo.toString().equals(stackTopologyUri)) {
                        throw logger.error_throwing(method, driverInstance + "conflicting sub-level topology with same URI:" + stackTopologyUri);
                    }
                }
                Resource stackTopo = RdfOwl.createResource(ontModel, stackTopologyUri, Nml.Topology);
                for (RDFNode subRootTopo : listTopo) {
                    ontModel.add(ontModel.createStatement(stackTopo, Nml.hasTopology, subRootTopo));
                    ontModel.add(ontModel.createStatement(stackTopo, Mrs.type, "aggregate"));
                }
                dm = new DriverModel();
                dm.setCommitted(true);
                dm.setOntModel(ontModel);
                Date creationDateTime = ModelUtil.modelDateFromString(creationTime);
                dm.setCreationTime(creationDateTime);
                ModelPersistenceManager.save(dm);
                vi = new VersionItem();
                vi.setModelRef(dm);
                vi.setReferenceUUID(version);
                vi.setDriverInstance(driverInstance);
                VersionItemPersistenceManager.save(vi);
                driverInstance.setHeadVersionItem(vi);
                DriverInstancePersistenceManager.merge(driverInstance);
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

    public String[] executeHttpMethod(String url, String method, String body, String authServer, String credential) throws IOException {
        this.prepareTrustStore(null); // skip and use global trust store config
        String methods[] = method.split("/");
        method = methods[0];
        String type = (methods.length > 1 ? methods[1] : "json");
        URL urlObj = new URL(url);
        URLConnection conn = urlObj.openConnection();
        if (url.startsWith("https:")) {
            ((HttpsURLConnection) conn).setRequestMethod(method);
        } else {
            ((HttpURLConnection) conn).setRequestMethod(method);
        }
        if (authServer != null && !authServer.isEmpty()) {
            URL urlObjAuth = new URL(authServer);
            // assume https for authentication server
            HttpsURLConnection authConn = (HttpsURLConnection) urlObjAuth.openConnection();
            authConn.setRequestMethod("POST");
            //authConn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            String authBody = credential;
            authConn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(authConn.getOutputStream());
            wr.writeBytes(authBody);
            wr.flush();
            StringBuilder responseStr = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(authConn.getInputStream()));
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
            //log.info("Return from authServer" + responseStr);
            JSONObject responseJSON = new JSONObject();
            try {
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(responseStr.toString());
                responseJSON = (JSONObject) obj;
            } catch (ParseException ex) {
                logger.error("executeHttpMethod", "cannot parsing json: "+responseStr.toString());
                throw (new IOException(ex));
            }
            String bearerToken = (String) responseJSON.get("access_token");
            //log.info("Got token from authServer"+bearerToken);
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            String refreshToken = (String) responseJSON.get("refresh_token");
            conn.setRequestProperty("Refresh",  refreshToken);
        } else if (credential != null && !credential.isEmpty()) {
            byte[] encoded = Base64.encodeBase64(credential.getBytes());
            String stringEncoded = new String(encoded);
            conn.setRequestProperty("Authorization", "Basic " + stringEncoded);
        }
        conn.setRequestProperty("Content-type", "application/" + type);
        conn.setRequestProperty("Accept", "application/"+type);
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
        }
        logger.trace("executeHttpMethod", String.format("Sending %s request to URL : %s", method, url));
        String response[] = new String[3];
        response[0] = Integer.toString(((HttpURLConnection) conn).getResponseCode());
        response[1] = ((HttpURLConnection) conn).getResponseMessage();
        StringBuilder responseStr;
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        responseStr = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            responseStr.append(inputLine);
        }
        response[2] = responseStr.toString();
        logger.trace("executeHttpMethod", String.format("Response Code : %s", response[0]));
        return response;
    }

    private void prepareTrustStore(String trustStore) {
        if (trustStore == null) {
            return;
        }
        if (trustStore.isEmpty()) {
            TrustManager trustAllCerts[] = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                logger.catching("prepareTrustStore", e);
            }
        }
        try {
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            InputStream is = new FileInputStream(trustStore);
            keyStore.load(is, "changeit".toCharArray());
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            kmf.init(keyStore, "changeit".toCharArray());
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory
                    .getDefaultAlgorithm());
            tmf.init(keyStore);
            final SSLContext sc = SSLContext.getInstance("TLSv1");
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());
            final SSLSocketFactory socketFactory = sc.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
        } catch (Exception e) {
            logger.catching("prepareTrustStore", e);
        }
    }
}
