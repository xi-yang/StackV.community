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
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
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

public class MCEBase implements IModelComputationElement {
    
    protected static final StackLogger logger = new StackLogger(MCEBase.class.getName(), "MCEBase");

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        throw logger.error_throwing("process", "Abstract method - cannot be called directly.");
    }

    protected Map<Resource, JSONObject> preProcess(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        String method = this.getClass().getSimpleName()+".preProcess";
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw logger.error_throwing(method, "target:ServiceDelta has null addition model for " + policy);
        }
        try {
            logger.trace(method, "DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(annotatedDelta.additionModel) -exception-"+ex);
        }
        // list resources depending on the current policy
        String sparql = "SELECT DISTINCT ?res WHERE {"
                + "?res spa:dependOn ?policy . "
                + "?policy a spa:PolicyAction. "
                + String.format("?policy spa:type '%s'. ", this.getClass().getSimpleName())
                + String.format("FILTER (not exists {?policy spa:dependOn ?other} && ?policy = <%s>)", policy.getURI())
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        List<Resource> listRes = new ArrayList();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource res = querySolution.get("res").asResource();
            listRes.add(res);
        }
        if (listRes.isEmpty()) {
            throw logger.error_throwing(method, "SPA model incorrectly composed: none reqsource dpending on policy: " + policy);
        }
        // list data the current policy imports
        sparql = "SELECT DISTINCT ?data ?type ?value WHERE {"
                + "?policy a spa:PolicyAction. "
                + String.format("?policy spa:type '%s'. ", this.getClass().getSimpleName())
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?type. ?data spa:value ?value. "
                + String.format("FILTER (not exists {?policy spa:dependOn ?other} && ?policy = <%s>)", policy.getURI())
                + "}";
        
        r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        List listData = new ArrayList();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resData = querySolution.get("data").asResource();
            RDFNode nodeDataType = querySolution.get("type");
            RDFNode nodeDataValue = querySolution.get("value");
            if (!nodeDataType.toString().equalsIgnoreCase("JSON")) {
                logger.warning(method, "import policy data: `" + resData + "' forcefully in JSON format");
            }
            listData.add(nodeDataValue.toString());
        }
        JSONObject jsonData;
        if (listData.isEmpty()) {
            //@TODO: use defaults from INPUT_Template ?
            throw logger.error_throwing(method, "SPA model incorrectly composed: none policy data imported by policy: " + policy);
        } else {
            try {
                jsonData = consolidateJsonData(listData);
            } catch (ParseException ex) {
                throw logger.throwing(method, ex);
            }
        }
        Map<Resource, JSONObject> policyResDataMap = new HashMap<>();
        if (listRes.size() == 1 && !jsonData.containsKey(listRes.get(0).toString())) {
            policyResDataMap.put(listRes.get(0), jsonData);
        } else {
            for (Resource res: listRes) {
                if (!jsonData.containsKey(res.toString())) {
                    throw logger.error_throwing(method, "SPA model incorrectly composed: resource: '" + res + "' has no correcponding entry in policy data");
                }
                if (!(jsonData.get(res.toString()) instanceof JSONObject)) {
                    throw logger.error_throwing(method, "SPA model incorrectly composed: resource: '" + res + "' has no correcponding entry (in JSON) in policy data");
                }
                policyResDataMap.put(res, (JSONObject)jsonData.get(res.toString()));
            }
        }
        return policyResDataMap;
    }
    
    private JSONObject consolidateJsonData(List<String> listJson) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonAll = null;
        for (String json : listJson) {
            JSONObject jsonObj = (JSONObject) parser.parse(json);
            if (jsonAll == null) {
                jsonAll = jsonObj;
            } else { // merge
                for (Object key : jsonObj.keySet()) {
                    jsonAll.put(key, jsonObj.get(key));
                }
            }
        }
        return jsonAll;
    }
    
    protected void postProcess(Resource policy, Map<Resource, JSONObject> policyResDataMap, OntModel spaModel, String outputTemplate) {
        String method = this.getClass().getSimpleName()+".postProcess";
        
        String outputJson = outputPolicyData(spaModel, outputTemplate);
        
        exportPolicyData(policy, spaModel, outputJson);
        
        MCETools.removeResolvedAnnotation(spaModel, policy); //@TODO: move from MCETools in here
        try {
            logger.trace(method, "DeltaAddModel Output=\n" + ModelUtil.marshalOntModel(spaModel));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(outputDelta.additionModel) -exception-"+ex);
        }
    }

    //@TODO genreate output JSON from resulting spaModel and  outputTemplate         
    protected String outputPolicyData(OntModel spaModel, String outputTemplate) {
        String method = this.getClass().getSimpleName()+".outputPolicyData";
        return null;
    }
    
    protected void exportPolicyData(Resource policy, OntModel spaModel, String outputJson) {
        String method = this.getClass().getSimpleName()+".exportPolicyData";
        String sparql = "SELECT DISTINCT ?data ?type ?value ?format WHERE {"
                + String.format("<%s> a spa:PolicyAction. ", policy)
                + String.format("<%s> spa:type '%s'. ", policy, this.getClass().getSimpleName())
                + String.format("<%s> spa:exportTo ?data . ", policy)
                + "OPTIONAL {?data spa:type ?type.} "
                + "OPTIONAL {?data spa:value ?value.} "
                + "OPTIONAL {?data spa:format ?format.} "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resData = querySolution.get("data").asResource();
            RDFNode dataType = querySolution.get("type");
            RDFNode dataValue = querySolution.get("value");

            if (dataType == null) {
                spaModel.add(resData, Spa.type, "JSON");
            } else if (!dataType.toString().equalsIgnoreCase("JSON")) {
                logger.warning(method, "export policy data: `" + resData + "' forcefully in JSON format");
            }
            
            if (querySolution.contains("format")) {
                String exportFormat = querySolution.get("format").toString();
                try {
                    outputJson = MCETools.formatJsonExport(outputJson, exportFormat); //@TODO: move from MCETools in here
                } catch (Exception ex) {
                    logger.warning(method, "formatJsonExport exception and ignored: "+ ex);
                    continue;
                }
            }
            if (dataValue != null) {
                spaModel.remove(resData, Spa.value, dataValue);
            }
            spaModel.add(resData, Spa.value, outputJson);
        }
    }
}