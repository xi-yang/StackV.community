package templateengine.helpers.vcn;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import templateengine.helpers.Helper;

public class ExportData implements Helper {

    JSONParser parser = new JSONParser();

    @Override
    public String apply(ArrayList<Object> obj) {
        String retString = "";
        JSONArray exports = (JSONArray) ((JSONObject) obj.get(1)).get("exports");
        for (Object exportObj : exports) {
            JSONObject export = (JSONObject) exportObj;
            String exportStr = (String) export.get("value");
            retString += ",'" + exportStr + "'";
        }

        return "[" + retString.substring(1) + "]";
    }
}
