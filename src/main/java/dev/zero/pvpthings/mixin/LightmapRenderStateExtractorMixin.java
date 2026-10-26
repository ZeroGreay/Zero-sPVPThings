package dev.zero.pvpthings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.zero.pvpthings.config.ZeroConfig;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces the brightness ("gamma") option value fed into the lightmap, allowing values far above 100%.
 */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
	// ordinal 0 is the gamma() value, ordinal 1 is darknessEffectScale()
	@ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0))
	private float zeropvp$overrideGamma(float brightness) {
		ZeroConfig config = ZeroConfig.get();
		return config.gammaEnabled ? config.gammaPercent / 100f : brightness;
	}
}
