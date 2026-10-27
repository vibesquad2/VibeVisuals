package dev.vibevs.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Small, dependency-free measurements shared by the ported HUD elements. Keeping these out of the
 * renderers means the watermark, the bottom-left readout and the target card all show exactly the
 * same ping, speed and TPS values.
 */
public final class HudMetrics {
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static long lastGameTime = -1L;
	private static long lastSampleNanos;
	private static double smoothedTps = 20.0;

	private HudMetrics() {
	}

	/** Called once per client tick, before the HUD is rendered. */
	public static void tick(Minecraft client) {
		if (client.level == null) {
			lastGameTime = -1L;
			smoothedTps = 20.0;
			return;
		}

		long gameTime = client.level.getGameTime();
		long now = System.nanoTime();

		if (lastGameTime >= 0L && gameTime > lastGameTime && lastSampleNanos != 0L) {
			double seconds = Math.max(1.0E-4, (now - lastSampleNanos) / 1_000_000_000.0);
			double instant = (gameTime - lastGameTime) / seconds;
			instant = Math.max(0.0, Math.min(20.0, instant));
			smoothedTps += (instant - smoothedTps) * 0.18;
		}

		lastGameTime = gameTime;
		lastSampleNanos = now;
	}

	public static double tps() {
		return smoothedTps;
	}

	public static int ping(Minecraft client) {
		LocalPlayer player = client.player;

		if (player == null || client.getConnection() == null) {
			return 0;
		}

		PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
		return info == null ? 0 : Math.max(0, info.getLatency());
	}

	/** Horizontal movement in blocks per second, matching the archive's old BPS readout. */
	public static double blocksPerSecond(LocalPlayer player) {
		Vec3 motion = player.getDeltaMovement();
		return Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 20.0;
	}

	public static String time() {
		return LocalTime.now().format(TIME);
	}

	public static String clock(int seconds) {
		int safe = Math.max(0, seconds);
		int hours = safe / 3600;
		int minutes = (safe / 60) % 60;
		int secs = safe % 60;

		if (hours > 0) {
			return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs);
		}

		return String.format(Locale.ROOT, "%d:%02d", minutes, secs);
	}

	public static String decimal(double value, int places) {
		return String.format(Locale.ROOT, "%." + places + "f", value);
	}
}
