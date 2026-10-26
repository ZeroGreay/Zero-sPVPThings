package dev.zero.pvpthings;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.crosshair.CrosshairRenderer;
import dev.zero.pvpthings.customhud.HudRenderer;
import dev.zero.pvpthings.customhud.ProfileManager;
import dev.zero.pvpthings.feature.GapTracker;
import dev.zero.pvpthings.feature.InventoryScaler;
import dev.zero.pvpthings.feature.ItemHighlighter;
import dev.zero.pvpthings.feature.MomentumBar;
import dev.zero.pvpthings.gui.ZeroConfigScreen;
import dev.zero.pvpthings.hud.CombatTracker;
import dev.zero.pvpthings.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZeroPvpThings implements ClientModInitializer {
	public static final String MOD_ID = "zeropvpthings";
	public static final Logger LOGGER = LoggerFactory.getLogger("Zero's PVP Things");

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(id("main"));

	public static KeyMapping openConfig;
	public static KeyMapping toggleGamma;
	public static KeyMapping toggleCustomHud;
	public static KeyMapping cycleHudProfile;
	public static KeyMapping toggleCrosshair;
	public static KeyMapping highlightHovered;
	public static KeyMapping scaleUp;
	public static KeyMapping scaleDown;
	public static KeyMapping scaleReset;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		ZeroConfig.load();
		ProfileManager.init();
		registerKeys();
		registerHud();

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			MomentumBar.tick(minecraft);
			GapTracker.tick(minecraft);
			CombatTracker.tick(minecraft);
			handleGameKeys(minecraft);
		});

		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof AbstractContainerScreen<?>) {
				ScreenKeyboardEvents.allowKeyPress(screen).register(ZeroPvpThings::handleInventoryKey);
			}
		});
	}

	private static KeyMapping key(String name, int glfwKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key." + MOD_ID + "." + name, InputConstants.Type.KEYSYM, glfwKey, CATEGORY));
	}

	private static void registerKeys() {
		openConfig = key("open_config", GLFW.GLFW_KEY_RIGHT_SHIFT);
		toggleGamma = key("toggle_gamma", GLFW.GLFW_KEY_G);
		toggleCustomHud = key("toggle_customhud", InputConstants.UNKNOWN.getValue());
		cycleHudProfile = key("cycle_hud_profile", InputConstants.UNKNOWN.getValue());
		toggleCrosshair = key("toggle_crosshair", InputConstants.UNKNOWN.getValue());
		highlightHovered = key("highlight_hovered", GLFW.GLFW_KEY_H);
		scaleUp = key("inventory_scale_up", GLFW.GLFW_KEY_EQUAL);
		scaleDown = key("inventory_scale_down", GLFW.GLFW_KEY_MINUS);
		scaleReset = key("inventory_scale_reset", GLFW.GLFW_KEY_0);
	}

	private static void registerHud() {
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (graphics, deltaTracker) -> {
			if (ZeroConfig.get().crosshairEnabled) {
				CrosshairRenderer.extractHud(graphics, deltaTracker);
			} else {
				vanilla.extractRenderState(graphics, deltaTracker);
			}
		});
		HudElementRegistry.replaceElement(VanillaHudElements.INFO_BAR, vanilla -> (graphics, deltaTracker) -> {
			if (MomentumBar.active()) {
				MomentumBar.extractBar(graphics, deltaTracker);
			} else {
				vanilla.extractRenderState(graphics, deltaTracker);
			}
		});
		HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, vanilla -> (graphics, deltaTracker) -> {
			if (MomentumBar.active()) {
				if (ZeroConfig.get().momentumShowNumber) {
					MomentumBar.extractNumber(graphics, deltaTracker);
				}
			} else {
				vanilla.extractRenderState(graphics, deltaTracker);
			}
		});
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, id("gap_tracker"), (graphics, deltaTracker) -> {
			if (!Minecraft.getInstance().gui.hud.isHidden()) {
				GapTracker.extract(graphics, deltaTracker);
			}
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("customhud"), HudRenderer::extract);
	}

	private static void handleGameKeys(Minecraft minecraft) {
		while (openConfig.consumeClick()) {
			minecraft.gui.setScreen(new ZeroConfigScreen(minecraft.gui.screen()));
		}
		while (toggleGamma.consumeClick()) {
			ZeroConfig config = ZeroConfig.get();
			config.gammaEnabled = !config.gammaEnabled;
			ZeroConfig.save();
			overlay(minecraft, Component.translatable(config.gammaEnabled ? "message.zeropvpthings.gamma_on" : "message.zeropvpthings.gamma_off", config.gammaPercent));
		}
		while (toggleCustomHud.consumeClick()) {
			ZeroConfig config = ZeroConfig.get();
			config.customHudEnabled = !config.customHudEnabled;
			ZeroConfig.save();
			overlay(minecraft, Component.translatable(config.customHudEnabled ? "message.zeropvpthings.customhud_on" : "message.zeropvpthings.customhud_off"));
		}
		while (cycleHudProfile.consumeClick()) {
			ProfileManager.cycle();
			overlay(minecraft, Component.translatable("message.zeropvpthings.profile", ZeroConfig.get().customHudProfile));
		}
		while (toggleCrosshair.consumeClick()) {
			ZeroConfig config = ZeroConfig.get();
			config.crosshairEnabled = !config.crosshairEnabled;
			ZeroConfig.save();
			overlay(minecraft, Component.translatable(config.crosshairEnabled ? "message.zeropvpthings.crosshair_on" : "message.zeropvpthings.crosshair_off"));
		}
	}

	/** Keys that work inside inventories: scale +/-/reset and "highlight hovered item". */
	private static boolean handleInventoryKey(Screen screen, KeyEvent event) {
		if (screen.getFocused() instanceof EditBox) {
			return true; // typing in a search box
		}
		if (scaleUp.matches(event)) {
			InventoryScaler.step(1);
			showScale();
			return false;
		}
		if (scaleDown.matches(event)) {
			InventoryScaler.step(-1);
			showScale();
			return false;
		}
		if (scaleReset.matches(event)) {
			InventoryScaler.reset();
			showScale();
			return false;
		}
		if (highlightHovered.matches(event)) {
			Slot slot = ((AbstractContainerScreenAccessor) screen).zeropvp$getHoveredSlot();
			if (slot != null && slot.hasItem()) {
				String color = ItemHighlighter.cycleColor(slot.getItem());
				overlay(Minecraft.getInstance(), color == null
						? Component.translatable("message.zeropvpthings.highlight_removed", slot.getItem().getHoverName())
						: Component.translatable("message.zeropvpthings.highlight_set", slot.getItem().getHoverName(), color));
				return false;
			}
		}
		return true;
	}

	private static void showScale() {
		overlay(Minecraft.getInstance(), Component.translatable("message.zeropvpthings.scale", ZeroConfig.get().inventoryScale));
	}

	private static void overlay(Minecraft minecraft, Component message) {
		if (minecraft.player != null) {
			minecraft.gui.hud.setOverlayMessage(message, false);
		}
	}
}
