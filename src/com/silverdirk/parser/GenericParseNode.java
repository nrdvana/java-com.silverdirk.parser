package com.silverdirk.parser;

import java.io.PrintWriter;
import java.util.*;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Generic Parse Node</p>
 * <p>Description: Node used by the default parse handler to simply builds a tree of tokens exactly as it was parsed</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * <p>Where most ParseHandlers build specific objects with the symbols matched by
 * the parse rule, the default handler just trees up the symbols using a
 * GenericParseNode.  This class is also used when "debug" is specified to the
 * parser's parse method.
 *
 * <p>This class has a nifty 'displayTree' method that sort of renders the parse
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
		Iterator lineItr= getDisplayLines().iterator();
		while (lineItr.hasNext()) {
			Iterator partItr= ((LinkedList)lineItr.next()).iterator();
			while (partItr.hasNext())
				out.print(partItr.next());
			out.println();
		}
	}

	static final String spaces= "                              ";
	static final String getSpaces(int count) {
		if (count <= spaces.length())
			return spaces.substring(0, count);
		else {
			StringBuffer sb= new StringBuffer(count);
			while (count - sb.length() > spaces.length())
				sb.append(spaces);
			sb.append(getSpaces(count - sb.length()));
			return sb.toString();
		}
	}

	LinkedList getDisplayLines() {
		String middle= "-"+type.toString();
		String spacer= getSpaces(middle.length());
		LinkedList lines= new LinkedList();
		for (int i=0; i<components.length; i++) {
			if (i != 0)
				lines.add(new LinkedList());
			if (components[i] instanceof GenericParseNode)
				lines.addAll(((GenericParseNode)components[i]).getDisplayLines());
			else {
				LinkedList line= new LinkedList();
				line.add("- "+components[i].toString());
				lines.add(line);
			}
		}
		Iterator lineItr= lines.iterator();
		int curNode= 0, curLine= 0;
		while (lineItr.hasNext()) {
			LinkedList line= (LinkedList) lineItr.next();
			boolean isNodeLine= (line.size() > 0) && (((String)line.getFirst()).charAt(0) != ' ');
			boolean isMidLine= curLine == (lines.size()>>1);
			if (components.length > 1) {
				if (isNodeLine && curNode == 0)
					line.addFirst(" ,");
				else if (isNodeLine && curNode == components.length-1)
					line.addFirst(" `");
				else if (curNode > 0 && curNode < components.length)
					line.addFirst(isMidLine? "-|":" |");
				else
					line.addFirst("  ");
			}
			line.addFirst(isMidLine? middle : spacer);
			if (isNodeLine)
				curNode++;
			curLine++;
		}
		return lines;
	}

}
