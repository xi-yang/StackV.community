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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    
    protected void postProcess(Resource policy, OntModel spaModel, OntModel modelRef, String outputTemplate, Map<Resource, JSONObject> policyResDataMap) {
        String method = this.getClass().getSimpleName()+".postProcess";

        String outputJson = outputPolicyData(spaModel, modelRef, outputTemplate, policyResDataMap);
        
        exportPolicyData(policy, spaModel, outputJson);
        
        MCETools.removeResolvedAnnotation(spaModel, policy); //@TODO: move from MCETools in here
    }

    //@TODO genreate output JSON from resulting spaModel and  outputTemplate         
    protected String outputPolicyData(OntModel spaModel, OntModel modelRef, String outputTemplate, Map<Resource, JSONObject> policyResDataMap) {
        String method = this.getClass().getSimpleName()+".outputPolicyData";
        JSONObject retJO = (JSONObject)querySparsqlTemplateJson(spaModel, modelRef, outputTemplate, policyResDataMap);
        return retJO.toJSONString();
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
            String exportValue = outputJson;
            if (querySolution.contains("format")) {
                String exportFormat = querySolution.get("format").toString();
                try {
                    exportValue = MCETools.formatJsonExport(exportValue, exportFormat); //@TODO: move from MCETools in here
                } catch (Exception ex) {
                    logger.warning(method, "formatJsonExport exception and ignored: "+ ex);
                    continue;
                }
            }
            if (dataValue != null) {
                spaModel.remove(resData, Spa.value, dataValue);
            }
            spaModel.add(resData, Spa.value, exportValue);
        }
    }
    

    //@TODO: combine the template processing with that of ServiceTemplate
    // top method to parse / query JSON template
    static public Object querySparsqlTemplateJson(OntModel model, OntModel modelRef, String template, Map<Resource, JSONObject> policyResDataMap) {
        String method = "querySparsqlTemplateJson";
        //parse temlate into json
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(template);
            if (obj instanceof JSONObject) {
                JSONObject jo = (JSONObject) obj;
                if (jo.containsKey("$$")) {
                    jo = expandJsonWildcardKey(jo, policyResDataMap);
                }
                JSONArray joArr = handleSparsqlJsonMap(jo, model, modelRef);;
                if (joArr != null && !joArr.isEmpty()) {
                    querySparsqlTemplateJsonRecursive ((JSONObject)joArr.get(0), model, modelRef);
                }
                return joArr.get(0);
            } else if (obj instanceof JSONArray) {
                JSONArray joArrRet = new JSONArray();
                for (Object subObj: (JSONArray)obj) {
                    JSONObject jo = (JSONObject) subObj;
                    JSONArray joArr = handleSparsqlJsonMap(jo, model, modelRef);
                    if (joArr != null && !joArr.isEmpty()) {
                        querySparsqlTemplateJsonRecursive (jo, model, modelRef);
                    }
                    joArrRet.add(joArr.get(0));
                }
                return joArrRet;
            }
            throw logger.error_throwing(method, "template contains non-json text: " + template);
        } catch (ParseException ex) {
            throw logger.throwing(method, "failed to parse: : " + template, ex);
        }
    }

    // common methods to parse / query JSON joTemplate ... 
    static private void querySparsqlTemplateJsonRecursive(JSONObject joTemplate, OntModel model, OntModel modelRef) {
        String method = "querySparsqlTemplateJsonRecursive";
        JSONArray jaRecursive = new JSONArray();
        JSONObject joVarMap = null;
        if (joTemplate.containsKey("#varmap") ) {
            joVarMap = (JSONObject)joTemplate.get("#varmap");
            joTemplate.remove("#varmap");
        } 
        logger.trace(method, "joTemplate => " + joTemplate.toJSONString());
        Iterator itKey = joTemplate.keySet().iterator();
        while (itKey.hasNext()) {
            Object key = itKey.next();
            Object obj = joTemplate.get(key);
            if (obj instanceof JSONObject) {
                if (joVarMap != null) {
                    ((JSONObject) obj).put("#varmap", joVarMap);
                }
                JSONArray jaResolved = handleSparsqlJsonMap((JSONObject)obj, model, modelRef);
                if (jaResolved == null) {
                    itKey.remove();
                } else {
                    joTemplate.put(key, jaResolved.get(0));
                    jaRecursive.add(jaResolved.get(0));
                }
            } else if (obj instanceof JSONArray) {
                if (joVarMap != null) {
                    ((JSONObject)((JSONArray)obj).get(0)).put("#varmap", joVarMap);
                }
                JSONArray jaResolved = handleSparsqlJsonMap((JSONObject)((JSONArray)obj).get(0), model,  modelRef);
                if (jaResolved == null) {
                    itKey.remove();
                } else {
                    joTemplate.put(key, jaResolved);
                    jaRecursive.addAll(jaResolved);
                }
            }
        }
        for (Object subObj : jaRecursive) {
            querySparsqlTemplateJsonRecursive((JSONObject) subObj, model, modelRef);
        }
    }
    
    static private JSONObject expandJsonWildcardKey(JSONObject jo, Map<Resource, JSONObject> policyResDataMap) {
        String method = "expandJsonWildcardKey";
        if (!jo.containsKey("$$")) {
            return null;
        }
        JSONParser parser = new JSONParser();
        for (Resource key : policyResDataMap.keySet()) {
            String json;
            if (jo.get("$$") instanceof JSONObject) {
                json = ((JSONObject) jo.get("$$")).toJSONString();
            } else if (jo.get("$$") instanceof JSONArray) {
                json = ((JSONArray) jo.get("$$")).toJSONString();
                if (json.contains("%%")) {
                    if (((JSONArray) jo.get("$$")).size() > 1) {
                        throw logger.error_throwing(method, "Invalid output format: %% must only be included in a single element array.");
                    }
                    Object obj1 = ((JSONArray) jo.get("$$")).get(0);
                    json = "[";
                    for (Object obj2 : policyResDataMap.get(key).keySet()) {
                        if (obj1 instanceof JSONObject && obj2 instanceof String) {
                            JSONObject oj1 = (JSONObject) obj1;
                            String json2 = oj1.toJSONString();
                            json2 = json2.replaceAll("%%", (String)obj2);
                            if (!json.equals("[")) {
                                json += ",";
                            }
                            json += json2;
                        }
                    }
                    json += "]";
                }
            } else {
                throw logger.error_throwing(method, key + " -> non-JSON value.");
            }
            json = json.replaceAll("\\$\\$", key.toString());
            try {
                Object obj = parser.parse(json);
                jo.put(key.toString(), obj);
            } catch (ParseException ex) {
                throw logger.throwing(method, "failed parse json: " + json, ex);
            }
        }
        jo.remove("$$");
        return jo;
    }

    static private JSONArray handleSparsqlJsonMap(JSONObject jo, OntModel model, OntModel modelRef) {
        String method = "handleSparsqlJsonMap";
        JSONArray joArr = new JSONArray();
        String sparql = null;
        boolean required = true;
        JSONObject varMap = null;
        if (jo.containsKey("#sparql")) {
            sparql = (String) jo.get("#sparql");
            jo.remove("#sparql");
        } else {
            joArr.add(jo);
            return joArr;
        }
        if (jo.containsKey("#required")) {
            if (((String) jo.get("#required")).equals("false")) {
                required = false;
            }
            jo.remove("#required");
        }
        if (jo.containsKey("#varmap")) {
            varMap = (JSONObject) jo.get("#varmap");
            jo.remove("#varmap");
        } else {
            varMap = new JSONObject();
        }
        logger.trace(method, "jo => " + jo.toJSONString());
        // Recondition sparql using varMap
        sparql = replaceSparqlVars(sparql, varMap);
        // Run sparql query and get vars and add to varMap
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        if (!rs.hasNext()) {
            if (required) {
                throw logger.error_throwing(method, "no required reqsult for manifest query for: " + sparql);
            } else {
                return null;
            }
        }
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            JSONObject newVarMap = (JSONObject) varMap.clone();
            // add resolved variables into varMap.clone
            Iterator<String> itVar = qs.varNames();
            while (itVar.hasNext()) {
                String var = itVar.next();
                String varMapped = qs.get(var).toString();
                newVarMap.put(var, varMapped);
            }
            JSONParser parser = new JSONParser();
            if ((jo.containsKey("#sparql-ref") || jo.containsKey("#sparql-ext")) && modelRef != null) {
                ResultSet rsFull;
                if (!jo.containsKey("#sparql-ref")) {
                    String sparqlFull = (String) jo.get("#sparql-ref");
                    sparqlFull = replaceSparqlVars(sparqlFull, newVarMap);
                    rsFull = ModelUtil.sparqlQuery(modelRef, sparqlFull);
                } else {
                    String sparqlFull = (String) jo.get("#sparql-ext");
                    sparqlFull = replaceSparqlVars(sparqlFull, newVarMap);
                    rsFull = ModelUtil.executeQueryUnion(sparqlFull, modelRef, model);                    
                }
                while (rsFull.hasNext()) {
                    qs = rsFull.next();
                    itVar = qs.varNames();
                    while (itVar.hasNext()) {
                        String var = itVar.next();
                        String varMapped = qs.get(var).toString();
                        newVarMap.put(var, varMapped);
                    }
                    String json = jo.toJSONString();
                    // replace every vaiable in json
                    json = replaceTemplateVars(json, newVarMap);
                    try {
                        // parse json into joResolved
                        Object obj = parser.parse(json);
                        JSONObject joResolved = (JSONObject) obj;
                        // add new resolvedVars into joResolved
                        joResolved.put("#varmap", newVarMap);
                        if (joResolved.containsKey("#sparql-ref")) {
                            joResolved.remove("#sparql-ref");
                        }
                        if (joResolved.containsKey("#sparql-ext")) {
                            joResolved.remove("#sparql-ext");
                        }
                        // add joResolved into joArr
                        joArr.add(joResolved);
                    } catch (ParseException ex) {
                        throw logger.throwing(method, "failed parse json: " + json, ex);
                    }
                }
            } else {
                String json = jo.toJSONString();
                // replace every vaiable in json
                json = replaceTemplateVars(json, newVarMap);
                try {
                    // parse json into joResolved
                    Object obj = parser.parse(json);
                    JSONObject joResolved = (JSONObject) obj;
                    // add new resolvedVars into joResolved
                    joResolved.put("#varmap", newVarMap);
                    if (joResolved.containsKey("#sparql-ref")) {
                        joResolved.remove("#sparql-ref");
                    }
                    if (joResolved.containsKey("#sparql-ext")) {
                        joResolved.remove("#sparql-ext");
                    }
                    // add joResolved into joArr
                    joArr.add(joResolved);
                } catch (ParseException ex) {
                    throw logger.throwing(method, "failed parse json: " + json, ex);
                }
            }
        }
        if (jo.containsKey("#sparql-ref")) {
            jo.remove("#sparql-ref");
        }
        if (jo.containsKey("#sparql-ext")) {
            jo.remove("#sparql-ext");
        }
        return joArr;
    }

    static private String replaceTemplateVars(String text, JSONObject varMap) {
        for (Object var : varMap.keySet()) {
            String varMapped = (String) varMap.get(var);
            varMapped = varMapped.replaceAll("\"", "'");
            text = text.replaceAll("\\?" + var + "\\?", varMapped);
        }
        return text;
    }

    static private String replaceSparqlVars(String text, JSONObject varMap) {
        for (Object var : varMap.keySet()) {
            text = text.replaceAll("\\?" + var + " ", "<" + varMap.get(var) + "> ");
            text = text.replaceAll("\\?" + var + "\\.", "<" + varMap.get(var) + ">.");
            text = text.replaceAll("\\?" + var + "\\)", "<" + varMap.get(var) + ">)");
            text = text.replaceAll("\\?" + var + "\\}", "<" + varMap.get(var) + ">}");
        }
        return text;
    }
}