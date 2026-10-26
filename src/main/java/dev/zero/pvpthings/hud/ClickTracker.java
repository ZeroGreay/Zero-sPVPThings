package dev.zero.pvpthings.hud;

import net.minecraft.util.Util;

import java.util.ArrayDeque;
import java.util.Deque;

/** Clicks per second, counted over a sliding one second window. */
public final class ClickTracker {
	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();

	private ClickTracker() {
	}

	public static void onClick(int button) {
		long now = Util.getMillis();
		if (button == 0) {
			LEFT.addLast(now);
		} else if (button == 1) {
			RIGHT.addLast(now);
		}
	}

	public static int leftCps() {
		return count(LEFT);
	}

	public static int rightCps() {
		return count(RIGHT);
	}

	private static int count(Deque<Long> clicks) {
		long cutoff = Util.getMillis() - 1000L;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
			clicks.pollFirst();
		}
		return clicks.size();
	}
}
