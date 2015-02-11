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
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.ModelUtil;

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
    
    protected List<ModelBase> decomposeBySpaActions(OntModel spaModel) {
        List<Resource> spaActions = getSpaActionList(spaModel);
        
        for (Resource resAction : spaActions) {
            // $$ test resAtion is leaf
            // $$ if true, traverse upwards to collect component model
        }
        
        return null; // place holder
    }
    
    protected List<Resource> getSpaActionList(OntModel spaModel) {
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n" +
                "SELECT ?action WHERE {"
                + "?action a spa:Abstraction ."
                + "?action a spa:Placement ."
                + "?action a spa:Connection ."
                + "?action a spa:Stitching ."
                + "}";
        Query query = QueryFactory.create(sparqlString);
        List<Resource> listRes = null;
        QueryExecution qexec = QueryExecutionFactory.create(query, spaModel);
        ResultSet r = (ResultSet) qexec.execSelect();
        while(r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("action");
            if (listRes == null)
            	listRes = new ArrayList<>();
            listRes.add(node.asResource());            
        }
        return listRes;
    }

    protected OntModel getReverseDependencyTree(OntModel spaModel, Resource leaf) {
        OntModel modelPart = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //@@
        return modelPart;
    }
    
    protected OntModel strip(OntModel spaModel) {
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
}
