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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import net.maxgigapop.mrs.common.ModelUtil;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.common.StackLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class OpenflowPush {

    private static final StackLogger logger = OpenflowRestconfDriver.logger;

    public String propagate(String modelRefTtl, String modelAddTtl, String modelReductTtl) {
        String method = "propagate";
        JSONObject jRequests = new JSONObject();
        try {
            OntModel modelRef = ModelUtil.unmarshalOntModel(modelRefTtl);
            OntModel modelAdd = ModelUtil.unmarshalOntModel(modelAddTtl);
            OntModel modelReduct = ModelUtil.unmarshalOntModel(modelReductTtl);
            JSONArray jDelete = this.extractFlows(modelRef, modelReduct);
            JSONArray jCreate = this.extractFlows(modelRef, modelAdd);
            jRequests.put("delete", jDelete);
            jRequests.put("create", jCreate);
        } catch (Exception ex) {
            throw logger.throwing(method, "failed to parse delta into requests", ex);
        }
        return jRequests.toJSONString();

    }

    private JSONArray extractFlows(OntModel modelRef, OntModel model) {
        JSONArray jFlows = new JSONArray();

        String query = "SELECT ?flow  WHERE {"
                + "?flow a mrs:Flow. "
                + "}";
        ResultSet r1 = ModelUtil.executeQuery(query, null, model);
        while (r1.hasNext()) {
            QuerySolution qs1 = r1.next();
            Resource flow = qs1.get("flow").asResource();
            Resource resTable = null;
            JSONObject jFlow = new JSONObject();
            jFlow.put("id", flow.getURI());
            JSONArray jFlowMatches = new JSONArray();
            jFlow.put("match", jFlowMatches);
            JSONArray jFlowActions = new JSONArray();
            jFlow.put("action", jFlowActions);
            String query2 = "SELECT ?table ?matchtype ?matchvalue ?actiontype ?actionvalue WHERE {"
                    + String.format("?table mrs:hasFlow <%s>. ", flow.getURI())
                    + String.format("<%s> mrs:flowMatch ?match. ?match mrs:type ?matchtype. ?match mrs:value ?matchvalue. ", flow.getURI())
                    + "}";
            ResultSet r2 = ModelUtil.executeQuery(query2, null, model);
            while (r2.hasNext()) {
                QuerySolution qs2 = r2.next();
                String matchType = qs2.get("matchtype").toString();
                String matchValue = qs2.get("matchvalue").toString();
                String strMatch = (matchType + "=" + matchValue);
                jFlowMatches.add(strMatch);
                if (resTable == null) {
                    resTable = qs2.getResource("table");
                }
            }
            query2 = "SELECT ?table ?matchtype ?matchvalue ?action ?actiontype ?actionvalue ?actionorder WHERE {"
                    + String.format("?table mrs:hasFlow <%s>. ", flow.getURI())
                    + String.format("<%s> mrs:flowAction ?action. ?action mrs:type ?actiontype. ?action mrs:value ?actionvalue. ", flow.getURI())
                    + String.format("OPTIONAL {?action mrs:order ?actionorder.} ", flow.getURI())
                    + "}";
            r2 = ModelUtil.executeQuery(query2, null, model);
            SortedMap<String, String> sortedActions = new TreeMap();
            while (r2.hasNext()) {
                QuerySolution qs2 = r2.next();
                String actionType = qs2.get("actiontype").toString();
                String actionValue = qs2.get("actionvalue").toString();
                String actionUri = qs2.get("action").toString();
                String strAction = (actionType + "=" + actionValue);
                if (qs2.contains("actionorder")) {
                    sortedActions.put(qs2.get("actionorder").toString(), strAction);
                } else {
                    sortedActions.put(actionUri, strAction);                    
                }
                if (resTable == null) {
                    resTable = qs2.getResource("table");
                }
            }
            if (resTable == null) {
                continue;
            }
            jFlowActions.addAll(sortedActions.values());
            String query3 = "SELECT ?node_name ?table_name  WHERE {"
                    + String.format("?node nml:hasService ?openflow_svc. ?openflow_svc mrs:providesFlowTable <%s>. ", resTable.getURI())
                    + String.format("?node nml:name ?node_name. <%s> nml:name ?table_name. ", resTable.getURI())
                    + "}";
            ResultSet r3 = ModelUtil.executeQuery(query3, null, modelRef);
            if (r3.hasNext()) {
                QuerySolution qs3 = r3.next();
                String nodeName = qs3.get("node_name").toString();
                jFlow.put("node", nodeName);
                String tableName = qs3.get("table_name").toString();
                jFlow.put("table", tableName);
            }

            jFlows.add(jFlow);
        }
        return jFlows;
    }

    public void commit(String user, String password, String requests, String baseUrl) {
        String method = "commit";
        JSONParser jsonParser = new JSONParser();
        JSONObject jRequests = null;
        try {
            jRequests = (JSONObject) jsonParser.parse(requests);
        } catch (ParseException ex) {
            throw logger.throwing(method, "failed to parse  JSON requests=" + requests, ex);
        }
        RestconfConnector restconf = new RestconfConnector();
        JSONArray jDelete = (JSONArray) jRequests.get("delete");
        for (Object o1 : jDelete) {
            JSONObject jFlow = (JSONObject) o1;
            if (!jFlow.containsKey("node") || !jFlow.containsKey("table") || !jFlow.containsKey("id")) {
                logger.warning(method, "cannot delete invalid flow =" + jFlow.toJSONString());
                continue;
            }
            try {
                restconf.pushDeleteFlow(baseUrl, user, password, jFlow.get("node").toString(), jFlow.get("table").toString(), jFlow.get("id").toString());
            } catch (Exception ex) {
                throw logger.throwing(method, ex);
            }
        }
        JSONArray jCreate = (JSONArray) jRequests.get("create");
        for (Object o1 : jCreate) {
            JSONObject jFlow = (JSONObject) o1;
            if (!jFlow.containsKey("node") || !jFlow.containsKey("table") || !jFlow.containsKey("id")) {
                logger.warning(method, "cannot create invalid flow=" + jFlow.toJSONString());
                continue;
            }
            try {
                restconf.pushModFlow(baseUrl, user, password, jFlow.get("node").toString(), jFlow.get("table").toString(), jFlow.get("id").toString(), (List) jFlow.get("match"), (List) jFlow.get("action"));
            } catch (Exception ex) {
                throw logger.throwing(method, ex);
            }
        }
    }

}
