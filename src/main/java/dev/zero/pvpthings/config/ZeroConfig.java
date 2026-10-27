package dev.zero.pvpthings.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.zero.pvpthings.ZeroPvpThings;
import dev.zero.pvpthings.crosshair.CrosshairSettings;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Every setting of the mod. Serialized as JSON to {@code config/zeropvpthings.json}.
 */
public final class ZeroConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ZeroConfig instance = new ZeroConfig();

	// ---- Item highlighting ----
	public boolean highlightEnabled = true;
	public HighlightStyle highlightStyle = HighlightStyle.FILL;
	/** 0-100 */
	public int highlightOpacity = 45;
	public boolean highlightHotbar = true;
	public List<HighlightRule> highlightRules = HighlightRule.defaults();

	// ---- Inventory scaler ----
	public boolean scalerEnabled = true;
	/** Percent, 50-400. */
	public int inventoryScale = 100;
	/** Percent added/removed per key press. */
	public int scaleStep = 10;
	public boolean scaleAllContainers = true;

	// ---- Momentum bar ----
	public boolean momentumEnabled = true;
	/** Tenths of blocks per second that fill the whole bar. */
	public int momentumMaxSpeed = 80;
	public boolean momentumShowNumber = true;
	public MomentumStyle momentumStyle = MomentumStyle.GRADIENT;

	// ---- Gamma ----
	public boolean gammaEnabled = false;
	/** Percent, 0-1500. */
	public int gammaPercent = 1500;

	// ---- Gap health tracker ----
	public boolean gapTrackerEnabled = true;
	public int gapLingerSeconds = 3;
	public boolean gapShowHearts = true;
	public int gapOffsetY = 14;

	// ---- Crosshair ----
	public boolean crosshairEnabled = false;
	public boolean crosshairAttackIndicator = true;
	public CrosshairSettings crosshair = new CrosshairSettings();

	// ---- CustomHUD ----
	public boolean customHudEnabled = true;
	/** 1-based index of the active profile file. */
	public int customHudProfile = 4;

	public static ZeroConfig get() {
		return instance;
	}

	public static Path configDir() {
		return FabricLoader.getInstance().getConfigDir().resolve(ZeroPvpThings.MOD_ID);
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(ZeroPvpThings.MOD_ID + ".json");
	}

	public static void load() {
		Path path = file();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				ZeroConfig loaded = GSON.fromJson(reader, ZeroConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | JsonParseException e) {
				ZeroPvpThings.LOGGER.error("Could not read {}, using defaults", path, e);
			}
		}
		instance.sanitize();
		save();
	}

	public static void save() {
		instance.sanitize();
		try {
			Files.createDirectories(file().getParent());
			try (Writer writer = Files.newBufferedWriter(file())) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			ZeroPvpThings.LOGGER.error("Could not save config", e);
		}
	}

	private void sanitize() {
		if (highlightStyle == null) highlightStyle = HighlightStyle.FILL;
		if (momentumStyle == null) momentumStyle = MomentumStyle.GRADIENT;
		if (highlightRules == null) highlightRules = new ArrayList<>();
		highlightRules.removeIf(rule -> rule == null || rule.match == null || rule.match.isBlank());
		if (crosshair == null) crosshair = new CrosshairSettings();
		crosshair.sanitize();
		highlightOpacity = clamp(highlightOpacity, 0, 100);
		inventoryScale = clamp(inventoryScale, 50, 400);
		scaleStep = clamp(scaleStep, 1, 100);
		momentumMaxSpeed = clamp(momentumMaxSpeed, 10, 400);
		gammaPercent = clamp(gammaPercent, 0, 1500);
		gapLingerSeconds = clamp(gapLingerSeconds, 0, 30);
		gapOffsetY = clamp(gapOffsetY, -200, 200);
		customHudProfile = clamp(customHudProfile, 1, 9);
	}

	public static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public enum HighlightStyle {
		FILL, OUTLINE, BOTH
	}

	public enum MomentumStyle {
		GRADIENT, XP_BAR
	}
}
