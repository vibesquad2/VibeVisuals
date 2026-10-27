package dev.vibevs.config;

import dev.vibevs.VibeVisuals;

import java.lang.reflect.Field;
import java.util.List;

/**
 * A single user-facing knob. Backed by a public field of {@link VibeConfig} so the JSON config stays
 * a plain data class while the ClickGUI can drive every option generically.
 */
public final class Setting {
	public enum Kind {
		TOGGLE,
		SLIDER,
		MODE
	}

	private static final VibeConfig DEFAULTS = new VibeConfig();

	private final String id;
	private final String name;
	private final String description;
	private final Category category;
	private final Kind kind;
	private final double min;
	private final double max;
	private final double step;
	private final double defaultValue;
	private final Field field;
	private final List<String> options;

	private Setting(String id, String name, Category category, String description, Kind kind,
			double min, double max, double step) {
		this(id, name, category, description, kind, min, max, step, List.of());
	}

	private Setting(String id, String name, Category category, String description, Kind kind,
			double min, double max, double step, List<String> options) {
		this.id = id;
		this.name = name;
		this.category = category;
		this.description = description;
		this.kind = kind;
		this.min = min;
		this.max = max;
		this.step = step;
		this.options = List.copyOf(options);
		this.field = resolve(id);
		this.defaultValue = this.field != null ? readFrom(DEFAULTS) : 0.0;
	}

	public static Setting toggle(String id, String name, Category category, String description) {
		return new Setting(id, name, category, description, Kind.TOGGLE, 0.0, 1.0, 1.0);
	}

	public static Setting slider(String id, String name, Category category, String description,
			double min, double max, double step) {
		return new Setting(id, name, category, description, Kind.SLIDER, min, max, step);
	}

	/**
	 * A fixed set of named options backed by an int field. The ClickGUI renders each option as its
	 * own button instead of an on/off pill.
	 */
	public static Setting mode(String id, String name, Category category, String description,
			String... options) {
		if (options.length < 2) {
			throw new IllegalArgumentException("Mode setting '" + id + "' needs at least two options");
		}

		return new Setting(id, name, category, description, Kind.MODE, 0.0, options.length - 1,
				1.0, List.of(options));
	}

	private static Field resolve(String id) {
		try {
			Field found = VibeConfig.class.getField(id);
			found.setAccessible(true);
			return found;
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException("VibeConfig has no field named '" + id + "'", e);
		}
	}

	// ------------------------------------------------------------------- data

	public String id() {
		return this.id;
	}

	public String name() {
		return this.name;
	}

	public String description() {
		return this.description;
	}

	public Category category() {
		return this.category;
	}

	public Kind kind() {
		return this.kind;
	}

	public double min() {
		return this.min;
	}

	public double max() {
		return this.max;
	}

	public double defaultValue() {
		return this.defaultValue;
	}

	/** Option labels for a {@link Kind#MODE} setting; empty for every other kind. */
	public List<String> options() {
		return this.options;
	}

	public String optionName(int index) {
		if (this.options.isEmpty()) {
			return String.valueOf(index);
		}

		return this.options.get(Math.max(0, Math.min(this.options.size() - 1, index)));
	}

	// ------------------------------------------------------------------ value

	public boolean asBool() {
		return value() >= 0.5;
	}

	public double value() {
		return readFrom(VibeConfig.get());
	}

	public float asFloat() {
		return (float) value();
	}

	public int asInt() {
		return (int) Math.round(value());
	}

	public void set(double newValue) {
		double clamped = Math.max(this.min, Math.min(this.max, snap(newValue)));

		try {
			// The config fields are instance fields, so writes must target the live config.
			Object target = VibeConfig.get();
			Class<?> type = this.field.getType();

			if (type == boolean.class) {
				this.field.setBoolean(target, clamped >= 0.5);
			} else if (type == float.class) {
				this.field.setFloat(target, (float) clamped);
			} else if (type == int.class) {
				this.field.setInt(target, (int) Math.round(clamped));
			} else {
				this.field.setDouble(target, clamped);
			}
		} catch (IllegalAccessException | RuntimeException e) {
			VibeVisuals.LOGGER.warn("Cannot write config field '{}'", this.id, e);
		}
	}

	public void toggle() {
		set(asBool() ? 0.0 : 1.0);
	}

	public void reset() {
		set(this.defaultValue);
	}

	public boolean isDefault() {
		return Math.abs(value() - this.defaultValue) < 1.0E-4;
	}

	private double snap(double raw) {
		if (this.step <= 0.0) {
			return raw;
		}

		return Math.round(raw / this.step) * this.step;
	}

	private double readFrom(VibeConfig config) {
		try {
			Object raw = this.field.get(config);

			if (raw instanceof Boolean value) {
				return value ? 1.0 : 0.0;
			}

			if (raw instanceof Number value) {
				return value.doubleValue();
			}

			throw new IllegalStateException("Config field '" + this.id + "' is not numeric or boolean");
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot read config field '" + this.id + "'", e);
		}
	}

	/** Percentage used by sliders, e.g. 0.25 -> 25%. */
	public String displayValue() {
		if (this.kind == Kind.TOGGLE) {
			return asBool() ? "ON" : "OFF";
		}

		if (this.kind == Kind.MODE) {
			return optionName(asInt());
		}

		if (this.max <= 4.0 && this.min >= 0.0) {
			return Math.round(value() * 100.0) + "%";
		}

		return String.valueOf(Math.round(value()));
	}
}
