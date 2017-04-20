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
import java.util.logging.Logger;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;
import net.maxgigapop.mrs.common.Sna;
import static net.maxgigapop.mrs.service.compute.MCETools.evaluateStatement_AnyTrue;
import java.lang.Math;
import java.util.logging.Level;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.onosystem.*;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author hbai
 */
@Stateless
public class MCE_L2FastRerouting implements IModelComputationElement {
    public int link_count=0;
    String[] bandwidth = new String[1000];

    private static final StackLogger logger = new StackLogger(MCE_L2FastRerouting.class.getName(), "MCE_L2FastRerouting");
    /*
     ** Simple L2 connection will create new SwitchingSubnet on every transit switching node.
     */

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        String method = "process";
        logger.refuuid(annotatedDelta.getReferenceUUID());
        try {
            logger.trace(method, "\n>>>MCE_L2FastRerouting--DeltaAddModel Input=\n{0}", ModelUtil.marshalModel(annotatedDelta.getModelAddition().getOntModel().getBaseModel()));
            logger.trace(method, "Entering L2FastRerouting process!");
        } catch (Exception ex) {
            logger.trace(method, "ModelUtil.marshalModel(annotatedDelta.getModelAddition().getOntModel().getBaseModel()) failed -- "+ex);
        }
        // importPolicyData : Link->Connection->List<PolicyData> of terminal Node/Topology
        String sparql = "SELECT ?link ?type ?value ?load ?data ?policyData ?spaType ?spaValue WHERE {"
                + "?link a spa:PolicyAction . "
                + "?link spa:type ?type . "
                //+ "?link spa:value ?value . "
                + "?link spa:load ?load . "
                + "?link spa:importFrom ?data . "
                + "?link spa:exportTo ?policyData . "
                + "?data spa:type ?spaType . ?data spa:value ?spaValue . "
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);

	String load="";
        Map<Resource, List> linkMap = new HashMap<>();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resLink = querySolution.get("link").asResource();
            if (!linkMap.containsKey(resLink)) {
                List linkList = new ArrayList<>();
                linkMap.put(resLink, linkList);
            }
            Resource resData = querySolution.get("data").asResource();  //"x-policy-annotation:data:left-location"
            Resource resPolicyData = querySolution.getResource("policyData").asResource();
            RDFNode resType = querySolution.get("type");    //"MCE_L2FastRerouting"

            //no need this value
            //RDFNode resValue = querySolution.get("value");
            RDFNode resLoad = querySolution.get("load");
            RDFNode resDataType = querySolution.get("spaType");     //"port"
            RDFNode resDataValue = querySolution.get("spaValue");   //"urn:ogf:network:onos.maxgigapop.net:network1:of:0000000000000003:port-s3-eth1"

            System.out.format("data: %s, type: %s, policyData: %s, spaType: %s, spaValue: %s, load: %s\n",
                    resData.toString(), resType.toString(), resPolicyData.toString(), resDataType.toString(), resDataValue.toString(), resLoad.toString());

            if (resType.toString().equals("MCE_L2FastRerouting")) {
                Map linkData = new HashMap<>();
                //linkData.put("port", resPort);
                linkData.put("dataValue", resDataValue.toString());//"urn:ogf:network:onos.maxgigapop.net:network1:of:0000000000000003:port-s3-eth1"
                linkData.put("dataType", resDataType.toString());//"port"
                linkData.put("policyData", resPolicyData);//"x-policy-annotation:data:path1-vlan"
                linkData.put("type", resType.toString());//"MCE_L2FastRerouting"
                //linkData.put("protection", resValue.toString());
                linkData.put("load", resLoad.toString());
                linkMap.get(resLink).add(linkData);
		load=resLoad.toString();
            }
        }

        logger.trace(method, String.format("There are %d link request in the spaModel", String.valueOf(linkMap.size())));
        ServiceDelta outputDelta = annotatedDelta.clone();
        // compute a List<Model> of L2Openflow connections
        for (Resource resLink : linkMap.keySet()) {

            List<Map> connTerminalData = linkMap.get(resLink);
            //String protectionType = ((Map) connTerminalData.get(0)).get("protection").toString();

            MCETools.Path l2path = new MCETools.Path();
            //MCETools.Path l2path_back = new MCETools.Path();
            String flow_model = "";
            //String flow_model_back = "";

            String[] macAddress = new String[2];
            macAddress = this.getMacAddress(systemModel.getOntModel(), resLink, connTerminalData);

            String ETH_SRC_MAC = macAddress[0];
            String ETH_DST_MAC = macAddress[1];
            System.out.format("ETH_SRC_MAC: %s\n", ETH_SRC_MAC);
            System.out.format("ETH_DST_MAC: %s\n", ETH_DST_MAC);

            
            //just find a feasible path, no srrg map anymore
            l2path = this.doReroutingPathFinding(systemModel.getOntModel(), 
                    annotatedDelta.getModelAddition().getOntModel(), resLink, linkMap.get(resLink), load);
	    updateLinkReservation(l2path,load);
            try {
                flow_model = generateFlowsPathModel_new(l2path, ETH_SRC_MAC, ETH_DST_MAC);

            } catch (IOException ex) {
                throw logger.error_throwing(method, "generateFlowsPathModel_new failed -- " + ex);
            }
            OntModel modelAddition;
            try {
                modelAddition = ModelUtil.unmarshalOntModel(flow_model);
            } catch (Exception ex) {
                throw logger.error_throwing(method, "unmarshalOntModel(flow_model) failed -- " + ex);
            }
            outputDelta.getModelAddition().getOntModel().add(modelAddition.getBaseModel());
        }

        try {
            logger.trace(method, "\n>>>MCE_L2FastRerouting--DeltaAddModel Output=\n{0}", ModelUtil.marshalModel(outputDelta.getModelAddition().getOntModel().getBaseModel()));
        } catch (Exception ex) {
            logger.trace(method, "ModelUtil.marshalModel(outputDelta.getModelAddition().getOntModel().getBaseModel()) failed -- " + ex);
        }

        return new AsyncResult(outputDelta);
    }

    private String[] getMacAddress(OntModel systemModel, Resource resLink, List<Map> connTerminalData) {

        String macAddress[] = new String[2];

        //first get the terminal data, as in doSrrgPathFinding
        OntModel transformedModel = MCETools.transformL2OpenflowPathModel(systemModel);

        if (!verifyConnectsToModel(transformedModel)) {
            throw new EJBException(String.valueOf("transformedModel is not fully bidirectional\n"));
        }

        List<Resource> terminals = new ArrayList<>();
        Resource terminal = null;
        for (Map entry : connTerminalData) {
            if (!entry.containsKey("policyData") || !entry.containsKey("type")) {
                continue;
            }
            if (entry.get("dataType").toString().equals("port")) {
                terminal = systemModel.getResource(entry.get("dataValue").toString());
                if (terminal == null) {
                    throw new EJBException(String.format("%s::process doSrrgPathFinding cannot identify terminal <%s>", MCE_L2OpenflowPath.class.getName(), (String) entry.get("port")));
                }
                terminals.add(terminal);
            }
        }

        if (terminals.size() != 2) {
            throw new EJBException(String.format("%s::process cannot doSrrgPathFinding for %s which provides not 2 terminals", this.getClass().getName(), resLink));
        }

        Resource nodeA = terminals.get(0);
        Resource nodeZ = terminals.get(1);

        System.out.format("nodeA : %s\nnodeZ : %s\n", nodeA.toString(), nodeZ.toString());

        //after get nodeA and nodeZ, try to find locatedAt property in baseModel to find the src_mac and dest_mac
        String sparql = "SELECT ?hostId ?address ?location ?addressType ?addressValue WHERE{"
                + "?hostId a nml:Node. "
                + "?hostId mrs:hasNetworkAddress ?address. "
                + "?hostId nml:locatedAt ?location. "
                + "?address mrs:type ?addressType . ?address mrs:value ?addressValue . "
                + "}";

        ResultSet r = ModelUtil.sparqlQuery(systemModel, sparql);
//        Map<Resource, List> hostMap = new HashMap<>();
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource resHost = querySolution.get("hostId").asResource();
//            if(!hostMap.containsKey(resHost)){
//                List hostList = new ArrayList<>();
//                hostMap.put(resHost, hostList);
//            }

            Resource resAddress = querySolution.get("address").asResource();
            Resource resLocation = querySolution.get("location").asResource();

            RDFNode resType = querySolution.get("addressType");
            RDFNode resValue = querySolution.get("addressValue");

            if (resType.toString().equals("macAddresses")) {
                if (resLocation.equals(nodeA)) {
                    macAddress[0] = resValue.toString();
                }
            }

            if (resType.toString().equals("macAddresses")) {
                if (resLocation.equals(nodeZ)) {
                    macAddress[1] = resValue.toString();
                }
            }

        }

        return macAddress;
    }

    private MCETools.Path doReroutingPathFinding(OntModel systemModel, OntModel spaModel, 
            Resource resLink, List<Map> connTerminalData, String load) {
        String method = "doReroutingPathFinding";
        String linkname="";

        System.out.println("In doReroutingPathFinding");
        //return one shortest path 
        //filter out irrelevant statements (based on property type, label type, has switchingService etc.)

        try {
                URL url = new URL("http://206.196.179.134:8181/onos/v1" + "/network/configuration/links");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                bandwidth=this.executeHttpMethod(url, conn, "GET", null, "karaf", "karaf");

        } catch (IOException ex) {
                throw logger.throwing(method, ex);
        }

        String [] json_links=new String[link_count];
        for(int i=0;i<link_count;i++){
                json_links[i] = bandwidth[i];
        }


        OntModel transformedModel = MCETools.transformL2OpenflowPathModel(systemModel);

        //verify is transformedModel is bidirectional connectsTo
        if (!verifyConnectsToModel(transformedModel)) {
            throw new EJBException(String.valueOf("transformedModel is not fully bidirectional\n"));
        }

        //get source and dest nodes (nodeA, nodeZ), initially Link points to 2 biDirectionalPort
        List<Resource> terminals = new ArrayList<>();
        Resource terminal = null;
        for (Map entry : connTerminalData) {
            if (!entry.containsKey("policyData") || !entry.containsKey("type") ) {
                continue;
            }
            if (entry.get("dataType").toString().equals("port")) {
                terminal = systemModel.getResource(entry.get("dataValue").toString());
                if (terminal == null) {
                    throw new EJBException(String.format("%s::process doReroutingPathFinding cannot identify terminal <%s>", MCE_L2FastRerouting.class.getName(), (String) entry.get("port")));
                }
                terminals.add(terminal);
            }
        }

        if (terminals.size() != 2) {
            throw new EJBException(String.format("%s::process cannot doReroutingPathFinding for %s which provides not 2 terminals", this.getClass().getName(), resLink));
        }

        Resource nodeA = terminals.get(0);
        Resource nodeZ = terminals.get(1);

        logger.trace(method, "Link-src: " + nodeA.toString());
        logger.trace(method, "Link-dst: " + nodeZ.toString());

        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
        List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);

        logger.trace(method, String.format("Found %d shortest path before verify", KSP.size()));

        int counting;
        int bwreq=Integer.parseInt(load);
        int bwfree=0;
        int[] unsetPath = new int[MCETools.KSP_K_DEFAULT];
        for(counting=0;counting<MCETools.KSP_K_DEFAULT;counting++){
                unsetPath[counting]=0;
        }
        counting=0;
        for (MCETools.Path eachPath : KSP) {
            String[] srcdstlink = new String[2];
            srcdstlink[0]="";
            srcdstlink[1]="";

            for (Statement stmtLink : eachPath) {
                if(stmtLink.toString().contains("connectsTo")){
                        srcdstlink[0] = stmtLink.toString().split(":of:")[1].split(",")[0];
                        srcdstlink[1] = stmtLink.toString().split(":of:")[2].split("}")[0];
                        if(srcdstlink[0].contains("port-") && srcdstlink[1].contains("port-")){
                                srcdstlink[0]="of:"+srcdstlink[0].replaceAll(":port-","/");
                                srcdstlink[1]="of:"+srcdstlink[1].replaceAll(":port-","/");
                                linkname=srcdstlink[0]+"-"+srcdstlink[1];
                                linkname=linkname.split("]")[0];
                                for(int k=0;k<link_count;k++){
                                        if(json_links[k].contains(linkname)){
                                                if(json_links[k].contains("bandwidth")){
                                                        String stringfreeload=json_links[k].split("bandwidth\":")[1].split("}}")[0];
                                                        bwfree=Integer.parseInt(stringfreeload);
                                                        if(bwreq>bwfree){
                                                                unsetPath[counting]=1;
								break;
                                                        }
                                                }
                                        }
                                }
                        }
                }

            }
            counting = counting + 1;
        }




        //MCETools.printKSP(KSP);
        if (KSP == null || KSP.isEmpty()) {
            throw new EJBException(String.format("%s::process doReroutingPathFinding cannot find any feasible path for <%s>", this.getClass().getName(), resLink));
        }

        Iterator<MCETools.Path> itP = KSP.iterator();
	counting=0;
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();

            //verifyOpenflowPath: filter out useless ones
            boolean verified;
            try {
                verified = MCETools.verifyOpenFlowPath(transformedModel, candidatePath);
            } catch (Exception ex) {
                throw logger.error_throwing(method, "verifyOpenFlowPath(transformedModel, candidatePath) exception -- "+ex);
            }
            if (!verified) {
                itP.remove();
            } else {
                //generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                OntModel l2PathModel = MCETools.createL2PathVlanSubnets(transformedModel, candidatePath, null);
                if (l2PathModel == null) {
                    itP.remove();
                } else {
                        if(unsetPath[counting]==1) {
                        itP.remove();
                        }
                    candidatePath.setOntModel(l2PathModel);
                }
            }
	    counting++;
        }

        if (KSP.isEmpty()) {
            logger.trace(method, "Could not find any shortest path after verify");
            throw new EJBException(String.format("%s::process doReroutingPathFinding cannot find any feasible path for <%s>", this.getClass().getName(), resLink));
        } else {
            logger.trace(method, String.format("Found %d KSP after verify", KSP.size()));
        }

        //return the shortest path
        MCETools.Path solution = MCETools.getLeastCostPath(KSP);
        MCETools.printMCEToolsPath(solution);
        return solution;

    }

    private boolean verifyConnectsToModel(OntModel transformedModel) {

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
        int i = 0, j = 0;
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

   
    String generateFlowsPathModel_new(MCETools.Path l2path, String ETH_SRC_MAC, String ETH_DST_MAC)
            throws MalformedURLException, IOException {

        String[] device = new String[l2path.size() / 3 + 1];
        String[] inport = new String[l2path.size() / 3 + 1];
        String[] outport = new String[l2path.size() / 3 + 1];
        //flows only will go through l2path.size()/3 +1 devices, inport, outport, ...

        int j = 0;

        for (int i = 0; i < l2path.size(); i = i + 3) {
            Statement stmt = l2path.get(i);
            Resource resInport = stmt.getSubject();
            Resource resDevice = stmt.getObject().asResource();

            if ((i + 1) >= l2path.size()) {
                throw new EJBException(String.format("%s::process: something wrong with number of statement", this.getClass().getName()));
            }
            Statement nextStmt = l2path.get(i + 1);
            if (!resDevice.equals(nextStmt.getSubject())) {
                throw new EJBException(String.format("%s::process: something wrong with link statement consistensy", this.getClass().getName()));
            }
            Resource resOutport = nextStmt.getObject().asResource();

            device[j] = resDevice.toString();
            inport[j] = resInport.toString().split("port-")[1];
            //inport[j] = resInport.toString().split("-eth")[1];
            outport[j] = resOutport.toString().split("port-")[1];
            //outport[j] = resOutport.toString().split("-eth")[1];

            j++;
        }

        String flowModelPrefix = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> ."
                + "@prefix owl:   <http://www.w3.org/2002/07/owl#> ."
                + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> ."
                + "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ."
                + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> ."
                + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n";

        String flowModelString = "";

        flowModelString += flowModelPrefix;

        for (j = 0; j < (l2path.size() / 3 + 1); j++) {

            int rn = (int) (Math.random() * 1000000000);
            int rnreverse = (int) (Math.random() * 1000000000);
            String flowIdn = Integer.toString(rn);
            String flowIdnreverse = Integer.toString(rnreverse);

            //System.out.format("Device: %s, inport: %s, outport: %s\n", device[j], inport[j], outport[j]);
            String flowTable = String.format("%s:openflow-service:flow-table-0", device[j]);
            String flowID = String.format("%s:flow-%s", flowTable, flowIdn);
            String flowIDr = String.format("%s:flow-%s", flowTable, flowIdnreverse);
            String flowMatch0 = String.format("%s:rule-match-0", flowID);
            String flowMatch1 = String.format("%s:rule-match-1", flowID);
            String flowMatch2 = String.format("%s:rule-match-2", flowID);
            String flowAction = String.format("%s:rule-action-0", flowID);
            String flowMatch0r = String.format("%s:rule-match-0", flowIDr);
            String flowMatch1r = String.format("%s:rule-match-1", flowIDr);
            String flowMatch2r = String.format("%s:rule-match-2", flowIDr);
            String flowActionr = String.format("%s:rule-action-0", flowIDr);

            flowModelString
                    += "<" + flowTable + ">\n"
                    //+ "       mrs:providesFlow <" + flowID + "> .\n"
                    + "       mrs:providesFlow <" + flowID + "> ;\n"
                    + "       mrs:providesFlow <" + flowIDr + "> .\n"
                    + "\n"
                    + "<" + flowID + ">\n"
                    + "       a                        mrs:Flow , owl:NamedIndividual ;\n"
                    + "                mrs:flowMatch <" + flowMatch0 + "> ;\n"
                    + "                mrs:flowMatch <" + flowMatch1 + "> ;\n"
                    + "                mrs:flowMatch <" + flowMatch2 + "> ;\n"
                    + "                mrs:flowAction <" + flowAction + "> .\n"
                    + "\n"
                    + "<" + flowIDr + ">\n"
                    + "       a                        mrs:Flow , owl:NamedIndividual ;\n"
                    + "                mrs:flowMatch <" + flowMatch0r + "> ;\n"
                    + "                mrs:flowMatch <" + flowMatch1r + "> ;\n"
                    + "                mrs:flowMatch <" + flowMatch2r + "> ;\n"
                    + "                mrs:flowAction <" + flowActionr + "> .\n"
                    + "\n"
                    + "<" + flowMatch0 + ">\n"
                    + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                    + "                mrs:type \"IN_PORT\" ;\n"
                    + "                mrs:value \"" + inport[j] + "\" .\n"
                    + "\n"
                    + "<" + flowMatch0r + ">\n"
                    + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                    + "                mrs:type \"IN_PORT\" ;\n"
                    + "                mrs:value \"" + outport[j] + "\" .\n"
                    + "\n"
                    + "<" + flowAction + ">\n"
                    + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                    + "                mrs:type \"OUT_PORT\" ;\n"
                    + "                mrs:value \"" + outport[j] + "\" .\n"
                    + "\n"
                    + "<" + flowActionr + ">\n"
                    + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                    + "                mrs:type \"OUT_PORT\" ;\n"
                    + "                mrs:value \"" + inport[j] + "\" .\n"
                    + "\n"
                    + "<" + flowMatch1 + ">\n"
                    + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                    + "                mrs:type \"ETH_SRC_MAC\" ;\n"
                    + "                mrs:value \"" + ETH_SRC_MAC + "\" .\n"
                    + "\n"
                    + "<" + flowMatch1r + ">\n"
                    + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                    + "                mrs:type \"ETH_SRC_MAC\" ;\n"
                    + "                mrs:value \"" + ETH_DST_MAC + "\" .\n"
                    + "\n"
                    + "<" + flowMatch2 + ">\n"
                    + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                    + "                mrs:type \"ETH_DST_MAC\" ;\n"
                    + "                mrs:value \"" + ETH_DST_MAC + "\" .\n"
                    + "\n"
                    + "<" + flowMatch2r + ">\n"
                    + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                    + "                mrs:type \"ETH_DST_MAC\" ;\n"
                    + "                mrs:value \"" + ETH_SRC_MAC + "\" .\n"
                    + "\n";
        }
        return flowModelString;
    }


    private String[] executeHttpMethod(URL url, HttpURLConnection conn, String method, String body, String access_key_id, String secret_access_key) throws IOException {
        String username = access_key_id;
        String password = secret_access_key;
        String userPassword = username + ":" + password;
        byte[] encoded = Base64.encodeBase64(userPassword.getBytes());
        String stringEncoded = new String(encoded);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Basic " + stringEncoded);
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept-coding", "gzip");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
                conn.setDoOutput(true);
                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                        wr.writeBytes(body);
                        wr.flush();
                }
        }
        int responseCode = conn.getResponseCode();
        StringBuilder responseStr;
        String findStr = "}}";
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                link_count=0;
                responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                        responseStr.append(inputLine);
                }
                int firstIndex,lastIndex = 0;

                while(lastIndex != -1){
                  firstIndex = lastIndex;
                  lastIndex = responseStr.indexOf(findStr,lastIndex);
                  if(lastIndex != -1){
                         link_count ++;
                           lastIndex += findStr.length();
                  }
                }
         }
        bandwidth = new String[link_count];
        bandwidth=responseStr.toString().split(findStr);

        return(bandwidth);

    }

    private void updateLinkReservation(MCETools.Path l2path, String load) {

        int bwreq=Integer.parseInt(load);
        int newvalue=0;
        String jsonUpdate="{\"links\":{";
        //for (MCETools.Path eachPath : l2path) {
            String[] srcdstlink = new String[2];
            String linkname="";
            srcdstlink[0]="";
            srcdstlink[1]="";
            //for (Statement stmtLink : eachPath) {
            for (Statement stmtLink : l2path) {
                if(stmtLink.toString().contains("connectsTo")){
                        srcdstlink[0] = stmtLink.toString().split(":of:")[1].split(",")[0];
                        srcdstlink[1] = stmtLink.toString().split(":of:")[2].split("}")[0];
                        if(srcdstlink[0].contains("port-") && srcdstlink[1].contains("port-")){

                                srcdstlink[0]="of:"+srcdstlink[0].replaceAll(":port-","/");
                                srcdstlink[1]="of:"+srcdstlink[1].replaceAll(":port-","/");
                                linkname=srcdstlink[0]+"-"+srcdstlink[1];
                                linkname=linkname.split("]")[0];


                                for(int k=0;k<link_count;k++){
                                        if(bandwidth[k].contains(linkname)){
                                                if(bandwidth[k].contains("bandwidth")){
                                                        String stringfreeload=bandwidth[k].split("bandwidth\":")[1];
                                                        int bwfree=Integer.parseInt(stringfreeload);
                                                        newvalue=bwfree-bwreq;
                                                }
                                        }
                                }

                                jsonUpdate=jsonUpdate+"\""+linkname+"\":{\"basic\":{\"bandwidth\":"+newvalue+"}},";
                        }
                }

            }
        //}
        jsonUpdate=jsonUpdate+"}}";
        jsonUpdate=jsonUpdate.replaceAll("},}}","}}}");
        try{
                URL url = new URL("http://206.196.179.134:8181/onos/v1" + "/network/configuration/");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String response=this.postJsonLinks(url, conn, "POST", jsonUpdate, "karaf", "karaf");
        } catch (IOException ex) {
                        Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private String postJsonLinks(URL url, HttpURLConnection conn, String method, String body, String access_key_id, String secret_access_key) throws IOException {
        String username = access_key_id;
        String password = secret_access_key;
        String userPassword = username + ":" + password;
        byte[] encoded = Base64.encodeBase64(userPassword.getBytes());
        String stringEncoded = new String(encoded);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Basic " + stringEncoded);
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept-coding", "gzip");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
                conn.setDoOutput(true);
                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                        wr.writeBytes(body);
                        wr.flush();
                }
        }
        int responseCode = conn.getResponseCode();
        StringBuilder responseStr;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }
        return(responseStr.toString());
    }


}
