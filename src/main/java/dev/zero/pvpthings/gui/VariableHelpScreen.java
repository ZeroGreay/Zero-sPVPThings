package dev.zero.pvpthings.gui;

import dev.zero.pvpthings.customhud.Variables;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Paged list of every CustomHUD variable. */
public class VariableHelpScreen extends Screen {
	private static final int LINE = 11;
	private final Screen parent;
	private final List<String[]> entries = new ArrayList<>();
	private int page;

	public VariableHelpScreen(Screen parent) {
		super(Component.translatable("zeropvpthings.variables.title"));
		this.parent = parent;
		for (Map.Entry<String, String> entry : Variables.documentation().entrySet()) {
			entries.add(new String[]{"{" + entry.getKey() + "}", entry.getValue()});
		}
	}

	private int perPage() {
		return Math.max(1, (height - 70) / LINE);
	}

	@Override
	protected void init() {
		int pages = Math.max(1, (entries.size() + perPage() - 1) / perPage());
		page = Math.min(page, pages - 1);
		addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			page--;
			rebuildWidgets();
		}).bounds(width / 2 - 130, height - 26, 20, 20).build()).active = page > 0;
		addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			page++;
			rebuildWidgets();
		}).bounds(width / 2 + 110, height - 26, 20, 20).build()).active = page < pages - 1;
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 100, height - 26, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
		int left = Math.max(10, width / 2 - 220);
		int y = 30;
		int start = page * perPage();
		for (int i = start; i < Math.min(entries.size(), start + perPage()); i++) {
			graphics.text(font, entries.get(i)[0], left, y, 0xFFFFFF55);
			graphics.text(font, entries.get(i)[1], left + 140, y, 0xFFCCCCCC);
			y += LINE;
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
