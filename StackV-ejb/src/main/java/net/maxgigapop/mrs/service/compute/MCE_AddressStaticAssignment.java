/*
/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2015

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

package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.DriverUtil;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_AddressStaticAssignment extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_AddressStaticAssignment.class.getName(), "MCE_AwsDxStitching");

    private static final String OSpec_Template
            = "{\n"
            + "	\"$$\": [\n"
            + "		{\n"
            + "			\"interface\": \"?vif?\",\n"
            + "			\"address\": \"?addr?\",\n"
            + "			\"address_type\": \"?type?\",\n"
            + "			\"#sparql\": \"SELECT DISTINCT ?vif ?addr ?type WHERE {?vif a nml:BidirectionalPort. "
            + "?vif mrs:hasNetworkAddress ?na. ?na mrs:value ?addr. ?na mrs:type ?type}\",\n"
            + "			\"#required\": \"false\"\n"
            + "		}\n"
            + "	]\n"
            + "}";

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        logger.cleanup();
        String method = "process";
        logger.refuuid(annotatedDelta.getReferenceUUID());
        logger.start(method);
        try {
            logger.trace(method, "DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(annotatedDelta.additionModel) -exception-"+ex);
        }
        
        Map<Resource, JSONObject> policyResDataMap = this.preProcess(policy, systemModel, annotatedDelta);        

        // Specific MCE logic 
        ServiceDelta outputDelta = annotatedDelta.clone();
        for (Resource res : policyResDataMap.keySet()) {
            OntModel stitchModel = this.assignAddresses(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), res, policyResDataMap.get(res));
            if (stitchModel != null) {
                outputDelta.getModelAddition().getOntModel().add(stitchModel.getBaseModel());
            }
        }
        
        this.postProcess(policy, outputDelta.getModelAddition().getOntModel(), systemModel.getOntModel(), OSpec_Template, policyResDataMap);

        try {
            logger.trace(method, "DeltaAddModel Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(outputDelta.additionModel) -exception-"+ex);
        }
        logger.end(method);
        return new AsyncResult(outputDelta);
    }

    //@TODO: Stitch ( VGW | VPC | Subnet? )to ( DcVx | L2Path )

    // General logic: 1. find the "terminal / end" containing resource (eg. Host Node or Topology)
    // 2. identify the "attach-point" resource (eg. VLAN port) along with a stitching path
    // 3. add statements to the stitching path to connect the terminal to the attach-point (if applicable)
    private OntModel assignAddresses(OntModel systemModel, OntModel spaModel, Resource res, JSONObject jsonAssignReq) {
        String method = "assignAddresses";
        logger.message(method, "@assignAddresses -> " + res);

        OntModel assignModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Model unionSysModel = spaModel.union(systemModel);

        if (!jsonAssignReq.containsKey("address_pool") || !jsonAssignReq.containsKey("assign_to")) {
            throw logger.error_throwing(method, "imported incomplete JSON data");
        }
        JSONArray jsonAddresses = (JSONArray)jsonAssignReq.get("address_pool");
        JSONArray jsonAssignments = (JSONArray) jsonAssignReq.get("assign_to");
        String addressType = jsonAssignReq.containsKey("address_type") ? (String) jsonAssignReq.get("address_type") : "ipv4-address";
        // for each jsonAssignment in the jsonAssignments array, get one address from jsonAddresses 
        for (Object obj: jsonAssignments) {
            JSONObject jsonAssignFilter = (JSONObject)obj;
            if (jsonAssignFilter.containsKey("l2path") && jsonAssignFilter.containsKey("terminals")) {
                JSONArray assignToPath = (JSONArray) jsonAssignFilter.get("l2path");
                JSONArray assignToTerminals = (JSONArray) jsonAssignFilter.get("terminals");
                List<String> addresses = this.checkoutAddresses(jsonAddresses, assignToTerminals.size()); 
                // exception if running out of addresses
                if (addresses == null) {
                   throw logger.error_throwing(method, String.format("%d addresses are requested but address pool has only %d", assignToTerminals.size(), jsonAddresses.size()) );
                }
                for (int i = 0; i < addresses.size(); i++) {
                    Resource resAssignToInterface = this.lookupL2PathTerminalInterface(unionSysModel, assignToPath, (String)assignToTerminals.get(i));
                    String addressUri = resAssignToInterface.getURI()+":"+addressType+"+"+DriverUtil.addressUriEscape(addresses.get(i));
                    Resource resAddress = RdfOwl.createResourceUnverifiable(assignModel, addressUri, Mrs.NetworkAddress); 
                    assignModel.add(assignModel.createStatement(resAddress, Mrs.type, addressType));
                    assignModel.add(assignModel.createStatement(resAddress, Mrs.value, addresses.get(i)));
                    assignModel.add(assignModel.createStatement(resAssignToInterface, Mrs.hasNetworkAddress, resAddress));
                }
            }
        }
        return assignModel;
    }
    
    private List<String> checkoutAddresses(List poolAddresses, int num) {
        List<String> listAddresses = new ArrayList();
        if (num > poolAddresses.size()) {
            return null;
        }
        for (int i = 0; i < num; i++) {
            listAddresses.add((String)poolAddresses.get(0));
            poolAddresses.remove(0);
        }
        return listAddresses;
    }

    private Resource lookupL2PathTerminalInterface(Model unionSysModel, JSONArray l2pathHops, String terminalUri) {
        String method = "lookupL2PathTerminalInterface";
        for (Object obj : l2pathHops) {
            JSONObject jsonObj = (JSONObject) obj;
            if (!jsonObj.containsKey("hop")) {
                throw logger.error_throwing(method, String.format("l2path has invalid hop: %s", jsonObj));
            }
            String hopUri = (String) jsonObj.get("hop");
            if (hopUri.equals(terminalUri)) {
                return unionSysModel.getResource(hopUri);
            }
            String sparql = "SELECT ?vif WHERE { {"
                    + "?terminal nml:hasBidirectionalPort ?port."
                    + "?port nml:hasBidirectionalPort ?vif."
                    + String.format("FILTER (?terminal = <%s> && ?vif = <%s>)", terminalUri, hopUri)
                    + "} UNION {"
                    + "?terminal nml:hasBidirectionalPort ?vif."
                    + String.format("FILTER (?terminal = <%s> && ?vif = <%s>)", terminalUri, hopUri)
                    + "} }";
            ResultSet r = ModelUtil.sparqlQuery(unionSysModel, sparql);
            if (r.hasNext()) {
                QuerySolution solution = r.nextSolution();
                return solution.getResource("vif");
            }
        }
        throw logger.error_throwing(method, String.format("cannot find an interface on terminal '%s' for l2path '%s' ", terminalUri, l2pathHops));
    }
}
