import java.util.regex.*;

/**
 * <p>Project: Brainstormer</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright Copyright (c) 2007</p>
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class Util {
	static final String trimPossibleNull(String val) {
		return (val == null)? "" : val.trim();
	}

	static final int parseOrDefault(String number, int defaultVal) {
		try {
			if (number != null)
				return Integer.parseInt(number);
		}
		catch (NumberFormatException ex) {}
		return defaultVal;
	}

	static final Pattern javaStringEscapesPattern= Pattern.compile("[\\\\\"]");
	static final String displayAsJavaString(String str) {
		Matcher m= javaStringEscapesPattern.matcher(str);
		StringBuffer sb= new StringBuffer(str.length()+10);
		sb.append('"');
		while (m.find())
			m.appendReplacement(sb, "\\\\$0");
		m.appendTail(sb);
		sb.append('"');
		return sb.toString();
	}
}
