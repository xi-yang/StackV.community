package templateengine.helpers.vcn;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import templateengine.helpers.Helper;

public class VPCExport implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        String retString = "spa:exportTo ";
        JSONObject vpc = (JSONObject) obj.get(0);
        JSONArray subnets = (JSONArray) vpc.get("subnets");
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

        if (((String) vpc.get("parent")).contains("amazon")) {
            retString += "&lt;x-policy-annotation:data:vpc-aws-export&gt;,\n\t";
        }

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).replace(" ", "_");
            retString += "&lt;x-policy-annotation:data:vpc-subnet-" + name + "-criteria&gt;";
            if (i < names.size() - 1) {
                retString += ",\n";
            } else {
                retString += ".\n";
            }
        }

        return retString;
    }
}
