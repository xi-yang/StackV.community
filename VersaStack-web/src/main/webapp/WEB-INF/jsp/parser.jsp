<%@page contentType="application/json; charset=UTF-8"%>
<%@page import="com.hp.hpl.jena.ontology.OntModel"%>
<%@page import="com.hp.hpl.jena.rdf.model.*"%>
<%
    String in = application.getRealPath("/WEB-INF/data/max-nml-mrs-v1.ttl");

    // read the turtle file into a model
    OntModel model = ModelFactory.createOntologyModel();
    model.read(in, "TURTLE");

    // write model as JSON
    model.write(out, "RDF/JSON");
%>
