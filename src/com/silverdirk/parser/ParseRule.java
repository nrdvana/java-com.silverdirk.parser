package com.silverdirk.parser;

import java.util.*;

/**
 * <p>Project: Dynamic LR(1) Parsing Library</p>
 * <p>Title: Parse Rule</p>
 * <p>Description: Represents one production of the form "Symbol ::= token [, ...]"</p>
 * <p>Copyright: Copyright (c) 2004-2006</p>
 *
 * <p>This is an immutable class representing a production rule.
 * It is immutable, to save the effort of making a mechanism to invalidate
 * the parse tables, and also because I don't see a reason to alter a
 * production rule instead of creating a new one.
 *
 * <p>ParseRule has one method in particular that will be frequently overridden:
 * <code>  Object reduce(SourcePos from, Object[] symbols);  </code>
 * This is the method that the parser calls when the rule has matched a series
 * of symbols and should be reduced to its target, and any processing that
 * needs to be done can be placed in a subclass that overrides this method.
 *
 * <p>As an alternative, you can create an instance of ReduceMethod which
 * implements
 * <code>  Object reduce(ParseRule rule, SourcePos from, Object[] symbols);  </code>
 * and then pass this object to the ParseRule constructor.
 *
 * @author Michael Conrad
 * @version $Revision$
 */
public class ParseRule implements ReduceMethod {
	Nonterminal target;
	Object[] symbols;
	ReduceMethod handler;

	/**
	 * Create a parser production specifying that the target can be created
	 * from an instance of the specified symbols, using the function provided
	 * by the handler.
	 *
	 * @param target The nonterminal symbol that is produced by this rule
	 * @param symbols The specific values (or classes of values) which must be matched
	 * @param handler The function whick converts the symbol values into an object representing the nonterminal
	 */
	public ParseRule(Nonterminal target, Object[] symbols, ReduceMethod handler) {
		this.target= target;
		this.symbols= (Object[]) symbols.clone();
		this.handler= handler;
	}

	/**
	 * Create a parser production rule with the specified target and symbols,
	 * using a default handler that generates GenericParseNode objects.
	 *
	 * @param target Same as main constructor
	 * @param symbols Same as main constructor
	 */
	public ParseRule(Nonterminal target, Object[] symbols) {
		this(target, symbols, null);
	}

	/** Get the rule target,
	 * <p>This function returns the nonterminal that this rule produces.
	 * @return the nonterminal symbol product of the rule
	 */
	public Nonterminal getNonterminal() {
		return target;
	}

	/** Get the rule symbol list.
	 * <p>This function creates a copy of the list of symbols that make up the body of the rule
	 * @return a copy of the symbol list
	 */
	public Object[] getSymbols() {
		return (Object[]) symbols.clone();
	}

	/** Reduce the given symbols to an object.
	 * <p>The parser calls this when it has matched this rule and wants to reduce
	 * it to an object to use in other rules.  If a ReduceMethod was given to
	 * the constructor, this function calls it.  Otherwise the default action
	 * is to create a GenericParseNode.
	 *
	 * @param from SourcePos The area in the source where this rule is applying
	 * @param symbols Object[] The symbols that were parsed according to this rule
	 * @return Object An object representing the target of this rule
	 */
	public Object reduce(SourcePos from, Object[] symbols) {
		if (handler != null)
			return handler.reduce(this, from, symbols);
		else
			return new GenericParseNode(target, from, symbols);
	}

	/** Implementation of interface ReduceMethod.
	 * <p>This allows ParseRules to be used as instances of ReduceMethod.
	 *
	 * @param rule ParseRule Ignored. This parse rule should not have expectations of which rule was processed
	 * @param from SourcePos The area in the source where this rule is applying
	 * @param symbols Object[] The symbols that were parsed according to this rule
	 * @return Object An object representing the target of this rule
	 */
	public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
		return reduce(from, symbols);
	}

	public String toString() {
		StringBuffer result= new StringBuffer();
		result.append(target.name).append(" ::=");
		for (int i=0; i<symbols.length; i++)
			if (symbols[i] instanceof Nonterminal)
				result.append(" ").append(symbols[i]);
			else
				result.append(" '").append(symbols[i]).append("'");
		return result.toString();
	}
}
