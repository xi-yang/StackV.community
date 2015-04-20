/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Resource;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.network.*;
import org.openstack4j.model.storage.block.*;
import org.openstack4j.openstack.compute.domain.NovaInterfaceAttachment;

/**
 *
 * @author max
 */
public class OpenStackGet {

    public OSClient client = null;
    private List<? extends Network> networks = null;
    private List<? extends Subnet> subnets = null;
    private List<? extends Port> ports = null;
    private List<? extends Server> servers = null;
    private List<? extends Volume> volumes = null;
    private List<? extends NetFloatingIP> floatingIps = null;

    public  OpenStackGet(String url, String username, String password, String tenantName) {
        //authenticate
        Authenticate authenticate = new Authenticate();
        client = authenticate.openStackAuthenticate(url, username, password, tenantName);

        //get the resources
        networks = client.networking().network().list();
        subnets = client.networking().subnet().list();
        ports = client.networking().port().list();
        servers = client.compute().servers().list();
        volumes = client.blockStorage().volumes().list();
        floatingIps = client.networking().floatingip().list();
        

    }

    public OpenStackGet() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    //get all the networks in the tenant
    public List<? extends Network> getNetworks() {
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
    public List<? extends Subnet> getSubnets() {
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
    
    public List<Subnet> getSubnets(String id){
        List<Subnet> subnetList = new ArrayList();
        for(Subnet sub : subnets){
            if (sub.getNetworkId().equals(id))
                subnetList.add(sub);
            else if (sub.getId().equals(id)){
                subnetList.add(sub);
                
                
            }
            
        }
        
        return subnetList;
        
        
        
    }
    
    
    
    
        /*public List<Subnet> getSubnets(String id)
    {
        List<Subnet> subnetList=new ArrayList();
        for(Subnet sub : subnets)
        {
            if(sub.getNetworkId().equals(id))
                subnetList.add(sub);
            else if(sub..equals(id))
            {
                subnetList.add(sub);
                return subnetList;
            }
        }
        return subnetList;
    }
*/
    //get all the ports in the tenant
    public List<? extends Port> getPorts() {
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

    //get all servers in the tenant
    public List<? extends Server> getServers() {
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
    
    public List<String> gethostlist(){
        List<String> l= new ArrayList<>();
        for (Server server : servers){
            String a = server.getHost();
            l.add(a);
        
        }
        return l;
    }
        
        
        
    
    //get all volumes in the tenant
    public List<? extends Volume> getVolumes() {
        return volumes;
    }

    //get a volume by its id
    public Volume getVolume(String id) {
        for (Volume volume: volumes) {
            if (volume.getId().equals(id) || volume.getName().equals(id)) {
                return volume;
            }
        }
        return null;
    }
    
    //get all floating ips in the tenant
    public List<? extends NetFloatingIP> getFloatingIp() {
        return floatingIps;
    }
   
    //get a floating ip  by its id
    public NetFloatingIP getFloatingIp(String id) {
        for (NetFloatingIP ip: floatingIps) {
            if (ip.getId().equals(id)) {
                return ip;
            }
        }
        return null;
    }
    
    //get the OpenStack client
    public OSClient getClient()
    {
        return client;
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
    public List<String> getServerSubnets(Server server) {
        List<String> nets = new ArrayList();
        
        for (Port port : ports) {
            /*if (port.getDeviceId().equals(server.getId())) {
                NovaInterfaceAttachment att = new NovaInterfaceAttachment(port.getId());
                for(InterfaceAttachment.FixedIp attIp : att.getFixedIps())
                {
                    nets.add(getSubnet(attIp.getSubnetId()));
                }
                    */
            
            Iterator ip = port.getFixedIps().iterator();
            while (ip.hasNext()){
                IP ipadd = (IP) ip.next();
                nets.add(ipadd.getSubnetId());
            }
            
                        
            }
        
        return nets;
    }

    
    //get server interface
   
    
    //get name from a resource
    //if the resource does not have a nane, return the ID
    public String getResourceName(Resource r)
    {
        String name = r.getName();
        if(name.isEmpty())
            return r.getId();
        else 
            return r.getName();
    }
}
