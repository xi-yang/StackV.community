package net.maxgigapop.mrs.common;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.*;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author xyang
 */
public class RdfOwl {

    private static Model m_model = ModelFactory.createDefaultModel();

    public static final String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static String getRdfURI() {
        return rdfNS;
    }

    public static final String rdfsNS = "http://www.w3.org/2000/01/rdf-schema#";

    public static String getRdfsURI() {
        return rdfsNS;
    }

    public static final String owlNS = "http://www.w3.org/2002/07/owl#";

    public static String getOwlURI() {
        return owlNS;
    }

    public static final String xsdNS = "http://www.w3.org/2001/XMLSchema#";

    public static String getXsdURI() {
        return xsdNS;
    }

    public static final Property type = m_model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    public static final Resource NamedIndividual = m_model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual");
    public static final Resource labelTypeVLAN = m_model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan");

    public static Resource createResource(OntModel model, String uri, Resource type) {
        Resource res = model.createResource(uri);
        //model.add(model.createStatement(res, RdfOwl.type, RdfOwl.NamedIndividual));
        model.add(model.createStatement(res, RdfOwl.type, type));
        return res;
    }
    public static Resource createResourceUnverifiable(OntModel model, String uri, Resource type) {
        Resource res = model.createResource(uri);
        model.add(model.createStatement(res, RdfOwl.type, type));
        model.add(model.createStatement(res, Mrs.type, "unverifiable"));
        return res;
    }
}
