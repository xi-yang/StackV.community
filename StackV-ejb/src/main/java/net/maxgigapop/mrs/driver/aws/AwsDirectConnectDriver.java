/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2015-2016

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
package net.maxgigapop.mrs.driver.aws;

import net.maxgigapop.mrs.driver.aws.AwsModelBuilder;
import net.maxgigapop.mrs.driver.aws.AwsPush;
import net.maxgigapop.mrs.driver.aws.AwsDirectConnectDriver;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.directconnect.model.Connection;
import com.amazonaws.services.directconnect.model.VirtualInterface;
import com.amazonaws.services.directconnect.model.VirtualInterfaceState;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
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
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.ResourceTool;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;
import org.json.simple.JSONObject;

/**
 *
 * @author muzcategui
 */
//TODO make the stirng returned by the push.progate function to be in JSON format
// and adapt it to the driver
//TODO make request not to be in the database as a driver property, as they do not
//truly get deleted.
@Stateless
public class AwsDirectConnectDriver implements IHandleDriverSystemCall {

    public static final StackLogger logger = new StackLogger(AwsDirectConnectDriver.class.getName(), "AwsDirectConnectDriver");

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        String access_key_id = driverInstance.getProperty("aws_access_key_id");
        String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
        String r = driverInstance.getProperty("region");
        Regions region = Regions.fromName(r);
        String topologyURI = driverInstance.getProperty("topologyUri");
        String defaultVlanRange = driverInstance.getProperty("defaultVlanRange");
        OntModel model = driverInstance.getHeadVersionItem().getModelRef().getOntModel();
        OntModel modelAdd = aDelta.getModelAddition().getOntModel();
        OntModel modelReduc = aDelta.getModelReduction().getOntModel();
        String requests = this.pushPropagate(topologyURI, model, modelAdd, modelReduc);
        String requestId = driverInstance.getId().toString() + aDelta.getReferenceUUID().toString();
        driverInstance.putProperty(requestId, requests);
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
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        String requestId = driverInstance.getId().toString() + aDelta.getReferenceUUID().toString();
        String requests = driverInstance.getProperty(requestId);
        if (requests == null) {
            throw logger.error_throwing(method, "requests == null - trying to commit after propagate failed, requestId="+requestId);
        }
        if (requests.isEmpty()) {
            driverInstance.getProperties().remove(requestId);
            DriverInstancePersistenceManager.merge(driverInstance);
            logger.warning(method, "requests.isEmpty - no change to commit, requestId="+requestId);
        }        
        String access_key_id = driverInstance.getProperty("aws_access_key_id");
        String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
        String r = driverInstance.getProperty("region");
        Regions region = Regions.fromName(r);
        String topologyURI = driverInstance.getProperty("topologyUri");
        String defaultVlanRange = driverInstance.getProperty("defaultVlanRange");
        driverInstance.getProperties().remove(requestId);
        DriverInstancePersistenceManager.merge(driverInstance);
        try {
            AwsDCGet dcClient = new AwsDCGet(access_key_id, secret_access_key, region);
            this.pushCommit(dcClient, topologyURI, requests);
        } catch (com.amazonaws.AmazonServiceException ex) {
            throw logger.throwing(method, ex);
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
            String access_key_id = driverInstance.getProperty("aws_access_key_id");
            String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
            String r = driverInstance.getProperty("region");
            String topologyURI = driverInstance.getProperty("topologyUri");
            String defaultVlanRange = driverInstance.getProperty("defaultVlanRange");
            Regions region = Regions.fromName(r);
            AwsDCGet dcClient = new AwsDCGet(access_key_id, secret_access_key, region);
            OntModel ontModel = this.createOntology(dcClient, topologyURI, defaultVlanRange);

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
            throw logger.throwing(method, driverInstance + " failed AwsModelBuilder.createOntology", e);
        } catch (Exception ex) {
            throw logger.throwing(method, driverInstance + " failed pull model", ex);
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }
    
    private OntModel createOntology(AwsDCGet dcClient, String topologyURI, String defaultVlanRange) throws IOException {
        AwsPrefix awsPrefix = new AwsPrefix(topologyURI);
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        
        Resource resTopology = RdfOwl.createResource(model, topologyURI, Nml.Topology);
        
        for (Connection dc : dcClient.getConnections()) {
            Resource resDC = RdfOwl.createResource(model, ResourceTool.getResourceUri(dc.getConnectionId(), awsPrefix.directConnectService(), dc.getConnectionId()), Nml.BidirectionalPort);
            model.add(model.createStatement(resTopology, Nml.hasBidirectionalLink, resDC));
            model.add(model.createStatement(resDC, Nml.name, dc.getConnectionId()));
            model.add(model.createStatement(resDC, Nml.name, dc.getConnectionName()));
            Resource resVlanRange = RdfOwl.createResource(model, String.format(awsPrefix.labelGroup(), resDC.getURI(), "vlan-range"), Nml.LabelGroup);
            model.add(model.createStatement(resVlanRange, Nml.values, defaultVlanRange));
            model.add(model.createStatement(resDC, Nml.hasLabelGroup, resVlanRange));
            model.add(model.createStatement(resVlanRange, Nml.labeltype, RdfOwl.labelTypeVLAN));
            for (VirtualInterface vi : dcClient.getVirtualInterfaces()) {
                String vlanNum = Integer.toString(vi.getVlan());
                String virtualInterfaceState = vi.getVirtualInterfaceState();
                String[] invalidStates = {VirtualInterfaceState.Deleted.toString(), VirtualInterfaceState.Deleting.toString()};
                if ((Arrays.asList(invalidStates).contains(virtualInterfaceState))) {
                    continue;
                }
                if (vi.getConnectionId().equals(dc.getConnectionId())) {
                    Resource resVirtualIf = RdfOwl.createResource(model, ResourceTool.getResourceUri(vi.getVirtualInterfaceId(), awsPrefix.vif(), resDC.getURI(), vi.getVlan().toString()), Nml.BidirectionalPort);
                    model.add(model.createStatement(resVirtualIf, Nml.name, vi.getVirtualInterfaceId()));
                    model.add(model.createStatement(resVirtualIf, Mrs.type, "direct-connect-vif"));
                    model.add(model.createStatement(resVirtualIf, Mrs.value, "direct-connect-vif+" + vi.getVirtualInterfaceType()));
                    Resource resVirtualIfVlanRange = RdfOwl.createResource(model, ResourceTool.getResourceUri(vlanNum, awsPrefix.labelGroup(), resVirtualIf.getURI(), vlanNum), Nml.LabelGroup);
                    model.add(model.createStatement(resVirtualIfVlanRange, Nml.values, vlanNum));
                    model.add(model.createStatement(resVirtualIf, Nml.hasLabelGroup, resVirtualIfVlanRange));
                    model.add(model.createStatement(resVirtualIfVlanRange, Nml.labeltype, RdfOwl.labelTypeVLAN));
                    model.add(model.createStatement(resDC, Nml.hasBidirectionalPort, resVirtualIf));
                    // individual VLAN is considered "allocated" label under the DC connection
                    Resource resVirtualIfVlan = RdfOwl.createResource(model, ResourceTool.getResourceUri(vlanNum, awsPrefix.label(),resVirtualIf.getURI(),vlanNum), Nml.Label);
                    model.add(model.createStatement(resVirtualIfVlan, Nml.labeltype, RdfOwl.labelTypeVLAN));
                    model.add(model.createStatement(resVirtualIfVlan, Nml.value, vlanNum));
                    model.add(model.createStatement(resDC, Nml.hasLabel, resVirtualIfVlan));
                    // VLAN also considered as "allocated" under the Virtual Interface if associcated with a VGW
                    String[] acceptedStates = {VirtualInterfaceState.Available.toString(), VirtualInterfaceState.Deleting.toString(), 
                        VirtualInterfaceState.Pending.toString(), VirtualInterfaceState.Verifying.toString(), "down"};
                    if(vi.getVirtualGatewayId() != null && (Arrays.asList(acceptedStates).contains(virtualInterfaceState)))
                    {
                        model.add(model.createStatement(resVirtualIf, Nml.hasLabel, resVirtualIfVlan));
                    }
                    // Layer 3 properties
                    Resource vifAsn = RdfOwl.createResource(model, resVirtualIf.getURI() + ":asn", Mrs.NetworkAddress);
                    model.add(model.createStatement(resVirtualIf, Mrs.hasNetworkAddress, vifAsn));
                    model.add(model.createStatement(vifAsn, Mrs.type, "bgp-asn"));
                    model.add(model.createStatement(vifAsn, Mrs.value, vi.getAsn().toString()));
                    if (vi.getAmazonAddress() != null && !vi.getAmazonAddress().isEmpty()) {
                        Resource vifAmazonIp = RdfOwl.createResource(model, resVirtualIf.getURI() + ":amazon_ip", Mrs.NetworkAddress);
                        model.add(model.createStatement(resVirtualIf, Mrs.hasNetworkAddress, vifAmazonIp));
                        model.add(model.createStatement(vifAmazonIp, Mrs.type, "ipv4-address:amazon"));
                        model.add(model.createStatement(vifAmazonIp, Mrs.value, vi.getAmazonAddress()));
                    }
                    if (vi.getCustomerAddress() != null && !vi.getCustomerAddress().isEmpty()) {
                        Resource vifCustomerIp = RdfOwl.createResource(model, resVirtualIf.getURI() + ":customer_ip", Mrs.NetworkAddress);
                        model.add(model.createStatement(resVirtualIf, Mrs.hasNetworkAddress, vifCustomerIp));
                        model.add(model.createStatement(vifCustomerIp, Mrs.type, "ipv4-address:customer"));
                        model.add(model.createStatement(vifCustomerIp, Mrs.value, vi.getCustomerAddress()));
                    }
                    if (vi.getAuthKey() != null && !vi.getAuthKey().isEmpty()) {
                        Resource vifBgpAuthKey = RdfOwl.createResource(model, resVirtualIf.getURI() + ":bgp_authkey", Mrs.NetworkAddress);
                        model.add(model.createStatement(resVirtualIf, Mrs.hasNetworkAddress, vifBgpAuthKey));
                        model.add(model.createStatement(vifBgpAuthKey, Mrs.type, "bgp-authkey"));
                        model.add(model.createStatement(vifBgpAuthKey, Mrs.value, vi.getAuthKey()));
                    }
                    // owner account info
                    if (vi.getOwnerAccount()!= null && !vi.getOwnerAccount().isEmpty()) {
                        Resource vifOwnerAccount = RdfOwl.createResource(model, resVirtualIf.getURI() + ":owner_account", Mrs.NetworkAddress);
                        model.add(model.createStatement(resVirtualIf, Mrs.hasNetworkAddress, vifOwnerAccount));
                        model.add(model.createStatement(vifOwnerAccount, Mrs.type, "owner-account"));
                        model.add(model.createStatement(vifOwnerAccount, Mrs.value, vi.getOwnerAccount()));
                    }

                }
            }
        }
        return model;
    }

    private String pushPropagate(String topologyURI, OntModel modelRef, OntModel modelAdd, OntModel modelReduct) {
        JSONObject jsonRequests = new JSONObject();
        return jsonRequests.toJSONString();
    }

    private void pushCommit(AwsDCGet dcClient, String topologyURI, String requests) {

    }

}
