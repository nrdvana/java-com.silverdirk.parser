package com.silverdirk.parser;

import java.util.*;

/**
 * <p>Project: 42</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004-2005</p>
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class Parser {
	Nonterminal start;
	HashMap[] actionTable;
	HashMap[] gotoTable;

	public Parser(Nonterminal startSymbol, ParseRule[] rules, Priorities priorities) {
		start= startSymbol;
		buildTable(rules, priorities);
	}

	public Parser(Grammar g) {
		start= g.start;
		buildTable(g.rules, g.priorities);
	}

	public Parser(Nonterminal startSymbol, Collection rules, Priorities priorities) {
		ParseRule[] ruleArray= (ParseRule[]) rules.toArray(new ParseRule[rules.size()]);
		start= startSymbol;
		buildTable(ruleArray, priorities);
	}

	public Nonterminal getStartSymbol() {
		return start;
	}

	private void buildTable(ParseRule[] ruleArray, Priorities priorities) {
		TableBuilder.Tables t= TableBuilder.generate(start, ruleArray, priorities);
		if (t.conflicts.size() > 0) {
			StringBuffer conflictList= new StringBuffer();
			conflictList.append("Grammar has conflicts:\n");
			for (Iterator itr= t.conflicts.iterator(); itr.hasNext();) {
				conflictList.append(itr.next()).append('\n');
			}
			throw new RuntimeException(conflictList.toString());
		}
		actionTable= t.actionTable;
		gotoTable= t.gotoTable;
	}

	public Object parse(TokenSource input) throws ParseException {
		return parse(input, false);
	}

	public Object debugParse(TokenSource input) throws ParseException {
		return parse(input, true);
	}

	public Object parse(TokenSource input, boolean debug) throws ParseException {
		ParseState state;
		int symbol;
		Stack parseStack= new Stack();
		parseStack.push(new ParseState(0, null, null));
		Object nextTok= input.curToken();
		while (true) {
			state= (ParseState) parseStack.peek();
			ParseAction action= (ParseAction) actionTable[state.id].get(nextTok);
			if (action == null)
				action= (ParseAction) actionTable[state.id].get(nextTok.getClass());
			if (action == null)
				throw new ParseException("Unexpected "+nextTok+" encountered", input.getContext(), actionTable[state.id].keySet().toArray(), input.curTokenPos());
			switch (action.type) {
			case ParseAction.SHIFT:
				parseStack.push(new ParseState(action.nextState, nextTok, input.curTokenPos()));
				input.next();
				nextTok= input.curToken();
				break;
			case ParseAction.REDUCE:
			case ParseAction.ACCEPT:
				ParseRule rule= action.rule;
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
					int nextState= ((Integer) gotoTable[state.id].get(rule.target)).intValue();
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
			NONASSOC= 2;
		public static final Integer DEF_PRI= new Integer(0);

		public static class PriorityLevel {
			Object[] items;
			int assoc;
			int level;
			public PriorityLevel(Object[] items, int assoc, int level) {
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

		public Integer getInt(Object symbol) {
			Integer pri= (Integer) priorities.get(symbol);
			if (pri != null) return pri;
			if (!(symbol instanceof Nonterminal))
				pri= (Integer) priorities.get(symbol.getClass());
			return pri;
		}

		public int get(Object symbol) {
			Integer pri= getInt(symbol);
			if (pri == null) return 0;
			else return pri.intValue();
		}

		public void set(Object symbol, int value) {
			if (value < 0)
				throw new RuntimeException("Bad associativity value");
			setPriVal(symbol, value);
		}

		public void set(PriorityLevel lev) {
			set(lev.items, lev.assoc, lev.level);
		}

		public void set(Object[] symbols, int associativity, int value) {
			if (value < 0)
				throw new RuntimeException("Bad associativity value");
			setAssociativity(value, associativity);
			for (int i=0; i<symbols.length; i++)
				setPriVal(symbols[i], value);
		}

		private void setPriVal(Object symbol, int value) {
			if (value != 0)
				priorities.put(symbol, new Integer(value));
			else
				priorities.remove(symbol);
		}

		public int getAssociativity(int priVal) {
			if (priVal < 1)
				throw new RuntimeException("Bad associativity value");
			Integer assocVal= (Integer) associativity.get(new Integer(priVal));
			if (assocVal == null)
				return LEFT;
			else
				return assocVal.intValue();
		}

		public void setAssociativity(int priVal, int association) {
			if (priVal < 1)
				throw new RuntimeException("Bad associativity value");
			if (association < LEFT || association > NONASSOC)
				throw new RuntimeException("Bad associativity value");
			associativity.put(new Integer(priVal), new Integer(association));
		}
	}

	private static final class ParseState {
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

	static final class ParseAction {
		int type;
		ParseRule rule;
		int nextState;

		private ParseAction(int type) {
			this.type= type;
			rule= null;
			nextState= 0;
		}

		public static ParseAction CreateShift(int nextState) {
			ParseAction result= new ParseAction(SHIFT);
			result.nextState= nextState;
			return result;
		}

		public static ParseAction CreateReduce(ParseRule rule) {
			ParseAction result= new ParseAction(REDUCE);
			result.rule= rule;
			return result;
		}

		// XXX make this an inner class so this mess can get fixed up
		public String toString(ParseRule[] ruleset, int priority) {
			String base;
			switch (type) {
			case SHIFT: base= "[Shift "+nextState+"]"; break;
			case REDUCE: base= "[Reduce "+ruleset[rule]+"]"; break;
			case ACCEPT: base= "[Accept "+ruleset[rule]+"]"; break;
			case NONASSOC_ERR: base= "[NonAssoc]"; break;
			default:
				throw new RuntimeException("This can't happen");
			}
			String priString= (priority == Priorities.DEF_PRI)? "DEFAULT" : Integer.toString(priority);
			return (type==NONASSOC_ERR)? base : base.substring(0, base.length()-1)+", pri="+priority+"]";
		}

		public String toString() {
			switch (type) {
			case SHIFT: return "[Shift "+nextState+"]";
			case REDUCE: return "[Reduce "+rule+"]";
			case ACCEPT: return "[Accept "+rule+"]";
			case NONASSOC_ERR: return "[NonAssoc]";
			default:
				throw new RuntimeException("This can't happen");
			}
		}

		public static final int
			SHIFT= 0,
			REDUCE= 1,
			ACCEPT= 2,
			NONASSOC_ERR= 3;
	}

	public interface ProductionHandler {
		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols);
	}
}