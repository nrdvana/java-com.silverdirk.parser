package com.silverdirk.parser;

import java.util.*;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Regex Specification</p>
 * <p>Description: Object tree representing the components of a regex</p>
 * <p>Copyright: Copyright (c) 2006-2006</p>
 *
 * Soon to be used in the implementation of Scanner.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class RegexSpec {
	Object spec;

	public RegexSpec(String regexPattern) {
		spec= regexPattern;
	}

	public Object getSpec() {
		compile();
		return spec;
	}

	public void compile() {
		if (spec instanceof String)
			spec= new PatternParser((String)spec).parse();
	}

	static class ParseException extends RuntimeException {
		ParseException(String msg) { super(msg); }
	}

	private static class PatternParser {
		char[] pattern;
		int pos, eofPos;

		public PatternParser(String patternString) {
			// append a bit of garbage so we can make some fun assumptions and
			//  not have to check for pos > length quite as often
			this.pattern= (patternString+")\0").toCharArray();
			eofPos= pattern.length-2;
		}

		Object parse() {
			pos= 0;
			Object result= parseExpr(false);
			if (pos < eofPos)
				throw new ParseException("At "+pos+": Extra characters at end of pattern");
			return result;
		}

		// A quick and dirty LL(1) parser.. but the regex language is pretty simple.
		Object parseExpr(boolean isSubExpr) {
			ArrayList options= new ArrayList();
			ArrayList sequence= new ArrayList();
			char ch= 0;
			loop: while (true) {
				switch (ch= pattern[pos++]) {
				case '[': sequence.add(parseCharSet()); break;
				case '{': makeRepetition(sequence, parseGroupCount()); break;
				case '(': sequence.add(new RegexGroup(parseExpr(true))); break;
				case '*': makeRepetition(sequence, RegexRepetition.ZERO_OR_GREATER); break;
				case '?': makeRepetition(sequence, RegexRepetition.ZERO_OR_ONE); break;
				case '+': makeRepetition(sequence, RegexRepetition.ONE_OR_GREATER); break;
				case '.': sequence.add(RegexCharSet.ALL); break;
				case '|': options.add(sequence); sequence= new ArrayList(); break;
				case ')': break loop;
				case '\\':
					ch= pattern[pos++]; // fall through to next
				case '\0': // its an allowed char, but hints us that we might be at EOF.
					if (pos > eofPos)
						throw new ParseException("At "+pos+": "+(isSubExpr?"subexpression":"pattern")+" was incorrectly terminated.");
					// otherwise fall through
				default:
					sequence.add(new Character(ch));
				}
			}
			if (options.size() > 0) {
				options.add(sequence);
				return new RegexOption(options.toArray());
			}
			else if (sequence.size() == 1)
				return sequence.get(0);
			else
				return sequence.toArray();
		}
		private void makeRepetition(List currentResult, int count) {
			int idx= currentResult.size()-1;
			if (idx < 0) throw new ParseException("At "+pos+": Bad placement of repetition ( +, *, or {#} ) in pattern.");
			Object prev= currentResult.get(idx);
			Object replacement;
			if (prev instanceof RegexRepetition)
				replacement= ((RegexRepetition)prev).combine(count);
			else
				replacement= new RegexRepetition(count, prev);
			currentResult.set(idx, replacement);
		}

		RegexCharSet parseCharSet() {
			StringBuffer ranges= new StringBuffer(), singles= new StringBuffer();
			final int NONE= 0x1000;
			int lastChar= NONE, swChar= 0;
			boolean isRange= false, invert= (pattern[pos] == '^');
			if (invert) pos++;
			int first= pos;
			do {
				switch (swChar= pattern[pos++]) {
				case '-':
					// Note that the way I wrote this means that you can get
					// a '-' into some rather unusual places, like "A-Z-qwerty"
					// or "A--".  This won't break any existing expressions, but
					// will allow more liberal usage of '-' than the standard.
					if (/*pos != first+1 &&*/ lastChar != NONE && pattern[pos] != ']' && pattern[pos] != '\0') {
						int a= lastChar, b= pattern[pos++]&0xFFFF;
						if (a > b) { int temp= a; a= b; b= temp; }
						ranges.append((char)a);
						ranges.append((char)b);
						lastChar= NONE;
						break;
					}
					// else fall through
				case '\0':
					if (pos > eofPos)
						throw new ParseException("At "+pos+": unterminated set of characters from position "+(first-1)+" ( expecting ']' )");
				default:
					if (lastChar != NONE) singles.append((char)lastChar);
					lastChar= swChar&0xFFFF;
				}
			} while (swChar != ']' || pos == first+1);
			// there is still a char in "lastChar", but it will be ']'.
			RegexCharSet result= new RegexCharSet(ranges.toString().toCharArray(), singles.toString().toCharArray());
			if (invert) result.invert();
			return result;
		}

		int parseGroupCount() {
			int number= 0;
			char ch;
			while ((ch=pattern[pos++]) >= '0' && ch <= '9')
				if (number >= (0x7FFFFFFF-9)/10)
					throw new ParseException("At "+pos+": Integer value too large.");
				else
					number= number*10 + (ch-'0');
			if (number == 0)
				throw new ParseException("At "+pos+": Invalid count for \"{#}\" construct.");
			if (ch != '}')
				throw new ParseException("At "+pos+": Invalid character while reading \"{#}\" construct.");
			return number;
		}
	}
}

class RegexRepetition {
	int count;
	Object content;
	boolean greedy= true;

	public RegexRepetition(int count, Object content) {
		this.count= count;
		this.content= content;
	}

	public RegexRepetition combine(int appendedCode) {
		if (appendedCode == ZERO_OR_ONE && (count == ZERO_OR_GREATER || count == ONE_OR_GREATER)) {
			greedy= false;
			return this;
		}
		else return new RegexRepetition(appendedCode, this);
	}

	static final int
		ZERO_OR_GREATER= 0,
		ONE_OR_GREATER= -1,
		ZERO_OR_ONE= -2;
}

class RegexGroup {
	Object content;

	public RegexGroup(Object content) {
		this.content= content;
	}
}

class RegexOption {
	Object[] options;

	public RegexOption(Object[] options) {
		this.options= options;
	}
}

class RegexCharSet {
	int[] ranges; // start char in upper two bytes, range end in lower two
	int hash= 0;

	public RegexCharSet(char[] rangePairs, char[] singles) {
		ranges= new int[(rangePairs.length>>1) + singles.length];
		int destIdx= 0;
		for (int i=0; i<rangePairs.length-1; i+=2)
			ranges[destIdx++]= ((rangePairs[i]&0xFFFF) << 16) | (rangePairs[i+1]&0xFFFF);
		for (int i=0; i<singles.length; i++)
			ranges[destIdx++]= ((singles[i]&0xFFFF) << 16) | (singles[i]&0xFFFF);
		normalize();
	}

	public void invert() {
		boolean startSpan= ((ranges[0]>>>16) != 0);
		boolean endSpan= ((ranges[ranges.length-1]&0xFFFF) != 0xFFFF);
		int[] newRanges= new int[ranges.length-1+(startSpan? 1:0)+(endSpan? 1:0)];
		int newIdx= 0;
		if (startSpan)
			newRanges[newIdx++]= (ranges[0]>>>16)-1;
		for (int i=1; i<ranges.length; i++)
			newRanges[newIdx++]= ((ranges[i-1]+1)<<16) | (ranges[i]>>>16)-1;
		if (endSpan)
			newRanges[newIdx++]= ((ranges[ranges.length-1]+1)<<16) | 0xFFFF;
		ranges= newRanges;
		hash= 0;
	}

	public boolean equals(Object other) {
		return (other instanceof RegexCharSet) && equals((RegexCharSet)other);
	}

	public boolean equals(RegexCharSet other) {
		if (other.ranges.length != ranges.length) return false;
		for (int i=0; i<ranges.length; i++)
			if (other.ranges[i] != ranges[i]) return false;
		return true;
	}

	public int hashCode() {
		if (hash == 0) { // yeah it could hash to 0, but its unlikely, and would only be a small performance hit
			int accum= ~ranges.length;
			for (int i=0; i<ranges.length; i++)
				accum+= ranges[i];
			hash= accum;
		}
		return hash;
	}

	private void normalize() {
		sort(0, ranges.length-1);
		int prev= 0;
		for (int i=1; i<ranges.length; i++) {
			int prevEnd= ranges[prev]&0xFFFF, start= ranges[i]>>>16, end= ranges[i]&0xFFFF;
			// if ranges overlap or concatenate, merge them
			if (prevEnd+1 >= start) {
				ranges[prev]= (ranges[prev]&0xFFFF0000) | Math.max(prevEnd, end);
				ranges[i]= INVALID_RANGE;
			}
			else {
				prev++;
				ranges[prev]= ranges[i];
			}
		}
		int newCount= prev+1;
		if (newCount != ranges.length) {
			int[] newRanges= new int[newCount];
			System.arraycopy(ranges, 0, newRanges, 0, newCount);
			ranges= newRanges;
		}
	}

	// quicksort, on the start end of the ranges
	private void sort(int low, int high) {
		int temp;
		if (low >= high) return;
		if (low+1 == high) {
			if ((ranges[low] >>> 1) > (ranges[high] >>> 1))
				swap(low, high);
			return;
		}
		int greaterIdx= high+1, lessIdx= low-1;
		long pivot= ((ranges[low] >>> 1) + (ranges[high] >>> 1)) >> 1;
		while (true) {
			do { lessIdx++; } while ((ranges[lessIdx]>>>1) < pivot);
			do { greaterIdx--; } while ((ranges[greaterIdx]>>>1) > pivot);
			if (lessIdx >= greaterIdx) break;
			swap(lessIdx, greaterIdx);
		}
		sort(low, lessIdx-1);
		sort(lessIdx, high);
	}

	private final void swap(int idx, int idx2) {
		int temp= ranges[idx];
		ranges[idx]= ranges[idx2];
		ranges[idx2]= temp;
	}

	public static final RegexCharSet ALL= new RegexCharSet(new char[] { Character.MIN_VALUE, Character.MAX_VALUE }, new char[0]);
	private static final int INVALID_RANGE= 0x00010000;
}
