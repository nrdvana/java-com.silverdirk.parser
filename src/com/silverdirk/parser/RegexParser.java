package com.silverdirk.parser;

import java.util.*;
import com.silverdirk.datastruct.SetOfChar;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Regex Parser</p>
 * <p>Description: Simple hand-coded LL parser for regexes</p>
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Soon to be used in the implementation of Scanner.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class RegexParser {
	char[] pattern;
	int pos, eofPos, groupCount;
	static final SetOfChar ALL_CHARS;
	static {
		ALL_CHARS= new SetOfChar();
		ALL_CHARS.add(new char[] {Character.MIN_VALUE, Character.MAX_VALUE}, new char[0]);
	}
	static final int
		ZERO_OR_GREATER= -1,
		ONE_OR_GREATER= -2,
		ZERO_OR_ONE= -3;

	private RegexParser(String patternString) {
		// append a bit of garbage so we can make some fun assumptions and
		//  not have to check for pos > length quite as often
		this.pattern= (patternString+")\0").toCharArray();
		pos= 0;
		groupCount= 0;
		eofPos= pattern.length-2;
	}

	public static final NFAGraph parse(String regexPattern) throws ParseException {
		RegexParser parser= new RegexParser(regexPattern);
		NFAGraph result= parser.parseExpr(false);
		if (parser.pos < parser.eofPos)
			throw parser.makeParseException("Extra characters at end of pattern");
		return result;
	}

	public static final String escapeString(String literal) {
		boolean safe= true;
		int i= 0;
		int stop=literal.length();
		for (; i<stop; i++)
			if (needEsc(literal.charAt(i))) {
				safe= false;
				break;
			}
		if (safe)
			return literal;
		else {
			StringBuffer ret= new StringBuffer((stop-i)*2);
			ret.append(literal.substring(0, i));
			for (; i<stop; i++) {
				char ch= literal.charAt(i);
				if (needEsc(ch))
					ret.append('\\');
				ret.append(ch);
			}
			return ret.toString();
		}
	}
	private static final boolean needEsc(char c) {
		switch (c) {
		case '(': case ')': case '{': case '}':
		case '*': case '+': case '?': case '.':
		case '|': case '[': case ']': case '\\':
			return true;
		default:
			return false;
		}
	}

	// A quick and dirty LL(1) parser.. but the regex language is pretty simple.
	NFAGraph parseExpr(boolean isSubExpr) {
		ArrayList options= new ArrayList();
		ArrayList sequence= new ArrayList();
		char ch= 0;
		loop: while (true) {
			switch (ch= pattern[pos++]) {
			case '(': sequence.add(new RegexGroup(groupCount++, parseExpr(true))); break;
			case '{': makeRepetition(sequence, parseGroupCount()); break;
			case '*': makeRepetition(sequence, ZERO_OR_GREATER); break;
			case '+': makeRepetition(sequence, ONE_OR_GREATER); break;
			case '?': makeRepetition(sequence, ZERO_OR_ONE); break;
			case '.': sequence.add(new SimpleTransition(ALL_CHARS)); break;
			case '|': options.add(makeSequence(sequence)); sequence.clear(); break;
			case '[': sequence.add(new SimpleTransition(parseCharSet())); break;
			case ')': break loop;
			case '\\':
				ch= pattern[pos++]; // fall through to next
			case '\0': // its an allowed char, but hints us that we might be at EOF.
				if (pos > eofPos)
					throw makeParseException((isSubExpr?"Subexpression":"Pattern")+" was incorrectly terminated.");
				// otherwise fall through
			default:
				sequence.add(new SimpleTransition(new SetOfChar(ch, ch)));
			}
		}
		if (options.size() > 0) {
			options.add(makeSequence(sequence));
			return new RegexOption((NFAGraph[])options.toArray(new NFAGraph[options.size()]));
		}
		else
			return makeSequence(sequence);
	}

	static final NFAGraph LambdaEdge= new SimpleTransition(NFAGraph.LAMBDA);

	private NFAGraph makeSequence(List sequence) {
		if (sequence.size() == 1)
			return (NFAGraph) sequence.get(0);
		return new RegexSequence((NFAGraph[])sequence.toArray(new NFAGraph[sequence.size()]));
	}

	private void makeRepetition(List currentResult, int count) {
		int idx= currentResult.size()-1;
		if (idx < 0) throw makeParseException("Bad placement of repetition ( *, +, ?, or {#} ) in pattern; these operators must follow a character or set of characters or group");
		NFAGraph prev= (NFAGraph) currentResult.get(idx);
		NFAGraph replacement;
		if (count > 0)
			replacement= new RegexRepetition(count, prev);
		else if (count == ZERO_OR_ONE)
			replacement= new RegexOption(new NFAGraph[] {LambdaEdge, prev});
		else
			replacement= new RegexLoop(count==ZERO_OR_GREATER, prev);
		currentResult.set(idx, replacement);
	}

	SetOfChar parseCharSet() {
		StringBuffer ranges= new StringBuffer(), singles= new StringBuffer();
		final int NONE= -1;
		int lastChar= NONE, swChar= 0;
		boolean invert= (pattern[pos] == '^');
		if (invert) pos++;
		int first= pos;
		do {
			switch (swChar= pattern[pos++]) {
			case '\0':
				if (pos > eofPos)
					throw makeParseException("Unterminated set of characters from position "+(first-1)+" ( expecting ']' )");
			case '-':
				// Note that the way I wrote this means that you can get
				// a '-' into some rather unusual places, like "A-Z-qwerty"
				// or "A--".  This won't break any existing expressions, but
				// will allow more liberal usage of '-' than the standard.
				if (/*pos != first+1 &&*/ lastChar != NONE && pattern[pos] != ']' && pattern[pos] != '\0') {
					int a= lastChar, b= pattern[pos++];
					if (a > b) { a^= b; b^= a; a^= b; /* hackish "swap" implementation */}
					ranges.append((char)a);
					ranges.append((char)b);
					lastChar= NONE;
					break;
				}
				// else fall through
			default:
				if (lastChar != NONE) singles.append((char)lastChar);
				lastChar= swChar;
			}
		} while (swChar != ']' || pos == first+1);
		// there is still a char in "lastChar", but it will be ']'.
		SetOfChar result= new SetOfChar();
		result.add(ranges.toString().toCharArray(), singles.toString().toCharArray());
		if (invert) result.invert();
		return result;
	}

	int parseGroupCount() {
		int number= 0;
		char ch;
		while ((ch=pattern[pos++]) >= '0' && ch <= '9') {
			int tmp= number*10 + (ch-'0');
			if (tmp < number)
				throw makeParseException("Repetition count too large. (limited by 'int')");
			number= tmp;
		}
		if (ch != '}')
			throw makeParseException("Invalid character in \"{#}\" repetition notation: "+ch);
		if (number == 0)
			throw makeParseException("Zero is not a valid count for \"{#}\" repetition notation.");
		return number;
	}

	ParseException makeParseException(String msg) {
		return new ParseException(msg, new String(pattern, 0, pattern.length-2), new SourcePos(1, pos, 1, pos));
	}
}

class SimpleTransition extends NFAGraph {
	SetOfChar transitionChars;

	public SimpleTransition(SetOfChar transitionChars) {
		this.transitionChars= transitionChars;
		groupCount= 0;
	}

	int nodeCount() {
		return 1;
	}

	int getExitCount(int nodeId) {
		checkNodeId(nodeId, 1);
		return 1;
	}

	int getExitPeer(int nodeId, int exitId) {
		checkNodeId(nodeId, 1);
		checkExitId(exitId, 1);
		return 1;
	}

	SetOfChar getTransitionChar(int nodeId, int exitId) {
		checkNodeId(nodeId, 1);
		checkExitId(exitId, 1);
		return transitionChars;
	}

	int getGroupCode(int nodeId) {
		return 0;
	}
}

class RegexRepetition extends NFAGraph {
	int loopCount;
	NFAGraph content;
	int contentNodeCount;

	public RegexRepetition(int count, NFAGraph content) {
		loopCount= count;
		this.content= content;
		contentNodeCount= content.nodeCount();
		groupCount= content.getGroupCount();
	}

	int nodeCount() {
		return contentNodeCount*loopCount;
	}

	int getExitCount(int nodeId) {
		checkNodeId(nodeId, nodeCount());
		return content.getExitCount(nodeId%contentNodeCount);
	}

	int getExitPeer(int nodeId, int exitId) {
		checkNodeId(nodeId, nodeCount());
		int subNodeIdx= nodeId%contentNodeCount;
		return content.getExitPeer(subNodeIdx, exitId)+(nodeId-subNodeIdx);
	}

	SetOfChar getTransitionChar(int nodeId, int exitId) {
		checkNodeId(nodeId, nodeCount());
		return content.getTransitionChar(nodeId%contentNodeCount, exitId);
	}

	int getGroupCode(int nodeId) {
		checkNodeId(nodeId, nodeCount());
		return content.getGroupCode(nodeId%contentNodeCount);
	}
}

class RegexLoop extends NFAGraph {
	int loopNodeIdx;
	int contentOffset;
	NFAGraph content;

	public RegexLoop(boolean zeroAllowed, NFAGraph content) {
		this.loopNodeIdx= zeroAllowed? 0 : content.nodeCount();
		this.contentOffset= zeroAllowed? 1 : 0;
		this.content= content;
		groupCount= content.getGroupCount();
	}

	int nodeCount() {
		return 1+content.nodeCount();
	}

	int getExitCount(int nodeId) {
		return (nodeId == loopNodeIdx)? 2 : content.getExitCount(nodeId-contentOffset);
	}

	int getExitPeer(int nodeId, int exitId) {
		if (nodeId == loopNodeIdx)
			switch (exitId) {
			case 0: return contentOffset;
			case 1: return 1+content.nodeCount();
			default:
				checkExitId(exitId, 2); // throws a more descritpive RuntimeException
				throw new RuntimeException(); // satisfies the compiler
			}
		else
			return content.getExitPeer(nodeId-contentOffset, exitId)+contentOffset;
	}

	SetOfChar getTransitionChar(int nodeId, int exitId) {
		if (nodeId == loopNodeIdx)
			return LAMBDA;
		else
			return content.getTransitionChar(nodeId-contentOffset, exitId);
	}

	int getGroupCode(int nodeId) {
		if (nodeId == loopNodeIdx)
			return 0;
		else
			return content.getGroupCode(nodeId-contentOffset);
	}
}

class RegexSequence extends NFAGraph {
	int loopCount;
	NFAGraph[] content;
	int[] subgraphOffsets;
	int totalNodes;

	public RegexSequence(NFAGraph[] content) {
		this.content= content;
		subgraphOffsets= new int[content.length];
		totalNodes= 0;
		groupCount= 0;
		for (int i=0; i<content.length; i++) {
			subgraphOffsets[i]= totalNodes;
			totalNodes+= content[i].nodeCount();
			groupCount+= content[i].getGroupCount();
		}
	}

	int nodeCount() {
		return totalNodes;
	}

	int getExitCount(int nodeId) {
		int gIdx= getSubgraphIdx(nodeId);
		return content[gIdx].getExitCount(nodeId-subgraphOffsets[gIdx]);
	}

	int getExitPeer(int nodeId, int exitId) {
		int gIdx= getSubgraphIdx(nodeId);
		return content[gIdx].getExitPeer(nodeId-subgraphOffsets[gIdx], exitId)+subgraphOffsets[gIdx];
	}

	SetOfChar getTransitionChar(int nodeId, int exitId) {
		int gIdx= getSubgraphIdx(nodeId);
		return content[gIdx].getTransitionChar(nodeId-subgraphOffsets[gIdx], exitId);
	}

	int getGroupCode(int nodeId) {
		int gIdx= getSubgraphIdx(nodeId);
		return content[gIdx].getGroupCode(nodeId-subgraphOffsets[gIdx]);
	}

	int getSubgraphIdx(int nodeId) {
		int min= 0, max= subgraphOffsets.length-1, mid;
		while (min < max) {
			mid= (min+max+1) >>> 1;
			if (nodeId < subgraphOffsets[mid]) max= mid-1;
			else min= mid;
		}
		return max;
	}
}

class RegexOption extends RegexSequence {
	public RegexOption(NFAGraph[] content) {
		super(content);
	}

	int nodeCount() {
		return totalNodes+1;
	}

	int getExitCount(int nodeId) {
		return (nodeId == 0)? content.length : super.getExitCount(nodeId-1);
	}

	int getExitPeer(int nodeId, int exitId) {
		if (nodeId == 0)
			return subgraphOffsets[exitId]+1;
		else
			return super.getExitPeer(nodeId-1, exitId)+1;
	}

	SetOfChar getTransitionChar(int nodeId, int exitId) {
		return (nodeId == 0)? LAMBDA : super.getTransitionChar(nodeId-1, exitId);
	}

	int getGroupCode(int nodeId) {
		return (nodeId == 0)? 0 : super.getGroupCode(nodeId-1);
	}
}

class RegexGroup extends NFAGraph {
	NFAGraph content;
	int groupId;
	int endNodeIdx;

	public RegexGroup(int groupId, NFAGraph content) {
		this.groupId= groupId;
		this.content= content;
		endNodeIdx= 1+content.nodeCount();
		groupCount= content.getGroupCount()+1;
	}

	int nodeCount() {
		return 2+content.nodeCount();
	}

	int getExitCount(int nodeId) {
		if (nodeId == 0 || nodeId == endNodeIdx)
			return 1;
		else
			return content.getExitCount(nodeId-1);
	}

	int getExitPeer(int nodeId, int exitId) {
		if (nodeId == 0 || nodeId == endNodeIdx)
			return nodeId+1;
		else
			return content.getExitPeer(nodeId-1, exitId)+1;
	}

	SetOfChar getTransitionChar(int nodeId, int exitId) {
		if (nodeId == 0 || nodeId == endNodeIdx)
			return LAMBDA;
		else
			return content.getTransitionChar(nodeId-1, exitId);
	}

	int getGroupCode(int nodeId) {
		if (nodeId == 0)
			return groupId;
		else if (nodeId == endNodeIdx)
			return -groupId;
		else
			return content.getGroupCode(nodeId-1);
	}
}
