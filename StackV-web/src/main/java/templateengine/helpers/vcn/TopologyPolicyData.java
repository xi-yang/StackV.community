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

public class TopologyPolicyData implements Helper {

    JSONParser parser = new JSONParser();

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject root = (JSONObject) obj.get(0);
        final JSONArray subnets = new JSONArray();
        JSONArray routes = new JSONArray();

        try {
            routes.add(parser.parse("{\"to\": \"0.0.0.0/0\",\"nextHop\": \"internet\"}"));
        } catch (ParseException ex) {
            Logger.getLogger(TopologyPolicyData.class.getName()).log(Level.SEVERE, null, ex);
        }

        ((JSONArray) root.get("subnets")).forEach(new Consumer<Object>() {
            @Override
            public void accept(Object subnetObj) {
                try {
                    JSONObject subnet = (JSONObject) subnetObj;
                    JSONObject net = new JSONObject();
                    net.put("name", subnet.get("name"));
                    if (subnet.containsKey("cidr")) {
                        net.put("cidr", subnet.get("cidr"));
                    }
                    if (subnet.containsKey("routes")) {
                        net.put("routes", nextHop((JSONArray) subnet.get("routes")));
                    }

                    if (subnet.containsKey("vpn_route_propagation")) {
                        if (!net.containsKey("routes")) {
                            net.put("routes", new JSONArray());
                        }
                        ((JSONArray) net.get("routes")).add(parser.parse("{\"from\":\"vpn\",\"to\":\"0.0.0.0/0\",\"nextHop\":\"vpn\"}"));
                        ((JSONArray) net.get("routes")).add(parser.parse("{\"to\":\"0.0.0.0/0\",\"nextHop\":\"internet\"}"));
                    }
                    if (subnet.containsKey("internet_routable") && subnet.get("internet_routable").equals(true)) {
                        if (!net.containsKey("routes")) {
                            net.put("routes", new JSONArray());
                        }
                        ((JSONArray) net.get("routes")).add(parser.parse("{\"to\":\"0.0.0.0/0\",\"nextHop\":\"internet\"}"));
                    }

                    subnets.add(net);
                } catch (ParseException ex) {
                    Logger.getLogger(TopologyPolicyData.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            private JSONArray nextHop(JSONArray routes) {
                routes.forEach(new Consumer<Object>() {
                    @Override
                    public void accept(Object obj) {
                        JSONObject route = (JSONObject) obj;
                        if (route.containsKey("next_hop")) {
                            route.put("nextHop", route.get("next_hop"));
                            route.remove("next_hop");
                        }
                    }
                });
                return routes;
            }
        });

        try {
            JSONObject network = (JSONObject) parser.parse("{\"type\":\"internal\"}");
            network.put("cidr", root.get("cidr"));
            network.put("parent", root.get("parent"));
            network.put("subnets", subnets);
            network.put("routes", routes);

            if ((root.containsKey("options") && contains((JSONArray) root.get("options"), "aws-form"))
                    || ((String) root.get("parent")).contains("amazon")) {
                network.put("gateways", parser.parse("[{\"type\":\"internet\"},{\"type\":\"vpn\"}]"));
            }

            return network.toString();
        } catch (ParseException ex) {
            Logger.getLogger(TopologyPolicyData.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private boolean contains(JSONArray jsonArray, String key) {
        return jsonArray.toString().contains("\"" + key + "\"");
    }
}
