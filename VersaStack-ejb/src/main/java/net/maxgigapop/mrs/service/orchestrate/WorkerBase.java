/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.core.SystemModelCoordinator;
import net.maxgigapop.mrs.service.compute.IModelComputationElement;
import net.maxgigapop.mrs.service.compute.MCE_MPVlanConnection;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author xyang
 */
public class WorkerBase {
    ModelBase referenceSystemModel = null;
    DeltaBase annoatedModel = null;
    DeltaBase resultModelDelta = null;
    List<ActionBase> rootActions = new ArrayList<>();
       
    public void setAnnoatedModel(DeltaBase annoatedModel) {
        this.annoatedModel = annoatedModel;
    }

    public DeltaBase getResultModelDelta() {
        return resultModelDelta;
    }

    public List<ActionBase> getRootActions() {
        return rootActions;
    }

    public void addRooAction(ActionBase action) {
        rootActions.add(action);
    }

    public void addDependency(ActionBase parent, ActionBase child) {
        parent.addDependent(child);
    }
    
    // DFS to return the first Idle action that has none un-merged child
    protected ActionBase lookupIdleLeafAction() {
        for (ActionBase action: rootActions) {
            ActionBase idleLeaf = action.getIdleLeaf();
            if (idleLeaf != null)
                return idleLeaf;
        }
        return null; 
    } 

    // return set of actions that are Idle AND (not in its subtree) AND (have none un-merged child)
    // the returned list include the input ActionBase itself
    protected Set<ActionBase> lookupIndependentActions(ActionBase action) {
        Set<ActionBase> list = new HashSet<>();
        list.add(action);
        for (ActionBase aRoot: rootActions) {
            Set<ActionBase> listAdd = aRoot.getIndependentIdleLeaves(action);
            if (listAdd != null) {
                list.addAll(listAdd);
            }
        }
        return list;
    }
    
    protected void runWorkflow() {
        final ActionBase mergedRoot = rootActions.get(0);
        Map<ActionBase, Future<DeltaBase>> resultMap = new HashMap<>();

        // start workflow run
        //1. lookupDeepestIdleAction to get an available action;
        ActionBase nextAction = this.lookupIdleLeafAction();
        //2.  lookupIndependentActions to find list of parallel actions for the step
        Set<ActionBase> batchOfActions = this.lookupIndependentActions(nextAction);
        // Top loop to exhaust idle actions in rootActions list and sub-trees
        //$$ Timeout for the top loop ?
        while (!batchOfActions.isEmpty()) {
            //3. execute the idle actions in the list (asynchronously)
            Iterator<ActionBase> itA = batchOfActions.iterator();
            while (itA.hasNext()){
                ActionBase action = itA.next();
                if (action.getState().equals(ActionState.IDLE)) {
                    action.setReferenceModel(this.referenceSystemModel);
                    Future<DeltaBase> asyncResult = action.execute();
                    resultMap.put(action, asyncResult);
                } else if (action.getState().equals(ActionState.FINISHED) 
                        || action.getState().equals(ActionState.MERGED)) {
                    itA.remove();
                }
            }
            //4. poll action status 
            long timeout = 3000; // 3000 intervals x 100ms = 300 seconds 
            while (timeout-- > 0 && !resultMap.isEmpty()) {
                try {
                    sleep(100);
                } catch (InterruptedException ex) {
                    ;
                }
                itA = resultMap.keySet().iterator();
                while (itA.hasNext()) {
                    ActionBase action = itA.next();
                    Future<DeltaBase> asyncResult = resultMap.get(action);
                    if (asyncResult.isDone()) {
                        action.setState(ActionState.FINISHED);
                        try {
                            DeltaBase resultDelta = asyncResult.get(); // exception may be thrown
                            action.setOutputDelta(resultDelta);
                            // if a run action return successfully
                            // collect resultDelta and merge to parent input annotatedDelta
                            if (!action.getUppers().isEmpty()) {
                                for (ActionBase upperAction: action.getUppers()) {
                                    upperAction.mergeResult(resultDelta);
                                    action.setState(ActionState.MERGED);
                                }
                            } else if (!action.equals(mergedRoot)){
                                // merge root actions delta to the first root element
                                mergedRoot.mergeResult(resultDelta);
                            }
                            itA.remove();
                        } catch (Exception ex) {
                            //$$ TODO: cleanup and cancel/shutdown other actions in workflow
                            String errMsg;
                            if (action.getState() == ActionState.FAILED)
                                errMsg = "workflow caught exception from " + action;
                            else 
                                errMsg = "workflow is interrupted when " + action + " is in sate " + action.getState();
                            throw new EJBException(errMsg + action, ex);
                        }
                    }
                }
                // lookupDeepestIdleAction to get next available action
                nextAction = this.lookupIdleLeafAction();
                if (nextAction != null) {
                    //lookupIndependentActions and add all independent idel actions to execution batch
                    batchOfActions.addAll(this.lookupIndependentActions(nextAction));
                    break;
                }
            }
            // continue to batch execution (to exectute new action and/or wait ones in processing)            
        }
        //$$ TODO: throw exception if top loop times out
        
        mergedRoot.cleanupOutputDelta();
        try {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.INFO, "\n>>>Workflow--DeltaAddModel Output=\n" + ModelUtil.marshalOntModel(mergedRoot.getOutputDelta().getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.resultModelDelta = mergedRoot.getOutputDelta();
    }
    
    protected void retrieveSystemModel() {
        try {
            Context ejbCxt = new InitialContext();
            SystemModelCoordinator systemModelCoordinator = (SystemModelCoordinator)ejbCxt.lookup("java:module/SystemModelCoordinator");
            VersionGroup referenceVersionGroup = systemModelCoordinator.getLatestVersionGroupWithUnionModel();
            if (referenceVersionGroup == null) {
                throw new EJBException(this.getClass().getName() + " got null referenceVersionGroup - systemModelCoordinator is not ready");
            }
            referenceSystemModel = referenceVersionGroup.getCachedModelBase();
            if (referenceSystemModel == null) {
                throw new EJBException(this.getClass().getName() + " got null  referenceSystemModel from " + referenceVersionGroup);
            }
        } catch (NamingException ex) {
            throw new EJBException(this.getClass().getName() + " failed to inject systemModelCoordinator");
        }        
    }
    
    public void run() {
        // get system base model from SystemModelCoordinator singleton
        retrieveSystemModel();
        // annoatedModel and rootActions should have been instantiated by caller
        if (annoatedModel == null) {
            throw new EJBException("Workerflow cannot run with null annoatedModel");
        }
        if (rootActions.isEmpty()) {
            throw new EJBException("Workerflow cannot run with empty rootActions");
        }
        this.runWorkflow();
    }
}
