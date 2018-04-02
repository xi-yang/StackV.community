package templateengine;

import java.util.HashMap;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
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
                break;
            case "vcn":
                template = new VCNTemplate();
                break;
            case "ahc":
                template = new AHCTemplate();
                break;    
            case "ecc":
                template = new ECTemplate();
                break;
        }
        render = template.getTemplate();

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
            if (block.isInput()) {
                recurBody = recurBody.replaceFirst(Pattern.quote(blockStr), block.render());
            }
            else {
                recurBody = recurBody.replace(blockStr, block.render());
            }

            start = recurBody.indexOf("{{");
        }

        // Postprocessing
        prepareModel();
        
        recurBody = recurBody.replace("&#123;", "{");
        recurBody = recurBody.replace("&#125;", "}");
        
        return recurBody;
    }

    private void prepareModel() {
        int start = render.indexOf("<modelAddition>") + 15;
        int end = render.indexOf("</modelAddition>");

        String body = render.substring(start, end);
        body = body.replace("<", "&lt;");
        body = body.replace(">", "&gt;");        

        render = render.substring(0, start) + body + render.substring(end);
    }
}
