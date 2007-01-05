import com.silverdirk.parser.*;
import com.silverdirk.parser.ReduceMethod;
import com.silverdirk.parser.Parser.Priorities;
import com.silverdirk.parser.Parser.Priorities.PriorityLevel;

import java.util.regex.*;
import java.io.*;

/**
 * <p>Project: Parser Demo</p>
 * <p>Title: Main ParseDemo Class</p>
 * <p>Description: Implements a simple arithmetic grammar and command line.</p>
 * <p>Copyright Copyright (c) 2006-2007</p>
 *
 * @author Michael Conrad / TheSilverDirk
 * @version $Revision$
 */
public class CalcDemo {
	public static void main(String[] args) throws Exception {
		CalcDemo demo= new CalcDemo();
		demo.run();
	}

	static class Function {
		double evalUnary(double a) { throw new UnsupportedOperationException(); }
		double evalBinary(double a, double b) { throw new UnsupportedOperationException(); }
	}
	static class Add extends Function {
		double evalBinary(double a, double b) { return a+b; }
		static Add INSTANCE= new Add();
	}
	static class Sub extends Function {
		double evalUnary(double a) { return -a; }
		double evalBinary(double a, double b) { return a-b; }
		static Sub INSTANCE= new Sub();
	}
	static class Mul extends Function {
		double evalBinary(double a, double b) { return a*b; }
		static Mul INSTANCE= new Mul();
	}
	static class Div extends Function {
		double evalBinary(double a, double b) { return a/b; }
		static Div INSTANCE= new Div();
	}
	static class Sin extends Function {
		double evalUnary(double a) { return Math.sin(a); }
		static Sin INSTANCE= new Sin();
	}
	static class Cos extends Function {
		double evalUnary(double a) { return Math.sin(a); }
		static Cos INSTANCE= new Cos();
	}

	static final ScanRuleSet scanRules= new ScanRuleSet("Default State", new ScanRule[] {
		new ScanRule("+", Add.INSTANCE),
		new ScanRule("-", Sub.INSTANCE),
		new ScanRule("*", Mul.INSTANCE),
		new ScanRule("/", Div.INSTANCE),
		new ScanRule("sin", Sin.INSTANCE),
		new ScanRule("cos", Cos.INSTANCE),
		new ScanRule("("),
		new ScanRule(")"),
		new ScanRule(Pattern.compile("[ \t]+"), ScanRule.EMIT_NOTHING),
		new ScanRule(Pattern.compile("\r?\n|\r")) {
			public Object onMatch(String text, Scanner sender) {
				sender.incLineNo();
				return ScanRule.EMIT_NOTHING;
			}
		},
		new ScanRule(Pattern.compile("([0-9]+(\\.[0-9]*)?|[0-9]*\\.[0-9]+)([Ee][+-]?[0-9]+)?")) {
			public Object onMatch(String text, Scanner sender) {
				return new Double(text);
			}
		}
	});

	static final Nonterminal
		Expr= new Nonterminal("Expression");

	static final ReduceMethod
		infixHandler= new ReduceMethod() {
			public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
				double a= ((Double)symbols[0]).doubleValue();
				double b= ((Double)symbols[2]).doubleValue();
				Function op= (Function)symbols[1];
				return new Double(op.evalBinary(a, b));
			}
		},
		unaryHandler= new ReduceMethod() {
			public Object reduce(ParseRule rule, SourcePos from, Object[] symbols) {
				double a= ((Double)symbols[1]).doubleValue();
				Function op= (Function)symbols[0];
				return new Double(op.evalUnary(a));
			}
		};

	static ParseRule parensRule, addRule, subRule, mulRule, divRule, negRule, sinRule, cosRule;
	static ParseRule[] parseRules= new ParseRule[] {
		addRule= new ParseRule(Expr, new Object[] {Expr, Add.INSTANCE, Expr}, infixHandler),
		subRule= new ParseRule(Expr, new Object[] {Expr, Sub.INSTANCE, Expr}, infixHandler),
		mulRule= new ParseRule(Expr, new Object[] {Expr, Mul.INSTANCE, Expr}, infixHandler),
		divRule= new ParseRule(Expr, new Object[] {Expr, Div.INSTANCE, Expr}, infixHandler),
		negRule= new ParseRule(Expr, new Object[] {Sub.INSTANCE, Expr}, unaryHandler),
		sinRule= new ParseRule(Expr, new Object[] {Sin.INSTANCE, Expr}, unaryHandler),
		cosRule= new ParseRule(Expr, new Object[] {Cos.INSTANCE, Expr}, unaryHandler),
		parensRule= new ParseRule(Expr, new Object[] {"(", Expr, ")"}, new ParseRule.PassthroughHandler(1)),
		new ParseRule(Expr, new Object[] {Double.class}, ParseRule.FIRSTELEM_PASSTHROUGH),
	};
	static Priorities rulePri= new Priorities(new PriorityLevel[] {
		new PriorityLevel(new ParseRule[] {addRule, subRule}, Priorities.LEFT, 1),
		new PriorityLevel(new ParseRule[] {mulRule, divRule}, Priorities.LEFT, 2),
		new PriorityLevel(new ParseRule[] {negRule, sinRule, cosRule}, Priorities.LEFT, 3),
		new PriorityLevel(new ParseRule[] {parensRule}, Priorities.LEFT, 4)
	});

	Parser parser;

	public CalcDemo() {
		parser= new Parser(new Grammar(Expr, parseRules, rulePri));
	}

	public void run() throws Exception {
		BufferedReader lineIn= new BufferedReader(new InputStreamReader(System.in));
		PrintWriter lineOut= new PrintWriter(System.out);
		String input;
		do {
			lineOut.write("> ");
			lineOut.flush();
			input= lineIn.readLine();
			if (input != null && !input.equals("quit") && !input.equals("exit")) {
				try {
					Scanner scanner= new Scanner(new ScanRuleSet[] {scanRules}, input);
					Object result= parser.parse(scanner);
					lineOut.println(result);
					lineOut.println();
				}
				catch (ParseException ex) {
					lineOut.print(ex.getFullMessage());
				}
				lineOut.flush();
			}
			else break;
		} while (true);
	}
}
