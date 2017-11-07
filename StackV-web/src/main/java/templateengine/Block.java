package templateengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Block {

    char flag;
    boolean isInput;
    String str;
    String body;
    String tag;
    String param = null;
    Template template;
    JSONObject input;
    JSONObject scopedInput;
    HashMap<String, String> context;
    //private final StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "Block");

    public Block(String _str, JSONObject _input, JSONObject _scopedInput, Template _template, HashMap<String, String> _context) {
        str = _str;
        context = _context;
        body = str.substring(2);

        flag = body.charAt(0);
        if (flag == '~') {
            context.put("whitespace", "trim");
            body = body.substring(1);
        } else if (flag == '?') {
            context.put("hidden", "true");
            body = body.substring(1);
        }

        flag = body.charAt(0);
        if (flag == '~') {
            context.put("whitespace", "replace");
            body = body.substring(1);
        }

        flag = body.charAt(0);
        String arr[];
        switch (flag) {
            case '*':
                tag = body.substring(1, body.indexOf("}}"));
                if (body.contains("{{/" + tag + "}}")) {
                    body = body.substring(tag.length() + 3, body.indexOf("{{/" + tag + "}}"));
                    isInput = false;
                } else {
                    arr = tag.split(" ", 2);
                    tag = arr[0];
                    param = arr[1];
                    isInput = true;
                }
                break;
            case '#':
                tag = body.substring(1, body.indexOf("}}"));
                body = body.substring(tag.length() + 3, body.indexOf("{{/" + tag + "}}"));
                isInput = false;
                break;
            case '$':
                arr = body.substring(1, body.indexOf("}}")).split(" ", 3);
                tag = arr[0] + " " + arr[1];

                if (arr.length > 2) {
                    param = arr[1] + " " + arr[2];
                } else {
                    param = arr[1];
                }

                body = body.substring(body.indexOf("}}") + 2, body.indexOf("{{/" + tag));
                body = body.trim();
                isInput = false;
                break;
            case '{':
                body = body.substring(1, body.length() - 2);
                isInput = true;
                break;
            case '/':
                body = body.substring(1, body.length() - 2);
                isInput = false;
                break;
            default:
                body = body.substring(0, body.length() - 2);
                isInput = true;
        }

        input = _input;
        scopedInput = _scopedInput;
        template = _template;
    }

    public String render() {
        if (tag != null) {
            System.out.println("--- Rendering || Flag: " + flag + " | Tag: " + tag);
        } else {
            System.out.println("--- Rendering || Flag: " + flag + " | Body: " + body);
        }
        switch (flag) {
            case '!':
                // Helper block
                String blockArr[] = body.split(" ");
                ArrayList<Object> paramArr = new ArrayList<>();
                for (int i = 1; i < blockArr.length; i++) {
                    paramArr.add(retrieveInput(blockArr[i]));
                }

                paramArr.add(input);
                paramArr.add(scopedInput);
                return finalize(template.applyHelper(blockArr[0].substring(1), paramArr));
            case '$':
                // Flow block
                boolean eval = evaluateFlow();
                if (eval) {
                    return finalize(blockRunner(scopedInput));
                } else {
                    return "";
                }
            case '#':
                // Recursive block
                if (tag.charAt(0) == '@') {
                    scopedInput = input;
                    tag = tag.substring(1);
                }
                if (scopedInput.containsKey(tag)) {
                    Object dataObj = scopedInput.get(tag);
                    if (dataObj instanceof JSONArray) {
                        // Array
                        JSONArray dataArr = (JSONArray) dataObj;
                        String retString = "";
                        int count = 1;
                        for (Object obj2 : dataArr) {
                            if (dataArr.size() > 1) {
                                context.put("progress", count + "/" + dataArr.size());
                            } else {
                                context.put("progress", "1/1");
                            }

                            JSONObject data = (JSONObject) obj2;
                            if (context.get("whitespace") != null) {
                                retString += "\n" + finalize(blockRunner(data));
                            } else {
                                retString += finalize(blockRunner(data));
                            }
                            count++;
                        }
                        return retString;
                    } else if (dataObj instanceof JSONObject) {
                        // Object
                        JSONObject data = (JSONObject) dataObj;
                        return finalize(blockRunner(data));
                    }
                } else {
                    return "";
                }
            case '*':
                // Partial
                String retString;
                if (param == null) {
                    retString = finalize(blockRunner(scopedInput));
                    template.partials.put(tag, retString);
                    if (context.containsKey("hidden")) {
                        return "";
                    } else {
                        return retString;
                    }
                } else {
                    retString = retrieveInput(param).toString();
                    template.partials.put(tag, retString);
                    return "";
                }
            case '/':
                // Comment
                return "";
            default:
                // Basic variable substitution
                return finalize(retrieveInput(body).toString());
        }
    }

    private String blockRunner(JSONObject newScope) {
        String recurBody = body;
        int start = recurBody.indexOf("{{");
        while (start > -1) {
            // Continue operating on stashes until no more exist
            int end = recurBody.indexOf("}}");
            String blockStr = recurBody.substring(start, end + 2);
            String cleanTag = blockStr.replace("~", "").replace("?", "");
            if (cleanTag.charAt(2) == '#') {
                String endTag = cleanTag.substring(0, 2) + "/" + cleanTag.substring(3);
                blockStr = recurBody.substring(start, recurBody.indexOf(endTag) + endTag.length());
            } else if (cleanTag.charAt(2) == '$') {
                String endTag = cleanTag.substring(2, cleanTag.length() - 2);
                if (hasTag(endTag)) {
                    endTag = endTag.substring(1);
                }
                String strArr[] = endTag.split(" ", 3);
                endTag = "{{/" + strArr[0] + " " + strArr[1] + "}}";

                blockStr = recurBody.substring(start, recurBody.indexOf(endTag) + endTag.length());
            } else if (blockStr.indexOf('*') > -1) {
                String endTag = cleanTag.substring(2, cleanTag.length() - 2);
                if (hasTag(endTag)) {
                    endTag = endTag.substring(1);
                }
                String strArr[] = endTag.split(" ", 3);
                endTag = "{{/" + strArr[0] + "}}";

                if (recurBody.contains(endTag)) {
                    blockStr = recurBody.substring(start, recurBody.indexOf(endTag) + endTag.length());
                } else {
                    blockStr = recurBody.substring(start, start + blockStr.length());
                }
            }

            Block block = new Block(blockStr, input, newScope, template, (HashMap<String, String>) context.clone());
            if (block.isInput()) {
                recurBody = recurBody.replaceFirst(Pattern.quote(blockStr), Matcher.quoteReplacement(block.render()));
            } else {
                recurBody = recurBody.replace(blockStr, block.render());
            }

            start = recurBody.indexOf("{{");
        }
        return finalize(recurBody);
    }

    private boolean evaluateFlow() {
        boolean eval = false;

        // Equality branch
        if (param.contains("=")) {
            String paramArr[] = param.split("=");
            ArrayList<Object> resultArr = new ArrayList<>();
            for (String paramEle : paramArr) {
                paramEle = paramEle.trim();
                switch (paramEle.charAt(0)) {
                    case '^':
                        // Context option
                        switch (param) {
                            case "^first":
                                resultArr.add(Float.parseFloat(context.get("progress")) == 0);
                                break;
                            case "^last":
                                resultArr.add(Float.parseFloat(context.get("progress")) == 1);
                                break;
                        }
                        break;
                    case '!':
                        // Helper option
                        String helper[] = param.substring(1).split(" ", 3);
                        ArrayList<Object> paramArr2 = new ArrayList<>();
                        for (int i = 1; i < helper.length; i++) {
                            paramArr2.add(retrieveInput(helper[i]));
                        }

                        paramArr2.add(input);
                        paramArr2.add(scopedInput);
                        resultArr.add(template.applyHelper(helper[0], paramArr2));
                        break;
                    case '"':
                        resultArr.add(paramEle.replace("\"", ""));
                        break;
                    default:
                        resultArr.add(scopedInput.get(paramEle));
                        break;
                }
            }

            if (resultArr.get(0) instanceof String) {
                eval = resultArr.get(0).equals(resultArr.get(1));
                switch (tag.split(" ")[0]) {
                    case "if":
                        return eval;
                    case "unless":
                        return !eval;
                }
            }
        } else {
            // Check parameter
            switch (param.charAt(0)) {
                case '^':
                    String prog[] = context.get("progress").split("/");
                    // Context option
                    switch (param) {
                        case "^first":
                            eval = prog[0].equals("1");
                            break;
                        case "^last":
                            eval = prog[0].equals(prog[1]);
                            break;
                    }
                    break;
                case '!':
                    // Helper option
                    String helper[] = param.substring(1).split(" ");
                    ArrayList<Object> paramArr = new ArrayList<>();
                    for (int i = 1; i < helper.length; i++) {
                        paramArr.add(retrieveInput(helper[i]));
                    }

                    paramArr.add(input);
                    paramArr.add(scopedInput);
                    eval = Boolean.valueOf(template.applyHelper(helper[0], paramArr));
                    break;
                default:
                    eval = inputExists(param);
                    break;
            }

            switch (tag.split(" ")[0]) {
                case "if":
                    return eval;
                case "unless":
                    return !eval;
            }
        }
        return false;
    }

    private String finalize(String str) {
        String mode = context.get("whitespace");
        if (mode == null) {
            return str;
        } else {
            switch (mode) {
                case "trim":
                    return str.trim();
                case "replace":
                    return str.replaceAll("\\s+", "");
                default:
                    return str;
            }
        }
    }

    private Object retrieveInput(String keyStr) {
        if (keyStr.equals("this")) {
            return scopedInput;
        }
        if (keyStr.charAt(0) == '\"' && keyStr.charAt(keyStr.length() - 1) == '\"') {
            return keyStr.substring(1, keyStr.length() - 1);
        }
        if (template.partials.containsKey(keyStr)) {
            return template.partials.get(keyStr);
        }

        String keyArr[] = keyStr.split("/");
        JSONObject recur;
        if (keyArr[0].charAt(0) == '@') {
            keyArr[0] = keyArr[0].substring(1);

            // Check for context commands
            switch (keyArr[0]) {
                case "index":
                    return String.valueOf(Integer.parseInt(context.get("progress").split("/")[0]) - 1);
                case "root":
                    return input;
            }

            recur = input;
        } else {
            recur = scopedInput;
        }
        
        for (int i = 0; i < keyArr.length - 1; i++) {
            String key = keyArr[i];
            String eleArr[] = key.split("\\.");
            Object obj = recur.get(eleArr[0]);
            if (obj instanceof JSONObject) {
                recur = (JSONObject) obj;
            } else {
                int index = Integer.parseInt(eleArr[1]);
                recur = (JSONObject) ((JSONArray) obj).get(index);
                break;
            }
        }
        if (recur.containsKey(keyArr[keyArr.length - 1])) {
            Object obj = recur.get(keyArr[keyArr.length - 1]);
            if (obj instanceof String) {
                return ((String) obj).replaceAll(" ", "_");
            } else {
                return recur.get(keyArr[keyArr.length - 1]);
            }
        } else {
            System.out.println("ERROR: Input not found - " + keyStr);
            // logger.error("Input not found: " + keyStr);
            return keyStr.toUpperCase();
        }
    }

    private boolean inputExists(String keyStr) {
        String keyArr[] = keyStr.split("/");
        JSONObject recur;
        if (keyArr[0].charAt(0) == '@') {
            keyArr[0] = keyArr[0].substring(1);

            recur = input;
        } else {
            recur = scopedInput;
        }
        for (int i = 0; i < keyArr.length - 1; i++) {
            String key = keyArr[i];
            String eleArr[] = key.split("\\.");
            Object obj = recur.get(eleArr[0]);
            if (obj instanceof JSONObject) {
                recur = (JSONObject) obj;
            } else {
                int index = Integer.parseInt(eleArr[1]);
                recur = (JSONObject) ((JSONArray) obj).get(index);
                break;
            }
        }
        if (recur.containsKey(keyArr[keyArr.length - 1])) {
            return true;
        } else if (template.partials.containsKey(keyArr[keyArr.length - 1])) {
            return true;
        } else {
            System.out.println("ERROR: Input not found - " + keyStr);
            // logger.error("Input not found: " + keyStr);
            return false;
        }
    }

    static boolean hasTag(String str) {
        return (str.charAt(0) == '#'
                || str.charAt(0) == '$'
                || str.charAt(0) == '!'
                || str.charAt(0) == '/'
                || str.charAt(0) == '*');

    }

    boolean isInput() {
        return isInput;
    }
}
