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

package net.maxgigapop.mrs.service.orchestrate;

import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.service.compile.CompilerBase;
import net.maxgigapop.mrs.service.compile.CompilerFactory;
import net.maxgigapop.mrs.service.compute.MCE_AwsDxStitching;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
public class NegotiableWorker extends WorkerBase {

    private static final StackLogger logger = new StackLogger(NegotiableWorker.class.getName(), "NegotiableWorker");

    @Override
    public void run() {
        String method = "run";
        // annoatedModel and rootActions should have been instantiated by caller
        if (annoatedModelDelta == null) {
            throw logger.error_throwing(method, "Workerflow cannot run with null annoatedModel");
        }
        // retrieve latest system (ref) model
        retrieveSystemModel();
        // apply annoatedModelDelta->negotiationMarkup to this.referenceSystemModelVG.getCachedModelBase()
        if (annoatedModelDelta.getNegotiationMarkup() != null && !annoatedModelDelta.getNegotiationMarkup().isEmpty()) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObj;
            try {
                jsonObj = (JSONObject) parser.parse(annoatedModelDelta.getNegotiationMarkup());
            } catch (ParseException ex1) {
                throw logger.error_throwing(method, "failed to parse negotiation markup JSON: " + ex1);
            }
            if (!jsonObj.containsKey("markup") || !(jsonObj.get("markup") instanceof JSONArray)) {
                throw logger.error_throwing(method, "received negotiation markup JSON has no 'markup' array");
            }
            //@TODO: validate with markup data UUID and Expires parameters
            for (Object obj : (JSONArray) jsonObj.get("markup")) {
                JSONObject jsonDelta = (JSONObject) obj;
                DeltaBase delta = new DeltaBase();
                //@TODO: turn jsonDelta into delta
                this.referenceSystemModelVG.getCachedModelBase().applyDelta(delta);
            }
            //@TODO: incorporate 'conflict' array into workflow to constrain some MCEs in special need
        }
        try {
            CompilerBase simpleCompiler = CompilerFactory.createCompiler("net.maxgigapop.mrs.service.compile.SimpleCompiler");
            simpleCompiler.setSpaDelta(this.annoatedModelDelta);
            simpleCompiler.compile(this);
            this.runWorkflow();
        } catch (Exception ex) {
            throw logger.throwing("run", ex);
        }
    }
}
