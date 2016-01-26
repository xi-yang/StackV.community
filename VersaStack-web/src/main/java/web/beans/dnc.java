/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dnc;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package web.beans;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Map;

/**
 *
 * @author ranjitha
 */
public class Dnc {
    
     public  int createConnection(Map<String, String> paraMap){
     
        String topoUri = null;
        List<String> connection = new ArrayList<>();
        int i=1;String connPara;
        String vgUuid = null;int x =1;
                
        for(Map.Entry<String, String> entry : paraMap.entrySet()){
            if(entry.getKey().equalsIgnoreCase("topoUri"))
                topoUri = entry.getValue();
            else if(entry.getKey().equalsIgnoreCase("conn"))
                connection.add(entry.getValue());
        


            
        /*paraMap.put(“topoUri”, “urn:ogf:network:vo1.maxgigapop.net:link”);
          paraMap.put(“conn1”, “urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*&
          vlan_tag+3021-3029\r\nurn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*&vlan_tag+3021-3029”);
          paraMap.put(“conn2”,“urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*&
          vlan_tag+3021-3029\r\nurn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*&vlan_tag+3021-3029”);*/
            
        JSONObject jsonConnect = new JSONObject();   
        JSONArray jsonLink = new JSONArray();
        //
            for(String link : connection){
                connPara = topoUri+"conn"+i;
                // <<<<<<urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*&vlan_tag+3021-3029>>>>>\r\n<<<<<<urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-1-2:link=*&vlan_tag+3021-3029>>>>>”
                JSONObject jsonPort = new JSONObject();
                String[] linkPara = link.split("\r\n");
                    for(String port : linkPara){
                        //<<<<<<urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*>>>>>>&<<<<<vlan_tag+3021-3029>>>>>>
                        String[] portPara = port.split("&");
                        JSONObject jsonVlan = new JSONObject();
                            for(String vlan : portPara){
                                if(vlan.contains("vlan"))
                                {
                                    //vlan_tag+3021-3029
                                    String[] vlanPara = vlan.split("+");
                                    jsonVlan.put(vlanPara[0], vlanPara[1]);
                                }
                            jsonPort.put(portPara[0], jsonVlan);   
                            }
                    jsonLink.add(jsonPort);        
                    }
                    i++;
            jsonConnect.put(connPara, jsonLink);
            }
            
      System.out.println(jsonConnect.toString());    
      
     String delta = "<serviceDelta>\n<uuid>" + UUID.randomUUID().toString()+
                "</uuid>\n<workerClassPath>net.maxgigapop.mrs.service.orchestrate.SimpleWorker</workerClassPath>"+
                "\n\n<modelAddition>\n" 
                + "@prefix rdfs:  &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .\n"
                + "@prefix owl:   &lt;http://www.w3.org/2002/07/owl#&gt; .\n"
                + "@prefix xsd:   &lt;http://www.w3.org/2001/XMLSchema#&gt; .\n"
                + "@prefix rdf:   &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .\n"
                + "@prefix nml:   &lt;http://schemas.ogf.org/nml/2013/03/base#&gt; .\n"
                + "@prefix mrs:   &lt;http://schemas.ogf.org/mrs/2013/12/topology#&gt; .\n\n";
    
    for(String link : connection){
        
        delta += "&lt;urn:ogf:network:vo1.maxgigapop.net:link=conn" + i + "&gt;\n" +
                 "a            mrs:SwitchingSubnet ;\n" +
                 "spa:dependOn &lt;x-policy-annotation:action:create-path&gt;.\n\n";
        i++;
    }
    
    delta += "&lt;x-policy-annotation:action:create-path&gt;\n"+
    "a            spa:PolicyAction ;\n"+
    "spa:type     \"MCE_MPVlanConnection\" ;\n"+
    "spa:importFrom &lt;x-policy-annotation:data:conn-criteria&gt ;\n"+
    "spa:exportTo &lt;x-policy-annotation:data:conn-criteriaexport&gt; .\n\n"+
    "&lt;x-policy-annotation:data:conn-criteria&gt;\n"+
    "a            spa:PolicyData;\n"+
    "spa:type     \"JSON\";"+
    "spa:value    \"\"\""+ jsonConnect.toString().replace("\\", "") +
    "\"\"\".\n\n&lt;x-policy-annotation:data:vpc-criteriaexport&gt;\n" +
    "    a            spa:PolicyData;\n\n" + 
    "</modelAddition>\n\n" +
    "</serviceDelta>";
              
     }
        
    
        return 0;
     }
}
     
     

