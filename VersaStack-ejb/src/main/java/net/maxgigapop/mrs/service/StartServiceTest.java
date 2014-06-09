/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import net.maxgigapop.mrs.bean.ModelBase;

import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.PersistenceManager;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
@Startup
public class StartServiceTest {
    private @PersistenceContext(unitName="RAINSAgentPU")
    EntityManager entityManager;

    @PostConstruct
    public void init() {
        if (PersistenceManager.getEntityManager() == null) {
            PersistenceManager.initialize(entityManager);
        }
        ModelBase model1 = new ModelBase();
        model1.setTtlModel("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.\n" +
"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.\n" +
"@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.\n" +
"@prefix owl: <http://www.w3.org/2002/07/owl#>.\n" +
"@prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>.\n" +
"@prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>.\n"
                + "<http://www.maxgigapop.net/mrs/2013/topology#> a owl:Ontology;\n" +
"    rdfs:label \"NML-MRS Description of the MAX Research Infrastructure\".\n" +
"<urn:ogf:network:rains.maxgigapop.net:2013:topology>\n" +
"    a   nml:Topology,\n" +
"        owl:NamedIndividual;\n" +
"    nml:hasNode\n" +
"        <urn:ogf:network:rains.maxgigapop.net:2013:clpk-msx-1>,\n" +
"        <urn:ogf:network:rains.maxgigapop.net:2013:clpk-msx-4>.");
            /*
            ModelPersistenceManager.save(model1);
            ModelBase model2 = ModelPersistenceManager.find(ModelBase.class, model1.getId());
            List<ModelBase> listModels = ModelPersistenceManager.retrieveAll();
            System.out.println(listModels.toString());
            */
        try {
            Context ejbCxt = new InitialContext();
            HandleSystemCall systemCallHandler = (HandleSystemCall) ejbCxt.lookup("java:global/RAINSAgent-ear-1.0-SNAPSHOT/RAINSAgent-ejb-1.0-SNAPSHOT/HandleSystemCall");
            Map<String, String> driverProperties = new HashMap<>();
            driverProperties.put("topologyUri", "testdomain1.org");
            driverProperties.put("driverEjbPath", "java:global/RAINSAgent-ear-1.0-SNAPSHOT/RAINSAgent-ejb-1.0-SNAPSHOT/StubSystemDriver");
            driverProperties.put("stubModelTtl", model1.getTtlModel());
            systemCallHandler.plugDriverInstance(driverProperties);
            //systemCallHandler.unplugDriverInstance("testdomain1.org");
        } catch (NamingException ex) {
            Logger.getLogger(StartServiceTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
