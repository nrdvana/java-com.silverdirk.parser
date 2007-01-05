package com.silverdirk.parser;

import junit.framework.*;
import com.silverdirk.datastruct.SetOfChar;

public class _TestRegexParser extends TestCase {
	public _TestRegexParser(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSimpleString() throws Exception {
		NFAGraph g= RegexParser.parse("qwerty");
		char[] expected= "qwerty".toCharArray();
		assertEquals(expected.length, g.nodeCount());
		int node= 0;
		for (int i=0; i<g.nodeCount(); i++) {
			assertEquals(1, g.getExitCount(node));
			SetOfChar chs= g.getTransitionChar(node, 0);
			assertEquals(1, chs.size());
			assertTrue("trans from "+i+" to "+(i+1)+" on "+expected[i], chs.contains(expected[i]));
			node= g.getExitPeer(node, 0);
		}
		assertEquals(g.nodeCount(), node);
	}

	public void testWildcard() throws Exception {
		NFAGraph g= RegexParser.parse("qwerty.");
		char[] expected= "qwerty".toCharArray();
		assertEquals(expected.length+1, g.nodeCount());
		int node= 0;
		for (int i=0; i<expected.length; i++) {
			assertEquals(1, g.getExitCount(node));
			SetOfChar chs= g.getTransitionChar(node, 0);
			assertEquals(1, chs.size());
			assertTrue(chs.contains(expected[i]));
			node= g.getExitPeer(node, 0);
		}
		assertEquals(g.nodeCount()-1, node);
		SetOfChar chs= g.getTransitionChar(node, 0);
		assertEquals(chs, RegexParser.ALL_CHARS);
		node= g.getExitPeer(node, 0);
		assertEquals(g.nodeCount(), node);
	}

	public void testGroups() throws Exception {
		NFAGraph g= RegexParser.parse("qw(e.t)y");
		assertEquals(1, g.getGroupCount());

		g= RegexParser.parse("()()qw(e.t)y");
		assertEquals(3, g.getGroupCount());

		g= RegexParser.parse("()(()(()))qw(e.t)y");
		assertEquals(6, g.getGroupCount());

		g= RegexParser.parse("[()]\\(\\)\\\\(\\\\)");
		assertEquals(1, g.getGroupCount());

		g= RegexParser.parse("[()]\\(\\)\\\\\\(\\\\\\)");
		assertEquals(0, g.getGroupCount());
	}

	public void testEscapeString() throws Exception {
		String rawStr= "[]{};':\",.<>/?\\|=+-_)(*&^%$#@!`~";
		String escapedStr= RegexParser.escapeString(rawStr);
		NFAGraph g= RegexParser.parse(escapedStr);
		assertEquals(rawStr.length(), g.nodeCount());
		int node= 0;
		for (int i=0; i<rawStr.length(); i++) {
			assertEquals(1, g.getExitCount(node));
			SetOfChar trans= g.getTransitionChar(node, 0);
			assertEquals(1, trans.size());
			assertTrue(trans.contains(rawStr.charAt(i)));
			node= g.getExitPeer(node, 0);
		}
		assertEquals(node, rawStr.length());
	}

	public void testCharSets() {
		NFAGraph g1= RegexParser.parse("[0-9a-z]");
		NFAGraph g2= RegexParser.parse("[a-z0-9]");

		g1= RegexParser.parse("[-]");

		g1= RegexParser.parse("[]]");

		g1= RegexParser.parse("[]-]");

		g1= RegexParser.parse("[][foO-]");

		g1= RegexParser.parse("[abcd]");

		g1= RegexParser.parse("[^bcd]");
	}

	public void testCount() throws Exception {
		NFAGraph g= RegexParser.parse("qw(e.t){005120}y");
		assertEquals(5*5120+3, g.nodeCount());

		g= RegexParser.parse("qw(e[.t])?y");

		g= RegexParser.parse("qw.*");

		g= RegexParser.parse("qw[t]+y");

		g= RegexParser.parse("qw.*?");
	}

	public void testFloatRegex() throws Exception {
		NFAGraph g1= RegexParser.parse("([0-9]+(\\.[0-9]*)?|[0-9]*\\.[0-9]+)([Ee][+-]?[0-9]+)?");
	}
}
