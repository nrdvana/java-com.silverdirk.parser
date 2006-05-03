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
public class ArrayTokenSource implements TokenSource {
	Object[] tokens;
	SourcePos[] locations;
	int curPos= 0;
	String context;

	public ArrayTokenSource(String context, Object[] tokens, SourcePos[] locations) {
		this.tokens= tokens;
		this.locations= locations;
		this.context= context;
	}

	public String getContext() {
		return context;
	}

	public void rewind() {
		curPos= 0;
	}

	public Object curToken() {
		if (curPos < tokens.length)
			return tokens[curPos];
		else if (curPos == tokens.length)
			return EOF;
		else
			throw new RuntimeException();
	}

	public SourcePos curTokenPos() {
		if (locations == null)
			return new SourcePos();
		else if (curPos < tokens.length)
			return locations[curPos];
		else if (curPos == tokens.length)
			return null;
		else
			throw new RuntimeException();
	}

	public void next() {
		curPos++;
	}
}