package dev.eagerlib.api;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class TextHudElement implements HudElement {
	private final String id;
	private final String label;
	private final Supplier<String> text;
	private final String preview;
	private IntSupplier color = () -> 0xFFFFFFFF;
	private boolean enabledByDefault;
	private float x = 0.02F;
	private float y = 0.02F;

	public TextHudElement(String id, String label, Supplier<String> text, String preview) {
		this.id = id;
		this.label = label;
		this.text = text;
		this.preview = preview;
	}

	public TextHudElement color(IntSupplier c) { this.color = c; return this; }

	public TextHudElement enabled(boolean on) { this.enabledByDefault = on; return this; }

	public TextHudElement at(float fx, float fy) { this.x = fx; this.y = fy; return this; }

	@Override public String id() { return id; }

	@Override public String label() { return label; }

	@Override public String text(boolean previewMode) {
		String t = text.get();
		return (t == null || t.isEmpty()) && previewMode ? preview : t;
	}

	@Override public int color() { return color.getAsInt(); }

	@Override public boolean enabledByDefault() { return enabledByDefault; }

	@Override public float defaultX() { return x; }

	@Override public float defaultY() { return y; }
}
