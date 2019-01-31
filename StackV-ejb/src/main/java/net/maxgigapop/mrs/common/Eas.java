/*
* Copyright (c) 2013-2018 University of Maryland
* Created by: SaiArvind Ganganapalle 2018
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and/or hardware specification (the “Work”) to deal in the
* Work without restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
* the Work, and to permit persons to whom the Work is furnished to do so,
* subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Work.
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
* IN THE WORK.
 */
package net.maxgigapop.mrs.common;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author saiarvind
 */
public class Eas {
    
    public static final StackLogger logger = new StackLogger(Eas.class.getName(), "Eas");
    
    JSONParser parser = new JSONParser();
    
    IPATool ipaTool; 
    
    public Eas() {
        ipaTool = new IPATool();
    }
    
    public String createEasTaskForVF(JSONObject params) {
        System.out.print("Eas - createEaskTaskForVF: " + params.toJSONString());
        String result = "";
        
        if (params == null || params.isEmpty()) {
            result = "easTaskCreation for Virtual Function failed. No parameters provided.";
        } else {            
            if (createEasTask(params)) {
                result = "Created EAS Task for Virtual Function";
            } else {
                result = "EAS Task Creation for Virtual Function Failed";
            } 
               
        }
        
        return result;
    }
    
    /**
     * Posts the given JSON object to the IPA server and returns the JSON response
     * @param ipaJSON
     * @return 
     */
    private JSONObject runIpaRequest(JSONObject ipaJSON) {
        JSONObject resultJSON = new JSONObject();
        if (ipaTool == null) {
            ipaTool = new IPATool();
        }
        
        String response = ipaTool.ipaEndpoint(ipaJSON.toJSONString());
        
        try {
            resultJSON = (JSONObject) parser.parse(response);
        } catch (ParseException ex) {            
            logger.error("runIpaRequest", "IPA Request failed. Exception: " + ex);
            resultJSON.put("error", true);
        }
               
        return resultJSON;
    }
    
    private boolean createEasTask(JSONObject params) {       
        JSONObject createEasTaskResponseJSON = createAndRunEasTaskJSON(params);
        
         // parse the JSON response for the address
        // if there is no error
        if (createEasTaskResponseJSON.get("error") == null && createEasTaskResponseJSON.get("result") != null) {
            JSONObject resultJSON = (JSONObject) createEasTaskResponseJSON.get("result");
            JSONObject innerResultJSON = (JSONObject) resultJSON.get("result");
            if (innerResultJSON.get("value").equals((String) params.get("cn"))) {
                logger.trace("createEasTask", "EAS Task creation success");
                return true;
            }
        }
        
        logger.warning("createEasTask", "EAS Task creation failed. Response object: " + createEasTaskResponseJSON.toJSONString());
        return false;
    }
    
    private JSONObject createAndRunEasTaskJSON(JSONObject params) {
        JSONObject leaseJSON = new JSONObject();
        leaseJSON.put("id", 0);
        leaseJSON.put("method", "easTask_add");
                      
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("cn", params.get("cn"));
        paramsArrArgs.put("eastasktype", params.get("eastasktype"));
        paramsArrArgs.put("eastaskaction", params.get("eastaskaction"));
        paramsArrArgs.put("eastaskstatus", params.get("eastaskstatus"));
        
        paramsArrArgs.put("eastasktriggers", params.get("eastasktriggers"));
        paramsArrArgs.put("eastasklockedby", params.get("eastasklockedby"));
        paramsArrArgs.put("eastasklockexpires", params.get("eastasklockexpires"));
        paramsArrArgs.put("eastaskresourcerefdn", params.get("eastaskresourcerefdn"));
        paramsArrArgs.put("eastaskoptions", params.get("eastaskoptions"));
        
       
        paramsArr.add(paramsArrArgs);
        
        leaseJSON.put("params", paramsArr);
        
        return runIpaRequest(leaseJSON);
    }
    
    
    private boolean isStrNullOrEmpty(String s) {
        return (s == null || s.isEmpty());
    }
    
}
