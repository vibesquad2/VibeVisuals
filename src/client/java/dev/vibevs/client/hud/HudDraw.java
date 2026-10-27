package dev.vibevs.client.hud;

import dev.vibevs.util.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Neon/cyberpunk drawing kit shared by every screen and HUD element. Panels are chamfered
 * (top-left and bottom-right corners cut), carry a thin neon rail, corner brackets, scanlines and
 * a soft outer glow so the whole interface reads as one visual system.
 */
public final class HudDraw {
	public static final int PAD = 5;
	public static final int HEIGHT = 14;
	public static final int HUD_HEADER = 17;

	public static final int CYAN = 0xFF00E5FF;
	public static final int MAGENTA = 0xFFFF2BD6;
	public static final int VIOLET = 0xFF8A5CFF;
	public static final int LIME = 0xFFB6FF00;
	public static final int AMBER = 0xFFFFC400;
	public static final int DANGER = 0xFFFF3860;
	public static final int TEXT = 0xFFE8FBFF;
	public static final int TEXT_DIM = 0xFF6F8FA6;
	public static final int PANEL = 0xF0070A14;
	public static final int PANEL_SOFT = 0xE60B101C;

	private HudDraw() {
	}

	/** Neon panel with the default cyan accent, {@code HEIGHT} pixels tall. */
	public static void panel(GuiGraphics graphics, int x, int y, int width) {
		panel(graphics, x, y, width, HEIGHT, CYAN);
	}

	/** Neon panel with a custom height and accent. */
	public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
		if (width <= 0 || height <= 0) {
			return;
		}

		glow(graphics, x, y, width, height, accent);
		cutFill(graphics, x, y, width, height, 3, PANEL);
		graphics.fillGradient(x + 3, y + 1, x + width - 3, y + height - 1,
				Colors.withAlpha(accent, 0x12), 0x00000000);
		scanlines(graphics, x + 4, y + 4, width - 8, height - 6, 3, 0x05FFFFFF);
		graphics.fill(x + 1, y + 4, x + 2, y + height - 4, accent);
		gradientFill(graphics, x + 4, y, width - 8, 1, Colors.withAlpha(accent, 0xD0),
				Colors.withAlpha(accent, 0x00));

		int bracket = Colors.withAlpha(accent, 0xB0);
		graphics.fill(x + width - 8, y + 1, x + width - 3, y + 2, bracket);
		graphics.fill(x + width - 3, y + 1, x + width - 2, y + 7, bracket);
		int dimBracket = Colors.withAlpha(accent, 0x70);
		graphics.fill(x + 1, y + height - 7, x + 2, y + height - 2, dimBracket);
		graphics.fill(x + 1, y + height - 2, x + 7, y + height - 1, dimBracket);
	}

	/**
	 * The redesigned HUD surface: a dark glass card with a chromatic left rail, a faint grid, a
	 * hairline highlight and a compact outer shadow. Kept separate from {@link #panel} so the
	 * ClickGUI keeps its flatter look.
	 */
	public static void glassPanel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int cut = 4;
		cutFill(graphics, x + 2, y + 2, width, height, cut, 0x4A000000);
		glow(graphics, x, y, width, height, accent);
		cutFill(graphics, x, y, width, height, cut, 0xF0070B13);
		graphics.fillGradient(x + 3, y + 1, x + width - 3, y + height - 1,
				Colors.withAlpha(accent, 0x12), 0x00000000);

		for (int row = 3; row < height - 2; row += 4) {
			graphics.fill(x + 4, y + row, x + width - 4, y + row + 1, 0x03FFFFFF);
		}

		for (int row = 0; row < height; row++) {
			int rail = Colors.mix(accent, VIOLET, row / (float) Math.max(1, height - 1));
			int alpha = row < 4 ? 0xE0 : 0xB8;
			graphics.fill(x + 1, y + row, x + 3, y + row + 1, Colors.withAlpha(rail, alpha));
		}

		gradientFill(graphics, x + 5, y, width - 10, 1, Colors.withAlpha(accent, 0xC8),
				Colors.withAlpha(accent, 0x00));
		graphics.fill(x + 3, y + 1, x + width - 3, y + 2, Colors.withAlpha(accent, 0x12));
		graphics.fill(x + 3, y + height - 2, x + width - 3, y + height - 1, 0x14FFFFFF);
		graphics.fill(x + width - 2, y + 3, x + width - 1, y + height - 3,
				Colors.withAlpha(accent, 0x16));

		int bracket = Colors.withAlpha(accent, 0x78);
		graphics.fill(x + width - 9, y + 1, x + width - 3, y + 2, bracket);
		graphics.fill(x + width - 3, y + 1, x + width - 2, y + 8, bracket);
		int dimBracket = Colors.withAlpha(accent, 0x4C);
		graphics.fill(x + 3, y + height - 7, x + 4, y + height - 2, dimBracket);
		graphics.fill(x + 3, y + height - 2, x + 9, y + height - 1, dimBracket);
	}

	/**
	 * Rounded glass chip used by the ported IM-style HUD (watermark, armor strip, cooldown,
	 * staff list and scoreboard). It keeps the neon accent but drops the chamfered corners.
	 */
	public static void softPanel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
		if (width <= 0 || height <= 0) {
			return;
		}

		roundFill(graphics, x - 1, y - 1, width + 2, height + 2, 4, Colors.withAlpha(accent, 0x1C));
		roundFill(graphics, x, y, width, height, 3, 0xE6090D16);
		gradientFill(graphics, x + 3, y, width - 6, 1, Colors.withAlpha(accent, 0xA0), 0x00000000);
		graphics.fill(x + 1, y + 2, x + 2, y + height - 2, Colors.withAlpha(accent, 0x90));
		graphics.fill(x + 3, y + height - 1, x + width - 3, y + height, 0x14FFFFFF);
		roundFill(graphics, x + 1, y + 1, width - 2, 1, 1, Colors.withAlpha(accent, 0x30));
	}

	/** Rounded header used by the ported panels: pixel icon, uppercase title and a count. */
	public static void softHeader(GuiGraphics graphics, Font font, int x, int y, int width,
			String title, String right, HudIcons.Icon icon, int accent) {
		HudIcons.icon(graphics, icon, x + 7, y + 5, accent);
		Fonts.draw(graphics, font, title, x + 20, y + 4, TEXT);

		if (right != null && !right.isEmpty()) {
			Fonts.draw(graphics, font, right,
					x + width - 7 - Fonts.width(font, right), y + 4, TEXT_DIM);
		}

		gradientFill(graphics, x + 6, y + HUD_HEADER - 2, width - 12, 1,
				Colors.withAlpha(accent, 0xA8), 0x00000000);
		graphics.fill(x + 6, y + HUD_HEADER - 2, x + 26, y + HUD_HEADER - 1,
				Colors.withAlpha(accent, 0xD0));
	}

	/** Standalone rounded glass row, used for chips and individual HUD rows. */
	public static void chip(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
		softPanel(graphics, x, y, width, height, accent);
		graphics.fill(x + 4, y + height - 1, x + width - 4, y + height, 0x0DFFFFFF);
	}

	public static void hudHeader(GuiGraphics graphics, Font font, int x, int y, int width,
			String title, String right, int accent) {
		Fonts.draw(graphics, font, "//", x + 7, y + 5, Colors.withAlpha(accent, 0xE0));
		Fonts.draw(graphics, font, title, x + 18, y + 5, TEXT);

		if (right != null && !right.isEmpty()) {
			Fonts.draw(graphics, font, right,
					x + width - 7 - Fonts.width(font, right), y + 5, TEXT_DIM);
		}

		gradientFill(graphics, x + 7, y + HUD_HEADER - 2, width - 14, 1,
				Colors.withAlpha(accent, 0xA8), 0x00000000);
		graphics.fill(x + 7, y + HUD_HEADER - 2, x + 27, y + HUD_HEADER - 1,
				Colors.withAlpha(accent, 0xD0));
	}

	/** Pulsing live marker used by the brand and status cards. */
	public static void liveDot(GuiGraphics graphics, int x, int y, int accent) {
		float pulse = pulse(0.0f);
		int halo = 0x20 + Math.round(pulse * 0x38);
		int core = 0xC0 + Math.round(pulse * 0x3F);
		diamond(graphics, x, y, 2, Colors.withAlpha(accent, halo));
		diamond(graphics, x, y, 1, Colors.withAlpha(accent, Math.min(0xFF, core)));
	}

	/** A slow global breathing value in {@code 0..1}. */
	public static float pulse(float phase) {
		return 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 500.0 + phase);
	}

	/** Small moving highlight along a 1px edge. */
	public static void sheen(GuiGraphics graphics, int x, int y, int width, int accent, float phase) {
		if (width <= 0) {
			return;
		}

		int length = Math.min(24, width);
		int travel = (int) ((System.currentTimeMillis() * 0.035 + phase * 130.0) % (width + length)) - length;

		for (int i = 0; i < length; i += 2) {
			int pixel = travel + i;

			if (pixel < 0 || pixel >= width) {
				continue;
			}

			float wave = (float) Math.sin(i / (double) Math.max(1, length - 1) * Math.PI);
			graphics.fill(x + pixel, y, Math.min(x + pixel + 2, x + width), y + 1,
					Colors.withAlpha(accent, Math.round(wave * 0x70)));
		}
	}

	/** Soft two-layer neon halo behind a rectangle. */
	public static void glow(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
		graphics.fill(x - 2, y - 1, x + width + 2, y + height + 1, Colors.withAlpha(accent, 0x12));
		graphics.fill(x - 1, y, x + width + 1, y + height, Colors.withAlpha(accent, 0x0D));
	}

	/** Chamfered rectangle: the top-left and bottom-right corners are cut diagonally. */
	public static void cutFill(GuiGraphics graphics, int x, int y, int width, int height, int cutSize, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int cut = Math.max(0, Math.min(cutSize, Math.min(width / 2, height / 2)));

		if (cut == 0) {
			graphics.fill(x, y, x + width, y + height, color);
			return;
		}

		for (int row = 0; row < height; row++) {
			int left = row < cut ? cut - row : 0;
			int right = row >= height - cut ? row - (height - cut) + 1 : 0;
			int x1 = x + left;
			int x2 = x + width - right;

			if (x2 > x1) {
				graphics.fill(x1, y + row, x2, y + row + 1, color);
			}
		}
	}

	/** Chamfered neon button: bright border, dark glass fill, glow on hover. */
	public static void button(GuiGraphics graphics, int x, int y, int width, int height, int accent,
			boolean hovered, int fill) {
		if (hovered) {
			glow(graphics, x, y, width, height, accent);
		}

		cutFill(graphics, x, y, width, height, 3, Colors.withAlpha(accent, hovered ? 0xE6 : 0x66));
		cutFill(graphics, x + 1, y + 1, width - 2, height - 2, 2, fill);
		gradientFill(graphics, x + 4, y + 1, width - 8, 1, Colors.withAlpha(accent, hovered ? 0xE0 : 0x50),
				0x00000000);

		if (hovered) {
			graphics.fill(x + 2, y + 4, x + 3, y + height - 4, accent);
		}
	}

	/** Horizontal colour ramp used for neon edges, tracks and progress bars. */
	public static void gradientFill(GuiGraphics graphics, int x, int y, int width, int height, int from, int to) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int steps = Math.min(32, Math.max(1, width));

		for (int step = 0; step < steps; step++) {
			int x1 = x + step * width / steps;
			int x2 = x + (step + 1) * width / steps;

			if (x2 > x1) {
				graphics.fill(x1, y, x2, y + height, Colors.mix(from, to, step / (float) Math.max(1, steps - 1)));
			}
		}
	}

	public static void scanlines(GuiGraphics graphics, int x, int y, int width, int height, int step, int color) {
		if (width <= 0 || height <= 0 || step <= 0) {
			return;
		}

		for (int row = 0; row < height; row += step) {
			graphics.fill(x, y + row, x + width, y + row + 1, color);
		}
	}

	/** Fully rounded fill, kept for toggle knobs and small circular details. */
	public static void roundFill(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
		int r = Math.min(radius, height / 2);

		for (int row = 0; row < height; row++) {
			int inset = 0;

			if (row < r) {
				inset = r - row - 1;
			} else if (row >= height - r) {
				inset = r - (height - row - 1) - 1;
			}

			inset = Math.max(0, Math.min(Math.max(0, r - 1), inset));
			graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
		}
	}

	/** Small filled diamond, the recurring neon marker. */
	public static void diamond(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
		for (int row = -radius; row <= radius; row++) {
			int half = radius - Math.abs(row);
			graphics.fill(centerX - half, centerY + row, centerX + half + 1, centerY + row + 1, color);
		}
	}

	/** L-shaped corner brackets around a rectangle, used by the HUD editor. */
	public static void corners(GuiGraphics graphics, int x, int y, int width, int height, int length, int color) {
		int right = x + width;
		int bottom = y + height;
		graphics.fill(x, y, x + length, y + 1, color);
		graphics.fill(x, y, x + 1, y + length, color);
		graphics.fill(right - length, y, right, y + 1, color);
		graphics.fill(right - 1, y, right, y + length, color);
		graphics.fill(x, bottom - 1, x + length, bottom, color);
		graphics.fill(x, bottom - length, x + 1, bottom, color);
		graphics.fill(right - length, bottom - 1, right, bottom, color);
		graphics.fill(right - 1, bottom - length, right, bottom, color);
	}
}
