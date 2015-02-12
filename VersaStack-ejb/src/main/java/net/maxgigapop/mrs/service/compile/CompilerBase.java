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
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DeltaBase;
/**
 *
 * @author xyang
 */
public class CompilerBase {
    protected DeltaBase spaDelta = null;

    public CompilerBase(DeltaBase spaDelta) {
        this.spaDelta = spaDelta;
    }

    public DeltaBase getSpaDelta() {
        return spaDelta;
    }

    public void setSpaDelta(DeltaBase spaDelta) {
        this.spaDelta = spaDelta;
    }

    public void compile() {
        throw new EJBException("CompilerBase::compile() is abstract. Use a specific implementation instead!");
    }
    
    protected List<OntModel> decomposeByPolicyActions(OntModel spaModel) {
        List<OntModel> listModelParts = null;
        List<Resource> spaActions = getPolicyActionList(spaModel);
        for (Resource policy : spaActions) {
            // test resAtion is terminal/leaf
            if (this.isLeafPolicy(spaModel, policy)) {
                OntModel modelPart = getReverseDependencyTree(spaModel, policy);
                if (listModelParts == null)
                    listModelParts = new ArrayList<>();
                listModelParts.add(modelPart);
            }
        }
        
        return listModelParts;
    }
    
    protected List<Resource> getPolicyActionList(OntModel spaModel) {
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n" +
                "SELECT ?policy WHERE {"
                + "?policy a spa:Abstraction ."
                + "?policy a spa:Placement ."
                + "?policy a spa:Connection ."
                + "?policy a spa:Stitching ."
                + "}";
        Query query = QueryFactory.create(sparqlString);
        List<Resource> listRes = null;
        QueryExecution qexec = QueryExecutionFactory.create(query, spaModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        while(r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("policy");
            if (listRes == null)
            	listRes = new ArrayList<>();
            listRes.add(node.asResource());            
        }
        return listRes;
    }

    protected boolean isLeafPolicy(OntModel spaModel, Resource resPolicy) {
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n" +
                String.format("SELECT ?o WHERE {<%s> spa:dependOn ?o . <%s> spa:importFrom ?o . }", resPolicy.toString(), resPolicy.toString());
        Query query = QueryFactory.create(sparqlString);
        List<Resource> listRes = null;
        QueryExecution qexec = QueryExecutionFactory.create(query, spaModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        if(r.hasNext())
            return false;
        return true;
    }
    
    protected OntModel stripPolicyAnnotation(OntModel spaModel) {
        OntModel newModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, spaModel);
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n" +
                "DELETE WHERE {?s ?p ?o . FILTER regex(?p , '^spa:', 'i')}";
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
        if ( visited.contains( res )) {
            return;
        }
        else {
            visited.add( res );
            List<Statement> listStmt = this.listUpDownStatements(ontModel, res);
            if (listStmt != null) {
                allStmts.addAll(listStmt);
                for (Statement stmt: listStmt) {
                    reverseDFS(ontModel, stmt.getSubject(), visited, allStmts);
                }
            }
        }
    }

    private List<Statement> listUpDownStatements(OntModel model, Resource res) {
        List<Statement> listStmt = null; 
        StmtIterator its = model.listStatements(null, null, res);
        while (its.hasNext()) {
            Statement stmt = its.next();
            Resource subject = stmt.getSubject();
            Property predicate = stmt.getPredicate();
            if (predicate.getURI().contains("spa#") || predicate.getURI().contains("#has")) {
                if (listStmt == null)
                    listStmt = new ArrayList<>();
                listStmt.add(stmt);
            }
        }
        return listStmt;
    }
}
