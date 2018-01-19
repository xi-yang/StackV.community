package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import templateengine.helpers.Helper;

public class VMType implements Helper {

    JSONParser parser = new JSONParser();

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject vm = (JSONObject) obj.get(1);

        ArrayList<String> paramArr = new ArrayList<>();
        if (vm.containsKey("instance_type")) {
            paramArr.add("instance+" + (String) vm.get("instance_type"));
        }
        else if (vm.containsKey("flavor")) {
            paramArr.add("instance+" + (String) vm.get("flavor"));
        }
                
        if (vm.containsKey("image")) {
            paramArr.add("image+" + (String) vm.get("image"));
        }
        if (vm.containsKey("security_group")) {
            paramArr.add("secgroup+" + (String) vm.get("security_group"));
        }
        if (vm.containsKey("keypair_name")) {
            paramArr.add("keypair+" + (String) vm.get("keypair_name"));
        }
        
        if (paramArr.isEmpty()) {
            return "";
        } else {
            return "mrs:type \"" + String.join(",", paramArr) + "\";";
        }
    }

    private boolean contains(JSONArray jsonArray, String key) {
        return jsonArray.toString().contains("\"" + key + "\"");
    }
}
