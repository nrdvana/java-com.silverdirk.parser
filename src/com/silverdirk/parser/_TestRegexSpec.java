package com.silverdirk.parser;

import junit.framework.*;

public class _TestRegexSpec extends TestCase {
	public _TestRegexSpec(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSimpleString() throws Exception {
		RegexSpec r0= new RegexSpec("qwerty");
		char[] expected= "qwerty".toCharArray();
		Object[] actual= (Object[]) r0.getSpec();
		assertEquals(expected.length, actual.length);
		for (int i=0; i<expected.length; i++)
			assertEquals(new Character(expected[i]), actual[i]);
	}

	public void testWildcard() throws Exception {
		RegexSpec r0= new RegexSpec("qwerty.");
		Object[] actual= (Object[]) r0.getSpec();
		assertEquals(RegexCharSet.ALL, actual[actual.length-1]);
	}

	public void testParens() throws Exception {
		RegexSpec r0= new RegexSpec("qw(e.t)y");
		Object[] actual= (Object[]) r0.getSpec();
		assertEquals(4, actual.length);
		assertEquals(Character.class, actual[0].getClass());
		assertEquals(RegexGroup.class, actual[2].getClass());
		RegexGroup g= (RegexGroup) actual[2];
		assertEquals(RegexCharSet.ALL, ((Object[])g.content)[1]);
	}

	public void testCharSets() {
		RegexSpec r0= new RegexSpec("[0-9a-z]");
		RegexCharSet actual0= (RegexCharSet) r0.getSpec();
		assertEquals(2, actual0.ranges.length);
		assertEquals((((int)'0')<<16) | ((int)'9'), actual0.ranges[0]);
		assertEquals((((int)'a')<<16) | ((int)'z'), actual0.ranges[1]);

		RegexSpec r1= new RegexSpec("[a-z0-9]");
		RegexCharSet actual1= (RegexCharSet) r0.getSpec();
		assertEquals(actual0.hashCode(), actual1.hashCode());
		assertEquals(actual0, actual1);

		assertEquals(2, actual1.ranges.length);
		assertEquals((((int)'0')<<16) | ((int)'9'), actual1.ranges[0]);
		assertEquals((((int)'a')<<16) | ((int)'z'), actual1.ranges[1]);

		actual0.invert();
		assertEquals(3, actual0.ranges.length);
		assertEquals((((int)'\0')<<16) | ((int)'/'), actual0.ranges[0]);
		assertEquals((((int)':')<<16) | ((int)'`'), actual0.ranges[1]);
		assertEquals((((int)'{')<<16) | ((int)'\uFFFF'), actual0.ranges[2]);

		actual1.invert();
		assertEquals(actual0.hashCode(), actual1.hashCode());
		assertEquals(actual0, actual1);

		r0= new RegexSpec("[-]");
		actual0= (RegexCharSet) r0.getSpec();
		assertEquals(1, actual0.ranges.length);
		assertEquals((((int)'-')<<16) | ((int)'-'), actual0.ranges[0]);

		r0= new RegexSpec("[]]");
		actual0= (RegexCharSet) r0.getSpec();
		assertEquals(1, actual0.ranges.length);
		assertEquals((((int)']')<<16) | ((int)']'), actual0.ranges[0]);

		r0= new RegexSpec("[]-]");
		actual0= (RegexCharSet) r0.getSpec();
		assertEquals(2, actual0.ranges.length);
		assertEquals((((int)'-')<<16) | ((int)'-'), actual0.ranges[0]);
		assertEquals((((int)']')<<16) | ((int)']'), actual0.ranges[1]);

		r0= new RegexSpec("[][foO-]");
		actual0= (RegexCharSet) r0.getSpec();
		assertEquals(6, actual0.ranges.length);
		assertEquals((((int)'-')<<16) | ((int)'-'), actual0.ranges[0]);
		assertEquals((((int)'O')<<16) | ((int)'O'), actual0.ranges[1]);
		assertEquals((((int)'[')<<16) | ((int)'['), actual0.ranges[2]);
		assertEquals((((int)']')<<16) | ((int)']'), actual0.ranges[3]);
		assertEquals((((int)'f')<<16) | ((int)'f'), actual0.ranges[4]);
		assertEquals((((int)'o')<<16) | ((int)'o'), actual0.ranges[5]);

		r0= new RegexSpec("[abcd]");
		actual0= (RegexCharSet) r0.getSpec();
		assertEquals(1, actual0.ranges.length);
		assertEquals((((int)'a')<<16) | ((int)'d'), actual0.ranges[0]);

		r0= new RegexSpec("[^bcd]");
		actual0= (RegexCharSet) r0.getSpec();
		assertEquals(2, actual0.ranges.length);
		assertEquals((((int)'\0')<<16) | ((int)'a'), actual0.ranges[0]);
		assertEquals((((int)'e')<<16) | ((int)'\uFFFF'), actual0.ranges[1]);
	}

	public void testCount() throws Exception {
		RegexSpec r0= new RegexSpec("qw(e.t){005120}y");
		Object[] actual= (Object[]) r0.getSpec();
		assertEquals(4, actual.length);
		assertEquals(RegexRepetition.class, actual[2].getClass());
		RegexRepetition rep= (RegexRepetition) actual[2];
		assertEquals(5120, rep.count);
		assertEquals(RegexGroup.class, rep.content.getClass());
		RegexGroup g= (RegexGroup) rep.content;
		assertEquals(RegexCharSet.ALL, ((Object[])g.content)[1]);

		r0= new RegexSpec("qw(e[.t])?y");
		actual= (Object[]) r0.getSpec();
		assertEquals(4, actual.length);
		assertEquals(RegexRepetition.class, actual[2].getClass());
		rep= (RegexRepetition) actual[2];
		assertEquals(RegexRepetition.ZERO_OR_ONE, rep.count);
		assertEquals(RegexGroup.class, rep.content.getClass());

		r0= new RegexSpec("qw.*");
		actual= (Object[]) r0.getSpec();
		assertEquals(RegexRepetition.class, actual[2].getClass());
		rep= (RegexRepetition) actual[2];
		assertEquals(RegexRepetition.ZERO_OR_GREATER, rep.count);
		assertEquals(RegexCharSet.ALL, rep.content);

		r0= new RegexSpec("qw[t]+y");
		actual= (Object[]) r0.getSpec();
		assertEquals(4, actual.length);
		assertEquals(RegexRepetition.class, actual[2].getClass());
		rep= (RegexRepetition) actual[2];
		assertEquals(RegexRepetition.ONE_OR_GREATER, rep.count);
		assertEquals(RegexCharSet.class, rep.content.getClass());

		r0= new RegexSpec("qw.*?");
		actual= (Object[]) r0.getSpec();
		assertEquals(RegexRepetition.class, actual[2].getClass());
		rep= (RegexRepetition) actual[2];
		assertEquals(RegexRepetition.ZERO_OR_GREATER, rep.count);
		assertEquals(false, rep.greedy);
		assertEquals(RegexCharSet.ALL, rep.content);
	}
}
