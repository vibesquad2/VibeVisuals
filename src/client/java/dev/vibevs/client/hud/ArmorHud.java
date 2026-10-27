package dev.vibevs.client.hud;

import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The archive ArmorHud: four compact slots above the hotbar, in helmet-to-boots order. Items are
 * drawn with the normal GUI item renderer and get a tiny durability line; no vanilla widget
 * texture is required, so the strip stays readable at every GUI scale.
 */
public class ArmorHud implements HudElement {
	private static final int SLOT = 18;
	private static final int PAD = 4;
	private static final int HEIGHT = SLOT + PAD * 2;
	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	@Nullable
	private HudRect lastBounds;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();

		if (!HudEditState.previewing() && (!config.hudEnabled || !config.hudArmor)) {
			this.lastBounds = null;
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;

		if (player == null && !HudEditState.previewing()) {
			this.lastBounds = null;
			return;
		}

		int width = PAD * 2 + SLOTS.length * SLOT;
		boolean hasArmor = false;

		for (EquipmentSlot slot : SLOTS) {
			if (player != null && !player.getItemBySlot(slot).isEmpty()) {
				hasArmor = true;
				break;
			}
		}

		if (!hasArmor && !HudEditState.previewing()) {
			this.lastBounds = null;
			return;
		}

		int x = config.hudArmorX >= 0.0f
				? Math.round(config.hudArmorX)
				: (graphics.guiWidth() - width) / 2;
		int y = config.hudArmorY >= 0.0f
				? Math.round(config.hudArmorY)
				: graphics.guiHeight() - HEIGHT - 22;
		x = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - width), x));
		y = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - HEIGHT), y));

		HudDraw.softPanel(graphics, x, y, width, HEIGHT, HudDraw.LIME);

		for (int i = 0; i < SLOTS.length; i++) {
			int slotX = x + PAD + i * SLOT;
			int slotY = y + PAD;
			graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x70040A12);
			graphics.fill(slotX, slotY, slotX + 16, slotY + 1, Colors.withAlpha(HudDraw.LIME, 0x36));

			ItemStack stack = player != null ? player.getItemBySlot(SLOTS[i]) : ItemStack.EMPTY;

			if (!stack.isEmpty()) {
				graphics.renderItem(stack, slotX, slotY);
				drawDurability(graphics, stack, slotX, slotY + 15);
			} else {
				graphics.fill(slotX + 5, slotY + 7, slotX + 11, slotY + 8,
						Colors.withAlpha(HudDraw.LIME, 0x48));
			}
		}

		this.lastBounds = new HudRect(x, y, width, HEIGHT);
	}

	private static void drawDurability(GuiGraphics graphics, ItemStack stack, int x, int y) {
		if (!stack.isDamageableItem()) {
			return;
		}

		int max = Math.max(1, stack.getMaxDamage());
		float ratio = Math.max(0.0f, 1.0f - stack.getDamageValue() / (float) max);
		int fill = Math.max(1, Math.round(16 * ratio));
		int color = Colors.mix(HudDraw.DANGER, HudDraw.LIME, ratio);
		graphics.fill(x, y, x + 16, y + 1, 0x88040A12);
		graphics.fill(x, y, x + fill, y + 1, Colors.withAlpha(color, 0xE0));
	}

	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}
}
