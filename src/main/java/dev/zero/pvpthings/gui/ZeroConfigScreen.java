package dev.zero.pvpthings.gui;

import com.mojang.serialization.Codec;
import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.customhud.ProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * The main settings screen (also opened from Mod Menu).
 */
public class ZeroConfigScreen extends OptionsSubScreen {
	private static final String PREFIX = "zeropvpthings.option.";

	public ZeroConfigScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("zeropvpthings.title"));
	}

	@Override
	protected void addOptions() {
		if (list == null) {
			return;
		}
		ZeroConfig c = ZeroConfig.get();

		list.addHeader(Component.translatable("zeropvpthings.section.highlight"));
		list.addSmall(
				bool("highlight", c.highlightEnabled, v -> c.highlightEnabled = v),
				bool("highlight_hotbar", c.highlightHotbar, v -> c.highlightHotbar = v));
		list.addSmall(
				cycle("highlight_style", ZeroConfig.HighlightStyle.class, c.highlightStyle, v -> c.highlightStyle = v),
				slider("highlight_opacity", 0, 100, c.highlightOpacity, v -> v + "%", v -> c.highlightOpacity = v));
		list.addBig(button("highlight_rules", () -> minecraft.gui.setScreen(new HighlightRulesScreen(this))));

		list.addHeader(Component.translatable("zeropvpthings.section.scaler"));
		list.addSmall(
				bool("scaler", c.scalerEnabled, v -> c.scalerEnabled = v),
				bool("scale_all", c.scaleAllContainers, v -> c.scaleAllContainers = v));
		list.addSmall(
				slider("inventory_scale", 10, 80, Math.round(c.inventoryScale / 5f), v -> (v * 5) + "%", v -> c.inventoryScale = v * 5),
				slider("scale_step", 1, 50, c.scaleStep, v -> v + "%", v -> c.scaleStep = v));

		list.addHeader(Component.translatable("zeropvpthings.section.momentum"));
		list.addSmall(
				bool("momentum", c.momentumEnabled, v -> c.momentumEnabled = v),
				bool("momentum_number", c.momentumShowNumber, v -> c.momentumShowNumber = v));
		list.addSmall(
				cycle("momentum_style", ZeroConfig.MomentumStyle.class, c.momentumStyle, v -> c.momentumStyle = v),
				slider("momentum_max", 10, 200, c.momentumMaxSpeed, v -> String.format(Locale.ROOT, "%.1f b/s", v / 10f), v -> c.momentumMaxSpeed = v));

		list.addHeader(Component.translatable("zeropvpthings.section.gamma"));
		list.addSmall(
				bool("gamma", c.gammaEnabled, v -> c.gammaEnabled = v),
				slider("gamma_value", 0, 1500, c.gammaPercent, v -> v + "%", v -> c.gammaPercent = v));

		list.addHeader(Component.translatable("zeropvpthings.section.gap"));
		list.addSmall(
				bool("gap", c.gapTrackerEnabled, v -> c.gapTrackerEnabled = v),
				bool("gap_hearts", c.gapShowHearts, v -> c.gapShowHearts = v));
		list.addSmall(
				slider("gap_linger", 0, 30, c.gapLingerSeconds, v -> v + "s", v -> c.gapLingerSeconds = v),
				slider("gap_offset", -100, 100, c.gapOffsetY, v -> String.valueOf(v), v -> c.gapOffsetY = v));

		list.addHeader(Component.translatable("zeropvpthings.section.crosshair"));
		list.addSmall(
				bool("crosshair", c.crosshairEnabled, v -> c.crosshairEnabled = v),
				bool("crosshair_attack", c.crosshairAttackIndicator, v -> c.crosshairAttackIndicator = v));
		list.addBig(button("crosshair_editor", () -> minecraft.gui.setScreen(new CrosshairEditorScreen(this))));

		list.addHeader(Component.translatable("zeropvpthings.section.customhud"));
		list.addSmall(
				bool("customhud", c.customHudEnabled, v -> c.customHudEnabled = v),
				slider("customhud_profile", 1, ProfileManager.PROFILE_COUNT, c.customHudProfile, String::valueOf, v -> c.customHudProfile = v));
		list.addSmall(
				button("customhud_editor", () -> minecraft.gui.setScreen(new CustomHudEditorScreen(this))),
				button("customhud_folder", () -> Util.getPlatform().openPath(ProfileManager.directory())));
	}

	@Override
	public void removed() {
		ZeroConfig.save();
	}

	// ---------------------------------------------------------------- option helpers

	private static Component caption(String key) {
		return Component.translatable(PREFIX + key);
	}

	private static OptionInstance<Boolean> bool(String key, boolean value, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(PREFIX + key, OptionInstance.cachedConstantTooltip(Component.translatable(PREFIX + key + ".tooltip")), value, setter::accept);
	}

	private static OptionInstance<Integer> slider(String key, int min, int max, int value, IntFunction<String> format, Consumer<Integer> setter) {
		return new OptionInstance<>(PREFIX + key, OptionInstance.cachedConstantTooltip(Component.translatable(PREFIX + key + ".tooltip")),
				(caption, v) -> Options.genericValueLabel(caption, Component.literal(format.apply(v))),
				new OptionInstance.IntRange(min, max), ZeroConfig.clamp(value, min, max), setter::accept);
	}

	private static <E extends Enum<E>> OptionInstance<E> cycle(String key, Class<E> type, E value, Consumer<E> setter) {
		Codec<E> codec = Codec.STRING.xmap(name -> Enum.valueOf(type, name), Enum::name);
		return new OptionInstance<>(PREFIX + key, OptionInstance.cachedConstantTooltip(Component.translatable(PREFIX + key + ".tooltip")),
				(caption, v) -> Options.genericValueLabel(caption, Component.translatable(PREFIX + key + "." + v.name().toLowerCase(Locale.ROOT))),
				new OptionInstance.Enum<>(Arrays.asList(type.getEnumConstants()), codec), value, setter::accept);
	}

	private static Button button(String key, Runnable action) {
		return Button.builder(caption(key), b -> action.run()).build();
	}
}
