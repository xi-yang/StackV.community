/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.service.compute.IModelComputationElement;

/**
 *
 * @author xyang
 */
public class Action {
    protected String name = "";
    protected String mceBeanPath = "";
    protected String state = ActionState.IDLE;
    protected ModelBase referenceModel = null;
    protected DeltaBase inputDelta = null;
    protected DeltaBase outputDelta = null;
    protected List<Action> dependencies = new ArrayList<>();
    protected List<Action> uppers  = new ArrayList<>();

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

    public List<Action> getUppers() {
        return uppers;
    }

    public void setUppers(List<Action> uppers) {
        this.uppers = uppers;
    }

    
    public List<Action> getDependencies() {
        return dependencies;
    }

    public void addDependent(Action action) {
        if (!dependencies.contains(action)) {
            dependencies.add(action);
            if (!action.getUppers().contains(this)) 
                action.getUppers().add(this);
        }
    }

    public void removeDependent(Action action) {
        if (dependencies.contains(action)) {
            dependencies.remove(action);
            dependencies.add(action);
            if (action.getUppers().contains(this)) 
                action.getUppers().remove(this);
        }
    }

    public Action getIdleLeaf() {
        for (Action action: this.dependencies) {
            Action deeperAction = action.getIdleLeaf();
            if (deeperAction != null)
                return deeperAction;
        }
        if (this.state.equals(ActionState.IDLE))
            return this;
        return null;
    }
    
    public List<Action> getIndependentIdleLeaves(Action action) {
        if (this.equals(action))
            return null;
        List<Action> retList = null;
        for (Action A: this.dependencies) {
            if (A.equals(action))
                continue;
            List<Action> addList = A.getIndependentIdleLeaves(action);
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
        for (Action action: this.dependencies) {
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
            return ejbMce.process(referenceModel, inputDelta);
        } catch (NamingException ex) {
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
