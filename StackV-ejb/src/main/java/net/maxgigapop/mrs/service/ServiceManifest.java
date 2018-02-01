/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2016

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

package net.maxgigapop.mrs.service;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Iterator;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.core.DataConcurrencyPoster;
import net.maxgigapop.mrs.core.SystemModelCoordinator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServiceManifest {
    
    private static final StackLogger logger = new StackLogger(HandleServiceCall.class.getName(), "ServiceManifest");
    
    static public JSONObject generateManifest (String jsonModel, String serviceType) {
        String method = "generateManifest";
        JSONObject joMan = new JSONObject();
        OntModel omAdd = null;
        try {
            omAdd = ModelUtil.unmarshalOntModelJson(jsonModel);
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
        DataConcurrencyPoster dataConcurrencyPoster = DataConcurrencyPoster.getSingleton();
        OntModel omRef = dataConcurrencyPoster.getSystemModelCoordinator_cachedOntModel();
        
        if (serviceType.equals("Dynamic Network Connection")) {
            joMan.put("connections", generateManifestDncLinks(omAdd));
        } else if (serviceType.equals("Virtual Cloud Network")) {
            if (isModelVcnAws(omAdd, omRef)) {
                joMan.put("public_cloud", generateManifestVcnAws(omAdd));
            } else {
                joMan.put("private_cloud", generateManifestVcnOps(omAdd));
            }
            JSONArray joLinks = generateManifestDncLinks(omAdd);
            if (!joLinks.isEmpty()) {
                joMan.put("links", joLinks);
            }
        } else if (serviceType.equals("Hybrid Cloud")) {
            joMan.put("public_cloud", generateManifestVcnAws(omAdd));
            joMan.put("private_cloud", generateManifestVcnAws(omAdd));
            joMan.put("connections", generateManifestDncLinks(omAdd));
        } else {
            throw logger.error_throwing(method, "Cannot generate maniest for service type " + serviceType);
        }
        return joMan;
    }

    static public JSONObject resolveManifestJsonTemplate (String serviceTemplate, String jsonModel) {
        String method = "resolveManifestJsonTemplate";
        OntModel omAdd = null;
        try {
            omAdd = ModelUtil.unmarshalOntModelJson(jsonModel);
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
        DataConcurrencyPoster dataConcurrencyPoster = DataConcurrencyPoster.getSingleton();
        OntModel omRef = dataConcurrencyPoster.getSystemModelCoordinator_cachedOntModel();
        return (JSONObject)querySparsqlTemplateJson(serviceTemplate, omAdd, omRef);
    }
    
    static public JSONArray generateManifestDncLinks (OntModel model) {
        JSONArray joArr = new JSONArray();
        
        return joArr;
    }
    
    static public JSONObject generateManifestVcnAws (OntModel model) {
        String awsManifestTemplate = "{\n" +
"    \"name\": \"?vpc?\",\n" +
"    \"cidr\": \"?vpc_cidr?\",\n" +
"    \"subnets\": [\n" +
"        {\n" +
"            \"name\": \"?subnet?\",\n" +
"            \"cidr\": \"?subnet_cidr?\",\n" +
"            \"vms\": [\n" +
"                {\n" +
"                    \"name\": \"?vm?\",\n" +
"                    \"instruction\": \"To access the VM: ssh -i ?vm_keyname? ec2_user@?vm_public_ip? \",\n" +
"                    \"sparql\": \"SELECT ?vm ?vm_keyname ?vm_keyname ?vm_public_ip WHERE { ?subnet nml:hasBidirectionalPort ?vm_eth0. ?vm  nml:hasBidirectionalPort ?vm_eth0. ?ec2 mrs:providesVM ?vm. OPTIONAL { ?vm mrs:type ?vm_keyname. FILTER (regex(?vm_keyname,'^keypair.+$')) } OPTIONAL { ?vm mrs:hasNetworkAddress ?vm_public_na.?vm_public_na mrs:type \\\"ipv4:public\\\".  ?vm_public_na mrs:value ?vm_public_ip. } }\",\n" +
"                    \"required\": \"false\"\n" +
"                }\n" +
"            ],\n" +
"            \"sparql\": \"SELECT ?subnet ?subnet_cidr WHERE { ?vpc nml:hasService ?vpc_sw_svc. ?vpc_sw_svc mrs:providesSubnet ?subnet. ?subnet mrs:hasNetworkAddress ?subnet_cidr_na. ?subnet_cidr_na mrs:type \\\"ipv4-prefix\\\". ?subnet_cidr_na  mrs:value ?subnet_cidr. }\",\n" +
"            \"required\": \"true\"\n" +
"        }\n" +
"    ],\n" +
"    \"sparql\": \"SELECT ?vpc ?vpc_cidr WHERE { ?aws nml:hasTopology ?vpc. ?vpc_svc mrs:providesVPC ?vpc. ?vpc nml:hasService ?vpc_rt_svc. ?vpc_rt_svc mrs:providesRoute ?vpc_local_rt. ?vpc_local_rt mrs:nextHop \\\"local\\\". ?vpc_local_rt mrs:routeTo ?vpc_cidr_na. ?vpc_cidr_na mrs:type \\\"ipv4-prefix\\\". ?vpc_cidr_na  mrs:value ?vpc_cidr. }\",\n" +
"    \"required\": \"true\"\n" +
"}";
        return (JSONObject)querySparsqlTemplateJson(awsManifestTemplate, model, null);
    }
    
    static public JSONObject generateManifestVcnOps (OntModel model) {
        JSONObject jo = new JSONObject();
        
        return jo;
    }
    
    //@TODO this has to use union model!
    static public boolean isModelVcnAws(OntModel model, OntModel refModel) {
        String sparql = "SELECT ?aws WHERE {"
                + "?aws nml:hasTopology ?vpc. "
                + "?vpc_svc mrs:providesVPC ?vpc. "
                + "}";
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        if (!rs.hasNext()) {
            return false;
        }
        Resource resAws = rs.next().getResource("aws");
        sparql = "SELECT ?ec2_svc WHERE {"
                + "?aws nml:hasService ?ec2_svc. "
                + "?ec2_svc a mrs:HypervisorService. "
                + "}";
        rs = ModelUtil.sparqlQuery(refModel, sparql);
        return rs.hasNext();
    }
    
    // top method to parse / query JSON template
    static public Object querySparsqlTemplateJson(String template, OntModel model, OntModel modelRef) {
        String method = "querySparsqlTemplateJson";
        //parse temlate into json
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(template);
            if (obj instanceof JSONObject) {
                JSONObject jo = (JSONObject) obj;
                JSONArray joArr = handleSparsqlJsonMap(jo, model, modelRef);
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
        if (joTemplate.containsKey("varmap") ) {
            joVarMap = (JSONObject)joTemplate.get("varmap");
            joTemplate.remove("varmap");
        } 
        logger.trace(method, "joTemplate => " + joTemplate.toJSONString());
        Iterator itKey = joTemplate.keySet().iterator();
        while (itKey.hasNext()) {
            Object key = itKey.next();
            Object obj = joTemplate.get(key);
            if (obj instanceof JSONObject) {
                if (joVarMap != null) {
                    ((JSONObject) obj).put("varmap", joVarMap);
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
                    ((JSONObject)((JSONArray)obj).get(0)).put("varmap", joVarMap);
                }
                JSONArray jaResolved = handleSparsqlJsonMap((JSONObject)((JSONArray)obj).get(0), model, modelRef);
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
    
    static private JSONArray handleSparsqlJsonMap(JSONObject jo, OntModel model, OntModel modelRef) {
        String method = "handleSparsqlJsonMap";
        JSONArray joArr = new JSONArray();
        String sparql = null;
        boolean required = true;
        JSONObject varMap = null;
        if (jo.containsKey("sparql")) {
            sparql = (String)jo.get("sparql");
            jo.remove("sparql");
        } else {
            joArr.add(jo);
            return joArr;
        }
        if (jo.containsKey("required")) {
            if (((String) jo.get("required")).equals("false")) {
                required = false;
            }
            jo.remove("required");
        }
        if (jo.containsKey("varmap")) {
            varMap = (JSONObject)jo.get("varmap");
            jo.remove("varmap");
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
                throw logger.error_throwing(method, "no required reqsult for manifest query for: "+sparql);
            } else {
                return null;
            }
        }
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            JSONObject newVarMap = (JSONObject)varMap.clone();
            // add resolved variables into varMap.clone
            Iterator<String> itVar = qs.varNames();
            while (itVar.hasNext()) {
                String var = itVar.next();
                String varMapped = qs.get(var).toString();
                newVarMap.put(var, varMapped);
            }
            JSONParser parser = new JSONParser();
            if (jo.containsKey("sparql-ext") && modelRef != null) {
                String sparqlFull = (String) jo.get("sparql-ext");
                sparqlFull = replaceSparqlVars(sparqlFull, newVarMap);
                ResultSet rsFull = ModelUtil.sparqlQuery(modelRef, sparqlFull);
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
                        joResolved.put("varmap", newVarMap);
                        if (joResolved.containsKey("sparql-ext")) {
                            joResolved.remove("sparql-ext");
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
                    joResolved.put("varmap", newVarMap);
                    if (joResolved.containsKey("sparql-ext")) {
                        joResolved.remove("sparql-ext");
                    }
                    // add joResolved into joArr
                    joArr.add(joResolved);
                } catch (ParseException ex) {
                    throw logger.throwing(method, "failed parse json: " + json, ex);
                }
            }
        }
        if (jo.containsKey("sparql-ext")) {
            jo.remove("sparql-ext");
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
