/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.ModelUtil;

/**
 *
 * @author xin
 */
public class DTNPush {

    private String topologyUri = null;
    static final Logger logger = Logger.getLogger(DTNPush.class.getName());
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    private String output;
    private String error;

    //todo: push dynamic infomation
    public DTNPush(String user_account, String access_key, String address, String topologyUri) {
        //have all the information regarding the topology
        //do an adjustment to the topologyUri
        this.topologyUri = topologyUri + ":";
    }

    /**
     * ***********************************************
     * function to propagate all the requests
     * ************************************************
     */
    public String pushPropagate(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) throws Exception {
        String requests = "";

        //delete a data transfer
        requests += cancelDataTransfer(modelRef, modelReduct);

        //start a data transfer
        requests += createDataTransfer(modelRef, modelAdd);
//        logger.log(Level.INFO, requests);
        return requests;
    }

    public void pushCommit(String r) {
        String[] requests = r.split("[\\n]");

        for (String request : requests) {
            if (request.contains("CreateDataTransfer")) {
//                String[] parameters = request.split("\\s+");
                String[] parameters = request.split(";");
                String method = parameters[1];
                String type;
                if (parameters[2].compareTo("file")==0)
                    type = "";
                else
                    type = "-r";
                ArrayList<String> cmdarray = new ArrayList<String>();
                String taskid = parameters[3];
                String source = parameters[4];
                String destination = parameters[5];
                String[] options = null;
                if (method.compareTo("globus-cli")==0 ){
                    //start transfer using globus online
                    //todo: get credential automatically
                    //activate credential beforehand
                    cmdarray.add("gsissh"); cmdarray.add("cli.globusonline.org"); cmdarray.add("transfer");
                    cmdarray.add("--taskid="+taskid);
                    if(parameters.length == 7) {
                        options = parameters[6].split("\\s+");
                        cmdarray.addAll(Arrays.asList(options));
                    }
                    cmdarray.add("--"); cmdarray.add(source); cmdarray.add(destination);
                    if(parameters[2].compareTo("file")!=0)
                        cmdarray.add("-r");
                    cmdarray.add("&");
                    String cmd[] = new String[cmdarray.size()];
                    cmd = cmdarray.toArray(cmd);
                    int exit = runcommand(cmd);
                    if (exit==0)
                        logger.info("Request 'CreateDataTransfer' successful committed " + this.output);
                }
                else if (method.compareTo("globus-url-copy")==0 ){
                    //start transfer using globus-url-copy
                    String credential = parameters[6];
                    String[] temp = credential.split("\\s+");
                    String src_cred = temp[1];
                    String dst_cred = temp[3];
                    String src_dn = getDN(src_cred);
                    String dst_dn = getDN(dst_cred);
                    cmdarray.add("globus-url-copy");
                    cmdarray.add("-sc"); cmdarray.add(src_cred);
                    cmdarray.add("-dc"); cmdarray.add(dst_cred);
                    cmdarray.add("-data-cred"); cmdarray.add("auto");
                    cmdarray.add("-ss"); cmdarray.add(src_dn);
                    cmdarray.add("-ds"); cmdarray.add(dst_dn);
                    if(parameters.length == 8) {
                        options = parameters[7].split("\\s+");
                        cmdarray.addAll(Arrays.asList(options));
                    }
                    cmdarray.add("gsiftp://" + source);
                    cmdarray.add("gsiftp://" + destination);
                    String cmd[] = new String[cmdarray.size()];
                    cmd = cmdarray.toArray(cmd);
                    int exit = runcommand(cmd);
                    //domain name resolve
                    for(int i=0; i<2; i++){
                        if(exit != 0){
                            //error happens, possibly domain name unsolved
                            if(this.error.contains("Authorization denied: The name of the remote entity")) {
                                int first1 = this.error.indexOf("(");
                                int first2 = this.error.indexOf(")");
                                int second1 = this.error.indexOf("(", first1+1);
                                int second2 = this.error.indexOf(")", first2+1);
                                String newdn = this.error.substring(first1+1, first2);
                                String olddn = this.error.substring(second1+1, second2);
                                if(olddn.compareTo(src_dn)==0)
                                    cmdarray.set(8, newdn);
                                else if(olddn.compareTo(dst_dn)==0)
                                    cmdarray.set(10,newdn);
                                cmd = new String[cmdarray.size()];
                                cmd = cmdarray.toArray(cmd);
                                exit = runcommand(cmd);
                            }
                        }
                    }
                    if(exit==0)
                        logger.info("Request 'CreateDataTransfer' successful committed " + this.output);
                }
            }

            else if (request.contains("CancelDataTransfer")){
                String[] parameters = request.split(";");
                String taskid = parameters[1];
                String[] cmd = {"gsissh", "cli.globusonline.org", "cancel", taskid};
                int exit = runcommand(cmd);
                if (exit==0)
                    logger.info("Request 'CancelDataTransfer' successful committed " + this.output);
            }
        }
    }

    private String cancelDataTransfer(OntModel model, OntModel modelReduct){
        String requests = "";
        String query;
        query = "SELECT ?transfer WHERE {?transfer a nml:DataTransfer}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode transfer = querySolution.get("transfer");
            String transferTagValue = transfer.asResource().toString().replace(topologyUri, "");

            //find out the id of transfer
            query = "SELECT ?taskid WHERE {<" + transfer.asResource() + "> mrs:taskid ?taskid}";
            ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("model reduction does not specified taskid for data transfer: %s", transfer));
            }
            QuerySolution querySolution1 = r1.next();
            RDFNode taskid = querySolution1.get("taskid");

            requests += String.format("CancelDataTransfer;%s;\n", taskid);
        }

        return requests;

    }

    private String createDataTransfer(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query;
        query = "SELECT ?transfer WHERE {?transfer a mrs:DataTransfer}";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode transfer = querySolution.get("transfer");
            String transferTagValue = transfer.asResource().toString().replace(topologyUri, "");

            //find out the source of transfer
            query = "SELECT ?source WHERE {<" + transfer.asResource() + "> mrs:source ?source}";
            ResultSet r1 = executeQuery(query, emptyModel, modelAdd);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("model addition does not specify source of data transfer: %s", transfer));
            }
            QuerySolution querySolution1 = r1.next();
            RDFNode source = querySolution1.get("source");

            //find out the destination of transfer
            query = "SELECT ?destination WHERE {<" + transfer.asResource() + "> mrs:destination ?destination}";
            r1 = executeQuery(query, emptyModel, modelAdd);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("model addition does not specify destination of data transfer: %s", transfer));
            }
            querySolution1 = r1.next();
            RDFNode destination = querySolution1.get("destination");

            //find out the parameters of transfer
            query = "SELECT ?parameter WHERE {<" + transfer.asResource() + "> nml:parameter ?parameter}";
            r1 = executeQuery(query, emptyModel, modelAdd);
            String parameters = "";
            if (r1.hasNext()) {
                querySolution1 = r1.next();
                RDFNode parameter = querySolution1.get("parameter");
                parameters = parameter.asLiteral().getString();
            }

            //find out the credential paths of transfer
            query = "SELECT ?credential WHERE {<" + transfer.asResource() + "> mrs:credential ?credential}";
            r1 = executeQuery(query, emptyModel, modelAdd);
            String credentials = "";
            if (r1.hasNext()) {
                querySolution1 = r1.next();
                RDFNode credential = querySolution1.get("credential");
                credentials = credential.asLiteral().getString();
            }

            //find out the type of transfer, either file or directory
            query = "SELECT ?type WHERE {<" + transfer.asResource() + "> mrs:type ?type}";
            r1 = executeQuery(query, emptyModel, modelAdd);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("model addition does not specify type of data transfer: %s", transfer));
            }
            querySolution1 = r1.next();
            RDFNode type = querySolution1.get("type");

            String s_addr = source.asLiteral().getString().split("/")[0];
            String d_addr = destination.asLiteral().getString().split("/")[0];
            if(s_addr.contains("#") || d_addr.contains("#")){
                //generate taskid for data transfer
                String[] cmd = {"gsissh", "cli.globusonline.org", "transfer", "--generate-id"};
                int exit = runcommand(cmd);
                if(exit==0){
                    String[] tokens = this.output.split("\n");
                    String taskid = tokens[0];
                    requests += String.format("CreateDataTransfer;globus-cli;%s;%s;%s;%s;%s;\n", type.asLiteral().getString(),
                            taskid, source.asLiteral().getString(), destination.asLiteral().getString(), parameters);
                }
            }
            else{
                //globus-url-copy transfer
                requests += String.format("CreateDataTransfer;globus-url-copy;%s;%s;%s;%s;%s;%s;\n", type.asLiteral().getString(),
                        transferTagValue, source.asLiteral().getString(), destination.asLiteral().getString(), credentials, parameters);
            }
        }
        return requests;
    }

    /**
     * ****************************************************************
     * function that executes a query using a model addition/subtraction and a
     * reference model, returns the result of the query
     * ****************************************************************
     */
    private ResultSet executeQuery(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet r = qexec.execSelect();

        //check on reference model if the statement is not in the model addition,
        //or model subtraction
        if (!r.hasNext()) {
            qexec = QueryExecutionFactory.create(query, refModel);
            r = qexec.execSelect();
        }
        return r;
    }

    private String getDN(String cred_file) {
        String dn="";
        String[] cmd = {"grid-proxy-info", "-file", cred_file,"-issuer"};
        int exit = runcommand(cmd);
        if(exit==0){
            dn= this.output.split("\n")[0];
        }
        return dn;
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
//            System.out.println("Exit: "+exitVal+"\nOut: " + this.output+ "Error: "+this.error);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ex) {
            Logger.getLogger(DTNGet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return exitVal;
    }

}
