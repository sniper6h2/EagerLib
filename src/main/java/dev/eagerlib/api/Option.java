package dev.eagerlib.api;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

public abstract class Option<T> {
	private final String id;
	private final String label;
	private String description = "";
	private final T defaultValue;
	private T value;
	private BooleanSupplier visible = () -> true;
	private Consumer<T> listener = v -> {};
	EagerMod owner;

	protected Option(String id, String label, T defaultValue) {
		this.id = id;
		this.label = label;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String id() { return id; }
	public String label() { return label; }
	public String description() { return description; }
	public T defaultValue() { return defaultValue; }
	public EagerMod owner() { return owner; }

	public T get() { return value; }

	public void set(T newValue) {
		if (newValue == null || newValue.equals(value)) return;
		value = sanitize(newValue);
		if (owner != null) owner.markDirty();
		listener.accept(value);
	}

	protected T sanitize(T v) { return v; }

	public void reset() { set(defaultValue); }

	public boolean isDefault() { return value.equals(defaultValue); }

	public boolean persistent() { return true; }

	public boolean isVisible() { return visible.getAsBoolean(); }

	@SuppressWarnings("unchecked")
	public <O extends Option<T>> O describe(String text) {
		this.description = text == null ? "" : text;
		return (O) this;
	}

	@SuppressWarnings("unchecked")
	public <O extends Option<T>> O visibleWhen(BooleanSupplier condition) {
		this.visible = condition;
		return (O) this;
	}

	@SuppressWarnings("unchecked")
	public <O extends Option<T>> O onChange(Consumer<T> l) {
		this.listener = l;
		return (O) this;
	}

	public boolean matches(String query) {
		String q = query.toLowerCase(Locale.ROOT);
		return label.toLowerCase(Locale.ROOT).contains(q) || description.toLowerCase(Locale.ROOT).contains(q);
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement json);

	protected void loadValue(T v) {
		this.value = sanitize(v);
	}
}
