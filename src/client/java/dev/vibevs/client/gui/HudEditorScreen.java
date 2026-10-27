package dev.vibevs.client.gui;

import dev.vibevs.client.hud.ArmorHud;
import dev.vibevs.client.hud.CooldownHud;
import dev.vibevs.client.hud.CrosshairHud;
import dev.vibevs.client.hud.EffectsHud;
import dev.vibevs.client.hud.Fonts;
import dev.vibevs.client.hud.HudDraw;
import dev.vibevs.client.hud.HudEditState;
import dev.vibevs.client.hud.HudRect;
import dev.vibevs.client.hud.InfoHud;
import dev.vibevs.client.hud.IslandHud;
import dev.vibevs.client.hud.KeybindHud;
import dev.vibevs.client.hud.ScoreboardHud;
import dev.vibevs.client.hud.TargetHud;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Drag-and-drop layout editor for the HUD elements. Opened from the ClickGUI button. Left click
 * drags an element, right click resets it to the default anchor, Esc or the Done button returns.
 */
public class HudEditorScreen extends Screen {
	private static final int DONE_WIDTH = 88;
	private static final int DONE_HEIGHT = 17;
	private static final int RESET_WIDTH = 104;
	private static final int RESET_HEIGHT = 17;
	private static final int HINT_COLOR = HudDraw.TEXT_DIM;
	private static final int LABEL_COLOR = 0xFF9FEFFF;

	private final Screen parent;
	private final IslandHud island = new IslandHud();
	private final EffectsHud effects = new EffectsHud();
	private final KeybindHud keybinds = new KeybindHud();
	private final TargetHud target = new TargetHud();
	private final InfoHud info = new InfoHud();
	private final CrosshairHud crosshair = new CrosshairHud();
	private final ArmorHud armor = new ArmorHud();
	private final CooldownHud cooldown = new CooldownHud();
	private final ScoreboardHud scoreboard = new ScoreboardHud();

	@Nullable
	private Element dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	private int doneX;
	private int doneY;
	private int resetX;
	private int resetY;

	private enum Element {
		ISLAND, INFO, KEYBINDS, EFFECTS, TARGET, ARMOR, COOLDOWN, SCOREBOARD, CROSSHAIR
	}

	public HudEditorScreen(@Nullable Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
		announceInChat();
	}

	/** The layout editor is driven from chat as well, so print the controls there. */
	private static void announceInChat() {
		LocalPlayer player = Minecraft.getInstance().player;

		if (player == null) {
			return;
		}

		player.displayClientMessage(Component.literal("VibeVisuals").withStyle(ChatFormatting.AQUA)
				.append(Component.literal(" — редактор HUD: ЛКМ тянет элемент, ПКМ сбрасывает позицию.")
						.withStyle(ChatFormatting.WHITE)), false);
		player.displayClientMessage(Component.literal("Новая раскладка: кнопка ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal("RESET LAYOUT").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(". Открыть снова: ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("/vibevs hud").withStyle(ChatFormatting.YELLOW)), false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void init() {
		super.init();
		HudEditState.setPreviewing(true);
		this.doneX = this.width - DONE_WIDTH - 8;
		this.doneY = this.height - 30;
		this.resetX = Math.max(8, this.doneX - RESET_WIDTH - 12);
		this.resetY = this.doneY;
	}

	@Override
	public void removed() {
		super.removed();
		HudEditState.setPreviewing(false);
	}

	@Override
	public void onClose() {
		VibeConfig.get().requestSave();
		Minecraft.getInstance().setScreen(this.parent);
	}

	// ------------------------------------------------------------- rendering

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0xA8050710);

		for (int x = 0; x < this.width; x += 48) {
			graphics.fill(x, 0, x + 1, this.height, 0x0600E5FF);
		}

		for (int y = 0; y < this.height; y += 48) {
			graphics.fill(0, y, this.width, y + 1, 0x0600E5FF);
		}

		HudDraw.scanlines(graphics, 0, 0, this.width, this.height, 4, 0x03FFFFFF);

		DeltaTracker delta = Minecraft.getInstance().getDeltaTracker();
		this.island.render(graphics, delta);
		this.effects.render(graphics, delta);
		this.keybinds.render(graphics, delta);
		this.target.render(graphics, delta);
		this.info.render(graphics, delta);
		this.armor.render(graphics, delta);
		this.cooldown.render(graphics, delta);
		this.scoreboard.render(graphics, delta);
		this.crosshair.render(graphics, delta);

		drawOutline(graphics, Element.ISLAND, "WATERMARK", mouseX, mouseY);
		drawOutline(graphics, Element.INFO, "INFO", mouseX, mouseY);
		drawOutline(graphics, Element.KEYBINDS, "KEYBINDS", mouseX, mouseY);
		drawOutline(graphics, Element.EFFECTS, "POTIONS", mouseX, mouseY);
		drawOutline(graphics, Element.TARGET, "TARGET HUD", mouseX, mouseY);
		drawOutline(graphics, Element.ARMOR, "ARMOR", mouseX, mouseY);
		drawOutline(graphics, Element.COOLDOWN, "COOLDOWNS", mouseX, mouseY);
		drawOutline(graphics, Element.SCOREBOARD, "SCOREBOARD", mouseX, mouseY);
		drawOutline(graphics, Element.CROSSHAIR, "CROSSHAIR", mouseX, mouseY);

		this.doneX = this.width - DONE_WIDTH - 8;
		this.doneY = this.height - 30;
		this.resetX = Math.max(8, this.doneX - RESET_WIDTH - 12);
		this.resetY = this.doneY;
		boolean hovered = isInside(mouseX, mouseY, this.doneX, this.doneY, DONE_WIDTH, DONE_HEIGHT);
		HudDraw.button(graphics, this.doneX, this.doneY, DONE_WIDTH, DONE_HEIGHT, HudDraw.MAGENTA, hovered,
				hovered ? 0xE0160A1E : 0xE00A0D18);
		Fonts.drawCentered(graphics, this.font, "DONE", this.doneX + DONE_WIDTH / 2, this.doneY + 4, 0xFFF6E9FF);

		boolean resetHovered = isInside(mouseX, mouseY, this.resetX, this.resetY, RESET_WIDTH, RESET_HEIGHT);
		HudDraw.button(graphics, this.resetX, this.resetY, RESET_WIDTH, RESET_HEIGHT, HudDraw.AMBER,
				resetHovered, resetHovered ? 0xE01E1606 : 0xE00D0C09);
		Fonts.drawCentered(graphics, this.font, "RESET LAYOUT", this.resetX + RESET_WIDTH / 2,
				this.resetY + 4, 0xFFFFE9B0);

		String title = "HUD // NEON LAYOUT";
		String hint = "Drag to move - right click resets - Esc returns";
		int titleX = Math.max(8, this.width - 8 - Fonts.width(this.font, title));
		int hintX = Math.max(8, this.width - 8 - Fonts.width(this.font, hint));
		Fonts.draw(graphics, this.font, title, titleX, this.height - 50, HudDraw.CYAN);
		Fonts.draw(graphics, this.font, hint, hintX, this.height - 39, HINT_COLOR);
	}

	private void drawOutline(GuiGraphics graphics, Element element, String label, int mouseX, int mouseY) {
		HudRect rect = boundsOf(element);

		if (rect == null) {
			return;
		}

		boolean active = element == this.dragging || rect.contains(mouseX, mouseY);
		int accent = active ? HudDraw.MAGENTA : HudDraw.CYAN;
		int x = rect.x() - 2;
		int y = rect.y() - 2;
		int width = rect.width() + 4;
		int height = rect.height() + 4;

		if (active) {
			HudDraw.glow(graphics, x, y, width, height, accent);
		}

		int faint = Colors.withAlpha(accent, active ? 0x70 : 0x28);
		graphics.fill(x, y, x + width, y + 1, faint);
		graphics.fill(x, y + height - 1, x + width, y + height, faint);
		graphics.fill(x, y, x + 1, y + height, faint);
		graphics.fill(x + width - 1, y, x + width, y + height, faint);
		HudDraw.corners(graphics, x, y, width, height, 5, accent);
		int labelY = rect.y() - 11;
		int labelX = rect.x();

		if (labelY < 0) {
			labelY = Math.min(this.height - 9, rect.y() + rect.height() + 3);
		}

		labelX = Math.max(4, Math.min(this.width - 4 - Fonts.width(this.font, label), labelX));
		Fonts.draw(graphics, this.font, label, labelX, labelY, active ? 0xFFFF9FE5 : LABEL_COLOR);
	}

	// ------------------------------------------------------------ interaction

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		if (isInside(event.x(), event.y(), this.doneX, this.doneY, DONE_WIDTH, DONE_HEIGHT)) {
			onClose();
			return true;
		}

		if (isInside(event.x(), event.y(), this.resetX, this.resetY, RESET_WIDTH, RESET_HEIGHT)) {
			resetLayout();
			return true;
		}

		for (Element element : Element.values()) {
			HudRect rect = boundsOf(element);

			if (rect == null || !rect.contains(event.x(), event.y())) {
				continue;
			}

			if (event.button() == 1) {
				reset(element);
				VibeConfig.get().requestSave();
				return true;
			}

			this.dragging = element;
			this.dragOffsetX = (int) event.x() - rect.x();
			this.dragOffsetY = (int) event.y() - rect.y();
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.dragging == null) {
			return super.mouseDragged(event, dragX, dragY);
		}

		HudRect rect = boundsOf(this.dragging);

		if (rect != null) {
			int x = (int) event.x() - this.dragOffsetX;
			int y = (int) event.y() - this.dragOffsetY;
			x = Math.max(0, Math.min(Math.max(0, this.width - rect.width()), x));
			y = Math.max(0, Math.min(Math.max(0, this.height - rect.height()), y));
			move(this.dragging, x, y);
		}

		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.dragging != null) {
			this.dragging = null;
			VibeConfig.get().requestSave();
			return true;
		}

		return super.mouseReleased(event);
	}

	// ------------------------------------------------------------- positions

	@Nullable
	private HudRect boundsOf(Element element) {
		return switch (element) {
			case ISLAND -> this.island.bounds();
			case INFO -> this.info.bounds();
			case KEYBINDS -> this.keybinds.bounds();
			case EFFECTS -> this.effects.bounds();
			case TARGET -> this.target.bounds();
			case ARMOR -> this.armor.bounds();
			case COOLDOWN -> this.cooldown.bounds();
			case SCOREBOARD -> this.scoreboard.bounds();
			case CROSSHAIR -> this.crosshair.bounds();
		};
	}

	private static void move(Element element, int x, int y) {
		VibeConfig config = VibeConfig.get();

		switch (element) {
			case ISLAND -> {
				config.hudIslandX = x;
				config.hudIslandY = y;
			}
			case INFO -> {
				config.hudInfoX = x;
				config.hudInfoY = y;
			}
			case KEYBINDS -> {
				config.hudKeybindsX = x;
				config.hudKeybindsY = y;
			}
			case EFFECTS -> {
				config.hudEffectsX = x;
				config.hudEffectsY = y;
			}
			case TARGET -> {
				config.hudTargetX = x;
				config.hudTargetY = y + (config.hudTargetArmor ? 11 : 0);
			}
			case ARMOR -> {
				config.hudArmorX = x;
				config.hudArmorY = y;
			}
			case COOLDOWN -> {
				config.hudCooldownX = x;
				config.hudCooldownY = y;
			}
			case SCOREBOARD -> {
				config.hudScoreboardX = x;
				config.hudScoreboardY = y;
			}
			case CROSSHAIR -> {
				config.hudCrosshairX = x;
				config.hudCrosshairY = y;
			}
		}
	}

	private static void reset(Element element) {
		VibeConfig config = VibeConfig.get();

		switch (element) {
			case ISLAND -> {
				config.hudIslandX = -1.0f;
				config.hudIslandY = 4.0f;
			}
			case INFO -> {
				config.hudInfoX = -1.0f;
				config.hudInfoY = -1.0f;
			}
			case KEYBINDS -> {
				config.hudKeybindsX = -1.0f;
				config.hudKeybindsY = 4.0f;
			}
			case EFFECTS -> {
				config.hudEffectsX = -1.0f;
				config.hudEffectsY = -1.0f;
			}
			case TARGET -> {
				config.hudTargetX = -1.0f;
				config.hudTargetY = -1.0f;
			}
			case ARMOR -> {
				config.hudArmorX = -1.0f;
				config.hudArmorY = -1.0f;
			}
			case COOLDOWN -> {
				config.hudCooldownX = -1.0f;
				config.hudCooldownY = -1.0f;
			}
			case SCOREBOARD -> {
				config.hudScoreboardX = -1.0f;
				config.hudScoreboardY = -1.0f;
			}
			case CROSSHAIR -> {
				config.hudCrosshairX = -1.0f;
				config.hudCrosshairY = -1.0f;
			}
		}
	}

	private void resetLayout() {
		for (Element element : Element.values()) {
			reset(element);
		}

		VibeConfig.get().requestSave();
		LocalPlayer player = Minecraft.getInstance().player;

		if (player != null) {
			player.displayClientMessage(
					Component.literal("HUD: новая раскладка применена").withStyle(ChatFormatting.AQUA), true);
		}
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
