package dev.zero.pvpthings.gui;

import dev.zero.pvpthings.config.HighlightRule;
import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.util.Colors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Edit the item -> color highlight rules. First matching rule wins.
 */
public class HighlightRulesScreen extends Screen {
	private static final String KEY = "zeropvpthings.rules.";
	private static final int ROW = 24;
	private static final int TOP = 40;

	private final Screen parent;
	private int page;

	public HighlightRulesScreen(Screen parent) {
		super(Component.translatable(KEY + "title"));
		this.parent = parent;
	}

	private int rowsPerPage() {
		return Math.max(1, (height - TOP - 60) / ROW);
	}

	@Override
	protected void init() {
		List<HighlightRule> rules = ZeroConfig.get().highlightRules;
		int perPage = rowsPerPage();
		int pages = Math.max(1, (rules.size() + perPage - 1) / perPage);
		page = Math.min(page, pages - 1);
		int rowWidth = 320;
		int left = (width - rowWidth) / 2;

		for (int i = 0; i < perPage; i++) {
			int index = page * perPage + i;
			if (index >= rules.size()) {
				break;
			}
			HighlightRule rule = rules.get(index);
			int y = TOP + i * ROW;

			EditBox match = new EditBox(font, left, y, 180, 20, Component.translatable(KEY + "match"));
			match.setMaxLength(128);
			match.setValue(rule.match);
			match.setResponder(value -> rule.match = value);
			addRenderableWidget(match);

			EditBox color = new EditBox(font, left + 186, y, 80, 20, Component.translatable(KEY + "color"));
			color.setMaxLength(9);
			color.setValue(rule.color);
			color.setResponder(value -> {
				rule.color = value;
				color.setTextColor(Colors.parse(value, 0) != 0 ? 0xFFE0E0E0 : 0xFFFF5555);
			});
			addRenderableWidget(color);

			addRenderableWidget(Button.builder(Component.literal("↑"), b -> move(index, -1)).bounds(left + 290, y, 14, 20).build());
			addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
				rules.remove(index);
				rebuildWidgets();
			}).bounds(left + 306, y, 14, 20).build());
		}

		int bottom = height - 50;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			page = Math.max(0, page - 1);
			rebuildWidgets();
		}).bounds(left, bottom, 20, 20).build()).active = page > 0;
		addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			page++;
			rebuildWidgets();
		}).bounds(left + rowWidth - 20, bottom, 20, 20).build()).active = page < pages - 1;
		addRenderableWidget(Button.builder(Component.translatable(KEY + "add"), b -> {
			rules.add(new HighlightRule("minecraft:", "#FF5555"));
			page = (rules.size() - 1) / perPage;
			rebuildWidgets();
		}).bounds(left + 25, bottom, 130, 20).build());
		addRenderableWidget(Button.builder(Component.translatable(KEY + "defaults"), b -> {
			ZeroConfig.get().highlightRules = HighlightRule.defaults();
			page = 0;
			rebuildWidgets();
		}).bounds(left + 165, bottom, 130, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 100, height - 26, 200, 20).build());
	}

	private void move(int index, int direction) {
		List<HighlightRule> rules = ZeroConfig.get().highlightRules;
		int target = index + direction;
		if (target >= 0 && target < rules.size()) {
			rules.add(target, rules.remove(index));
			rebuildWidgets();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable(KEY + "help"), width / 2, 24, 0xFFAAAAAA);
		List<HighlightRule> rules = ZeroConfig.get().highlightRules;
		int perPage = rowsPerPage();
		int left = (width - 320) / 2;
		for (int i = 0; i < perPage; i++) {
			int index = page * perPage + i;
			if (index >= rules.size()) {
				break;
			}
			int color = Colors.parse(rules.get(index).color, 0);
			int y = TOP + i * ROW;
			graphics.fill(left + 270, y + 2, left + 286, y + 18, 0xFF000000);
			graphics.fill(left + 271, y + 3, left + 285, y + 17, color | 0xFF000000);
		}
	}

	@Override
	public void onClose() {
		ZeroConfig.save();
		minecraft.gui.setScreen(parent);
	}
}
