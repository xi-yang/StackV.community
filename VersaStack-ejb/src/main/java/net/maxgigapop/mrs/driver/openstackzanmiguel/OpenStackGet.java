/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Resource;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.network.*;
import org.openstack4j.model.storage.block.*;
import org.openstack4j.openstack.compute.domain.NovaFloatingIP;
import org.openstack4j.openstack.compute.domain.NovaInterfaceAttachment;
import org.openstack4j.openstack.compute.internal.ext.InterfaceServiceImpl;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;

/**
 *
 * @author max
 */
public class OpenStackGet {

    private OSClient client = null;
    private List< Network> networks = new ArrayList();
    private List<Subnet> subnets = new ArrayList();
    private List<Port> ports = new ArrayList();
    private List<Server> servers = new ArrayList();
    private List<Volume> volumes = new ArrayList();
    private List<NetFloatingIP> floatingIps = new ArrayList();
    private List<Router> routers = new ArrayList();
    private List<RouterInterface> routerinterface = new ArrayList();
    public List<HostRoute> hostroute = new ArrayList();
    public List<Hypervisor> hypervisors = new ArrayList();
    public List<NovaFloatingIP> novafloatingIps =new ArrayList();

    public  OpenStackGet(String url,String NATServer, String username, String password, String tenantName) {
        //authenticate
        Authenticate authenticate = new Authenticate();
        NeutronRouterInterface ri = new NeutronRouterInterface();
        
         client = authenticate.openStackAuthenticate(url,NATServer, username, password, tenantName);


        //get the resources
        networks = (List<Network>) client.networking().network().list();
        subnets = (List<Subnet>)client.networking().subnet().list();
        ports = (List<Port>)client.networking().port().list();
        servers = (List<Server>)client.compute().servers().list();
        volumes = (List<Volume>)client.blockStorage().volumes().list();
        floatingIps = (List<NetFloatingIP>)client.networking().floatingip().list();
        routers = (List<Router>)client.networking().router().list();
        novafloatingIps = (List<NovaFloatingIP>) client.compute().floatingIps().list();
    }

    //get all the nets in the tenant
    public List<Network> getNetworks() {
        return networks;
    }

    //get a network by its id
    public Network getNetwork(String id) {
        for (Network net : networks) {
            if (net.getId().equals(id) || net.getName().equals(id)) {
                return net;
            }
        }
        return null;
    }

    //get all the subnets in the tenant
    public List<Subnet> getSubnets() {
        return subnets;
    }

    //get a subnet by its id
    public Subnet getSubnet(String id) {
        for (Subnet net : subnets) {
            if (net.getId().equals(id) || net.getName().equals(id)) {
                return net;
            }
        }
        return null;
    }

    //get a Lsit of subnets under a network
    public List<Subnet> getSubnets(String id) {
        List<Subnet> subnetList = new ArrayList();
        for (Subnet sub : subnets) {
            if (sub.getNetworkId().equals(id)) {
                subnetList.add(sub);
            } else if (sub.getId().equals(id)) {
                subnetList.add(sub);
            }
        }
        return subnetList;
    }

    //get all the ports in the tenant
    public List<Port> getPorts() {
        return ports;
    }

    //get a port by its id
    public Port getPort(String id) {
        for (Port port : ports) {
            if (port.getId().equals(id) || port.getName().equals(id)) {
                return port;
            }
        }
        return null;
    }

    //get port subnet id
    public List<String> getPortSubnetID(Port port) {
        List<IP> snID = new ArrayList();
        List<String> subID = new ArrayList();
        for (IP ip : port.getFixedIps()) {
            subID.add(ip.getSubnetId());
        }
        return subID;
    }

    //get all servers in the tenant
    public List<Server> getServers() {
        return servers;
    }

    //get a server by its id
    public Server getServer(String id) {
        for (Server server : servers) {
            if (server.getId().equals(id) || server.getName().equals(id)) {
                return server;
            }
        }
        return null;
    }

    //get the Networks of  a server
    public List<Network> getServerNetworks(Server server) {
        List<Network> nets = new ArrayList();
        for (Port port : ports) {
            if (port.getDeviceId().equals(server.getId())) {
                Network net = getNetwork(port.getNetworkId());
                nets.add(net);
            }
        }
        return nets;
    }

    //get the Subnets of  a server
    public List<Subnet> getServerSubnets(Server server) {
        List<Subnet> nets = new ArrayList();
        InterfaceServiceImpl impl = new InterfaceServiceImpl();
        for (InterfaceAttachment att : impl.list(server.getId())) {
            for (InterfaceAttachment.FixedIp attIp : att.getFixedIps()) {
                if (!nets.contains(getSubnet(attIp.getSubnetId()))) {
                    nets.add(getSubnet(attIp.getSubnetId()));
                }
            }
        }
        return nets;
    }

    //get the ports of  a server
    public List<Port> getServerPorts(Server server) {
        List<Port> portList = new ArrayList();
        InterfaceServiceImpl impl = new InterfaceServiceImpl();
        for (InterfaceAttachment att : impl.list(server.getId())) {
            Port p = getPort(att.getPortId());
            if (!portList.contains(p)) {
                portList.add(p);
            }
        }
        return portList;
    }

    //get all volumes in the tenant
    public List<Volume> getVolumes() {
        return volumes;
    }

    //get a volume by its id
    public Volume getVolume(String id) {
        for (Volume volume : volumes) {
            if (volume.getId().equals(id) || volume.getName().equals(id)) {
                return volume;
            }
        }
        return null;
    }

    //get all floating ips in the tenant
    public List<NetFloatingIP> getFloatingIp() {
        return floatingIps;
    }

    //get a floating ip  by its id
    public NetFloatingIP getFloatingIp(String id) {
        for (NetFloatingIP ip : floatingIps) {
            if (ip.getId().equals(id)) {
                return ip;
            }
        }
        return null;
    }
   public List<NovaFloatingIP> getNovaFloatingIP(){
       return novafloatingIps;
   }
    //get a list of all the hypervisors
    public List<Hypervisor> getHypervisors() {
        return hypervisors;
    }

    //get a specific hypervisor by id 
    public Hypervisor getHypervisor(String id) {
        for (Hypervisor h : hypervisors) {
            if (h.getId().equals(id)) {
                return h;
            }
        }
        return null;
    }
    
    //get all the routers
    public List<Router> getRouters()
    {
        return routers;
    }
    
    //get a specific route by id or name
    public Router getRouter(String id)
    {
        for (Router router : routers) {
            if (router.getId().equals(id) || router.getName().equals(id)) {
                return router;
            }
        }
        return null;
    }

    //get the OpenStack client
    public OSClient getClient() {
        return client;
    }

    //get name from a resource
    //if the resource does not have a nane, return the ID
    public String getResourceName(Resource r) {
        String name = r.getName();
        if (name.isEmpty()) {
            return r.getId();
        } else {
            return r.getName();
        }
    }

    //get the name of a server 
    public String getServereName(Server r) {
        String name = r.getName();
        if (name ==null || name.isEmpty()) {
            return r.getId();
        } else {
            return r.getName();
        }
    }
    
    public String getVolumeName(Volume r)
    {
        String name = r.getName();
        if (name == null || name.isEmpty()) {
            return r.getId();
        } else {
            return r.getName();
        }
    }
    public String getInterfaceSubnetID(NeutronRouterInterface i){
        return i.getSubnetId();
    }
    public String getInterfacePortID(NeutronRouterInterface i){
        return i.getPortId();
    }
    public String getInterfaceRouterID(NeutronRouterInterface i){
        return i.getId();
    }
    
   
}
