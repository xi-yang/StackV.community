@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl:   <http://www.w3.org/2002/07/owl#> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix rdf:   <http://schemas.ogf.org/nml/2013/03/base#> .
@prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#> .
@prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#> .
@prefix spa:   <http://schemas.ogf.org/mrs/2015/02/spa#> .

{{#data}}
{{#virtual_clouds}}
{{> virtual_cloud UUID=../../UUID}}
{{/virtual_clouds}}
{{/data}}







{{! remainder of raw output

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_clouds:tag+vtn2>
    a                         nml:Topology ;
    spa:dependOn <x-policy-annotation:action:create-vtn2>.

<x-policy-annotation:data:vtn2-subnet0-vm-criteria>
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:format    """{
       "place_into": "%$.subnets[0].uri%"
    }""" .

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1>
    a                         nml:Node ;
    nml:name         "ops-vtn1-vm1";
    mrs:type       "instance+5,secgroup+rains,keypair+demo-key,image+03555952-e619-4b26-bffd-6b9a62ae15da";
    mrs:hasVolume       <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:volume+ceph0>, <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:volume+ceph1>;
    nml:hasBidirectionalPort   <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:eth0> ;
    nml:hasService  <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:routingservice> ;
    spa:dependOn <x-policy-annotation:action:create-ops-vtn1-vm1>.

<x-policy-annotation:action:create-ops-vtn1-vm1>
    a            spa:PolicyAction ;
    spa:type     "MCE_VMFilterPlacement" ;
    spa:dependOn <x-policy-annotation:action:create-vtn2> ;
    spa:importFrom <x-policy-annotation:data:vtn2-ops-vtn1-vm1-host-criteria>.

<x-policy-annotation:action:create-ops-vtn1-vm1-eth0>
    a            spa:PolicyAction ;
    spa:type     "MCE_VMFilterPlacement" ;
    spa:dependOn <x-policy-annotation:action:create-vtn2> ;
    spa:importFrom <x-policy-annotation:data:vtn2-subnet0-vm-criteria>.

<x-policy-annotation:data:vtn2-ops-vtn1-vm1-host-criteria>
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:value    """{
       "place_into": "any"
    }""" .

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:eth0>
    a            nml:BidirectionalPort ;
    spa:dependOn <x-policy-annotation:action:create-ops-vtn1-vm1-eth0> ;
    mrs:hasNetworkAddress          <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:eth0:floatingip> .

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:eth0:floatingip>
    a            mrs:NetworkAddress;
    mrs:type     "floating-ip";
    mrs:value     "any".

<x-policy-annotation:action:ucs-sriov-stitch1>
    a            spa:PolicyAction ;
    spa:type     "MCE_UcsSriovStitching" ;
    spa:dependOn <x-policy-annotation:action:create-ops-vtn1-vm1>, <x-policy-annotation:action:create-ops-vtn1-vm1-eth0>, <x-policy-annotation:action:create-aws-ops-path>;
    spa:importFrom <x-policy-annotation:data:sriov-criteria1>, <x-policy-annotation:data:aws-ops-criteriaexport> .

<x-policy-annotation:data:sriov-criteria1>
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:format    """{
       "stitch_from": "urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1",
       "to_l2path": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn1%
       "mac_address": "aa:bb:cc:ff:01:11",
       "ip_address": "10.10.0.1/24"
    }""" .

<x-policy-annotation:data:aws-ops-criteria>
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:value    """{
        "urn:ogf:network:vo1_maxgigapop_net:link=conn1": {
            "urn:ogf:network:openstack.com:openstack-cloud":{"vlan_tag":"any"},
            "urn:ogf:network:aws.amazon.com:aws-cloud":{"vlan_tag":"any"}
        }
    }""".

<x-policy-annotation:action:nfv-quagga-bgp1>
    a            spa:PolicyAction ;
    spa:type     "MCE_NfvBgpRouting";
    spa:dependOn <x-policy-annotation:action:create-dc1>, <x-policy-annotation:action:ucs-sriov-stitch1>;
    spa:importFrom <x-policy-annotation:data:quagga-bgp1-remote>, <x-policy-annotation:data:quagga-bgp1-local>.

<x-policy-annotation:data:quagga-bgp1-remote>
    a            spa:PolicyData ;
    spa:type     "JSON" ;
    spa:format   """{"neighbors":[{"local_ip":"%$..customer_ip%","bgp_authkey":"versastack","remote_ip":"%$..amazon_ip%","remote_asn":"7224"}],"as_number":"%$..customer_asn%","router_id":"%$..customer_ip%"}""" .

<x-policy-annotation:data:quagga-bgp1-local>
    a            spa:PolicyData ;
    spa:type     "JSON" ;
    spa:value   """{
       "parent": "urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1",
       "networks":["10.10.0.0/16"]
    }""" .

<x-policy-annotation:action:ucs-sriov-stitch-external-ops-vtn1-vm1-sriov2>
    a            spa:PolicyAction ;
    spa:type     "MCE_UcsSriovStitching" ;
    spa:dependOn <x-policy-annotation:action:create-ops-vtn1-vm1>, <x-policy-annotation:action:create-ops-vtn1-vm1-eth0>;
    spa:importFrom <x-policy-annotation:data:sriov-criteria-external-ops-vtn1-vm1-sriov2>.

<x-policy-annotation:data:sriov-criteria-external-ops-vtn1-vm1-sriov2>
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:value    """{
       "stitch_from": "urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1",
       "to_port_profile": "Ceph-Storage",
       "mac_address": "aa:bb:cc:ff:01:12",
       "ip_address": "10.10.200.164/24"
    }""" .

<x-policy-annotation:action:ucs-sriov-stitch-external-ops-vtn1-vm1-sriov3>
    a            spa:PolicyAction ;
    spa:type     "MCE_UcsSriovStitching" ;
    spa:dependOn <x-policy-annotation:action:create-ops-vtn1-vm1>, <x-policy-annotation:action:create-ops-vtn1-vm1-eth0>;
    spa:importFrom <x-policy-annotation:data:sriov-criteria-external-ops-vtn1-vm1-sriov3>.

<x-policy-annotation:data:sriov-criteria-external-ops-vtn1-vm1-sriov3>
    a            spa:PolicyData;
    spa:type     "JSON";
    spa:value    """{
       "stitch_from": "urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1",
       "to_port_profile": "External-Access",
       "mac_address": "aa:bb:cc:dd:01:49",
       "ip_address": "206.196.179.149/28"
    }""" .

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:routingservice>
     a   mrs:RoutingService;
     mrs:providesRoutingTable     <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:routingservice:routingtable+linux> .
<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:routingservice:routingtable+linux>
     a   mrs:RoutingTable;
     mrs:type   "linux";
     mrs:hasRoute    
            <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:routingservice:routingtable+linux:route+1>
,
            <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:routingservice:routingtable+linux:route+2>
. 

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:routingservice:routingtable+linux:route+1>
      a  mrs:Route;
      mrs:routeTo [a    mrs:NetworkAddress; mrs:type    "ipv4-prefix"; mrs:value   "10.10.0.0/16"];      mrs:nextHop [a    mrs:NetworkAddress; mrs:type    "ipv4-address"; mrs:value   "10.1.0.1"];.

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:routingservice:routingtable+linux:route+2>
      a  mrs:Route;
      mrs:routeTo [a    mrs:NetworkAddress; mrs:type    "ipv4-prefix"; mrs:value   "0.0.0.0/0"];      mrs:nextHop [a    mrs:NetworkAddress; mrs:type    "ipv4-address"; mrs:value   "206.196.179.145"];.

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:volume+ceph0>
   a  mrs:Volume;
   mrs:disk_gb "1024";
   mrs:mount_point "/mnt/ceph0_1tb".

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:volume+ceph1>
   a  mrs:Volume;
   mrs:disk_gb "1024";
   mrs:mount_point "/mnt/ceph1_1tb".

<urn:ogf:network:openstack.com:openstack-cloud:ceph-rbd>
   mrs:providesVolume <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:volume+ceph0>, <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:volume+ceph1> .

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus>
   a  mrs:EndPoint ;
   mrs:type "globus:connect" ;
mrs:hasNetworkAddress <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus:username> ;
mrs:hasNetworkAddress <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus:password> ;
mrs:hasNetworkAddress <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus:directory> ;
mrs:hasNetworkAddress <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus:interface> ;
   nml:name "MAX-SDMZ-EP-X1" .

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1>
    nml:hasService       <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus>. 

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus:username>
   a mrs:NetworkAddress ;
   mrs:type "globus:username";
   mrs:value "globus_user" .
<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus:password>
   a mrs:NetworkAddress ;
   mrs:type "globus:password";
   mrs:value "globus_pass" .
<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus:directory>
   a mrs:NetworkAddress ;
   mrs:type "globus:directory";
   mrs:value "/mnt/ceph0_1tb" .
<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+globus:interface>
   a mrs:NetworkAddress ;
   mrs:type "globus:interface";
   mrs:value "206.196.179.157" .
<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+nfs>
   a  mrs:EndPoint ;
   mrs:type "nfs" ;
mrs:hasNetworkAddress <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+nfs:expots> .
<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1>
    nml:hasService       <urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+nfs>. 

<urn:ogf:network:service+932147bc-30a2-46a7-87c8-3b85ee5ec47f:resource+virtual_machines:tag+ops-vtn1-vm1:service+nfs:expots>
   a mrs:NetworkAddress ;
   mrs:type "nfs:exports";
   mrs:value "['/mnt/ceph1_1tb 206.196.0.0/16(rw,sync,no_subtree_check)']" .
<x-policy-annotation:action:create-vtn2>
    a            spa:PolicyAction ;
    spa:type     "MCE_VirtualNetworkCreation" ;
    spa:importFrom <x-policy-annotation:data:vtn2-criteria> ;
    spa:exportTo <x-policy-annotation:data:vtn2-subnet0-vm-criteria>.

<urn:ogf:network:openstack.com:openstack-cloud:vt>
   a  nml:Topology;
   spa:type spa:Abstraction;
   spa:dependOn  <x-policy-annotation:action:nfv-quagga-bgp1>, <x-policy-annotation:action:ucs-sriov-stitch1>, <x-policy-annotation:action:ucs-sriov-stitch-external-ops-vtn1-vm1-sriov2>, <x-policy-annotation:action:ucs-sriov-stitch-external-ops-vtn1-vm1-sriov3>.

<x-policy-annotation:data:vtn2-criteria>
    a            spa:PolicyData;
    spa:type     nml:Topology;
    spa:value    """{"parent":"urn:ogf:network:openstack.com:openstack-cloud","routes":[{"nextHop":"internet","next_hop":{"value":"internet"},"to":"0.0.0.0/0"}],"gateways":[{"name":"ceph-net","from":[{"type":"port_profile","value":"Ceph-Storage"}],"type":"ucs_port_profile"},{"name":"intercloud-1","to":[{"type":"peer_cloud","value":"urn:ogf:network:aws.amazon.com:aws-cloud?vlan=any"}],"type":"inter_cloud_network"},{"name":"ext-net","from":[{"type":"port_profile","value":"External-Access"}],"type":"ucs_port_profile"}],"cidr":"10.1.0.0/16","subnets":[{"routes":[{"nextHop":"internet","next_hop":{"value":"internet"},"to":"0.0.0.0/0"}],"name":"subnet1","cidr":"10.1.0.0/24"}],"type":"internal"}""".

<x-policy-annotation:action:create-dc1>
    a            spa:PolicyAction ;
    spa:type     "MCE_AwsDxStitching" ;
    spa:importFrom <x-policy-annotation:data:vpc1-export>, <x-policy-annotation:data:aws-ops-criteriaexport> ;
    spa:dependOn <x-policy-annotation:action:create-vpc1>, <x-policy-annotation:action:create-aws-ops-path>;
    spa:exportTo <x-policy-annotation:data:quagga-bgp1-remote>.

<x-policy-annotation:action:create-aws-ops-path>
    a            spa:PolicyAction ;
    spa:type     "MCE_MPVlanConnection" ;
    spa:importFrom <x-policy-annotation:data:aws-ops-criteria> ;
    spa:exportTo <x-policy-annotation:data:aws-ops-criteriaexport>, <x-policy-annotation:data:sriov-criteria1> .

<x-policy-annotation:data:aws-ops-criteriaexport>
    a            spa:PolicyData;
    spa:type     "JSON" ;
    spa:format   """{
       "to_l2path": %$.urn:ogf:network:vo1_maxgigapop_net:link=conn1%
    }""" .






}}
