package dev.eagerlib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import dev.eagerlib.core.Keys;

public final class KeyOption extends Option<Integer> {
	public static final int NONE = -1;
	public static final int MOUSE_OFFSET = 1000;

	private Runnable action = () -> {};
	private boolean wasDown;

	public KeyOption(String id, String label, int defKey) {
		super(id, label, defKey);
	}

	public KeyOption onPress(Runnable r) {
		this.action = r == null ? () -> {} : r;
		return this;
	}

	public int key() { return get(); }

	public boolean isBound() { return get() != NONE; }

	public boolean isDown() { return isBound() && Keys.isDown(get()); }

	public String keyName() { return Keys.name(get()); }

	public void poll(boolean allowed) {
		boolean down = allowed && isDown();
		if (down && !wasDown) action.run();
		wasDown = down;
	}

	@Override public JsonElement save() { return new JsonPrimitive(Keys.id(get())); }

	@Override public void load(JsonElement json) {
		if (json != null && json.isJsonPrimitive()) loadValue(Keys.fromId(json.getAsString()));
	}
}
