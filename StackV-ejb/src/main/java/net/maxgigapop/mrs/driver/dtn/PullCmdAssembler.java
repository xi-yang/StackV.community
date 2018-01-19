package net.maxgigapop.mrs.driver.dtn;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

class PullCmdAssembler {
	private String pullInvokePattern;
	private Logger logger;

	public PullCmdAssembler(String pullInvokePattern, Logger logger) {
		this.pullInvokePattern = pullInvokePattern;
		this.logger = logger;
	}

	public String generateCmdStr(Map<String, String> pullCommandParamMap) {
		String cmdStr = pullInvokePattern;
		HashMap<String, String> pullUtilParamsCopy = new HashMap<>();
		shallowCopy(pullCommandParamMap, pullUtilParamsCopy);
		String[] keys = new String[pullUtilParamsCopy.size()];		
		
		while (!pullUtilParamsCopy.isEmpty()) {
			// Find matching argument placeholder, log error and throw exception if not found
			String param = pullUtilParamsCopy.keySet().toArray(keys)[0],
				   val = pullUtilParamsCopy.get(param);
			pullUtilParamsCopy.remove(param);
			param = "@" + param;
			int spliceHeadSubstrCutoff = cmdStr.indexOf(param),
				spliceTailSubstrBegin = spliceHeadSubstrCutoff + param.length();
//			if (spliceHeadSubstrCutoff == 0) {
//				String errMsg = String.format("Malformed pull command pattern ('%s' found at head)", param);
//				logger.log(Level.SEVERE, errMsg);
//				throw new IllegalStateException(errMsg);
//			}
			if (spliceHeadSubstrCutoff < 0) {
				String errMsg = String.format("Placeholder '%s' not found in pull command pattern '%s'", param, cmdStr);
				logger.log(Level.SEVERE, errMsg);
				throw new IllegalStateException(errMsg);
			}
			
			// Splice in argument value
			StringBuilder sb = new StringBuilder();
			sb.append(cmdStr.substring(0, spliceHeadSubstrCutoff));
			sb.append(val);
			sb.append(cmdStr.substring(spliceTailSubstrBegin));
			cmdStr = sb.toString();
		}

		return cmdStr;
	}
	
//	private static int indexOf(Pattern pattern, String s) {
//    Matcher matcher = pattern.matcher(s);
//    return matcher.find() ? matcher.start() : -1;
//	}

	private static void shallowCopy(Map<String, String> pullCommandParamMap, HashMap<String, String> dst) {
		for (Entry<String, String> content : pullCommandParamMap.entrySet())
			dst.put(content.getKey(), content.getValue());
	}
}
