/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Zan Wang 2015
 * Modified by: Xi Yang 2015-2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */
package net.maxgigapop.mrs.driver.openstack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private List<Network> networks = new ArrayList<>();
    private List<Subnet> subnets = new ArrayList<>();
    private List<Port> ports = new ArrayList<>();
    private List<Server> servers = new ArrayList<>();
    private List<Image> images = new ArrayList<>();
    private List<Flavor> flavors = new ArrayList<>();
    private List<Volume> volumes = new ArrayList<>();
    private List<NetFloatingIP> floatingIps = new ArrayList<>();
    private List<Router> routers = new ArrayList<>();
    private List<RouterInterface> routerinterface = new ArrayList<>();
    private List<HostRoute> hostroute = new ArrayList<>();
    private List<Hypervisor> hypervisors = new ArrayList<>();
    private List<NovaFloatingIP> novafloatingIps =new ArrayList<>();
    private Map<Server, Map<String, String>> metadata = new HashMap<>();
    
    public  OpenStackGet(String url,String NATServer, String username, String password, String tenantName) {
        Authenticate authenticate = new Authenticate();        
        client = authenticate.openStackAuthenticate(url,NATServer, username, password, tenantName);
        fetchAddResources(client);
    }

    OpenStackGet(String url, String user_name, String password, String tenantName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void fetchAddResources(OSClient client) {
        List<? extends Network> nets = client.networking().network().list();
        if (nets != null && !nets.isEmpty()) {
            networks.addAll(nets);
        }
        List<? extends Subnet> subs = client.networking().subnet().list();
        if (subs != null && !subs.isEmpty()) {
            subnets.addAll(subs);
        }
        List<? extends Port> pors = client.networking().port().list();
        if (pors != null && !pors.isEmpty()) {
            ports.addAll(pors);
        }
        try {
            List<? extends Hypervisor> hyps = client.compute().hypervisors().list();
            if (hyps != null && !hyps.isEmpty()) {
                hypervisors.addAll(hyps);
            }
        } catch (Exception e) {
            ;
        }
        List<? extends Server> sers = client.compute().servers().list();
        if (sers != null && !sers.isEmpty()) {
            servers.addAll(sers);
        }
        List<? extends Image> imgs = client.compute().images().list();
        if (imgs != null && !imgs.isEmpty()) {
            images.addAll(imgs);
        }
        List<? extends Flavor> flas = client.compute().flavors().list();
        if (flas != null && !flas.isEmpty()) {
            flavors.addAll(flas);
        }
        List<? extends Volume> vols = client.blockStorage().volumes().list();
        if (vols != null && !vols.isEmpty()) {
            volumes.addAll(vols);
        }
        List<? extends NetFloatingIP> fips = client.networking().floatingip().list();
        if (fips != null && !fips.isEmpty()) {
            floatingIps.addAll(fips);
        }
        List<? extends Router> rous = client.networking().router().list();
        if (rous != null && !rous.isEmpty()) {
            routers.addAll(rous);
        }
        List<? extends NovaFloatingIP> nfips = (List<NovaFloatingIP>) client.compute().floatingIps().list();
        if (nfips != null && !nfips.isEmpty()) {
            novafloatingIps.addAll(nfips);
        }
        for (Server server : servers) {
            Map<String, String> data = client.compute().servers().getMetadata(server.getId());
            if (data == null) {
                continue;
            }
            if (metadata == null && !data.isEmpty()) {
                metadata = new HashMap<>();
            }
            if (!data.isEmpty()) {
                metadata.put(server, data);
            }
        }
    }

    public void updateResources(String type) {
        switch (type) {
            case "Network":
                networks.clear();
                List<? extends Network> nets = client.networking().network().list();
                if (nets != null && !nets.isEmpty()) {
                    networks.addAll(nets);
                }
                break;
            case "Subnet":
                subnets.clear();
                List<? extends Subnet> subs = client.networking().subnet().list();
                if (subs != null && !subs.isEmpty()) {
                    subnets.addAll(subs);
                }
                break;
            case "Port":
                ports.clear();
                List<? extends Port> pors = client.networking().port().list();
                if (pors != null && !pors.isEmpty()) {
                    ports.addAll(pors);
                }
                break;
            case "Server":
                servers.clear();
                List<? extends Server> sers = client.compute().servers().list();
                if (sers != null && !sers.isEmpty()) {
                    servers.addAll(sers);
                }
                break;
            case "Image":
                images.clear();
                List<? extends Image> imgs = client.compute().images().list();
                if (imgs != null && !imgs.isEmpty()) {
                    images.addAll(imgs);
                }
                break;
            case "Flavor":
                flavors.clear();
                List<? extends Flavor> flas = client.compute().flavors().list();
                if (flas != null && !flas.isEmpty()) {
                    flavors.addAll(flas);
                }
                break;
            case "Volume":
                volumes.clear();
                List<? extends Volume> vols = client.blockStorage().volumes().list();
                if (vols != null && !vols.isEmpty()) {
                    volumes.addAll(vols);
                }
                break;
            case "NetFloatingIP":
                floatingIps.clear();
                List<? extends NetFloatingIP> fips = client.networking().floatingip().list();
                if (fips != null && !fips.isEmpty()) {
                    floatingIps.addAll(fips);
                }
                break;
            case "Router":
                routers.clear();
                List<? extends Router> rous = client.networking().router().list();
                if (rous != null && !rous.isEmpty()) {
                    routers.addAll(rous);
                }
                break;
            case "NovaFloatingIP":
                novafloatingIps.clear();
                List<? extends NovaFloatingIP> nfips = (List<NovaFloatingIP>) client.compute().floatingIps().list();
                if (nfips != null && !nfips.isEmpty()) {
                    novafloatingIps.addAll(nfips);
                }
                break;
            case "MetaData":
                metadata.clear();
                for (Server server : servers) {
                    Map<String, String> data = client.compute().servers().getMetadata(server.getId());
                    if (metadata == null && !data.isEmpty()) {
                        metadata = new HashMap<>();
                    }
                    if (!data.isEmpty()) {
                        metadata.put(server, data);
                    }
                }
                break;
            default:
                ;
        }
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

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public List<Flavor> getFlavors() {
        return flavors;
    }

    public void setFlavors(List<Flavor> flavors) {
        this.flavors = flavors;
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

    //get a floating ip  by its floatingIP address
    public NetFloatingIP findFloatingIp(String ip) {
            List<? extends NetFloatingIP> fips = client.networking().floatingip().list();
        for (NetFloatingIP fip : fips) {
            if (fip.getFloatingIpAddress().equals(ip)) {
                return fip;
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

    public Map<Server, Map<String, String>> getMetadata() {
        return metadata;
    }

    public Map<String, String> getMetadata(Server server) {
        Map<String, String> data = client.compute().servers().getMetadata(server.getId());
        if (data == null)
            return null;
        metadata.put(server, data);        
        return data;
    }
    
    public String getMetadata(Server server, String key){
        Map<String, String> data = this.getMetadata(server);
        if (data == null)
            return null;
        if (!data.containsKey(key)) {            
            return null;
        }
        return data.get(key);
    }
    
    public void setMetadata(String serverId, String key, String value){ 
        Server server = this.getServer(serverId);
        if (server == null)
            return;
        Map<String, String> data = client.compute().servers().getMetadata(server.getId());
        if (data == null) {
            data = new HashMap();
        }
        data.put(key, value);
        metadata.put(server, data);
        client.compute().servers().updateMetadata(server.getId(), data);
    }
}
