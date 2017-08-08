<serviceDelta>
<uuid>{{uuid}}</uuid>

<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>

<modelAddition>

@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .
@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .
@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .
@prefix rdf:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .
@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .
@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .
@prefix spa:   &lt;http://schemas.ogf.org/mrs/2015/02/spa#&gt; .

&lt;urn:ogf:network:vo1.maxgigapop.net:link=abstract&gt;
	a            nml:Link ;
	spa:type     spa:Abstraction ;
	spa:dependOn &lt;x-policy-annotation:action:create-path&gt;.

&lt;x-policy-annotation:action:create-path&gt;
	a            spa:PolicyAction ;
    spa:type     "{{DNC-Type type}}" ;
	spa:importFrom &lt;x-policy-annotation:data:conn-criteria&gt; ;
	spa:exportTo &lt;x-policy-annotation:data:conn-criteriaexport&gt; .

&lt;x-policy-annotation:data:conn-criteria&gt;
	a            spa:PolicyData;
	spa:type     "JSON";
    spa:value    """{ {{~&gt;DNC-PolicyData}}    }""".

&lt;x-policy-annotation:data:conn-criteriaexport&gt;
    a            spa:PolicyData.

</modelAddition>
</serviceDelta>
