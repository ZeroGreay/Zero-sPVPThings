package dev.zero.pvpthings.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.zero.pvpthings.feature.InventoryScaler;
import dev.zero.pvpthings.hud.ClickTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	/** Mouse X in the virtual space of a scaled inventory. */
	@Inject(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("RETURN"), cancellable = true)
	private static void zeropvp$scaleMouseX(Window window, double x, CallbackInfoReturnable<Double> cir) {
		float scale = InventoryScaler.currentScale();
		if (scale != 1f) {
			cir.setReturnValue(cir.getReturnValue() / scale);
		}
	}

	@Inject(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("RETURN"), cancellable = true)
	private static void zeropvp$scaleMouseY(Window window, double y, CallbackInfoReturnable<Double> cir) {
		float scale = InventoryScaler.currentScale();
		if (scale != 1f) {
			cir.setReturnValue(cir.getReturnValue() / scale);
		}
	}

	/** Count clicks for the {cps} / {lcps} / {rcps} HUD variables. */
	@Inject(method = "onButton", at = @At("HEAD"))
	private void zeropvp$countClicks(long handle, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
		if (action == 1 && Minecraft.getInstance().gui.screen() == null) {
			ClickTracker.onClick(buttonInfo.button());
		}
	}
}
