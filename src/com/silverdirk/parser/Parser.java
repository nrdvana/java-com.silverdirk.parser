package com.silverdirk.parser;

import java.util.*;
import com.silverdirk.parser.LR1_Table$ParseAction;

/**
 * <p>Project: com.silverdirk.parser</p>
 * <p>Title: Parser</p>
 * <p>Description: Parsing engine driven by LR(1) tables</p>
 * <p>Copyright: Copyright (c) 2005-2006</p>
 *
 * This class is really just one function, 'parse', bound to a set of rules and
 * an LR(1) table.  For further explanation, find a good book or website about
 * parsing, LL, LR, LR(1), or LALR.  This algorithm was implemented from the
 * description and pseudocode in
 * "Engineering a Compiler", by Cooper and Torczon. [Morgan Kaufmann 2004]
 *
 * Some ideas may also have been borrowed from the Java CUP project, which I
 * used in Compiler Theory class, and whose source code I investigated.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class Parser {
	ParseRule[] rules;
	LR1_Table table;

	public Parser(Grammar g) {
		this(g, new LR1_Table(g));
	}

	public Parser(Grammar g, LR1_Table precompiledTable) {
		rules= g.rules;
		table= precompiledTable;
	}

	/** Parse (non-debug).
	 * This simply calls 'parse' with debug mode off.
	 *
	 * @param input A source of tokens
	 * @return An object describing the entire tree
	 * @throws ParseException whenever a token is encountered which cannot match a current rule, and if there is no error action registered
	 */
	public Object parse(TokenSource input) throws ParseException {
		return parse(input, false);
	}

	/** Parse (debug).
	 * This simply calls 'parse' with debug mode on.
	 *
	 * @param input A source of tokens
	 * @return An object describing the entire tree
	 * @throws ParseException whenever a token is encountered which cannot match a current rule, and if there is no error action registered
	 */
	public GenericParseNode debugParse(TokenSource input) throws ParseException {
		return (GenericParseNode) parse(input, true);
	}

	/** Parse
	 * This parses a token stream using the tables given to the constructor, and
	 * returns the root of the parse tree.
	 *
	 * It has an optional debugging mode, where GenericparseNodes are returned
	 * instead of running the user's code during a rule-reduce.
	 *
	 * @param input A source of tokens
	 * @param debug Whether or not to use debugging behavior.
	 * @return An object describing the entire tree
	 * @throws ParseException whenever a token is encountered which cannot match a current rule, and if there is no error action registered
	 */
	public Object parse(TokenSource input, boolean debug) throws ParseException {
		ParseState state;
		int symbol;
		Stack parseStack= new Stack();
		parseStack.push(new ParseState(0, null, null));
		Object nextTok= input.curToken();
		while (true) {
			state= (ParseState) parseStack.peek();
			ParseAction action= table.getAction(state.id, nextTok);
			if (action == null) {
				Class typ= nextTok.getClass();
				do {
					action= table.getAction(state.id, typ);
					typ= typ.getSuperclass();
				} while (action == null && typ != Object.class);
				if (action == null) {
					Object[] expectedSet= table.getOptions(state.id);
					action= table.getErrAction(state.id, nextTok, expectedSet, input.curTokenPos());
					if (action == null)
						throw new ParseException("Unexpected "+nextTok+" encountered",
							input.getContext(),
							(ParseState[]) parseStack.toArray(new ParseState[parseStack.size()]),
							expectedSet, input.curTokenPos());
				}
			}
			switch (action.type) {
			case ParseAction.SHIFT:
				parseStack.push(new ParseState(action.nextState, nextTok, input.curTokenPos()));
				input.next();
				nextTok= input.curToken();
				break;
			case ParseAction.REDUCE:
			case ParseAction.ACCEPT:
				ParseRule rule= rules[action.rule];
				SourcePos pos= new SourcePos();
				Object[] symbols= new Object[rule.symbols.length];
				if (symbols.length > 0) {
					ParseState st= (ParseState) parseStack.peek();
					pos.lineEnd= st.pos.lineEnd;
					pos.charEnd= st.pos.charEnd;
					for (int i= symbols.length-1; i>0; i--)
						symbols[i]= ((ParseState) parseStack.pop()).data;
					st= (ParseState) parseStack.pop();
					symbols[0]= st.data;
					pos.lineStart= st.pos.lineStart;
					pos.charStart= st.pos.charStart;
				}
				Object data= debug? new GenericParseNode(rule.getNonterminal(), pos, symbols)
					: rule.getHandler().reduce(rule, pos, symbols);
				if (action.type == ParseAction.ACCEPT && parseStack.size() == 1)
					return data;
				else {
					state= (ParseState) parseStack.peek();
					int nextState= table.getStateTrans(state.id, rule.target);
					parseStack.push(new ParseState(nextState, data, pos));
				}
				break;
			case ParseAction.NONASSOC_ERR:
				throw new ParseException("Cannot use multiple "+nextTok+" without grouping them. (nonassociative operator)", input.getContext(), input.curTokenPos());
			default:
				throw new RuntimeException("Undefined action code");
			}
		}
	}

	public static class Priorities {
		HashMap priorities= new HashMap();
		HashMap associativity= new HashMap();
		public static final int
			LEFT= 0,
			RIGHT= 1,
			NONASSOC= 2,
			DEF_PRI= -1;
		static final Integer
			DEF_PRI_OBJ= new Integer(DEF_PRI);

		public static class PriorityLevel {
			ParseRule[] items;
			int assoc;
			int level;
			public PriorityLevel(ParseRule[] items, int assoc, int level) {
				this.items= items;
				this.assoc= assoc;
				this.level= level;
			}
		}

		public Priorities() {
		}

		public Priorities(PriorityLevel[] levels) {
			for (int i=0; i<levels.length; i++)
				set(levels[i]);
		}

		public void set(PriorityLevel lev) {
			set(lev.items, lev.assoc, lev.level);
		}

		public void set(ParseRule[] rules, int associativity, int value) {
			setAssociativity(value, associativity);
			for (int i=0; i<rules.length; i++)
				set(rules[i], value);
		}

		public int get(ParseRule rule) {
			Integer pri= (Integer) priorities.get(rule);
			return (pri == null)? DEF_PRI : pri.intValue();
		}

		public void set(ParseRule rules, int value) {
			checkPriVal(value, true);
			if (value != DEF_PRI)
				priorities.put(rules, new Integer(value));
			else
				priorities.remove(rules);
		}

		public int getAssociativity(int priVal) {
			checkPriVal(priVal, false);
			Integer assocVal= (Integer) associativity.get(new Integer(priVal));
			return (assocVal == null)? LEFT : assocVal.intValue();
		}

		public void setAssociativity(int priVal, int association) {
			checkPriVal(priVal, false);
			checkAssocVal(association);
			if (association != LEFT)
				associativity.put(new Integer(priVal), new Integer(association));
			else
				associativity.remove(new Integer(priVal));
		}

		void checkPriVal(int value, boolean allowDefault) {
			if ((allowDefault?value:value-1) < DEF_PRI)
				throw new RuntimeException("Bad priority");
		}
		void checkAssocVal(int value) {
			if (value < LEFT || value > NONASSOC)
				throw new RuntimeException("Bad associativity value");
		}
	}

	static final class ParseState {
		int id;
		Object data;
		SourcePos pos;

		ParseState() {}

		ParseState(int id, Object data, SourcePos pos) {
			this.id= id;
			this.data= data;
			this.pos= pos;
		}
	}

	public interface ProductionHandler {
		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols);
	}
}