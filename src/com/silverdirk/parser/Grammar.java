package com.silverdirk.parser;

/**
 * <p>Project: 42</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004-2005</p>
 *
 * @author not attributable
 * @version $Revision$
 */
public class Grammar {
	public Nonterminal start;
	public ParseRule[] rules;
	public Parser.Priorities priorities;

	public Grammar(Nonterminal start, java.util.Collection rules) {
		this(start, (ParseRule[]) rules.toArray(new ParseRule[rules.size()]));
	}

	public Grammar(Nonterminal start, ParseRule[] rules) {
		this(start, rules, null);
	}

	public Grammar(Nonterminal start, java.util.Collection rules, Parser.Priorities priorities) {
		this(start, (ParseRule[]) rules.toArray(new ParseRule[rules.size()]), priorities);
	}

	public Grammar(Nonterminal start, ParseRule[] rules, Parser.Priorities priorities) {
		this.start= start;
		this.rules= rules;
		this.priorities= priorities;
	}
}