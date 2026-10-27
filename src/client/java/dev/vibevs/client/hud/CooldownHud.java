package dev.vibevs.client.hud;

import dev.vibevs.client.mixin.CooldownInstanceAccessor;
import dev.vibevs.client.mixin.ItemCooldownAccessor;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Item cooldown card ported from the archive. The vanilla cooldown table is read through an
 * accessor, then matched back to the player's items so each row shows the real icon, item name and
 * remaining time. The card disappears when nothing is cooling down, except in the HUD editor.
 */
public class CooldownHud implements HudElement {
	private static final int WIDTH = 138;
	private static final int HEADER = 17;
	private static final int ROW = 16;
	private static final int PAD = 4;
	private static final int MAX_ROWS = 6;

	@Nullable
	private HudRect lastBounds;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();
		boolean preview = HudEditState.previewing();

		if (!preview && (!config.hudEnabled || !config.hudCooldown)) {
			this.lastBounds = null;
			return;
		}

		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;

		if (player == null && !preview) {
			this.lastBounds = null;
			return;
		}

		List<Row> rows = player != null ? collect(player) : new ArrayList<>();

		if (rows.isEmpty() && preview) {
			rows.add(new Row(new ItemStack(Items.GOLDEN_APPLE), "Golden Apple", 8.0f));
			rows.add(new Row(new ItemStack(Items.ENDER_PEARL), "Ender Pearl", 2.0f));
			rows.add(new Row(new ItemStack(Items.SHIELD), "Shield", 1.4f));
		}

		if (rows.isEmpty()) {
			this.lastBounds = null;
			return;
		}

		int visible = Math.min(MAX_ROWS, rows.size());
		int height = HEADER + PAD + visible * ROW + 2;
		int x = config.hudCooldownX >= 0.0f
				? Math.round(config.hudCooldownX)
				: 4;
		int y = config.hudCooldownY >= 0.0f
				? Math.round(config.hudCooldownY)
				: 72;
		x = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - WIDTH), x));
		y = Math.max(0, Math.min(Math.max(0, graphics.guiHeight() - height), y));

		HudDraw.softPanel(graphics, x, y, WIDTH, height, HudDraw.AMBER);
		HudDraw.softHeader(graphics, client.font, x, y, WIDTH, "COOLDOWNS",
				String.valueOf(rows.size()), HudIcons.Icon.CLOCK, HudDraw.AMBER);

		for (int i = 0; i < visible; i++) {
			Row row = rows.get(i);
			int rowX = x + PAD;
			int rowY = y + HEADER + 1 + i * ROW;
			int rowWidth = WIDTH - PAD * 2;
			HudDraw.roundFill(graphics, rowX, rowY, rowWidth, ROW - 2, 3, 0x8A0A111C);
			graphics.fill(rowX, rowY + 2, rowX + 1, rowY + ROW - 4,
					Colors.withAlpha(HudDraw.AMBER, 0xA0));
			graphics.renderItem(row.item, rowX + 1, rowY - 1);

			String name = trim(client.font, row.name, rowWidth - 56);
			Fonts.draw(graphics, client.font, name, rowX + 21, rowY + 3, HudDraw.TEXT);
			String time = HudMetrics.clock((int) Math.ceil(row.seconds));
			Fonts.draw(graphics, client.font, time,
					rowX + rowWidth - 5 - Fonts.width(client.font, time), rowY + 3, HudDraw.AMBER);
		}

		this.lastBounds = new HudRect(x, y, WIDTH, height);
	}

	private static List<Row> collect(LocalPlayer player) {
		ItemCooldowns cooldowns = player.getCooldowns();
		ItemCooldownAccessor accessor = (ItemCooldownAccessor) cooldowns;
		Map<Identifier, ?> table = accessor.vibevs$cooldowns();
		int now = accessor.vibevs$tickCount();
		Map<Identifier, Row> found = new LinkedHashMap<>();

		if (table.isEmpty()) {
			return new ArrayList<>();
		}

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);

			if (stack.isEmpty()) {
				continue;
			}

			Identifier group = cooldowns.getCooldownGroup(stack);

			if (!table.containsKey(group) || found.containsKey(group)) {
				continue;
			}

			Object value = table.get(group);

			if (value instanceof CooldownInstanceAccessor instance) {
				int remaining = instance.vibevs$endTime() - now;

				if (remaining > 0) {
					found.put(group, new Row(stack.copy(), stack.getHoverName().getString(), remaining / 20.0f));
				}
			}
		}

		List<Row> rows = new ArrayList<>(found.values());
		rows.sort(Comparator.comparingDouble(Row::seconds));
		return rows;
	}

	private static String trim(Font font, String text, int maxWidth) {
		if (Fonts.width(font, text) <= maxWidth) {
			return text;
		}

		String ellipsis = "...";

		while (!text.isEmpty() && Fonts.width(font, text + ellipsis) > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}

		return text + ellipsis;
	}

	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}

	private record Row(ItemStack item, String name, float seconds) {
	}
}
