import com.silverdirk.parser.*;
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
public class ParseDemo {
	public static void main(String[] args) throws Exception {
		ParseDemo demo= new ParseDemo();
		demo.run();
	}

	static final ScanRuleSet scanRules= new ScanRuleSet("Default State", new ScanRule[] {
		new ScanRule("+"),
		new ScanRule("-"),
		new ScanRule("*"),
		new ScanRule("/"),
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

	static ParseRule parens, add, sub, mul, div, neg;
	static ParseRule[] parseRules= new ParseRule[] {
		add= new ParseRule(Expr, new Object[] {Expr, "+", Expr}),
		sub= new ParseRule(Expr, new Object[] {Expr, "-", Expr}),
		mul= new ParseRule(Expr, new Object[] {Expr, "*", Expr}),
		div= new ParseRule(Expr, new Object[] {Expr, "/", Expr}),
		neg= new ParseRule(Expr, new Object[] {"-", Expr}),
		parens= new ParseRule(Expr, new Object[] {"(", Expr, ")"}, new ParseRule.PassthroughHandler(1)),
		new ParseRule(Expr, new Object[] {Double.class}),
	};
	static Priorities rulePri= new Priorities(new PriorityLevel[] {
		new PriorityLevel(new ParseRule[] {add, sub}, Priorities.LEFT, 1),
		new PriorityLevel(new ParseRule[] {mul, div}, Priorities.LEFT, 2),
		new PriorityLevel(new ParseRule[] {neg}, Priorities.LEFT, 3),
		new PriorityLevel(new ParseRule[] {parens}, Priorities.LEFT, 4)
	});

	Parser parser;

	public ParseDemo() {
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
					Object parseTree= parser.parse(scanner);
					((GenericParseNode) parseTree).displayTree(lineOut);
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
