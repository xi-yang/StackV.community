<%@page import="com.hp.hpl.jena.rdf.model.ModelFactory"%>
<%@page import="com.hp.hpl.jena.ontology.OntModel"%>
<%@page contentType="application/json; charset=UTF-8"%>
<%
    String in = application.getRealPath("/data/max-nml-mrs-v1-full.ttl");

    // read the turtle file into a model
    OntModel model = ModelFactory.createOntologyModel();
    model.read(in, "TURTLE");

    // write model as JSON
    model.write(out, "RDF/JSON");
%>
