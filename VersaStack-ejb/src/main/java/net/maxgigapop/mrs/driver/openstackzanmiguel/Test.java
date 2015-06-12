/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.openstackzanmiguel;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import java.io.IOException;

import org.openstack4j.model.network.NetworkType;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.JsonObject;
import org.openstack4j.model.network.NetworkType;

import static net.maxgigapop.mrs.driver.openstackzanmiguel.OpenStackNeutronModelBuilder.createOntology;
import org.json.simple.JSONObject;
import org.apache.jena.atlas.json.*;
import org.openstack4j.model.network.HostRoute;
import org.openstack4j.model.network.Router;
import org.openstack4j.openstack.networking.domain.NeutronHostRoute;

/**
 *
 * @author tcm
 */
public class Test {

    public static void main(String args[]) throws IOException, Exception {


        //createOntology("http://lab-blade.maxgigapop.net:35357/v2.0","206.196.176.151", "urn:ogf:network:lab-blade.maxgigapop.net:2015", "cjohnson", "67ou2UbR3", "admin");
        createOntology("http://max-vlsr2.dragon.maxgigapop.net:35357/v2.0","", "urn:ogf:network:max-vlsr2.maxgigapop.net:2015", "admin", "1234", "admin");
        



        //createOntology();
        OpenStackGet client = new OpenStackGet("", "", "", "", "");
        HostRoute route = new NeutronHostRoute();
        Router r = client.getRouter("miguel's router");
        route = r.getRoutes().get(0);
        r.getRoutes().remove(route);
        client.getClient().networking().router().update(r.toBuilder().route("150.0.0.0/24","10.196.175.1").build());
        System.out.println(client.getClient());
>>>>>>> VersaStack-MiguelUzcategui
>>>>>>> origin/VersaStack-miguel-zan

    }
}
