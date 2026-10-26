package dev.zero.pvpthings.customhud;

import dev.zero.pvpthings.util.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A parsed CustomHUD-style profile.
 *
 * <pre>
 * // comment
 * ==TextShadow: true==               global setting (before the first section)
 * ==Section:TopLeft==                start a section (optionally ==Section:TopLeft,4,10== for an x/y offset)
 * &aFPS: {fps}                       text, &-color codes, {variables}
 * {{fps >= 60, "&a", "&c"}}{fps}     inline conditional: cond, value, cond, value, ..., else
 * =if: target=                       block conditional (=elseif: x=, =else=, =endif=)
 * </pre>
 */
public final class HudProfile {
	public final Settings settings = new Settings();
	public final List<Section> sections = new ArrayList<>();
	public final List<String> errors = new ArrayList<>();

	public static HudProfile parse(String source) {
		HudProfile profile = new HudProfile();
		Section current = null;
		// Stack of open =if= blocks: each holds the element list we're currently appending to.
		List<IfBlock> openIfs = new ArrayList<>();
		String[] lines = source.replace("\r", "").split("\n", -1);
		for (int number = 0; number < lines.length; number++) {
			String raw = lines[number];
			String line = raw.strip();
			try {
				if (line.startsWith("//")) {
					continue;
				}
				if (line.length() > 4 && line.startsWith("==") && line.endsWith("==")) {
					String inner = line.substring(2, line.length() - 2).strip();
					int colon = inner.indexOf(':');
					String key = (colon < 0 ? inner : inner.substring(0, colon)).strip().toLowerCase(Locale.ROOT);
					String value = colon < 0 ? "" : inner.substring(colon + 1).strip();
					if (key.equals("section")) {
						if (!openIfs.isEmpty()) {
							profile.errors.add("Line " + (number + 1) + ": missing =endif= before new section");
							openIfs.clear();
						}
						current = Section.create(value, profile.settings);
						profile.sections.add(current);
					} else {
						(current == null ? profile.settings : current.settings).apply(key, value);
					}
					continue;
				}
				if (current == null) {
					if (!line.isEmpty()) {
						// Text before any section goes to the top left, like CustomHUD.
						current = Section.create("TopLeft", profile.settings);
						profile.sections.add(current);
					} else {
						continue;
					}
				}
				List<Element> target = openIfs.isEmpty() ? current.elements : openIfs.getLast().currentBranch();
				String lower = line.toLowerCase(Locale.ROOT);
				if (lower.startsWith("=if:") && line.endsWith("=")) {
					IfBlock block = new IfBlock();
					block.conditions.add(Expr.parse(line.substring(4, line.length() - 1)));
					block.branches.add(new ArrayList<>());
					target.add(block);
					openIfs.add(block);
				} else if (lower.startsWith("=elseif:") && line.endsWith("=")) {
					if (openIfs.isEmpty()) throw new IllegalArgumentException("=elseif= without =if=");
					IfBlock block = openIfs.getLast();
					block.conditions.add(Expr.parse(line.substring(8, line.length() - 1)));
					block.branches.add(new ArrayList<>());
				} else if (lower.equals("=else=")) {
					if (openIfs.isEmpty()) throw new IllegalArgumentException("=else= without =if=");
					IfBlock block = openIfs.getLast();
					block.elseBranch = new ArrayList<>();
				} else if (lower.equals("=endif=")) {
					if (openIfs.isEmpty()) throw new IllegalArgumentException("=endif= without =if=");
					openIfs.removeLast();
				} else {
					target.add(new Line(parseNodes(raw.stripTrailing()), line.isEmpty()));
				}
			} catch (RuntimeException e) {
				profile.errors.add("Line " + (number + 1) + ": " + e.getMessage());
			}
		}
		if (!openIfs.isEmpty()) {
			profile.errors.add("Missing =endif=");
		}
		return profile;
	}

	// ---------------------------------------------------------------- line parsing

	static List<Node> parseNodes(String s) {
		List<Node> nodes = new ArrayList<>();
		StringBuilder text = new StringBuilder();
		int i = 0;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c == '\\' && i + 1 < s.length() && (s.charAt(i + 1) == '{' || s.charAt(i + 1) == '}')) {
				text.append(s.charAt(i + 1));
				i += 2;
			} else if (s.startsWith("{{", i)) {
				int end = findClose(s, i + 2, true);
				flush(nodes, text);
				nodes.add(parseConditional(s.substring(i + 2, end)));
				i = end + 2;
			} else if (c == '{') {
				int end = findClose(s, i + 1, false);
				flush(nodes, text);
				nodes.add(parseVariable(s.substring(i + 1, end)));
				i = end + 1;
			} else {
				text.append(c);
				i++;
			}
		}
		flush(nodes, text);
		return nodes;
	}

	private static void flush(List<Node> nodes, StringBuilder text) {
		if (!text.isEmpty()) {
			nodes.add(new TextNode(text.toString()));
			text.setLength(0);
		}
	}

	/** Index of the closing "}}" (double) or "}" (single) matching an opening brace before {@code from}. */
	private static int findClose(String s, int from, boolean doubleBrace) {
		int depth = 0;
		char quote = 0;
		for (int i = from; i < s.length(); i++) {
			char c = s.charAt(i);
			if (quote != 0) {
				if (c == quote) quote = 0;
				continue;
			}
			if (doubleBrace && (c == '"' || c == '\'')) {
				quote = c;
			} else if (c == '{') {
				depth++;
			} else if (c == '}') {
				if (depth == 0) {
					if (!doubleBrace) return i;
					if (i + 1 < s.length() && s.charAt(i + 1) == '}') return i;
				} else {
					depth--;
				}
			}
		}
		throw new IllegalArgumentException("Unclosed '" + (doubleBrace ? "{{" : "{") + "'");
	}

	private static Node parseVariable(String content) {
		String[] tokens = content.strip().split("\\s+");
		String name = tokens[0].toLowerCase(Locale.ROOT);
		VariableNode node = new VariableNode(name);
		for (int i = 1; i < tokens.length; i++) {
			String flag = tokens[i].toLowerCase(Locale.ROOT);
			if (flag.startsWith("-p")) {
				node.precision = Integer.parseInt(flag.substring(2));
			} else if (flag.startsWith("-zf")) {
				node.zeroFill = Integer.parseInt(flag.substring(3));
			} else if (flag.equals("-uc")) {
				node.upper = true;
			} else if (flag.equals("-lc")) {
				node.lower = true;
			} else {
				throw new IllegalArgumentException("Unknown flag '" + tokens[i] + "' on {" + name + "}");
			}
		}
		return node;
	}

	private static Node parseConditional(String content) {
		List<String> args = splitArgs(content);
		ConditionalNode node = new ConditionalNode();
		int i = 0;
		while (i < args.size()) {
			String arg = args.get(i).strip();
			if (i == args.size() - 1 && isQuoted(arg) && args.size() % 2 == 1 && args.size() > 1) {
				node.elseValue = parseNodes(unquote(arg));
				break;
			}
			Expr condition = Expr.parse(arg);
			List<Node> value = i + 1 < args.size() ? parseNodes(unquote(args.get(i + 1).strip())) : List.of();
			node.conditions.add(condition);
			node.values.add(value);
			i += 2;
		}
		return node;
	}

	private static List<String> splitArgs(String s) {
		List<String> args = new ArrayList<>();
		int depth = 0;
		char quote = 0;
		int start = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (quote != 0) {
				if (c == quote) quote = 0;
			} else if (c == '"' || c == '\'') {
				quote = c;
			} else if (c == '{' || c == '(') {
				depth++;
			} else if (c == '}' || c == ')') {
				depth--;
			} else if (c == ',' && depth == 0) {
				args.add(s.substring(start, i));
				start = i + 1;
			}
		}
		args.add(s.substring(start));
		return args;
	}

	private static boolean isQuoted(String s) {
		return s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') && s.charAt(s.length() - 1) == s.charAt(0);
	}

	private static String unquote(String s) {
		return isQuoted(s) ? s.substring(1, s.length() - 1) : s;
	}

	// ---------------------------------------------------------------- model

	public enum Anchor {
		TOP_LEFT(0, 0), TOP_CENTER(1, 0), TOP_RIGHT(2, 0),
		CENTER_LEFT(0, 1), CENTER(1, 1), CENTER_RIGHT(2, 1),
		BOTTOM_LEFT(0, 2), BOTTOM_CENTER(1, 2), BOTTOM_RIGHT(2, 2);

		public final int horizontal;
		public final int vertical;

		Anchor(int horizontal, int vertical) {
			this.horizontal = horizontal;
			this.vertical = vertical;
		}

		static Anchor parse(String name) {
			String key = name.replace("_", "").replace("-", "").replace(" ", "").toLowerCase(Locale.ROOT);
			for (Anchor anchor : values()) {
				if (anchor.name().replace("_", "").toLowerCase(Locale.ROOT).equals(key)) {
					return anchor;
				}
			}
			return switch (key) {
				case "centertop", "top" -> TOP_CENTER;
				case "centerbottom", "bottom" -> BOTTOM_CENTER;
				case "left" -> CENTER_LEFT;
				case "right" -> CENTER_RIGHT;
				case "middle" -> CENTER;
				default -> throw new IllegalArgumentException("Unknown section '" + name + "'");
			};
		}
	}

	public static final class Settings {
		public Boolean textShadow;
		public Integer backgroundColor;
		public Integer foregroundColor;
		public Float scale;
		public Integer lineSpacing;
		public Integer padding;
		private final Settings parent;

		Settings() {
			this(null);
			textShadow = true;
			backgroundColor = 0x50000000;
			foregroundColor = 0xFFFFFFFF;
			scale = 1f;
			lineSpacing = 1;
			padding = 1;
		}

		Settings(Settings parent) {
			this.parent = parent;
		}

		void apply(String key, String value) {
			switch (key) {
				case "textshadow", "shadow" -> textShadow = Boolean.parseBoolean(value);
				case "backgroundcolor", "background" -> backgroundColor = value.equalsIgnoreCase("none") ? 0 : color(value);
				case "foregroundcolor", "textcolor", "color" -> foregroundColor = color(value);
				case "scale" -> scale = Float.parseFloat(value.replace("%", "")) / (value.contains("%") ? 100f : 1f);
				case "linespacing" -> lineSpacing = Integer.parseInt(value);
				case "padding" -> padding = Integer.parseInt(value.split(",")[0].strip());
				default -> throw new IllegalArgumentException("Unknown setting '" + key + "'");
			}
		}

		private static int color(String value) {
			int parsed = Colors.parse(value, 1);
			if (parsed == 1) throw new IllegalArgumentException("Bad color '" + value + "'");
			return parsed;
		}

		public boolean textShadow() {
			return textShadow != null ? textShadow : parent.textShadow();
		}

		public int backgroundColor() {
			return backgroundColor != null ? backgroundColor : parent.backgroundColor();
		}

		public int foregroundColor() {
			return foregroundColor != null ? foregroundColor : parent.foregroundColor();
		}

		public float scale() {
			return scale != null ? scale : parent.scale();
		}

		public int lineSpacing() {
			return lineSpacing != null ? lineSpacing : parent.lineSpacing();
		}

		public int padding() {
			return padding != null ? padding : parent.padding();
		}
	}

	public static final class Section {
		public Anchor anchor;
		public int offsetX;
		public int offsetY;
		public final Settings settings;
		public final List<Element> elements = new ArrayList<>();

		private Section(Settings parent) {
			settings = new Settings(parent);
		}

		static Section create(String spec, Settings global) {
			Section section = new Section(global);
			String[] parts = spec.split(",");
			section.anchor = Anchor.parse(parts[0].strip());
			if (parts.length > 1) section.offsetX = Integer.parseInt(parts[1].strip());
			if (parts.length > 2) section.offsetY = Integer.parseInt(parts[2].strip());
			return section;
		}
	}

	public sealed interface Element permits Line, IfBlock {
	}

	public record Line(List<Node> nodes, boolean blank) implements Element {
	}

	public static final class IfBlock implements Element {
		public final List<Expr> conditions = new ArrayList<>();
		public final List<List<Element>> branches = new ArrayList<>();
		public List<Element> elseBranch;

		List<Element> currentBranch() {
			return elseBranch != null ? elseBranch : branches.getLast();
		}
	}

	public sealed interface Node permits TextNode, VariableNode, ConditionalNode {
	}

	public record TextNode(String text) implements Node {
	}

	public static final class VariableNode implements Node {
		public final String name;
		public int precision = -1;
		public int zeroFill = 0;
		public boolean upper;
		public boolean lower;

		VariableNode(String name) {
			this.name = name;
		}
	}

	public static final class ConditionalNode implements Node {
		public final List<Expr> conditions = new ArrayList<>();
		public final List<List<Node>> values = new ArrayList<>();
		public List<Node> elseValue = List.of();
	}
}
