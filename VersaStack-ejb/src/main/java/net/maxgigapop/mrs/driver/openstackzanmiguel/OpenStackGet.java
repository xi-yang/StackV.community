/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import java.util.List;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.*;

/**
 *
 * @author max
 */
public class OpenStackGet 
{
    public OSClient client = null;
    private List <? extends Network> networks = null;
    private List <? extends Subnet> subnets =null;
    private List <? extends Port> ports =null;
    
    public OpenStackGet(String url, String username, String password, String tenantName)
    {
        //authenticate
        Authenticate authenticate = new Authenticate();
        client =  authenticate.openStackAuthenticate(url,username,password,tenantName);
        
        //get the resources
        networks = client.networking().network().list();
        subnets = client.networking().subnet().list();
        ports = client.networking().port().list();
        
    }
    
    //get all the networks in the tenant
    public List<? extends Network> getNetworks()
    {
       return networks;
    }
    
    //get a network by its name
    public Network getNetwork(String name)
    {
        for(Network net: networks)
        {
            if(net.getName().equals(name))
                return net;
        }
        return null;
    }
    
    //get all the sybnets in the tenant
    public List<? extends Subnet> getSubnets()
    {
       return subnets;
    }
    
    //get a network by its name
    public Subnet getSubnet(String name)
    {
        for(Subnet net: subnets)
        {
            if(net.getName().equals(name))
                return net;
        }
        return null;
    }
    
    //get all the ports in the tenant
    public List<? extends Port> getPorts()
    {
        return ports;
    }
    
    //get a port by its name
    public Port getPort(String name)
    {
        for(Port port : ports)
        {
            if(port.getName().equals(name))
                return port;
        }
        return null;
    }
    
    
}
