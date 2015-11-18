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
    public static String uri = "%s";
    public static String bucket = uri + ":bucket+%s";
    
    public static String networkService = uri + ":network-service";

    public static String routingService = uri +  ":routing-service+%s";

    public static String cinderService = uri +  ":cinder-service+%s";

    public static String PORT = uri + ":port+%s";

    public static String public_address = uri + ":port+%s :public-ip-address+%s";

    public static String private_address = uri + ":port+%s :private-ip-address+%s";
    
    public static String host = uri + ":host+%s";
    
    public static String hypervisor = uri + ":hypervisor+%s";
    
    public static String vm = uri + ":server+%s";
    
    public static String NETWORK = uri + ":network+%s";
    
    public static String switching_service = uri + ":network+%s:switchingservice";
    
    public static String subnet_network_address = uri + ":network+%s:subnet+%s:subnetnetworkaddress";
    
    public static String floating_ip_in_using  = uri + ":network+%s:subnet+%s:floatingip-inuse";
    
    public static String floating_ip_pool  = uri + ":network+%s:subnet+%s:floatingip-pool";
    
    public static String gateway  = uri + ":network+%s:subnet+%s:subnetgateway+%s";
    
    public static String gateway_address  = uri + ":network+%s:subnet+%s:subnet_gateway_address+%s";
    
    public static String network_routing_service  = uri + ":network+%s:network-routingservice";
    
    public static String network_routing_table  = uri + ":network+%s:route-routingtable";
    
    public static String network_route  = uri + ":network+%s:dest_ip+%s:subnet+%s:route";
    
    public static String network_route_next_hop  = uri + ":network+%s:next-hop+%s:route-nexthop";
    
    public static String network_route_dest  = uri + ":network+%s:dest_ip+%s:route-dest";
    
    public static String router_interface_route  = uri + ":router+%s:interfaceip+%s:router-interface-route";
    
    public static String router  = uri + ":router+%s";
    
    public static String router_interface_routingtable  = uri + ":router+%s:router-interface-routingtable";
    
    public static String router_interface_next_hop  = uri + ":router+%s:router-interface-nexthop+%s:-router-interface-route-nexthop";
    
    public static String router_host_route  = uri + ":routername+%s:hostroute-to+%s:hostroute-nexthop+%s:router-host-route";
    
    public static String router_host_routingtable  = uri + ":routername+%s:router-host-routing-table";
    
    public static String router_host_routeto  = uri + ":routername+%s:hostroute-to+%s";
    
    public static String router_host_route_nexthop  = uri + ":routername+%s:hostroute-nexthop+%s";
    
    public static String local_route  = uri + ":network+%s:subnet+%s:local-route";
    
    public static String host_route  = uri + ":network+%s:dest_ip+%s:from_subnet+%s:hostroute";
    
    public static String host_route_route_from  = uri + ":network+%s:from_subnet+%s:route_from";
    
    public static String host_route_to  = uri + ":network+%s:subnet+%s:dest+%s:routeto";
    
    public static String host_route_next_hop  = uri + ":network+%s:dest_ip+%s:subnet+%s:next-hop+%s:nexthop";
    
    public static String host_route_routing_table  = uri + ":network+%s:dest_ip+%s:subnet+%s:hostroutingtable";
    
    
    public static String nic = uri + ":vpc+%s:subnet+%s:nic+%s";
    
    public static String nicNetworkAddress = uri + ":vpc+%s:subnet+%s:nic+%s:ip+%s";
    
    //public static String publicAddress = uri + ":public-address";

    public static String route = uri + ":vpc+%s:routingtable+%s:route+%s";

    public static String routeFrom = uri + ":vpc+%s:routingtable+%s:route+%s:routefrom";

    public static String routeTo = uri + ":vpc+%s:routingtable+%s:route+%s:routeto";

   

    public static String routingTable = uri + ":vpc+%s:routingtable+%s";

    public static String s3Service = uri + ":s3service+%s";

    public static String subnet = uri + ":network+%s:subnet+%s";

    public static String subnetNetworkAddress = uri + ":vpc+%s:subnet+%s:cidr";

   // public static String switchingService = uri + ":vpc+%s:switchingservice";

    public static String vif = uri + ":vif%s";

    public static String vlan = uri + ":vif+%s:vlan+%s";

    public static String volume = uri + ":volume+%s";

    public static String vpc = uri + ":vpc+%s";

    public static String vpcNetworkAddress = uri + ":vpc+%s:cidr";

    public static String vpcService = uri + ":vpcservice+%s";
}
