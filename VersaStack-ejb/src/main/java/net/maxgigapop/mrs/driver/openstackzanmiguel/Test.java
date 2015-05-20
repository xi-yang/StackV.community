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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.JsonObject;
import org.openstack4j.model.network.NetworkType;


import static net.maxgigapop.mrs.driver.openstackzanmiguel.OpenStackNeutronModelBuilder.createOntology;
import org.json.simple.JSONObject;
import org.apache.jena.atlas.json.*;


/**
 *
 * @author tcm
 */
public class Test {

    public static void main(String args[]) throws IOException, Exception {

        //createOntology("http://lab-blade.maxgigapop.net:35357/v2.0", "206.196.176.151", "lab-blade.maxgigapop.net", "cjohnson", "67ou2UbR3", "admin");
        OpenStackGet client  = new OpenStackGet("", "", "", "", "");
        //client.getRouter("miguel's router").toBuilder().route("150.0.0.0/24", "10.196.175.1");
        //client.getClient().networking().router().update(client.getRouter("miguel's router"));
        //System.out.println(client.getRouter("miguel's router").getRoutes());
        List<JSONObject> l = new ArrayList();
        
        JSONObject o = new JSONObject();
        o.put("request","request1");
        o.put("r1parameter1","paramter1");
        JSONObject p = new JSONObject();
        p.put("request", "request2");
        p.put("r2parameter1","pam1");
        p.put("r2parameter2","pam2");
        l.add(o);
        l.add(p);
        System.out.println(o.toString());
        HashMap<String,Object> result =
        new ObjectMapper().readValue(o.toJSONString(), HashMap.class);
        JSONObject m = new JSONObject(result);
        System.out.println(m);
        
    }
}
