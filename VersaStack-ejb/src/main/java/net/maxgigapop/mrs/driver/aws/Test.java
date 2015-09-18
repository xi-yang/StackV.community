/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.aws;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.common.Nml;
import net.maxgigapop.mrs.common.RdfOwl;

/**
 *
 * @author max
 */
public class Test {

    public static void main(String[] args) throws Exception {
        String modelttl = "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .\n"
                + "@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .\n"
                + "\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-123456>        \n"
                + "	a                     nml:BidirectionalPort , owl:NamedIndividual ;\n"
                + "        mrs:hasTag            <urn:ogf:network:aws.amazon.com:aws-cloud:portTag> ;        \n"
                + "	mrs:hasBatch	      <urn:ogf:network:aws.amazon.com:aws-cloud:eni-123456batch> .\n"
                + "\n"
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:eni-123456batch>\n"
                + "	a		      mrs:Batch ;\n"
                + "        mrs:value 	      \"10\" ;\n"
                + "	mrs:batch_rule        \"numbered\" ."
                + "<urn:ogf:network:aws.amazon.com:aws-cloud:subnet-2cd6ad16>\n"
                + "	nml:hasBidirectionalPort <urn:ogf:network:aws.amazon.com:aws-cloud:eni-123456> ";
        
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        //$$ TODO: add ontology schema and namespace handling code
        /*try {
            model.read(new ByteArrayInputStream(modelttl.getBytes()), null, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
        }
        BatchResourcesTool batch = new BatchResourcesTool();
        Model m = batch.addBatchToModel(model);
        
        StringWriter out = new StringWriter();
        try {
            m.write(out, "TURTLE");
        } catch (Exception e) {
            throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
        }
        String ttl = out.toString();
        
        //System.out.println(ttl);*/
    }

}
