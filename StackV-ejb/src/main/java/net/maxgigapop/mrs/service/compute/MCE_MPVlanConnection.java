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

package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONObject;

/**
 *
 * @author xyang
 */

/**
 * Simple L2 connection that will create new SwitchingSubnet on every transit switching node.
 */

@Stateless
public class MCE_MPVlanConnection extends MCEBase {

    private static final StackLogger logger = new StackLogger(MCE_MPVlanConnection.class.getName(), "MCE_MPVlanConnection");
    
    private static final String OSpec_Template
            = "{\n"
            + "	\"$$\": [\n"
            + "		{\n"
            + "			\"hop\": \"?hop?\",\n"
            + "			\"vlan_tag\": \"?vid?\",\n"
            + "			\"#sparql\": \"SELECT DISTINCT ?hop ?vid WHERE {?hop a nml:BidirectionalPort. "
            + "?hop nml:hasLabel ?vlan. ?vlan nml:value ?vid. ?hop mrs:tag \\\"l2path+$$:%%\\\".}\"\n"
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

        // Specific MCE logic - compute a List<Model> of MPVlan connections
        ServiceDelta outputDelta = annotatedDelta.clone();
        for (Resource res: policyResDataMap.keySet()) {
            Map<String, MCETools.Path> l2pathMap = this.doMultiPathFinding(systemModel.getOntModel(), res, policyResDataMap.get(res));
            for (String connId : l2pathMap.keySet()) {
                outputDelta.getModelAddition().getOntModel().add(l2pathMap.get(connId).getOntModel().getBaseModel());
            }
        }
        
        this.postProcess(policy, outputDelta.getModelAddition().getOntModel(), systemModel.getOntModel(), OSpec_Template, policyResDataMap);
        
        try {
            logger.message(method, "DeltaAddModel Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.message(method, "marshalOntModel(outputDelta.additionModel) -exception-"+ex);
        }
        logger.end(method);        
        return new AsyncResult(outputDelta);
    }

    private Map<String, MCETools.Path> doMultiPathFinding(OntModel systemModel, Resource resConn, Map<String, JSONObject> connDataMap) {
        String method = "doMultiPathFinding";
        // transform network graph
        // filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2NetworkModel(systemModel);
        
        try {
            logger.trace(method, "\n>>>MCE_MPVlanConnection--SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            logger.trace(method, "marshalModel(transformedModel) failed -- "+ex);
        }
        
        Map<String, MCETools.Path> mapConnPaths = new HashMap<>();
        for (String connId: connDataMap.keySet()) {
            List<Resource> terminals = new ArrayList<>();
            JSONObject jsonConnReq = (JSONObject)connDataMap.get(connId);
            if (jsonConnReq.size() != 2) {
                throw logger.error_throwing(method, String.format("cannot find path for connection '%s' - request must have exactly 2 terminals", connId));
            }
            for (Object key : jsonConnReq.keySet()) {
                Resource terminal = systemModel.getResource((String) key);
                if (!systemModel.contains(terminal, null)) {
                    throw logger.error_throwing(method, String.format("cannot identify terminal <%s> in JSON data", key));
                }
                terminals.add(terminal);
            }
            Resource nodeA = terminals.get(0);
            Resource nodeZ = terminals.get(1);
            // KSP-MP path computation on the connected graph model (point2point for now - will do MP in future)
            List<MCETools.Path> KSP;
            try {
                KSP = MCETools.computeFeasibleL2KSP(transformedModel, nodeA, nodeZ, jsonConnReq);
            } catch (Exception ex) {
                throw logger.throwing(method, String.format("connectionId=%s computeFeasibleL2KSP(nodeA=%s, nodeZ=%s, jsonConnReq=%s) exception -- ", connId, nodeA, nodeZ, jsonConnReq), ex);
            }
            if (KSP == null || KSP.size() == 0) {
                throw logger.error_throwing(method, String.format("cannot find feasible path for connection '%s'", connId));
            }
            // pick the shortest path from remaining/feasible paths in KSP
            MCETools.Path connPath = MCETools.getLeastCostPath(KSP);
            MCETools.tagPathHops(connPath, "l2path+"+resConn.getURI()+":"+connId+"");
            transformedModel.add(connPath.getOntModel());
            mapConnPaths.put(connId, connPath);
        }
        return mapConnPaths;
    }
}