package dev.vibevs.client.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side cosmetic pets that follow the player. Everything is rendered as the mod's own
 * geometry: no entities are added to the world, no packets are sent and no vanilla particles are
 * used. The cat, dog and pug are built from shaded cuboids, the ghost gets an extra line aura, and
 * the figure is a miniature player whose body uses the local player's real skin texture.
 */
public final class Pets {
	public static final int TYPE_CAT = VibeConfig.PET_CAT;
	public static final int TYPE_DOG = VibeConfig.PET_DOG;
	public static final int TYPE_PUG = VibeConfig.PET_PUG;
	public static final int TYPE_GHOST = VibeConfig.PET_GHOST;
	public static final int TYPE_FIGURE = VibeConfig.PET_FIGURE;

	private static final int FULL_BRIGHT = 0xF000F0;
	private static final double PET_FOLLOW_DISTANCE = 1.25;
	private static final double PET_SIDE_OFFSET = 0.55;

	private static final int CAT_BODY = 0xFFE6A052;
	private static final int CAT_BELLY = 0xFFF4D6A4;
	private static final int CAT_DARK = 0xFF9A5A2C;
	private static final int CAT_EYE = 0xFF7FE7C4;
	private static final int CAT_NOSE = 0xFFFF9DB2;

	private static final int DOG_BODY = 0xFFB97848;
	private static final int DOG_BELLY = 0xFFE8C49A;
	private static final int DOG_MUZZLE = 0xFFF3D8B8;
	private static final int DOG_DARK = 0xFF75462C;
	private static final int DOG_EYE = 0xFF9BE8FF;
	private static final int DOG_TONGUE = 0xFFFF8D9F;

	private static final int PUG_BODY = 0xFFE0B477;
	private static final int PUG_LIGHT = 0xFFF2D7A8;
	private static final int PUG_DARK = 0xFF4B3A32;
	private static final int PUG_MUZZLE = 0xFF6B554B;
	private static final int PUG_EYE = 0xFF211815;
	private static final int PUG_GLINT = 0xFFF8FFE8;
	private static final int PUG_COLLAR = 0xFFCC4E58;
	private static final int PUG_TAG = 0xFFFFD36E;

	private static final int GHOST_BODY = 0xFFA8F4FF;
	private static final int GHOST_LIGHT = 0xFFD9FCFF;
	private static final int GHOST_DARK = 0xFF4FB7C9;
	private static final int GHOST_EYE = 0xFF173C55;

	private static final int FIGURE_TRIM = 0xFF54E4F4;

	@Nullable
	private static ClientLevel lastLevel;
	private static boolean active;
	private static Vec3 position = Vec3.ZERO;
	private static Vec3 previous = Vec3.ZERO;
	private static float yaw;
	private static float previousYaw;
	private static double walkPhase;

	private Pets() {
	}

	/** Called once per client tick. */
	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		LocalPlayer player = client.player;
		VibeConfig config = VibeConfig.get();

		if (level == null || player == null || !config.pets || player.isSpectator()) {
			active = false;
			lastLevel = level;
			return;
		}

		if (level != lastLevel) {
			active = false;
			lastLevel = level;
		}

		Vec3 desired = followPoint(player);

		if (!active || position.distanceToSqr(desired) > 24.0 * 24.0) {
			position = desired;
			previous = desired;
			yaw = player.getYRot();
			previousYaw = yaw;
			walkPhase = 0.0;
			active = true;
			return;
		}

		previous = position;
		previousYaw = yaw;

		float speed = Math.max(0.4f, Math.min(1.5f, config.petSpeed));
		Vec3 delta = desired.subtract(position);
		double distance = delta.length();
		double follow = (distance > 10.0 ? 0.48 : distance > 3.0 ? 0.28 : 0.18) * speed;
		position = position.add(delta.scale(follow));

		double moved = position.distanceTo(previous);
		walkPhase += moved * 11.5 * speed;

		float targetYaw = player.getYRot();
		yaw = approachAngle(yaw, targetYaw, 0.18f * speed);
	}

	/** Called from {@code WorldRenderEvents.AFTER_ENTITIES}. */
	public static void render(WorldRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		VibeConfig config = VibeConfig.get();

		if (!active || !config.pets || client.level == null || client.player == null) {
			return;
		}

		PoseStack poseStack = context.matrices();

		if (poseStack == null) {
			return;
		}

		Camera camera = client.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.position();
		float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 drawPosition = lerp(previous, position, partial);

		if (drawPosition.distanceToSqr(cameraPos) > 96.0 * 96.0) {
			return;
		}

		float drawYaw = previousYaw + wrapDegrees(yaw - previousYaw) * partial;
		float scale = Math.max(0.5f, config.petScale);
		int type = Math.max(TYPE_CAT, Math.min(TYPE_FIGURE, config.petType));
		float time = (float) (client.level.getGameTime() + partial);
		float swing = (float) Math.sin(walkPhase);

		poseStack.pushPose();
		poseStack.translate(drawPosition.x - cameraPos.x, drawPosition.y - cameraPos.y,
				drawPosition.z - cameraPos.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(-drawYaw));
		poseStack.scale(scale, scale, scale);

		MultiBufferSource consumers = context.consumers();
		VertexConsumer quads = consumers.getBuffer(RenderTypes.debugFilledBox());

		switch (type) {
			case TYPE_DOG -> renderDog(poseStack, quads, time, swing);
			case TYPE_PUG -> renderPug(poseStack, quads, time, swing);
			case TYPE_GHOST -> renderGhost(poseStack, quads, time, swing);
			case TYPE_FIGURE -> renderFigure(poseStack, consumers, client.player, time, swing);
			default -> renderCat(poseStack, quads, time, swing);
		}

		if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch(RenderTypes.debugFilledBox());
		}

		renderGhostAura(poseStack, consumers, type, time);
		renderFigureGlow(poseStack, consumers, type, time);

		poseStack.popPose();
	}

	// --------------------------------------------------------------- movement

	private static Vec3 followPoint(LocalPlayer player) {
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0, look.z);

		if (forward.lengthSqr() < 1.0E-4) {
			double radians = Math.toRadians(player.getYRot());
			forward = new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
		} else {
			forward = forward.normalize();
		}

		Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
		return player.position()
				.subtract(forward.scale(PET_FOLLOW_DISTANCE))
				.add(right.scale(PET_SIDE_OFFSET));
	}

	private static Vec3 lerp(Vec3 from, Vec3 to, float delta) {
		return new Vec3(from.x + (to.x - from.x) * delta,
				from.y + (to.y - from.y) * delta,
				from.z + (to.z - from.z) * delta);
	}

	private static float approachAngle(float from, float to, float step) {
		float difference = wrapDegrees(to - from);
		return from + difference * Math.min(1.0f, step);
	}

	private static float wrapDegrees(float degrees) {
		float wrapped = degrees % 360.0f;

		if (wrapped >= 180.0f) {
			wrapped -= 360.0f;
		}

		if (wrapped < -180.0f) {
			wrapped += 360.0f;
		}

		return wrapped;
	}

	// -------------------------------------------------------------- the pets

	private static void renderCat(PoseStack poseStack, VertexConsumer buffer, float time, float swing) {
		// Four little legs, swinging around their hips.
		catLeg(poseStack, buffer, -0.12f, -0.17f, swing * 0.55f);
		catLeg(poseStack, buffer, 0.12f, -0.17f, -swing * 0.55f);
		catLeg(poseStack, buffer, -0.12f, 0.17f, -swing * 0.55f);
		catLeg(poseStack, buffer, 0.12f, 0.17f, swing * 0.55f);

		// Body and a lighter belly patch.
		box(poseStack, buffer, -0.19f, 0.24f, -0.29f, 0.19f, 0.46f, 0.22f, CAT_BODY);
		box(poseStack, buffer, -0.12f, 0.22f, -0.24f, 0.12f, 0.29f, 0.18f, CAT_BELLY);
		box(poseStack, buffer, -0.19f, 0.38f, -0.22f, 0.19f, 0.43f, 0.16f, CAT_DARK);
		box(poseStack, buffer, -0.19f, 0.30f, -0.16f, 0.19f, 0.35f, 0.14f, CAT_DARK);

		// Head, ears, muzzle, eyes and nose.
		box(poseStack, buffer, -0.16f, 0.45f, 0.13f, 0.16f, 0.69f, 0.39f, CAT_BODY);
		box(poseStack, buffer, -0.15f, 0.67f, 0.18f, -0.055f, 0.80f, 0.32f, CAT_BODY);
		box(poseStack, buffer, 0.055f, 0.67f, 0.18f, 0.15f, 0.80f, 0.32f, CAT_BODY);
		box(poseStack, buffer, -0.15f, 0.79f, 0.19f, -0.07f, 0.82f, 0.29f, CAT_DARK);
		box(poseStack, buffer, 0.07f, 0.79f, 0.19f, 0.15f, 0.82f, 0.29f, CAT_DARK);
		box(poseStack, buffer, -0.09f, 0.47f, 0.38f, 0.09f, 0.56f, 0.44f, CAT_BELLY);
		box(poseStack, buffer, -0.035f, 0.535f, 0.435f, 0.035f, 0.57f, 0.455f, CAT_NOSE);
		box(poseStack, buffer, -0.095f, 0.60f, 0.385f, -0.045f, 0.64f, 0.405f, CAT_EYE);
		box(poseStack, buffer, 0.045f, 0.60f, 0.385f, 0.095f, 0.64f, 0.405f, CAT_EYE);
		box(poseStack, buffer, -0.15f, 0.59f, 0.31f, -0.12f, 0.66f, 0.39f, CAT_DARK);
		box(poseStack, buffer, 0.12f, 0.59f, 0.31f, 0.15f, 0.66f, 0.39f, CAT_DARK);

		// Tail: three segments curling up behind the cat.
		poseStack.pushPose();
		poseStack.translate(-0.04f, 0.40f, -0.28f);
		poseStack.mulPose(Axis.XP.rotationDegrees(-48.0f));
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time * 0.09f) * 10.0f - 12.0f));
		box(poseStack, buffer, -0.045f, 0.0f, -0.045f, 0.045f, 0.20f, 0.045f, CAT_BODY);

		poseStack.translate(0.0f, 0.18f, 0.0f);
		poseStack.mulPose(Axis.XP.rotationDegrees(28.0f));
		box(poseStack, buffer, -0.04f, 0.0f, -0.04f, 0.04f, 0.18f, 0.04f, CAT_DARK);

		poseStack.translate(0.0f, 0.16f, 0.0f);
		poseStack.mulPose(Axis.XP.rotationDegrees(30.0f));
		box(poseStack, buffer, -0.035f, 0.0f, -0.035f, 0.035f, 0.15f, 0.035f, CAT_BELLY);
		poseStack.popPose();
	}

	private static void catLeg(PoseStack poseStack, VertexConsumer buffer, float x, float z, float angle) {
		poseStack.pushPose();
		poseStack.translate(x, 0.25f, z);
		poseStack.mulPose(Axis.XP.rotation(angle));
		box(poseStack, buffer, -0.055f, -0.25f, -0.06f, 0.055f, 0.0f, 0.06f, CAT_BODY);
		box(poseStack, buffer, -0.06f, -0.25f, -0.07f, 0.06f, -0.20f, 0.07f, CAT_BELLY);
		poseStack.popPose();
	}

	private static void renderDog(PoseStack poseStack, VertexConsumer buffer, float time, float swing) {
		dogLeg(poseStack, buffer, -0.16f, -0.21f, swing * 0.60f);
		dogLeg(poseStack, buffer, 0.16f, -0.21f, -swing * 0.60f);
		dogLeg(poseStack, buffer, -0.16f, 0.20f, -swing * 0.60f);
		dogLeg(poseStack, buffer, 0.16f, 0.20f, swing * 0.60f);

		box(poseStack, buffer, -0.24f, 0.27f, -0.34f, 0.24f, 0.54f, 0.25f, DOG_BODY);
		box(poseStack, buffer, -0.16f, 0.24f, -0.28f, 0.16f, 0.34f, 0.21f, DOG_BELLY);
		box(poseStack, buffer, -0.24f, 0.43f, -0.26f, 0.24f, 0.49f, 0.20f, DOG_DARK);
		box(poseStack, buffer, -0.24f, 0.34f, -0.18f, 0.24f, 0.39f, 0.16f, DOG_MUZZLE);

		// Big square head, floppy ears and a long muzzle.
		box(poseStack, buffer, -0.20f, 0.53f, 0.13f, 0.20f, 0.81f, 0.44f, DOG_BODY);
		box(poseStack, buffer, -0.15f, 0.49f, 0.40f, 0.15f, 0.60f, 0.56f, DOG_MUZZLE);
		box(poseStack, buffer, -0.05f, 0.56f, 0.56f, 0.05f, 0.59f, 0.58f, DOG_DARK);
		box(poseStack, buffer, -0.115f, 0.68f, 0.435f, -0.055f, 0.72f, 0.455f, DOG_EYE);
		box(poseStack, buffer, 0.055f, 0.68f, 0.435f, 0.115f, 0.72f, 0.455f, DOG_EYE);
		box(poseStack, buffer, -0.08f, 0.52f, 0.555f, 0.08f, 0.56f, 0.575f, DOG_TONGUE);
		box(poseStack, buffer, -0.28f, 0.59f, 0.20f, -0.20f, 0.81f, 0.40f, DOG_DARK);
		box(poseStack, buffer, 0.20f, 0.59f, 0.20f, 0.28f, 0.81f, 0.40f, DOG_DARK);

		// Collar with a tiny cyan tag.
		box(poseStack, buffer, -0.21f, 0.48f, 0.10f, 0.21f, 0.53f, 0.44f, 0xFF35B9D0);
		box(poseStack, buffer, -0.035f, 0.42f, 0.43f, 0.035f, 0.48f, 0.47f, 0xFFC9FBFF);

		// Stubby tail, wagging.
		poseStack.pushPose();
		poseStack.translate(0.0f, 0.47f, -0.33f);
		poseStack.mulPose(Axis.XP.rotationDegrees(-62.0f));
		poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time * 0.16f) * 18.0f));
		box(poseStack, buffer, -0.055f, 0.0f, -0.055f, 0.055f, 0.22f, 0.055f, DOG_BODY);
		box(poseStack, buffer, -0.045f, 0.17f, -0.045f, 0.045f, 0.25f, 0.045f, DOG_BELLY);
		poseStack.popPose();
	}

	private static void dogLeg(PoseStack poseStack, VertexConsumer buffer, float x, float z, float angle) {
		poseStack.pushPose();
		poseStack.translate(x, 0.30f, z);
		poseStack.mulPose(Axis.XP.rotation(angle));
		box(poseStack, buffer, -0.07f, -0.30f, -0.075f, 0.07f, 0.0f, 0.075f, DOG_BODY);
		box(poseStack, buffer, -0.075f, -0.30f, -0.085f, 0.075f, -0.24f, 0.085f, DOG_BELLY);
		poseStack.popPose();
	}

	private static void renderPug(PoseStack poseStack, VertexConsumer buffer, float time, float swing) {
		// Short, stocky legs with a slower waddle than the dog.
		pugLeg(poseStack, buffer, -0.14f, -0.18f, swing * 0.42f);
		pugLeg(poseStack, buffer, 0.14f, -0.18f, -swing * 0.42f);
		pugLeg(poseStack, buffer, -0.14f, 0.18f, -swing * 0.42f);
		pugLeg(poseStack, buffer, 0.14f, 0.18f, swing * 0.42f);

		// Barrel body with a pale chest and belly.
		box(poseStack, buffer, -0.23f, 0.22f, -0.29f, 0.23f, 0.47f, 0.22f, PUG_BODY);
		box(poseStack, buffer, -0.15f, 0.20f, -0.24f, 0.15f, 0.28f, 0.18f, PUG_LIGHT);
		box(poseStack, buffer, -0.14f, 0.27f, 0.215f, 0.14f, 0.42f, 0.235f, PUG_LIGHT);

		// Big, square pug head with a flat face.
		box(poseStack, buffer, -0.23f, 0.43f, 0.12f, 0.23f, 0.70f, 0.43f, PUG_BODY);
		box(poseStack, buffer, -0.13f, 0.625f, 0.432f, 0.13f, 0.645f, 0.444f, PUG_DARK);
		box(poseStack, buffer, -0.10f, 0.588f, 0.432f, 0.10f, 0.602f, 0.444f, PUG_DARK);
		box(poseStack, buffer, -0.17f, 0.45f, 0.425f, 0.17f, 0.60f, 0.45f, PUG_DARK);

		// Flat muzzle, nose and a tiny tongue.
		box(poseStack, buffer, -0.12f, 0.45f, 0.445f, 0.12f, 0.55f, 0.52f, PUG_MUZZLE);
		box(poseStack, buffer, -0.045f, 0.515f, 0.515f, 0.045f, 0.548f, 0.54f, PUG_EYE);
		box(poseStack, buffer, -0.04f, 0.448f, 0.515f, 0.04f, 0.468f, 0.535f, 0xFFFFA0AD);

		// Large amber eyes with a bright glint, set into the dark mask.
		box(poseStack, buffer, -0.16f, 0.56f, 0.432f, -0.09f, 0.62f, 0.452f, PUG_EYE);
		box(poseStack, buffer, 0.09f, 0.56f, 0.432f, 0.16f, 0.62f, 0.452f, PUG_EYE);
		box(poseStack, buffer, -0.138f, 0.575f, 0.452f, -0.112f, 0.605f, 0.457f, 0xFFFFC65C);
		box(poseStack, buffer, 0.112f, 0.575f, 0.452f, 0.138f, 0.605f, 0.457f, 0xFFFFC65C);
		box(poseStack, buffer, -0.132f, 0.594f, 0.457f, -0.118f, 0.607f, 0.459f, PUG_GLINT);
		box(poseStack, buffer, 0.118f, 0.594f, 0.457f, 0.132f, 0.607f, 0.459f, PUG_GLINT);

		// Floppy ears and a red collar with a gold tag.
		box(poseStack, buffer, -0.29f, 0.54f, 0.17f, -0.22f, 0.68f, 0.35f, PUG_DARK);
		box(poseStack, buffer, 0.22f, 0.54f, 0.17f, 0.29f, 0.68f, 0.35f, PUG_DARK);
		box(poseStack, buffer, -0.21f, 0.40f, 0.08f, 0.21f, 0.45f, 0.435f, PUG_COLLAR);
		box(poseStack, buffer, -0.04f, 0.345f, 0.43f, 0.04f, 0.405f, 0.47f, PUG_TAG);

		// The pug's signature curled tail.
		poseStack.pushPose();
		poseStack.translate(-0.04f, 0.45f, -0.29f);
		poseStack.mulPose(Axis.XP.rotationDegrees(-45.0f));
		poseStack.mulPose(Axis.ZP.rotationDegrees(24.0f + (float) Math.sin(time * 0.09f) * 5.0f));
		box(poseStack, buffer, -0.045f, 0.0f, -0.045f, 0.045f, 0.13f, 0.045f, PUG_BODY);

		poseStack.translate(0.0f, 0.12f, 0.0f);
		poseStack.mulPose(Axis.XP.rotationDegrees(88.0f));
		box(poseStack, buffer, -0.04f, 0.0f, -0.04f, 0.04f, 0.10f, 0.04f, PUG_BODY);

		poseStack.translate(0.0f, 0.09f, 0.0f);
		poseStack.mulPose(Axis.XP.rotationDegrees(82.0f));
		box(poseStack, buffer, -0.035f, 0.0f, -0.035f, 0.035f, 0.08f, 0.035f, PUG_LIGHT);
		poseStack.popPose();
	}

	private static void pugLeg(PoseStack poseStack, VertexConsumer buffer, float x, float z, float angle) {
		poseStack.pushPose();
		poseStack.translate(x, 0.24f, z);
		poseStack.mulPose(Axis.XP.rotation(angle));
		box(poseStack, buffer, -0.065f, -0.24f, -0.07f, 0.065f, 0.0f, 0.07f, PUG_BODY);
		box(poseStack, buffer, -0.07f, -0.24f, -0.075f, 0.07f, -0.19f, 0.075f, PUG_LIGHT);
		poseStack.popPose();
	}

	private static void renderGhost(PoseStack poseStack, VertexConsumer buffer, float time, float swing) {
		float hover = 0.28f + (float) Math.sin(time * 0.07f) * 0.045f;
		poseStack.translate(0.0f, hover, 0.0f);

		// A rounded head and a tapered body; the light blocks read as a soft glow.
		box(poseStack, buffer, -0.22f, 0.57f, -0.21f, 0.22f, 0.96f, 0.23f, GHOST_BODY);
		box(poseStack, buffer, -0.16f, 0.86f, -0.13f, 0.16f, 0.99f, 0.16f, GHOST_LIGHT);
		box(poseStack, buffer, -0.19f, 0.43f, -0.18f, 0.19f, 0.60f, 0.20f, GHOST_LIGHT);
		box(poseStack, buffer, -0.16f, 0.28f, -0.14f, 0.16f, 0.46f, 0.17f, GHOST_BODY);
		box(poseStack, buffer, -0.12f, 0.17f, -0.10f, 0.12f, 0.31f, 0.13f, GHOST_LIGHT);
		box(poseStack, buffer, -0.07f, 0.09f, -0.06f, 0.07f, 0.20f, 0.09f, GHOST_BODY);

		// Eyes and a small mouth.
		box(poseStack, buffer, -0.14f, 0.75f, 0.22f, -0.055f, 0.83f, 0.245f, GHOST_EYE);
		box(poseStack, buffer, 0.055f, 0.75f, 0.22f, 0.14f, 0.83f, 0.245f, GHOST_EYE);
		box(poseStack, buffer, -0.035f, 0.65f, 0.22f, 0.035f, 0.69f, 0.24f, GHOST_DARK);

		// Two little arms that wave while the ghost drifts.
		poseStack.pushPose();
		poseStack.translate(-0.20f, 0.55f, 0.0f);
		poseStack.mulPose(Axis.ZP.rotationDegrees(20.0f + swing * 12.0f));
		box(poseStack, buffer, -0.07f, -0.16f, -0.06f, 0.04f, 0.02f, 0.06f, GHOST_BODY);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0.20f, 0.55f, 0.0f);
		poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0f - swing * 12.0f));
		box(poseStack, buffer, -0.04f, -0.16f, -0.06f, 0.07f, 0.02f, 0.06f, GHOST_BODY);
		poseStack.popPose();

		// Three wavy tail wisps.
		for (int i = 0; i < 3; i++) {
			float wave = (float) Math.sin(time * 0.11f + i * 1.7f) * 0.045f;
			float y = 0.16f - i * 0.075f;
			box(poseStack, buffer, -0.10f + i * 0.02f + wave, y - 0.08f, -0.08f,
					0.10f - i * 0.02f + wave, y, 0.08f, GHOST_LIGHT);
		}
	}

	private static void renderFigure(PoseStack poseStack, MultiBufferSource consumers,
			LocalPlayer player, float time, float swing) {
		float bob = (float) Math.sin(time * 0.07f) * 0.55f;
		float unit = 0.028f;
		Identifier texture = player.getSkin().body().texturePath();
		VertexConsumer skin = consumers.getBuffer(RenderTypes.entityTranslucent(texture));

		// Build the figure in the same 64-pixel space as the vanilla player skin, then scale it
		// down to pet size. Every body part uses the real skin texture.
		poseStack.pushPose();
		poseStack.translate(0.0f, 0.06f, 0.0f);
		poseStack.scale(unit, unit, unit);
		poseStack.translate(0.0f, bob, 0.0f);

		// Legs (right and left use their own side UVs).
		Uv legTop = uv(4, 16, 8, 20);
		Uv legBottom = uv(8, 16, 12, 20);
		Uv legRight = uv(0, 20, 4, 32);
		Uv legFront = uv(4, 20, 8, 32);
		Uv legLeft = uv(8, 20, 12, 32);
		Uv legBack = uv(12, 20, 16, 32);
		Uv leftLegTop = uv(20, 48, 24, 52);
		Uv leftLegBottom = uv(24, 48, 28, 52);
		Uv leftLegRight = uv(16, 48, 20, 60);
		Uv leftLegFront = uv(20, 48, 24, 60);
		Uv leftLegLeft = uv(24, 48, 28, 60);
		Uv leftLegBack = uv(28, 48, 32, 60);
		skinLimb(poseStack, skin, -2.0f, 12.0f, 4.0f, 12.0f, 4.0f, swing * 0.35f,
				legTop, legBottom, legRight, legFront, legLeft, legBack);
		skinLimb(poseStack, skin, 2.0f, 12.0f, 4.0f, 12.0f, 4.0f, -swing * 0.35f,
				leftLegTop, leftLegBottom, leftLegRight, leftLegFront, leftLegLeft, leftLegBack);

		// Torso.
		texturedBox(poseStack, skin, -4.0f, 12.0f, -2.0f, 4.0f, 24.0f, 2.0f,
				uv(20, 16, 28, 20), uv(28, 16, 36, 20), uv(16, 20, 20, 32),
				uv(20, 20, 28, 32), uv(28, 20, 32, 32), uv(32, 20, 40, 32));

		// Arms.
		Uv armTop = uv(44, 16, 48, 20);
		Uv armBottom = uv(48, 16, 52, 20);
		Uv armRight = uv(40, 20, 44, 32);
		Uv armFront = uv(44, 20, 48, 32);
		Uv armLeft = uv(48, 20, 52, 32);
		Uv armBack = uv(52, 20, 56, 32);
		Uv leftArmTop = uv(36, 48, 40, 52);
		Uv leftArmBottom = uv(40, 48, 44, 52);
		Uv leftArmRight = uv(32, 48, 36, 60);
		Uv leftArmFront = uv(36, 48, 40, 60);
		Uv leftArmLeft = uv(40, 48, 44, 60);
		Uv leftArmBack = uv(44, 48, 48, 60);
		skinLimb(poseStack, skin, -6.0f, 24.0f, 4.0f, 12.0f, 4.0f, -swing * 0.30f,
				armTop, armBottom, armRight, armFront, armLeft, armBack);
		skinLimb(poseStack, skin, 6.0f, 24.0f, 4.0f, 12.0f, 4.0f, swing * 0.30f,
				leftArmTop, leftArmBottom, leftArmRight, leftArmFront, leftArmLeft, leftArmBack);

		// Head with the actual face and skin.
		headWithSkin(poseStack, skin, 0.0f, 24.0f, 0.0f, 8.0f, 8.0f, 8.0f);

		poseStack.popPose();

		if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch(RenderTypes.entityTranslucent(texture));
		}

		// A floating ring under the figure.
		VertexConsumer lines = consumers.getBuffer(RenderTypes.linesTranslucent());
		float radius = 0.26f;
		float ringY = 0.015f + (float) Math.sin(time * 0.08f) * 0.01f;
		int ringColor = Colors.withAlpha(FIGURE_TRIM, 150);

		for (int i = 0; i < 24; i++) {
			double a1 = Math.PI * 2.0 * i / 24.0;
			double a2 = Math.PI * 2.0 * (i + 1) / 24.0;
			line(poseStack.last(), lines, ringColor, 1.2f,
					(float) Math.cos(a1) * radius, ringY, (float) Math.sin(a1) * radius,
					(float) Math.cos(a2) * radius, ringY, (float) Math.sin(a2) * radius);
		}

		if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch(RenderTypes.linesTranslucent());
		}
	}

	private static void skinLimb(PoseStack poseStack, VertexConsumer buffer, float centerX, float pivotY,
			float width, float height, float depth, float angle,
			Uv top, Uv bottom, Uv right, Uv front, Uv left, Uv back) {
		poseStack.pushPose();
		poseStack.translate(centerX, pivotY, 0.0f);
		poseStack.mulPose(Axis.XP.rotation(angle));
		texturedBox(poseStack, buffer, -width * 0.5f, -height, -depth * 0.5f,
				width * 0.5f, 0.0f, depth * 0.5f, top, bottom, right, front, left, back);
		poseStack.popPose();
	}

	private static void texturedBox(PoseStack poseStack, VertexConsumer buffer,
			float x0, float y0, float z0, float x1, float y1, float z1,
			Uv top, Uv bottom, Uv right, Uv front, Uv left, Uv back) {
		PoseStack.Pose pose = poseStack.last();
		skinFace(pose, buffer, Face.TOP, x0, y0, z0, x1, y1, z1,
				top.u0, top.v0, top.u1, top.v1);
		skinFace(pose, buffer, Face.BOTTOM, x0, y0, z0, x1, y1, z1,
				bottom.u0, bottom.v0, bottom.u1, bottom.v1);
		skinFace(pose, buffer, Face.RIGHT, x0, y0, z0, x1, y1, z1,
				right.u0, right.v0, right.u1, right.v1);
		skinFace(pose, buffer, Face.FRONT, x0, y0, z0, x1, y1, z1,
				front.u0, front.v0, front.u1, front.v1);
		skinFace(pose, buffer, Face.LEFT, x0, y0, z0, x1, y1, z1,
				left.u0, left.v0, left.u1, left.v1);
		skinFace(pose, buffer, Face.BACK, x0, y0, z0, x1, y1, z1,
				back.u0, back.v0, back.u1, back.v1);
	}

	private static Uv uv(float u0, float v0, float u1, float v1) {
		float unit = 1.0f / 64.0f;
		return new Uv(u0 * unit, v0 * unit, u1 * unit, v1 * unit);
	}

	private record Uv(float u0, float v0, float u1, float v1) {
	}

	// -------------------------------------------------------------- helpers

	private static void headWithSkin(PoseStack poseStack, VertexConsumer buffer, float x, float y, float z,
			float width, float height, float depth) {
		float x0 = x - width * 0.5f;
		float x1 = x + width * 0.5f;
		float y0 = y;
		float y1 = y + height;
		float z0 = z - depth * 0.5f;
		float z1 = z + depth * 0.5f;
		float u = 1.0f / 64.0f;

		// Base head UVs from the 64x64 skin: front, back, right, left, top, bottom.
		skinFace(poseStack.last(), buffer, Face.FRONT, x0, y0, z0, x1, y1, z1,
				8 * u, 8 * u, 16 * u, 16 * u);
		skinFace(poseStack.last(), buffer, Face.BACK, x0, y0, z0, x1, y1, z1,
				24 * u, 8 * u, 32 * u, 16 * u);
		skinFace(poseStack.last(), buffer, Face.RIGHT, x0, y0, z0, x1, y1, z1,
				0 * u, 8 * u, 8 * u, 16 * u);
		skinFace(poseStack.last(), buffer, Face.LEFT, x0, y0, z0, x1, y1, z1,
				16 * u, 8 * u, 24 * u, 16 * u);
		skinFace(poseStack.last(), buffer, Face.TOP, x0, y0, z0, x1, y1, z1,
				8 * u, 0 * u, 16 * u, 8 * u);
		skinFace(poseStack.last(), buffer, Face.BOTTOM, x0, y0, z0, x1, y1, z1,
				16 * u, 0 * u, 24 * u, 8 * u);
	}

	private enum Face {
		FRONT, BACK, RIGHT, LEFT, TOP, BOTTOM
	}

	private static void skinFace(PoseStack.Pose pose, VertexConsumer buffer, Face face,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float u0, float v0, float u1, float v1) {
		switch (face) {
			case FRONT -> {
				skinVertex(buffer, pose, x0, y0, z1, u0, v1, 0.0f, 0.0f, 1.0f);
				skinVertex(buffer, pose, x1, y0, z1, u1, v1, 0.0f, 0.0f, 1.0f);
				skinVertex(buffer, pose, x1, y1, z1, u1, v0, 0.0f, 0.0f, 1.0f);
				skinVertex(buffer, pose, x0, y1, z1, u0, v0, 0.0f, 0.0f, 1.0f);
			}
			case BACK -> {
				skinVertex(buffer, pose, x1, y0, z0, u0, v1, 0.0f, 0.0f, -1.0f);
				skinVertex(buffer, pose, x0, y0, z0, u1, v1, 0.0f, 0.0f, -1.0f);
				skinVertex(buffer, pose, x0, y1, z0, u1, v0, 0.0f, 0.0f, -1.0f);
				skinVertex(buffer, pose, x1, y1, z0, u0, v0, 0.0f, 0.0f, -1.0f);
			}
			case RIGHT -> {
				skinVertex(buffer, pose, x1, y0, z1, u0, v1, 1.0f, 0.0f, 0.0f);
				skinVertex(buffer, pose, x1, y0, z0, u1, v1, 1.0f, 0.0f, 0.0f);
				skinVertex(buffer, pose, x1, y1, z0, u1, v0, 1.0f, 0.0f, 0.0f);
				skinVertex(buffer, pose, x1, y1, z1, u0, v0, 1.0f, 0.0f, 0.0f);
			}
			case LEFT -> {
				skinVertex(buffer, pose, x0, y0, z0, u0, v1, -1.0f, 0.0f, 0.0f);
				skinVertex(buffer, pose, x0, y0, z1, u1, v1, -1.0f, 0.0f, 0.0f);
				skinVertex(buffer, pose, x0, y1, z1, u1, v0, -1.0f, 0.0f, 0.0f);
				skinVertex(buffer, pose, x0, y1, z0, u0, v0, -1.0f, 0.0f, 0.0f);
			}
			case TOP -> {
				skinVertex(buffer, pose, x0, y1, z1, u0, v1, 0.0f, 1.0f, 0.0f);
				skinVertex(buffer, pose, x1, y1, z1, u1, v1, 0.0f, 1.0f, 0.0f);
				skinVertex(buffer, pose, x1, y1, z0, u1, v0, 0.0f, 1.0f, 0.0f);
				skinVertex(buffer, pose, x0, y1, z0, u0, v0, 0.0f, 1.0f, 0.0f);
			}
			case BOTTOM -> {
				skinVertex(buffer, pose, x0, y0, z0, u0, v1, 0.0f, -1.0f, 0.0f);
				skinVertex(buffer, pose, x1, y0, z0, u1, v1, 0.0f, -1.0f, 0.0f);
				skinVertex(buffer, pose, x1, y0, z1, u1, v0, 0.0f, -1.0f, 0.0f);
				skinVertex(buffer, pose, x0, y0, z1, u0, v0, 0.0f, -1.0f, 0.0f);
			}
		}
	}

	private static void skinVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, float nx, float ny, float nz) {
		buffer.addVertex(pose, x, y, z)
				.setColor(0xFFFFFFFF)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(FULL_BRIGHT)
				.setNormal(nx, ny, nz);
	}

	private static void renderGhostAura(PoseStack poseStack, MultiBufferSource consumers, int type, float time) {
		if (type != TYPE_GHOST) {
			return;
		}

		VertexConsumer lines = consumers.getBuffer(RenderTypes.linesTranslucent());
		poseStack.pushPose();
		poseStack.translate(0.0f, 0.55f, 0.0f);

		for (int ring = 0; ring < 2; ring++) {
			float radius = 0.32f + ring * 0.09f;
			float y = (float) Math.sin(time * 0.08f + ring * 1.7f) * 0.045f;
			int color = Colors.withAlpha(GHOST_LIGHT, ring == 0 ? 135 : 75);

			for (int i = 0; i < 28; i++) {
				double a1 = Math.PI * 2.0 * i / 28.0;
				double a2 = Math.PI * 2.0 * (i + 1) / 28.0;
				line(poseStack.last(), lines, color, 1.1f,
						(float) Math.cos(a1) * radius, y, (float) Math.sin(a1) * radius,
						(float) Math.cos(a2) * radius, y, (float) Math.sin(a2) * radius);
			}
		}

		poseStack.popPose();

		if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch(RenderTypes.linesTranslucent());
		}
	}

	private static void renderFigureGlow(PoseStack poseStack, MultiBufferSource consumers, int type, float time) {
		if (type != TYPE_FIGURE) {
			return;
		}

		VertexConsumer lines = consumers.getBuffer(RenderTypes.linesTranslucent());
		poseStack.pushPose();
		poseStack.translate(0.0f, 0.90f, 0.0f);

		int color = Colors.withAlpha(FIGURE_TRIM, 100);
		float radius = 0.23f;

		for (int i = 0; i < 20; i++) {
			double a1 = Math.PI * 2.0 * i / 20.0;
			double a2 = Math.PI * 2.0 * (i + 1) / 20.0;
			line(poseStack.last(), lines, color, 1.0f,
					(float) Math.cos(a1) * radius, (float) Math.sin(time * 0.07f) * 0.02f,
					(float) Math.sin(a1) * radius,
					(float) Math.cos(a2) * radius, (float) Math.sin(time * 0.07f + 0.1f) * 0.02f,
					(float) Math.sin(a2) * radius);
		}

		poseStack.popPose();

		if (consumers instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch(RenderTypes.linesTranslucent());
		}
	}

	private static void line(PoseStack.Pose pose, VertexConsumer buffer, int color, float width,
			float xa, float ya, float za, float xb, float yb, float zb) {
		buffer.addVertex(pose, xa, ya, za)
				.setColor(color)
				.setNormal(pose, 0.0f, 1.0f, 0.0f)
				.setLineWidth(width);
		buffer.addVertex(pose, xb, yb, zb)
				.setColor(color)
				.setNormal(pose, 0.0f, 1.0f, 0.0f)
				.setLineWidth(width);
	}

	private static void box(PoseStack poseStack, VertexConsumer buffer, float x0, float y0, float z0,
			float x1, float y1, float z1, int color) {
		if (x0 > x1) {
			float swap = x0;
			x0 = x1;
			x1 = swap;
		}

		if (y0 > y1) {
			float swap = y0;
			y0 = y1;
			y1 = swap;
		}

		if (z0 > z1) {
			float swap = z0;
			z0 = z1;
			z1 = swap;
		}

		PoseStack.Pose pose = poseStack.last();
		int top = Colors.mix(color, 0xFFFFFFFF, 0.10f);
		int side = Colors.darken(color, 0.16f);
		int front = Colors.darken(color, 0.08f);
		int back = Colors.darken(color, 0.28f);
		int bottom = Colors.darken(color, 0.40f);

		quad(buffer, pose, top, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
		quad(buffer, pose, bottom, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		quad(buffer, pose, front, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		quad(buffer, pose, back, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
		quad(buffer, pose, side, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
		quad(buffer, pose, side, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
	}

	private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int color,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float x2, float y2, float z2, float x3, float y3, float z3) {
		buffer.addVertex(pose, x0, y0, z0).setColor(color);
		buffer.addVertex(pose, x1, y1, z1).setColor(color);
		buffer.addVertex(pose, x2, y2, z2).setColor(color);
		buffer.addVertex(pose, x3, y3, z3).setColor(color);
	}
}
