/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.rdf.model.Resource;
import java.util.concurrent.Future;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.ServiceDelta;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;
import net.maxgigapop.mrs.common.Spa;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.net.util.SubnetUtils;
 
/**
 *
 * @author aheard
 */
@Stateless
public class MCE_OperationalModelModification implements IModelComputationElement {

    private static final Logger log = Logger.getLogger(MCE_OperationalModelModification.class.getName());
    JSONParser parser = new JSONParser();

    @Override
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw new EJBException(String.format("%s::process ", this.getClass().getName()));
        }
        try {
            log.log(Level.FINE, "\n>>>MCE_OperationalModelModification--DeltaAddModel=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            Logger.getLogger(MCE_OperationalModelModification.class.getName()).log(Level.SEVERE, null, ex);
        }

        // importPolicyData
        String sparql = "SELECT ?policy ?data ?dataType ?dataValue WHERE {"
                + "?policy a spa:PolicyAction. "
                + "?policy spa:type 'MCE_OperationalModelModification'. "
                + "?policy spa:importFrom ?data. "
                + "?data spa:type ?dataType. ?data spa:value ?dataValue. "
                + String.format("FILTER (not exists {?policy spa:dependOn ?other} && ?policy = <%s>)", policy.getURI())
                + "}";
        Map<Resource, List> policyMap = new HashMap<>();
        ResultSet r = ModelUtil.sparqlQuery(annotatedDelta.getModelAddition().getOntModel(), sparql);
        RDFNode jsonInput = null;
        RDFNode policy1 = null;
        RDFNode policy2 = null;
        while (r.hasNext()) {
            QuerySolution querySolution = r.next();
            jsonInput  = querySolution.get("dataValue");
            policy1 = querySolution.get("policy");
        }
        
        String inputString = jsonInput.toString();
        
//        JSONObject inputJSON = new JSONObject();
//        try {
//            Object obj = parser.parse(inputString);
//            inputJSON = (JSONObject) obj;
//
//            System.out.println("Service API:: inputJSON: " + inputJSON.toJSONString());
//        } catch (ParseException ex) {
//            Logger.getLogger(MCE_OperationalModelModification.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        ServiceDelta outputDelta = annotatedDelta.clone();
        JSONArray toRemove = null;
        try {
            toRemove = (JSONArray)parser.parse(inputString);
        } catch (ParseException ex) {
            Logger.getLogger(MCE_OperationalModelModification.class.getName()).log(Level.SEVERE, null, ex);
        }
        Model subModel = ModelFactory.createDefaultModel();
        OntModel model = systemModel.getOntModel();
   //"#rdfDFS(Model refModel, RDFNode node, Set<RDFNode> visited, Model subModel, List<String> propMatchIncludes, List<String> propMatchExcludes, List<String> propExcludeEssentials) #
        for (int i = 0; i < toRemove.size(); i++) {
            String resourceURI = (String) toRemove.get(i);
            Resource node =  systemModel.getOntModel().getOntResource(resourceURI);
            List<String> includeMatches = new ArrayList<String>();
            List<String> excludeMatches = new ArrayList<String>();
            List<String> excludeEssentials = new ArrayList<String>();
            Set<RDFNode> visited = new HashSet<RDFNode>();
          // need better way to do this 
            //ModelUtil.rdfDFS(systemModel.getOntModel(), node, visited, subModel, includeMatches, excludeMatches, excludeMatches);  
            // perhaps go down as wel
            //MCETools.removeResolvedAnnotation(outputDelta.getModelReduction().getOntModel(), node);
        }
        
      //  MCETools.removeResolvedAnnotation(outputDelta.getModelReduction().getOntModel(), policy1.asResource());
//        Model newMA = outputDelta.getModelAddition().getOntModel().difference(subModel);
//        Model newMR = subModel;
//        outputDelta.getModelAddition().getOntModel().add(newMA);
        //outputDelta.getModelReduction().getOntModel().add(subModel);
        return new AsyncResult(outputDelta);
    }
   

}
