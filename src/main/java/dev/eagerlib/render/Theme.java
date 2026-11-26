package dev.eagerlib.render;

import dev.eagerlib.core.Settings;

public final class Theme {
	public int accent;
	public int glass;
	public float glassOpacity;
	public float highlight;
	public float border;
	public boolean shadows;
	public float shadowStrength;
	public float radius;
	public int text;
	public int textDim;
	public int textFaint;
	public float dim;
	public boolean blur;
	public boolean textShadow;

	public static Theme current() {
		Theme t = new Theme();
		t.accent = Settings.ACCENT.get() | 0xFF000000;
		t.glass = Settings.GLASS_TINT.get();
		t.glassOpacity = (float) (Settings.GLASS_OPACITY.get() / 100.0);
		t.highlight = (float) (Settings.HIGHLIGHT.get() / 100.0);
		t.border = (float) (Settings.BORDER.get() / 100.0);
		t.shadows = Settings.SHADOWS.get();
		t.shadowStrength = (float) (Settings.SHADOW_STRENGTH.get() / 100.0);
		t.radius = Settings.RADIUS.getFloat();
		t.text = Settings.TEXT_COLOR.get() | 0xFF000000;
		t.textDim = alpha(t.text & 0xFFFFFF, 0.68F);
		t.textFaint = alpha(t.text & 0xFFFFFF, 0.42F);
		t.dim = (float) (Settings.DIM.get() / 100.0);
		t.blur = Settings.BLUR.get();
		t.textShadow = Settings.TEXT_SHADOW.get();
		return t;
	}

	public static int alpha(int rgb, float a) {
		int ai = Math.max(0, Math.min(255, Math.round(a * 255)));
		return (ai << 24) | (rgb & 0xFFFFFF);
	}

	public static int scaleAlpha(int argb, float f) {
		int a = Math.max(0, Math.min(255, Math.round((argb >>> 24) * f)));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	public static int brighten(int argb, float f) {
		int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
		r = Math.min(255, Math.round(r + (255 - r) * f));
		g = Math.min(255, Math.round(g + (255 - g) * f));
		b = Math.min(255, Math.round(b + (255 - b) * f));
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	public static int mix(int a, int b, float t) {
		int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16) | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
	}

	public boolean lightText() {
		int r = (text >> 16) & 255, g = (text >> 8) & 255, b = text & 255;
		return (r * 299 + g * 587 + b * 114) / 1000 > 128;
	}

	public int onAccent() {
		int r = (accent >> 16) & 255, g = (accent >> 8) & 255, b = accent & 255;
		return (r * 299 + g * 587 + b * 114) / 1000 > 170 ? 0xFF101014 : 0xFFFFFFFF;
	}
}
