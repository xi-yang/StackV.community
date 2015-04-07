/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.openstackget;

import static com.hp.hpl.jena.enhanced.BuiltinPersonalities.model;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openstack4j.model.network.Network;

/**
 *
 * @author tcm
 */
public class ModelAdd {
   /* for(Network net : networks) {
            
            JSONArray node = (JSONArray) ((JSONObject) o).get("host");
            
            if(node != null) {
                
                JSONObject resource = (JSONObject) ((JSONObject)node.get(0)).get("resource");
                String nodeName = (String) resource.get("host");
                Long numCpu = (Long) resource.get("cpu");
                Long memMb = (Long) resource.get("memory_mb");
                Long diskGb = (Long) resource.get("disk_gb");
                
                Resource computeNode = model.createResource("urn:ogf:network:dragon.maxgigapop.net:" + nodeName);
                Literal cpu = model.createTypedLiteral(numCpu);
                Literal mem = model.createTypedLiteral(memMb);
                Literal disk = model.createTypedLiteral(diskGb);
                
                model.add(model.createStatement(OpenstackTopology, hasNode, computeNode));
                model.add(model.createStatement(computeNode, type, Node));
                model.add(model.createStatement(computeNode, type, NamedIndividual));
                model.add(model.createStatement(computeNode, hasService, Nova));
                
                model.add(model.createStatement(computeNode, memory_mb, mem));
                model.add(model.createStatement(computeNode, disk_gb, disk));
                model.add(model.createStatement(computeNode, num_core, cpu));                               
            }
    */
}
