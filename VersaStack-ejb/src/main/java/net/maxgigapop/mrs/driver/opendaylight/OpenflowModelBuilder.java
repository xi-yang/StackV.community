/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.maxgigapop.mrs.driver.opendaylight;

import net.maxgigapop.mrs.driver.onosystem.*;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import net.maxgigapop.mrs.common.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import javax.ejb.EJBException;

//TODO Escape \:


public class OpenflowModelBuilder {
    private static final Logger logger = Logger.getLogger(OnosRESTDriver.class.getName());

    public static OntModel createOntology(String topologyURI, String subsystemBaseUrl, String username, String password) {
        //create model object
        OntModel model = ModelUtil.newMrsOntModel(topologyURI);
        Resource resTopo = RdfOwl.createResource(model, topologyURI, Nml.Topology);
        //retrieve ODL information into model
        RestconfConnector restconf = new RestconfConnector();
        JSONObject jsonTopology = restconf.getNetworkTopology(subsystemBaseUrl, username, password);
        try {
            Object r = JsonPath.parse(jsonTopology).read("$.network-topology..node");
            if (r != null && r instanceof net.minidev.json.JSONArray && !((net.minidev.json.JSONArray)r).isEmpty()) {
                r = ((net.minidev.json.JSONArray)r).get(0);
                for (Object o1: (JSONArray)r) {
                    JSONObject j1 = (JSONObject)o1;
                    if(!j1.containsKey("node-id")) {
                        continue;
                    }
                    String nodeId = (String)j1.get("node-id");
                    Resource resNode = RdfOwl.createResource(model, topologyURI + ":node+" + nodeId, Nml.Node);
                    model.add(model.createStatement(resTopo, Nml.hasNode, resNode));
                    Resource resOpenFlow = RdfOwl.createResource(model, resNode.getURI() + ":openflow-service" , Mrs.OpenflowService);
                    model.add(model.createStatement(resNode, Nml.hasService, resOpenFlow));
                    if (!j1.containsKey("termination-point")) {
                        continue;
                    }
                    for (Object o2: (JSONArray)j1.get("termination-point")) {
                        JSONObject j2 = (JSONObject)o2;
                        if(!j2.containsKey("tp-id")) {
                            continue;
                        }
                        Resource resPort = RdfOwl.createResource(model, resNode.getURI() + ":port+" + j2.get("tp-id"), Nml.BidirectionalPort);
                        model.add(model.createStatement(resNode, Nml.hasBidirectionalPort, resPort));
                        model.add(model.createStatement(resOpenFlow, Nml.hasBidirectionalPort, resPort));
                    }
                }
            } else {
                throw new EJBException(String.format("OpenflowModelBuilder.createOntology failed to retrieve topology nodes from jsonTopology."));
            }
        } catch (Exception ex) {
            throw new EJBException(String.format("OpenflowModelBuilder.createOntology failed to parse the jsonTopology for nodes.", ex));
        }
        try {
            Object r = JsonPath.parse(jsonTopology).read("$.network-topology..link");
            if (r != null && r instanceof net.minidev.json.JSONArray && !((net.minidev.json.JSONArray)r).isEmpty()) {
                r = ((net.minidev.json.JSONArray)r).get(0);
                for (Object o1: (JSONArray)r) {
                    JSONObject j1 = (JSONObject)o1;
                    if(!j1.containsKey("source") || !j1.containsKey("destination")) {
                        continue;
                    }
                    String srcNode = (String)((JSONObject)j1.get("source")).get("source-node");
                    String srcPort = (String)((JSONObject)j1.get("source")).get("source-tp");
                    Resource resSrc = model.getResource(topologyURI + ":node+" + srcNode+":port+"+srcPort);
                    String dstNode = (String)((JSONObject)j1.get("destination")).get("dest-node");
                    String dstPort = (String)((JSONObject)j1.get("destination")).get("dest-tp");
                    Resource resDst = model.getResource(topologyURI + ":node+" + dstNode+":port+"+dstPort);
                    model.add(model.createStatement(resSrc, Nml.isAlias, resDst));
                }
            } else {
                logger.warning("OpenflowModelBuilder.createOntology failed to retrieve topology links from jsonTopology.");
            }
        } catch (Exception ex) {
            logger.warning(String.format("OpenflowModelBuilder.createOntology failed to parse the jsonTopology for links.", ex));
        }

        JSONObject jsonFlows = restconf.getConfigFlows(subsystemBaseUrl, username, password);
        /*
         for (int i = 0; i < qtyDevices; i++) {
            //add device to model
            Resource resNode = RdfOwl.createResource(model, topologyURI + ":" + device[i][0], node);
            model.add(model.createStatement(onosTopology, hasNode, resNode));
            
            Resource resOpenFlow = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service", openflowService);
            model.add(model.createStatement(resNode, hasService, resOpenFlow));

            //get all devicePorts and add to model
            if (device[i][1].equals("SWITCH") && device[i][2].equals("true")) {
                String devicePorts[][] = onos.getOnosDevicePorts(subsystemBaseUrl, device[i][0], access_key_id, secret_access_key);
                int qtyPorts = devicePorts.length;

                for (int j = 0; j < qtyPorts; j++) {
                    if (devicePorts[j][1].equals("true")) {

                        Resource resPort = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":port-" + devicePorts[j][4], biPort);
                        model.add(model.createStatement(resNode, hasBidirectionalPort, resPort));
                        
                        model.add(model.createStatement(resOpenFlow, hasBidirectionalPort, resPort));

                        //write src_portName and dst_portName into links[][6] and links[][7]
                        for (int k = 0; k < qtyLinks; k++) {
                            if (device[i][0].equals(links[k][0]) && devicePorts[j][0].equals(links[k][1])) {
                                links[k][6] = devicePorts[j][4];
                            }
                            if (device[i][0].equals(links[k][2]) && devicePorts[j][0].equals(links[k][3])) {
                                links[k][7] = devicePorts[j][4];
                            }
                        }
                    }
                }

                //add flow per device into model
                String deviceFlows[][] = onos.getOnosDeviceFlows(topologyURI, subsystemBaseUrl, device[i][0], mappingIdMatrix, mappingIdSize, access_key_id, secret_access_key);
                int qtyFlows = deviceFlows.length;
                
                //??
                Resource resOpenFlow = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service", openflowService);
                if (qtyFlows > 0) {
                    model.add(model.createStatement(resNode, hasService, resOpenFlow));
                }
                
                for (int j = 0; j < qtyFlows; j++) {

                    //add a flow table for each groupId
                    Resource resFlowTable = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + deviceFlows[j][1], flowTable);
                    model.add(model.createStatement(resOpenFlow, providesFlowTable, resFlowTable));

                    //add each flows in each flowTable
                    Resource resFlow = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + deviceFlows[j][1] + ":flow-" + deviceFlows[j][0], flow);
                    model.add(model.createStatement(resFlowTable, providesFlow, resFlow));

                    Resource resFlowRule0 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + deviceFlows[j][1] + ":flow-" + deviceFlows[j][0] + ":rule-match-0", flowRule);
                    Resource resFlowRule1 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + deviceFlows[j][1] + ":flow-" + deviceFlows[j][0] + ":rule-match-1", flowRule);
                    Resource resFlowRule2 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + deviceFlows[j][1] + ":flow-" + deviceFlows[j][0] + ":rule-match-2", flowRule);
                    Resource resFlowRule3 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + deviceFlows[j][1] + ":flow-" + deviceFlows[j][0] + ":rule-match-3", flowRule);
                    Resource resFlowRule4 = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + deviceFlows[j][1] + ":flow-" + deviceFlows[j][0] + ":rule-match-4", flowRule);
                    Resource resFlowAction = RdfOwl.createResource(model, topologyURI + ":" + device[i][0] + ":openflow-service:flow-table-" + deviceFlows[j][1] + ":flow-" + deviceFlows[j][0] + ":rule-action-0", flowRule);

                    //flowRule0: in_port
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule0));
                    model.add(model.createStatement(resFlowRule0, type, "IN_PORT"));
                    model.add(model.createStatement(resFlowRule0, value, deviceFlows[j][4]));

                    //flowRule1: ETH_SRC_MAC
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule1));
                    model.add(model.createStatement(resFlowRule1, type, "ETH_SRC_MAC"));
                    model.add(model.createStatement(resFlowRule1, value, deviceFlows[j][6]));

                    //flowRule2: ETH_DST_MAC
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule2));
                    model.add(model.createStatement(resFlowRule2, type, "ETH_DST_MAC"));
                    model.add(model.createStatement(resFlowRule2, value, deviceFlows[j][5]));

                    //flowRule3: ETH_SRC_VLAN
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule3));
                    model.add(model.createStatement(resFlowRule3, type, "ETH_SRC_VLAN"));
                    model.add(model.createStatement(resFlowRule3, value, deviceFlows[j][7]));

                    //flowRule4: ETH_DST_VLAN
                    model.add(model.createStatement(resFlow, flowMatch, resFlowRule4));
                    model.add(model.createStatement(resFlowRule4, type, "ETH_DST_VLAN"));
                    model.add(model.createStatement(resFlowRule4, value, deviceFlows[j][8]));

                    //flowAction: OUT_PORT
                    model.add(model.createStatement(resFlow, flowAction, resFlowAction));
                    model.add(model.createStatement(resFlowAction, type, "OUT_PORT"));
                    model.add(model.createStatement(resFlowAction, value, deviceFlows[j][3]));

                }

            }
        }
        
        for (int i = 0; i < qtyHosts; i++) {
            //add hosts mac to model
            Resource resNode = RdfOwl.createResource(model, topologyURI + ":" + hosts[i][0], node);
            model.add(model.createStatement(onosTopology, hasNode, resNode));

            Resource resMac = RdfOwl.createResource(model, topologyURI + ":" + hosts[i][0] + ":macAddress", networkAddress);
            Resource resIP = RdfOwl.createResource(model, topologyURI + ":" + hosts[i][0] + ":ipAddress", networkAddress);
         //Resource resLocation = RdfOwl.createResource(model, topologyURI + ":" + hosts[i][0] + ":location", location);

            model.add(model.createStatement(resNode, hasNetworkAddress, resMac));
            model.add(model.createStatement(resNode, hasNetworkAddress, resIP));
         //model.add(model.createStatement(resNode, locatedAt, resLocation));

            model.add(model.createStatement(resMac, type, "macAddresses"));
            model.add(model.createStatement(resMac, value, hosts[i][1]));
            model.add(model.createStatement(resIP, type, "ipAddresses"));
            model.add(model.createStatement(resIP, value, hosts[i][3]));

            for (int j = 0; j < qtyDevices; j++) {
                if (device[j][0].equals(hosts[i][4]) && device[j][2].equals("true")) {
                    String checkPort[][] = onos.getOnosDevicePorts(subsystemBaseUrl, device[j][0], access_key_id, secret_access_key);
                    int portNum = checkPort.length;
                    for (int k = 0; k < portNum; k++) {
                        if (checkPort[k][0].equals(hosts[i][5]) && checkPort[k][1].equals("true")) {
                            Resource resPort = RdfOwl.createResource(model, topologyURI + ":" + device[j][0] + ":port-" + checkPort[k][4], biPort);
                        //model.add(model.createStatement(resLocation, type, "port"));
                            //model.add(model.createStatement(resLocation, value, resPort));
                            model.add(model.createStatement(resNode, locatedAt, resPort));
                        }
                    }
                }
            }

        }
         

        for (int i = 0; i < qtyLinks; i++) {
            if (links[i][5].equals("ACTIVE")) {
                //add link into model
                Resource resSrcPort = RdfOwl.createResource(model, topologyURI + ":" + links[i][0] + ":port-" + links[i][6], biPort);
                Resource resDstPort = RdfOwl.createResource(model, topologyURI + ":" + links[i][2] + ":port-" + links[i][7], biPort);
                model.add(model.createStatement(resSrcPort, Nml.isAlias, resDstPort));
            }
        }

        //manully read from a SRRG json file 
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(srrgFile);

        JSONArray f = (JSONArray) jsonObject.get("SRRG");
        int srrg_num = f.size();

        for (int i = 0; i < srrg_num; i++) {
            JSONObject t = (JSONObject) f.get(i);
            String id = t.get("id").toString();
            Resource resSRRG = RdfOwl.createResource(model, topologyURI + ":" + id, SRRG);
            model.add(model.createStatement(onosTopology, hasNode, resSRRG));

            String severity_str = t.get("severity").toString();
            String occurenceProbability_str = t.get("occurenceProbability").toString();

            String devices_str = t.get("devices").toString();
            if (devices_str.equals("")) {
                Resource resNode = RdfOwl.createResource(model, "", node);
                model.add(model.createStatement(resSRRG, hasNode, resNode));
            } else {
                List<String> devices_list = Arrays.asList(devices_str.split(","));
                int devices_list_size = devices_list.size();

                for (int j = 0; j < devices_list_size; j++) {
                    Resource resNode = RdfOwl.createResource(model, topologyURI + ":" + devices_list.get(j).toString(), node);
                    model.add(model.createStatement(resSRRG, hasNode, resNode));
                }
            }

            String ports_str = t.get("bidirectionalPorts").toString();
            if (ports_str.equals("")) {
                Resource resPort = RdfOwl.createResource(model, "", biPort);
                model.add(model.createStatement(resSRRG, hasBidirectionalPort, resPort));
            } else {
                List<String> ports_list = Arrays.asList(ports_str.split(","));
                int ports_list_size = ports_list.size();

                for (int j = 0; j < ports_list_size; j++) {
                    Resource resPort = RdfOwl.createResource(model, topologyURI + ":" + ports_list.get(j).toString(), biPort);
                    model.add(model.createStatement(resSRRG, hasBidirectionalPort, resPort));
                }
            }

            model.add(model.createStatement(resSRRG, severity, severity_str));
            model.add(model.createStatement(resSRRG, occurenceProbability, occurenceProbability_str));
        }
        */
        return model;
    }
}
