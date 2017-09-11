/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2013

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
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.bean.DeltaBase;

/**
 *
 * @author xyang
 */
public class ModelUtil {

    static public OntModel unmarshalOntModel(String ttl) throws Exception {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //$$ TODO: add ontology schema and namespace handling code
        try {
            model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
        }
        return model;
    }

    static public String marshalOntModel(OntModel model) throws Exception {
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

    static public OntModel unmarshalOntModelJson(String json) throws Exception {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //$$ TODO: add ontology schema and namespace handling code
        try {
            model.read(new ByteArrayInputStream(json.getBytes()), null, "RDF/JSON");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
        }
        return model;
    }

    static public String marshalOntModelJson(OntModel model) throws Exception {
        //$$ TODO: add namespace handling code
        StringWriter out = new StringWriter();
        try {
            model.write(out, "RDF/JSON");
        } catch (Exception e) {
            throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
        }
        String ttl = out.toString();
        return ttl;
    }

    static public Model unmarshalModel(String ttl) throws Exception {
        Model model = ModelFactory.createDefaultModel();
        //$$ TODO: add ontology schema and namespace handling code
        try {
            model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
        }
        return model;
    }

    static public String marshalModel(Model model) throws Exception {
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

    static public Model unmarshalModelJson(String ttl) throws Exception {
        Model model = ModelFactory.createDefaultModel();
        //$$ TODO: add ontology schema and namespace handling code
        try {
            model.read(new ByteArrayInputStream(ttl.getBytes()), null, "RDF/JSON");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
        }
        return model;
    }

    static public String marshalModelJson(Model model) throws Exception {
        //$$ TODO: add namespace handling code
        StringWriter out = new StringWriter();
        try {
            model.write(out, "RDF/JSON");
        } catch (Exception e) {
            throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
        }
        String ttl = out.toString();
        return ttl;
    }
    
    static public OntModel cloneOntModel(OntModel model) {
        OntModel cloned = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        cloned.add(model.getBaseModel());
        return cloned;
    }

    static public boolean isEmptyModel(Model model) {
        if (model == null) {
            return true;
        }
        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            // check subject will be enough
            if (stmt.getSubject().isResource() && stmt.getPredicate().toString().contains("ogf")) {
                return false;
            }
        }
        return true;
    }

    static public Model getOddModel(Model model) {
        Model odd = ModelFactory.createDefaultModel();
        if (model == null) {
            return odd;
        }
        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            // check subject will be enough
            if (stmt.getSubject().isResource() && stmt.getPredicate().toString().contains("ogf")) {
                odd.add(stmt);
            }
        }
        return odd;
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
                + "    rdfs:label \"An MRS topology description.\".\n"
                + "<%s>\n"
                + "    a   nml:Topology.\n", topoUri, topoUri);
        //$$ TODO: add ontology schema and namespace handling code
        model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
        return model;
    }

    static public void logDumpModel(StackLogger logger, Model model) {
        try {
            logger.trace("logDumpModel", ModelUtil.marshalModel(model));
        } catch (Exception ex) {
            logger.catching("logDumpModel", ex);
        }
    }

    static public ResultSet sparqlQuery(Model model, String sparqlStringWithoutPrefix) {
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "prefix sna: <http://schemas.ogf.org/sna/2015/08/network#>\n"
                + sparqlStringWithoutPrefix;
        Query query = QueryFactory.create(sparqlString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet rs = (ResultSet) qexec.execSelect();
        return rs;
    }

    static public void sparqlExec(Model model, String sparqlStringWithoutPrefix) {
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "prefix sna: <http://schemas.ogf.org/sna/2015/08/network#>\n"
                + sparqlStringWithoutPrefix;
        UpdateRequest update = UpdateFactory.create(sparqlString);
        UpdateAction.execute(update, model);
    }

    static public ResultSet executeQuery(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;
        
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet r = qexec.execSelect();
        //check on reference model if the statement is not in the model
        if (!r.hasNext() && refModel != null && !refModel.isEmpty()) {
            qexec = QueryExecutionFactory.create(query, refModel);
            r = qexec.execSelect();
        }
        return r;
    }

    static public ResultSet executeQueryUnion(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;
        
        Model unionModel = ModelFactory.createUnion(refModel, model);
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, unionModel);
        ResultSet r = qexec.execSelect();
        return r;
    }    
    
    public static boolean evaluateStatement(Model model, Statement stmt, String sparql) throws Exception {
        // static bindings stmt->subject => $$s; stmt->predicate => $$p; $stmt->object => $$o
        // sparql example "SELECT $s $p $o WHERE $s a nml:Topology; $o a nml:Node FILTER ($p = <http://schemas.ogf.org/nml/2013/03/base#hasNode>)"
        sparql = sparql.replace("$$s", stmt.getSubject().getURI());
        sparql = sparql.replace("$$p", stmt.getPredicate().getURI());
        sparql = sparql.replace("$$o", stmt.getObject().toString());
        if (sparql.contains("$$")) {
            throw new Exception(String.format("ModelUtl.evaluateStatementBySparql('%s', '%s'): Binding incomplete", stmt, sparql));
        }
        ResultSet r = ModelUtil.sparqlQuery(model, sparql);
        return r.hasNext();
    }

    public static boolean isResourceOfType(Model model, Resource res, Resource resType) {
        String sparql = String.format("SELECT $s WHERE {$s a $t. FILTER($s = <%s> && $t = <%s>)}", res, resType);
        ResultSet r = ModelUtil.sparqlQuery(model, sparql);
        return r.hasNext();
    }

    static public OntModel createUnionOntModel(List<OntModel> modelList) {
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        for (OntModel model : modelList) {
            ontModel.addSubModel(model);
        }
        // rebind and run inference?
        return ontModel;
    }

    static public Map<String, OntModel> splitOntModelByTopology(OntModel model, DeltaBase delta) throws Exception {
        Map<String, OntModel> topoModelMap = new HashMap<String, OntModel>();
        List<RDFNode> listTopo = getTopologyList(model);
        if (listTopo == null) {
            throw new Exception("ModelUtil.splitOntModelByTopology getTopologyList returns on " + model);
        }
        for (RDFNode topoNode : listTopo) {
            OntModel modelTopology = getTopology(model, topoNode);
            topoModelMap.put(topoNode.asResource().getURI(), modelTopology);
        }
        for (OntModel modelTopology : topoModelMap.values()) {
            model.remove(modelTopology.getBaseModel());
        }

        //verify full decomposition (no nml: mrs: namespace objects left, otherwise thrown exception)
        if (!isEmptyModel(model.getBaseModel())) {
            StringWriter writer1 = new StringWriter();
            if (delta != null) {
                // with delta present, ignore statements which is not in delta
                // throw exception if having odd statement that is also in delta
                if (delta.getModelAddition() != null && delta.getModelAddition().getOntModel() != null) {
                    Model odd = getOddModel(model);
                    if (!odd.isEmpty()) {
                        odd = delta.getModelAddition().getOntModel().intersection(odd);
                    }
                    if (!odd.isEmpty()) {
                        odd.write(writer1, "TURTLE");  
                    }
                }
                if (delta.getModelReduction() != null && delta.getModelReduction().getOntModel() != null ){
                    Model odd = getOddModel(model);
                    if (!odd.isEmpty()) {
                        odd = delta.getModelReduction().getOntModel().intersection(odd);
                    }
                    if (!odd.isEmpty()) {
                        odd.write(writer1, "TURTLE");  
                    }
                }
            } else {
                model.getBaseModel().write(writer1, "TURTLE");                
            }
            if (!writer1.getBuffer().toString().isEmpty()) {
                throw new Exception("ModelUtil.splitOntModelByTopology encounters non-dispatchable nml/mrs objects - " + (delta == null ? "" : writer1.getBuffer().toString()));
            }
        }
        return topoModelMap;
    }

    public static List<RDFNode> getTopologyList(OntModel model) {
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology WHERE {?topology a nml:Topology}";
        Query query = QueryFactory.create(sparqlString);
        List<RDFNode> listRes = null;
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet r = (ResultSet) qexec.execSelect();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode node = querySolution.get("topology");
            ResIterator riParentTopology = model.listResourcesWithProperty(Nml.hasTopology, node);
            // skip non-root Topology
            if (riParentTopology.hasNext()) {
                continue;
            }
            if (listRes == null) {
                listRes = new ArrayList<RDFNode>();
            }
            listRes.add(node);
        }
        return listRes;
    }

    private static OntModel getTopology(Model refModel, RDFNode node) {
        OntModel subModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        Set<RDFNode> visited = new HashSet<RDFNode>();
        List<String> includeMatches = new ArrayList<String>();
        List<String> excludeMatches = new ArrayList<String>();
        List<String> excludeEssentials = new ArrayList<String>();
        excludeMatches.add("#isAlias");
        rdfDFS(refModel, node, visited, subModel, includeMatches, excludeMatches, excludeEssentials);
        return subModel;
    }

    private static boolean isEssentialResource(Model model, Resource res) {
        String sparql = "SELECT ?res WHERE {?s ?p ?res. "
                + String.format("FILTER(regex(str(?p), '#has|#provides') && (?res = <%s>))", res.getURI())
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(model, sparql);
        if (r.hasNext()) {
            return true;
        }
        return false;
    }
    private static void rdfDFS(Model refModel, RDFNode node, Set<RDFNode> visited, Model subModel, List<String> propMatchIncludes, List<String> propMatchExcludes, List<String> propExcludeEssentials) {
        if (visited.contains(node)) {
            return;
        } else {
            visited.add(node);
            if (node.isResource()) {
                StmtIterator stmts = node.asResource().listProperties();
                while (stmts.hasNext()) {
                    Statement stmt = stmts.next();
                    subModel.add(stmt);
                    // included==true if empty list or matched pattern
                    boolean included = propMatchIncludes.isEmpty();
                    for (String matchStr : propMatchIncludes) {
                        if (stmt.getPredicate().toString().contains(matchStr)) {
                            included = true;
                            break;
                        }
                    }
                    // excluded==true only if matched pattern
                    boolean excluded = false;
                    for (String matchStr : propMatchExcludes) {
                        if (stmt.getPredicate().toString().contains(matchStr)) {
                            excluded = true;
                            break;
                        }
                    }
                    for (String matchStr : propExcludeEssentials) {
                        if (stmt.getPredicate().toString().contains(matchStr) && stmt.getObject().isResource()
                                && ModelUtil.isEssentialResource(refModel, stmt.getObject().asResource())) {
                            excluded = true;
                            break;
                        }
                    }
                    if (included && !excluded) {
                        rdfDFS(refModel, stmt.getObject(), visited, subModel, propMatchIncludes, propMatchExcludes, propExcludeEssentials);
                    }
                }
            }
        }
    }

    private static void rdfDFSReverse(Model refModel, RDFNode node, Set<RDFNode> visited, Model subModel, List<String> propMatchIncludes, List<String> propMatchExcludes) {
        if (visited.contains(node)) {
            return;
        } else {
            visited.add(node);
            if (node.isResource()) {
                StmtIterator stmts = refModel.listStatements(null, null, node);
                while (stmts.hasNext()) {
                    Statement stmt = stmts.next();
                    subModel.add(stmt);
                    // optional: add type statements
                    StmtIterator stmts2 = refModel.listStatements(stmt.getSubject(), RdfOwl.type, (RDFNode) null);
                    while (stmts2.hasNext()) {
                        subModel.add(stmts2.next());
                    }
                    boolean included = propMatchIncludes.isEmpty();
                    for (String matchStr : propMatchIncludes) {
                        if (stmt.getPredicate().toString().contains(matchStr)) {
                            included = true;
                            break;
                        }
                    }
                    boolean excluded = false;
                    for (String matchStr : propMatchExcludes) {
                        if (stmt.getPredicate().toString().contains(matchStr)) {
                            excluded = true;
                            break;
                        }
                    }
                    if (included && !excluded) {
                        rdfDFSReverse(refModel, stmt.getSubject(), visited, subModel, propMatchIncludes, propMatchExcludes);
                    }
                }
            }
        }
    }

    public static void listRecursiveDownTree(RDFNode node, Set<RDFNode> visited, List<String> filterList, List<Statement> listStmts) {
        if (visited.contains(node)) {
            return;
        } else {
            visited.add(node);
            if (node.isResource()) {
                StmtIterator stmts = node.asResource().listProperties();
                while (stmts.hasNext()) {
                    Statement stmt = stmts.next();
                    for (String matchStr : filterList) {
                        // match by contains or regex
                        if (stmt.getPredicate().toString().contains(matchStr)) {
                            listStmts.add(stmt);
                            listRecursiveDownTree(stmt.getObject(), visited, filterList, listStmts);
                        } else if (stmt.getPredicate().toString().matches(matchStr)) {
                            listStmts.add(stmt);
                            listRecursiveDownTree(stmt.getObject(), visited, filterList, listStmts);
                        } else if (stmt.getObject().toString().contains(matchStr)) {
                            listStmts.add(stmt);
                        } else if (stmt.getPredicate().toString().matches(matchStr)) {
                            listStmts.add(stmt);
                        }
                    }
                }
            }
        }
    }

    public static void listRecursiveDownTree(RDFNode node, String matchStr, List<Statement> listStmts) {
        Set<RDFNode> visited = new HashSet<>();
        List filterList = new ArrayList<>();
        filterList.add(matchStr);
        listRecursiveDownTree(node, visited, filterList, listStmts);
    }

    static public class ModelViewFilter {

        long seqNum = 0;
        String sparql = null;
        boolean inclusive = true;
        boolean subtreeRecursive = false;
        boolean suptreeRecursive = false;

        public long getSeqNum() {
            return seqNum;
        }

        public void setSeqNum(long seqNum) {
            this.seqNum = seqNum;
        }

        public String getSparql() {
            return sparql;
        }

        public void setSparql(String sparql) {
            this.sparql = sparql;
        }

        public boolean isInclusive() {
            return inclusive;
        }

        public void setInclusive(boolean inclusive) {
            this.inclusive = inclusive;
        }

        public boolean isSubtreeRecursive() {
            return subtreeRecursive;
        }

        public void setSubtreeRecursive(boolean subtreeRecursive) {
            this.subtreeRecursive = subtreeRecursive;
        }

        public boolean isSuptreeRecursive() {
            return suptreeRecursive;
        }

        public void setSuptreeRecursive(boolean suptreeRecursive) {
            this.suptreeRecursive = suptreeRecursive;
        }
    }

    public static OntModel queryViewFilter(OntModel model, ModelViewFilter mvf) throws Exception {
        if (mvf.getSparql() == null || mvf.getSparql().isEmpty() || (!mvf.getSparql().contains("CONSTRUCT") && !mvf.getSparql().contains("construct"))) {
            throw new Exception(String.format("ModelViewFilter(#%d) has empty or none-construct SPARQL", mvf.getSeqNum()));
        }
        String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n"
                + "prefix sna: <http://schemas.ogf.org/sna/2015/08/network#>\n" + mvf.getSparql();
        Query query = QueryFactory.create(sparql);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        Model modelConstructed = qexec.execConstruct();
        ResIterator resIter = modelConstructed.listResourcesWithProperty(Nml.hasService);
        List<String> includeMatches = new ArrayList<String>();
        List<String> excludeMatches = new ArrayList<String>();
        List<String> excludeEssentials = new ArrayList<String>();
        includeMatches.add("#has");
        includeMatches.add("#provide");
        includeMatches.add("#type");
        includeMatches.add("#value");
        Set<RDFNode> visited = new HashSet<RDFNode>();
        while (resIter.hasNext()) {
            Resource node = resIter.next();
            visited.clear();
            if (mvf.isSubtreeRecursive()) {
                Model subModel = ModelFactory.createDefaultModel();
                node = model.getResource(node.toString());
                rdfDFS(model, node, visited, subModel, includeMatches, excludeMatches, excludeEssentials);
                modelConstructed.add(subModel);
            }
            visited.clear();
            if (mvf.isSuptreeRecursive()) {
                Model subModel = ModelFactory.createDefaultModel();
                rdfDFSReverse(model, node, visited, subModel, includeMatches, excludeMatches);
                modelConstructed.add(subModel);
            }
        }
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        if (mvf.isInclusive()) {
            ontModel.add(modelConstructed);
        } else {
            ontModel.remove(modelConstructed);
        }
        return ontModel;
    }

    public static Model getModelSubTree(Model refModel, List<Resource> resList, List<String> includeMatches, List<String> excludeMatches, List<String> excludeExtentials) {
        Model retModel = ModelFactory.createDefaultModel();
        Set<RDFNode> visited = new HashSet<RDFNode>();
        Iterator<Resource> resIter = resList.iterator();
        while (resIter.hasNext()) {
            Resource node = resIter.next();
            //visited.clear();
            Model subModel = ModelFactory.createDefaultModel();
            node = refModel.getResource(node.toString());
            rdfDFS(refModel, node, visited, subModel, includeMatches, excludeMatches, excludeExtentials);
            retModel.add(subModel);
        }
        return retModel;
    }
            
    public static String modelDateToString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return dateFormat.format(date).toString();
    }

    public static Date modelDateFromString(String str) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return dateFormat.parse(str);
    }
    
    static public class DeltaVerification {
        protected String modelAdditionVerified = null;
        protected String modelReductionVerified = null;
        protected String modelAdditionUnverified = null;
        protected String modelReductionUnverified = null;
        protected Boolean additionVerified = null;
        protected Boolean reductionVerified = null;

        public String getModelAdditionVerified() {
            return modelAdditionVerified;
        }

        public void setModelAdditionVerified(String modelAdditionVerified) {
            this.modelAdditionVerified = modelAdditionVerified;
        }

        public String getModelReductionVerified() {
            return modelReductionVerified;
        }

        public void setModelReductionVerified(String modelReductionVerified) {
            this.modelReductionVerified = modelReductionVerified;
        }

        public String getModelAdditionUnverified() {
            return modelAdditionUnverified;
        }

        public void setModelAdditionUnverified(String modelAdditionUnverified) {
            this.modelAdditionUnverified = modelAdditionUnverified;
        }

        public String getModelReductionUnverified() {
            return modelReductionUnverified;
        }

        public void setModelReductionUnverified(String modelReductionUnverified) {
            this.modelReductionUnverified = modelReductionUnverified;
        }

        public Boolean getAdditionVerified() {
            return additionVerified;
        }

        public void setAdditionVerified(Boolean additionVerified) {
            this.additionVerified = additionVerified;
        }

        public Boolean getReductionVerified() {
            return reductionVerified;
        }

        public void setReductionVerified(Boolean reductionVerified) {
            this.reductionVerified = reductionVerified;
        }

    }
    
    static public class DeltaRetrieval {
        protected String modelAdditionSvc = null;
        protected String modelReductionSvc = null;
        protected String modelAdditionSys = null;
        protected String modelReductionSys = null;
        protected String referenceModelUUID = null;

        public String getModelAdditionSvc() {
            return modelAdditionSvc;
        }

        public void setModelAdditionSvc(String modelAdditionSvc) {
            this.modelAdditionSvc = modelAdditionSvc;
        }

        public String getModelReductionSvc() {
            return modelReductionSvc;
        }

        public void setModelReductionSvc(String modelReductionSvc) {
            this.modelReductionSvc = modelReductionSvc;
        }

        public String getModelAdditionSys() {
            return modelAdditionSys;
        }

        public void setModelAdditionSys(String modelAdditionSys) {
            this.modelAdditionSys = modelAdditionSys;
        }

        public String getModelReductionSys() {
            return modelReductionSys;
        }

        public void setModelReductionSys(String modelReductionSys) {
            this.modelReductionSys = modelReductionSys;
        }

        public String getReferenceModelUUID() {
            return referenceModelUUID;
        }

        public void setReferenceModelUUID(String referenceModelUUID) {
            this.referenceModelUUID = referenceModelUUID;
        }

    }
}
