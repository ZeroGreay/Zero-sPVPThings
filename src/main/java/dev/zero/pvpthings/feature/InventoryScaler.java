package dev.zero.pvpthings.feature;

import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/**
 * Scales container screens independently from the GUI scale.
 * <p>
 * The trick: a scaled screen is initialised with a smaller virtual size (real size / scale), rendered with
 * the pose scaled by {@code scale}, and receives mouse coordinates divided by {@code scale}. Everything a
 * screen does stays consistent, so slots, buttons, tooltips and the recipe book keep working.
 */
public final class InventoryScaler {
	/** Set while the current screen is being drawn with our scale applied (used to fix guiWidth/guiHeight). */
	private static float renderScale = 1f;

	private InventoryScaler() {
	}

	public static boolean applies(Screen screen) {
		ZeroConfig config = ZeroConfig.get();
		if (!config.scalerEnabled || config.inventoryScale == 100 || !(screen instanceof AbstractContainerScreen<?>)) {
			return false;
		}
		return config.scaleAllContainers || screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
	}

	/**
	 * The scale to use for this screen, clamped so the container still fits in the window.
	 * Returns 1 when the screen is not scaled.
	 */
	public static float scaleFor(Screen screen) {
		if (!applies(screen)) {
			return 1f;
		}
		float scale = ZeroConfig.get().inventoryScale / 100f;
		if (scale > 1f) {
			Minecraft minecraft = Minecraft.getInstance();
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
			int guiWidth = minecraft.getWindow().getGuiScaledWidth();
			int guiHeight = minecraft.getWindow().getGuiScaledHeight();
			float maxScale = Math.min(guiWidth / (float) Math.max(1, accessor.zeropvp$getImageWidth()),
					guiHeight / (float) Math.max(1, accessor.zeropvp$getImageHeight()));
			scale = Math.max(1f, Math.min(scale, maxScale));
		}
		return scale;
	}

	/** Scale of the screen currently open, or 1. */
	public static float currentScale() {
		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = minecraft == null || minecraft.gui == null ? null : minecraft.gui.screen();
		return screen == null ? 1f : scaleFor(screen);
	}

	public static float renderScale() {
		return renderScale;
	}

	public static void setRenderScale(float scale) {
		renderScale = scale;
	}

	/** Changes the configured scale by {@code steps} increments and re-lays out the open screen. */
	public static void step(int steps) {
		ZeroConfig config = ZeroConfig.get();
		int next = ZeroConfig.clamp(config.inventoryScale + steps * config.scaleStep, 50, 400);
		if (next == config.inventoryScale) {
			return;
		}
		config.inventoryScale = next;
		ZeroConfig.save();
		relayout();
	}

	public static void reset() {
		ZeroConfig.get().inventoryScale = 100;
		ZeroConfig.save();
		relayout();
	}

	public static void relayout() {
		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = minecraft.gui.screen();
		if (screen != null) {
			// resize() goes through our ScreenMixin, which divides the size by the new scale.
			screen.resize(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
		}
	}
}
