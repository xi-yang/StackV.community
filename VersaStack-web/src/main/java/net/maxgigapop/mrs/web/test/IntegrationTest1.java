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

  private final String modelAdditionStr = "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60>\n"
                + "        a                         nml:Node , owl:NamedIndividual ;\n"
                + "        mrs:hasVolume             <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> , <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be> ;\n"
                + "        mrs:providedByService     <urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1> ;\n"
                + "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> , <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e9>\n"
                + "        a               nml:Topology , owl:NamedIndividual ;\n"
                + "        nml:hasNode     <urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60> ;\n"
                + "        nml:hasService  <urn:ogf:network:aws.amazon.com:aws-cloud:rtb-864b05e3> , <urn:ogf:network:aws.amazon.com:aws-cloud:vpc-8c5f22e910.0.0.0/16> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:ec2service-us-east-1>\n"
                + "        a               mrs:HypervisorService , owl:NamedIndividual ;\n"
                + "        mrs:providesVM  <urn:ogf:network:aws.amazon.com:aws-cloud:i-4f93ec60> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:subnet-2cd6ad16>\n"
                + "        a               mrs:SwitchingSubnet , owl:NamedIndividual ;\n"
                + "        nml:hasBidirectionalPort  <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> , <urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b083> \n"
                + "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                + "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-b9f9b084> \n"
                + "        a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                + "        mrs:privateIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.231> ;\n"
                + "        mrs:publicIpAddress  <urn:ogf:network:aws.amazon.com:aws-cloud:54.152.72.205> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:ebsservice-us-east-1>\n"
                + "        a                   mrs:BlockStorageService , owl:NamedIndividual ;\n"
                + "        mrs:providesVolume  <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf> , <urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be> .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50bf>\n"
                + "        a          mrs:Volume , owl:NamedIndividual ;\n"
                + "        mrs:value  \"gp2\" ;\n"
                + "        mrs:target_device \"/dev/xvdba\" ;\n"
                + "        mrs:disk_gb \"8\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:vol-f05f50be>\n"
                + "        a          mrs:Volume , owl:NamedIndividual ;\n"
                + "        mrs:value  \"standard\" ;\n"
                + "        mrs:target_device \"/dev/xvda\" ;\n"
                + "        mrs:disk_gb \"8\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.230>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:value  \"10.0.0.230\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:10.0.0.231>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:value  \"10.0.0.231\" .\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:54.152.72.205>\n"
                + "        a          mrs:NetworkAddress , owl:NamedIndividual ;\n"
                + "        mrs:value  \"54.152.72.205\" .\n";




    
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
