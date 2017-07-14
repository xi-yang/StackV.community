/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author xin
 */
@Stateless
public class MCE_TransferCreation extends MCEBase{

    private static final Logger log = Logger.getLogger(MCE_TransferCreation.class.getName());
    private String output;
    private String error;

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        log.log(Level.FINE, "MCE_TransferCreation::process {0}", annotatedDelta);
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw new EJBException(String.format("%s::process ", this.getClass().getName()));
        }
        try {
            log.log(Level.FINE, "\n>>>MCE_TransferCreation--DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_TransferCreation.class.getName()).log(Level.SEVERE, null, ex);
        }
        // importPolicyData
        String sparql = "SELECT ?res ?policy ?data ?dataType ?dataValue WHERE {"
                + "?res spa:dependOn ?policy . "
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_TransferCreation'. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?dataType. ?data spa:value ?dataValue. "
                + String.format("FILTER (not exists {?policy spa:dependOn ?other} && not exists {?res a spa:PolicyAction} && ?policy = <%s>)", policy.getURI())
                + "}";
        Map<Resource, List> policyMap = new HashMap<>();
        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            Resource res = querySolution.get("res").asResource();
            if (!policyMap.containsKey(res)) {
                List policyList = new ArrayList<>();
                policyMap.put(res, policyList);
            }
            Resource resPolicy = querySolution.get("policy").asResource();
            Resource resData = querySolution.get("data").asResource();
            RDFNode nodeDataType = querySolution.get("dataType");
            RDFNode nodeDataValue = querySolution.get("dataValue");
            Map policyData = new HashMap<>();
            policyData.put("policy", resPolicy);
            policyData.put("data", resData);
            policyData.put("type", nodeDataType.toString());
            policyData.put("value", nodeDataValue.toString());
            policyMap.get(res).add(policyData);
        }
 
        ServiceDelta outputDelta = annotatedDelta.clone();
        
        for (Resource res : policyMap.keySet()) {
            //1. compute placement based on filter/match criteria *policyData*
            // returned placementModel contains the hosting Node/Topology from systemModel
            OntModel placementModel = this.doCreation(systemModel.getOntModel(), annotatedDelta.getModelAddition().getOntModel(), res, policyMap.get(res));
            if (placementModel == null) {
                throw new EJBException(String.format("%s::process cannot resolve any policy to place %s", this.getClass().getName(), res));
            }
            
            //2. merge the placement satements into spaModel
            outputDelta.getModelAddition().getOntModel().add(placementModel.getBaseModel());
            
            //3. update policyData this action exportTo 
//            this.exportPolicyData(outputDelta.getModelAddition().getOntModel(), res);

            //4. remove policy and all related SPA statements receursively under the res from spaModel
            //   and also remove all statements that say dependOn this 'policy'
            removeResolvedAnnotation(outputDelta.getModelAddition().getOntModel(), res);            
        }
        try {
            log.log(Level.FINE, "\n>>>MCE_TransferCreation--outputDelta Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_VMFilterPlacement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return new AsyncResult(outputDelta);
    }

    
    private OntModel doCreation(OntModel systemModel, OntModel spaModel, Resource res, List<Map> placementCriteria) {
        OntModel placementModel = null;
        for (Map filterCriterion : placementCriteria) {
            if (!filterCriterion.containsKey("data") || !filterCriterion.containsKey("type") || !filterCriterion.containsKey("value")) {
                continue;
            }
            JSONObject jsonTransReqs = null;
            if (((String) filterCriterion.get("type")).equalsIgnoreCase("JSON")) {
                JSONParser parser = new JSONParser();
                try {
                    JSONObject jsonObj = (JSONObject) parser.parse((String) filterCriterion.get("value"));
                    if (jsonTransReqs == null) {
                        jsonTransReqs = jsonObj;
                    } else { // merge
                        for (Object key: jsonObj.keySet()) {
                            jsonTransReqs.put(key, jsonObj.get(key));
                        }
                    }
                } catch (ParseException e) {
                    throw new EJBException(String.format("%s::process cannot parse json string %s", this.getClass().getName(), (String) filterCriterion.get("value")));
                }  
            } 
           if (jsonTransReqs == null || jsonTransReqs.isEmpty()) {
                throw new EJBException(String.format("%s::process  cannot import data from %s", this.getClass().getName(), (String) filterCriterion.get("value")));
            }
            OntModel hostModel = filterTopologyNode(systemModel, spaModel, res, jsonTransReqs);
            if (hostModel == null) {
                throw new EJBException(String.format("%s::process cannot place %s based on polocy %s", this.getClass().getName(), res, filterCriterion.get("policy")));
            }
            //$$ create transfer and relation
            //$$ assemble placementModel;
            if (placementModel == null) {
                placementModel = hostModel;
            } else {
                placementModel.add(hostModel.getBaseModel());
            }
            // ? place to a specific Node ?
            //$$ Other types of filter methods have yet to be implemented.
        }
        return placementModel;
    }
        
        
    private OntModel filterTopologyNode(OntModel systemModel, OntModel spaModel, Resource transRes, JSONObject jsonTransReqs) {
        //1 get all the info in the array that matters for this MCE
        //1.1 get topology info it should be Stirngs
        String transfertype = (String) jsonTransReqs.get("type");
        //1.2 get the sources and destinations, they should be JSONArrays
        JSONArray srcs = (JSONArray) jsonTransReqs.get("sources");
        JSONArray dsts = (JSONArray) jsonTransReqs.get("destinations");
        String dtnUri, srcUri = null, dstUri = null;
        String srcpath = null, dstpath=null;
        //2 verify source and destination
        Resource srcTopology = null, srcdtClusterService = null, dstTopology = null, dstdtClusterService = null;
        String srcEnp=null, srcTraffic = null, srcAT = null, srcHistory = null, srcMax = null;
        String dstEnp=null, dstTraffic = null, dstAT = null, dstHistory = null, dstMax = null; 
        boolean found = false;
        if (srcs != null) {
            ListIterator srcIt = srcs.listIterator();
            while (srcIt.hasNext() && found == false) {            
                JSONObject o = (JSONObject) srcIt.next();
                dtnUri = (String) o.get("uri");
                srcEnp = (String) o.get("endpoint");
                srcpath = srcEnp + (String) o.get("path");
                
                //2.1 verify src model
                String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + "SELECT ?topology ?dtClusterService ?active_transfers ?avg_traffic ?history ?max WHERE {"
                + "?topology a nml:Topology ."
                + "?topology nml:hasService ?dtClusterService ."
                + "?dtClusterService a mrs:DataTransferClusterService ."
                + "?dtClusterService mrs:active_transfers ?active_transfers ."
                + "?dtClusterService mrs:average_traffic ?avg_traffic ."
                + "?dtClusterService mrs:transfer_history ?history ."
                + "?dtClusterService nml:parameter ?max ."
                + String.format("FILTER (?topology = <%s>)}", dtnUri);                
                Query query = QueryFactory.create(sparql);
                QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
                ResultSet r = (ResultSet) qexec.execSelect();
                if (!r.hasNext()) {
                    continue;
                }
                found = true;
                srcUri = dtnUri;
                QuerySolution querySolution = r.next();
                srcTopology = querySolution.get("topology").asResource();
                srcdtClusterService = querySolution.get("dtClusterService").asResource();   
                srcTraffic = querySolution.getLiteral("avg_traffic").getString();
                srcAT = querySolution.getLiteral("active_transfers").getString();
                srcHistory = querySolution.getLiteral("history").getString();
                srcMax = querySolution.getLiteral("max").getString();
            }
            if(found == true && dsts != null) {
                found = false;
                ListIterator dstIt = dsts.listIterator();
                while (dstIt.hasNext() && found == false) {            
                    JSONObject o = (JSONObject) dstIt.next();
                    dtnUri = (String) o.get("uri");
                    dstEnp = (String) o.get("endpoint");
                    dstpath = dstEnp + (String) o.get("path");

                    //2.2 verify dst model      
                    String sparql = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                    + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                    + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                    + "SELECT ?topology ?dtClusterService ?active_transfers ?avg_traffic ?history ?max WHERE {"
                    + "?topology a nml:Topology ."
                    + "?topology nml:hasService ?dtClusterService ."
                    + "?dtClusterService a mrs:DataTransferClusterService ."
                    + "?dtClusterService mrs:active_transfers ?active_transfers ."
                    + "?dtClusterService mrs:average_traffic ?avg_traffic ."
                    + "?dtClusterService mrs:transfer_history ?history ."
                    + "?dtClusterService nml:parameter ?max ."
                    + String.format("FILTER (?topology = <%s>)}", dtnUri);              
                    Query query = QueryFactory.create(sparql);
                    QueryExecution qexec = QueryExecutionFactory.create(query, systemModel);
                    ResultSet r = (ResultSet) qexec.execSelect();
                    if (!r.hasNext()) {
                        continue;
                    }
                    found = true;
                    dstUri = dtnUri;
                    QuerySolution querySolution = r.next();
                    dstTopology = querySolution.get("topology").asResource();
                    dstdtClusterService = querySolution.get("dtClusterService").asResource(); 
                    dstTraffic = querySolution.getLiteral("avg_traffic").getString();
                    dstAT = querySolution.getLiteral("active_transfers").getString();
                    dstHistory = querySolution.getLiteral("history").getString();
                    dstMax = querySolution.getLiteral("max").getString();
                }
            }
        }
         
        //3 create the basic transfer model
        if(found == true) {
            //check if endpoint are activated
            boolean valid = checkEndpointActivation(srcEnp, dstEnp);            
            if(!valid){
                log.log(Level.INFO, "Please activate endpoint before start transfer");
            }
            else {
//                System.out.println("GO ready");
                //create taskid
                String taskid = createTaskID();
                String label = transRes.getURI().substring(transRes.getURI().lastIndexOf(':') + 1);                
                String id  = taskid;
                String id_copy = taskid+"-copy";              
                
                //3.1 create transfer on source dtn model
                Resource srcDTN = RdfOwl.createResource(spaModel, srcUri , Nml.Topology);
                Resource transfer = RdfOwl.createResource(spaModel, srcUri + ":" +id, Mrs.DataTransfer);
                spaModel.add(srcDTN, Nml.hasService, srcdtClusterService);
                spaModel.add(srcdtClusterService, Mrs.hasTransfer, transfer);

                spaModel.addLiteral(transfer, Mrs.source, ResourceFactory.createTypedLiteral(srcpath,XSDDatatype.XSDstring));
                spaModel.addLiteral(transfer, Mrs.destination, ResourceFactory.createTypedLiteral(dstpath,XSDDatatype.XSDstring));
                spaModel.addLiteral(transfer, Mrs.type, ResourceFactory.createTypedLiteral(transfertype,XSDDatatype.XSDstring)); 
                spaModel.addLiteral(transfer, Mrs.active_transfers, ResourceFactory.createTypedLiteral(srcAT,XSDDatatype.XSDstring)); 
                spaModel.addLiteral(transfer, Mrs.average_traffic, ResourceFactory.createTypedLiteral(srcTraffic,XSDDatatype.XSDstring)); 

                //3.2 create transfer on destination dtn model
                Resource dstDTN = RdfOwl.createResource(spaModel, dstUri , Nml.Topology);
                Resource transfer2 = RdfOwl.createResource(spaModel, dstUri + ":" +id_copy, Mrs.DataTransfer);
                spaModel.add(dstDTN, Nml.hasService, dstdtClusterService);
                spaModel.add(dstdtClusterService, Mrs.hasTransfer, transfer2);

                spaModel.addLiteral(transfer2, Mrs.source, ResourceFactory.createTypedLiteral(srcpath,XSDDatatype.XSDstring));
                spaModel.addLiteral(transfer2, Mrs.destination, ResourceFactory.createTypedLiteral(dstpath,XSDDatatype.XSDstring));
                spaModel.addLiteral(transfer2, Mrs.type, ResourceFactory.createTypedLiteral(transfertype,XSDDatatype.XSDstring)); 
                spaModel.addLiteral(transfer2, Mrs.active_transfers, ResourceFactory.createTypedLiteral(dstAT,XSDDatatype.XSDstring)); 
                spaModel.addLiteral(transfer2, Mrs.average_traffic, ResourceFactory.createTypedLiteral(dstTraffic,XSDDatatype.XSDstring)); 
                
                //3.3 apply appropriate parameters
                String parameters = findParameter(srcpath, srcMax, srcTraffic, srcAT, srcHistory, 
                                                  dstpath, dstMax, dstTraffic, dstAT, dstHistory);
                spaModel.addLiteral(transfer, Nml.parameter, ResourceFactory.createTypedLiteral(parameters,XSDDatatype.XSDstring)); 
                spaModel.addLiteral(transfer2, Nml.parameter, ResourceFactory.createTypedLiteral(parameters,XSDDatatype.XSDstring)); 
            }
        }
        
        return spaModel;
    }
    
    @Asynchronous
    private String findParameter(String srcpath, String srcMax, String srcTraffic, String srcAT, String srcHistory,
                                 String dstpath, String dstMax, String dstTraffic, String dstAT, String dstHistory) {
        String parameters;
        int concurrency=0, parallelism=0;   
        int max_p = 0, max_cc = 0;
        double[][] src_traffic = new double[4][4];
        double[][] dst_traffic = new double[4][4];
        double[] predict = new double[29];
        
        String srcEnp = srcpath.substring(0, srcpath.indexOf("/")); 
        String dstEnp = dstpath.substring(0, dstpath.indexOf("/"));
        
        //find maximum allowed pp and c value
        String[] tmp = srcMax.split(",");
        for (String item:tmp){
            String value = item.split("=")[1];
            if(item.contains("max_p"))
                max_p = Integer.parseInt(value);
            if(item.contains("max_cc"))
                max_cc = Integer.parseInt(value);
        }
        tmp = dstMax.split(",");
        for (String item:tmp){
            String value = item.split("=")[1];
            if(item.contains("max_p"))
                max_p = Math.min(max_p, Integer.parseInt(value));
            if(item.contains("max_cc"))
                max_cc = Math.min(max_cc, Integer.parseInt(value));
        }                
//        System.out.println("max_p="+max_p+" max_cc="+max_cc);
        
        //find file size and file number
        long[] results = {0,0};
        String cmd = "gsissh cli.globusonline.org ls -la " +srcpath;
        int exitVal = runcommand(cmd.split(" "));
        if(exitVal==0){
            String[] lists = this.output.split("\n");
            results = calcSizeandNumber(lists, results, srcpath);
        }
        long file_number = results[0];
        long size = results[1];
        
        //parse current traffic
        String[] srcTraf = srcTraffic.split("--");
        String[] dstTraf = dstTraffic.split("--");
        for(int i=0; i<srcTraf.length;i++){
            String[] parts1 = srcTraf[i].split(",");
            String[] parts2 = dstTraf[i].split(",");
            for(int j=0; j<parts1.length;j++){
                    src_traffic[i][j] = Double.parseDouble(parts1[j]);
                    dst_traffic[i][j] = Double.parseDouble(parts2[j]);
            }
        }
        
        for(int i=0;i<src_traffic[0].length;i++){
            predict[i+5] = src_traffic[1][i];
            predict[i+10] = dst_traffic[0][i];
        }
        
        //parse current active transfers
        double src_at = Double.parseDouble(srcAT);
        double dst_at = Double.parseDouble(dstAT);
        predict[4] = (double) src_at;
        predict[9] = (double) dst_at;
        
        //find default parameters based on size and file numbers
        double avg_size = (double) size/file_number/1024;
        if(avg_size < 50 && file_number > 100){
            parallelism = 2;
            concurrency = 2;
        }
        else if (avg_size > 250 && file_number > 100) {
            parallelism = 8;
            concurrency = 2;            
        } else {
            parallelism = 4;
            concurrency = 2;            
        }
        
        //randomize based on history size        
        
        long now = (new Date().getTime());
//        System.out.println("now: "+now);
        String remain = parseHistory(srcHistory, dstHistory, srcEnp, dstEnp, now);
        String[] history = remain.split("\n");
        int histSize = history.length;
        Random ran = new Random();
        int flag = 1;
        if(histSize < 10) { //50% change for random parameters
            flag = ran.nextInt(2);
        } else if (histSize < 20) { //25% change for random parameters
            flag = ran.nextInt(4);
        }else if(histSize < 30) { //10% change for random parameters
            flag = ran.nextInt(10);
        }
        
        if(flag==0){    //randomize parameters
            parallelism = ran.nextInt(max_p)+1;
            concurrency = ran.nextInt(max_cc)+1;   
            log.log(Level.INFO, "randomize parameters: p="+parallelism+" cc="+concurrency);
        }
        else if (histSize >= 15){
            //construct variables
            String[] namespaces = { "size","#file","p","cc",
                "src_AT","src_traffic_1min","src_traffic_5min","src_traffic_10min","src_traffic_30min",
                "dst_AT","dst_traffic_1min","dst_traffic_5min","dst_traffic_10min","dst_traffic_30min",
                "size^2", "#file^2","p^2","cc^2",
                "src_AT^2","src_traffic_1min^2","src_traffic_5min^2","src_traffic_10min^2","src_traffic_30min^2",
                "dst_AT^2","dst_traffic_1min^2","dst_traffic_5min^2","dst_traffic_10min^2","dst_traffic_30min^2",
                "Mbits/sec"  };
            
            double[][] x = new double[histSize][namespaces.length] ;
            double[] coefficients = new double[namespaces.length];
            
            
            for(int i=0; i<histSize;i++){
                String[] parts = history[i].split(",");
                for(int j=0; j<parts.length-1; j++){
                    x[i][j] = Double.parseDouble(parts[j]);
                }
                for(int j=parts.length; j<namespaces.length-1;j++){
                    double temp = Double.parseDouble(parts[j-parts.length]);
                    x[i][j] = temp * temp;
                }
                x[i][namespaces.length-1] = Double.parseDouble(parts[parts.length-1]);
            }
            
            for(int i=0;i<histSize;i++){
//                System.out.print("x["+i+"]: ");
                for(int j=0; j<x[i].length; j++){
//                    System.out.print(x[i][j]+" ");
                }
//                System.out.print("\n");
            }
            
            List<Instance> instances = new ArrayList<Instance>();
            ArrayList<Attribute> atts = new ArrayList<Attribute>();
            for(int j=0;j<namespaces.length;j++){
                Attribute tmpatt = new Attribute(namespaces[j],j);
                for(int i=0; i<histSize; i++){
                    instances.get(i).setValue(tmpatt, x[i][j]);
                }
                atts.add(tmpatt);
            }
            // Create new dataset
            Instances dataset = new Instances("Dataset", atts, instances.size());

            // Fill in data objects
            for(Instance inst : instances){
                dataset.add(inst);  
            }
//            System.out.println(dataset.size());

            //linear fitting
            LinearRegression model = new LinearRegression();
            try {            
                model.buildClassifier(dataset);
                coefficients = model.coefficients();            
                //find the best parameters
                double max_perf = 0;
                for(int c=1;c<=max_cc;c++){
                    for(int p=1; p<=max_p; p++){
                        predict[2] = p;
                        predict[3] = c;
                        for(int j=14; j<namespaces.length-1;j++){
                            predict[j] = predict[j-14] * predict[j-14];
                        }
                        predict[28] = 0;
                        //create instance
                        Instance curr_transfer = dataset.lastInstance().copy(predict);
                        curr_transfer.setMissing(namespaces.length-1);
                        //make prediction
                        double perf = model.classifyInstance(curr_transfer);
                        if(perf > max_perf){
                            max_perf = perf;
                            parallelism = p;
                            concurrency = c; 
                        }
                    }                    
                }
                log.log(Level.INFO, "modeled parameters: p="+parallelism+" cc="+concurrency);
            } catch (Exception ex) {
                log.log(Level.INFO, ex.getMessage());
            }            
        }
        parameters = "--perf-p="+parallelism+" --perf-cc="+concurrency;     
        return parameters;
    }

    
    
    private int runcommand(String[] cmd){
        String s = null, output = "", error="";
        int exitVal = -1;
        try {
            // using the Runtime exec method:            
            Process p = Runtime.getRuntime().exec(cmd);
             
            BufferedReader stdInput = new BufferedReader(new
                 InputStreamReader(p.getInputStream()));
 
            BufferedReader stdError = new BufferedReader(new
                 InputStreamReader(p.getErrorStream()));
            
            // read the output from the command
            while ((s = stdInput.readLine()) != null) {
               output += s+"\n";
            }
          
            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                error += s+"\n";
            }
            exitVal = p.waitFor();
            this.error = error;
            this.output = output;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ex) {
           log.log(Level.SEVERE, ex.getMessage());
        }
        return exitVal;
    }    
    
/*
    private void exportPolicyData(OntModel spaModel, Resource res) {
        // find Placement policy -> exportTo -> policyData
        String sparql = "SELECT ?hostPlace ?policyData WHERE {"
                + String.format("?hostPlace nml:hasNode <%s> .", res.getURI()) 
                + "?hvservice a mrs:HypervisorService . "
                + String.format("?hvservice mrs:providesVM <%s> .", res.getURI())
                + String.format("<%s> spa:dependOn ?policyAction .", res.getURI())
                + "?policyAction a spa:PolicyAction. "
                + "?policyAction spa:type 'MCE_VMFilterPlacement'. "
                + "?policyAction spa:exportTo ?policyData . "
                + "?policyData a spa:PolicyData . "
                + "OPTIONAL {?policyData spa:format ?format.}"
                + "}";
        ResultSet r = ModelUtil.sparqlQuery(spaModel, sparql);
        List<QuerySolution> solutions = new ArrayList<>();
        while (r.hasNext()) {
            solutions.add(r.next());
        }
        for (QuerySolution querySolution : solutions) {
            Resource resHost = querySolution.get("hostPlace").asResource();
            Resource resData = querySolution.get("policyData").asResource();
            spaModel.add(resData, Spa.type, "JSON");
            // add export data
            JSONObject output = new JSONObject();
            output.put("uri", res.getURI());
            output.put("place_into", resHost.getURI());
            //add output as spa:value of the export resrouce
            String exportValue = output.toJSONString();
            if (querySolution.contains("format")) {
                String exportFormat = querySolution.get("format").toString();
                exportValue = MCETools.formatJsonExport(exportValue, exportFormat);
            }
            spaModel.add(resData, Spa.value, exportValue);
        }
    }
*/    

    private String parseHistory(String srcHistory, String dstHistory, String srcEnp, String dstEnp, long now) {
        String remains="";
        String[] srcHist = srcHistory.split("\n");
        String[] dstHist = dstHistory.split("\n");
        long timeLimit = now - 30*24*60*60;     //restrict results to no older than 1 month 
        int counter = 100;  //restrict results to 100 lines
        for(int i=srcHist.length-1; i>=0; i--){
            String line = srcHist[i];
            if(!line.isEmpty() && counter >0){
                String[] parts = line.split(";");
                String src = parts[0];
                String dst = parts[1];
                String id = parts[2];
                long start_time = Long.parseLong(parts[7]);
                double bt = Double.parseDouble(parts[3])/1024/1024;
                String src_ip_traffic = parts[11].split("--")[1];
                String src_ib_traffic = parts[11].split("--")[3];
                if(src.equalsIgnoreCase(srcEnp)&& dst.equalsIgnoreCase(dstEnp) && start_time>timeLimit){
                    //find #AT and background traffic in dst endpoint
                    for(String aline:dstHist){
                        if(!aline.isEmpty()){
                            String[] parts2 = aline.split(";");
                            String id2 = parts2[2];
                            String dst_ip_traffic = parts2[11].split("--")[0];
                            String dst_ib_traffic = parts2[11].split("--")[2];                            
                            if(id.equalsIgnoreCase(id2))
                                line = parts[3]+","         //bytes_transferred
                                     + parts[4]+","         //file_transferred
                                     + parts[5]+","         //parallelism
                                     + parts[6]+","         //concurrency
                                     + parts[10]+","        //src_active_transfer
                                     + src_ip_traffic+","   //src_ip_traffic
                                     + parts2[10]+","       //dst_active_transfer
                                     + dst_ip_traffic+","   //dst_ip_traffic
                                     + parts[9];            //mbits/sec    
                        }
                    }                    
                    remains += line+"\n";
                    counter--;
                }
            }
        }    
        return remains;
    }
    
    private boolean checkEndpointActivation(String srcEnp, String dstEnp) {
        String cmd = "gsissh cli.globusonline.org endpoint-details "+srcEnp+" -f credential_time_left -O csv";
        int exit = runcommand(cmd.split(" "));
        if (exit == 0) {
            if (this.output.equalsIgnoreCase("n/a")){
                return false;
            }
        }        
        cmd = "gsissh cli.globusonline.org endpoint-details "+dstEnp+" -f credential_time_left -O csv";
        exit = runcommand(cmd.split(" "));
        if (exit == 0) {
            if (this.output.equalsIgnoreCase("n/a"))
                return false;
        }                
        return true;
    }

    private String createTaskID() {
        String id = null;
        String[] cmd = {"gsissh", "cli.globusonline.org", "transfer", "--generate-id"};
        int exit = runcommand(cmd);
        if (exit == 0) {
            String[] tokens = this.output.split("\n");
            id = tokens[0];
        }
        return id;
    }

    private long[] calcSizeandNumber(String[] lists, long[] results, String path) {
        for(String line:lists){
            String[] parts = line.split("\\s+");
            if(!parts[6].equalsIgnoreCase("./") && !parts[6].equalsIgnoreCase("../") && !line.contains("->")) {
                results[1] += Long.parseLong(parts[3]);
                if(parts[6].charAt(parts[6].length()-1)=='/'){ //is a directory
                    String newpath = path+parts[6];
                    String cmd = "gsissh cli.globusonline.org ls -la " + newpath;
                    int exitVal = runcommand(cmd.split(" "));
                    if(exitVal==0){
                        String[] newlists = this.output.split("\n");
                        results = calcSizeandNumber(newlists, results, newpath);
                    }
                } else {    //is a file
                    results[0]++;
                }
            } 
        }
//        System.out.println("number: "+results[0]+" size: "+results[1]);
        return results;
    }
}