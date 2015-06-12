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
<<<<<<< HEAD
        OSClient client = null;
        
=======
        OSClient client = null;        
>>>>>>> VersaStack-MiguelUzcategui
       // If the OpenStack controller  is behind NAT, it needs to be specified
       //to authenticate 
        if (NATServer.isEmpty()) {
           try{ client = OSFactory.builder()
                    .endpoint(url)
                    .credentials(username, password)
                    .tenantName(tenantName)
                    .withConfig(Config.DEFAULT)
                    .authenticate();
           }catch(Exception e){
               System.out.println("Caught Exception"+ e.getMessage());
           }
           

        } 
        else {
            try{client = OSFactory.builder()
                    .endpoint(url)
                    .credentials(username, password)
                    .tenantName(tenantName)
                    .withConfig(Config.newConfig().withEndpointNATResolution(NATServer))
                    .authenticate();
            }catch(Exception e){
                 System.out.println("Caught Exception"+ e.getMessage());
            }
        }

        return client;
    }

}
