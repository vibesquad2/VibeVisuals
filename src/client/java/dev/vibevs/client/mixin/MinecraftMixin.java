package dev.vibevs.client.mixin;

import dev.vibevs.VibeVisuals;
import dev.vibevs.client.feature.LeaveGuard;
import dev.vibevs.client.gui.ConfirmLeaveScreen;
import dev.vibevs.client.gui.VibeMainMenu;
import dev.vibevs.config.VibeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PvPSafe, the custom main menu and the branded window title.
 *
 * <p>Leaving a world and closing the game are both intercepted and replaced with a confirmation
 * modal while PvPSafe is on; the OS close request is cleared so Alt+F4 cannot bypass it. The vanilla
 * title screen is swapped for {@link VibeMainMenu} when the Custom Main Menu setting is enabled.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
	private void vibevs$customWindowTitle(CallbackInfoReturnable<String> callback) {
		callback.setReturnValue(VibeVisuals.WINDOW_TITLE);
	}

	@Inject(method = "disconnectFromWorld", at = @At("HEAD"), cancellable = true)
	private void vibevs$confirmWorldLeave(Component message, CallbackInfo callback) {
		Minecraft client = (Minecraft) (Object) this;

		if (!VibeConfig.get().pvpSafe || LeaveGuard.isBypassing() || client.level == null
				|| client.player == null || client.screen instanceof ConfirmLeaveScreen) {
			return;
		}

		boolean local = client.isLocalServer();
		Screen parent = client.screen;
		callback.cancel();
		client.setScreen(new ConfirmLeaveScreen(parent,
				local ? "Save and quit this world?" : "Disconnect from this server?",
				local ? "The world is saved before leaving." : "You will return to the main menu.",
				local ? "Save & Quit" : "Disconnect",
				() -> LeaveGuard.runConfirmed(() -> client.disconnectFromWorld(message))));
	}

	@Inject(method = "stop", at = @At("HEAD"), cancellable = true)
	private void vibevs$confirmQuit(CallbackInfo callback) {
		Minecraft client = (Minecraft) (Object) this;

		if (!VibeConfig.get().pvpSafe || LeaveGuard.isBypassing() || client.level == null
				|| client.player == null) {
			return;
		}

		GLFW.glfwSetWindowShouldClose(client.getWindow().handle(), false);
		callback.cancel();

		if (client.screen instanceof ConfirmLeaveScreen) {
			return;
		}

		Screen parent = client.screen;
		client.setScreen(new ConfirmLeaveScreen(parent, "Quit the game?", "Minecraft will close.", "Quit",
				() -> LeaveGuard.runConfirmed(client::stop)));
	}

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void vibevs$replaceTitleScreen(Screen screen, CallbackInfo callback) {
		if (!VibeConfig.get().customMainMenu || screen == null || screen.getClass() != TitleScreen.class) {
			return;
		}

		Minecraft client = (Minecraft) (Object) this;
		callback.cancel();
		client.setScreen(new VibeMainMenu());
	}

	@Inject(method = "getSituationalMusic", at = @At("HEAD"), cancellable = true)
	private void vibevs$menuMusic(CallbackInfoReturnable<Music> callback) {
		Minecraft client = (Minecraft) (Object) this;

		if (VibeConfig.get().customMainMenu && client.screen instanceof VibeMainMenu) {
			callback.setReturnValue(Musics.MENU);
		}
	}
}
