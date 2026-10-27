package dev.vibevs.client.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Reads the otherwise private cooldown table so the HUD can show live item timers. */
@Mixin(ItemCooldowns.class)
public interface ItemCooldownAccessor {
	@Accessor("cooldowns")
	Map<Identifier, ?> vibevs$cooldowns();

	@Accessor("tickCount")
	int vibevs$tickCount();
}
