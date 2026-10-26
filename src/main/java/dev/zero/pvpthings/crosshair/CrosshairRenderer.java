package dev.zero.pvpthings.crosshair;

import dev.zero.pvpthings.config.ZeroConfig;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws {@link CrosshairSettings} with plain rectangles so any shape/size/color combination works.
 */
public final class CrosshairRenderer {
	private static final Identifier ATTACK_BACKGROUND = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
	private static final Identifier ATTACK_PROGRESS = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
	private static final Identifier ATTACK_FULL = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_full");

	private CrosshairRenderer() {
	}

	/** HUD element: replaces the vanilla crosshair when the custom crosshair is enabled. */
	public static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
			return;
		}
		if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
			return;
		}
		ZeroConfig config = ZeroConfig.get();
		float cooldown = minecraft.player.getAttackStrengthScale(0.0F);
		graphics.nextStratum();
		draw(graphics, config.crosshair, graphics.guiWidth() / 2, graphics.guiHeight() / 2, cooldown);

		if (config.crosshairAttackIndicator && minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
			int x = graphics.guiWidth() / 2 - 8;
			int y = graphics.guiHeight() / 2 - 7 + 16;
			if (cooldown < 1.0F) {
				int progress = (int) (cooldown * 17.0F);
				graphics.blitSprite(RenderPipelines.CROSSHAIR, ATTACK_BACKGROUND, x, y, 16, 4);
				graphics.blitSprite(RenderPipelines.CROSSHAIR, ATTACK_PROGRESS, 16, 4, 0, 0, x, y, progress, 4);
			} else if (minecraft.crosshairPickEntity != null && minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F) {
				graphics.blitSprite(RenderPipelines.CROSSHAIR, ATTACK_FULL, x, y, 16, 16);
			}
		}
	}

	/**
	 * Draws the crosshair centered on (centerX, centerY).
	 *
	 * @param cooldown attack strength 0..1, used for the dynamic gap
	 */
	public static void draw(GuiGraphicsExtractor graphics, CrosshairSettings s, int centerX, int centerY, float cooldown) {
		List<int[]> rects = new ArrayList<>();
		int t = s.thickness;
		int half = t / 2;
		int gap = s.gap + (s.dynamicGap ? Math.round((1f - Math.max(0f, Math.min(1f, cooldown))) * 6f) : 0);

		if (s.shape.lines && s.length > 0) {
			if (s.shape == CrosshairSettings.Shape.X_SHAPE) {
				// Diagonal arms made of small squares.
				for (int i = 0; i < s.length; i++) {
					int d = gap + i + 1;
					rects.add(new int[]{d - half, d - half, d - half + t, d - half + t});
					rects.add(new int[]{-d - half, d - half, -d - half + t, d - half + t});
					rects.add(new int[]{d - half, -d - half, d - half + t, -d - half + t});
					rects.add(new int[]{-d - half, -d - half, -d - half + t, -d - half + t});
				}
			} else {
				// The center "pixel" spans [-half, -half + t); arms start `gap` pixels away from it.
				int lo = -half - gap;
				int hi = -half + t + gap;
				rects.add(new int[]{hi, -half, hi + s.length, -half + t}); // right
				rects.add(new int[]{lo - s.length, -half, lo, -half + t}); // left
				rects.add(new int[]{-half, hi, -half + t, hi + s.length}); // bottom
				if (s.shape != CrosshairSettings.Shape.T_SHAPE) {
					rects.add(new int[]{-half, lo - s.length, -half + t, lo}); // top
				}
			}
		}
		if (s.shape.dot) {
			int d = s.dotSize;
			int dh = d / 2;
			if (d >= 4) {
				addDisc(rects, d / 2f);
			} else {
				rects.add(new int[]{-dh, -dh, -dh + d, -dh + d});
			}
		}
		if (s.shape.circle) {
			addRing(rects, s.circleRadius, t);
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) centerX, (float) centerY);
		float scale = s.scale / 100f;
		graphics.pose().scale(scale, scale);
		if (s.outline) {
			int o = s.outlineThickness;
			int outline = s.outlineArgb();
			for (int[] r : rects) {
				graphics.fill(r[0] - o, r[1] - o, r[2] + o, r[3] + o, outline);
			}
		}
		int color = s.colorArgb();
		for (int[] r : rects) {
			graphics.fill(r[0], r[1], r[2], r[3], color);
		}
		graphics.pose().popMatrix();
	}

	/** Filled circle as horizontal spans. */
	private static void addDisc(List<int[]> rects, float radius) {
		int r = (int) Math.ceil(radius);
		for (int y = -r; y < r; y++) {
			float cy = y + 0.5f;
			float w = (float) Math.sqrt(Math.max(0f, radius * radius - cy * cy));
			int x0 = Math.round(-w);
			int x1 = Math.round(w);
			if (x1 > x0) {
				rects.add(new int[]{x0, y, x1, y + 1});
			}
		}
	}

	/** Ring of the given radius/thickness as horizontal spans (two per row). */
	private static void addRing(List<int[]> rects, int radius, int thickness) {
		float outer = radius + thickness / 2f;
		float inner = Math.max(0f, outer - thickness);
		int r = (int) Math.ceil(outer);
		for (int y = -r; y < r; y++) {
			float cy = y + 0.5f;
			float wo = (float) Math.sqrt(Math.max(0f, outer * outer - cy * cy));
			float wi = Math.abs(cy) < inner ? (float) Math.sqrt(inner * inner - cy * cy) : 0f;
			int xo = Math.round(wo);
			int xi = Math.round(wi);
			if (xo <= 0) {
				continue;
			}
			if (xi <= 0) {
				rects.add(new int[]{-xo, y, xo, y + 1});
			} else if (xo > xi) {
				rects.add(new int[]{-xo, y, -xi, y + 1});
				rects.add(new int[]{xi, y, xo, y + 1});
			}
		}
	}
}
