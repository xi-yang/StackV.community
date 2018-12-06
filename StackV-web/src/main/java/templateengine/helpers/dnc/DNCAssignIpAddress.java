package templateengine.helpers.dnc;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import templateengine.helpers.Helper;

public class DNCAssignIpAddress implements Helper {

    JSONParser parser = new JSONParser();

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject root = (JSONObject) obj.get(0);
        JSONObject retJSON = new JSONObject();
        retJSON.put("address_type", "ipv4-address");
        JSONArray addressPoolJSON = new JSONArray();
        if (root.containsKey("suggest_ip_ranges")) {
            retJSON.put("address_pool", addressPoolJSON);
            for (Object ipRangeObj : (JSONArray) root.get("suggest_ip_ranges")) {
                JSONObject ipRangeJSON = (JSONObject) ipRangeObj;
                String ipRange = ipRangeJSON.get("start") + "-" + ipRangeJSON.get("end");
                addressPoolJSON.add(ipRange);
            }
        }
        JSONArray assignToJSONArray = new JSONArray();
        retJSON.put("assign_to", assignToJSONArray);
        for (Object connObj : (JSONArray) root.get("connections")) {
            JSONObject conn = (JSONObject) connObj;
            String connName = ((String)conn.get("name")).replace(" ", "_");
            
            JSONObject assignToJSON = new JSONObject();
            assignToJSONArray.add(assignToJSON);
            assignToJSON.put("l2path", "%[$..[?(@.name==\""+connName+"\")].hops.*]%");
            JSONArray assignToTerminalArray = new JSONArray();
            assignToJSON.put("terminals", assignToTerminalArray);
            for (Object termObj : (JSONArray) conn.get("terminals")) {
                JSONObject term = (JSONObject) termObj;
                if (term.containsKey("assign_ip") && (boolean) term.get("assign_ip")) {
                    assignToTerminalArray.add(term.get("uri"));
                }
            }
        }
        return retJSON.toJSONString().replace("\"%[", "%").replace("]%\"", "%");
    }
}
