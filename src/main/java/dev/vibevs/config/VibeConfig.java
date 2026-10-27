package dev.vibevs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.vibevs.VibeVisuals;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain JSON config stored in {@code .minecraft/config/vibevs.json}.
 *
 * <p>Field names are the setting ids used by {@link Setting} and therefore by the ClickGUI — rename
 * with care.
 */
public final class VibeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(VibeVisuals.MOD_ID + ".json");
	private static final long SAVE_DELAY_MS = 2_000L;

	@Nullable
	private static VibeConfig instance;

	private transient boolean dirty;
	private transient long dirtySince;

	// -------------------------------------------------------- tools: zoom etc.

	/** Ignore light levels entirely (bright caves, no blinding darkness). */
	public boolean fullbright = false;

	/** Master switch for the hold-to-zoom feature. */
	public boolean zoom = true;

	/** FOV multiplier applied while the zoom key is held. 0.25 = 4x zoom. */
	public float zoomFactor = 0.25f;

	/** Smoothly interpolate in and out of the zoom instead of snapping. */
	public boolean smoothZoom = true;

	/** Stop the camera from tilting when the player is hurt. */
	public boolean noHurtCamera = false;

	/** Ask for confirmation before leaving a world or quitting the game (anti-relog). */
	public boolean pvpSafe = true;

	// ------------------------------------------------------- overlays and hud

	/** Hide the vanilla vignette overlay. */
	public boolean hideVignette = false;

	/** Hide the red flash / tint when the player takes damage. */
	public boolean hideHurtOverlay = false;

	/** Hide the pumpkin blur overlay. */
	public boolean hidePumpkinBlur = false;

	/** Hide the spyglass scope overlay. */
	public boolean hideSpyglassOverlay = false;

	/** Replace the vanilla title screen with the custom VibeVisuals menu. */
	public boolean customMainMenu = true;

	/** Draw a custom crosshair instead of the vanilla one. */
	public boolean customCrosshair = false;

	/** Size multiplier for the custom crosshair. */
	public float crosshairSize = 1.0f;

	/** Master switch for the info overlay. */
	public boolean hudEnabled = true;

	/** Bumped when the HUD layout changes enough to replace old saved drag positions. */
	public int hudLayoutVersion = 0;

	/** Top-left brand card: the VibeVisuals name and Telegram handle. */
	public boolean hudIsland = true;

	/** Standalone effects card with icons, countdowns and drain lines. */
	public boolean hudEffects = true;

	/** Standalone keybind card, separate from the brand card. */
	public boolean hudKeybinds = true;

	/** Compact bottom-left TPS / ping / position / speed readout. */
	public boolean hudInfo = true;

	/** Four armor slots above the hotbar. */
	public boolean hudArmor = true;

	/** Item-cooldown card. */
	public boolean hudCooldown = true;

	/** Replace the vanilla scoreboard sidebar with the custom panel. */
	public boolean hudScoreboard = true;

	/** Target HUD: the entity in the crosshair with its name and health bar. */
	public boolean hudTarget = true;

	/** Show the target's armor and held items above the target card. */
	public boolean hudTargetArmor = true;

	/** Spawn the target card's own tiny hit particles (never vanilla particles). */
	public boolean hudTargetParticles = true;

	/** Colour the target health bar green -> red instead of using the theme colour. */
	public boolean hudTargetColorHp = true;

	/** Shrink the target head slightly while it is in its hurt animation. */
	public boolean hudTargetHeadShrink = true;

	// --------------------------------------------------------------- hud layout
	// Screen coordinates in GUI pixels; a negative x/y means "use the default anchor".

	public float hudIslandX = -1.0f;
	public float hudIslandY = 4.0f;
	public float hudInfoX = -1.0f;
	public float hudInfoY = -1.0f;
	public float hudCrosshairX = -1.0f;
	public float hudCrosshairY = -1.0f;
	public float hudKeybindsX = -1.0f;
	public float hudKeybindsY = 4.0f;
	public float hudEffectsX = -1.0f;
	public float hudEffectsY = -1.0f;
	public float hudTargetX = -1.0f;
	public float hudTargetY = -1.0f;
	public float hudArmorX = -1.0f;
	public float hudArmorY = -1.0f;
	public float hudCooldownX = -1.0f;
	public float hudCooldownY = -1.0f;
	public float hudScoreboardX = -1.0f;
	public float hudScoreboardY = -1.0f;

	public boolean hudShowFps = true;
	public boolean hudShowCoords = true;
	public boolean hudShowSpeed = true;
	public boolean hudShowBiome = false;
	public boolean hudShowDirection = true;
	public boolean hudShowPing = true;
	public boolean hudShowUser = true;
	public boolean hudShowTime = true;
	public boolean hudShowTps = true;

	// ------------------------------------------------------------ sky, weather

	/** Replace the vanilla biome sky colour with a custom hue-shifted one. */
	public boolean customSky = false;

	/** Hue rotation applied to the sky, in degrees (0 = vanilla colours). */
	public float skyHueShift = 0.0f;

	/** Keep a warm golden-hour gradient around the horizon at all times. */
	public boolean sunsetGlow = false;

	/** Fog distance multiplier. Lower = thicker fog. */
	public float fogDensity = 1.0f;

	/** Render vanilla rain / snow fog. */
	public boolean rainFog = true;

	/** Do not render rain, snow or the accompanying darkening. */
	public boolean noWeather = false;

	/** Render every precipitation column as snow instead of rain. */
	public boolean forceSnow = false;

	/** Star brightness multiplier. */
	public float starBrightness = 1.0f;

	// --------------------------------------------------------- colour and post

	/** Client-side night vision tint. */
	public boolean nightVision = false;

	/** Hue of the world tint overlay, in degrees. */
	public float tintHue = 210.0f;

	/** Strength of the world tint overlay in percent (0 = off). */
	public float tintStrength = 0.0f;

	/** Animated rainbow accents in the ClickGUI. */
	public boolean rainbowGui = true;

	/** Screen shake on damage and explosions. */
	public boolean cameraShake = false;

	// --------------------------------------------------- effects and particles

	/** Sparkle trail behind the player. */
	public boolean playerTrail = false;

	/** Hit-particle style: neon shockwave, resolved sparks and an orbit. */
	public static final int HIT_STYLE_BURST = 0;

	/** Hit-particle style: miniature cobwebs with tiny crawling spiders. */
	public static final int HIT_STYLE_WEB = 1;

	/** Selected hit-particle style, indexed into the ClickGUI mode setting. */
	public int hitParticleStyle = HIT_STYLE_BURST;

	/** Impact sparks at the hit point plus a short orbit around the target. */
	public boolean hitParticles = false;

	/** Ambient dust motes and night-time fireflies. */
	public boolean ambientParticles = false;

	/** Trail behind arrows, tridents and thrown items. */
	public boolean projectileTrail = false;

	public static final int PET_CAT = 0;
	public static final int PET_DOG = 1;
	public static final int PET_PUG = 2;
	public static final int PET_GHOST = 3;
	public static final int PET_FIGURE = 4;

	/** Cosmetic pet that follows the player; rendered entirely by the mod. */
	public boolean pets = false;

	/** Selected pet style: cat, dog, pug, ghost or a small figure wearing the player's skin. */
	public int petType = PET_CAT;

	/** Pet type index layout version; bumped when the order changes. */
	public int petTypeVersion = 0;

	/** Size multiplier for the cosmetic pet. */
	public float petScale = 1.0f;

	/** Movement speed multiplier for the cosmetic pet. */
	public float petSpeed = 0.7f;

	public static final int TARGET_ESP_BOX = 0;
	public static final int TARGET_ESP_ORBS = 1;
	public static final int TARGET_ESP_RING = 2;
	public static final int TARGET_ESP_GHOST = 3;
	public static final int TARGET_ESP_SKULL = 4;
	public static final int TARGET_ESP_CHAINS = 5;

	/** Coloured outline around the player or mob in the crosshair. */
	public boolean targetEsp = true;

	/** How far the crosshair reaches for Target ESP, in blocks. */
	public float targetEspRange = 32.0f;

	/** Selected Target ESP style, indexed into the ClickGUI mode setting. */
	public int targetEspStyle = TARGET_ESP_BOX;

	/** Target ESP style layout version; bumped when the style moved from toggles into a mode. */
	public int targetEspVersion = 0;

	/** Legacy Target ESP style toggle, kept only so old configs can be migrated. */
	public boolean targetEspBox = true;

	/** Legacy Target ESP style toggle, kept only so old configs can be migrated. */
	public boolean targetEspOrbs = true;

	/** Legacy Target ESP style toggle, kept only so old configs can be migrated. */
	public boolean targetEspRing = true;

	/** Legacy Target ESP style toggle, kept only so old configs can be migrated. */
	public boolean targetEspGhost = false;

	/** Legacy Target ESP style toggle, kept only so old configs can be migrated. */
	public boolean targetEspSkull = false;

	/** Legacy Target ESP style toggle, kept only so old configs can be migrated. */
	public boolean targetEspChains = false;

	// --------------------------------------------------------------- keybinds

	/** Feature id -> GLFW key code, bound with the middle mouse button in the ClickGUI. */
	public Map<String, Integer> featureKeys = new LinkedHashMap<>();

	// -------------------------------------------------------------- life cycle

	public static VibeConfig get() {
		VibeConfig local = instance;

		if (local == null) {
			local = load();
		}

		return local;
	}

	public static void set(VibeConfig config) {
		instance = config;
	}

	public static VibeConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				VibeConfig loaded = GSON.fromJson(reader, VibeConfig.class);

				if (loaded != null) {
					loaded.migrateHudLayout();
					loaded.migratePetType();
					loaded.migrateTargetEspStyle();
					instance = loaded;
					return loaded;
				}
			} catch (IOException | JsonSyntaxException e) {
				VibeVisuals.LOGGER.warn("Could not read {}, falling back to defaults", PATH, e);
			}
		}

		VibeConfig fresh = new VibeConfig();
		fresh.hudLayoutVersion = 2;
		fresh.migratePetType();
		fresh.migrateTargetEspStyle();
		instance = fresh;
		fresh.save();
		return fresh;
	}

	/** Request a debounced save. Cheap to call from GUI callbacks. */
	public void requestSave() {
		this.dirty = true;
		this.dirtySince = System.currentTimeMillis();
	}

	/**
	 * The IM-style HUD replaces the old brand/status/keybind/effects layout. Reset every drag
	 * anchor once for configs created before the replacement; later loads keep the player's
	 * custom positions.
	 */
	private void migrateHudLayout() {
		if (this.hudLayoutVersion >= 2) {
			return;
		}

		this.hudLayoutVersion = 2;
		this.hudIslandX = -1.0f;
		this.hudIslandY = 4.0f;
		this.hudInfoX = -1.0f;
		this.hudInfoY = -1.0f;
		this.hudKeybindsX = -1.0f;
		this.hudKeybindsY = 4.0f;
		this.hudEffectsX = -1.0f;
		this.hudEffectsY = -1.0f;
		this.hudTargetX = -1.0f;
		this.hudTargetY = -1.0f;
		this.hudArmorX = -1.0f;
		this.hudArmorY = -1.0f;
		this.hudCooldownX = -1.0f;
		this.hudCooldownY = -1.0f;
		this.hudScoreboardX = -1.0f;
		this.hudScoreboardY = -1.0f;
		this.hudCrosshairX = -1.0f;
		this.hudCrosshairY = -1.0f;
		requestSave();
	}

	/**
	 * Pet types used to be Cat, Dog, Ghost, Figure. Pug was inserted after Dog, so old Ghost and
	 * Figure selections move up by one.
	 */
	private void migratePetType() {
		if (this.petTypeVersion >= 1) {
			return;
		}

		if (this.petType >= 2) {
			this.petType++;
		}

		this.petTypeVersion = 1;
		requestSave();
	}

	/**
	 * Target ESP styles used to be independent checkboxes. Keep the style the player had enabled
	 * when moving to the single mode selector; fall back to the box for untouched configs.
	 */
	private void migrateTargetEspStyle() {
		if (this.targetEspVersion >= 1) {
			return;
		}

		if (this.targetEspGhost) {
			this.targetEspStyle = TARGET_ESP_GHOST;
		} else if (this.targetEspSkull) {
			this.targetEspStyle = TARGET_ESP_SKULL;
		} else if (this.targetEspChains) {
			this.targetEspStyle = TARGET_ESP_CHAINS;
		} else if (this.targetEspBox) {
			this.targetEspStyle = TARGET_ESP_BOX;
		} else if (this.targetEspOrbs) {
			this.targetEspStyle = TARGET_ESP_ORBS;
		} else if (this.targetEspRing) {
			this.targetEspStyle = TARGET_ESP_RING;
		} else {
			this.targetEspStyle = TARGET_ESP_BOX;
		}

		this.targetEspVersion = 1;
		requestSave();
	}

	/** Called once per client tick: flushes the config after the debounce window. */
	public void tick() {
		if (this.dirty && System.currentTimeMillis() - this.dirtySince >= SAVE_DELAY_MS) {
			this.dirty = false;
			save();
		}
	}

	public void save() {
		this.dirty = false;

		try {
			Files.createDirectories(PATH.getParent());

			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			VibeVisuals.LOGGER.warn("Could not save {}", PATH, e);
		}
	}
}
