package net.maxgigapop.mrs.driver.ccsn;
import java.util.regex.Pattern;

public enum DriverPropertyType {
	DottedQuad(
			// "([0-9]{1,3}\\.){3}[0-9]{1,3}",
			// "IPv4 dotted quad",
			new DottedQuadSemanticChecker()),
	DottedQuadList(
			// "(\\((([0-9]{1,3})\\.){3}[0-9]{1,3}\\)){1,}",
			// "parentheses-separated list of IPv4 dotted quads",
			new DottedQuadListSemanticChecker()),
	Command(
			// "[\\w\\-_\\/]{2,}(\\s(@|)[\\w\\-_]+((=@|)[\\w\\-_]+|)){1,}",
			// "recognized command pattern sequence",
			new CommandSemanticChecker()),
	CommandList(
			// "(\\([\\w\\-_\\/]{2,}(\\s(@|)[\\w\\-_]+((=@|)[\\w\\-_]+|)){1,}\\)){1,}",
			// "parentheses-separated list of recognized command pattern sequences",
			null),
	Generic(
			// "[\\w\\-\\._#\\/:]+",
			// "allowed field values, which may only contain alphanumerics and the symbols '-', '#', '.', ':' and '_'",
			null),
	GenericList(
			// "(\\([\\w\\-\\._#\\/:]{3,}\\)){1,}",
			// "parenthes-separated list of allowed field values, which may only contain alphanumerics and the symbols '-', '#', '.', ':' and '_'",
			null);
	
	// private final String regex;
	// private final String errMssgFragm;
	private final IDriverPropertySemanticChecker checker;
	DriverPropertyType(IDriverPropertySemanticChecker checker) {
		// this.regex = regex;
		// this.errMssgFragm = errMessageFragment;
		this.checker = checker;
	}
	
	// public Pattern compile() {
	// 	return Pattern.compile(regex);
	// }
	
	// public String getErrorMessageFragment() {
	// 	return errMssgFragm;
	// }
	
	public IDriverPropertySemanticChecker getChecker() {
		return checker;
	}
}
