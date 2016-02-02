/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
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
import net.maxgigapop.mrs.service.compute.IModelComputationElement;

/**
 *
 * @author xyang
 */
public class SimpleCompiler extends CompilerBase {

    private static final Logger log = Logger.getLogger(SimpleCompiler.class.getName());

    @Override
    public void compile(WorkerBase worker) {
        OntModel spaOntModelReduction = this.spaDelta.getModelReduction() == null ? null : this.spaDelta.getModelReduction().getOntModel();
        OntModel spaOntModelAddition = this.spaDelta.getModelAddition() == null ? null : this.spaDelta.getModelAddition().getOntModel();
        if (spaOntModelReduction != null && !ModelUtil.isEmptyModel(spaOntModelReduction)) {
            Map<Resource, OntModel> reduceParts = this.decomposeByPolicyActions(spaOntModelReduction);
        }
        // assemble workflow parts for addition
        if (spaOntModelAddition != null && !ModelUtil.isEmptyModel(spaOntModelAddition)) {
            Queue<Resource> policyQueue = new LinkedBlockingQueue<>();
            // start with leaf policies
            Map<Resource, OntModel> addParts = this.decomposeByPolicyActions(spaOntModelAddition);
            if (addParts == null) {
                throw new EJBException(SimpleCompiler.class.getName() + " found none terminal / leaf policy action.");
            }
            // start queue terminal / leaf policy actions
            for (Resource policy : addParts.keySet()) {
                // enqueue child policy
                policyQueue.add(policy);
            }
            Map<Resource, List<ActionBase>> parentPolicyChildActionMap = new HashMap<>();
            while (!policyQueue.isEmpty()) {
                // dequeue policy
                Resource policy = policyQueue.poll();
                // create Action and assign delta part
                ActionBase action = this.createAction(spaOntModelAddition, policy);
                OntModel addOntModel = addParts.get(policy);
                if (addOntModel != null) {
                    DeltaModel addModel = new DeltaModel();
                    addModel.setOntModel(addParts.get(policy));
                    ServiceDelta inputDelta = action.getInputDelta();
                    if (inputDelta == null) {
                        inputDelta = new ServiceDelta();
                    }
                    inputDelta.setModelAddition(addModel);
                    action.setInputDelta(inputDelta);
                }
                // lookup dependency 
                if (parentPolicyChildActionMap.containsKey(policy)) {
                    List<ActionBase> childActions = parentPolicyChildActionMap.get(policy);
                    for (ActionBase child : childActions) {
                        worker.addDependency(action, child);
                    }
                    // check if the parent (action) has got all dependencies (children)
                    List<Resource> childPolicies = this.listChildPolicies(spaOntModelAddition, policy);
                    // If not, re-enqueue
                    if (childPolicies.size() > childActions.size()) {
                        policyQueue.add(policy);
                        // skip now, retry this policy in queue later
                        continue;
                    }
                }
                // traverse upwards to get parent actions
                List<Resource> parentPolicies = this.listParentPolicies(spaOntModelAddition, policy);
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
        }
    }

    private ActionBase createAction(OntModel spaModel, Resource policy) {
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
            try {
                Context ejbCxt = new InitialContext();
                IModelComputationElement ejbMce = (IModelComputationElement) ejbCxt.lookup(policyAction.getMceBeanPath());
                if (ejbMce == null) {
                    throw new EJBException(SimpleCompiler.class.getName() + ":createAction does not support policy action type: " + policyActionType);
                }
            } catch (NamingException ex) {
                throw new EJBException(SimpleCompiler.class.getName() + ":createAction does not support policy action type: " + policyActionType);
            }
            return policyAction;
        } else {
            throw new EJBException(SimpleCompiler.class.getName() + ":createAction encounters malformed policy action: " + policy.getLocalName());
        }
    }
}
