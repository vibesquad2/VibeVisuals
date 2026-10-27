package dev.vibevs.client.mixin;

import dev.vibevs.config.VibeConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overlay removal (vignette, spyglass scope, pumpkin blur) and the custom crosshair hook.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
	private void vibevs$hideVignette(GuiGraphics graphics, Entity entity, CallbackInfo callback) {
		if (VibeConfig.get().hideVignette) {
			callback.cancel();
		}
	}

	@Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
	private void vibevs$hideSpyglass(GuiGraphics graphics, float scale, CallbackInfo callback) {
		if (VibeConfig.get().hideSpyglassOverlay) {
			callback.cancel();
		}
	}

	@Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void vibevs$hidePumpkin(GuiGraphics graphics, Identifier texture, float alpha, CallbackInfo callback) {
		if (VibeConfig.get().hidePumpkinBlur && texture.getPath().contains("pumpkin")) {
			callback.cancel();
		}
	}

	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void vibevs$hideCrosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo callback) {
		if (VibeConfig.get().customCrosshair) {
			callback.cancel();
		}
	}

	@Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void vibevs$hideVanillaScoreboard(GuiGraphics graphics, DeltaTracker delta, CallbackInfo callback) {
		if (VibeConfig.get().hudEnabled && VibeConfig.get().hudScoreboard) {
			callback.cancel();
		}
	}
}
