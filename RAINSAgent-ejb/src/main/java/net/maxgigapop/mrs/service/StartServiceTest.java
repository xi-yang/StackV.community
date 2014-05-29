/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.service;

import net.maxgigapop.mrs.bean.ModelBase;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.PersistenceManager;

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
        PersistenceManager.initialize(entityManager);
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
        ModelPersistenceManager.save(model1);
        ModelBase model2 = ModelPersistenceManager.find(ModelBase.class, model1.getId());
        List<ModelBase> listModels = ModelPersistenceManager.retrieveAll();
        System.out.println(listModels.toString());
    }
}
