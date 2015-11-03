/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.orchestrate;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
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
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.service.compute.IModelComputationElement;
import net.maxgigapop.mrs.service.compute.TestMCE;
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.service.compute.MCE_MPVlanConnection;

/**
 *
 * @author xyang
 */
public class ActionBase {
    protected String name = "";
    protected String mceBeanPath = "";
    protected String state = ActionState.IDLE;
    protected ModelBase referenceModel = null;
    protected ServiceDelta inputDelta = null;
    protected ServiceDelta outputDelta = null;
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

    public ServiceDelta getInputDelta() {
        return inputDelta;
    }

    public void setInputDelta(ServiceDelta inputDelta) {
        this.inputDelta = inputDelta;
    }

    public ServiceDelta getOutputDelta() {
        return outputDelta;
    }

    public void setOutputDelta(ServiceDelta outputDelta) {
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
    
    public Future<ServiceDelta> execute() {
        try {
            Context ejbCxt = new InitialContext();
            IModelComputationElement ejbMce = (IModelComputationElement)ejbCxt.lookup(this.mceBeanPath);
            this.state = ActionState.PROCESSING;
            Future<ServiceDelta> asyncResult = ejbMce.process(referenceModel, inputDelta);
            //# not FINISHED yet
            return asyncResult;
        } catch (NamingException ex) {
            this.state = ActionState.FAILED;
            throw new EJBException(this + " failed to invoke MCE bean");
        }        
    }
    
    public void mergeResult(ServiceDelta childDelta) {
        if (inputDelta == null) {
            inputDelta = childDelta;
            return;
        }
        // if outputDelta is null (action not executed) merge to inputDelta
        ServiceDelta targetDelta = inputDelta;
        if (outputDelta != null) // otherwise, merge to outputDelta
            targetDelta = outputDelta;
        // merging models A (childDelta) into B (this.inputDelta)
        // merge addition 
        if (childDelta.getModelAddition() != null && childDelta.getModelAddition().getOntModel() != null &&
                targetDelta.getModelAddition() != null && targetDelta.getModelAddition().getOntModel() != null) {
            OntModel mergedAddition = this.mergeOntModel(childDelta.getModelAddition().getOntModel(), targetDelta.getModelAddition().getOntModel());
            targetDelta.getModelAddition().setOntModel(mergedAddition);
        }
        // merge reduction         
        if (childDelta.getModelReduction()!= null && childDelta.getModelReduction().getOntModel() != null &&
                targetDelta.getModelReduction() != null && targetDelta.getModelReduction().getOntModel() != null) {
            OntModel mergedReduction = this.mergeOntModel(childDelta.getModelReduction().getOntModel(), targetDelta.getModelReduction().getOntModel());
            targetDelta.getModelReduction().setOntModel(mergedReduction);
        }
    }
    
    protected OntModel mergeOntModel(OntModel modelA, OntModel modelB) {
        OntModel mergedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //## simplified merge --> child actions are responsible to remove unneeded annotations
        mergedModel.add(modelA.getBaseModel());        
        mergedModel.add(modelB.getBaseModel());    
        // find resolved (dungling) actions from B
        String sparql = "SELECT ?someResource ?policyAction WHERE {"
                + "?someResource spa:dependOn ?policyAction . "
                + "}";
        OntModel modelAbutB = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        modelAbutB.add(modelA.getBaseModel());
        modelAbutB.remove(modelB);
        ResultSet rs = ModelUtil.sparqlQuery(modelAbutB, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (rs.hasNext()) {
            solutions.add(rs.next());
        }
        // find resolved (dungling) actions from A
        OntModel modelBbutA = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        modelBbutA.add(modelB);
        modelBbutA.remove(modelA.getBaseModel());
        rs = ModelUtil.sparqlQuery(modelBbutA, sparql);
        while (rs.hasNext()) {
            solutions.add(rs.next());
        }
        // remove all ->dependOn->policy statements
        for (QuerySolution querySolution : solutions) {
            Resource resSome = querySolution.get("someResource").asResource();
            Resource resPolicy = querySolution.get("policyAction").asResource();
            mergedModel.remove(resSome, Spa.dependOn, resPolicy);
        }
        try {
            //log.log(Level.INFO, "\n>>>Merge--ModelA=\n" + ModelUtil.marshalModel(modelA.getBaseModel()));
            //log.log(Level.INFO, "\n>>>Merge--ModelB=\n" + ModelUtil.marshalModel(modelB.getBaseModel()));
            //log.log(Level.INFO, "\n>>>Merge--ModelAB=\n" + ModelUtil.marshalModel(mergedModel.getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mergedModel;
    }
    
    public void cleanupOutputDelta() {
        if (this.outputDelta != null) {
            if (this.outputDelta.getModelAddition() != null && this.outputDelta.getModelAddition().getOntModel() != null)
                cleanupSpaModel(this.outputDelta.getModelAddition().getOntModel());
            if (this.outputDelta.getModelReduction() != null && this.outputDelta.getModelReduction().getOntModel() != null)
                cleanupSpaModel(this.outputDelta.getModelReduction().getOntModel());
        }
    }
    
    private void cleanupSpaModel(OntModel spaModel) {
        List<Statement> listStmtsToRemove = new ArrayList<>();
        String sparql = "SELECT ?policyAction ?policyData WHERE {"
                + "{ ?policyAction spa:exportTo ?policyData . "
                //+ "FILTER not exists {?someRes spa:dependOn ?policyAction}"
                + "} UNION {"
                + "?policyAction spa:importFrom ?policyData . "
                //+ "FILTER not exists {?someRes spa:dependOn ?policyAction}"
                + "} }";
        ResultSet rs = ModelUtil.sparqlQuery(spaModel, sparql);
        while (rs.hasNext()) {
            QuerySolution solution = rs.next();
            Resource resPolicy = solution.getResource("policyAction");
            Resource resPolicyData = solution.getResource("policyData");
            List<Statement> listStmts = new ArrayList<>();
            
            resPolicy = spaModel.getResource(resPolicy.getURI());
            ModelUtil.listRecursiveDownTree(resPolicy, Spa.getURI(), listStmts);
            if (!listStmts.isEmpty())
                listStmtsToRemove.addAll(listStmts);
            
            resPolicyData = spaModel.getResource(resPolicyData.getURI());
            ModelUtil.listRecursiveDownTree(resPolicyData, Spa.getURI(), listStmts);
            if (!listStmts.isEmpty())
                listStmtsToRemove.addAll(listStmts);
        }
        
        
        // remove ?resData spa:dependON
        sparql = "SELECT ?resData WHERE {"
                + "?resData spa:dependOn ?somePolicy . "
                + "}";
        rs = ModelUtil.sparqlQuery(spaModel, sparql);
        while (rs.hasNext()){
            QuerySolution solution = rs.next();
            Resource resData = solution.getResource("resData");
            StmtIterator iterDO = spaModel.listStatements(resData, null, (RDFNode)null);
            while(iterDO.hasNext())
                listStmtsToRemove.add(iterDO.next());
            /*
            List<Statement> listStmts = new ArrayList<>();
            resData = spaModel.getResource(resData.getURI());
            ModelUtil.listRecursiveDownTree(resData, Nml.getURI(), listStmts);
            while(!listStmts.isEmpty())
                listStmtsToRemove.addAll(listStmts);
            */
        }
                
        // remove statements of resource of spa#Abstraction type
        sparql = "SELECT ?abstraction WHERE {"
                + String.format("?abstraction a <%s>. ", Spa.Abstraction)
                + "}";
        rs = ModelUtil.sparqlQuery(spaModel, sparql);
        while (rs.hasNext()) {
            QuerySolution solution = rs.next();
            Resource resAbstraction = solution.getResource("abstraction");
            StmtIterator iterAS = spaModel.listStatements(resAbstraction, null, (RDFNode)null);
            while (iterAS.hasNext())
                listStmtsToRemove.add(iterAS.next());
        }
        
        // sanity check
        spaModel.remove(listStmtsToRemove);
        sparql = "SELECT ?policyX WHERE {"
                + "?policyX ?p ?o. "
                + String.format("FILTER(regex(str(?policyX), '^%s'))", Spa.NS)
                + "}";
        rs = ModelUtil.sparqlQuery(spaModel, sparql);
        while (rs.hasNext()) {
            String policyAnotation = rs.next().getResource("policyX").toString();
            throw new EJBException(this + ".cleanupSpaModel() failed to clean up policy annotation: " +  policyAnotation);
        }
     }

    public String toString() {
        return "WorkerAction(" + this.name+"->"+this.mceBeanPath+")";
    }
}
