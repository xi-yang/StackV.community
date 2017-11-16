/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Adam Smith 2017

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
package net.maxgigapop.mrs.driver.googlecloud;

//import net.maxgigapop.mrs.driver.aws.AwsModelBuilder;
//import net.maxgigapop.mrs.driver.aws.AwsPush;
//import net.maxgigapop.mrs.driver.aws.AwsDriver;

//import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;//
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;//returned by commit and pull
import javax.ejb.Asynchronous;//commit and pull are asynchronous
import javax.ejb.Stateless;//entire class is stateless
import javax.ejb.TransactionAttribute;//
import javax.ejb.TransactionAttributeType;//

import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.StackLogger;//
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;//
//import static net.maxgigapop.mrs.driver.openstack.OpenStackDriver.logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Stateless
public class GcpDriver implements IHandleDriverSystemCall {
    
    public static final StackLogger logger = new StackLogger(GcpDriver.class.getName(), "GoogleCloudDriver");

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        System.out.println("START PROPAGATE DELTA");
        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId()); // refresh
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getId());
        logger.start(method);
        String jsonAuth = driverInstance.getProperty("gcp_access_json");
        String projectID = driverInstance.getProperty("projectID");
        //String region = driverInstance.getProperty("region");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String defaultImage = driverInstance.getProperty("defaultImage");
        String defaultInstanceType = driverInstance.getProperty("defaultInstanceType");
        //String defaultKeyPair = driverInstance.getProperty("defaultKeyPair");
        //String defaultSecGroup = driverInstance.getProperty("defaultSecGroup");
        String defaultRegion = driverInstance.getProperty("defaultRegion");
        String defaultZone = driverInstance.getProperty("defaultZone");
        
        OntModel model = driverInstance.getHeadVersionItem().getModelRef().getOntModel();
        OntModel modelAdd = aDelta.getModelAddition().getOntModel();
        OntModel modelReduc = aDelta.getModelReduction().getOntModel();
        
        GcpPush push = new GcpPush(jsonAuth, projectID, topologyURI, defaultImage, defaultInstanceType, defaultRegion, defaultZone);
        
        JSONArray requests = push.propagate(model, modelAdd, modelReduc);
        String requestId = driverInstance.getId().toString() + aDelta.getId();
        driverInstance.putProperty(requestId, requests.toString());
        DriverInstancePersistenceManager.merge(driverInstance);
        logger.end(method);
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    @Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        logger.cleanup();
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
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);
        if (requests == null) {
            throw logger.error_throwing(method, "requests == null - trying to commit after propagate failed, requestId="+requestId);
        }
        if (requests.isEmpty()) {
            driverInstance.getProperties().remove(requestId);
            DriverInstancePersistenceManager.merge(driverInstance);
            throw logger.error_throwing(method, "requests.isEmpty - no change to commit, requestId="+requestId);
        }
        String jsonAuth = driverInstance.getProperty("gcp_access_json");
        
        String projectID =  driverInstance.getProperty("projectID");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String defaultImage = driverInstance.getProperty("defaultImage");
        String defaultInstanceType = driverInstance.getProperty("defaultInstanceType");
        String defaultRegion = driverInstance.getProperty("defaultRegion");
        String defaultZone = driverInstance.getProperty("defaultZone");
        
        driverInstance.getProperties().remove(requestId);
        DriverInstancePersistenceManager.merge(driverInstance);
        GcpPush push = new GcpPush(jsonAuth, projectID, topologyURI, defaultImage, defaultInstanceType, defaultRegion, defaultZone);
        
        JSONParser parser = new JSONParser();
        
        try {
            JSONArray requestArray = (JSONArray) parser.parse(requests);
        } catch (ParseException ex) {
            throw logger.throwing(method, ex);
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JSONArray requestList;
                //= new ArrayList();
        try {
            requestList = mapper.readValue(requests, mapper.getTypeFactory().constructCollectionType(List.class, JSONObject.class));
        } catch (IOException ex) {
            throw logger.throwing(method, ex);
        }
        try {
            push.commit(requestList);
        } catch (InterruptedException e) {
            throw logger.throwing(method, e);
        }
        logger.end(method);
        return new AsyncResult<String>("SUCCESS");
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    @Override
    public Future<String> pullModel(Long driverInstanceId) {
        logger.cleanup();
        String method = "pullModel";
        logger.targetid(driverInstanceId.toString());
        logger.trace_start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }
        
        try {
            String jsonAuth = driverInstance.getProperty("gcp_access_json");
            String projectID = driverInstance.getProperty("projectID");
            String region = driverInstance.getProperty("region");
            String topologyURI = driverInstance.getProperty("topologyUri");
            //Regions region = Regions.fromName(r);
            OntModel ontModel = GcpModelBuilder.createOntology(jsonAuth, projectID, topologyURI);

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
            throw logger.throwing(method, driverInstance + " failed GoogleCloudModelBuilder.createOntology", e);
        } catch (Exception ex) {
            throw logger.throwing(method, driverInstance + " failed pull model due to "+ex.getMessage(), ex);
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }

}
