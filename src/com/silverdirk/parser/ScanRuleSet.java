package com.silverdirk.parser;

import java.util.*;
import java.util.regex.*;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Set of Scan Rules</p>
 * <p>Description: Holds a named set of scan rules representing the options for one state of the Scanner</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * <p>The implementation of this class is clumsy.  See comments at the top of
 * Scanner.java.  As I intend to destroy all the code in this file, I am not
 * going to document it further.
 *
 * <p>After the rewrite, this class will hold a reference to parameters for a
 * regex engine, so that the scanner can plug the parameters and the current
 * input into the regex engine and come up with a sequence of rules to execute.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class ScanRuleSet {
	String stateName;
	Pattern masterRegex;
	ScanRule[] rules;
	int[] ruleGroupIdx;

	public ScanRuleSet(String name, ScanRule[] rules) {
		this.stateName= name;
		this.rules= rules;
		buildMasterRegex();
	}

	public String getName() {
		return stateName;
	}

	public ScanRule[] getRules() {
		// sadly, we can't trust the public, so we have to make a copy
		return (ScanRule[]) rules.clone();
	}

	private void buildMasterRegex() {
		ruleGroupIdx= new int[rules.length+1];
		ruleGroupIdx[0]= 1;
		StringBuffer sb= new StringBuffer("(");
		for (int i=0; i<rules.length; i++) {
			Pattern p= Pattern.compile(rules[i].pattern);
			Matcher m= p.matcher("");
			if (m.lookingAt())
				throw new RuntimeException("ScanRules must consume at least one character.  The pattern "+rules[i].pattern+" can match an empty string");
			ruleGroupIdx[i+1]= ruleGroupIdx[i] + 1 + m.groupCount();
			if (i != 0) sb.append(")|(");
			sb.append(rules[i].pattern);
		}
		sb.append(')');
		masterRegex= Pattern.compile(sb.toString());
		// check for the condition of matching the empty string
		Matcher m= masterRegex.matcher("");
		if (m.lookingAt())
			throw new RuntimeException("BUG: Somehow the master regex can match the empty string, though none of the components can");
		if (m.groupCount() != ruleGroupIdx[rules.length]-1)
			throw new RuntimeException("Unexpected number of groups");
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
			Matcher m= masterRegex.matcher(source);
			if (m.lookingAt()) {
				int rule= -1;
				for (int i=0; rule == -1 && i<rules.length; i++)
					if (m.start(ruleGroupIdx[i]) != -1)
						rule= i;
				String[] groups= collectGroups(m, ruleGroupIdx[rule], ruleGroupIdx[rule+1]);
				token= rules[rule].onMatch(groups, sender);
				charsConsumed= m.end();
			}
		}
		catch (Exception ex) {
			throw (ex instanceof RuntimeException)? (RuntimeException)ex : new RuntimeException(ex);
		}
		return charsConsumed == 0? null : new ScanMatch(token, charsConsumed);
	}

	static final String[] collectGroups(Matcher m, int from, int to) {
		String[] result= new String[to-from+1];
		result[0]= m.group();
		for (int i=from; i<to; i++)
			result[i-from+1]= m.group(i);
		return result;
	}
}
