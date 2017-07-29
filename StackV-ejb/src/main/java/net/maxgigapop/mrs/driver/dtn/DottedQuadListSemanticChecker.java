package net.maxgigapop.mrs.driver.dtn;

import java.util.Map;

public class DottedQuadListSemanticChecker implements IDriverPropertySemanticChecker {

	@Override
	public int run(String propertyName, Map<String, String> properties) {
		int rc = 0;
		String propertyVal = properties.get(propertyName);
		String[] dottedQuads = propertyVal.split(DTNDriver.delimiterPattern);
		for (String dottedQuad : dottedQuads) {
			// Some delimiter patterns may create empty matches when splitting between symbols (e.g., ')' and '('),
			// so skip iteration
			// Note: Basic formatting should already have been validated
			if (dottedQuad.isEmpty())
				continue;
			String[] decimals = dottedQuad.split(".");
			for (String decimal : decimals) {
				if (decimal.startsWith("0") && decimal.length() > 1) {
					rc = 1;
					break;
				}
				if (Integer.valueOf(decimal) > 255) {
					rc = 2;
					break;
				}
			}
			if (rc > 0)
				break;
		}
		return rc;
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
