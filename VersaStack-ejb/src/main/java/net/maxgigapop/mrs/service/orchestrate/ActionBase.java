/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.service.compute.IModelComputationElement;
import net.maxgigapop.mrs.service.compute.TestMCE;

/**
 *
 * @author xyang
 */
public class ActionBase {
    protected String name = "";
    protected String mceBeanPath = "";
    protected String state = ActionState.IDLE;
    protected ModelBase referenceModel = null;
    protected DeltaBase inputDelta = null;
    protected DeltaBase outputDelta = null;
    protected List<ActionBase> dependencies = new ArrayList<>();
    protected List<ActionBase> uppers  = new ArrayList<>();
    private static Logger log = Logger.getLogger(ActionBase.class.getName());

    private ActionBase() { }
    
    public ActionBase(String name, String mceBean) {
        this.name = name;
        this.mceBeanPath = mceBean;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMceBeanPath() {
        return mceBeanPath;
    }

    public void setMceBeanPath(String mceBeanPath) {
        this.mceBeanPath = mceBeanPath;
    }

    
    public String getState() {
        return state;
    }

    public void setState(String mceState) {
        this.state = mceState;
    }
    
    public ModelBase getReferenceModel() {
        return referenceModel;
    }

    public void setReferenceModel(ModelBase referenceModel) {
        this.referenceModel = referenceModel;
    }

    public DeltaBase getInputDelta() {
        return inputDelta;
    }

    public void setInputDelta(DeltaBase inputDelta) {
        this.inputDelta = inputDelta;
    }

    public DeltaBase getOutputDelta() {
        return outputDelta;
    }

    public void setOutputDelta(DeltaBase outputDelta) {
        this.outputDelta = outputDelta;
    }

    public List<ActionBase> getUppers() {
        return uppers;
    }

    public void setUppers(List<ActionBase> uppers) {
        this.uppers = uppers;
    }

    
    public List<ActionBase> getDependencies() {
        return dependencies;
    }

    public void addDependent(ActionBase action) {
        if (!dependencies.contains(action)) {
            dependencies.add(action);
            if (!action.getUppers().contains(this)) 
                action.getUppers().add(this);
        }
    }

    public void removeDependent(ActionBase action) {
        if (dependencies.contains(action)) {
            dependencies.remove(action);
            dependencies.add(action);
            if (action.getUppers().contains(this)) 
                action.getUppers().remove(this);
        }
    }

    public ActionBase getIdleLeaf() {
        for (ActionBase action: this.dependencies) {
            ActionBase deeperAction = action.getIdleLeaf();
            if (deeperAction != null)
                return deeperAction;
        }
        if (this.state.equals(ActionState.IDLE))
            return this;
        return null;
    }
    
    public List<ActionBase> getIndependentIdleLeaves(ActionBase action) {
        if (this.equals(action))
            return null;
        List<ActionBase> retList = null;
        for (ActionBase A: this.dependencies) {
            if (A.equals(action))
                continue;
            List<ActionBase> addList = A.getIndependentIdleLeaves(action);
            if (retList == null) {
                retList = new ArrayList<>();
            }
            if (addList != null) {
                retList.addAll(addList);
            } else if (A.getState().equals(ActionState.IDLE) && action.hasAllDependenciesMerged()){
                retList.add(A);
            }
        }
        return retList;
    }
    
    public boolean hasAllDependenciesMerged() {
        for (ActionBase action: this.dependencies) {
            if (!action.getState().equals(ActionState.MERGED)) {
                return false;
            }
        }
        return true;
    }
    
    public Future<DeltaBase> execute() {
        try {
            Context ejbCxt = new InitialContext();
            IModelComputationElement ejbMce = (IModelComputationElement)ejbCxt.lookup(this.mceBeanPath);
            this.state = ActionState.PROCESSING;
            Future<DeltaBase> asyncResult = ejbMce.process(referenceModel, inputDelta);
            log.info(this+" FINISHED");
            this.state = ActionState.FINISHED;
            return asyncResult;
        } catch (NamingException ex) {
            this.state = ActionState.FAILED;
            throw new EJBException(this + " failed to invoke MCE bean");
        }        
    }
    
    public void mergeResult(DeltaBase childDelta) {
        //$$ TBD
    }
    
    public String toString() {
        return "WorkerAction(" + this.name+"->"+this.mceBeanPath+")";
    }
}
