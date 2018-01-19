package net.maxgigapop.mrs.driver.ccsn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DateTimeParser {
	private DateTimeParser() {}

	public static
	long timestampToSeconds(String duration) {
		long seconds = 0L;

		// Verify that string contains timestamp (i.e., sequence of 3 colon-separated numeric characters)
		String pattern = "\\d{2,3}:\\d{2}:\\d{2}";
		Pattern re = Pattern.compile(pattern);
		Matcher m = re.matcher(duration);

		if (m.find()) {
			// Extract 'days' parameter if present
			pattern = "^\\d+\\s*[d|D]";
			re = Pattern.compile(pattern);
			m = re.matcher(duration);
			if (m.find()) {
				// If found then truncate duration string and extract and convert days quantity to seconds,
				//  add to tally
				String days = m.group(0);
				duration = duration.substring(days.length()).trim();
				pattern = "\\d+";
				re = Pattern.compile(pattern);
				m = re.matcher(days);
				m.find();
				days = m.group(0);
				seconds = Integer.parseInt(days) * 86400L;
			}

			// Convert and tally total time in seconds
			long conv = 3600L;
			for (String field : duration.split(":")) {
				seconds += Integer.parseInt(field) * conv;
				conv /= 60;
			}
		}

		return seconds;
	}
}
