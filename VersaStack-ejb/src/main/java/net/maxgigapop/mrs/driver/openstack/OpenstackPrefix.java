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
    public static String uri ;
    public static String bucket = "%s:bucket+%s";
    
    public static String networkService = "%s:network-service";

    public static String routingService = "%s:routing-service+%s";

    public static String cinderService = "%s:cinder-service+%s";

    public static String PORT = "%s:port+%s";

    public static String public_address = "%s:port+%s:public-ip-address+%s";

    public static String private_address = "%s:port+%s:private-ip-address+%s";
    
    public static String host = "%s:host+%s";
    
    public static String hypervisor = "%s:hypervisor+%s";
    
    public static String vm = "%s:server+%s";
    
    public static String NETWORK = "%s:network+%s";
    
    public static String switching_service = "%s:network+%s:switchingservice";
    
    public static String subnet_network_address = "%s:network+%s:subnet+%s:subnetnetworkaddress";
    
    public static String floating_ip_in_using  = "%s:network+%s:subnet+%s:floatingip-inuse";
    
    public static String floating_ip_pool  = "%s:network+%s:subnet+%s:floatingip-pool";
    
    public static String gateway  = "%s:network+%s:subnet+%s:subnetgateway+%s";
    
    public static String gateway_address  = "%s:network+%s:subnet+%s:subnetgateway+%s:address";
    
    public static String network_routing_service  = "%s:network+%s:network-routingservice";
    
    public static String network_routing_table  = "%s:network+%s:route-routingtable";
    
    public static String network_route  = "%s:network+%s:dest_ip+%s:subnet+%s:route";
    
    public static String network_route_next_hop  = "%s:network+%s:next-hop+%s:route-nexthop";
    
    public static String network_route_dest  = "%s:network+%s:dest_ip+%s:route-dest";
    
    public static String router_interface_route  = "%s:router+%s:interfaceip+%s:router-interface-route";
    
    public static String router  = "%s:router+%s";
    
    public static String router_interface_routingtable  = "%s:router+%s:router-interface-routingtable";
    
    public static String router_interface_next_hop  = "%s:router+%s:router-interface-nexthop+%s:-router-interface-route-nexthop";
    
    public static String router_host_route  = "%s:routername+%s:hostroute-to+%s:hostroute-nexthop+%s:router-host-route";
    
    public static String router_host_routingtable  = "%s:routername+%s:router-host-routing-table";
    
    public static String router_host_routeto  = "%s:routername+%s:hostroute-to+%s";
    
    public static String router_host_route_nexthop  = "%s:routername+%s:hostroute-nexthop+%s";
    
    public static String local_route  = "%s:network+%s:subnet+%s:local-route";
    
    public static String host_route  = "%s:network+%s:dest_ip+%s:from_subnet+%s:hostroute";
    
    public static String host_route_route_from  = "%s:network+%s:from_subnet+%s:route_from";
    
    public static String host_route_to  = "%s:network+%s:subnet+%s:dest+%s:routeto";
    
    public static String host_route_next_hop  = "%s:network+%s:dest_ip+%s:subnet+%s:next-hop+%s:nexthop";
    
    public static String host_route_routing_table  = "%s:network+%s:dest_ip+%s:subnet+%s:hostroutingtable";
    
    
    public static String nic = "%s:vpc+%s:subnet+%s:nic+%s";
    
    public static String nicNetworkAddress = "%s:vpc+%s:subnet+%s:nic+%s:ip+%s";
    
    //public static String publicAddress = "%s:public-address";

    public static String route = "%s:vpc+%s:routingtable+%s:route+%s";

    public static String routeFrom = "%s:vpc+%s:routingtable+%s:route+%s:routefrom";

    public static String routeTo = "%s:vpc+%s:routingtable+%s:route+%s:routeto";

   

    public static String routingTable = "%s:vpc+%s:routingtable+%s";

    public static String s3Service = "%s:s3service+%s";

    public static String subnet = "%s:network+%s:subnet+%s";

    public static String subnetNetworkAddress = "%s:vpc+%s:subnet+%s:cidr";

   // public static String switchingService = "%s:vpc+%s:switchingservice";

    public static String vif = "%s:vif%s";

    public static String vlan = "%s:vif+%s:vlan+%s";

    public static String volume = "%s:volume+%s";

    public static String vpc = "%s:vpc+%s";

    public static String vpcNetworkAddress = "%s:vpc+%s:cidr";

    public static String vpcService = "%s:vpcservice+%s";
}
