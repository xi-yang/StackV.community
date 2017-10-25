/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.googlecloud;


import org.json.simple.JSONObject;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.util.ArrayList;

/**
 *
 * @author raymonddsmith
 */
public class GoogleCloudQuery {
    /*
    This class contains functions used by GoogleCloudPush for querying the addition
    and reduction model. Having these methods in their own class reduces file bloat.
    */
    private final OntModel modelRef, modelAdd, modelReduct;
    private static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    private static final String defaultRegion = "us-central1";
    private static final String defaultZone = "us-central1-c";
    
    public GoogleCloudQuery(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) {
        this.modelRef = modelRef;
        this.modelAdd = modelAdd;
        this.modelReduct = modelReduct;
    }
    
    //need name, zone
    public ArrayList<JSONObject> createInstanceRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "deleteInstanceRequests";
        String query = "SELECT ?instance WHERE {?vpc a nml:Topology ; "
                + "nml:hasNode ?instance . ?instance a nml:Node mrs:type ?instanceType . }";
        
        return output;
    }
    
    public ArrayList<JSONObject> deleteInstanceRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "createInstanceRequests";
        
        return output;
    }
    
    public ArrayList<JSONObject> createSubnetRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "createSubnetRequests";
        
        return output;
    }
    
    public ArrayList<JSONObject> deleteSubnetRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "deleteSubnetRequests";
        
        return output;
    }
    
    //Need name, uri, at least 1 subnet
    public ArrayList<JSONObject> createVpcRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "createVpcRequests";
        //Find an object that provides a vpc to something else, and is a topology
        //name, subnetName, subnetRegion are optional
        String query = "SELECT ?vpcUri ?vpcName ?subnetUri ?subnetName ?subnetRegion ?cidr "
                + "WHERE { ?service mrs:providesVPC ?vpcUri . ?vpcUri a nml:Topology ;"
                + "?vpcUri nml:hasService ?switchingService. "
                + "?switchingService a mrs:switchingService ; mrs:providesSubnet ?subnetUri"
                + "OPTIONAL { ?vpc nml:name ?vpcName}"
                + "OPTIONAL { ?vpc } }";
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            JSONObject vpcRequest = new JSONObject();
            String name;
            QuerySolution solution = r.next();
            String vpcUri = solution.get("vpc").toString();
            //the vpc may or may not have a name specified
            query = "SELECT ?name WHERE { "+ vpcUri +" nml:name ?name }";
            ResultSet nameResult = executeQuery(query, emptyModel, modelAdd);
            //We only need one name, so we don't care if r contains more than one solution
            if (r.hasNext()) {
                name = r.next().get("name").toString();
            } else {
                //remove gcp incompatible characters
                name = GoogleCloudModelBuilder.removeChars(vpcUri);
                //let the name be the final 60 chars of the uri
                name = name.substring(name.length()-60);
            }
            
            query = "SELECT ?subnetName ?subnetUri WHERE";
            
            vpcRequest.put("name", name);
            vpcRequest.put("uri", vpcUri);
            vpcRequest.put("cidr", "1.2.3.4/32");
            vpcRequest.put("initialSubnetName", "default");
            vpcRequest.put("initialSubnetUri", "temp");
            vpcRequest.put("initialSubnetRegion", defaultRegion);
            
        }
        return output;
    }
    
    public ArrayList<JSONObject> deleteVpcRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "deleteVpcRequests";
        
        return output;
    }

    
    /* ****************************************************************
     * function that executes a query using a model addition/subtraction and a
     * reference model, returns the result of the query
     * ****************************************************************/
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
    
    /**
     * ****************************************************************
     * function that executes a query using a model addition/subtraction and a
     * reference model, returns the result of the query
     * ****************************************************************
     */
    private ResultSet executeQueryUnion(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        Model unionModel = ModelFactory.createUnion(refModel, model);

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, unionModel);
        ResultSet r = qexec.execSelect();
        return r;
    }
}
