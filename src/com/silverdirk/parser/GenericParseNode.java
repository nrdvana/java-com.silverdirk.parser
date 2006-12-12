package com.silverdirk.parser;

import java.io.PrintWriter;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Generic Parse Node</p>
 * <p>Description: Node used by the default parse handler to simply builds a tree of tokens exactly as it was parsed</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * Where most ParseHandlers build specific objects with the symbols matched by
 * the parse rule, the default handler just trees up the symbols using a
 * GenericParseNode.  This class is also used when "debug" is specified to the
 * parser's parse method.
 *
 * This class has a nifty 'displayTree' method that sort of renders the parse
 * tree on a PrintWriter.
 *
 * @author Michael Conrad
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

	public String toString() {
		StringBuffer result= new StringBuffer(type.toString()).append('(');
		if (components != null && components.length > 0) {
			for (int i=0; i<components.length; i++)
				result.append(components[i]).append(", ");
			result.setLength(result.length()-2);
		}
		return result.append(')').toString();
	}

	public void displayTree(PrintWriter out) {
		displayTree(out, "", "");
	}
	public void displayTree(PrintWriter out, String prefix, String connector) {
		out.println(prefix+connector+"__"+type+"");
		String subPrefix= prefix+" |";
		String subConnector= "_";
		for (int i=0; i<components.length; i++) {
			if (i == components.length-1) {
				subPrefix= prefix+"  ";
				subConnector= "\\";
			}
			if (components[i] instanceof GenericParseNode)
				((GenericParseNode)components[i]).displayTree(out, subPrefix, subConnector);
			else
				out.println(subPrefix+subConnector+"__ "+components[i]);
		}
	}
}
