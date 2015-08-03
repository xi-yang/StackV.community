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
import java.util.ArrayList;
import java.util.List;
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
        
        
        
        
        return requests;
    }
    
    public void pushCommit(String r) {

    }

}
