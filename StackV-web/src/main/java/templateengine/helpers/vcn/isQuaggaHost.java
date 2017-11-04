package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class isQuaggaHost implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject root = (JSONObject) obj.get(0);
        JSONObject context = (JSONObject) obj.get(1);

        String str1 = (String) context.get("name");
        String str2 = (String) ((JSONObject) ((JSONObject) root.get("intercloud")).get("bgp")).get("vm_host");

        str1 = str1.replace(" ", "_");
        str2 = str2.replace(" ", "_");
        
        return str1.equals(str2) ? "true" : "false";
    }
}
