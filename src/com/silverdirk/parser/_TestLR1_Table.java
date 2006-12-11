package com.silverdirk.parser;

import junit.framework.*;
import com.silverdirk.parser.Parser$Priorities;
import com.silverdirk.parser.Parser$Priorities$PriorityLevel;

public class _TestLR1_Table extends TestCase {
	static final Nonterminal
		Expr= new Nonterminal("Expression");

	public _TestLR1_Table(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSerialize_ExpressionGrammar() throws Exception {
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
		LR1_Table table= new LR1_Table(g);

		Parser parser= new Parser(g);
		System.out.print(parser.table.toString());
		ArrayTokenSource input= new ArrayTokenSource("", new Object[] {
			new Integer(5), "+", new Integer(3), "*", new Integer(12),
			"+", new Integer(1), "*", "(", new Integer(8), "-", new Integer(3), ")"
		}, null);
		Object result= parser.parse(input);

		int[][] serData= table.serialize();
		LR1_Table table2= new LR1_Table(g.rules, serData);

		Parser parser2= new Parser(g, table2);
		System.out.print(parser.table.toString());
		input.rewind();
		Object result2= parser.parse(input);
		assertEquals(result.toString(), result2.toString());

		System.out.println(LR1_Table.intArraysToJava(serData));
	}
}
