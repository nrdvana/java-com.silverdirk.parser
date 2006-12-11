package com.silverdirk.parser;

import junit.framework.*;
import java.util.*;
import com.silverdirk.parser.TableBuilder$LR1Item;
import com.silverdirk.parser.LR1_Table$ParseAction;

public class _TestTableBuilder extends TestCase {
	private TableBuilder tableBuilder= null;

	public _TestTableBuilder(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		tableBuilder= null;
		super.tearDown();
	}

	public void testSymbolMapping() {
		TableBuilder tb= new TableBuilder(Goal, ruleMix, null);
		// test production maps
		List prods= tb.getProductionRules(Goal);
		assertTrue(prods != null);
		assertEquals(12, prods.size()); // 12 entries for Goal
		for (int i=5; i<=16; i++)
			assertTrue(prods.contains(ruleMix[i]));

		// test existance of all nonterminals
		for (int i=0; i<n.length; i++)
			try {
				tb.getNontermIdx(n[i]);
			}
			catch (Exception ex) {
				assertTrue("finding nonterminal "+i, false);
			}

		// test existance of all terminals
		for (int i=0; i<t.length; i++)
			try {
				tb.getTermIdx(t[i]);
			}
			catch (Exception ex) {
				assertTrue("finding terminal "+i, false);
			}
	}

	public void testExpressionFirstSets() {
		ParseRule[] exprGrammar= new ParseRule[] {
			new ParseRule(Expr, new Object[]{ Expr, PLUS, Term }),
			new ParseRule(Expr, new Object[]{ Expr, MINUS, Term }),
			new ParseRule(Expr, new Object[]{ Term }),
			new ParseRule(Term, new Object[]{ Term, TIMES, Factor }),
			new ParseRule(Term, new Object[]{ Term, DIV, Factor }),
			new ParseRule(Term, new Object[]{ Factor }),
			new ParseRule(Factor, new Object[]{Factor, ":", Factor }),
			new ParseRule(Factor, new Object[]{LPAREN, Expr, RPAREN}),
			new ParseRule(Factor, new Object[]{Number.class}),
			new ParseRule(Factor, new Object[]{String.class})
		};
		TableBuilder tb= new TableBuilder(Goal, exprGrammar, null);
		Set firsts= new HashSet();
		firsts.add(Number.class);
		firsts.add(String.class);
		firsts.add(LPAREN);
		tb.buildFirstSets();
		assertEquals("Expr first set", firsts, tb.getFirstSet(Expr));
		assertEquals("Term first set", firsts, tb.getFirstSet(Term));
		assertEquals("Factor first set", firsts, tb.getFirstSet(Factor));

		// test left-recursive-with-empty-rule
		ParseRule[] lrecSheepGrammar= new ParseRule[] {
			new ParseRule(SheepNoise, new Object[] {SheepNoise, "baa"}),
			new ParseRule(SheepNoise, new Object[] {})
		};
		tb= new TableBuilder(SheepNoise, lrecSheepGrammar, null);
		tb.buildFirstSets();
		firsts.clear();
		firsts.add("baa");
		firsts.add(TableBuilder.EMPTY);
		assertEquals(firsts, tb.getFirstSet(SheepNoise));
	}

	static final Set makeSet(Object[] elems) {
		HashSet result= new HashSet();
		for (int i=0; i<elems.length; i++)
			result.add(elems[i]);
		return result;
	}

	public void testClosure() {
		ParseRule[] grammar= new ParseRule[] {
			new ParseRule(Goal, new Object[]{ LPAREN, Term, Factor, RPAREN }),
			new ParseRule(Term, new Object[]{ Term, PLUS }),
			new ParseRule(Term, new Object[]{ }),
			new ParseRule(Factor, new Object[]{ Factor, TIMES }),
			new ParseRule(Factor, new Object[]{ }),
		};

		TableBuilder tb= new TableBuilder(Goal, grammar, null);
		tb.buildFirstSets();
		tb.buildCanonicalCollection();
		// look for CC[1], and make sure all the possibilities got added
		for (Iterator entries= tb.cc.entrySet().iterator(); entries.hasNext();) {
			Map.Entry e= (Map.Entry) entries.next();
			if (e.getValue().equals(new Integer(1))) {
				Set SS1= (Set) e.getKey();
				LR1Item[] items= new LR1Item[] {
					new LR1Item(grammar[0], 1, Collections.singleton(TokenSource.EOF)),
					new LR1Item(grammar[1], 0, makeSet(new Object[] {PLUS, TIMES, RPAREN})),
					new LR1Item(grammar[2], 0, makeSet(new Object[] {PLUS, TIMES, RPAREN})),
					new LR1Item(grammar[3], 0, makeSet(new Object[] {TIMES, RPAREN})),
					new LR1Item(grammar[4], 0, makeSet(new Object[] {TIMES, RPAREN}))
				};
				HashSet itemSet= new HashSet();
				for (int i=0; i<items.length; i++)
					itemSet.add(items[i]);
				assertEquals(itemSet, SS1);
				break;
			}
		}
	}

	public void testExpressionCCs() {
		ParseRule[] exprGrammar= new ParseRule[] {
			new ParseRule(Expr, new Object[]{ Expr, PLUS, Term }),
			new ParseRule(Expr, new Object[]{ Expr, MINUS, Term }),
			new ParseRule(Expr, new Object[]{ Term }),
			new ParseRule(Term, new Object[]{ Term, TIMES, Factor }),
			new ParseRule(Term, new Object[]{ Term, DIV, Factor }),
			new ParseRule(Term, new Object[]{ Factor }),
			new ParseRule(Factor, new Object[] {LPAREN, Expr, RPAREN}),
			new ParseRule(Factor, new Object[] {Number.class}),
			new ParseRule(Factor, new Object[] {String.class})
		};
		TableBuilder tb= new TableBuilder(Goal, exprGrammar, null);
		tb.buildFirstSets();
		tb.buildCanonicalCollection();



		TableBuilder.Tables result= tb.buildTables();
	}

	public void testSheepNoise() {
		ParseRule[] rules= new ParseRule[] {SN0, SN1, SN2};
		TableBuilder tb= new TableBuilder(Goal, rules, null);
		// test production maps
		assertEquals(tb.productionMap.size(), 2);
		List prods= tb.getProductionRules(Goal);
		assertTrue(prods != null);
		assertEquals(prods.size(), 1);
		assertEquals(prods.get(0), SN0);
		prods= tb.getProductionRules(SheepNoise);
		assertTrue(prods != null);
		assertEquals(prods.size(), 2);
		assertTrue(prods.contains(SN1));
		assertTrue(prods.contains(SN2));

		// test the first sets
		tb.buildFirstSets();
		assertTrue(tb.getFirstSet(Goal).size() == 1);
		assertTrue(tb.getFirstSet(Goal).contains("baa"));
		assertTrue(tb.getFirstSet(SheepNoise).size() == 1);
		assertTrue(tb.getFirstSet(SheepNoise).contains("baa"));

		// test collection building
		tb.buildCanonicalCollection();
		Set eofset= Collections.singleton(TokenSource.EOF);
		LR1Item[][] states= new LR1Item[][] {
			new LR1Item[] {
				new LR1Item(SN0, 0, eofset),
				new LR1Item(SN1, 0, eofset),
				new LR1Item(SN2, 0, eofset),
			},
			new LR1Item[] { new LR1Item(SN0, 1, eofset) },
			new LR1Item[] {
				new LR1Item(SN1, 1, eofset),
				new LR1Item(SN2, 1, eofset),
				new LR1Item(SN1, 0, eofset),
				new LR1Item(SN2, 0, eofset)
			},
			new LR1Item[] { new LR1Item(SN1, 2, eofset) }
		};
/*		assertEquals(4, tb.cc.size());
		HashSet cc[]= new HashSet[4];
		Iterator itr= tb.cc.values().iterator();
		for (int i=0; i<4; i++) {
			TableBuilder.ItemSetEntry entry= (TableBuilder.ItemSetEntry) itr.next();
			cc[entry.idx]= entry.itemSet;
		}

		for (int i=0; i<4; i++) {
			HashSet expectedState= new HashSet(4);
			for (int j=0; j<states[i].length; j++)
				expectedState.add(states[i][j]);
			assertEquals("CC["+i+"]", expectedState, cc[i]);
		}

		// test table construction
		TableBuilder.Tables result= tb.buildTables();
		ParseAction expectedOnEof[]= new ParseAction[] { null, ParseAction.CreateAccept(tb.rules[0]), ParseAction.CreateReduce(tb.rules[2]), ParseAction.CreateReduce(tb.rules[1]) };
		ParseAction expectedOnBaa[]= new Parser.ParseAction[] { ParseAction.CreateShift(2), null, ParseAction.CreateShift(2), null };
		Object expectedOnSheepnoise[]= new Object[] { new Integer(1), null, new Integer(3), null };

		for (int i=0; i<4; i++) {
			assertEquals(expectedOnEof[i], result.actionTable[i].get(TokenSource.EOF));
			assertEquals(expectedOnBaa[i], result.actionTable[i].get("baa"));
			assertEquals(expectedOnSheepnoise[i], result.gotoTable[i].get(SheepNoise));
		}
*/	}

	public void testConflictGrammar() throws Exception {
		ParseRule[] conflictExprGrammar= new ParseRule[] {
			new ParseRule(Expr, new Object[]{ Expr, PLUS, Expr }),
			new ParseRule(Expr, new Object[]{ Expr, MINUS, Expr }),
			new ParseRule(Expr, new Object[]{ Expr, TIMES, Expr }),
			new ParseRule(Expr, new Object[]{ Expr, DIV, Expr }),
			new ParseRule(Expr, new Object[]{ LPAREN, Expr, RPAREN }),
			new ParseRule(Expr, new Object[]{ Number.class }),
			new ParseRule(Expr, new Object[]{ String.class })
		};
		TableBuilder tb= new TableBuilder(Expr, conflictExprGrammar, null);
		tb.buildFirstSets();
		tb.buildCanonicalCollection();
		TableBuilder.Tables result= tb.buildTables();
//		System.out.println("Grammar Conflicts: "+result.conflicts.size());
//		for (Iterator itr= result.conflicts.iterator(); itr.hasNext();)
//			System.out.println(itr.next());
		assertEquals(32, result.conflicts.length);
	}

	public void testFixedConflictGrammar() throws Exception {
		ParseRule sumRule, difRule, prodRule, divRule;
		ParseRule[] conflictExprGrammar= new ParseRule[] {
			sumRule= new ParseRule(Expr, new Object[]{ Expr, PLUS, Expr }),
			difRule= new ParseRule(Expr, new Object[]{ Expr, MINUS, Expr }),
			prodRule= new ParseRule(Expr, new Object[]{ Expr, TIMES, Expr }),
			divRule= new ParseRule(Expr, new Object[]{ Expr, DIV, Expr }),
			new ParseRule(Expr, new Object[]{ LPAREN, Expr, RPAREN }),
			new ParseRule(Expr, new Object[]{ Number.class }),
			new ParseRule(Expr, new Object[]{ String.class })
		};
		Parser.Priorities pri= new Parser.Priorities();
		pri.set(sumRule, 1);
		pri.set(difRule, 1);
		pri.set(prodRule, 2);
		pri.set(divRule, 2);
		TableBuilder tb= new TableBuilder(Expr, conflictExprGrammar, pri);
		tb.buildFirstSets();
		tb.buildCanonicalCollection();
//		for (Iterator itr= tb.cc.values().iterator(); itr.hasNext();) {
//			TableBuilder.ItemSetEntry entry= (TableBuilder.ItemSetEntry) itr.next();
//			System.out.println(entry.idx);
//			System.out.println(entry.itemSet);
//			System.out.println();
//		}
		TableBuilder.Tables result= tb.buildTables();
//		System.out.println("Grammar Conflicts: "+result.conflicts.size());
//		for (Iterator itr= result.conflicts.iterator(); itr.hasNext();)
//			System.out.println(itr.next());
		assertEquals(0, result.conflicts.length);
	}

	static final Nonterminal
		Goal= new Nonterminal("Goal"),
		SheepNoise= new Nonterminal("SheepNoise"),
		Expr= new Nonterminal("Expr"),
		Term= new Nonterminal("Term"),
		Factor= new Nonterminal("Factor");
	static final Character
		PLUS= new Character('+'),
		MINUS= new Character('-'),
		TIMES= new Character('*'),
		DIV= new Character('/'),
		LPAREN= new Character('('),
		RPAREN= new Character(')');

	static final Object[] t= makeTerms();
	static final Nonterminal[] n= makeNonterms();
	static final ParseRule
		SN0= new ParseRule(Goal, new Object[] {SheepNoise}),
		SN1= new ParseRule(SheepNoise, new Object[] {"baa", SheepNoise}),
		SN2= new ParseRule(SheepNoise, new Object[] {"baa"}),
		ruleMix[]= new ParseRule[] {
		new ParseRule(n[31], new Object[] {t[1], t[3], t[5]}),
		new ParseRule(n[26], new Object[] {t[0], t[2], t[4]}),
		new ParseRule(n[17], new Object[] {t[6], t[8], t[10]}),
		new ParseRule(n[28], new Object[] {t[7], t[9], t[11]}),
		new ParseRule(n[25], new Object[] {t[12], t[14], t[16]}),
		new ParseRule(Goal, new Object[] {n[1], n[3], n[5]}),
		new ParseRule(Goal, new Object[] {n[0], n[2], n[4]}),
		new ParseRule(Goal, new Object[] {n[6], n[8], n[10]}),
		new ParseRule(Goal, new Object[] {n[7], n[9], n[11]}),
		new ParseRule(Goal, new Object[] {n[12], n[14], n[14]}),
		new ParseRule(Goal, new Object[] {n[18], n[20], n[22]}),
		new ParseRule(Goal, new Object[] {n[24], n[26], n[28]}),
		new ParseRule(Goal, new Object[] {n[30], n[30], n[30]}),
		new ParseRule(Goal, new Object[] {n[15], n[15], n[17]}),
		new ParseRule(Goal, new Object[] {n[19], n[21], n[23]}),
		new ParseRule(Goal, new Object[] {n[25], n[27], n[29]}),
		new ParseRule(Goal, new Object[] {n[31], n[31], n[31]}),
		new ParseRule(n[13], new Object[] {t[18], t[20], t[22]}),
		new ParseRule(n[23], new Object[] {t[24], t[26], t[28]}),
		new ParseRule(n[31], new Object[] {t[30], t[30], t[30]}),
		new ParseRule(n[16], new Object[] {t[13], t[15], t[17]}),
		new ParseRule(n[ 3], new Object[] {t[19], t[21], t[23]}),
		new ParseRule(n[ 1], new Object[] {t[25], t[27], t[29]}),
		new ParseRule(n[ 7], new Object[] {t[31], t[31], t[31]}),
		};
	static final Character DOT= new Character('.');

	static Object[] makeTerms() {
		Object[] result= new Object[32];
		for (int i=0; i<result.length; i++)
			result[i]= "/"+i+"/";
		return result;
	}
	static Nonterminal[] makeNonterms() {
		Nonterminal[] result= new Nonterminal[32];
		for (int i=0; i<result.length; i++)
			result[i]= new Nonterminal(""+i);
		return result;
	}
}
