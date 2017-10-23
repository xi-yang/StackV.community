package templateengine.helpers.vcn;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import templateengine.helpers.Helper;

public class DCSRIOVName implements Helper {

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject root = (JSONObject) obj.get(0);
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
                        return ((String) sriov.get("name")).replace(" ", "_");
                    }
                }
            }
        }
        return null;
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
