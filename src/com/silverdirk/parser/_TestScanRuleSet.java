package com.silverdirk.parser;

import junit.framework.*;
import java.util.regex.*;

public class _TestScanRuleSet extends TestCase {
	private ScanRuleSet scanRuleSet= null;

	private ScanRule[] rules= new ScanRule[] {
		new ScanRule("ab"),
		new ScanRule("ac"),
		new ScanRule("aa"),
	};

	public _TestScanRuleSet(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		scanRuleSet= new ScanRuleSet("Default State", rules);
	}

	protected void tearDown() throws Exception {
		scanRuleSet= null;
		super.tearDown();
	}

	public void test() {
	}
}
