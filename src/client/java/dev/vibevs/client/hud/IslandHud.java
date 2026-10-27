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
 * The IM-style watermark: a rounded brand tile with the Telegram handle, followed by two compact
 * rows of value chips (user, FPS, time, coordinates, ping, TPS and BPS). The chips wrap on narrow
 * GUI sizes, and every value respects its old visibility toggle.
 */
public class IslandHud implements HudElement {
	public static final String BRAND = "VIBEVISUALS";
	public static final String HANDLE = "t.me/vibevisualss";

	private static final int PAD = 6;
	private static final int LOGO = 18;
	private static final int CHIP_HEIGHT = 15;
	private static final int CHIP_GAP = 3;
	private static final int ROW_STEP = 18;
	private static final int MIN_BRAND_WIDTH = 112;
	private static final int MAX_ROWS = 4;

	@Nullable
	private HudRect lastBounds;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();

		if (!HudEditState.previewing() && (!config.hudEnabled || !config.hudIsland)) {
			this.lastBounds = null;
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		LocalPlayer player = client.player;
		boolean preview = HudEditState.previewing() && (player == null || client.level == null);
		List<Chip> chips = chips(config, client, player, preview);

		int brandWidth = Math.max(MIN_BRAND_WIDTH,
				PAD * 2 + LOGO + 8 + Math.max(Fonts.width(font, BRAND), Fonts.width(font, HANDLE)) + 12);
		int maxX = graphics.guiWidth() - 4;
		int startX = config.hudIslandX >= 0.0f ? Math.round(config.hudIslandX) : 4;
		int y = config.hudIslandY >= 0.0f ? Math.round(config.hudIslandY) : 4;
		startX = Math.max(0, Math.min(Math.max(0, maxX - brandWidth), startX));
		y = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - CHIP_HEIGHT), y));

		int available = Math.max(70, graphics.guiWidth() - (startX + brandWidth + CHIP_GAP) - 4);
		List<List<Chip>> rows = layout(chips, available);
		int rowCount = Math.max(1, rows.size());
		int brandHeight = Math.max(33, rowCount * ROW_STEP - CHIP_GAP);
		int watermarkWidth = brandWidth;

		if (!chips.isEmpty()) {
			int widestRow = 0;

			for (List<Chip> row : rows) {
				int rowWidth = rowWidth(row);
				widestRow = Math.max(widestRow, rowWidth);
			}

			watermarkWidth += CHIP_GAP + Math.min(available, widestRow);
		}

		int brandHeightFinal = Math.max(brandHeight, 33);
		drawBrand(graphics, font, startX, y, brandWidth, brandHeightFinal);

		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
			int chipX = startX + brandWidth + CHIP_GAP;
			int chipY = y + rowIndex * ROW_STEP;

			for (Chip chip : rows.get(rowIndex)) {
				drawChip(graphics, font, chipX, chipY, chip);
				chipX += chip.width + CHIP_GAP;
			}
		}

		this.lastBounds = new HudRect(startX, y, watermarkWidth,
				Math.max(brandHeightFinal, rowCount * ROW_STEP - CHIP_GAP));
	}

	private static void drawBrand(GuiGraphics graphics, Font font, int x, int y, int width, int height) {
		HudDraw.softPanel(graphics, x, y, width, height, HudDraw.CYAN);
		int logoY = y + 7;
		graphics.fill(x + PAD, logoY, x + PAD + 1, logoY + LOGO, Colors.withAlpha(HudDraw.CYAN, 0x70));
		graphics.fill(x + PAD, logoY, x + PAD + LOGO, logoY + 1, Colors.withAlpha(HudDraw.MAGENTA, 0x80));
		HudIcons.brand(graphics, x + PAD + 5, logoY + 5, HudDraw.CYAN, HudDraw.MAGENTA);

		int textX = x + PAD + LOGO + 8;
		Fonts.draw(graphics, font, BRAND, textX, y + 5, HudDraw.TEXT);
		Fonts.draw(graphics, font, HANDLE, textX, y + 16, 0xCC5FE9FF);
		HudDraw.gradientFill(graphics, textX, y + 28, Math.max(0, x + width - PAD - textX), 1,
				Colors.withAlpha(HudDraw.CYAN, 0x70), 0x00000000);
		HudDraw.liveDot(graphics, x + width - 9, y + 8, HudDraw.LIME);
	}

	private static void drawChip(GuiGraphics graphics, Font font, int x, int y, Chip chip) {
		HudDraw.chip(graphics, x, y, chip.width, CHIP_HEIGHT, chip.accent);
		HudIcons.icon(graphics, chip.icon, x + 5, y + 4, chip.accent);
		Fonts.draw(graphics, font, chip.text, x + 16, y + 3, HudDraw.TEXT);
	}

	private static List<Chip> chips(VibeConfig config, Minecraft client, @Nullable LocalPlayer player,
			boolean preview) {
		List<Chip> chips = new ArrayList<>();
		Font font = client.font;

		if ((config.hudShowUser || preview) && (player != null || preview)) {
			String name = player != null ? player.getName().getString() : "vibevisual";
			chips.add(chip(font, HudIcons.Icon.USER, name, HudDraw.VIOLET));
		}

		if (config.hudShowFps || preview) {
			chips.add(chip(font, HudIcons.Icon.FPS, (preview ? 240 : client.getFps()) + " FPS", HudDraw.LIME));
		}

		if (config.hudShowTime || preview) {
			chips.add(chip(font, HudIcons.Icon.CLOCK, preview ? "17:46:44" : HudMetrics.time(), HudDraw.AMBER));
		}

		if ((config.hudShowCoords || preview) && player != null) {
			String coords = String.format(Locale.ROOT, "%.1f %.1f %.1f",
					player.getX(), player.getY(), player.getZ());
			chips.add(chip(font, HudIcons.Icon.COORDS, coords, HudDraw.CYAN));
		} else if (preview) {
			chips.add(chip(font, HudIcons.Icon.COORDS, "120.0 64.0 -300.0", HudDraw.CYAN));
		}

		if ((config.hudShowPing || preview) && player != null) {
			int ping = preview ? 12 : HudMetrics.ping(client);
			chips.add(chip(font, HudIcons.Icon.PING, ping + " ms", ping <= 60 ? HudDraw.LIME : HudDraw.DANGER));
		} else if (preview) {
			chips.add(chip(font, HudIcons.Icon.PING, "12 ms", HudDraw.LIME));
		}

		if (config.hudShowTps || preview) {
			chips.add(chip(font, HudIcons.Icon.TPS,
					HudMetrics.decimal(preview ? 20.0 : HudMetrics.tps(), 1) + " TPS", HudDraw.CYAN));
		}

		if ((config.hudShowSpeed || preview) && player != null) {
			double bps = preview ? 5.6 : HudMetrics.blocksPerSecond(player);
			chips.add(chip(font, HudIcons.Icon.BPS, HudMetrics.decimal(bps, 1) + " BPS", HudDraw.MAGENTA));
		} else if (preview) {
			chips.add(chip(font, HudIcons.Icon.BPS, "5.6 BPS", HudDraw.MAGENTA));
		}

		return chips;
	}

	private static Chip chip(Font font, HudIcons.Icon icon, String text, int accent) {
		return new Chip(icon, text, 16 + Fonts.width(font, text) + 6, accent);
	}

	private static List<List<Chip>> layout(List<Chip> chips, int available) {
		List<List<Chip>> rows = new ArrayList<>();
		int primary = 0;

		while (primary < chips.size() && isPrimary(chips.get(primary).icon)) {
			primary++;
		}

		if (primary > 0) {
			List<Chip> first = new ArrayList<>(chips.subList(0, primary));
			rows.add(first);
		}

		List<Chip> row = null;

		for (int i = primary; i < chips.size(); i++) {
			Chip chip = chips.get(i);

			if (row == null || (!row.isEmpty() && rowWidth(row) + CHIP_GAP + chip.width > available
					&& rows.size() < MAX_ROWS)) {
				row = new ArrayList<>();
				rows.add(row);
			}

			if (row.size() < 8) {
				row.add(chip);
			}
		}

		return rows;
	}

	private static boolean isPrimary(HudIcons.Icon icon) {
		return icon == HudIcons.Icon.USER || icon == HudIcons.Icon.FPS || icon == HudIcons.Icon.CLOCK;
	}

	private static int rowWidth(List<Chip> row) {
		int width = 0;

		for (Chip chip : row) {
			width += chip.width + CHIP_GAP;
		}

		return Math.max(0, width - CHIP_GAP);
	}

	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}

	private record Chip(HudIcons.Icon icon, String text, int width, int accent) {
	}
}
