/*
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

package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntTools;
import static com.hp.hpl.jena.ontology.OntTools.findShortestPath;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.common.TagSet;
import org.json.simple.JSONObject;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.common.DateTimeUtil;
import net.maxgigapop.mrs.common.StackLogger;
import static net.maxgigapop.mrs.driver.opendaylight.OpenflowModelBuilder.URI_action;
import static net.maxgigapop.mrs.driver.opendaylight.OpenflowModelBuilder.URI_flow;
import static net.maxgigapop.mrs.driver.opendaylight.OpenflowModelBuilder.URI_match;

/**
 *
 * @author xyang
 */
public class MCETools {

    private static final StackLogger logger = new StackLogger(MCETools.class.getName(), "MCETools");

    public static class BandwidthProfile {
        public String type = "bestEffort"; // default  "bestEffort"
        public String unit = "bps"; // default "bps"
        public Long granularity = 1L; // default 1L
        public String priority = "0"; // default "0"
        public Long maximumCapacity = 0L; // default = 0
        public Long availableCapacity = null; // optional
        public Long reservableCapacity = null; // optional
        public Long individualCapacity = null;  // optional
        public Long minimumCapacity = null; // optional
        public Long usedCapacity = null;  // optional

        public BandwidthProfile(Long capacity) {
            this.maximumCapacity = capacity;
            this.reservableCapacity = capacity;
            this.individualCapacity = capacity;
        }
        
        public BandwidthProfile(Long maximumCapacity, Long reservableCapacity) {
            this.maximumCapacity = maximumCapacity;
            this.reservableCapacity = reservableCapacity;
            this.individualCapacity = reservableCapacity;
        }
    }
    
    public static class Path extends com.hp.hpl.jena.ontology.OntTools.Path {
        HashSet<Statement> maskedLinks = null;
        Resource deviationNode = null;
        OntModel ontModel = null;
        double failureProb = 0.0;
        BandwidthProfile bandwithProfile = null;
        BandwidthCalendar.BandwidthSchedule bandwithScedule = null;
        String connectionId = null;
        
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

        public OntModel getOntModel() {
            return ontModel;
        }

        public void setOntModel(OntModel ontModel) {
            this.ontModel = ontModel;
        }

        public BandwidthProfile getBandwithProfile() {
            return bandwithProfile;
        }

        public void setBandwithProfile(BandwidthProfile bandwithProfile) {
            this.bandwithProfile = bandwithProfile;
        }

        public BandwidthCalendar.BandwidthSchedule getBandwithScedule() {
            return bandwithScedule;
        }

        public void setBandwithScedule(BandwidthCalendar.BandwidthSchedule bandwithScedule) {
            this.bandwithScedule = bandwithScedule;
        }

        

        public String getConnectionId() {
            return connectionId;
        }

        public void setConnectionId(String connectionId) {
            this.connectionId = connectionId;
        }
    }

    public static Path getLeastCostPath(List<Path> candidates) {
        long cost = 1000000;
        Path solution = null;
        for (Path path : candidates) {
            if (path.size() < cost) {
                cost = path.size();
                solution = path;
            }
        }
        return solution;
    }

    public static Path findShortestPath(Model model, Resource nodeA, Resource nodeZ, Filter<Statement> filters) {
        if (filters == null) {
            filters = Filter.any();
        }
        OntTools.Path path = OntTools.findShortestPath(model, nodeA, nodeZ, filters);
        if (path == null) {
            return null;
        }
        return new MCETools.Path(path);
    }

    final public static int KSP_K_DEFAULT = 10;

    public static List<Path> computeKShortestPaths(Model model, Resource nodeA, Resource nodeZ, int K, Filter<Statement> filters) {
        final HashSet<Statement> maskedLinks = new HashSet<>();
        List<Path> KSP = new ArrayList<>();
        List<Path> candidatePaths = new ArrayList<>();
        // find the first shortest path
        Path nextPath = findShortestPath(model, nodeA, nodeZ, filters);
        if (nextPath == null) {
            return null;
        }
        KSP.add(nextPath);
        candidatePaths.add(nextPath);
        int kspCounter = 1;
        while (!candidatePaths.isEmpty() && KSP.size() <= K) {
            Path headPath = getLeastCostPath(candidatePaths);
            candidatePaths.remove(headPath);
            if (kspCounter > 1) {
                KSP.add(headPath);
            }
            if (kspCounter == K) {
                break;
            }
            for (Statement stmtLink : headPath) {
                if (headPath.getDeviationNode() != null) {
                    if (stmtLink.getSubject().equals(headPath.getDeviationNode())) {
                        break;
                    }
                }
                // mask (filter out) all statments to and from the localEnd of link
                StmtIterator itStmt = model.listStatements(stmtLink.getSubject(), null, (Resource) null);
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
            for (Statement stmtLink : headPath) {
                // filter out masked links in headPath 
                if (headPath.getMaskedLinks() != null) {
                    maskedLinks.addAll(headPath.getMaskedLinks());
                    for (Statement stmtMaskedLink : headPath.getMaskedLinks()) {
                        model = model.remove(stmtMaskedLink);
                    }
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
                            if (headPath.get(i).equals(stmtLink)) {
                                break;
                            }
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
                model.add(stmtLink);
                maskedLinks.remove(stmtLink);
            }
            kspCounter++;
        }
        //model.write(System.out, "TURTLE");
        for (Statement stmtMaskedLink : maskedLinks) {
            model.add(stmtMaskedLink);
        }
        //model.write(System.out, "TURTLE");
        return KSP;
    }

    public static List<MCETools.Path> computeFeasibleL2KSP(OntModel transformedModel, Resource nodeA, Resource nodeZ, JSONObject jsonConnReq) throws Exception {
            Property[] filterProperties = {Nml.connectsTo};
            Filter<Statement> connFilters = new OntTools.PredicatesFilter(filterProperties);
            List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);
            if (KSP == null || KSP.isEmpty()) {
                return KSP;
            }
            // Verify TE constraints (switching label and ?adaptation?), 
            Iterator<MCETools.Path> itP = KSP.iterator();
            while (itP.hasNext()) {
                MCETools.Path candidatePath = itP.next();
                // verify path
                boolean verified = false;
                try {
                    verified = MCETools.verifyL2Path(transformedModel, candidatePath);
                } catch (Exception ex) {
                    throw new Exception("MCETools.computeFeasibleL2KSP - cannot verifyL2Path", ex);
                }
                if (verified && jsonConnReq.containsKey("bandwidth")) {
                    JSONObject jsonBw = (JSONObject) jsonConnReq.get("bandwidth");
                    Long maximum = (jsonBw.containsKey("maximum") && jsonBw.get("maximum") != null) ? Long.parseLong(jsonBw.get("maximum").toString()) : null;
                    Long reservable = (jsonBw.containsKey("reservable") && jsonBw.get("reservable") != null) ? Long.parseLong(jsonBw.get("reservable").toString()) : null;
                    candidatePath.bandwithProfile = new MCETools.BandwidthProfile(maximum, reservable);
                    candidatePath.bandwithProfile.minimumCapacity = (jsonBw.containsKey("minimum") && jsonBw.get("minimum") != null) ? Long.parseLong(jsonBw.get("minimum").toString()) : null; //default = null
                    candidatePath.bandwithProfile.availableCapacity = (jsonBw.containsKey("available") && jsonBw.get("available") != null) ? Long.parseLong(jsonBw.get("available").toString()) : null; //default = null
                    candidatePath.bandwithProfile.individualCapacity = (jsonBw.containsKey("individual") && jsonBw.get("individual") != null) ? Long.parseLong(jsonBw.get("individual").toString()) : null; //default = null
                    candidatePath.bandwithProfile.granularity = (jsonBw.containsKey("granularity")  && jsonBw.get("granularity") != null) ? Long.parseLong(jsonBw.get("granularity").toString()) : 1L; //default = 1
                    candidatePath.bandwithProfile.type = (jsonBw.containsKey("qos_class") && jsonBw.get("qos_class")!= null) ? jsonBw.get("qos_class").toString() : "bestEffort"; //default = "bestEffort"
                    candidatePath.bandwithProfile.unit = (jsonBw.containsKey("unit") && jsonBw.get("unit") != null) ? jsonBw.get("unit").toString() : "bps"; //default = "bps"
                    candidatePath.bandwithProfile.priority = (jsonBw.containsKey("priority") && jsonBw.get("priority") != null) ? jsonBw.get("priority").toString() : "0"; //default = "0"
                    verified = MCETools.verifyPathBandwidthProfile(transformedModel, candidatePath);
                }
                if (verified && jsonConnReq.containsKey("schedule")) {
                    JSONObject jsonSchedule = (JSONObject) jsonConnReq.get("schedule");
                    candidatePath.bandwithScedule = new BandwidthCalendar.BandwidthSchedule();
                    String startTime = jsonSchedule.containsKey("start") ? jsonSchedule.get("start").toString() : "now";
                    String endTime = jsonSchedule.containsKey("end") ? jsonSchedule.get("end").toString() : null;
                    String duration = jsonSchedule.containsKey("duration") ? jsonSchedule.get("duration").toString() : null;
                    if (endTime == null && duration == null) {
                        throw new Exception("MCETools.computeFeasibleL2KSP - malformed schedule: " + jsonSchedule.toJSONString());
                    }
                    candidatePath.bandwithScedule.setStartTime(DateTimeUtil.getBandwidthScheduleSeconds(startTime));
                    if (endTime != null) {
                        if (endTime.startsWith("+")) {
                            candidatePath.bandwithScedule.setEndTime(candidatePath.bandwithScedule.getStartTime() + DateTimeUtil.getBandwidthScheduleSeconds(endTime));
                        } else {
                            candidatePath.bandwithScedule.setEndTime(DateTimeUtil.getBandwidthScheduleSeconds(endTime));
                        }
                    } else {
                        candidatePath.bandwithScedule.setEndTime(candidatePath.bandwithScedule.getStartTime() + DateTimeUtil.getBandwidthScheduleSeconds(duration));
                    }
                    if (candidatePath.bandwithProfile == null || candidatePath.bandwithProfile.reservableCapacity == null) {
                        throw new Exception("MCETools.computeFeasibleL2KSP - input schedule without bandwidth.");
                    }
                    candidatePath.bandwithScedule.setBandwidth(normalizeBandwidthPorfile(candidatePath.bandwithProfile).reservableCapacity);
                    JSONObject jsonScheduleOptions =  jsonSchedule.containsKey("options") ? (JSONObject)jsonSchedule.get("options") : new JSONObject();
                    if (endTime != null && duration != null ) { // sliding window
                        jsonScheduleOptions.put("sliding-duration", DateTimeUtil.getBandwidthScheduleSeconds(duration));
                    }
                    try {
                        BandwidthCalendar.BandwidthSchedule schedule = BandwidthCalendar.makePathBandwidthSchedule(transformedModel, candidatePath, jsonScheduleOptions);
                        if (schedule == null) {
                            verified = false;
                        } else {
                            candidatePath.setBandwithScedule(schedule);
                            if (candidatePath.getBandwithProfile() != null) {
                                //if (candidatePath.getBandwithProfile().maximumCapacity != null) {
                                //    candidatePath.getBandwithProfile().maximumCapacity = schedule.getBandwidth();
                                //}
                                if (candidatePath.getBandwithProfile().availableCapacity != null) {
                                    candidatePath.getBandwithProfile().availableCapacity = schedule.getBandwidth();
                                }
                                if (candidatePath.getBandwithProfile().reservableCapacity != null) {
                                    candidatePath.getBandwithProfile().reservableCapacity = schedule.getBandwidth();
                                }
                                candidatePath.getBandwithProfile().unit = "bps";
                                //@TODO: warning for granularity mismatch <= schedule.getBandwidth() % candidatePath.getBandwithProfile().granularity != 0
                            }
                        }
                    } catch (BandwidthCalendar.BandwidthCalendarException ex) {
                        logger.trace("computeFeasibleL2KSP", candidatePath.getConnectionId() + " -- " + ex.getMessage());
                        verified = false;
                    }
                }
                if (!verified) {
                    itP.remove();
                } else {
                    // generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                    candidatePath.setConnectionId((String)jsonConnReq.get("id"));
                    OntModel l2PathModel = MCETools.createL2PathVlanSubnets(transformedModel, candidatePath, (JSONObject)jsonConnReq.get("terminals"));
                    if (l2PathModel == null) {
                        itP.remove();
                    } else {
                        candidatePath.setOntModel(l2PathModel);
                    }
                }
            }
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

    private static String L2OpenflowPathRules
            = "[rule1:  (?a http://schemas.ogf.org/nml/2013/03/base#hasBidirectionalPort ?b)\n"
            + "  (?a rdf:type http://schemas.ogf.org/nml/2013/03/base#Node ) \n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#connectsTo ?b)\n"
            + "         (?b http://schemas.ogf.org/nml/2013/03/base#connectsTo ?a)\n"
            + "]\n"
            + "[rule2:  (?a http://schemas.ogf.org/nml/2013/03/base#isAlias ?b) \n"
            + "      -> (?a http://schemas.ogf.org/nml/2013/03/base#connectsTo ?b)\n"
            + "         (?b http://schemas.ogf.org/nml/2013/03/base#connectsTo ?a)\n"
            + "]";

    public static OntModel transformL2OpenflowPathModel(Model inputModel) {

        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(L2OpenflowPathRules));
        reasoner.setDerivationLogging(true);
        InfModel infModel = ModelFactory.createInfModel(reasoner, inputModel);

        OntModel outputModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        outputModel.add(infModel);
        return outputModel;
    }

    private static String[] openflowPathIntoSwitchConstraints = {
        "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:Node FILTER($s = <$$s> && $o = <$$o>)}",};

    private static String[] openflowPathOutSwitchConstraints = {
        "SELECT $s $p $o WHERE {$s a nml:Node. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",};

    private static String[] openflowPathBetweenPortConstraints = {
        "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",};

    public static boolean verifyOpenFlowPath(Model model, Path path) throws Exception {

        //frist stage must be IntroSwitch to start from a port
        String stage = "TakeOff";
        Iterator<Statement> itS = path.iterator();

        List<Resource> subjectList = new ArrayList<>();
        while (itS.hasNext()) {
            Statement stmt = itS.next();

            for (Resource subjectIter : subjectList) {
                if (stmt.getSubject().equals(subjectIter)) {
                    return false;
                }
            }
            subjectList.add(stmt.getSubject());

            if (stage.equals("TakeOff")) {
                if (!evaluateStatement_AnyTrue(model, stmt, openflowPathIntoSwitchConstraints)) {
                    return false;
                } else {
                    stage = "IntoSwitch";
                    continue;
                }
            }

            if (stage.equals("IntoSwitch")) {
                if (ModelUtil.isResourceOfType(model, stmt.getSubject(), Nml.Node)
                        && ModelUtil.isResourceOfType(model, stmt.getObject().asResource(), Nml.BidirectionalPort)) {
                    stage = "OutSwitch";
                    if (!evaluateStatement_AnyTrue(model, stmt, openflowPathOutSwitchConstraints)) {
                        return false;
                    } else {
                        continue;
                    }
                } else {
                    return false;
                }
            }

            if (stage.equals("OutSwitch")) {
                if (ModelUtil.isResourceOfType(model, stmt.getSubject(), Nml.BidirectionalPort)
                        && ModelUtil.isResourceOfType(model, stmt.getObject().asResource(), Nml.BidirectionalPort)) {
                    stage = "BetweenPort";
                    if (!evaluateStatement_AnyTrue(model, stmt, openflowPathBetweenPortConstraints)) {
                        return false;
                    } else {
                        continue;
                    }
                } else {
                    return false;
                }
            }

            if (stage.equals("BetweenPort")) {
                if (ModelUtil.isResourceOfType(model, stmt.getSubject(), Nml.BidirectionalPort)
                        && ModelUtil.isResourceOfType(model, stmt.getObject().asResource(), Nml.Node)) {
                    stage = "IntoSwitch";
                    if (!evaluateStatement_AnyTrue(model, stmt, openflowPathIntoSwitchConstraints)) {
                        return false;
                    } else {
                        continue;
                    }
                } else {
                    return false;
                }
            }
        }

        //last stage must point to a port
        if (stage.equals("IntoSwitch")) {
            return false;
        }
        return true;
    }

    public static void printMCEToolsPath(MCETools.Path path) {

        for (Statement stmtLink : path) {
            System.out.println(stmtLink.toString());
        }
    }

    public static void printKSP(List<MCETools.Path> KSP) {

        int count = 0;

        for (MCETools.Path candidatePath : KSP) {
            System.out.format("path[%d]: \n", count);
            for (Statement stmtLink : candidatePath) {
                System.out.println(stmtLink.toString());
            }
            count = count + 1;
        }
    }

    public static OntModel transformL2NetworkModel(Model inputModel) {
        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(l2NetworkReasonerRules));
        reasoner.setDerivationLogging(true);
        InfModel infModel = ModelFactory.createInfModel(reasoner, inputModel);
        // remove transit [node/topology connectsTo bidirectoinalPort) if they have switching or openflow services
        String sparql = "SELECT ?node ?port WHERE {"
                + "?node a ?type. "
                + "?port a nml:BidirectionalPort."
                + "?node nml:hasService ?svc. "
                + "?svc a ?svc_type. "
                + "?node nml:connectsTo ?port. "
                + "?svc nml:connectsTo ?port."
                + "FILTER (?type in (nml:Node, nml:Topology) && ?svc_type in (nml:SwitchingService, mrs:OpenflowService))"
                + "}";
        ResultSet rs = ModelUtil.sparqlQuery(infModel, sparql);
        List<Statement> stmtList = new ArrayList();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            Resource resNode = qs.getResource("node");
            Resource resPort = qs.getResource("port");
            stmtList.add(infModel.createLiteralStatement(resNode, Nml.connectsTo, resPort));
            stmtList.add(infModel.createLiteralStatement(resPort, Nml.connectsTo, resNode));
            sparql = "SELECT ?subport WHERE {"
                + String.format("<%s> nml:hasBidirectionalPort ?subport.", resPort.getURI())
                + "}";
            ResultSet rs2 = ModelUtil.sparqlQuery(infModel, sparql);
            while (rs2.hasNext()) {
                QuerySolution qs2 = rs2.next();
                Resource resSubPort = qs2.getResource("subport");
                stmtList.add(infModel.createLiteralStatement(resNode, Nml.connectsTo, resSubPort));
            }
        }
        // remove port to sub-port link if sub-port hasLabel 
        sparql = "SELECT ?port ?subport WHERE {"
                + "?port a nml:BidirectionalPort."
                + "?port nml:hasBidirectionalPort ?subport."
                + "?subport nml:hasLabel ?label. "
                + "FILTER (NOT EXISTS{?subport nml:hasLabelGroup ?labelgroup})"
                + "}";
        rs = ModelUtil.sparqlQuery(infModel, sparql);
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            Resource resPort = qs.getResource("port");
            Resource resSubPort = qs.getResource("subport");
            stmtList.add(infModel.createLiteralStatement(resSubPort, Nml.connectsTo, resPort));
            stmtList.add(infModel.createLiteralStatement(resPort, Nml.connectsTo, resSubPort));
        }
        OntModel outputModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        outputModel.add(infModel);
        outputModel.remove(stmtList);
        return outputModel;
    }

    // define constraint sets (@TODO: combine the multuple rules into one in each set)
    private static String[] l2PathTakeOffConstraints = {
        "SELECT $s $p $o WHERE {$s a nml:Topology. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Topology. $o a nml:Node FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Node. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Topology. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Node. $o a nml:SwitchingService FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Topology. $o a nml:SwitchingService FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Node. $o a mrs:OpenflowService FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Topology. $o a mrs:OpenflowService FILTER($s = <$$s> && $o = <$$o>)}",};
    private static String[] l2PathTransitConstraints = {
        "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:SwitchingService FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:SwitchingService. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a mrs:OpenflowService FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a mrs:OpenflowService. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",};
    private static String[] l2PathLandingConstraints = {
        "SELECT $s $p $o WHERE {$s a nml:SwitchingService. $o a nml:Node FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:SwitchingService. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a mrs:OpenflowService. $o a nml:Node FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a mrs:OpenflowService. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:Node FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Node. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",
        "SELECT $s $p $o WHERE {$s a nml:Topology. $o a nml:Topology FILTER($s = <$$s> && $o = <$$o>)}",};

    public static boolean verifyL2Path(Model model, Path path) throws Exception {
        String stage = "TAKEOFF";
        Iterator<Statement> itS = path.iterator();
        while (itS.hasNext()) {
            Statement stmt = itS.next();
            if (stage.equals("TRANSIT") && stmt.getObject().isResource()
                    && (ModelUtil.isResourceOfType(model, stmt.getObject().asResource(), Nml.Node)
                    || ModelUtil.isResourceOfType(model, stmt.getObject().asResource(), Nml.Topology))) {
                stage = "LANDING";
            }
            if (stage.equals("TAKEOFF")
                    && (ModelUtil.isResourceOfType(model, stmt.getSubject(), Nml.BidirectionalPort)
                    || ModelUtil.isResourceOfType(model, stmt.getSubject(), Nml.SwitchingService))) {
                stage = "TRANSIT";
            }

            if (stage.equals("TAKEOFF")) {
                if (!evaluateStatement_AnyTrue(model, stmt, l2PathTakeOffConstraints)) {
                    return false;
                }
            } else if (stage.equals("TRANSIT")) {
                if (!evaluateStatement_AnyTrue(model, stmt, l2PathTransitConstraints)) {
                    return false;
                }
            } else if (stage.equals("LANDING")) {
                if (!evaluateStatement_AnyTrue(model, stmt, l2PathLandingConstraints)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean verifyPathBandwidthProfile(Model model, Path path) {
        if (path.getBandwithProfile() == null) {
            return true;
        }
        BandwidthProfile pathAvailBwProfile = null;
        Iterator<Statement> itS = path.iterator();
        while (itS.hasNext()) {
            Statement link = itS.next();
            BandwidthProfile hopBwProfile = getHopBandwidthPorfile(model, link.getSubject().asResource());
            if (pathAvailBwProfile == null && hopBwProfile != null) {
                hopBwProfile = normalizeBandwidthPorfile(hopBwProfile);
                pathAvailBwProfile = hopBwProfile;
            }
            if (hopBwProfile != null) {
                hopBwProfile = normalizeBandwidthPorfile(hopBwProfile);
                pathAvailBwProfile = handleBandwidthProfile(pathAvailBwProfile, hopBwProfile);
            }
            if (!itS.hasNext()) {
                hopBwProfile = getHopBandwidthPorfile(model, link.getObject().asResource());
                if ( hopBwProfile != null) {
                    hopBwProfile = normalizeBandwidthPorfile(hopBwProfile);
                    pathAvailBwProfile = handleBandwidthProfile(pathAvailBwProfile, hopBwProfile);
                }
            }
        }
        // compare avaialble bandwidth profile to requested badnwidth profile
        BandwidthProfile pathRequestBwProfile = normalizeBandwidthPorfile(path.getBandwithProfile());
        if (pathAvailBwProfile != null && !canProvideBandwith(pathAvailBwProfile, pathRequestBwProfile)) {
            return false;
        }
        if (pathAvailBwProfile != null && path.getBandwithProfile().type.equalsIgnoreCase("anyAvailable")) {
            path.setBandwithProfile(pathAvailBwProfile); // replace requested with available profile
        }
        return true;
    }
    
    public static BandwidthProfile getHopBandwidthPorfile(Model model, Resource hop) {
        String sparql = "SELECT $maximum $available $reservable $granularity $qos_class $unit $minimum $individual $priority WHERE {"
                + String.format("<%s> a nml:BidirectionalPort. ", hop.getURI())
                + String.format("<%s> nml:hasService $bw_svc. ", hop.getURI())
                + "$bw_svc mrs:maximumCapacity $maximum. "
                + "$bw_svc mrs:reservableCapacity $reservable. "
                + "OPTIONAL {$bw_svc mrs:availableCapacity $available } "
                + "OPTIONAL {$bw_svc mrs:type $qos_class } "
                + "OPTIONAL {$bw_svc mrs:unit $unit } "
                + "OPTIONAL {$bw_svc mrs:granularity $granularity } "
                + "OPTIONAL {$bw_svc mrs:minimumCapacity $minimum } "
                + "OPTIONAL {$bw_svc mrs:individualCapacity $individual } "
                + "OPTIONAL {$bw_svc mrs:priority $priority } "
                + "}";
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        BandwidthProfile bwProfile = null;
        if (rs.hasNext()) {
            QuerySolution solution = rs.next();
            Long maximumBw = solution.get("maximum").asLiteral().getLong();
            Long reservableBw = solution.get("reservable").asLiteral().getLong();
            bwProfile = new BandwidthProfile(maximumBw, reservableBw);
            if (solution.contains("available")) {
                bwProfile.availableCapacity = solution.get("available").asLiteral().getLong();
            }
            if (solution.contains("granularity")) {
                bwProfile.granularity = solution.get("granularity").asLiteral().getLong();
            }
            if (solution.contains("minimum")) {
                bwProfile.minimumCapacity = solution.get("minimum").asLiteral().getLong();
            }
            if (solution.contains("individual")) {
                bwProfile.individualCapacity = solution.get("individual").asLiteral().getLong();
            }
            if (solution.contains("unit")) {
                bwProfile.unit = solution.get("unit").asLiteral().getString();
            }
            if (solution.contains("qos_class")) {
                bwProfile.type = solution.get("qos_class").asLiteral().getString();
            }
            if (solution.contains("priority")) {
                bwProfile.priority = solution.get("priority").asLiteral().getString();
            }
        }
        return bwProfile;
    }
    
    public static long normalizeBandwidth(long bw, String unit) {
        long factor = 1;
        if (unit.equalsIgnoreCase("kbps")) {
            factor = 1000;
        } else if (unit.equalsIgnoreCase("mbps")) {
            factor = 1000000;
        } else if (unit.equalsIgnoreCase("gbps")) {
            factor = 1000000000;
        }
        return bw*factor;
    }
    
    public static BandwidthProfile normalizeBandwidthPorfile(BandwidthProfile bwProfile) {
        if (bwProfile.unit == null || bwProfile.unit.equalsIgnoreCase("bps")) {
            return bwProfile;
        } 
        if (bwProfile.maximumCapacity != null) {
            bwProfile.maximumCapacity = normalizeBandwidth(bwProfile.maximumCapacity, bwProfile.unit);
        }
        if (bwProfile.availableCapacity != null) {
            bwProfile.availableCapacity = normalizeBandwidth(bwProfile.availableCapacity, bwProfile.unit);
        }
        if (bwProfile.reservableCapacity != null) {
            bwProfile.reservableCapacity = normalizeBandwidth(bwProfile.reservableCapacity, bwProfile.unit);
        }
        if (bwProfile.individualCapacity != null) {
            bwProfile.individualCapacity = normalizeBandwidth(bwProfile.individualCapacity, bwProfile.unit);
        }
        if (bwProfile.usedCapacity != null) {
            bwProfile.usedCapacity = normalizeBandwidth(bwProfile.usedCapacity, bwProfile.unit);
        } 
        if (bwProfile.minimumCapacity != null) {
            bwProfile.minimumCapacity = normalizeBandwidth(bwProfile.minimumCapacity, bwProfile.unit);
        }
        if (bwProfile.granularity != null) {
            bwProfile.granularity = normalizeBandwidth(bwProfile.granularity, bwProfile.unit);
        }
        bwProfile.unit = "bps";
        return bwProfile;
    }

    // compare and intersect bandwidthProfiles along path
    private static BandwidthProfile handleBandwidthProfile(BandwidthProfile pathBwProfile, BandwidthProfile hopBwProfile) {
        if (pathBwProfile.maximumCapacity == null || pathBwProfile.maximumCapacity > hopBwProfile.maximumCapacity) {
            pathBwProfile.maximumCapacity = hopBwProfile.maximumCapacity;
        }
        if (pathBwProfile.availableCapacity == null || (hopBwProfile.availableCapacity != null && pathBwProfile.availableCapacity > hopBwProfile.availableCapacity)) {
            pathBwProfile.availableCapacity = hopBwProfile.availableCapacity;
        }
        if (pathBwProfile.reservableCapacity == null || (hopBwProfile.reservableCapacity != null && pathBwProfile.reservableCapacity > hopBwProfile.reservableCapacity)) {
            pathBwProfile.reservableCapacity = hopBwProfile.reservableCapacity;
        }
        if (pathBwProfile.individualCapacity == null || (hopBwProfile.individualCapacity != null && pathBwProfile.individualCapacity > hopBwProfile.individualCapacity)) {
            pathBwProfile.individualCapacity = hopBwProfile.individualCapacity;
        }
        if (pathBwProfile.minimumCapacity == null || (hopBwProfile.minimumCapacity != null && pathBwProfile.minimumCapacity < hopBwProfile.minimumCapacity)) {
            pathBwProfile.minimumCapacity = hopBwProfile.minimumCapacity;
        }
        if (pathBwProfile.granularity == null || (hopBwProfile.granularity != null && pathBwProfile.granularity < hopBwProfile.granularity)) {
            pathBwProfile.granularity = hopBwProfile.granularity;
        }
        return pathBwProfile;
    }
    
    // compare against static path bandwidthProfile
    // 1. verify not exceeding maximumCapacity for bestEffort and softCapped
    // 2. verify not exceeding reservableCapacity for softCapped and guaranteedCapped (and availableCapacity if present)
    // 3. anyAvailable is treated the same as guaranteedCapped, except that it will override the bandwidth value later
    public static boolean canProvideBandwith(BandwidthProfile bwpfAvailable, BandwidthProfile bwpfRequest) {
        if (bwpfRequest.type.equalsIgnoreCase("bestEffort") || bwpfRequest.type.equalsIgnoreCase("softCapped")) { 
            if (bwpfAvailable.maximumCapacity != null && bwpfRequest.maximumCapacity != null 
                    && bwpfAvailable.maximumCapacity  < bwpfRequest.maximumCapacity) {
                return false;
            } else {
                return true;
            }
        }
        
        if (bwpfRequest.type.equalsIgnoreCase("guaranteedCapped") || bwpfRequest.type.equalsIgnoreCase("softCapped")
                || bwpfRequest.type.equalsIgnoreCase("anyAvailable")) {
            if (bwpfAvailable.individualCapacity != null && bwpfRequest.individualCapacity != null
                    && bwpfRequest.individualCapacity > bwpfAvailable.individualCapacity) {
                return false;
            } 
            if (bwpfAvailable.minimumCapacity != null 
                    && bwpfRequest.reservableCapacity < bwpfAvailable.minimumCapacity ) {
                return false;
            } 
            if (bwpfAvailable.granularity != null && bwpfAvailable.granularity > 0 
                    && bwpfRequest.reservableCapacity != 1
                    && bwpfRequest.reservableCapacity % bwpfAvailable.granularity != 0) {
                return false;
            } 
            
            if (bwpfRequest.type.equalsIgnoreCase("softCapped") && bwpfRequest.reservableCapacity == null) {
                return true;
            }
            if (bwpfAvailable.reservableCapacity == null 
                    || bwpfRequest.reservableCapacity >  bwpfAvailable.reservableCapacity) {
                return false;
            }             
            if (bwpfAvailable.availableCapacity != null 
                    && bwpfRequest.reservableCapacity > bwpfAvailable.availableCapacity) {
                return false;
            }
            return true;
        }        
        // unknown qos type
        return false;
    }

    public static boolean evaluateStatement_AnyTrue(Model model, Statement stmt, String[] constraints) throws Exception {
        for (String sparql : constraints) {
            if (ModelUtil.evaluateStatement(model, stmt, sparql)) {
                return true;
            }
        }
        return false;
    }

    public static OntModel createL2PathVlanSubnets(Model model, Path path, JSONObject portTeMap) {
        String method = "createL2PathVlanSubnets";
        HashMap<Resource, HashMap<String, Object>> portParamMap = new HashMap<>();
        ListIterator<Statement> itS = path.listIterator();
        boolean last = false;
        Resource prevHop = null, currentHop = null, nextHop = null;
        Resource lastPort = null;
        // Forward iteration to calculate and collect information
        while (itS.hasNext()) {
            Statement stmt = itS.next();
            if (!itS.hasNext()) {
                last = true;
            }
            currentHop = stmt.getSubject();
            nextHop = stmt.getObject().asResource();
            if (prevHop != null && (ModelUtil.isResourceOfType(model, prevHop, Nml.Node)
                    || ModelUtil.isResourceOfType(model, prevHop, Nml.Topology))) {
                Resource prevSwSvc = getSwitchingServiceForHop(model, prevHop, currentHop);
                if (prevSwSvc != null) {
                    prevHop = prevSwSvc;
                }
            }
            if (nextHop != null && (ModelUtil.isResourceOfType(model, nextHop, Nml.Node)
                    || ModelUtil.isResourceOfType(model, nextHop, Nml.Topology))) {
                Resource nextSwSvc = getSwitchingServiceForHop(model, nextHop, currentHop);
                if (nextSwSvc != null) {
                    nextHop = nextSwSvc;
                }
            }
            // handle a special case where port exits a switch Node or Topology and goes back to SwitchingService under the same Node or Topology
            if (prevHop == nextHop) {
                return null;
            }
            if (ModelUtil.isResourceOfType(model, currentHop, Nml.BidirectionalPort)) {
                try {
                    handleL2PathHop(model, path, prevHop, currentHop, nextHop, lastPort, portParamMap, portTeMap);
                    lastPort = currentHop;
                } catch (TagSet.NoneVlanExeption ex) {
                    ;
                } catch (TagSet.EmptyTagSetExeption | TagSet.InvalidVlanRangeExeption ex) {
                    logger.trace(method, String.format("current hop = '%s' -- ", currentHop) + ex);
                    return null; // throw Exception ?
                }
            }
            prevHop = currentHop;
            if (last) {
                prevHop = currentHop;
                currentHop = nextHop;
                nextHop = null;
                Resource prevSwSvc = getSwitchingServiceForHop(model, prevHop, currentHop);
                if (prevSwSvc != null) {
                    prevHop = prevSwSvc;
                }
                if (ModelUtil.isResourceOfType(model, currentHop, Nml.BidirectionalPort)) {
                    try {
                        handleL2PathHop(model, path, prevHop, currentHop, nextHop, lastPort, portParamMap, portTeMap);
                        lastPort = currentHop;
                    } catch (TagSet.NoneVlanExeption ex) {
                        ;
                    } catch (TagSet.EmptyTagSetExeption | TagSet.InvalidVlanRangeExeption ex) {
                        logger.trace(method, String.format("current hop = '%s' -- ", currentHop) + ex);
                        return null; // throw Exception ?
                    } 
                }
            }
        }
        // Reverse iteration to calculate suggestedVlan and create SwitchingSubnet
        // insert portParamMap for 
        OntModel l2PathModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        nextHop = null;
        while (itS.hasPrevious()) {
            Statement stmt = itS.previous();
            prevHop = stmt.getSubject();
            currentHop = stmt.getObject().asResource();
            if (portParamMap.containsKey(currentHop)) {
                OntModel subnetModel = null;
                if (portParamMap.get(currentHop).containsKey("openflowService")) {
                    subnetModel = createVlanFlowsOnHop(model, prevHop, currentHop, nextHop, lastPort, portParamMap);
                } else {
                    subnetModel = createVlanSubnetOnHop(model, prevHop, currentHop, nextHop, lastPort, portParamMap, path);
                }
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
                    OntModel subnetModel = null;
                    if (portParamMap.get(currentHop).containsKey("openflowService")) {
                        subnetModel = createVlanFlowsOnHop(model, prevHop, currentHop, nextHop, lastPort, portParamMap);
                    } else {
                        subnetModel = createVlanSubnetOnHop(model, prevHop, currentHop, nextHop, lastPort, portParamMap, path);
                    }
                    if (subnetModel != null) {
                        l2PathModel.add(subnetModel.getBaseModel());
                    }
                }
            }
        }
        return l2PathModel;
    }

    private static void handleL2PathHop(Model model, Path path, Resource prevHop, Resource currentHop, Resource nextHop, Resource lastPort, HashMap portParamMap, JSONObject portTeMap)
            throws TagSet.NoneVlanExeption, TagSet.EmptyTagSetExeption, TagSet.InvalidVlanRangeExeption {
        if (prevHop != null && ModelUtil.isResourceOfType(model, prevHop, Nml.BidirectionalPort)) {
            //@TODO: handling adaptation?
        }
        TagSet allowedVlanRange = null;
        HashMap<String, Object> paramMap = new HashMap<>();
        if (portTeMap != null) {
            if (portTeMap.containsKey(currentHop.toString())) {
                JSONObject jsonTe = (JSONObject) portTeMap.get(currentHop.toString());
                if (jsonTe.containsKey("vlan_tag")) {
                    allowedVlanRange = new TagSet((String) jsonTe.get("vlan_tag"));
                    paramMap.put("allowedVlanRange", allowedVlanRange);
                }
            }
            if (allowedVlanRange == null && prevHop != null && portTeMap.containsKey(prevHop.toString())) {
                JSONObject jsonTe = (JSONObject) portTeMap.get(prevHop.toString());
                if (jsonTe.containsKey("vlan_tag")) {
                    allowedVlanRange = new TagSet((String) jsonTe.get("vlan_tag"));
                    paramMap.put("allowedVlanRange", allowedVlanRange);
                }
            }
            if (allowedVlanRange == null && nextHop != null && portTeMap.containsKey(nextHop.toString())) {
                JSONObject jsonTe = (JSONObject) portTeMap.get(nextHop.toString());
                if (jsonTe.containsKey("vlan_tag")) {
                    allowedVlanRange = new TagSet((String) jsonTe.get("vlan_tag"));
                    paramMap.put("allowedVlanRange", allowedVlanRange);
                }
            }
        }       
        HashMap<String, Object> lastParamMap = null;
        if (lastPort != null && portParamMap.containsKey(lastPort)) {
            lastParamMap = (HashMap<String, Object>) portParamMap.get(lastPort);
        }
        // Get VLAN range
        TagSet vlanRange = getVlanRangeForPort(model, currentHop, path.getBandwithScedule());
        // do nothing for port without a Vlan labelGroup
        if (vlanRange == null) {
            //special handling OpenFlow port (also port in hybrid mode ?)
            Resource ofSvc = getOpenflowServiceForPort(model, currentHop);
            if (ofSvc != null) {
                paramMap.put("openflowService", ofSvc);
                paramMap.put("vlanTranslation", true);
                vlanRange = getVlanRangeForOpenFlowPort(model, currentHop);
                paramMap.put("allowedVlanRange", allowedVlanRange);
                if (lastParamMap != null && lastParamMap.containsKey("vlanRange")) {
                    vlanRange.intersect((TagSet) lastParamMap.get("vlanRange"));
                }
                if (allowedVlanRange != null && !allowedVlanRange.isEmpty()) {
                    vlanRange.intersect(allowedVlanRange);
                }
                if (vlanRange.isEmpty()) {
                    throw new TagSet.EmptyTagSetExeption();
                }
                paramMap.put("vlanRange", vlanRange);
                portParamMap.put(currentHop, paramMap);
            }
            // either way throw harmless exception here
            throw new TagSet.NoneVlanExeption();
        }
        // interception with input availableVlanRange 
        Boolean vlanTranslation = true;
        Resource egressSwitchingService = null;
        if (prevHop != null) {
            // check vlan translation
            String sparql = String.format("SELECT ?swapping WHERE {<%s> a nml:SwitchingService. <%s> nml:labelSwapping ?swapping.}", prevHop, prevHop);
            ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
            if (rs.hasNext()) {
                egressSwitchingService = prevHop;
            }
            if (!rs.hasNext() || !rs.next().getLiteral("swapping").getBoolean()) {
                // non-translation
                vlanTranslation = false;
            }
        } else if (nextHop != null) {
            // check vlan translation
            String sparql = String.format("SELECT ?swapping WHERE {<%s> a nml:SwitchingService. <%s> nml:labelSwapping ?swapping.}", nextHop, nextHop);
            ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
            if (rs.hasNext()) {
                egressSwitchingService = nextHop;
            }
            if (!rs.hasNext() || !rs.next().getLiteral("swapping").getBoolean()) {
                // non-translation
                vlanTranslation = false;
            }
        }
        // vlan translation
        TagSet lastVlanRange = TagSet.VlanRangeANY().clone();
        // no vlan translation
        if (!vlanTranslation && lastParamMap != null && lastParamMap.containsKey("vlanRange")) {
            lastVlanRange = (TagSet) lastParamMap.get("vlanRange");
        }
        if (allowedVlanRange != null && !allowedVlanRange.isEmpty()) {
            vlanRange.intersect(allowedVlanRange);
        }
        vlanRange.intersect(lastVlanRange);
        // exception if empty        
        if (vlanRange.isEmpty()) {
            throw new TagSet.EmptyTagSetExeption();
        }
        // store non-empty vlanRange
        paramMap.put("vlanRange", vlanRange);
        if (egressSwitchingService != null) {
            paramMap.put("egressSwitchingService", egressSwitchingService);
            if (lastParamMap != null) {
                lastParamMap.put("ingressSwitchingService", egressSwitchingService);
                if (vlanTranslation) {
                    lastParamMap.put("vlanTranslation", true);                
                }
            }
        }
        paramMap.put("vlanTranslation", vlanTranslation);
        portParamMap.put(currentHop, paramMap);
    }

    private static OntModel createVlanSubnetOnHop(Model model, Resource prevHop, Resource currentHop, Resource nextHop, Resource lastPort, HashMap portParamMap, Path path) {
        String method = "createVlanSubnetOnHop";
        HashMap paramMap = (HashMap) portParamMap.get(currentHop);
        BandwidthProfile bwProfile = path.getBandwithProfile();
        String connId = path.getConnectionId();
        if (!paramMap.containsKey("vlanRange")) {
            return null;
        }
        TagSet vlanRange = (TagSet) paramMap.get("vlanRange");
        if (vlanRange.isEmpty()) {
            return null;
        }
        HashMap lastParamMap = null;
        if (lastPort != null && portParamMap.containsKey(lastPort)) {
            lastParamMap = (HashMap) portParamMap.get(lastPort);
        }
        Integer suggestedVlan = null;
        Integer lastVlan = null;
        if (lastParamMap != null && lastParamMap.containsKey("suggestedVlan")) {
            suggestedVlan = (Integer) lastParamMap.get("suggestedVlan");
            lastVlan = suggestedVlan;
            if (!vlanRange.hasTag(suggestedVlan)) { // if no continuous vlan
                if (!paramMap.containsKey("vlanTranslation")
                        || !(Boolean) paramMap.get("vlanTranslation")) { // try translation but not able to
                    return null;
                } else {
                    suggestedVlan = null;
                }
            }
        }
        // init vlan or do tanslation to any tag
        if (suggestedVlan == null) {
            suggestedVlan = vlanRange.getRandom();
            if (lastVlan == null) {
                lastVlan = suggestedVlan;
            }
        }
        paramMap.put("suggestedVlan", suggestedVlan);
        
        // create port and subnet statements into vlanSubnetModel
        OntModel vlanSubnetModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        
        // special handling for port with subport which both has the suggestedVlan and belongs to a shared subnet
        String sparql = String.format("SELECT ?vlan_port ?subnet WHERE {"
                + "<%s> nml:hasBidirectionalPort ?vlan_port. "
                + "?vlan_port nml:hasLabel ?l. "
                + "?l nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. "
                + "?l nml:value \"%d\". "
                + "?subnet nml:hasBidirectionalPort ?vlan_port. "
                + "?subnet a mrs:SwitchingSubnet. "
                + "?subnet mrs:type \"shared\". "
                + "}", currentHop, suggestedVlan);
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        if (rs.hasNext()) {
            QuerySolution solution = rs.next();
            Resource resVlanPort = solution.getResource("vlan_port");
            // do not verify shared vlan port
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Mrs.type, "unverifiable"));
            Resource resSubnet = solution.getResource("subnet");
            String vlanLabelUrn = resVlanPort.getURI() + ":label+"+suggestedVlan;
            // do not verify shared vlan port label
            Resource resVlanPortLabel = RdfOwl.createResourceUnverifiable(vlanSubnetModel, vlanLabelUrn, Nml.Label);
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, RdfOwl.type, Nml.BidirectionalPort));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Nml.hasLabel, resVlanPortLabel));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPortLabel, Nml.labeltype, RdfOwl.labelTypeVLAN));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPortLabel, Nml.value, suggestedVlan.toString()));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resSubnet, Nml.hasBidirectionalPort, resVlanPort));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Nml.belongsTo, resSubnet));
            return vlanSubnetModel;
        }
        
        // special handling for non-VLAN port with VLAN capable subports
        // find a in-range sub port and use that to temporarily replace currentHop
        sparql = String.format("SELECT ?range WHERE {"
                + "<%s> nml:hasLabelGroup ?lg. ?lg nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. ?lg nml:values ?range."
                + "}", currentHop);
        Resource subPort = null;
        rs = ModelUtil.sparqlQuery(model, sparql);
        if (!rs.hasNext()) {
            sparql = String.format("SELECT ?sub_port ?range WHERE {"
                    + "<%s> nml:hasBidirectionalPort ?sub_port. ?sub_port nml:hasLabelGroup ?lg. ?lg nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. ?lg nml:values ?range."
                    + "}", currentHop);
            rs = ModelUtil.sparqlQuery(model, sparql);
            while (rs.hasNext()) {
                QuerySolution solution = rs.next();
                TagSet vlanSubRange;
                try {
                    vlanSubRange = new TagSet(solution.getLiteral("range").toString());
                } catch (TagSet.InvalidVlanRangeExeption ex) {
                    throw logger.throwing(method, ex);
                }
                if (vlanSubRange.hasTag(suggestedVlan)) {
                    subPort = solution.getResource("sub_port");
                    currentHop = subPort;
                    break;
                }
            }
            if (subPort == null) {
                return null;
            }
        }

        String vlanPortUrn;
        Resource resVlanPort;
        if (subPort == null) { // create new VLAN port 
            vlanPortUrn = currentHop.toString() + ":vlanport+" + suggestedVlan;
            resVlanPort = RdfOwl.createResource(vlanSubnetModel, vlanPortUrn, Nml.BidirectionalPort);
            vlanSubnetModel.add(vlanSubnetModel.createStatement(currentHop, Nml.hasBidirectionalPort, resVlanPort));
        } else { // use existig VLAN port
            vlanPortUrn = currentHop.toString();
            resVlanPort = RdfOwl.createResource(vlanSubnetModel, vlanPortUrn, Nml.BidirectionalPort);
        }
        // create lifetime if scheduled reservation
        Resource resVlanLifetime = null;

        // create vlan label for either new or existing VLAN port
        String vlanLabelUrn = vlanPortUrn + ":label+"+suggestedVlan;
        Resource resVlanPortLabel = RdfOwl.createResource(vlanSubnetModel, vlanLabelUrn, Nml.Label);
        vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Nml.hasLabel, resVlanPortLabel));
        vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPortLabel, Nml.labeltype, RdfOwl.labelTypeVLAN));
        vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPortLabel, Nml.value, suggestedVlan.toString()));

        String labelSwappable = "true";
        if ((!paramMap.containsKey("vlanTranslation") || !(Boolean) paramMap.get("vlanTranslation"))) {
            labelSwappable = "false";
        }
        // create ingressSubnet for ingressSwitchingService and add port the the new subnet
        if (paramMap.containsKey("ingressSwitchingService")) {
            Resource ingressSwitchingService = (Resource) paramMap.get("ingressSwitchingService");
            String vlanSubnetUrn = ingressSwitchingService.toString() + ":conn+" + connId + ":vlan+" + lastVlan;
            Resource ingressSwitchingSubnet = RdfOwl.createResource(vlanSubnetModel, vlanSubnetUrn, Mrs.SwitchingSubnet);
            vlanSubnetModel.add(vlanSubnetModel.createStatement(ingressSwitchingService, Mrs.providesSubnet, ingressSwitchingSubnet));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(ingressSwitchingSubnet, Nml.encoding, RdfOwl.labelTypeVLAN));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(ingressSwitchingSubnet, Nml.labelSwapping, labelSwappable));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(ingressSwitchingSubnet, Nml.belongsTo, ingressSwitchingService));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(ingressSwitchingSubnet, Nml.hasBidirectionalPort, resVlanPort));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Nml.belongsTo, ingressSwitchingSubnet));
            if (path.getBandwithScedule() != null && resVlanLifetime == null) {
                String vlanLifetimeUrn = ingressSwitchingSubnet + ":lifetime";
                resVlanLifetime = RdfOwl.createResource(vlanSubnetModel, vlanLifetimeUrn, Nml.Lifetime);
                Literal ltStart = model.createTypedLiteral(DateTimeUtil.longToDateString(path.getBandwithScedule().getStartTime() * 1000L));
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanLifetime, Nml.start, ltStart));
                Literal ltEnd = model.createTypedLiteral(DateTimeUtil.longToDateString(path.getBandwithScedule().getEndTime()* 1000L));
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanLifetime, Nml.end, ltEnd));
                vlanSubnetModel.add(vlanSubnetModel.createStatement(ingressSwitchingSubnet, Nml.existsDuring, resVlanLifetime));
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Nml.existsDuring, resVlanLifetime));
            }
        }

        // get egressSubnet for egressSwitchingService and add port the this existing subnet
        if (paramMap.containsKey("egressSwitchingService")) {
            Resource egressSwitchingService = (Resource) paramMap.get("egressSwitchingService");
            String vlanSubnetUrn = egressSwitchingService.toString() + ":conn+" + connId + ":vlan+" + lastVlan;
            Resource egressSwitchingSubnet = RdfOwl.createResource(vlanSubnetModel, vlanSubnetUrn, Mrs.SwitchingSubnet);
            vlanSubnetModel.add(vlanSubnetModel.createStatement(egressSwitchingService, Mrs.providesSubnet, egressSwitchingSubnet));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(egressSwitchingSubnet, Nml.encoding, RdfOwl.labelTypeVLAN));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(egressSwitchingSubnet, Nml.labelSwapping, labelSwappable));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(egressSwitchingSubnet, Nml.belongsTo, egressSwitchingService));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(egressSwitchingSubnet, Nml.hasBidirectionalPort, resVlanPort));
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Nml.belongsTo, egressSwitchingSubnet));
            if (path.getBandwithScedule() != null && resVlanLifetime == null) {
                String vlanLifetimeUrn = egressSwitchingSubnet + ":lifetime";
                resVlanLifetime = RdfOwl.createResource(vlanSubnetModel, vlanLifetimeUrn, Nml.Lifetime);
                Literal ltStart = model.createTypedLiteral(DateTimeUtil.longToDateString(path.getBandwithScedule().getStartTime() * 1000L));
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanLifetime, Nml.start, ltStart));
                Literal ltEnd = model.createTypedLiteral(DateTimeUtil.longToDateString(path.getBandwithScedule().getEndTime()* 1000L));
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanLifetime, Nml.end, ltEnd));
                vlanSubnetModel.add(vlanSubnetModel.createStatement(egressSwitchingSubnet, Nml.existsDuring, resVlanLifetime));
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Nml.existsDuring, resVlanLifetime));
            }
        }

        // add BandwidthService to vlan port if applicable
        if (bwProfile != null && bwProfile.type != null && !bwProfile.type.equalsIgnoreCase("bestEffort")) {
            String vlanBwServiceUrn = vlanPortUrn + ":service+bw";
            Resource resVlanBwService = RdfOwl.createResource(vlanSubnetModel, vlanBwServiceUrn, Mrs.BandwidthService);
            vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanPort, Nml.hasService, resVlanBwService));
            if (bwProfile.type != null) {
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanBwService, Mrs.type, bwProfile.type));
            }
            if (bwProfile.unit != null) {
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanBwService, Mrs.unit, bwProfile.unit));
            }
            if (bwProfile.priority != null) {
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanBwService, Mrs.priority, bwProfile.priority));
            }
            if (bwProfile.maximumCapacity != null) {
                Literal lMaxBw = model.createTypedLiteral(bwProfile.maximumCapacity);
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanBwService, Mrs.maximumCapacity, lMaxBw));
            }
            if (bwProfile.availableCapacity != null) {
                Literal lAvailBw = model.createTypedLiteral(bwProfile.availableCapacity);
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanBwService, Mrs.availableCapacity, lAvailBw));
            }
            if (bwProfile.reservableCapacity != null) {
                Literal lResvBw = model.createTypedLiteral(bwProfile.reservableCapacity);
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanBwService, Mrs.reservableCapacity, lResvBw));
            }
            if (bwProfile.granularity != null) {
                Literal lGranularity = model.createTypedLiteral(bwProfile.granularity);
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanBwService, Mrs.granularity, lGranularity));
            }
            if (resVlanLifetime != null) {
                vlanSubnetModel.add(vlanSubnetModel.createStatement(resVlanBwService, Nml.existsDuring, resVlanLifetime));
            }
        }
        return vlanSubnetModel;
    }
    
    public static void tagPathHops(MCETools.Path l2path, String tag) {
        OntModel model = l2path.getOntModel();
        String sparql = String.format("SELECT DISTINCT ?bp ?subnet  WHERE {"
                + " ?bp a nml:BidirectionalPort. "
                + " ?bp nml:hasLabel ?vlan."
                + " ?vlan nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> "
                + " OPTIONAL {?subnet nml:hasBidirectionalPort ?bp. ?subnet a mrs:SwitchingSubnet.} "
                + "}");
        ResultSet r = ModelUtil.sparqlQuery(model, sparql);
        List<Statement> addStmts = new ArrayList<>();
        while (r.hasNext()) {
            QuerySolution solution = r.next();
            Resource resSubport = solution.getResource("bp");
            addStmts.add(model.createStatement(resSubport, Mrs.tag, tag));
            if (solution.contains("subnet")) {
                Resource resSubnet = solution.getResource("subnet");
                addStmts.add(model.createStatement(resSubnet, Mrs.tag, tag));
            }
        }
        sparql = String.format("SELECT DISTINCT ?flow  WHERE {"
                + " ?flow a mrs:Flow. "
                + " ?of mrs:providesFlow ?flow."
                + "}");
        r = ModelUtil.sparqlQuery(model, sparql);
        while (r.hasNext()) {
            QuerySolution solution = r.next();
            Resource resFlow = solution.getResource("flow");
            addStmts.add(model.createStatement(resFlow, Mrs.tag, tag));
        }
        model.add(addStmts);
    }
    
    public static void pairupPathHops(MCETools.Path l2path,  OntModel refModel) {
        OntModel pathModel = l2path.getOntModel();
        String sparql = "SELECT DISTINCT ?port ?vlan_port WHERE {"
                + " ?vlan_port a nml:BidirectionalPort. "
                + " ?port nml:hasBidirectionalPort ?vlan_port. "
                + " FILTER NOT EXISTS { ?port a mrs:SwitchingSubnet. } "
                + "}";
        Map<String, Resource> parentChildPortMap = new HashMap();
        ResultSet r = ModelUtil.sparqlQuery(pathModel, sparql);
        while (r.hasNext()) {
            QuerySolution solution = r.next();
            Resource resPort = solution.getResource("port");
            Resource resVlanPort = solution.getResource("vlan_port");
            parentChildPortMap.put(resPort.getURI(), resVlanPort);
        }
        Iterator<Statement> it = l2path.iterator();
        while (it.hasNext()) {
            Statement hopStmt = it.next();
            Resource hopX = hopStmt.getSubject();
            Resource hopY = hopStmt.getObject().asResource();
            if (!parentChildPortMap.containsKey(hopX.getURI()) || !parentChildPortMap.containsKey(hopY.getURI())) {
                continue;
            }
            if (refModel.contains(hopX, Nml.isAlias, hopY) || refModel.contains(hopY, Nml.isAlias, hopX)) {
                pathModel.add(pathModel.createStatement(parentChildPortMap.get(hopX.getURI()), Nml.isAlias, parentChildPortMap.get(hopY.getURI())));
                pathModel.add(pathModel.createStatement(parentChildPortMap.get(hopY.getURI()), Nml.isAlias, parentChildPortMap.get(hopX.getURI())));
            }
        }
    }
    
    public static List<QuerySolution> getTerminalVlanLabels(MCETools.Path l2path) {
        OntModel model = l2path.getOntModel();
        String sparql = String.format("SELECT ?bp ?vlan ?tag WHERE {"
                + " ?bp a nml:BidirectionalPort. "
                + " ?bp nml:hasLabel ?vlan."
                + " ?vlan nml:value ?tag."
                + " ?vlan nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>}");
        ResultSet r = ModelUtil.sparqlQuery(model, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        return solutions;
    }
    
    //@TODO: use action name and value for URI, add mrs:order 
    private static OntModel createVlanFlowsOnHop(Model model, Resource prevHop, Resource currentHop, Resource nextHop, Resource lastPort, HashMap portParamMap) {
        HashMap paramMap = (HashMap) portParamMap.get(currentHop);
        if (!paramMap.containsKey("vlanRange")) {
            return null;
        }
        TagSet vlanRange = (TagSet) paramMap.get("vlanRange");
        if (vlanRange.isEmpty()) {
            return null;
        }
        // try use same suggested VLAN from next hop OpenFlow port or from last (reverse order) 'VLAN' port
        HashMap lastParamMap = null;
        if (nextHop != null && portParamMap.containsKey(nextHop)) {
            lastParamMap = (HashMap) portParamMap.get(nextHop);
        }
        if (lastParamMap == null || !lastParamMap.containsKey("suggestedVlan")) {
            lastParamMap = (HashMap) portParamMap.get(lastPort);
        }
        Integer flowNameVlan = null; // hold the tag for consistent flow naming
        Integer suggestedVlan = null;
        if (lastParamMap != null && lastParamMap.containsKey("suggestedVlan")) {
            suggestedVlan = (Integer) lastParamMap.get("suggestedVlan");
            if (!vlanRange.hasTag(suggestedVlan)) { // suggestedVlan invalid //@TODO: exception if (nextHop == lastPort)
                flowNameVlan = suggestedVlan;
                suggestedVlan = null;
            }
        }
        // init vlan or do tanslation to any tag
        if (suggestedVlan == null) {
            suggestedVlan = vlanRange.getRandom();
        }
        if (flowNameVlan == null) {
            flowNameVlan = suggestedVlan;
        }
        paramMap.put("suggestedVlan", suggestedVlan);
        
        // create port and subnet statements into vlanSubnetModel
        OntModel vlanFlowsModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        Resource resFlowSvc = (Resource) paramMap.get("openflowService");
        Resource resFlowTable = model.getResource(resFlowSvc.getURI()+":table=0"); // use assumed URI -> or search in model
        String portName = getNameForPort(model, currentHop);
        if (portName == null) {
            portName = currentHop.getURI();
        }
        // add 'VLAN' flows for this port 
        if (prevHop != null && prevHop.equals(resFlowSvc)) {
            //$$ add new flow with match currentHop as in_port & match suggestedVlan + action = strip VLAN
            String inFlowId = currentHop.getURI() + ":flow=input_vlan"+flowNameVlan;
            Resource resInFlow = RdfOwl.createResource(vlanFlowsModel, URI_flow(resFlowTable.getURI(), inFlowId), Mrs.Flow);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowTable, Mrs.hasFlow, resInFlow));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowSvc, Mrs.providesFlow, resInFlow));
            
            Resource resMatchRule1 = RdfOwl.createResource(vlanFlowsModel, URI_match(resInFlow.getURI(), "in_port"), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resInFlow, Mrs.flowMatch, resMatchRule1));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resMatchRule1, Mrs.type, "in_port"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resMatchRule1, Mrs.value, portName));
            
            Resource resMatchRule2 = RdfOwl.createResource(vlanFlowsModel, URI_match(resInFlow.getURI(), "dl_vlan"), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resInFlow, Mrs.flowMatch, resMatchRule2));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resMatchRule2, Mrs.type, "dl_vlan"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resMatchRule2, Mrs.value, suggestedVlan.toString()));
            Character flowActionOrder = '0';
            /*
            Resource resFlowAction1 = RdfOwl.createResource(vlanFlowsModel, URI_action(resInFlow.getURI(), "A"), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resInFlow, Mrs.flowAction, resFlowAction1));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction1, Mrs.type, "strip_vlan"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction1, Mrs.value, "strip_vlan"));
            */
            //$$ add new flow with action output to currentHop + swap suggestedVlan VLAN 
            String outFlowId = currentHop.getURI() + ":flow=output_vlan"+flowNameVlan;
            Resource resOutFlow = RdfOwl.createResource(vlanFlowsModel, URI_flow(resFlowTable.getURI(), outFlowId), Mrs.Flow);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowTable, Mrs.hasFlow, resOutFlow));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowSvc, Mrs.providesFlow, resOutFlow));

            Resource resFlowAction2 = RdfOwl.createResource(vlanFlowsModel, URI_action(resOutFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resOutFlow, Mrs.flowAction, resFlowAction2));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction2, Mrs.type, "mod_vlan_vid"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction2, Mrs.value, suggestedVlan.toString()));            

            Resource resFlowAction3 = RdfOwl.createResource(vlanFlowsModel, URI_action(resOutFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resOutFlow, Mrs.flowAction, resFlowAction3));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction3, Mrs.type, "output"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction3, Mrs.value, portName));
        }
        if (nextHop != null && nextHop.equals(resFlowSvc) && lastPort != null) {
            // swap input and output flow IDs from lastPort
            // check input and output flows exist before swap
            String inFlowId = lastPort.getURI() + ":flow=output_vlan" + flowNameVlan;
            Resource resInFlow = RdfOwl.createResource(vlanFlowsModel, URI_flow(resFlowTable.getURI(), inFlowId), Mrs.Flow);
            //$$ add match: currentHop as in_port & suggestedVlan
            //$$ add action: strip VLAN
            Resource resMatchRule1 = RdfOwl.createResource(vlanFlowsModel, URI_match(resInFlow.getURI(), "in_port"), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resInFlow, Mrs.flowMatch, resMatchRule1));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resMatchRule1, Mrs.type, "in_port"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resMatchRule1, Mrs.value, portName));

            Resource resMatchRule2 = RdfOwl.createResource(vlanFlowsModel, URI_match(resInFlow.getURI(), "dl_vlan"), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resInFlow, Mrs.flowMatch, resMatchRule2));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resMatchRule2, Mrs.type, "dl_vlan"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resMatchRule2, Mrs.value, suggestedVlan.toString()));
            Character flowActionOrder = '0';
            /*
            Resource resFlowAction1 = RdfOwl.createResource(vlanFlowsModel, URI_action(resInFlow.getURI(), "A"), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resInFlow, Mrs.flowAction, resFlowAction1));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction1, Mrs.type, "strip_vlan"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction1, Mrs.value, "strip_vlan"));
            */
            String outFlowId = lastPort.getURI() + ":flow=input_vlan" + flowNameVlan;
            Resource resOutFlow = RdfOwl.createResource(vlanFlowsModel, URI_flow(resFlowTable.getURI(), outFlowId), Mrs.Flow);

            //$$ add actions: output to currentHop + swap suggestedVlan 
            Resource resFlowAction2 = RdfOwl.createResource(vlanFlowsModel, URI_action(resOutFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resOutFlow, Mrs.flowAction, resFlowAction2));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction2, Mrs.type, "mod_vlan_vid"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction2, Mrs.value, suggestedVlan.toString()));

            Resource resFlowAction3 = RdfOwl.createResource(vlanFlowsModel, URI_action(resOutFlow.getURI(), (flowActionOrder++).toString()), Mrs.FlowRule);
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resOutFlow, Mrs.flowAction, resFlowAction3));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction3, Mrs.type, "output"));
            vlanFlowsModel.add(vlanFlowsModel.createStatement(resFlowAction3, Mrs.value, portName));
        }

        return vlanFlowsModel;
    }
    
    private static Resource getSwitchingServiceForHop(Model model, Resource nodeOrTopo, Resource port) {
        String sparql = String.format("SELECT ?sw WHERE {<%s> nml:hasService ?sw. ?sw a nml:SwitchingService. ?sw nml:hasBidirectionalPort <%s>.}", nodeOrTopo, port);
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        if (!rs.hasNext()) {
            return null;
        }
        return rs.next().getResource("sw");
    }

    private static Resource getOpenflowServiceForPort(Model model, Resource port) {
        String sparql = String.format("SELECT ?svc WHERE {"
                + "?node nml:hasService ?svc. ?svc nml:hasBidirectionalPort <%s>. ?svc a mrs:OpenflowService."
                + "}", port);
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        if (rs.hasNext()) {
            return rs.next().getResource("svc");
        }
        return null;
    }

    public static String getNameForPort(Model model, Resource port) {
        String sparql = String.format("SELECT ?name WHERE {"
                + "<%s> nml:name ?name."
                + "}", port);
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        if (rs.hasNext()) {
            return rs.next().get("name").toString();
        }
        return null;
    }

    public static boolean isSameNodePorts(Model model, Resource port1, Resource port2) {
        String sparql = String.format("SELECT ?node WHERE {{"
                + "?node nml:hasBidirectionalPort <%s>. "
                + "?node nml:hasBidirectionalPort <%s>. "
                + "} UNION {"
                + "?node nml:hasService ?svc. ?svc nml:hasBidirectionalPort <%s>. "
                + "?node nml:hasService ?svc. ?svc nml:hasBidirectionalPort <%s>. "
                + "}}", port1, port2, port1, port2);
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        return rs.hasNext();
    }
    
    // get VLAN range for the port plus all the available ranges in sub-ports (LabelGroup) and remove allocated vlans (Label)
    private static TagSet getVlanRangeForPort(Model model, Resource port, BandwidthCalendar.BandwidthSchedule schedule) 
            throws TagSet.InvalidVlanRangeExeption {
        TagSet vlanRange = null;
        String sparql = String.format("SELECT ?range WHERE {"
                + "<%s> nml:hasLabelGroup ?lg. ?lg nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. ?lg nml:values ?range."
                + "}", port);
        ResultSet rs = ModelUtil.sparqlQuery(model, sparql);
        if (rs.hasNext()) {
            vlanRange = new TagSet(rs.next().getLiteral("range").toString());
        } else {
            sparql = String.format("SELECT ?range WHERE {{"
                    + "<%s> nml:hasBidirectionalPort ?vlan_port. "
                    + "?vlan_port nml:hasLabelGroup ?lg. "
                    + "?lg nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. "
                    + "?lg nml:values ?range."
                    + "} UNION {"
                    + "<%s> nml:hasBidirectionalPort ?vlan_port. "
                    + "?subnet nml:hasBidirectionalPort ?vlan_port. "
                    + "?subnet a mrs:SwitchingSubnet. "
                    + "?subnet mrs:type \"shared\". "
                    + "?vlan_port nml:hasLabel ?l. "
                    + "?l nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. "
                    + "?l nml:value ?range."
                    + "}}", port, port);
            rs = ModelUtil.sparqlQuery(model, sparql);
            while (rs.hasNext()) {
                TagSet vlanRangeAdd = new TagSet(rs.next().getLiteral("range").toString());
                if (vlanRange == null) {
                    vlanRange = vlanRangeAdd;
                } else {
                    vlanRange.join(vlanRangeAdd);
                }
            }
        }
        if (vlanRange == null || vlanRange.isEmpty()) {
            return null;
        }
        sparql = String.format("SELECT DISTINCT ?vlan ?start ?end WHERE { {"
                + "<%s> nml:hasBidirectionalPort ?vlan_port. "
                + "?vlan_port nml:hasLabel ?l. ?l nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. "
                + "?l nml:value ?vlan."
                + "OPTIONAL {?vlan_port nml:existsDuring ?lifetime. ?lifetime nml:start ?start. ?lifetime nml:end ?end.} "
                + "FILTER NOT EXISTS { ?vlan_port mrs:type \"shared\". }"
                + "} UNION {"
                + "<%s> nml:hasLabel ?l. ?l nml:labeltype <http://schemas.ogf.org/nml/2012/10/ethernet#vlan>. "
                + "?l nml:value ?vlan. "
                + "FILTER NOT EXISTS { <%s> mrs:type \"shared\". }"
                + "}"
                + "FILTER NOT EXISTS {"
                + "?subnet nml:hasBidirectionalPort ?vlan_port. "
                + "?subnet a mrs:SwitchingSubnet. "
                + "?subnet mrs:type \"shared\". "
                + "} }", port, port, port);
        rs = ModelUtil.sparqlQuery(model, sparql);
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            String vlanStr = qs.getLiteral("?vlan").toString();
            Integer vlan = Integer.valueOf(vlanStr);
            if (qs.contains("start") && schedule != null) {
                try {
                    long start = DateTimeUtil.getBandwidthScheduleSeconds(qs.getLiteral("start").getString());
                    long end = DateTimeUtil.getBandwidthScheduleSeconds(qs.getLiteral("end").getString());
                    if (start >= schedule.getEndTime() || end <= schedule.getStartTime()) {
                        continue;
                    }
                } catch (Exception ex) {
                    continue; // something wrong with the schedule format, skip the VLAN 
                }
            }
            vlanRange.removeTag(vlan);
        }
        return vlanRange;
    }

    // get VLAN range for the port plus all the available ranges in sub-ports (LabelGroup) and remove allocated vlans (Label)
    private static TagSet getVlanRangeForOpenFlowPort(Model model, Resource port) {
        String method = "getVlanRangeForOpenFlowPort";
        String portName = getNameForPort(model, port);
        TagSet vlanRange = TagSet.VlanRangeANY().clone();
        String sparql = String.format("SELECT ?vlan WHERE {{"
                + "?flow mrs:flowMatch ?match_port. "
                + "?match_port mrs:type \"in_port\". "
                + "?match_port mrs:value \"%s\" . "
                + "?flow mrs:flowMatch ?match_vlan. "
                + "?match_vlan mrs:type \"dl_vlan\". "
                + "?match_vlan mrs:value ?vlan. "
                + "} UNION {"
                + "?flow mrs:flowAction ?action_port. "
                + "?action_port mrs:type \"output\". "
                + "?action_port mrs:value \"%s\" . "
                + "?flow mrs:flowAction ?action_vlan. "
                + "?action_vlan mrs:type \"mod_vlan_vid\". "
                + "?action_vlan mrs:value ?vlan. "
                + "}}", portName, portName);
        
        //@TODO: Switch wide VLAN exclusion: list all ports included in the same openflow service and exclude their VLANs.
        ResultSet r = ModelUtil.sparqlQuery(model, sparql);
        while (r.hasNext()) {
            QuerySolution qs = r.next();
            String vlan = qs.get("vlan").toString();
            try {
                int vtag = Integer.parseInt(vlan);
                vlanRange.removeTag(vtag);
            } catch (NumberFormatException ex) {
                logger.warning(method, String.format("port '%s' has invalid vlan tag '%s' in flows -- %s", port, vlan, ex));
            }
        }
        return vlanRange;
    }
}
