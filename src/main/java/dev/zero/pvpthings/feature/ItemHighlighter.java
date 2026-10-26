package dev.zero.pvpthings.feature;

import dev.zero.pvpthings.config.HighlightRule;
import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.util.Colors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Colors inventory slots based on the item they contain.
 */
public final class ItemHighlighter {
	/** Colors cycled through by the "highlight hovered item" key. */
	public static final String[] PALETTE = {
			"#FF5555", "#FFAA00", "#FFFF55", "#55FF55", "#55FFFF", "#5555FF", "#FF55FF", "#FFFFFF"
	};

	private static List<CompiledRule> compiled = List.of();
	private static List<HighlightRule> compiledFrom = null;
	private static int compiledHash = 0;

	private ItemHighlighter() {
	}

	/** Returns the ARGB color for the stack (alpha already includes the configured opacity), or 0 for none. */
	public static int colorFor(ItemStack stack) {
		ZeroConfig config = ZeroConfig.get();
		if (!config.highlightEnabled || stack.isEmpty()) {
			return 0;
		}
		for (CompiledRule rule : rules()) {
			if (rule.matches(stack)) {
				int alpha = (int) ((rule.color >>> 24) * (config.highlightOpacity / 100f));
				return (alpha << 24) | (rule.color & 0xFFFFFF);
			}
		}
		return 0;
	}

	/** Draws the highlight for a 16x16 slot whose top-left corner is at x, y. */
	public static void drawSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
		int color = colorFor(stack);
		if (color == 0) {
			return;
		}
		ZeroConfig.HighlightStyle style = ZeroConfig.get().highlightStyle;
		if (style == ZeroConfig.HighlightStyle.FILL || style == ZeroConfig.HighlightStyle.BOTH) {
			graphics.fill(x, y, x + 16, y + 16, color);
		}
		if (style == ZeroConfig.HighlightStyle.OUTLINE || style == ZeroConfig.HighlightStyle.BOTH) {
			graphics.outline(x, y, 16, 16, color | 0xFF000000);
		}
	}

	/**
	 * Cycles the highlight color of this item: none -> palette[0] -> ... -> palette[n-1] -> none.
	 * Only touches exact item-id rules so tag/glob rules stay intact.
	 */
	public static String cycleColor(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		List<HighlightRule> rules = ZeroConfig.get().highlightRules;
		HighlightRule existing = null;
		for (HighlightRule rule : rules) {
			if (normalizeId(rule.match).equals(id)) {
				existing = rule;
				break;
			}
		}
		String result;
		if (existing == null) {
			result = PALETTE[0];
			rules.add(0, new HighlightRule(id, result));
		} else {
			int index = -1;
			for (int i = 0; i < PALETTE.length; i++) {
				if (PALETTE[i].equalsIgnoreCase(existing.color)) {
					index = i;
				}
			}
			if (index + 1 >= PALETTE.length) {
				rules.remove(existing);
				result = null;
			} else {
				existing.color = PALETTE[index + 1];
				result = existing.color;
			}
		}
		ZeroConfig.save();
		return result;
	}

	private static String normalizeId(String match) {
		String trimmed = match.trim().toLowerCase(Locale.ROOT);
		if (!trimmed.startsWith("#") && !trimmed.contains("*") && !trimmed.contains(":")) {
			return "minecraft:" + trimmed;
		}
		return trimmed;
	}

	private static List<CompiledRule> rules() {
		List<HighlightRule> source = ZeroConfig.get().highlightRules;
		int hash = 1;
		for (HighlightRule rule : source) {
			hash = 31 * hash + (rule.match + "|" + rule.color).hashCode();
		}
		if (source != compiledFrom || hash != compiledHash) {
			List<CompiledRule> list = new ArrayList<>();
			for (HighlightRule rule : source) {
				CompiledRule compiledRule = CompiledRule.compile(rule);
				if (compiledRule != null) {
					list.add(compiledRule);
				}
			}
			compiled = list;
			compiledFrom = source;
			compiledHash = hash;
		}
		return compiled;
	}

	private record CompiledRule(Identifier id, TagKey<Item> tag, Pattern glob, int color) {
		static CompiledRule compile(HighlightRule rule) {
			if (rule.match == null || rule.color == null) {
				return null;
			}
			int color = Colors.parse(rule.color, 0);
			if (color == 0) {
				return null;
			}
			if ((color >>> 24) == 0) {
				color |= 0xFF000000;
			}
			String match = normalizeId(rule.match);
			if (match.startsWith("#")) {
				Identifier tagId = Identifier.tryParse(match.substring(1));
				return tagId == null ? null : new CompiledRule(null, TagKey.create(Registries.ITEM, tagId), null, color);
			}
			if (match.contains("*")) {
				StringBuilder regex = new StringBuilder();
				for (String part : match.split("\\*", -1)) {
					if (!regex.isEmpty()) {
						regex.append(".*");
					}
					regex.append(Pattern.quote(part));
				}
				return new CompiledRule(null, null, Pattern.compile(regex.toString()), color);
			}
			Identifier id = Identifier.tryParse(match);
			return id == null ? null : new CompiledRule(id, null, null, color);
		}

		boolean matches(ItemStack stack) {
			if (tag != null) {
				return stack.typeHolder().is(tag);
			}
			Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (id != null) {
				return id.equals(key);
			}
			String full = key.toString();
			return glob.matcher(full).matches() || glob.matcher(key.getPath()).matches();
		}
	}
}
