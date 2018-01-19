package net.maxgigapop.mrs.driver.dtn;

import java.util.Map;

public interface IDriverPropertySemanticChecker {
	int run(String propertyName, Map<String, String> properties);
	String mapErrorCode(int code);
}
