package dev.zero.pvpthings.customhud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Evaluates and draws a {@link HudProfile}.
 */
public final class HudRenderer {
	private static final int ICON_SIZE = 10;
	private static final int MARGIN = 2;

	private HudRenderer() {
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		HudProfile profile = ProfileManager.active();
		Minecraft minecraft = Minecraft.getInstance();
		if (profile == null || minecraft.player == null || minecraft.gui.hud.isHidden()) {
			return;
		}
		for (HudProfile.Section section : profile.sections) {
			List<RenderedLine> lines = new ArrayList<>();
			evaluate(section.elements, lines);
			drawSection(graphics, minecraft.font, section, lines);
		}
	}

	// ---------------------------------------------------------------- evaluation

	private static void evaluate(List<HudProfile.Element> elements, List<RenderedLine> out) {
		for (HudProfile.Element element : elements) {
			if (element instanceof HudProfile.Line line) {
				RenderedLine rendered = renderLine(line.nodes());
				if (line.blank() || rendered.visible) {
					out.add(rendered);
				}
			} else if (element instanceof HudProfile.IfBlock block) {
				List<HudProfile.Element> chosen = block.elseBranch;
				for (int i = 0; i < block.conditions.size(); i++) {
					if (safeTest(block.conditions.get(i))) {
						chosen = block.branches.get(i);
						break;
					}
				}
				if (chosen != null) {
					evaluate(chosen, out);
				}
			}
		}
	}

	private static boolean safeTest(Expr expr) {
		try {
			return expr.test(Variables::get);
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static RenderedLine renderLine(List<HudProfile.Node> nodes) {
		RenderedLine line = new RenderedLine();
		TextFormatter[] formatter = {new TextFormatter()};
		appendNodes(nodes, line, formatter);
		line.finish(formatter[0]);
		return line;
	}

	private static void appendNodes(List<HudProfile.Node> nodes, RenderedLine line, TextFormatter[] formatter) {
		for (HudProfile.Node node : nodes) {
			if (node instanceof HudProfile.TextNode text) {
				formatter[0].append(text.text());
			} else if (node instanceof HudProfile.VariableNode variable) {
				Object value = Variables.get(variable.name);
				if (value instanceof ItemStack stack) {
					if (!stack.isEmpty()) {
						line.finish(formatter[0]);
						formatter[0] = new TextFormatter();
						line.pieces.add(stack);
						line.visible = true;
					}
					continue;
				}
				int precision = variable.precision >= 0 ? variable.precision : Variables.precision(variable.name);
				String shown = Variables.display(value, precision);
				if (variable.zeroFill > 0 && value instanceof Number) {
					boolean negative = shown.startsWith("-");
					String digits = negative ? shown.substring(1) : shown;
					int intLength = digits.contains(".") ? digits.indexOf('.') : digits.length();
					StringBuilder padded = new StringBuilder();
					for (int i = intLength; i < variable.zeroFill; i++) padded.append('0');
					shown = (negative ? "-" : "") + padded + digits;
				}
				if (variable.upper) shown = shown.toUpperCase(Locale.ROOT);
				if (variable.lower) shown = shown.toLowerCase(Locale.ROOT);
				formatter[0].append(shown);
			} else if (node instanceof HudProfile.ConditionalNode conditional) {
				List<HudProfile.Node> chosen = conditional.elseValue;
				for (int i = 0; i < conditional.conditions.size(); i++) {
					if (safeTest(conditional.conditions.get(i))) {
						chosen = conditional.values.get(i);
						break;
					}
				}
				appendNodes(chosen, line, formatter);
			}
		}
	}

	// ---------------------------------------------------------------- drawing

	private static void drawSection(GuiGraphicsExtractor graphics, Font font, HudProfile.Section section, List<RenderedLine> lines) {
		if (lines.isEmpty()) {
			return;
		}
		HudProfile.Settings settings = section.settings;
		float scale = settings.scale();
		int padding = settings.padding();
		int lineHeight = font.lineHeight + settings.lineSpacing();
		int screenWidth = Math.round(graphics.guiWidth() / scale);
		int screenHeight = Math.round(graphics.guiHeight() / scale);
		int totalHeight = lines.size() * lineHeight + padding;

		int top = switch (section.anchor.vertical) {
			case 0 -> MARGIN;
			case 1 -> (screenHeight - totalHeight) / 2;
			default -> screenHeight - totalHeight - MARGIN;
		} + section.offsetY;

		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		int y = top;
		for (RenderedLine line : lines) {
			int width = line.width(font);
			int x = switch (section.anchor.horizontal) {
				case 0 -> MARGIN;
				case 1 -> (screenWidth - width) / 2;
				default -> screenWidth - width - MARGIN;
			} + section.offsetX;
			int background = settings.backgroundColor();
			if (width > 0 && (background >>> 24) != 0) {
				graphics.fill(x - padding, y - padding, x + width + padding, y + lineHeight - padding, background);
			}
			line.draw(graphics, font, x, y, settings.foregroundColor(), settings.textShadow());
			y += lineHeight;
		}
		graphics.pose().popMatrix();
	}

	/** One output line: a sequence of text components and item icons. */
	private static final class RenderedLine {
		final List<Object> pieces = new ArrayList<>();
		boolean visible;

		void finish(TextFormatter formatter) {
			if (formatter.hasVisibleText()) {
				visible = true;
			}
			Component component = formatter.build();
			if (!component.getString().isEmpty()) {
				pieces.add(component);
			}
		}

		int width(Font font) {
			int width = 0;
			for (Object piece : pieces) {
				width += piece instanceof Component component ? font.width(component) : ICON_SIZE;
			}
			return width;
		}

		void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, int color, boolean shadow) {
			for (Object piece : pieces) {
				if (piece instanceof Component component) {
					graphics.text(font, component, x, y, color, shadow);
					x += font.width(component);
				} else if (piece instanceof ItemStack stack) {
					graphics.pose().pushMatrix();
					graphics.pose().translate((float) x, (float) (y - 1));
					graphics.pose().scale(ICON_SIZE / 16f, ICON_SIZE / 16f);
					graphics.item(stack, 0, 0);
					graphics.pose().popMatrix();
					x += ICON_SIZE;
				}
			}
		}
	}
}
