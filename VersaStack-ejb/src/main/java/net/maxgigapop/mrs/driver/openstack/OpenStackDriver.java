/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.json.simple.JSONObject;

/**
 *
 * @author muzcategui
 */
@Stateless
public class OpenStackDriver implements IHandleDriverSystemCall {

    static final Logger logger = Logger.getLogger(OpenStackDriver.class.getName());

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    //@Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {

        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId());

        String username = driverInstance.getProperty("username");
        String password = driverInstance.getProperty("password");
        String tenant = driverInstance.getProperty("tenant");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String url = driverInstance.getProperty("url");
        String NATServer = driverInstance.getProperty("NATServer");

        OntModel model = driverInstance.getHeadVersionItem().getModelRef().getOntModel();
        OntModel modelAdd = aDelta.getModelAddition().getOntModel();
        OntModel modelReduc = aDelta.getModelReduction().getOntModel();

        OpenStackPush push = new OpenStackPush(url, NATServer, username, password, tenant, topologyURI);
        List<JSONObject> requests = null;
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        try {
            requests = push.propagate(model, modelAdd, modelReduc);
            driverInstance.putProperty(requestId, requests.toString());
        } catch (Exception ex) {
            Logger.getLogger(OpenStackDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        DriverInstancePersistenceManager.merge(driverInstance);
        Logger.getLogger(OpenStackDriver.class.getName()).log(Level.INFO, "OpenStack driver delta models succesfully propagated");
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    //@Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {

        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw new EJBException(String.format("commitDelta see null driverInance for %s", aDelta));
        }

        String username = driverInstance.getProperty("username");
        String password = driverInstance.getProperty("password");
        String tenant = driverInstance.getProperty("tenant");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String url = driverInstance.getProperty("url");
        String NATServer = driverInstance.getProperty("NATServer");
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);

        OpenStackPush push = new OpenStackPush(url, NATServer, username, password, tenant, topologyURI);
        ObjectMapper mapper = new ObjectMapper();
        List<JSONObject> r = new ArrayList();
        try {
            r = mapper.readValue(requests, mapper.getTypeFactory().constructCollectionType(List.class, JSONObject.class));
        } catch (IOException ex) {
            Logger.getLogger(OpenStackDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            push.pushCommit(r, url, NATServer, username, password, tenant, topologyURI);
        } catch (InterruptedException ex) {
            Logger.getLogger(OpenStackDriver.class.getName()).log(Level.SEVERE, null, ex);
        }

        driverInstance.getProperties().remove(requestId);
        DriverInstancePersistenceManager.merge(driverInstance);

        Logger.getLogger(OpenStackDriver.class.getName()).log(Level.INFO, "OpenStack driver delta models succesfully commited");
        return new AsyncResult<String>("SUCCESS");
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    @Override
    public Future<String> pullModel(Long driverInstanceId) {

        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInstance(id=%d)", driverInstanceId));
        }

        try {
            String username = driverInstance.getProperty("username");
            String password = driverInstance.getProperty("password");
            String tenant = driverInstance.getProperty("tenant");
            String url = driverInstance.getProperty("url");
            String topologyUri = driverInstance.getProperty("topologyUri");
            String NATServer = driverInstance.getProperty("NATServer");

            OntModel ontModel = OpenStackNeutronModelBuilder.createOntology(url, NATServer, topologyUri, username, password, tenant);

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
            Logger.getLogger(OpenStackDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        //Logger.getLogger(AwsDriver.class.getName()).log(Level.INFO, "AWS driver ontology model succesfully pulled");
        return new AsyncResult<>("SUCCESS");
    }

}
