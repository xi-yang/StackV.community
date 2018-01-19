/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.ccsn;

import com.hp.hpl.jena.ontology.OntModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import net.maxgigapop.mrs.common.validation.DriverPropertyType;
import net.maxgigapop.mrs.common.validation.DriverPropertyValidator;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;

/**
 *
 * @author xin
 */
@Stateless
public class CCSNDriver implements IHandleDriverSystemCall {

    Logger logger = Logger.getLogger(CCSNDriver.class.getName());
    public static final String delimiterPattern = "[\\(\\)]";

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        logger.log(Level.INFO, "CCSN delta propagate stub called");
    }

    @Asynchronous
    @Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        logger.log(Level.INFO, "CCSN delta commit stub called");
        return new AsyncResult<>("SUCCESS");
    }

    @Override
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null)
            throw new EJBException(String.format("pullModel cannot find driverInstance(id=%d)", driverInstanceId));

        try {
        	// Retrieve instance configuration properties and package pull command parameters for CCSNPull module
            Map<String, String> configs = driverInstance.getProperties();
            String topologyURI = DriverPropertyValidator.validateAndReturn("topologyUri", configs, DriverPropertyType.Generic),
            	   executable = DriverPropertyValidator.validateAndReturn("retrieval-pattern", configs, DriverPropertyType.Command);
            
            List<String> executableParams = extractCmdParamNames(executable);
            Map<String, String[]> pullUtilArgMap = new HashMap<>();
            for (String param : executableParams) {
            	String args = DriverPropertyValidator.validateAndReturn(param, configs, DriverPropertyType.Generic, DriverPropertyType.GenericList);
            	pullUtilArgMap.put(param, args.split(delimiterPattern));
            }
            
            // Retrieve properties required to build model and validate field keys and values
            Map<String, List<Object>> endpointConfigurationMap = new HashMap<>();
            String idStr = DriverPropertyValidator.validateAndReturn("endpoint-IDs", configs, DriverPropertyType.Generic, DriverPropertyType.GenericList),
            	   loginNodeIPStr = DriverPropertyValidator.validateAndReturn("login-IPs", configs, DriverPropertyType.DottedQuad, DriverPropertyType.DottedQuadList),
            	   clusterNameStr = DriverPropertyValidator.validateAndReturn("canonical-names", configs, DriverPropertyType.Generic, DriverPropertyType.GenericList);
            
            // Map login node IDs, IPs and associated cluster IDs configs to login node, assemble pull utility command params on per-node basis and associate with node configs
            String[]
                    ids = idStr.split(delimiterPattern),
                    loginNodeIPs = loginNodeIPStr.split(delimiterPattern),
                    clusterNames = clusterNameStr.split(delimiterPattern);
            for (int i = 0; i < ids.length; ++i) {
            	// Splitting on delimiter pattern [\(\)] will create empty matches between consecutive ) and ( in all cases, so skip iteration
            	if (ids[i].isEmpty())
                    continue;
            	List<Object> mapped = new ArrayList<>();
            	mapped.add(loginNodeIPs[i]);
            	mapped.add(clusterNames[i]);
            	
            	Map<String, String> pullUtilParams = new HashMap<>();
            	for (String key : pullUtilArgMap.keySet()) {
                    String[] propertyVals = pullUtilArgMap.get(key);
                    pullUtilParams.put(key, propertyVals.length > 1 ? propertyVals[i] : propertyVals[0]);
            	}
            	mapped.add(pullUtilParams);
            	endpointConfigurationMap.put(ids[i], mapped);
            }
            
            // Create and persist new ontology (if changes have been made)
            OntModel ontModel = CCSNModelBuilder.createOntology(topologyURI, executable, endpointConfigurationMap);
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
            logger.log(Level.SEVERE, ex.getMessage());
            return new AsyncResult<>("FAILURE");
        }

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
