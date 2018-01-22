/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import com.hp.hpl.jena.ontology.OntModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;

/**
 *
 * @author xin
 */
@Stateless
public class DTNDriver implements IHandleDriverSystemCall {

    private static final StackLogger logger = new StackLogger(DTNDriver.class.getName(), "DTNDriver");
    public static final String delimiterPattern = "[\\(\\)]";

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        String user_account = driverInstance.getProperty("user_account");
        String access_key = driverInstance.getProperty("access_key");
        String address = driverInstance.getProperty("address");
        String topologyURI = driverInstance.getProperty("topologyUri");

        OntModel model = driverInstance.getHeadVersionItem().getModelRef().getOntModel();
        OntModel modelAdd = aDelta.getModelAddition().getOntModel();
        OntModel modelReduc = aDelta.getModelReduction().getOntModel();

        DTNPush push = new DTNPush(user_account, access_key, address, topologyURI);
        String requests = null;
        try {
            requests = push.pushPropagate(model, modelAdd, modelReduc);
        } catch (Exception ex) {
            throw logger.error_throwing(method, ex.getMessage());
        }

        String requestId = driverInstance.getId().toString() + aDelta.getReferenceUUID().toString();
        driverInstance.putProperty(requestId, requests);
        logger.end(method);
    }

    @Asynchronous
    @Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        logger.cleanup();
        String method = "commitDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(aDelta.getDriverInstance().getId());
        if (driverInstance == null) {
            throw logger.error_throwing(method, String.format("commitDelta see null driverInance for %s", aDelta));
        }

        String user_account = driverInstance.getProperty("user_account");
        String access_key = driverInstance.getProperty("access_key");
        String address = driverInstance.getProperty("address");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String map = driverInstance.getProperty("mappingId");

        String requestId = driverInstance.getId().toString() + aDelta.getReferenceUUID().toString();
        String requests = driverInstance.getProperty(requestId);

        DTNPush push = new DTNPush(user_account, access_key, address, topologyURI);
        push.pushCommit(requests);

        driverInstance.getProperties().remove(requestId);
        DriverInstancePersistenceManager.merge(driverInstance);

        logger.message(method, "DTN driver delta models succesfully commited");
        logger.end(method);
        return new AsyncResult<>("SUCCESS");
    }

    @Override
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId) {
        String method = "commitDelta";
        logger.start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInstance(id=%d)", driverInstanceId));
        }
        try {

        	Map<String, String> properties = driverInstance.getProperties();
            String
    				topologyURI = DriverPropertyValidator.validateAndReturn("topologyUri", properties, DriverPropertyType.Generic),
            		pullInvokePattern = DriverPropertyValidator.validateAndReturn("retrieval-pattern", properties, DriverPropertyType.Command),
            		endpoint = DriverPropertyValidator.validateAndReturn("src-endpoint", properties, DriverPropertyType.Generic),
            		addressStr = DriverPropertyValidator.validateAndReturn("addresses", properties, DriverPropertyType.DottedQuad, DriverPropertyType.DottedQuadList);
            Map<String, String[]> pullCommandArgMap = new HashMap<>();
            List<String> paramNames = extractCmdParamNames(pullInvokePattern);
            for (String param : paramNames) {
            	String args = DriverPropertyValidator.validateAndReturn(param, properties, DriverPropertyType.Generic, DriverPropertyType.GenericList);
            	pullCommandArgMap.put(param, args.split(delimiterPattern));
            }
            Map<String, List<Object>> endpointModelConfigMap = new HashMap<>();
            String[] addresses = addressStr.split(delimiterPattern);
            for (int i = 0; i < addresses.length; ++i) {
            	// Splitting on delimiter pattern [\(\)] will create empty matches between consecutive ) and ( in all cases, so skip those iterations
            	if (addresses[i].isEmpty())
            		continue;
            	List<Object> mapped = new ArrayList<>();
            	Map<String, String> pullUtilParams = new HashMap<>();
            	for (String param : pullCommandArgMap.keySet()) {
            		String[] propertyVals = pullCommandArgMap.get(param);
            		pullUtilParams.put(param, propertyVals.length > 1 ? propertyVals[i] : propertyVals[0]);
            	}
            	mapped.add(pullUtilParams);
            	endpointModelConfigMap.put(addresses[i], mapped);
            }

            OntModel ontModel = DTNModelBuilder.createOntology(topologyURI, endpoint, pullInvokePattern, endpointModelConfigMap);

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
            logger.error(method, ex.getMessage());
            return new AsyncResult<>("FAILURE");
        }
        logger.end(method);
        return new AsyncResult<>("SUCCESS");
    }

	private static List<String> extractCmdParamNames(String cmdPattern) {
		List<String> params = new ArrayList<>();
		Matcher matcher = Pattern.compile("(?<=@|#|\\$|%)[\\w\\-_]+").matcher(cmdPattern);
		while(matcher.find())
			params.add(matcher.group(0));
		return params;
	}
}
