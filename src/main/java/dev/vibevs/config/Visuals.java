package dev.vibevs.config;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of every knob the ClickGUI knows about.
 *
 * <p>Settings are declared first, then grouped into {@link Feature}s: a feature is one master
 * toggle (left click) with an optional list of child settings (revealed with a right click). A
 * setting that is not claimed by any feature shows up as a standalone slider row in its category.
 */
public final class Visuals {
	private static final List<Setting> ALL = new ArrayList<>();
	private static final Map<String, Setting> BY_ID = new LinkedHashMap<>();
	private static final Map<Category, List<Setting>> BY_CATEGORY = new EnumMap<>(Category.class);
	private static final Map<Category, List<Feature>> FEATURES = new EnumMap<>(Category.class);
	private static final List<Feature> FEATURE_LIST = new ArrayList<>();
	private static final Map<String, Feature> FEATURE_BY_ID = new LinkedHashMap<>();
	private static final Set<String> CLAIMED = new LinkedHashSet<>();

	static {
		// ---------------------------------------------------------------- tools

		register(Setting.toggle("fullbright", "Fullbright", Category.TOOLS,
				"Ignore light levels — everything is lit"));
		register(Setting.toggle("zoom", "Zoom", Category.TOOLS,
				"Hold the zoom key (C) to zoom in"));
		register(Setting.slider("zoomFactor", "Zoom Amount", Category.TOOLS,
				"FOV multiplier while zooming (lower = stronger)", 0.05, 1.0, 0.05));
		register(Setting.toggle("smoothZoom", "Smooth Zoom", Category.TOOLS,
				"Interpolate the zoom instead of snapping"));
		register(Setting.toggle("noHurtCamera", "No Hurt Camera", Category.TOOLS,
				"Stop the camera tilt on damage"));
		register(Setting.toggle("pvpSafe", "PvPSafe", Category.TOOLS,
				"Ask for confirmation before leaving a world or quitting the game (anti-relog)"));

		// --------------------------------------------------------- overlay & hud

		register(Setting.toggle("hudEnabled", "HUD", Category.OVERLAY,
				"Master switch for the HUD cards, crosshair and the screen tint"));
		register(Setting.toggle("customCrosshair", "Custom Crosshair", Category.OVERLAY,
				"Draw the neon crosshair with a target-lock state"));
		register(Setting.slider("crosshairSize", "Crosshair Size", Category.OVERLAY,
				"Size multiplier for the custom crosshair", 0.5, 2.0, 0.1));
		register(Setting.toggle("hudShowFps", "FPS", Category.OVERLAY, "Show frames per second"));
		register(Setting.toggle("hudShowCoords", "Coordinates", Category.OVERLAY, "Show player coordinates"));
		register(Setting.toggle("hudShowSpeed", "Speed", Category.OVERLAY, "Show movement speed in blocks/s"));
		register(Setting.toggle("hudShowBiome", "Biome", Category.OVERLAY, "Show the current biome"));
		register(Setting.toggle("hudShowDirection", "Facing", Category.OVERLAY, "Show the compass direction"));
		register(Setting.toggle("hudShowPing", "Ping", Category.OVERLAY, "Show the server latency"));
		register(Setting.toggle("hudShowUser", "Watermark User", Category.OVERLAY,
				"Show the username chip in the watermark"));
		register(Setting.toggle("hudShowTime", "Watermark Time", Category.OVERLAY,
				"Show the local time chip in the watermark"));
		register(Setting.toggle("hudShowTps", "Watermark TPS", Category.OVERLAY,
				"Show the server tick-rate chip in the watermark"));
		register(Setting.slider("tintHue", "Tint Hue", Category.OVERLAY,
				"Hue of the screen tint, in degrees", 0.0, 360.0, 1.0));
		register(Setting.slider("tintStrength", "Tint Strength", Category.OVERLAY,
				"Opacity of the screen tint in percent (0 = off)", 0.0, 100.0, 1.0));
		register(Setting.toggle("hudIsland", "Watermark", Category.OVERLAY,
				"Top-left watermark: brand, username, FPS, time, coordinates, ping, TPS and BPS chips"));
		register(Setting.toggle("hudInfo", "Info Overlay", Category.OVERLAY,
				"Compact bottom-left TPS / ping / position / speed readout"));
		register(Setting.toggle("hudEffects", "Active Potions", Category.OVERLAY,
				"Potion rows: icon, name, countdown and drain line"));
		register(Setting.toggle("hudKeybinds", "Keybinds", Category.OVERLAY,
				"Right-side keybind rows: only enabled functions with a bound key"));
		register(Setting.toggle("hudArmor", "Armor HUD", Category.OVERLAY,
				"Four armor slots above the hotbar with durability lines"));
		register(Setting.toggle("hudCooldown", "Cooldowns", Category.OVERLAY,
				"Active item cooldowns with live timers and item icons"));
		register(Setting.toggle("hudScoreboard", "Scoreboard", Category.OVERLAY,
				"Custom scoreboard sidebar replacing the vanilla one"));
		register(Setting.toggle("hudTarget", "Target HUD", Category.OVERLAY,
				"Compact target card below the crosshair with a live health bar"));
		register(Setting.toggle("hudTargetArmor", "Target Armor", Category.OVERLAY,
				"Show the target's armor and held items above its card"));
		register(Setting.toggle("hudTargetParticles", "Target Particles", Category.OVERLAY,
				"Tiny custom particles around the target card on hits"));
		register(Setting.toggle("hudTargetColorHp", "Target HP Colours", Category.OVERLAY,
				"Blend the target health bar from green to red"));
		register(Setting.toggle("hudTargetHeadShrink", "Target Head Bump", Category.OVERLAY,
				"Shrink the target head slightly during its hurt animation"));
		register(Setting.toggle("hideVignette", "No Vignette", Category.OVERLAY,
				"Remove the dark corners and world border vignette"));
		register(Setting.toggle("hideHurtOverlay", "No Damage Tint", Category.OVERLAY,
				"Entities no longer flash red when they take damage"));
		register(Setting.toggle("hidePumpkinBlur", "No Pumpkin Blur", Category.OVERLAY,
				"Remove the pumpkin mask overlay"));
		register(Setting.toggle("hideSpyglassOverlay", "No Spyglass Scope", Category.OVERLAY,
				"Remove the spyglass vignette"));
		register(Setting.toggle("customMainMenu", "Custom Main Menu", Category.OVERLAY,
				"Replace the vanilla title screen with the VibeVisuals menu"));

		// ---------------------------------------------------------- sky & weather

		register(Setting.toggle("customSky", "Custom Sky", Category.SKY,
				"Hue-shift the sky instead of using vanilla biome colours"));
		register(Setting.slider("skyHueShift", "Sky Hue", Category.SKY,
				"Hue rotation applied to the sky, in degrees", 0.0, 360.0, 1.0));
		register(Setting.toggle("sunsetGlow", "Sunset Glow", Category.SKY,
				"Permanent warm gradient around the horizon"));
		register(Setting.toggle("noWeather", "No Weather", Category.SKY,
				"Skip rain and snow rendering entirely"));
		register(Setting.toggle("forceSnow", "Snow", Category.SKY,
				"Render every precipitation column as snow instead of rain (priority over No Weather)"));
		register(Setting.toggle("rainFog", "Rain Fog", Category.SKY,
				"Render vanilla rain fog when weather is on"));
		register(Setting.slider("fogDensity", "Fog Density", Category.SKY,
				"Fog distance multiplier (lower = thicker)", 0.1, 3.0, 0.05));
		register(Setting.slider("starBrightness", "Star Brightness", Category.SKY,
				"Multiplier for the night sky stars", 0.0, 2.0, 0.1));

		// ------------------------------------------------------------ colour & post

		register(Setting.toggle("nightVision", "Night Vision", Category.COLOR,
				"Vanilla-style client side night vision"));
		register(Setting.toggle("rainbowGui", "Rainbow GUI", Category.COLOR,
				"Animated rainbow accents in the ClickGUI and HUD"));
		register(Setting.toggle("cameraShake", "Camera Shake", Category.COLOR,
				"Extra camera roll on damage"));

		// --------------------------------------------------- effects & particles

		register(Setting.toggle("playerTrail", "Player Trail", Category.EFFECTS,
				"Sparkle trail behind the player"));
		register(Setting.toggle("hitParticles", "Hit Particles", Category.EFFECTS,
				"Neon impact burst or a miniature cobweb with crawling spiders"));
		register(Setting.mode("hitParticleStyle", "Effect Style", Category.EFFECTS,
				"Burst: shockwave and sparks. Web: mini cobwebs and tiny spiders.",
				"Burst", "Web"));
		register(Setting.toggle("ambientParticles", "Ambient Motes", Category.EFFECTS,
				"Dust motes by day, fireflies by night"));
		register(Setting.toggle("projectileTrail", "Projectile Trail", Category.EFFECTS,
				"Trail behind arrows, tridents and thrown items"));
		register(Setting.toggle("pets", "Pets", Category.EFFECTS,
				"A cosmetic pet that follows the player: cat, dog, pug, ghost or a figure wearing your skin"));
		register(Setting.mode("petType", "Pet Type", Category.EFFECTS,
				"Cat and Dog walk with you, Pug waddles, Ghost floats, Figure wears your skin.",
				"Cat", "Dog", "Pug", "Ghost", "Figure"));
		register(Setting.slider("petScale", "Pet Size", Category.EFFECTS,
				"Size multiplier for the cosmetic pet", 0.5, 1.6, 0.05));
		register(Setting.slider("petSpeed", "Pet Speed", Category.EFFECTS,
				"Movement speed multiplier for the cosmetic pet", 0.4, 1.5, 0.05));
		register(Setting.toggle("targetEsp", "Target ESP", Category.EFFECTS,
				"Custom outline effects around the player or mob you look at"));
		register(Setting.slider("targetEspRange", "ESP Range", Category.EFFECTS,
				"How far the crosshair may reach for Target ESP", 4.0, 64.0, 1.0));
		register(Setting.mode("targetEspStyle", "ESP Mode", Category.EFFECTS,
				"Choose one Target ESP style: box, ghost orbs, ring, ghost, skull or chains.",
				"Box", "Orbs", "Ring", "Ghost", "Skull", "Chains"));

		buildFeatures();
	}

	private Visuals() {
	}

	private static void register(Setting setting) {
		if (BY_ID.putIfAbsent(setting.id(), setting) != null) {
			throw new IllegalStateException("Duplicate setting id '" + setting.id() + "'");
		}

		ALL.add(setting);
		BY_CATEGORY.computeIfAbsent(setting.category(), key -> new ArrayList<>()).add(setting);
	}

	/**
	 * Declares the feature layout of every column. The first id is the master toggle, the rest are
	 * its child settings.
	 */
	private static void buildFeatures() {
		feature("fullbright");
		feature("zoom", "zoomFactor", "smoothZoom");
		feature("noHurtCamera");

		feature("hudEnabled", "customCrosshair", "crosshairSize",
				"hudShowFps", "hudShowCoords", "hudShowSpeed", "hudShowBiome", "hudShowDirection", "hudShowPing",
				"hudShowUser", "hudShowTime", "hudShowTps",
				"tintHue", "tintStrength",
				"hudIsland", "hudInfo", "hudEffects", "hudTarget", "hudKeybinds",
				"hudArmor", "hudCooldown", "hudScoreboard",
				"hudTargetArmor", "hudTargetParticles", "hudTargetColorHp", "hudTargetHeadShrink");
		feature("hideVignette");
		feature("hideHurtOverlay");
		feature("hidePumpkinBlur");
		feature("hideSpyglassOverlay");

		feature("customSky", "skyHueShift", "sunsetGlow");
		feature("noWeather");
		feature("forceSnow");
		feature("rainFog");

		feature("nightVision");
		feature("rainbowGui");
		feature("cameraShake");

		feature("playerTrail");
		feature("hitParticles", "hitParticleStyle");
		feature("ambientParticles");
		feature("projectileTrail");
		feature("pets", "petType", "petScale", "petSpeed");
		feature("targetEsp", "targetEspRange", "targetEspStyle");
	}

	private static void feature(String toggleId, String... childIds) {
		Setting toggle = require(toggleId);
		List<Setting> children = new ArrayList<>();

		for (String id : childIds) {
			children.add(require(id));
		}

		claim(toggleId);

		for (String id : childIds) {
			claim(id);
		}

		Feature feature = new Feature(toggle.id(), toggle.name(), toggle.description(),
				toggle.category(), toggle, children);
		FEATURES.computeIfAbsent(toggle.category(), key -> new ArrayList<>()).add(feature);
		FEATURE_LIST.add(feature);
		FEATURE_BY_ID.put(feature.id(), feature);
	}

	private static Setting require(String id) {
		Setting setting = BY_ID.get(id);

		if (setting == null) {
			throw new IllegalStateException("No setting registered under id '" + id + "'");
		}

		return setting;
	}

	private static void claim(String id) {
		if (!CLAIMED.add(id)) {
			throw new IllegalStateException("Setting '" + id + "' is used by more than one feature");
		}
	}

	public static List<Setting> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static List<Setting> in(Category category) {
		return Collections.unmodifiableList(BY_CATEGORY.getOrDefault(category, List.of()));
	}

	/** Features of a column, in display order. */
	public static List<Feature> features(Category category) {
		return Collections.unmodifiableList(FEATURES.getOrDefault(category, List.of()));
	}

	/** Every feature of every column, in declaration order. */
	public static List<Feature> allFeatures() {
		return Collections.unmodifiableList(FEATURE_LIST);
	}

	/** Feature by id, or {@code null} when nothing is registered under it. */
	@Nullable
	public static Feature feature(String id) {
		return FEATURE_BY_ID.get(id);
	}

	/** Setting by id (feature toggles and their children are settings), or {@code null}. */
	@Nullable
	public static Setting setting(String id) {
		return BY_ID.get(id);
	}

	/** Settings of a column that no feature claims; they render as plain rows. */
	public static List<Setting> standalone(Category category) {
		List<Setting> result = new ArrayList<>();

		for (Setting setting : in(category)) {
			if (!CLAIMED.contains(setting.id())) {
				result.add(setting);
			}
		}

		return Collections.unmodifiableList(result);
	}

	public static int count() {
		return ALL.size();
	}
}
