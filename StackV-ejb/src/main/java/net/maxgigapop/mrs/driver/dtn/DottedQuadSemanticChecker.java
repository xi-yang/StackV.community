package net.maxgigapop.mrs.driver.dtn;

import java.util.Map;

class DottedQuadSemanticChecker implements IDriverPropertySemanticChecker {

	@Override
	public int run(String propertyName, Map<String, String> properties) {
		int rc = 0;
		String propertyVal = properties.get(propertyName);
		String[] quads = propertyVal.split(".");
		for (String quad : quads) {
			if (quad.startsWith("0") && quad.length() > 1) {
				rc = 1;
				break;
			}
			if (Integer.valueOf(quad) > 255) {
				rc = 2;
				break;
			}
		}
		return rc;
	}

	@Override
	public String mapErrorCode(int code) {
		String mesg;
		switch (code) {
		case 1:
			mesg = "Improperly formatted decimal detected in IPv4 address, please remove preceding 0's";
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
