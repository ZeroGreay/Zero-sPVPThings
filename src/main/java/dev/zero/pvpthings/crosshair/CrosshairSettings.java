package dev.zero.pvpthings.crosshair;

import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.util.Colors;

/**
 * A custom crosshair. All sizes are in GUI pixels before {@link #scale} is applied.
 */
public final class CrosshairSettings {
	public Shape shape = Shape.CROSS;
	public int length = 4;
	public int thickness = 1;
	public int gap = 2;
	public int dotSize = 1;
	public int circleRadius = 5;
	public String color = "#FFFFFF";
	public boolean outline = true;
	public String outlineColor = "#000000";
	public int outlineThickness = 1;
	/** Percent, 25-400. */
	public int scale = 100;
	/** Opens the gap while the attack cooldown recharges. */
	public boolean dynamicGap = false;

	public CrosshairSettings copy() {
		CrosshairSettings copy = new CrosshairSettings();
		copy.shape = shape;
		copy.length = length;
		copy.thickness = thickness;
		copy.gap = gap;
		copy.dotSize = dotSize;
		copy.circleRadius = circleRadius;
		copy.color = color;
		copy.outline = outline;
		copy.outlineColor = outlineColor;
		copy.outlineThickness = outlineThickness;
		copy.scale = scale;
		copy.dynamicGap = dynamicGap;
		return copy;
	}

	public void sanitize() {
		if (shape == null) shape = Shape.CROSS;
		length = ZeroConfig.clamp(length, 0, 30);
		thickness = ZeroConfig.clamp(thickness, 1, 10);
		gap = ZeroConfig.clamp(gap, 0, 30);
		dotSize = ZeroConfig.clamp(dotSize, 1, 12);
		circleRadius = ZeroConfig.clamp(circleRadius, 1, 30);
		outlineThickness = ZeroConfig.clamp(outlineThickness, 1, 4);
		scale = ZeroConfig.clamp(scale, 25, 400);
		if (Colors.parse(color, 0) == 0) color = "#FFFFFF";
		if (Colors.parse(outlineColor, 0) == 0 && !"#00000000".equals(outlineColor)) outlineColor = "#000000";
	}

	public int colorArgb() {
		return Colors.parse(color, 0xFFFFFFFF);
	}

	public int outlineArgb() {
		return Colors.parse(outlineColor, 0xFF000000);
	}

	public enum Shape {
		CROSS(true, false, false),
		CROSS_DOT(true, true, false),
		T_SHAPE(true, false, false),
		X_SHAPE(true, false, false),
		DOT(false, true, false),
		CIRCLE(false, false, true),
		CIRCLE_DOT(false, true, true),
		CIRCLE_CROSS(true, false, true);

		public final boolean lines;
		public final boolean dot;
		public final boolean circle;

		Shape(boolean lines, boolean dot, boolean circle) {
			this.lines = lines;
			this.dot = dot;
			this.circle = circle;
		}
	}
}
