package com.silverdirk.parser;

import junit.framework.*;
import java.util.*;
import com.silverdirk.parser.Parser.Priorities;
import com.silverdirk.parser.Parser.Priorities.PriorityLevel;

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
		Grammar g= new Grammar(SheepNoise,
			new ParseRule[] {
				new ParseRule(SheepNoise, new Object[] {"baa", SheepNoise}, sheepHandlInst),
				new ParseRule(SheepNoise, new Object[] {"baa"}, sheepHandlInst)
			}
		);
		LR1_Table table= new LR1_Table(g, System.out);
		parser= new Parser(g, table);
		System.out.print(parser.table.toString());
		TokenSource input= new ArrayTokenSource("", new Object[] {"baa", "baa", "baa", "baa"}, null);

		Object result= parser.parse(input);
		assertEquals(Vector.class, result.getClass());
		Vector baaList= (Vector) result;
		assertEquals(4, baaList.size());
		for (int i=0; i<baaList.size(); i++)
			assertEquals("baa", baaList.get(i));
	}

	public void testLrecSheepNoise() throws Exception {
		Grammar g= new Grammar(SheepNoise,
			new ParseRule[] {
				new ParseRule(SheepNoise, new Object[] {SheepNoise, "baa"}, sheepHandlInst2),
				new ParseRule(SheepNoise, new Object[] {}, sheepHandlInst2)
			}
		);
		LR1_Table table= new LR1_Table(g, System.out);
		parser= new Parser(g, table);
		System.out.print(parser.table.toString());
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

	public void testExpressionGrammar() throws Exception {
		ParseRule parens, add, sub, mul, div;
		ParseRule[] rules= new ParseRule[] {
			add= new ParseRule(Expr, new Object[] {Expr, "+", Expr}),
			sub= new ParseRule(Expr, new Object[] {Expr, "-", Expr}),
			mul= new ParseRule(Expr, new Object[] {Expr, "*", Expr}),
			div= new ParseRule(Expr, new Object[] {Expr, "/", Expr}),
			parens= new ParseRule(Expr, new Object[] {"(", Expr, ")"}, new ParseRule.PassthroughHandler(1)),
			new ParseRule(Expr, new Object[] {Integer.class}, ParseRule.FIRSTELEM_PASSTHROUGH),
		};
		Priorities pri= new Priorities(new PriorityLevel[] {
			new PriorityLevel(new ParseRule[] {add, sub}, Priorities.LEFT, 1),
			new PriorityLevel(new ParseRule[] {mul, div}, Priorities.LEFT, 2),
			new PriorityLevel(new ParseRule[] {parens}, Priorities.LEFT, 3)
		});
		Grammar g= new Grammar(Expr, rules, pri);
		LR1_Table table= new LR1_Table(g, System.out);
		parser= new Parser(g, table);
		System.out.print(parser.table.toString());
		TokenSource input= new ArrayTokenSource("", new Object[] {
			new Integer(5), "+", new Integer(3), "*", new Integer(12),
			"+", new Integer(1), "*", "(", new Integer(8), "-", new Integer(3), ")"
		}, null);
		Object result= parser.parse(input);
		assertEquals("<Expression>(<Expression>(5, +, <Expression>(3, *, 12)), +, <Expression>(1, *, <Expression>(8, -, 3)))", result.toString());
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
		SheepNoise= new Nonterminal("SheepNoise"),
		Expr= new Nonterminal("Expression");

	static final Character DOT= new Character('.');
}
