package com.silverdirk.parser;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Nonterminal Symbol</p>
 * <p>Description: An object representing a nonterminal symbol of a grammar</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * <p>Nonterminals are unique immutable objects.  They simply act as keys for
 * a large number of activities in the Parser and TableBuilder.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class Nonterminal {
	String name;

	public Nonterminal() {
		name= "";
	}

	public Nonterminal(String name) {
		this.name= name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "<"+name+">";
	}
}
