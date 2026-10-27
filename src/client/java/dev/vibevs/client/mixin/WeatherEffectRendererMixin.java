package dev.vibevs.client.mixin;

import dev.vibevs.config.VibeConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "No Weather" skips the rain / snow columns and the splash particles that go with them,
 * "Snow" rewrites every precipitation column to snow.
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void vibevs$skipWeatherColumns(MultiBufferSource bufferSource, Vec3 cameraPosition,
			WeatherRenderState renderState, CallbackInfo callback) {
		VibeConfig config = VibeConfig.get();

		if (config.noWeather && !config.forceSnow) {
			callback.cancel();
		}
	}

	@Inject(method = "tickRainParticles", at = @At("HEAD"), cancellable = true)
	private void vibevs$skipRainSplashes(ClientLevel level, Camera camera, int ticks, ParticleStatus particleStatus,
			int radius, CallbackInfo callback) {
		if (VibeConfig.get().noWeather) {
			callback.cancel();
		}
	}

	@Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
	private void vibevs$forceSnow(Level level, BlockPos position, CallbackInfoReturnable<Biome.Precipitation> callback) {
		if (VibeConfig.get().forceSnow) {
			callback.setReturnValue(Biome.Precipitation.SNOW);
		}
	}
}
