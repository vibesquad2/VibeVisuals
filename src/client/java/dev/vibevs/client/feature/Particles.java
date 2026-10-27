package dev.vibevs.client.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's own cosmetic particle system.
 *
 * <p>Every effect is stored and drawn by this class as coloured line geometry — no vanilla
 * {@code ParticleTypes}, no particle engine and no client-side packets. The renderer hooks
 * {@code WorldRenderEvents.AFTER_ENTITIES}, which gives access to the world vertex buffers while
 * the camera-relative transform is active, so the particles are properly depth tested against
 * terrain and entities.</p>
 */
public final class Particles {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final List<Particle> PARTICLES = new ArrayList<>();
	private static final List<Impact> IMPACTS = new ArrayList<>();
	private static final List<Orbit> ORBITS = new ArrayList<>();
	private static final List<WebBurst> WEB_BURSTS = new ArrayList<>();

	private static final int MAX_PARTICLES = 800;
	private static final int MAX_IMPACTS = 8;
	private static final int MAX_ORBITS = 4;
	private static final int MAX_WEB_BURSTS = 4;
	private static final int ORBIT_TICKS = 26;
	private static final int IMPACT_TICKS = 9;
	private static final int WEB_TICKS = 24;

	private static final int CYAN = 0xFF63F5FF;
	private static final int MAGENTA = 0xFFFF5BD5;
	private static final int LIME = 0xFFB7FF63;
	private static final int AMBER = 0xFFFFC85A;
	private static final int VIOLET = 0xFFA77BFF;
	private static final int WHITE = 0xFFF4FAFF;
	private static final int[] HIT_COLORS = {CYAN, CYAN, MAGENTA, MAGENTA, LIME, VIOLET, WHITE};

	private static ClientLevel lastLevel;

	private Particles() {
	}

	/** Called once per client tick. */
	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		LocalPlayer player = client.player;

		if (level == null || player == null) {
			clearAll();
			lastLevel = null;
			return;
		}

		if (level != lastLevel) {
			clearAll();
			lastLevel = level;
		}

		VibeConfig config = VibeConfig.get();

		if (!config.playerTrail) {
			PARTICLES.removeIf(particle -> particle.source == Source.TRAIL);
		}

		if (!config.ambientParticles) {
			PARTICLES.removeIf(particle -> particle.source == Source.AMBIENT);
		}

		if (!config.projectileTrail) {
			PARTICLES.removeIf(particle -> particle.source == Source.PROJECTILE);
		}

		if (!config.hitParticles) {
			PARTICLES.removeIf(particle -> particle.source == Source.HIT);
			IMPACTS.clear();
			ORBITS.clear();
			WEB_BURSTS.clear();
		}

		long now = level.getGameTime();
		PARTICLES.removeIf(particle -> particle.age >= particle.lifetime);
		IMPACTS.removeIf(impact -> now - impact.startTick > impact.duration);
		ORBITS.removeIf(orbit -> now - orbit.startTick > ORBIT_TICKS
				|| orbit.target.isRemoved()
				|| orbit.target.level() != level);
		WEB_BURSTS.removeIf(web -> now - web.startTick > WEB_TICKS);

		for (Particle particle : PARTICLES) {
			particle.tick();
		}

		if (player.isSpectator()) {
			clearAll();
			return;
		}

		if (config.playerTrail) {
			playerTrail(player);
		}

		if (config.ambientParticles) {
			ambient(level, player);
		}

		if (config.projectileTrail) {
			projectiles(level, player);
		}
	}

	/** Sparkle trail behind the player, only while actually moving. */
	private static void playerTrail(LocalPlayer player) {
		Vec3 motion = player.getDeltaMovement();

		if (motion.x * motion.x + motion.z * motion.z < 0.0025) {
			return;
		}

		double x = player.getX() - motion.x * 3.0 + (RANDOM.nextDouble() - 0.5) * 0.55;
		double y = player.getY() + 0.1 + RANDOM.nextDouble() * 0.55;
		double z = player.getZ() - motion.z * 3.0 + (RANDOM.nextDouble() - 0.5) * 0.55;
		Vec3 position = new Vec3(x, y, z);
		Vec3 velocity = new Vec3(-motion.x * 0.06, 0.008 + RANDOM.nextDouble() * 0.012,
				-motion.z * 0.06);
		int color = RANDOM.nextBoolean() ? CYAN : MAGENTA;

		spawn(new Particle(Source.TRAIL, Shape.MOTE, position, velocity,
				12 + RANDOM.nextInt(9), 0.022f + RANDOM.nextFloat() * 0.018f, color,
				0.0f, 0.94f, 0.0f, RANDOM.nextFloat() * 6.0f));
	}

	/** Dust motes during the day, fireflies after dusk. */
	private static void ambient(ClientLevel level, LocalPlayer player) {
		if (RANDOM.nextFloat() > 0.22f) {
			return;
		}

		double angle = RANDOM.nextDouble() * Math.PI * 2.0;
		double radius = 4.0 + RANDOM.nextDouble() * 8.0;
		double x = player.getX() + Math.cos(angle) * radius;
		double z = player.getZ() + Math.sin(angle) * radius;

		if (!level.hasChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4)) {
			return;
		}

		double y = player.getY() + (RANDOM.nextDouble() - 0.35) * 4.5;
		boolean night = isNight(level);
		int color = night ? LIME : (RANDOM.nextBoolean() ? AMBER : WHITE);
		Vec3 velocity = new Vec3((RANDOM.nextDouble() - 0.5) * 0.004,
				night ? RANDOM.nextDouble() * 0.004 : -0.002 - RANDOM.nextDouble() * 0.003,
				(RANDOM.nextDouble() - 0.5) * 0.004);

		spawn(new Particle(Source.AMBIENT, night ? Shape.FIREFLY : Shape.MOTE,
				new Vec3(x, y, z), velocity, 35 + RANDOM.nextInt(35),
				night ? 0.050f + RANDOM.nextFloat() * 0.020f : 0.026f + RANDOM.nextFloat() * 0.022f,
				color, 0.0f, 0.997f, 0.0f, RANDOM.nextFloat() * 6.0f));
	}

	/** Trail behind arrows, tridents and thrown items. */
	private static void projectiles(ClientLevel level, LocalPlayer player) {
		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof AbstractArrow) && !(entity instanceof ThrowableProjectile)) {
				continue;
			}

			if (entity.isRemoved() || entity.distanceToSqr(player) > 64.0 * 64.0) {
				continue;
			}

			Vec3 motion = entity.getDeltaMovement();

			if (motion.lengthSqr() < 0.01 || RANDOM.nextFloat() > 0.65f) {
				continue;
			}

			Vec3 position = new Vec3(entity.getX() + (RANDOM.nextDouble() - 0.5) * 0.12,
					entity.getY() + 0.1 + (RANDOM.nextDouble() - 0.5) * 0.12,
					entity.getZ() + (RANDOM.nextDouble() - 0.5) * 0.12);
			Vec3 velocity = motion.scale(-0.035).add(
					new Vec3((RANDOM.nextDouble() - 0.5) * 0.008, 0.003,
							(RANDOM.nextDouble() - 0.5) * 0.008));
			int color = entity instanceof AbstractArrow ? MAGENTA : CYAN;

			spawn(new Particle(Source.PROJECTILE, Shape.SPARK, position, velocity,
					7 + RANDOM.nextInt(7), 0.024f + RANDOM.nextFloat() * 0.026f, color,
					0.0f, 0.92f, 0.45f + RANDOM.nextDouble() * 0.35,
					RANDOM.nextFloat() * 6.0f));
		}
	}

	/**
	 * Creates the custom impact for the selected style: either the neon shockwave with sparks and an
	 * orbit, or the miniature cobweb with tiny crawling spiders. Called from the game mode mixin on
	 * every attack click; all resulting geometry is drawn by {@link #render}.
	 */
	public static void hitBurst(Player attacker, Entity target) {
		if (!VibeConfig.get().hitParticles || !(target.level() instanceof ClientLevel level)
				|| target.isRemoved() || attacker.level() != level) {
			return;
		}

		Vec3 point = hitPoint(attacker, target);
		Vec3 look = attacker.getViewVector(1.0f).normalize();

		if (VibeConfig.get().hitParticleStyle == VibeConfig.HIT_STYLE_WEB) {
			ORBITS.removeIf(orbit -> orbit.target == target || orbit.target.isRemoved());
			webBurst(level, point, look);
			return;
		}

		impactBurst(level, point, look);

		ORBITS.removeIf(orbit -> orbit.target == target || orbit.target.isRemoved());

		while (ORBITS.size() >= MAX_ORBITS) {
			ORBITS.remove(0);
		}

		ORBITS.add(new Orbit(target, level.getGameTime(), RANDOM.nextInt(360)));
	}

	/** Custom shockwave, cross-flare and the fast sparks thrown out of the hit point. */
	private static void impactBurst(ClientLevel level, Vec3 point, Vec3 look) {
		while (IMPACTS.size() >= MAX_IMPACTS) {
			IMPACTS.remove(0);
		}

		IMPACTS.add(new Impact(point, look, level.getGameTime(), CYAN, MAGENTA, IMPACT_TICKS));

		for (int i = 0; i < 20; i++) {
			double spread = i < 4 ? 1.35 : 1.0;
			double x = (RANDOM.nextDouble() - 0.5) * 2.0 * spread + look.x * 0.8;
			double y = (RANDOM.nextDouble() - 0.5) * 2.0 * spread + look.y * 0.8;
			double z = (RANDOM.nextDouble() - 0.5) * 2.0 * spread + look.z * 0.8;
			Vec3 direction = new Vec3(x, y, z);

			if (direction.lengthSqr() < 1.0E-5) {
				direction = look;
			}

			direction = direction.normalize();
			double speed = i < 4 ? 0.62 + RANDOM.nextDouble() * 0.30
					: 0.22 + RANDOM.nextDouble() * 0.46;
			Vec3 velocity = direction.scale(speed).add(look.scale(0.08))
					.add(new Vec3(0.0, 0.04 + RANDOM.nextDouble() * 0.05, 0.0));
			Vec3 position = point.add(new Vec3(
					(RANDOM.nextDouble() - 0.5) * 0.16,
					(RANDOM.nextDouble() - 0.5) * 0.16,
					(RANDOM.nextDouble() - 0.5) * 0.16));
			int color = i < 4 ? LIME : i < 8 ? WHITE : HIT_COLORS[RANDOM.nextInt(HIT_COLORS.length)];
			double tail = i < 4 ? 0.85 + RANDOM.nextDouble() * 0.35
					: 0.48 + RANDOM.nextDouble() * 0.52;

			spawn(new Particle(Source.HIT, Shape.SPARK, position, velocity,
					7 + RANDOM.nextInt(11), 0.020f + RANDOM.nextFloat() * 0.032f, color,
					0.032f, 0.84f, tail, RANDOM.nextFloat() * 6.0f));
		}
	}

	/** Records a web-style impact that the renderer turns into silk and its tiny spiders. */
	private static void webBurst(ClientLevel level, Vec3 point, Vec3 look) {
		Vec3 normal = look.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();

		while (WEB_BURSTS.size() >= MAX_WEB_BURSTS) {
			WEB_BURSTS.remove(0);
		}

		WEB_BURSTS.add(new WebBurst(point, normal, level.getGameTime(), RANDOM.nextInt(360)));
	}

	/** The point where the attacker's look ray meets the (slightly padded) target hitbox. */
	private static Vec3 hitPoint(Player attacker, Entity target) {
		Vec3 eye = attacker.getEyePosition();
		Vec3 end = eye.add(attacker.getViewVector(1.0f).scale(6.0));
		return target.getBoundingBox().inflate(0.1).clip(eye, end)
				.orElseGet(() -> target.getBoundingBox().getCenter());
	}

	private static void spawn(Particle particle) {
		if (PARTICLES.size() < MAX_PARTICLES) {
			PARTICLES.add(particle);
		}
	}

	private static void clearAll() {
		PARTICLES.clear();
		IMPACTS.clear();
		ORBITS.clear();
		WEB_BURSTS.clear();
	}

	/**
	 * Draws every stored effect with the world renderer's camera-relative vertex buffer. Nothing in
	 * this method uses Minecraft's particle engine or a vanilla particle render type.
	 */
	public static void render(WorldRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;

		if (level == null || client.player == null
				|| (PARTICLES.isEmpty() && IMPACTS.isEmpty() && ORBITS.isEmpty() && WEB_BURSTS.isEmpty())) {
			return;
		}

		PoseStack poseStack = context.matrices();

		if (poseStack == null) {
			return;
		}

		MultiBufferSource consumers = context.consumers();
		Camera camera = client.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();
		float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		long now = level.getGameTime();
		float lineWidth = Math.max(1.0f, client.getWindow().getAppropriateLineWidth());
		PoseStack.Pose pose = poseStack.last();

		Vec3 forward = new Vec3(camera.forwardVector().x(), camera.forwardVector().y(),
				camera.forwardVector().z());
		Vec3 up = new Vec3(camera.upVector().x(), camera.upVector().y(), camera.upVector().z());
		Vec3 right = forward.cross(up);

		if (right.lengthSqr() < 1.0E-6) {
			right = new Vec3(1.0, 0.0, 0.0);
		} else {
			right = right.normalize();
		}

		VertexConsumer buffer = consumers.getBuffer(RenderTypes.linesTranslucent());

		for (Orbit orbit : ORBITS) {
			if (!orbit.target.isRemoved() && orbit.target.level() == level && now - orbit.startTick <= ORBIT_TICKS) {
				renderOrbit(buffer, pose, cameraPos, orbit, partial, now, lineWidth, right, up);
			}
		}

		for (Impact impact : IMPACTS) {
			if (now - impact.startTick <= impact.duration) {
				renderImpact(buffer, pose, cameraPos, impact, partial, now, lineWidth, forward, right, up);
			}
		}

		for (WebBurst web : WEB_BURSTS) {
			if (now - web.startTick <= WEB_TICKS) {
				renderWebBurst(buffer, pose, cameraPos, web, partial, now, lineWidth, forward, right, up);
			}
		}

		for (Particle particle : PARTICLES) {
			renderParticle(buffer, pose, cameraPos, particle, partial, forward, right, up, lineWidth);
		}

		if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch(RenderTypes.linesTranslucent());
		}
	}

	private static void renderParticle(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Particle particle, float partial, Vec3 forward, Vec3 right, Vec3 up, float lineWidth) {
		float life = (particle.age + partial) / particle.lifetime;

		if (life >= 1.0f) {
			return;
		}

		Vec3 position = lerp(particle.previous, particle.position, partial);

		if (position.distanceToSqr(cameraPos) > 128.0 * 128.0) {
			return;
		}

		float fade = Math.min(1.0f, life / 0.12f) * Math.min(1.0f, (1.0f - life) / 0.55f);
		fade = Math.max(0.0f, fade);

		if (fade <= 0.01f) {
			return;
		}

		if (particle.shape == Shape.SPARK) {
			renderSpark(buffer, pose, cameraPos, particle, position, life, fade, right, up, lineWidth);
			return;
		}

		double size = particle.size * (particle.shape == Shape.FIREFLY ? 1.15 : 1.0);
		double pulse = particle.shape == Shape.FIREFLY
				? 0.72 + 0.28 * Math.sin(particle.phase + (particle.age + partial) * 0.32)
				: 1.0;
		int color = Colors.withAlpha(particle.color, Math.round(190.0f * fade * (float) pulse));

		if (particle.shape == Shape.FIREFLY) {
			diamond(buffer, pose, cameraPos, position, right, up, size * 1.55,
					Colors.withAlpha(particle.color, Math.round(48.0f * fade)), lineWidth * 0.7f);
			line(buffer, pose, cameraPos, position.add(right.scale(size)), position.subtract(right.scale(size)),
					color, lineWidth);
			line(buffer, pose, cameraPos, position.add(up.scale(size)), position.subtract(up.scale(size)),
					color, lineWidth);
			line(buffer, pose, cameraPos, position.add(forward.scale(size)), position.subtract(forward.scale(size)),
					Colors.withAlpha(WHITE, Math.round(150.0f * fade)), lineWidth * 0.7f);
		} else {
			line(buffer, pose, cameraPos, position.add(right.scale(size)), position.subtract(right.scale(size)),
					color, lineWidth * 0.8f);
			line(buffer, pose, cameraPos, position.add(up.scale(size)), position.subtract(up.scale(size)),
					color, lineWidth * 0.8f);
			line(buffer, pose, cameraPos, position.add(forward.scale(size)), position.subtract(forward.scale(size)),
					Colors.withAlpha(particle.color, Math.round(90.0f * fade)), lineWidth * 0.6f);
		}
	}

	private static void renderSpark(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Particle particle, Vec3 position, float life, float fade, Vec3 right, Vec3 up, float lineWidth) {
		double speed = particle.velocity.length();
		Vec3 direction = speed > 1.0E-5 ? particle.velocity.scale(1.0 / speed) : right;
		double tailLength = particle.tail * (0.55 + 0.45 * (1.0 - life));
		Vec3 tail = position.subtract(direction.scale(tailLength));
		int color = Colors.withAlpha(particle.color, Math.round(180.0f * fade));
		int core = Colors.withAlpha(WHITE, Math.round(210.0f * fade));

		line(buffer, pose, cameraPos, tail, position, color, lineWidth * 1.05f);
		line(buffer, pose, cameraPos, position.subtract(direction.scale(tailLength * 0.38)), position,
				core, lineWidth * 0.75f);

		double size = particle.size * (0.95 + Math.min(0.75, speed * 0.9));
		diamond(buffer, pose, cameraPos, position, right, up, size,
				Colors.withAlpha(particle.color, Math.round(205.0f * fade)), lineWidth);
		diamond(buffer, pose, cameraPos, position, right, up, size * 1.55,
				Colors.withAlpha(particle.color, Math.round(28.0f * fade)), lineWidth * 0.7f);
	}

	private static void renderImpact(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Impact impact, float partial, long now, float lineWidth, Vec3 forward, Vec3 right, Vec3 up) {
		float progress = Math.max(0.0f, Math.min(1.0f,
				(now + partial - impact.startTick) / (float) impact.duration));
		float fade = (float) Math.pow(1.0f - progress, 1.55);
		float scale = 1.0f - (float) Math.pow(1.0f - progress, 2.5);
		Vec3 normal = impact.normal;

		if (normal.lengthSqr() < 1.0E-6) {
			normal = forward;
		}

		Vec3 reference = Math.abs(normal.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 side = normal.cross(reference).normalize();
		Vec3 ringUp = side.cross(normal).normalize();
		double radius = 0.12 + 0.62 * scale;

		ring(buffer, pose, cameraPos, impact.center, side, ringUp, radius,
				Colors.withAlpha(impact.color, Math.round(160.0f * fade)), lineWidth * 1.15f, 28);
		ring(buffer, pose, cameraPos, impact.center, side, ringUp, radius * 0.72,
				Colors.withAlpha(impact.accent, Math.round(85.0f * fade)), lineWidth * 0.8f, 24);
		ring(buffer, pose, cameraPos, impact.center, side, ringUp, radius * 1.18,
				Colors.withAlpha(impact.color, Math.round(24.0f * fade)), lineWidth * 0.7f, 28);

		for (int i = 0; i < 6; i++) {
			double angle = Math.PI * 2.0 * i / 6.0 + progress * 0.35;
			Vec3 direction = side.scale(Math.cos(angle)).add(ringUp.scale(Math.sin(angle)));
			int rayColor = i % 2 == 0 ? impact.color : impact.accent;
			line(buffer, pose, cameraPos, impact.center.add(direction.scale(radius * 0.12)),
					impact.center.add(direction.scale(radius * 0.55)),
					Colors.withAlpha(rayColor, Math.round(130.0f * fade)), lineWidth * 0.9f);
		}

		line(buffer, pose, cameraPos, impact.center.subtract(normal.scale(0.24 * scale)),
				impact.center.add(normal.scale(0.24 * scale)),
				Colors.withAlpha(WHITE, Math.round(95.0f * fade)), lineWidth * 0.75f);
		diamond(buffer, pose, cameraPos, impact.center, right, up, 0.065 * (1.0 - progress) + 0.015,
				Colors.withAlpha(WHITE, Math.round(195.0f * fade)), lineWidth);
	}

	private static void renderWebBurst(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			WebBurst web, float partial, long now, float lineWidth, Vec3 forward, Vec3 right, Vec3 up) {
		float progress = Math.max(0.0f, Math.min(1.0f,
				(now + partial - web.startTick) / (float) WEB_TICKS));
		float in = Math.min(1.0f, progress / 0.10f);
		float out = Math.min(1.0f, (1.0f - progress) / 0.38f);
		float fade = Math.max(0.0f, Math.min(in, out));

		if (fade <= 0.01f) {
			return;
		}

		Vec3 normal = web.normal.lengthSqr() < 1.0E-6 ? forward : web.normal.normalize();
		Vec3[] basis = webBasis(normal, web.seed, progress);
		Vec3 webNormal = basis[0];
		Vec3 webU = basis[1];
		Vec3 webV = basis[2];
		Vec3 center = web.center.subtract(webNormal.scale(0.035));
		float bloom = 1.0f - (float) Math.pow(1.0f - progress, 2.4f);
		double radius = 0.14 + 0.50 * bloom;

		drawWeb(buffer, pose, cameraPos, center, webU, webV, radius, 3, 8, web.seed,
				fade, lineWidth);

		Vec3 miniCenterA = center.add(webU.scale(radius * 0.54)).add(webV.scale(radius * 0.42));
		Vec3 miniNormalA = webNormal.scale(0.82).add(webU.scale(0.42)).add(webV.scale(0.24));
		Vec3[] miniBasisA = webBasis(miniNormalA, web.seed + 41, progress + 0.03f);
		double miniRadiusA = radius * 0.34;
		drawWeb(buffer, pose, cameraPos, miniCenterA, miniBasisA[1], miniBasisA[2], miniRadiusA,
				2, 6, web.seed + 41, fade * 0.92f, lineWidth * 0.9f);

		Vec3 miniCenterB = center.subtract(webU.scale(radius * 0.44)).add(webV.scale(radius * 0.52));
		Vec3 miniNormalB = webNormal.scale(0.86).subtract(webU.scale(0.30)).add(webV.scale(0.34));
		Vec3[] miniBasisB = webBasis(miniNormalB, web.seed + 73, progress + 0.05f);
		double miniRadiusB = radius * 0.29;
		drawWeb(buffer, pose, cameraPos, miniCenterB, miniBasisB[1], miniBasisB[2], miniRadiusB,
				2, 6, web.seed + 73, fade * 0.88f, lineWidth * 0.85f);

		int connection = Colors.withAlpha(VIOLET, Math.round(44.0f * fade));
		line(buffer, pose, cameraPos, center, miniCenterA, connection, lineWidth * 0.6f);
		line(buffer, pose, cameraPos, center, miniCenterB, connection, lineWidth * 0.6f);

		for (int i = 0; i < 5; i++) {
			double angle = web.seed * 0.03 + i * (Math.PI * 2.0 / 5.0) + progress * 0.26;
			double distance = radius * (0.30 + 0.58 * bloom)
					* (0.82 + 0.18 * noise(web.seed, i, 9));
			Vec3 position = center.add(webU.scale(Math.cos(angle) * distance))
					.add(webV.scale(Math.sin(angle) * distance));
			double size = 0.075 * (0.92 + 0.16 * noise(web.seed, i, 12));
			drawSpider(buffer, pose, cameraPos, position, webU, webV, size, fade,
					lineWidth, web.seed + i * 17, now + partial);
		}

		Vec3 spiderA = miniCenterA.add(miniBasisA[1].scale(miniRadiusA * 0.34))
				.add(miniBasisA[2].scale(miniRadiusA * 0.16));
		drawSpider(buffer, pose, cameraPos, spiderA, miniBasisA[1], miniBasisA[2], 0.052,
				fade * 0.92f, lineWidth * 0.9f, web.seed + 131, now + partial);

		Vec3 spiderB = miniCenterB.add(miniBasisB[1].scale(-miniRadiusB * 0.22))
				.add(miniBasisB[2].scale(miniRadiusB * 0.30));
		drawSpider(buffer, pose, cameraPos, spiderB, miniBasisB[1], miniBasisB[2], 0.046,
				fade * 0.88f, lineWidth * 0.85f, web.seed + 173, now + partial);

		diamond(buffer, pose, cameraPos, center, right, up, 0.034,
				Colors.withAlpha(WHITE, Math.round(205.0f * fade)), lineWidth * 0.9f);
		diamond(buffer, pose, cameraPos, center, right, up, 0.075,
				Colors.withAlpha(VIOLET, Math.round(34.0f * fade)), lineWidth * 0.65f);
	}

	/** Draws one irregular silk web: radial spokes plus sagging polygonal rings. */
	private static void drawWeb(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Vec3 center, Vec3 u, Vec3 v, double radius, int rings, int spokes, int seed,
			float fade, float lineWidth) {
		int spokeColor = Colors.withAlpha(VIOLET, Math.round(118.0f * fade));
		int ringColor = Colors.withAlpha(WHITE, Math.round(142.0f * fade));
		int glintColor = Colors.withAlpha(CYAN, Math.round(82.0f * fade));
		double hub = Math.max(0.012, radius * 0.055);

		for (int s = 0; s < spokes; s++) {
			double angle = Math.PI * 2.0 * s / spokes + (noise(seed, s, 0) - 0.5) * 0.12;
			double reach = radius * (0.94 + noise(seed, s, 1) * 0.10);
			Vec3 root = center.add(u.scale(Math.cos(angle) * hub))
					.add(v.scale(Math.sin(angle) * hub));
			line(buffer, pose, cameraPos, root, ringPoint(center, u, v, reach, angle),
					(s & 1) == 0 ? spokeColor : glintColor, lineWidth * 0.78f);
		}

		for (int r = 1; r <= rings; r++) {
			double ringRadius = radius * (r / (double) rings) * (0.96 + noise(seed, r, 2) * 0.08);

			for (int s = 0; s < spokes; s++) {
				double a1 = Math.PI * 2.0 * s / spokes;
				double a2 = Math.PI * 2.0 * (s + 1) / spokes;
				double mid = (a1 + a2) * 0.5;
				double r1 = ringRadius * (0.94 + noise(seed, r * 17 + s, 3) * 0.08);
				double r2 = ringRadius * (0.94 + noise(seed, r * 17 + s + 1, 4) * 0.08);
				double rm = ringRadius * (0.79 + noise(seed, r * 17 + s, 5) * 0.07);
				Vec3 first = ringPoint(center, u, v, r1, a1);
				Vec3 middle = ringPoint(center, u, v, rm, mid);
				Vec3 second = ringPoint(center, u, v, r2, a2);
				int color = (r & 1) == 0 ? ringColor : glintColor;
				line(buffer, pose, cameraPos, first, middle, color, lineWidth * 0.62f);
				line(buffer, pose, cameraPos, middle, second, color, lineWidth * 0.62f);
			}
		}

		for (int s = 0; s < spokes; s += 3) {
			double angle = Math.PI * 2.0 * s / spokes + 0.06;
			Vec3 glint = ringPoint(center, u, v, radius * (0.97 + noise(seed, s, 8) * 0.06), angle);
			diamond(buffer, pose, cameraPos, glint, u, v, 0.014,
					Colors.withAlpha(WHITE, Math.round(118.0f * fade)), lineWidth * 0.55f);
		}
	}

	/**
	 * A tiny eight-legged silhouette in the web plane. The body and legs are line geometry, so it
	 * remains part of the same custom renderer and never touches the vanilla particle engine.
	 */
	private static void drawSpider(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Vec3 position, Vec3 u, Vec3 v, double size, float fade, float lineWidth,
			int seed, double time) {
		int bodyColor = Colors.withAlpha(WHITE, Math.round(230.0f * fade));
		int legColor = Colors.withAlpha(VIOLET, Math.round(180.0f * fade));
		int legGlow = Colors.withAlpha(CYAN, Math.round(92.0f * fade));
		double pulse = 0.92 + 0.08 * Math.sin(time * 0.55 + seed);
		double body = size * 0.30 * pulse;
		Vec3 abdomen = position.subtract(v.scale(size * 0.18));
		Vec3 head = position.add(v.scale(size * 0.20));

		diamond(buffer, pose, cameraPos, position, u, v, size * 0.66,
				Colors.withAlpha(VIOLET, Math.round(28.0f * fade)), lineWidth * 0.55f);
		diamond(buffer, pose, cameraPos, abdomen, u, v, body, bodyColor, lineWidth * 0.8f);
		diamond(buffer, pose, cameraPos, head, u, v, body * 0.56, bodyColor, lineWidth * 0.7f);

		for (int side = -1; side <= 1; side += 2) {
			for (int leg = 0; leg < 4; leg++) {
				double row = leg - 1.5;
				double wobble = Math.sin(time * 1.7 + seed * 0.4 + leg * 1.3 + side * 0.8)
						* size * 0.10;
				Vec3 root = position.add(u.scale(side * size * 0.12))
						.add(v.scale(row * size * 0.13));
				Vec3 knee = position.add(u.scale(side * size * 0.68))
						.add(v.scale(row * size * 0.50 + wobble));
				Vec3 tip = knee.add(u.scale(side * size * 0.36))
						.add(v.scale(row * size * 0.18 - size * 0.20));
				line(buffer, pose, cameraPos, root, knee, legColor, lineWidth * 0.68f);
				line(buffer, pose, cameraPos, knee, tip,
						(leg & 1) == 0 ? legGlow : legColor, lineWidth * 0.60f);
			}
		}
	}

	/** Builds two in-plane axes and applies a deterministic roll to a web. */
	private static Vec3[] webBasis(Vec3 normal, int seed, float progress) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : normal.normalize();
		Vec3 reference = Math.abs(n.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
		Vec3 u = n.cross(reference);

		if (u.lengthSqr() < 1.0E-6) {
			u = n.cross(new Vec3(1.0, 0.0, 0.0));
		}

		u = u.normalize();
		Vec3 v = n.cross(u).normalize();
		double roll = seed * 0.017453292519943295 + progress * 0.15;
		Vec3 rolledU = u.scale(Math.cos(roll)).add(v.scale(Math.sin(roll))).normalize();
		Vec3 rolledV = n.cross(rolledU).normalize();
		return new Vec3[] {n, rolledU, rolledV};
	}

	/** Stable pseudo-random value in {@code 0..1} for web strand jitter. */
	private static double noise(int seed, int a, int b) {
		double value = Math.sin(seed * 0.731 + a * 12.9898 + b * 78.233) * 43758.5453;
		return value - Math.floor(value);
	}

	private static void renderOrbit(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Orbit orbit, float partial, long now, float lineWidth, Vec3 right, Vec3 up) {
		Entity target = orbit.target;
		Vec3 targetPos = target.getPosition(partial);
		double width = Math.max(0.45, target.getBbWidth() * 0.62) + 0.10;
		Vec3 center = new Vec3(targetPos.x, targetPos.y + target.getBbHeight() * 0.52, targetPos.z);
		float progress = Math.max(0.0f, Math.min(1.0f,
				(now + partial - orbit.startTick) / (float) ORBIT_TICKS));
		float in = Math.min(1.0f, progress / 0.12f);
		float out = Math.min(1.0f, (1.0f - progress) / 0.28f);
		float fade = Math.max(0.0f, Math.min(in, out));
		double angle = (now + partial - orbit.startTick) * 0.16 + orbit.seed * 0.0175;
		Vec3 ringA = new Vec3(1.0, 0.0, 0.0);
		Vec3 ringB = new Vec3(0.0, 0.0, 1.0);
		Vec3 tiltA = new Vec3(1.0, 0.0, 0.0);
		Vec3 tiltB = new Vec3(0.0, 0.42, 0.91).normalize();

		ring(buffer, pose, cameraPos, center, ringA, ringB, width,
				Colors.withAlpha(orbit.color, Math.round(32.0f * fade)), lineWidth * 0.7f, 30);
		ring(buffer, pose, cameraPos, center, tiltA, tiltB, width,
				Colors.withAlpha(orbit.accent, Math.round(26.0f * fade)), lineWidth * 0.7f, 30);

		renderOrbitBead(buffer, pose, cameraPos, center, ringA, ringB, width, angle,
				orbit.color, fade, lineWidth, right, up);
		renderOrbitBead(buffer, pose, cameraPos, center, ringA, ringB, width, angle + Math.PI,
				orbit.accent, fade * 0.85f, lineWidth, right, up);
		renderOrbitBead(buffer, pose, cameraPos, center, tiltA, tiltB, width, -angle + 0.8,
				orbit.accent, fade, lineWidth, right, up);
		renderOrbitBead(buffer, pose, cameraPos, center, tiltA, tiltB, width, -angle + Math.PI + 0.8,
				orbit.color, fade * 0.85f, lineWidth, right, up);
	}

	private static void renderOrbitBead(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Vec3 center, Vec3 u, Vec3 v, double radius, double angle, int color, float fade,
			float lineWidth, Vec3 right, Vec3 up) {
		Vec3 position = ringPoint(center, u, v, radius, angle);
		int bright = Colors.withAlpha(color, Math.round(240.0f * fade));

		for (int i = 0; i < 4; i++) {
			double from = angle - i * 0.085;
			double to = from - 0.085;
			Vec3 a = ringPoint(center, u, v, radius, from);
			Vec3 b = ringPoint(center, u, v, radius, to);
			int alpha = Math.round(180.0f * fade * (1.0f - i / 4.0f));
			line(buffer, pose, cameraPos, a, b, Colors.withAlpha(color, alpha), lineWidth * 0.95f);
		}

		diamond(buffer, pose, cameraPos, position, right, up, 0.06,
				bright, lineWidth * 1.1f);
		line(buffer, pose, cameraPos, position.add(right.scale(0.030)), position.subtract(right.scale(0.030)),
				Colors.withAlpha(WHITE, Math.round(210.0f * fade)), lineWidth);
		line(buffer, pose, cameraPos, position.add(up.scale(0.030)), position.subtract(up.scale(0.030)),
				Colors.withAlpha(WHITE, Math.round(210.0f * fade)), lineWidth);
	}

	private static void ring(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos, Vec3 center,
			Vec3 u, Vec3 v, double radius, int color, float lineWidth, int segments) {
		for (int i = 0; i < segments; i++) {
			double a1 = Math.PI * 2.0 * i / segments;
			double a2 = Math.PI * 2.0 * (i + 1) / segments;
			line(buffer, pose, cameraPos, ringPoint(center, u, v, radius, a1),
					ringPoint(center, u, v, radius, a2), color, lineWidth);
		}
	}

	private static Vec3 ringPoint(Vec3 center, Vec3 u, Vec3 v, double radius, double angle) {
		return center.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
	}

	private static void diamond(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos, Vec3 center,
			Vec3 right, Vec3 up, double radius, int color, float lineWidth) {
		Vec3 east = center.add(right.scale(radius));
		Vec3 north = center.add(up.scale(radius));
		Vec3 west = center.subtract(right.scale(radius));
		Vec3 south = center.subtract(up.scale(radius));
		line(buffer, pose, cameraPos, east, north, color, lineWidth);
		line(buffer, pose, cameraPos, north, west, color, lineWidth);
		line(buffer, pose, cameraPos, west, south, color, lineWidth);
		line(buffer, pose, cameraPos, south, east, color, lineWidth);
	}

	private static void line(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Vec3 from, Vec3 to, int color, float lineWidth) {
		vertex(buffer, pose, cameraPos, from, color, lineWidth);
		vertex(buffer, pose, cameraPos, to, color, lineWidth);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos,
			Vec3 position, int color, float lineWidth) {
		buffer.addVertex(pose, (float) (position.x - cameraPos.x), (float) (position.y - cameraPos.y),
						(float) (position.z - cameraPos.z))
				.setColor(color)
				.setNormal(pose, 0.0f, 1.0f, 0.0f)
				.setLineWidth(Math.max(1.0f, lineWidth));
	}

	private static Vec3 lerp(Vec3 from, Vec3 to, float delta) {
		double t = Math.max(0.0, Math.min(1.0, delta));
		return new Vec3(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t,
				from.z + (to.z - from.z) * t);
	}

	private static boolean isNight(ClientLevel level) {
		long time = level.getDayTime() % 24000L;
		return time > 12800L && time < 23200L;
	}

	private enum Source {
		TRAIL,
		AMBIENT,
		PROJECTILE,
		HIT
	}

	private enum Shape {
		SPARK,
		MOTE,
		FIREFLY
	}

	private static final class Particle {
		private final Source source;
		private final Shape shape;
		private Vec3 previous;
		private Vec3 position;
		private Vec3 velocity;
		private final int lifetime;
		private final float size;
		private final int color;
		private final float gravity;
		private final float drag;
		private final double tail;
		private final float phase;
		private int age;

		private Particle(Source source, Shape shape, Vec3 position, Vec3 velocity, int lifetime,
				float size, int color, float gravity, float drag, double tail, float phase) {
			this.source = source;
			this.shape = shape;
			this.previous = position;
			this.position = position;
			this.velocity = velocity;
			this.lifetime = lifetime;
			this.size = size;
			this.color = color;
			this.gravity = gravity;
			this.drag = drag;
			this.tail = tail;
			this.phase = phase;
		}

		private void tick() {
			this.previous = this.position;
			this.position = this.position.add(this.velocity);
			this.velocity = this.velocity.scale(this.drag)
					.subtract(0.0, this.gravity, 0.0);
			this.age++;
		}
	}

	private record Impact(Vec3 center, Vec3 normal, long startTick, int color, int accent, int duration) {
	}

	private record WebBurst(Vec3 center, Vec3 normal, long startTick, int seed) {
	}

	private record Orbit(Entity target, long startTick, int seed, int color, int accent) {
		private Orbit(Entity target, long startTick, int seed) {
			this(target, startTick, seed, (seed & 1) == 0 ? CYAN : MAGENTA,
					(seed & 2) == 0 ? MAGENTA : VIOLET);
		}
	}
}
