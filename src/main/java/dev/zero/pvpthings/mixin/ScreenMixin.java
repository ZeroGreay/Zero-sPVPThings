package dev.zero.pvpthings.mixin;

import dev.zero.pvpthings.feature.InventoryScaler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Lays scaled container screens out in a smaller (or bigger) virtual space: real size / scale.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@ModifyVariable(method = "init(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int zeropvp$scaleInitWidth(int width) {
		return zeropvp$scaled(width);
	}

	@ModifyVariable(method = "init(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int zeropvp$scaleInitHeight(int height) {
		return zeropvp$scaled(height);
	}

	@ModifyVariable(method = "resize(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int zeropvp$scaleResizeWidth(int width) {
		return zeropvp$scaled(width);
	}

	@ModifyVariable(method = "resize(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int zeropvp$scaleResizeHeight(int height) {
		return zeropvp$scaled(height);
	}

	private int zeropvp$scaled(int size) {
		float scale = InventoryScaler.scaleFor((Screen) (Object) this);
		return scale == 1f ? size : Math.round(size / scale);
	}
}
