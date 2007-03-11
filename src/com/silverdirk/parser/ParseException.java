package com.silverdirk.parser;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Parse Exception</p>
 * <p>Description: Exception class that also holds context information</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * <p>Used by both Parser and Scanner, this class contains an error message,
 * optional context string, optional parser state stack, optional list of
 * expected tokens, and optional source-location of the error.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class ParseException extends RuntimeException {
	public ParseException(String msg, String source, SourcePos loc) {
		this(msg, source, null, null, loc);
	}
	public ParseException(String msg, String source, Parser.ParseState[] parseStack, Object[] expected, SourcePos loc) {
		super(msg);
		this.source= source;
		this.location= loc;
		this.stack= parseStack;
		this.expectedList= expected;
	}

	/** Get the source-location where the error happened.
	 * <p>Might be null.
	 */
	public SourcePos getLocation() {
		return location;
	}

	/** Get the context string associated with this error.
	 * <p>Might be null.
	 */
	public String getSource() {
		return source;
	}

	/** Get a semi-verbose message describing the parse error.
	 */
	public String getParserMsg() {
		String result= getMessage()+" at "+getLocation();
		if (stack != null)
			result+="\n Had: "+getStackString();
		if (expectedList != null)
			result+="\n Expecting: "+getExpectationStr();
		return result;
	}

	/** Get a string showing the state of the parser stack.
	 */
	public String getStackString() {
		if (stack == null) return null;
		StringBuffer result= new StringBuffer().append("{\n    ");
		for (int i=0; i<stack.length; i++)
			result.append(stack[i].data).append("\n    ");
		if (expectedList.length > 0)
			result.replace(result.length()-3, result.length()-1, "}");
		return result.toString();
	}

	/** Get a string of the symbols that were expected at this point.
	 */
	public String getExpectationStr() {
		if (expectedList == null) return null;
		StringBuffer expect= new StringBuffer();
		for (int i=0; i<expectedList.length; i++)
			expect.append(expectedList[i]).append(' ');
		if (expectedList.length > 0)
			expect.deleteCharAt(expect.length()-1);
		return expect.toString();
	}

	/** Produce a full "syntax error" message as seen in most parsers and interpreters.
	 */
	public String getFullMessage() {
		StringBuffer msg= new StringBuffer(getMessage());
		if (location != null)
			msg.append(" at line ").append(location.lineStart);
		msg.append('\n');
		String expected= getExpectationStr();
		if (expected != null)
			msg.append("Expecting one of: ").append(expected).append("\n");
		if (location != null) {
			int locStart= location.charStart-1; // 1-based indicies are annoying
			int locEnd= location.charEnd-1;
			int contextStart= Math.max(0, locStart-7);
			int contextEnd= Math.min(Math.min(contextStart+70, locEnd+7), source.length());
			msg.append(source.substring(contextStart, contextEnd)).append('\n');
			for (int i=contextStart; i<locStart; i++)
				msg.append(' ');
			msg.append('^');
			int width= Math.min(locEnd-locStart, contextEnd-locStart);
			for (int i= width-2; i>=0; i--)
				msg.append('-');
			if (width > 1)
				msg.append('^');
			msg.append('\n');
		}
		return msg.toString();
	}

	public String toString() {
		return getFullMessage();
	}

	Object[] expectedList= null;
	Parser.ParseState[] stack;
	SourcePos location;
	String source;
}
