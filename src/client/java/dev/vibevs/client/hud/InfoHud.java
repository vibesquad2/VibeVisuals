package dev.vibevs.client.hud;

import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The bottom-left Information overlay from the archive: two very compact lines with TPS and ping
 * on top of position and speed. Unlike the old status card it has no title and no grid; it is a
 * quiet text-only readout with a soft dark backing for readability over the world.
 */
public class InfoHud implements HudElement {
	private static final int PAD = 5;
	private static final int LINE_HEIGHT = 10;
	private static final int HEIGHT = 24;

	@Nullable
	private HudRect lastBounds;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();

		if (!HudEditState.previewing() && (!config.hudEnabled || !config.hudInfo)) {
			this.lastBounds = null;
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;

		if (player == null && !HudEditState.previewing()) {
			this.lastBounds = null;
			return;
		}

		boolean preview = player == null || client.level == null;
		Font font = client.font;
		List<Segment> top = new ArrayList<>();
		List<Segment> bottom = new ArrayList<>();

		top.add(new Segment("TPS: ", HudDraw.CYAN));
		top.add(new Segment(HudMetrics.decimal(preview ? 20.0 : HudMetrics.tps(), 1), HudDraw.TEXT));

		if (config.hudShowPing || preview) {
			top.add(new Segment("  |  ", HudDraw.TEXT_DIM));
			top.add(new Segment("Ping: ", HudDraw.CYAN));
			top.add(new Segment((preview ? 12 : HudMetrics.ping(client)) + " ms", HudDraw.TEXT));
		}

		if ((config.hudShowCoords || preview) && player != null) {
			bottom.add(new Segment("Pos: ", HudDraw.MAGENTA));
			bottom.add(new Segment(String.format(Locale.ROOT, "%.1f, %.1f, %.1f",
					player.getX(), player.getY(), player.getZ()), HudDraw.TEXT));
		} else if (preview) {
			bottom.add(new Segment("Pos: ", HudDraw.MAGENTA));
			bottom.add(new Segment("120.0, 64.0, -300.0", HudDraw.TEXT));
		}

		if ((config.hudShowSpeed || preview) && player != null) {
			if (!bottom.isEmpty()) {
				bottom.add(new Segment("  |  ", HudDraw.TEXT_DIM));
			}

			bottom.add(new Segment("Speed: ", HudDraw.VIOLET));
			bottom.add(new Segment(HudMetrics.decimal(preview ? 5.6 : HudMetrics.blocksPerSecond(player), 1)
					+ " bps", HudDraw.TEXT));
		} else if (preview) {
			bottom.add(new Segment("Speed: ", HudDraw.VIOLET));
			bottom.add(new Segment("5.6 bps", HudDraw.TEXT));
		}

		if (bottom.isEmpty()) {
			bottom.add(new Segment("VibeVisuals", HudDraw.TEXT_DIM));
		}

		int width = Math.max(width(font, top), width(font, bottom)) + PAD * 2;
		int x = config.hudInfoX >= 0.0f ? Math.round(config.hudInfoX) : 4;
		int y = config.hudInfoY >= 0.0f
				? Math.round(config.hudInfoY)
				: graphics.guiHeight() - HEIGHT - 4;
		x = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - width), x));
		y = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - HEIGHT), y));

		HudDraw.roundFill(graphics, x - 2, y - 2, width + 4, HEIGHT + 4, 4, 0x9A070A12);
		graphics.fill(x, y + 2, x + 1, y + HEIGHT - 2, Colors.withAlpha(HudDraw.CYAN, 0xB0));
		drawSegments(graphics, font, top, x + PAD, y + 3);
		drawSegments(graphics, font, bottom, x + PAD, y + 3 + LINE_HEIGHT);

		this.lastBounds = new HudRect(x - 2, y - 2, width + 4, HEIGHT + 4);
	}

	private static void drawSegments(GuiGraphics graphics, Font font, List<Segment> segments,
			int x, int y) {
		for (Segment segment : segments) {
			Fonts.draw(graphics, font, segment.text, x, y, segment.color);
			x += Fonts.width(font, segment.text);
		}
	}

	private static int width(Font font, List<Segment> segments) {
		int width = 0;

		for (Segment segment : segments) {
			width += Fonts.width(font, segment.text);
		}

		return width;
	}

	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}

	private record Segment(String text, int color) {
	}
}
