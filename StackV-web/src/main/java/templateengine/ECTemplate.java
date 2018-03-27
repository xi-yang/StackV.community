package templateengine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import templateengine.helpers.dnc.*;

public class ECTemplate extends Template {

    public ECTemplate() {
        super();
        // Load template
        InputStream fileStream = ECTemplate.class.getResourceAsStream("/ecc.svt");
        template = null;
        try (Scanner scanner = new Scanner(fileStream, StandardCharsets.UTF_8.name())) {
            template = scanner.useDelimiter("\\A").next();
        }

        // Load helpers
        helpers.put("ECPolicyData", new ECPolicyData());
    }
}
