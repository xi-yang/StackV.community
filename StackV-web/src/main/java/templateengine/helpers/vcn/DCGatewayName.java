package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class DCGatewayName implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject root = (JSONObject) obj.get(0);
        JSONArray gateways = (JSONArray) ((JSONObject) root.get("openstack")).get("gateways");
        for (Object gatewayObj : gateways) {
            JSONObject gateway = (JSONObject) gatewayObj;
            if (gateway.get("type").equals("Intercloud Network")) {
                return (String) gateway.get("name");
            }
        }
        return null;
    }
}
