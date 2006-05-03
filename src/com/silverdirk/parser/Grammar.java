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

	public Grammar(Nonterminal start, ParseRule[] rules, Parser.Priorities priorities) {
		this.start= start;
		this.rules= rules;
		this.priorities= priorities;
	}
}