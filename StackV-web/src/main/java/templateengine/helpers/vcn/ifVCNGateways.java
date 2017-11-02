package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class ifVCNGateways implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        if (obj.get(0) instanceof JSONArray) {
            JSONArray arr = (JSONArray) obj.get(0);
            for (Object eleObj : arr) {
                JSONObject ele = (JSONObject) eleObj;
                if (ele.get("type") != null && ele.get("type").equals("AWS Direct Connect")) {
                    return "true";
                }
            }
        }
        return "false";
    }
}
