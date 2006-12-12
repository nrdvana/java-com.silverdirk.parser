package com.silverdirk.parser;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Token Source</p>
 * <p>Description: Similar to an iterator, TokenSource iterates through tokens while also providing context information</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public interface TokenSource {
	/** Get the current token.
	 * This method returns the same token value until 'next' is called.
	 * If no tokens remain, it will always return TokenSource.EOF
	 * @return Object The object representation of the current token.
	 */
	public Object curToken();

	/** Get the source position where the current token was scanned.
	 * The SourcePos indicates a two-dimensional text rectangle of characters
	 * that were scanned to produce the current token.  The charStart and
	 * charEnd values are 1-based indicies into the string specified in
	 * 'context'.
	 * If no position coordinates are known, this returns null.
	 *
	 * @return SourcePos The coordinates describing the origin of the token, or null if none are available
	 */
	public SourcePos curTokenPos();

	/** Get the context for the coordinates used in the SourcePos.
	 * This usually returns the string of the line from which the token was
	 * scanned, however the only real requirement is that it be a string that
	 * contains the coordinates used by curTokenPos.charStart and charEnd.
	 * If no context is available, this function returns null.
	 *
	 * @return String The context which pos.charStart and pos.charEnd refer to, or null if this information is not known
	 */
	public String getContext();

	/** Advance to the next token.
	 * Calling this method changes curToken and curTokenPos, and possibly getContext.
	 * This method can be called after the token source has been exhausted and
	 * will set 'curToken' to TokenSource.EOF
	 *
	 * @throws ParseException in the event of scanning trouble
	 */
	public void next() throws ParseException;

	// the value returned bu curToken() after no more tokens are available
	public static final Object EOF= new Object() {
		public String toString() { return "/eof/"; }
	};

	public interface ScanError {} // used for recognizing error tokens
}
