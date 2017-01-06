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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DeltaBase;
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
public class MCE_L2OpenflowPath implements IModelComputationElement {
    public int link_count=0;
    String[] bandwidth = new String[1000];

    private static final Logger log = Logger.getLogger(MCE_L2OpenflowPath.class.getName());
    /*
     ** Simple L2 connection will create new SwitchingSubnet on every transit switching node.
     */

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {

/*	try{
		URL url = new URL("http://206.196.179.134:8181/onos/v1" + "/network/configuration/links");

        	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	String response=this.postJsonLinks(url, conn, "GET", null, "karaf", "karaf");
	} catch (IOException ex) {
                        Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
	}
*/

        try {
            log.log(Level.INFO, "\n>>>MCE_L2OpenflowPath--DeltaAddModel Input=\n{0}", ModelUtil.marshalModel(annotatedDelta.getModelAddition().getOntModel().getBaseModel()));
            log.log(Level.INFO, "Entering L2OpenflowPath process!");
        } catch (Exception ex) {
            Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
        }

        // importPolicyData : Link->Connection->List<PolicyData> of terminal Node/Topology
                
        String sparql = "SELECT ?link ?type ?value ?load ?data ?policyData ?spaType ?spaValue WHERE {"
                + "?link a spa:PolicyAction . "
                + "?link spa:type ?type . "
                + "?link spa:value ?value . "
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
            Resource resData = querySolution.get("data").asResource();
            Resource resPolicyData = querySolution.getResource("policyData").asResource();
            RDFNode resType = querySolution.get("type");
            RDFNode resValue = querySolution.get("value");
            RDFNode resLoad = querySolution.get("load");
            
            RDFNode resDataType = querySolution.get("spaType");
            RDFNode resDataValue = querySolution.get("spaValue");
            
            //System.out.format("data: %s, type: %s, protection: %s, policyData: %s, spaType: %s, spaValue: %s\n", 
            System.out.format("data: %s, type: %s, protection: %s, policyData: %s, spaType: %s, spaValue: %s, load: %s\n", 
                    resData.toString(), resType.toString(), resValue.toString(), resPolicyData.toString(), resDataType.toString(), resDataValue.toString(), resLoad.toString());

            if (resType.toString().equals("MCE_L2OpenflowPath")) {
                Map linkData = new HashMap<>();                
                //linkData.put("port", resPort);
                linkData.put("dataValue", resDataValue.toString());
                linkData.put("dataType", resDataType.toString());
                linkData.put("policyData", resPolicyData);
                linkData.put("type", resType.toString());
                linkData.put("protection", resValue.toString());
                linkData.put("load", resLoad.toString());
                linkMap.get(resLink).add(linkData);
		load=resLoad.toString();
            }
        }
        
        Map<Resource, List> srrgMap = this.getSrrgInfo(systemModel.getOntModel());

        //test printout
        log.log(Level.INFO, "There are {0} SRRG in the model", String.valueOf(srrgMap.size()));
        log.log(Level.INFO, "There are {0} link request in the spaModel", String.valueOf(linkMap.size()));
        ServiceDelta outputDelta = annotatedDelta.clone();
        // compute a List<Model> of L2Openflow connections
        for (Resource resLink : linkMap.keySet()) {

            List<Map> connTerminalData = linkMap.get(resLink);
            String protectionType = ((Map) connTerminalData.get(0)).get("protection").toString();

            MCETools.Path l2path = new MCETools.Path();
            MCETools.Path l2path_back = new MCETools.Path();
            String flow_model = "";
            String flow_model_back = "";
            
            String[] macAddress = new String[2];
/*            for (Map entry : connTerminalData){
                if (!entry.containsKey("policyData") || !entry.containsKey("type") || !entry.containsKey("protection")){
                    continue;
                }
                if(entry.get("dataType").toString().equals("ETH_SRC_MAC")){
                    ETH_SRC_MAC = entry.get("dataValue").toString();
                }
                if(entry.get("dataType").toString().equals("ETH_DST_MAC")){
                    ETH_DST_MAC = entry.get("dataValue").toString();
                }
            }
*/            
            macAddress = this.getMacAddress(systemModel.getOntModel(), resLink, connTerminalData);
            
            String ETH_SRC_MAC = macAddress[0];
            String ETH_DST_MAC = macAddress[1];
            System.out.format("ETH_SRC_MAC: %s\n", ETH_SRC_MAC);
            System.out.format("ETH_DST_MAC: %s\n", ETH_DST_MAC);
            //TO-DO: modify SRRG, no port in Map anymore, replaced by dataValue and specified by dataType
            /****************************************************************/
            
            switch (protectionType) {
                case "SRRG Path": {
                            
                            l2path = this.doSrrgPathFinding(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), resLink, linkMap.get(resLink), srrgMap, load);
                    try {
                        //flow_model = generateFlowsPathModel(l2path, topologyURI, ETH_SRC_MAC, ETH_DST_MAC);
                        flow_model = generateFlowsPathModel_new(l2path, ETH_SRC_MAC, ETH_DST_MAC);
                        
                    } catch (IOException ex) {
                        Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    try {
                        OntModel modelAddition = ModelUtil.unmarshalOntModel(flow_model);
                        outputDelta.getModelAddition().getOntModel().add(modelAddition.getBaseModel());
                        
                    } catch (Exception ex){
                        Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                }
                case "SRRG Path Pair": {
                    System.out.println("In SRRG Path Pair Computation, testing...");
                    List<MCETools.Path> l2pathList = this.doSrrgPairPathFinding(
                            systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(),
                            resLink, linkMap.get(resLink), srrgMap, load);
                    if (l2pathList == null) {
                        throw new EJBException(String.format("%s::process cannot find a path pair for %s", this.getClass().getName(), resLink));
                    } else if (l2pathList.size() != 2) {
                        throw new EJBException(String.format("%s::process encounter an error when finding a path pair for %s", this.getClass().getName(), resLink));
                    } else {
                        l2path = l2pathList.get(0);
                        l2path_back = l2pathList.get(1);
			updateLinkReservation(l2pathList,load);
                        try {
                            //flow_model = generateFlowsPathModel(l2path, topologyURI, ETH_SRC_MAC, ETH_DST_MAC);
                            flow_model = generateFlowsPathModel_new(l2path, ETH_SRC_MAC, ETH_DST_MAC);
                            //TO-DO:
                            //PUT l2path_back into another flow_model_back, then put it into modelReduction
                            //flow_model = generateFlowsPathPairModel(l2path, l2path_back, topologyURI);
                            flow_model_back = generateFlowsPathModel_new(l2path_back, ETH_SRC_MAC, ETH_DST_MAC);

                        } catch (IOException ex) {
                            Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        try {
                            OntModel modelAddition = ModelUtil.unmarshalOntModel(flow_model);
                            OntModel modelReduction = ModelUtil.unmarshalOntModel(flow_model_back);
                            modelReduction.add(outputDelta.getModelAddition().getOntModel().getBaseModel());
                            outputDelta.getModelAddition().getOntModel().add(modelAddition.getBaseModel());
                            DeltaModel dmReduction = new DeltaModel();
                            dmReduction.setCommitted(false);
                            dmReduction.setIsAddition(false);
                            dmReduction.setOntModel(modelReduction);
                            outputDelta.setModelReduction(dmReduction);
                        } catch (Exception ex){
                            Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        break;
                    }
                }
                default:
                    throw new EJBException(String.format("%s::process does not support protection type %s", this.getClass().getName(), protectionType));
            }
            
            //System.out.format("flow_Model is: \n%s\n", flow_model);
            //System.out.format("flow_Model back is: \n%s\n", flow_model_back);
            
            //this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), policyAction, connId, l2pathMap.get(connId));           
            //System.out.format("\nl2path_model: \n%s", l2path.getOntModel().getBaseModel().toString());
/*
            try {
                OntModel modelAddition = ModelUtil.unmarshalOntModel(flow_model);
                //outputDelta.getModelAddition().setOntModel(modelAddition);
                outputDelta.getModelAddition().getOntModel().add(modelAddition.getBaseModel());             
            } catch (Exception ex) {
                Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
            }
*/           
            //COMMENT THIS ONE: 12-29
            //outputDelta.getModelAddition().getOntModel().remove(l2path.getOntModel().getBaseModel());            
            //3. update policyData this action exportTo 
            //this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), resLink, l2path);
            //4. remove policy and all related SPA statements receursively under link from spaModel
            //   and also remove all statements that say dependOn this 'policy'
            //MCETools.removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), resLink);
            //5. mark the Link as an Abstraction
            //outputDelta.getModelAddition().getOntModel().add(outputDelta.getModelAddition().getOntModel().createStatement(resLink, RdfOwl.type, Spa.Abstraction));
        }

        //try {
        //    log.log(Level.FINE, "\n>>>MCE_L2OpenflowPath--DeltaAddModel Output=\n{0}", ModelUtil.marshalModel(outputDelta.getModelAddition().getOntModel().getBaseModel()));
        //} catch (Exception ex) {
        //    Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
        //}
        
//        System.out.println("\n\nDone with MCE");
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
    
    private String[] getMacAddress (OntModel systemModel, Resource resLink, List<Map> connTerminalData ){
        
        String macAddress[] = new String[2];
        
        //first get the terminal data, as in doSrrgPathFinding
        OntModel transformedModel = MCETools.transformL2OpenflowPathModel(systemModel);
        
        if (!verifyConnectsToModel(transformedModel)){
            throw new EJBException(String.valueOf("transformedModel is not fully bidirectional\n"));
        }
        
        List<Resource> terminals = new ArrayList<>();
        Resource terminal = null;
        for (Map entry : connTerminalData) {
            if (!entry.containsKey("policyData") || !entry.containsKey("type") || !entry.containsKey("protection")) {
                continue;
            }
            if(entry.get("dataType").toString().equals("port")){
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
        while (r.hasNext()){
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
            
            if (resType.toString().equals("macAddresses")){
                if (resLocation.equals(nodeA)){
                    macAddress[0] = resValue.toString();
                }
            }
            
            if (resType.toString().equals("macAddresses")){
                if (resLocation.equals(nodeZ)){
                    macAddress[1] = resValue.toString();
                }
            }
            
        }
     
        return macAddress;
    }

    private MCETools.Path doSrrgPathFinding(OntModel systemModel, OntModel spaModel, Resource resLink, List<Map> connTerminalData, Map<Resource, List> srrgMap, String load) {
	String linkname="";

        System.out.println("In doSrrgPathFinding");
        //String[] bandwidth = new String[1000];

	try {
		URL url = new URL("http://206.196.179.134:8181/onos/v1" + "/network/configuration/links");

        	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		bandwidth=this.executeHttpMethod(url, conn, "GET", null, "karaf", "karaf");

	} catch (IOException ex) {
                        Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
	}
        //return one shortest path with minimum SRRG probability
        //filter out irrelevant statements (based on property type, label type, has switchingService etc.)

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

            if (!entry.containsKey("policyData") || !entry.containsKey("type") || !entry.containsKey("protection")) {
                continue;
            }
            if(entry.get("dataType").toString().equals("port")){
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

        log.log(Level.INFO, "Link-src: {0}", nodeA.toString());
        log.log(Level.INFO, "Link-dst: {0}", nodeZ.toString());

        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
        List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);

        log.log(Level.INFO, "Found {0} shortest path before verify", KSP.size());




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
            //System.out.format("path[%d]: %d\n", counting, (int) (eachPath.size()+1)/3);
            for (Statement stmtLink : eachPath) {
		if(stmtLink.toString().contains("connectsTo")){
                	//System.out.println(stmtLink.toString());
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
            throw new EJBException(String.format("%s::process doSrrgPathFinding cannot find any feasible path for <%s>", this.getClass().getName(), resLink));
        }

        Iterator<MCETools.Path> itP = KSP.iterator();
	counting=0;
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();


            //verifyOpenflowPath: filter out useless ones
            if (!MCETools.verifyOpenFlowPath(transformedModel, candidatePath)) {
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
	    	//    System.out.println("\n\n\n"+candidatePath.getOntModel().toString()+"\n\n\n");
                }
            }
	    counting++;
        }

        if (KSP.isEmpty()) {
            log.log(Level.INFO, "Could not find any shortest path after verify");
            throw new EJBException(String.format("%s::process doSrrgPathFinding cannot find any feasible path for <%s>", this.getClass().getName(), resLink));
        } else {
            log.log(Level.INFO, "Found {0} KSP after verify", KSP.size());
        }

        //MCETools.printKSP(KSP);
        MCETools.Path solution = getLeastSrrgCostPath(KSP, systemModel, srrgMap);
        log.log(Level.INFO, "Successfully find path with fail prob: {0}", String.valueOf(solution.failureProb));
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

    private List<MCETools.Path> doSrrgPairPathFinding(OntModel systemModel, OntModel spaModel, Resource resLink, List<Map> connTerminalData, Map<Resource, List> srrgMap, String load) {

	String linkname="";

        //String[] bandwidth = new String[1000];

        try {
                URL url = new URL("http://206.196.179.134:8181/onos/v1" + "/network/configuration/links");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                bandwidth=this.executeHttpMethod(url, conn, "GET", null, "karaf", "karaf");

        } catch (IOException ex) {
                        Logger.getLogger(MCE_L2OpenflowPath.class.getName()).log(Level.SEVERE, null, ex);
        }



        //return one shortest path with minimum SRRG probability
        //        //filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        
        String [] json_links=new String[link_count];
        for(int i=0;i<link_count;i++){
                json_links[i] = bandwidth[i];
        }


        //return a list that has two elements, one is primary path, the other is backup path
        List<MCETools.Path> solutionList = new ArrayList<>();

        //filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        //filter out irrelevant statements (based on property type, label type, has switchingService etc.)
        OntModel transformedModel = MCETools.transformL2OpenflowPathModel(systemModel);

        //verify is transformedModel is bidirectional connectsTo
        if (!verifyConnectsToModel(transformedModel)) {
            throw new EJBException(String.valueOf("transformedModel is not fully bidirectional\n"));
        }

        //get source and dest nodes (nodeA, nodeZ), initially Link points to 2 biDirectionalPort
        List<Resource> terminals = new ArrayList<>();
        Resource terminal = null;
        for (Map entry : connTerminalData) {

            if (!entry.containsKey("policyData") || !entry.containsKey("type") || !entry.containsKey("protection")) {
                continue;
            }
            if(entry.get("dataType").toString().equals("port")){
                terminal = systemModel.getResource(entry.get("dataValue").toString());
                if (terminal == null) {
                    throw new EJBException(String.format("%s::process doSrrgPathFinding cannot identify terminal <%s>", MCE_L2OpenflowPath.class.getName(), (String) entry.get("port")));
                }
                terminals.add(terminal);
            }
        }
/*        for (Map entry : connTerminalData) {
            if (!entry.containsKey("policyData") || !entry.containsKey("type") || !entry.containsKey("protection")) {
                continue;
            }
            Resource terminal = systemModel.getResource(entry.get("port").toString());
            if (terminal == null) {
                throw new EJBException(String.format("%s::process doSrrgPathFinding cannot identify terminal <%s>", MCE_L2OpenflowPath.class.getName(), (String) entry.get("port")));
            }
            terminals.add(terminal);
        }
*/
        if (terminals.size() != 2) {
            throw new EJBException(String.format("%s::process cannot doSrrgPathFinding for %s which provides not 2 terminals", this.getClass().getName(), resLink));
        }

        Resource nodeA = terminals.get(0);
        Resource nodeZ = terminals.get(1);

        log.log(Level.INFO, "Link-src: {0}", nodeA.toString());
        log.log(Level.INFO, "Link-dst: {0}", nodeZ.toString());

        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
        List<MCETools.Path> KSP = MCETools.computeKShortestPaths(transformedModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);

        log.log(Level.INFO, "Find {0} KSP (working) before verify", KSP.size());


	int qtyUnset=0;
	int flagunset=0;
        int counting;
        int bwreq=Integer.parseInt(load);
        int bwfree=0;
        int[] unsetPath = new int[MCETools.KSP_K_DEFAULT];
        for(counting=0;counting<MCETools.KSP_K_DEFAULT;counting++){
                unsetPath[counting]=0;
        }
        counting=0;
        for (MCETools.Path eachPath : KSP) {
	    flagunset=0;
            String[] srcdstlink = new String[2];
            srcdstlink[0]="";
            srcdstlink[1]="";
            //System.out.format("path[%d]: %d\n", counting, (int) (eachPath.size()+1)/3);
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
								qtyUnset++;
								flagunset=1;
								break;
                                                        }
                                                }
                                        }
                                }
                        }
                }
		if(flagunset==1) break;
            }
            counting = counting + 1;
	}

        if (KSP == null || KSP.isEmpty()) {
            throw new EJBException(String.format("%s::process doSrrgPairPathFinding cannot find any working feasible path for <%s>", this.getClass().getName(), resLink));
        }

        Iterator<MCETools.Path> itP = KSP.iterator();
        counting=0;
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();

            //verifyOpenflowPath: filter out useless ones
            if (!MCETools.verifyOpenFlowPath(transformedModel, candidatePath)) {
                itP.remove();
            } else {
                //generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                OntModel l2PathModel = MCETools.createL2PathVlanSubnets(transformedModel, candidatePath, null);
                if (l2PathModel == null) {
                    itP.remove();
                } else {
			if(qtyUnset<KSP.size()){
                        if(unsetPath[counting]==1) {
                        itP.remove();
                        }}
                    candidatePath.setOntModel(l2PathModel);
                }
            }
            counting++;
        }

        if (KSP.isEmpty()) {
            log.log(Level.INFO, "Could not find any shortest path after verify");
            throw new EJBException(String.format("%s::process doSrrgPairPathFinding cannot find any working feasible path for <%s>", this.getClass().getName(), resLink));
        } else {
            log.log(Level.INFO, "Find {0} KSP (working) after verify", KSP.size());
        }

        double jointProbability = 1.0;
        double cost = 100000.0;
        int flag = -1;
        MCETools.Path solutionBack = null;
	
	MCETools.Path path = getLeastSrrgCostPath(KSP, systemModel, srrgMap);
	solutionBack = this.getLinkDisjointPath(transformedModel, path, resLink, terminals, srrgMap, load, json_links,unsetPath);
/*        for (int i = 0; i < KSP.size(); i++) {

            //log.log(Level.INFO, "for working path {0}:", i);
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
            jointProbability = (1 - this.getPathProbability(transformedModel, path, srrgMap))
                    * (1 - this.getPathProbability(transformedModel, backPath, srrgMap));

            if (jointProbability < cost) {
                flag = i;
                cost = jointProbability;
                solutionBack = backPath;
            }
        }

        if (flag == -1 || solutionBack == null) {
            throw new EJBException(String.format("%s::process doSrrgPairPathFinding cannot find any backup feasible path for <%s>", this.getClass().getName(), resLink));
        }

        if (flag >= KSP.size()) {
            throw new EJBException(String.format("%s::process doPathPairFinding encounter an error when finding backup paths <%s>", this.getClass().getName(), resLink));
        }
*/
        log.log(Level.INFO, "Successfully find path pair");

        //solutionList.add(KSP.get(flag));
        //solutionList.add(solutionBack);
	
	solutionList.add(path);
	solutionList.add(solutionBack);
        
	MCETools.printKSP(solutionList);



        //log.log(Level.INFO, "Successfully find path with fail prob: {0}", String.valueOf(solutionList.get(1).failureProb));
        //MCETools.printMCEToolsPath(solutionList.get(1));


        return solutionList;
    }

    private MCETools.Path getLeastSrrgCostPath(List<MCETools.Path> candidates, OntModel systemModel, Map<Resource, List> srrgMap) {

        double cost = 1000000.0;
        MCETools.Path solution = null;

        for (MCETools.Path path : candidates) {

            //getPathProbability returns path success probability = (1-p1)(1-p2)...
            path.failureProb = 1 - this.getPathProbability(systemModel, path, srrgMap);
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
	System.out.format("Find path with failure prob: %f\n", solution.failureProb);
        return solution;
    }

    private MCETools.Path getLinkDisjointPath(OntModel systemModel, MCETools.Path primaryPath, Resource resLink, List<Resource> terminals, Map<Resource, List> srrgMap, String load, String[] json_links, int[] unsetPath) {

	String linkname="";
        //return one path with min srrg probability that is link disjoint to primary path

        //MCETools.printMCEToolsPath(primaryPath);
        String[] toNodeConstraint = {
	    "SELECT $s $p $o WHERE {$s a nml:Node. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",};
        String[] isAliasConstraint = {
            "SELECT $s $p $o WHERE {$s a nml:BidirectionalPort. $o a nml:BidirectionalPort FILTER($s = <$$s> && $o = <$$o>)}",};
        //first mask the link(isAlias) of primaryPath
        Iterator<Statement> itS = primaryPath.iterator();
	int i=0;
	System.out.format("path size = %d\n",primaryPath.size());
        while (itS.hasNext()) {
            Statement stmt = itS.next();
	    /*
	    //debug to run node+link disjoint
	    if (MCETools.evaluateStatement_AnyTrue(systemModel, stmt, toNodeConstraint) && i!=1 && i!=primaryPath.size()-1) {
		System.out.format("i=%d\n", i);
		//Resource node = stmt.getSubject();
		Property connFilters = Nml.connectsTo;
		StmtIterator itStmt = systemModel.listStatements(stmt.getSubject(), connFilters, (Resource) null);
		ArrayList<Statement> stmtList = new ArrayList<Statement>();
		while (itStmt.hasNext()){
		    Statement stmtTemp = itStmt.next();
		    System.out.format("remove stmt: %s%n", stmtTemp.toString());
		    stmtList.add(stmtTemp);
		}

		itStmt = systemModel.listStatements(null, connFilters, stmt.getSubject());
		while (itStmt.hasNext()){
		    Statement stmtTemp = itStmt.next();
		    System.out.format("remove stmt: %s%n", stmtTemp.toString());
		    stmtList.add(stmtTemp);
		}

		systemModel = (OntModel) systemModel.remove(stmtList);
	    }
	    //debug to run node+link disjoint
	    */
            if (MCETools.evaluateStatement_AnyTrue(systemModel, stmt, isAliasConstraint)) {
                //System.out.format("remove this stmt: \n%s\n", stmt.toString());
                systemModel = (OntModel) systemModel.remove(stmt);
            }
	    i=i+1;
	    //Statement stmt = itS.next();
        }

        Resource nodeA = terminals.get(0);
        Resource nodeZ = terminals.get(1);

        Property[] filterProperties = {Nml.connectsTo};
        Filter<Statement> connFilters = new PredicatesFilter(filterProperties);
        List<MCETools.Path> backupKSP = MCETools.computeKShortestPaths(systemModel, nodeA, nodeZ, MCETools.KSP_K_DEFAULT, connFilters);

        log.log(Level.INFO, "Find {0} KSP (backup) before verify", backupKSP.size());




	int qtyUnset=0;
	int flagunset=0;
        int counting;
        int bwreq=Integer.parseInt(load);
        int bwfree=0;
	int reservation_flag=0;
        //int[] unsetPath = new int[MCETools.KSP_K_DEFAULT];
        //for(counting=0;counting<MCETools.KSP_K_DEFAULT;counting++){
	//	if(unsetPath[counting]==1) reservation_flag=1; 
        //        unsetPath[counting]=0;
        //}
        counting=0;
	//if(reservation_flag==0){
        for (MCETools.Path eachPath : backupKSP) {
	    flagunset=0;
            String[] srcdstlink = new String[2];
            srcdstlink[0]="";
            srcdstlink[1]="";
            //System.out.format("path[%d]: %d\n", counting, (int) (eachPath.size()+1)/3);
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
								qtyUnset++;
								flagunset=1;
								break;
                                                        }
                                                }
                                        }
                                }
                        }
                }
		
		if(flagunset==1) break;
            }
        //}
            counting = counting + 1;
        }


        if (backupKSP == null || backupKSP.isEmpty()) {
            //throw new EJBException(String.format("%s::process doPathFinding cannot find feasible path for <%s>", resLink));
            return null;
        }

        Iterator<MCETools.Path> itP = backupKSP.iterator();
	counting=0;
        while (itP.hasNext()) {
            MCETools.Path candidatePath = itP.next();

            if (!MCETools.verifyOpenFlowPath(systemModel, candidatePath)) {
                itP.remove();
            } else {
                //generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                OntModel l2PathModel = MCETools.createL2PathVlanSubnets(systemModel, candidatePath, null);
                if (l2PathModel == null) {
                    itP.remove();
                } else {
			if(qtyUnset<backupKSP.size()){
                        if(unsetPath[counting]==1) {
                        itP.remove();
                        }}
                    candidatePath.setOntModel(l2PathModel);
                }
            }
	    counting++;
        }
        if (backupKSP.isEmpty()) {
            return null;
        } else {
            //log.log(Level.INFO, "Find {0} KSP (backup) after verify", backupKSP.size());
        }

        //pick one in backupKSP that has minimum srrg probability
        //MCETools.printKSP(backupKSP);
        MCETools.Path backPath = this.getLeastSrrgCostPath(backupKSP, systemModel, srrgMap);

        //System.out.println("\nbackup path is:");
        //MCETools.printMCEToolsPath(backPath);
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
                    throw new EJBException(String.format("%s::process getPathProbability cannot identify port <%s>", MCE_L2OpenflowPath.class.getName(), (String) srrgData.get("port")));
                }

                Resource resNode = systemModel.getResource(srrgData.get("node").toString());
                if (resNode == null) {
                    throw new EJBException(String.format("%s::process getPathProbability cannot identify port <%s>", MCE_L2OpenflowPath.class.getName(), (String) srrgData.get("node")));
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
        String src = "urn:ogf:network:onos.maxgigapop.net:network:of:0000000000000001:port-1";
        String dst = "urn:ogf:network:onos.maxgigapop.net:network:of:0000000000000003:port-1";

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
            for (Statement stmtLink : candidatePath) {
                System.out.println(stmtLink.toString());
            }
            flag = flag + 1;

            // verify path
            if (!MCETools.verifyL2Path(transformedModel, candidatePath)) {
                System.out.println("Remove this one\n");
                itP.remove();
            } else {
                // generating connection subnets (statements added to candidatePath) while verifying VLAN availability
                OntModel l2PathModel = MCETools.createL2PathVlanSubnets(transformedModel, candidatePath, null);
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
    
    
    String generateFlowsPathModel_new(MCETools.Path l2path, String ETH_SRC_MAC, String ETH_DST_MAC) 
            throws MalformedURLException, IOException {
        
        String[] device = new String[l2path.size()/3 + 1];
        String[] inport = new String[l2path.size()/3 + 1];
        String[] outport = new String[l2path.size()/3 + 1];
        //flows only will go through l2path.size()/3 +1 devices, inport, outport, ...
        
        int  j=0;

        
        for (int i=0; i< l2path.size(); i=i+3 ){
            Statement stmt = l2path.get(i);
            Resource resInport = stmt.getSubject();
            Resource resDevice = stmt.getObject().asResource();
            
            if ((i+1) >= l2path.size()){
                throw new EJBException(String.format("%s::process: something wrong with number of statement", this.getClass().getName()));
            }
            Statement nextStmt = l2path.get(i+1);
            if (!resDevice.equals(nextStmt.getSubject())){
                throw new EJBException(String.format("%s::process: something wrong with link statement consistensy", this.getClass().getName()));
            }           
            Resource resOutport = nextStmt.getObject().asResource();
            
            device[j] = resDevice.toString();
            if(resInport.toString().split("-eth").length > 1) {
            	inport[j] = resInport.toString().split("-eth")[1];
	    }
	    else {
                inport[j] = resInport.toString().split("port-")[1];	
	    }
            //inport[j] = resInport.toString().split("-eth")[1];
            
            if(resOutport.toString().split("-eth").length > 1) {
            	outport[j] = resOutport.toString().split("-eth")[1];
	    }
	    else {
                outport[j] = resOutport.toString().split("port-")[1];	
	    }
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
        
        
        for (j=0; j< (l2path.size()/3 + 1); j++){
            
            int rn = (int)(Math.random()*1000000000);
            int rnreverse = (int)(Math.random()*1000000000);
            String flowIdn=Integer.toString(rn);
            String flowIdnreverse=Integer.toString(rnreverse);
            
            
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
        
            flowModelString += 
                      "<" + flowTable + ">\n"
                    //+ "       mrs:providesFlow <" + flowID + "> .\n"
                    +"       mrs:providesFlow <" + flowID + "> ;\n"
                    +"       mrs:providesFlow <" + flowIDr + "> .\n"
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
                    + "\n"
                    
                    ;
        }
        return flowModelString;
    }

    String generateFlowsPathModel(MCETools.Path l2path, String topologyURI, String ETH_SRC_MAC, String ETH_DST_MAC) 
            throws MalformedURLException, IOException {
        String flowModel = "";

        String l2path_string = l2path.toString();
        String[] l2path_array = l2path_string.split(",");
        int l2path_array_size = l2path_array.length;
        String[] src_path = new String[l2path_array_size / 3];
        String[] dst_path = new String[l2path_array_size / 3];
        String[] src_dev = new String[l2path_array_size / 3];
        String[] src_port = new String[l2path_array_size / 3];
        String[] src_port_name = new String[l2path_array_size / 3];
        String[] dst_dev = new String[l2path_array_size / 3];
        String[] dst_port = new String[l2path_array_size / 3];
        String[] dst_port_name = new String[l2path_array_size / 3];
        int j = 0;
        for (int i = 0; i < l2path_array_size; i = i + 3) { 
            //l2path_array_size is 3 * l2path.size() 
            //each i only search statement subject
            //each i+2 search statement object
            src_dev[j] = "";
            src_port[j] = "";
            src_port_name[j] = "";
            dst_dev[j] = "";
            dst_port[j] = "";
            dst_port_name[j] = "";
            src_path[j] = l2path_array[i].replaceAll("\\[", "").replaceAll("\\]", "");
            dst_path[j] = l2path_array[i + 2].replaceAll("\\[", "").replaceAll("\\]", "");
            if (src_path[j].contains(topologyURI + ":")) {
                src_dev[j] = src_path[j].split(topologyURI + ":")[1];
            }
            if (src_dev[j].contains(":port")) {
                String[] auxsplit = src_dev[j].split(":port");
                src_dev[j] = auxsplit[0];
                src_port_name[j] = auxsplit[1];
            }
            //if (src_path[j].contains("-eth")) {
            //    src_port[j] = src_path[j].split("-eth")[1];
            //}
            if (src_path[j].contains("port-")) {
                src_port[j] = src_path[j].split("port-")[1];
            }

            if (dst_path[j].contains(topologyURI + ":")) {
                dst_dev[j] = dst_path[j].split(topologyURI + ":")[1];
            }
            if (dst_dev[j].contains(":port")) {
                String[] auxsplit = dst_dev[j].split(":port");
                dst_dev[j] = auxsplit[0];
                dst_port_name[j] = auxsplit[1];
            }
            //if (dst_path[j].contains("-eth")) {
            //    dst_port[j] = dst_path[j].split("-eth")[1];
            //}
            if (dst_path[j].contains("port-")) {
                dst_port[j] = dst_path[j].split("port-")[1];
            }

            j++;
        }
        j = 0;
        String[][] device_flow = new String[((l2path_array_size / 3) + 1) / 3][2];
        for (int i = 0; i < l2path_array_size / 3; i = i + 3) { //i= 0, 3, 6
            device_flow[j][0] = src_dev[i];     
            device_flow[j][1] = createFlowModel(topologyURI, device_flow[j][0], src_port[i], src_port_name[i], 
                    dst_port[i + 1], dst_port_name[i + 1], ETH_SRC_MAC, ETH_DST_MAC);
            flowModel = flowModel + device_flow[j][1];
            j++;
        }

        j = 0;

        return flowModel;
    }

    String generateFlowsPathPairModel(MCETools.Path l2path, MCETools.Path l2path_back, String topologyURI, String ETH_SRC_MAC, String ETH_DST_MAC) 
            throws MalformedURLException, IOException {
        String flowModel = "";

        String l2path_string = l2path.toString();
        String[] l2path_array = l2path_string.split(",");
        int l2path_array_size = l2path_array.length;
        String[] src_path = new String[l2path_array_size / 3];
        String[] dst_path = new String[l2path_array_size / 3];
        String[] src_dev = new String[l2path_array_size / 3];
        String[] src_port = new String[l2path_array_size / 3];
        String[] src_port_name = new String[l2path_array_size / 3];
        String[] dst_dev = new String[l2path_array_size / 3];
        String[] dst_port = new String[l2path_array_size / 3];
        String[] dst_port_name = new String[l2path_array_size / 3];
        int j = 0;
        for (int i = 0; i < l2path_array_size; i = i + 3) {
            src_dev[j] = "";
            src_port[j] = "";
            src_port_name[j] = "";
            dst_dev[j] = "";
            dst_port[j] = "";
            dst_port_name[j] = "";
            src_path[j] = l2path_array[i].replaceAll("\\[", "").replaceAll("\\]", "");
            dst_path[j] = l2path_array[i + 2].replaceAll("\\[", "").replaceAll("\\]", "");
            if (src_path[j].contains(topologyURI + ":")) {
                src_dev[j] = src_path[j].split(topologyURI + ":")[1];
            }
            if (src_dev[j].contains(":port")) {
                String[] auxsplit = src_dev[j].split(":port");
                src_dev[j] = auxsplit[0];
                src_port_name[j] = auxsplit[1];
            }
            //if (src_path[j].contains("-eth")) {
            //    src_port[j] = src_path[j].split("-eth")[1];
            //}
            if (src_path[j].contains("port-")) {
                src_port[j] = src_path[j].split("port-")[1];
            }

            if (dst_path[j].contains(topologyURI + ":")) {
                dst_dev[j] = dst_path[j].split(topologyURI + ":")[1];
            }
            if (dst_dev[j].contains(":port")) {
                String[] auxsplit = dst_dev[j].split(":port");
                dst_dev[j] = auxsplit[0];
                dst_port_name[j] = auxsplit[1];
            }
            //if (dst_path[j].contains("-eth")) {
            //    dst_port[j] = dst_path[j].split("-eth")[1];
            //}
            if (dst_path[j].contains("port-")) {
                dst_port[j] = dst_path[j].split("port-")[1];
            }

            j++;
        }
        j = 0;
        String[][] device_flow = new String[((l2path_array_size / 3) + 1) / 3][2];
        for (int i = 0; i < l2path_array_size / 3; i = i + 3) {
            device_flow[j][0] = src_dev[i];
            device_flow[j][1] = createFlowModel(topologyURI, device_flow[j][0], src_port[i], src_port_name[i], 
                    dst_port[i + 1], dst_port_name[i + 1], ETH_SRC_MAC, ETH_DST_MAC);
            flowModel = flowModel + device_flow[j][1];
            j++;
        }

        j = 0;
        if (!l2path_back.equals(null)) {
            String l2path_back_string = l2path_back.toString();
            String[] l2path_back_array = l2path_back_string.split(",");
            int l2path_back_array_size = l2path_back_array.length;
            String[] src_path_back = new String[l2path_back_array_size / 3];
            String[] dst_path_back = new String[l2path_back_array_size / 3];
            String[] src_dev_back = new String[l2path_back_array_size / 3];
            String[] src_port_back = new String[l2path_back_array_size / 3];
            String[] src_port_name_back = new String[l2path_back_array_size / 3];
            String[] dst_dev_back = new String[l2path_back_array_size / 3];
            String[] dst_port_back = new String[l2path_back_array_size / 3];
            String[] dst_port_name_back = new String[l2path_back_array_size / 3];

            for (int i = 0; i < l2path_back_array_size; i = i + 3) {
                src_dev_back[j] = "";
                src_port_back[j] = "";
                src_port_name_back[j] = "";
                dst_dev_back[j] = "";
                dst_port_back[j] = "";
                dst_port_name_back[j] = "";
                src_path_back[j] = l2path_back_array[i].replaceAll("\\[", "").replaceAll("\\]", "");
                dst_path_back[j] = l2path_back_array[i + 2].replaceAll("\\[", "").replaceAll("\\]", "");
                if (src_path_back[j].contains(topologyURI + ":")) {
                    src_dev_back[j] = src_path_back[j].split(topologyURI + ":")[1];
                }
                if (src_dev_back[j].contains(":port")) {
                    String[] auxsplit = src_dev_back[j].split(":port");
                    src_dev_back[j] = auxsplit[0];
                    src_port_name_back[j] = auxsplit[1];

                }
                //if (src_path_back[j].contains("-eth")) {
                //    src_port_back[j] = src_path_back[j].split("-eth")[1];
                //}
                if (src_path_back[j].contains("port-")) {
                    src_port_back[j] = src_path_back[j].split("port-")[1];
                }

                if (dst_path_back[j].contains(topologyURI + ":")) {
                    dst_dev_back[j] = dst_path_back[j].split(topologyURI + ":")[1];
                }
                if (dst_dev_back[j].contains(":port")) {
                    String[] auxsplit = dst_dev_back[j].split(":port");
                    dst_dev_back[j] = auxsplit[0];
                    dst_port_name_back[j] = auxsplit[1];
                }
                //if (dst_path_back[j].contains("-eth")) {
                //    dst_port_back[j] = dst_path_back[j].split("-eth")[1];
                //}
                if (dst_path_back[j].contains("port-")) {
                    dst_port_back[j] = dst_path_back[j].split("port-")[1];
                }

                j++;
            }
            j = 0;
            String[][] device_flow_back = new String[((l2path_back_array_size / 3) + 1) / 3][2];
            for (int i = 0; i < l2path_back_array_size / 3; i = i + 3) {
                device_flow_back[j][0] = src_dev_back[i];
                device_flow_back[j][1] = createFlowModel(topologyURI, device_flow_back[j][0], src_port_back[i], src_port_name_back[i], 
                        dst_port_back[i + 1], dst_port_name_back[i + 1], ETH_SRC_MAC, ETH_DST_MAC);
                flowModel = flowModel + device_flow_back[j][1];

                j++;
            }

        }

        return flowModel;
    }

    String createFlowModel(String topologyURI, String device_flow, String src_port, String src_port_name, String dst_port, String dst_port_name, 
            String ETH_SRC_MAC, String ETH_DST_MAC) {
        int rn = (int)(Math.random()*1000000000);
        String flowIdn=Integer.toString(rn);
        String flowModelString = ""
                + /*String flowModelString="/<delta> \n" +
                 "<id>1</id>\n" +
                 "<creationTime>2015-09-01T10:49:40-05:00</creationTime>\n" +
                 "<referenceVersion>35a31ca5-4419-4785-8321-9d3eef3c984e</referenceVersion>\n" +
                 "<modelReduction>\n" +
                 "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .@prefix owl:   <http://www.w3.org/2002/07/owl#> .@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n" +
                 "</modelReduction>\n" +
                 "\n" +
                 "<modelAddition>\n" +*/ "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .@prefix owl:   <http://www.w3.org/2002/07/owl#> .@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
                + "<" + topologyURI + ":" + device_flow + ">\n"
                + "        a      nml:Node , owl:NamedIndividual ;\n"
                + "        nml:hasService <" + topologyURI + ":" + device_flow + ":openflow-service> .\n"
                + "\n"
                + "<" + topologyURI + ":" + device_flow + ":openflow-service>\n"
                + "        a                       mrs:OpenflowService , owl:NamedIndividual ;\n"
                + "        nml:hasBidirectionalPort <" + topologyURI + ":" + device_flow + ":port" + src_port_name + "> , <" + topologyURI + ":" + device_flow + ":port" + dst_port_name + "> ;\n"
                + "        mrs:providesFlowTable <" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0> .\n"
                + "\n"
                + "<" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0>\n"
                + "       a                        mrs:FlowTable , owl:NamedIndividual .\n"
                + "\n"
                + "<" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0>\n"
                + "       a                        mrs:FlowTable , owl:NamedIndividual ;\n"
                + "       mrs:providesFlow <" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0:flow-"+flowIdn+"> .\n"
                + "\n"
                + "<" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0:flow-"+flowIdn+">\n"
                + "       a                        mrs:Flow , owl:NamedIndividual ;\n"
                + "                mrs:flowMatch <" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0:flow-"+flowIdn+":rule-match-0> ;\n"
                //+ "                mrs:flowMatch <"+topologyURI+":"+device_flow+":openflow-service:flow-table-0:flow-"+flowIdn+":rule-match-1> ;\n"
                //+ "                mrs:flowMatch <"+topologyURI+":"+device_flow +":openflow-service:flow-table-0:flow-"+flowIdn+":rule-match-2> ;\n" 
                //+"                mrs:flowMatch <"+topologyURI+":"+device_flow +":openflow-service:flow-table-0:flow-"+flowIdn+":rule-match-3> ;\n" +
                //+"                mrs:flowMatch <"+topologyURI+":"+device_flow +":openflow-service:flow-table-0:flow-"+flowIdn+":rule-match-4> ;\n" +
                + "                mrs:flowAction <" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0:flow-"+flowIdn+":rule-action-0> .\n"
                + "\n"
                + "<" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0:flow-"+flowIdn+":rule-match-0>\n"
                + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                + "                mrs:type \"IN_PORT\" ;\n"
                + "                mrs:value \"" + src_port + "\" .\n"
                + "\n"
                + "<" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0:flow-"+flowIdn+":rule-action-0>\n"
                + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                + "                mrs:type \"OUT_PORT\" ;\n"
                + "                mrs:value \"" + dst_port + "\" .\n"
                + "\n"
                + "<" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0:flow-"+flowIdn+":rule-match-1>\n"
                + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                + "                mrs:type \"ETH_SRC_MAC\" ;\n"
                + "                mrs:value \"" + ETH_SRC_MAC + "\" .\n"
                + "\n"
                + "<" + topologyURI + ":" + device_flow + ":openflow-service:flow-table-0:flow-"+flowIdn+":rule-match-2>\n"
                + "          a             mrs:FlowRule , owl:NamedIndividual ;\n"
                + "                mrs:type \"ETH_DST_MAC\" ;\n"
                + "                mrs:value \"" + ETH_DST_MAC + "\" .\n"
                + "\n"
                ; /*+
         "\n" +
         "</modelAddition>\n" +
         "</delta>";*/

        return flowModelString;
    }


    //send GET to HTTP server and retrieve response
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
	//int count = 0;
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




    private void updateLinkReservation(List<MCETools.Path> l2pathList, String load) {

        int bwreq=Integer.parseInt(load);
	int newvalue=0;
	//for(int i=0;i<link_count;i++){
	//	System.out.println("link info: "+bandwidth[i]);
	//}
	String jsonUpdate="{\"links\":{";
        for (MCETools.Path eachPath : l2pathList) {
            String[] srcdstlink = new String[2];
	    String linkname="";
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
        }
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
