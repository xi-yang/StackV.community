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

    public static void main(String args[]) throws IOException, Exception {

        //createOntology("http://lab-blade.maxgigapop.net:35357/v2.0","206.196.176.151", "urn:ogf:network:lab-blade.maxgigapop.net:2015", "cjohnson", "67ou2UbR3", "admin");
        createOntology("http://max-vlsr2.dragon.maxgigapop.net:35357/v2.0","", "urn:ogf:network:max-vlsr2.maxgigapop.net:2015", "admin", "1234", "admin");
        

    }
}
