package com.silverdirk.parser;

import junit.framework.*;

public class _TestPackage extends TestCase {

	public _TestPackage(String s) {
		super(s);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(com.silverdirk.parser._TestScanRuleSet.class);
		suite.addTestSuite(com.silverdirk.parser._TestScanner.class);
		suite.addTestSuite(com.silverdirk.parser._TestTableBuilder.class);
		suite.addTestSuite(com.silverdirk.parser._TestLR1_Table.class);
		suite.addTestSuite(com.silverdirk.parser._TestParser.class);
		return suite;
	}
}
