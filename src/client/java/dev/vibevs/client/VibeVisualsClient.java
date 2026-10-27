package dev.vibevs.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vibevs.VibeVisuals;
import dev.vibevs.client.feature.AltManager;
import dev.vibevs.client.feature.FeatureKeys;
import dev.vibevs.client.feature.Particles;
import dev.vibevs.client.feature.Pets;
import dev.vibevs.client.feature.TargetTracker;
import dev.vibevs.client.feature.Zoom;
import dev.vibevs.client.gui.ClickGuiScreen;
import dev.vibevs.client.hud.ArmorHud;
import dev.vibevs.client.hud.CooldownHud;
import dev.vibevs.client.hud.CrosshairHud;
import dev.vibevs.client.hud.EffectsHud;
import dev.vibevs.client.hud.HudMetrics;
import dev.vibevs.client.hud.InfoHud;
import dev.vibevs.client.hud.IslandHud;
import dev.vibevs.client.hud.KeybindHud;
import dev.vibevs.client.hud.ScoreboardHud;
import dev.vibevs.client.hud.ScreenTint;
import dev.vibevs.client.hud.TargetHud;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.config.Visuals;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class VibeVisualsClient implements ClientModInitializer {
	public static final KeyMapping.Category KEY_CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "main"));

	/** Opens and closes the ClickGUI. Defaults to right shift. */
	public static final KeyMapping OPEN_GUI = new KeyMapping(
			"key." + VibeVisuals.MOD_ID + ".open_gui",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_RSHIFT,
			KEY_CATEGORY);

	/** Hold to zoom in. Defaults to C. */
	public static final KeyMapping ZOOM = new KeyMapping(
			"key." + VibeVisuals.MOD_ID + ".zoom",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_C,
			KEY_CATEGORY);

	private static final Identifier TINT_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "screen_tint");
	private static final Identifier CROSSHAIR_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "crosshair");
	private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "info_hud");
	private static final Identifier ISLAND_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "island");
	private static final Identifier EFFECTS_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "effects");
	private static final Identifier KEYBINDS_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "keybinds");
	private static final Identifier TARGET_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "target");
	private static final Identifier ARMOR_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "armor");
	private static final Identifier COOLDOWN_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "cooldown");
	private static final Identifier SCOREBOARD_ID = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "scoreboard");

	/** ClickGUI key presses arriving right after closing are swallowed instead of re-opening it. */
	private static long suppressOpenUntil;

	/** The saved offline account is applied once, after Minecraft has finished constructing. */
	private static boolean altSessionRestored;

	public static void suppressGuiOpen(long millis) {
		suppressOpenUntil = System.currentTimeMillis() + millis;
	}

	@Override
	public void onInitializeClient() {
		VibeConfig.load();

		KeyBindingHelper.registerKeyBinding(OPEN_GUI);
		KeyBindingHelper.registerKeyBinding(ZOOM);

		HudElementRegistry.addFirst(TINT_ID, new ScreenTint());
		HudElementRegistry.addLast(CROSSHAIR_ID, new CrosshairHud());
		HudElementRegistry.addLast(HUD_ID, new InfoHud());
		HudElementRegistry.addLast(ISLAND_ID, new IslandHud());
		HudElementRegistry.addLast(EFFECTS_ID, new EffectsHud());
		HudElementRegistry.addLast(KEYBINDS_ID, new KeybindHud());
		HudElementRegistry.addLast(TARGET_ID, new TargetHud());
		HudElementRegistry.addLast(ARMOR_ID, new ArmorHud());
		HudElementRegistry.addLast(COOLDOWN_ID, new CooldownHud());
		HudElementRegistry.addLast(SCOREBOARD_ID, new ScoreboardHud());

		WorldRenderEvents.AFTER_ENTITIES.register(Particles::render);
		WorldRenderEvents.AFTER_ENTITIES.register(Pets::render);

		VibeVisualsCommands.register();
		ClientTickEvents.END_CLIENT_TICK.register(VibeVisualsClient::onEndTick);

		VibeVisuals.LOGGER.info("VibeVisuals ready with {} settings", Visuals.count());
	}

	private static void onEndTick(Minecraft client) {
		if (!altSessionRestored) {
			altSessionRestored = true;
			AltManager.get().restoreSession();
		}

		VibeConfig.get().tick();
		HudMetrics.tick(client);

		while (OPEN_GUI.consumeClick()) {
			if (System.currentTimeMillis() < suppressOpenUntil) {
				continue;
			}

			if (client.screen == null) {
				client.setScreen(new ClickGuiScreen());
			}
		}

		if (client.level != null && client.player != null) {
			Zoom.tick(client);
			Particles.tick(client);
			Pets.tick(client);
			TargetTracker.tick(client);
		}

		FeatureKeys.tick(client);
	}
}
