package templateengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TemplateEngineDriver {

    public static void main(String[] args) throws IOException {
        try {
            TemplateEngine template = new TemplateEngine();
            
            String content = new String(Files.readAllBytes(Paths.get("/Users/rikenavadur/NetBeansProjects/TemplateEngine/src/templateengine/input_dnc")));
            //String content = new String(Files.readAllBytes(Paths.get("/Users/rikenavadur/NetBeansProjects/TemplateEngine/src/templateengine/input_0")));
            //String content = new String(Files.readAllBytes(Paths.get("/Users/rikenavadur/NetBeansProjects/TemplateEngine/src/templateengine/input_nic")));
            //String content = new String(Files.readAllBytes(Paths.get("/Users/rikenavadur/NetBeansProjects/TemplateEngine/src/templateengine/input_aws")));
            //String content = new String(Files.readAllBytes(Paths.get("/Users/rikenavadur/NetBeansProjects/TemplateEngine/src/templateengine/input_ops")));
            //String content = new String(Files.readAllBytes(Paths.get("/Users/rikenavadur/NetBeansProjects/TemplateEngine/src/templateengine/input_ahc")));
            //String content = new String(Files.readAllBytes(Paths.get("/Users/rikenavadur/NetBeansProjects/TemplateEngine/src/templateengine/input_ec")));
            JSONParser parser = new JSONParser();
            JSONObject input = (JSONObject) parser.parse(content);
            
            String retString = template.apply(input);            
            System.out.println("\n\n\nResult:\n" + retString);
        } catch (ParseException ex) {
            Logger.getLogger(TemplateEngineDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
