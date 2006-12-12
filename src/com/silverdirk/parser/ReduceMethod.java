package com.silverdirk.parser;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Reduce Method</p>
 * <p>Description: Simulates a function pointer of type "reduce"</p>
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Instances of this class can be used to prevent having to subclass
 * multiple parse rules with the same code.
 *
 * <p>Within this interface are several useful implementations for building lists
 * and returning single elements form the parameter list.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public interface ReduceMethod {
	public Object reduce(ParseRule rule, SourcePos from, Object[] symbols);

	public static class PassthroughHandler implements ReduceMethod {
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

	public static class ElemChooseHandler implements ReduceMethod {
		int[] elemIdxs;
		public static final int EMPTY= -1;
		public static final int REMAINDER= -2;

		public ElemChooseHandler(int a) {
			this.elemIdxs= new int[] {a};
		}

		public ElemChooseHandler(int a, int b) {
			this.elemIdxs= new int[] {a, b};
		}

		public ElemChooseHandler(int a, int b, int c) {
			this.elemIdxs= new int[] {a, b, c};
		}

		public ElemChooseHandler(int a, int b, int c, int d) {
			this.elemIdxs= new int[] {a, b, c, d};
		}

		public ElemChooseHandler(int[] elems) {
			this.elemIdxs= elems;
		}

		public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
			ArrayList result= new ArrayList(Math.max(elemIdxs.length, symbols.length));
			for (int i= 0; i < elemIdxs.length; i++) {
				if (elemIdxs[i] == EMPTY)
					result.add(null);
				else if (elemIdxs[i] == REMAINDER) {
					int fromIdx= (i == 0) ? 0 : elemIdxs[i - 1];
					for (int j= fromIdx; j < symbols.length; j++)
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

	public static class ListBuildHandler implements ReduceMethod {
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

	public static final ReduceMethod
		FIRSTELEM_PASSTHROUGH= new PassthroughHandler(0),
		ALL_PASSTHROUGH= new PassthroughHandler( -1);
}
