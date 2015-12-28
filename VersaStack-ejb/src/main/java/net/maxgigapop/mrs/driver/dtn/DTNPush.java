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
import java.io.IOException;
import java.util.ArrayList;
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
    public String pushPropagate(String modelRefTtl, String modelAddTtl, String modelReductTtl) throws EJBException, Exception {
        String requests = "";

        OntModel modelRef = ModelUtil.unmarshalOntModel(modelRefTtl);
        OntModel modelAdd = ModelUtil.unmarshalOntModel(modelAddTtl);
        OntModel modelReduct = ModelUtil.unmarshalOntModel(modelReductTtl);

        //delete a data transfer
        requests += deleteDataTransfer(modelRef, modelReduct);

        //start a data transfer
        requests += createDataTransfer(modelRef, modelAdd);

        return requests;
    }

    public void pushCommit(String r) {
        String[] requests = r.split("[\\n]");

        for (String request : requests) {
            if (request.contains("CreateDataTransfer")) {
                String[] parameters = request.split("\\s+");
                String source = parameters[1];
                String destination = parameters[2];

                //start transfer using globus online
                //todo: get credential automatically
                //activate credential beforehand
                String cmd = "gsissh cli.globusonline.org transfer -- " + source + destination;
                runcommand(cmd);

            } else if (request.contains("DeleteDataTransfer")) {
                String[] parameters = request.split("\\s+");
                String task_id = parameters[1];
                String cmd = "gsissh cli.globusonline.org cancel " + task_id;
                runcommand(cmd);
            }
        }

    }

    private String deleteDataTransfer(OntModel model, OntModel modelReduct) {
        String requests = "";
        String query;
        query = "SELECT ?transfer WHERE {?transfer a nml:DataTransfer}";
        ResultSet r = executeQuery(query, emptyModel, modelReduct);
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode transfer = querySolution.get("transfer");
            String transferTagValue = transfer.asResource().toString().replace(topologyUri, "");

            //find out the id of transfer
            query = "SELECT ?task_id WHERE {<" + transfer.asResource() + "> mrs:task_id ?task_id}";
            ResultSet r1 = executeQuery(query, emptyModel, modelReduct);
            if (!r1.hasNext()) {
                throw new EJBException(String.format("model addition does not specify id of data transfer: %s", transfer));
            }
            QuerySolution querySolution1 = r1.next();
            RDFNode task_id = querySolution1.get("source");

            requests += String.format("DeleteDataTransfer %s \n", task_id);
        }

        return requests;

    }

    private String createDataTransfer(OntModel model, OntModel modelAdd) {
        String requests = "";
        String query;
        query = "SELECT ?transfer WHERE {?transfer a nml:DataTransfer}";
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

            requests += String.format("CreateDataTransfer %s %s  \n", source.asLiteral().getString(), destination.asLiteral().getString());
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

    private int runcommand(String cmd) {
//        String s = null;
        int exitVal = -1;
        try {
            // using the Runtime exec method:
            Process p = Runtime.getRuntime().exec(cmd);
//             
//            BufferedReader stdInput = new BufferedReader(new
//                 InputStreamReader(p.getInputStream()));
// 
//            BufferedReader stdError = new BufferedReader(new
//                 InputStreamReader(p.getErrorStream()));
// 
//            // read the output from the command
//            while ((s = stdInput.readLine()) != null) {
//                System.out.println(s);
//            }
//             
//            // read any errors from the attempted command
//            while ((s = stdError.readLine()) != null) {
//                System.out.println(s);
//            }
            exitVal = p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException ex) {
            Logger.getLogger(DTNGet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return exitVal;
    }

}
