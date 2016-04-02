/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstack;

/**
 *
 * @author tcm
 */
public class OpenstackPrefix {
    public static String prefix = "openstack.com:openstack-cloud";
    //@TODO: dynamic pattern prefix
    public static String bucket = prefix+":bucket+%s";
    
    public static String networkService = prefix+"%s:networkservice";

    public static String routingService = prefix+"%s:routingservice";

    public static String cinderService = prefix+"%s:cinderservice";

    public static String PORT = prefix+":port+%s";

    public static String public_address = prefix+":port+%s:public-ip-address+%s";

    public static String private_address = prefix+":port+%s:private-ip-address+%s";
    
    public static String mac_address = prefix+":port+%s:mac-address+%s";

    public static String ucs_port_profile = prefix+":port-profile+%s";

    public static String host = prefix+":host+%s";
    
    public static String hypervisor = prefix+":hypervisor+%s";
    
    public static String vm = prefix+":server+%s";
    
    public static String NETWORK = prefix+":vpc+%s";
    
    public static String switching_service = prefix+":vpc+%s:switchingservice";
    
    public static String subnet_network_address = prefix+":vpc+%s:subnet+%s:subnetnetworkaddress";
    
    public static String floating_ip  = prefix+":floatingip+%s";

    public static String floating_ip_in_using  = prefix+":vpc+%s:subnet+%s:floatingip-inuse";
    
    public static String floating_ip_pool  = prefix+":vpc+%s:subnet+%s:floatingip-pool";
    
    public static String gateway  = prefix+":vpc+%s:subnet+%s:subnetgateway+%s";
    
    public static String gateway_address  = prefix+":vpc+%s:subnet+%s:subnetgateway+%s:address";
    
    public static String network_routing_service  = prefix+":vpc+%s:routingservice";
    
    public static String network_routing_table  = prefix+":vpc+%s:routingtable";
    public static String routingTable = prefix+":vpc+%s:routingtable+%s";

    public static String network_route  = prefix+":vpc+%s:dest_ip+%s:subnet+%s:route";
    
    public static String network_route_next_hop  = prefix+":vpc+%s:next-hop+%s:route-nexthop";
    
    public static String network_route_dest  = prefix+":vpc+%s:dest_ip+%s:route-dest";
    
    public static String router  = prefix+":router+%s";
    
    public static String router_interface_routingtable  = prefix+":router+%s:routingtable-router-interface";
    
    public static String router_interface_route  = prefix+":router+%s:interfaceip+%s:router-interface-route";
    
    public static String router_interface_next_hop  = prefix+":router+%s:router-interface-nexthop+%s:-router-interface-route-nexthop";
    
    public static String router_host_route  = prefix+":routername+%s:hostroute-to+%s:hostroute-nexthop+%s:router-host-route";
    
    public static String router_host_routingtable  = prefix+":routername+%s:router-host-routing-table";
    
    public static String router_host_routeto  = prefix+":routername+%s:hostroute-to+%s";
    
    public static String router_host_route_nexthop  = prefix+":routername+%s:hostroute-nexthop+%s";
    
    public static String local_route  = prefix+":vpc+%s:subnet+%s:route-local";
    
    public static String host_route  = prefix+":vpc+%s:dest_ip+%s:from_subnet+%s:hostroute";
    
    public static String host_route_route_from  = prefix+":vpc+%s:from_subnet+%s:route_from";
    
    public static String host_route_to  = prefix+":vpc+%s:subnet+%s:dest+%s:routeto";
    
    public static String host_route_next_hop  = prefix+":vpc+%s:dest_ip+%s:subnet+%s:next-hop+%s:nexthop";
    
    public static String host_route_routing_table  = prefix+":vpc+%s:dest_ip+%s:subnet+%s:hostroutingtable";
    
    public static String hypervisorBypassSvc = prefix+":host+%s:hypervisor-bypass-svc+%s";
    
    public static String nic = prefix+":vpc+%s:subnet+%s:nic+%s";
    
    public static String nicNetworkAddress = prefix+":vpc+%s:subnet+%s:nic+%s:ip+%s";
    
    public static String route = prefix+":vpc+%s:routingtable+%s:route+%s";

    public static String routeFrom = prefix+":vpc+%s:routingtable+%s:route+%s:routefrom";

    public static String routeTo = prefix+":vpc+%s:routingtable+%s:route+%s:routeto";


    public static String s3Service = prefix+":s3service+%s";

    public static String subnet = prefix+":vpc+%s:subnet+%s";

    public static String subnetNetworkAddress = prefix+":vpc+%s:subnet+%s:cidr";
    
    public static String vif = prefix+":vif%s";

    public static String vlan = prefix+":vif+%s:vlan+%s";

    public static String volume = prefix+":volume+%s";

    public static String vpc = prefix+":vpc+%s";

    public static String vpcNetworkAddress = prefix+":vpc+%s:cidr";

    public static String vpcService = prefix+":vpcservice+%s";
}
