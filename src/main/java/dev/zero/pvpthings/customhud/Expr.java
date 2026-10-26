package dev.zero.pvpthings.customhud;

import java.util.Locale;
import java.util.function.Function;

/**
 * Tiny expression language used by conditionals:
 * <pre>
 *   fps >= 60 & !sprinting | health < 6
 *   item:main = "Diamond Sword"
 *   (health + absorption) / 2 > 5
 * </pre>
 * Operators: {@code | || or}, {@code & && and}, {@code !}, {@code = == != > < >= <=}, {@code + - * /}.
 */
public abstract class Expr {
	public abstract Object eval(Function<String, Object> variables);

	public boolean test(Function<String, Object> variables) {
		return truthy(eval(variables));
	}

	public static boolean truthy(Object value) {
		if (value == null) return false;
		if (value instanceof Boolean b) return b;
		if (value instanceof Number n) return n.doubleValue() != 0;
		if (value instanceof String s) return !s.isEmpty() && !s.equalsIgnoreCase("false");
		if (value instanceof net.minecraft.world.item.ItemStack stack) return !stack.isEmpty();
		return true;
	}

	static Double number(Object value) {
		if (value instanceof Number n) return n.doubleValue();
		if (value instanceof Boolean b) return b ? 1.0 : 0.0;
		if (value instanceof String s) {
			try {
				return Double.parseDouble(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	public static Expr parse(String text) {
		Parser parser = new Parser(text);
		Expr expr = parser.or();
		parser.skipSpaces();
		if (parser.pos < text.length()) {
			throw new IllegalArgumentException("Unexpected '" + text.substring(parser.pos) + "' in condition '" + text + "'");
		}
		return expr;
	}

	private static final class Parser {
		private final String s;
		private int pos;

		Parser(String s) {
			this.s = s;
		}

		void skipSpaces() {
			while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
		}

		boolean eat(String token) {
			skipSpaces();
			if (s.startsWith(token, pos)) {
				// don't let "=" eat the first half of "==" etc. handled by caller ordering
				pos += token.length();
				return true;
			}
			return false;
		}

		boolean eatWord(String word) {
			skipSpaces();
			int end = pos + word.length();
			if (s.regionMatches(true, pos, word, 0, word.length()) && (end >= s.length() || !isIdentChar(s.charAt(end)))) {
				pos = end;
				return true;
			}
			return false;
		}

		Expr or() {
			Expr left = and();
			while (true) {
				if (eat("||") || eat("|") || eatWord("or")) {
					Expr l = left, r = and();
					left = new Expr() {
						public Object eval(Function<String, Object> v) {
							return l.test(v) || r.test(v);
						}
					};
				} else {
					return left;
				}
			}
		}

		Expr and() {
			Expr left = not();
			while (true) {
				if (eat("&&") || eat("&") || eatWord("and")) {
					Expr l = left, r = not();
					left = new Expr() {
						public Object eval(Function<String, Object> v) {
							return l.test(v) && r.test(v);
						}
					};
				} else {
					return left;
				}
			}
		}

		Expr not() {
			skipSpaces();
			if (pos < s.length() && s.charAt(pos) == '!' && !s.startsWith("!=", pos)) {
				pos++;
				Expr inner = not();
				return new Expr() {
					public Object eval(Function<String, Object> v) {
						return !inner.test(v);
					}
				};
			}
			return comparison();
		}

		Expr comparison() {
			Expr left = sum();
			String op = null;
			for (String candidate : new String[]{">=", "<=", "!=", "==", "=", ">", "<"}) {
				if (eat(candidate)) {
					op = candidate;
					break;
				}
			}
			if (op == null) {
				return left;
			}
			Expr right = sum();
			String operator = op;
			return new Expr() {
				public Object eval(Function<String, Object> v) {
					return compare(left.eval(v), right.eval(v), operator);
				}
			};
		}

		Expr sum() {
			Expr left = product();
			while (true) {
				skipSpaces();
				if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
					char op = s.charAt(pos++);
					Expr l = left, r = product();
					left = new Expr() {
						public Object eval(Function<String, Object> v) {
							Double a = number(l.eval(v)), b = number(r.eval(v));
							if (a == null || b == null) return op == '+' ? String.valueOf(l.eval(v)) + r.eval(v) : null;
							return op == '+' ? a + b : a - b;
						}
					};
				} else {
					return left;
				}
			}
		}

		Expr product() {
			Expr left = primary();
			while (true) {
				skipSpaces();
				if (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/' || s.charAt(pos) == '%')) {
					char op = s.charAt(pos++);
					Expr l = left, r = primary();
					left = new Expr() {
						public Object eval(Function<String, Object> v) {
							Double a = number(l.eval(v)), b = number(r.eval(v));
							if (a == null || b == null) return null;
							return switch (op) {
								case '*' -> a * b;
								case '/' -> b == 0 ? 0.0 : a / b;
								default -> b == 0 ? 0.0 : a % b;
							};
						}
					};
				} else {
					return left;
				}
			}
		}

		Expr primary() {
			skipSpaces();
			if (pos >= s.length()) {
				throw new IllegalArgumentException("Condition ends early: '" + s + "'");
			}
			char c = s.charAt(pos);
			if (c == '(') {
				pos++;
				Expr inner = or();
				if (!eat(")")) throw new IllegalArgumentException("Missing ')' in '" + s + "'");
				return inner;
			}
			if (c == '"' || c == '\'') {
				int end = s.indexOf(c, pos + 1);
				if (end < 0) throw new IllegalArgumentException("Unclosed quote in '" + s + "'");
				String literal = s.substring(pos + 1, end);
				pos = end + 1;
				return constant(literal);
			}
			if (Character.isDigit(c) || (c == '-' && pos + 1 < s.length() && Character.isDigit(s.charAt(pos + 1)))) {
				int start = pos++;
				while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
				return constant(Double.parseDouble(s.substring(start, pos)));
			}
			if (isIdentChar(c)) {
				int start = pos;
				while (pos < s.length() && isIdentChar(s.charAt(pos))) pos++;
				String name = s.substring(start, pos);
				if (name.equalsIgnoreCase("true")) return constant(true);
				if (name.equalsIgnoreCase("false")) return constant(false);
				String key = name.toLowerCase(Locale.ROOT);
				return new Expr() {
					public Object eval(Function<String, Object> v) {
						return v.apply(key);
					}
				};
			}
			throw new IllegalArgumentException("Unexpected '" + c + "' in '" + s + "'");
		}

		static boolean isIdentChar(char c) {
			return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '.';
		}

		static Expr constant(Object value) {
			return new Expr() {
				public Object eval(Function<String, Object> v) {
					return value;
				}
			};
		}
	}

	static boolean compare(Object a, Object b, String op) {
		Double x = number(a), y = number(b);
		int cmp;
		if (x != null && y != null) {
			cmp = Double.compare(x, y);
		} else {
			String sa = Variables.display(a, -1), sb = Variables.display(b, -1);
			cmp = sa.compareToIgnoreCase(sb);
		}
		return switch (op) {
			case ">=" -> cmp >= 0;
			case "<=" -> cmp <= 0;
			case ">" -> cmp > 0;
			case "<" -> cmp < 0;
			case "!=" -> cmp != 0;
			default -> cmp == 0;
		};
	}
}
