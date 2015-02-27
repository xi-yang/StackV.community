/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.driver.openstack.OpenStackModelBuilder;

/**
 *
 * @author xyang
 */
@Stateless
public class MCE_VMFilterPlacement implements IModelComputationElement {
    private static final Logger log = Logger.getLogger(MCE_VMFilterPlacement.class.getName());
    
    @Override
    @Asynchronous
    public Future<DeltaBase> process(ModelBase systemModel, DeltaBase annotatedDelta) {
        // $$ MCE_VMFilterPlacement deals with add model only for now.
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw new EJBException(String.format("%s::process ", this.getClass().getName()));
        }
        try {
            log.log(Level.INFO, "\n>>>MCE_VMFilterPlacement--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_MPVlanConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        String sparqlString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n" +
                "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n" +
                "prefix spa: <http://schemas.ogf.org/mrs/2015/02/spa#>\n" +
                "SELECT ?vm ?policy ?data ?type ?value WHERE {"
                + "?vm a nml:Node ."
                + "?vm spa:dependOn ?policy . "
                + "?policy a spa:Placement. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?type. ?data spa:value ?value. "
                + "FILTER not exists {?policy spa:dependOn ?other} "
                + "}";
        // TODO: sparql also needs to retrieve the VM Node that dependOn this action
        Query query = QueryFactory.create(sparqlString);
        List<Resource> listPolicy = null;
        QueryExecution qexec = QueryExecutionFactory.create(query, annotatedDelta.getModelAddition().getOntModel());
        ResultSet r = (ResultSet) qexec.execSelect();
        while(r.hasNext()) {
            QuerySolution querySolution = r.next();
            RDFNode nodePolicy = querySolution.get("policy");
            if (listPolicy == null)
            	listPolicy = new ArrayList<>();
            if (!listPolicy.contains(nodePolicy.asResource()))
                listPolicy.add(nodePolicy.asResource());
            RDFNode nodeVM = querySolution.get("vm");
            RDFNode nodeData = querySolution.get("data");
            RDFNode nodeDataType = querySolution.get("type");
            RDFNode nodeDataValue = querySolution.get("value");
        }
        
        //$$ clone outputDelta from annotatedDelta
        DeltaBase outputDelta = annotatedDelta; // TODO
        //$$ remove vm and policy and all down-tree statements
        //$$ do placement based on filter/match criteria *data*
        //$$ add *placed* Node with full up-tree (HypervisorService and Topology etc.) and connect to other elements
        //$$ TODO: do computation and create outputDelta
        
        return new AsyncResult(outputDelta);
    }
    
    // placeIntoTopology
    // placeIntoTopologyCandidates
    
    // placeMatchingRegExURI
    // placeMatchingRegExURICandidates
    
    // placeWithMultiFilter
}
