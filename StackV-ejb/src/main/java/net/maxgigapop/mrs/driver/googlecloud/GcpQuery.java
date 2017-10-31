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
public class GcpQuery {
    /*
    This class contains functions used by GoogleCloudPush for querying the addition
    and reduction model. Having these methods in their own class reduces file bloat.
    */
    private final OntModel modelRef, modelAdd, modelReduct;
    private static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    private static final String defaultRegion = "us-central1";
    private static final String defaultZone = "us-central1-c";
    
    public GcpQuery(OntModel modelRef, OntModel modelAdd, OntModel modelReduct) {
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
        //need to find 
        
        
        return output;
    }
    
    public ArrayList<JSONObject> createSubnetRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "createSubnetRequests";
        
        //subnet region and name are optional. vpcname is used to build
        String query = "SELECT ?vpcUri ?vpcName ?subnetUri ?subnetName ?subnetCIDR ?subnetRegion"
                + "WHERE { ?service mrs:providesVPC ?vpcUri . ?vpcUri a nml:Topology ;"
                + "?vpcUri nml:hasService ?switchingService . "
                + "?switchingService mrs:providesSubnet ?subnetUri."
                + "?subnetUri mrs:hasNetworkAddress ?addressUri . "
                + "?addressUri a mrs:networkAddress ; mrs:value ?subnetCIDR"
                + "OPTIONAL { ?subnetUri nml:name ?vpcName }"
                + "OPTIONAL { ?subnetUri nml:name ?subnetName} "
                + "OPTIONAL { ?subnetUri mrs:type ?subnetRegion} }";
        
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            JSONObject subnetRequest = new JSONObject();
            QuerySolution solution = r.next();
            String vpcUri = solution.get("vpcUri").toString();
            String vpcName = solution.get("vpcName").toString();
            String subnetUri = solution.get("subnetUri").toString();
            String subnetCIDR = solution.get("subnetCIDR").toString();
            String subnetName = solution.get("subnetName").toString();
            String subnetRegion = solution.get("subnetRegion").toString();
            if (vpcName == null) vpcName = makeNameFromUri(vpcUri);
            if (subnetName == null) subnetName = makeNameFromUri(subnetUri);
            if (subnetRegion == null) subnetRegion = defaultRegion;
            
            subnetRequest.put("type", "create_subnet");
            subnetRequest.put("vpc_name", vpcName);
            subnetRequest.put("subnet_uri", subnetUri);
            subnetRequest.put("subnet_cidr", subnetCIDR);
            subnetRequest.put("subnet_name", subnetName);
            subnetRequest.put("subnet_region", subnetRegion);
            //for now, just print
            //System.out.printf("output: %s\n", subnetRequest);
            output.add(subnetRequest);
        }
        
        return output;
    }
    
    public ArrayList<JSONObject> deleteSubnetRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "deleteSubnetRequests";
        
        return output;
    }
    
    //Need name
    public ArrayList<JSONObject> createVpcRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "createVpcRequests";
        //Find an object that provides a vpc to something else, and is a topology
        //Name is optional. Only URI is mandatory
        String query = "SELECT ?vpcUri ?vpcName"
                + "WHERE { ?service mrs:providesVPC ?vpcUri . ?vpcUri a nml:Topology ;"
                + "OPTIONAL { ?vpcUri nml:name ?vpcName} }";
        
        ResultSet r = executeQuery(query, emptyModel, modelAdd);
        while (r.hasNext()) {
            JSONObject vpcRequest = new JSONObject();
            String name;
            QuerySolution solution = r.next();
            String vpcUri = solution.get("vpc").toString();
            String vpcName = solution.get("vpcName").toString();
            String cidr = solution.get("cidr").toString();
            
            if (vpcName == null) vpcName = makeNameFromUri(vpcUri);
            
            vpcRequest.put("type", "create_vpc");
            vpcRequest.put("name", vpcName);
            vpcRequest.put("uri", vpcUri);
            
            //for now, just print
            //System.out.printf("output: %s\n", vpcRequest);
            output.add(vpcRequest);
        }
        return output;
    }
    
    public ArrayList<JSONObject> deleteVpcRequests() {
        ArrayList<JSONObject> output = new ArrayList<>();
        String method = "deleteVpcRequests";
        
        return output;
    }
    
    public static String makeNameFromUri(String uri) {
        //given a uri, returns a string that can be used to name a resource.
        //useful when a name for a resource is not available in model
        String output = removeChars(uri);
        return output.substring(output.length() - 60);
    }
    
    //Removes non-alphanumeric non-hyphen characters from strings
    //Similar to makeUriGcpCompatible from gcpModelBuilder, but it removes
    //instead of escaping offending characters.
    public static String removeChars (String input) {
        return input.replaceAll("[^a-zA-Z0-9\\-]", "");
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
