package templateengine;

import java.util.ArrayList;
import java.util.HashMap;
import templateengine.helpers.Helper;
import templateengine.helpers.add;
import templateengine.helpers.equals;

public class Template {

    String template;
    HashMap<String, Helper> helpers = new HashMap<>();
    HashMap<String, String> partials = new HashMap<>();

    public Template() {
        // Load helpers
        helpers.put("equals", new equals());
        helpers.put("add", new add());
    }

    public String getTemplate() {
        return template;
    }

    // Helpers
    public String applyHelper(String helper, ArrayList<Object> obj) {
        return helpers.get(helper).apply(obj);
    }
}
