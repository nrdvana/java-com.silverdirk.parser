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
public class Nonterminal {
	String name;

	public Nonterminal() {
		name= "";
	}

	public Nonterminal(String name) {
		this.name= name;
	}

	public String toString() {
		return "<"+name+">";
	}
}