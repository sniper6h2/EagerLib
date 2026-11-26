package dev.eagerlib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class TextOption extends Option<String> {
	private final int maxLength;
	private String placeholder = "";

	public TextOption(String id, String label, String def, int maxLength) {
		super(id, label, def);
		this.maxLength = maxLength;
	}

	public int maxLength() { return maxLength; }

	public String placeholder() { return placeholder; }

	public TextOption placeholder(String p) { this.placeholder = p; return this; }

	@Override protected String sanitize(String v) { return v.length() > maxLength ? v.substring(0, maxLength) : v; }

	@Override public JsonElement save() { return new JsonPrimitive(get()); }

	@Override public void load(JsonElement json) {
		if (json != null && json.isJsonPrimitive()) loadValue(json.getAsString());
	}
}
