package dev.vibevs.config;

/**
 * Groups shown as columns in the ClickGUI.
 */
public enum Category {
	SKY("Sky & Weather", 0xFF00E5FF),
	OVERLAY("Overlay & HUD", 0xFFFF2BD6),
	COLOR("Color & Post", 0xFF8A5CFF),
	EFFECTS("Effects & Particles", 0xFFB6FF00),
	TOOLS("Tools", 0xFFFFC400);

	private final String displayName;
	private final int accent;

	Category(String displayName, int accent) {
		this.displayName = displayName;
		this.accent = accent;
	}

	public String displayName() {
		return this.displayName;
	}

	/** ARGB accent used by the GUI. */
	public int accent() {
		return this.accent;
	}
}
