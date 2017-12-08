/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.onosystem;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import net.maxgigapop.mrs.common.ModelUtil;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverSystemDeltaPersistenceManager;
import net.maxgigapop.mrs.common.StackLogger;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author diogo
 */
//TODO availability zone problems in volumes and subnets and instancees.
//add a property in the model to speicfy availability zone.
//TODO associate and disassociate address methods do not do anything. Reason is
//elastic IPs are not linked in any way to the root topology, find a way to do this
//in the model to make the two methods work.
public class OnosPush {

    private static final StackLogger logger = OnosRESTDriver.logger;

    String localFakeMap;
    //private Regions region = null;
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

    public OnosPush() {

    }

    public String pushPropagate(String access_key_id, String secret_access_key, String mappingId, OntModel modelRef, OntModel modelAdd, OntModel modelReduct, String topologyURI, String subsystemBaseUrl) throws Exception {

        String requests = "";
        String reduction_string = "";
        String addition_string = "";
        reduction_string = parseFlowsReduct(modelReduct, mappingId, topologyURI);
        if (reduction_string.length() > 1) {
            requests = "Reduction\n" + reduction_string + "end_Reduct";
        }
        addition_string = parseFlows(modelAdd, topologyURI);

        if (addition_string.length() > 1) {
            requests = requests + "Addition\n" + addition_string + "end_Add";
        }

        return requests;

    }

    public void pushCommit(String access_key_id, String secret_access_key, String model, String fakeMap, String topologyURI, String subsystemBaseUrl, DriverSystemDelta aDelta) throws Exception {
        String method = "pushCommit";
        aDelta = (DriverSystemDelta) DriverSystemDeltaPersistenceManager.findByReferenceUUID(aDelta.getReferenceUUID()); // refresh
        String stringModelAdd = ModelUtil.marshalOntModel(aDelta.getSystemDelta().getModelAddition().getOntModel());
        String newStringModelAdd = stringModelAdd;

        localFakeMap = fakeMap;

        String reduction = "";
        String addition = "";
        if (model.contains("Reduction")) {
            reduction = model.split("Reduction\n")[1].split("\nend_Reduct")[0];

        }
        if (model.contains("Addition")) {
            addition = model.split("Addition\n")[1].split("\nend_Add")[0];

        }
        String[] json_string;
        if (!addition.isEmpty()) {
            json_string = addition.split("\n");
            for (int i = 0; i < json_string.length; i++) {
                URL url = new URL(String.format(subsystemBaseUrl + "/flows/" + json_string[i]));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                String status[] = new String[2];
                status = this.executeHttpMethod(access_key_id, secret_access_key, url, conn, "POST", json_string[i + 2]);
                if (Integer.parseInt(status[0]) != 201) {
                    throw logger.error_throwing(method, String.format("Failed to push %s into %s", json_string[i + 2], json_string[i]));
                }
                String realFlowId = status[1].split(json_string[i] + "/")[1];
                String fakeFlowId = json_string[i + 1];
                if (newStringModelAdd.contains(fakeFlowId)) {
                    //newStringModelAdd=newStringModelAdd.replace(fakeFlowId, realFlowId);
                    localFakeMap = localFakeMap + topologyURI + ":" + json_string[i] + ":openflow-service:flow-table-0:flow-" + fakeFlowId + "-->>" + topologyURI + ":" + json_string[i] + ":openflow-service:flow-table-0:flow-" + realFlowId + "\n";

                }
                //if(newStringDriverModelAdd.contains(fakeFlowId)){
                //    newStringDriverModelAdd=newStringDriverModelAdd.replace(fakeFlowId, realFlowId);
                //}
                i = i + 2;
            }
            //OntModel newModelAdd =  ModelUtil.unmarshalOntModel(newStringModelAdd);
            //aDelta.getSystemDelta().getModelAddition().setOntModel(newModelAdd);

            //OntModel newDriverModelAdd =  ModelUtil.unmarshalOntModel(newStringDriverModelAdd);
            //aDelta.getModelAddition().setOntModel(newDriverModelAdd);
            //aDelta.getModelAddition().saveOrUpdate();
            //aDelta.getSystemDelta().getModelAddition().saveOrUpdate();
        }
        if (!reduction.isEmpty()) {
            json_string = reduction.split("\n");
            for (int i = 0; i < json_string.length; i++) {
                URL url = new URL(String.format(subsystemBaseUrl + "/flows/" + json_string[i] + "/" + json_string[i + 1]));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                String status[] = new String[2];
                String fakeMapMatrix[] = localFakeMap.split("\n");
                int fakeMapSize = fakeMapMatrix.length;
                for (int j = 0; j < fakeMapSize; j++) {

                    if (fakeMapMatrix[j].contains(json_string[i + 1])) {
                        json_string[i + 1] = fakeMapMatrix[j].split("-->>")[1].split("table-0:flow-")[1];
                        localFakeMap = localFakeMap.replaceAll(fakeMapMatrix[j], "");
                    }
                }

                status = this.executeHttpMethod(access_key_id, secret_access_key, url, conn, "DELETE", null);
                if (Integer.parseInt(status[0]) != 204) {
                    throw logger.error_throwing(method, String.format("Failed to delete %s from %s", json_string[i + 1], json_string[i]));
                }
                i++;
            }
        }

    }

    private ResultSet executeQuery(String queryString, OntModel refModel, OntModel model) {
        queryString = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "prefix owl: <http://www.w3.org/2002/07/owl#>\n"
                + "prefix nml: <http://schemas.ogf.org/nml/2013/03/base#>\n"
                + "prefix mrs: <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
                + queryString;

        //get all the nodes that will be added
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet r = qexec.execSelect();

        //check on reference model if the statement is not in the model addition,
        //or model subtraction
        if (!r.hasNext()) {
            qexec = QueryExecutionFactory.create(query, refModel);
            r = qexec.execSelect();
        }
        return r;
    }

    private String[] executeHttpMethod(String access_key_id, String secret_access_key, URL url, HttpURLConnection conn, String method, String body) throws IOException {
        conn.setRequestMethod(method);
        String username = access_key_id;
        String password = secret_access_key;
        String userPassword = username + ":" + password;
        byte[] encoded = Base64.encodeBase64(userPassword.getBytes());
        String stringEncoded = new String(encoded);
        conn.setRequestProperty("Authorization", "Basic " + stringEncoded);
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }
        logger.trace("executeHttpMethod", String.format("Sending %s request to URL : %s", method, url));
        String responseCode[] = new String[2];
        responseCode[1] = null;
        responseCode[1] = conn.getHeaderField("Location");
        responseCode[0] = Integer.toString(conn.getResponseCode());
        logger.trace("executeHttpMethod", "Response Code : " + responseCode[0]);

        return responseCode;
    }

    private String parseFlows(OntModel modelAdd, String topologyURI) {
        String requests = "";
//        String reversible="";

        String query = "SELECT ?flow  WHERE {"
                + "?flow a mrs:Flow. "
                + "}";

        ResultSet r1 = executeQuery(query, emptyModel, modelAdd);

        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();

            Resource flow = querySolution1.get("flow").asResource();
            String query2 = "SELECT ?flowrule ?rulematch WHERE {"
                    + "?flowrule mrs:value ?rulematch. "
                    + "}";

            ResultSet r2 = executeQuery(query2, emptyModel, modelAdd);
            String[] flowdata = new String[6];

            while (r2.hasNext()) {
                QuerySolution querySolution2 = r2.next();
                Resource flow2 = querySolution2.get("flowrule").asResource();
                RDFNode rulematch = querySolution2.get("rulematch");
                if (flow2.toString().equals(flow.toString() + ":rule-match-0")) {
                    flowdata[0] = rulematch.toString();
                    //IN_PORT
                }
                if (flow2.toString().equals(flow.toString() + ":rule-match-1")) {
                    flowdata[1] = rulematch.toString();
                    //ETH_SRC_MAC
                }
                if (flow2.toString().equals(flow.toString() + ":rule-match-2")) {
                    flowdata[2] = rulematch.toString();
                    //ETH_DST_MAC
                }
                if (flow2.toString().equals(flow.toString() + ":rule-match-3")) {
                    flowdata[3] = rulematch.toString();
                    //ETH_SRC_VLAN
                }
                if (flow2.toString().equals(flow.toString() + ":rule-match-4")) {
                    flowdata[4] = rulematch.toString();
                    //ETH_DST_VLAN
                }
                if (flow2.toString().equals(flow.toString() + ":rule-action-0")) {
                    flowdata[5] = rulematch.toString();
                    //OUT_PORT
                }
//                if(flow2.toString().equals(flow.toString()+":reversible")){
//                    reversible=rulematch.toString();                   
//                }
            }

            String[] json_string = new String[3];
            json_string[2] = flow.toString().split("table-0:flow-")[1] + "\n";
            json_string[0] = flow.toString().split(topologyURI + ":")[1].split(":openflow-service")[0] + "\n";
            json_string[1] = "{\"isPermanent\": true,\"priority\": 100,"
                    + "\"selector\": {\"criteria\": [{\"port\": " + flowdata[0] + ",\"type\":"
                    + " \"IN_PORT\"},"
                    + "{\"mac\": \"" + flowdata[1] + "\","
                    + "\"type\": \"ETH_SRC\"},{\"mac\": \"" + flowdata[2] + "\","
                    + "\"type\": \"ETH_DST\"}]},"
                    + "\"treatment\": {"
                    + "\"instructions\": [{\"port\": " + flowdata[5] + ",\"type\": \"OUTPUT\"}]}}\n";

            requests = requests + json_string[0] + json_string[2] + json_string[1];
            /*        if(reversible.equals("1")){
            json_string[2]=flow.toString().split("table-0:flow-")[1]+"r\n";
            json_string[1]="{\"isPermanent\": true,\"priority\": 100,"
                + "\"selector\": {\"criteria\": [{\"port\": "+flowdata[5]+",\"type\":"
                + " \"IN_PORT\"},"
                + "{\"mac\": \""+flowdata[2]+"\","
                + "\"type\": \"ETH_SRC\"},{\"mac\": \""+flowdata[1]+"\","
                + "\"type\": \"ETH_DST\"}]},"
                + "\"treatment\": {"
                + "\"instructions\": [{\"port\": "+flowdata[0]+",\"type\": \"OUTPUT\"}]}}\n";
            
            requests=requests+json_string[0]+json_string[2]+json_string[1];*/
            //       } 

        }
        return requests;
    }

    private String parseFlowsReduct(OntModel modelReduct, String mappingId, String topologyURI) {
        String requests = "";

        String query = "SELECT ?flow WHERE {"
                + "?flow a mrs:Flow . "
                //+ "?flow mrs:type ?type . "
                //+ "?flow mrs:value ?value . "
                + "}";

        String mappingIdMatrix[] = mappingId.split("\n");
        int mappingIdSize = mappingIdMatrix.length;

        ResultSet r1 = executeQuery(query, emptyModel, modelReduct);

        while (r1.hasNext()) {
            QuerySolution querySolution1 = r1.next();
            Resource flowId = querySolution1.get("flow").asResource();
            String sFlowId = flowId.toString();
            for (int k = 0; k < mappingIdSize; k++) {
                if (mappingIdMatrix[k].contains(sFlowId)) {
                    sFlowId = mappingIdMatrix[k].split("-->>")[1].split("\n")[0];

                }
            }
            String device = sFlowId.split(topologyURI + ":")[1].split(":openflow-service:")[0];
            sFlowId = sFlowId.split(":flow-")[2];
            requests = requests + device + "\n" + sFlowId + "\n";
            //}
        }
        return requests;
    }

    public String getFakeFlowId() {

        return localFakeMap;
    }

}
