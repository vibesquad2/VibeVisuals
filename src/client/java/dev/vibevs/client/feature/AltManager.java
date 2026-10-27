package dev.vibevs.client.feature;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import dev.vibevs.VibeVisuals;
import dev.vibevs.client.mixin.MinecraftAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Local offline (cracked) account manager. Accounts are nicknames only: no Microsoft login, no
 * tokens and no password is stored. Selecting an account swaps the client {@link User}, offline
 * UUID, profile and API services for the current session.
 */
public final class AltManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(VibeVisuals.MOD_ID + "-alts.json");
	private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
	private static final String RANDOM_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
	private static final String RANDOM_FIRST = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int RANDOM_MIN = 4;
	private static final int RANDOM_MAX = 12;
	private static final int MAX_NAME_LENGTH = 16;

	@Nullable
	private static AltManager instance;

	private final List<String> names = new ArrayList<>();
	private String selected = "";

	private AltManager() {
	}

	public static AltManager get() {
		AltManager local = instance;

		if (local == null) {
			local = load();
			instance = local;
		}

		return local;
	}

	public enum AddResult {
		ADDED,
		EXISTING,
		INVALID
	}

	public List<String> accounts() {
		return List.copyOf(this.names);
	}

	public String selected() {
		return this.selected;
	}

	/**
	 * Applies the saved account on the first client tick. The initial launcher session is used when
	 * no account has been saved yet, so the first launch never changes the player's name.
	 */
	public void restoreSession() {
		Minecraft client = Minecraft.getInstance();
		String current = client.getUser().getName();
		String target = find(this.selected);

		if (target == null) {
			target = find(current);
		}

		if (target == null) {
			this.names.add(current);
			target = current;
		}

		this.selected = target;

		if (!target.equals(current)) {
			applySession(target);
		}

		save();
	}

	/**
	 * Adds a nickname and immediately switches to it. If the nickname is already saved, the
	 * existing entry is selected instead.
	 */
	public AddResult add(String rawName) {
		String name = rawName == null ? "" : rawName.trim();

		if (!VALID_NAME.matcher(name).matches()) {
			return AddResult.INVALID;
		}

		String existing = find(name);

		if (existing != null) {
			select(existing);
			return AddResult.EXISTING;
		}

		this.names.add(name);
		select(name);
		save();
		return AddResult.ADDED;
	}

	/** Switches to a saved nickname and persists the choice. */
	public boolean select(String rawName) {
		String target = find(rawName);

		if (target == null) {
			return false;
		}

		this.selected = target;
		String current = Minecraft.getInstance().getUser().getName();

		if (!target.equals(current)) {
			applySession(target);
		}

		save();
		return true;
	}

	/** Removes a saved nickname. The active account cannot be removed. */
	public boolean remove(String rawName) {
		String target = find(rawName);

		if (target == null || target.equals(this.selected)) {
			return false;
		}

		this.names.remove(target);
		save();
		return true;
	}

	/** Generates a unique offline nickname between 4 and 12 characters long. */
	public String randomName() {
		ThreadLocalRandom random = ThreadLocalRandom.current();

		for (int attempt = 0; attempt < 64; attempt++) {
			int length = random.nextInt(RANDOM_MIN, RANDOM_MAX + 1);
			StringBuilder name = new StringBuilder(length);
			name.append(RANDOM_FIRST.charAt(random.nextInt(RANDOM_FIRST.length())));

			for (int i = 1; i < length; i++) {
				name.append(RANDOM_ALPHABET.charAt(random.nextInt(RANDOM_ALPHABET.length())));
			}

			String candidate = name.toString();

			if (find(candidate) == null) {
				return candidate;
			}
		}

		String candidate;

		do {
			candidate = "Alt" + Integer.toHexString(random.nextInt(0x1000000));
		} while (find(candidate) != null);

		return candidate;
	}

	@Nullable
	private String find(@Nullable String name) {
		if (name == null || name.isBlank()) {
			return null;
		}

		for (String saved : this.names) {
			if (saved.equalsIgnoreCase(name)) {
				return saved;
			}
		}

		return null;
	}

	private static AltManager load() {
		AltManager manager = new AltManager();
		State state = null;

		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				state = GSON.fromJson(reader, State.class);
			} catch (IOException | JsonSyntaxException e) {
				VibeVisuals.LOGGER.warn("Could not read {}, starting with an empty alt list", PATH, e);
			}
		}

		if (state != null && state.names != null) {
			Set<String> seen = new LinkedHashSet<>();

			for (String raw : state.names) {
				if (raw == null) {
					continue;
				}

				String name = raw.trim();

				if (!VALID_NAME.matcher(name).matches()) {
					continue;
				}

				String key = name.toLowerCase(Locale.ROOT);

				if (seen.add(key)) {
					manager.names.add(name);
				}
			}
		}

		if (state != null && state.selected != null) {
			manager.selected = state.selected.trim();
		}

		Minecraft client = Minecraft.getInstance();
		String current = client.getUser().getName();

		if (manager.names.isEmpty()) {
			manager.names.add(current);
		}

		String target = manager.find(manager.selected);

		if (target == null) {
			target = manager.find(current);
		}

		if (target == null) {
			manager.names.add(current);
			target = current;
		}

		manager.selected = target;
		manager.save();
		return manager;
	}

	/** Replaces the live client session with an offline profile. */
	private static void applySession(String name) {
		Minecraft client = Minecraft.getInstance();
		UUID uuid = UUIDUtil.createOfflinePlayerUUID(name);
		User user = new User(name, uuid, "", Optional.empty(), Optional.empty());
		MinecraftAccessor accessor = (MinecraftAccessor) client;
		accessor.setUser(user);
		accessor.setProfileFuture(CompletableFuture.completedFuture(new ProfileResult(UUIDUtil.createOfflineProfile(name))));
		accessor.setUserApiService(UserApiService.OFFLINE);
		accessor.setUserPropertiesFuture(CompletableFuture.completedFuture(UserApiService.OFFLINE_PROPERTIES));
		accessor.setProfileKeyPairManager(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
		client.updateTitle();
		VibeVisuals.LOGGER.info("Switched to offline account '{}'", name);
	}

	private void save() {
		try {
			Files.createDirectories(PATH.getParent());
			State state = new State();
			state.names = new ArrayList<>(this.names);
			state.selected = this.selected;

			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(state, writer);
			}
		} catch (IOException e) {
			VibeVisuals.LOGGER.warn("Could not save {}", PATH, e);
		}
	}

	private static final class State {
		private List<String> names;
		private String selected;
	}
}
