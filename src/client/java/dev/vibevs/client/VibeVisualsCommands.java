package dev.vibevs.client;

import dev.vibevs.client.gui.ClickGuiScreen;
import dev.vibevs.client.gui.HudEditorScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client side chat commands. {@code /vibevs hud} opens the HUD layout editor so elements can be
 * moved straight from the chat, {@code /vibevs gui} opens the ClickGUI.
 */
public final class VibeVisualsCommands {
	private VibeVisualsCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("vibevs")
						.executes(context -> {
							context.getSource().sendFeedback(Component.literal("/vibevs hud").withStyle(ChatFormatting.YELLOW)
									.append(Component.literal(" — редактор HUD (перетаскивание элементов мышью)").withStyle(ChatFormatting.WHITE)));
							context.getSource().sendFeedback(Component.literal("/vibevs gui").withStyle(ChatFormatting.YELLOW)
									.append(Component.literal(" — открыть меню мода (то же, что Right Shift)").withStyle(ChatFormatting.WHITE)));
							return 1;
						})
						.then(ClientCommandManager.literal("hud").executes(context -> {
							Minecraft client = context.getSource().getClient();
							client.execute(() -> client.setScreen(new HudEditorScreen(null)));
							return 1;
						}))
						.then(ClientCommandManager.literal("gui").executes(context -> {
							Minecraft client = context.getSource().getClient();
							client.execute(() -> client.setScreen(new ClickGuiScreen()));
							return 1;
						}))));
	}
}
