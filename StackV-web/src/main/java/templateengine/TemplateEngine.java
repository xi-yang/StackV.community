package templateengine;

import java.util.HashMap;
import org.json.simple.JSONObject;

public class TemplateEngine {

    //private final StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "TemplateEngine");
    Template template;
    String render;

    public TemplateEngine() {        
    }

    public String apply(JSONObject input) {
        String type = (String) input.get("service");
        switch (type) {
            case "dnc":
                template = new DNCTemplate();
                render = template.getTemplate();
                break;
            case "vcn":
                template = new VCNTemplate();
                render = template.getTemplate();
                break;
        }
        
        // Begin loop
        String recurBody = render;
        int start = recurBody.indexOf("{{");
        while (start > -1) {
            // Continue operating on stashes until no more exist
            int end = recurBody.indexOf("}}");
            String blockStr = recurBody.substring(start, end + 2);
            String cleanTag = blockStr.replaceAll("~", "");
            if (cleanTag.charAt(2) == '#') {
                String endTag = cleanTag.substring(0, 2) + "/" + cleanTag.substring(3);
                blockStr = recurBody.substring(start, recurBody.indexOf(endTag) + endTag.length());
            } else if (cleanTag.charAt(2) == '$') {
                String endTag = cleanTag.substring(2, cleanTag.length() - 2);
                if (Block.hasTag(endTag)) {
                    endTag = endTag.substring(1);
                }
                String strArr[] = endTag.split(" ", 3);
                endTag = "{{/" + strArr[0] + " " + strArr[1] + "}}";

                blockStr = recurBody.substring(start, recurBody.indexOf(endTag) + endTag.length());
            } else if (blockStr.indexOf('*') > -1) {
                String endTag = cleanTag.substring(2, cleanTag.length() - 2);
                if (Block.hasTag(endTag)) {
                    endTag = endTag.substring(1);
                }
                String strArr[] = endTag.split(" ", 3);
                endTag = "{{/" + strArr[0] + "}}";

                blockStr = recurBody.substring(start, recurBody.indexOf(endTag) + endTag.length());
            }

            Block block = new Block(blockStr, (JSONObject) input.get("data"), (JSONObject) input.get("data"), template, new HashMap<String, String>());
            recurBody = recurBody.replace(blockStr, block.render());

            start = recurBody.indexOf("{{");
        }               
        
        // Postprocessing
        escapeModel();
        
        return recurBody;
    }
    
    private void escapeModel() {
        int start = render.indexOf("<modelAddition>") + 15;
        int end = render.indexOf("</modelAddition>");
        
        String body = render.substring(start, end);
        body = body.replaceAll("<", "&lt;");
        body = body.replaceAll(">", "&gt;");
        
        render = render.substring(0, start) + body + render.substring(end);
    }
}
