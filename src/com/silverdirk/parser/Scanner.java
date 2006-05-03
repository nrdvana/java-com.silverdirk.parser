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
	int state= 0;
	int pos= 0;
	int lastPos= 0;
	Object token;
	String data;

	public Scanner(ScanRuleSet[] stateRuleSets, String data) {
		states= stateRuleSets;
		this.data= data;
		next();
	}

	public Object curToken() {
		return token;
	}

	public SourcePos curTokenPos() {
		return new SourcePos(0, lastPos, 0, pos);
	}

	public void next() {
		lastPos= pos;
		do {
			if (pos >= data.length()) {
				token= EOF;
				return;
			}
			ScanRuleSet curState= states[state];
			String bufferTail= data.substring(pos, data.length());
			ScanRule[] options= curState.getRulesFor(bufferTail);
			boolean success= false;
			for (int i=0; i<options.length; i++) {
				ScanRule.ScanMatch match= options[i].getMatch(bufferTail);
				if (match != null) {
					success= true;
					token= match.token;
					pos+= match.charsConsumed;
					if (options[i].hasStateTransition())
						state= options[i].getStateTransition();
				}
			}
			if (!success)
				throw new RuntimeException("Scan error near "+getContext());
		} while (token == ScanRule.EMIT_NOTHING);
	}

	public String getContext() {
		return data.substring(Math.max(0, pos-50), 100);
	}
}