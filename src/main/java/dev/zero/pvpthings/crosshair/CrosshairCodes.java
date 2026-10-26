package dev.zero.pvpthings.crosshair;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.zero.pvpthings.util.Colors;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Crosshair share codes.
 * <ul>
 *     <li><b>Marvel Rivals</b> reticle codes, e.g.
 *     {@code 3;1;10.0,10.0,10.0,10.0;100.0,100.0,100.0,100.0;50.0,50.0,50.0,50.0;100.0,100.0,100.0,100.0}</li>
 *     <li>Our own lossless format: {@code ZPVP1:<base64 json>}</li>
 * </ul>
 * Marvel Rivals does not publish its code layout, so the mapping below is inferred from community codes:
 * <pre>
 *  field 0      reticle type      1 = cross, 2 = circle, 3 = dot, 4 = cross + dot (anything else: cross)
 *  field 1      outline           0 / 1
 *  group A      shape (0-100 %)   line length, line thickness, center gap, dot / circle size
 *  group B      main color        R, G, B, opacity (0-100 %)
 *  group C      outline           R, G, B, opacity (0-100 %)
 *  group D      circle            radius, thickness, (unused), (unused)  (0-100 %)
 * </pre>
 * Missing groups keep their defaults, so partial or truncated codes still import.
 */
public final class CrosshairCodes {
	private static final Gson GSON = new Gson();
	private static final String ZPVP_PREFIX = "ZPVP1:";

	private CrosshairCodes() {
	}

	public static String export(CrosshairSettings settings) {
		String json = GSON.toJson(settings);
		return ZPVP_PREFIX + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	/** Imports either code format. */
	public static CrosshairSettings importCode(String code) throws IllegalArgumentException {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("Code is empty");
		}
		String trimmed = code.trim();
		if (trimmed.startsWith(ZPVP_PREFIX)) {
			try {
				String json = new String(Base64.getDecoder().decode(trimmed.substring(ZPVP_PREFIX.length())), StandardCharsets.UTF_8);
				CrosshairSettings settings = GSON.fromJson(json, CrosshairSettings.class);
				if (settings == null) {
					throw new IllegalArgumentException("Empty ZPVP code");
				}
				settings.sanitize();
				return settings;
			} catch (IllegalArgumentException | JsonParseException e) {
				throw new IllegalArgumentException("Invalid ZPVP code: " + e.getMessage());
			}
		}
		return importMarvelRivals(trimmed);
	}

	public static CrosshairSettings importMarvelRivals(String code) {
		String[] fields = code.replace(" ", "").split(";");
		if (fields.length < 3) {
			throw new IllegalArgumentException("Not a Marvel Rivals crosshair code (expected fields separated by ';')");
		}
		CrosshairSettings s = new CrosshairSettings();
		int type = (int) Math.round(number(fields[0]));
		s.shape = switch (type) {
			case 2 -> CrosshairSettings.Shape.CIRCLE;
			case 3 -> CrosshairSettings.Shape.DOT;
			case 4 -> CrosshairSettings.Shape.CROSS_DOT;
			default -> CrosshairSettings.Shape.CROSS;
		};
		s.outline = number(fields[1]) >= 0.5;

		List<double[]> groups = new ArrayList<>();
		for (int i = 2; i < fields.length; i++) {
			groups.add(group(fields[i]));
		}

		double[] shape = groupOr(groups, 0, new double[]{20, 10, 15, 10});
		s.length = (int) Math.round(percent(shape[0]) * 20);
		s.thickness = Math.max(1, (int) Math.round(percent(shape[1]) * 6));
		s.gap = (int) Math.round(percent(shape[2]) * 15);
		double size = percent(shape[3]);
		s.dotSize = Math.max(1, (int) Math.round(size * 10));
		s.circleRadius = Math.max(2, (int) Math.round(size * 20));
		if (type == 1 && s.length == 0) {
			// A "cross" with no lines is effectively a dot.
			s.shape = CrosshairSettings.Shape.DOT;
		}

		double[] color = groupOr(groups, 1, new double[]{100, 100, 100, 100});
		s.color = Colors.toHex(rgba(color));
		double[] outline = groupOr(groups, 2, new double[]{0, 0, 0, 100});
		s.outlineColor = Colors.toHex(rgba(outline));
		if (groups.size() > 3) {
			double[] circle = groups.get(3);
			if (s.shape == CrosshairSettings.Shape.CIRCLE && percent(circle[0]) > 0) {
				s.circleRadius = Math.max(2, (int) Math.round(percent(circle[0]) * 20));
			}
		}
		s.sanitize();
		return s;
	}

	private static int rgba(double[] values) {
		int r = channel(values[0]);
		int g = channel(values[1]);
		int b = channel(values[2]);
		int a = values.length > 3 ? channel(values[3]) : 255;
		if (a == 0) {
			a = 255; // an invisible crosshair is never what anyone wants
		}
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int channel(double percentValue) {
		return (int) Math.round(percent(percentValue) * 255);
	}

	/** Values above 100 are treated as already being 0-255. */
	private static double percent(double value) {
		if (value > 100) {
			return Math.min(1.0, value / 255.0);
		}
		return Math.max(0.0, value / 100.0);
	}

	private static double[] groupOr(List<double[]> groups, int index, double[] fallback) {
		if (index >= groups.size()) {
			return fallback;
		}
		double[] group = groups.get(index);
		double[] result = fallback.clone();
		System.arraycopy(group, 0, result, 0, Math.min(group.length, result.length));
		return result;
	}

	private static double[] group(String field) {
		String[] parts = field.split(",");
		double[] values = new double[Math.max(4, parts.length)];
		for (int i = 0; i < parts.length; i++) {
			values[i] = number(parts[i]);
		}
		return values;
	}

	private static double number(String text) {
		try {
			return Double.parseDouble(text.trim().toLowerCase(Locale.ROOT));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Bad number in code: '" + text + "'");
		}
	}

	/** Built-in starting points for the editor. */
	public static List<Preset> presets() {
		List<Preset> presets = new ArrayList<>();
		presets.add(new Preset("Classic", base(CrosshairSettings.Shape.CROSS, 4, 1, 2, 1, 5, "#FFFFFF")));
		presets.add(new Preset("Small Dot", base(CrosshairSettings.Shape.DOT, 0, 1, 0, 2, 5, "#00FF66")));
		presets.add(new Preset("Big Dot", base(CrosshairSettings.Shape.DOT, 0, 1, 0, 5, 5, "#FF3355")));
		presets.add(new Preset("Circle", base(CrosshairSettings.Shape.CIRCLE_DOT, 0, 1, 0, 1, 6, "#55FFFF")));
		presets.add(new Preset("T-Shape", base(CrosshairSettings.Shape.T_SHAPE, 5, 1, 3, 1, 5, "#FFFF55")));
		presets.add(new Preset("Tight Cross", base(CrosshairSettings.Shape.CROSS_DOT, 3, 1, 1, 1, 5, "#00FFFF")));
		presets.add(new Preset("X", base(CrosshairSettings.Shape.X_SHAPE, 4, 1, 1, 1, 5, "#FF55FF")));
		presets.add(new Preset("Scope", base(CrosshairSettings.Shape.CIRCLE_CROSS, 6, 1, 2, 1, 10, "#FFFFFF")));
		return presets;
	}

	private static CrosshairSettings base(CrosshairSettings.Shape shape, int length, int thickness, int gap, int dot, int radius, String color) {
		CrosshairSettings s = new CrosshairSettings();
		s.shape = shape;
		s.length = length;
		s.thickness = thickness;
		s.gap = gap;
		s.dotSize = dot;
		s.circleRadius = radius;
		s.color = color;
		return s;
	}

	public record Preset(String name, CrosshairSettings settings) {
	}
}
