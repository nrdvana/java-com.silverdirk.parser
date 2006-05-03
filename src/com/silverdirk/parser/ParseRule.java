package com.silverdirk.parser;

import com.silverdirk.parser.Parser$ProductionHandler;
import java.util.*;

/**
 * <p>Project: 42</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004-2005</p>
 *
 * This is an immutable class representing a production rule.
 * It is immutable, to save the effort of making a mechanism to invalidate
 * the parse tables, and also because I don't see a reason to alter a
 * production rule instead of creating a new one.
 *
 * @author not attributable
 * @version $Revision$
 */
public class ParseRule {
	Nonterminal target;
	Object[] symbols;
	ProductionHandler handler;

	/**
	 * Create a parser production specifying that the target can be created
	 * from an instance of the specified symbols, using the function provided
	 * by the handler.
	 *
	 * @param target The nonterminal symbol that is produced by this rule
	 * @param symbols The specific values (or classes of values) which must be matched
	 * @param handler The function whick converts the symbol values into an object representing the nonterminal
	 */
	public ParseRule(Nonterminal target, Object[] symbols, ProductionHandler handler) {
		this.target= target;
		this.symbols= (Object[]) symbols.clone();
		this.handler= handler;
	}

	/**
	 * Create a parser production rule with the specified target and symbols,
	 * using a default handler that generates GenericParseNode objects.
	 *
	 * @param target Same as main constructor
	 * @param symbols Same as main constructor
	 */
	public ParseRule(Nonterminal target, Object[] symbols) {
		this(target, symbols, ParseRule.GenericHandler);
	}

	/** Get the rule target,
	 * This function returns the nonterminal that this rule produces.
	 * @return the nonterminal symbol product of the rule
	 */
	public Nonterminal getNonterminal() {
		return target;
	}

	/** Get the rule symbol list.
	 * This function creates a copy of the list of symbols that make up the body of the rule
	 * @return a copy of the symbol list
	 */
	public Object[] getSymbols() {
		return (Object[]) symbols.clone();
	}

	/** Get the production handler.
	 * This function returns that function used to implement reductions based on this rule
	 * @return a reference to the handler object
	 */
	public ProductionHandler getHandler() {
		return handler;
	}

	public String toString() {
		StringBuffer result= new StringBuffer();
		result.append(target.name).append(" ::=");
		for (int i=0; i<symbols.length; i++)
			if (symbols[i] instanceof Nonterminal)
				result.append(" ").append(symbols[i]);
			else
				result.append(" '").append(symbols[i]).append("'");
		return result.toString();
	}

	public static final ProductionHandler GenericHandler= new ProductionHandler() {
		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
			return new GenericParseNode(rule.target, from, symbols);
		}
	};

	public static class PassthroughHandler implements ProductionHandler {
		int fieldIdx;
		public static final int ALL= -1;

		public PassthroughHandler(int fieldIdx) {
			this.fieldIdx= fieldIdx;
		}
		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
			if (fieldIdx < 0)
				return symbols;
			else if (symbols.length > fieldIdx)
				return symbols[fieldIdx];
			else
				return null;
		}
	}

	public static class ElemChooseHandler implements ProductionHandler {
		int[] elemIdxs;
		public static final int EMPTY= -1;
		public static final int REMAINDER= -2;

		public ElemChooseHandler(int a) {
			this.elemIdxs= new int[] { a };
		}
		public ElemChooseHandler(int a, int b) {
			this.elemIdxs= new int[] { a, b };
		}
		public ElemChooseHandler(int a, int b, int c) {
			this.elemIdxs= new int[] { a, b, c };
		}
		public ElemChooseHandler(int a, int b, int c, int d) {
			this.elemIdxs= new int[] { a, b, c, d };
		}
		public ElemChooseHandler(int[] elems) {
			this.elemIdxs= elems;
		}
		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
			ArrayList result= new ArrayList(Math.max(elemIdxs.length, symbols.length));
			for (int i=0; i<elemIdxs.length; i++) {
				if (elemIdxs[i] == EMPTY)
					result.add(null);
				else if (elemIdxs[i] == REMAINDER) {
					int fromIdx= (i == 0)? 0 : elemIdxs[i-1];
					for (int j=fromIdx; j<symbols.length; j++)
						result.add(symbols[j]);
				}
				else {
					if (elemIdxs[i] > symbols.length)
						result.add(null);
					else
						result.add(symbols[elemIdxs[i]]);
				}
			}
			return new GenericParseNode(rule.getNonterminal(), from, result.toArray());
		}
	}

	public static class ListBuildHandler implements ProductionHandler {
		int listIdx, itemIdx;

		public ListBuildHandler(int listIdx, int itemIdx) {
			this.listIdx= listIdx;
			this.itemIdx= itemIdx;
		}
		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
			Object item= null;
			List list= null;
			if (symbols.length > listIdx) {
				if (symbols[listIdx] instanceof List)
					list= (List) symbols[listIdx];
				else {
					list= new LinkedList();
					list.add(symbols[listIdx]);
				}
			}
			if (symbols.length > itemIdx)
				item= symbols[itemIdx];
			if (list == null)
				list= new LinkedList();
			if (item != null) {
				if (itemIdx > listIdx) list.add(item);
				else list.add(0, item);
			}
			return list;
		}
	}

	public static final ProductionHandler
		FIRSTELEM_PASSTHROUGH= new PassthroughHandler(0),
		ALL_PASSTHROUGH= new PassthroughHandler(-1);
}