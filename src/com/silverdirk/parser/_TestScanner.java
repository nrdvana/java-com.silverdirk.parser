package com.silverdirk.parser;

import junit.framework.*;
import java.util.regex.*;

public class _TestScanner extends TestCase {
	private Scanner scanner= null;
	private ScanRule[] initialRules= new ScanRule[] {
		new ScanRule(";"),
		new ScanRule("::"),
		new ScanRule(":"),
		new ScanRule("\\["),
		new ScanRule("\\]"),
		new ScanRule("\\("),
		new ScanRule("\\)"),
		new ScanRule("/\\*", null, 1),
		new ScanRule("[0-9]+"),
		new ScanRule("[A-Za-z_][A-Za-z0-9_]*")
	},
	commentRules= new ScanRule[] {
		new ScanRule("\\*/", null, 0)
	};
	ScanRuleSet[] rulesets;

	public _TestScanner(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		rulesets= new ScanRuleSet[] {new ScanRuleSet("Default", initialRules)};
	}

	protected void tearDown() throws Exception {
		scanner= null;
		rulesets= null;
		super.tearDown();
	}

	public void testSimpleRules() throws Exception {
		String data= "abc[def]ghi;jkl:mno";
		Object[]  expected= new Object[] {
			"abc", "[", "def", "]",
			"ghi", ";", "jkl", ":",
			"mno", TokenSource.EOF
		};
		scanner= new Scanner(rulesets, data);
		for (int i = 0; i < expected.length; i++, scanner.next())
			assertEquals(expected[i], scanner.curToken());
	}

	public void testMultiState() {

	}
}
