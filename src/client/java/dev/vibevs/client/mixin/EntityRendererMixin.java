package dev.vibevs.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.vibevs.client.feature.TargetTracker;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the selected Target ESP mode with the mod's own geometry instead of vanilla rendering: the
 * hitbox box, the up-and-down ring, the two orbiting ghost orbs, the spectral ghost, the skull or
 * the animated chains are submitted as coloured {@code LINES} nodes (no vanilla particles, no glow,
 * no outline renderer) with a slowly pulsing ghost-blue tint. The mode comes from a single ClickGUI
 * setting, so exactly one style is drawn at a time.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
	private static final int CIRCLE_SEGMENTS = 36;
	private static final int TRAIL_SEGMENTS = 4;
	private static final double ORB_RADIUS = 0.11;
	private static final double TRAIL_STEP = 0.16;

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
			at = @At("TAIL"))
	private void vibevs$captureTargetBox(Entity entity, EntityRenderState state, float partialTick,
			CallbackInfo callback) {
		TargetTracker.captureOutline(entity, state, partialTick);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At("HEAD"))
	private void vibevs$submitTargetOutline(EntityRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo callback) {
		AABB box = TargetTracker.outlineBox(state);
		VibeConfig config = VibeConfig.get();

		if (box == null || !config.targetEsp) {
			return;
		}

		int style = Math.max(VibeConfig.TARGET_ESP_BOX,
				Math.min(VibeConfig.TARGET_ESP_CHAINS, config.targetEspStyle));

		long phase = System.currentTimeMillis() % 1600L;
		float wave = phase < 800L ? phase / 800.0f : (1600L - phase) / 800.0f;
		int color = Colors.hsb(198.0f, 0.32f, 0.80f + 0.20f * wave);
		int ghostColor = Colors.hsb(186.0f, 0.26f, 0.86f + 0.14f * wave);
		int skullColor = Colors.mix(0xFFF1E8CC, 0xFFFFFFFF, wave * 0.25f);
		int chainColor = Colors.hsb(272.0f, 0.45f, 0.82f + 0.18f * wave);
		float lineWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
		double centerX = (box.minX + box.maxX) * 0.5;
		double centerZ = (box.minZ + box.maxZ) * 0.5;
		Vec3 cameraUp = new Vec3(Minecraft.getInstance().gameRenderer.getMainCamera().upVector());
		Vec3 cameraForward = new Vec3(Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector());
		Vec3 cameraRight = cameraForward.cross(cameraUp);

		if (cameraRight.lengthSqr() < 1.0E-6) {
			cameraRight = new Vec3(1.0, 0.0, 0.0);
		} else {
			cameraRight = cameraRight.normalize();
		}

		Vec3 billboardRight = cameraRight;
		Vec3 billboardUp = cameraUp;
		long animation = System.currentTimeMillis();

		collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, consumer) -> {
			if (style == VibeConfig.TARGET_ESP_BOX) {
				double x0 = box.minX;
				double y0 = box.minY;
				double z0 = box.minZ;
				double x1 = box.maxX;
				double y1 = box.maxY;
				double z1 = box.maxZ;
				vibevs$edge(pose, consumer, color, lineWidth, x0, y0, z0, x1, y0, z0);
				vibevs$edge(pose, consumer, color, lineWidth, x0, y1, z0, x1, y1, z0);
				vibevs$edge(pose, consumer, color, lineWidth, x0, y0, z1, x1, y0, z1);
				vibevs$edge(pose, consumer, color, lineWidth, x0, y1, z1, x1, y1, z1);
				vibevs$edge(pose, consumer, color, lineWidth, x0, y0, z0, x0, y1, z0);
				vibevs$edge(pose, consumer, color, lineWidth, x0, y0, z1, x0, y1, z1);
				vibevs$edge(pose, consumer, color, lineWidth, x1, y0, z0, x1, y1, z0);
				vibevs$edge(pose, consumer, color, lineWidth, x1, y0, z1, x1, y1, z1);
				vibevs$edge(pose, consumer, color, lineWidth, x0, y0, z0, x0, y0, z1);
				vibevs$edge(pose, consumer, color, lineWidth, x0, y1, z0, x0, y1, z1);
				vibevs$edge(pose, consumer, color, lineWidth, x1, y0, z0, x1, y0, z1);
				vibevs$edge(pose, consumer, color, lineWidth, x1, y1, z0, x1, y1, z1);
			}

			if (style == VibeConfig.TARGET_ESP_RING) {
				double radius = Math.max(box.getXsize(), box.getZsize()) * 0.5 + 0.30;
				double ringY = box.minY + 0.12
						+ TargetTracker.ringWave() * Math.max(0.1, box.getYsize() - 0.24);
				vibevs$circleXZ(pose, consumer, color, lineWidth, centerX, ringY, centerZ, radius);
				vibevs$circleXZ(pose, consumer, Colors.withAlpha(color, 70), lineWidth,
						centerX, ringY, centerZ, radius + 0.05);
			}

			if (style == VibeConfig.TARGET_ESP_ORBS) {
				double orbitRadius = Math.max(box.getXsize(), box.getZsize()) * 0.5 + 0.55;
				double baseY = box.minY + box.getYsize() * 0.55;
				double angle = TargetTracker.orbitAngle();

				for (int i = 0; i < 2; i++) {
					vibevs$orb(pose, consumer, color, lineWidth, centerX, baseY, centerZ, orbitRadius,
							angle + i * Math.PI);
				}
			}

			if (style == VibeConfig.TARGET_ESP_GHOST) {
				vibevs$ghost(pose, consumer, box, billboardRight, billboardUp, ghostColor, lineWidth, animation);
			}

			if (style == VibeConfig.TARGET_ESP_SKULL) {
				vibevs$skull(pose, consumer, box, billboardRight, billboardUp, skullColor, lineWidth, animation);
			}

			if (style == VibeConfig.TARGET_ESP_CHAINS) {
				vibevs$chains(pose, consumer, box, chainColor, lineWidth, animation);
			}
		});
	}

	/**
	 * A spectral ghost silhouette floating above the target: a wobbly head, two bright eyes, a
	 * scalloped tail and a few fading wisps. Drawn on a camera-facing billboard so it reads from
	 * every angle.
	 */
	private static void vibevs$ghost(PoseStack.Pose pose, VertexConsumer consumer, AABB box,
			Vec3 right, Vec3 up, int color, float lineWidth, long now) {
		double size = Math.max(0.34, Math.min(0.62,
				Math.max(box.getXsize(), box.getZsize()) * 0.90));
		double centerX = (box.minX + box.maxX) * 0.5;
		double centerZ = (box.minZ + box.maxZ) * 0.5;
		double bob = Math.sin(now * 0.0031) * 0.055;
		double centerY = box.maxY + 0.32 + size * 0.55 + bob;
		int bright = Colors.hsb(184.0f, 0.18f, 1.0f);
		int faint = Colors.withAlpha(color, 62);
		double headY = centerY + size * 0.32;
		double headRadiusX = size * 0.42;
		double headRadiusY = size * 0.47;

		// Wobbly head outline.
		vibevs$billboardEllipse(pose, consumer, bright, lineWidth, centerX, headY, centerZ,
				right, up, headRadiusX, headRadiusY, 28);
		vibevs$billboardEllipse(pose, consumer, faint, lineWidth, centerX, headY, centerZ,
				right, up, headRadiusX + size * 0.07, headRadiusY + size * 0.07, 24);

		// Two sides and the scalloped lower edge of the ghost body.
		double bodyBottom = centerY - size * 0.78;
		double bottomOffset = bodyBottom - headY;

		vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, headY, centerZ, right, up,
				-size * 0.34, -size * 0.35, -size * 0.39, bottomOffset);
		vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, headY, centerZ, right, up,
				size * 0.34, -size * 0.35, size * 0.39, bottomOffset);

		for (int i = 0; i < 24; i++) {
			double t1 = i / 24.0;
			double t2 = (i + 1) / 24.0;
			double u1 = -size * 0.39 + t1 * size * 0.78;
			double u2 = -size * 0.39 + t2 * size * 0.78;
			double v1 = bottomOffset + Math.sin(t1 * Math.PI * 3.0 + now * 0.002) * size * 0.055;
			double v2 = bottomOffset + Math.sin(t2 * Math.PI * 3.0 + now * 0.002) * size * 0.055;
			vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, headY, centerZ, right, up,
					u1, v1, u2, v2);
		}

		// Two bright eyes and a tiny mouth.
		vibevs$billboardEllipse(pose, consumer, bright, lineWidth, centerX - size * 0.17,
				headY + size * 0.06, centerZ, right, up, size * 0.085, size * 0.115, 10);
		vibevs$billboardEllipse(pose, consumer, bright, lineWidth, centerX + size * 0.17,
				headY + size * 0.06, centerZ, right, up, size * 0.085, size * 0.115, 10);
		vibevs$billboardEdge(pose, consumer, bright, lineWidth, centerX, headY, centerZ, right, up,
				-size * 0.045, -size * 0.16, size * 0.045, -size * 0.16);

		// Three fading wisps below the ghost.
		for (int i = 0; i < 3; i++) {
			double x = (i - 1) * size * 0.18;
			double length = size * (0.34 + i * 0.06);
			vibevs$billboardEdge(pose, consumer, Colors.withAlpha(color, 80 - i * 18), lineWidth,
					centerX, headY, centerZ, right, up,
					x, -size * 0.72, x + Math.sin(now * 0.002 + i) * size * 0.10, -size * 0.72 - length);
		}

		// A ghostly ring around the target itself.
		double auraY = box.minY + box.getYsize() * 0.52;
		double auraRadius = Math.max(box.getXsize(), box.getZsize()) * 0.5 + 0.30;
		vibevs$circleXZ(pose, consumer, Colors.withAlpha(color, 58), lineWidth,
				centerX, auraY, centerZ, auraRadius);
	}

	/**
	 * A hovering skull built from billboard lines: cranium, jaw, hollow eye sockets, a triangular
	 * nose cavity and a row of teeth.
	 */
	private static void vibevs$skull(PoseStack.Pose pose, VertexConsumer consumer, AABB box,
			Vec3 right, Vec3 up, int color, float lineWidth, long now) {
		double size = Math.max(0.32, Math.min(0.58,
				Math.max(box.getXsize(), box.getZsize()) * 0.86));
		double centerX = (box.minX + box.maxX) * 0.5;
		double centerZ = (box.minZ + box.maxZ) * 0.5;
		double centerY = box.maxY + 0.30 + size * 0.52 + Math.sin(now * 0.0034) * 0.045;
		int dark = 0xD91D1713;

		// Cranium, upper jawline and rounded jaw bottom.
		vibevs$billboardEllipse(pose, consumer, color, lineWidth, centerX, centerY, centerZ,
				right, up, size * 0.44, size * 0.50, 26);
		vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, centerY, centerZ, right, up,
				-size * 0.35, -size * 0.28, -size * 0.28, -size * 0.52);
		vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, centerY, centerZ, right, up,
				size * 0.35, -size * 0.28, size * 0.28, -size * 0.52);

		for (int i = 0; i < 10; i++) {
			double t1 = i / 10.0;
			double t2 = (i + 1) / 10.0;
			vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, centerY, centerZ, right, up,
					-size * 0.28 + t1 * size * 0.56, -size * 0.52 + Math.sin(t1 * Math.PI) * size * 0.06,
					-size * 0.28 + t2 * size * 0.56, -size * 0.52 + Math.sin(t2 * Math.PI) * size * 0.06);
		}

		// Hollow eyes, with crossed lines to make them read as sockets from a distance.
		for (int sign = -1; sign <= 1; sign += 2) {
			double eyeU = sign * size * 0.17;
			double eyeV = size * 0.10;
			double eyeX = centerX + right.x * eyeU + up.x * eyeV;
			double eyeY = centerY + right.y * eyeU + up.y * eyeV;
			double eyeZ = centerZ + right.z * eyeU + up.z * eyeV;
			vibevs$billboardEllipse(pose, consumer, dark, lineWidth, eyeX, eyeY, eyeZ,
					right, up, size * 0.105, size * 0.14, 12);
			vibevs$billboardEdge(pose, consumer, dark, lineWidth, centerX, centerY, centerZ, right, up,
					eyeU - size * 0.065, eyeV - size * 0.09,
					eyeU + size * 0.065, eyeV + size * 0.09);
			vibevs$billboardEdge(pose, consumer, dark, lineWidth, centerX, centerY, centerZ, right, up,
					eyeU - size * 0.065, eyeV + size * 0.09,
					eyeU + size * 0.065, eyeV - size * 0.09);
		}

		// Nose cavity.
		vibevs$billboardEdge(pose, consumer, dark, lineWidth, centerX, centerY, centerZ, right, up,
				0.0, size * 0.01, -size * 0.065, -size * 0.14);
		vibevs$billboardEdge(pose, consumer, dark, lineWidth, centerX, centerY, centerZ, right, up,
				0.0, size * 0.01, size * 0.065, -size * 0.14);
		vibevs$billboardEdge(pose, consumer, dark, lineWidth, centerX, centerY, centerZ, right, up,
				-size * 0.065, -size * 0.14, size * 0.065, -size * 0.14);

		// Teeth: a horizontal bite line with small notches.
		vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, centerY, centerZ, right, up,
				-size * 0.25, -size * 0.36, size * 0.25, -size * 0.36);

		for (int i = -2; i <= 2; i++) {
			vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, centerY, centerZ, right, up,
					i * size * 0.08, -size * 0.36, i * size * 0.08, -size * 0.50);
		}

		// Bone-like halo fragments behind the skull.
		vibevs$billboardEdge(pose, consumer, Colors.withAlpha(color, 76), lineWidth,
				centerX, centerY, centerZ, right, up,
				-size * 0.64, size * 0.15, -size * 0.50, size * 0.35);
		vibevs$billboardEdge(pose, consumer, Colors.withAlpha(color, 76), lineWidth,
				centerX, centerY, centerZ, right, up,
				size * 0.64, size * 0.15, size * 0.50, size * 0.35);
	}

	/**
	 * Two animated chains wrapping around the target. Every other link is rotated ninety degrees,
	 * which gives the familiar alternating chain silhouette.
	 */
	private static void vibevs$chains(PoseStack.Pose pose, VertexConsumer consumer, AABB box,
			int color, float lineWidth, long now) {
		double centerX = (box.minX + box.maxX) * 0.5;
		double centerZ = (box.minZ + box.maxZ) * 0.5;
		double radius = Math.max(box.getXsize(), box.getZsize()) * 0.5 + 0.18;
		double bottom = box.minY + 0.08;
		double top = box.maxY + 0.08;
		double spin = (now % 5000L) / 5000.0 * Math.PI * 2.0;
		double linkSize = Math.max(0.035, Math.min(0.065,
				Math.max(box.getXsize(), box.getZsize()) * 0.10));
		int faint = Colors.withAlpha(color, 90);
		int bright = Colors.mix(color, 0xFFFFFFFF, 0.35f);

		for (int chain = 0; chain < 2; chain++) {
			double offset = chain * Math.PI;
			double previousX = 0.0;
			double previousY = 0.0;
			double previousZ = 0.0;

			for (int i = 0; i <= 18; i++) {
				double t = i / 18.0;
				double angle = offset + t * Math.PI * 4.0 + spin;
				double x = centerX + Math.cos(angle) * radius;
				double y = bottom + (top - bottom) * t;
				double z = centerZ + Math.sin(angle) * radius;

				if (i > 0) {
					vibevs$edge(pose, consumer, faint, lineWidth, previousX, previousY, previousZ, x, y, z);

					double midX = (previousX + x) * 0.5;
					double midY = (previousY + y) * 0.5;
					double midZ = (previousZ + z) * 0.5;
					Vec3 tangent = new Vec3(x - previousX, y - previousY, z - previousZ).normalize();
					Vec3 radial = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
					Vec3 side = tangent.cross(radial);

					if (side.lengthSqr() < 1.0E-5) {
						side = new Vec3(0.0, 1.0, 0.0);
					} else {
						side = side.normalize();
					}

					Vec3 other = (i & 1) == 0 ? radial : side;
					Vec3 along = tangent.scale(linkSize);
					Vec3 across = other.scale(linkSize * 0.58);
					double previousPointX = 0.0;
					double previousPointY = 0.0;
					double previousPointZ = 0.0;

					for (int segment = 0; segment <= 12; segment++) {
						double a = Math.PI * 2.0 * segment / 12.0;
						double px = midX + along.x * Math.cos(a) + across.x * Math.sin(a);
						double py = midY + along.y * Math.cos(a) + across.y * Math.sin(a);
						double pz = midZ + along.z * Math.cos(a) + across.z * Math.sin(a);

						if (segment > 0) {
							vibevs$edge(pose, consumer, segment % 3 == 0 ? bright : color, lineWidth,
									previousPointX, previousPointY, previousPointZ, px, py, pz);
						}

						previousPointX = px;
						previousPointY = py;
						previousPointZ = pz;
					}
				}

				previousX = x;
				previousY = y;
				previousZ = z;
			}
		}

		// A subtle spiral glow outside the chain links.
		for (int i = 0; i < 32; i++) {
			double t1 = i / 32.0;
			double t2 = (i + 1) / 32.0;
			double a1 = t1 * Math.PI * 4.0 + spin + Math.PI * 0.5;
			double a2 = t2 * Math.PI * 4.0 + spin + Math.PI * 0.5;
			vibevs$edge(pose, consumer, Colors.withAlpha(color, 45), lineWidth,
					centerX + Math.cos(a1) * (radius + 0.08), bottom + (top - bottom) * t1,
					centerZ + Math.sin(a1) * (radius + 0.08),
					centerX + Math.cos(a2) * (radius + 0.08), bottom + (top - bottom) * t2,
					centerZ + Math.sin(a2) * (radius + 0.08));
		}
	}

	/** Draws one line on a camera-facing plane around {@code center}. */
	private static void vibevs$billboardEdge(PoseStack.Pose pose, VertexConsumer consumer, int color,
			float lineWidth, double centerX, double centerY, double centerZ,
			Vec3 right, Vec3 up, double u1, double v1, double u2, double v2) {
		vibevs$edge(pose, consumer, color, lineWidth,
				centerX + right.x * u1 + up.x * v1,
				centerY + right.y * u1 + up.y * v1,
				centerZ + right.z * u1 + up.z * v1,
				centerX + right.x * u2 + up.x * v2,
				centerY + right.y * u2 + up.y * v2,
				centerZ + right.z * u2 + up.z * v2);
	}

	/** Closed ellipse on a camera-facing billboard plane. */
	private static void vibevs$billboardEllipse(PoseStack.Pose pose, VertexConsumer consumer,
			int color, float lineWidth, double centerX, double centerY, double centerZ,
			Vec3 right, Vec3 up, double radiusX, double radiusY, int segments) {
		for (int i = 0; i < segments; i++) {
			double a1 = Math.PI * 2.0 * i / segments;
			double a2 = Math.PI * 2.0 * (i + 1) / segments;
			vibevs$billboardEdge(pose, consumer, color, lineWidth, centerX, centerY, centerZ,
					right, up, Math.cos(a1) * radiusX, Math.sin(a1) * radiusY,
					Math.cos(a2) * radiusX, Math.sin(a2) * radiusY);
		}
	}

	/**
	 * One ghost orb: three orthogonal great circles read as a small glowing sphere from any angle,
	 * a bright core in the middle and a short fading trail behind it.
	 */
	private static void vibevs$orb(PoseStack.Pose pose, VertexConsumer consumer, int color,
			float lineWidth, double centerX, double baseY, double centerZ, double orbitRadius,
			double angle) {
		double x = centerX + Math.cos(angle) * orbitRadius;
		double y = baseY + Math.sin(angle * 2.0) * 0.12;
		double z = centerZ + Math.sin(angle) * orbitRadius;
		int core = Colors.hsb(196.0f, 0.18f, 1.0f);

		vibevs$circleXZ(pose, consumer, color, lineWidth, x, y, z, ORB_RADIUS);
		vibevs$circleXY(pose, consumer, color, lineWidth, x, y, z, ORB_RADIUS);
		vibevs$circleYZ(pose, consumer, color, lineWidth, x, y, z, ORB_RADIUS);

		vibevs$edge(pose, consumer, core, lineWidth, x - 0.025, y, z, x + 0.025, y, z);
		vibevs$edge(pose, consumer, core, lineWidth, x, y - 0.025, z, x, y + 0.025, z);
		vibevs$edge(pose, consumer, core, lineWidth, x, y, z - 0.025, x, y, z + 0.025);

		for (int i = 0; i < TRAIL_SEGMENTS; i++) {
			double from = angle - i * TRAIL_STEP;
			double to = from - TRAIL_STEP;
			int trail = Colors.withAlpha(color, Math.max(20, 150 - i * 34));
			vibevs$edge(pose, consumer, trail, lineWidth,
					centerX + Math.cos(from) * orbitRadius,
					baseY + Math.sin(from * 2.0) * 0.12,
					centerZ + Math.sin(from) * orbitRadius,
					centerX + Math.cos(to) * orbitRadius,
					baseY + Math.sin(to * 2.0) * 0.12,
					centerZ + Math.sin(to) * orbitRadius);
		}
	}

	/** Circle in the horizontal X/Z plane. */
	private static void vibevs$circleXZ(PoseStack.Pose pose, VertexConsumer consumer, int color,
			float lineWidth, double cx, double cy, double cz, double radius) {
		for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
			double a1 = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
			double a2 = Math.PI * 2.0 * (i + 1) / CIRCLE_SEGMENTS;
			vibevs$edge(pose, consumer, color, lineWidth,
					cx + Math.cos(a1) * radius, cy, cz + Math.sin(a1) * radius,
					cx + Math.cos(a2) * radius, cy, cz + Math.sin(a2) * radius);
		}
	}

	/** Circle in the vertical X/Y plane. */
	private static void vibevs$circleXY(PoseStack.Pose pose, VertexConsumer consumer, int color,
			float lineWidth, double cx, double cy, double cz, double radius) {
		for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
			double a1 = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
			double a2 = Math.PI * 2.0 * (i + 1) / CIRCLE_SEGMENTS;
			vibevs$edge(pose, consumer, color, lineWidth,
					cx + Math.cos(a1) * radius, cy + Math.sin(a1) * radius, cz,
					cx + Math.cos(a2) * radius, cy + Math.sin(a2) * radius, cz);
		}
	}

	/** Circle in the vertical Y/Z plane. */
	private static void vibevs$circleYZ(PoseStack.Pose pose, VertexConsumer consumer, int color,
			float lineWidth, double cx, double cy, double cz, double radius) {
		for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
			double a1 = Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
			double a2 = Math.PI * 2.0 * (i + 1) / CIRCLE_SEGMENTS;
			vibevs$edge(pose, consumer, color, lineWidth,
					cx, cy + Math.sin(a1) * radius, cz + Math.cos(a1) * radius,
					cx, cy + Math.sin(a2) * radius, cz + Math.cos(a2) * radius);
		}
	}

	/** One segment: two line vertices sharing the same colour and width. */
	private static void vibevs$edge(PoseStack.Pose pose, VertexConsumer consumer, int color,
			float lineWidth, double xa, double ya, double za, double xb, double yb, double zb) {
		consumer.addVertex(pose, (float) xa, (float) ya, (float) za)
				.setColor(color)
				.setNormal(pose, 0.0f, 1.0f, 0.0f)
				.setLineWidth(lineWidth);
		consumer.addVertex(pose, (float) xb, (float) yb, (float) zb)
				.setColor(color)
				.setNormal(pose, 0.0f, 1.0f, 0.0f)
				.setLineWidth(lineWidth);
	}
}
