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

&lt;x-policy-annotation:data:vpc-criteria&gt;
    a            spa:PolicyData ;
    spa:type     "JSON" ;
    spa:value    """{{topologyPolicyData @root}}""".

&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_clouds:tag+vpc1&gt;
    a                         nml:Topology ;
{{#if_directConnect gateways}}
    {{#gateways}}
    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt;, &lt;x-policy-annotation:action:create-mce_dc1&gt; .

&lt;urn:ogf:network:vo1_maxgigapop_net:link=conn1&gt;
    a            mrs:SwitchingSubnet;
    spa:type     spa:Abstraction;
    spa:dependOn &lt;x-policy-annotation:action:create-path&gt;.

&lt;x-policy-annotation:action:create-path&gt;
    a            spa:PolicyAction ;
    spa:type     "MCE_MPVlanConnection" ;
    spa:importFrom &lt;x-policy-annotation:data:conn-criteria1&gt; ;
    spa:exportTo &lt;x-policy-annotation:data:conn-export&gt; .

&lt;x-policy-annotation:action:create-mce_dc1&gt;
    a            spa:PolicyAction ;
    spa:type     "MCE_AwsDxStitching" ;
    spa:importFrom &lt;x-policy-annotation:data:vpc-export&gt;, &lt;x-policy-annotation:data:conn-export&gt; ;
    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt;, &lt;x-policy-annotation:action:create-path&gt;.

&lt;x-policy-annotation:data:vpc-export&gt;
    a            spa:PolicyData ;
    spa:type     "JSON" ;
    spa:format   """{{PolicyData parent=../parent stitch_from="%%$.gateways[?(@.type=='vpn-gateway')].uri%%"}}""".

&lt;x-policy-annotation:data:conn-export&gt;
    a            spa:PolicyData;
    spa:type     "JSON" ;
    spa:format   """{
        "to_l2path": %%$.urn:ogf:network:vo1_maxgigapop_net:link=conn1%%
    }""" .

&lt;x-policy-annotation:data:conn-criteria1&gt;
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:value    """{
        "urn:ogf:network:vo1_maxgigapop_net:link=conn1": {
            "{{directConnDest connects.[0].to}}": {
                "vlan_tag":"{{directConnVlan connects.[0].to}}"
            },
            "{{../parent}}": {
                "vlan_tag":"{{directConnVlan connects.[0].to}}"
            }
        }
    }""".
    {{/gateways}}
    {{else}}
    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt; .
{{/if_directConnect}}
{{#if_aws @root}} {{! AWS }}
    {{#subnets}}
        {{#vms}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}&gt;
    a           nml:Node ;
    nml:name    "{{name}}";
    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}:eth0&gt;;
    spa:dependOn &lt;x-policy-annotation:action:create-{{name}}&gt; .

&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}:eth0&gt;
    a           nml:BidirectionalPort;
    spa:dependOn &lt;x-policy-annotation:action:create-{{name}}&gt;
{{addressString name interfaces @root.uuid}}

&lt;x-policy-annotation:action:create-{{name}}&gt;
    a            spa:PolicyAction ;
    spa:type     "MCE_VMFilterPlacement" ;
    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt; ;
    spa:importFrom  &lt;x-policy-annotation:data:vpc-subnet-{{name}}-criteria&gt;.

&lt;x-policy-annotation:data:vpc-subnet-{{name}}-criteria&gt;
    a           spa:PolicyData;
    spa:type    "JSON";
    spa:format  """{
        "place_into": "%%$.subnets[{{@index}}].uri%%"}""" .
        {{/vms}}
    {{/subnets}}
{{/if_aws}}

{{#if_ops @root}} {{! OPS }}
    {{#subnets}}
        {{#vms}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}&gt;
    a                         nml:Node ;
    nml:name         "{{name}}";
            {{#if type}}
    mrs:type    "internal"; {{!TODO includes other pieces (image, etc) }}
            {{/if}}
    nml:hasBidirectionalPort   &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}:eth0&gt; ;
            {{#if routes}}
    nml:hasService  &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}:routingservice&gt; ;
            {{/if}}
    spa:dependOn &lt;x-policy-annotation:action:create-{{name}}&gt;.

&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}:eth0&gt;
    a            nml:BidirectionalPort;
    spa:dependOn &lt;x-policy-annotation:action:create-{{name}}-eth0&gt;
            {{#if interfaces }}
;
{{addressString name interfaces @root.uuid}}  {{! check on conditional relating to blank addressString }}
            {{#sriovs}}
                {{#each @root.gateways}}
                    {{#if_eq name ../hosting_gateway}}
                        {{#if_eq type 'UCS Port Profile'}}
                        {{log this}}
&lt;x-policy-annotation:action:ucs-sriov-stitch-external-{{../../name}}-sriov{{@../index}}&gt;
    a            spa:PolicyAction ;
    spa:type     "MCE_UcsSriovStitching" ;
    spa:dependOn &lt;x-policy-annotation:action:create-{{../../name}}&gt;, &lt;x-policy-annotation:action:create-{{../../name}}-eth0&gt;;
    spa:importFrom &lt;x-policy-annotation:data:sriov-criteria-external-{{../../name}}-sriov{{@../index}}&gt;.

&lt;x-policy-annotation:data:sriov-criteria-external-{{../../name}}-sriov{{@../index}}&gt;
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:value    """ {
        "stitch_from": "urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../../name}}",
        "to_port_profile": "{{connects.0.from}}",
        "mac_address": "{{../mac_address}}"
        {{sriovIP ../ip_address}}
                                {{#if connects}}
        ,   "routes" : {{toJSON connects}} {{!TODO check if desired format/data, can be changed to connects.[0] }}
                                {{/if}}
        } """ .
                        {{/if_eq}}
                        {{#if_eq type 'L2 Stitch Port'}}
&lt;x-policy-annotation:action:ucs-{{../../name}}-sriov{{@../index}}-stitch&gt;
    a            spa:PolicyAction ;
    spa:type     "MCE_UcsSriovStitching" ;
    spa:dependOn &lt;x-policy-annotation:action:create-{{../../name}}&gt;, &lt;x-policy-annotation:action:create-path&gt;;
    spa:importFrom &lt;x-policy-annotation:data:{{../../name}}-sriov{{@../index}}-criteria&gt; .

&lt;x-policy-annotation:data:{{../../name}}-sriov{{@../index}}-criteria&gt;
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:format   """ {
       "stitch_from": "urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../../name}}", {{!TODO ensure every reference to 'name' is in correct context}}
       "to_l2path": %%$.urn:ogf:network:vo1_maxgigapop_net:link=conn{{../name}}%%,
       "mac_address": "{{sriovMac ../address}}"
        {{sriovIP ../address}}
                                {{#if connects}}
        ,   "routes" : {{toJSON connects}} {{!TODO same as above }}
                                {{/if}}
        } """ .
                        {{/if_eq}}
                    {{/if_eq}}
                {{/each}}
            {{/sriovs}}
            {{else}}
.
            {{/if}}
            {{#if routes}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}:routingservice&gt;
     a   mrs:RoutingService;
     mrs:providesRoutingTable     &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}:routingservice:routingtable+linux&gt; .
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}:routingservice:routingtable+linux&gt;
     a   mrs:RoutingTable;
     mrs:type   "linux";
     mrs:hasRoute
                {{#routes}}
     {{#unless @first}},{{/unless}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:routingservice:routingtable+linux:route+{{add @index 1}}&gt;
     {{#if @last}}.{{/if}}
                {{/routes}}
                {{#routes}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:routingservice:routingtable+linux:route+{{add @index 1}}&gt;
      a  mrs:Route;
                    {{#if to}}
      mrs:routeTo "{{to}}"; {{!might have to format this differently, check networkAddressFromJson method}}
                    {{/if}}
                    {{#if from}}
      mrs:routeFrom "{{from}}"; {{!might have to format this differently, check networkAddressFromJson method}}
                    {{/if}}
                    {{#if next_hop}}
      mrs:nextHop "{{next_hop}}"; {{!might have to format this differently, check networkAddressFromJson method}}
                    {{/if}}
.
                {{/routes}}
            {{/if}}

            {{#if ceph_rbd}}
                {{#ceph_rbd}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:volume+ceph{{@index}}&gt;
   a  mrs:Volume;
   mrs:disk_gb "{{disk_gb}}";
   mrs:mount_point "{{mount_point}}".
                {{/ceph_rbd}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}&gt;
   mrs:hasVolume
                {{#ceph_rbd}} {{! ensure that ceph_rbd blocks will be handled in same order regardless of when called, if not, need to make a helper}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:volume+ceph{{@index}}&gt;{{#unless @last}},{{/unless}}
                {{/ceph_rbd}}
.
            {{/if}}

&lt;x-policy-annotation:action:create-{{name}}&gt;
    a            spa:PolicyAction ;
    spa:type     "MCE_VMFilterPlacement" ;
    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt; ;
    spa:importFrom &lt;x-policy-annotation:data:{{name}}-host-criteria&gt;.

&lt;x-policy-annotation:data:{{name}}-host-criteria&gt;
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:value    """{
       "place_into": "{{../../parent}}:host+{{host}}"
    }""" .

&lt;x-policy-annotation:action:create-{{name}}-eth0&gt;
    a            spa:PolicyAction ;
    spa:type     "MCE_VMFilterPlacement" ;
    spa:dependOn &lt;x-policy-annotation:action:create-vpc&gt; ;
    spa:importFrom &lt;x-policy-annotation:data:vpc-subnet-{{name}}-criteria&gt;.

&lt;x-policy-annotation:data:vpc-subnet-{{name}}-criteria&gt;
    a           spa:PolicyData;
    spa:type    "JSON";
    spa:format  """{ "place_into": "%%$.subnets[{{@../index}}].uri%%"}""" .
        {{/vms}}
    {{/subnets}}

    {{#subnets}}

    {{#if_createPathExportTo @root}}
    {{log this}}
&lt;x-policy-annotation:action:create-path&gt;
    a            spa:PolicyAction ;
    spa:type     "MCE_MPVlanConnection" ;
    spa:importFrom &lt;x-policy-annotation:data:conn-criteria&gt; ;
    spa:exportTo
        {{#vms}}
            {{#sriovs}}
&lt;x-policy-annotation:data:{{../name}}-sriov{{@index}}-criteria&gt;{{#unless @last}},{{/unless}}
            {{/sriovs}}
            .
        {{/vms}}

&lt;x-policy-annotation:data:conn-criteria&gt;
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:value    """ {
            {{#vms}}
                {{#if interfaces }}
                    {{#sriovs}}
                        {{#each @root.gateways}}
                            {{#if_eq name ../hosting_gateway}}

                            {{/if_eq}}
                        {{/each}}
                    {{/sriovs}}
                {{/if}}
            {{/vms}}
    } """ .
    {{/if_createPathExportTo}}

{{! svcDeltaCeph }}
    {{#vms}}
        {{#ceph_rbd}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:volume+ceph{{@index}}&gt;
   a  mrs:Volume;
   mrs:disk_gb "{{disk_gb}}" ;
   mrs:mount_point "{{mount_point}}" .
        {{/ceph_rbd}}
        {{#if ceph_rbd}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{name}}&gt;
    mrs:hasVolume
        {{/if}}
        {{#ceph_rbd}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:volume+ceph{{@index}}&gt;{{#if @last}}.{{else}},{{/if}}
        {{/ceph_rbd}}
    {{/vms}}

{{! svcDeltaEndPoints}}
    {{#vms}}
        {{#globus_connect}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus&gt;
   a  mrs:EndPoint ;
   mrs:type "globus:connect" ;
            {{#if username}}
    mrs:hasNetworkAddress &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus:username&gt; ;
            {{/if}}
            {{#if password}}
    mrs:hasNetworkAddress &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus:password&gt; ;
            {{/if}}
            {{#if default_directory }}
    mrs:hasNetworkAddress &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus:directory&gt; ;
            {{/if}}
            {{#if data_interface}}
    mrs:hasNetworkAddress &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus:interface&gt; ;
            {{/if}}
   nml:name "{{short_name}}" .
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}&gt;
   nml:hasService       &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus&gt;.
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus:username&gt;
   a mrs:NetworkAddress ;
   mrs:type "globus:username";
   mrs:value "{{username}}" .
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus:password&gt;
   a mrs:NetworkAddress ;
   mrs:type "globus:password";
   mrs:value "{{password}}" .
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus:directory&gt;
   a mrs:NetworkAddress ;
   mrs:type "globus:directory";
   mrs:value "{{default_directory}}" .
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+globus:interface&gt;
   a mrs:NetworkAddress ;
   mrs:type "globus:interface";
   mrs:value "{{data_interface}}" .
        {{/globus_connect}}
    {{/vms}}
    {{#vms}}
        {{#nfs}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+nfs&gt;
   a  mrs:EndPoint ;
   mrs:type "nfs";
            {{#exports}}
mrs:hasNetworkAddress &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../../name}}:service+nfs:exports&gt; .
            {{/exports}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}&gt;
    nml:hasService       &lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+nfs&gt;.
            {{#if exports}}
&lt;urn:ogf:network:service+{{@root.uuid}}:resource+virtual_machines:tag+{{../name}}:service+nfs:exports&gt;
   a mrs:NetworkAddress ;
   mrs:type "nfs:exports";
   mrs:value "{{{exports}}}".
            {{/if}}
        {{/nfs}}
    {{/vms}}

{{! dependOn }}
    {{#vms}}
        {{#sriovs}}
            {{#each ../../../gateways}}
                {{#if connects.0.from}}
                    {{#if_eq type 'port_profile'}}
&lt;x-policy-annotation:action:ucs-sriov-stitch-external-{{../name}}-sriov{{@index}}&gt;,
                    {{/if_eq}}
                {{/if}}
                {{#if connects.0.to}}
                    {{#if_eq type 'stitch_port'}}
&lt;x-policy-annotation:action:ucs-{{../name}}-sriov{{@index}}-stitch&gt;,
                    {{/if_eq}}
                {{/if}}
            {{/each}}
        {{/sriovs}}
    {{/vms}}
    {{/subnets}}
{{/if_ops}}

&lt;x-policy-annotation:action:create-vpc&gt;
    a           spa:PolicyAction ;
    spa:type     "MCE_VirtualNetworkCreation" ;
    spa:importFrom &lt;x-policy-annotation:data:vpc-criteria&gt;

{{#subnets}}
    {{#if vms}}
;
    spa:exportTo &lt;x-policy-annotation:data:vpc-export&gt;,
        {{~#vms}}
&lt;x-policy-annotation:data:vpc-subnet-{{name}}-criteria&gt;{{#unless @last}},{{/unless}}
        {{/vms}}
        .
    {{else}}
.
    {{/if}}
{{/subnets}}

</modelAddition>

</serviceDelta>


{{!TODO polish punctuation/whitespace/order after base templates are working}}
