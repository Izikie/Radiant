package net.optifine.shaders.config;

import net.optifine.Log;
import net.optifine.expr.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroState {
	private static final Pattern PATTERN_DIRECTIVE = Pattern.compile("\\s*#\\s*(\\w+)\\s*(.*)");
	private static final Pattern PATTERN_DEFINED = Pattern.compile("defined\\s+(\\w+)");
	private static final Pattern PATTERN_DEFINED_FUNC = Pattern.compile("defined\\s*\\(\\s*(\\w+)\\s*\\)");
	private static final Pattern PATTERN_MACRO = Pattern.compile("(\\w+)");
	private static final String DEFINE = "define";
	private static final String UNDEF = "undef";
	private static final String IFDEF = "ifdef";
	private static final String IFNDEF = "ifndef";
	private static final String IF = "if";
	private static final String ELSE = "else";
	private static final String ELIF = "elif";
	private static final String ENDIF = "endif";
	private static final List<String> MACRO_NAMES = Arrays.asList("define", "undef", "ifdef", "ifndef", "if", "else", "elif", "endif");
	private final Deque<Boolean> dequeState = new ArrayDeque<>();
	private final Deque<Boolean> dequeResolved = new ArrayDeque<>();
	private final Map<String, String> mapMacroValues = new HashMap<>();
	private boolean active = true;

	public static boolean isMacroLine(String line) {
		Matcher matcher = PATTERN_DIRECTIVE.matcher(line);

		if (!matcher.matches()) {
			return false;
		} else {
			String s = matcher.group(1);
			return MACRO_NAMES.contains(s);
		}
	}

	public boolean processLine(String line) {
		Matcher matcher = PATTERN_DIRECTIVE.matcher(line);

		if (!matcher.matches()) {
			return this.active;
		} else {
			String s = matcher.group(1);
			String s1 = matcher.group(2);
			int i = s1.indexOf("//");

			if (i >= 0) {
				s1 = s1.substring(0, i);
			}

			boolean flag = this.active;
			this.processMacro(s, s1);
			this.active = !this.dequeState.contains(Boolean.FALSE);
			return this.active || flag;
		}
	}

	private void processMacro(String name, String param) {
		StringTokenizer stringtokenizer = new StringTokenizer(param, " \t");
		String s = stringtokenizer.hasMoreTokens() ? stringtokenizer.nextToken() : "";
		String s1 = stringtokenizer.hasMoreTokens() ? stringtokenizer.nextToken("").trim() : "";

		if (name.equals("define")) {
			this.mapMacroValues.put(s, s1);
		} else if (name.equals("undef")) {
			this.mapMacroValues.remove(s);
		} else if (name.equals("ifdef")) {
			boolean flag6 = this.mapMacroValues.containsKey(s);
			this.dequeState.add(flag6);
			this.dequeResolved.add(flag6);
		} else if (name.equals("ifndef")) {
			boolean flag5 = !this.mapMacroValues.containsKey(s);
			this.dequeState.add(flag5);
			this.dequeResolved.add(flag5);
		} else if (name.equals("if")) {
			boolean flag4 = this.eval(param);
			this.dequeState.add(flag4);
			this.dequeResolved.add(flag4);
		} else if (!this.dequeState.isEmpty()) {
			switch (name) {
				case "elif" -> {
					boolean flag3 = this.dequeState.removeLast();
					boolean flag7 = this.dequeResolved.removeLast();

					if (flag7) {
						this.dequeState.add(Boolean.FALSE);
						this.dequeResolved.add(flag7);
					} else {
						boolean flag8 = this.eval(param);
						this.dequeState.add(flag8);
						this.dequeResolved.add(flag8);
					}
				}
				case "else" -> {
					boolean flag = this.dequeState.removeLast();
					boolean flag1 = this.dequeResolved.removeLast();
					boolean flag2 = !flag1;
					this.dequeState.add(flag2);
					this.dequeResolved.add(Boolean.TRUE);
				}
				case "endif" -> {
					this.dequeState.removeLast();
					this.dequeResolved.removeLast();
				}
			}
		}
	}

	private boolean eval(String str) {
		Matcher matcher = PATTERN_DEFINED.matcher(str);
		str = matcher.replaceAll("defined_$1");
		Matcher matcher1 = PATTERN_DEFINED_FUNC.matcher(str);
		str = matcher1.replaceAll("defined_$1");
		boolean flag;
		int i = 0;

		while (true) {
			flag = false;
			Matcher matcher2 = PATTERN_MACRO.matcher(str);

			while (matcher2.find()) {
				String s = matcher2.group();

				if (!s.isEmpty()) {
					char c0 = s.charAt(0);

					if ((Character.isLetter(c0) || c0 == 95) && this.mapMacroValues.containsKey(s)) {
						String s1 = this.mapMacroValues.get(s);

						if (s1 == null) {
							s1 = "1";
						}

						int j = matcher2.start();
						int k = matcher2.end();
						str = str.substring(0, j) + " " + s1 + " " + str.substring(k);
						flag = true;
						++i;
						break;
					}
				}
			}

			if (!flag || i >= 100) {
				break;
			}
		}

		if (i == 100) {
			Log.error("Too many iterations: " + i + ", when resolving: " + str);
			return true;
		} else {
			try {
				IExpressionResolver iexpressionresolver = new MacroExpressionResolver(this.mapMacroValues);
				ExpressionParser expressionparser = new ExpressionParser(iexpressionresolver);
				IExpression iexpression = expressionparser.parse(str);

				if (iexpression.getExpressionType() == ExpressionType.BOOL) {
					IExpressionBool iexpressionbool = (IExpressionBool) iexpression;
					return iexpressionbool.eval();
				} else if (iexpression.getExpressionType() == ExpressionType.FLOAT) {
					IExpressionFloat iexpressionfloat = (IExpressionFloat) iexpression;
					float f = iexpressionfloat.eval();
					return f != 0.0F;
				} else {
					throw new ParseException("Not a boolean or float expression: " + iexpression.getExpressionType());
				}
			} catch (ParseException exception) {
				Log.error("Invalid macro expression: " + str);
				Log.error("Error: " + exception.getMessage());
				return false;
			}
		}
	}
}
