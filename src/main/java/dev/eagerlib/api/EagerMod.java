package dev.eagerlib.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class EagerMod {
	private final String id;
	private String name;
	private String author = "";
	private String version = "";
	private String description = "";
	private String iconPath;
	private String sourceUrl;
	private String websiteUrl;
	private boolean listed = true;
	private final List<Section> sections = new ArrayList<>();
	private final Map<String, Option<?>> byId = new LinkedHashMap<>();
	private final List<Changelog> changelog = new ArrayList<>();
	private final List<HudElement> hud = new ArrayList<>();
	private boolean dirty;
	private Consumer<EagerMod> saver = m -> {};

	EagerMod(String id) {
		this.id = id;
		this.name = id;
	}

	public String id() { return id; }
	public String name() { return name; }
	public String author() { return author; }
	public String version() { return version; }
	public String description() { return description; }
	public String iconPath() { return iconPath; }
	public String sourceUrl() { return sourceUrl; }
	public String websiteUrl() { return websiteUrl; }
	public boolean listed() { return listed; }
	public List<Section> sections() { return Collections.unmodifiableList(sections); }
	public List<Changelog> changelog() { return Collections.unmodifiableList(changelog); }
	public List<HudElement> hudElements() { return Collections.unmodifiableList(hud); }
	public Map<String, Option<?>> options() { return Collections.unmodifiableMap(byId); }

	void index(Option<?> option) {
		byId.put(option.id(), option);
	}

	public void markDirty() { dirty = true; }

	public boolean isDirty() { return dirty; }

	public void save() {
		dirty = false;
		saver.accept(this);
	}

	public void resetAll() {
		for (Option<?> o : byId.values()) {
			if (o.persistent()) o.reset();
		}
	}

	public void addHud(HudElement e) { hud.add(e); }

	public static final class Builder {
		private final EagerMod mod;
		private final Consumer<EagerMod> onBuild;

		public Builder(String id, Consumer<EagerMod> onBuild) {
			this.mod = new EagerMod(id);
			this.onBuild = onBuild;
		}

		public Builder name(String v) { mod.name = v; return this; }
		public Builder author(String v) { mod.author = v; return this; }
		public Builder version(String v) { mod.version = v; return this; }
		public Builder description(String v) { mod.description = v; return this; }
		public Builder icon(String classpathPath) { mod.iconPath = classpathPath; return this; }
		public Builder source(String url) { mod.sourceUrl = url; return this; }
		public Builder website(String url) { mod.websiteUrl = url; return this; }
		public Builder unlisted() { mod.listed = false; return this; }

		public Section section(String title) {
			Section s = new Section(title, mod);
			mod.sections.add(s);
			return s;
		}

		public Builder changelog(String version, String date, String... changes) {
			mod.changelog.add(new Changelog(version, date, List.of(changes)));
			return this;
		}

		public Builder hud(HudElement e) { mod.hud.add(e); return this; }

		public EagerMod build() {
			onBuild.accept(mod);
			return mod;
		}
	}

	public void setSaver(Consumer<EagerMod> s) { this.saver = s; }

	public void clearDirty() { dirty = false; }

	public void fillMeta(String field, String value) {
		if (value == null) return;
		switch (field) {
			case "version" -> version = value;
			case "name" -> name = value;
			case "author" -> author = value;
			case "description" -> description = value;
			case "sourceUrl" -> sourceUrl = value;
			case "websiteUrl" -> websiteUrl = value;
			default -> {}
		}
	}
}
