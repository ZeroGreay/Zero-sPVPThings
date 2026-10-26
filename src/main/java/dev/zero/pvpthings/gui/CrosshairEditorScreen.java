package dev.zero.pvpthings.gui;

import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.crosshair.CrosshairCodes;
import dev.zero.pvpthings.crosshair.CrosshairRenderer;
import dev.zero.pvpthings.crosshair.CrosshairSettings;
import dev.zero.pvpthings.util.Colors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Crosshair creator: shape/size/color controls, live preview, presets, and Marvel Rivals / ZPVP code import-export.
 */
public class CrosshairEditorScreen extends Screen {
	private static final String KEY = "zeropvpthings.crosshair.";
	private static final int ROW = 22;
	private static final int COLUMN_WIDTH = 140;

	private final Screen parent;
	private final CrosshairSettings working;
	private EditBox codeBox;
	private String pendingCode = "";
	private int presetIndex = 0;
	private Component status = Component.empty();
	private int statusColor = 0xFFAAAAAA;

	public CrosshairEditorScreen(Screen parent) {
		super(Component.translatable(KEY + "title"));
		this.parent = parent;
		this.working = ZeroConfig.get().crosshair.copy();
	}

	@Override
	protected void init() {
		int left = 10;
		int right = left + COLUMN_WIDTH + 5;
		int y = 28;
		CrosshairSettings s = working;

		List<CrosshairCodes.Preset> presets = CrosshairCodes.presets();
		addRenderableWidget(CycleButton.<CrosshairCodes.Preset>builder(p -> Component.literal(p.name()), presets.get(presetIndex))
				.withValues(presets)
				.create(left, y, COLUMN_WIDTH, 20, Component.translatable(KEY + "preset"), (button, preset) -> {
					presetIndex = presets.indexOf(preset);
					applySettings(preset.settings().copy());
					setStatus(Component.translatable(KEY + "preset_applied", preset.name()), 0xFF55FF55);
				}));
		addRenderableWidget(CycleButton.<CrosshairSettings.Shape>builder(shape -> Component.translatable(KEY + "shape." + shape.name().toLowerCase(Locale.ROOT)), s.shape)
				.withValues(Arrays.asList(CrosshairSettings.Shape.values()))
				.create(right, y, COLUMN_WIDTH, 20, Component.translatable(KEY + "shape"), (button, shape) -> s.shape = shape));
		y += ROW;
		addRenderableWidget(slider(left, y, "length", 0, 30, s.length, v -> s.length = v));
		addRenderableWidget(slider(right, y, "thickness", 1, 10, s.thickness, v -> s.thickness = v));
		y += ROW;
		addRenderableWidget(slider(left, y, "gap", 0, 30, s.gap, v -> s.gap = v));
		addRenderableWidget(slider(right, y, "dot_size", 1, 12, s.dotSize, v -> s.dotSize = v));
		y += ROW;
		addRenderableWidget(slider(left, y, "circle_radius", 1, 30, s.circleRadius, v -> s.circleRadius = v));
		addRenderableWidget(new IntSlider(right, y, COLUMN_WIDTH, 20, Component.translatable(KEY + "scale"), 25, 400, s.scale, v -> v + "%", v -> s.scale = v));
		y += ROW;
		addRenderableWidget(CycleButton.onOffBuilder(s.outline).create(left, y, COLUMN_WIDTH, 20, Component.translatable(KEY + "outline"), (button, value) -> s.outline = value));
		addRenderableWidget(slider(right, y, "outline_thickness", 1, 4, s.outlineThickness, v -> s.outlineThickness = v));
		y += ROW;
		CycleButton<Boolean> dynamic = CycleButton.onOffBuilder(s.dynamicGap).create(left, y, COLUMN_WIDTH, 20, Component.translatable(KEY + "dynamic_gap"), (button, value) -> s.dynamicGap = value);
		dynamic.setTooltip(Tooltip.create(Component.translatable(KEY + "dynamic_gap.tooltip")));
		addRenderableWidget(dynamic);
		addRenderableWidget(CycleButton.onOffBuilder(ZeroConfig.get().crosshairEnabled).create(right, y, COLUMN_WIDTH, 20, Component.translatable(KEY + "enabled"), (button, value) -> ZeroConfig.get().crosshairEnabled = value));
		y += ROW;
		addRenderableWidget(colorBox(left, y, s.color, value -> s.color = value, KEY + "color"));
		addRenderableWidget(colorBox(right, y, s.outlineColor, value -> s.outlineColor = value, KEY + "outline_color"));
		y += ROW + 10;

		codeBox = new EditBox(font, left, y, COLUMN_WIDTH * 2 + 5, 20, Component.translatable(KEY + "code"));
		codeBox.setMaxLength(4096);
		codeBox.setHint(Component.translatable(KEY + "code_hint"));
		codeBox.setValue(pendingCode);
		codeBox.setResponder(value -> pendingCode = value);
		addRenderableWidget(codeBox);
		y += ROW;
		Button importButton = Button.builder(Component.translatable(KEY + "import"), b -> importCode()).bounds(left, y, COLUMN_WIDTH, 20).build();
		importButton.setTooltip(Tooltip.create(Component.translatable(KEY + "import.tooltip")));
		addRenderableWidget(importButton);
		addRenderableWidget(Button.builder(Component.translatable(KEY + "export"), b -> exportCode()).bounds(right, y, COLUMN_WIDTH, 20).build());
		y += ROW;
		addRenderableWidget(Button.builder(Component.translatable(KEY + "reset"), b -> applySettings(new CrosshairSettings())).bounds(left, y, COLUMN_WIDTH, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(right, y, COLUMN_WIDTH, 20).build());
	}

	private IntSlider slider(int x, int y, String key, int min, int max, int value, java.util.function.IntConsumer setter) {
		return new IntSlider(x, y, COLUMN_WIDTH, 20, Component.translatable(KEY + key), min, max, value, v -> v + "px", setter);
	}

	private EditBox colorBox(int x, int y, String value, java.util.function.Consumer<String> setter, String key) {
		EditBox box = new EditBox(font, x + 60, y, COLUMN_WIDTH - 60, 20, Component.translatable(key));
		box.setMaxLength(9);
		box.setValue(value);
		box.setResponder(text -> {
			if (Colors.parse(text, 0) != 0 || text.equalsIgnoreCase("#00000000")) {
				setter.accept(text.startsWith("#") ? text : "#" + text);
				box.setTextColor(0xFFE0E0E0);
			} else {
				box.setTextColor(0xFFFF5555);
			}
		});
		return box;
	}

	private void applySettings(CrosshairSettings settings) {
		settings.sanitize();
		working.shape = settings.shape;
		working.length = settings.length;
		working.thickness = settings.thickness;
		working.gap = settings.gap;
		working.dotSize = settings.dotSize;
		working.circleRadius = settings.circleRadius;
		working.color = settings.color;
		working.outline = settings.outline;
		working.outlineColor = settings.outlineColor;
		working.outlineThickness = settings.outlineThickness;
		working.scale = settings.scale;
		working.dynamicGap = settings.dynamicGap;
		rebuildWidgets();
	}

	private void importCode() {
		try {
			CrosshairSettings imported = CrosshairCodes.importCode(pendingCode);
			applySettings(imported);
			ZeroConfig.get().crosshairEnabled = true;
			setStatus(Component.translatable(KEY + "imported"), 0xFF55FF55);
		} catch (IllegalArgumentException e) {
			setStatus(Component.literal(e.getMessage()), 0xFFFF5555);
		}
	}

	private void exportCode() {
		String code = CrosshairCodes.export(working);
		minecraft.keyboardHandler.setClipboard(code);
		pendingCode = code;
		codeBox.setValue(code);
		setStatus(Component.translatable(KEY + "exported"), 0xFF55FF55);
	}

	private void setStatus(Component message, int color) {
		status = message;
		statusColor = color;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);

		int left = 10;
		int right = left + COLUMN_WIDTH + 5;
		int colorRow = 28 + ROW * 6;
		graphics.text(font, Component.translatable(KEY + "color"), left, colorRow + 6, 0xFFE0E0E0);
		graphics.text(font, Component.translatable(KEY + "outline_color"), right, colorRow + 6, 0xFFE0E0E0);
		swatch(graphics, left + 46, colorRow + 4, working.colorArgb());
		swatch(graphics, right + 46, colorRow + 4, working.outlineArgb());
		graphics.text(font, status, left, 28 + ROW * 10 + 10 + 4, statusColor);

		// Preview panel
		int panelLeft = right + COLUMN_WIDTH + 15;
		int panelRight = width - 10;
		int panelTop = 28;
		int panelBottom = height - 10;
		if (panelRight - panelLeft < 60) {
			return;
		}
		int midY = (panelTop + panelBottom) / 2;
		graphics.fill(panelLeft, panelTop, panelRight, midY, 0xFF6A9BD8);   // sky
		graphics.fill(panelLeft, midY, panelRight, panelBottom, 0xFF3B3B3B); // stone
		graphics.outline(panelLeft, panelTop, panelRight - panelLeft, panelBottom - panelTop, 0xFFFFFFFF);
		int centerX = (panelLeft + panelRight) / 2;
		float cooldown = minecraft.player != null ? minecraft.player.getAttackStrengthScale(0f) : 1f;
		CrosshairRenderer.draw(graphics, working, centerX, (panelTop + midY) / 2, cooldown);
		graphics.centeredText(font, Component.translatable(KEY + "preview_actual"), centerX, panelTop + 4, 0xFFFFFFFF);

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) centerX, (float) ((midY + panelBottom) / 2));
		graphics.pose().scale(3f, 3f);
		CrosshairRenderer.draw(graphics, working, 0, 0, cooldown);
		graphics.pose().popMatrix();
		graphics.centeredText(font, Component.translatable(KEY + "preview_zoom"), centerX, midY + 4, 0xFFFFFFFF);
	}

	private static void swatch(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x - 1, y - 1, x + 11, y + 11, 0xFF000000);
		graphics.fill(x, y, x + 10, y + 10, color);
	}

	@Override
	public void onClose() {
		ZeroConfig.get().crosshair = working.copy();
		ZeroConfig.save();
		minecraft.gui.setScreen(parent);
	}
}
