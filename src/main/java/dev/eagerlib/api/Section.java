package dev.eagerlib.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class Section {
	private final String title;
	private final EagerMod owner;
	private final List<Option<?>> options = new ArrayList<>();

	Section(String title, EagerMod owner) {
		this.title = title;
		this.owner = owner;
	}

	public String title() { return title; }

	public EagerMod owner() { return owner; }

	public List<Option<?>> options() { return Collections.unmodifiableList(options); }

	public <O extends Option<?>> O add(O option) {
		option.owner = owner;
		options.add(option);
		if (owner != null) owner.index(option);
		return option;
	}

	public BoolOption toggle(String id, String label, boolean def) {
		return add(new BoolOption(id, label, def));
	}

	public NumberOption slider(String id, String label, double def, double min, double max, double step) {
		return add(new NumberOption(id, label, def, min, max, step));
	}

	public ColorOption color(String id, String label, int defArgb) {
		return add(new ColorOption(id, label, defArgb, false));
	}

	public ColorOption colorAlpha(String id, String label, int defArgb) {
		return add(new ColorOption(id, label, defArgb, true));
	}

	public <E extends Enum<E>> ChoiceOption<E> choice(String id, String label, E def, Function<E, String> names) {
		return add(ChoiceOption.ofEnum(id, label, def, names));
	}

	public <E> ChoiceOption<E> choice(String id, String label, E def, List<E> values, Function<E, String> names) {
		return add(new ChoiceOption<>(id, label, def, values, names));
	}

	public KeyOption keybind(String id, String label, int defKey) {
		return add(new KeyOption(id, label, defKey));
	}

	public TextOption text(String id, String label, String def, int maxLength) {
		return add(new TextOption(id, label, def, maxLength));
	}

	public ButtonOption button(String id, String label, String buttonText, Runnable action) {
		return add(new ButtonOption(id, label, buttonText, false, action));
	}

	public ButtonOption dangerButton(String id, String label, String buttonText, Runnable action) {
		return add(new ButtonOption(id, label, buttonText, true, action));
	}

	public static Section detached(String title) {
		return new Section(title, null);
	}
}
