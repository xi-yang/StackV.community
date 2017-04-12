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

package net.maxgigapop.mrs.service.compile;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.service.orchestrate.ActionBase;
import net.maxgigapop.mrs.service.orchestrate.SimpleWorker;
import net.maxgigapop.mrs.service.orchestrate.WorkerBase;
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.service.compute.IModelComputationElement;

/**
 *
 * @author xyang
 */
public class SimpleCompiler extends CompilerBase {

    private static final StackLogger logger = new StackLogger(CompilerBase.class.getName(), "CompilerBase");

    @Override
    public void compile(WorkerBase worker) {
        String method = "compile";
        logger.start(method);
        OntModel spaOntModelReduction = this.spaDelta.getModelReduction() == null ? null : this.spaDelta.getModelReduction().getOntModel();
        OntModel spaOntModelAddition = this.spaDelta.getModelAddition() == null ? null : this.spaDelta.getModelAddition().getOntModel();
        // assemble workflow parts for reduction
        if (spaOntModelReduction != null && !ModelUtil.isEmptyModel(spaOntModelReduction)) {
            logger.trace(method, "compiling reduction model");
            compileModel(worker, spaOntModelReduction);
        }
        // assemble workflow parts for addition
        if (spaOntModelAddition != null && !ModelUtil.isEmptyModel(spaOntModelAddition)) {
            logger.trace(method, "compiling addition model");
            compileModel(worker, spaOntModelAddition);
        }
        logger.end(method);
    }

    private void compileModel(WorkerBase worker, OntModel spaModel) {
        String method = "compileModel";
        logger.start(method);
        Queue<Resource> policyQueue = new LinkedBlockingQueue<>();
        // start with leaf policies
        Map<Resource, OntModel> modelParts = this.decomposeByPolicyActions(spaModel);
        if (modelParts == null) {
            throw logger.error_throwing(method, "Found none terminal / leaf policy action.");
        }
        // start queue terminal / leaf policy actions
        for (Resource policy : modelParts.keySet()) {
            // enqueue child policy
            policyQueue.add(policy);
        }
        Map<Resource, List<ActionBase>> parentPolicyChildActionMap = new HashMap<>();
        while (!policyQueue.isEmpty()) {
            // dequeue policy
            Resource policy = policyQueue.poll();
            // create Action and assign delta part
            ActionBase action = this.createAction(spaModel, policy);
            OntModel ontModel = modelParts.get(policy);
            if (ontModel != null) {
                DeltaModel model = new DeltaModel();
                model.setOntModel(modelParts.get(policy));
                ServiceDelta inputDelta = action.getInputDelta();
                if (inputDelta == null) {
                    inputDelta = new ServiceDelta();
                }
                inputDelta.setModelAddition(model);
                action.setInputDelta(inputDelta);
            }
            // lookup dependency 
            if (parentPolicyChildActionMap.containsKey(policy)) {
                List<ActionBase> childActions = parentPolicyChildActionMap.get(policy);
                for (ActionBase child : childActions) {
                    worker.addDependency(action, child);
                }
                // check if the parent (action) has got all dependencies (children)
                List<Resource> childPolicies = this.listChildPolicies(spaModel, policy);
                // If not, re-enqueue
                if (childPolicies.size() > childActions.size()) {
                    policyQueue.add(policy);
                    // skip now, retry this policy in queue later
                    continue;
                }
            }
            // traverse upwards to get parent actions
            List<Resource> parentPolicies = this.listParentPolicies(spaModel, policy);
            if (parentPolicies != null) {
                for (Resource parent : parentPolicies) {
                    // enqueue parent policy
                    if (!policyQueue.contains(parent)) {
                        policyQueue.add(parent);
                    }
                    // map action as dependency of the parent policy for lookup at dequeue
                    List<ActionBase> childActions = parentPolicyChildActionMap.get(parent);
                    if (childActions == null) {
                        childActions = new ArrayList<>();
                        parentPolicyChildActionMap.put(parent, childActions);
                    }
                    childActions.add(action);
                }
            } else {
                // action without parent policy is a root action
                worker.addRooAction(action);
            }
        }
        logger.end(method);
    }

    private ActionBase createAction(OntModel spaModel, Resource policy) {
        String method = "createAction";
        logger.trace(method, "policy="+policy);
        String policyActionType = null;

        String sparql = "SELECT ?policyAction ?actionType WHERE {"
                + "?policyAction a spa:PolicyAction. "
                + "?policyAction spa:type ?actionType. "
                + "?anyOther spa:dependOn ?policyAction . "
                + "?policyData a spa:PolicyData . "
                + String.format("FILTER (?policyAction = <%s>) ", policy)
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        if (r.hasNext()) {
            QuerySolution solution = r.next();
            String actionType = solution.getLiteral("actionType").toString();
            ActionBase policyAction = new ActionBase(policy.getURI(), "java:module/" + actionType);
            policyAction.setPolicy(policy);
            try {
                Context ejbCxt = new InitialContext();
                IModelComputationElement ejbMce = (IModelComputationElement) ejbCxt.lookup(policyAction.getMceBeanPath());
                if (ejbMce == null) {
                    throw logger.error_throwing(method, " does not support policy action type: " + policyActionType);
                }
            } catch (NamingException ex) {
                throw logger.throwing(method, ex);
            }
            return policyAction;
        } else {
            throw logger.error_throwing(method, " encounters malformed policy action: " + policy.getLocalName());
        }
    }
}
