package dev.vibevs.client.hud;

import dev.vibevs.client.feature.TargetTracker;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Compact target card ported from the archive: a 26 px head, name and numeric HP, a green-to-red
 * health bar with absorption overflow and an armor/held-item row above the card. Hits spawn tiny
 * custom pixel particles (plain GUI fills, never Minecraft particles), and the head shrinks a
 * little during the hurt animation.
 */
public class TargetHud implements HudElement {
	private static final int WIDTH = 126;
	private static final int HEIGHT = 38;
	private static final int HEAD = 26;
	private static final int FRAME = 28;
	private static final int PAD = 5;
	private static final int BAR_HEIGHT = 4;
	private static final int TRACK_COLOR = 0xCC04070C;
	private static final int FLASH_COLOR = 0x00FF3B30;
	private static final String PLACEHOLDER_NAME = "Target";
	private static final int ARMOR_ROW_HEIGHT = 11;

	@Nullable
	private HudRect lastBounds;
	@Nullable
	private LivingEntity trackedTarget;
	private float shownHealth = -1.0f;
	private float delayedHealth = -1.0f;
	private float lastRenderedHealth = -1.0f;
	private int lastHurtTime;
	private long lastFrameNanos;
	private final List<PixelParticle> particles = new ArrayList<>();
	private final Random random = new Random();

	@Override
	public void render(GuiGraphics graphics, DeltaTracker delta) {
		VibeConfig config = VibeConfig.get();

		if (!HudEditState.previewing() && (!config.hudEnabled || !config.hudTarget)) {
			this.lastBounds = null;
			this.particles.clear();
			return;
		}

		LivingEntity target = TargetTracker.target() instanceof LivingEntity living && living.isAlive()
				? living
				: null;

		if (target == null && !HudEditState.previewing()) {
			this.lastBounds = null;
			this.trackedTarget = null;
			this.particles.clear();
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		float health = target != null ? target.getHealth() : 14.0f;
		float maxHealth = target != null ? Math.max(1.0f, target.getMaxHealth()) : 20.0f;
		float absorption = target != null ? target.getAbsorptionAmount() : 0.0f;
		updateHealth(target, health);
		updateParticles(delta);

		if (target != null) {
			spawnHitParticles(config, target, health);
			this.lastRenderedHealth = health;
			this.lastHurtTime = target.hurtTime;
		}

		float fraction = Math.max(0.0f, Math.min(1.0f, this.shownHealth / maxHealth));
		float delayedFraction = Math.max(0.0f, Math.min(1.0f, this.delayedHealth / maxHealth));
		float absorptionFraction = Math.max(0.0f, Math.min(1.0f, absorption / maxHealth));
		String name = target != null ? target.getName().getString() : PLACEHOLDER_NAME;
		String hp = (health == Math.round(health) ? Integer.toString(Math.round(health))
				: String.format(Locale.ROOT, "%.1f", health));
		int hpWidth = Fonts.width(font, hp);
		int contentX = PAD + FRAME + PAD;
		name = Fonts.fit(font, name, WIDTH - contentX - hpWidth - PAD - 6);

		int topPadding = config.hudTargetArmor ? ARMOR_ROW_HEIGHT : 0;
		int x = config.hudTargetX >= 0.0f
				? Math.round(config.hudTargetX)
				: (graphics.guiWidth() - WIDTH) / 2;
		int y = config.hudTargetY >= 0.0f
				? Math.round(config.hudTargetY)
				: graphics.guiHeight() / 2 + 32;
		x = Math.max(0, Math.min(Math.max(0, graphics.guiWidth() - WIDTH), x));
		y = Math.max(topPadding, Math.min(Math.max(topPadding, graphics.guiHeight() - HEIGHT), y));

		int accent = target instanceof AbstractClientPlayer ? HudDraw.CYAN : HudDraw.MAGENTA;
		int flash = target != null ? Math.min(10, target.hurtTime) : 0;
		HudDraw.softPanel(graphics, x, y, WIDTH, HEIGHT, accent);

		if (flash > 0) {
			graphics.fill(x + 1, y + 1, x + 3, y + HEIGHT - 1,
					Colors.withAlpha(HudDraw.DANGER, 0x40 + flash * 0x08));
			graphics.fill(x + 3, y + 1, x + width() - 3, y + 2,
					Colors.withAlpha(HudDraw.DANGER, flash * 0x07));
		}

		if (config.hudTargetArmor) {
			drawArmorRow(graphics, target, x, y - 1, WIDTH);
		}

		int headX = x + PAD;
		int headY = y + (HEIGHT - HEAD) / 2;
		HudDraw.cutFill(graphics, headX - 3, headY - 3, FRAME, FRAME, 4,
				Colors.withAlpha(accent, 0xC0));
		HudDraw.cutFill(graphics, headX - 2, headY - 2, HEAD + 4, HEAD + 4, 3, 0xF0070B13);
		graphics.fill(headX - 3, headY - 3, headX + HEAD + 3, headY - 2,
				Colors.withAlpha(accent, 0xD8));
		HudDraw.corners(graphics, headX - 3, headY - 3, HEAD + 6, HEAD + 6, 3,
				Colors.withAlpha(accent, 0x70));

		int headShrink = config.hudTargetHeadShrink && flash > 0 ? 2 : 0;
		drawHead(graphics, client, target, headX + headShrink / 2, headY + headShrink / 2,
				HEAD - headShrink, flash);

		int textY = y + 5;
		Fonts.draw(graphics, font, name, x + contentX, textY, HudDraw.TEXT);

		int hpColor = config.hudTargetColorHp
				? Colors.hsb(120.0f * fraction, 0.86f, 1.0f)
				: HudDraw.CYAN;
		Fonts.draw(graphics, font, hp, x + WIDTH - PAD - hpWidth, textY, hpColor);

		int barX = x + contentX;
		int barWidth = Math.max(1, WIDTH - contentX - PAD);
		int barY = y + HEIGHT - PAD - BAR_HEIGHT;
		int absorptionY = barY - 5;

		if (absorption > 0.0f) {
			int absorptionWidth = Math.max(2, Math.round(barWidth * absorptionFraction));
			HudDraw.cutFill(graphics, barX, absorptionY, barWidth, 2, 1, 0x88040A12);
			HudDraw.cutFill(graphics, barX, absorptionY, absorptionWidth, 2, 1, HudDraw.AMBER);
		}

		int delayed = Math.round(barWidth * delayedFraction);
		int filled = Math.round(barWidth * fraction);
		HudDraw.cutFill(graphics, barX, barY, barWidth, BAR_HEIGHT, 2, TRACK_COLOR);

		if (delayed > filled) {
			HudDraw.cutFill(graphics, barX, barY, delayed, BAR_HEIGHT, 2,
					Colors.withAlpha(HudDraw.DANGER, 0xC8));
		}

		if (filled > 0) {
			graphics.fill(barX - 1, barY - 1, barX + filled + 1, barY + BAR_HEIGHT + 1,
					Colors.withAlpha(hpColor, 0x28));
			HudDraw.cutFill(graphics, barX, barY, Math.max(3, filled), BAR_HEIGHT, 2, hpColor);
			graphics.fill(barX + 1, barY, Math.min(barX + filled, barX + barWidth), barY + 1,
					Colors.withAlpha(HudDraw.TEXT, 0x70));
		}

		drawParticles(graphics, x, y);
		this.lastBounds = new HudRect(x, y - topPadding, WIDTH, HEIGHT + topPadding);
	}

	private static int width() {
		return WIDTH;
	}

	private void spawnHitParticles(VibeConfig config, LivingEntity target, float health) {
		if (!config.hudTargetParticles) {
			return;
		}

		boolean damaged = this.lastRenderedHealth >= 0.0f && health < this.lastRenderedHealth - 0.01f;
		boolean newHurt = target.hurtTime > this.lastHurtTime && target.hurtTime >= 10;

		if (!damaged && !newHurt) {
			return;
		}

		for (int i = 0; i < 6; i++) {
			float nx = PAD + FRAME + 2 + this.random.nextFloat() * (WIDTH - FRAME - 12);
			float ny = HEIGHT - 7 + this.random.nextFloat() * 4;
			float vx = (this.random.nextFloat() - 0.5f) * 28.0f;
			float vy = -12.0f - this.random.nextFloat() * 24.0f;
			int color = i % 2 == 0 ? HudDraw.CYAN : HudDraw.DANGER;
			this.particles.add(new PixelParticle(nx, ny, vx, vy, 0.45f + this.random.nextFloat() * 0.25f, color));
		}
	}

	private void updateParticles(DeltaTracker delta) {
		float dt = Math.min(0.1f, delta.getGameTimeDeltaTicks() / 20.0f);
		dt = Math.max(0.001f, dt);

		for (PixelParticle particle : this.particles) {
			particle.life -= dt;
			particle.vy += 42.0f * dt;
			particle.nx += particle.vx * dt;
			particle.ny += particle.vy * dt;
		}

		this.particles.removeIf(particle -> particle.life <= 0.0f);
	}

	private void drawParticles(GuiGraphics graphics, int x, int y) {
		for (PixelParticle particle : this.particles) {
			float alpha = Math.max(0.0f, Math.min(1.0f, particle.life / particle.maxLife));
			int color = Colors.withAlpha(particle.color, Math.round(alpha * 220.0f));
			int px = x + Math.round(particle.nx);
			int py = y + Math.round(particle.ny);
			graphics.fill(px, py, px + 2, py + 2, color);
			graphics.fill(px + 1, py - 1, px + 2, py, Colors.withAlpha(particle.color, Math.round(alpha * 110.0f)));
		}
	}

	private void updateHealth(@Nullable LivingEntity target, float health) {
		long now = System.nanoTime();
		float dt = this.lastFrameNanos == 0L ? 0.016f
				: Math.min(0.1f, (now - this.lastFrameNanos) / 1_000_000_000.0f);
		this.lastFrameNanos = now;

		if (target != this.trackedTarget) {
			this.trackedTarget = target;
			this.shownHealth = health;
			this.delayedHealth = health;
			this.lastRenderedHealth = health;
			this.lastHurtTime = target != null ? target.hurtTime : 0;
			this.particles.clear();
			return;
		}

		if (this.shownHealth < 0.0f) {
			this.shownHealth = health;
			this.delayedHealth = health;
			return;
		}

		float showLerp = 1.0f - (float) Math.exp(-dt * 14.0f);
		this.shownHealth += (health - this.shownHealth) * showLerp;

		if (Math.abs(health - this.shownHealth) < 0.02f) {
			this.shownHealth = health;
		}

		if (this.delayedHealth < this.shownHealth) {
			this.delayedHealth = this.shownHealth;
		} else {
			float trailLerp = 1.0f - (float) Math.exp(-dt * 2.8f);
			this.delayedHealth += (this.shownHealth - this.delayedHealth) * trailLerp;

			if (this.delayedHealth - this.shownHealth < 0.02f) {
				this.delayedHealth = this.shownHealth;
			}
		}
	}

	private static void drawArmorRow(GuiGraphics graphics, @Nullable LivingEntity target, int x, int y, int width) {
		List<ItemStack> items = new ArrayList<>();

		if (target != null) {
			ItemStack offhand = target.getItemBySlot(EquipmentSlot.OFFHAND);
			ItemStack mainhand = target.getItemBySlot(EquipmentSlot.MAINHAND);

			if (!offhand.isEmpty()) {
				items.add(offhand);
			}

			if (!mainhand.isEmpty()) {
				items.add(mainhand);
			}

			for (EquipmentSlot slot : new EquipmentSlot[] {
					EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
			}) {
				ItemStack stack = target.getItemBySlot(slot);

				if (!stack.isEmpty()) {
					items.add(stack);
				}
			}
		} else if (HudEditState.previewing()) {
			items.add(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
			items.add(new ItemStack(net.minecraft.world.item.Items.SHIELD));
			items.add(new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
			items.add(new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
			items.add(new ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS));
			items.add(new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
		}

		if (items.isEmpty()) {
			return;
		}

		int step = 9;
		int total = (items.size() - 1) * step + 8;
		int startX = x + (width - total) / 2;
		int rowY = y - ARMOR_ROW_HEIGHT;

		HudDraw.roundFill(graphics, startX - 3, rowY - 1, total + 6, 10, 3, 0x77070A12);

		for (int i = 0; i < items.size(); i++) {
			int itemX = startX + i * step;
			graphics.pose().pushMatrix();
			graphics.pose().translate(itemX, rowY);
			graphics.pose().scale(0.5f, 0.5f);
			graphics.renderItem(items.get(i), 0, 0);
			graphics.pose().popMatrix();
		}
	}

	/** Player skin face, spawn egg for mobs, flat placeholder in the editor. Reddens on hits. */
	private static void drawHead(GuiGraphics graphics, Minecraft client, @Nullable LivingEntity target,
			int headX, int headY, int headSize, int flash) {
		if (target instanceof AbstractClientPlayer player) {
			int tint = 0xFFFFFFFF;

			if (flash > 0) {
				int channel = Math.max(70, 255 - flash * 17);
				tint = 0xFF000000 | (0xFF << 16) | (channel << 8) | channel;
			}

			PlayerFaceRenderer.draw(graphics, player.getSkin(), headX, headY, headSize, tint);
			return;
		}

		if (target == null && client.player != null && HudEditState.previewing()) {
			PlayerFaceRenderer.draw(graphics, client.player.getSkin(), headX, headY, headSize);
			return;
		}

		ItemStack egg = null;

		if (target != null) {
			SpawnEggItem spawnEgg = SpawnEggItem.byId(target.getType());

			if (spawnEgg != null) {
				egg = new ItemStack(spawnEgg);
			}
		}

		if (egg != null) {
			graphics.renderItem(egg, headX + Math.max(0, (headSize - 16) / 2),
					headY + Math.max(0, (headSize - 16) / 2));
		} else {
			HudDraw.roundFill(graphics, headX, headY, headSize, headSize, 4, TRACK_COLOR);
		}

		if (flash > 0) {
			int alpha = Math.min(0xAA, 0x30 + flash * 0x0C);
			graphics.fill(headX, headY, headX + headSize, headY + headSize, (alpha << 24) | FLASH_COLOR);
		}
	}

	@Nullable
	public HudRect bounds() {
		return this.lastBounds;
	}

	private static final class PixelParticle {
		private float nx;
		private float ny;
		private float vx;
		private float vy;
		private float life;
		private final float maxLife;
		private final int color;

		private PixelParticle(float nx, float ny, float vx, float vy, float life, int color) {
			this.nx = nx;
			this.ny = ny;
			this.vx = vx;
			this.vy = vy;
			this.life = life;
			this.maxLife = life;
			this.color = color;
		}
	}
}
