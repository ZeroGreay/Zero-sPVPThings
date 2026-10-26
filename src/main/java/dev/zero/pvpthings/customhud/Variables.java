package dev.zero.pvpthings.customhud;

import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.feature.GapTracker;
import dev.zero.pvpthings.feature.MomentumBar;
import dev.zero.pvpthings.hud.ClickTracker;
import dev.zero.pvpthings.hud.CombatTracker;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * All {variables} usable in CustomHUD profiles. Values are Numbers, Strings, Booleans or ItemStacks (icons).
 */
public final class Variables {
	private static final Map<String, Entry> SIMPLE = new LinkedHashMap<>();
	private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ROOT);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final String[] DIRECTIONS = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};

	private Variables() {
	}

	private static void add(String name, int precision, Function<LocalPlayer, Object> getter, String description) {
		SIMPLE.put(name, new Entry(getter, precision, description));
	}

	private static void add(String name, Function<LocalPlayer, Object> getter, String description) {
		add(name, 2, getter, description);
	}

	static {
		Minecraft mc = Minecraft.getInstance();
		// Performance / connection
		add("fps", p -> mc.getFps(), "Frames per second");
		add("ping", p -> {
			PlayerInfo info = p.connection.getPlayerInfo(p.getUUID());
			return info == null ? 0 : info.getLatency();
		}, "Your latency in ms");
		add("players", p -> p.connection.getOnlinePlayers().size(), "Players online");
		add("server", p -> {
			ServerData data = mc.getCurrentServer();
			return data == null ? "Singleplayer" : data.ip;
		}, "Server address");
		add("server_name", p -> {
			ServerData data = mc.getCurrentServer();
			return data == null ? "Singleplayer" : data.name;
		}, "Server list name");
		add("singleplayer", p -> mc.hasSingleplayerServer(), "true in singleplayer");
		add("multiplayer", p -> !mc.hasSingleplayerServer(), "true in multiplayer");
		add("memory_used", p -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L, "Used memory (MB)");
		add("memory_max", p -> Runtime.getRuntime().maxMemory() / 1048576L, "Max memory (MB)");
		add("memory_percent", p -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100L / Runtime.getRuntime().maxMemory(), "Used memory (%)");
		add("version", p -> SharedConstants.getCurrentVersion().name(), "Minecraft version");
		add("java_version", p -> System.getProperty("java.version"), "Java version");
		add("username", p -> p.getGameProfile().name(), "Your name");
		add("entities", p -> mc.level == null ? 0 : mc.level.getEntityCount(), "Loaded entities");

		// Position
		add("x", 3, p -> p.getX(), "X coordinate");
		add("y", 3, p -> p.getY(), "Y coordinate");
		add("z", 3, p -> p.getZ(), "Z coordinate");
		add("bx", p -> p.blockPosition().getX(), "Block X");
		add("by", p -> p.blockPosition().getY(), "Block Y");
		add("bz", p -> p.blockPosition().getZ(), "Block Z");
		add("cx", p -> p.blockPosition().getX() >> 4, "Chunk X");
		add("cz", p -> p.blockPosition().getZ() >> 4, "Chunk Z");
		add("nether_x", 0, p -> isNether(p) ? p.getX() * 8 : p.getX() / 8, "X in the other dimension (nether <-> overworld)");
		add("nether_z", 0, p -> isNether(p) ? p.getZ() * 8 : p.getZ() / 8, "Z in the other dimension (nether <-> overworld)");
		add("yaw", 1, p -> wrapDegrees(p.getYRot()), "Yaw");
		add("pitch", 1, p -> p.getXRot(), "Pitch");
		add("facing", p -> facing(p), "Facing direction (north, northeast, ...)");
		add("facing_short", p -> shortFacing(facing(p)), "Facing direction (N, NE, ...)");
		add("facing_axis", p -> switch (p.getDirection()) {
			case NORTH -> "-Z";
			case SOUTH -> "+Z";
			case WEST -> "-X";
			case EAST -> "+X";
			default -> "";
		}, "Facing axis");
		add("biome", p -> {
			ClientLevel level = mc.level;
			return level == null ? "" : prettyId(level.getBiome(p.blockPosition()).unwrapKey().map(k -> k.identifier()).orElse(null));
		}, "Biome");
		add("dimension", p -> prettyId(p.level().dimension().identifier()), "Dimension");
		add("light", p -> mc.level == null ? 0 : mc.level.getMaxLocalRawBrightness(p.blockPosition()), "Light level");
		add("speed", p -> MomentumBar.speed(), "Horizontal speed (blocks/s)");
		add("speed_kmh", 1, p -> MomentumBar.speed() * 3.6, "Horizontal speed (km/h)");

		// Time
		add("time", p -> LocalDateTime.now().format(TIME_24), "Real time (24h)");
		add("time12", p -> LocalDateTime.now().format(TIME_12), "Real time (12h)");
		add("date", p -> LocalDateTime.now().format(DATE), "Real date");
		add("day", p -> mc.level == null ? 0 : mc.level.getOverworldClockTime() / 24000L, "In-game day");
		add("hour24", p -> mc.level == null ? 0 : String.format(Locale.ROOT, "%02d", (int) ((mc.level.getOverworldClockTime() / 1000L + 6) % 24)), "In-game hour (24h)");
		add("minute", p -> mc.level == null ? 0 : String.format(Locale.ROOT, "%02d", (int) (mc.level.getOverworldClockTime() % 1000L * 60 / 1000)), "In-game minute");

		// Player
		add("health", 1, p -> p.getHealth(), "Health points");
		add("max_health", 0, p -> p.getMaxHealth(), "Max health points");
		add("hearts", 1, p -> p.getHealth() / 2f, "Health in hearts");
		add("absorption", 1, p -> p.getAbsorptionAmount(), "Absorption points");
		add("total_health", 1, p -> p.getHealth() + p.getAbsorptionAmount(), "Health + absorption");
		add("armor", p -> p.getArmorValue(), "Armor points");
		add("food", p -> p.getFoodData().getFoodLevel(), "Hunger points");
		add("saturation", 1, p -> p.getFoodData().getSaturationLevel(), "Saturation");
		add("air", p -> p.getAirSupply(), "Air supply");
		add("xp_level", p -> p.experienceLevel, "XP level");
		add("sprinting", p -> p.isSprinting(), "Is sprinting");
		add("sneaking", p -> p.isShiftKeyDown(), "Is sneaking");
		add("on_ground", p -> p.onGround(), "Is on ground");
		add("in_water", p -> p.isInWater(), "Is in water");
		add("burning", p -> p.isOnFire(), "Is on fire");
		add("hurt", p -> p.hurtTime > 0, "Was just hurt");
		add("attack_cooldown", 0, p -> p.getAttackStrengthScale(0f) * 100f, "Attack cooldown (%)");
		add("selected_slot", p -> p.getInventory().getSelectedSlot() + 1, "Selected hotbar slot");

		// PvP
		add("cps", p -> ClickTracker.leftCps(), "Left clicks per second");
		add("lcps", p -> ClickTracker.leftCps(), "Left clicks per second");
		add("rcps", p -> ClickTracker.rightCps(), "Right clicks per second");
		add("reach", p -> CombatTracker.reach(), "Distance of your last hit");
		add("combo", p -> CombatTracker.combo(), "Current combo (resets when you get hit)");
		add("target", p -> CombatTracker.target() != null, "true while you have a recent target");
		add("target_name", p -> {
			LivingEntity t = CombatTracker.target();
			return t == null ? "" : t.getName().getString();
		}, "Name of your last target");
		add("target_health", 1, p -> {
			LivingEntity t = CombatTracker.target();
			return t == null ? 0 : t.getHealth() + t.getAbsorptionAmount();
		}, "Health of your last target (if the server sends it)");
		add("target_max_health", 0, p -> {
			LivingEntity t = CombatTracker.target();
			return t == null ? 0 : t.getMaxHealth();
		}, "Max health of your last target");
		add("target_distance", 1, p -> {
			LivingEntity t = CombatTracker.target();
			return t == null ? 0 : p.distanceTo(t);
		}, "Distance to your last target");
		add("gap_lost", 1, p -> GapTracker.lost(), "Health lost during the current/last gap");
		add("eating_gap", p -> GapTracker.eating(), "true while eating a golden apple");
		add("gamma", p -> ZeroConfig.get().gammaEnabled ? ZeroConfig.get().gammaPercent : (int) Math.round(mc.options.gamma().get() * 100), "Brightness (%)");
	}

	/** Names and descriptions, for the help screen / README. */
	public static Map<String, String> documentation() {
		Map<String, String> docs = new LinkedHashMap<>();
		SIMPLE.forEach((name, entry) -> docs.put(name, entry.description));
		docs.put("item:<slot>", "Item name in main/off/head/chest/legs/feet (also :count :dur :max_dur :dur_percent :icon)");
		docs.put("count:<item>", "How many of an item you carry, e.g. {count:golden_apple}");
		docs.put("icon:<item>", "Draws an item icon, e.g. {icon:totem_of_undying}");
		docs.put("effect:<effect>", "Effect level (0 when missing), e.g. {effect:speed}; :duration in seconds");
		return docs;
	}

	/** Resolves a variable name (lowercase) to a value, or null if unknown / no player. */
	public static Object get(String name) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return null;
		}
		Entry entry = SIMPLE.get(name);
		if (entry != null) {
			try {
				return entry.getter.apply(player);
			} catch (RuntimeException e) {
				return null;
			}
		}
		return dynamic(player, name);
	}

	public static int precision(String name) {
		Entry entry = SIMPLE.get(name);
		return entry == null ? 2 : entry.precision;
	}

	private static Object dynamic(LocalPlayer player, String name) {
		String[] parts = name.split(":");
		switch (parts[0]) {
			case "item": {
				if (parts.length < 2) return null;
				ItemStack stack = slot(player, parts[1]);
				if (stack == null) return null;
				String prop = parts.length > 2 ? parts[2] : "name";
				return switch (prop) {
					case "name" -> stack.isEmpty() ? "" : stack.getHoverName().getString();
					case "id" -> stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
					case "count" -> stack.getCount();
					case "dur" -> stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : 0;
					case "max_dur" -> stack.isDamageableItem() ? stack.getMaxDamage() : 0;
					case "dur_percent" -> stack.isDamageableItem() ? (stack.getMaxDamage() - stack.getDamageValue()) * 100 / stack.getMaxDamage() : 0;
					case "icon" -> stack;
					default -> null;
				};
			}
			case "count": {
				Item item = item(name.substring("count:".length()));
				if (item == null) return 0;
				int count = 0;
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack stack = player.getInventory().getItem(i);
					if (stack.getItem() == item) {
						count += stack.getCount();
					}
				}
				return count;
			}
			case "icon": {
				Item item = item(name.substring("icon:".length()));
				return item == null ? ItemStack.EMPTY : new ItemStack(item);
			}
			case "effect": {
				boolean duration = name.endsWith(":duration");
				String id = duration ? name.substring("effect:".length(), name.length() - ":duration".length()) : name.substring("effect:".length());
				Identifier key = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
				if (key == null) return 0;
				Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.get(key);
				if (effect.isEmpty()) return 0;
				MobEffectInstance instance = player.getEffect(effect.get());
				if (instance == null) return 0;
				if (duration) {
					return instance.isInfiniteDuration() ? "∞" : String.format(Locale.ROOT, "%d:%02d", instance.getDuration() / 1200, instance.getDuration() / 20 % 60);
				}
				return instance.getAmplifier() + 1;
			}
			default:
				return null;
		}
	}

	private static ItemStack slot(LocalPlayer player, String slot) {
		return switch (slot) {
			case "main", "mainhand", "hand" -> player.getMainHandItem();
			case "off", "offhand" -> player.getOffhandItem();
			case "head", "helmet" -> player.getItemBySlot(EquipmentSlot.HEAD);
			case "chest", "chestplate" -> player.getItemBySlot(EquipmentSlot.CHEST);
			case "legs", "leggings" -> player.getItemBySlot(EquipmentSlot.LEGS);
			case "feet", "boots" -> player.getItemBySlot(EquipmentSlot.FEET);
			default -> null;
		};
	}

	private static Item item(String id) {
		Identifier key = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
		if (key == null) return null;
		return BuiltInRegistries.ITEM.getOptional(key).orElse(null);
	}

	private static boolean isNether(LocalPlayer player) {
		return player.level().dimension().identifier().getPath().equals("the_nether");
	}

	private static float wrapDegrees(float degrees) {
		float wrapped = degrees % 360f;
		if (wrapped >= 180f) wrapped -= 360f;
		if (wrapped < -180f) wrapped += 360f;
		return wrapped;
	}

	private static String facing(LocalPlayer player) {
		int index = Math.floorMod(Math.round((wrapDegrees(player.getYRot()) + 360f) / 45f), 8);
		return DIRECTIONS[index];
	}

	private static String shortFacing(String direction) {
		StringBuilder builder = new StringBuilder();
		for (String part : direction.split("(?=east|west)")) {
			if (!part.isEmpty()) builder.append(Character.toUpperCase(part.charAt(0)));
		}
		return builder.toString();
	}

	private static String prettyId(Identifier id) {
		if (id == null) return "";
		String[] words = id.getPath().split("_");
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) continue;
			if (!builder.isEmpty()) builder.append(' ');
			builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return builder.toString();
	}

	/** Formats a value for display. precision < 0 means "use a sensible default". */
	public static String display(Object value, int precision) {
		if (value == null) return "";
		if (value instanceof Double || value instanceof Float) {
			double d = ((Number) value).doubleValue();
			int p = precision < 0 ? 2 : precision;
			return String.format(Locale.ROOT, "%." + p + "f", d);
		}
		if (value instanceof Number n) return String.valueOf(n.longValue());
		if (value instanceof ItemStack stack) return stack.isEmpty() ? "" : stack.getHoverName().getString();
		return String.valueOf(value);
	}

	private record Entry(Function<LocalPlayer, Object> getter, int precision, String description) {
	}
}
