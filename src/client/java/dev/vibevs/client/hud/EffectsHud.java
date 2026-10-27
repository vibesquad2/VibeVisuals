package dev.vibevs.client.hud;

import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The right-hand effects card: one live row per potion effect with a framed vanilla icon, name,
 * remaining time and a thin drain line. The panel sits above the hotbar by default and falls back
 * to two sample rows in the HUD editor so it can always be positioned.
 */
public class EffectsHud implements HudElement {
	private static final int PAD = 6;
	private static final int FRAME = 12;
	private static final int ICON = 10;
	private static final int ICON_GAP = 5;
	private static final int TIME_GAP = 8;
	private static final int ROW_HEIGHT = 17;
	private static final String[] ROMAN = {"", "II", "III", "IV", "V", "VI", "VII", "VIII"};

	@Nullable
	private HudRect lastBounds;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();
		Minecraft client = Minecraft.getInstance();

		if (!HudEditState.previewing() && (!config.hudEnabled || !config.hudEffects)) {
			this.lastBounds = null;
			return;
		}

		Font font = client.font;
		List<Row> rows = rows(client);

		if (rows.isEmpty() && HudEditState.previewing()) {
			rows = List.of(
					new Row("Speed", "1:23", MobEffects.SPEED, 0.72f),
					new Row("Regeneration", "0:45", MobEffects.REGENERATION, 0.45f));
		}

		if (rows.isEmpty()) {
			this.lastBounds = null;
			return;
		}

		int nameWidth = 0;
		int timeWidth = 0;

		for (Row row : rows) {
			nameWidth = Math.max(nameWidth, Fonts.width(font, row.name()));
			timeWidth = Math.max(timeWidth, Fonts.width(font, row.time()));
		}

		int textOffset = FRAME + ICON_GAP;
		int rowWidth = PAD * 2 + textOffset + nameWidth + TIME_GAP + timeWidth;
		String count = String.format("%02d", rows.size());
		int headerWidth = 20 + Fonts.width(font, "POTIONS") + 10 + Fonts.width(font, count) + 7;
		int width = Math.max(rowWidth, headerWidth);
		int height = HudDraw.HUD_HEADER + rows.size() * ROW_HEIGHT + 4;
		int x = config.hudEffectsX >= 0.0f
				? Math.round(config.hudEffectsX)
				: graphics.guiWidth() - width - 4;
		int y = config.hudEffectsY >= 0.0f
				? Math.round(config.hudEffectsY)
				: graphics.guiHeight() - height - 54;
		x = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - width), x));
		y = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - height), y));

		HudDraw.softPanel(graphics, x, y, width, height, HudDraw.MAGENTA);
		HudDraw.softHeader(graphics, font, x, y, width, "POTIONS", count, HudIcons.Icon.CLOCK, HudDraw.MAGENTA);

		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			int rowY = y + HudDraw.HUD_HEADER + i * ROW_HEIGHT;
			int textX = x + PAD + textOffset;
			int textY = rowY + 2;
			int accent = accent(row.effect());
			Identifier sprite = Gui.getMobEffectSprite(row.effect());

			HudDraw.roundFill(graphics, x + PAD, rowY + 1, width - PAD * 2, ROW_HEIGHT - 2, 3, 0x8A0A111C);
			HudDraw.cutFill(graphics, x + PAD, rowY + 3, FRAME, FRAME, 2, 0x70040A12);
			graphics.fill(x + PAD, rowY + 4, x + PAD + 1, rowY + 13,
					Colors.withAlpha(accent, 0xA0));
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + PAD + 1, rowY + 4, ICON, ICON);
			Fonts.draw(graphics, font, row.name(), textX, textY, HudDraw.TEXT);

			int timeColor = Colors.mix(accent, HudDraw.TEXT, 0.42f);
			Fonts.draw(graphics, font, row.time(), x + width - PAD - Fonts.width(font, row.time()), textY,
					timeColor);

			int barX = textX;
			int barY = rowY + ROW_HEIGHT - 4;
			int barWidth = x + width - PAD - barX;

			if (barWidth > 0) {
				graphics.fill(barX, barY, barX + barWidth, barY + 1, 0x28FFFFFF);
				int fill = Math.max(1, Math.round(barWidth * Math.max(0.0f, Math.min(1.0f, row.ratio()))));
				int barColor = Colors.mix(HudDraw.DANGER, HudDraw.LIME,
						Math.max(0.0f, Math.min(1.0f, row.ratio())));
				graphics.fill(barX, barY, barX + fill, barY + 1, Colors.withAlpha(barColor, 0xB8));
			}
		}

		this.lastBounds = new HudRect(x, y, width, height);
	}

	private static List<Row> rows(Minecraft client) {
		if (client.player == null) {
			return List.of();
		}

		List<MobEffectInstance> effects = new ArrayList<>(client.player.getActiveEffects());
		effects.sort(Comparator.comparing(effect -> effect.getEffect().value().getDisplayName().getString()));

		List<Row> rows = new ArrayList<>(effects.size());

		for (MobEffectInstance effect : effects) {
			rows.add(new Row(name(effect), formatDuration(effect), effect.getEffect(), durationRatio(effect)));
		}

		return rows;
	}

	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}

	private static int accent(Holder<MobEffect> effect) {
		int color = effect.value().getColor();
		return 0xFF000000 | (color & 0xFFFFFF);
	}

	private static float durationRatio(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) {
			return 1.0f;
		}

		return Math.max(0.02f, Math.min(1.0f, effect.getDuration() / 1200.0f));
	}

	private static String name(MobEffectInstance effect) {
		String name = effect.getEffect().value().getDisplayName().getString();
		int amplifier = effect.getAmplifier();

		if (amplifier <= 0) {
			return name;
		}

		return name + " " + (amplifier < ROMAN.length ? ROMAN[amplifier] : Integer.toString(amplifier + 1));
	}

	/** Remaining time, live: {@code m:ss}, or {@code h:mm:ss} for very long effects. */
	private static String formatDuration(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) {
			// Golos Text has no infinity glyph; a short ASCII marker keeps the row readable.
			return "INF";
		}

		int totalSeconds = Math.max(0, effect.getDuration()) / 20;
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}

		return String.format("%d:%02d", minutes, seconds);
	}

	private record Row(String name, String time, Holder<MobEffect> effect, float ratio) {
	}
}
