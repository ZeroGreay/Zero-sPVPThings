package dev.zero.pvpthings.config;

import java.util.ArrayList;
import java.util.List;

/**
 * One item highlight rule.
 * <p>
 * {@code match} can be:
 * <ul>
 *     <li>an item id: {@code minecraft:golden_apple} (namespace optional)</li>
 *     <li>an item tag: {@code #minecraft:swords}</li>
 *     <li>a glob on the item id: {@code *_potion}, {@code minecraft:diamond_*}</li>
 * </ul>
 * {@code color} is a hex string, {@code #RRGGBB} or {@code #AARRGGBB}.
 */
public final class HighlightRule {
	public String match;
	public String color;

	public HighlightRule() {
	}

	public HighlightRule(String match, String color) {
		this.match = match;
		this.color = color;
	}

	public static List<HighlightRule> defaults() {
		List<HighlightRule> rules = new ArrayList<>();
		rules.add(new HighlightRule("minecraft:enchanted_golden_apple", "#FF55FF"));
		rules.add(new HighlightRule("minecraft:golden_apple", "#FFD700"));
		rules.add(new HighlightRule("minecraft:totem_of_undying", "#FFFF55"));
		rules.add(new HighlightRule("minecraft:ender_pearl", "#1ABC9C"));
		rules.add(new HighlightRule("minecraft:splash_potion", "#FF5555"));
		rules.add(new HighlightRule("minecraft:potion", "#E67E22"));
		rules.add(new HighlightRule("minecraft:end_crystal", "#C77DFF"));
		rules.add(new HighlightRule("minecraft:obsidian", "#5B3FA0"));
		rules.add(new HighlightRule("minecraft:respawn_anchor", "#8E44AD"));
		rules.add(new HighlightRule("minecraft:glowstone", "#F1C40F"));
		rules.add(new HighlightRule("minecraft:experience_bottle", "#7CFC00"));
		rules.add(new HighlightRule("minecraft:cobweb", "#DDDDDD"));
		rules.add(new HighlightRule("#minecraft:swords", "#55FFFF"));
		rules.add(new HighlightRule("*_axe", "#3498DB"));
		rules.add(new HighlightRule("minecraft:mace", "#95A5A6"));
		return rules;
	}
}
