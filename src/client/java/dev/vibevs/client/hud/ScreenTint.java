package dev.vibevs.client.hud;

import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Full screen colour filter driven by {@code tintHue} / {@code tintStrength}. Registered as the
 * very first HUD element so every vanilla element is drawn on top of it.
 */
public class ScreenTint implements HudElement {
	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();

		if (!config.hudEnabled || config.tintStrength <= 0.0f) {
			return;
		}

		int alpha = Math.round(Math.min(100.0f, config.tintStrength) * 2.55f);
		int color = Colors.withAlpha(Colors.hsb(config.tintHue, 1.0f, 1.0f), alpha);
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
	}
}
