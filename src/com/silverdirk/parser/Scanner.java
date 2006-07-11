package com.silverdirk.parser;

/**
 * <p>Project: com.silverdirk</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * @author not attributable
 * @version $Revision$
 */
public class Scanner implements TokenSource {
	ScanRuleSet[] states;
	Object[] stateData;
	int state= 0;
	int pos= 0;
	int lastPos= 0;
	int lineNum= 1, lineStart= 0;
	Object token;
	CharSequence data;

	public Scanner(ScanRuleSet[] stateRuleSets, CharSequence data) throws ParseException {
		states= stateRuleSets;
		stateData= new Object[states.length];
		this.data= data;
		next();
	}

	public Object curToken() {
		return token;
	}

	public SourcePos curTokenPos() {
		return new SourcePos(0, lastPos, 0, pos);
	}

	public void next() throws ParseException {
		lastPos= pos; // last time there was a successful result
		token= ScanRule.EMIT_NOTHING;
		while (token == ScanRule.EMIT_NOTHING) {
			if (pos >= data.length()) {
				token= EOF;
				break;
			}
			ScanRuleSet curState= states[state];
			CharSequence bufferTail= data.subSequence(pos, data.length());
			ScanRule[] options= curState.getRulesFor(bufferTail);
			boolean success= false;
			for (int i=0; i<options.length; i++) {
				ScanRule.ScanMatch match;
				try {
					match= options[i].getMatch(this, bufferTail);
				}
				catch (Exception ex) {
					throw new ParseException(ex.getMessage(), getContext(), getSourcePos());
				}
				if (match != null) {
					if (match.charsConsumed <= 0)
						throw new RuntimeException("Error in spec: scan rule can potentially match empty string");
					success= true;
					token= match.token;
					pos+= match.charsConsumed;
					break;
				}
			}
			if (!success)
				throw new ParseException("Scan error while processing "+states[state].stateName, getContext(), getSourcePos());
		}
	}

	public String getContext() {
		return data.subSequence(lineStart, Math.min(pos+20, data.length())).toString();
	}

	public SourcePos getSourcePos() {
		return new SourcePos(lineNum, lastPos-lineStart+1, lineNum, pos-lineStart+1);
	}

	public void initAllStateData(Object[] newStateData) {
		if (newStateData.length != states.length)
			throw new RuntimeException("Array length mismatch");
		stateData= newStateData;
	}
	public Object[] getAllStateData() {
		return stateData;
	}

	public Object getStateData() {
		return stateData[state];
	}

	public void setStateData(Object newVal) {
		stateData[state]= newVal;
	}

	public int getState() {
		return state;
	}

	public void stateTrans(int newState) {
		if (newState >= states.length)
			throw new ArrayIndexOutOfBoundsException();
		state= newState;
	}

	public int getLineNo() {
		return lineNum;
	}

	public void incLineNo() {
		lineNum++;
		lineStart= pos;
	}

	public void incLineNo(int offset) {
		lineNum+= offset;
		lineStart= pos;
	}
}