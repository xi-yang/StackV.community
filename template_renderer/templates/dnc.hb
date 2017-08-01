<serviceDelta>
<uuid>{{uuid}}</uuid>

<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>

<modelAddition>

@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl:   <http://www.w3.org/2002/07/owl#> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .
@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .
@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .
@prefix spa:   <http://schemas.ogf.org/mrs/2015/02/spa#> .

<urn:ogf:network:vo1.maxgigapop.net:link=abstract>
	a            nml:Link ;
	spa:type            spa:Abstraction ;
	spa:dependOn <x-policy-annotation:action:create-path>.

<x-policy-annotation:action:create-path>
	a            spa:PolicyAction ;
	spa:type     "MCE_MPVlanConnection" ;
	spa:importFrom <x-policy-annotation:data:conn-criteria> ;
	spa:exportTo <x-policy-annotation:data:conn-criteriaexport> .

<x-policy-annotation:data:conn-criteria>
	a            spa:PolicyData;
	spa:type     "JSON";
    spa:value    """{{>getLinks}}""".

<x-policy-annotation:data:conn-criteriaexport>
    a            spa:PolicyData.

</modelAddition>
</serviceDelta>
