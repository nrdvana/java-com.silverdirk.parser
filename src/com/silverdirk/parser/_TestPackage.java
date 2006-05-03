package com.silverdirk.parser;

import junit.framework.*;

public class _TestPackage extends TestCase {

	public _TestPackage(String s) {
		super(s);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(com.silverdirk.parser._TestParser.class);
		suite.addTestSuite(com.silverdirk.parser._TestTableBuilder.class);
		return suite;
	}
}
