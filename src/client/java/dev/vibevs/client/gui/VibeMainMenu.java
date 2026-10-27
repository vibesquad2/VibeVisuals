package dev.vibevs.client.gui;

import dev.vibevs.VibeVisuals;
import dev.vibevs.client.hud.Fonts;
import dev.vibevs.client.hud.HudDraw;
import dev.vibevs.util.Colors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Random;

/**
 * Custom title screen: a synthwave skyline, neon sun and perspective grid instead of the vanilla
 * panorama. Replaces {@code TitleScreen} through {@code MinecraftMixin}; can be turned off with the
 * {@code Custom Main Menu} setting.
 */
public class VibeMainMenu extends Screen {
	private static final String TITLE = "VibeVisuals";
	private static final String SUBTITLE = "// CUSTOM CLIENT VISUALS";
	private static final String[] BUTTON_LABELS = {"Singleplayer", "Multiplayer", "Alt Manager", "Options", "Quit Game"};
	private static final int[] BUTTON_ACCENTS = {HudDraw.CYAN, HudDraw.MAGENTA, HudDraw.VIOLET, HudDraw.LIME,
			HudDraw.DANGER};
	private static final int BUTTON_COUNT = BUTTON_LABELS.length;
	private static final int BUTTON_WIDTH = 200;
	private static final int BUTTON_HEIGHT = 21;
	private static final int BUTTON_GAP = 7;
	private static final int CHIP_WIDTH = 92;
	private static final int CHIP_HEIGHT = 17;
	private static final int TEXT_DIM = 0xFF6F8FA6;

	private static final String VERSION = FabricLoader.getInstance()
			.getModContainer(VibeVisuals.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("dev");
	private static final String MINECRAFT_VERSION = FabricLoader.getInstance()
			.getModContainer("minecraft")
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("1.21.11");

	private final int[] buttonX = new int[BUTTON_COUNT];
	private final int[] buttonY = new int[BUTTON_COUNT];
	private int chipX;
	private int chipY;
	private int horizonY;

	public VibeMainMenu() {
		super(Component.literal("VibeVisuals"));
	}

	@Override
	protected void init() {
		int stackHeight = BUTTON_COUNT * BUTTON_HEIGHT + (BUTTON_COUNT - 1) * BUTTON_GAP;
		int stackTop = Math.min(this.height / 2 + 18, this.height - 42 - stackHeight);
		stackTop = Math.max(64, stackTop);

		for (int i = 0; i < BUTTON_COUNT; i++) {
			this.buttonX[i] = (this.width - BUTTON_WIDTH) / 2;
			this.buttonY[i] = stackTop + i * (BUTTON_HEIGHT + BUTTON_GAP);
		}

		this.chipX = this.width - 14 - CHIP_WIDTH;
		this.chipY = 12;
		this.horizonY = Math.max(140, Math.min(this.height - 70, Math.round(this.height * 0.58f)));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		drawSky(graphics);
		drawSun(graphics);
		graphics.fillGradient(0, this.horizonY, this.width, this.height, 0xF0040510, 0xFF020308);
		drawCity(graphics);
		drawGrid(graphics);
		drawMotes(graphics);
		HudDraw.scanlines(graphics, 0, 0, this.width, this.height, 4, 0x04FFFFFF);

		drawTitle(graphics);

		for (int i = 0; i < BUTTON_COUNT; i++) {
			drawButton(graphics, i, mouseX, mouseY);
		}

		drawChip(graphics, mouseX, mouseY);
		drawFooter(graphics);
		HudDraw.corners(graphics, 8, 8, this.width - 16, this.height - 16, 12, 0x2800E5FF);
	}

	private void drawSky(GuiGraphics graphics) {
		graphics.fillGradient(0, 0, this.width, this.horizonY, 0xFF03040B, 0xFF130A24);

		int radius = sunRadius();
		graphics.fillGradient(0, this.horizonY - radius * 3, this.width, this.horizonY,
				0x00FF2BD6, 0x2CFF2BD6);
		graphics.fillGradient(0, this.horizonY - radius, this.width, this.horizonY,
				0x0000E5FF, 0x1800E5FF);
	}

	private int sunRadius() {
		return Math.max(42, Math.min(104, Math.min(this.width, this.height) / 7));
	}

	private void drawSun(GuiGraphics graphics) {
		int radius = sunRadius();
		int centerX = this.width / 2;
		int centerY = this.horizonY - radius / 2;
		int center = 0xFFFF7A00;

		for (int row = -radius; row <= radius; row++) {
			int half = (int) Math.sqrt(radius * radius - row * row);
			float t = (row + radius) / (2.0f * radius);
			int color = t < 0.5f
					? Colors.mix(HudDraw.AMBER, center, t * 2.0f)
					: Colors.mix(center, HudDraw.MAGENTA, (t - 0.5f) * 2.0f);
			graphics.fill(centerX - half, centerY + row, centerX + half + 1, centerY + row + 1, color);
		}

		for (int stripeY = centerY - radius + 14; stripeY < this.horizonY; stripeY += 11) {
			int offset = stripeY - centerY;

			if (Math.abs(offset) >= radius) {
				continue;
			}

			int half = (int) Math.sqrt(radius * radius - offset * offset);
			graphics.fill(centerX - half, stripeY, centerX + half + 1, stripeY + 3, 0xE6030408);
		}
	}

	private void drawCity(GuiGraphics graphics) {
		Random random = new Random(0x5EEDC0DEL);
		int x = 0;
		int centerX = this.width / 2;
		int radius = sunRadius();

		while (x < this.width) {
			int width = 12 + random.nextInt(34);
			int height = 8 + random.nextInt(Math.max(12, this.height / 14));
			int top = this.horizonY - height;

			// Keep the synthwave sun visible in the middle of the skyline.
			if (x + width > centerX - radius - 24 && x < centerX + radius + 24) {
				x += width + 4;
				continue;
			}

			graphics.fill(x, top, x + width, this.horizonY, 0xE6050610);

			int roofColor = random.nextBoolean() ? 0x9000E5FF : 0x80FF2BD6;
			graphics.fill(x, top, x + width, top + 1, roofColor);

			if (random.nextInt(3) == 0) {
				int antennaX = x + width / 2;
				int antennaHeight = 4 + random.nextInt(9);
				graphics.fill(antennaX, top - antennaHeight, antennaX + 1, top, 0x60FF2BD6);
				graphics.fill(antennaX, top - antennaHeight, antennaX + 1, top - antennaHeight + 1, 0xFFFF2BD6);
			}

			for (int windowX = x + 3; windowX < x + width - 2; windowX += 5) {
				for (int windowY = top + 5; windowY < this.horizonY - 4; windowY += 6) {
					if (random.nextInt(5) != 0) {
						continue;
					}

					int windowColor = random.nextBoolean() ? 0x6000E5FF : 0x50FF2BD6;
					graphics.fill(windowX, windowY, windowX + 1, windowY + 1, windowColor);
				}
			}

			x += width + 4;
		}
	}

	private void drawGrid(GuiGraphics graphics) {
		int centerX = this.width / 2;
		int floorHeight = Math.max(1, this.height - this.horizonY);

		int horizonGlow = 0x3000E5FF;
		graphics.fill(0, this.horizonY - 1, this.width, this.horizonY + 1, horizonGlow);
		HudDraw.gradientFill(graphics, 0, this.horizonY, this.width, 1, HudDraw.CYAN, HudDraw.MAGENTA);

		int rays = 21;

		for (int ray = -rays / 2; ray <= rays / 2; ray++) {
			int bottomX = centerX + ray * (this.width / (rays - 1));
			int rayColor = (ray & 1) == 0 ? HudDraw.CYAN : HudDraw.MAGENTA;

			for (int y = this.horizonY + 2; y < this.height; y += 4) {
				float t = (y - this.horizonY) / (float) floorHeight;
				int x = Math.round(centerX + (bottomX - centerX) * t);
				int alpha = (int) (0x12 + 0x46 * t);
				graphics.fill(x, y, x + (t > 0.55f ? 2 : 1), Math.min(this.height, y + 4),
						Colors.withAlpha(rayColor, alpha));
			}
		}

		int rows = 8;

		for (int row = 1; row <= rows; row++) {
			float t = row / (float) rows;
			int y = this.horizonY + Math.round(floorHeight * t * t);
			graphics.fill(0, y, this.width, y + 1, Colors.withAlpha(HudDraw.CYAN, (int) (0x10 + 0x42 * t)));
		}
	}

	private void drawTitle(GuiGraphics graphics) {
		float scale = 2.4f;
		int textWidth = Fonts.width(this.font, TITLE);
		float x = (this.width - textWidth * scale) / 2.0f;
		int baseY = Math.max(24, this.buttonY[0] - 86);
		long time = System.currentTimeMillis();
		boolean glitch = (time / 90L) % 47L < 3L;
		int jitter = glitch ? (int) ((time / 37L) % 9L) - 4 : 0;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + jitter, baseY);
		graphics.pose().scale(scale, scale);
		Fonts.draw(graphics, this.font, TITLE, 3, 2, 0x90000000);
		Fonts.draw(graphics, this.font, TITLE, glitch ? -3 : -2, 0, 0x5500E5FF);
		Fonts.draw(graphics, this.font, TITLE, glitch ? 3 : 2, 0, 0x55FF2BD6);
		Fonts.draw(graphics, this.font, TITLE, 0, 0, 0xFFF2FFFF);
		graphics.pose().popMatrix();

		int underlineY = baseY + (int) (11 * scale) + 7;
		int lineWidth = Math.max(90, Math.min(180, this.width / 4));
		graphics.fill(this.width / 2 - lineWidth / 2 - 2, underlineY - 2,
				this.width / 2 + lineWidth / 2 + 2, underlineY + 4, 0x20FF2BD6);
		HudDraw.gradientFill(graphics, this.width / 2 - lineWidth / 2, underlineY,
				lineWidth, 2, HudDraw.CYAN, HudDraw.MAGENTA);
		HudDraw.diamond(graphics, this.width / 2, underlineY + 1, 2, 0xFFF2FFFF);
		Fonts.drawCentered(graphics, this.font, SUBTITLE, this.width / 2, underlineY + 8,
				0xFF7FC9D8);
	}

	private void drawButton(GuiGraphics graphics, int index, int mouseX, int mouseY) {
		int x = this.buttonX[index];
		int y = this.buttonY[index];
		int accent = BUTTON_ACCENTS[index];
		boolean hovered = isInside(mouseX, mouseY, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);

		HudDraw.button(graphics, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, accent, hovered,
				hovered ? 0xE00C1420 : 0xC0070B14);
		graphics.fill(x + 4, y + 5, x + 6, y + BUTTON_HEIGHT - 5,
				hovered ? accent : Colors.withAlpha(accent, 0x70));
		Fonts.draw(graphics, this.font, BUTTON_LABELS[index], x + 18, y + 6,
				hovered ? 0xFFFFFFFF : 0xFFCFEAF2);

		String number = String.format(Locale.ROOT, "%02d", index + 1);
		Fonts.draw(graphics, this.font, number, x + BUTTON_WIDTH - 22, y + 6,
				hovered ? accent : TEXT_DIM);

		if (hovered) {
			HudDraw.diamond(graphics, x + BUTTON_WIDTH - 10, y + BUTTON_HEIGHT / 2, 1, accent);
		}
	}

	private void drawChip(GuiGraphics graphics, int mouseX, int mouseY) {
		boolean hovered = isInside(mouseX, mouseY, this.chipX, this.chipY, CHIP_WIDTH, CHIP_HEIGHT);
		HudDraw.button(graphics, this.chipX, this.chipY, CHIP_WIDTH, CHIP_HEIGHT, HudDraw.MAGENTA, hovered,
				hovered ? 0xE0180A1C : 0xC0080B14);
		HudDraw.diamond(graphics, this.chipX + 10, this.chipY + CHIP_HEIGHT / 2, 2, HudDraw.MAGENTA);
		Fonts.draw(graphics, this.font, "CLICKGUI", this.chipX + 18, this.chipY + 5,
				hovered ? 0xFFFFFFFF : 0xFFFFB8F0);
	}

	private void drawFooter(GuiGraphics graphics) {
		Fonts.draw(graphics, this.font, "VV // " + VERSION, 12, this.height - 14, TEXT_DIM);

		String center = "MC " + MINECRAFT_VERSION + " // FABRIC";
		Fonts.drawCentered(graphics, this.font, center, this.width / 2, this.height - 14,
				0xFF7FC9D8);

		String social = "t.me/VibeVisualss";
		Fonts.draw(graphics, this.font, social, this.width - 12 - Fonts.width(this.font, social),
				this.height - 14, 0xFF9FEFFF);
	}

	private void drawMotes(GuiGraphics graphics) {
		long time = System.currentTimeMillis();

		for (int i = 0; i < 9; i++) {
			double angle = time / 9000.0 + i * 1.7;
			int x = (int) (this.width * (0.08 + 0.84 * (Math.sin(angle) + 1.0) / 2.0));
			int y = (int) (this.height * (0.10 + 0.48 * (Math.cos(angle * 1.3 + i) + 1.0) / 2.0));
			int radius = i % 3 == 0 ? 2 : 1;
			int color = i % 2 == 0 ? HudDraw.CYAN : HudDraw.MAGENTA;
			HudDraw.diamond(graphics, x, y, radius, Colors.withAlpha(color, 0x55));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			if (isInside(event.x(), event.y(), this.chipX, this.chipY, CHIP_WIDTH, CHIP_HEIGHT)) {
				Minecraft.getInstance().setScreen(new ClickGuiScreen(this));
				return true;
			}

			for (int i = 0; i < BUTTON_COUNT; i++) {
				if (isInside(event.x(), event.y(), this.buttonX[i], this.buttonY[i], BUTTON_WIDTH,
						BUTTON_HEIGHT)) {
					activate(i);
					return true;
				}
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	private void activate(int index) {
		Minecraft client = Minecraft.getInstance();

		switch (index) {
			case 0 -> client.setScreen(new SelectWorldScreen(this));
			case 1 -> client.setScreen(new JoinMultiplayerScreen(this));
			case 2 -> client.setScreen(new AltManagerScreen(this));
			case 3 -> client.setScreen(new OptionsScreen(this, client.options));
			case 4 -> client.stop();
			default -> {
			}
		}
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
