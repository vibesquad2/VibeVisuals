package dev.vibevs.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vibevs.config.Setting;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.config.Visuals;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The right-hand keybind card. Rows are compact glass strips with colour-coded markers, the
 * enabled feature name and a bordered keycap on the far right. Entries are sorted alphabetically,
 * with the GUI and Zoom entries intentionally never listed.
 */
public class KeybindHud implements HudElement {
	private static final int PAD = 6;
	private static final int ROW_HEIGHT = 14;
	private static final int KEY_GAP = 8;
	private static final int NAME_OFFSET = 9;

	@Nullable
	private HudRect lastBounds;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();

		if (!HudEditState.previewing() && (!config.hudEnabled || !config.hudKeybinds)) {
			this.lastBounds = null;
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		List<Entry> entries = entries(config);

		if (entries.isEmpty() && HudEditState.previewing()) {
			// Placeholder so the element stays draggable before anything is bound.
			entries = List.of(new Entry("Example", "R"), new Entry("Fullbright", "G"));
		}

		if (entries.isEmpty()) {
			this.lastBounds = null;
			return;
		}

		int nameWidth = 0;
		int keyWidth = 0;

		for (Entry entry : entries) {
			nameWidth = Math.max(nameWidth, Fonts.width(font, entry.name()));
			keyWidth = Math.max(keyWidth, Fonts.width(font, entry.key()));
		}

		int chipWidth = keyWidth + 8;
		int rowWidth = PAD * 2 + NAME_OFFSET + nameWidth + KEY_GAP + chipWidth;
		String count = String.format("%02d", entries.size());
		int headerWidth = 20 + Fonts.width(font, "KEY BINDS") + 10 + Fonts.width(font, count) + 7;
		int width = Math.max(rowWidth, headerWidth);
		int height = HudDraw.HUD_HEADER + entries.size() * ROW_HEIGHT + 4;
		int x = config.hudKeybindsX >= 0.0f
				? Math.round(config.hudKeybindsX)
				: graphics.guiWidth() - width - 4;
		int y = config.hudKeybindsY >= 0.0f ? Math.round(config.hudKeybindsY) : 4;
		x = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - width), x));
		y = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - height), y));

		HudDraw.softPanel(graphics, x, y, width, height, HudDraw.AMBER);
		HudDraw.softHeader(graphics, font, x, y, width, "KEY BINDS", count, HudIcons.Icon.KEY, HudDraw.AMBER);

		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int rowY = y + HudDraw.HUD_HEADER + i * ROW_HEIGHT;
			int rowAccent = config.rainbowGui
					? Colors.hsb((System.currentTimeMillis() / 18.0f + i * 32.0f) % 360.0f, 0.65f, 1.0f)
					: (i % 2 == 0 ? HudDraw.CYAN : HudDraw.MAGENTA);

			HudDraw.roundFill(graphics, x + PAD, rowY + 1, width - PAD * 2, ROW_HEIGHT - 2, 3, 0x8A0A111C);
			graphics.fill(x + PAD, rowY + 3, x + PAD + 1, rowY + ROW_HEIGHT - 3,
					Colors.withAlpha(rowAccent, 0x80));
			HudDraw.diamond(graphics, x + PAD + 4, rowY + ROW_HEIGHT / 2, 1, rowAccent);
			Fonts.draw(graphics, font, entry.name(), x + PAD + NAME_OFFSET, rowY + 2, HudDraw.TEXT);

			int chipX = x + width - PAD - chipWidth;
			int chipY = rowY + 2;
			HudDraw.roundFill(graphics, chipX, chipY, chipWidth, 10, 2, 0xC4070C15);
			graphics.fill(chipX, chipY, chipX + 1, chipY + 10, Colors.withAlpha(HudDraw.AMBER, 0xB8));
			graphics.fill(chipX, chipY, chipX + chipWidth, chipY + 1, Colors.withAlpha(HudDraw.AMBER, 0x30));
			Fonts.draw(graphics, font, entry.key(), chipX + 4, chipY + 1, HudDraw.AMBER);
		}

		this.lastBounds = new HudRect(x, y, width, height);
	}

	/** Enabled on/off settings with a bound key; "Zoom" is filtered out. */
	private static List<Entry> entries(VibeConfig config) {
		List<Entry> entries = new ArrayList<>();

		for (Setting setting : Visuals.all()) {
			Integer key = config.featureKeys.get(setting.id());

			if (key == null || setting.kind() != Setting.Kind.TOGGLE
					|| setting.id().equals("zoom") || !setting.asBool()) {
				continue;
			}

			entries.add(new Entry(setting.name(), keyName(key)));
		}

		entries.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
		return entries;
	}

	private static String keyName(int key) {
		return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
	}

	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}

	private record Entry(String name, String key) {
	}
}
