package com.silverdirk.parser;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Text Source Position</p>
 * <p>Description: This class describes a start and end coordinate in terms of lines and columns of text</p>
 * <p>Copyright: Copyright (c) 2004-200</p>
 *
 * This class performs no processing, so its fields are public.
 * All coordinates are 1-based by convention, since this is how line numbers
 * and character positions are usually reported.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class SourcePos {
	public SourcePos() {
	}

	public SourcePos(int lineStart, int charStart, int lineEnd, int charEnd) {
		this.lineStart= lineStart;
		this.lineEnd= lineEnd;
		this.charStart= charStart;
		this.charEnd= charEnd;
	}

	public int lineStart, lineEnd;
	public int charStart, charEnd;

	public String toString() {
		return "line "+lineStart+":"+charStart+"-"+charEnd;
	}
}
