package dev.zero.pvpthings.mixin;

import dev.zero.pvpthings.config.ZeroConfig;
import dev.zero.pvpthings.feature.ItemHighlighter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
	/** Highlight items in the hotbar too. */
	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void zeropvp$highlightHotbarSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed, CallbackInfo ci) {
		if (ZeroConfig.get().highlightHotbar) {
			ItemHighlighter.drawSlot(graphics, itemStack, x, y);
		}
	}
}
