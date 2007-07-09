import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.silverdirk.parser.*;

/**
 * <p>Project: Parser Demo</p>
 * <p>Title: Scanner demonstration</p>
 * <p>Description: Shows how a set of scanner rules can break the input into tokens</p>
 * <p>Copyright Copyright (c) 2006-2007</p>
 *
 * @author Michael Conrad / TheSilverDirk
 * @version $Revision$
 */
public class ScanDemo {
	public static void main(String[] args) throws Exception {
		ScanDemo demo= new ScanDemo();
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

	public ScanDemo() {
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
					for (; scanner.curToken() != scanner.EOF; scanner.next()) {
						lineOut.println(scanner.curToken()+" @"+scanner.curTokenPos());
					}
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
