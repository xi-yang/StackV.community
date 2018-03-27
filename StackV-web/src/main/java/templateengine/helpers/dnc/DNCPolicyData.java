package templateengine.helpers.dnc;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import templateengine.helpers.Helper;

public class DNCPolicyData implements Helper {

    JSONParser parser = new JSONParser();

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject root = (JSONObject) obj.get(0);
        JSONObject retJSON = new JSONObject();
        for (Object connObj : (JSONArray) root.get("connections")) {
            JSONObject conn = (JSONObject) connObj;
            String connName = ((String)conn.get("name")).replace(" ", "_");
            JSONObject retConn = new JSONObject();
            
            JSONObject retTerm = new JSONObject();
            for (Object termObj : (JSONArray) conn.get("terminals")) {
                JSONObject term = (JSONObject) termObj;
                JSONObject retVlan = new JSONObject();
                retVlan.put("vlan_tag", term.get("vlan_tag"));
                retTerm.put(term.get("uri"), retVlan);
            }
            retConn.put("terminals", retTerm);
            
            JSONObject retBand = new JSONObject();
            JSONObject band = (JSONObject) conn.get("bandwidth");
            retBand.put("maximum", band.get("capacity"));
            retBand.put("reservable", band.get("capacity"));
            retBand.put("available", band.get("capacity"));
            retBand.put("unit", "mbps");
            retBand.put("qos_class", band.get("qos_class"));
            retConn.put("bandwidth", retBand);
             
            JSONObject schedule = (JSONObject) conn.get("schedule");
            if (conn.containsKey("schedule")) {
                JSONObject retSchedule = new JSONObject();
                retSchedule.put("start", schedule.containsKey("start") && !((String)schedule.get("start")).isEmpty() ? schedule.get("start") : "now");
                retSchedule.put("end", schedule.get("end"));
                if (schedule.containsKey("duration")) {
                    retSchedule.put("duration", schedule.get("duration"));
                }
                retConn.put("schedule", retSchedule);
            }

            retJSON.put(connName, retConn);
        }
        
        return retJSON.toJSONString();
    }
}
