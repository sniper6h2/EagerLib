package dev.eagerlib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public final class ButtonOption extends Option<String> {
	private final Runnable action;
	private final boolean danger;

	public ButtonOption(String id, String label, String buttonText, boolean danger, Runnable action) {
		super(id, label, buttonText);
		this.action = action;
		this.danger = danger;
	}

	public void press() { action.run(); }

	public boolean danger() { return danger; }

	@Override public boolean persistent() { return false; }

	@Override public boolean isDefault() { return true; }

	@Override public JsonElement save() { return JsonNull.INSTANCE; }

	@Override public void load(JsonElement json) {}
}
