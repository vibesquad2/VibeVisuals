package dev.vibevs.client.hud;

import dev.vibevs.client.feature.TargetTracker;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The custom crosshair: four razor-thin arms, a diamond core and a faint outer ring. It turns
 * magenta and breathes outward when a living target is under the crosshair, which gives the player
 * an instant read without adding another panel to the screen.
 */
public class CrosshairHud implements HudElement {
	private static final int OUTLINE_COLOR = 0xB4000000;

	@Nullable
	private HudRect lastBounds;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();

		if (!HudEditState.previewing() && (!config.hudEnabled || !config.customCrosshair)) {
			this.lastBounds = null;
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;

		if (player == null || player.isSpectator()) {
			this.lastBounds = null;
			return;
		}

		LivingEntity target = TargetTracker.target() instanceof LivingEntity living && living.isAlive()
				? living
				: null;
		boolean locked = target != null;
		int accent = locked ? HudDraw.MAGENTA : HudDraw.CYAN;
		float pulse = HudDraw.pulse(0.0f);
		float size = Math.max(0.5f, config.crosshairSize);
		int thickness = Math.max(1, Math.round(size));
		int gap = Math.max(2, Math.round((locked ? 3.2f : 2.2f) * size));
		int arm = Math.max(3, Math.round((locked ? 5.5f : 4.2f) * size));
		int centerX = config.hudCrosshairX >= 0.0f ? Math.round(config.hudCrosshairX) : graphics.guiWidth() / 2;
		int centerY = config.hudCrosshairY >= 0.0f ? Math.round(config.hudCrosshairY) : graphics.guiHeight() / 2;
		int half = thickness / 2;
		int extent = gap + arm + 2;
		int ring = extent + 4;
		centerX = Math.max(ring, Math.min(Math.max(ring, graphics.guiWidth() - ring), centerX));
		centerY = Math.max(ring, Math.min(Math.max(ring, graphics.guiHeight() - ring), centerY));

		int halo = Colors.withAlpha(accent, locked ? 0x30 : 0x1C);
		fill(graphics, centerX - gap - arm - 2, centerY - half - 2, centerX - gap + 2,
				centerY - half + thickness + 2, halo);
		fill(graphics, centerX + gap - 2, centerY - half - 2, centerX + gap + arm + 2,
				centerY - half + thickness + 2, halo);
		fill(graphics, centerX - half - 2, centerY - gap - arm - 2, centerX - half + thickness + 2,
				centerY - gap + 2, halo);
		fill(graphics, centerX - half - 2, centerY + gap - 2, centerX - half + thickness + 2,
				centerY + gap + arm + 2, halo);

		fill(graphics, centerX - gap - arm - 1, centerY - half - 1, centerX - gap + 1,
				centerY - half + thickness + 1, OUTLINE_COLOR);
		fill(graphics, centerX + gap - 1, centerY - half - 1, centerX + gap + arm + 1,
				centerY - half + thickness + 1, OUTLINE_COLOR);
		fill(graphics, centerX - half - 1, centerY - gap - arm - 1, centerX - half + thickness + 1,
				centerY - gap + 1, OUTLINE_COLOR);
		fill(graphics, centerX - half - 1, centerY + gap - 1, centerX - half + thickness + 1,
				centerY + gap + arm + 1, OUTLINE_COLOR);

		fill(graphics, centerX - gap - arm, centerY - half, centerX - gap, centerY - half + thickness, accent);
		fill(graphics, centerX + gap, centerY - half, centerX + gap + arm, centerY - half + thickness, accent);
		fill(graphics, centerX - half, centerY - gap - arm, centerX - half + thickness, centerY - gap, accent);
		fill(graphics, centerX - half, centerY + gap, centerX - half + thickness, centerY + gap + arm, accent);

		// Bright inner tips keep the arms legible over bright blocks.
		fill(graphics, centerX - gap - 1, centerY - half, centerX - gap,
				centerY - half + thickness, Colors.withAlpha(HudDraw.TEXT, 0xD0));
		fill(graphics, centerX + gap, centerY - half, centerX + gap + 1,
				centerY - half + thickness, Colors.withAlpha(HudDraw.TEXT, 0xD0));
		fill(graphics, centerX - half, centerY - gap - 1, centerX - half + thickness,
				centerY - gap, Colors.withAlpha(HudDraw.TEXT, 0xD0));
		fill(graphics, centerX - half, centerY + gap, centerX - half + thickness,
				centerY + gap + 1, Colors.withAlpha(HudDraw.TEXT, 0xD0));

		int dotAlpha = locked ? 0x48 + Math.round(pulse * 0x58) : 0x2C;
		HudDraw.diamond(graphics, centerX, centerY - ring, 1, Colors.withAlpha(accent, dotAlpha));
		HudDraw.diamond(graphics, centerX, centerY + ring, 1, Colors.withAlpha(accent, dotAlpha));
		HudDraw.diamond(graphics, centerX - ring, centerY, 1, Colors.withAlpha(accent, dotAlpha));
		HudDraw.diamond(graphics, centerX + ring, centerY, 1, Colors.withAlpha(accent, dotAlpha));

		HudDraw.diamond(graphics, centerX, centerY, locked ? 2 : 1,
				Colors.withAlpha(accent, locked ? 0x38 + Math.round(pulse * 0x30) : 0x20));
		HudDraw.diamond(graphics, centerX, centerY, 1, locked ? HudDraw.LIME : HudDraw.MAGENTA);

		int bound = ring + 2;
		this.lastBounds = new HudRect(centerX - bound, centerY - bound, bound * 2, bound * 2);
	}

	/** Bounds of the last rendered crosshair, for the HUD editor. */
	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}

	private static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
		if (x2 > x1 && y2 > y1) {
			graphics.fill(x1, y1, x2, y2, color);
		}
	}
}
