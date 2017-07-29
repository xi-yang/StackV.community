package net.maxgigapop.mrs.driver.ccsn;
import java.util.Map;

public class DriverPropertyValidator {
	private DriverPropertyValidator() {}
	public static String validateAndReturn(String propertyName, Map<String, String> properties, DriverPropertyType... types) {
		// Perform basic format checks
		String propertyVal = properties.get(propertyName);
		if (properties.get(propertyName) == null)
			throw new IllegalStateException(String.format("Invalid driver configuration file: No <key> found for required field '%s'", propertyName));
		if (properties.get(propertyName).isEmpty())
			throw new IllegalStateException(String.format("Invalid driver configuration file: Empty <value> for required field '%s'", propertyName));
		boolean isValid = false;
		StringBuilder errorMessageFragmentList = new StringBuilder();
		for (DriverPropertyType type : types) {
			isValid |= type.compile().matcher(propertyVal).matches();
			errorMessageFragmentList.append(
					errorMessageFragmentList.length() > 0 ? String.format("; %s", type.getErrorMessageFragment()) : type.getErrorMessageFragment()
					);
		}
		if (!isValid)
			throw new IllegalStateException(
					String.format("Invalid driver property value: Format of '%s' does not conform to %s (%s)", propertyName, errorMessageFragmentList, propertyVal)
					);
		
		// Perform deep semantic checks if available for DriverPropertyType(s)
		isValid = false;
		boolean semanticsChecked = false;
		StringBuilder errorMessageList = new StringBuilder();
		for (DriverPropertyType type : types) {
			if (type.getChecker() != null) {
				semanticsChecked = true;
				int rc = type.getChecker().run(propertyName, properties);
				isValid |= (rc == 0);
				if (rc > 0) {
					errorMessageList.append(
							errorMessageList.length() > 0 ? String.format("; %s", type.getChecker().mapErrorCode(rc)) : type.getChecker().mapErrorCode(rc)
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
