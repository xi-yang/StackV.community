/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import org.openstack4j.api.OSClient;
import org.openstack4j.core.transport.Config;
import org.openstack4j.openstack.OSFactory;

/**
 *
 * @author max
 */
public class Authenticate {

    public OSClient openStackAuthenticate(String url,String NATServer, String username, String password, String tenantName) {

        //define OS Client
        OSClient client = null;        
       // If the OpenStack controller  is behind NAT, it needs to be specified
       //to authenticate 
        if (NATServer == null || NATServer.isEmpty()) {
            client = OSFactory.builder()
                    .endpoint(url)
                    .credentials(username, password)
                    .tenantName(tenantName)
                    .authenticate();

        } 
        else {
            Config conf = Config.DEFAULT;
            client = OSFactory.builder()
                    .endpoint(url)
                    .credentials(username, password)
                    .tenantName(tenantName)
                    .withConfig(Config.newConfig().withEndpointNATResolution(NATServer))
                    .authenticate();
        }

        return client;
    }

}
