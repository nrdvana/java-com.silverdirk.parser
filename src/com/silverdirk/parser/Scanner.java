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
	CharSequence data;

	public Scanner(ScanRuleSet[] stateRuleSets, CharSequence data) {
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
			CharSequence bufferTail= data.subSequence(pos, data.length());
			ScanRule[] options= curState.getRulesFor(bufferTail);
			boolean success= false;
			for (int i=0; i<options.length; i++) {
				ScanRule.ScanMatch match= options[i].getMatch(bufferTail);
				if (match != null) {
					if (match.charsConsumed <= 0)
						throw new RuntimeException("Error in spec: scan rule can potentially match empty string");
					success= true;
					token= match.token;
					pos+= match.charsConsumed;
					if (options[i].hasStateTransition())
						state= options[i].getStateTransition();
					break;
				}
			}
			if (!success)
				throw new RuntimeException("Scan error at \""+bufferTail.subSequence(0, Math.min(10, bufferTail.length())).toString()+"\" near \""+getContext()+"\"");
		} while (token == ScanRule.EMIT_NOTHING);
	}

	public String getContext() {
		return data.subSequence(Math.max(0, pos-20), Math.min(pos+20, data.length())).toString();
	}
}