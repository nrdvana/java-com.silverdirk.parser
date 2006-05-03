package com.silverdirk.parser;

import junit.framework.*;
import java.util.regex.*;

public class _TestScanRuleSet extends TestCase {
	private ScanRuleSet scanRuleSet= null;

	private ScanRule[] rules= new ScanRule[] {
		new ScanRule('a'),
		new ScanRule("ab"),
		new ScanRule("ac"),
		new ScanRule("aa"),
		new ScanRule(Pattern.compile("aa")),
		new ScanRule("bb"),
		new ScanRule(Pattern.compile("bb"))
	};

	public _TestScanRuleSet(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		scanRuleSet= new ScanRuleSet(rules);
	}

	protected void tearDown() throws Exception {
		scanRuleSet= null;
		super.tearDown();
	}

	public void test() {
		ScanRule[] actual= scanRuleSet.getRulesFor("aaaa\n");
		ScanRule[] expected= new ScanRule[] { rules[0], rules[1], rules[2], rules[3], rules[4], rules[6]};
		assertEquals(actual.length, expected.length);
		for (int i=0; i<actual.length; i++)
			assertEquals(actual[i], expected[i]);
	}
}
