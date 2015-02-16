/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compile;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.service.orchestrate.ActionBase;
import net.maxgigapop.mrs.service.orchestrate.SimpleWorker;
import net.maxgigapop.mrs.service.orchestrate.WorkerBase;

/**
 *
 * @author xyang
 */
public class SimpleCompiler extends CompilerBase {        
    
    @Override
    public void compile(WorkerBase worker) {        
        OntModel spaOntModelReduction = this.spaDelta.getModelReduction() == null ? null : this.spaDelta.getModelReduction().getOntModel();
        OntModel spaOntModelAddition = this.spaDelta.getModelAddition() == null ? null : this.spaDelta.getModelAddition().getOntModel();
        // assemble workflow parts for reduction
        if (this.spaDelta.getModelReduction() != null) {
            Map<Resource, OntModel> reduceParts = this.decomposeByPolicyActions(spaOntModelReduction);
        }
        
        // assemble workflow parts for addition
        if (this.spaDelta.getModelAddition() != null) {
            Queue<Resource> policyQueue = new LinkedBlockingQueue<>();
            Map<Resource, OntModel> addParts = this.decomposeByPolicyActions(spaOntModelAddition);
            if (addParts == null)
                throw new EJBException(SimpleCompiler.class.getName() + " found none terminal / leaf policy action.");
            // start queue terminal / leaf policy actions
            for (Resource policy: addParts.keySet()) {
                // enqueue child policy
                policyQueue.add(policy); 
            }
            Map<Resource, List<ActionBase>> parentPolicyChildActionMap = new HashMap<>();
            while(!policyQueue.isEmpty()) {
                // dequeue policy
                Resource policy = policyQueue.poll();
                // create Action and assign delta part
                ActionBase action = this.createAction(spaOntModelAddition, policy);
                DeltaModel addModel = new DeltaModel();
                addModel.setOntModel(addParts.get(policy));
                DeltaBase inputDelta = new DeltaBase();
                inputDelta.setModelAddition(addModel);
                action.setInputDelta(inputDelta);
                // lookup dependency 
                if (parentPolicyChildActionMap.containsKey(policy)) {
                    List<ActionBase> childActions = parentPolicyChildActionMap.get(policy);
                    for (ActionBase child: childActions) {
                        worker.addDependency(action, child);
                    }
                }
                // traverse upwards to get parent actions
                List<Resource> parentPolicies = this.listParentPolicies(spaOntModelAddition, policy);
                if (parentPolicies != null) {
                    for (Resource parent: parentPolicies) {
                        // enqueue parent policy
                        policyQueue.add(parent);
                        // map action as dependency of the parent policy for lookup at dequeue
                        List<ActionBase> childActions = parentPolicyChildActionMap.get(policy);
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
        NodeIterator nodeIter = spaModel.listObjectsOfProperty(policy, RdfOwl.type);
        while (nodeIter.hasNext()) {
            RDFNode rn = nodeIter.next();
            if (rn.isResource() && rn.asResource().getURI().contains("spa#")) {
                ActionBase action = new ActionBase(rn.asResource().getURI(), "java:module/TestMCE");
                return action;
            }
        }
        throw new EJBException(SimpleCompiler.class.getName() + " cannot create action for policy " + policy.getURI());
    }
}
