package dev.zero.pvpthings.feature;

import dev.zero.pvpthings.config.ZeroConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Shows how much health (including absorption) you lose in total while eating a golden apple.
 */
public final class GapTracker {
	private static boolean eating;
	private static float lastTotal;
	private static float lost;
	private static float progress;
	private static long finishedAt = -1;

	private GapTracker() {
	}

	public static boolean isGap(ItemStack stack) {
		return stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || !ZeroConfig.get().gapTrackerEnabled) {
			eating = false;
			finishedAt = -1;
			return;
		}
		boolean nowEating = player.isUsingItem() && isGap(player.getUseItem());
		float total = player.getHealth() + player.getAbsorptionAmount();
		if (nowEating && !eating) {
			// Started eating: reset.
			lost = 0;
			lastTotal = total;
			finishedAt = -1;
		} else if (nowEating) {
			if (total < lastTotal) {
				lost += lastTotal - total;
			}
			lastTotal = total;
			int duration = player.getUseItem().getUseDuration(player);
			progress = duration <= 0 ? 0 : player.getTicksUsingItem() / (float) duration;
		} else if (eating) {
			// Stopped (finished or cancelled). The gap's own absorption lands this tick, so don't sample it.
			finishedAt = Util.getMillis();
		}
		eating = nowEating;
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		ZeroConfig config = ZeroConfig.get();
		if (!config.gapTrackerEnabled) {
			return;
		}
		boolean lingering = finishedAt >= 0 && Util.getMillis() - finishedAt < config.gapLingerSeconds * 1000L;
		if (!eating && !lingering) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		String amount = config.gapShowHearts
				? String.format(Locale.ROOT, "-%.1f❤", lost / 2f)
				: String.format(Locale.ROOT, "-%.1f HP", lost);
		String text = eating
				? String.format(Locale.ROOT, "Gap %d%%  %s", Math.round(Math.min(1f, progress) * 100), amount)
				: "Gap done  " + amount;
		int color = lost <= 0 ? 0xFF55FF55 : lost < 4 ? 0xFFFFFF55 : 0xFFFF5555;
		int x = (graphics.guiWidth() - font.width(text)) / 2;
		int y = graphics.guiHeight() / 2 + config.gapOffsetY;
		graphics.text(font, text, x, y, color, true);
	}

	public static float lost() {
		return lost;
	}

	public static boolean eating() {
		return eating;
	}
}
