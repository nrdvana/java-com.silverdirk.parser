import javax.servlet.http.HttpServlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.regex.*;
import com.silverdirk.parser.*;

/**
 * <p>Project: </p>
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright Copyright (c) 2004</p>
 *
 * @author Michael Conrad / TheSilverDirk
 * @version $Revision$
 */
public class Demo extends HttpServlet {
	enum State {
		edit, scanTest, parseTest;
	}
	static final String[] tabNames= new String[] { "Edit Rules", "Scanner Test", "Parser Test" };

	static class Fields {
		State state, action;
		String scanRules;
		String parseRules;
		String parseTableStr;
		String input;
		String exampleRequested;
		boolean debug;

		public Fields(HttpServletRequest req) {
			state= State.edit;
			if (req.getParameter("state") != null)
				try {
					state= State.valueOf(req.getParameter("state"));
				}
				catch (Exception ex) {}
			action= null;
			String actionStr= req.getParameter("action");
			if (actionStr != null)
				for (int i=0; i<tabNames.length; i++)
					if (actionStr.equals(tabNames[i]))
						action= State.values()[i];
			scanRules= Util.trimPossibleNull(req.getParameter("ScanRules"));
			parseRules= Util.trimPossibleNull(req.getParameter("ParseRules"));
			parseTableStr= Util.trimPossibleNull(req.getParameter("ParseTable"));
			input= Util.trimPossibleNull(req.getParameter("Input"));
			if (action != null)
				state= action;
			exampleRequested= req.getParameter("example");
			if (exampleRequested != null) {
				if (exampleRequested.equals("1")) {
					scanRules= "ASSIGN =\n"
						+"LPAREN  \\(\n"
						+"RPAREN  \\)\n"
						+"LBRAK  \\[\n"
						+"RBRAK  \\]\n"
						+"COMMA  ,\n"
						+"IDENT  [A-Za-z_]\\w*\n"
						+"NUMBR  \\d+\n"
						+"NEWLN  \\r?\\n|\\r\n"
						+"DOT    \\.\n"
						+"   [ \\t]";
					parseRules= "StmtList ::= Stmt NEWLN StmtList | Stmt |\n"
						+"Stmt ::= Assign | Invoke\n"
						+"Expr ::= Assign | ExprUnit\n"
						+"#1:L Expr ::= Expr ADD Expr | Expr SUB Expr\n"
						+"#2:L Expr ::= Expr MUL Expr | Expr DIV Expr\n"
						+"ExprUnit ::= Invoke | ScopedExpr | SubscriptExpr | IDENT | NUMBR | LPAREN Expr RPAREN\n"
						+"Assign ::= ExprUnit ASSIGN Expr\n"
						+"Invoke ::= ExprUnit LPAREN List RPAREN\n"
						+"ScopedExpr ::= ExprUnit DOT IDENT\n"
						+"SubscriptExpr ::= ExprUnit LBRAK Expr RBRAK\n"
						+"List ::= Expr COMMA List | Expr | Expr NEWLN | NEWLN List |";
				}
			}
			debug= req.getParameter("debug") != null;
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		HtmlGL hgl= new HtmlGL(req, resp);
		try {
			Fields fields= new Fields(req);
			hgl.beginPage("Parser Demo", new String[] { "Page.css", "Widgets.css" });
			if (fields.debug)
				hgl.pDebugData();
			hgl.p("\n<form action='index.html' method='post'><div class='content'>"
				+"\n<input type='hidden' name='state' value='").p(fields.state.name()).p("'/>\n");
			hgl.beginTabControl("action", tabNames, fields.state.ordinal(), 2);
			switch (fields.state) {
			case edit:      renderEditPage(fields, hgl); break;
			case scanTest:  renderScanTestPage(fields, hgl); break;
			case parseTest: renderParseTestPage(fields, hgl); break;
			}
		}
		catch (UserException ex) {
			ex.render(hgl);
		}
		catch (Exception ex) {
			hgl.pException(ex);
		}
		hgl.endTabControl();
		hgl.p("\n</div></form>");
		hgl.endPage();
	}

	static class Terminal {
		String name;
		Terminal(String name) {
			this.name= name;
		}
		public String toString() { return name; }
	}

	static class Token {
		Terminal ttype;
		String value;
		public Token(Terminal ttype) {
			this.ttype= ttype;
		}
		public int hashCode() {
			return ttype.hashCode();
		}
		public boolean equals(Object other) {
			return other instanceof Token
				&& ttype == ((Token)other).ttype;
		}
		public String toString() { return ttype.name+(value != null? " {"+value+"}" : ""); }
	}

	static class TerminalSet {
		private final Map<String,Terminal> map= new HashMap<String,Terminal>();
		Terminal forName(String name) {
			Terminal result= map.get(name);
			if (result == null) {
				result= new Terminal(name);
				map.put(name, result);
			}
			return result;
		}
		Terminal getExisting(String name) {
			return map.get(name);
		}
	}

	static class NonterminalSet {
		private final Map<String,Nonterminal> nontermMap= new HashMap<String,Nonterminal>();
		Nonterminal forName(String name) {
			Nonterminal result= nontermMap.get(name);
			if (result == null) {
				result= new Nonterminal(name);
				nontermMap.put(name, result);
			}
			return result;
		}
		Nonterminal getExisting(String name) {
			return nontermMap.get(name);
		}
	}

	static class TokenGenScanRule extends ScanRule {
		Terminal ttype;
		public TokenGenScanRule(Terminal ttype, String regex, int stateTrans) {
			super(regex, ttype != null? ttype : EMIT_NOTHING, stateTrans);
			this.ttype= ttype;
		}
		public Object onMatch(String[] matchGroups, com.silverdirk.parser.Scanner scanner) {
			if (ttype == null)
				return EMIT_NOTHING;
			Token result= new Token(ttype);
			result.value= matchGroups[0];
			return result;
		}
	}

	static void renderEditPage(Fields fields, HtmlGL hgl) {
		TerminalSet terminals= new TerminalSet();
		NonterminalSet nonterminals= new NonterminalSet();
		hgl.p("\n  <input type='hidden' name='Input' value='").pText(fields.input).p("'/>");

		hgl.p("\nScan Rules:");
		ScanRuleSet[] scanRules= null;
		try {
			scanRules= buildScanRules(fields.scanRules, terminals);
		}
		catch (PatternSyntaxException ex) {
			hgl.beginErrorMsg("Syntax error in Regex");
			hgl.p("<pre>").pText(ex.getMessage()).p("</pre>");
			hgl.endErrorMsg();
		}
		catch (UserException ex) {
			ex.render(hgl);
		}
		hgl.p("\n<br/><textarea name='ScanRules' cols='60' rows='14'>").pText(fields.scanRules).p("\n</textarea><br/>");
		if (scanRules.length != 0) {
			hgl.beginContentToggle("ScanSrc", true);
			hgl.beginContentSelectorButton("ScanSrc", 1, false).p("Show java source</a><br/>\n");
			hgl.nextContentToggle("ScanSrc", 1, false);
			hgl.beginContentSelectorButton("ScanSrc", 0, false).p("Hide</a>\n<pre>");
			hgl.p("\nTerminal");
			for (Terminal t: terminals.map.values())
				hgl.p("\n\t").pText(t.name).p("= new Terminal(").p(Util.displayAsJavaString(t.name)).p("),");
			hgl.p(";");
			hgl.pText("\nnew ScanRuleSet[] {");
			for (ScanRuleSet sset: scanRules) {
				hgl.pText("\n\tnew ScanRuleSet(").pText(Util.displayAsJavaString(sset.getName())).p(", new ScanRule[] {");
				for (ScanRule srule: sset.getRules()) {
					hgl.pText("\n\t\tnew ScanRule(").pText(Util.displayAsJavaString(srule.getPattern()));
					if (srule.getToken() != ScanRule.EMIT_NOTHING && srule.getToken() != ScanRule.EMIT_MATCH)
						hgl.p(", ").p(srule.getToken().toString());
					if (srule.getStateTransition() != ScanRule.NO_STATE_TRANS)
						hgl.p(", ").p(srule.getStateTransition());
					hgl.p("),");
				}
				hgl.pText("\n\t}),");
			}
			hgl.pText("\n};");
			hgl.endContentToggle();
		}
		hgl.p("\n<br/>"
			+"\nParse Rules:");
		Grammar g= null;
		try {
			g= buildParseRules(fields.parseRules, terminals, nonterminals);
			Parser p;
			if (g.rules.length > 0)
				p= new Parser(g);
		}
		catch (UserException ex) {
			ex.render(hgl);
		}
		catch (Exception ex) {
			hgl.beginErrorMsg("Exception");
			hgl.p("<pre>").pText(Util.trimPossibleNull(ex.getMessage())).p("</pre>");
			hgl.endErrorMsg();
		}
		hgl.p("\n<br/><textarea name='ParseRules' cols='60' rows='14'>").pText(fields.parseRules).p("\n</textarea><br/>"
			+"\n<br/>"
			+"\n<input type='submit' value='Check' tabindex='1'/> "
			+"<input type='reset' value='Reset' tabindex='2'/><br/>\n");
		if (g.rules.length != 0) {
			hgl.beginContentToggle("ParseSrc", true);
			hgl.beginContentSelectorButton("ParseSrc", 1, false).p("Show java source</a><br/>\n");
			hgl.nextContentToggle("ParseSrc", 1, false);
			hgl.beginContentSelectorButton("ParseSrc", 0, false).p("Hide</a>\n<pre>");
			hgl.p("\nNonterminal");
			for (Nonterminal nt: nonterminals.nontermMap.values())
				hgl.p("\n\t").pText(nt.getName()).p("= new Nonterminal(").p(Util.displayAsJavaString(nt.getName())).p("),");
			hgl.p("\n\tstart= ").pText(g.start.getName()).p(";");
			hgl.pText("\nnew ParseRule[] {");
			for (ParseRule rule: g.rules) {
				hgl.pText("\n\tnew ParseRule(").p(rule.getNonterminal().getName()).p(", new Object[] {");
				for (Object obj: rule.getSymbols()) {
					if (obj instanceof Nonterminal)
						hgl.p(((Nonterminal)obj).getName());
					else if (obj instanceof Terminal)
						hgl.p(((Token)obj).toString());
					else
						hgl.p(obj.toString());
					hgl.p(", ");
				}
				hgl.p("}),");
			}
			hgl.pText("\n};");
			hgl.endContentToggle();
		}
	}

	static void renderScanTestPage(Fields fields, HtmlGL hgl) {
		TerminalSet terminals= new TerminalSet();
		hgl.p("\n  <input type='hidden' name='ScanRules' value='").pText(fields.scanRules).p("'/>"
			+"\n  <input type='hidden' name='ParseRules' value='").pText(fields.parseRules).p("'/>"
			+"\n  <input type='hidden' name='ParseTable' value='").pText(fields.parseTableStr).p("'/>");
		ScanRuleSet[] scanRules= null;
		try {
			scanRules= buildScanRules(fields.scanRules, terminals);
			hgl.p("\n <input type='hidden' name='ParseTable' value='").p(fields.parseTableStr).p("'/>");
		}
		catch (Exception ex) {
			hgl.beginErrorMsg("Scanner Not Ready");
			hgl.p("Head back to the grammar editor to fix the problem.<br/>\n");
			hgl.p(ex.getMessage());
			hgl.endErrorMsg();
			hgl.p("<input name='Input' type='hidden' value='").pText(fields.input).p("'/>");
			return;
		}
		hgl.p("\nEnter some text:<br/>"
			+"\n<textarea name='Input' cols='80' rows='20'>").pText(fields.input).p("\n</textarea><br/>"
			+"\n<br/>"
			+"\n<input type='submit' name='submit' value='Test' tabindex='1'/><br/>\n<br/>\n");
		if (fields.input.length() > 0) {
			hgl.p("The following tokens were scanned:<br/>\n");
			try {
				hgl.beginGroupBox();
				hgl.p("<div class='scan-output'>\n");
				try {
					com.silverdirk.parser.Scanner scanner= new com.silverdirk.parser.Scanner(scanRules, fields.input);
					while (scanner.curToken() != scanner.EOF) {
						hgl.p("<span>").pText(scanner.curToken().toString()).p("</span> ");
						scanner.next();
					}
				}
				finally {
					hgl.p("</div>\n").endGroupBox();
				}
			}
			catch (ParseException ex) {
				hgl.beginErrorMsg(ex.getMessage());
				hgl.p("<pre>");
				hgl.pText(ex.getFullMessage());
				hgl.p("</pre>");
				hgl.endErrorMsg();
			}
		}
	}

	static void renderParseTestPage(Fields fields, HtmlGL hgl) {
		TerminalSet terminals= new TerminalSet();
		NonterminalSet nonterminals= new NonterminalSet();
		hgl.p("\n  <input type='hidden' name='ScanRules' value='").pText(fields.scanRules).p("'/>"
			+"\n  <input type='hidden' name='ParseRules' value='").pText(fields.parseRules).p("'/>"
			+"\n  <input type='hidden' name='ParseTable' value='").pText(fields.parseTableStr).p("'/>");
		ScanRuleSet[] scanRules= null;
		Parser parser= null;
		try {
			scanRules= buildScanRules(fields.scanRules, terminals);
			Grammar g= buildParseRules(fields.parseRules, terminals, nonterminals);
			LR1_Table table;
			if (fields.parseTableStr.length() > 0) {
				int[][] parseTableInts= null;
				parseTableInts= decodeParseTable(fields.parseTableStr);
				table= new LR1_Table(g, parseTableInts);
			}
			else {
				table= new LR1_Table(g);
				fields.parseTableStr= encodeParseTable(table.serialize());
			}
			parser= new Parser(g, table);
			hgl.p("\n <input type='hidden' name='ParseTable' value='").p(fields.parseTableStr).p("'/>");
		}
		catch (Exception ex) {
			hgl.beginErrorMsg("Scanner/Parser Not Ready");
			hgl.p("Head back to the grammar editor to fix the problem.<br/>\n");
			hgl.p(ex.getMessage());
			hgl.endErrorMsg();
			hgl.p("<input name='Input' type='hidden' value='").pText(fields.input).p("'/>");
			return;
		}
		hgl.p("\nEnter some text:<br/>"
			+"\n<textarea name='Input' cols='80' rows='20'>").pText(fields.input).p("\n</textarea><br/>"
			+"\n<br/>"
			+"\n<input type='submit' name='submit' value='Test' tabindex='1'/>");
		if (fields.input.length() > 0) {
			com.silverdirk.parser.Scanner scanner= null;
			try {
				hgl.beginContentToggle("scan", true);
				hgl.beginContentSelectorButton("scan", 1, false).p("Show scanned tokens</a>\n");
				hgl.nextContentToggle("scan", 1, false);
				hgl.beginContentSelectorButton("scan", 0, false).p("Hide scanned tokens</a>\n");
				scanner= new com.silverdirk.parser.Scanner(scanRules, fields.input);
				hgl.beginGroupBox();
				hgl.p("<div class='scan-output'>\n");
				while (scanner.curToken() != scanner.EOF) {
					hgl.p("<span>").pText(scanner.curToken().toString()).p("</span> ");
					scanner.next();
				}
				hgl.p("</div>\n");
				hgl.endGroupBox();
				hgl.endContentToggle();

				scanner= new com.silverdirk.parser.Scanner(scanRules, fields.input);
				GenericParseNode result= (GenericParseNode) parser.parse(scanner);
				hgl.p("<pre class='parse-tree'>\n");
				result.displayTree(hgl.getTextEscapingWriter());
				hgl.p("</pre>\n");
			}
			catch (ParseException ex) {
				hgl.beginErrorMsg(ex.getMessage());
				hgl.p("<pre>");
				hgl.pText(ex.getFullMessage());
				hgl.p("</pre>");
				hgl.endErrorMsg();
			}
		}
	}

	static Pattern
		wsPattern= Pattern.compile("\\s"),
		newlinePattern= Pattern.compile("\r?\n|\r"),
		scanRulePattern= Pattern.compile("(\\w*)(->\\d+)?\\s+(.*)"),
		scanStatePattern= Pattern.compile("---"),
		parseRulePattern= Pattern.compile("(#(\\d+):([lLrRnN])\\s*)?(\\w+)\\s*::=((\\s*(\\w+|\\|))*)");

	static ScanRuleSet[] buildScanRules(String src, TerminalSet terminals) {
		String[] lines= newlinePattern.split(src, -1);
		LinkedList<ScanRuleSet> ruleSets= new LinkedList<ScanRuleSet>();
		LinkedList<ScanRule> scanRules= new LinkedList<ScanRule>();
		for (int i=0; i<lines.length;) {
			Matcher m;
			for (; i<lines.length; i++) {
				if (lines[i].trim().length() == 0)
					continue;
				else if ((m= scanRulePattern.matcher(lines[i])).matches()) {
					String name= m.group(1);
					int newState= m.start(2) == -1? ScanRule.NO_STATE_TRANS : Integer.parseInt(m.group(2).substring(2));
					String pattern= m.group(3);
					Terminal terminal= name.length() == 0? null : terminals.forName(name);
					scanRules.add(new TokenGenScanRule(terminal, pattern, newState));
				}
				else if ((m= scanStatePattern.matcher(lines[i])).matches())
					break;
				else
					throw new RuleParseError("Scan rule syntax error", lines, i);
			}
			if (!scanRules.isEmpty()) {
				ruleSets.add(new ScanRuleSet("State "+ruleSets.size(), scanRules.toArray(new ScanRule[scanRules.size()])));
				scanRules.clear();
			}
		}
		return ruleSets.toArray(new ScanRuleSet[ruleSets.size()]);
	}

	static class RuleParseError extends UserException {
		String[] lines;
		String caption;
		int failIdx;
		RuleParseError(String caption, String[] lines, int failIdx) {
			this.caption= caption;
			this.lines= lines;
			this.failIdx= failIdx;
		}

		public void render(HtmlGL hgl) {
			hgl.beginErrorMsg(caption);
			hgl.p("<ul class='errordump'>");
			int from= Math.max(failIdx-3, 0);
			int to= Math.min(failIdx+3, lines.length-1);
			for (int i=from; i<=to; i++)
				hgl.p(i==failIdx? "\n  <li class='errorline'>":"\n  <li>").pText(lines[i]).p("&nbsp;</li>");
			hgl.p("\n</ul>\n").endErrorMsg();
		}
	}

	static Grammar buildParseRules(String src, TerminalSet terminals, NonterminalSet nonterminals) {
		LinkedList<ParseRule> parseRules= new LinkedList<ParseRule>();
		Parser.Priorities rulePri= new Parser.Priorities();
		Nonterminal start= null;
		String[] lines=  newlinePattern.split(src, -1);
		for (int i=0; i<lines.length; i++) {
			Matcher m;
			if (lines[i].trim().length() == 0)
				continue;
			else if ((m= parseRulePattern.matcher(lines[i])).matches()) {
				int priLevel= -1;
				if (m.start(1) != -1) {
					priLevel= Integer.parseInt(m.group(2));
					int assoc;
					switch (lines[i].charAt(m.start(3))) {
					case 'l':
					case 'L': assoc= Parser.Priorities.LEFT; break;
					case 'r':
					case 'R': assoc= Parser.Priorities.RIGHT; break;
					case 'n':
					case 'N': assoc= Parser.Priorities.NONASSOC; break;
					default:
						throw new RuntimeException();
					}
					rulePri.setAssociativity(priLevel, assoc);
				}
				String name= m.group(4);
				String rule= m.group(5);
				Nonterminal nterm= nonterminals.forName(name);
				if (start == null)
					start= nterm;
				LinkedList ruleObjs= new LinkedList();
				for (String symbol: wsPattern.split(rule)) {
					Terminal t;
					if (symbol.trim().length() == 0)
						continue;
					else if (symbol.equals("|"))
						makeParseRule(parseRules, nterm, ruleObjs, priLevel, rulePri);
					else if ((t=terminals.getExisting(symbol)) != null)
						ruleObjs.add(new Token(t));
					else
						ruleObjs.add(nonterminals.forName(symbol));
				}
				makeParseRule(parseRules, nterm, ruleObjs, priLevel, rulePri);
			}
			else
				throw new RuleParseError("Parse rule syntax error", lines, i);
		}
		return new Grammar(start, parseRules, rulePri);
	}
	static private void makeParseRule(List<ParseRule> parseRules, Nonterminal nterm, List objs, int priLevel, Parser.Priorities rulePri) {
		ParseRule p= new ParseRule(nterm, objs.toArray());
		objs.clear();
		parseRules.add(p);
		if (priLevel != -1)
			rulePri.set(p, priLevel);
	}

	static int[][] decodeParseTable(String tableText) {
		String[] rows= newlinePattern.split(tableText);
		int[][] result= new int[rows.length][];
		for (int i=0; i<rows.length; i++) {
			String[] numStr= wsPattern.split(rows[i]);
			int[] vals= new int[numStr.length];
			for (int j=0; j<numStr.length; j++)
				vals[j]= Integer.parseInt(numStr[j]);
			result[i]= vals;
		}
		return result;
	}

	static String encodeParseTable(int[][] table) {
		StringBuffer sb= new StringBuffer(table.length*table[0].length*4);
		for (int[] row: table) {
			for (int val: row)
				sb.append(val).append(' ');
			sb.append('\n');
		}
		return sb.toString();
	}
}
