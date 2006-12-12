package com.silverdirk.parser;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Array Token Source</p>
 * <p>Description: A simple token-source for when all the tokens are prescanned and stored in an array</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class ArrayTokenSource implements TokenSource {
	Object[] tokens;
	SourcePos[] locations;
	int curPos= 0;
	String context;

	/** Constructor.
	 * @param context String The text which the tokens originated from, if any.  May be null
	 * @param tokens Object[] The values to return as tokens
	 * @param locations SourcePos[] The 'CurTokenPos' values to return. Values referr to character positions of 'context'.  May be null.
	 */
	public ArrayTokenSource(String context, Object[] tokens, SourcePos[] locations) {
		this.tokens= tokens;
		this.locations= locations;
		this.context= context;
	}

	/** Simplified constructor.
	 * <p>No context or source-location information will be available from this TokenSource.
	 * @param tokens Object[] The values to return as tokens.
	 */
	public ArrayTokenSource(Object[] tokens) {
		this(null, tokens, null);
	}

	/** Rewind the current token back to element 0 of the array.
	 * <p>Call this function to use the tokenSource over form the beginning.
	 */
	public void rewind() {
		curPos= 0;
	}

	/** Get the current token.
	 * <p>Returns the current token, or TokenSource.EOF if no more tokens are available.
	 * @return Object The object representation of the current token.
	 */
	public Object curToken() {
		if (curPos < tokens.length)
			return tokens[curPos];
		else if (curPos == tokens.length)
			return EOF;
		else
			throw new RuntimeException();
	}

	/** Get the source position where the current token was scanned.
	 * <p>If the positions given tot he constructor were null, this returns null.
	 *
	 * @return SourcePos The coordinates describing the origin of the token, or null if none are available
	 */
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

	/** Get the context for the coordinates used in the SourcePos.
	 * <p>Returns the context given to the constructor.
	 *
	 * @return String The context which pos.charStart and pos.charEnd refer to, or null if this information is not known
	 */
	public String getContext() {
		return context;
	}

	/** Advance to the next token.
	 * <p>If all tokens in the array have been used, returns TokenSource.EOF.
	 *
	 * @throws ParseException in the event of scanning trouble
	 */
	public void next() {
		if (curPos < tokens.length)
			curPos++;
	}
}
