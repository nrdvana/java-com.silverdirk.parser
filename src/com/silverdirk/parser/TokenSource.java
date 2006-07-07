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
public interface TokenSource {
	public Object curToken();
	public SourcePos curTokenPos();
	public void next() throws ParseException;
	public String getContext();

	public static final Object EOF= new Object() {
		public String toString() { return "/eof/"; }
	};

	public interface ScanError {} // used for recognizing error tokens
}