/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntTools.PredicatesFilter;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;

import net.maxgigapop.mrs.common.Sna;
import static net.maxgigapop.mrs.service.compute.MCETools.evaluateStatement_AnyTrue;

/**
 *
 * @author hbai
 */
@Stateless
public class MCE_L2OpenflowPath implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_MPVlanConnection.class.getName());
    /*
     ** Simple L2 connection will create new SwitchingSubnet on every transit switching node.
     */

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(ModelBase systemModel, ServiceDelta annotatedDelta) {
        try {
            log.log(Level.INFO, "\n>>>MCE_L2OpenflowPath--DeltaAddModel Input=\n" + ModelUtil.marshalModel(annotatedDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("entering L2OpenflowPath process!");

        // need to fix this part to read from spa-compile-onos2.xml
        // importPolicyData : Link->Connection->List<PolicyData> of terminal Node/Topology
        String sparql = "SELECT ?link ?type ?data ?policyData WHERE {"
                + "?link a spa:PolicyAction . "
                + "?link spa:type ?type . "
                + "?link spa:importFrom ?data . "
                + "?link spa:exportTo ?policyData . "
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        
        Map<Resource, List> linkMap = new HashMap<>();
        while(r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resLink = querySolution.get("link").asResource();
            if (!linkMap.containsKey(resLink)) {
                List linkList = new ArrayList<>();
                linkMap.put(resLink, linkList);
            }
            Resource resData = querySolution.get("data").asResource();
            Resource resPolicyData = querySolution.getResource("policyData").asResource();
            RDFNode resType = querySolution.get("type");
            
            String sqlValue =  String.format("SELECT ?value WHERE {<%s> a spa:PolicyData. <%s> spa:value ?value.}", resData.toString(), resData.toString());
            ResultSet portValueResult = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sqlValue);
            Resource resPort = portValueResult.next().get("value").asResource();
            
            if(resType.toString().equals("MCE_L2OpenflowPath")){
                Map linkData = new HashMap<>();
                linkData.put("port", resPort);
                linkData.put("policyData", resPolicyData);
                linkData.put("type", resType.toString());
                linkMap.get(resLink).add(linkData);
            }
        }
        
        Map<Resource, List> srrgMap = this.getSrrgInfo(systemModel.getOntModel());
        
        //test printout
        //System.out.format("there are %d SRRG\n", srrgMap.size());
        
        ServiceDelta outputDelta = annotatedDelta.clone();

        // compute a List<Model> of L2Openflow connections
        for (Resource resLink : linkMap.keySet()) {

            //MCETools.Path l2path = this.doSrrgPathFinding(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), resLink, linkMap.get(resLink), srrgMap);
            
            List<MCETools.Path> l2pathList = this.doSrrgPairPathFinding(
                    systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), 
                    resLink, linkMap.get(resLink), srrgMap);
            
            if(l2pathList == null){
                throw new EJBException(String.format("%s::process cannot find a path pair for %s", this.getClass().getName(), resLink));
            }
            
            MCETools.Path l2path = l2pathList.get(0);
            MCETools.Path l2path_back = l2pathList.get(1);
            
            if (l2path == null) {
                throw new EJBException(String.format("%s::process cannot find a path for %s", this.getClass().getName(), resLink));
            }
            //System.out.println("Done finding a srrg Path");
            
            //2. merge the placement satements into spaModel
            outputDelta.getModelAddition().getOntModel().add(l2path.getOntModel().getBaseModel());

            //3. update policyData this action exportTo 
            this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), resLink, l2path);

         //4. remove policy and all related SPA statements receursively under link from spaModel
            //   and also remove all statements that say dependOn this 'policy'
            MCETools.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), resLink);

            //5. mark the Link as an Abstraction
            outputDelta.getModelAddition().getOntModel().add(outputDelta.getModelAddition().getOntModel().createStatement(resLink, RdfOwl.type, Spa.Abstraction));
        }

        try {
            log.log(Level.FINE, "\n>>>MCE_MPVlanConnection--DeltaAddModel Output=\n" + ModelUtil.marshalModel(outputDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new AsyncResult(outputDelta);
    }

    private Map<Resource, List> getSrrgInfo(OntModel systemModel) {

        //get SRRG information from OntModel
        String sparql = "SELECT ?srrg ?port ?node ?severity ?probability WHERE{"
                + "?srrg a sna:SRRG. "
                + "?srrg nml:hasBidirectionalPort ?port. "
                + "?srrg nml:hasNode ?node. "
                + "?srrg sna:severity ?severity. "
                + "?srrg sna:occurenceProbability ?probability. "
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(systemModel, sparql);
        Map<Resource, List> srrgMap = new HashMap<>();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resSRRG = querySolution.get("srrg").asResource();
            if (!srrgMap.containsKey(resSRRG)) {
                List srrgList = new ArrayList<>();
                srrgMap.put(resSRRG, srrgList);
            }
            Resource resBidirectionalPort = querySolution.get("port").asResource();
            Resource resNode = querySolution.get("node").asResource();
            RDFNode severity = querySolution.get("severity");
            RDFNode probability = querySolution.get("probability");

            Map srrgData = new HashMap<>();
            srrgData.put("port", resBidirectionalPort);
            srrgData.put("node", resNode);
            srrgData.put("severity", severity.toString());
            srrgData.put("probability", probability.toString());
            srrgMap.get(resSRRG).add(srrgData);

        }

        return srrgMap;
    }

    private MCETools.Path doSrrgPathFinding(OntModel systemModel, OntModel spaModel, Resource resLink, List<Map> connTerminalData, Map<Resource, List> srrgMap) {

        //return one shortest path with minimum SRRG probability
        //filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2OpenflowPathModel(systemModel);
        
        //verify is transformedModel is bidirectional connectsTo
        if(!verifyConnectsToModel(transformedModel)){
            throw new EJBException(String.valueOf("transformedModel is not fully bidirectional\n"));
        }
        
        //get source and dest nodes (nodeA, nodeZ), initially Link points to 2 biDirectionalPort
        List<Resource> terminals = new ArrayList<>();
 
        for (Map entry : connTerminalData) {
            if (!entry.containsKey("policyData") || !entry.containsKey("type")) {
                continue;
            }
            Resource terminal = systemModel.getResource(entry.get("port").toString());
            if (terminal == null) {
                throw new EJBException(String.format("%s::process doSrrgPathFinding cannot identify terminal <%s>", (String) entry.get("port")));
            }
            terminals.add(terminal);
        }
       
        if (terminals.size() != 2) {
            throw new EJBException(String.format("%s::process cannot doSrrgPathFinding for %s which provides not 2 terminals", this.getClass().getName(), resLink));
        }

        Resource nodeA = terminals.get(0);
        Resource nodeZ = terminals.get(1);
        
        System.out.format("src: %s\ndst: %s\n", nodeA.toString(), nodeZ.toString());

        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
        List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, 20, connFilters);
        
        System.out.format("Found %d shortest path before verify\n", KSP.size());
        
        if (KSP == null || KSP.isEmpty()) {
            throw new EJBException(String.format("%s::process doSrrgPathFinding cannot find any feasible path for <%s>", resLink));
        }

        Iterator<MCETools.Path> itP = KSP.iterator();
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();
            
            //verifyOpenflowPath: filter out useless ones
            if (!MCETools.verifyOpenFlowPath(transformedModel, candidatePath)) {
                itP.remove();
            } else {
                //generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                OntModel l2PathModel = MCETools.createL2PathVlanSubnets(transformedModel, candidatePath);
                if (l2PathModel == null) {
                    itP.remove();
                } else {
                    candidatePath.setOntModel(l2PathModel);
                }
            }
        }
        
        if (KSP.isEmpty()) {
            System.out.println("Could not find any shortest path after verify\n");
            return null;
        } else {
            System.out.format("Find %d KSP after verify\n", KSP.size());
        }

        //MCETools.printKSP(KSP);
        
        MCETools.Path solution = getLeastSrrgCostPath(KSP, systemModel, srrgMap);
        System.out.format("Picked path with fail prob: %f\n", solution.failureProb);
        MCETools.printMCEToolsPath(solution);
        return solution;

    }

    private boolean verifyConnectsToModel(OntModel transformedModel){
        
        //test: print out who connects to who
        String sparql = "SELECT ?data1 ?data2 WHERE {"
                + "?data1 nml:connectsTo ?data2. "
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(transformedModel, sparql);
        
        Map<Resource, List> connMap = new HashMap<>();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resData1 = querySolution.get("data1").asResource();
            if (!connMap.containsKey(resData1)) {
                List connList = new ArrayList<>();
                connMap.put(resData1, connList);
            }

            Resource resData2 = querySolution.get("data2").asResource();
            Map connData = new HashMap<>();
            connData.put("dst", resData2);
            connMap.get(resData1).add(connData);
        }
        //verify
        int i=0, j=0;
        for (Resource src : connMap.keySet()) {
            List connList = connMap.get(src);
            
            for (i = 0; i < connList.size(); i++) {
                Resource dst = (Resource) ((Map) connList.get(i)).get("dst");
                List connListCheck = connMap.get(dst);
                for (j = 0; j < connListCheck.size(); j++) {
                    Resource srcCheck = (Resource) ((Map) connListCheck.get(j)).get("dst");
                    if (src.equals(srcCheck)) {
                        break;
                    }
                }
                if (j == connListCheck.size()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private List<MCETools.Path> doSrrgPairPathFinding(OntModel systemModel, OntModel spaModel, Resource resLink, List<Map> connTerminalData, Map<Resource, List> srrgMap) {

        //return a list that has two elements, one is primary path, the other is backup path
        List<MCETools.Path> solutionList = new ArrayList<>();

        //filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        //filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2OpenflowPathModel(systemModel);
        
        //verify is transformedModel is bidirectional connectsTo
        if(!verifyConnectsToModel(transformedModel)){
            throw new EJBException(String.valueOf("transformedModel is not fully bidirectional\n"));
        }
        
        //get source and dest nodes (nodeA, nodeZ), initially Link points to 2 biDirectionalPort
        List<Resource> terminals = new ArrayList<>();
 
        for (Map entry : connTerminalData) {
            if (!entry.containsKey("policyData") || !entry.containsKey("type")) {
                continue;
            }
            Resource terminal = systemModel.getResource(entry.get("port").toString());
            if (terminal == null) {
                throw new EJBException(String.format("%s::process doSrrgPathFinding cannot identify terminal <%s>", (String) entry.get("port")));
            }
            terminals.add(terminal);
        }
       
        if (terminals.size() != 2) {
            throw new EJBException(String.format("%s::process cannot doSrrgPathFinding for %s which provides not 2 terminals", this.getClass().getName(), resLink));
        }

        Resource nodeA = terminals.get(0);
        Resource nodeZ = terminals.get(1);
        
        System.out.format("src: %s\ndst: %s\n", nodeA.toString(), nodeZ.toString());

        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
        
        List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, 20, connFilters);
        
        System.out.format("Found %d KSP (working) before verify\n", KSP.size());
        
        if (KSP == null || KSP.isEmpty()) {
            throw new EJBException(String.format("%s::process doSrrgPathFinding cannot find any feasible path for <%s>", resLink));
        }

        Iterator<MCETools.Path> itP = KSP.iterator();
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();
            
            //verifyOpenflowPath: filter out useless ones
            if (!MCETools.verifyOpenFlowPath(transformedModel, candidatePath)) {
                itP.remove();
            } else {
                //generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                OntModel l2PathModel = MCETools.createL2PathVlanSubnets(transformedModel, candidatePath);
                if (l2PathModel == null) {
                    itP.remove();
                } else {
                    candidatePath.setOntModel(l2PathModel);
                }
            }
        }
        
        if (KSP.isEmpty()) {
            System.out.println("Could not find any shortest path after verify\n");
            return null;
        } else {
            System.out.format("Find %d KSP (working) after verify\n", KSP.size());
        }
        

        double jointProbability = 1.0;
        double cost = 100000.0;
        int flag = -1;
        MCETools.Path solutionBack = null;
        
        for (int i = 0; i < KSP.size(); i++) {

            System.out.format("\nfor working path %d:\n", i);
            MCETools.Path path = KSP.get(i);
            
            transformedModel = MCETools.transformL2OpenflowPathModel(systemModel);

            //verify is transformedModel is bidirectional connectsTo
            if (!verifyConnectsToModel(transformedModel)) {
                throw new EJBException(String.valueOf("transformedModel is not fully bidirectional\n"));
            }

            MCETools.Path backPath = this.getLinkDisjointPath(transformedModel, path, resLink, terminals, srrgMap);

            if (backPath == null) {
                continue;
            }

            //joint fail prob: both working and protection fails
            jointProbability = (1-this.getPathProbability(transformedModel, path, srrgMap)) 
                    * (1-this.getPathProbability(transformedModel, backPath, srrgMap));

            if ( jointProbability < cost) {
                flag = i;
                cost = jointProbability;
                solutionBack = backPath;
            }
        }

        if (flag == -1) {
            return null;
        }
        
        if (flag >= KSP.size()) {
            throw new EJBException(String.format("%s::process doPathFinding cannot find feasible path for <%s>", resLink));
        }

        System.out.format("\nselect pair %d\n", flag);
        
        solutionList.add(KSP.get(flag));
        solutionList.add(solutionBack);

        return solutionList;
    }

    private MCETools.Path getLeastSrrgCostPath(List<MCETools.Path> candidates, OntModel systemModel, Map<Resource, List> srrgMap) {
      
        double cost = 1000000.0;
        MCETools.Path solution = null;
        
        for (MCETools.Path path : candidates) {
            
            //getPathProbability returns path success probability = (1-p1)(1-p2)...
            path.failureProb = 1- this.getPathProbability(systemModel, path, srrgMap);
            //System.out.format("this path's probability is %f\n", pathProbability);
            if (path.failureProb < cost) {
                cost = path.failureProb;
                solution = path;
            }

            if (path.failureProb == cost) {
                if (path.size() < solution.size()) {
                    cost = path.failureProb;
                    solution = path;
                }
            }
        }
        return solution;
    }

    private MCETools.Path getLinkDisjointPath(OntModel systemModel, MCETools.Path primaryPath, Resource resLink, List<Resource> terminals, Map<Resource, List> srrgMap) {
        //return one path with min srrg probability that is link disjoint to primary path
        
        MCETools.printMCEToolsPath(primaryPath);
        
        String[] isAliasConstraint = {
            "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",};
        //first mask the link(isAlias) of primaryPath
        Iterator<Statement> itS = primaryPath.iterator();
        while(itS.hasNext()){
            Statement stmt = itS.next();
            if(MCETools.evaluateStatement_AnyTrue(systemModel, stmt, isAliasConstraint)){
                //System.out.format("remove this stmt: \n%s\n", stmt.toString());
                systemModel = (OntModel) systemModel.remove(stmt);
            }
        }
        
        Resource nodeA = terminals.get(0);
        Resource nodeZ = terminals.get(1);

        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
        List<MCETools.Path> backupKSP = MCETools.computeKShortestPaths(systemModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);

        System.out.format("Find %d KSP (backup) before verify\n", backupKSP.size());
        
        if (backupKSP == null || backupKSP.isEmpty()) {
            //throw new EJBException(String.format("%s::process doPathFinding cannot find feasible path for <%s>", resLink));
            return null;
        }

        Iterator<MCETools.Path> itP = backupKSP.iterator();
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();

            if (!MCETools.verifyOpenFlowPath(systemModel, candidatePath)) {
                itP.remove();
            } else {
                //generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                OntModel l2PathModel = MCETools.createL2PathVlanSubnets(systemModel, candidatePath);
                if (l2PathModel == null) {
                    itP.remove();
                } else {
                    candidatePath.setOntModel(l2PathModel);
                }
            }
        }
        if (backupKSP.isEmpty()) {
            return null;
        } else{
            System.out.format("Find %d KSP (backup) after verify\n", backupKSP.size());
        }

        //pick one in backupKSP that has minimum srrg probability
        MCETools.Path backPath = this.getLeastSrrgCostPath(backupKSP, systemModel, srrgMap);

        System.out.println("\backup path is:");
        MCETools.printMCEToolsPath(backPath);
        
        return backPath;

    }

    private double getPathProbability(OntModel systemModel, MCETools.Path path, Map<Resource, List> srrgMap) {
        
        //System.out.println("entering get path probability");
        double pathSuccessProb = 1.0;
        
        for (Resource resSRRG : srrgMap.keySet()) {
            List srrgList = srrgMap.get(resSRRG);

            int findInThisSrrg = 0;

            for (int i = 0; i < srrgList.size(); i++) {

                if (findInThisSrrg == 1) {
                    break;
                }
                
                Map srrgData = (Map) srrgList.get(i);
                double probability = Double.parseDouble(srrgData.get("probability").toString());

                Resource resPort = systemModel.getResource(srrgData.get("port").toString());
                if (resPort == null) {
                    throw new EJBException(String.format("%s::process getPathProbability cannot identify port <%s>", (String) srrgData.get("port")));
                }

                Resource resNode = systemModel.getResource(srrgData.get("node").toString());
                if (resNode == null) {
                    throw new EJBException(String.format("%s::process getPathProbability cannot identify port <%s>", (String) srrgData.get("node")));
                }

                for (Statement stmt : path) {
                    if (stmt.getSubject().equals(resNode) || stmt.getSubject().equals(resPort)) {
                        pathSuccessProb = pathSuccessProb * (1 - probability);
                        findInThisSrrg = 1;
                        break;
                    }
                }
            }
        }

        return pathSuccessProb;
    }

    //same from MCE_MPVlanConnection.java
    private MCETools.Path doPathFinding(OntModel systemModel, OntModel spaModel, Resource resLink, List<Map> connTerminalData) {
        // transform network graph
        // filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2NetworkModel(systemModel);
        try {
            log.log(Level.FINE, "\n>>>MCE_MPVlanConnection--SystemModel=\n" + ModelUtil.marshalModel(transformedModel));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        // get source and destination nodes (nodeA, nodeZ) -- only picks fist two terminals for now 
        List<Resource> terminals = new ArrayList<>();
        /*
        for (Map entry : connTerminalData) {
            if (!entry.containsKey("data") || !entry.containsKey("type") || !entry.containsKey("value")) {
                continue;
            }
            Resource terminal = systemModel.getResource((String) entry.get("value"));
            if (terminal == null) {
                throw new EJBException(String.format("%s::process doPathFinding cannot identify terminal <%s>", (String) entry.get("value")));
            }
            terminals.add(terminal);
        }
        if (terminals.size() < 2) {
            throw new EJBException(String.format("%s::process cannot doPathFinding for %s which provides fewer than 2 terminals", this.getClass().getName(), resLink));
        }
                */
        String src = "urn:ogf:network:onos.maxgigapop.net:network:of:0000000000000001:port-s1-eth1";
        String dst = "urn:ogf:network:onos.maxgigapop.net:network:of:0000000000000003:port-s3-eth1";

        Resource nodeA = transformedModel.getResource(src);
        Resource nodeZ = transformedModel.getResource(dst);
        
        //Resource nodeA = terminals.get(0);
        //Resource nodeZ = terminals.get(1);
        // KSP-MP path computation on the connected graph model (point2point for now - will do MP in future)
        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);

        List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);
        System.out.format("Found %d KSP found\n", KSP.size());

        if (KSP == null || KSP.isEmpty()) {
            throw new EJBException(String.format("%s::process doPathFinding cannot find feasible path for <%s>", MCE_MPVlanConnection.class.getName(), resLink));
        }
        // Verify TE constraints (switching label and ?adaptation?), 
        Iterator<MCETools.Path> itP = KSP.iterator();
        
        int flag = 0;
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();
            
            System.out.format("%d path: \n", flag);
            for (Statement stmtLink: candidatePath){
                System.out.println(stmtLink.toString());
            }
            flag = flag +1;
            
            // verify path
            if (!MCETools.verifyL2Path(transformedModel, candidatePath)) {
                System.out.println("Remove this one\n");
                itP.remove();
            } else {
                // generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                OntModel l2PathModel = MCETools.createL2PathVlanSubnets(transformedModel, candidatePath);
                if (l2PathModel == null) {
                    System.out.println("Remove here\n");
                    itP.remove();
                } else {
                    candidatePath.setOntModel(l2PathModel);
                }
            }
        }
        if (KSP.size() == 0) {
            System.out.println("Could not find any shortest path\n");
            return null;
        }

        // pick the shortest path from remaining/feasible paths in KSP
        return MCETools.getLeastCostPath(KSP);
    }

    private void exportPolicyData(OntModel spaModel, Resource resLink, MCETools.Path l2Path) {
        // find Connection policy -> exportTo -> policyData
        String sparql = "SELECT ?policyAction ?policyData WHERE {"
                + String.format("<%s> spa:dependOn ?policyAction .", resLink.getURI())
                + "?policyAction a spa:PolicyAction. "
                + "?policyAction spa:type 'MCE_MPVlanConnection'. "
                + "?policyAction spa:exportTo ?policyData . "
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resPolicy = querySolution.get("policyAction").asResource();
            Resource resData = querySolution.get("policyData").asResource();
            // add to export data with references to (terminal (src/dst) vlan labels from l2Path
            List<QuerySolution> terminalVlanSolutions = getTerminalVlanLabels(l2Path);
            // require two terminal vlan ports and labels.
            if (solutions.isEmpty()) {
                throw new EJBException("exportPolicyData failed to find '2' terminal Vlan tags for " + l2Path);
            }
            spaModel.add(resData, Spa.type, "MPVlanConnection:VlanPorts");
            for (QuerySolution aSolution : terminalVlanSolutions) {
                Resource bidrPort = aSolution.getResource("bp");
                Resource vlanTag = aSolution.getResource("vlan");
                spaModel.add(resData, Spa.value, bidrPort);
            }
            // remove Connection->exportTo statement so the exportData can be kept in spaModel during receurive removal
            //spaModel.remove(resPolicy, Spa.exportTo, resData);
        }
    }

    private List<QuerySolution> getTerminalVlanLabels(MCETools.Path l2path) {
        Resource resSrc = l2path.get(0).getSubject();
        Resource resDst = l2path.get(l2path.size() - 1).getObject().asResource();
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

}
