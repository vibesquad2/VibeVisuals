package dev.vibevs.client.mixin;

import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every sky / fog / star value the renderer reads goes through the environment attribute probe, so
 * this is the single place needed to recolour the sky, thicken the fog and brighten the stars.
 */
@Mixin(EnvironmentAttributeProbe.class)
public abstract class EnvironmentAttributeProbeMixin {
	private static final int SUNSET_COLOR = 0xFFFFA860;

	@Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
	private void vibevs$modifyEnvironment(EnvironmentAttribute<?> attribute, float partialTick,
			CallbackInfoReturnable<Object> callback) {
		VibeConfig config = VibeConfig.get();
		Object value = callback.getReturnValue();

		if (value == null) {
			return;
		}

		if (attribute == EnvironmentAttributes.SKY_COLOR && value instanceof Integer sky) {
			callback.setReturnValue(applySkyStyle(sky, config));
			return;
		}

		if (attribute == EnvironmentAttributes.FOG_COLOR && value instanceof Integer fog) {
			callback.setReturnValue(applySkyStyle(fog, config));
			return;
		}

		if (attribute == EnvironmentAttributes.STAR_BRIGHTNESS && value instanceof Float stars
				&& config.starBrightness != 1.0f) {
			callback.setReturnValue(Math.max(0.0f, stars * config.starBrightness));
			return;
		}

		if (value instanceof Float distance && isFogDistance(attribute)) {
			float modified = distance * config.fogDensity;

			if (!config.rainFog && isRaining()) {
				modified *= 1.75f;
			}

			if (modified != distance) {
				callback.setReturnValue(modified);
			}
		}
	}

	private static boolean isFogDistance(EnvironmentAttribute<?> attribute) {
		return attribute == EnvironmentAttributes.FOG_END_DISTANCE
				|| attribute == EnvironmentAttributes.SKY_FOG_END_DISTANCE
				|| attribute == EnvironmentAttributes.CLOUD_FOG_END_DISTANCE;
	}

	private static int applySkyStyle(int color, VibeConfig config) {
		int result = color;

		if (config.customSky && config.skyHueShift != 0.0f) {
			result = Colors.shiftHue(result, config.skyHueShift);
		}

		if (config.sunsetGlow) {
			result = Colors.mix(result, SUNSET_COLOR, 0.45f);
		}

		return result;
	}

	private static boolean isRaining() {
		Minecraft client = Minecraft.getInstance();
		return client.level != null && client.level.isRaining();
	}
}
