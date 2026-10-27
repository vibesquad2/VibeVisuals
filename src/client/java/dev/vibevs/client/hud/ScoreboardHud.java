package dev.vibevs.client.hud;

import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Custom scoreboard sidebar from the archive: a rounded glass card, the objective title in a small
 * icon header and up to fifteen team-formatted lines. Vanilla sidebar rendering is cancelled by
 * {@code GuiMixin} while this element is enabled, so the server scoreboard is never drawn twice.
 */
public class ScoreboardHud implements HudElement {
	private static final int HEADER = 17;
	private static final int LINE = 10;
	private static final int PAD = 5;
	private static final int MAX_LINES = 10;

	@Nullable
	private HudRect lastBounds;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();
		boolean preview = HudEditState.previewing();

		if (!preview && (!config.hudEnabled || !config.hudScoreboard)) {
			this.lastBounds = null;
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Entry title = new Entry(Component.literal("SCOREBOARD"), null);
		List<Entry> lines = new ArrayList<>();

		if (client.level != null) {
			Scoreboard scoreboard = client.level.getScoreboard();
			Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

			if (objective != null) {
				title = new Entry(objective.getDisplayName(), null);
				List<PlayerScoreEntry> scores = new ArrayList<>(scoreboard.listPlayerScores(objective));
				scores.removeIf(entry -> entry.isHidden() || entry.owner().startsWith("#"));
				scores.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());

				for (int i = 0; i < Math.min(MAX_LINES, scores.size()); i++) {
					PlayerScoreEntry entry = scores.get(i);
					lines.add(new Entry(entry.display(), entry));
				}
			}
		}

		if (lines.isEmpty() && preview) {
			title = new Entry(Component.literal("VIBEVISUALS"), null);
			lines.add(new Entry(Component.literal("Online: 24").withStyle(ChatFormatting.GRAY), null));
			lines.add(new Entry(Component.literal("TPS: 20.0").withStyle(ChatFormatting.GRAY), null));
			lines.add(new Entry(Component.literal("Ping: 12 ms").withStyle(ChatFormatting.GRAY), null));
			lines.add(new Entry(Component.literal("Restart: 4m").withStyle(ChatFormatting.GRAY), null));
			lines.add(new Entry(Component.literal("Store: vibevs.ru").withStyle(ChatFormatting.GRAY), null));
		}

		if (lines.isEmpty()) {
			this.lastBounds = null;
			return;
		}

		Font font = client.font;
		int textWidth = font.width(title.text);

		for (Entry line : lines) {
			textWidth = Math.max(textWidth, font.width(line.text));
		}

		// Leave room for the header icon and the line counter, not just the body lines.
		textWidth = Math.max(textWidth, font.width(title.text) + 28);
		int width = Math.max(104, Math.min(230, textWidth + PAD * 2 + 6));
		int height = HEADER + PAD + lines.size() * LINE + 3;
		int x = config.hudScoreboardX >= 0.0f
				? Math.round(config.hudScoreboardX)
				: graphics.guiWidth() - width - 4;
		int y = config.hudScoreboardY >= 0.0f
				? Math.round(config.hudScoreboardY)
				: 80;
		x = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - width), x));
		y = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - height), y));

		HudDraw.softPanel(graphics, x, y, width, height, HudDraw.VIOLET);
		HudDraw.softHeader(graphics, font, x, y, width,
				trim(font, title.text.getString(), width - 44),
				String.valueOf(lines.size()), HudIcons.Icon.SCORE, HudDraw.VIOLET);

		for (int i = 0; i < lines.size(); i++) {
			Entry line = lines.get(i);
			int lineY = y + HEADER + 2 + i * LINE;

			if (i % 2 == 0) {
				graphics.fill(x + 3, lineY - 1, x + width - 3, lineY + LINE - 1, 0x1800E5FF);
			}

			graphics.drawString(font, line.text, x + PAD, lineY, HudDraw.TEXT, false);
		}

		this.lastBounds = new HudRect(x, y, width, height);
	}

	private static String trim(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}

		String ellipsis = "...";

		while (!text.isEmpty() && font.width(text + ellipsis) > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}

		return text + ellipsis;
	}

	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}

	private record Entry(Component text, @Nullable PlayerScoreEntry entry) {
	}
}
