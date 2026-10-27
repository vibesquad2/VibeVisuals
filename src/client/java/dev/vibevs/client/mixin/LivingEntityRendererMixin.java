package dev.vibevs.client.mixin;

import dev.vibevs.config.VibeConfig;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes the red damage tint from entity models. The dying tint is kept so mobs still look like
 * they are dying.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "getOverlayCoords", at = @At("HEAD"), cancellable = true)
	private static void vibevs$noDamageTint(LivingEntityRenderState state, float whiteOverlayProgress,
			CallbackInfoReturnable<Integer> callback) {
		if (VibeConfig.get().hideHurtOverlay && state.hasRedOverlay && state.deathTime <= 0.0f) {
			callback.setReturnValue(OverlayTexture.NO_OVERLAY);
		}
	}
}
