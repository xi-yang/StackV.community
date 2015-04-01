/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import java.util.List;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.openstack4j.openstack.compute.domain.NovaServer;

/**
 *
 * @author max
 */
public class Test {
    
    public static void  main (String args[])
    {
        Authenticate authenticate = new Authenticate();
        OSClient client =  authenticate.openStackAuthenticate("","","","");
        
        System.out.println(client.networking().subnet().list());
        List<? extends Server> server = client.compute().servers().list();
        
        System.out.println(server.get(0).getFlavor().toString());
        
    }
    
}
