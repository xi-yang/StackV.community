/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compute;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
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
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.service.compile.CompilerBase;
import net.maxgigapop.mrs.service.compile.CompilerFactory;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
 
/**
 *
 * @author aheard
 */
@Stateless
public class MCE_OperationalModelModification implements IModelComputationElement {

    private static final StackLogger logger = new StackLogger(MCE_OperationalModelModification.class.getName(), "MCE_OperationalModelModification");

    JSONParser parser = new JSONParser();

    @Override
    @Asynchronous
    public Future<ServiceDelta> process(Resource policy, ModelBase systemModel, ServiceDelta annotatedDelta) {
        String method = "process";
        logger.refuuid(annotatedDelta.getReferenceUUID());
        logger.start(method);
        if (annotatedDelta.getModelAddition() == null || annotatedDelta.getModelAddition().getOntModel() == null) {
            throw logger.error_throwing(method, "target:ServiceDelta has null addition model");
        }
        try {
            logger.trace(method, "DeltaAddModel Input=\n" + ModelUtil.marshalOntModel(annotatedDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(annotatedDelta.additionModel) -exception-"+ex);
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
                
        JSONArray resourcesToRemove = null;
        try {
            resourcesToRemove = (JSONArray)parser.parse(inputString);
            if (resourcesToRemove == null)throw new Exception();
        } catch ( Exception ex) {
            throw logger.throwing(method, "cannot parse json string "+inputString, ex);
        } 
        
        Model subModel = ModelFactory.createDefaultModel();
        OntModel model = systemModel.getOntModel();
        
        List<Resource> resources = new ArrayList<>();
        List<String> includeMatches = new ArrayList<String>();
        List<String> excludeMatches = new ArrayList<String>();
        List<String> excludeExtentials = new ArrayList<String>();
        
        CompilerBase simpleCompiler = CompilerFactory.createCompiler("net.maxgigapop.mrs.service.compile.SimpleCompiler");
  
        for (int i = 0; i < resourcesToRemove.size(); i++) {
            String resourceURI = (String) resourcesToRemove.get(i);
            if (isTopLevelTopology(systemModel.getOntModel(), resourceURI) || isTopLevelService(systemModel.getOntModel(), resourceURI)) {
                throw new UnsupportedOperationException("MCE_OperationalModelModification::Cannot delete top level service or topology from model. ");
            }
            Resource node =  systemModel.getOntModel().getOntResource(resourceURI);
            if (node != null) {
                try {
                    subModel.add(simpleCompiler.listUpDownStatements(systemModel.getOntModel(), node));
                } catch (Exception ex) {
                    throw logger.error_throwing(method, String.format("listUpDownStatements(%s) -exception- %s", node, ex));
                }
                resources.add(node);
            } else {
                throw new NullPointerException("MCE_OperationalModelModification:: Resource cannot be null.");
            }
        }
        subModel.add(ModelUtil.getModelSubTree(systemModel.getOntModel(), resources, includeMatches, excludeMatches, excludeExtentials)); 
       
        ServiceDelta outputDelta = new ServiceDelta();
        DeltaModel dmReduction = new DeltaModel();
        dmReduction.setOntModel(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF));
        dmReduction.getOntModel().add(subModel);
        outputDelta.setModelReduction(dmReduction);
        
        try {
            logger.trace(method, "DeltaAddModel Output=\n" + ModelUtil.marshalOntModel(outputDelta.getModelAddition().getOntModel()));
        } catch (Exception ex) {
            logger.trace(method, "marshalOntModel(outputDelta.additionModel) -exception-"+ex);
        }
        logger.end(method);        
        return new AsyncResult(outputDelta);
    }   
        
    public boolean isTopLevelTopology(OntModel systemModel, String URI) {
        String sparql = "SELECT ?resource WHERE {"   
               + "{?resource a nml:Topology.} UNION {?resource a nml:Node.}" 
               + "MINUS { {?parent nml:hasNode ?resource.} UNION {?parent nml:hasTopology ?resource.}}" 
               + String.format("FILTER ( ?resource = <%s>)", URI)                
               + "}";
        
        ResultSet r = ModelUtil.sparqlQuery(systemModel, sparql);
        return r.hasNext();
    }
    
    public boolean isTopLevelService(OntModel systemModel, String URI) {
        String sparql = "SELECT ?resource WHERE {" 
           + " {" 
           +"  	{{?parent nml:hasService ?resource} UNION {?parent nml:providesService ?resouce.}}" 
           +" 	MINUS {{?other nml:hasNode ?parent.} UNION {?other nml:hasTopology ?parent.}}" 
           +"  }" 
           + String.format("FILTER ( ?resource = <%s>)", URI)                
           + "}";
        
        ResultSet r = ModelUtil.sparqlQuery(systemModel, sparql);
        return r.hasNext();   
    }
}
 