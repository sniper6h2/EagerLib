package dev.eagerlib.ui;

final class Colors {
	private Colors() {}

	static float[] toHsv(int argb) {
		float r = ((argb >> 16) & 255) / 255F, g = ((argb >> 8) & 255) / 255F, b = (argb & 255) / 255F;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h = 0;
		if (d > 0) {
			if (max == r) h = ((g - b) / d) % 6;
			else if (max == g) h = (b - r) / d + 2;
			else h = (r - g) / d + 4;
			h /= 6F;
			if (h < 0) h += 1;
		}
		float s = max == 0 ? 0 : d / max;
		return new float[] {h, s, max, ((argb >>> 24) & 255) / 255F};
	}

	static int fromHsv(float h, float s, float v, float a) {
		h = ((h % 1F) + 1F) % 1F;
		float c = v * s;
		float x = c * (1 - Math.abs((h * 6) % 2 - 1));
		float m = v - c;
		float r, g, b;
		int seg = (int) (h * 6);
		switch (seg) {
			case 0 -> { r = c; g = x; b = 0; }
			case 1 -> { r = x; g = c; b = 0; }
			case 2 -> { r = 0; g = c; b = x; }
			case 3 -> { r = 0; g = x; b = c; }
			case 4 -> { r = x; g = 0; b = c; }
			default -> { r = c; g = 0; b = x; }
		}
		int ri = Math.round((r + m) * 255), gi = Math.round((g + m) * 255), bi = Math.round((b + m) * 255);
		int ai = Math.round(Math.max(0, Math.min(1, a)) * 255);
		return (ai << 24) | (ri << 16) | (gi << 8) | bi;
	}
}
