/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.maxgigapop.mrs.bean.SystemInstance;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author xyang
 */
@Stateless
public class StackSystemDriver implements IHandleDriverSystemCall {

    private static Map<DriverSystemDelta, SystemInstance> driverSystemSessionMap = new HashMap<DriverSystemDelta, SystemInstance>();
    private static final Logger logger = Logger.getLogger(StackSystemDriver.class.getName());

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        //driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId());
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }
        VersionItem refVI = aDelta.getReferenceVersionItem();
        if (refVI == null) {
            throw new EJBException(String.format("%s has no referenceVersionItem", aDelta));
        }
        try {
            // Step 1. create systemInstance
            URL url = new URL(String.format("%s/model/systeminstance", subsystemBaseUrl));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String systemInstanceUUID = this.executeHttpMethod(url, conn, "GET", null);
            // Step 2. propagate delta to systemInstance
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
            url = new URL(String.format("%s/delta/%s/propagate", subsystemBaseUrl, systemInstanceUUID));
            conn = (HttpURLConnection) url.openConnection();
            String status = this.executeHttpMethod(url, conn, "POST", deltaJSON.toString());
            if (!status.toUpperCase().contains("SUCCESS")) {
                throw new EJBException(String.format("%s failed to propagate %s", driverInstance, aDelta));
            }
            driverInstance.putProperty("systemInstanceUUID:" + driverInstance.getId().toString() + aDelta.getId().toString(), systemInstanceUUID);
            DriverInstancePersistenceManager.merge(driverInstance);
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
        String systemInstanceUUID = driverInstance.getProperty("systemInstanceUUID:" + driverInstance.getId().toString() + aDelta.getId().toString());
        if (systemInstanceUUID == null) {
            throw new EJBException(String.format("%s has no property key=systemInstanceUUID as required for commit", driverInstance));
        }
        driverInstance.getProperties().remove("systemInstanceUUID:" + driverInstance.getId().toString() + aDelta.getId().toString());
        DriverInstancePersistenceManager.merge(driverInstance);
        // Step 1. commit to systemInstance
        try {
            URL url = new URL(String.format("%s/delta/%s/commit", subsystemBaseUrl, systemInstanceUUID));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String status = this.executeHttpMethod(url, conn, "PUT", null);
            if (status.toUpperCase().contains("FAILED")) {
                throw new EJBException(String.format("%s failed to commit %s", driverInstance, aDelta));
            }
        } catch (IOException ex) {
            throw new EJBException(String.format("%s failed to connect to subsystem with exception (%s)", driverInstance, ex));
        }
        // Step 2. query systemInstance
        boolean doPoll = true;
        int maxNumPolls = 20; // timeout after 10 minutes -> ? make configurable
        while (doPoll && (maxNumPolls--) > 0) {
            try {
                sleep(30000L); // poll every 30 seconds -> ? make configurable
                // pull model from REST API
                URL url = new URL(String.format("%s/delta/%s/checkstatus", subsystemBaseUrl, systemInstanceUUID));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                String status = this.executeHttpMethod(url, conn, "GET", null);
                if (status.toUpperCase().equals("SUCCESS")) {
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
        // Step 3. delete systemInstance
        try {
            URL url = new URL(String.format("%s/model/systeminstance/%s", subsystemBaseUrl, systemInstanceUUID));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String status = this.executeHttpMethod(url, conn, "DELETE", null);
            if (!status.toUpperCase().contains("SUCCESS")) {
                throw new EJBException(String.format("%s failed to delete systeminstance %s", driverInstance, systemInstanceUUID));
            }
        } catch (IOException ex) {
            throw new EJBException(String.format("%s failed to connect to subsystem with exception (%s)", driverInstance, ex));
        }
        return new AsyncResult<>("SUCCESS");
    }
    // TODO: terminate or reuse sessions in driverSystemSessionMap after commit

    @Override
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInance(id=%d)", driverInstanceId));
        }
        String stackTopologyUri = driverInstance.getProperty("topologyUri");
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }
        if (DriverInstancePersistenceManager.getDriverInstanceByTopologyMap() == null
                || !DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().containsKey(driverInstance.getTopologyUri())) {
            return new AsyncResult<>("INITIALIZING");
        }

        DriverInstance syncOnDriverInstance = DriverInstancePersistenceManager.getDriverInstanceByTopologyMap().get(driverInstance.getTopologyUri());
        synchronized (syncOnDriverInstance) {
            String creationTime = null;
            String version = null;
            String jsonModel = null;
            try {
                VersionItem headVI = driverInstance.getHeadVersionItem();
                if (headVI != null) {
                    URL url = new URL(subsystemBaseUrl + "/model/" + headVI.getReferenceUUID());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String responseStr = this.executeHttpMethod(url, conn, "PUT", null);
                    JSONObject responseJSON = (JSONObject) JSONValue.parseWithException(responseStr);
                    creationTime = responseJSON.get("creationTime").toString();
                    // if creationTime remains the same, skip update
                    Date dateCreationTime = ModelUtil.modelDateFromString(creationTime);
                    Date dateCreationTimeLast = headVI.getModelRef().getCreationTime();
                    if (dateCreationTime.equals(dateCreationTimeLast)) {
                        return new AsyncResult<String>("SUCCESS");
                    }
                    version = responseJSON.get("version").toString();
                    if (version == null || version.isEmpty()) {
                        throw new EJBException(String.format("%s pulled model from subsystem with null/empty version", driverInstance));
                    }
                    jsonModel = responseJSON.get("ttlModel").toString();
                } else {
                    // pull model from REST API
                    URL url = new URL(subsystemBaseUrl + "/model");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String responseStr = this.executeHttpMethod(url, conn, "GET", null);
                    JSONObject responseJSON = (JSONObject) JSONValue.parseWithException(responseStr);
                    creationTime = responseJSON.get("creationTime").toString();
                    version = responseJSON.get("version").toString();
                    if (version == null || version.isEmpty()) {
                        throw new EJBException(String.format("%s pulled model from subsystem with null/empty version", driverInstance));
                    }
                    jsonModel = responseJSON.get("ttlModel").toString();
                }
            } catch (IOException ex) {
                throw new EJBException(String.format("%s failed to connect to subsystem with exception (%s)", driverInstance, ex));
            } catch (org.json.simple.parser.ParseException ex) {
                throw new EJBException(String.format("%s failed to parse pulled model in JSON format with exception (%s)", driverInstance, ex));
            } catch (java.text.ParseException ex) {
                throw new EJBException(String.format("%s failed to parse version datetime (%s)", driverInstance, creationTime));
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
                        throw new EJBException(String.format("%s encounters conflicting sub-level topology with same URI: %s", driverInstance, stackTopologyUri));
                    }
                }
                Resource stackTopo = RdfOwl.createResource(ontModel, stackTopologyUri, Nml.Topology);
                for (RDFNode subRootTopo : listTopo) {
                    ontModel.add(ontModel.createStatement(stackTopo, Nml.hasTopology, subRootTopo));
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
        //logger.log(Level.FINEST, "Sending {0} request to URL : {1}", new Object[]{method, url});
        int responseCode = conn.getResponseCode();
        //logger.log(Level.FINEST, "Response Code : {0}", responseCode);
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
