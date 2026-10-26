package dev.zero.pvpthings.mixin;

import dev.zero.pvpthings.feature.ItemHighlighter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	/** Draw the highlight behind the item (the pose is already translated to the container origin). */
	@Inject(method = "extractSlot", at = @At("HEAD"))
	private void zeropvp$highlightSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		ItemHighlighter.drawSlot(graphics, slot.getItem(), slot.x, slot.y);
	}
}
