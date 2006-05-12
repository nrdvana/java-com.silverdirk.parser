package com.silverdirk.parser;

import junit.framework.*;
import java.util.*;
import com.silverdirk.parser.Parser$Priorities;
import com.silverdirk.parser.Parser$Priorities$PriorityLevel;

public class _TestParser extends TestCase {
	private Parser parser = null;

	protected void setUp() throws Exception {
		super.setUp();
		parser= null;
	}

	protected void tearDown() throws Exception {
		parser= null;
		super.tearDown();
	}

	public void testSimple() throws Exception {
		parser= new Parser(new Grammar(Goal, new ParseRule[] {new ParseRule(Goal, new Object[] {DOT})}));
		TokenSource input= new ArrayTokenSource("", new Object[] {DOT}, null);
		Object result= parser.parse(input);
		assertEquals(GenericParseNode.class, result.getClass());
		GenericParseNode root= (GenericParseNode) result;
		assertEquals(Goal, root.type);
		assertEquals(1, root.components.length);
		assertEquals(DOT, root.components[0]);
	}

	public void testSheepNoise() throws Exception {
		parser= new Parser(new Grammar(Goal,
			new ParseRule[] {
				new ParseRule(Goal, new Object[] {SheepNoise}),
				new ParseRule(SheepNoise, new Object[] {"baa", SheepNoise}, sheepHandlInst),
				new ParseRule(SheepNoise, new Object[] {"baa"}, sheepHandlInst)
			}
		));

		TokenSource input= new ArrayTokenSource("", new Object[] {"baa", "baa", "baa", "baa"}, null);

		Object result= parser.parse(input);
		assertEquals(GenericParseNode.class, result.getClass());
		result= ((GenericParseNode)result).components[0];
		assertEquals(Vector.class, result.getClass());
		Vector baaList= (Vector) result;
		assertEquals(4, baaList.size());
		for (int i=0; i<baaList.size(); i++)
			assertEquals("baa", baaList.get(i));
	}

	public void testLrecSheepNoise() throws Exception {
		parser= new Parser(new Grammar(
			SheepNoise,
			new ParseRule[] {
				new ParseRule(SheepNoise, new Object[] {SheepNoise, "baa"}, sheepHandlInst2),
				new ParseRule(SheepNoise, new Object[] {}, sheepHandlInst2)
			}
		));

		TokenSource input= new ArrayTokenSource("", new Object[] {"baa", "baa", "baa", "baa"}, null);

		Object result= parser.parse(input);
		assertEquals(Vector.class, result.getClass());
		Vector baaList= (Vector) result;
		assertEquals(4, baaList.size());
		for (int i=0; i<baaList.size(); i++)
			assertEquals("baa", baaList.get(i));

		// test an empty list
		input= new ArrayTokenSource("", new Object[] {}, null);
		result= parser.parse(input);
		assertEquals(Vector.class, result.getClass());
		baaList= (Vector) result;
		assertEquals(0, baaList.size());
	}

	public void testNullableFirstNonterm() throws Exception {
		parser= new Parser(new Grammar(
			Goal,
			new ParseRule[] {
				new ParseRule(Goal, new Object[] { MaybeNothing, SheepNoise }),
				new ParseRule(SheepNoise, new Object[] {SheepNoise, "baa"}, new ParseRule.ListBuildHandler(0, 1)),
				new ParseRule(SheepNoise, new Object[] {"baa"}, ParseRule.FIRSTELEM_PASSTHROUGH),
				new ParseRule(MaybeNothing, new Object[] {"something"}, ParseRule.FIRSTELEM_PASSTHROUGH),
				new ParseRule(MaybeNothing, new Object[] {}, ParseRule.FIRSTELEM_PASSTHROUGH),
			}
		));

		TokenSource
			input1= new ArrayTokenSource("", new Object[] {"something", "baa", "baa", "baa", "baa"}, null),
			input2= new ArrayTokenSource("", new Object[] {"baa", "baa", "baa", "baa"}, null);

		GenericParseNode result= (GenericParseNode) parser.parse(input1);
		assertEquals(2, result.components.length);
		assertEquals("something", result.components[0]);
		assertEquals(4, ((List)result.components[1]).size());

		result= (GenericParseNode) parser.parse(input2);
		assertEquals(2, result.components.length);
		assertEquals(null, result.components[0]);
		assertEquals(4, ((List)result.components[1]).size());
	}

	static final class SheepHandler implements Parser.ProductionHandler {
		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
			Vector result;
			if (symbols.length > 1)
				result= (Vector) symbols[1];
			else
				result= new Vector();
			result.add(symbols[0]);
			return result;
		}
	}
	static final class LRecSheepHandler implements Parser.ProductionHandler {
		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
			Vector result;
			if (symbols.length > 1) {
				result= (Vector) symbols[0];
				result.add(symbols[1]);
			}
			else
				result= new Vector();
			return result;
		}
	}
	static final SheepHandler sheepHandlInst= new SheepHandler();
	static final LRecSheepHandler sheepHandlInst2= new LRecSheepHandler();

	static final Nonterminal
		Goal= new Nonterminal("Goal"),
		MaybeNothing= new Nonterminal("MaybeNothing"),
		SheepNoise= new Nonterminal("SheepNoise");

	static final Character DOT= new Character('.');
}
