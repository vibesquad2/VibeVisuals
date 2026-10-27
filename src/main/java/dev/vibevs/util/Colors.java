package dev.vibevs.util;

/**
 * Tiny ARGB helper used by both the ClickGUI and the rendering hooks. Kept dependency free so it can
 * live in the shared source set.
 */
public final class Colors {
	private Colors() {
	}

	/** Opaque colour from hue (degrees), saturation and brightness in {@code 0..1}. */
	public static int hsb(float hueDegrees, float saturation, float brightness) {
		float hue = ((hueDegrees % 360.0f) + 360.0f) % 360.0f / 60.0f;
		int sector = (int) Math.floor(hue) % 6;
		float fraction = hue - (float) Math.floor(hue);
		float p = brightness * (1.0f - saturation);
		float q = brightness * (1.0f - saturation * fraction);
		float t = brightness * (1.0f - saturation * (1.0f - fraction));

		float r;
		float g;
		float b;

		switch (sector) {
			case 0 -> {
				r = brightness;
				g = t;
				b = p;
			}
			case 1 -> {
				r = q;
				g = brightness;
				b = p;
			}
			case 2 -> {
				r = p;
				g = brightness;
				b = t;
			}
			case 3 -> {
				r = p;
				g = q;
				b = brightness;
			}
			case 4 -> {
				r = t;
				g = p;
				b = brightness;
			}
			default -> {
				r = brightness;
				g = p;
				b = q;
			}
		}

		return 0xFF000000 | channel(r) << 16 | channel(g) << 8 | channel(b);
	}

	private static int channel(float value) {
		return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
	}

	/** Rotates the hue of an ARGB colour, keeping alpha, saturation and brightness. */
	public static int shiftHue(int argb, float degrees) {
		int alpha = argb >>> 24;
		float r = ((argb >> 16) & 0xFF) / 255.0f;
		float g = ((argb >> 8) & 0xFF) / 255.0f;
		float b = (argb & 0xFF) / 255.0f;

		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hue = 0.0f;

		if (delta > 1.0E-6f) {
			if (max == r) {
				hue = 60.0f * (((g - b) / delta) % 6.0f);
			} else if (max == g) {
				hue = 60.0f * ((b - r) / delta + 2.0f);
			} else {
				hue = 60.0f * ((r - g) / delta + 4.0f);
			}
		}

		float saturation = max <= 1.0E-6f ? 0.0f : delta / max;
		return (alpha << 24) | (hsb(hue + degrees, saturation, max) & 0xFFFFFF);
	}

	/** Linear blend from {@code a} (t = 0) to {@code b} (t = 1), alpha included. */
	public static int mix(int a, int b, float t) {
		float clamped = Math.max(0.0f, Math.min(1.0f, t));

		int aa = a >>> 24;
		int ba = b >>> 24;
		int ar = (a >> 16) & 0xFF;
		int br = (b >> 16) & 0xFF;
		int ag = (a >> 8) & 0xFF;
		int bg = (b >> 8) & 0xFF;
		int ab = a & 0xFF;
		int bb = b & 0xFF;

		return lerp(aa, ba, clamped) << 24
				| lerp(ar, br, clamped) << 16
				| lerp(ag, bg, clamped) << 8
				| lerp(ab, bb, clamped);
	}

	private static int lerp(int from, int to, float t) {
		return Math.round(from + (to - from) * t);
	}

	/** Replaces the alpha channel of an ARGB colour. */
	public static int withAlpha(int argb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
	}

	/** Same colour, darker, keeping alpha. */
	public static int darken(int argb, float amount) {
		float factor = Math.max(0.0f, 1.0f - amount);
		int alpha = argb >>> 24;
		int r = Math.round(((argb >> 16) & 0xFF) * factor);
		int g = Math.round(((argb >> 8) & 0xFF) * factor);
		int b = Math.round((argb & 0xFF) * factor);
		return alpha << 24 | r << 16 | g << 8 | b;
	}
}
