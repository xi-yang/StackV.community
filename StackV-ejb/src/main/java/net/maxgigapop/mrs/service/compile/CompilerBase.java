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
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.service.orchestrate.ActionBase;
import net.maxgigapop.mrs.service.orchestrate.WorkerBase;
import net.maxgigapop.mrs.common.*;

/**
 *
 * @author xyang
 */
public class CompilerBase {
    
    private static final StackLogger logger = new StackLogger(CompilerBase.class.getName(), "CompilerBase");
    protected ServiceDelta spaDelta = null;

    public ServiceDelta getSpaDelta() {
        return spaDelta;
    }

    public void setSpaDelta(ServiceDelta spaDelta) {
        this.spaDelta = spaDelta;
    }

    public void compile(WorkerBase worker) {
        throw logger.error_throwing("compile", "Cannot call abstract method. Use a specific implementation instead.");
    }

    protected Map<Resource, OntModel> decomposeByPolicyActions(OntModel spaModel) {
        String method = "decomposeByPolicyActions";
        logger.refuuid(spaDelta.getServiceInstance().getReferenceUUID());
        logger.targetid(spaDelta.getReferenceUUID());
        Map<Resource, OntModel> leafPolicyModelMap = null;
        List<Resource> spaActions = getPolicyActionList(spaModel);
        if (spaActions == null) {
            throw logger.error_throwing(method, "Found none terminal / leaf action.");
        }
        for (Resource policy : spaActions) {
            // test resAtion is terminal/leaf
            if (this.isLeafPolicy(spaModel, policy)) {
                OntModel modelPart = getModelPartByPolicy(spaModel, policy);
                if (leafPolicyModelMap == null) {
                    leafPolicyModelMap = new HashMap<>();
                }
                leafPolicyModelMap.put(policy, modelPart);
            }
        }

        return leafPolicyModelMap;
    }

    protected OntModel getModelPartByPolicy(OntModel spaModel, Resource policy) {
        String method = "getModelPartByPolicy";
        logger.refuuid(spaDelta.getServiceInstance().getReferenceUUID());
        logger.targetid(spaDelta.getReferenceUUID());
        OntModel modelPart;
        try {
            modelPart = getReverseDependencyTree(spaModel, policy);
        } catch (Exception ex) {
            throw logger.error_throwing(method, String.format("getReverseDependencyTree(%s) -exception- %s", policy, ex));
        }
        StmtIterator stmtIter = spaModel.listStatements(policy, Spa.importFrom, (Resource) null);
        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.next();
            modelPart.add(stmt);
            StmtIterator stmtIter2 = spaModel.listStatements(stmt.getObject().asResource(), null, (Resource) null);
            while (stmtIter2.hasNext()) {
                modelPart.add(stmtIter2.next());
            }
        }
        stmtIter = spaModel.listStatements(policy, Spa.exportTo, (Resource) null);
        while (stmtIter.hasNext()) {
            Statement stmt = stmtIter.next();
            modelPart.add(stmt);
            StmtIterator stmtIter2 = spaModel.listStatements(stmt.getObject().asResource(), null, (Resource) null);
            while (stmtIter2.hasNext()) {
                modelPart.add(stmtIter2.next());
            }
        }
        logger.trace(method, "returned model part for policy="+policy);
        return modelPart;
    }
    
    protected boolean isPolicyAction(OntModel spaModel, Resource res) {
        NodeIterator nodeIter = spaModel.listObjectsOfProperty(res, RdfOwl.type);
        while (nodeIter.hasNext()) {
            RDFNode node = nodeIter.next();
            if (node.isResource() && node.asResource().getURI().contains("spa#PolicyAction")) {
                return true;
            }
        }
        return false;
    }

    protected List<Resource> getPolicyActionList(OntModel spaModel) {
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "SELECT ?policy WHERE {"
                + "{?policy a spa:PolicyAction} UNION"
                + "{?policy a spa:Abstraction}"
                + "}";
        Query query = QueryFactory.create(sparqlString);
        List<Resource> listRes = null;
        QueryExecution qexec = QueryExecutionFactory.create(query, spaModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("policy");
            if (listRes == null) {
                listRes = new ArrayList<>();
            }
            listRes.add(node.asResource());
        }
        return listRes;
    }

    protected boolean isLeafPolicy(OntModel spaModel, Resource resPolicy) {
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + String.format("SELECT ?o WHERE { <%s> spa:dependOn ?o }", resPolicy.toString(), resPolicy.toString());
        Query query = QueryFactory.create(sparqlString);
        QueryExecution qexec = QueryExecutionFactory.create(query, spaModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if (r.hasNext()) {
            return false;
        }
        return true;
    }

    protected OntModel stripPolicyAnnotation(OntModel spaModel) {
        OntModel newModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, spaModel);
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "DELETE WHERE {?s ?p ?o . FILTER regex(?p , '^spa:', 'i')}";
        UpdateRequest update = UpdateFactory.create(sparqlString);
        UpdateAction.execute(update, newModel);
        return newModel;
    }

    protected OntModel getReverseDependencyTree(OntModel spaModel, Resource leaf) throws Exception {
        OntModel modelPart = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        List<Resource> visited = new ArrayList<>();
        List<Statement> stmts = new ArrayList<>();
        reverseDFS(spaModel, leaf, visited, stmts);
        modelPart.add(stmts);
        return modelPart;
    }

    private void reverseDFS(OntModel ontModel, Resource res, List<Resource> visited, List<Statement> allStmts) throws Exception {
        if (visited.contains(res)) {
            return;
        } else {
            visited.add(res);
            List<Statement> listStmt = this.listUpDownStatements(ontModel, res);
            if (listStmt != null) {
                allStmts.addAll(listStmt);
                for (Statement stmt : listStmt) {
                    reverseDFS(ontModel, stmt.getSubject(), visited, allStmts);
                }
            }
        }
    }

    public List<Statement> listUpDownStatements(OntModel model, Resource res) throws Exception {
        List<Statement> listStmt = null;
        StmtIterator its = model.listStatements(null, null, res);
        while (its.hasNext()) {
            Statement stmt = its.next();
            Property predicate = stmt.getPredicate();
            if (predicate.getURI().contains("/nml/")
                    || predicate.getURI().contains("/mrs/")
                    || predicate.getURI().contains("spa#")) {
                if (listStmt == null) {
                    listStmt = new ArrayList<>();
                }
                listStmt.add(stmt);
            }
        }
        // get spa# subtree for policyData 
        // add exportTo and related policyData statements
        its = model.listStatements(res, Spa.exportTo, (Resource) null);
        while (its.hasNext()) {
            Statement stmt = its.next();
            if (!stmt.getObject().isResource()) {
                continue;
            }
            if (listStmt == null) {
                throw new Exception("CompilerBase::listUpDownStatements() found none resource refering to policy action: " + res);
            }
            listStmt.add(stmt);
            Resource object = stmt.getObject().asResource();
            StmtIterator its2 = object.listProperties();
            while (its2.hasNext()) {
                listStmt.add(its2.next());
            }
        }
        // add importFrom and related policyData statements
        its = model.listStatements(res, Spa.importFrom, (Resource) null);
        while (its.hasNext()) {
            Statement stmt = its.next();
            if (!stmt.getObject().isResource()) {
                continue;
            }
            if (listStmt == null) {
                throw new Exception("CompilerBase::listUpDownStatements() found none resource refering to policy action: " + res);
            }
            listStmt.add(stmt);
            Resource object = stmt.getObject().asResource();
            StmtIterator its2 = object.listProperties();
            while (its2.hasNext()) {
                listStmt.add(its2.next());
            }
        }
        // add a statement for res type
        its = model.listStatements(res, RdfOwl.type, (Resource) null);
        while (its.hasNext()) {
            Statement stmt = its.next();
            if (listStmt == null) {
                listStmt = new ArrayList<>();
            }
            listStmt.add(stmt);
        }
        its = model.listStatements(res, Spa.type, (Resource) null);
        while (its.hasNext()) {
            Statement stmt = its.next();
            if (listStmt == null) {
                listStmt = new ArrayList<>();
            }
            listStmt.add(stmt);
        }
        // add spa:value statement for res type
        its = model.listStatements(res, Spa.value, (Resource) null);
        while (its.hasNext()) {
            Statement stmt = its.next();
            if (listStmt == null) {
                listStmt = new ArrayList<>();
            }
            listStmt.add(stmt);
        }
        return listStmt;
    }

    protected List<Resource> listParentPolicies(OntModel model, Resource res) {
        List<Resource> listParents = null;
        StmtIterator its = model.listStatements(null, Spa.dependOn, res);
        while (its.hasNext()) {
            Statement stmt = its.next();
            Resource subject = stmt.getSubject();
            if (!isPolicyAction(model, subject)) {
                continue;
            }
            if (listParents == null) {
                listParents = new ArrayList<>();
            }
            listParents.add(stmt.getSubject());
        }
        return listParents;
    }

    protected List<Resource> listChildPolicies(OntModel model, Resource res) {
        List<Resource> listChildren = null;
        NodeIterator itn = model.listObjectsOfProperty(res, Spa.dependOn);
        while (itn.hasNext()) {
            Resource child = itn.next().asResource();
            if (!isPolicyAction(model, child)) {
                continue;
            }
            if (listChildren == null) {
                listChildren = new ArrayList<>();
            }
            listChildren.add(child);
        }
        return listChildren;
    }
}
