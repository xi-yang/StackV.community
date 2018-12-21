package templateengine.helpers.dnc;

import templateengine.helpers.dnc.*;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class isIPAssigned implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject root = (JSONObject) obj.get(0);
        for (Object connObj : (JSONArray) root.get("connections")) {
            JSONObject conn = (JSONObject) connObj;
            for (Object termObj : (JSONArray) conn.get("terminals")) {
                JSONObject term = (JSONObject) termObj;
                if (term.containsKey("assign_ip") && (boolean) term.get("assign_ip")) {
                    return "true";
                }
            }
        }
        return "false";
    }
}
