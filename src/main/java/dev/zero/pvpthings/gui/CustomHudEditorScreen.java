package dev.zero.pvpthings.gui;

import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.customhud.HudProfile;
import dev.zero.pvpthings.customhud.ProfileManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * In-game text editor for CustomHUD profiles.
 */
public class CustomHudEditorScreen extends Screen {
	private static final String KEY = "zeropvpthings.customhud.";

	private final Screen parent;
	private int profile;
	private String text;
	private MultiLineEditBox editor;
	private List<String> errors = List.of();

	public CustomHudEditorScreen(Screen parent) {
		super(Component.translatable(KEY + "title"));
		this.parent = parent;
		this.profile = ZeroConfig.get().customHudProfile;
		this.text = ProfileManager.read(profile);
		validate();
	}

	@Override
	protected void init() {
		int boxWidth = Math.min(width - 20, 500);
		int left = (width - boxWidth) / 2;
		int boxHeight = height - 100;
		editor = MultiLineEditBox.builder()
				.setX(left)
				.setY(28)
				.setShowBackground(true)
				.build(font, boxWidth, boxHeight, Component.translatable(KEY + "title"));
		editor.setCharacterLimit(100_000);
		editor.setValue(text);
		editor.setValueListener(value -> {
			text = value;
			validate();
		});
		addRenderableWidget(editor);

		int y = height - 46;
		List<Integer> profiles = new ArrayList<>(IntStream.rangeClosed(1, ProfileManager.PROFILE_COUNT).boxed().toList());
		addRenderableWidget(CycleButton.<Integer>builder(i -> Component.literal(String.valueOf(i)), profile)
				.withValues(profiles)
				.create(left, y, 95, 20, Component.translatable(KEY + "profile"), (button, value) -> {
					save();
					profile = value;
					text = ProfileManager.read(profile);
					validate();
					rebuildWidgets();
				}));
		addRenderableWidget(Button.builder(Component.translatable(KEY + "save"), b -> save()).bounds(left + 100, y, 95, 20).build());
		addRenderableWidget(Button.builder(Component.translatable(KEY + "use"), b -> {
			save();
			ZeroConfig.get().customHudProfile = profile;
			ZeroConfig.get().customHudEnabled = true;
			ZeroConfig.save();
		}).bounds(left + 200, y, 95, 20).build());
		addRenderableWidget(Button.builder(Component.translatable(KEY + "reset"), b -> {
			text = ProfileManager.defaultProfile(profile);
			validate();
			rebuildWidgets();
		}).bounds(left + 300, y, 95, 20).build());
		addRenderableWidget(Button.builder(Component.translatable(KEY + "variables"), b -> {
			save();
			minecraft.gui.setScreen(new VariableHelpScreen(this));
		}).bounds(left, y + 22, 95, 20).build());
		addRenderableWidget(Button.builder(Component.translatable(KEY + "folder"), b -> Util.getPlatform().openPath(ProfileManager.directory()))
				.bounds(left + 100, y + 22, 95, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + 200, y + 22, 195, 20).build());
	}

	private void validate() {
		errors = HudProfile.parse(text).errors;
	}

	private void save() {
		ProfileManager.write(profile, text);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, Component.translatable(KEY + "heading", profile), width / 2, 10, 0xFFFFFFFF);
		int y = height - 58;
		if (errors.isEmpty()) {
			graphics.centeredText(font, Component.translatable(KEY + "ok"), width / 2, y, 0xFF55FF55);
		} else {
			graphics.centeredText(font, Component.literal(errors.getFirst() + (errors.size() > 1 ? "  (+" + (errors.size() - 1) + ")" : "")), width / 2, y, 0xFFFF5555);
		}
	}

	@Override
	public void onClose() {
		save();
		minecraft.gui.setScreen(parent);
	}
}
