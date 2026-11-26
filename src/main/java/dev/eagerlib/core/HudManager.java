package dev.eagerlib.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import dev.eagerlib.api.EagerMod;
import dev.eagerlib.api.HudElement;
import dev.eagerlib.platform.Platform;
import dev.eagerlib.render.Canvas;
import dev.eagerlib.render.Gfx;
import dev.eagerlib.render.Theme;

public final class HudManager {
	public static final class Entry {
		public final EagerMod mod;
		public final HudElement element;
		public boolean enabled;
		public float x;
		public float y;
		public float scale = 1F;

		Entry(EagerMod mod, HudElement element) {
			this.mod = mod;
			this.element = element;
			reset();
		}

		public void reset() {
			enabled = element.enabledByDefault();
			x = element.defaultX();
			y = element.defaultY();
			scale = 1F;
		}

		public String key() { return mod.id() + ":" + element.id(); }
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static boolean editing;
	private static JsonObject saved = new JsonObject();

	private HudManager() {}

	public static List<Entry> entries() { return Collections.unmodifiableList(ENTRIES); }

	public static void setEditing(boolean e) { editing = e; }

	static void register(EagerMod mod, HudElement element) {
		Entry e = new Entry(mod, element);
		apply(e);
		ENTRIES.add(e);
	}

	private static void apply(Entry e) {
		if (!saved.has(e.key()) || !saved.get(e.key()).isJsonObject()) return;
		JsonObject o = saved.getAsJsonObject(e.key());
		try {
			if (o.has("enabled")) e.enabled = o.get("enabled").getAsBoolean();
			if (o.has("x")) e.x = o.get("x").getAsFloat();
			if (o.has("y")) e.y = o.get("y").getAsFloat();
			if (o.has("scale")) e.scale = Math.max(0.5F, Math.min(3F, o.get("scale").getAsFloat()));
		} catch (Exception ignored) {
		}
	}

	static void load() {
		saved = ConfigIO.read("hud.json");
		for (Entry e : ENTRIES) apply(e);
	}

	public static void save() {
		JsonObject root = new JsonObject();
		for (Entry e : ENTRIES) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", e.enabled);
			o.addProperty("x", e.x);
			o.addProperty("y", e.y);
			o.addProperty("scale", e.scale);
			root.add(e.key(), o);
		}
		saved = root;
		ConfigIO.write("hud.json", root);
	}

	public static float padding() { return Settings.HUD_PADDING == null ? 3 : Settings.HUD_PADDING.getFloat(); }

	public static float width(Gfx g, Entry e, String text) {
		return (g.width(text) + padding() * 2 + (Settings.HUD_BACKGROUND.get() ? 2 : 0)) * e.scale;
	}

	public static float height(Gfx g, Entry e) {
		return (g.lineHeight() - 1 + padding() * 2 + (Settings.HUD_BACKGROUND.get() ? 2 : 0)) * e.scale;
	}

	public static float[] position(Gfx g, Entry e, String text, int screenW, int screenH) {
		float w = width(g, e, text), h = height(g, e);
		float x = Math.max(0, Math.min(screenW - w, e.x * screenW));
		float y = Math.max(0, Math.min(screenH - h, e.y * screenH));
		return new float[] {x, y, w, h};
	}

	public static void draw(Gfx g, Theme t, Entry e, String text, float x, float y) {
		g.pushScale(e.scale);
		float sx = x / e.scale, sy = y / e.scale;
		float pad = padding();
		boolean bg = Settings.HUD_BACKGROUND.get();
		float extra = bg ? 1 : 0;
		if (bg) {
			float w = g.width(text) + pad * 2 + 2, h = g.lineHeight() - 1 + pad * 2 + 2;
			float r = Math.min(t.radius * 0.6F, h / 2F);
			g.round(sx, sy, w, h, r, Settings.HUD_BG_COLOR.get());
			if (t.border > 0.01F) g.outline(sx, sy, w, h, r, Theme.alpha(0xFFFFFF, 0.18F * t.border));
		}
		g.text(text, sx + pad + extra, sy + pad + extra, e.element.color(), Settings.HUD_SHADOW.get());
		g.popScale();
	}

	public static void render(Canvas canvas) {
		if (editing || ENTRIES.isEmpty() || Platform.hudHidden() || Mc.mc().player == null) return;
		Gfx g = null;
		Theme t = null;
		for (Entry e : ENTRIES) {
			if (!e.enabled) continue;
			String text;
			try {
				text = e.element.text(false);
			} catch (Throwable ex) {
				continue;
			}
			if (text == null || text.isEmpty()) continue;
			if (g == null) {
				g = new Gfx(canvas);
				t = Theme.current();
			}
			float[] p = position(g, e, text, canvas.guiWidth(), canvas.guiHeight());
			draw(g, t, e, text, p[0], p[1]);
		}
	}
}
