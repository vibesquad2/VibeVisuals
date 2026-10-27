package dev.vibevs.config;

import java.util.List;

/**
 * A ClickGUI feature: one master toggle plus the settings revealed under it.
 *
 * <p>Left click on a feature row toggles {@link #toggle()}, right click only expands or collapses
 * the {@link #settings()} so the column stays readable. Right click never changes a value.
 */
public record Feature(String id, String name, String description, Category category, Setting toggle,
		List<Setting> settings) {
	public Feature {
		settings = List.copyOf(settings);
	}
}
