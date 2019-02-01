package templateengine;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
            String content = new String(
                    Files.readAllBytes(Paths.get("StackV-web/src/main/java/templateengine/input_1")));
            JSONParser parser = new JSONParser();
            JSONObject input = (JSONObject) parser.parse(content);

            String retString = template.apply(input);
            System.out.println("\n\n\nResult:\n" + retString);
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter("StackV-web/src/main/java/templateengine/output"));
            writer.write(retString);
            writer.close();
        } catch (ParseException ex) {
            Logger.getLogger(TemplateEngineDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
