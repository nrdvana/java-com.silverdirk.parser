package com.silverdirk.parser;

import java.util.*;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: LR(1) Table</p>
 * <p>Description: Implementation of the Action and Goto tables needed for LR(1) parsing.</p>
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * This particular implementation stores both "tables" in an array of hash
 * tables.  For a given row, the non-empty cells of the action table are stored
 * in the hash table keyed by a terminal symbol, and the non-empty cells of the
 * goto table are stored keyed by a nonterminal symbol.  This is an optimization
 * for sparse tables, though often LR(1) tables are not sparse.  However, with
 * a future optimization (storing a "default action") the tables might indeed
 * usually be sparse. In the meantime, the hastable is necessary because the
 * terminals and nonterminals are not integers, and mapping them to integers
 * and then looking up the integers in the table would take longer than just
 * mapping them directly to actions and gotos.
 *
 * Another important aspect of this class is its ability to serialize to an
 * array of integers.  This allows a table to be re-used, however the list of
 * rules must still be provided to the constructor, and must be the exact same
 * list that was used to geenrate the table originally.
 *
 * There is a static "intArrayToJava" method which can produce text that can be
 * pasted into a java source file.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class LR1_Table {
	Map[] table;
	ParseRule[] rules;

	public LR1_Table(Grammar g) {
		this(g, (java.io.PrintStream)null);
	}

	/** Constructor.
	 * Create a LR(1) Table form the give Grammar, optionally writing debugging
	 * information to a PrintStream.  This will run the LR(1) table construction
	 * algorithm, and may fail and throw an Exception if the grammar contains
	 * conflicts.
	 *
	 * @param g Grammar A Grammar, whose rules will be used to generate a table
	 * @param debug PrintStream Any PrintStream, or null if no debugging information is wanted
	 */
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

	/** Constructor.
	 * Create a LR(1) table for the specified grammar, but instead of
	 * generating the table, load its entries from serialized data.  This data
	 * must have been serialized form a LR(1) table instance using the exact
	 * same grammar and parse rules.
	 *
	 * @param g Grammar A Grammar, which was previously used to construct the serialized table
	 * @param tableEntries int[][] An array of integers holding the serialized table
	 */
	public LR1_Table(Grammar g, int[][] tableEntries) {
		this(g.rules, tableEntries);
	}

	/** Constructor.
	 * Create a LR(1) table for the specified parse rules, but instead of
	 * generating the table, load its entries from serialized data.  This data
	 * must have been serialized form a LR(1) table instance using the exact
	 * same parse rules.
	 *
	 * @param rules ParseRule[] The same list of parse rules that were used to generate the table originally
	 * @param tableEntries int[][] An array of integers holding the serialized table
	 */
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

	/** Serialize the table to arrays of integers.
	 *
	 * @return int[][] An array of integers representing the entries of the table
	 */
	public int[][] serialize() {
		Object[] intToSymbol= buildSymbolLists(rules);
		Map symbolToInt= buildArrayReverseMapping(intToSymbol);
		int[][] result= new int[table.length][];
		for (int row=0; row<table.length; row++)
			result[row]= serializeTableRow(symbolToInt, table[row]);
		return result;
	}

	/** Get an entry from the 'action' table.
	 *
	 * @param state int The parse state, or row of the table.
	 * @param token Object The terminal symbol, or column of the table.
	 * @return ParseAction
	 */
	public ParseAction getAction(int state, Object token) {
		return (ParseAction) table[state].get(token);
	}

	/** Get the action to recover from an error.
	 * <h2>Unimplemented.</h2>
	 */
	public ParseAction getErrAction(int state, Object token, Object[] expectedSet, SourcePos pos) {
		throw new UnsupportedOperationException();
	}

	/** Get an entry from the 'goto' table.
	 *
	 * @param state int The parser state, or row of the table
	 * @param symbol Nonterminal The nonterminal symbol, or column of the table
	 * @return int The parse state to transition to
	 */
	public int getStateTrans(int state, Nonterminal symbol) {
		return ((Integer)table[state].get(symbol)).intValue();
	}

	/** Get a list of what terminal symbols could give valid actions for this parse state.
	 *
	 * @param state int The current state of the parser
	 * @return Object[] The list of terminal symbols that were expected in this state
	 */
	public Object[] getOptions(int state) {
		ArrayList result= new ArrayList(table[state].size());
		for (Iterator itr= table[state].keySet().iterator(); itr.hasNext();) {
			Object key= itr.next();
			if (!(key instanceof Nonterminal))
				result.add(key);
		}
		return result.toArray();
	}

	/** Build a list of all terminals and nonterminals used in the parse rules.
	 * This function is used to form a mapping from integers to symbols so that
	 * the table can be serialized.
	 *
	 * @param rules ParseRule[] The list of parse rules used to generate this table
	 * @return Object[] A deterministicly-ordered array of all the symbols
	 */
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

	/** Buld a map from symbol to index-of-symbol.
	 * This function is used to map a symbol to its index within the list.
	 *
	 * @param array Object[] A deterministicly-ordered list of unique symbols
	 * @return Map A map from each symbol to its index within the list
	 */
	static Map buildArrayReverseMapping(Object[] array) {
		HashMap result= new HashMap();
		for (int i=0; i<array.length; i++)
			result.put(array[i], new Integer(i));
		return result;
	}

	/** Serialize one row of the table, converting the Hashmap to an array of integers.
	 *
	 * @param symbolIdMap Map A mapping from symbol to index
	 * @param rowMap Map The map which makes one row of the parse table
	 * @return int[] An array of integers representing this row of the table
	 */
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

	/** Deserialize an array of integers into a row of the table.
	 *
	 * @param symbolList Object[] The map (implemented by a simple array) from index to symbol
	 * @param codes int[] The array of integers for this row.
	 * @return Map A map representing a row of the table
	 */
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

	/** Produce java source-code for an array of integers.
	 *
	 * @param serData int[][] The array of integers
	 * @return String A string containing Java source code to declare those values
	 */
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

	/**
	 * <p>Title: Parser Action</p>
	 * <p>Description: An object representing one of SHIFT, REDUCE, or ACCEPT</p>
	 * <p>Copyright Copyright (c) 2006-2007</p>
	 *
	 * @author Michael Conrad / TheSilverDirk
	 * @version $Revision$
	 */
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

		/** Display this action as its action-type with associated reduce-rule, and priority.
		 *
		 * @param rules ParseRule[] The list of parse rules
		 * @param priority int The priority of this action
		 */
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

		/** Display a action as its action-type and integer parameters.
		 */
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

		/** Encode this action as a series of integers.
		 *
		 * @param buffer int[] Destination buffer
		 * @param pos int Current position within the buffer
		 * @return int Number of bytes written to the buffer
		 */
		int serializeToBuffer(int[] buffer, int pos) {
			buffer[pos]= type;
			switch (type) {
			case REDUCE: buffer[pos+1]= rule; return 2;
			case SHIFT: buffer[pos+1]= nextState; return 2;
			default: return 1;
			}
		}

		/** Encode the fields for this action from a series of integers.
		 *
		 * @param buffer int[] Source buffer
		 * @param pos int Current position within the buffer
		 * @return int Number of bytes read from the buffer
		 */
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
