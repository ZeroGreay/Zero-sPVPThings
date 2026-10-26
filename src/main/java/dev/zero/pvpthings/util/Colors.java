package dev.zero.pvpthings.util;

import java.util.Locale;

public final class Colors {
	private Colors() {
	}

	/** Parses {@code #RRGGBB}, {@code #AARRGGBB} (the '#' and "0x" prefixes are optional). Returns fallback when invalid. */
	public static int parse(String text, int fallback) {
		if (text == null) {
			return fallback;
		}
		String hex = text.trim();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		} else if (hex.toLowerCase(Locale.ROOT).startsWith("0x")) {
			hex = hex.substring(2);
		}
		try {
			if (hex.length() == 6) {
				return 0xFF000000 | Integer.parseInt(hex, 16);
			}
			if (hex.length() == 8) {
				return (int) Long.parseLong(hex, 16);
			}
		} catch (NumberFormatException ignored) {
		}
		return fallback;
	}

	public static String toHex(int argb) {
		if ((argb >>> 24) == 0xFF) {
			return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
		}
		return String.format(Locale.ROOT, "#%08X", argb);
	}

	public static int withAlpha(int rgb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	/** Linear interpolation between two ARGB colors. */
	public static int lerp(int from, int to, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
