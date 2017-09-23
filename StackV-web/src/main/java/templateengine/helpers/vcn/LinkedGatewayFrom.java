package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class LinkedGatewayFrom implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONArray arr = (JSONArray) obj.get(0);
        JSONObject scopedInput = (JSONObject) obj.get(2);
        String host = (String) scopedInput.get("hosting_gateway");
        for (Object eleObj : arr) {                        
            JSONObject ele = (JSONObject) eleObj;
            if (((String) ele.get("name")).equalsIgnoreCase(host)) {                
                return (String) ((JSONObject) ((JSONArray) ele.get("connects")).get(0)).get("from");
            }
        }
        return "false";
    }
}
