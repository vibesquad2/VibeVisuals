package dev.vibevs.client.mixin;

import dev.vibevs.client.feature.PinnedServer;
import java.util.List;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the promoted Piglax server to the top of the multiplayer list right after
 * {@link ServerList#load()} reads {@code servers.dat}.
 */
@Mixin(ServerList.class)
public abstract class ServerListMixin {
	@Shadow
	@Final
	private List<ServerData> serverList;

	@Shadow
	@Final
	private List<ServerData> hiddenServerList;

	@Inject(method = "load", at = @At("RETURN"))
	private void vibevs$pinPiglax(CallbackInfo callback) {
		if (PinnedServer.ensurePinned(this.serverList, this.hiddenServerList)) {
			((ServerList) (Object) this).save();
		}
	}
}
