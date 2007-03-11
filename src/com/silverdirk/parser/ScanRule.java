package com.silverdirk.parser;

import java.util.regex.*;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Scan Rule</p>
 * <p>Description: Essentially a robust wrapper around a regex representing the rule</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * <p>Scan rule is a class that is meant to be subclasses to provide an "onMatch"
 * action that performs actions like changing the state of the scanner,
 * incrementing the line number maintained by the scanner, and generating tokens
 * for use by the parser.
 *
 * <p>As subclassing is somewhat annoying, ScanRule also has a number of
 * parameters that let it do common tasks with the default 'onMatch' method.
 *
 * <p>The 'target' parameter can be a Regex, a literal string, or a literal
 * character.  If the scanner's next characters can match this target, then
 * the rule is activated.  the default action is to emit the token specified in
 * the constructor, and change the state of the parser to the state specified
 * in the constructor if it is greater than zero.
 *
 * <p>The 'token' parameter can be either an actual token to return, or EMIT_MATCH
 * to return the matched string, or EMIT_NOTHING to tell the scanner not to
 * generate a token from this rule.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class ScanRule {
	String pattern;
	Object token;
	int stateTrans= NO_STATE_TRANS;

	static final Pattern regexSpecialChars= Pattern.compile("([]\\[(){}*+?.|\\\\^$])");
	public static final String escapeLiteralStr(String literal) {
		return regexSpecialChars.matcher(literal).replaceAll("\\\\$1");
	}

	public ScanRule(String word, Object token, int stateTrans) {
		this.pattern= word;
		this.token= token;
		this.stateTrans= stateTrans;
		if (word.length() == 0)
			throw new RuntimeException("A ScanRule string-match parameter must be at least one character");
	}

	public ScanRule(String word, Object token) {
		this(word, token, NO_STATE_TRANS);
	}

	public ScanRule(String word) {
		this(word, EMIT_MATCH);
	}

	public String getPattern() {
		return pattern;
	}

	public Object getToken() {
		return token;
	}

	public int getStateTransition() {
		return stateTrans;
	}
	public void setStateTransition(int value) {
		this.stateTrans= value;
	}

	public boolean hasStateTransition() {
		return stateTrans >= 0;
	}

	/** Return the token scanned, and perform other actions.
	 * The default action is to return the token described in the constructor.
	 *
	 * @param scannedData String The "groups" of the Matcher which matched the string
	 * @param scanner Scanner The scanner calling this method
	 * @return Object An object, including NULL, which should be passed as a token to the parser, or EMIT_NOTHING if no token should be generated.
	 * @throws Exception for convenience to the user overriding this method
	 */
	public Object onMatch(String[] matchGroups, Scanner scanner) throws Exception {
		if (hasStateTransition())
			scanner.stateTrans(getStateTransition());
		return (token == EMIT_MATCH)? matchGroups[0] : token;
	}

	public static final Object
		EMIT_MATCH= new Object(),
		EMIT_NOTHING= new Object();
	public static final int NO_STATE_TRANS= -1;
}
