package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class isDCHost implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject vm = (JSONObject) obj.get(1);
        JSONArray sriovs = (JSONArray) vm.get("sriovs");        
        for (Object sriovObj : sriovs) {
            JSONObject sriov = (JSONObject) sriovObj;
            JSONObject gateway = getGateway((JSONObject) obj.get(0), (String) sriov.get("hosting_gateway"));
            
            if (gateway.get("type").equals("Intercloud Network")) {
                return "true";
            }
        }                       
        return "false";
    }
    
    private JSONObject getGateway(JSONObject root, String name) {
        JSONArray gateways = (JSONArray) ((JSONObject) root.get("openstack")).get("gateways");
        for (Object gatewayObj : gateways) {
            JSONObject gateway = (JSONObject) gatewayObj;
            if (gateway.get("name").equals(name)) {
                return gateway;
            }
        }
        return null;
    }
}
