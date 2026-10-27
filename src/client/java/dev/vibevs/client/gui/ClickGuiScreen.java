package dev.vibevs.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vibevs.client.VibeVisualsClient;
import dev.vibevs.client.hud.Fonts;
import dev.vibevs.client.hud.HudDraw;
import dev.vibevs.config.Category;
import dev.vibevs.config.Feature;
import dev.vibevs.config.Setting;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.config.Visuals;
import dev.vibevs.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single-window ClickGUI: a category rail on the left and the rows of the selected category on the
 * right, with a header, a description footer and the HUD editor button.
 *
 * <p>Every row is generated from {@link Visuals}: each feature shows as a master toggle (left
 * click enables / disables it) with its settings under it. Right click only expands / collapses
 * those settings and never resets or toggles a value.
 */
public class ClickGuiScreen extends Screen {
	private static final int MARGIN = 24;
	private static final int MIN_WINDOW_WIDTH = 300;
	private static final int MAX_WINDOW_WIDTH = 470;
	private static final int MAX_WINDOW_HEIGHT = 300;
	private static final int MIN_WINDOW_HEIGHT = 172;
	private static final int WINDOW_CUT = 8;
	private static final int HEADER_HEIGHT = 26;
	private static final int FOOTER_HEIGHT = 24;
	private static final int RAIL_WIDTH = 124;
	private static final int RAIL_ROW_HEIGHT = 23;
	private static final int RAIL_ROW_GAP = 4;
	private static final int FEATURE_HEIGHT = 18;
	private static final int TOGGLE_HEIGHT = 16;
	private static final int SLIDER_HEIGHT = 24;
	private static final int MODE_HEIGHT = 32;
	private static final int MODE_CHIP_HEIGHT = 14;
	private static final int MODE_CHIP_GAP = 4;
	private static final int ROW_GAP = 2;
	private static final int INDENT = 10;
	private static final int CLOSE_SIZE = 16;
	private static final int EDIT_WIDTH = 64;
	private static final int EDIT_HEIGHT = 15;
	private static final int PILL_WIDTH = 26;
	private static final int PILL_HEIGHT = 11;

	private static final int WINDOW_BG = 0xF5070A14;
	private static final int ROW_BG = 0x990B1220;
	private static final int ROW_BG_HOVER = 0xCC101B2A;
	private static final int CHILD_BG = 0x60070C16;
	private static final int CHILD_BG_HOVER = 0x8A0E1724;
	private static final int TEXT = 0xFFE8FBFF;
	private static final int TEXT_DIM = 0xFF6F8FA6;
	private static final int TRACK_BG = 0xFF0B1520;
	private static final int PILL_OFF = 0xFF17212E;
	private static final int PILL_KNOB_OFF = 0xFF5E7890;

	/** Features whose settings are unfolded; kept between GUI sessions. */
	private static final Set<String> EXPANDED = new HashSet<>(Set.of("hitParticles"));

	/** Category selected on the rail; kept between GUI sessions. */
	private static int selectedCategory;

	private final List<Row> rows = new ArrayList<>();
	private final double[] scroll = new double[Category.values().length];

	@Nullable
	private final Screen parent;
	@Nullable
	private Setting dragging;
	@Nullable
	private Setting binding;
	private int dragTrackX;
	private int dragTrackWidth;

	private int windowX;
	private int windowY;
	private int windowWidth;
	private int windowHeight;
	private int railX;
	private int railTop;
	private int railRowHeight;
	private int railRowGap;
	private int contentX;
	private int contentWidth;
	private int contentTop;
	private int contentBottom;
	private int footerY;
	private int closeX;
	private int closeY;
	private int editX;
	private int editY;

	public ClickGuiScreen() {
		this(null);
	}

	public ClickGuiScreen(@Nullable Screen parent) {
		super(Component.literal("VibeVisuals"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		VibeVisualsClient.suppressGuiOpen(300L);
		Minecraft client = Minecraft.getInstance();

		if (this.parent != null) {
			client.setScreen(this.parent);
		} else {
			super.onClose();
		}
	}

	@Override
	protected void init() {
		rebuild();
	}

	// ------------------------------------------------------------- rendering

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0x9A02040A);

		for (int x = 0; x < this.width; x += 48) {
			graphics.fill(x, 0, x + 1, this.height, 0x0800E5FF);
		}

		for (int y = 0; y < this.height; y += 48) {
			graphics.fill(0, y, this.width, y + 1, 0x0800E5FF);
		}

		HudDraw.scanlines(graphics, 0, 0, this.width, this.height, 4, 0x03FFFFFF);
		rebuild();

		Category category = Category.values()[selectedCategory];
		int accent = accentFor(selectedCategory);
		int second = accentFor((selectedCategory + 2) % Category.values().length);

		drawWindow(graphics, accent, second, mouseX, mouseY);
		drawRail(graphics, mouseX, mouseY);
		drawContent(graphics, category, accent, mouseX, mouseY);
		drawFooter(graphics, mouseX, mouseY, accent);
	}

	private void rebuild() {
		int maxWidth = Math.max(200, Math.min(MAX_WINDOW_WIDTH, this.width - 2 * MARGIN));
		this.windowWidth = Math.min(maxWidth, this.width);
		int maxHeight = Math.max(140, Math.min(MAX_WINDOW_HEIGHT, this.height - 2 * MARGIN));
		this.windowHeight = Math.min(maxHeight, this.height);
		this.windowX = (this.width - this.windowWidth) / 2;
		this.windowY = (this.height - this.windowHeight) / 2;

		this.railX = this.windowX + 8;
		this.railTop = this.windowY + HEADER_HEIGHT + 8;
		boolean tight = this.windowHeight < 250;
		this.railRowHeight = tight ? 18 : RAIL_ROW_HEIGHT;
		this.railRowGap = tight ? 1 : RAIL_ROW_GAP;

		this.contentX = this.railX + RAIL_WIDTH + 12;
		this.contentWidth = Math.max(60, this.windowX + this.windowWidth - 12 - this.contentX);
		this.contentTop = this.windowY + HEADER_HEIGHT + 28;
		this.contentBottom = this.windowY + this.windowHeight - FOOTER_HEIGHT - 4;
		this.footerY = this.windowY + this.windowHeight - FOOTER_HEIGHT;
		this.closeX = this.windowX + this.windowWidth - 10 - CLOSE_SIZE;
		this.closeY = this.windowY + 5;
		this.editX = this.windowX + this.windowWidth - 12 - EDIT_WIDTH;
		this.editY = this.footerY + (FOOTER_HEIGHT - EDIT_HEIGHT) / 2;

		buildRows();
	}

	private void buildRows() {
		this.rows.clear();
		Category category = Category.values()[selectedCategory];
		double maxScroll = Math.max(0.0,
				contentHeight(category) - (this.contentBottom - this.contentTop));
		this.scroll[selectedCategory] = Math.max(0.0, Math.min(maxScroll, this.scroll[selectedCategory]));

		int y = this.contentTop - (int) Math.round(this.scroll[selectedCategory]);
		int width = Math.max(40, this.contentWidth - 6);

		for (Feature feature : Visuals.features(category)) {
			this.rows.add(new Row(feature, null, false, this.contentX, y, width, FEATURE_HEIGHT));
			y += FEATURE_HEIGHT + ROW_GAP;

			if (!EXPANDED.contains(feature.id())) {
				continue;
			}

			for (Setting child : feature.settings()) {
				int height = heightOf(child);
				this.rows.add(new Row(null, child, true, this.contentX + INDENT, y, width - INDENT, height));
				y += height + ROW_GAP;
			}
		}

		for (Setting setting : Visuals.standalone(category)) {
			int height = heightOf(setting);
			this.rows.add(new Row(null, setting, false, this.contentX, y, width, height));
			y += height + ROW_GAP;
		}
	}

	private void drawWindow(GuiGraphics graphics, int accent, int second, int mouseX, int mouseY) {
		long now = System.currentTimeMillis();
		int x = this.windowX;
		int y = this.windowY;
		int width = this.windowWidth;
		int height = this.windowHeight;

		for (int layer = 3; layer >= 1; layer--) {
			HudDraw.cutFill(graphics, x - layer, y - layer, width + layer * 2, height + layer * 2,
					WINDOW_CUT + layer, Colors.withAlpha(accent, 0x0A));
		}

		HudDraw.cutFill(graphics, x - 1, y - 1, width + 2, height + 2, WINDOW_CUT + 1,
				Colors.withAlpha(accent, 0x80));
		HudDraw.cutFill(graphics, x, y, width, height, WINDOW_CUT, WINDOW_BG);

		for (int gridX = x + 12; gridX < x + width - 12; gridX += 24) {
			graphics.fill(gridX, y + HEADER_HEIGHT, gridX + 1, y + height - 4, 0x0500E5FF);
		}

		for (int gridY = y + HEADER_HEIGHT; gridY < y + height - 4; gridY += 24) {
			graphics.fill(x + 8, gridY, x + width - 8, gridY + 1, 0x0500E5FF);
		}

		HudDraw.gradientFill(graphics, x + 14, y + 1, width - 28, 2, accent, second);
		graphics.fillGradient(x + 8, y + 8, x + width - 8, y + 42, Colors.withAlpha(accent, 0x12), 0x00000000);
		HudDraw.scanlines(graphics, x + 8, y + 8, width - 16, height - 16, 3, 0x04FFFFFF);

		int sweep = y + 10 + (int) ((now / 12) % Math.max(1, height - 20));
		graphics.fill(x + 9, sweep, x + width - 9, sweep + 1, Colors.withAlpha(accent, 0x18));

		int bracket = Colors.withAlpha(accent, 0xC0);
		graphics.fill(x + 8, y + 8, x + 20, y + 9, bracket);
		graphics.fill(x + 8, y + 8, x + 9, y + 20, bracket);
		int secondBracket = Colors.withAlpha(second, 0xC0);
		graphics.fill(x + width - 21, y + 8, x + width - 8, y + 9, secondBracket);
		graphics.fill(x + width - 9, y + 8, x + width - 8, y + 20, secondBracket);

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 14.0f, y + 8.0f);
		graphics.pose().scale(1.2f, 1.2f);
		Fonts.draw(graphics, this.font, "VIBEVS", 0, 0, 0xFFFFFFFF);
		graphics.pose().popMatrix();

		int wordWidth = (int) (Fonts.width(this.font, "VIBEVS") * 1.2f);
		Fonts.draw(graphics, this.font, "// CLICKGUI", x + 14 + wordWidth + 8, y + 11,
				Colors.withAlpha(accent, 0xD0));

		boolean closeHovered = isInside(mouseX, mouseY, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE);
		HudDraw.button(graphics, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE, HudDraw.DANGER, closeHovered,
				closeHovered ? 0xE0200810 : 0xE0090D16);
		Fonts.drawCentered(graphics, this.font, "\u00D7", this.closeX + CLOSE_SIZE / 2, this.closeY + 4,
				closeHovered ? 0xFFFFFFFF : 0xFFB8D9E8);

		int railY = y + HEADER_HEIGHT + 5;
		int railHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT - 10;
		HudDraw.cutFill(graphics, this.railX - 6, railY, RAIL_WIDTH + 12, railHeight, 6, 0x7003070D);

		int separatorX = this.railX + RAIL_WIDTH + 5;
		graphics.fillGradient(separatorX, y + HEADER_HEIGHT + 8, separatorX + 1, this.footerY - 8,
				Colors.withAlpha(accent, 0x80), 0x00FF2BD6);
	}

	private void drawRail(GuiGraphics graphics, int mouseX, int mouseY) {
		for (int i = 0; i < Category.values().length; i++) {
			Category category = Category.values()[i];
			int accent = accentFor(i);
			int y = this.railTop + i * (this.railRowHeight + this.railRowGap);
			boolean selected = i == selectedCategory;
			boolean hovered = isInside(mouseX, mouseY, this.railX, y, RAIL_WIDTH, this.railRowHeight);

			if (selected) {
				HudDraw.cutFill(graphics, this.railX, y, RAIL_WIDTH, this.railRowHeight, 4,
						Colors.withAlpha(accent, 0x26));
				graphics.fill(this.railX + 1, y + 4, this.railX + 3, y + this.railRowHeight - 4, accent);
				graphics.fillGradient(this.railX + 3, y + 1, this.railX + RAIL_WIDTH - 3, y + 2,
						Colors.withAlpha(accent, 0x70), 0x00000000);
			} else if (hovered) {
				HudDraw.cutFill(graphics, this.railX, y, RAIL_WIDTH, this.railRowHeight, 4, 0x12FFFFFF);
				graphics.fill(this.railX, y + 6, this.railX + 1, y + this.railRowHeight - 6,
						Colors.withAlpha(accent, 0x60));
			}

			HudDraw.diamond(graphics, this.railX + 10, y + this.railRowHeight / 2, 2,
					selected ? accent : Colors.withAlpha(accent, 0x90));
			String name = fit(category.displayName(), RAIL_WIDTH - 26);
			Fonts.draw(graphics, this.font, name, this.railX + 21, y + (this.railRowHeight - 8) / 2,
					selected ? 0xFFFFFFFF : hovered ? TEXT : TEXT_DIM);
		}

		Fonts.draw(graphics, this.font, "// " + Visuals.count() + " SETTINGS", this.railX + 2,
				this.footerY - 13, TEXT_DIM);
	}

	private void drawContent(GuiGraphics graphics, Category category, int accent, int mouseX, int mouseY) {
		int headerY = this.windowY + HEADER_HEIGHT + 9;
		Fonts.draw(graphics, this.font, "//", this.contentX, headerY, Colors.withAlpha(accent, 0x90));
		Fonts.draw(graphics, this.font, category.displayName(), this.contentX + 13, headerY, accent);

		int enabled = 0;
		int total = 0;

		for (Feature feature : Visuals.features(category)) {
			total++;

			if (feature.toggle().asBool()) {
				enabled++;
			}
		}

		String count = enabled + " / " + total + " ON";
		Fonts.draw(graphics, this.font, count,
				this.contentX + this.contentWidth - Fonts.width(this.font, count), headerY, TEXT_DIM);
		HudDraw.gradientFill(graphics, this.contentX, this.windowY + HEADER_HEIGHT + 22, this.contentWidth, 1,
				Colors.withAlpha(accent, 0xB0), 0x00000000);

		graphics.enableScissor(this.contentX - 2, this.contentTop, this.contentX + this.contentWidth,
				this.contentBottom);

		for (Row row : this.rows) {
			if (row.y() + row.height() < this.contentTop || row.y() > this.contentBottom) {
				continue;
			}

			boolean hovered = isInside(mouseX, mouseY, row.x(), row.y(), row.width(), row.height())
					&& mouseY >= this.contentTop && mouseY < this.contentBottom;

			if (row.feature() != null) {
				drawFeature(graphics, row, hovered, accent);
			} else {
				drawSetting(graphics, row, hovered, accent, mouseX, mouseY);
			}
		}

		graphics.disableScissor();
		drawScrollbar(graphics, category, accent);
	}

	private void drawFeature(GuiGraphics graphics, Row row, boolean hovered, int accent) {
		Feature feature = row.feature();
		boolean on = feature.toggle().asBool();
		boolean expanded = EXPANDED.contains(feature.id());
		HudDraw.cutFill(graphics, row.x(), row.y(), row.width(), row.height(), 3,
				hovered ? ROW_BG_HOVER : ROW_BG);

		if (on) {
			graphics.fill(row.x(), row.y() + 3, row.x() + 2, row.y() + row.height() - 3, accent);
			graphics.fillGradient(row.x() + 2, row.y(), row.x() + row.width(), row.y() + 1,
					Colors.withAlpha(accent, 0x70), 0x00000000);
		} else if (hovered) {
			graphics.fill(row.x(), row.y() + 3, row.x() + 1, row.y() + row.height() - 3,
					Colors.withAlpha(accent, 0x60));
		}

		int textY = row.y() + (row.height() - 8) / 2;
		HudDraw.diamond(graphics, row.x() + 8, row.y() + row.height() / 2, on ? 2 : 1,
				on ? accent : 0xFF304050);

		if (!feature.settings().isEmpty()) {
			Fonts.draw(graphics, this.font, expanded ? "-" : "+", row.x() + 14, textY,
					expanded ? accent : TEXT_DIM);
		}

		int pillX = row.x() + row.width() - 6 - PILL_WIDTH;
		String state = on ? "ON" : "OFF";
		int stateX = pillX - 6 - Fonts.width(this.font, state);
		int keyRight = stateX - 7;
		int nameMax = Math.max(20, keyRight - keyWidth(feature.toggle()) - (row.x() + 22) - 4);
		Fonts.draw(graphics, this.font, fit(feature.name(), nameMax), row.x() + 22, textY,
				on ? TEXT : TEXT_DIM);
		drawKey(graphics, feature.toggle(), keyRight, textY, accent);
		drawPill(graphics, pillX, row.y() + (row.height() - PILL_HEIGHT) / 2, on, accent);
		Fonts.draw(graphics, this.font, state, stateX, textY, on ? accent : TEXT_DIM);
	}

	private void drawSetting(GuiGraphics graphics, Row row, boolean hovered, int accent,
			int mouseX, int mouseY) {
		Setting setting = row.setting();
		int background = row.child() ? (hovered ? CHILD_BG_HOVER : CHILD_BG)
				: (hovered ? ROW_BG_HOVER : ROW_BG);
		HudDraw.cutFill(graphics, row.x(), row.y(), row.width(), row.height(), 3, background);

		if (row.child()) {
			graphics.fill(row.x() - 4, row.y(), row.x() - 3, row.y() + row.height(),
					Colors.withAlpha(accent, 0x50));
			graphics.fill(row.x() - 3, row.y() + row.height() / 2 - 1, row.x(), row.y() + row.height() / 2,
					Colors.withAlpha(accent, 0x50));
		}

		if (setting.kind() == Setting.Kind.TOGGLE) {
			int textY = row.y() + (row.height() - 8) / 2;
			int pillX = row.x() + row.width() - 6 - PILL_WIDTH;
			String state = setting.displayValue();
			int stateX = pillX - 6 - Fonts.width(this.font, state);
			int keyRight = stateX - 7;
			int nameMax = Math.max(20, keyRight - keyWidth(setting) - (row.x() + 9) - 6);
			Fonts.draw(graphics, this.font, fit(setting.name(), nameMax), row.x() + 9, textY,
					setting.asBool() ? TEXT : TEXT_DIM);
			drawKey(graphics, setting, keyRight, textY, accent);
			drawPill(graphics, pillX, row.y() + (row.height() - PILL_HEIGHT) / 2, setting.asBool(), accent);
			Fonts.draw(graphics, this.font, state, stateX, textY, setting.asBool() ? accent : TEXT_DIM);
			return;
		}

		if (setting.kind() == Setting.Kind.MODE) {
			drawModeSetting(graphics, row, hovered, accent, mouseX, mouseY);
			return;
		}

		Fonts.draw(graphics, this.font, setting.name(), row.x() + 9, row.y() + 3,
				hovered ? TEXT : 0xFFC8E6F2);
		String value = setting.displayValue();
		Fonts.draw(graphics, this.font, value, row.x() + row.width() - 9 - Fonts.width(this.font, value),
				row.y() + 3, accent);

		int trackX = row.x() + 9;
		int trackWidth = row.width() - 18;
		int trackY = row.y() + row.height() - 8;
		graphics.fill(trackX, trackY, trackX + trackWidth, trackY + 3, TRACK_BG);

		for (int tick = 1; tick < 4; tick++) {
			int tickX = trackX + trackWidth * tick / 4;
			graphics.fill(tickX, trackY - 1, tickX + 1, trackY + 4, 0x18FFFFFF);
		}

		double progress = (setting.value() - setting.min())
				/ Math.max(1.0E-6, setting.max() - setting.min());
		int filled = Math.max(0,
				Math.min(trackWidth, (int) Math.round(progress * trackWidth)));

		if (filled > 0) {
			graphics.fill(trackX - 1, trackY - 1, trackX + filled + 1, trackY + 4,
					Colors.withAlpha(accent, 0x30));
			HudDraw.gradientFill(graphics, trackX, trackY, filled, 3, Colors.darken(accent, 0.25f), accent);
		}

		int knobX = trackX + filled;
		boolean active = hovered || this.dragging == setting;

		if (active) {
			HudDraw.diamond(graphics, knobX, trackY + 1, 4, Colors.withAlpha(accent, 0x30));
		}

		HudDraw.diamond(graphics, knobX, trackY + 1, 3, accent);
		HudDraw.diamond(graphics, knobX, trackY + 1, 1, 0xFF060A10);
	}

	/**
	 * Mode settings draw their choices as separate button cards. Unlike the toggle pill this shows
	 * exactly one active option and makes the alternative style directly clickable.
	 */
	private void drawModeSetting(GuiGraphics graphics, Row row, boolean hovered, int accent,
			int mouseX, int mouseY) {
		Setting setting = row.setting();
		Fonts.draw(graphics, this.font, setting.name(), row.x() + 9, row.y() + 3,
				hovered ? TEXT : 0xFFC8E6F2);

		int selected = setting.asInt();
		int chipY = modeChipY(row);
		int chipWidth = modeChipWidth(row, setting);

		for (int i = 0; i < setting.options().size(); i++) {
			int chipX = modeChipX(row, setting, i);
			boolean isSelected = i == selected;
			boolean chipHovered = isInside(mouseX, mouseY, chipX, chipY, chipWidth, MODE_CHIP_HEIGHT);
			int fill = isSelected ? Colors.withAlpha(accent, 0x32)
					: chipHovered ? 0xB00E1724 : 0x72060B13;

			HudDraw.cutFill(graphics, chipX, chipY, chipWidth, MODE_CHIP_HEIGHT, 3, fill);
			graphics.fill(chipX + 3, chipY, chipX + chipWidth - 3, chipY + 1,
					Colors.withAlpha(accent, isSelected ? 0xB8 : chipHovered ? 0x60 : 0x1E));
			graphics.fill(chipX, chipY + 3, chipX + 1, chipY + MODE_CHIP_HEIGHT - 3,
					Colors.withAlpha(accent, isSelected ? 0xA0 : chipHovered ? 0x60 : 0x20));

			String label = setting.optionName(i).toUpperCase(Locale.ROOT);
			Fonts.drawCentered(graphics, this.font, label, chipX + chipWidth / 2, chipY + 3,
					isSelected ? 0xFFFFFFFF : chipHovered ? TEXT : TEXT_DIM);
		}
	}

	private void drawScrollbar(GuiGraphics graphics, Category category, int accent) {
		int viewHeight = this.contentBottom - this.contentTop;
		int content = contentHeight(category);

		if (content <= viewHeight) {
			return;
		}

		int x = this.contentX + this.contentWidth - 2;
		double ratio = (double) viewHeight / content;
		int barHeight = Math.max(12, (int) Math.round(viewHeight * ratio));
		double max = content - viewHeight;
		double offset = max <= 0.0 ? 0.0 : this.scroll[selectedCategory] / max;
		int barY = this.contentTop + (int) Math.round((viewHeight - barHeight) * offset);

		graphics.fill(x - 1, barY - 1, x + 3, barY + barHeight + 1, Colors.withAlpha(accent, 0x20));
		graphics.fillGradient(x, barY, x + 2, barY + barHeight, Colors.withAlpha(accent, 0xE0),
				Colors.withAlpha(accent, 0x60));
	}

	private void drawFooter(GuiGraphics graphics, int mouseX, int mouseY, int accent) {
		HudDraw.gradientFill(graphics, this.windowX + 10, this.footerY, this.windowWidth - 20, 1,
				Colors.withAlpha(accent, 0x80), 0x00000000);

		String description = null;

		for (Row row : this.rows) {
			if (row.y() + row.height() < this.contentTop || row.y() > this.contentBottom) {
				continue;
			}

			if (isInside(mouseX, mouseY, row.x(), row.y(), row.width(), row.height())) {
				description = row.feature() != null ? row.feature().description() : row.setting().description();
				break;
			}
		}

		if (this.binding != null) {
			description = "Press a key for '" + this.binding.name() + "' - Backspace clears - Esc cancels";
		} else if (description == null) {
			description = "LMB toggle/select - RMB show/hide settings - MMB bind - scroll";
		}

		int descriptionWidth = Math.max(40, this.editX - (this.windowX + 14) - 12);
		Fonts.draw(graphics, this.font, fit(description, descriptionWidth), this.windowX + 14,
				this.footerY + 8, this.binding != null ? 0xFFFF9FE5 : TEXT_DIM);

		boolean hovered = isInside(mouseX, mouseY, this.editX, this.editY, EDIT_WIDTH, EDIT_HEIGHT);
		HudDraw.button(graphics, this.editX, this.editY, EDIT_WIDTH, EDIT_HEIGHT, accent, hovered,
				hovered ? 0xE00E1A28 : 0xE0070C16);
		Fonts.drawCentered(graphics, this.font, "EDIT HUD", this.editX + EDIT_WIDTH / 2, this.editY + 4,
				hovered ? 0xFFFFFFFF : TEXT);
	}

	// ------------------------------------------------------------ interaction

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		rebuild();

		if (event.button() == 0) {
			if (isInside(event.x(), event.y(), this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE)) {
				onClose();
				return true;
			}

			if (isInside(event.x(), event.y(), this.editX, this.editY, EDIT_WIDTH, EDIT_HEIGHT)) {
				Minecraft.getInstance().setScreen(new HudEditorScreen(this));
				return true;
			}

			for (int i = 0; i < Category.values().length; i++) {
				int y = this.railTop + i * (this.railRowHeight + this.railRowGap);

				if (isInside(event.x(), event.y(), this.railX, y, RAIL_WIDTH, this.railRowHeight)) {
					selectedCategory = i;
					rebuild();
					return true;
				}
			}
		}

		if (event.y() < this.contentTop || event.y() >= this.contentBottom) {
			return super.mouseClicked(event, doubleClick);
		}

		for (Row row : this.rows) {
			if (!isInside(event.x(), event.y(), row.x(), row.y(), row.width(), row.height())) {
				continue;
			}

			if (row.feature() != null) {
				handleFeatureClick(row.feature(), event.button());
				return true;
			}

			handleSettingClick(row, event.button(), (int) event.x(), (int) event.y());
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	private void handleFeatureClick(Feature feature, int button) {
		if (button == 2) {
			this.binding = feature.toggle();
		} else if (button == 1) {
			if (!EXPANDED.remove(feature.id())) {
				EXPANDED.add(feature.id());
			}

			rebuild();
		} else if (button == 0) {
			feature.toggle().toggle();
			save();
		}
	}

	private void handleSettingClick(Row row, int button, int mouseX, int mouseY) {
		Setting setting = row.setting();

		if (button == 2) {
			if (setting.kind() == Setting.Kind.TOGGLE) {
				this.binding = setting;
			}

			return;
		}

		// Right click is reserved for expanding / collapsing a module. It must never change a
		// setting value, so clicks on child rows simply do nothing.
		if (button == 1) {
			return;
		}

		if (button != 0) {
			return;
		}

		if (setting.kind() == Setting.Kind.MODE) {
			int index = modeIndexAt(row, setting, mouseX, mouseY);

			if (index >= 0) {
				setting.set(index);
				save();
			}

			return;
		}

		if (setting.kind() == Setting.Kind.TOGGLE) {
			setting.toggle();
			save();
			return;
		}

		this.dragging = setting;
		this.dragTrackX = row.x() + 9;
		this.dragTrackWidth = row.width() - 18;
		dragTo(mouseX);
	}

	private int modeIndexAt(Row row, Setting setting, int mouseX, int mouseY) {
		int chipY = modeChipY(row);

		if (mouseY < chipY || mouseY >= chipY + MODE_CHIP_HEIGHT) {
			return -1;
		}

		int chipWidth = modeChipWidth(row, setting);

		for (int i = 0; i < setting.options().size(); i++) {
			int chipX = modeChipX(row, setting, i);

			if (mouseX >= chipX && mouseX < chipX + chipWidth) {
				return i;
			}
		}

		return -1;
	}

	private static int modeChipY(Row row) {
		return row.y() + row.height() - MODE_CHIP_HEIGHT - 4;
	}

	private static int modeChipWidth(Row row, Setting setting) {
		int inner = Math.max(20, row.width() - 18);
		int count = Math.max(1, setting.options().size());
		return Math.max(20, (inner - MODE_CHIP_GAP * (count - 1)) / count);
	}

	private static int modeChipX(Row row, Setting setting, int index) {
		return row.x() + 9 + index * (modeChipWidth(row, setting) + MODE_CHIP_GAP);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.dragging != null) {
			dragTo((int) event.x());
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.dragging != null) {
			this.dragging = null;
			save();
			return true;
		}

		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (isInside(mouseX, mouseY, this.contentX - 4, this.contentTop, this.contentWidth + 8,
				this.contentBottom - this.contentTop)) {
			rebuild();
			Category category = Category.values()[selectedCategory];
			double max = Math.max(0.0,
					contentHeight(category) - (this.contentBottom - this.contentTop));
			this.scroll[selectedCategory] = Math.max(0.0,
					Math.min(max, this.scroll[selectedCategory] - scrollY * 14.0));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.binding != null) {
			Setting setting = this.binding;
			this.binding = null;
			int key = event.key();

			if (key != InputConstants.KEY_ESCAPE) {
				if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
					VibeConfig.get().featureKeys.remove(setting.id());
				} else {
					VibeConfig.get().featureKeys.put(setting.id(), key);
				}

				save();
			}

			return true;
		}

		if (VibeVisualsClient.OPEN_GUI.matches(event)) {
			onClose();
			return true;
		}

		return super.keyPressed(event);
	}

	// --------------------------------------------------------------- helpers

	private void drawPill(GuiGraphics graphics, int x, int y, boolean on, int accent) {
		if (on) {
			graphics.fill(x - 2, y - 2, x + PILL_WIDTH + 2, y + PILL_HEIGHT + 2,
					Colors.withAlpha(accent, 0x22));
		}

		HudDraw.roundFill(graphics, x, y, PILL_WIDTH, PILL_HEIGHT, PILL_HEIGHT / 2,
				on ? Colors.darken(accent, 0.55f) : PILL_OFF);
		graphics.fill(x + 3, y + 1, x + PILL_WIDTH - 3, y + 2, on ? 0x80FFFFFF : 0x18FFFFFF);

		int knobX = on ? x + PILL_WIDTH - 9 : x + 1;

		if (on) {
			HudDraw.roundFill(graphics, knobX - 1, y, 10, 10, 5, Colors.withAlpha(accent, 0x50));
		}

		HudDraw.roundFill(graphics, knobX, y + 1, 8, 8, 4, on ? 0xFF060A10 : PILL_KNOB_OFF);

		if (on) {
			graphics.fill(knobX + 3, y + 4, knobX + 5, y + 6, accent);
		}
	}

	private void drawKey(GuiGraphics graphics, Setting setting, int rightX, int y, int accent) {
		if (this.binding == setting) {
			drawKeyChip(graphics, "\u2026", rightX, y, accent);
			return;
		}

		Integer bound = VibeConfig.get().featureKeys.get(setting.id());

		if (bound == null) {
			return;
		}

		drawKeyChip(graphics, keyName(bound), rightX, y, accent);
	}

	private void drawKeyChip(GuiGraphics graphics, String key, int rightX, int y, int accent) {
		int chipWidth = Fonts.width(this.font, key) + 8;
		int chipX = rightX - chipWidth;
		HudDraw.cutFill(graphics, chipX, y - 2, chipWidth, 11, 2, 0x80040912);
		graphics.fill(chipX, y - 2, chipX + 1, y + 9, Colors.withAlpha(accent, 0x80));
		Fonts.draw(graphics, this.font, key, chipX + 4, y, 0xFFFF9FE5);
	}

	private int keyWidth(Setting setting) {
		if (this.binding == setting) {
			return Fonts.width(this.font, "\u2026") + 8;
		}

		Integer bound = VibeConfig.get().featureKeys.get(setting.id());
		return bound == null ? 0 : Fonts.width(this.font, keyName(bound)) + 8;
	}

	private String fit(String value, int maxWidth) {
		if (maxWidth <= 0) {
			return "";
		}

		if (Fonts.width(this.font, value) <= maxWidth) {
			return value;
		}

		String dots = "\u2026";
		int end = value.length();

		while (end > 0 && Fonts.width(this.font, value.substring(0, end) + dots) > maxWidth) {
			end--;
		}

		return value.substring(0, end) + dots;
	}

	private int accentFor(int index) {
		Category category = Category.values()[index];

		if (!VibeConfig.get().rainbowGui) {
			return category.accent();
		}

		float hue = (System.currentTimeMillis() / 14.0f + index * 34.0f) % 360.0f;
		return Colors.hsb(hue, 0.8f, 1.0f);
	}

	private int contentHeight(Category category) {
		int height = 0;

		for (Feature feature : Visuals.features(category)) {
			height += FEATURE_HEIGHT + ROW_GAP;

			if (!EXPANDED.contains(feature.id())) {
				continue;
			}

			for (Setting child : feature.settings()) {
				height += heightOf(child) + ROW_GAP;
			}
		}

		for (Setting setting : Visuals.standalone(category)) {
			height += heightOf(setting) + ROW_GAP;
		}

		return height;
	}

	private static int heightOf(Setting setting) {
		return switch (setting.kind()) {
			case SLIDER -> SLIDER_HEIGHT;
			case MODE -> MODE_HEIGHT;
			case TOGGLE -> TOGGLE_HEIGHT;
		};
	}

	private static String keyName(int key) {
		return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
	}

	private void dragTo(int mouseX) {
		if (this.dragging == null) {
			return;
		}

		double progress = (mouseX - this.dragTrackX) / (double) Math.max(1, this.dragTrackWidth);
		Setting setting = this.dragging;
		setting.set(setting.min() + progress * (setting.max() - setting.min()));
	}

	private static void save() {
		VibeConfig.get().requestSave();
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private record Row(@Nullable Feature feature, @Nullable Setting setting, boolean child, int x, int y,
			int width, int height) {
	}
}
