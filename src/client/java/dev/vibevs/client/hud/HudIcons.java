package dev.vibevs.client.hud;

import dev.vibevs.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Tiny pixel-glyph icons drawn with plain GUI fills. Keeping them in code avoids texture atlases
 * that could clash with resource packs and keeps the HUD crisp at every GUI scale.
 */
public final class HudIcons {
	public enum Icon {
		FPS, PING, SPEED, COORDS, BIOME, FACING, ARMOR, CLOCK, SCORE, USER, TPS, BPS, KEY
	}

	private static final String[] BRAND = {
			"##....##",
			"##....##",
			".##..##.",
			".##..##.",
			"..####..",
			"..####..",
			"...##...",
			"........"
	};

	private static final String[] FPS = {
			"########",
			"#......#",
			"#..##..#",
			"#..##..#",
			"#......#",
			"########",
			"...##...",
			"..####.."
	};

	private static final String[] PING = {
			"........",
			"......#.",
			"......#.",
			"....#.#.",
			"....#.#.",
			"..#.#.#.",
			"..#.#.#.",
			"..#.#.#."
	};

	private static final String[] SPEED = {
			"........",
			"..#.....",
			"..##....",
			"..###...",
			"..####..",
			"..###...",
			"..##....",
			"..#....."
	};

	private static final String[] COORDS = {
			"........",
			"...##...",
			"...##...",
			"#..##..#",
			"#..##..#",
			"...##...",
			"...##...",
			"........"
	};

	private static final String[] BIOME = {
			"....##..",
			"..#####.",
			".#######",
			"########",
			"#######.",
			".#####..",
			"..###...",
			"...#...."
	};

	private static final String[] FACING = {
			"...##...",
			"..####..",
			".######.",
			"########",
			"...##...",
			"...##...",
			"...##...",
			"...##..."
	};

	private static final String[] ARMOR = {
			"..####..",
			".######.",
			"##.##.##",
			"##.##.##",
			"########",
			".######.",
			".##..##.",
			".##..##."
	};

	private static final String[] CLOCK = {
			"..####..",
			".#....#.",
			"#..#...#",
			"#..#...#",
			"#..###.#",
			"#......#",
			".#....#.",
			"..####.."
	};

	private static final String[] SCORE = {
			"########",
			"#......#",
			"#.####.#",
			"#......#",
			"#.####.#",
			"#......#",
			"#.####.#",
			"########"
	};

	private static final String[] USER = {
			"...##...",
			"..####..",
			"...##...",
			"..####..",
			".######.",
			"##.##.##",
			"...##...",
			"..####.."
	};

	private static final String[] TPS = {
			"#.....#.",
			"##...##.",
			".##.##..",
			"..###...",
			"...#....",
			"..###...",
			".##.##..",
			"##...##."
	};

	private static final String[] BPS = {
			".....#..",
			"....##..",
			"...###..",
			"..####..",
			"..####..",
			"...#....",
			"...#....",
			"...#...."
	};

	private static final String[] KEY = {
			"..####..",
			".#....#.",
			".#.##.#.",
			".#.##.#.",
			".#....#.",
			".#....#.",
			"..####..",
			"........"
	};

	private HudIcons() {
	}

	/** The VibeVisuals V-mark, vertically graded from cyan to magenta. */
	public static void brand(GuiGraphics graphics, int x, int y, int topColor, int bottomColor) {
		gradientGlyph(graphics, x, y, BRAND, topColor, bottomColor);
	}

	public static void icon(GuiGraphics graphics, Icon icon, int x, int y, int color) {
		glyph(graphics, x, y, switch (icon) {
			case FPS -> FPS;
			case PING -> PING;
			case SPEED -> SPEED;
			case COORDS -> COORDS;
			case BIOME -> BIOME;
			case FACING -> FACING;
			case ARMOR -> ARMOR;
			case CLOCK -> CLOCK;
			case SCORE -> SCORE;
			case USER -> USER;
			case TPS -> TPS;
			case BPS -> BPS;
			case KEY -> KEY;
		}, color);
	}

	private static void gradientGlyph(GuiGraphics graphics, int x, int y, String[] rows,
			int topColor, int bottomColor) {
		float denominator = Math.max(1, rows.length - 1);

		for (int row = 0; row < rows.length; row++) {
			int color = Colors.mix(topColor, bottomColor, row / denominator);
			drawRow(graphics, x, y + row, rows[row], color);
		}
	}

	private static void glyph(GuiGraphics graphics, int x, int y, String[] rows, int color) {
		for (int row = 0; row < rows.length; row++) {
			drawRow(graphics, x, y + row, rows[row], color);
		}
	}

	private static void drawRow(GuiGraphics graphics, int x, int y, String row, int color) {
		for (int column = 0; column < row.length(); column++) {
			if (row.charAt(column) == '#') {
				graphics.fill(x + column, y, x + column + 1, y + 1, color);
			}
		}
	}
}
