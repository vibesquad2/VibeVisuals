package dev.vibevs.client.mixin;

import com.mojang.blaze3d.buffers.Std140Builder;
import dev.vibevs.config.VibeConfig;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lightmap tweaks.
 *
 * <p>The light texture shader mixes in a "not gamma" correction with the brightness the options
 * slider provides, so pushing that value past 1.0 is what makes caves fully lit. Night vision uses
 * the very same night vision factor vanilla applies for the potion effect.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	private static final float FULLBRIGHT_BRIGHTNESS = 8.0f;

	@Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
	private static float vibevs$brightness(float zero, float brightness) {
		if (VibeConfig.get().fullbright) {
			return Math.max(brightness, FULLBRIGHT_BRIGHTNESS);
		}

		return Math.max(zero, brightness);
	}

	/** Uniform slot 4 of the lightmap UBO is the night vision factor. */
	@Redirect(method = "updateLightTexture", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
			ordinal = 3))
	private static Std140Builder vibevs$nightVision(Std140Builder builder, float nightVisionFactor) {
		return builder.putFloat(VibeConfig.get().nightVision ? 1.0f : nightVisionFactor);
	}
}
