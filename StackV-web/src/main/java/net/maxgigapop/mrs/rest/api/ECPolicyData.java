package templateengine.helpers.dnc;

import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import templateengine.helpers.Helper;

public class ECPolicyData implements Helper {

    JSONParser parser = new JSONParser();

    @Override
    public String apply(ArrayList<Object> obj) {
        JSONObject root = (JSONObject) obj.get(0);
        JSONObject retJSON = new JSONObject();
        int index = 0;
        for (Object connObj : (JSONArray) root.get("connections")) {
            JSONObject conn = (JSONObject) connObj;
            String connName = "urn:ogf:network:vo1_maxgigapop_net:link=conn" + index++;
            JSONObject retConn = new JSONObject();

            JSONObject retTerms = new JSONObject();

            JSONObject retTerm = new JSONObject();
            retTerm.put("vlan_tag", conn.get("l2_customer_vlan"));

            if (conn.get("bandwidth_limit") != null) {
                retTerm.put("bandwidth", conn.get("bandwidth_limit"));
            }

            retTerms.put(conn.get("l2_customer_uri"), retTerm);

            JSONObject cloud = (JSONObject) root.get("cloud_details");
            switch ((String) cloud.get("provider")) {
                case "Amazon":
                    JSONObject retAma = new JSONObject();
                    retAma.put("vlan_tag", "any");
                    retTerms.put(cloud.get("uri"), retAma);
                    break;
            }

            retConn.put("terminals", retTerms);
            retJSON.put(connName, retConn);
        }

        return retJSON.toJSONString();
    }
}
