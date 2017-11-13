package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class DCSRIOVs implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        String retString = "";
        JSONObject root = (JSONObject) obj.get(1);
        JSONArray subnets = (JSONArray) ((JSONObject) root.get("openstack")).get("subnets");
        for (Object subnetObj : subnets) {
            JSONObject subnet = (JSONObject) subnetObj;
            JSONArray vms = (JSONArray) subnet.get("vms");
            for (Object vmObj : vms) {
                JSONObject vm = (JSONObject) vmObj;
                JSONArray sriovs = (JSONArray) vm.get("sriovs");
                for (Object sriovObj : sriovs) {
                    JSONObject sriov = (JSONObject) sriovObj;
                    JSONObject gateway = getGateway(root, (String) sriov.get("hosting_gateway"));

                    if (gateway.get("type").equals("Intercloud Network")) {
                        String mode = (String) obj.get(0);
                        if (mode.equalsIgnoreCase("action")) {
                            retString += ", &lt;x-policy-annotation:action:sriov-criteria-external-"; 
                        } else {
                            retString += ", &lt;x-policy-annotation:data:sriov-criteria-external-";
                        }
                        
                        retString += ((String) gateway.get("name")).replace(" ", "_") 
                                + "-" + ((String) sriov.get("name")).replace(" ", "_")  +"&gt;\n\t";
                    }
                }
            }
        }

        return retString + " .";
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
