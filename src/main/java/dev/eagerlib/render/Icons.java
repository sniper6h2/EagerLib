package dev.eagerlib.render;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public final class Icons {
	public static final class Scaled {
		final int[] runs;
		final int res;

		Scaled(int[] runs, int res) {
			this.runs = runs;
			this.res = res;
		}
	}

	private static final int MAX_RES = 16;

	private static boolean close(int a, int b) {
		if (a == b) return true;
		if ((a >>> 24) == 0 || (b >>> 24) == 0) return false;
		for (int shift = 0; shift <= 24; shift += 8) {
			if (Math.abs(((a >>> shift) & 255) - ((b >>> shift) & 255)) > 10) return false;
		}
		return true;
	}

	public static final class Icon {
		private final Png png;
		private final Map<Integer, Scaled> cache = new HashMap<>();

		Icon(Png png) { this.png = png; }

		Scaled scaled(int size) {
			return cache.computeIfAbsent(Math.min(size, MAX_RES), this::build);
		}

		private Scaled build(int size) {
			int[] px = new int[size * size];
			double sx = png.width / (double) size, sy = png.height / (double) size;
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					int x0 = (int) Math.floor(x * sx), x1 = Math.max(x0 + 1, (int) Math.floor((x + 1) * sx));
					int y0 = (int) Math.floor(y * sy), y1 = Math.max(y0 + 1, (int) Math.floor((y + 1) * sy));
					long a = 0, r = 0, g = 0, b = 0;
					int n = 0;
					for (int yy = y0; yy < Math.min(y1, png.height); yy++) {
						for (int xx = x0; xx < Math.min(x1, png.width); xx++) {
							int c = png.argb[yy * png.width + xx];
							int ca = c >>> 24;
							a += ca;
							r += ((c >> 16) & 255) * ca;
							g += ((c >> 8) & 255) * ca;
							b += (c & 255) * ca;
							n++;
						}
					}
					if (n == 0 || a == 0) continue;
					int oa = (int) (a / n);
					px[y * size + x] = (oa << 24) | ((int) (r / a) << 16) | ((int) (g / a) << 8) | (int) (b / a);
				}
			}
			java.util.ArrayList<Integer> runs = new java.util.ArrayList<>();
			for (int y = 0; y < size; y++) {
				int x = 0;
				while (x < size) {
					int c = px[y * size + x];
					int start = x;
					while (x < size && close(px[y * size + x], c)) x++;
					if ((c >>> 24) == 0) continue;
					runs.add(start);
					runs.add(y);
					runs.add(x);
					runs.add(c);
				}
			}
			int[] arr = new int[runs.size()];
			for (int i = 0; i < arr.length; i++) arr[i] = runs.get(i);
			return new Scaled(arr, size);
		}
	}

	private static final Map<String, Optional<Icon>> ICONS = new HashMap<>();

	private Icons() {}

	public static Icon forMod(String modId, String path) {
		String key = modId + ":" + path;
		return ICONS.computeIfAbsent(key, k -> load(modId, path)).orElse(null);
	}

	private static Optional<Icon> load(String modId, String path) {
		try {
			String p = path;
			if (p == null) {
				Optional<ModContainer> mc = FabricLoader.getInstance().getModContainer(modId);
				if (mc.isEmpty()) return Optional.empty();
				Optional<String> ip = mc.get().getMetadata().getIconPath(64);
				if (ip.isEmpty()) return Optional.empty();
				p = ip.get();
			}
			Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(modId);
			Optional<Path> file = container.flatMap(c -> c.findPath(path == null ? c.getMetadata().getIconPath(64).orElse("") : path));
			if (file.isPresent() && Files.exists(file.get())) {
				try (InputStream in = Files.newInputStream(file.get())) {
					return Optional.of(new Icon(Png.read(in)));
				}
			}
			InputStream res = Icons.class.getClassLoader().getResourceAsStream(p.startsWith("/") ? p.substring(1) : p);
			if (res != null) {
				try (InputStream in = res) {
					return Optional.of(new Icon(Png.read(in)));
				}
			}
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger("EagerLib").warn("Could not load icon for {}: {}", modId, e.toString());
		}
		return Optional.empty();
	}
}
