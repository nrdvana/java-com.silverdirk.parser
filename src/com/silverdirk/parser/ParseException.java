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
		super(msg);
		location= loc;
		this.source= source;
	}
	public ParseException(String msg, String source, Object[] expectedList, SourcePos loc) {
		this(msg, source, loc);
		if (expectedList.length > 0)
			this.expectedList= expectedList;
	}

	public SourcePos getLocation() {
		return location;
	}

	public String getSource() {
		return source;
	}

	public String getParserMsg() {
		return getMessage()+" on "+getLocation()+" while looking for: "+getExpectationStr();
	}

	public String getExpectationStr() {
		StringBuffer expect= new StringBuffer();
		if (expectedList != null) {
			if (expectedList.length > 0) {
				expect.append(expectedList[0]);
				for (int i=1; i<expectedList.length; i++)
					expect.append(" ").append(expectedList[i]);
			}
		}
		return expect.toString();
	}

	Object[] expectedList= null;
	SourcePos location;
	String source;
}