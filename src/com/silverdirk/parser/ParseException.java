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
public class ParseException extends Exception {
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

	public SourcePos getLocation() {
		return location;
	}

	public String getSource() {
		return source;
	}

	public String getParserMsg() {
		String result= getMessage()+" at "+getLocation();
		if (stack != null)
			result+="\n Had: "+getStackString();
		if (expectedList != null)
			result+="\n Expecting: "+getExpectationStr();
		return result;
	}

	public String getStackString() {
		if (stack == null) return null;
		StringBuffer result= new StringBuffer().append("{\n    ");
		for (int i=0; i<stack.length; i++)
			result.append(stack[i].data).append("\n    ");
		if (expectedList.length > 0)
			result.replace(result.length()-3, result.length()-1, "}");
		return result.toString();
	}

	public String getExpectationStr() {
		if (expectedList == null) return null;
		StringBuffer expect= new StringBuffer();
		for (int i=0; i<expectedList.length; i++)
			expect.append(expectedList[i]).append(' ');
		if (expectedList.length > 0)
			expect.deleteCharAt(expect.length()-1);
		return expect.toString();
	}

	Object[] expectedList= null;
	Parser.ParseState[] stack;
	SourcePos location;
	String source;
}