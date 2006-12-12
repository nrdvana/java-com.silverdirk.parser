package com.silverdirk.parser;

import java.util.regex.*;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Scan Rule</p>
 * <p>Description: Essentially a robust wrapper around a regex representing the rule</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * Scan rule is a class that is meant to be subclasses to provide an "onMatch"
 * action that performs actions like changing the state of the scanner,
 * incrementing the line number maintained by the scanner, and generating tokens
 * for use by the parser.
 *
 * As subclassing is somewhat annoying, ScanRule also has a number of
 * parameters that let it do common tasks with the default 'onMatch' method.
 *
 * The 'target' parameter can be a Regex, a literal string, or a literal
 * character.  If the scanner's next characters can match this target, then
 * the rule is activated.  the default action is to emit the token specified in
 * the constructor, and change the state of the parser to the state specified
 * in the constructor if it is greater than zero.
 *
 * The 'token' parameter can be either an actual token to return, or EMIT_MATCH
 * to return the matched string, or EMIT_NOTHING to tell the scanner not to
 * generate a token from this rule.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class ScanRule {
	Object matchTarget;
	Object token;
	int stateTrans= NO_STATE_TRANS;


	public ScanRule(Object target, Object token, int stateTrans) {
		matchTarget= target;
		this.token= token;
		this.stateTrans= stateTrans;
	}

	public ScanRule(Pattern regex, Object token) {
		this((Object)regex, token, NO_STATE_TRANS);
	}
	public ScanRule(String word, Object token) {
		this((Object)word, token, NO_STATE_TRANS);
		if (word.length() == 0)
			throw new RuntimeException("A ScanRule string-match parameter must be at least one character");
	}
	public ScanRule(char ch, Object token) {
		this(new Character(ch), token, NO_STATE_TRANS);
	}
	public ScanRule(Character ch, Object token) {
		this((Object)ch, token, NO_STATE_TRANS);
	}

	public ScanRule(Pattern regex) {
		this(regex, EMIT_MATCH);
	}
	public ScanRule(String word) {
		this(word, EMIT_MATCH);
	}
	public ScanRule(char ch) {
		this(new Character(ch));
	}
	public ScanRule(Character ch) {
		this(ch, ch);
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

	static class ScanMatch {
		int charsConsumed;
		Object token;
		public ScanMatch(Object token, int charsConsumed) {
			this.token= token;
			this.charsConsumed= charsConsumed;
		}
	}

	/** Attempt to match this rule against a characterSource, and return a match if successful.
	 *
	 * @param sender Scanner The scanner object calling this method
	 * @param source CharSequence The character source to compare against
	 * @return ScanMatch An object describing the match if successful, or null if no match was found
	 */
	ScanMatch getMatch(Scanner sender, CharSequence source) {
		String text= null;
		Object token= null;
		int charsConsumed= 0;
		try {
			if (matchTarget instanceof Character) {
				if (source.charAt(0) == ((Character)matchTarget).charValue()) {
					token= onMatch(source.subSequence(0, 1).toString(), sender);
					charsConsumed= 1;
				}
			}
			else if (matchTarget instanceof String) {
				String str= (String) matchTarget;
				if (source.length() >= str.length()
					&& str.equals(source.subSequence(0, str.length()).toString()))
				{
					token= onMatch(str, sender);
					charsConsumed= str.length();
				}
			}
			else if (matchTarget instanceof Pattern) {
				Matcher m= ((Pattern)matchTarget).matcher(source);
				if (m.lookingAt()) {
					token= onMatch(m, sender);
					charsConsumed= m.end();
					if (charsConsumed == 0)
						throw new RuntimeException("ScanRules must consume at least one character.  The pattern "+matchTarget+" can match an empty string");
				}
			}
			else
				throw new RuntimeException("BUG");
		}
		catch (Exception ex) {
			throw (ex instanceof RuntimeException)? (RuntimeException)ex : new RuntimeException(ex);
		}
		return charsConsumed == 0? null : new ScanMatch(token, charsConsumed);
	}

	/** Return the token scanned, and perform other actions.
	 * The default action is to return onMatch(match.group(), scanner)
	 *
	 * @param match Matcher The regex Matcher object
	 * @param scanner Scanner The scanner calling this method
	 * @return Object An object, including NULL, which should be passed as a token to the parser, or EMIT_NOTHING if no token should be generated.
	 * @throws Exception for convenience to the user overriding this method
	 */
	public Object onMatch(Matcher match, Scanner scanner) throws Exception {
		return onMatch(match.group(), scanner);
	}

	/** Return the token scanned, and perform other actions.
	 * The default action is to return the token described in the constructor.
	 *
	 * @param scannedData String The string matched
	 * @param scanner Scanner The scanner calling this method
	 * @return Object An object, including NULL, which should be passed as a token to the parser, or EMIT_NOTHING if no token should be generated.
	 * @throws Exception for convenience to the user overriding this method
	 */
	public Object onMatch(String scannedData, Scanner scanner) throws Exception {
		if (hasStateTransition())
			scanner.stateTrans(getStateTransition());
		return (token == EMIT_MATCH)? scannedData : token;
	}

	public static final Object
		EMIT_MATCH= new Object(),
		EMIT_NOTHING= new Object();
	public static final int NO_STATE_TRANS= -1;
}
