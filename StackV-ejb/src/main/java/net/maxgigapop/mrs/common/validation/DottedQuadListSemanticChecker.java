package net.maxgigapop.mrs.common.validation;

import java.util.Map;

public class DottedQuadListSemanticChecker implements IDriverPropertySemanticChecker {
    static enum ERC implements IReturnCode {
        PASS(0, null),
        BAD_DECIMAL(1, "Improperly formatted decimal detected in IPv4 address, remove preceding 0's"),
        DECIMAL_OVERFLOW(2, "Decimal value greater than 255 detected"),
        BAD_IPV4_FORMAT(3, "Property value failed IPv4 dotted quad format check"),
        BAD_LIST_FORMAT(4, "Property value failed generic list format check");

        private final int rc;
        private final String errMsg;
        
        ERC(int rc, String errMsg) {
            this.rc = rc;
            this.errMsg = errMsg;
        }
        
        @Override
        public String getMessage() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int toCode() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }

    @Override
    public IReturnCode run(String propertyName, Map<String, String> properties) {
        ERC erc = ERC.PASS;
        String propertyVal = properties.get(propertyName);
        String[] dottedQuads = propertyVal.split(DriverPropertyValidator.getDelimPattern());
        for (String dottedQuad : dottedQuads) {
            // Some delimiter patterns may create empty matches when splitting between symbols (e.g., ')' and '('),
            // so skip iteration
            // Note: Basic formatting should already have been validated
            if (dottedQuad.isEmpty())
                continue;
            String[] decimals = dottedQuad.split(".");
            for (String decimal : decimals) {
                if (decimal.startsWith("0") && decimal.length() > 1) {
                    erc = ERC.BAD_DECIMAL;
                    break;
                }
                if (Integer.valueOf(decimal) > 255) {
                    erc = ERC.DECIMAL_OVERFLOW;
                    break;
                }
            }
            if (erc != ERC.PASS)
                break;
        }
        return erc;
    }

    @Override
    public String mapErrorCode(int code) {
        String mesg;
        switch (code) {
        case 1:
                mesg = "Improperly formatted decimal detected in IPv4 address, remove preceding 0's";
                break;
        case 2:
                mesg = "Decimal value greater than 255 detected";
                break;
        default:
                mesg = "Unknown error type, please submit bug report to development team";
        }
        return mesg;
    }

}
