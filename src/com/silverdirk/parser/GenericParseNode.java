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
public class GenericParseNode {
	public Nonterminal type;
	public Object[] components;
	public SourcePos from;

	public GenericParseNode(Nonterminal type, SourcePos from, Object[] symbols) {
		this.type= type;
		this.from= from;
		this.components= symbols;
	}
}