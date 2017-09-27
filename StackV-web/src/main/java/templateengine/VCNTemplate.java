package templateengine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import templateengine.helpers.vcn.*;

public class VCNTemplate extends Template {

    public VCNTemplate() {
        super();
        // Load template
        InputStream fileStream = VCNTemplate.class.getResourceAsStream("/vcn.svt");
        template = null;
        try (Scanner scanner = new Scanner(fileStream, StandardCharsets.UTF_8.name())) {
            template = scanner.useDelimiter("\\A").next();
        }
        
        // Load helpers
        helpers.put("TopologyPolicyData", new TopologyPolicyData());
        helpers.put("VPCSubnetData", new VPCSubnetData());
        helpers.put("ifVCNGateways", new ifVCNGateways());
        helpers.put("DirectConnDest", new DirectConnDest());
        helpers.put("DirectConnVLAN", new DirectConnVLAN());
        helpers.put("isAWS", new isAWS());
        helpers.put("isOPS", new isOPS());
        helpers.put("VMType", new VMType());
        helpers.put("AddressString", new AddressString());
        helpers.put("LinkedGatewayType", new LinkedGatewayType());
        helpers.put("LinkedGatewayFrom", new LinkedGatewayFrom());
    }
}
