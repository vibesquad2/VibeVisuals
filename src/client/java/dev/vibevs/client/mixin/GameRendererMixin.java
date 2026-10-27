package dev.vibevs.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vibevs.client.feature.Zoom;
import dev.vibevs.config.VibeConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zoom (FOV multiplier) plus the camera hooks for the damage tilt and the optional shake.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void vibevs$applyZoom(Camera camera, float partialTick, boolean useFovSetting,
			CallbackInfoReturnable<Float> callback) {
		float multiplier = Zoom.multiplier();

		if (multiplier != 1.0f) {
			callback.setReturnValue(callback.getReturnValueF() * multiplier);
		}
	}

	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void vibevs$cancelHurtTilt(PoseStack poseStack, float partialTick, CallbackInfo callback) {
		if (VibeConfig.get().noHurtCamera) {
			callback.cancel();
		}
	}

	@Inject(method = "bobHurt", at = @At("TAIL"))
	private void vibevs$shakeOnDamage(PoseStack poseStack, float partialTick, CallbackInfo callback) {
		VibeConfig config = VibeConfig.get();

		if (!config.cameraShake || config.noHurtCamera) {
			return;
		}

		Minecraft client = Minecraft.getInstance();

		if (client.player == null) {
			return;
		}

		float hurt = client.player.hurtTime - partialTick;

		if (hurt <= 0.0f) {
			return;
		}

		float amount = Math.min(1.0f, hurt / 10.0f);
		float time = (client.player.tickCount + partialTick) * 3.5f;
		poseStack.mulPose(new Quaternionf()
				.rotateZ((float) Math.sin(time) * 0.028f * amount)
				.rotateX((float) Math.cos(time * 1.3f) * 0.014f * amount));
	}
}
