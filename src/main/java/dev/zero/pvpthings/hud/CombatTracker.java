package dev.zero.pvpthings.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Tracks reach, combo and last target for the CustomHUD variables. */
public final class CombatTracker {
	private static final long COMBO_TIMEOUT_MS = 2000L;
	private static final long TARGET_TIMEOUT_MS = 5000L;

	private static double lastReach = 0;
	private static int combo = 0;
	private static long lastHitTime = 0;
	private static Entity lastTarget;
	private static int lastHurtTime = 0;

	private CombatTracker() {
	}

	public static void onAttack(Player player, Entity target) {
		double reach = Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
		lastReach = reach;
		long now = Util.getMillis();
		if (target != lastTarget || now - lastHitTime > COMBO_TIMEOUT_MS) {
			combo = 0;
		}
		combo++;
		lastHitTime = now;
		lastTarget = target;
	}

	/** Called every client tick: getting hit resets the combo. */
	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			combo = 0;
			lastTarget = null;
			return;
		}
		if (player.hurtTime > lastHurtTime) {
			combo = 0;
		}
		lastHurtTime = player.hurtTime;
		if (Util.getMillis() - lastHitTime > COMBO_TIMEOUT_MS) {
			combo = 0;
		}
	}

	public static double reach() {
		return lastReach;
	}

	public static int combo() {
		return combo;
	}

	/** The last entity we hit, while it is still relevant (alive and hit in the last few seconds). */
	public static LivingEntity target() {
		if (lastTarget instanceof LivingEntity living && living.isAlive() && Util.getMillis() - lastHitTime < TARGET_TIMEOUT_MS) {
			return living;
		}
		return null;
	}
}
