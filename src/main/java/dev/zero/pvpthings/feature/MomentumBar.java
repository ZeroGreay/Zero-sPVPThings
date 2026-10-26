package dev.zero.pvpthings.feature;

import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.util.Colors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Replaces the experience bar with a bar that shows how much horizontal momentum (speed) the player has.
 */
public final class MomentumBar {
	private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/experience_bar_background");
	private static final Identifier PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/experience_bar_progress");
	private static final int WIDTH = 182;
	private static final int HEIGHT = 5;

	private static double speed;
	private static double previousSpeed;
	private static double peakSpeed;
	private static int peakTicks;

	private MomentumBar() {
	}

	public static boolean active() {
		Minecraft minecraft = Minecraft.getInstance();
		return ZeroConfig.get().momentumEnabled && minecraft.player != null && !minecraft.player.isSpectator();
	}

	/** Horizontal speed in blocks per second, measured every tick. */
	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		previousSpeed = speed;
		if (player == null) {
			speed = 0;
			return;
		}
		double dx = player.getX() - player.xo;
		double dz = player.getZ() - player.zo;
		speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
		if (speed >= peakSpeed || --peakTicks <= 0) {
			peakSpeed = speed;
			peakTicks = 30;
		}
	}

	public static double speed(float partialTick) {
		return Mth.lerp(partialTick, previousSpeed, speed);
	}

	public static double speed() {
		return speed;
	}

	public static void extractBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		ZeroConfig config = ZeroConfig.get();
		double current = speed(deltaTracker.getGameTimeDeltaPartialTick(false));
		double max = config.momentumMaxSpeed / 10.0;
		float fill = (float) Mth.clamp(current / max, 0.0, 1.0);
		int left = (graphics.guiWidth() - WIDTH) / 2;
		int top = graphics.guiHeight() - 24 - HEIGHT;
		int progress = (int) (fill * (WIDTH + 1));

		if (config.momentumStyle == ZeroConfig.MomentumStyle.XP_BAR) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, left, top, WIDTH, HEIGHT);
			if (progress > 0) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_SPRITE, WIDTH, HEIGHT, 0, 0, left, top, progress, HEIGHT);
			}
		} else {
			// Dark frame + gradient fill: green (slow) -> yellow -> red (max momentum).
			graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xFF101010);
			graphics.fill(left + 1, top + 1, left + WIDTH - 1, top + HEIGHT - 1, 0xFF2A2A2A);
			int color = fill < 0.5f
					? Colors.lerp(0xFF3CE05A, 0xFFFFE14D, fill * 2f)
					: Colors.lerp(0xFFFFE14D, 0xFFFF4040, (fill - 0.5f) * 2f);
			int inner = Math.min(progress, WIDTH - 2);
			if (inner > 0) {
				graphics.fill(left + 1, top + 1, left + 1 + inner, top + HEIGHT - 1, color);
				graphics.fill(left + 1, top + 1, left + 1 + inner, top + 2, Colors.lerp(color, 0xFFFFFFFF, 0.35f));
			}
		}
		// Peak marker
		int peak = (int) (Mth.clamp(peakSpeed / max, 0.0, 1.0) * (WIDTH - 2));
		if (peak > 1) {
			graphics.fill(left + peak, top, left + peak + 1, top + HEIGHT, 0xFFFFFFFF);
		}
	}

	/** Draws the speed where the XP level number normally sits. */
	public static void extractNumber(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		String text = String.format(Locale.ROOT, "%.2f b/s", speed(deltaTracker.getGameTimeDeltaPartialTick(false)));
		int x = (graphics.guiWidth() - font.width(text)) / 2;
		int y = graphics.guiHeight() - 24 - 9 - 2;
		graphics.text(font, text, x + 1, y, 0xFF000000, false);
		graphics.text(font, text, x - 1, y, 0xFF000000, false);
		graphics.text(font, text, x, y + 1, 0xFF000000, false);
		graphics.text(font, text, x, y - 1, 0xFF000000, false);
		graphics.text(font, text, x, y, 0xFF80FF20, false);
	}
}
