package dev.vibevs.client.feature;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vibevs.config.Setting;
import dev.vibevs.config.VibeConfig;
import dev.vibevs.config.Visuals;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Toggles every on/off function whose key was bound with the middle mouse button in the ClickGUI:
 * both feature rows and their child toggles. Bindings live in {@code featureKeys} (setting id ->
 * GLFW key code) and fire on key press (edge detected).
 */
public final class FeatureKeys {
	/** Previous down state per key code, so a press toggles exactly once. */
	private static final Map<Integer, Boolean> DOWN = new HashMap<>();

	private FeatureKeys() {
	}

	public static void tick(Minecraft client) {
		VibeConfig config = VibeConfig.get();

		for (Map.Entry<String, Integer> entry : config.featureKeys.entrySet()) {
			int key = entry.getValue();
			boolean down = InputConstants.isKeyDown(client.getWindow(), key);
			boolean wasDown = DOWN.getOrDefault(key, false);
			DOWN.put(key, down);

			if (!down || wasDown || client.screen != null) {
				continue;
			}

			@Nullable
			Setting setting = Visuals.setting(entry.getKey());

			if (setting == null || setting.kind() != Setting.Kind.TOGGLE) {
				continue;
			}

			setting.toggle();
			config.requestSave();
			client.gui.setOverlayMessage(Component.literal(setting.name() + (setting.asBool() ? " ON" : " OFF")), false);
		}
	}
}
