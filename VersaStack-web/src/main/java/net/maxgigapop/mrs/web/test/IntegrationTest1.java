/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.web.test;

import com.hp.hpl.jena.ontology.OntModel;
import static java.lang.Thread.sleep;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.bean.SystemInstance;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.system.*;

/**
 *
 * @author xyang
 */
@Singleton
@LocalBean
@Startup
public class IntegrationTest1 {
    private static final Logger logger = Logger.getLogger(IntegrationTest1.class.getName());

    @EJB
    HandleSystemCall systemCallHandler;

    private final String modelAdditionStr = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
"@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
"@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n" +
"@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n" +
"@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n" +
"<urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching>\n" +
"        mrs:providesSubnet        <urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching:crs-svc1> ."+
"<urn:ogf:network:domain=sdnx.maxgigapop.net:node=DRAGON:l2switching:crs-svc1>\n" +
"        a                         mrs:SwitchingSubnet , owl:NamedIndividual ;\n" +
"        nml:encoding              <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> ;\n" +
"        nml:hasBidirectionalPort  <urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*:vlan-3203> , <urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*:vlan-3203> ;\n" +
"        nml:labelSwapping         \"false\" .\n" +
"<urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*:vlan-3203>\n" +
"        a              nml:BidirectionalPort , owl:NamedIndividual ;\n" +
"        nml:hasLabel   <urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*:vlan-3203:label> .\n" +
"<urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*:vlan-3203>\n" +
"        a              nml:BidirectionalPort , owl:NamedIndividual ;\n" +
"        nml:hasLabel   <urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*:vlan-3203:label> .\n" +
"<urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*:vlan-3203:label>\n" +
"        a              nml:Label , owl:NamedIndividual ;\n" +
"        nml:labeltype  <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> ;\n" +
"        nml:value      \"3203\" .\n" +
"<urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*:vlan-3203:label>\n" +
"        a              nml:Label , owl:NamedIndividual ;\n" +
"        nml:labeltype  <http://schemas.ogf.org/nml/2012/10/ethernet#vlan> ;\n" +
"        nml:value      \"3203\" .";
    
    private final String modelReductionStr = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
"@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
"@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
"@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n" +
"@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n" +
"@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .";
    
    @PostConstruct
    public void testSystemPullAndPush1() {
        try {
            sleep(180000L);
            VersionGroup vg = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
            SystemInstance sysInstance = systemCallHandler.createInstance();
            SystemDelta sysDelta = new SystemDelta();
            sysDelta.setReferenceVersionGroup(vg);
            OntModel modelAddition = ModelUtil.unmarshalOntModel(modelAdditionStr);
            DeltaModel dmAddition = new DeltaModel();
            dmAddition.setCommitted(false);
            dmAddition.setDelta(sysDelta);
            dmAddition.setIsAddition(true);
            dmAddition.setOntModel(modelAddition);
            OntModel modelReduction = ModelUtil.unmarshalOntModel(modelReductionStr);
            DeltaModel dmReduction = new DeltaModel();
            dmReduction.setCommitted(false);
            dmReduction.setDelta(sysDelta);
            dmReduction.setIsAddition(false);
            dmReduction.setOntModel(modelReduction);
            sysDelta.setModelAddition(dmAddition);
            sysDelta.setModelReduction(dmReduction);
            systemCallHandler.propagateDelta(sysInstance, sysDelta);
            Future<String> asyncStatus = (Future<String>) systemCallHandler.commitDelta(sysInstance);
            while (!asyncStatus.isDone()) {
                sleep(10000);
            }
            logger.info("commit status="+asyncStatus.get());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}
