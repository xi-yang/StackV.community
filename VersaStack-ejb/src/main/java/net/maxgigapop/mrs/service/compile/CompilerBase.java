/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import net.maxgigapop.mrs.service.compute.MCE_MPVlanConnection;

/**
 *
 * @author xyang
 */
public class CompilerBase {
    private static final Logger log = Logger.getLogger(MCE_MPVlanConnection.class.getName());
    protected ServiceDelta spaDelta = null;

    public ServiceDelta getSpaDelta() {
        return spaDelta;
    }

    public void setSpaDelta(ServiceDelta spaDelta) {
        this.spaDelta = spaDelta;
    }

    public void compile(WorkerBase worker) {
        throw new EJBException("CompilerBase::compile() is abstract. Use a specific implementation instead!");
    }

    //@TODO: add back all the non-spa statements
    protected Map<Resource, OntModel> decomposeByPolicyActions(OntModel spaModel) {
        Map<Resource, OntModel> leafPolicyModelMap = null;
        List<Resource> spaActions = getPolicyActionList(spaModel);
        if (spaActions == null) {
            throw new EJBException("CompilerBase::decomposeByPolicyActions() found none terminal / leaf action!");
        }
        for (Resource policy : spaActions) {
            // test resAtion is terminal/leaf
            if (this.isLeafPolicy(spaModel, policy)) {
                OntModel modelPart = getReverseDependencyTree(spaModel, policy);
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
                if (leafPolicyModelMap == null) {
                    leafPolicyModelMap = new HashMap<>();
                }
                leafPolicyModelMap.put(policy, modelPart);
            }
        }

        return leafPolicyModelMap;
    }

    protected boolean isPolicyAction(OntModel spaModel, Resource res) {
        NodeIterator nodeIter = spaModel.listObjectsOfProperty(res, RdfOwl.type);
        while (nodeIter.hasNext()) {
            RDFNode node = nodeIter.next();
            if (node.isResource()
                    && (node.asResource().getURI().contains("spa#PolicyAction")
                    || node.asResource().getURI().contains("spa#Abstraction"))) {
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

    protected OntModel getReverseDependencyTree(OntModel spaModel, Resource leaf) {
        OntModel modelPart = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        List<Resource> visited = new ArrayList<>();
        List<Statement> stmts = new ArrayList<>();
        reverseDFS(spaModel, leaf, visited, stmts);
        modelPart.add(stmts);
        return modelPart;
    }

    private void reverseDFS(OntModel ontModel, Resource res, List<Resource> visited, List<Statement> allStmts) {
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

    protected List<Statement> listUpDownStatements(OntModel model, Resource res) {
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
        // nml/mrs statements to be added back later
        /*
        // add mrs:type statement for res type
        its = model.listStatements(res, Mrs.type, (Resource) null);
        while (its.hasNext()) {
            Statement stmt = its.next();
            if (listStmt == null) {
                listStmt = new ArrayList<>();
            }
            listStmt.add(stmt);
        }
        // add spa:value statement for res type
        its = model.listStatements(res, Mrs.value, (Resource) null);
        while (its.hasNext()) {
            Statement stmt = its.next();
            if (listStmt == null) {
                listStmt = new ArrayList<>();
            }
            listStmt.add(stmt);
        }
        */
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
