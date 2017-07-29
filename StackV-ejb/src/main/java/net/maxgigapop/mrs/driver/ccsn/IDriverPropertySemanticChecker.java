package net.maxgigapop.mrs.driver.ccsn;

import java.util.Map;

public interface IDriverPropertySemanticChecker {
	int run(String propertyName, Map<String, String> properties);
	String mapErrorCode(int code);
}
