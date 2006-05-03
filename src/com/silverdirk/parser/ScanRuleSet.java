package com.silverdirk.parser;

import java.util.*;
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
public class ScanRuleSet {
	ScanRule[] rules;
	char[] switchLookup; // a sorted array of characters indicating which list of rules should be used
	int[][] switchBody; // the list of rules that should be processed for this character
	int[] leftoverRegexes;

	public ScanRuleSet(ScanRule[] rules) {
		this.rules= rules;
		buildSwitchTable();
	}

	public ScanRule[] getRulesFor(String str) {
		char firstChar= str.charAt(0);
		int low= 0, high= switchLookup.length;
		while (high-low > 1) {
			int mid= (low+high)>>>1;
			if (firstChar > switchLookup[mid])
				low= mid;
			else if (firstChar < switchLookup[mid])
				high= mid;
			else
				low= high= mid;
		}
		int switchIdx= low;
		// now merge the lists, on rule index (because rule order specifies precedence)
		int[] a= switchBody[switchIdx], b= leftoverRegexes;
		ScanRule[] result= new ScanRule[a.length+b.length];
		int i= 0, aIdx=0, bIdx=0;
		for (; aIdx<a.length && bIdx<b.length; i++)
			if (a[aIdx] < b[bIdx])
				result[i]= rules[a[aIdx++]];
			else
				result[i]= rules[b[bIdx++]];
		if (aIdx >= a.length) {
			aIdx= bIdx;
			a= b;
		}
		for (int offset= aIdx-i; i<result.length; i++)
			result[i]= rules[a[i+offset]];
		return result;
	}

	private void buildSwitchTable() {
		SortedMap charMap= new TreeMap();
		ArrayList patterns= new ArrayList();
		for (int i=0; i<rules.length; i++) {
			if (rules[i].matchTarget instanceof Pattern)
				patterns.add(new Integer(i));
			else if (rules[i].matchTarget instanceof String)
				appendMatchFor(charMap, new Character(((String)rules[i].matchTarget).charAt(0)), i);
			else if (rules[i].matchTarget instanceof Character)
				appendMatchFor(charMap, (Character)rules[i].matchTarget, i);
			else
				throw new RuntimeException("Bug in ScanRuleSet");
		}
		switchLookup= new char[charMap.size()];
		switchBody= new int[charMap.size()][];
		Iterator chars= charMap.entrySet().iterator();
		for (int i=0; chars.hasNext(); i++) {
			Map.Entry cur= (Map.Entry) chars.next();
			switchLookup[i]= ((Character)cur.getKey()).charValue();
			switchBody[i]= toIntArray((List) cur.getValue());
		}
		leftoverRegexes= toIntArray(patterns);
	}
	private static void appendMatchFor(Map mapping, Character ch, int ruleIdx) {
		List list= (List) mapping.get(ch);
		if (list == null) {
			list= new ArrayList();
			mapping.put(ch, list);
		}
		list.add(new Integer(ruleIdx));
	}
	private int[] toIntArray(List list) {
		int[] result= new int[list.size()];
		Iterator item= list.iterator();
		for (int i=0; item.hasNext(); i++)
			result[i]= ((Integer)item.next()).intValue();
		return result;
	}
}