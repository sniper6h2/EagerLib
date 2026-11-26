package dev.eagerlib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ColorOption extends Option<Integer> {
	private final boolean alpha;

	public ColorOption(String id, String label, int defArgb, boolean alpha) {
		super(id, label, alpha ? defArgb : (defArgb | 0xFF000000));
		this.alpha = alpha;
	}

	public boolean hasAlpha() { return alpha; }

	public int argb() { return get(); }

	public int rgb() { return get() & 0xFFFFFF; }

	@Override protected Integer sanitize(Integer v) { return alpha ? v : (v | 0xFF000000); }

	public String hex() {
		return alpha ? String.format("%08X", get()) : String.format("%06X", get() & 0xFFFFFF);
	}

	@Override public JsonElement save() { return new JsonPrimitive(hex()); }

	@Override public void load(JsonElement json) {
		if (json == null || !json.isJsonPrimitive()) return;
		try {
			String s = json.getAsString().trim();
			if (s.startsWith("#")) s = s.substring(1);
			long v = Long.parseLong(s, 16);
			loadValue(s.length() <= 6 ? (int) (v | 0xFF000000L) : (int) v);
		} catch (NumberFormatException ignored) {
		}
	}
}
