package dev.zero.pvpthings.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.zero.pvpthings.feature.InventoryScaler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws scaled container screens with the pose scaled up/down. Mouse coordinates are already divided by
 * the scale in {@link MouseHandlerMixin}.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
	private void zeropvp$scaleScreen(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, Operation<Void> original) {
		float scale = InventoryScaler.scaleFor(screen);
		if (scale == 1f) {
			original.call(screen, graphics, mouseX, mouseY, a);
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		InventoryScaler.setRenderScale(scale);
		try {
			original.call(screen, graphics, mouseX, mouseY, a);
		} finally {
			InventoryScaler.setRenderScale(1f);
			graphics.pose().popMatrix();
		}
	}
}
