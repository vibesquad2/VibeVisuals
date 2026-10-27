package dev.vibevs.client.feature;

import dev.vibevs.client.VibeVisualsClient;
import dev.vibevs.config.VibeConfig;
import net.minecraft.client.Minecraft;

/**
 * Hold-to-zoom. The actual FOV change happens in
 * {@code dev.vibevs.client.mixin.GameRendererMixin}, this class only tracks the (optionally smoothed)
 * multiplier.
 */
public final class Zoom {
	private static final float SNAP = 0.001f;
	private static final float SMOOTHING = 0.35f;

	private static float current = 1.0f;

	private Zoom() {
	}

	public static void tick(Minecraft client) {
		VibeConfig config = VibeConfig.get();
		boolean down = config.zoom && VibeVisualsClient.ZOOM.isDown();
		float target = down ? config.zoomFactor : 1.0f;

		if (config.smoothZoom) {
			current += (target - current) * SMOOTHING;
		} else {
			current = target;
		}

		if (Math.abs(current - target) < SNAP) {
			current = target;
		}
	}

	/** Multiplier applied to the final FOV. 1.0 = untouched. */
	public static float multiplier() {
		return current;
	}

	public static boolean active() {
		return Math.abs(current - 1.0f) > SNAP;
	}
}
