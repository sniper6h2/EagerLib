package dev.eagerlib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolOption extends Option<Boolean> {
	public BoolOption(String id, String label, boolean def) {
		super(id, label, def);
	}

	public boolean on() { return get(); }

	public void toggle() { set(!get()); }

	@Override public JsonElement save() { return new JsonPrimitive(get()); }

	@Override public void load(JsonElement json) {
		if (json != null && json.isJsonPrimitive()) loadValue(json.getAsBoolean());
	}
}
