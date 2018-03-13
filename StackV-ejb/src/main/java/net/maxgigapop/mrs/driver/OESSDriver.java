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
package net.maxgigapop.mrs.driver;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.DriverUtil;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.ResourceTool;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author muzcategui
 */
@Stateless
public class OESSDriver implements IHandleDriverSystemCall {

    public static final StackLogger logger = new StackLogger(OESSDriver.class.getName(), "OESSDriver");

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        String topologyUri = driverInstance.getProperty("topologyUri");
        if (topologyUri == null) {
            throw logger.error_throwing(method, driverInstance + "has no property key=topologyUri");
        }
        OntModel model = driverInstance.getHeadVersionItem().getModelRef().getOntModel();
        OntModel modelAdd = aDelta.getModelAddition().getOntModel();
        OntModel modelReduc = aDelta.getModelReduction().getOntModel();
        String requests = this.pushPropagate(topologyUri, model, modelAdd, modelReduc);
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
        try {
            String baseUrl = driverInstance.getProperty("subsystemBaseUrl");
            if (baseUrl == null) {
                throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
            }
            String username = driverInstance.getProperty("apiUsername");
            if (username == null) {
                throw logger.error_throwing(method, driverInstance +"has no property key=apiUsername");
            }
            String password = driverInstance.getProperty("apiPassword");
            if (username == null) {
                throw logger.error_throwing(method, driverInstance +"has no property key=apiPassword");
            }
            String defaultGroup = driverInstance.getProperty("defaultGroup"); // it can be null
            driverInstance.getProperties().remove(requestId);
            DriverInstancePersistenceManager.merge(driverInstance);
            this.pushCommit(baseUrl, username, password, defaultGroup, requests);
        } catch (com.amazonaws.AmazonServiceException ex) {
            throw logger.throwing(method, ex);
        }
        logger.end(method);
        return new AsyncResult<String>("SUCCESS");
    }

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
            String topologyUri = driverInstance.getProperty("topologyUri");
            if (topologyUri == null) {
                throw logger.error_throwing(method, driverInstance +"has no property key=topologyUri");
            }
            String baseUrl = driverInstance.getProperty("subsystemBaseUrl");
            if (baseUrl == null) {
                throw logger.error_throwing(method, driverInstance +"has no property key=subsystemBaseUrl");
            }
            String username = driverInstance.getProperty("apiUsername");
            if (username == null) {
                throw logger.error_throwing(method, driverInstance +"has no property key=apiUsername");
            }
            String password = driverInstance.getProperty("apiPassword");
            if (username == null) {
                throw logger.error_throwing(method, driverInstance +"has no property key=apiPassword");
            }
            String defaultGroup = driverInstance.getProperty("defaultGroup"); // it can be null
            JSONObject jsonExt = null;
            String modelExt = driverInstance.getProperty("modelExt");
            if (modelExt != null) {
                jsonExt = (JSONObject) JSONValue.parseWithException(modelExt);
            }
            OntModel ontModel = this.createOntology(topologyUri, baseUrl, username, password, defaultGroup, jsonExt);

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
            throw logger.throwing(method, driverInstance + " failed to createOntology - ", e);
        } catch (Exception ex) {
            throw logger.throwing(method, driverInstance + " failed to pull model - ", ex);
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }
    
    private OntModel createOntology(String topologyURI, String baseUrl, String username, String password, String workgroup, JSONObject jsonExt) throws IOException {
        String method = "createOntology";
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Resource resTopology = RdfOwl.createResource(model, topologyURI, Nml.Topology);
        Resource resSwSvc = RdfOwl.createResource(model, resTopology.getURI() + ":l2switching", Nml.SwitchingService);
        model.add(model.createStatement(resSwSvc, Nml.encoding, RdfOwl.labelTypeVLAN));
        model.add(model.createStatement(resSwSvc, Nml.labelSwapping, "true"));
        model.add(model.createStatement(resTopology, Nml.hasService, resSwSvc));
        // get a valid group_id if defaultGroup == null
        if (workgroup == null) {
            workgroup = getGroupId(baseUrl, username, password);
            if (workgroup == null) {
                throw logger.error_throwing(method, "failed to get OESS workgroup for user: " + username);
            }
        }
        String url = baseUrl + "/data.cgi?action=get_all_resources_for_workgroup&workgroup_id=" + workgroup;
        String[] response = DriverUtil.executeHttpMethod(username, password, url, "GET", null);
        JSONObject jsonResponse;
        try {
            jsonResponse = (JSONObject) JSONValue.parseWithException(response[0]);
        } catch (ParseException ex) {
            logger.catching(method, ex);
            return null;
        }
        JSONArray jsonResults = (JSONArray) jsonResponse.get("results");
        for (Object obj : jsonResults) {
            JSONObject jsonIntf = (JSONObject) obj;
            if (jsonIntf.get("operational_state").equals("up")) {
                String nodeName = (String) jsonIntf.get("node_name");
                String intfName = (String) jsonIntf.get("interface_name");
                String intfUrn = topologyURI + ":node+" + nodeName + ":port+" + intfName.replaceAll("/", "_");
                String vlanRange = (String) jsonIntf.get("vlan_tag_range");
                Resource resPort = RdfOwl.createResource(model, intfUrn, Nml.BidirectionalPort);
                model.add(model.createStatement(resPort, Nml.name, intfName));
                model.add(model.createStatement(resTopology, Nml.hasBidirectionalPort, resPort));
                model.add(model.createStatement(resSwSvc, Nml.hasBidirectionalPort, resPort));
                if (vlanRange != null && !vlanRange.isEmpty()) {
                    Resource resLabelGroup = RdfOwl.createResource(model, resPort.getURI()+":vlan-range", Nml.LabelGroup);
                    model.add(model.createStatement(resPort, Nml.hasLabelGroup, resLabelGroup));
                    model.add(model.createStatement(resLabelGroup, Nml.labeltype, RdfOwl.labelTypeVLAN));
                    model.add(model.createStatement(resLabelGroup, Nml.values, vlanRange));
                }
                Resource resNaNodeName = RdfOwl.createResource(model, intfUrn+":node_name", Mrs.NetworkAddress);
                model.add(model.createStatement(resPort, Mrs.hasNetworkAddress, resNaNodeName));
                model.add(model.createStatement(resNaNodeName, Mrs.type, "node_name"));
                model.add(model.createStatement(resNaNodeName, Mrs.value, nodeName));
                // get peer URI for resPort
                String peerUri = this.lookupAliasUri(jsonExt, nodeName, intfName);
                if (peerUri != null) {
                    Resource resPeer = model.createResource(peerUri);
                    model.add(model.createStatement(resPort, Nml.isAlias, resPeer));
                }
            }
        }
        url = baseUrl + "/data.cgi?action=get_existing_circuits&workgroup_id=" + workgroup;
        response = DriverUtil.executeHttpMethod(username, password, url, "GET", null);
        try {
            jsonResponse = (JSONObject) JSONValue.parseWithException(response[0]);
        } catch (ParseException ex) {
            logger.catching(method, ex);
            return null;
        }
        jsonResults = (JSONArray) jsonResponse.get("results");
        for (Object obj : jsonResults) {
            JSONObject jsonCircuit = (JSONObject) obj;
            String circuitId = (String) jsonCircuit.get("circuit_id");
            String description = (String) jsonCircuit.get("description");
            Resource resSubnet = RdfOwl.createResource(model, ResourceTool.getResourceUri(description, "%s:circuit+%s", resSwSvc.getURI(), circuitId), Mrs.SwitchingSubnet);
            model.add(model.createStatement(resSwSvc, Mrs.providesSubnet, resSubnet));
            model.add(model.createStatement(resSubnet, Nml.encoding, RdfOwl.labelTypeVLAN));
            model.add(model.createStatement(resSubnet, Nml.labelSwapping, "true"));
            Resource resNaCircuitId = RdfOwl.createResource(model, resSubnet.getURI() + ":circuit_id", Mrs.NetworkAddress);
            model.add(model.createStatement(resSubnet, Mrs.hasNetworkAddress, resNaCircuitId));
            model.add(model.createStatement(resNaCircuitId, Mrs.type, "circuit-id"));
            model.add(model.createStatement(resNaCircuitId, Mrs.value, circuitId));
            String circuitName = (String) jsonCircuit.get("name");
            if (circuitName != null && !circuitName.isEmpty()) {
                model.add(model.createStatement(resSubnet, Nml.name, circuitName));
            }
            JSONArray jsonEndpoints = (JSONArray) jsonCircuit.get("endpoints"); 
            for (Object obj2 : jsonEndpoints) {
                JSONObject jsonEndpoint = (JSONObject) obj2;
                String nodeName = (String) jsonEndpoint.get("node");
                String intfName = (String) jsonEndpoint.get("interface");
                String intfUrn = topologyURI + ":node+" + nodeName + ":port+" + intfName.replaceAll("/", "_");
                String vlanTag = (String) jsonEndpoint.get("tag");
                Resource resPort = RdfOwl.createResource(model, intfUrn, Nml.BidirectionalPort);
                Resource resVlanPort = RdfOwl.createResource(model, intfUrn+":vlanport+"+vlanTag, Nml.BidirectionalPort);
                model.add(model.createStatement(resPort, Nml.hasBidirectionalPort, resVlanPort));
                model.add(model.createStatement(resSubnet, Nml.hasBidirectionalPort, resVlanPort));
                if (vlanTag != null && !vlanTag.isEmpty()) {
                    Resource resLabel = RdfOwl.createResource(model, resVlanPort.getURI()+":label+"+vlanTag, Nml.LabelGroup);
                    model.add(model.createStatement(resVlanPort, Nml.hasLabel, resLabel));
                    model.add(model.createStatement(resLabel, Nml.labeltype, RdfOwl.labelTypeVLAN));
                    model.add(model.createStatement(resLabel, Nml.values, vlanTag));
                }
            }
            //@TODO: add bandwidth service
        }
        return model;
    }

    private String getGroupId(String baseUrl, String username, String password) throws IOException {
        String[] response = DriverUtil.executeHttpMethod(username, password, baseUrl+"/data.cgi?method=get_workgroups", "GET", null);
        JSONObject jsonResponse;
        try {
            jsonResponse = (JSONObject) JSONValue.parseWithException(response[0]);
        } catch (ParseException ex) {
            logger.catching("getGroupId", ex);
            return null;
        }
        if (jsonResponse.containsKey("results") && jsonResponse.get("results") != null) {
            JSONArray jsonGroups = (JSONArray)jsonResponse.get("results");
            if (!jsonGroups.isEmpty()) {
                return ((JSONObject)jsonGroups.get(0)).get("workgroup_id").toString();
            }
        }
        return null;        
    }
    
    private String lookupAliasUri(JSONObject jsonExt, String nodeName, String intfName) {
        if (!jsonExt.containsKey("edgeports")) {
            return null;
        }
        JSONArray jEdgeports = (JSONArray) jsonExt.get("edgeports");
        for (Object obj: jEdgeports) {
            JSONObject jEdgeport = (JSONObject) obj;
            if (jEdgeport.get("node").equals(nodeName) && jEdgeport.get("interface").equals(intfName) && jEdgeport.containsKey("peer")) {
                return (String) jEdgeport.get("peer");
            }
        }
        return null;
    }
    
    private String pushPropagate(String topologyURI, OntModel modelRef, OntModel modelAdd, OntModel modelReduct) {
        String method = "pushPropagate";
        JSONArray jsonRequests = new JSONArray();
        // delete VLAN circuit scenario 1: 
        String query = "SELECT DISTINCT ?subnet ?circuit_id WHERE {"
                + "?swsvc mrs:providesSubnet ?subnet ."
                + String.format("?subnet nml:encoding <%s>. ", RdfOwl.labelTypeVLAN.getURI())
                + "?subnet mrs:hasNetworkAddress ?na. "
                + "?na mrs:type \"circuit-id\".  "
                + "?na mrs:value ?circuit_id . "
                + "}";
        ResultSet r = ModelUtil.executeQuery(query, null, modelReduct);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            JSONObject jsonReq = new JSONObject();
            jsonReq.put("command", "deleteCircuit");
            Resource resSubnet = q.get("subnet").asResource();
            String circuitId = q.get("circuit_id").asLiteral().getString();
            jsonReq.put("subnet_uri", resSubnet.getURI());
            jsonReq.put("circuit_id", circuitId);
            jsonRequests.add(jsonReq);
        }
        
        //@TODO: delete VLAN scenario 2: identify circuit by matching port and vlan
        
        // add VLAN circuit
        query = "SELECT DISTINCT ?subnet WHERE {"
                + "?swsvc mrs:providesSubnet ?subnet ."
                + String.format("?subnet nml:encoding <%s>. ", RdfOwl.labelTypeVLAN.getURI())
                + "}";
        r = ModelUtil.executeQuery(query, null, modelAdd);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            JSONObject jsonReq = new JSONObject();
            jsonReq.put("command", "addCircuit");
            Resource resSubnet = q.get("subnet").asResource();
            jsonReq.put("subnet_uri", resSubnet.getURI());
            query = "SELECT DISTINCT ?node_name ?port_name ?vlan_tag ?mac_list WHERE {"
                + String.format("<%s> nml:hasBidirectionalPort ?vlanport. ", resSubnet.getURI())
                + "?port nml:hasBidirectionalPort ?vlanport . "
                + "?port a nml:BidirectionalPort . "
                + "?port nml:name ?port_name . "
                + "?port mrs:hasNetworkAddress ?na_node . "
                + "?na_node mrs:type \"node_name\". "
                + "?na_node mrs:value ?node_name. "
                + "?vlanport nml:hasLabel ?a_label. "
                + String.format("?a_label nml:labeltype <%s>. ", RdfOwl.labelTypeVLAN.getURI())
                + "?a_label nml:value ?vlan_tag. "
                + "OPTIONAL {"
                + " ?vlanport mrs:hasNetworkAddress ?na_mac_list. "
                + " ?na_mac_list mrs:type \"mac-address-list\". "
                + " ?na_mac_list mrs:value ?mac_list. "
                + "}"
                + "}";
            ResultSet r2 = ModelUtil.executeQueryUnion(query, modelRef, modelAdd);
            jsonReq.put("endpoints", new JSONArray());
            while (r2.hasNext()) {
                QuerySolution q2 = r2.next();
                JSONObject jsonEndpoint = new JSONObject();
                String nodeName = q2.get("node_name").asLiteral().getString();
                String portName = q2.get("port_name").asLiteral().getString();
                String vlanTag = q2.get("vlan_tag").asLiteral().getString();
                jsonEndpoint.put("node_name", nodeName);
                jsonEndpoint.put("port_name", portName);
                jsonEndpoint.put("vlan_tag", vlanTag);
                if (q2.contains("mac_list")) {
                    String macList = q2.get("mac_list").asLiteral().getString();
                    jsonEndpoint.put("mac_list", macList);
                }
                ((JSONArray)jsonReq.get("endpoints")).add(jsonEndpoint);
            }
            if (((JSONArray)jsonReq.get("endpoints")).size() < 2) {
                throw logger.error_throwing(method, "a VLAN circuit subnet needs at least two endpoints");
            }
            //@TODO: support bandwidth service
            jsonRequests.add(jsonReq);
        }

        return jsonRequests.toJSONString();
    }

    private void pushCommit(String baseUrl, String username, String password, String workgroup, String requests) {
        String method = "pushCommit";
        JSONParser jsonParser = new JSONParser();
        JSONArray jRequests = null;
        try {
            jRequests = (JSONArray) jsonParser.parse(requests);
        } catch (ParseException ex) {
            throw logger.throwing(method, "failed to parse  JSON requests=" + requests, ex);
        }
        // get a valid group_id if defaultGroup == null
        if (workgroup == null) {
            try {
                workgroup = getGroupId(baseUrl, username, password);
            } catch (IOException ex) {
                throw logger.error_throwing(method, ex + " - failed to get OESS workgroup for user: " + username);
            }
            if (workgroup == null) {
                throw logger.error_throwing(method, "unknown OESS workgroup for user: " + username);
            }
        }
        for (Object obj: jRequests) {
            JSONObject jReq = (JSONObject) obj;
            String command = (String)jReq.get("command");
            if (command.equals("deleteCircuit")) {
                String url = baseUrl + "/provisioning.cgi?action=remove_circuit&circuit_id=" + jReq.get("circuit_id") + "&remove_time=-1&workgroup_id=" + workgroup;
                try {
                    String[] response = DriverUtil.executeHttpMethod(username, password, url, "GET", null);
                    JSONObject jsonResponse;
                    try {
                        jsonResponse = (JSONObject) JSONValue.parseWithException(response[0]);
                    } catch (ParseException ex) {
                        throw logger.throwing(method, ex);
                    }
                    if (jsonResponse.containsKey("error") && ((Long)jsonResponse.get("error")) == 1) {
                        throw logger.error_throwing(method, jsonResponse.get("error_text") + " for subnet: " + jReq.get("subnet_uri"));
                    }
                    JSONArray jsonResults = (JSONArray) jsonResponse.get("results");
                    if (jsonResults.isEmpty() || !((JSONObject)jsonResults.get(0)).containsKey("success") || ((Long)((JSONObject)jsonResults.get(0)).get("success")) != 1) {
                        throw logger.error_throwing(method, "remove_circuit call failed for circuit: "+jReq.get("circuit_id"));
                    }
                } catch (IOException ex) {
                    throw logger.throwing(method, ex);
                }
            } else if (command.equals("addCircuit")) {
                String bandwidth = jReq.containsKey("bandwidth") ? (String) jReq.get("bandwidth") : "0";
                String description;
                try {
                    description = URLEncoder.encode((String)jReq.get("subnet_uri"), "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    throw logger.error_throwing(method, ex + " - malformed subnet URI: " + (String)jReq.get("subnet_uri"));
                }
                String provUrl = baseUrl + "/provisioning.cgi?action=provision_circuit&circuit_id=-1"
                        + "&description=" + description
                        + "&bandwidth=" + bandwidth + "&provision_time=-1&remove_time=-1&workgroup_id=" + workgroup;
                String pathUrl = baseUrl + "/data.cgi?action=get_shortest_path&type=mpls";
                JSONArray jsonEndpoints = (JSONArray) jReq.get("endpoints");
                for (Object obj2 : jsonEndpoints) {
                    JSONObject jEp = (JSONObject) obj2;
                    provUrl += "&node=" + jEp.get("node_name");
                    pathUrl += "&node=" + jEp.get("node_name");
                    provUrl += "&interface=" + jEp.get("port_name");
                    pathUrl += "&interface=" + jEp.get("port_name");
                    provUrl += "&tag=" + jEp.get("vlan_tag");
                    pathUrl += "&tag=" + jEp.get("vlan_tag");
                }
                try {
                    // compute shortest path
                    String[] response = DriverUtil.executeHttpMethod(username, password, pathUrl, "GET", null);
                    JSONObject jsonResponse;
                    try {
                        jsonResponse = (JSONObject) JSONValue.parseWithException(response[0]);
                    } catch (ParseException ex) {
                        throw logger.throwing(method, ex);
                    }
                    if (jsonResponse.containsKey("error") && ((Long)jsonResponse.get("error")) == 1) {
                        throw logger.error_throwing(method, jsonResponse.get("error_text") + " for subnet: " + jReq.get("subnet_uri"));
                    }
                    JSONArray jsonResults = (JSONArray) jsonResponse.get("results");
                    if (jsonResults == null || jsonResults.isEmpty()) {
                        throw logger.error_throwing(method, "empty results from get_shortest_path call for subnet: " + jReq.get("subnet_uri"));
                    }
                    for (Object obj3 : jsonResults) {
                        JSONObject jLink = (JSONObject) obj3;
                        provUrl += "&link=" + jLink.get("link");
                    }
                    //@TODO: support mac address array (endpoint_mac_address_num Nx, mac_address Nx{multiple})
                    
                    // provision path
                    response = DriverUtil.executeHttpMethod(username, password, provUrl, "GET", null);
                    try {
                        jsonResponse = (JSONObject) JSONValue.parseWithException(response[0]);
                    } catch (ParseException ex) {
                        throw logger.throwing(method, ex);
                    }
                    if (jsonResponse.containsKey("error") && ((Long)jsonResponse.get("error")) == 1) {
                        throw logger.error_throwing(method, jsonResponse.get("error_text") + " for subnet: " + jReq.get("subnet_uri"));
                    }
                    JSONObject jsonResult = (JSONObject) jsonResponse.get("results");
                    if (!jsonResult.containsKey("success") || ((Long) jsonResult.get("success")) != 1) {
                        throw logger.error_throwing(method, "provision_circuit call failed for subnet: " + jReq.get("subnet_uri"));
                    }
                } catch (IOException ex) {
                    throw logger.throwing(method, ex);
                }
            }
        }
    }

}
