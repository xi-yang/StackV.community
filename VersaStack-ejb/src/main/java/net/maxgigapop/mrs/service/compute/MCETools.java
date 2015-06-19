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
import com.hp.hpl.jena.query.ResultSet;
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.TagSet;

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

    private static String l2NetworkConstructSparql
            = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
            + "PREFIX mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
            + "CONSTRUCT {\n"
            + "# get Topology/Node -> (SwService -> *) / (BidirectionalPort -> *)\n"
            + "  ?topo ?has_node_or_topology ?node.\n"
            + "  ?node nml:hasService ?swsvc.\n"
            + "  ?node nml:hasBidirectionalPort ?biport .\n"
            + "  ?swsvc a nml:SwitchingService .\n"
            + "  ?swsvc ?nml_p1 ?nml_o1 .\n"
            + "  ?biport a nml:BidirectionalPort .\n"
            + "  ?biport ?nml_p2 ?nml_o2 .\n"
            + "# get SwSubnet as well\n"
            + "  ?swsvc mrs:providesSubnet ?subnet .\n"
            + "  ?subnet a mrs:SwitchingSubnet .\n"
            + "  ?subnet ?nml_p2_1 ?nml_o2_1 .\n"
            + "# get BidirectionalPort -> Label/LabelGroup -> *\n"
            + "  ?label a ?label_or_labelgroup .\n"
            + "  ?label ?nml_p3 ?nml_o3 .\n"
            + "# TODO: get everything under mrs:(De)AdaptationService\n"
            + "} WHERE {\n"
            + "  { ?node a  ?nodetype.\n"
            + "    ?node nml:hasService ?swsvc .\n"
            + "    ?swsvc a nml:SwitchingService .\n"
            + "    ?swsvc ?nml_p1 ?nml_o1 .\n"
            + "    ?swsvc nml:encoding <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> .\n"
            + "    ?node nml:hasBidirectionalPort ?biport .\n"
            + "    ?biport a nml:BidirectionalPort .\n"
            + "    ?biport ?nml_p2 ?nml_o2 .\n"
            + "    OPTIONAL {\n"
            + "      ?swsvc mrs:providesSubnet ?subnet .\n"
            + "      ?subnet a mrs:SwitchingSubnet .\n"
            + "      ?subnet ?nml_p2_1 ?nml_o2_1 .\n"
            + "      FILTER ((REGEX(STR(?nml_p2_1), '^http://schemas.ogf.org/nml/2013/03/base#')))\n"
            + "    }\n"
            + "    FILTER ((?nodetype in (nml:Topology, nml:Node)) &&\n"
            + "        (REGEX(STR(?nml_p1), '^http://schemas.ogf.org/nml/2013/03/base#')) &&\n"
            + "        (REGEX(STR(?nml_p2), '^http://schemas.ogf.org/nml/2013/03/base#'))\n"
            + "    )\n"
            + "  } UNION {\n"
            + "    ?biport a nml:BidirectionalPort .\n"
            + "    ?biport ?haslabel_or_labelgroup ?label .\n"
            + "    ?label a ?label_or_labelgroup .\n"
            + "    ?label ?nml_p3 ?nml_o3 .\n"
            + "    FILTER ((?label_or_labelgroup in (nml:Label, nml:LabelGroup)) &&\n"
            + "        (?haslabel_or_labelgroup in (nml:hasLabel, nml:hasLabelGroup)) &&\n"
            + "        (REGEX(STR(?nml_p3), '^http://schemas.ogf.org/nml/2013/03/base#'))\n"
            + "    )\n"
            + "  } UNION {\n"
            + "    ?topo a  nml:Topology.\n"
            + "    ?topo ?has_node_or_topology ?node .\n"
            + "    ?node nml:hasService ?swsvc.\n"
            + "    ?swsvc a nml:SwitchingService .\n"
            + "    ?swsvc ?nml_p1 ?nml_o1 .\n"
            + "    ?swsvc nml:encoding <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> .\n"
            + "    OPTIONAL {\n"
            + "      ?swsvc mrs:providesSubnet ?subnet .\n"
            + "      ?subnet a mrs:SwitchingSubnet .\n"
            + "      ?subnet ?nml_p2_1 ?nml_o2_1 .\n"
            + "      FILTER ((REGEX(STR(?nml_p2_1), '^http://schemas.ogf.org/nml/2013/03/base#')))\n"
            + "    }\n"
            + "    FILTER ( (?has_node_or_topology in (nml:hasTopology, nml:hasNode))\n"
            + "    )\n"
            + "  }\n"
            + "}";

    private static String l2NetworkReasonerRules
            = "[rule1:  (?a http://schemas.ogf.org/nml/2013/03/base#hasService ?b) \n"
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
        //Query query = QueryFactory.create(l2NetworkConstructSparql);
        //QueryExecution qexec = QueryExecutionFactory.create(query, inputModel);
        //inputModel = qexec.execConstruct();

        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(l2NetworkReasonerRules));
        reasoner.setDerivationLogging(true);
        InfModel infModel = ModelFactory.createInfModel(reasoner, inputModel);

        OntModel outputModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        outputModel.add(infModel);
        return outputModel;
    }
    
    // define constraint sets (@TODO: combine the multuple rules into one in each set)
    private static String[] l2PathTakeOffConstraints = {
            "SELECT $s $p $o WHERE {$s a nml:Topology. $o a nml:Node FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:Node. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:Topology. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:Node. $o a nml:SwitchingService FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:Topology. $o a nml:SwitchingService FILTER($s = <$$s> && $o = <$$o>)}",
    };
    private static String[] l2PathTransitConstraints = {
            "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:SwitchingService FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:SwitchingService. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",
    };
    private static String[] l2PathLandingConstraints = {
            "SELECT $s $p $o WHERE {$s a nml:SwitchingService. $o a nml:Node FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:SwitchingService. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:Node FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:Node. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",
            "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",
    };
    
    public static boolean verifyL2Path(Model model, Path path) {
        String stage = "TAKEOFF";
        Iterator<Statement> itS = path.iterator();
        while (itS.hasNext()) {
            Statement stmt = itS.next();
            if (stage.equals("TRANSIT") && stmt.getObject().isResource() &&
                    ( ModelUtil.isResourceOfType(model,stmt.getObject().asResource(), Nml.Node)
                    || ModelUtil.isResourceOfType(model,stmt.getObject().asResource(), Nml.Topology) )
                    )
                stage = "LANDING";
            if (stage.equals("TAKEOFF") &&
                    ( ModelUtil.isResourceOfType(model,stmt.getSubject(), Nml.BidirectionalPort)
                    || ModelUtil.isResourceOfType(model,stmt.getSubject(), Nml.SwitchingService) )
                    )
                stage = "TRANSIT";

            if (stage.equals("TAKEOFF")) {
                if (!evaluateStatement_AnyTrue(model, stmt, l2PathTakeOffConstraints))
                    return false;
            } else if (stage.equals("TRANSIT")) {
                if (!evaluateStatement_AnyTrue(model, stmt, l2PathTransitConstraints))
                    return false;
            } else if (stage.equals("LANDING")) {
                if (!evaluateStatement_AnyTrue(model, stmt, l2PathLandingConstraints))
                    return false;                
            }
        }
        return true;
    }
    
    public static boolean evaluateStatement_AnyTrue(Model model, Statement stmt, String[] constraints) {
        for (String sparql: constraints) {
            if (ModelUtil.evaluateStatement(model, stmt, sparql))
                return true;
        }
        return false;
    }
    
    public static OntModel createL2PathVlanSubnets(Model model, Path path) {
        HashMap<Resource, HashMap<String, Object>> portParamMap = new HashMap<>();
        ListIterator<Statement> itS = path.listIterator();
        boolean last = false;
        Resource prevHop = null, currentHop = null, nextHop = null;
        Resource lastPort = null;
        // Forward iteration to calculate and collect information
        while (itS.hasNext()) {
            Statement stmt = itS.next();
            if (!itS.hasNext())
                last = true;
            currentHop = stmt.getSubject();
            nextHop = stmt.getObject().asResource();
            if ( prevHop != null && 
                    (ModelUtil.isResourceOfType(model, prevHop, Nml.Node)
                    || ModelUtil.isResourceOfType(model, prevHop, Nml.Topology)) ) {
                Resource prevSwSvc = getSwitchingServiceForHop(model, prevHop, currentHop);
                if (prevSwSvc != null)
                    prevHop = prevSwSvc;
                Resource nextSwSvc = getSwitchingServiceForHop(model, nextHop, currentHop);
                if (nextSwSvc != null)
                    nextHop = nextSwSvc;
            }
            if (ModelUtil.isResourceOfType(model, currentHop, Nml.BidirectionalPort)) {
                try {
                    handleL2PathHop(model, prevHop, currentHop, nextHop, portParamMap, lastPort);
                    lastPort = currentHop;
                } catch (TagSet.NoneVlanExeption ex) {
                    ;
                } catch (TagSet.EmptyTagSetExeption ex) {
                  return null; // throw Exception ?
                }
            }
            prevHop = currentHop;
            if (last) {
                prevHop = currentHop;
                currentHop = nextHop;
                nextHop = null;
                Resource prevSwSvc = getSwitchingServiceForHop(model, prevHop, currentHop);
                if (prevSwSvc != null)
                    prevHop = prevSwSvc;
                if (ModelUtil.isResourceOfType(model, currentHop, Nml.BidirectionalPort)) {
                    try {
                        handleL2PathHop(model, prevHop, currentHop, nextHop, portParamMap, lastPort);
                        lastPort = currentHop;
                    } catch (TagSet.NoneVlanExeption ex) {
                        ;
                    } catch (TagSet.EmptyTagSetExeption ex) {
                        return null; // throw Exception ?
                    }
                }
            }
        }
        // Reverse iteration to calculate suggestedVlan and create SwitchingSubnet
        OntModel l2PathModel = ModelUtil.newMrsOntModel("");
        nextHop = null;
        while (itS.hasPrevious()) {
            Statement stmt = itS.previous();
            prevHop = stmt.getSubject();
            currentHop = stmt.getObject().asResource();
            if (portParamMap.containsKey(currentHop)) {
                OntModel subnetModel = createVlanSubnetOnHop(model, prevHop, currentHop, nextHop, portParamMap, lastPort);
                if (subnetModel != null) {
                    l2PathModel.add(subnetModel.getBaseModel());
                }
                lastPort = currentHop; // last port in reverse order
            }
            nextHop = currentHop; // prev and next hops are still in forward order
            if (!itS.hasPrevious() && stmt.getObject().isResource()) {
                nextHop = currentHop;
                currentHop = prevHop;
                prevHop = null;
                if (portParamMap.containsKey(currentHop)) {
                    OntModel subnetModel  = createVlanSubnetOnHop(model, prevHop, currentHop, nextHop, portParamMap, lastPort);
                    if (subnetModel != null) {
                    l2PathModel.add(subnetModel.getBaseModel());
                    }
                }
            }
        }
        return l2PathModel;
    }
    
 
    //add hashMap (port, availableVlanRange + translation + ingressForSwService, egressForSwService) as param
    private static void handleL2PathHop(Model model, Resource prevHop, Resource currentHop, Resource nextHop, HashMap portParamMap, Resource lastPort) 
            throws TagSet.NoneVlanExeption, TagSet.EmptyTagSetExeption {
        if (ModelUtil.isResourceOfType(model, prevHop, Nml.BidirectionalPort)) {
            //TODO: handling adaptation?
        }
        HashMap<String, Object> paramMap = new HashMap<>();
        HashMap<String, Object> lastParamMap = null;
        if (lastPort != null && portParamMap.containsKey(lastPort))
            lastParamMap = (HashMap<String, Object>) portParamMap.get(lastPort);
        // Get VLAN range
        TagSet vlanRange = getVlanRangeForPort(model, currentHop);
        // do nothing for port without a Vlan labelGroup
        if (vlanRange == null)
            throw new TagSet.NoneVlanExeption();
        // interception with input availableVlanRange 
        Boolean vlanTranslation = true;
        Resource egressSwithingService = null; 
        if (!vlanRange.isEmpty() && lastParamMap != null && lastParamMap.containsKey("vlanRange")) {
            // vlan translation
            TagSet lastVlanRange = TagSet.VlanRangeANY;
            String sparql = String.format("SELECT $swapping WHERE {<%s> a nml:SwitchingService. <%s> nml:labelSwapping $swapping.}", prevHop, prevHop);
            ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
            if (rs.hasNext())
                egressSwithingService = prevHop;
            if (!rs.hasNext() || !rs.next().getLiteral(sparql).getBoolean()) {
                // non-translation
                vlanTranslation = false;
                lastVlanRange = (TagSet) lastParamMap.get("vlanRange");
            }
            vlanRange.intersect(lastVlanRange);
        }
        // exception if empty        
        if (vlanRange.isEmpty())
            throw new TagSet.EmptyTagSetExeption();
        paramMap.put("vlanRange", vlanRange);
        if (egressSwithingService != null) {
            paramMap.put("egressSwithingService", egressSwithingService);
            if (lastParamMap != null)
                lastParamMap.put("ingressSwithingService", egressSwithingService);
        }
        paramMap.put("vlanTranslation", vlanTranslation);
        portParamMap.put(currentHop, paramMap);
    }
    
    private static OntModel createVlanSubnetOnHop(Model model, Resource prevHop, Resource currentHop, Resource nextHop, HashMap portParamMap, Resource lastPort) {
        HashMap paramMap = (HashMap) portParamMap.get(currentHop);
        if (!paramMap.containsKey("vlanRange")) {
            return null;
        }
        TagSet vlanRange = (TagSet) paramMap.get("vlanRange");
        if (vlanRange.isEmpty()) {
            return null;
        }
        HashMap lastParamMap = null;
        if (lastPort != null && portParamMap.containsKey(lastPort))
            lastParamMap = (HashMap) portParamMap.get(lastPort);
        Integer suggestedVlan = null;
        if (lastParamMap != null && lastParamMap.containsKey("suggestedVlan")) {
            if (vlanRange.hasTag(suggestedVlan)) { // non-translation
                suggestedVlan = (Integer) lastParamMap.get("suggestedVlan");
            } else if (!paramMap.containsKey("vlanTranslation") 
                        || !(Boolean)paramMap.get("vlanTranslation") ) { // try translation but not able to
                return null;
            }
        }
        // init without lastPort or do tanslation
        if (suggestedVlan == null)
            suggestedVlan = vlanRange.getRandom();        
        paramMap.put("suggestedVlan", suggestedVlan);
        
        // create port and subnet statements
        OntModel vlanSubnetModel = ModelUtil.newMrsOntModel("");

        // create new VLAN port with vlan label
        String vlanPortUrn = currentHop.toString() + ":vlanport+" + suggestedVlan;
        Resource resVlanPort = RdfOwl.createResource(vlanSubnetModel, vlanPortUrn, Nml.BidirectionalPort);
        String vlanLabelUrn = vlanPortUrn+":label";
        Resource resVlanPortLabel = RdfOwl.createResource(vlanSubnetModel, vlanLabelUrn, Nml.Label);
        vlanSubnetModel.createStatement(resVlanPort, Nml.hasLabel, resVlanPortLabel);
        vlanSubnetModel.createStatement(resVlanPortLabel, Nml.labeltype, RdfOwl.labelTypeVLAN);
        vlanSubnetModel.createStatement(resVlanPortLabel, Nml.value, suggestedVlan.toString());

        // create ingressSubnet for ingressSwitchingService and add port the the new subnet
        if (paramMap.containsKey("ingressSwitchingService")) {
            Resource ingressSwitchingService = (Resource) paramMap.get("ingressSwitchingService");
            String vlanSubnetUrn = ingressSwitchingService.toString() + ":vlan+" + suggestedVlan;
            Resource ingressSwitchingSubnet = RdfOwl.createResource(vlanSubnetModel, vlanSubnetUrn, Mrs.SwitchingSubnet);
            vlanSubnetModel.createStatement(ingressSwitchingService, Mrs.providesSubnet, ingressSwitchingSubnet);
            vlanSubnetModel.createStatement(ingressSwitchingSubnet, Nml.belongsTo, ingressSwitchingService);
            vlanSubnetModel.createStatement(ingressSwitchingSubnet, Nml.hasBidirectionalPort, resVlanPort);
            vlanSubnetModel.createStatement(resVlanPort, Nml.belongsTo, ingressSwitchingSubnet);
        }
        
        // get egressSubnet for egressSwitchingService and add port the this existing subnet
        if (paramMap.containsKey("egressSwitchingSubnet")) {
            Resource egressSwitchingSubnet = (Resource) paramMap.get("egressSwitchingService");
            vlanSubnetModel.createStatement(egressSwitchingSubnet, Nml.hasBidirectionalPort, resVlanPort);
            vlanSubnetModel.createStatement(resVlanPort, Nml.belongsTo, egressSwitchingSubnet);
        }
        
        return vlanSubnetModel;
    }
    
    private static Resource getSwitchingServiceForHop(Model model, Resource nodeOrTopo, Resource port) {
            String sparql = String.format("SELECT $sw WHERE {<%s> nml:hasService $sw. $sw a nml:SwitchingService. $sw nml:hasBidirectionalPort <%s>.}", nodeOrTopo, port);
            ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
            if (!rs.hasNext())
                return null;
            return rs.next().getResource("sw");
    }
    
    //TODO: get VLAN range and remove allocated ports
    private static TagSet getVlanRangeForPort(Model model, Resource port) {
            String sparql = String.format("SELECT $range WHERE {"
                    + "<%s> nml:hasLabelGroup $lg. $lg nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. $lg nml:values $range."
                    + "}", port);
            ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
            if (!rs.hasNext())
                return null;
            TagSet vlanRange = new TagSet(rs.next().getLiteral("$range").toString());
            sparql = String.format("SELECT $range WHERE {"
                    + "<%s> nml:hasBidirectionalPort $vlan_port. $vlan_port nml:hasLabel $l. $l nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. $l nml:value $vlan."
                    + "}", port);            
            rs = ModelUtil.sparqlQuery(model, sparql);
            while (rs.hasNext()) {
                String vlanStr = rs.next().getLiteral("$vlan").toString();
                Integer vlan = Integer.getInteger(vlanStr);
                vlanRange.removeTag(vlan);
            }
            return vlanRange;
    }
}
