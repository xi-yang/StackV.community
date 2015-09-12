/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver.onosystem;

//import net.maxgigapop.mrs.driver.onosystem.OnosModelBuilder;
//import net.maxgigapop.mrs.driver.onosystem.OnosPush;
//import net.maxgigapop.mrs.driver.onosystem.OnosDriver;
import static com.hp.hpl.jena.sparql.lang.SPARQLParserRegistry.parser;
import static com.hp.hpl.jena.sparql.lang.UpdateParserRegistry.parser;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
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
import net.maxgigapop.mrs.bean.VersionItem;
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
            String topologyURI = driverInstance.getProperty("topologyUri");
            String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
            String srrgFile = driverInstance.getProperty("srrg");
            
            String access_key_id = driverInstance.getProperty("username");
            String secret_access_key = driverInstance.getProperty("password");
            
        if (subsystemBaseUrl == null) {
            throw new EJBException(String.format("%s has no property key=subsystemBaseUrl", driverInstance));
        }

            
        //    Regions region = Regions.fromName(r);
            //Creates an Ontology Model for ONOS Server
        OntModel ontModel = OnosModelBuilder.createOntology(topologyURI,subsystemBaseUrl, srrgFile, access_key_id, secret_access_key);
        
        //System.out.println("\nsuccess after createOntology\n");
            
            
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
        
        //System.out.println("\nsuccess getting head version\n");
        
        } catch (Exception ex) {
            Logger.getLogger(OnosRESTDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        
        return new AsyncResult<>("SUCCESS");
    }
}