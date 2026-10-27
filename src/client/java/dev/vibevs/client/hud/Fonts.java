package dev.vibevs.client.hud;

import dev.vibevs.VibeVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

/**
 * Smooth TrueType text for the mod interface. Every string is wrapped into a component whose style
 * points at the bundled Golos Text font ({@code assets/vibevs/font/golos.json}); when those
 * resources cannot be found the vanilla pixel font is used instead.
 */
public final class Fonts {
	private static final Identifier DEFINITION = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "font/golos.json");
	private static final Identifier FONT_FILE = Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "font/golos.ttf");
	private static final Style SMOOTH = Style.EMPTY.withFont(
			new FontDescription.Resource(Identifier.fromNamespaceAndPath(VibeVisuals.MOD_ID, "golos")));

	@Nullable
	private static Boolean available;

	private Fonts() {
	}

	/** Draws the value in the smooth font, keeping the vanilla shadow. */
	public static void draw(GuiGraphics graphics, Font font, String value, int x, int y, int color) {
		graphics.drawString(font, text(value), x, y, color, true);
	}

	/** Draws the value centered on {@code centerX} in the smooth font. */
	public static void drawCentered(GuiGraphics graphics, Font font, String value, int centerX, int y, int color) {
		graphics.drawCenteredString(font, text(value), centerX, y, color);
	}

	/** Width of the value in the smooth font — matches what {@link #draw} renders. */
	public static int width(Font font, String value) {
		return font.width(text(value));
	}

	/** Shortens a value with an ASCII ellipsis so it never overflows its HUD cell. */
	public static String fit(Font font, String value, int maxWidth) {
		if (maxWidth <= 0) {
			return "";
		}

		if (width(font, value) <= maxWidth) {
			return value;
		}

		String ellipsis = "...";
		int ellipsisWidth = width(font, ellipsis);

		if (ellipsisWidth > maxWidth) {
			return "";
		}

		int end = value.length();

		while (end > 0 && width(font, value.substring(0, end)) + ellipsisWidth > maxWidth) {
			end--;
		}

		return value.substring(0, end) + ellipsis;
	}

	/** The value as a component drawn in the smooth font (falls back to the default font). */
	public static Component text(String value) {
		MutableComponent component = Component.literal(value);
		return available() ? component.withStyle(SMOOTH) : component;
	}

	private static boolean available() {
		Boolean cached = available;

		if (cached != null) {
			return cached;
		}

		boolean found = false;

		try {
			ResourceManager resources = Minecraft.getInstance().getResourceManager();
			found = resources.getResource(DEFINITION).isPresent() && resources.getResource(FONT_FILE).isPresent();
		} catch (Exception ignored) {
			// Not ready yet — fall back to the vanilla font and retry on the next call.
		}

		if (found) {
			available = Boolean.TRUE;
		}

		return found;
	}
}
