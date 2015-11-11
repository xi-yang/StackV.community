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
import java.util.logging.Logger;
import javax.ejb.EJBException;
import net.maxgigapop.mrs.common.ModelUtil;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
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

    //private AmazonEC2Client ec2 = null;
    //private AmazonDirectConnectClient dc = null;
    private OnosServer ec2Client = null;
    private String topologyUri = null;
    //private Regions region = null;
    static final Logger logger = Logger.getLogger(OnosPush.class.getName());
    static final OntModel emptyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

    public OnosPush(String access_key_id, String secret_access_key, String subsystemBaseUrl, String topologyURI) {

    }

    public String pushPropagate(String access_key_id, String secret_access_key, String model, String modelAddTtl, String topologyURI, String subsystemBaseUrl) throws EJBException, Exception {
        String requests = "";

        OntModel modelRef = ModelUtil.unmarshalOntModel(model);
        OntModel modelAdd = ModelUtil.unmarshalOntModel(modelAddTtl);
        //OntModel modelReduct = ModelUtil.unmarshalOntModel(modelReductTtl);

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

                }
                if (flow2.toString().equals(flow.toString() + ":rule-match-1")) {
                    flowdata[1] = rulematch.toString();

                }
                if (flow2.toString().equals(flow.toString() + ":rule-match-2")) {
                    flowdata[2] = rulematch.toString();

                }
                if (flow2.toString().equals(flow.toString() + ":rule-match-3")) {
                    flowdata[3] = rulematch.toString();

                }
                if (flow2.toString().equals(flow.toString() + ":rule-match-4")) {
                    flowdata[4] = rulematch.toString();

                }
                if (flow2.toString().equals(flow.toString() + ":rule-action-0")) {
                    flowdata[5] = rulematch.toString();

                }
            }

            String[] json_string = new String[2];
            json_string[0] = flow.toString().split(topologyURI + ":")[1].split(":openflow-service")[0] + "\n";
            json_string[1] = "{\"isPermanent\": true,\"priority\": 100,"
                    + "\"selector\": {\"criteria\": [{\"port\": " + flowdata[0] + ",\"type\":"
                    + " \"IN_PORT\"}]},"
                    //+ "{\"mac\": \""+flowdata[1]+"\","
                    //+ "\"type\": \"ETH_SRC\"},{\"mac\": \""+flowdata[2]+"\","
                    //+ "\"type\": \"ETH_DST\"}]},"
                    + "\"treatment\": {"
                    + "\"instructions\": [{\"port\": " + flowdata[5] + ",\"type\": \"OUTPUT\"}]}}\n";

            requests = requests + json_string[0] + json_string[1];

        }

        return requests;

    }

    public void pushCommit(String access_key_id, String secret_access_key, String model, String topologyURI, String subsystemBaseUrl) throws EJBException, Exception {

        String[] json_string = model.split("\n");
        for (int i = 0; i < json_string.length; i++) {
            System.out.println("Device: " + json_string[i] + "\nFlow: " + json_string[i + 1] + "\n");
            URL url = new URL(String.format(subsystemBaseUrl + "/flows/" + json_string[i]));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int status = this.executeHttpMethod(access_key_id, secret_access_key, url, conn, "POST", json_string[i + 1]);
            if (status != 201) {
                throw new EJBException(String.format("Failed to push %s into %s", json_string[i + 1], json_string[i]));
            }
            i++;
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

    private int executeHttpMethod(String access_key_id, String secret_access_key, URL url, HttpURLConnection conn, String method, String body) throws IOException {
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
        logger.log(Level.INFO, "Sending {0} request to URL : {1}", new Object[]{method, url});
        int responseCode = conn.getResponseCode();
        logger.log(Level.INFO, "Response Code : {0}", responseCode);

        /*StringBuilder responseStr;
         try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
         String inputLine;
         responseStr = new StringBuilder();
         while ((inputLine = in.readLine()) != null) {
         responseStr.append(inputLine);
         }
         }
         //return responseStr.toString();*/
        return responseCode;
    }

}
