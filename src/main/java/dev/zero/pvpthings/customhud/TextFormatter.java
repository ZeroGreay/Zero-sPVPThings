package dev.zero.pvpthings.customhud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Turns {@code &}-coded text into styled components. Supports {@code &0-&f}, {@code &k &l &m &n &o &r} and
 * hex colors as {@code &#RRGGBB} or {@code &{#RRGGBB}}. A style carries over between {@link #append} calls so a
 * color set before a variable also colors the variable.
 */
public final class TextFormatter {
	private static final int[] LEGACY = {
			0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
	};

	private final MutableComponent result = Component.empty();
	private Style style = Style.EMPTY;
	private final StringBuilder pending = new StringBuilder();
	private boolean visible;

	public TextFormatter append(String text) {
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if ((c == '&' || c == '§') && i + 1 < text.length()) {
				char code = Character.toLowerCase(text.charAt(i + 1));
				int hexStart = -1;
				int consumed = 0;
				if (code == '#' && i + 8 <= text.length()) {
					hexStart = i + 2;
					consumed = 8;
				} else if (code == '{' && i + 10 <= text.length() && text.charAt(i + 2) == '#' && text.charAt(i + 9) == '}') {
					hexStart = i + 3;
					consumed = 10;
				}
				if (hexStart >= 0) {
					try {
						int rgb = Integer.parseInt(text.substring(hexStart, hexStart + 6), 16);
						flush();
						style = Style.EMPTY.withColor(rgb);
						i += consumed;
						continue;
					} catch (NumberFormatException ignored) {
						// not a color, print as-is
					}
				}
				int legacy = Character.digit(code, 16);
				if (legacy >= 0) {
					flush();
					style = Style.EMPTY.withColor(LEGACY[legacy]);
					i += 2;
					continue;
				}
				Style formatted = switch (code) {
					case 'k' -> style.withObfuscated(true);
					case 'l' -> style.withBold(true);
					case 'm' -> style.withStrikethrough(true);
					case 'n' -> style.withUnderlined(true);
					case 'o' -> style.withItalic(true);
					case 'r' -> Style.EMPTY;
					default -> null;
				};
				if (formatted != null) {
					flush();
					style = formatted;
					i += 2;
					continue;
				}
			}
			pending.append(c);
			if (!Character.isWhitespace(c)) {
				visible = true;
			}
			i++;
		}
		return this;
	}

	private void flush() {
		if (!pending.isEmpty()) {
			result.append(Component.literal(pending.toString()).setStyle(style));
			pending.setLength(0);
		}
	}

	/** True if anything other than whitespace and formatting codes was appended. */
	public boolean hasVisibleText() {
		return visible;
	}

	public Component build() {
		flush();
		return result;
	}
}
