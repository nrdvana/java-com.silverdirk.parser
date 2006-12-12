package com.silverdirk.parser;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Scanner</p>
 * <p>Description: Cheap implementation of a scanner for use with the parser</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * <p>This class should be rewritten as a regex engine which processes multiple
 * regex patterns in parallel looking for the first match, perhaps with a
 * priority system.
 *
 * <p>This could almost be accomplished with java.util.regex by taking each scan
 * rule's pattern and merging them into a giant multi-condition regex:
 * <code>String[] { a, b, c, d } => "(a)|(b)|(c)|(d)"</code>
 * and then checking which group was found to determine which rule was matched.
 * However, this would also involve parsing the regexes to determine how many
 * groups the user had used in their pattern for each rule, and by the time I
 * do that I might as well just implement the regex engine.
 *
 * @author Michael Conrad
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

	/** Constructor.
	 *
	 * @param stateRuleSets ScanRuleSet[] A list of sets of scan rules, one for each state the canner can reach.
	 * @param data CharSequence The sequence of characters to scan
	 * @throws ParseException whever a token cannot be generated, or when no rules match the input
	 */
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
		return new SourcePos(lineNum, lastPos-lineStart+1, lineNum, pos-lineStart+1);
	}

	public String getContext() {
		return data.subSequence(lineStart, Math.min(pos+20, data.length())).toString();
	}

	/** Advance to the next token.
	 * <p>This method repeatedly calls getMatch on appropriate ScanRules until it
	 * receives a token form one of them.  The funtion then returns and the new
	 * token is available with curToken().  Also calculates the token's
	 * position.
	 * @throws ParseException if no rules match the current input
	 */
	public void next() throws ParseException {
		token= ScanRule.EMIT_NOTHING;
		while (token == ScanRule.EMIT_NOTHING) {
			lastPos= pos; // beginning of the char range we will find
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
					throw new ParseException(ex.getClass().getName()+": "+ex.getMessage(), getContext(), curTokenPos());
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
				throw new ParseException("Scan error while processing "+states[state].stateName, getContext(), curTokenPos());
		}
	}

	/** Initialize the data for each state.
	 * <p>Each state gets "stateData" where temporary values can be stored.  This
	 * fnction can be used to initialize the state data for all the states used
	 * by your scan rules.
	 * @param newStateData Object[] An array of objects, one for each state
	 */
	public void initAllStateData(Object[] newStateData) {
		if (newStateData.length != states.length)
			throw new RuntimeException("Array length mismatch");
		stateData= newStateData;
	}

	/** Get the stateData for all states, as an array
	 *
	 * @return Object[] An array of the data for each state
	 */
	public Object[] getAllStateData() {
		return stateData;
	}

	/** Get the state data for the current state.
	 *
	 * @return Object This state's data
	 */
	public Object getStateData() {
		return stateData[state];
	}

	/** Set the data for the current state.
	 *
	 * @param newVal Object A value which will be associated with this state.
	 */
	public void setStateData(Object newVal) {
		stateData[state]= newVal;
	}

	/** Get the index of the current state.
	 * <p>State indicies are non-negative integers.
	 * @return int The index of the current state, 0..N
	 */
	public int getState() {
		return state;
	}

	/** Transition to the specified state.
	 * @param newState int The index of the new state.
	 */
	public void stateTrans(int newState) {
		if (newState < 0 || newState >= states.length)
			throw new ArrayIndexOutOfBoundsException();
		state= newState;
	}

	/** Get the index of the current line.
	 * <p>These values are 1-based by default, though the scan rules can set the
	 * line number to any value they choose.
	 * @return int The index of the current line.
	 */
	public int getLineNo() {
		return lineNum;
	}

	/** Increment the current line number.
	 */
	public void incLineNo() {
		lineNum++;
		lineStart= pos;
	}

	/** Set the current line number to an arbitrary value.
	 * <p>This value is normally a 1-based index of a line number, but can be any
	 * value.  It is used in getTokenPos(), reported in parseExceptions, and
	 * possibly examined by other scna rules.
	 *
	 * @param newVal int The new lineNo value
	 */
	public void setLineNo(int newVal) {
		lineNum= newVal;
		lineStart= pos;
	}
}
