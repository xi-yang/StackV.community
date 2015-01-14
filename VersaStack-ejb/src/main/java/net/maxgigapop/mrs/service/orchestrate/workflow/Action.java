/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate.workflow;

import java.util.ArrayList;
import java.util.List;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.service.ServiceException;

/**
 *
 * @author xyang
 */
public class Action {
    protected String name = "";
    protected String mceBeanName = "";
    protected String state = ActionState.IDLE;
    protected ModelBase referenceModel = null;
    protected DeltaBase inputDelta = null;
    protected DeltaBase outputDelta = null;
    protected List<Action> dependencies = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void addDependent(Action action) {
        if (!dependencies.contains(action))
            dependencies.add(action);
    }

    public void removeDependent(Action action) {
        if (dependencies.contains(action))
            dependencies.remove(action);
    }

    public String toString() {
        return "WorkerAction(" + this.name+"->"+this.mceBeanName+")";
    }
}
