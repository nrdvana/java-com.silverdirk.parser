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