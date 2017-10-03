package net.maxgigapop.mrs.common.validation;
import java.util.Map;

public class DriverPropertyValidator {
    private DriverPropertyValidator() {}
    private static String delimPattern = "[\\(\\)]";
    
    public static void setDelimPattern(String delimPattern) {
        DriverPropertyValidator.delimPattern = delimPattern;
    }
    static String getDelimPattern() {
        return DriverPropertyValidator.delimPattern;
    }
    
    public static String validateAndReturn(String propertyName, Map<String, String> properties, DriverPropertyType... types) {
        // Perform basic format checks
        String propertyVal = properties.get(propertyName);
        if (properties.get(propertyName) == null)
            throw new IllegalStateException(String.format("Invalid driver configuration file: No <key> found for required field '%s'", propertyName));
        if (properties.get(propertyName).isEmpty())
            throw new IllegalStateException(String.format("Invalid driver configuration file: Empty <value> for required field '%s'", propertyName));

        boolean isValid = false;
        boolean semanticsChecked = false;
        StringBuilder errorMessageList = new StringBuilder();
        for (DriverPropertyType type : types) {
            if (type.getChecker() != null) {
                semanticsChecked = true;
                IReturnCode rc = type.getChecker().run(propertyName, properties);
                isValid |= (rc.toCode() == 0);
                if (rc.toCode() > 0) {
                    errorMessageList.append(
                            errorMessageList.length() > 0 ? String.format("; %s--%s", type.getClass(), rc.getMessage()) : String.format("%s--%s", type.getClass(), rc.getMessage())
                            );
                }
            }
        }
        if (semanticsChecked && !isValid)
            throw new IllegalStateException(
                    String.format("Invalid driver property value for '%s': %s", propertyName, errorMessageList)
                    );

        return propertyVal;
    }
}
