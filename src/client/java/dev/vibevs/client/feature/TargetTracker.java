package dev.vibevs.client.feature;

import dev.vibevs.config.VibeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The living entity under the crosshair, shared by Target ESP (hitbox box, ghost orbs and the
 * ghost ring — all drawn by the mod's own geometry, no vanilla particles) and the Target HUD. Only
 * what the player actually looks at counts: the ray stops at the first block, and the entity must
 * sit inside the vanilla attack zone.
 */
public final class TargetTracker {
	/** Milliseconds one full down-and-up sweep of the ghost ring takes. */
	private static final long RING_PERIOD = 2600L;
	/** Milliseconds one full lap of the ghost orbs around the target takes. */
	private static final long ORBIT_PERIOD = 2400L;

	@Nullable
	private static Entity target;
	/** Render state captured while the target was extracted; the custom outline is drawn for it. */
	@Nullable
	private static EntityRenderState outlineState;
	/** Target hitbox in entity space (relative to its interpolated position), slightly inflated. */
	@Nullable
	private static AABB outlineBox;

	private TargetTracker() {
	}

	@Nullable
	public static Entity target() {
		return target;
	}

	public static void tick(Minecraft client) {
		VibeConfig config = VibeConfig.get();
		boolean esp = config.targetEsp;
		boolean hud = config.hudEnabled && config.hudTarget;
		Entity found = null;

		if ((esp || hud) && client.player != null && client.level != null) {
			found = pick(client, esp ? config.targetEspRange : 32.0);

			// Attack-zone gate: ESP and HUD only ever show what the player could actually hit.
			if (found != null && !client.player.isWithinEntityInteractionRange(found, 0.0)) {
				found = null;
			}
		}

		target = found;

		if (found == null || !esp) {
			outlineState = null;
			outlineBox = null;
		}
	}

	/**
	 * Called from the entity renderer while a render state is extracted: when the entity is the
	 * tracked target, its hitbox is captured in entity space for the custom outline pass.
	 */
	public static void captureOutline(Entity entity, EntityRenderState state, float partialTick) {
		if (entity != target || !VibeConfig.get().targetEsp) {
			return;
		}

		Vec3 position = entity.getPosition(partialTick);
		outlineState = state;
		outlineBox = entity.getBoundingBox().move(-position.x, -position.y, -position.z)
				.inflate(0.04);
	}

	/** Entity-space hitbox waiting for custom drawing for this render state, or {@code null}. */
	@Nullable
	public static AABB outlineBox(EntityRenderState state) {
		return state == outlineState ? outlineBox : null;
	}

	/**
	 * 0 at the lowest point, 1 at the highest; eased so the ghost ring lingers at the ends. The
	 * renderer draws the ring from this wave, so its motion is smooth at any frame rate.
	 */
	public static double ringWave() {
		double phase = (System.currentTimeMillis() % RING_PERIOD) / (double) RING_PERIOD;
		return 0.5 - 0.5 * Math.cos(Math.PI * 2.0 * phase);
	}

	/** Leading ghost orb angle in radians, driven by wall-clock time for frame-smooth motion. */
	public static double orbitAngle() {
		return (System.currentTimeMillis() % ORBIT_PERIOD) / (double) ORBIT_PERIOD * Math.PI * 2.0;
	}

	/** Ray-traces the crosshair and returns the first living entity in front of any block. */
	@Nullable
	private static Entity pick(Minecraft client, double range) {
		Entity camera = client.getCameraEntity();

		if (camera == null) {
			return null;
		}

		Vec3 from = camera.getEyePosition();
		Vec3 look = camera.getViewVector(1.0f);
		Vec3 to = from.add(look.scale(range));
		AABB box = camera.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(camera, from, to, box,
				entity -> entity instanceof LivingEntity && entity.isPickable() && entity.isAlive()
						&& !entity.isSpectator() && entity != camera && entity != client.player,
				range * range);

		if (hit == null) {
			return null;
		}

		HitResult block = camera.pick(range, 0.0f, false);

		if (block.getType() == HitResult.Type.BLOCK
				&& block.getLocation().distanceToSqr(from) < hit.getLocation().distanceToSqr(from)) {
			return null;
		}

		return hit.getEntity();
	}
}
