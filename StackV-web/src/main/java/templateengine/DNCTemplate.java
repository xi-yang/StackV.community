package templateengine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import templateengine.helpers.dnc.DNCType;

public class DNCTemplate extends Template {

    public DNCTemplate() {
        super();
        // Load template
        InputStream fileStream = DNCTemplate.class.getResourceAsStream("/dnc.svt");
        template = null;
        try (Scanner scanner = new Scanner(fileStream, StandardCharsets.UTF_8.name())) {
            template = scanner.useDelimiter("\\A").next();
        }

        // Load helpers
        helpers.put("DNCType", new DNCType());
    }
}
