/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.service.compute.IModelComputationElement;
import net.maxgigapop.mrs.service.compute.TestMCE;
import net.maxgigapop.www.rains.ontmodel.Spa;

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
    private static final Logger log = Logger.getLogger(ActionBase.class.getName());

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
        boolean hasChildInProcessing = false;
        for (ActionBase action: this.dependencies) {
            ActionBase deeperAction = action.getIdleLeaf();
            if (deeperAction != null)
                return deeperAction;
            if (action.getState().equals(ActionState.PROCESSING))
                hasChildInProcessing = true;
        }
        if (this.state.equals(ActionState.IDLE) && !hasChildInProcessing)
            return this;
        return null;
    }
    
    public Set<ActionBase> getIndependentIdleLeaves(ActionBase action) {
        if (this.equals(action))
            return null;
        Set<ActionBase> retList = null;
        for (ActionBase A: this.dependencies) {
            if (A.equals(action))
                continue;
            Set<ActionBase> addList = A.getIndependentIdleLeaves(action);
            if (retList == null) {
                retList = new HashSet<>();
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
            //# not FINISHED yet
            return asyncResult;
        } catch (NamingException ex) {
            this.state = ActionState.FAILED;
            throw new EJBException(this + " failed to invoke MCE bean");
        }        
    }
    
    public void mergeResult(DeltaBase childDelta) {
        if (inputDelta == null) {
            inputDelta = childDelta;
            return;
        }
        // merging models A (childDelta) into B (this.inputDelta)
        // merge addition 
        if (childDelta.getModelAddition() != null && childDelta.getModelAddition().getOntModel() != null &&
                inputDelta.getModelAddition() != null && inputDelta.getModelAddition().getOntModel() != null) {
            OntModel mergedAddition = this.mergeOntModel(childDelta.getModelAddition().getOntModel(), inputDelta.getModelAddition().getOntModel());
            inputDelta.getModelAddition().setOntModel(mergedAddition);
        }
        // merge reduction         
        if (childDelta.getModelReduction()!= null && childDelta.getModelReduction().getOntModel() != null &&
                inputDelta.getModelReduction() != null && inputDelta.getModelReduction().getOntModel() != null) {
            OntModel mergedReduction = this.mergeOntModel(childDelta.getModelReduction().getOntModel(), inputDelta.getModelReduction().getOntModel());
            inputDelta.getModelReduction().setOntModel(mergedReduction);
        }
    }
    
    //@@ TODO: include all importFrom statements when merging
    protected OntModel mergeOntModel(OntModel modelA, OntModel modelB) {
        // 1. Get dA = A.remove(B) and dB = B.remove(A)
        OntModel modelAbutB = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        modelAbutB.add(modelA.getBaseModel());
        modelAbutB.remove(modelB);
        OntModel modelBbutA = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        modelBbutA.add(modelB);
        modelBbutA.remove(modelA.getBaseModel());
        OntModel mergedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        mergedModel.add(modelA.getBaseModel());        
        // 2. if dA has r1 *with* annotation while r1 is not in dB, A.remove(r1)
        StmtIterator stmtIter = modelAbutB.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.next();
            Property P = stmt.getPredicate();
            if (!P.getNameSpace().equals(Spa.getURI()))
                continue;
            if (!modelBbutA.contains(stmt))
                mergedModel.remove(stmt);
        }
        // 3. if dB has r2 *without* annoation amd r2 is not in dA, A.add(r2)
        stmtIter = modelBbutA.listStatements();
        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.next();
            Property P = stmt.getPredicate();
            if (P.getNameSpace().equals(Spa.getURI()))
                continue;
            if (!modelAbutB.contains(stmt))
                mergedModel.add(stmt);
        }
        return mergedModel;
    }
    
    public String toString() {
        return "WorkerAction(" + this.name+"->"+this.mceBeanPath+")";
    }
}
