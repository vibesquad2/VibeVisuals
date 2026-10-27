package dev.vibevs.client.mixin;

import dev.vibevs.client.feature.Particles;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spawns the hit particle burst on the client as soon as an attack is applied locally.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Inject(method = "attack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;attack(Lnet/minecraft/world/entity/Entity;)V",
			shift = At.Shift.AFTER))
	private void vibevs$hitParticles(Player player, Entity target, CallbackInfo callback) {
		Particles.hitBurst(player, target);
	}
}
