/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate.workflow;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;

/**
 *
 * @author xyang
 */
public class WorkerBase {
    ModelBase referenceSystemModel = null;
    DeltaBase annoatedModel = null;
    DeltaBase resultModel = null;
    List<Action> rootActions = new ArrayList<>();
    List<List<Action>> actionBatches = new ArrayList<>();
            
    public void setAnnoatedModel(DeltaBase annoatedModel) {
        this.annoatedModel = annoatedModel;
    }

    public void setResultModel(DeltaBase resultModel) {
        this.resultModel = resultModel;
    }

    public DeltaBase getResultModel() {
        return resultModel;
    }

    public List<Action> getRootActions() {
        return rootActions;
    }

    void addRooAction(Action action) {
        rootActions.add(action);
    }

    void addDependency(Action parent, Action child) {
        parent.addDependent(child);
    }
    
    // DFS to return the first Idle action that has none un-merged child
    Action lookupDeepestIdleAction() {
        //$ TODO
        return null; 
    } 

    // return set of actions that are Idle AND (not in its path to root) AND (have none un-merged child)
    // thE returned list include the input Action itself
    List<Action> lookupIndependentActions(Action action) {
        List<Action> list = new ArrayList<>();
        list.add(action);
        //$ TODO
        return list;
    }
            
    void runWorkflow() {
        //$ TODO
        // retrieve (copy?) referenceSystemModel from core
        
        if (referenceSystemModel == null) {
            throw new EJBException("Workerflow run with null referenceSystemModel");
        }
        if (annoatedModel == null) {
            throw new EJBException("Workerflow run with null annoatedModel");
        }
        if (rootActions.isEmpty()) {
            throw new EJBException("Workerflow run with empty rootActions");
        }
        
        //$ TODO: drive workflow logic
        // form a loop to exhaust idel actions in rootActions list:
            //1. lookupDeepestIdleAction to get an available action;
            //2. then lookupIndependentActions to find list of parallel actions for the step
            //3. execute the idle actions in the list (asynchronously)
            //4. pause to poll on action status 
                // fail on exception, cleanup and shutdown workflow
                // if a run action return successfully
                // collect resultDelta and merge to parent input annotatedDelta
                // lookupDeepestIdleAction to get next available action
                // goto 2
    }
}
