package net.maxgigapop.mrs.driver.ccsn;

import java.util.Map;
import java.util.regex.Pattern;

class DottedQuadSemanticChecker implements IDriverPropertySemanticChecker {

    static enum ERC implements IReturnCode {
        PASS(0, null),
        BAD_DECIMAL(1, "Improperly formatted decimal detected in IPv4 address, please remove preceding 0's"),
        DECIMAL_OVERRUN(2, "Decimal value greater than 255 detected"),
        BAD_FORMAT(3, "Property value failed IPv4 dotted quad format check");
        
        private final int rc;
        private final String errMsg;
        
        ERC(int rc, String errMsg) {
            this.rc = rc;
            this.errMsg = errMsg;
        }

        @Override
        public String getMessage() {
            return errMsg;
        }

        @Override
        public int toCode() {
            return rc;
        }
    }
    
    private static final Pattern dottedQuadPattern = Pattern.compile("([0-9]{1,3}\\.){3}[0-9]{1,3}");

    @Override
    public IReturnCode run(String propertyName, Map<String, String> properties) {
        ERC erc = ERC.PASS;
        // Verify formatting dotted quad format
        if (!dottedQuadPattern.matcher(propertyName).matches())
            erc = ERC.BAD_FORMAT;
        else {
            // Verify legal decimal values
            String propertyVal = properties.get(propertyName);
            String[] quads = propertyVal.split(".");
            for (String quad : quads) {
                if (quad.startsWith("0") && quad.length() > 1) {
                    erc = ERC.BAD_DECIMAL;
                    break;
                }
                if (Integer.valueOf(quad) > 255) {
                    erc = ERC.DECIMAL_OVERRUN;
                    break;
                }
            }
        }
        return erc;
    }

    @Override
    public String mapErrorCode(int code) {
        String mesg = null;
        switch (code) {
        case 1:
            mesg = "Improperly formatted decimal detected in IPv4 address, please remove preceding 0's";
            break;
        case 2:
            mesg = "Decimal value greater than 255 detected";
            break;
        case 3:
            mesg = "Property value failed IPv4 dotted quad format check";
            break;
        default:;
        }
        return mesg;
    }
}
