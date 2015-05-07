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

    public OSClient openStackAuthenticate(String url, String username, String password, String tenantName) {
        
        // Authenticate
        Config conf = Config.DEFAULT;
        OSClient client = OSFactory.builder()
                .endpoint(url)
                .credentials(username,password)
                .tenantName(tenantName)
                .withConfig(Config.newConfig().withEndpointNATResolution("206.196.176.151"))
                .authenticate();
        
        return client;
    }

}
