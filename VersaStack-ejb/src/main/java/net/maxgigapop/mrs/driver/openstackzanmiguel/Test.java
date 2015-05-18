/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import java.io.IOException;
import org.openstack4j.model.network.NetworkType;



import static net.maxgigapop.mrs.driver.openstackzanmiguel.OpenStackNeutronModelBuilder.createOntology;




/**
 *
 * @author tcm
 */
public class Test {
    public static void main(String args[]) throws IOException, Exception{
<<<<<<< HEAD
       //OpenStackGet openstackget =  new OpenStackGet("http://max-vlsr2.dragon.maxgigapop.net","admin","1234","admin");
       
       
       //openstackget.getSubnets();
       //openstackget.getNetworks();
       //OpenStackModelBuilder.createOntology("http://max-vlsr2.dragon.maxgigapop.net", "admin", "1234", "urn:ogf:network:max-vlsr2.dragon.maxgigapop.net:openstack-neutron");
        createOntology("http://lab-blade.maxgigapop.net","lab-blade.maxgigapop.net","username","password","admin");
        // createOntology("http://max-vlsr2.dragon.maxgigapop.net","max-vlsr2.dragon.maxgigapop.net","admin","1234","admin");
        
    }
}
=======
        createOntology("http://lab-blade.maxgigapop.net:35357/v2.0","","","","","");      
    }
}
>>>>>>> VersaStack-MiguelUzcategui
