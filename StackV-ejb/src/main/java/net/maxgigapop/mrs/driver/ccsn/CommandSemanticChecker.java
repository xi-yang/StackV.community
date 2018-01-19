package net.maxgigapop.mrs.driver.ccsn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
// import java.util.regex.Matcher;

class CommandSemanticChecker implements IDriverPropertySemanticChecker {
    static enum ERC implements IReturnCode {
        PASS(0, null),
        BAD_INVOKE(1, "Malformed shell command given in command pattern"),
        MISSING_ARGVAL(2, "Missing switch value detected"),
        BAD_ARGFORMAT(3, "Bad switch-value formatting"),
        NO_ARGS(4, "No arguments detected in command pattern"),
        NO_PLACEHOLDERS(5, "No placeholders detected in command pattern"),
        UNKNOWN_PROPERTY(6, "Unmatched placeholder found in command pattern");

        private final String mesg;
        private final int rc;

        ERC(int rc, String mesg) {
            this.rc = rc;
            this.mesg = mesg;
        }

        @Override
        public String getMessage() {
            return mesg;
        }
        
        @Override
        public int toCode() {
            return rc;
        }

        public static ERC fromCode(int code) {
            for (ERC rc : ERC.values()) {
                if (rc.toCode() == code)
                    return rc;
            }
            return null;
        }
    }

    private final static Pattern
            cmdPathNamePattern = Pattern.compile("([\\/]{0,1}[\\w\\.]+)+"),
            switchNamePattern = Pattern.compile("-[a-zA-Z0-9]+"),
            negativeIntegerPattern = Pattern.compile("-[0-9]+"),
            placeHolderPattern = Pattern.compile("(@|#|\\$|%).+"),
            keyEqualValPattern = Pattern.compile("--[a-zA-Z0-9]+=.+");

    @Override
    public IReturnCode run(String propertyName, Map<String, String> properties) {
        String propertyVal = properties.get(propertyName);
        String[] commandFrags = propertyVal.split("\\s");

        // Check that there are arguments
        if (commandFrags.length < 2)
            return ERC.NO_ARGS;

        // Check command path/name
        if (cmdPathNamePattern.matcher(commandFrags[0]).matches())
            return ERC.BAD_INVOKE;
		
        // Check arguments to ensure that there is at least one placeholder
        int cursor = 1;
            ERC erc = null;
            List<String> placeholders = new ArrayList<>();
            while (cursor < commandFrags.length) {
                // Check for standard switch-value pair argument (-sw value)
                //   Return error code if switch name is not followed by a value
                //   Otherwise add to list of discovered placeholders if value prefixed with '@'
                if (switchNamePattern.matcher(commandFrags[cursor]).matches()) {
                    if (cursor + 1 == commandFrags.length) {
                        erc = ERC.MISSING_ARGVAL;
                        break;
                    }
                    String nextFrag = commandFrags[++cursor];
                    if (switchNamePattern.matcher(nextFrag).matches() && negativeIntegerPattern.matcher(nextFrag).matches()) {
                        erc = ERC.MISSING_ARGVAL;
                        break;
                    }
                    if (placeHolderPattern.matcher(nextFrag).matches())
                            placeholders.add(nextFrag.substring(1));
                }

                // Check for newer '=' separated switch-value argument (--sw=value)
                //   Return error code if multiple assignment '=' chars detected
                //   Otherwise add to list of discovered placeholder if value prefixed with '@'
                else if (keyEqualValPattern.matcher(commandFrags[cursor]).matches()) {
                    String[] argFrags = commandFrags[cursor].split("=");
                    if (argFrags.length > 2) {
                        erc = ERC.BAD_ARGFORMAT;
                        break;
                    }
                    if (placeHolderPattern.matcher(argFrags[1]).matches())
                        placeholders.add(argFrags[1].substring(1));
                }

                // Check for "orphaned" argument (i.e., no obvious switch-value pair), add to list
                // of discovered placeholders if value prefixed with '@'
                else if (placeHolderPattern.matcher(commandFrags[cursor]).matches())
                    placeholders.add(commandFrags[cursor].substring(1));

                ++cursor;
            }
            // Exit immediately if errors found
            if (erc != null)
                    return erc;

            if (placeholders.size() == 0)
                    return ERC.NO_PLACEHOLDERS;

            // Check that placeholders map to known driver properties
            for (String property : placeholders)
                    if (!properties.containsKey(property))
                            return ERC.UNKNOWN_PROPERTY;

            return ERC.PASS;
    }

    @Override
    public String mapErrorCode(int rc) {
        ERC erc = ERC.fromCode(rc);
        String errMsg = null;
        if (erc == null)
            throw new IllegalArgumentException(String.format("CommandSemanticChecker: Unrecognized error code '%s'", rc));
        switch (erc) {
        case BAD_INVOKE:
            errMsg = "Malformed shell command given in command pattern";
            break;
        case MISSING_ARGVAL:
            errMsg = "Missing switch value detected";
            break;
        case BAD_ARGFORMAT:
            errMsg = "Bad switch-value formatting";
            break;
        case NO_ARGS:
            errMsg = "No arguments detected in command pattern";
            break;
        case NO_PLACEHOLDERS:
            errMsg = "No placeholders detected in command pattern";
        }
        return errMsg;
    }
}