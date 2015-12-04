/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.Volume;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.ResourceTool;
import org.json.simple.JSONObject;

/**
 *
 * @author muzcategui
 */

/*
 ***********************************************
 Class to deal with bacthed resources, meaning there
 could me multiple resources that have the same 
 properties
 ***********************************************
 */
public class AwsBatchResourcesTool {

    private static final Logger log = Logger.getLogger(AwsBatchResourcesTool.class.getName());
    private AwsEC2Get ec2Client = null;

    public AwsBatchResourcesTool(String access_key_id, String secret_access_key, Regions region) {
        ec2Client = new AwsEC2Get(access_key_id, secret_access_key, region);
    }

    public AwsBatchResourcesTool() {

    }

    public Resource isbatched(OntModel model, Resource r) {
        Resource resource = null;
        String query = "SELECT ?batch WHERE {<" + r + "> mrs:hasBatch ?batch}";
        ResultSet r1 = executeQuery(query, model);
        if (r1.hasNext()) {
            QuerySolution q1 = r1.next();
            RDFNode p = q1.get("batch");
            resource = p.asResource();
        }
        return resource;
    }

    public Resource getBatchedResource(OntModel model, Resource r, String nameRule) {
        String resource = r.toString();
        resource += nameRule;
        System.out.println(resource);
        return model.getResource(resource);
    }

    public int getNumberBatched(OntModel model, Resource r) {
        int i = 0;
        String query = "SELECT ?number WHERE {<" + r + "> mrs:value ?number}";
        ResultSet r1 = executeQuery(query, model);
        if (r1.hasNext()) {
            QuerySolution q1 = r1.next();
            RDFNode number = q1.get("number");
            Literal literal = number.asLiteral();
            i = Integer.parseInt(literal.toString());
        }
        return i;
    }

    /*
     *************************************************************************
     Method to modify a model containing batches into specific resources, this
     method provides an exact representation of a model after all the batched 
     resources have been explicitly specified in the model
     *************************************************************************
     */
    public OntModel expandBatchAbstraction(OntModel model) throws Exception {
        String query;
        OntModel modelCopy = model;
        List<Resource> resourcesToDelete = new ArrayList();

        ModelUtil.marshalOntModel(modelCopy);
        ModelUtil.marshalOntModel(model);

        //query for the batches
        query = "SELECT ?r ?n ?batch WHERE {?r mrs:hasBatch ?batch ."
                + "?batch mrs:value ?n}";
        ResultSet r1 = executeQuery(query, model);
        OntModel tmp = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        //create the new model with the batch resources 
        while (r1.hasNext()) {
            QuerySolution q1 = r1.next();
            int n = Integer.parseInt(q1.get("n").asLiteral().toString());
            String name = q1.get("r").asResource().toString();
            Resource resourceCopy = model.getResource(name);
            resourcesToDelete.add(resourceCopy);
            resourcesToDelete.add(q1.getResource("batch").asResource());

            //insert into the copy model all the batches identified by number
            for (int i = 1; i <= n; i++) {
                //create a new bacth batchResource
                String resourceName = name + "batch" + Integer.toString(i);
                Resource newResource = tmp.getResource(resourceName);
                if (newResource == null) {
                    newResource = tmp.createResource(resourceName);
                }
                StmtIterator iterator = resourceCopy.listProperties();
                while (iterator.hasNext()) {
                    Statement next = iterator.next();
                    Property property = next.getPredicate();
                    RDFNode object = next.getObject();
                    if (object.isResource()) {
                        //if the object is a batchResource, this batchResource could be batched too, in this case we have to scenarios
                        //new batch batchResource to single batchResource and new batch batchResource to batch
                        query = "SELECT ?x WHERE {<" + object.asResource().toString() + "> mrs:hasBatch ?x}";
                        ResultSet r2 = executeQuery(query, model);
                        if (!r2.hasNext()) { //case of new batch batchResource to single Resource
                            tmp.add(tmp.createStatement(newResource, next.getPredicate(), object.asResource()));
                        } else { //case of  new batch batchResource to batch batchResource
                            String re = object.asResource().toString() + "batch" + Integer.toString(i);
                            Resource childResource = tmp.getResource(re);
                            if (childResource == null) {
                                childResource = tmp.createResource(re);
                            }
                            tmp.add(tmp.createStatement(newResource, next.getPredicate(), childResource));
                        }
                    } else {
                        tmp.add(tmp.createStatement(newResource, next.getPredicate(), object.asLiteral()));
                    }

                }

                //remove the hasBatch property
                tmp.remove(newResource.getProperty(Mrs.hasBatch));

                //get the objects on top of this batchResource
                query = "SELECT ?parent ?property WHERE {?parent ?property <" + resourceCopy + ">}";
                ResultSet r2 = executeQuery(query, model);
                while (r2.hasNext()) {
                    //if you find the parent and the parent has a batch, link new batch resources with parent's batch resources 
                    QuerySolution q2 = r2.next();
                    String p = q2.get("property").asResource().toString();
                    Property property = tmp.createProperty(p);
                    Resource parent = q2.getResource("parent").asResource();
                    //if the object is a batchResource, this batchResource could be batched too, in this case we have to scenarios
                    //new batch batchResource to single batchResource and new batch batchResource to batch
                    query = "SELECT ?x WHERE {<" + parent.asResource().toString() + "> mrs:hasBatch ?x}";
                    ResultSet r3 = executeQuery(query, model);
                    if (!r3.hasNext()) { //case of parent batchResource to new batch batchResource 
                        tmp.add(tmp.createStatement(parent, property, newResource));
                    } else { //case of  new batch batchResource to batch batchResource
                        String re = parent.asResource().toString() + "batch" + Integer.toString(i);
                        Resource parentResource = tmp.getResource(re);
                        if (parentResource == null) {
                            parentResource = tmp.createResource(re);
                        }
                        tmp.add(tmp.createStatement(parentResource, property, newResource));
                    }
                }
            }
        }

        //clean up every old batchResource that had batch and its corresponding batch statement
        for (Resource r : resourcesToDelete) {
            model.removeAll(r, null, null);
            model.removeAll(null, null, r);
        }

        modelCopy.add(tmp.listStatements().toList());
        return modelCopy;
    }

    /*
     *************************************************************************
     Method to modify a model containing explicit resources into batches, this
     method provides an abstract representation of a model after all the explicit
     resources are unified into batch resources if they are part of a batch
     *************************************************************************
     */
    public OntModel contractVMbatch(OntModel model) {
        Map<String, String> batches = new HashMap<>();
        JSONObject batchNumber = new JSONObject();

        //look for all Nodes in the model and contract the batches
        String query = "SELECT ?node WHERE {?node a nml:Node}";
        ResultSet r = executeQuery(query, model);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            String nodeTag = ResourceTool.getResourceName(q.get("node").asResource().toString(), AwsPrefix.instance);
            String nodeId = ec2Client.getInstanceId(nodeTag);
            Instance node = ec2Client.getInstance(nodeId);
            String tagValue = ec2Client.getTagValue("batch", node.getTags());
            if (tagValue != null) {
                if (batches.get(nodeTag) == null) {
                    batches.put(nodeTag, tagValue);
                }
                if (batchNumber.get(tagValue) == null) {
                    batchNumber.put(tagValue, "0");
                }
                String number = Integer.toString(Integer.parseInt(batchNumber.get(tagValue).toString()) + 1);
                batchNumber.remove(tagValue);
                batchNumber.put(tagValue, number);
            }
        }

        //look for all nics in the model abd contract the batches
        query = "SELECT ?port WHERE {?port a nml:BidirectionalPort ."
                + "?subnet a mrs:SwitchingSubnet ."
                + "?subnet  nml:hasBidirectionalPort ?port ."
                + "FILTER (NOT EXISTS {?port mrs:type ?type})}";
        r = executeQuery(query, model);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            String portTag = ResourceTool.getResourceName(q.get("port").asResource().toString(), AwsPrefix.nic);
            String portId = ec2Client.getResourceId(portTag);
            NetworkInterface port = ec2Client.getNetworkInterface(portId);
            String tagValue = ec2Client.getTagValue("batch", port.getTagSet());
            if (tagValue != null) {
                if (batches.get(portTag) == null) {
                    batches.put(portTag, tagValue);
                }
                if (batchNumber.get(tagValue) == null) {
                    batchNumber.put(tagValue, "0");
                }
                String number = Integer.toString(Integer.parseInt(batchNumber.get(tagValue).toString()) + 1);
                batchNumber.remove(tagValue);
                batchNumber.put(tagValue, number);
            }
        }

        //look for all nics in the model abd contract the batches
        query = "SELECT ?volume WHERE {?volume a mrs:Volume}";
        r = executeQuery(query, model);
        while (r.hasNext()) {
            QuerySolution q = r.next();
            String volumeTag = ResourceTool.getResourceName(q.get("volume").asResource().toString(), AwsPrefix.volume);
            String volumeId = ec2Client.getVolumeId(volumeTag);
            Volume vol = ec2Client.getVolume(volumeId);
            String tagValue = ec2Client.getTagValue("batch", vol.getTags());
            if (tagValue != null) {
                if (batches.get(volumeTag) == null) {
                    batches.put(volumeTag, tagValue);
                }
                if (batchNumber.get(tagValue) == null) {
                    batchNumber.put(tagValue, "0");
                }
                String number = Integer.toString(Integer.parseInt(batchNumber.get(tagValue).toString()) + 1);
                batchNumber.remove(tagValue);
                batchNumber.put(tagValue, number);
            }
        }

        //return if there is nothing to change
        if (batches.isEmpty()) {
            return model;
        }

        //get a list of all the statements and put the batches
        List<Statement> stmtsToRemove = new ArrayList<>();
        List<Statement> stmtsToAdd = new ArrayList<>();
        StmtIterator statements = model.listStatements();
        while (statements.hasNext()) {
            Statement st = statements.next();
            Resource resource = st.getSubject();
            RDFNode object = st.getObject();
            Property property = st.getPredicate();

            if (resource != null && batches.containsKey(resource.toString())) {
                String resourceUri = batches.get(resource.toString()).replace("batch", "");
                resource = model.getResource(resourceUri);
                //model.remove(st);
                //model.add(model.createStatement(resource, property, object));
                stmtsToRemove.add(st);
                stmtsToAdd.add(model.createStatement(resource, property, object));
            }

            if (object != null && object.isResource() && batches.containsKey(object.asResource().toString())) {
                String objectUri = batches.get(object.asResource().toString()).replace("batch", "");
                object = model.getResource(objectUri);
                //model.remove(st);
                //model.add(model.createStatement(resource, property, object));
                stmtsToRemove.add(st);
                stmtsToAdd.add(model.createStatement(resource, property, object));
            }
        }

        if (!stmtsToRemove.isEmpty()) {
            model.remove(stmtsToRemove);
        }
        if (!stmtsToAdd.isEmpty()) {
            model.add(stmtsToAdd);
        }

        //create batch statements and delete all explicit resources
        for (String key : batches.keySet()) {
            //remove old resources, this will make sure deletion was succesfull
            Resource oldResource = model.getResource(key);
            model.removeAll(oldResource, null, null);
            model.removeAll(null, null, oldResource);

            //create batch statements
            Resource baseResource = model.getResource(batches.get(key).replace("batch", ""));
            Resource batch = RdfOwl.createResource(model, batches.get(key), Mrs.Batch);
            model.add(model.createStatement(baseResource, Mrs.hasBatch, batch));
            model.add(model.createStatement(batch, Mrs.BatchRule, "numbered"));
            model.add(model.createStatement(batch, Mrs.value, batchNumber.get(batches.get(key)).toString()));
        }

        //temporal hack
        //TODO improve this part
        //eliminate the network addresses of those network interfaces that are batches
        for (Object key : batchNumber.keySet()) {
            Resource baseResource = model.getResource(key.toString().replace("batch", ""));
            Statement st = model.getProperty(baseResource, Mrs.hasNetworkAddress);
            while (st !=null){
               Resource object = st.getObject().asResource();
               model.removeAll(object,null,null);
               model.removeAll(null,null,object);
               st = model.getProperty(baseResource, Mrs.hasNetworkAddress);
            }

        }

        //add the batch statements
        //replace all ocurrences of the nodes
        return model;
    }

    /**
     * ****************************************************************
     * function that executes a query using a model addition/subtraction and a
     * reference model, returns the result of the query
     * ****************************************************************
     */
    private ResultSet executeQuery(String queryString, OntModel refModel) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        //do the query in SparkQL syntax
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, refModel);
        ResultSet r = qexec.execSelect();

        return r;
    }
}
