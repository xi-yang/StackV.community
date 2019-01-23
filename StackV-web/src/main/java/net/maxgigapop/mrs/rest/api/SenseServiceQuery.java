package net.maxgigapop.mrs.rest.api;

import static net.maxgigapop.mrs.rest.api.WebResource.executeHttpMethod;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.spi.HttpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.maxgigapop.mrs.common.TokenHandler;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentRequestQueries;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentResponseQueries;

/**
 *
 * @author xyang
 */

public class SenseServiceQuery {
    private final String restapi = "http://127.0.0.1:8080/StackV-web/restapi";

    public static void preQueries(JSONObject jsonRequest, List<ServiceIntentRequestQueries> queries) {
        for (ServiceIntentRequestQueries queryRequest : queries) {
            String query = queryRequest.getAsk();
            ServiceIntentResponseQueries queryResponse = new ServiceIntentResponseQueries().asked(query);
            handlePreQuery(query, queryRequest.getOptions(), jsonRequest);
        }
    }

    public static void postQueries(List<ServiceIntentRequestQueries> queries,
            List<ServiceIntentResponseQueries> responseQueries, String ttlModel, HttpRequest httpRequest)
            throws IOException, ParseException {
        for (ServiceIntentRequestQueries queryRequest : queries) {
            String query = queryRequest.getAsk();
            ServiceIntentResponseQueries queryResponse = new ServiceIntentResponseQueries().asked(query);
            List<Object> results = handlePostQuery(query, queryRequest.getOptions(), ttlModel, httpRequest);
            queryResponse.setResults(results);
            responseQueries.add(queryResponse);
        }
    }

    public static void handlePreQuery(String query, List<Object> options, JSONObject jsonRequest) {
        // @TODO break up clauses into seprate methods
        if (query.equalsIgnoreCase("maximum-bandwidth")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    JSONArray jsonConns = (JSONArray) jsonRequest.get("connections");
                    for (Object objConn : jsonConns) {
                        JSONObject jsonConn = (JSONObject) objConn;
                        if (jsonConn.get("name").equals(connName)) {
                            if (jsonConn.containsKey("bandwidth")) {
                                jsonConn.remove("bandwidth");
                            }
                            JSONObject jsonBwProfile = new JSONObject();
                            jsonBwProfile.put("qos_class", "anyAvailable");
                            jsonBwProfile.put("capacity", "1");
                            jsonConn.put("bandwidth", jsonBwProfile);
                            if (jsonConn.containsKey("schedule")) {
                                JSONObject jsonSchedule = (JSONObject) jsonConn.get("schedule");
                                if (!jsonSchedule.containsKey("options")) {
                                    jsonSchedule.put("options", new JSONObject());
                                }
                                ((JSONObject) jsonSchedule.get("options")).put("use-tbmb", "true");
                            }
                        }
                    }
                }
            }
        } else if (query.equalsIgnoreCase("flexible-schedule")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    JSONArray jsonConns = (JSONArray) jsonRequest.get("connections");
                    for (Object objConn : jsonConns) {
                        JSONObject jsonConn = (JSONObject) objConn;
                        if (jsonConn.get("name").equals(connName)) {
                            JSONObject jsonSchedule;
                            if (jsonConn.containsKey("schedule")) {
                                jsonSchedule = (JSONObject) jsonConn.get("schedule");
                            } else {
                                jsonSchedule = new JSONObject();
                                jsonConn.put("schedule", jsonSchedule);
                            }
                            if (option.containsKey("start-after")) {
                                jsonSchedule.put("start", (String) option.get("start-after"));
                            }
                            if (option.containsKey("end-before")) {
                                jsonSchedule.put("end", (String) option.get("end-before"));
                            }
                            if (option.containsKey("duration")) {
                                jsonSchedule.put("duration", (String) option.get("duration"));
                            }
                        }
                    }
                }
            }
        } else if (query.equalsIgnoreCase("time-bandwidth-product")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    JSONArray jsonConns = (JSONArray) jsonRequest.get("connections");
                    for (Object objConn : jsonConns) {
                        JSONObject jsonConn = (JSONObject) objConn;
                        if (jsonConn.get("name").equals(connName)) {
                            // flexible bandwidth
                            if (jsonConn.containsKey("bandwidth")) {
                                jsonConn.remove("bandwidth");
                            }
                            JSONObject jsonBwProfile = new JSONObject();
                            jsonBwProfile.put("qos_class", "anyAvailable");
                            jsonBwProfile.put("capacity", "1");
                            jsonConn.put("bandwidth", jsonBwProfile);
                            // flexible schedule
                            JSONObject jsonSchedule;
                            if (jsonConn.containsKey("schedule")) {
                                jsonSchedule = (JSONObject) jsonConn.get("schedule");
                            } else {
                                jsonSchedule = new JSONObject();
                                jsonConn.put("schedule", jsonSchedule);
                            }
                            if (option.containsKey("start-after")) {
                                jsonSchedule.put("start", (String) option.get("start-after"));
                            }
                            if (option.containsKey("end-before")) {
                                jsonSchedule.put("end", (String) option.get("end-before"));
                            }
                            if (option.containsKey("tbp-mbytes")) {
                                if (!jsonSchedule.containsKey("options")) {
                                    jsonSchedule.put("options", new JSONObject());
                                }
                                ((JSONObject) jsonSchedule.get("options")).put("tbp-mbytes",
                                        (String) option.get("tbp-mbytes"));
                                if (option.containsKey("bandwidth-mbps >=")) {
                                    ((JSONObject) jsonSchedule.get("options")).put("bandwidth-mbps >=",
                                            (String) option.get("bandwidth-mbps >="));
                                }
                                if (option.containsKey("bandwidth-mbps <=")) {
                                    ((JSONObject) jsonSchedule.get("options")).put("bandwidth-mbps <=",
                                            (String) option.get("bandwidth-mbps <="));
                                }
                                if (option.containsKey("use-highest-bandwidth")) {
                                    ((JSONObject) jsonSchedule.get("options")).put("use-highest-bandwidth",
                                            (String) option.get("use-highest-bandwidth"));
                                }
                                if (option.containsKey("use-lowest-bandwidth")) {
                                    ((JSONObject) jsonSchedule.get("options")).put("use-lowest-bandwidth",
                                            (String) option.get("use-lowest-bandwidth"));
                                }
                            }
                        }
                    }
                }
            }
        } else if (query.equalsIgnoreCase("total-block-maximum-bandwidth")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    JSONArray jsonConns = (JSONArray) jsonRequest.get("connections");
                    for (Object objConn : jsonConns) {
                        JSONObject jsonConn = (JSONObject) objConn;
                        if (jsonConn.get("name").equals(connName)) {
                            if (jsonConn.containsKey("bandwidth")) {
                                jsonConn.remove("bandwidth");
                            }
                            JSONObject jsonBwProfile = new JSONObject();
                            jsonBwProfile.put("qos_class", "anyAvailable");
                            jsonBwProfile.put("capacity", "1");
                            jsonConn.put("bandwidth", jsonBwProfile);
                            JSONObject jsonSchedule;
                            if (jsonConn.containsKey("schedule")) {
                                jsonSchedule = (JSONObject) jsonConn.get("schedule");
                            } else {
                                jsonSchedule = new JSONObject();
                                jsonConn.put("schedule", jsonSchedule);
                            }
                            if (!jsonSchedule.containsKey("options")) {
                                jsonSchedule.put("options", new JSONObject());
                            }
                            ((JSONObject) jsonSchedule.get("options")).put("use-tbmb", "true");
                            if (option.containsKey("start-after")) {
                                jsonSchedule.put("start", (String) option.get("start-after"));
                            }
                            if (option.containsKey("end-before")) {
                                jsonSchedule.put("end", (String) option.get("end-before"));
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<Object> handlePostQuery(String query, List<Object> options, String ttlModel,
            HttpRequest httpRequest) throws IOException, ParseException {
        List<Object> results = new ArrayList();
        // @TODO break up clauses into seprate methods
        if (query.equalsIgnoreCase("maximum-bandwidth")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    String tagPattern = "^l2path\\\\\\+.+:" + connName.replace(" ", "_") + "$";
                    final String jsonTemplate = "{\n" + "    \"connections\": [\n" + "        {\n"
                            + "           \"required\": \"false\",\n" + "           \"bandwidth\": \"?bandwidth?\",\n"
                            + String.format(
                                    "           \"sparql\": \"SELECT DISTINCT ?bandwidth WHERE {?bp nml:hasService ?bwProfile.  ?bp mrs:tag ?tag. ?bwProfile mrs:reservableCapacity ?bandwidth. FILTER regex(?tag, '%s', 'i') }\"\n",
                                    tagPattern)
                            + "	       }\n" + "    ]\n" + "}";

                    String responseStr;
                    String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
                    final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
                    final TokenHandler token = new TokenHandler(refresh);
                    URL url = new URL("http://127.0.0.1:8080/StackV-web/restapi/service/manifest");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                            + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n<jsonModel>\n%s</jsonModel>\n</serviceManifest>",
                            jsonTemplate, ttlModel);
                    responseStr = executeHttpMethod(url, conn, "POST", data, token.auth());
                    JSONParser parser = new JSONParser();
                    JSONObject jo;
                    jo = (JSONObject) parser.parse(responseStr);
                    jo = (JSONObject) parser.parse((String) jo.get("jsonTemplate"));
                    Map result = new LinkedHashMap();
                    result.put("name", connName);
                    results.add(result);
                    if (jo != null && !jo.isEmpty()) {
                        JSONArray jsonConns = (JSONArray) jo.get("connections");
                        if (!jsonConns.isEmpty()) {
                            JSONObject jsonConn = (JSONObject) jsonConns.get(0);
                            if (jsonConn.containsKey("bandwidth")) {
                                String bandwidth = (String) jsonConn.get("bandwidth");
                                if (bandwidth.contains("^^")) {
                                    bandwidth = bandwidth.split("^")[0];
                                }
                                result.put("bandwidth", bandwidth);
                            }
                        }
                    }
                }
            }
        } else if (query.equalsIgnoreCase("flexible-schedule")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    String tagPattern = "^l2path\\\\\\+.+:" + connName.replace(" ", "_") + "$";
                    final String jsonTemplate = "{\n" + "    \"connections\": [\n" + "        {\n"
                            + "           \"required\": \"false\",\n" + "           \"start-time\": \"?start?\",\n"
                            + "           \"end-time\": \"?end?\",\n"
                            + String.format(
                                    "           \"sparql\": \"SELECT DISTINCT ?start ?end WHERE {?bp mrs:tag ?tag. ?bp nml:existsDuring ?lifetime. ?lifetime nml:start ?start. ?lifetime nml:end ?end. FILTER regex(?tag, '%s', 'i') }\"\n",
                                    tagPattern)
                            + "	       }\n" + "    ]\n" + "}";

                    String responseStr;
                    String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
                    final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
                    final TokenHandler token = new TokenHandler(refresh);
                    URL url = new URL("http://127.0.0.1:8080/StackV-web/restapi/service/manifest");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                            + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n<jsonModel>\n%s</jsonModel>\n</serviceManifest>",
                            jsonTemplate, ttlModel);
                    responseStr = executeHttpMethod(url, conn, "POST", data, token.auth());
                    JSONParser parser = new JSONParser();
                    JSONObject jo;
                    jo = (JSONObject) parser.parse(responseStr);
                    jo = (JSONObject) parser.parse((String) jo.get("jsonTemplate"));
                    Map result = new LinkedHashMap();
                    result.put("name", connName);
                    results.add(result);
                    if (jo != null && !jo.isEmpty()) {
                        JSONArray jsonConns = (JSONArray) jo.get("connections");
                        if (!jsonConns.isEmpty()) {
                            JSONObject jsonConn = (JSONObject) jsonConns.get(0);
                            if (jsonConn.containsKey("start-time")) {
                                String timedate = (String) jsonConn.get("start-time");
                                if (timedate.contains("^^")) {
                                    timedate = timedate.split("^")[0];
                                }
                                result.put("start-time", timedate);
                            }
                            if (jsonConn.containsKey("end-time")) {
                                String timedate = (String) jsonConn.get("end-time");
                                if (timedate.contains("^^")) {
                                    timedate = timedate.split("^")[0];
                                }
                                result.put("end-time", timedate);
                            }
                        }
                    }
                }
            }
        } else if (query.equalsIgnoreCase("time-bandwidth-product")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    String tagPattern = "^l2path\\\\\\+.+:" + connName.replace(" ", "_") + "$";
                    final String jsonTemplate = "{\n" + "    \"connections\": [\n" + "        {\n"
                            + "           \"required\": \"false\",\n" + "           \"bandwidth\": \"?bandwidth?\",\n"
                            + "           \"start-time\": \"?start?\",\n" + "           \"end-time\": \"?end?\",\n"
                            + String.format(
                                    "           \"sparql\": \"SELECT DISTINCT ?bandwidth ?start ?end WHERE {?bp nml:hasService ?bwProfile. ?bp mrs:tag ?tag. ?bwProfile mrs:reservableCapacity ?bandwidth. ?bwProfile nml:existsDuring ?lifetime. ?lifetime nml:start ?start. ?lifetime nml:end ?end. FILTER regex(?tag, '%s', 'i') }\"\n",
                                    tagPattern)
                            + "	       }\n" + "    ]\n" + "}";

                    String responseStr;
                    String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
                    final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
                    final TokenHandler token = new TokenHandler(refresh);
                    URL url = new URL("http://127.0.0.1:8080/StackV-web/restapi/service/manifest");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                            + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n<jsonModel>\n%s</jsonModel>\n</serviceManifest>",
                            jsonTemplate, ttlModel);
                    responseStr = executeHttpMethod(url, conn, "POST", data, token.auth());
                    JSONParser parser = new JSONParser();
                    JSONObject jo;
                    jo = (JSONObject) parser.parse(responseStr);
                    jo = (JSONObject) parser.parse((String) jo.get("jsonTemplate"));
                    Map result = new LinkedHashMap();
                    result.put("name", connName);
                    results.add(result);
                    if (jo != null && !jo.isEmpty()) {
                        JSONArray jsonConns = (JSONArray) jo.get("connections");
                        if (!jsonConns.isEmpty()) {
                            JSONObject jsonConn = (JSONObject) jsonConns.get(0);
                            if (jsonConn.containsKey("bandwidth")) {
                                String bandwidth = (String) jsonConn.get("bandwidth");
                                if (bandwidth.contains("^^")) {
                                    bandwidth = bandwidth.split("^")[0];
                                }
                                result.put("bandwidth", bandwidth);
                            }
                            if (jsonConn.containsKey("start-time")) {
                                String timedate = (String) jsonConn.get("start-time");
                                if (timedate.contains("^^")) {
                                    timedate = timedate.split("^")[0];
                                }
                                result.put("start-time", timedate);
                            }
                            if (jsonConn.containsKey("end-time")) {
                                String timedate = (String) jsonConn.get("end-time");
                                if (timedate.contains("^^")) {
                                    timedate = timedate.split("^")[0];
                                }
                                result.put("end-time", timedate);
                            }
                        }
                    }
                }
            }
        } else if (query.equalsIgnoreCase("total-block-maximum-bandwidth")) {
            for (Object obj : options) {
                Map option = (Map) obj;
                if (option.containsKey("name")) {
                    String connName = (String) option.get("name");
                    String tagPattern = "^l2path\\\\\\+.+:" + connName.replace(" ", "_") + "$";
                    final String jsonTemplate = "{\n" + "    \"connections\": [\n" + "        {\n"
                            + "           \"required\": \"false\",\n" + "           \"bandwidth\": \"?bandwidth?\",\n"
                            + "           \"start-time\": \"?start?\",\n" + "           \"end-time\": \"?end?\",\n"
                            + String.format(
                                    "           \"sparql\": \"SELECT DISTINCT ?bandwidth ?start ?end WHERE {?bp nml:hasService ?bwProfile. ?bp mrs:tag ?tag. ?bwProfile mrs:reservableCapacity ?bandwidth. ?bwProfile nml:existsDuring ?lifetime. ?lifetime nml:start ?start. ?lifetime nml:end ?end. FILTER regex(?tag, '%s', 'i') }\"\n",
                                    tagPattern)
                            + "	       }\n" + "    ]\n" + "}";

                    String responseStr;
                    String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
                    final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
                    final TokenHandler token = new TokenHandler(refresh);
                    URL url = new URL("http://127.0.0.1:8080/StackV-web/restapi/service/manifest");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                            + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n<jsonModel>\n%s</jsonModel>\n</serviceManifest>",
                            jsonTemplate, ttlModel);
                    responseStr = executeHttpMethod(url, conn, "POST", data, token.auth());
                    JSONParser parser = new JSONParser();
                    JSONObject jo;
                    jo = (JSONObject) parser.parse(responseStr);
                    jo = (JSONObject) parser.parse((String) jo.get("jsonTemplate"));
                    Map result = new LinkedHashMap();
                    result.put("name", connName);
                    results.add(result);
                    if (jo != null && !jo.isEmpty()) {
                        JSONArray jsonConns = (JSONArray) jo.get("connections");
                        if (!jsonConns.isEmpty()) {
                            JSONObject jsonConn = (JSONObject) jsonConns.get(0);
                            if (jsonConn.containsKey("bandwidth")) {
                                String bandwidth = (String) jsonConn.get("bandwidth");
                                if (bandwidth.contains("^^")) {
                                    bandwidth = bandwidth.split("^")[0];
                                }
                                result.put("bandwidth", bandwidth);
                            }
                            if (jsonConn.containsKey("start-time")) {
                                String timedate = (String) jsonConn.get("start-time");
                                if (timedate.contains("^^")) {
                                    timedate = timedate.split("^")[0];
                                }
                                result.put("start-time", timedate);
                            }
                            if (jsonConn.containsKey("end-time")) {
                                String timedate = (String) jsonConn.get("end-time");
                                if (timedate.contains("^^")) {
                                    timedate = timedate.split("^")[0];
                                }
                                result.put("end-time", timedate);
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

}
