package com.silverdirk.parser;

import java.util.regex.*;

/**
 * <p>Project: com.silverdirk</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * @author not attributable
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
		this(word, word);
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
		return stateTrans != NO_STATE_TRANS;
	}

	static class ScanMatch {
		int charsConsumed;
		Object token;
		public ScanMatch(Object token, int charsConsumed) {
			this.token= token;
			this.charsConsumed= charsConsumed;
		}
	}

	public ScanMatch getMatch(CharSequence source) {
		Object token= null;
		int charsConsumed= 0;
		try {
			if (matchTarget instanceof Character) {
				if (source.charAt(0) == ((Character)matchTarget).charValue()) {
					token= onMatch(source.subSequence(0, 1).toString());
					charsConsumed= 1;
				}
			}
			else if (matchTarget instanceof String) {
				String str= (String) matchTarget;
				if (source.length() >= str.length()
					&& str.equals(source.subSequence(0, str.length()).toString()))
				{
					token= onMatch(str);
					charsConsumed= str.length();
				}
			}
			else if (matchTarget instanceof Pattern) {
				Matcher m= ((Pattern)matchTarget).matcher(source);
				if (m.lookingAt()) {
					token= onMatch(m.group());
					charsConsumed= m.end();
				}
			}
			else
				throw new RuntimeException("BUG");
		}
		catch (Exception ex) {
			throw (ex instanceof RuntimeException)? (RuntimeException)ex : new RuntimeException(ex);
		}
		return (token == null)? null : new ScanMatch(token, charsConsumed);
	}

	public Object onMatch(String scannedData) throws Exception {
		return (token == EMIT_MATCH)? scannedData : token;
	}

	public static final Object
		EMIT_MATCH= new Object(),
		EMIT_NOTHING= new Object();
	public static final int NO_STATE_TRANS= -1;
}