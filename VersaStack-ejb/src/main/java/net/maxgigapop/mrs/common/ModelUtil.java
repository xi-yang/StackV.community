/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.common;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
/**
 *
 * @author xyang
 */
public class ModelUtil {
    static public OntModel unmarshalOntModel (String ttl) throws Exception {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //$$ TODO: add ontology schema and namespace handling code
        try {
            model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
        }
        return model;
    }
    
    static public String marshalOntModel (OntModel model) throws Exception {
        //$$ TODO: add namespace handling code
        StringWriter out = new StringWriter();
        try {
            model.write(out, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
        }
        String ttl = out.toString();
        return ttl;
    }

    static public boolean isEmptyModel(Model model) {
        if (model == null) {
            return true;
        }
        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            // check subject will be enough
            if (stmt.getSubject().isResource() && stmt.getSubject().toString().contains("ogf.org")) {
                return false;
            }
        }
        return true;
    }

    static public OntModel newMrsOntModel(String topoUri) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        String ttl = String.format("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.\n"
                + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.\n"
                + "@prefix owl: <http://www.w3.org/2002/07/owl#>.\n"
                + "@prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>.\n"
                + "@prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>.\n"
                + "<%s#ontology> a owl:Ontology;\n"
                + "    rdfs:label \"An MRS topology description.\n"
                + "<%s>\n"
                + "    a   nml:Topology,\n"
                + "        owl:NamedIndividual.\n", topoUri);
        //$$ TODO: add ontology schema and namespace handling code
        model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
        return model;
    }
    
    static public OntModel createUnionOntModel(List<OntModel> modelList) {
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        for (OntModel model: modelList) {
            ontModel.addSubModel(model);
        }
        // rebind and run inference?
        return ontModel;
    }
            
    static public Map<String, OntModel> splitOntModelByTopology (OntModel model) {
        Map<String, OntModel> topoModelMap = new HashMap<String, OntModel>();
        List<RDFNode> listTopo = getTopologyList(model);
        if (listTopo == null) {
        	throw new EJBException("ModelUtil.splitOntModelByTopology getTopologyList returns on " + model);
        }
        for (RDFNode topoNode: listTopo) {
        	OntModel modelTopology = getTopology(model, topoNode);
        	model.remove(modelTopology);
            topoModelMap.put(topoNode.asResource().getURI(), modelTopology);
        }
        //verify full decomposition (no nml: mrs: namespace objects left, otherwise thrown exception)
        if (isEmptyModel(model)) {
        	throw new EJBException("ModelUtil.splitOntModelByTopology encounters non-dispatchable nml/mrs objects in " + model);
        }
        return topoModelMap;
    }
    
    private static List<RDFNode> getTopologyList(Model model) {
        String sparqlString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\"\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\"\n" +
                "PREFIX nml: <http://schemas.ogf.org/nml/2013/03/base#>\"\n" +
                "PREFIX mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "SELECT ?topology WHERE {?topology a nml:Topology}";
        Query query = QueryFactory.create(sparqlString);
        List<RDFNode> listRes = null;
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet r = (ResultSet) qexec.execSelect();
        while(r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("topology");
            if (listRes == null)
            	listRes = new ArrayList<RDFNode>();
            listRes.add(node);            
        }
        return listRes;
    }

    private static OntModel getTopology(Model model, RDFNode node) {
        OntModel subModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Set<RDFNode> visited = new HashSet<RDFNode>();
        rdfDFS(node, visited, subModel);
        return subModel;
    }
    
    private static void rdfDFS( RDFNode node, Set<RDFNode> visited, OntModel ontModel) {
        if ( visited.contains( node )) {
            return;
        }
        else {
            visited.add( node );
            if ( node.isResource() ) {
                StmtIterator stmts = node.asResource().listProperties();
                while ( stmts.hasNext() ) {
                    Statement stmt = stmts.next();
                    ontModel.add(stmt);
                    //stmt.remove();
                    if (!stmt.getPredicate().toString().contains("#isAlias")) {
                    	rdfDFS( stmt.getObject(), visited, ontModel);
                    }
                }
            }
        }
    }
}
