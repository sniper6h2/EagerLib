package dev.eagerlib.api;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ChoiceOption<E> extends Option<E> {
	private final List<E> values;
	private final Function<E, String> names;

	public ChoiceOption(String id, String label, E def, List<E> values, Function<E, String> names) {
		super(id, label, def);
		this.values = List.copyOf(values);
		this.names = names;
	}

	public static <E extends Enum<E>> ChoiceOption<E> ofEnum(String id, String label, E def, Function<E, String> names) {
		return new ChoiceOption<>(id, label, def, List.of(def.getDeclaringClass().getEnumConstants()), names);
	}

	public List<E> values() { return values; }

	public String name(E v) { return names.apply(v); }

	public String currentName() { return names.apply(get()); }

	public void cycle(int dir) {
		int i = values.indexOf(get());
		set(values.get(Math.floorMod(i + dir, values.size())));
	}

	@Override public JsonElement save() { return new JsonPrimitive(values.indexOf(get()) >= 0 ? key(get()) : ""); }

	private String key(E v) { return v instanceof Enum<?> e ? e.name() : String.valueOf(v); }

	@Override public void load(JsonElement json) {
		if (json == null || !json.isJsonPrimitive()) return;
		String s = json.getAsString();
		for (E v : values) {
			if (key(v).equals(s)) {
				loadValue(v);
				return;
			}
		}
	}
}
