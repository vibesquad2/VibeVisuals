package dev.vibevs.client.feature;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Keeps the promoted Piglax server pinned to the very top of the multiplayer server list.
 *
 * <p>The entry is injected when {@code ServerList} loads its file, so it is present even on a
 * fresh game directory, and the list is saved afterwards to make the position persistent.
 */
public final class PinnedServer {
	public static final String NAME = "ЛУЧШИЙ СЕРВЕР МАЙНКРАФТ";
	public static final String ADDRESS = "piglax.ru";

	private PinnedServer() {
	}

	/**
	 * Ensures the promoted server exists at index 0 and removes duplicate entries for the same
	 * address from both the visible and hidden lists.
	 *
	 * @return {@code true} when the lists were changed and need to be saved
	 */
	public static boolean ensurePinned(List<ServerData> visible, List<ServerData> hidden) {
		boolean changed = false;
		ServerData pinned = null;

		for (Iterator<ServerData> iterator = visible.iterator(); iterator.hasNext();) {
			ServerData server = iterator.next();
			if (!ADDRESS.equals(server.ip)) {
				continue;
			}

			if (pinned == null) {
				pinned = server;
			} else {
				iterator.remove();
				changed = true;
			}
		}

		for (Iterator<ServerData> iterator = hidden.iterator(); iterator.hasNext();) {
			ServerData server = iterator.next();
			if (!ADDRESS.equals(server.ip)) {
				continue;
			}

			if (pinned == null) {
				pinned = server;
			}

			iterator.remove();
			changed = true;
		}

		if (pinned == null) {
			pinned = new ServerData(NAME, ADDRESS, ServerData.Type.OTHER);
			changed = true;
		}

		if (!NAME.equals(pinned.name)) {
			pinned.name = NAME;
			changed = true;
		}

		int index = visible.indexOf(pinned);
		if (index != 0) {
			if (index > 0) {
				visible.remove(index);
			}

			visible.add(0, pinned);
			changed = true;
		}

		return changed;
	}
}
