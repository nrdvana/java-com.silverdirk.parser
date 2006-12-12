package com.silverdirk.parser;
import java.util.Set;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Grammar</p>
 * <p>Description: An object holding the parts of a grammar</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * This class doesn't really do anything aside from holding a collection of
 * values that usually need to be passed as a group.
 *
 * Grammar now contains a "startFolowSet" which is a set of all nonterminal
 * symbols that are allowed to follow a complete parse of the start symbol.
 * This set defaults to "EOF" which means that the entire token stream must be
 * consumed for a parse to be successful.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class Grammar {
	public Nonterminal start;
	public ParseRule[] rules;
	public Parser.Priorities priorities;
	public Set startFollowSet= TableBuilder.EOF_SET;

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
	public Grammar(Nonterminal start, Set startFollowSet, ParseRule[] rules, Parser.Priorities priorities) {
		this(start, rules, priorities);
		this.startFollowSet= startFollowSet;
	}
}
