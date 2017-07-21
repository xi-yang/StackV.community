<urn:ogf:network:service+{{ UUID }}:resource+virtual_clouds:tag+{{name}}>
    a                         nml:Topology ;
    spa:dependOn <x-policy-annotation:action:create-{{name}}>, {{! find condition for this <x-policy-annotation:action:create-dc1>}}.

<x-policy-annotation:data:{{name}}-export>
    a            spa:PolicyData ;
    spa:type     "JSON" ;
    spa:format   """{
       "parent":"{{parent}}",
       "stitch_from": "%$.gateways[?(@.type=='vpn-gateway')].uri%",
    }""" .

<x-policy-annotation:data:{{name}}-subnet0-vm-criteria>
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:format    """{
       "place_into": "%$.subnets[0].uri%"
    }""" .

{{#subnets}}
{{#virtual_machines}}
<urn:ogf:network:service+{{ UUID }}:resource+virtual_machines:tag+{{name}}>
    a                         nml:Node ;
    nml:name         "{{name}}";
    mrs:type       "{{type}}";
    nml:hasBidirectionalPort   <urn:ogf:network:service+{{ UUID }}:resource+virtual_machines:tag+{{name}}:eth0> ;
    spa:dependOn <x-policy-annotation:action:create-{{name}}>.

<urn:ogf:network:service+{{ UUID }}:resource+virtual_machines:tag+{{name}}:eth0>
    a            nml:BidirectionalPort;
    spa:dependOn <x-policy-annotation:action:create-{{name}}>.


{{/virtual_machines}}
{{/subnets}}
<x-policy-annotation:action:create-{{subnets/virtual_machines/name}}>
    a            spa:PolicyAction ;
    spa:type     "MCE_VMFilterPlacement" ;
    spa:dependOn <x-policy-annotation:action:create-{{name}}> ;
    spa:importFrom <x-policy-annotation:data:{{name}}-subnet0-vm-criteria> .

<x-policy-annotation:action:create-{{name}}>
    a            spa:PolicyAction ;
    spa:type     "MCE_VirtualNetworkCreation" ;
    spa:importFrom <x-policy-annotation:data:{{name}}-criteria> ;
    spa:exportTo <x-policy-annotation:data:{{name}}-export>,<x-policy-annotation:data:{{name}}-subnet0-vm-criteria> .

<x-policy-annotation:data:{{name}}-criteria>
    a            spa:PolicyData;
    spa:type     nml:Topology;
    spa:value    """{{PolicyData .}}""".

