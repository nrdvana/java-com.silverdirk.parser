package com.silverdirk.parser;

import java.util.*;
import com.silverdirk.parser.Parser$Priorities;
import com.silverdirk.parser.Parser$ParseAction;

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
	HashMap rulePriCache= new HashMap();
	HashMap productionMap= new HashMap();
	HashMap terminals= new HashMap();
	HashMap nonterminals= new HashMap();
	HashMap firstSets= new HashMap();
	HashMap cc= new HashMap(); // cc == Canonical Collection
	LinkedList transitions= new LinkedList();
	LinkedList reductions= new LinkedList();
	int acceptState= -1;

	public static final class Tables {
		public HashMap[] actionTable;
		public HashMap[] gotoTable;
		public List conflicts;
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
			HashSet paths= new HashSet();
			for (Iterator items= curItemSet.iterator(); items.hasNext();) {
				LR1Item item= (LR1Item) items.next();
				if (item.placeholder < item.rule.symbols.length)
					paths.add(item.rule.symbols[item.placeholder]);
				else
					reductions.add(new Reduction(curIdx, item.rule, item.lookahead));
			}
			for (Iterator pathTerms= paths.iterator(); pathTerms.hasNext();) {
				Object symbol= pathTerms.next();
				HashSet nextState= calcNextState(curItemSet, symbol);
				ItemSetEntry entry= (ItemSetEntry) cc.get(nextState);
				if (entry == null) { // its a new one
					entry= appendItemSet(nextState);
					worklist.add(nextState);
				}
				transitions.add(new Transition(curIdx, entry.idx, symbol));
			}
		}
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
		HashMap rulePrec= new HashMap();
		ArrayList conflicts= new ArrayList(0);
		rulePriCache.clear(); // reset the priority cache in case they have changed
		HashMap[] actionTable= new HashMap[cc.size()];
		HashMap[] gotoTable= new HashMap[cc.size()];
		int actionTableBuckets= nonterminals.size(); // number of hash buckets
		int gotoTableBuckets= (terminals.size()/3)<<2;
		for (int i=0; i<actionTable.length; i++) {
			actionTable[i]= new HashMap(actionTableBuckets);
			gotoTable[i]= new HashMap(gotoTableBuckets);
		}

		for (int i=transitions.size(); i>0; i--) {
			Transition tr= (Transition) transitions.removeFirst();
			if (tr.symbol instanceof Nonterminal)
				gotoTable[tr.fromState].put(tr.symbol, new Integer(tr.toState));
			else
				actionTable[tr.fromState].put(tr.symbol, ParseAction.CreateShift(tr.toState));
		}
		for (int i=reductions.size(); i>0; i--) {
			Reduction reduc= (Reduction) reductions.removeFirst();
			for (Iterator lookahead= reduc.terminals.iterator(); lookahead.hasNext();) {
				Object terminal= lookahead.next();
				ParseAction prevAcn= (ParseAction) actionTable[reduc.state].get(terminal);
				ParseAction newAcn= ParseAction.CreateReduce(reduc.rule);
				if (terminal == TokenSource.EOF && reduc.rule.target == start)
					newAcn.type= ParseAction.ACCEPT;
				ParseAction winner= decidePrecedence(newAcn, prevAcn, terminal, conflicts);
				if (winner != prevAcn)
					actionTable[reduc.state].put(terminal, winner);
			}
		}
		Tables result= new Tables();
		result.actionTable= actionTable;
		result.gotoTable= gotoTable;
		result.conflicts= conflicts;
		return result;
	}

	ParseAction decidePrecedence(ParseAction reducAcn, ParseAction prevAcn, Object lookahead, List conflicts) {
		if (prevAcn == null)
			return reducAcn;
		if (prevAcn.type == ParseAction.SHIFT) {
			// Shift-Reduce conflict
			int reducPrec= getRulePrecedence(reducAcn.rule);
			int shiftPrec= getTermPrecedence(lookahead);
			if (reducPrec == 0 || shiftPrec == 0) {
				conflicts.add("Shift/Reduce error deciding between "+reducAcn+" and "+prevAcn+" with a lookahead of "+lookahead+".");
				return prevAcn;
			}
			if (reducPrec < shiftPrec)
				return prevAcn;
			if (reducPrec > shiftPrec)
				return reducAcn;
			// if precedence of rule and terminal are equal, then use associativity
			switch (priorities.getAssociativity(reducPrec)) {
			case Priorities.LEFT:
				return reducAcn;
			case Priorities.RIGHT:
				return prevAcn;
			case Priorities.NONASSOC:
				return ParseAction.CreateNonassocSyntaxError();
			}
			throw new RuntimeException("This can't happen");
		}
		else if (prevAcn.type == ParseAction.REDUCE || prevAcn.type == ParseAction.ACCEPT) {
			// Reduce-Reduce conflict
			conflicts.add("Reduce/Reduce error deciding between "+reducAcn+" and "+prevAcn+" with a lookahead of "+lookahead+".");
			return prevAcn;
		}
		else // action type is NONASSOC_ERR
			throw new RuntimeException("Can this happen?");
	}

	int getRulePrecedence(ParseRule rule) {
		Integer pri= (Integer) rulePriCache.get(rule);
		if (pri == null) {
			pri= (Integer) priorities.getInt(rule);
			if (pri == null) {
				// calculate default priority of the rule based on last terminal symbol
				int i;
				for (i= rule.symbols.length-1; i >= 0; i--)
					if (!(rule.symbols[i] instanceof Nonterminal)) {
						pri= priorities.getInt(rule.symbols[i]);
						break;
					}
			}
			if (pri == null)
				pri= Priorities.DEF_PRI;
			rulePriCache.put(rule, pri);
		}
		return pri.intValue();
	}

	int getTermPrecedence(Object terminal) {
		Integer pri= priorities.getInt(terminal);
		if (pri == null)
			return 0;
		else
			return pri.intValue();
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
			hash= ((rule.hashCode() ^ placeholder)<<5) + lookahead.hashCode();
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

		public Transition(int fromState, int toState, Object terminal) {
			this.fromState= fromState;
			this.toState= toState;
			this.symbol= terminal;
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
	}

	public static final Object EMPTY= new Object() {
		public String toString() {
			return "/empty/";
		}
	};
}