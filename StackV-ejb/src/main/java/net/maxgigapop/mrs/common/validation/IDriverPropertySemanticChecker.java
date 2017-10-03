package net.maxgigapop.mrs.common.validation;

import java.util.Map;

public interface IDriverPropertySemanticChecker {
    IReturnCode run(String propertyName, Map<String, String> properties);
    String mapErrorCode(int code);
}
