package dev.vibevs.client.feature;

/**
 * Re-entrancy guard for PvPSafe. When the player confirms leaving, the guarded action (disconnect
 * or shutdown) runs with the guard disabled so the confirmation cannot ask a second time.
 */
public final class LeaveGuard {
	private static boolean bypassing;

	private LeaveGuard() {
	}

	public static boolean isBypassing() {
		return bypassing;
	}

	public static void runConfirmed(Runnable action) {
		boolean previous = bypassing;
		bypassing = true;

		try {
			action.run();
		} finally {
			bypassing = previous;
		}
	}
}
