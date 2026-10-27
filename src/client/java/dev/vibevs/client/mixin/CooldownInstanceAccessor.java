package dev.vibevs.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor for the package-private cooldown record nested inside {@link ItemCooldowns}. */
@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface CooldownInstanceAccessor {
	@Accessor("endTime")
	int vibevs$endTime();
}
