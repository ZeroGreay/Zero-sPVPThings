package dev.zero.pvpthings.customhud;

import dev.zero.pvpthings.ZeroPvpThings;
import dev.zero.pvpthings.config.ZeroConfig;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads profile_N.txt files from config/zeropvpthings/customhud and hot-reloads them when they change on disk.
 */
public final class ProfileManager {
	public static final int PROFILE_COUNT = 4;
	private static final long CHECK_INTERVAL_MS = 1000L;

	private static HudProfile active;
	private static int activeIndex = -1;
	private static long activeModified = -1;
	private static long lastCheck;

	private ProfileManager() {
	}

	public static Path directory() {
		return ZeroConfig.configDir().resolve("customhud");
	}

	public static Path file(int index) {
		return directory().resolve("profile_" + index + ".txt");
	}

	/** Writes the bundled default profiles if they don't exist yet. */
	public static void init() {
		try {
			Files.createDirectories(directory());
			for (int i = 1; i <= PROFILE_COUNT; i++) {
				Path path = file(i);
				if (!Files.exists(path)) {
					Files.writeString(path, defaultProfile(i), StandardCharsets.UTF_8);
				}
			}
		} catch (IOException e) {
			ZeroPvpThings.LOGGER.error("Could not create CustomHUD profiles", e);
		}
	}

	public static String defaultProfile(int index) {
		String resource = "/assets/" + ZeroPvpThings.MOD_ID + "/customhud/profile_" + index + ".txt";
		try (InputStream stream = ProfileManager.class.getResourceAsStream(resource)) {
			if (stream != null) {
				return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			ZeroPvpThings.LOGGER.error("Could not read {}", resource, e);
		}
		return "==Section:TopLeft==\n&aFPS: &f{fps}\n";
	}

	/** The parsed active profile, or null when CustomHUD is disabled. */
	public static HudProfile active() {
		ZeroConfig config = ZeroConfig.get();
		if (!config.customHudEnabled) {
			return null;
		}
		int index = config.customHudProfile;
		long now = Util.getMillis();
		if (index != activeIndex || now - lastCheck > CHECK_INTERVAL_MS) {
			lastCheck = now;
			Path path = file(index);
			long modified = -1;
			try {
				modified = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1;
			} catch (IOException ignored) {
			}
			if (index != activeIndex || modified != activeModified) {
				active = load(index);
				activeIndex = index;
				activeModified = modified;
			}
		}
		return active;
	}

	public static HudProfile load(int index) {
		try {
			Path path = file(index);
			String source = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
			HudProfile profile = HudProfile.parse(source);
			for (String error : profile.errors) {
				ZeroPvpThings.LOGGER.warn("CustomHUD profile {}: {}", index, error);
			}
			return profile;
		} catch (IOException e) {
			ZeroPvpThings.LOGGER.error("Could not read CustomHUD profile {}", index, e);
			return HudProfile.parse("");
		}
	}

	public static String read(int index) {
		try {
			Path path = file(index);
			return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
		} catch (IOException e) {
			return "";
		}
	}

	public static void write(int index, String source) {
		try {
			Files.createDirectories(directory());
			Files.writeString(file(index), source, StandardCharsets.UTF_8);
			activeIndex = -1; // force reload
		} catch (IOException e) {
			ZeroPvpThings.LOGGER.error("Could not save CustomHUD profile {}", index, e);
		}
	}

	public static void cycle() {
		ZeroConfig config = ZeroConfig.get();
		config.customHudProfile = config.customHudProfile % PROFILE_COUNT + 1;
		ZeroConfig.save();
	}
}
