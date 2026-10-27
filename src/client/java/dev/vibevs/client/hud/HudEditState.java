package dev.vibevs.client.hud;

/**
 * Shared state between the HUD editor and the HUD elements: while the editor is open, elements
 * draw themselves even when they are switched off so everything stays draggable.
 */
public final class HudEditState {
	private static boolean previewing;

	private HudEditState() {
	}

	public static boolean previewing() {
		return previewing;
	}

	public static void setPreviewing(boolean value) {
		previewing = value;
	}
}
