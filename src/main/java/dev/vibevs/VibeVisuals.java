package dev.vibevs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point data for VibeVisuals.
 *
 * <p>Everything the mod does happens on the client, so there is no {@code main} entrypoint —
 * this class only holds shared constants and the logger.
 */
public final class VibeVisuals {
	public static final String MOD_ID = "vibevs";
	public static final String MOD_NAME = "VibeVisuals";
	public static final String WINDOW_TITLE = MOD_NAME + " - t.me/vibevisualss";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	private VibeVisuals() {
	}
}
