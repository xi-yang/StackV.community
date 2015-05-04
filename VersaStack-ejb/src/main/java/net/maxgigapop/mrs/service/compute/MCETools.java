/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntTools;
import static com.hp.hpl.jena.ontology.OntTools.findShortestPath;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.Filter;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author xyang
 */
public class MCETools {
    public static class Path extends com.hp.hpl.jena.ontology.OntTools.Path {
        private HashSet<Statement> maskedLinks = null;
        Resource deviationNode = null;

        public Path() {
            super();
        }
        
        public Path(OntTools.Path basePath) {
            super(basePath);
        }
        
        public HashSet<Statement> getMaskedLinks() {
            return maskedLinks;
        }

        public void setMaskedLinks(HashSet<Statement> maskedLinks) {
            this.maskedLinks = maskedLinks;
        }

        public Resource getDeviationNode() {
            return deviationNode;
        }

        public void setDeviationNode(Resource deviationNode) {
            this.deviationNode = deviationNode;
        }
        
    }

    public static Path getLeastCostPath(List<Path> candidates) {
        long cost = 1000000;
        Path solution = null;
        for (Path path: candidates) {
            if (path.size() < cost) {
                cost = path.size();
                solution = path;
            }
        }
        return solution;
    }
    
    public static Path findShortestPath(Model model, Resource nodeA, Resource nodeZ, Filter<Statement> filters) {
        if (filters == null)
            filters = Filter.any();
        OntTools.Path path = OntTools.findShortestPath(model, nodeA, nodeZ, filters);
        if (path == null)
            return null;
        return new MCETools.Path(path);
    }
    
    final public static int KSP_K_DEFAULT = 20;
    public static List<Path> computeKShortestPaths(Model model, Resource nodeA, Resource nodeZ, int K, Filter<Statement> filters) {
        final HashSet<Statement> maskedLinks = new HashSet<>();
        List<Path> KSP = new ArrayList<>();
        List<Path> candidatePaths = new ArrayList<>();
        // find the first shortest path
        Path nextPath = findShortestPath(model, nodeA, nodeZ, filters);
        if (nextPath == null)
            return null;
        KSP.add(nextPath);
        candidatePaths.add(nextPath);
        int kspCounter = 1;
        while (!candidatePaths.isEmpty() && KSP.size() <= K) {
            Path headPath = getLeastCostPath(candidatePaths);
            candidatePaths.remove(headPath);
            if (kspCounter > 1) 
                KSP.add(headPath);
            if (kspCounter == K) 
                break;
            for (Statement stmtLink: headPath) {
                if (headPath.getDeviationNode() != null) {
                    if (stmtLink.getSubject().equals(headPath.getDeviationNode()))
                        break;
                }
                // mask (filter out) all statments to and from the localEnd of link
                StmtIterator itStmt = model.listStatements(stmtLink.getSubject(), null, (Resource)null);
                if (itStmt.hasNext()) {
                    Statement linkToMask = itStmt.next();
                    maskedLinks.add(linkToMask);
                    model = model.remove(linkToMask);
                }
                itStmt = model.listStatements(null, null, stmtLink.getSubject());
                if (itStmt.hasNext()) {
                    Statement linkToMask = itStmt.next();
                    maskedLinks.add(linkToMask);
                    model = model.remove(linkToMask);
                }
            }
            for (Statement stmtLink: headPath) {
                // filter out masked links in headPath 
                if (headPath.getMaskedLinks() != null) {
                    maskedLinks.addAll(headPath.getMaskedLinks());
                    for (Statement stmtMaskedLink: headPath.getMaskedLinks())
                        model = model.remove(stmtMaskedLink);
                }
                // mask current link
                model = model.remove(stmtLink);
                maskedLinks.add(stmtLink);
                Path deviationPath = findShortestPath(model, stmtLink.getSubject(), headPath.getTerminalResource(), filters);
                nextPath = new Path();
                if (deviationPath != null) {
                    if (!stmtLink.equals(headPath.get(0))) {
                        // add headPath[begin : stmtLink) to nextPath
                        for (int i = 0; i < headPath.size(); i++) {
                            if (headPath.get(i).equals(stmtLink))
                                break;
                            nextPath.add(headPath.get(i));
                        }
                    }
                    // keep record for deviation node of nextPath
                    nextPath.setDeviationNode(stmtLink.getSubject());
                    // append deviationPath to nextPath 
                    nextPath.addAll(deviationPath);
                    // adjust masking for nextPath
                    if (nextPath.getMaskedLinks() == null) {
                        nextPath.setMaskedLinks(new HashSet<Statement>());
                    }
                    if (headPath.getMaskedLinks() != null) {
                        nextPath.getMaskedLinks().addAll(headPath.getMaskedLinks());
                    }
                    nextPath.getMaskedLinks().add(stmtLink);
                    // add another candiate path
                    candidatePaths.add(nextPath);
                }
                //?? add back current link?
                //model.add(stmtLink);
                //maskedLinks.remove(stmtLink);
            }
            kspCounter++;
        }
        //model.write(System.out, "TURTLE");
        for (Statement stmtMaskedLink: maskedLinks)
            model.add(stmtMaskedLink);
        //model.write(System.out, "TURTLE");
        return KSP;
    }

    private static String l2NetworkConstructSparql = 
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
    		+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
    		+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
    		+ "PREFIX nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
    		+ "PREFIX mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
            + "CONSTRUCT {\n"
            + "# get Topology/Node -> (SwService -> *) / (BidirectionalPort -> *)\n"
            + "  ?node nml:hasService ?swsvc. \n"
            + "  ?node nml:hasBidirectionalPort ?biport . \n"
            + "  ?swsvc a nml:SwitchingService . \n"
            + "  ?swsvc ?nml_p1 ?nml_o1 . \n"
            + "  ?biport a nml:BidirectionalPort . \n"
            + "  ?biport ?nml_p2 ?nml_o2 . \n"
            + "# get SwSubnet as well\n"
            + "  ?swsvc mrs:providesSubnet ?subnet . \n"
            + "  ?subnet a mrs:SwitchingSubnet .\n"
            + "  ?subnet ?nml_p2_1 ?nml_o2_1 .\n"
            + "# get BidirectionalPort -> Label/LabelGroup -> *\n"
            + "  ?label a ?label_or_labelgroup .\n"
            + "  ?label ?nml_p3 ?nml_o3 . \n"
            + "# TODO: get everything under mrs:(De)AdaptationService\n"
            + "} WHERE {\n"
            + "  { ?node a  ?nodetype.\n"
            + "    ?node nml:hasService ?swsvc . \n"
            + "    ?swsvc a nml:SwitchingService . \n"
            + "    ?swsvc ?nml_p1 ?nml_o1 . \n"
            + "    ?swsvc nml:encoding <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> . \n"
            + "    ?node nml:hasBidirectionalPort ?biport . \n"
            + "    ?biport a nml:BidirectionalPort . \n"
            + "    ?biport ?nml_p2 ?nml_o2 . \n"
            + "    OPTIONAL {\n"
            + "      ?swsvc mrs:providesSubnet ?subnet . \n"
            + "      ?subnet a mrs:SwitchingSubnet .\n"
            + "      ?subnet ?nml_p2_1 ?nml_o2_1 .\n"
            + "      FILTER ((REGEX(STR(?nml_p2_1), '^http://schemas.ogf.org/nml/2013/03/base#')))\n"
            + "    }\n"
            + "    FILTER ((?nodetype in (nml:Topology, nml:Node)) &&\n"
            + "        (REGEX(STR(?nml_p1), '^http://schemas.ogf.org/nml/2013/03/base#')) &&\n"
            + "        (REGEX(STR(?nml_p2), '^http://schemas.ogf.org/nml/2013/03/base#'))\n"
            + "    )\n"
            + "  } UNION {\n"
            + "    ?biport a nml:BidirectionalPort . \n"
            + "    ?biport ?haslabel_or_labelgroup ?label . \n"
            + "    ?label a ?label_or_labelgroup .\n"
            + "    ?label ?nml_p3 ?nml_o3 . \n"
            + "    FILTER ((?label_or_labelgroup in (nml:Label, nml:LabelGroup)) &&\n"
            + "    	(?haslabel_or_labelgroup in (nml:hasLabel, nml:hasLabelGroup)) &&\n"
            + "    	(REGEX(STR(?nml_p3), '^http://schemas.ogf.org/nml/2013/03/base#'))\n"
            + "    )\n"
            + "  }\n"
            + "}";
    
    private static String l2NetworkReasonerRules = 
            "[rule1:  (?a http://schemas.ogf.org/nml/2013/03/base#hasService ?b) \n"
            + "         (?b http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort ?c)\n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort ?c)\n"
            + "]\n"
            + "[rule2:  (?a http://schemas.ogf.org/nml/2013/03/base#hasService ?b) \n"
            + "	 (?b http://schemas.ogf.org/mrs/2013/12/topology#providesVNic ?c) \n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort ?c)\n"
            + "]\n"
            + "[rule3:  (?a http://schemas.ogf.org/nml/2013/03/base#hasService ?b) \n"
            + "	 (?b http://schemas.ogf.org/mrs/2013/12/topology#providesVM ?c) \n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#hasNode ?c)\n"
            + "]\n"
            + "[rule4:  (?a http://schemas.ogf.org/nml/2013/03/base#hasTopology ?b)\n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#connectsTo ?b)\n"
            + "         (?b http://schemas.ogf.org/nml/2013/03/base#connectsTo ?a)\n"
            + "]\n"
            + "[rule5:  (?a http://schemas.ogf.org/nml/2013/03/base#hasNode ?b)\n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#connectsTo ?b)\n"
            + "         (?b http://schemas.ogf.org/nml/2013/03/base#connectsTo ?a)\n"
            + "]\n"
            + "[rule6:  (?a http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort ?b)\n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#connectsTo ?b)\n"
            + "         (?b http://schemas.ogf.org/nml/2013/03/base#connectsTo ?a)\n"
            + "]\n"
            + "[rule7:  (?a http://schemas.ogf.org/nml/2013/03/base#isAlias ?b) \n"
            + "      -> (?b http://schemas.ogf.org/nml/2013/03/base#isAlias ?a)\n"
            + "         (?a http://schemas.ogf.org/nml/2013/03/base#connectsTo ?b)\n"
            + "         (?b http://schemas.ogf.org/nml/2013/03/base#connectsTo ?a)\n"
            + "]\n"
            + "[rule8:  (?a http://schemas.ogf.org/nml/2013/03/base#hasService ?b) \n"
            + "	 (?b http://schemas.ogf.org/mrs/2013/12/topology#providesSubnet ?c) \n"
            + "         (?c http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort ?d)\n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#connectsTo ?d)\n"
            + "         (?d http://schemas.ogf.org/nml/2013/03/base#connectsTo ?a)\n"
            + "]";
    
    public static OntModel transformL2NetworkModel(Model inputModel) {
        Query query = QueryFactory.create(l2NetworkConstructSparql);
        QueryExecution qexec = QueryExecutionFactory.create(query, inputModel);
        Model modelConstructed = qexec.execConstruct();

        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(l2NetworkReasonerRules));
        reasoner.setDerivationLogging(true);
        InfModel infModel = ModelFactory.createInfModel(reasoner, modelConstructed);

        OntModel outputModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        outputModel.add(infModel);
        return outputModel;
    }
}
