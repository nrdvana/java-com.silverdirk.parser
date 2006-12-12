package com.silverdirk.parser;

import java.util.*;
import com.silverdirk.parser.Parser.Priorities;
import com.silverdirk.parser.LR1_Table.ParseAction;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: LR(1) Table Builder</p>
 * <p>Description: Builds the components of a LR(1) table from the specified components of a grammar</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * Most of this class is an implementation of the table generation algorithms,
 * pseudocode, and general discussion of LR(1) variations and optimizations in
 *   Engineering a Compiler
 *   Keith D. Cooper & Linda Torczon
 *   Morgan Kaufmann Publishers, 2004
 *   ISBN: 1-55860-698-X
 *
 * Several parts have been 'tweaked' to get features I desired from a parser,
 * though they may have been invented by others prior to this.
 * (it is dangerous to 'innovate' in a heavily researched field ;-)
 * If I am missing credits, simply inform me and I will apply it where due.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class TableBuilder {
	Nonterminal start;
	ParseRule[] rules;
	Priorities priorities;
	HashMap productionMap= new HashMap();
	HashMap terminals= new HashMap();
	HashMap nonterminals= new HashMap();
	HashMap firstSets= new HashMap();
	HashMap cc= new HashMap(); // cc == Canonical Collection, as seen in the text
	Set rootFollowSet;
	List transitions= new ArrayList();
	List reductions= new ArrayList();
	java.io.PrintStream debugOutput;
	int acceptState= -1;
	private static Nonterminal GOAL= new Nonterminal("#Goal#");
	static Set EOF_SET= Collections.singleton(TokenSource.EOF);

	public static final class Tables {
		public Map[] actionTable;
		public Map[] gotoTable;
		public String[] conflicts;
	}

	public static final Tables generate(Nonterminal startSymbol, ParseRule[] rules, Priorities priorities) {
		return generate(startSymbol, EOF_SET, rules, priorities, null);
	}
	public static final Tables generate(Nonterminal startSymbol, Set startSymFollowSet, ParseRule[] rules, Priorities priorities, java.io.PrintStream debug) {
		TableBuilder tb= new TableBuilder(startSymbol, startSymFollowSet, rules, priorities);
		tb.debugOutput= debug;
		tb.buildFirstSets();
		tb.buildCanonicalCollection();
		return tb.buildTables();
	}

	TableBuilder(Nonterminal startSymbol, Set startSymFollowSet, ParseRule[] rules, Priorities priorities) {
		start= startSymbol;
		rootFollowSet= startSymFollowSet;
		this.rules= rules;
		this.priorities= priorities != null? priorities : new Priorities();
		catalogSymbol(start);
		for (int i=0; i<rules.length; i++)
			processRule(rules[i]);
	}

	/** Process the rule, recording all its symbols and adding it to a reverse
	 * map of target-symbol => production
	 */
	private void processRule(ParseRule r) {
		catalogSymbol(r.target);
		for (int i=0; i<r.symbols.length; i++)
			catalogSymbol(r.symbols[i]);
		LinkedList prods= (LinkedList) productionMap.get(r.target);
		prods.add(r);
	}

	/** Record the existance of this terminal or nonterminal for the table building algorithm.
	 * This method is called for every symbol referenced in any parse rule,
	 * building a complete set of terminals and nonterminals.
	 * @param sym A symbol, either a nonterminal or terminal.
	 */
	private void catalogSymbol(Object sym) {
		if (sym instanceof Nonterminal) {
			if (!nonterminals.containsKey(sym)) {
				nonterminals.put(sym, new Integer(nonterminals.size()));
				productionMap.put(sym, new LinkedList());
			}
		}
		else
			if (!terminals.containsKey(sym))
				terminals.put(sym, new Integer(terminals.size()));
	}

	int getItemSetIdx(HashSet itemSet) {
		return ((ItemSetEntry) cc.get(itemSet)).idx;
	}

	int getNontermIdx(Nonterminal nonterm) {
		return ((Integer)nonterminals.get(nonterm)).intValue();
	}

	int getTermIdx(Object terminal) {
		return ((Integer)terminals.get(terminal)).intValue();
	}

	List getProductionRules(Nonterminal nt) {
		return (List) productionMap.get(nt);
	}

	/** Get the 'firstSet' for a terminal or nonterminal.
	 * The 'firstSet' of a nonterminal was calculated by 'buildFirstSets'.
	 * The 'firstSet' of a terminal is the terminal itself.
	 *
	 * @param symbol The nonterminal or terminal symbol in question
	 * @return A set of terminal symbols that can begin an instance of this symbol
	 */
	Set getFirstSet(Object symbol) {
		if (symbol instanceof Nonterminal)
			return (HashSet) firstSets.get(symbol);
		else
			return Collections.singleton(symbol);
	}

	/** Calculate the set of terminals that could be seen before each nonterminal.
	 * Uses the algorithm described in Cooper & Torczon to find the FirstSet
	 * for each nonterminal.  Repeatedly iterates through the parse rules
	 * increasing the 'firstSet' of each possible leading nonterminal by any
	 * elements in the 'firstSet' of the parse rule's target.
	 *
	 * Algorithm completes when no sets are altered for an entire iteration of
	 * the list of parse rules.
	 *
	 * The sets are maintained within fields of this class, and only used for
	 * the table building algorithm.
	 */
	void buildFirstSets() {
		firstSets.clear();
		for (Iterator itr= nonterminals.keySet().iterator(); itr.hasNext();)
			firstSets.put(itr.next(), new HashSet());
		boolean changed= true;
		while (changed) {
			changed= false;
			for (int i=0; i<rules.length; i++) {
				Set firstSet= getFirstSet(rules[i].target);
				Object[] symbols= rules[i].symbols;
				boolean containedEmpty= true;
				Set symbolFirstSet;
				if (symbols == null) symbols= new Object[0];
				for (int b= 0; containedEmpty && b<symbols.length; b++) {
					symbolFirstSet= getFirstSet(symbols[b]);
					containedEmpty= symbolFirstSet.remove(EMPTY);
					changed= changed || firstSet.addAll(symbolFirstSet);
					if (containedEmpty) symbolFirstSet.add(EMPTY);
				}
				// if we left the loop because we ran out of elements, then it means all of the elements could be empty-string
				// so add EMPTY as a possible first for our production rule's target
				if (containedEmpty)
					changed= changed || firstSet.add(EMPTY);
			}
		}
	}

	/** Append a new set of LR1Items to the canonical collection.
	 * This is called each time a new parse state is generated.  The
	 * object returned represents the <code>CC<sub>i</sub> = { ... }</code>
	 * notation seen in the text.
	 *
	 * If a debugging stream is enabled the set is written as a string;
	 * after repeated calls this generates a list much like that used in the
	 * examples in the text.
	 *
	 * @param itemSet The newly generated set of items.
	 * @return An object representing the set of items.
	 */
	private ItemSetEntry appendParseState(HashSet itemSet) {
		ItemSetEntry result= new ItemSetEntry(cc.size(), itemSet);
		cc.put(itemSet, result);
		if (debugOutput != null)
			debugOutput.println(result);
		return result;
	}

	/** Build the canonical collection of parse states.
	 * This algorithm (based on pseudocode and discussion in the text) computes
	 * every possible state the parser can reach from the initial rule, and
	 * records the set of rules and their position at that state.
	 *
	 * It starts with a GOAL nonterminal which is not part of the given grammar
	 * and computed the 'closure' of that parse rule to get the initial set of
	 * rules that the parser is at in the initial state.
	 *
	 * For each generated state, the algorithm generates REDUCE actions for
	 * parse rules that have completed and adds the next symbol in each rule
	 * still in progress to a list of 'paths'.
	 *
	 * For each possible path, the algorithm calculates the new set of rules
	 * and their progress, and if this is a new unique set it creates a parse
	 * state for it.  Transitions are then added to a list which will later
	 * be converted to SHIFTs or entries in the GOTO table.
	 *
	 * The the reductions and the transitions are the product of this algorithm.
	 * The parse states can be discarded afterward, though they might be usable
	 * for new improvements to the tables, such as error handling.
	 */
	void buildCanonicalCollection() {
		LR1Item primerGoal= new LR1Item(new ParseRule(GOAL, new Object[] { null, start }), 1, rootFollowSet);
		HashSet curItemSet;
		LinkedList worklist= new LinkedList();

		curItemSet= closure(new HashSet(Collections.singleton(primerGoal)));
//		curItemSet.remove(primerGoal);
		appendParseState(curItemSet);
		worklist.add(curItemSet);
		while (worklist.size() > 0) {
			curItemSet= (HashSet) worklist.removeFirst();
			int curIdx= getItemSetIdx(curItemSet);
			HashMap paths= new HashMap();
			for (Iterator items= curItemSet.iterator(); items.hasNext();) {
				LR1Item item= (LR1Item) items.next();
				if (item.placeholder < item.rule.symbols.length)
					// collect 'SHIFT's for after this loop
					addHighestPriorityPath(paths, item.rule.symbols[item.placeholder], priorities.get(item.rule));
				else
					// 'ACCEPT' actions get added as reductions, for now
					reductions.add(new Reduction(curIdx, item.rule, item.lookahead));
			}
			for (Iterator pathTerms= paths.entrySet().iterator(); pathTerms.hasNext();) {
				Map.Entry path= (Map.Entry) pathTerms.next();
				Object symbol= path.getKey();
				HashSet nextState= calcNextState(curItemSet, symbol);
				ItemSetEntry entry= (ItemSetEntry) cc.get(nextState);
				if (entry == null) { // its a new one
					entry= appendParseState(nextState);
					worklist.add(nextState);
				}
				transitions.add(new Transition(curIdx, entry.idx, symbol, ((Integer)path.getValue()).intValue()));
			}
		}
	}

	/** Add a transition and its greatest priority to the list of paths.
	 * Part of buildCanonicalCollection.
	 *
	 * The priority of a transition is determined by the 'priorities' of the
	 * grammar.  Sometimes several of the same transition can have different
	 * priorities, and it is important to record the highest priority of the
	 * transition so that it can be correctly compared with the priority of
	 * REDUCE rules.
	 *
	 * @param paths A set of transition paths for the current parse state
	 * @param symbol The symbol that causes this transition
	 * @param priority The newly-discovered priority of this transition, which may be more or less than the previously-discovered priority
	 */
	void addHighestPriorityPath(Map paths, Object symbol, int priority) {
		Integer oldPri= (Integer) paths.get(symbol);
		if (oldPri == null || oldPri.intValue() < priority)
			paths.put(symbol, new Integer(priority));
	}

	/** Caculate the next state reached from the current state after recognizing the given symbol.
	 * Part of buildCanonicalCollection.
	 *
	 * This simply creates a new rule set and adds all rules which can be
	 * advanced by the given symbol.  A new instance of each matching rule is
	 * creates with its 'position' advanced.
	 *
	 * The closure is also calculated, to include other rules which might now
	 * be reached.
	 *
	 * @param fromState The set of LR1 items in the current parse state
	 * @param symbol The symbol with which to advance each rule
	 * @return The closure of the new state reached
	 */
	private HashSet calcNextState(HashSet fromState, Object symbol) {
		HashSet nextState= new HashSet();
		Iterator items= fromState.iterator();
		while (items.hasNext()) {
			LR1Item cur= (LR1Item) items.next();
			if (cur.placeholder < cur.rule.symbols.length)
				if (cur.rule.symbols[cur.placeholder].equals(symbol))
					nextState.add(new LR1Item(cur.rule, cur.placeholder+1, cur.lookahead));
		}
		return closure(nextState);
	}

	/** Calculate the closure of the current set of items, modifying the set.
	 * This is a direct implementation of the Closure algorithm pseudocode
	 * from the text.
	 *
	 * Conceptually, this function adds any new rules to the set which the
	 * parser could now be processing.  In other words, any nonterminal at the
	 * current position of any rule means that all rules that reduce to that
	 * nonterminal could also be reached from this state, and are thus at
	 * 'position 0' in this state.
	 *
	 * Details:
	 * This function expands all possible nonterminal symbols in each LR1 item
	 * which are at the placeholder using every rule for that nonterminal.
	 * For each rule expanded, it then also expands leading nonterminal symbols.
	 *
	 * Along the way, it calculates all the possible lookahead terminals which
	 * could appear after an expansion of the rule.  At the end, it uses the
	 * list of rules and their corresponding lookahead sets to create new LR1
	 * items, which it adds to the itemSet, forming the closure.
	 *
	 * This algorithm depends on itemSet not starting with any productions with
	 * a placeholder of zero.  This is always true because the only sets closure
	 * is called on are generated by calcNextState, which only adds items if it
	 * moves the placeholder.  The initial 'primer' item set is handled
	 * specially so as not to violate this algorithm.
	 * (see source code of buildCanonicalCollection)
	 *
	 * @param itemSet The set of LR1 items to compute the closure for
	 * @return The same set object with the new items added
	 */
	HashSet closure(HashSet itemSet) {
		LinkedList worklist= new LinkedList();
		HashMap optionMap= new HashMap(); // map rule to a set of lookaheads that can appear after it
		for (Iterator itr= itemSet.iterator(); itr.hasNext();) {
			LR1Item item= (LR1Item) itr.next();
			if (item.placeholder < item.rule.symbols.length)
				if (item.rule.symbols[item.placeholder] instanceof Nonterminal)
					buildRuleOptions(item.rule, item.placeholder, item.lookahead, optionMap, worklist);
		}
		while (!worklist.isEmpty()) {
			ParseRule rule= (ParseRule) worklist.removeFirst();
			buildRuleOptions(rule, 0, (Set) optionMap.get(rule), optionMap, worklist);
		}
		for (Iterator itr= optionMap.entrySet().iterator(); itr.hasNext();) {
			Map.Entry ent= (Map.Entry) itr.next();
			itemSet.add(new LR1Item((ParseRule)ent.getKey(), 0, (Set)ent.getValue()));
		}
		return itemSet;
	}

	/** Find any new options for rules expanded from this rule.
	 * This is part of the 'closure' algorithm, moved to a separate function so
	 * that it could be called from two places.
	 *
	 * @param rule The current rule to expand a nonterminal of
	 * @param symbolIdx The index to expand in this rule
	 * @param outerOptions The possible lookahead symbols for 'rule'
	 * @param optionMap The map of rule-to-option being built in 'closure'
	 * @param worklist a list of rules that need (re?)investigated
	 */
	private void buildRuleOptions(ParseRule rule, int symbolIdx, Set outerOptions, HashMap optionMap, LinkedList worklist) {
		// see what lookahead options we have for this expansion
		Set options= new HashSet();
		options.add(EMPTY);
		for (int i=symbolIdx+1; i<rule.symbols.length && options.contains(EMPTY); i++) {
			options.remove(EMPTY);
			options.addAll(getFirstSet(rule.symbols[i]));
		}
		if (options.remove(EMPTY))
			options.addAll(outerOptions);
		// now see what we're expanding
		Nonterminal expandSym= (Nonterminal) rule.symbols[symbolIdx];
		// and list these options as possible lookaheads for these rules
		Iterator rules= getProductionRules(expandSym).iterator();
		while (rules.hasNext()) {
			ParseRule curRule= (ParseRule) rules.next();
			HashSet ruleOps= (HashSet) optionMap.get(curRule);
			if (ruleOps == null) {
				ruleOps= new HashSet();
				optionMap.put(curRule, ruleOps);
			}
			// only add to worklist if it changed, and if its expandable
			if (ruleOps.addAll(options))
				if (curRule.symbols.length > 0)
					if (curRule.symbols[0] instanceof Nonterminal)
						worklist.add(curRule);
		}
	}

	/** Build the action and goto tables from the results of buildCanonicalCollection.
	 * The inputs are the fields 'transitions' and 'reductions', generated by
	 * buildCanonicalCollection.
	 *
	 * The algorithm simply adds all the entries to the tables and resolves
	 * conflicts by comparing priorities of the actions.
	 *
	 * @return A set of data representing the tables needed for an LR1 parse.
	 */
	Tables buildTables() {
		Map ruleToIdx= LR1_Table.buildArrayReverseMapping(rules);
		HashMap rulePrec= new HashMap();
		Set conflicts= new HashSet();
		Map acnPriMap= new HashMap();
		Map[] actionTable= new Map[cc.size()];
		Map[] gotoTable= new Map[cc.size()];
		int actionTableBuckets= nonterminals.size(); // number of hash buckets
		int gotoTableBuckets= (terminals.size()/3)<<2;
		for (int i=0; i<actionTable.length; i++) {
			actionTable[i]= new HashMap(actionTableBuckets);
			gotoTable[i]= new HashMap(gotoTableBuckets);
		}

		for (Iterator itr=transitions.iterator(); itr.hasNext();) {
			Transition tr= (Transition) itr.next();
			if (tr.symbol instanceof Nonterminal)
				gotoTable[tr.fromState].put(tr.symbol, new Integer(tr.toState));
			else
				setTableEntry(tr.fromState, tr.symbol, ParseAction.MkShift(tr.toState), tr.pri, actionTable, acnPriMap, conflicts);
		}
		for (Iterator itr=reductions.iterator(); itr.hasNext();) {
			Reduction reduc= (Reduction) itr.next();
			for (Iterator lookahead= reduc.terminals.iterator(); lookahead.hasNext();) {
				Object terminal= lookahead.next();
				ParseAction newAcn;
				if (reduc.rule.getNonterminal() == GOAL)
					newAcn= ParseAction.MkAccept();
				else
					newAcn= ParseAction.MkReduce(((Integer)ruleToIdx.get(reduc.rule)).intValue());
				setTableEntry(reduc.state, terminal, newAcn, priorities.get(reduc.rule), actionTable, acnPriMap, conflicts);
			}
		}
		Tables result= new Tables();
		result.actionTable= actionTable;
		result.gotoTable= gotoTable;
		result.conflicts= (String[]) conflicts.toArray(new String[conflicts.size()]);
		return result;
	}

	/** Set a cell of the parse table to the given action, if it has priority.
	 * This routine compares the new action with any existing actions, and
	 * either sets the table entry to the new action, ignores the new action,
	 * or detects a conflict which it adds to the conflict set.
	 *
	 * @param state The index of the current parser state (row of actionTable)
	 * @param lookahead The terminal symbol which the parser will look for (column of actionTable)
	 * @param acn The action to take when encountering this terminal
	 * @param pri The priority of this action
	 * @param actionTable The table being edited
	 * @param acnPriMap A map of the priorities of each action that was added to the table
	 * @param conflicts A set of conflicts to which any new conflicts will be added
	 */
	void setTableEntry(int state, Object lookahead, ParseAction acn, int pri, Map[] actionTable, Map acnPriMap, Set conflicts) {
		ParseAction prevAcn= (ParseAction) actionTable[state].get(lookahead);
		ParseAction decision= prevAcn;
		if (prevAcn == null)
			decision= acn;
		else {
			int prevPri= ((Integer) acnPriMap.get(prevAcn)).intValue();
			if (prevAcn.type == ParseAction.SHIFT && acn.type == ParseAction.SHIFT) {
				// Special handling for SHIFT-SHIFT conflicts, which aren't really conflicts at all.
				// We need to update the priority map with whichever priority is highest.
				decision= prevAcn;
				if (pri > prevPri)
					acnPriMap.put(prevAcn, new Integer(pri));
			}
			else if (acn.type == ParseAction.ACCEPT) {
				// Accept shopuld never conflict?  XXX
				throw new RuntimeException();
				// in a SHIFT-ACCEPT conflict, shift always wins.  No sense in
				// stopping the parse when we could keep going
//				decision= prevAcn;
			}
			else {
				if (pri != Priorities.DEF_PRI && prevPri != Priorities.DEF_PRI && (prevAcn.type == ParseAction.SHIFT || pri != prevPri)) {
					if (pri != prevPri)
						decision= (pri > prevPri)? acn : prevAcn;
					else {
						// if precedence of reduction rule and shift rule are equal, then use associativity
						switch (priorities.getAssociativity(pri)) {
						case Priorities.LEFT:     decision= acn; break;
						case Priorities.RIGHT:    decision= prevAcn; break;
						case Priorities.NONASSOC: decision= ParseAction.MkNonassoc(); break;
						default: throw new RuntimeException("This can't happen");
						}
					}
				}
				else {
					String conflictMsg= getConflictString(prevAcn, prevPri, acn, pri, lookahead);
					conflicts.add(conflictMsg);
					if (debugOutput != null)
						debugOutput.println(conflictMsg);
				}
			}
			if (debugOutput != null && decision != prevAcn)
				debugOutput.println("state="+state+" lookahead="+lookahead+": changed "+prevAcn+" to "+decision);
		}
		if (decision != prevAcn) {
			actionTable[state].put(lookahead, decision);
			acnPriMap.put(decision, new Integer(pri));
		}
	}

	String getConflictString(ParseAction oldAcn, int oldPri, ParseAction newAcn, int newPri, Object lookahead) {
		return ParseAction.ACTION_NAMES[oldAcn.type]+'/'
			+ParseAction.ACTION_NAMES[oldAcn.type]
			+" error deciding between "+newAcn.toStringVerbose(rules, newPri)
			+" and "+oldAcn.toStringVerbose(rules, oldPri)
			+" with a lookahead of "+lookahead+".";
	}

	/**
	 * <p>Title: LR1 Item
	 * <p>Description: This class represents a parse rule with a position marker, and a set of lookahead symbols
	 *
	 * Instances of this class are immutable and can act as the key for a
	 * hashtable.  They also support a deep-equals.  The toString method
	 * generates notation similar to that used in the text.
	 */
	static final class LR1Item {
		ParseRule rule;
		int placeholder;
		Set lookahead;
		int hash; // it'll get called a lot, so cache it

		public LR1Item(ParseRule rule, int placeholder, Set lookahead) {
			this.rule= rule;
			this.placeholder= placeholder;
			this.lookahead= lookahead;
			hash= ((rule.hashCode() ^ placeholder)*37) + lookahead.hashCode();
		}

		public int hashCode() {
			return hash;
		}

		public boolean equals(Object other) {
			if (!(other instanceof LR1Item))
				return false;
			LR1Item otherItem= (LR1Item) other;
			return rule == otherItem.rule && placeholder == otherItem.placeholder
				&& lookahead.equals(otherItem.lookahead);
		}

		public String toString() {
			StringBuffer result= new StringBuffer();
			result.append("[ ").append(rule.target.name).append("->");
			for (int i=0; i<rule.symbols.length; i++) {
				if (i == placeholder) result.append(" @");
				result.append(" ").append(rule.symbols[i]);
			}
			if (placeholder == rule.symbols.length)
				result.append(" @");
			result.append(", ").append(lookahead).append("]");
			return result.toString();
		}
	}

	/**
	 * <p>Title: Item Set Entry</p>
	 * <p>Description: Represents a parser state which is part of the Canonical Collection</p>
	 *
	 * The main purpose of this class is to associate an index with the set of
	 * LR1 items.
	 */
	static final class ItemSetEntry {
		int idx;
		HashSet itemSet;

		public ItemSetEntry(int idx, HashSet itemSet) {
			this.idx= idx;
			this.itemSet= itemSet;
		}

		public String toString() {
			StringBuffer sb= new StringBuffer("CC_"+idx+" {\n");
			for (Iterator i= itemSet.iterator(); i.hasNext();)
				sb.append("\t").append(i.next()).append("\n");
			return sb.append("}\n").toString();
		}
	}

	// Represents a transition which will be recorded in one of the tables.
	static final class Transition {
		int fromState, toState;
		Object symbol;
		int pri;

		public Transition(int fromState, int toState, Object terminal, int priority) {
			this.fromState= fromState;
			this.toState= toState;
			this.symbol= terminal;
			pri= priority;
		}
	}

	// Represents a reduction or accept which will be recorded in the action table.
	static final class Reduction {
		int state;
		ParseRule rule;
		Set terminals;

		public Reduction(int state, ParseRule rule, Set terminals) {
			this.state= state;
			this.rule= rule;
			this.terminals= terminals;
		}

		public int hashCode() {
			return state ^ rule.hashCode() ^ terminals.hashCode();
		}

		public boolean equals(Object other) {
			return (other instanceof Reduction) && this.equals((Reduction)other);
		}
		public boolean equals(Reduction other) {
			return other.state == state && other.rule == rule && other.terminals.equals(terminals);
		}
	}

	// An object representing the zero-length-string symbol needed for lookaheads
	public static final Object EMPTY= new Object() {
		public String toString() {
			return "/empty/";
		}
	};
}

