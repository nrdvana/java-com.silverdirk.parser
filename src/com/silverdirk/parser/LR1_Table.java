package com.silverdirk.parser;

import java.util.*;

/**
 * <p>Project: UDT Editor</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * @author not attributable
 * @version $Revision$
 */
public class LR1_Table {
	Map[] table;
	ParseRule[] rules;

	public LR1_Table(Grammar g) {
		this(g, (java.io.PrintStream)null);
	}
	public LR1_Table(Grammar g, java.io.PrintStream debug) {
		this.rules= g.rules;
		TableBuilder.Tables t= TableBuilder.generate(g.start, g.startFollowSet, g.rules, g.priorities, debug);
		if (t.conflicts.length > 0) {
			StringBuffer conflictList= new StringBuffer();
			conflictList.append("Grammar has conflicts:\n");
			for (int i=0; i<t.conflicts.length; i++)
				conflictList.append(t.conflicts[i]).append('\n');
			throw new RuntimeException(conflictList.toString());
		}
		assert(t.actionTable.length == t.gotoTable.length);

		table= new Map[t.actionTable.length];
		for (int i=0; i<t.actionTable.length; i++) {
			table[i]= t.actionTable[i];
			table[i].putAll(t.gotoTable[i]);
		}
	}

	public LR1_Table(Grammar g, int[][] tableEntries) {
		this(g.rules, tableEntries);
	}

	public LR1_Table(ParseRule[] rules, int[][] tableEntries) {
		this.rules= rules;
		Object[] intToSymbol= buildSymbolLists(rules);
		table= new Map[tableEntries.length];
		for (int row=0; row<tableEntries.length; row++)
			table[row]= deserializeTableRow(intToSymbol, tableEntries[row]);
	}

	public String toString() {
		StringBuffer sb= new StringBuffer();
		for (int row=0; row<table.length; row++) {
			sb.append(row).append(": ");
			String gotoTable= "";
			for (Iterator i= table[row].entrySet().iterator(); i.hasNext();) {
				Map.Entry ent= (Map.Entry) i.next();
				if (ent.getKey() instanceof Nonterminal)
					gotoTable+= "  "+ent.getKey()+":"+ent.getValue();
				else
					sb.append("  ").append(ent.getKey()).append(":").append(ent.getValue());
			}
			sb.append("  | Goto: ").append(gotoTable).append('\n');
		}
		return sb.toString();
	}

	public int[][] serialize() {
		Object[] intToSymbol= buildSymbolLists(rules);
		Map symbolToInt= buildArrayReverseMapping(intToSymbol);
		int[][] result= new int[table.length][];
		for (int row=0; row<table.length; row++)
			result[row]= serializeTableRow(symbolToInt, table[row]);
		return result;
	}

	public ParseAction getAction(int state, Object token) {
		return (ParseAction) table[state].get(token);
	}

	public ParseAction getErrAction(int state, Object token, Object[] expectedSet, SourcePos pos) {
		return null; // unimplemented
	}

	public int getStateTrans(int state, Nonterminal symbol) {
		return ((Integer)table[state].get(symbol)).intValue();
	}

	public Object[] getOptions(int state) {
		ArrayList result= new ArrayList(table[state].size());
		for (Iterator itr= table[state].keySet().iterator(); itr.hasNext();) {
			Object key= itr.next();
			if (!(key instanceof Nonterminal))
				result.add(key);
		}
		return result.toArray();
	}

	static Object[] buildSymbolLists(ParseRule[] rules) {
		HashSet seen= new HashSet();
		ArrayList list= new ArrayList();
		seen.add(TokenSource.EOF);
		list.add(TokenSource.EOF);
		for (int rule=0; rule<rules.length; rule++) {
			if (seen.add(rules[rule].getNonterminal()))
				list.add(rules[rule].getNonterminal());
			Object[] symbols= rules[rule].getSymbols();
			for (int i=0; i<symbols.length; i++)
				if (seen.add(symbols[i]))
					list.add(symbols[i]);
		}
		return list.toArray();
	}

	static Map buildArrayReverseMapping(Object[] array) {
		HashMap result= new HashMap();
		for (int i=0; i<array.length; i++)
			result.put(array[i], new Integer(i));
		return result;
	}

	static int[] serializeTableRow(Map symbolIdMap, Map rowMap) {
		int[] buffer= new int[rowMap.size() * 3]; // max of 3 ints generated per entry
		int pos= 0;
		for (Iterator i= rowMap.entrySet().iterator(); i.hasNext();) {
			Map.Entry ent= (Map.Entry) i.next();
			buffer[pos++]= ((Integer)symbolIdMap.get(ent.getKey())).intValue();
			if (ent.getKey() instanceof Nonterminal)
				buffer[pos++]= ((Integer)ent.getValue()).intValue();
			else
				pos+= ((ParseAction) ent.getValue()).serializeToBuffer(buffer, pos);
		}
		int[] result= new int[pos];
		System.arraycopy(buffer, 0, result, 0, pos);
		return result;
	}

	static String intArraysToJava(int[][] serData) {
		StringBuffer sb= new StringBuffer(512);
		sb.append("new int[][] {\n");
		for (int row=0; row<serData.length; row++) {
			sb.append("\tnew int[] {");
			for (int col=0; col<serData[row].length; col++)
				sb.append(serData[row][col]).append(',');
			sb.deleteCharAt(sb.length()-1);
			sb.append("},\n");
		}
		return sb.append("};").toString();
	}

	Map deserializeTableRow(Object[] symbolList, int[] codes) {
		HashMap result= new HashMap();
		int pos= 0;
		while (pos < codes.length) {
			Object key= symbolList[codes[pos++]];
			if (key instanceof Nonterminal)
				result.put(key, new Integer(codes[pos++]));
			else {
				ParseAction acn= new ParseAction(0, 0, 0);
				pos+= acn.deserializeFromBuffer(codes, pos);
				result.put(key, acn);
			}
		}
		return result;
	}

	public static class ParseAction {
		int type, rule, nextState;

		ParseAction() {}
		ParseAction(int type, int rule, int nextState) {
			this.type= type;
			this.rule= rule;
			this.nextState= nextState;
		}

		public static ParseAction MkShift(int nextState) {
			return new ParseAction(SHIFT, 0, nextState);
		}

		public static ParseAction MkReduce(int rule) {
			return new ParseAction(REDUCE, rule, 0);
		}

		public static ParseAction MkAccept() {
			return new ParseAction(ACCEPT, 0, 0);
		}
		public static ParseAction MkNonassoc() {
			return new ParseAction(ParseAction.NONASSOC_ERR, 0, 0);
		}

		public String toStringVerbose(ParseRule[] rules, int priority) {
			String base;
			switch (type) {
			case SHIFT: base= "[Shift "+nextState+"]"; break;
			case REDUCE: base= "[Reduce "+rules[rule]+"]"; break;
			case ACCEPT: base= "[Accept]"; break;
			case NONASSOC_ERR: base= "[NonAssoc]"; break;
			default:
				throw new RuntimeException("This can't happen");
			}
			String priString= (priority == Parser.Priorities.DEF_PRI)? "DEFAULT" : Integer.toString(priority);
			return (type==NONASSOC_ERR)? base : base.substring(0, base.length()-1)+", pri="+priString+"]";
		}

		public String toString() {
			switch (type) {
			case SHIFT: return "[Shift "+nextState+"]";
			case REDUCE: return "[Reduce "+rule+"]";
			case ACCEPT: return "[Accept]";
			case NONASSOC_ERR: return "[NonAssoc]";
			default:
				throw new RuntimeException("This can't happen");
			}
		}

		int serializeToBuffer(int[] buffer, int pos) {
			buffer[pos]= type;
			switch (type) {
			case REDUCE: buffer[pos+1]= rule; return 2;
			case SHIFT: buffer[pos+1]= nextState; return 2;
			default: return 1;
			}
		}

		int deserializeFromBuffer(int[] buffer, int pos) {
			type= buffer[pos];
			switch (type) {
			case REDUCE: rule= buffer[pos+1]; return 2;
			case SHIFT: nextState= buffer[pos+1]; return 2;
			default: return 1;
			}
		}

		public static final int
			SHIFT= 0,
			REDUCE= 1,
			ACCEPT= 2,
			NONASSOC_ERR= 3;
		public static final String[] ACTION_NAMES= new String[4];
		static {
			ACTION_NAMES[SHIFT]= "Shift";
			ACTION_NAMES[REDUCE]= "Reduce";
			ACTION_NAMES[ACCEPT]= "Accept";
			ACTION_NAMES[NONASSOC_ERR]= "Nonassociation";
		}
	}
}