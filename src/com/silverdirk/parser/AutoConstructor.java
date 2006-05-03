package com.silverdirk.parser;

import java.lang.reflect.*;

/**
 * <p>Project: 42</p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004-2005</p>
 *
 * @author not attributable
 * @version $Revision$
 */
public class AutoConstructor implements Parser.ProductionHandler {
	Class targetClass;
	Constructor cons;
	int consType;

	public AutoConstructor(Class target) {
		targetClass= target;
		findValidConstructor();
	}

	private void findValidConstructor() {
		Class[][] paramOptions= new Class[][] {
			new Class[] {Object[].class},
			new Class[] {SourcePos.class, Object[].class},
			new Class[] {ParseRule.class, Object[].class},
			new Class[] {ParseRule.class, SourcePos.class, Object[].class}
		};
		for (int i=paramOptions.length-1; i>=0; i--)
			try {
				cons= targetClass.getConstructor(paramOptions[i]);
				consType= i;
				return;
			}
			catch (NoSuchMethodException nsmex2) {
			}
		throw new RuntimeException("Class "+targetClass.getName()+" has no valid constructor for making reductions.");
	}

	public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
		try {
			switch (consType) {
			case 0: return cons.newInstance(new Object[] {symbols});
			case 1: return cons.newInstance(new Object[] {from, symbols});
			case 2: return cons.newInstance(new Object[] {rule, symbols});
			case 3: return cons.newInstance(new Object[] {rule, from, symbols});
			default:
				throw new RuntimeException();
			}
		}
		catch (Exception ex) {
			if (ex instanceof RuntimeException)
				throw (RuntimeException) ex;
			else
				throw new RuntimeException(ex);
		}
	}
}