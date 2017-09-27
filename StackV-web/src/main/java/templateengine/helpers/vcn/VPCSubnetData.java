package templateengine.helpers.vcn;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import templateengine.helpers.Helper;

public class VPCSubnetData implements Helper {

    JSONParser parser = new JSONParser();

    @Override
    public String apply(ArrayList<Object> obj) {
        String retString = "";
        JSONArray subnets = (JSONArray) obj.get(0);
        ArrayList<String> names = new ArrayList<>();
        for (Object subnetObj : subnets) {
            JSONObject subnet = (JSONObject) subnetObj;
            if (subnet.containsKey("vms")) {
                JSONArray vms = (JSONArray) subnet.get("vms");
                for (Object vmObj : vms) {
                    JSONObject vm = (JSONObject) vmObj;
                    names.add((String) vm.get("name"));
                }
            }
        }

        for (int i = 0;
                i < names.size();
                i++) {
            String name = names.get(i);
            retString += "<x-policy-annotation:data:vpc-subnet-" + name + "-criteria>";
            if (i < names.size() - 1) {
                retString += ",\n";
            } else {
                retString += ".\n";
            }
        }

        return retString;
    }
}
