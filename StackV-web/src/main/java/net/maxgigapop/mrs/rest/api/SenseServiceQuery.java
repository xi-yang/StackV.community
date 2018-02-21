package net.maxgigapop.mrs.rest.api;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.maxgigapop.mrs.common.TokenHandler;
import static net.maxgigapop.mrs.rest.api.WebResource.executeHttpMethod;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentRequestQueries;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentResponseQueries;
import org.jboss.resteasy.spi.HttpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */


public class SenseServiceQuery {
    private final String restapi = "http://127.0.0.1:8080/StackV-web/restapi";

    public static void preQueries(JSONObject jsonRequest, List<ServiceIntentRequestQueries> queries) {
        for (ServiceIntentRequestQueries queryRequest: queries) {
            String query = queryRequest.getAsk();
            ServiceIntentResponseQueries queryResponse = new ServiceIntentResponseQueries()
                    .asked(query);
            handlePreQuery(query, queryRequest.getOptions(), jsonRequest);
        }
    }
    
    public static void postQueries(List<ServiceIntentRequestQueries> queries, List<ServiceIntentResponseQueries> responseQueries, String ttlModel, HttpRequest httpRequest) throws IOException {
        for (ServiceIntentRequestQueries queryRequest: queries) {
            String query = queryRequest.getAsk();
            ServiceIntentResponseQueries queryResponse = new ServiceIntentResponseQueries()
                    .asked(query);
            List<Object> results = handlePostQuery(query, queryRequest.getOptions(), ttlModel, httpRequest);
            queryResponse.setResults(results);
            responseQueries.add(queryResponse);
        }
    }

    public static void handlePreQuery(String query, List<Object> options, JSONObject jsonRequest) {
        //@TODO break up clauses into seprate methods
        if (query.equalsIgnoreCase("maximum-bandwidth")) {
            for (Object obj: options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    JSONArray jsonConns = (JSONArray)jsonRequest.get("connections");
                    for (Object objConn: jsonConns) {
                        JSONObject jsonConn = (JSONObject) objConn;
                        if (jsonConn.get("name").equals(connName)) {
                            if (jsonConn.containsKey("bandwidth")) {
                                jsonConn.remove("bandwidth");
                            }
                            JSONObject jsonBwProfile = new JSONObject();
                            jsonBwProfile.put("qos_class", "anyAvailable");
                            jsonConn.put("bandwidth", jsonBwProfile);
                        }
                    }
                }
            }
        }
    }

    public static List<Object> handlePostQuery(String query, List<Object> options, String ttlModel, HttpRequest httpRequest) throws IOException {
        List<Object> results = new ArrayList();
        //@TODO break up clauses into seprate methods
        if (query.equalsIgnoreCase("maximum-bandwidth")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    String tagPattern = "^l2path\\\\\\+.+:" + connName.replace(" ", "_") + "$";
                    final String jsonTemplate = "{\n"
                            + "    \"connections\": [\n"
                            + "        {\n"
                            + "           \"required\": \"false\",\n"
                            + "           \"maximum-bandwidth\": \"?bandwidth?\",\n"
                            + String.format("           \"sparql\": \"SELECT DISTINCT ?bandwidth WHERE {?bp nml:hasService ?bwProfile.  ?bp mrs:tag ?tag. ?bwProfile mrs:reservableCapacity ?bandwidth. FILTER regex(?tag, '%s', 'i') }\"\n", tagPattern)
                            + "	       }\n"
                            + "    ]\n"
                            + "}";

                    String responseStr;
                    String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
                    final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
                    final TokenHandler token = new TokenHandler(auth, refresh);
                    URL url = new URL("http://127.0.0.1:8080/StackV-web/restapi/service/manifest");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                            + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n<jsonModel>\n%s</jsonModel>\n</serviceManifest>",
                            jsonTemplate, ttlModel);
                    responseStr = executeHttpMethod(url, conn, "POST", data, token.auth());
                    JSONParser parser = new JSONParser();
                    JSONObject jo;
                    try {
                        jo = (JSONObject)parser.parse(responseStr);
                        jo = (JSONObject)parser.parse((String)jo.get("jsonTemplate"));
                        //$$ further parse the template result 
                            //$$ empty jsonTemplate '{}' means no answer for the question
                    } catch (ParseException ex) {
                        Logger.getLogger(SenseServiceQuery.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        return results;
    }

}
