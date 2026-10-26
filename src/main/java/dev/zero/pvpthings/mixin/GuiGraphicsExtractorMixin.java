package dev.zero.pvpthings.mixin;

import dev.zero.pvpthings.feature.InventoryScaler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While a scaled screen is drawn, report the virtual size so tooltips and backgrounds stay on screen.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@Inject(method = "guiWidth", at = @At("RETURN"), cancellable = true)
	private void zeropvp$scaledWidth(CallbackInfoReturnable<Integer> cir) {
		float scale = InventoryScaler.renderScale();
		if (scale != 1f) {
			cir.setReturnValue(Math.round(cir.getReturnValue() / scale));
		}
	}

	@Inject(method = "guiHeight", at = @At("RETURN"), cancellable = true)
	private void zeropvp$scaledHeight(CallbackInfoReturnable<Integer> cir) {
		float scale = InventoryScaler.renderScale();
		if (scale != 1f) {
			cir.setReturnValue(Math.round(cir.getReturnValue() / scale));
		}
	}
}
