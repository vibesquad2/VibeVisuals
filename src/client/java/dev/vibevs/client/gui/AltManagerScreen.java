package dev.vibevs.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vibevs.client.feature.AltManager;
import dev.vibevs.client.hud.Fonts;
import dev.vibevs.client.hud.HudDraw;
import dev.vibevs.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Offline Alt Manager: saved nicknames, a custom nickname field, random nickname generation and
 * one-click account switching. Everything is local; Microsoft authentication is not supported.
 */
public class AltManagerScreen extends Screen {
	private static final int PANEL_WIDTH = 400;
	private static final int PANEL_HEIGHT = 350;
	private static final int HEADER_HEIGHT = 30;
	private static final int FOOTER_HEIGHT = 30;
	private static final int INNER_PAD = 12;
	private static final int CURRENT_HEIGHT = 30;
	private static final int FIELD_HEIGHT = 20;
	private static final int ROW_HEIGHT = 25;
	private static final int ROW_GAP = 3;
	private static final int CLOSE_SIZE = 16;
	private static final int MAX_NAME_LENGTH = 16;

	private static final int TEXT = 0xFFE8FBFF;
	private static final int TEXT_DIM = 0xFF6F8FA6;
	private static final int ROW_BG = 0x990B1220;
	private static final int ROW_BG_HOVER = 0xCC101B2A;
	private static final int WINDOW_BG = 0xF5070A14;

	private final AltManager manager = AltManager.get();
	@Nullable
	private final Screen parent;

	private String input = "";
	private int cursor;
	private boolean inputFocused = true;
	private double scroll;
	@Nullable
	private String status;
	private int statusColor = TEXT_DIM;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int innerX;
	private int innerWidth;
	private int closeX;
	private int closeY;
	private int currentY;
	private int inputLabelY;
	private int fieldX;
	private int fieldY;
	private int fieldWidth;
	private int addX;
	private int addWidth;
	private int randomX;
	private int randomWidth;
	private int listHeaderY;
	private int listTop;
	private int listBottom;
	private int footerY;

	public AltManagerScreen(@Nullable Screen parent) {
		super(Component.literal("Alt Manager"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		if (this.parent != null) {
			Minecraft.getInstance().setScreen(this.parent);
		} else {
			super.onClose();
		}
	}

	@Override
	protected void init() {
		super.init();
		layout();
		this.inputFocused = true;
	}

	private void layout() {
		this.panelWidth = Math.max(300, Math.min(PANEL_WIDTH, this.width - 24));
		this.panelHeight = Math.max(250, Math.min(PANEL_HEIGHT, this.height - 24));
		this.panelX = (this.width - this.panelWidth) / 2;
		this.panelY = (this.height - this.panelHeight) / 2;
		this.innerX = this.panelX + INNER_PAD;
		this.innerWidth = this.panelWidth - INNER_PAD * 2;
		this.closeX = this.panelX + this.panelWidth - 10 - CLOSE_SIZE;
		this.closeY = this.panelY + 7;
		this.currentY = this.panelY + 40;
		this.inputLabelY = this.currentY + CURRENT_HEIGHT + 11;
		this.fieldX = this.innerX;
		this.fieldY = this.inputLabelY + 10;
		this.addWidth = 56;
		this.randomWidth = 80;
		int gap = 6;
		this.fieldWidth = this.innerWidth - this.addWidth - this.randomWidth - gap * 2;
		this.addX = this.fieldX + this.fieldWidth + gap;
		this.randomX = this.addX + this.addWidth + gap;
		this.listHeaderY = this.fieldY + FIELD_HEIGHT + 8;
		this.listTop = this.listHeaderY + 14;
		this.footerY = this.panelY + this.panelHeight - FOOTER_HEIGHT;
		this.listBottom = this.footerY - 6;
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

		drawPanel(graphics, mouseX, mouseY);
		drawCurrentAccount(graphics);
		drawInput(graphics, mouseX, mouseY);
		drawAccounts(graphics, mouseX, mouseY);
		drawFooter(graphics, mouseX, mouseY);
	}

	private void drawPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int accent = HudDraw.VIOLET;
		int second = HudDraw.CYAN;
		long now = System.currentTimeMillis();
		int x = this.panelX;
		int y = this.panelY;
		int width = this.panelWidth;
		int height = this.panelHeight;

		for (int layer = 3; layer >= 1; layer--) {
			HudDraw.cutFill(graphics, x - layer, y - layer, width + layer * 2, height + layer * 2,
					8 + layer, Colors.withAlpha(accent, 0x0A));
		}

		HudDraw.cutFill(graphics, x - 1, y - 1, width + 2, height + 2, 9,
				Colors.withAlpha(accent, 0x90));
		HudDraw.cutFill(graphics, x, y, width, height, 8, WINDOW_BG);

		for (int gridX = x + 12; gridX < x + width - 12; gridX += 24) {
			graphics.fill(gridX, y + HEADER_HEIGHT, gridX + 1, y + height - 4, 0x0500E5FF);
		}

		for (int gridY = y + HEADER_HEIGHT; gridY < y + height - 4; gridY += 24) {
			graphics.fill(x + 8, gridY, x + width - 8, gridY + 1, 0x0500E5FF);
		}

		HudDraw.gradientFill(graphics, x + 14, y + 1, width - 28, 2, accent, second);
		graphics.fillGradient(x + 8, y + 8, x + width - 8, y + 45,
				Colors.withAlpha(accent, 0x18), 0x00000000);
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
		Fonts.draw(graphics, this.font, "ALT MANAGER", 0, 0, 0xFFFFFFFF);
		graphics.pose().popMatrix();

		int wordWidth = (int) (Fonts.width(this.font, "ALT MANAGER") * 1.2f);
		Fonts.draw(graphics, this.font, "// OFFLINE", x + 14 + wordWidth + 8, y + 11,
				Colors.withAlpha(accent, 0xE0));

		boolean closeHovered = isInside(mouseX, mouseY, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE);
		HudDraw.button(graphics, this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE, HudDraw.DANGER, closeHovered,
				closeHovered ? 0xE0200810 : 0xE0090D16);
		Fonts.drawCentered(graphics, this.font, "\u00D7", this.closeX + CLOSE_SIZE / 2, this.closeY + 4,
				closeHovered ? 0xFFFFFFFF : 0xFFB8D9E8);
	}

	private void drawCurrentAccount(GuiGraphics graphics) {
		String current = this.minecraft.getUser().getName();
		int accent = HudDraw.MAGENTA;
		int x = this.innerX;
		int y = this.currentY;
		int width = this.innerWidth;
		HudDraw.cutFill(graphics, x, y, width, CURRENT_HEIGHT, 4, 0x900B1220);
		graphics.fill(x, y + 4, x + 2, y + CURRENT_HEIGHT - 4, accent);
		HudDraw.gradientFill(graphics, x + 2, y + 1, width - 2, 1,
				Colors.withAlpha(accent, 0x70), 0x00000000);

		HudDraw.cutFill(graphics, x + 10, y + 5, 20, 20, 3, Colors.withAlpha(accent, 0x1E));
		graphics.fill(x + 10, y + 8, x + 11, y + 22, accent);
		String initial = current.isEmpty() ? "?" : current.substring(0, 1).toUpperCase(Locale.ROOT);
		Fonts.drawCentered(graphics, this.font, initial, x + 20, y + 11, accent);

		Fonts.draw(graphics, this.font, "CURRENT ACCOUNT", x + 39, y + 5, TEXT_DIM);
		Fonts.draw(graphics, this.font, fit(current, width - 39 - 72), x + 39, y + 15, TEXT);

		int badgeWidth = 52;
		int badgeX = x + width - 10 - badgeWidth;
		HudDraw.cutFill(graphics, badgeX, y + 9, badgeWidth, 12, 2, 0x500B1220);
		graphics.fill(badgeX, y + 11, badgeX + 1, y + 19, Colors.withAlpha(HudDraw.LIME, 0xA0));
		Fonts.drawCentered(graphics, this.font, "OFFLINE", badgeX + badgeWidth / 2, y + 11,
				Colors.withAlpha(HudDraw.LIME, 0xE0));
	}

	private void drawInput(GuiGraphics graphics, int mouseX, int mouseY) {
		int accent = HudDraw.CYAN;
		String label = "NICKNAME // A-Z, 0-9, _   (3-16)";
		Fonts.draw(graphics, this.font, label, this.innerX, this.inputLabelY, TEXT_DIM);

		boolean fieldHovered = isInside(mouseX, mouseY, this.fieldX, this.fieldY, this.fieldWidth,
				FIELD_HEIGHT);
		HudDraw.button(graphics, this.fieldX, this.fieldY, this.fieldWidth, FIELD_HEIGHT, accent,
				this.inputFocused || fieldHovered, this.inputFocused ? 0xE00C1824 : 0xD0070C15);

		int textX = this.fieldX + 8;
		int textY = this.fieldY + 6;
		graphics.enableScissor(this.fieldX + 4, this.fieldY + 2, this.fieldX + this.fieldWidth - 4,
				this.fieldY + FIELD_HEIGHT - 2);

		if (this.input.isEmpty()) {
			Fonts.draw(graphics, this.font, "enter nickname", textX, textY, 0xFF4E6B80);
		} else {
			Fonts.draw(graphics, this.font, this.input, textX, textY, TEXT);
		}

		if (this.inputFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
			int cursorX = textX + Fonts.width(this.font, this.input.substring(0, this.cursor));
			graphics.fill(cursorX, this.fieldY + 5, cursorX + 1, this.fieldY + 15, accent);
		}

		graphics.disableScissor();

		boolean addHovered = isInside(mouseX, mouseY, this.addX, this.fieldY, this.addWidth, FIELD_HEIGHT);
		HudDraw.button(graphics, this.addX, this.fieldY, this.addWidth, FIELD_HEIGHT, HudDraw.LIME,
				addHovered, addHovered ? 0xE0122110 : 0xE0070D12);
		Fonts.drawCentered(graphics, this.font, "ADD", this.addX + this.addWidth / 2, this.fieldY + 6,
				addHovered ? 0xFFFFFFFF : TEXT);

		boolean randomHovered = isInside(mouseX, mouseY, this.randomX, this.fieldY, this.randomWidth,
				FIELD_HEIGHT);
		HudDraw.button(graphics, this.randomX, this.fieldY, this.randomWidth, FIELD_HEIGHT,
				HudDraw.MAGENTA, randomHovered, randomHovered ? 0xE0180A1C : 0xE0090D16);
		Fonts.drawCentered(graphics, this.font, "RANDOM", this.randomX + this.randomWidth / 2,
				this.fieldY + 6, randomHovered ? 0xFFFFFFFF : 0xFFFFB8F0);

		Fonts.draw(graphics, this.font, "RND 4-12", this.innerX + this.innerWidth
				- Fonts.width(this.font, "RND 4-12"), this.inputLabelY, Colors.withAlpha(HudDraw.MAGENTA, 0xB0));
	}

	private void drawAccounts(GuiGraphics graphics, int mouseX, int mouseY) {
		int accent = HudDraw.VIOLET;
		List<String> accounts = this.manager.accounts();
		String count = accounts.size() + " SAVED";
		Fonts.draw(graphics, this.font, "// ACCOUNTS", this.innerX, this.listHeaderY, accent);
		Fonts.draw(graphics, this.font, count, this.innerX + this.innerWidth - Fonts.width(this.font, count),
				this.listHeaderY, TEXT_DIM);
		HudDraw.gradientFill(graphics, this.innerX, this.listHeaderY + 10, this.innerWidth, 1,
				Colors.withAlpha(accent, 0x80), 0x00000000);

		int viewHeight = this.listBottom - this.listTop;
		double contentHeight = accounts.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
		double maxScroll = Math.max(0.0, contentHeight - viewHeight);
		this.scroll = Math.max(0.0, Math.min(maxScroll, this.scroll));
		String current = this.minecraft.getUser().getName();

		graphics.enableScissor(this.innerX - 1, this.listTop, this.innerX + this.innerWidth + 1,
				this.listBottom);

		for (int i = 0; i < accounts.size(); i++) {
			int rowY = this.listTop + i * (ROW_HEIGHT + ROW_GAP) - (int) Math.round(this.scroll);

			if (rowY + ROW_HEIGHT < this.listTop || rowY > this.listBottom) {
				continue;
			}

			String name = accounts.get(i);
			boolean active = name.equalsIgnoreCase(current);
			boolean hovered = isInside(mouseX, mouseY, this.innerX, rowY, this.innerWidth, ROW_HEIGHT)
					&& mouseY >= this.listTop && mouseY < this.listBottom;
			boolean deleteHovered = !active && isInside(mouseX, mouseY, this.innerX + this.innerWidth - 24,
					rowY + 5, 14, 14);
			drawAccountRow(graphics, name, i, rowY, active, hovered, deleteHovered);
		}

		graphics.disableScissor();

		if (maxScroll > 0.0) {
			int trackX = this.innerX + this.innerWidth - 3;
			int thumbHeight = Math.max(12, (int) Math.round(viewHeight * (viewHeight / contentHeight)));
			int thumbY = this.listTop
					+ (int) Math.round((viewHeight - thumbHeight) * (this.scroll / maxScroll));
			graphics.fill(trackX, this.listTop, trackX + 1, this.listBottom, 0x2000E5FF);
			graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight,
					Colors.withAlpha(accent, 0xD0));
		}
	}

	private void drawAccountRow(GuiGraphics graphics, String name, int index, int y, boolean active,
			boolean hovered, boolean deleteHovered) {
		int accent = active ? HudDraw.MAGENTA : HudDraw.VIOLET;
		int x = this.innerX;
		int width = this.innerWidth;

		if (active) {
			HudDraw.cutFill(graphics, x, y, width, ROW_HEIGHT, 3, Colors.withAlpha(accent, 0x20));
			graphics.fill(x, y + 3, x + 2, y + ROW_HEIGHT - 3, accent);
			HudDraw.gradientFill(graphics, x + 2, y, width - 2, 1,
					Colors.withAlpha(accent, 0x80), 0x00000000);
		} else {
			HudDraw.cutFill(graphics, x, y, width, ROW_HEIGHT, 3, hovered ? ROW_BG_HOVER : ROW_BG);

			if (hovered) {
				graphics.fill(x, y + 4, x + 1, y + ROW_HEIGHT - 4,
						Colors.withAlpha(accent, 0x70));
			}
		}

		String number = String.format(Locale.ROOT, "%02d", index + 1);
		Fonts.draw(graphics, this.font, number, x + 8, y + 8, active ? accent : 0xFF4E6B80);
		HudDraw.diamond(graphics, x + 30, y + ROW_HEIGHT / 2, active ? 2 : 1,
				active ? accent : Colors.withAlpha(accent, 0x80));

		int right = active ? x + width - 12 - 45 : x + width - 28;
		Fonts.draw(graphics, this.font, fit(name, Math.max(20, right - (x + 40) - 8)),
				x + 40, y + 8, active ? 0xFFFFFFFF : hovered ? TEXT : 0xFFC8E6F2);

		if (active) {
			int badgeWidth = 45;
			int badgeX = x + width - 12 - badgeWidth;
			HudDraw.cutFill(graphics, badgeX, y + 7, badgeWidth, 11, 2,
					Colors.withAlpha(accent, 0x28));
			graphics.fill(badgeX, y + 9, badgeX + 1, y + 16, accent);
			Fonts.drawCentered(graphics, this.font, "ACTIVE", badgeX + badgeWidth / 2, y + 8, accent);
		} else {
			int deleteX = x + width - 24;
			HudDraw.cutFill(graphics, deleteX, y + 5, 14, 14, 2,
					deleteHovered ? Colors.withAlpha(HudDraw.DANGER, 0x30) : 0x30040912);
			Fonts.drawCentered(graphics, this.font, "\u00D7", deleteX + 7, y + 8,
					deleteHovered ? 0xFFFF7D94 : 0xFF8A5C6D);
		}
	}

	private void drawFooter(GuiGraphics graphics, int mouseX, int mouseY) {
		int accent = HudDraw.VIOLET;
		HudDraw.gradientFill(graphics, this.innerX, this.footerY, this.innerWidth, 1,
				Colors.withAlpha(accent, 0x80), 0x00000000);

		String message = this.status != null
				? this.status
				: "Click an account to switch // Add or random a nickname";
		Fonts.draw(graphics, this.font, fit(message, this.innerWidth), this.innerX,
				this.footerY + 4, this.status != null ? this.statusColor : TEXT_DIM);

		String warning = "OFFLINE / CRACKED ONLY  \u00B7  NO MICROSOFT LOGIN";
		String hint = "ESC CLOSE";
		int hintWidth = Fonts.width(this.font, hint);
		Fonts.draw(graphics, this.font, fit(warning, this.innerWidth - hintWidth - 10),
				this.innerX, this.footerY + 16, Colors.withAlpha(HudDraw.AMBER, 0xB0));
		Fonts.draw(graphics, this.font, hint, this.innerX + this.innerWidth - hintWidth,
				this.footerY + 16, TEXT_DIM);
	}

	// ------------------------------------------------------------ interaction

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			if (isInside(event.x(), event.y(), this.closeX, this.closeY, CLOSE_SIZE, CLOSE_SIZE)) {
				onClose();
				return true;
			}

			if (isInside(event.x(), event.y(), this.fieldX, this.fieldY, this.fieldWidth, FIELD_HEIGHT)) {
				this.inputFocused = true;
				this.cursor = cursorAt(event.x());
				return true;
			}

			this.inputFocused = false;

			if (isInside(event.x(), event.y(), this.addX, this.fieldY, this.addWidth, FIELD_HEIGHT)) {
				submit();
				return true;
			}

			if (isInside(event.x(), event.y(), this.randomX, this.fieldY, this.randomWidth, FIELD_HEIGHT)) {
				this.input = this.manager.randomName();
				this.cursor = this.input.length();
				this.inputFocused = true;
				setStatus("Random nickname generated", HudDraw.MAGENTA);
				return true;
			}

			if (event.y() >= this.listTop && event.y() < this.listBottom) {
				List<String> accounts = this.manager.accounts();
				String current = this.minecraft.getUser().getName();

				for (int i = 0; i < accounts.size(); i++) {
					int rowY = this.listTop + i * (ROW_HEIGHT + ROW_GAP) - (int) Math.round(this.scroll);

					if (!isInside(event.x(), event.y(), this.innerX, rowY, this.innerWidth, ROW_HEIGHT)) {
						continue;
					}

					String name = accounts.get(i);
					boolean active = name.equalsIgnoreCase(current);

					if (!active && isInside(event.x(), event.y(), this.innerX + this.innerWidth - 24,
							rowY + 5, 14, 14)) {
						if (this.manager.remove(name)) {
							setStatus("Removed " + name, HudDraw.AMBER);
						} else {
							setStatus("The active account cannot be removed", HudDraw.DANGER);
						}

						return true;
					}

					if (active) {
						setStatus("Already using " + name, HudDraw.MAGENTA);
					} else if (this.manager.select(name)) {
						setStatus("Switched to " + name, HudDraw.LIME);
					}

					return true;
				}
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (isInside(mouseX, mouseY, this.innerX, this.listTop, this.innerWidth,
				this.listBottom - this.listTop)) {
			List<String> accounts = this.manager.accounts();
			double contentHeight = accounts.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
			double maxScroll = Math.max(0.0, contentHeight - (this.listBottom - this.listTop));
			this.scroll = Math.max(0.0, Math.min(maxScroll, this.scroll - scrollY * 18.0));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (this.inputFocused && event.isAllowedChatCharacter()) {
			insert(event.codepointAsString());
			return true;
		}

		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();

		if (key == InputConstants.KEY_ESCAPE) {
			onClose();
			return true;
		}

		if (!this.inputFocused) {
			return super.keyPressed(event);
		}

		if (event.hasControlDownWithQuirk() && key == InputConstants.KEY_V) {
			insert(Minecraft.getInstance().keyboardHandler.getClipboard());
			return true;
		}

		if (key == InputConstants.KEY_BACKSPACE) {
			if (this.cursor > 0) {
				this.input = this.input.substring(0, this.cursor - 1) + this.input.substring(this.cursor);
				this.cursor--;
				setStatus(null, TEXT_DIM);
			}

			return true;
		}

		if (key == InputConstants.KEY_DELETE) {
			if (this.cursor < this.input.length()) {
				this.input = this.input.substring(0, this.cursor) + this.input.substring(this.cursor + 1);
				setStatus(null, TEXT_DIM);
			}

			return true;
		}

		if (key == InputConstants.KEY_LEFT) {
			this.cursor = Math.max(0, this.cursor - 1);
			return true;
		}

		if (key == InputConstants.KEY_RIGHT) {
			this.cursor = Math.min(this.input.length(), this.cursor + 1);
			return true;
		}

		if (key == InputConstants.KEY_HOME) {
			this.cursor = 0;
			return true;
		}

		if (key == InputConstants.KEY_END) {
			this.cursor = this.input.length();
			return true;
		}

		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			submit();
			return true;
		}

		return super.keyPressed(event);
	}

	private void submit() {
		String value = this.input.trim();
		AltManager.AddResult result = this.manager.add(value);

		if (result == AltManager.AddResult.INVALID) {
			setStatus("Nickname must be 3-16 characters: A-Z, 0-9, _", HudDraw.DANGER);
			return;
		}

		if (result == AltManager.AddResult.ADDED) {
			setStatus("Added and switched to " + this.manager.selected(), HudDraw.LIME);
		} else {
			setStatus("Already saved - switched to " + this.manager.selected(), HudDraw.AMBER);
		}

		this.input = "";
		this.cursor = 0;
		this.inputFocused = false;
	}

	private void insert(String text) {
		if (text == null || text.isEmpty()) {
			return;
		}

		StringBuilder filtered = new StringBuilder();

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);

			if (isNameCharacter(c)) {
				filtered.append(c);
			}
		}

		if (filtered.isEmpty()) {
			return;
		}

		int room = MAX_NAME_LENGTH - this.input.length();

		if (room <= 0) {
			return;
		}

		String addition = filtered.substring(0, Math.min(room, filtered.length()));
		this.input = this.input.substring(0, this.cursor) + addition + this.input.substring(this.cursor);
		this.cursor += addition.length();
		setStatus(null, TEXT_DIM);
	}

	private int cursorAt(double mouseX) {
		int local = (int) Math.round(mouseX) - (this.fieldX + 8);

		for (int i = 1; i <= this.input.length(); i++) {
			int width = Fonts.width(this.font, this.input.substring(0, i));

			if (local < width) {
				int previous = Fonts.width(this.font, this.input.substring(0, i - 1));
				return local - previous < width - local ? i - 1 : i;
			}
		}

		return this.input.length();
	}

	private void setStatus(@Nullable String value, int color) {
		this.status = value;
		this.statusColor = color;
	}

	private static boolean isNameCharacter(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
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

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
