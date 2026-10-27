package dev.vibevs.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vibevs.client.hud.Fonts;
import dev.vibevs.client.hud.HudDraw;
import dev.vibevs.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * PvPSafe confirmation modal. Shown instead of leaving the world or closing the game, so an
 * accidental disconnect (or Alt+F4) cannot drop the player out of a fight. "Stay" returns to the
 * previous screen; the confirm button replays the guarded action.
 */
public class ConfirmLeaveScreen extends Screen {
	private static final int PANEL_WIDTH = 252;
	private static final int PANEL_HEIGHT = 124;
	private static final int BUTTON_WIDTH = 108;
	private static final int BUTTON_HEIGHT = 18;
	private static final int TEXT = 0xFFE8FBFF;
	private static final int TEXT_DIM = 0xFF6F8FA6;

	@Nullable
	private final Screen parent;
	private final String message;
	private final String hint;
	private final String confirmLabel;
	private final Runnable onConfirm;

	private int panelX;
	private int panelY;
	private int stayX;
	private int confirmX;
	private int buttonsY;

	public ConfirmLeaveScreen(@Nullable Screen parent, String message, String hint, String confirmLabel,
			Runnable onConfirm) {
		super(Component.literal("PvPSafe"));
		this.parent = parent;
		this.message = message;
		this.hint = hint;
		this.confirmLabel = confirmLabel;
		this.onConfirm = onConfirm;
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0x9A02040A);

		for (int x = 0; x < this.width; x += 48) {
			graphics.fill(x, 0, x + 1, this.height, 0x0700E5FF);
		}

		for (int y = 0; y < this.height; y += 48) {
			graphics.fill(0, y, this.width, y + 1, 0x0700E5FF);
		}

		HudDraw.scanlines(graphics, 0, 0, this.width, this.height, 4, 0x03FFFFFF);

		this.panelX = (this.width - PANEL_WIDTH) / 2;
		this.panelY = (this.height - PANEL_HEIGHT) / 2;
		this.buttonsY = this.panelY + 92;
		this.stayX = this.panelX + 12;
		this.confirmX = this.panelX + PANEL_WIDTH - 12 - BUTTON_WIDTH;

		int x = this.panelX;
		int y = this.panelY;

		for (int layer = 3; layer >= 1; layer--) {
			HudDraw.cutFill(graphics, x - layer, y - layer, PANEL_WIDTH + layer * 2, PANEL_HEIGHT + layer * 2,
					7 + layer, Colors.withAlpha(HudDraw.DANGER, 0x0C));
		}

		HudDraw.cutFill(graphics, x - 1, y - 1, PANEL_WIDTH + 2, PANEL_HEIGHT + 2, 8,
				Colors.withAlpha(HudDraw.DANGER, 0x90));
		HudDraw.cutFill(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, 7, 0xF5070A13);
		graphics.fillGradient(x + 8, y + 4, x + PANEL_WIDTH - 8, y + 34,
				Colors.withAlpha(HudDraw.DANGER, 0x16), 0x00000000);
		HudDraw.scanlines(graphics, x + 8, y + 8, PANEL_WIDTH - 16, PANEL_HEIGHT - 16, 3, 0x04FFFFFF);

		for (int stripe = 0; stripe < PANEL_WIDTH - 24; stripe += 10) {
			int stripeX = x + 12 + stripe;
			graphics.fill(stripeX, y + 2, Math.min(x + PANEL_WIDTH - 12, stripeX + 5), y + 4,
					(stripe / 10) % 2 == 0 ? Colors.withAlpha(HudDraw.DANGER, 0xB0)
							: Colors.withAlpha(HudDraw.AMBER, 0x70));
		}

		int bracket = Colors.withAlpha(HudDraw.DANGER, 0xC0);
		graphics.fill(x + 8, y + 8, x + 20, y + 9, bracket);
		graphics.fill(x + 8, y + 8, x + 9, y + 20, bracket);
		graphics.fill(x + PANEL_WIDTH - 20, y + PANEL_HEIGHT - 9, x + PANEL_WIDTH - 8, y + PANEL_HEIGHT - 8,
				bracket);
		graphics.fill(x + PANEL_WIDTH - 9, y + PANEL_HEIGHT - 20, x + PANEL_WIDTH - 8, y + PANEL_HEIGHT - 8,
				bracket);

		HudDraw.diamond(graphics, x + 17, y + 19, 4, HudDraw.DANGER);
		Fonts.draw(graphics, this.font, "PvPSafe", x + 28, y + 13, 0xFFFF9BAD);
		String tag = "ANTI-RELOG";
		Fonts.draw(graphics, this.font, tag, x + PANEL_WIDTH - 12 - Fonts.width(this.font, tag),
				y + 14, TEXT_DIM);

		Fonts.draw(graphics, this.font, this.message, x + 12, y + 38, TEXT);
		Fonts.draw(graphics, this.font, this.hint, x + 12, y + 52, TEXT_DIM);
		HudDraw.gradientFill(graphics, x + 12, y + 70, PANEL_WIDTH - 24, 1,
				Colors.withAlpha(HudDraw.DANGER, 0x80), 0x00FF2BD6);

		boolean stayHovered = isInside(mouseX, mouseY, this.stayX, this.buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT);
		HudDraw.button(graphics, this.stayX, this.buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT, HudDraw.CYAN,
				stayHovered, stayHovered ? 0xE00C1A24 : 0xE0070C15);
		Fonts.drawCentered(graphics, this.font, "STAY", this.stayX + BUTTON_WIDTH / 2,
				this.buttonsY + 5, stayHovered ? 0xFFFFFFFF : TEXT);

		boolean confirmHovered = isInside(mouseX, mouseY, this.confirmX, this.buttonsY, BUTTON_WIDTH,
				BUTTON_HEIGHT);
		HudDraw.button(graphics, this.confirmX, this.buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT, HudDraw.DANGER,
				confirmHovered, confirmHovered ? 0xE03A0A1C : 0xE01A0810);
		Fonts.drawCentered(graphics, this.font, this.confirmLabel.toUpperCase(java.util.Locale.ROOT),
				this.confirmX + BUTTON_WIDTH / 2, this.buttonsY + 5,
				confirmHovered ? 0xFFFFFFFF : 0xFFFFC2CC);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			if (isInside(event.x(), event.y(), this.stayX, this.buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
				onClose();
				return true;
			}

			if (isInside(event.x(), event.y(), this.confirmX, this.buttonsY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
				this.onConfirm.run();
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();

		if (key == InputConstants.KEY_ESCAPE) {
			onClose();
			return true;
		}

		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			this.onConfirm.run();
			return true;
		}

		return super.keyPressed(event);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
