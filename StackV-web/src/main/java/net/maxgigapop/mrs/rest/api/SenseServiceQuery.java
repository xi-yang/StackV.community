package net.maxgigapop.mrs.rest.api;

import java.util.List;
import java.util.ArrayList;
import javax.ejb.EJB;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentRequestQueries;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceIntentResponseQueries;
import net.maxgigapop.mrs.service.HandleServiceCall;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author xyang
 */


public class SenseServiceQuery {
    
    public static void preQueries(JSONObject jsonRequest, List<ServiceIntentRequestQueries> queries) {
        // verify / validate / modify requests based on query details
        //@TODO
    }
    
    public static void postQueries(List<ServiceIntentRequestQueries> queries, List<ServiceIntentResponseQueries> responseQueries) {
        for (ServiceIntentRequestQueries queryRequest: queries) {
            String query = queryRequest.getAsk();
            ServiceIntentResponseQueries queryResponse = new ServiceIntentResponseQueries()
                    .asked(query);
            List<Object> results = handleQuery(query, queryRequest.getOptions());
            queryResponse.setResults(results);
            responseQueries.add(queryResponse);
        }
    }

    public static List<Object> handleQuery(String query, List<Object> options) {
        List<Object> results = new ArrayList();
        
        //@TODO
        
        return results;
    }

}
