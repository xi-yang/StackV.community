/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.openstackget;

import com.mysql.fabric.Server;
import java.util.List;
import static org.openstack4j.api.Builders.router;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.storage.block.Volume;

/**
 *
 * @author tcm
 */

public class OpenstackGet {
    
    
    
    public OSClient client = null;
    private List<? extends Network> networks = null;
    private List<? extends Subnet> subnets = null;
    private List<? extends Port> ports = null;
    private List<? extends Server> servers = null;
    private List<? extends Volume> volumes = null;
    private List<? extends NetFloatingIP> floatingIps = null;
    private List<? extends Router> routers = null;
    
    
    
        public OpenstackGet(String url, String username, String password, String tenantName) {
        //authenticate
        Authenticate authenticate = new Authenticate();
        client = authenticate.openStackAuthenticate(url, username, password, tenantName);

        //get the resources
        networks = client.networking().network().list();
        subnets = client.networking().subnet().list();
        ports = client.networking().port().list();
        servers = (List<? extends Server>) client.compute().servers().list();
        volumes = client.blockStorage().volumes().list();
        floatingIps = client.networking().floatingip().list();
        routers =(List<? extends Router>) client.networking().router().list();
        

    }

    public OpenstackGet() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    //get all the networks in the tenant
    public List<? extends Network> getNetworks() {
        //return networks;
        System.out.println(networks.get(0).toString());
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
        System.out.println(subnets.get(0).toString());
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
    /*public Server getServer(String id) {
        for (Server server : servers) {
            if (server.equals(id) || server.getId().equals(id)) {
                return server;
            } else {
            }
        }
        return null;
    }
    */
    
    
    
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
    
    public List<? extends Router>  getRouter(){
        return routers;
    }
    
    
    public Router getRouter(String id){
        for (Router r : routers){
            if (id.equals(r.getId())){
                return r;
            } 
         }
         return null;
         
            
    }
     
    
}
