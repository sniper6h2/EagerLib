package dev.eagerlib.render;

import java.util.HashMap;
import java.util.Map;

final class Corners {
	static final class Mask {
		final int size;
		final int[] fullStart;
		final int[] edgeStart;
		final float[] edgeCov;

		Mask(int size, int[] fullStart, int[] edgeStart, float[] edgeCov) {
			this.size = size;
			this.fullStart = fullStart;
			this.edgeStart = edgeStart;
			this.edgeCov = edgeCov;
		}
	}

	private static final Map<Long, Mask> CACHE = new HashMap<>();
	private static final int SS = 4;

	private Corners() {}

	static Mask fill(float r) {
		long key = Math.round(r * 8);
		return CACHE.computeIfAbsent(key, k -> build(r, -1));
	}

	static Mask ring(float r, int t) {
		long key = (Math.round(r * 8) << 8) | (t & 0xFF) | (1L << 40);
		return CACHE.computeIfAbsent(key, k -> build(r, t));
	}

	private static float coverage(float r, float px, float py) {
		int in = 0;
		for (int sy = 0; sy < SS; sy++) {
			for (int sx = 0; sx < SS; sx++) {
				float x = px + (sx + 0.5F) / SS;
				float y = py + (sy + 0.5F) / SS;
				if (x >= r || y >= r) {
					in++;
					continue;
				}
				float dx = r - x;
				float dy = r - y;
				if (dx * dx + dy * dy <= r * r) in++;
			}
		}
		return in / (float) (SS * SS);
	}

	private static Mask build(float r, int t) {
		int n = (int) Math.ceil(r);
		int[] fullStart = new int[n];
		int[] edgeStart = new int[n];
		float[] edgeCov = new float[n];
		for (int j = 0; j < n; j++) {
			float[] cov = new float[n];
			for (int k = 0; k < n; k++) {
				float c = coverage(r, k, j);
				if (t >= 0) {
					float ri = Math.max(0, r - t);
					float inner = (k - t < 0 || j - t < 0) ? 0 : (ri <= 0 ? 1 : coverage(ri, k - t, j - t));
					c = Math.max(0, c - inner);
				}
				cov[k] = c;
			}
			if (t < 0) {
				int fs = n;
				while (fs > 0 && cov[fs - 1] >= 0.999F) fs--;
				int es = fs;
				while (es > 0 && cov[es - 1] > 0.02F) es--;
				float sum = 0;
				for (int k = es; k < fs; k++) sum += cov[k];
				fullStart[j] = fs;
				edgeStart[j] = es;
				edgeCov[j] = fs > es ? sum / (fs - es) : 0;
			} else {
				int first = -1, last = -1;
				float sum = 0;
				for (int k = 0; k < n; k++) {
					if (cov[k] > 0.02F) {
						if (first < 0) first = k;
						last = k;
					}
				}
				if (first >= 0) for (int k = first; k <= last; k++) sum += cov[k];
				fullStart[j] = first < 0 ? n : last + 1;
				edgeStart[j] = first < 0 ? n : first;
				edgeCov[j] = first < 0 ? 0 : sum / (last - first + 1);
			}
		}
		return new Mask(n, fullStart, edgeStart, edgeCov);
	}
}
