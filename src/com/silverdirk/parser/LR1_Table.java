package com.silverdirk.parser;

import java.util.*;
import com.silverdirk.parser.Parser$ParseAction;

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
		this.rules= g.rules;
		TableBuilder.Tables t= TableBuilder.generate(g.start, g.rules, g.priorities);
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
		for (int row=0; row<tableEntries.length; row++)
			table[row]= deserializeTableRow(intToSymbol, tableEntries[row]);
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
		int[] buffer= new int[rowMap.size() * 4]; // max of 4 ints generated per entry
		int pos= 0;
		for (Iterator i= rowMap.entrySet().iterator(); i.hasNext();) {
			Map.Entry ent= (Map.Entry) i.next();
			buffer[pos++]= ((Integer)symbolIdMap.get(ent.getKey())).intValue();
			if (ent.getKey() instanceof Nonterminal) {
				ParseAction acn= (ParseAction) ent.getValue();
				buffer[pos++]= acn.type;
				buffer[pos++]= acn.rule;
				buffer[pos++]= acn.nextState;
			}
			else
				buffer[pos++]= ((Integer)ent.getValue()).intValue();
		}
		int[] result= new int[pos];
		System.arraycopy(buffer, 0, result, 0, pos);
		return result;
	}

	static Map deserializeTableRow(Object[] symbolList, int[] codes) {
		HashMap result= new HashMap();
		for (int i=0; i<codes.length; i++) {
			Object key= symbolList[codes[i]];
			if (key instanceof Nonterminal) {
				ParseAction acn= new ParseAction(0, 0, 0); //a bit dangerous to deserialize when the constructor params are all the same type
				acn.type= codes[++i];
				acn.rule= codes[++i];
				acn.nextState= codes[++i];
				result.put(key, acn);
			}
			else
				result.put(key, new Integer(codes[i++]));
		}
		return result;
	}
}