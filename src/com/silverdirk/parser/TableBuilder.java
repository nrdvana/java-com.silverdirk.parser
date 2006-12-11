package com.silverdirk.parser;

import java.util.*;
import com.silverdirk.parser.Parser$Priorities;
import com.silverdirk.parser.LR1_Table$ParseAction;

/**
 * <p>Project: 42</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004-2005</p>
 *
 * @author not attributable
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
	HashMap cc= new HashMap(); // cc == Canonical Collection
	List transitions= new ArrayList();
	List reductions= new ArrayList();
	int acceptState= -1;

	public static final class Tables {
		public Map[] actionTable;
		public Map[] gotoTable;
		public String[] conflicts;
	}

	public static final Tables generate(Nonterminal startSymbol, ParseRule[] rules, Priorities priorities) {
		TableBuilder tb= new TableBuilder(startSymbol, rules, priorities);
		tb.buildFirstSets();
		tb.buildCanonicalCollection();
		return tb.buildTables();
	}

	TableBuilder(Nonterminal startSymbol, ParseRule[] rules, Priorities priorities) {
		start= startSymbol;
		this.rules= rules;
		this.priorities= priorities != null? priorities : new Priorities();
		catalogSymbol(start);
		for (int i=0; i<rules.length; i++)
			processRule(rules[i]);
	}

	private void processRule(ParseRule r) {
		catalogSymbol(r.target);
		for (int i=0; i<r.symbols.length; i++)
			catalogSymbol(r.symbols[i]);
		LinkedList prods= (LinkedList) productionMap.get(r.target);
		prods.add(r);
	}

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

	Set getFirstSet(Object symbol) {
		if (symbol instanceof Nonterminal)
			return (HashSet) firstSets.get(symbol);
		else
			return Collections.singleton(symbol);
	}

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

	private ItemSetEntry appendItemSet(HashSet itemSet) {
		ItemSetEntry result= new ItemSetEntry(cc.size(), itemSet);
		cc.put(itemSet, result);
		return result;
	}

	void buildCanonicalCollection() {
		LR1Item primerGoal= new LR1Item(new ParseRule(null, new Object[] { null, start }), 1, Collections.singleton(TokenSource.EOF));
		HashSet curItemSet;
		LinkedList worklist= new LinkedList();

		curItemSet= closure(new HashSet(Collections.singleton(primerGoal)));
		curItemSet.remove(primerGoal);
		appendItemSet(curItemSet);
		worklist.add(curItemSet);
		while (worklist.size() > 0) {
			curItemSet= (HashSet) worklist.removeFirst();
			int curIdx= getItemSetIdx(curItemSet);
			HashMap paths= new HashMap();
			for (Iterator items= curItemSet.iterator(); items.hasNext();) {
				LR1Item item= (LR1Item) items.next();
				if (item.placeholder < item.rule.symbols.length)
					addHighestPriorityPath(paths, item.rule.symbols[item.placeholder], priorities.get(item.rule));
				else
					reductions.add(new Reduction(curIdx, item.rule, item.lookahead));
			}
			for (Iterator pathTerms= paths.entrySet().iterator(); pathTerms.hasNext();) {
				Map.Entry path= (Map.Entry) pathTerms.next();
				Object symbol= path.getKey();
				HashSet nextState= calcNextState(curItemSet, symbol);
				ItemSetEntry entry= (ItemSetEntry) cc.get(nextState);
				if (entry == null) { // its a new one
					entry= appendItemSet(nextState);
					worklist.add(nextState);
				}
				transitions.add(new Transition(curIdx, entry.idx, symbol, ((Integer)path.getValue()).intValue()));
			}
		}
	}

	void addHighestPriorityPath(Map paths, Object symbol, int priority) {
		Integer oldPri= (Integer) paths.get(symbol);
		if (oldPri == null || oldPri.intValue() < priority)
			paths.put(symbol, new Integer(priority));
	}

	/** Calculate the closure of the current set of items, modifying the set.
	 * This function expands all possible nonterminal symbols in each LR1 item
	 * which are at the placeholder using every rule for that nonterminal.
	 * For each rule expanded, it then also expands leading nonterminal symbols.
	 *
	 * Along the way, it calculates all the possible lookahead terminals which
	 * could appear after an expansion of the rule.  At the end, it uses the
	 * list of rules and their corresponding lookagead sets to create new LR1
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

	Tables buildTables() {
		Map ruleToIdx= LR1_Table.buildArrayReverseMapping(rules);
		HashMap rulePrec= new HashMap();
		Set conflicts= new HashSet();
		Map acnPriMap= new HashMap();
//		rulePriCache.clear(); // reset the priority cache in case they have changed
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
				setTableEntry(tr.fromState, tr.symbol, CreateShift(tr.toState), tr.pri, actionTable, acnPriMap, conflicts);
		}
		for (Iterator itr=reductions.iterator(); itr.hasNext();) {
			Reduction reduc= (Reduction) itr.next();
			int ruleIdx= ((Integer)ruleToIdx.get(reduc.rule)).intValue();
			for (Iterator lookahead= reduc.terminals.iterator(); lookahead.hasNext();) {
				Object terminal= lookahead.next();
				boolean isAccept= (terminal == TokenSource.EOF) && (reduc.rule.target == start);
				ParseAction newAcn= CreateReduce(ruleIdx, isAccept);
				setTableEntry(reduc.state, terminal, newAcn, priorities.get(reduc.rule), actionTable, acnPriMap, conflicts);
			}
		}
		Tables result= new Tables();
		result.actionTable= actionTable;
		result.gotoTable= gotoTable;
		result.conflicts= (String[]) conflicts.toArray(new String[conflicts.size()]);
		return result;
	}

	public void setTableEntry(int state, Object lookahead, ParseAction acn, int pri, Map[] actionTable, Map acnPriMap, Set conflicts) {
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
			else {
				if (pri != Priorities.DEF_PRI && prevPri != Priorities.DEF_PRI && (prevAcn.type == ParseAction.SHIFT || pri != prevPri)) {
					if (pri != prevPri)
						decision= (pri > prevPri)? acn : prevAcn;
					else {
						// if precedence of reduction rule and shift rule are equal, then use associativity
						switch (priorities.getAssociativity(pri)) {
						case Priorities.LEFT:     decision= acn; break;
						case Priorities.RIGHT:    decision= prevAcn; break;
						case Priorities.NONASSOC: decision= CreateNonassocSyntaxError(); break;
						default: throw new RuntimeException("This can't happen");
						}
					}
				}
				else
					conflicts.add(getConflictString(prevAcn, prevPri, acn, pri, lookahead));
			}
		}
		if (decision != prevAcn) {
			actionTable[state].put(lookahead, decision);
			acnPriMap.put(decision, new Integer(pri));
		}
	}

	String getConflictString(ParseAction oldAcn, int oldPri, ParseAction newAcn, int newPri, Object lookahead) {
		return (oldAcn.type==ParseAction.SHIFT?"Shift":"Reduce")+'/'
			+(newAcn.type==ParseAction.SHIFT?"Shift":"Reduce")
			+" error deciding between "+newAcn.toString(rules, newPri)
			+" and "+oldAcn.toString(rules, oldPri)
			+" with a lookahead of "+lookahead+".";
	}

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

	static final class ItemSetEntry {
		int idx;
		HashSet itemSet;

		public ItemSetEntry(int idx, HashSet itemSet) {
			this.idx= idx;
			this.itemSet= itemSet;
		}
	}

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

	public static final Object EMPTY= new Object() {
		public String toString() {
			return "/empty/";
		}
	};

	public static ParseAction CreateShift(int nextState) {
		ParseAction result= new ParseAction(ParseAction.SHIFT, 0, 0);
		result.nextState= nextState;
		return result;
	}

	public static ParseAction CreateReduce(int rule, boolean isAcceptReduction) {
		ParseAction result= new ParseAction(isAcceptReduction? ParseAction.ACCEPT:ParseAction.REDUCE, 0, 0);
		result.rule= rule;
		return result;
	}

	public static ParseAction CreateNonassocSyntaxError() {
		return new ParseAction(ParseAction.NONASSOC_ERR, 0, 0);
	}
}

