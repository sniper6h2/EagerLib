package dev.eagerlib.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;

import dev.eagerlib.api.BoolOption;
import dev.eagerlib.api.ButtonOption;
import dev.eagerlib.api.ChoiceOption;
import dev.eagerlib.api.ColorOption;
import dev.eagerlib.api.KeyOption;
import dev.eagerlib.api.NumberOption;
import dev.eagerlib.api.Option;
import dev.eagerlib.api.TextOption;
import dev.eagerlib.core.Mc;
import dev.eagerlib.core.Settings;
import dev.eagerlib.render.Gfx;
import dev.eagerlib.render.Glyphs;
import dev.eagerlib.render.Theme;

final class OptionListView implements View {
	record Group(Supplier<String> title, List<Option<?>> options) {}

	private record K(Object o, int k) {}

	private record Placed(Option<?> option, float y, float h, List<String> lines, float extra, boolean first) {}

	private static final int TOGGLE = 1, SLIDER = 2, VALUE_BOX = 3, SWATCH = 4, CHANNEL = 5, HEX = 6, PRESET = 7,
		CHOICE = 8, KEY = 9, TEXT = 10, BUTTON = 11, RESET = 12;

	private static final int[] PRESETS = {0xFFFFFFFF, 0xFF8C8C8C, 0xFFFF5A5A, 0xFFFF9F43, 0xFFFFD93D, 0xFF51E08A, 0xFF2ED3E6, 0xFF3D8BFF, 0xFF8E7CFF, 0xFFFF7AC6};

	private final List<Group> groups;
	private final Anim anim;
	private final List<Hit> hits = new ArrayList<>();
	private final List<Placed> placed = new ArrayList<>();
	private final List<float[]> cards = new ArrayList<>();
	private final List<Object[]> headers = new ArrayList<>();
	private final Map<ColorOption, float[]> hsv = new HashMap<>();
	private final Set<ColorOption> expanded = new HashSet<>();
	private final String emptyText;

	private Option<?> editing;
	private TextEdit edit;
	private int editKind;
	private KeyOption listening;
	private Object dragTarget;
	private int dragChannel = -1;
	private float dragX;
	private float dragW;
	private float controlWidth;
	private float clipTop = -1e9F;
	private float clipBottom = 1e9F;

	@Override
	public void clip(float top, float bottom) {
		clipTop = top;
		clipBottom = bottom;
	}

	OptionListView(List<Group> groups, Anim anim, String emptyText) {
		this.groups = groups;
		this.anim = anim;
		this.emptyText = emptyText;
	}

	@Override
	public float layout(Gfx g, Theme t, float w) {
		placed.clear();
		cards.clear();
		headers.clear();
		int base = Settings.DENSITY.get().rowHeight;
		boolean desc = Settings.DESCRIPTIONS.get();
		controlWidth = Math.min(190, w * 0.44F);
		float textW = w - 24;
		float y = 2;
		for (Group grp : groups) {
			List<Option<?>> visible = new ArrayList<>();
			for (Option<?> o : grp.options()) if (o.isVisible()) visible.add(o);
			if (visible.isEmpty()) continue;
			headers.add(new Object[] {grp.title().get(), y});
			y += 14;
			float cardY = y;
			y += 3;
			boolean first = true;
			for (Option<?> o : visible) {
				List<String> lines = desc && !o.description().isEmpty() ? g.wrap(o.description(), Math.max(60, textW)) : List.of();
				if (lines.size() > 4) lines = lines.subList(0, 4);
				float h = lines.isEmpty() ? base : Math.max(base, 6 + 9 + 2 + lines.size() * 10 + 4);
				float extra = 0;
				if (o instanceof ColorOption co) {
					float open = anim.to(new K(co, 99), expanded.contains(co) ? 1F : 0F, 14F);
					extra = colorEditorHeight(co) * Anim.ease(open);
				}
				placed.add(new Placed(o, y, h, lines, extra, first));
				first = false;
				y += h + extra;
			}
			y += 3;
			cards.add(new float[] {cardY, y - cardY});
			y += 10;
		}
		return Math.max(y, 40);
	}

	private static float colorEditorHeight(ColorOption co) {
		return (co.hasAlpha() ? 4 : 3) * 15 + 24;
	}

	private void hit(float x, float y, float w, float h, Object target, int kind, int extra) {
		hits.add(new Hit(x, y, w, h, target, kind, extra));
	}

	@Override
	public void render(Gfx g, Theme t, float x, float y, float w, int mx, int my, boolean hover) {
		hits.clear();
		if (placed.isEmpty()) {
			g.textCentered(emptyText, x + w / 2, y + 30, t.textDim);
			return;
		}
		float r = Math.max(2, t.radius * 0.8F);
		for (Object[] h : headers) {
			float hy = y + (float) h[1];
			if (hy > clipBottom || hy + 12 < clipTop) continue;
			g.text("§l" + h[0], x + 4, y + (float) h[1] + 2, t.text);
		}
		for (float[] c : cards) {
			if (y + c[0] > clipBottom || y + c[0] + c[1] < clipTop) continue;
			g.round(x, y + c[0], w, c[1], r, Theme.scaleAlpha(t.glass, 0.55F * t.glassOpacity));
			if (t.border > 0.01F) g.outline(x, y + c[0], w, c[1], r, Theme.alpha(0xFFFFFF, 0.10F * t.border));
		}
		for (Placed p : placed) {
			float ry = y + p.y();
			if (ry > clipBottom || ry + p.h() + p.extra() < clipTop) continue;
			boolean hovered = hover && mx >= x && mx < x + w && my >= ry && my < ry + p.h() + p.extra();
			float ha = anim.to(new K(p.option(), 0), hovered ? 1F : 0F, 18F);
			if (ha > 0.01F) g.round(x + 3, ry + 1, w - 6, p.h() + p.extra() - 2, r * 0.7F, Theme.alpha(t.text & 0xFFFFFF, 0.05F * ha));
			if (!p.first()) g.rect(x + 10, ry - 0.25F, w - 20, 0.5F, Theme.alpha(t.text & 0xFFFFFF, 0.08F));
			float labelY = ry + (p.lines().isEmpty() ? (p.h() - 8) / 2F : 6);
			g.text(g.ellipsize(p.option().label(), w - controlWidth - 40), x + 12, labelY, t.text);
			for (int i = 0; i < p.lines().size(); i++) g.text(p.lines().get(i), x + 12, labelY + 11 + i * 10, t.textDim);
			float cy = labelY + 4;
			float right = x + w - 12;
			float left = control(g, t, p.option(), right, cy, hovered);
			if (p.option().persistent() && !p.option().isDefault() && !(p.option() instanceof ButtonOption)) {
				float rx = left - 15, ryy = cy - 5;
				boolean rh = mx >= rx - 2 && mx < rx + 12 && my >= ryy - 2 && my < ryy + 12;
				g.glyph(Glyphs.RESET, rx, ryy, 1F, rh ? t.accent : Theme.alpha(t.text & 0xFFFFFF, 0.35F + 0.4F * ha));
				hit(rx - 2, ryy - 2, 14, 14, p.option(), RESET, 0);
			}
			if (p.option() instanceof ColorOption co && p.extra() > 0.5F) {
				g.scissor(x, ry + p.h(), w, p.extra());
				colorEditor(g, t, co, x + 12, ry + p.h(), w - 24, mx, my);
				g.noScissor();
			}
		}
	}

	private float control(Gfx g, Theme t, Option<?> o, float right, float cy, boolean rowHover) {
		int field = Theme.alpha(t.text & 0xFFFFFF, 0.10F);
		if (o instanceof BoolOption b) {
			float tw = 24, th = 13, tx = right - tw, ty = cy - th / 2;
			float on = anim.to(new K(b, 1), b.get() ? 1F : 0F, 16F);
			g.round(tx, ty, tw, th, th / 2, Theme.mix(Theme.alpha(t.text & 0xFFFFFF, 0.18F), t.accent, on));
			float kx = tx + 6.5F + (tw - 13) * on;
			g.softCircle(kx, cy + 0.6F, 5.4F, 0x38000000);
			g.circle(kx, cy, 5F, 0xFFFFFFFF);
			hit(tx - 3, ty - 3, tw + 6, th + 6, b, TOGGLE, 0);
			return tx;
		}
		if (o instanceof NumberOption n) {
			float bw = Math.max(38, g.width(n.format()) + 10), bh = 13, bx = right - bw;
			boolean ed = editing == n;
			g.round(bx, cy - bh / 2, bw, bh, 4, ed ? Theme.alpha(t.accent & 0xFFFFFF, 0.25F) : field);
			if (ed) g.outline(bx, cy - bh / 2, bw, bh, 4, t.accent);
			drawFieldText(g, t, ed ? edit : null, n.format(), bx, cy, bw, true);
			hit(bx, cy - bh / 2, bw, bh, n, VALUE_BOX, 0);
			float trW = Math.max(40, Math.min(120, controlWidth - bw - 14));
			float trX = bx - 9 - trW;
			float f = anim.to(new K(n, 1), (float) n.fraction(), 24F);
			g.round(trX, cy - 2, trW, 4, 2, Theme.alpha(t.text & 0xFFFFFF, 0.18F));
			g.round(trX, cy - 2, Math.max(4, trW * f), 4, 2, t.accent);
			float kx = trX + trW * f;
			boolean active = dragTarget == n;
			g.softCircle(kx, cy + 0.6F, active ? 6.2F : 5.4F, 0x38000000);
			g.circle(kx, cy, active ? 5.8F : 5F, 0xFFFFFFFF);
			hit(trX - 6, cy - 7, trW + 12, 14, n, SLIDER, 0);
			return trX - 6;
		}
		if (o instanceof ColorOption co) {
			float sw = 30, sh = 13, sx = right - sw, sy = cy - sh / 2;
			checker(g, sx, sy, sw, sh, co.hasAlpha());
			g.round(sx, sy, sw, sh, 4, co.get());
			g.outline(sx, sy, sw, sh, 4, expanded.contains(co) ? t.accent : Theme.alpha(0xFFFFFF, 0.35F));
			String hex = "#" + co.hex();
			g.text(hex, sx - 6 - g.width(hex), cy - 4, t.textDim);
			hit(sx - 8 - g.width(hex), sy, sw + 8 + g.width(hex), sh, co, SWATCH, 0);
			return sx - 6 - g.width(hex);
		}
		if (o instanceof ChoiceOption<?> c) {
			String v = c.currentName();
			float pw = Math.max(88, g.width(v) + 34), px = right - pw;
			g.round(px, cy - 6.5F, pw, 13, 6.5F, field);
			g.glyph(Glyphs.CHEVRON_LEFT, px + 6, cy - 3.5F, 1F, t.textDim);
			g.glyph(Glyphs.CHEVRON_RIGHT, px + pw - 10, cy - 3.5F, 1F, t.textDim);
			g.textCentered(g.ellipsize(v, pw - 30), px + pw / 2, cy - 4, t.text);
			hit(px, cy - 6.5F, pw / 2, 13, c, CHOICE, -1);
			hit(px + pw / 2, cy - 6.5F, pw / 2, 13, c, CHOICE, 1);
			return px;
		}
		if (o instanceof KeyOption k) {
			boolean listen = listening == k;
			String s = listen ? "Press a key..." : k.keyName();
			float pw = Math.max(80, g.width(s) + 18), px = right - pw;
			float pulse = listen ? 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 160.0) : 0;
			g.round(px, cy - 6.5F, pw, 13, 4, listen ? Theme.alpha(t.accent & 0xFFFFFF, 0.20F + 0.2F * pulse) : field);
			if (listen) g.outline(px, cy - 6.5F, pw, 13, 4, t.accent);
			g.textCentered(s, px + pw / 2, cy - 4, k.isBound() || listen ? t.text : t.textDim);
			hit(px, cy - 6.5F, pw, 13, k, KEY, 0);
			return px;
		}
		if (o instanceof TextOption to) {
			float fw = Math.min(150, controlWidth), fx = right - fw;
			boolean ed = editing == to;
			g.round(fx, cy - 6.5F, fw, 13, 4, ed ? Theme.alpha(t.accent & 0xFFFFFF, 0.18F) : field);
			if (ed) g.outline(fx, cy - 6.5F, fw, 13, 4, t.accent);
			String shown = to.get().isEmpty() && !ed ? to.placeholder() : to.get();
			g.scissor(fx + 3, cy - 6.5F, fw - 6, 13);
			if (ed) drawFieldText(g, t, edit, "", fx, cy, fw, false);
			else g.text(shown, fx + 5, cy - 4, to.get().isEmpty() ? t.textFaint : t.text);
			g.noScissor();
			hit(fx, cy - 6.5F, fw, 13, to, TEXT, 0);
			return fx;
		}
		if (o instanceof ButtonOption b) {
			String label = b.defaultValue();
			float pw = g.width(label) + 22, px = right - pw;
			int base = b.danger() ? 0xFFE5484D : t.accent;
			g.round(px, cy - 7, pw, 14, 7, Theme.scaleAlpha(base, rowHover ? 1F : 0.85F));
			g.textCentered(label, px + pw / 2, cy - 4, b.danger() ? 0xFFFFFFFF : t.onAccent());
			hit(px, cy - 7, pw, 14, b, BUTTON, 0);
			return px;
		}
		return right;
	}

	private void drawFieldText(Gfx g, Theme t, TextEdit e, String fallback, float bx, float cy, float bw, boolean center) {
		if (e == null) {
			if (center) g.textCentered(fallback, bx + bw / 2, cy - 4, t.text);
			else g.text(fallback, bx + 5, cy - 4, t.text);
			return;
		}
		String v = e.value();
		float tx = center ? bx + bw / 2 - g.width(v) / 2F : bx + 5;
		float overflow = g.width(v) - (bw - 10);
		if (!center && overflow > 0) tx -= overflow;
		g.text(v, tx, cy - 4, t.text);
		if (e.caretVisible()) {
			float cx = tx + g.width(v.substring(0, Math.min(e.caret, v.length())));
			g.rect(cx, cy - 5, 0.75F, 10, t.accent);
		}
	}

	private void checker(Gfx g, float x, float y, float w, float h, boolean show) {
		if (!show) return;
		g.round(x, y, w, h, 4, 0xFFFFFFFF);
		for (int i = 0; i < w / 3; i++) {
			for (int j = 0; j < h / 3; j++) {
				if ((i + j) % 2 == 0) g.rect(x + i * 3 + 0.5F, y + j * 3 + 0.5F, Math.min(3, w - i * 3 - 1), Math.min(3, h - j * 3 - 1), 0xFFC8C8C8);
			}
		}
	}

	private float[] hsvOf(ColorOption co) {
		float[] v = hsv.get(co);
		int cur = co.get();
		if (v == null || Colors.fromHsv(v[0], v[1], v[2], v[3]) != cur) {
			float[] n = Colors.toHsv(cur);
			if (v != null && n[1] < 0.001F) n[0] = v[0];
			if (v != null && n[2] < 0.001F) { n[0] = v[0]; n[1] = v[1]; }
			hsv.put(co, n);
			return n;
		}
		return v;
	}

	private void colorEditor(Gfx g, Theme t, ColorOption co, float x, float y, float w, int mx, int my) {
		float[] v = hsvOf(co);
		String[] names = {"H", "S", "B", "A"};
		int channels = co.hasAlpha() ? 4 : 3;
		float trX = x + 16, trW = w - 16 - 40;
		for (int c = 0; c < channels; c++) {
			float rowY = y + 6 + c * 15;
			float cy = rowY + 4;
			g.text(names[c], x + 2, rowY, t.textDim);
			if (c == 3) checker(g, trX, cy - 3, trW, 6, true);
			int segs = 28;
			int first = channelColor(v, c, 0), last = channelColor(v, c, 1);
			g.round(trX, cy - 3, 8, 6, 3, first);
			g.round(trX + trW - 8, cy - 3, 8, 6, 3, last);
			float inner = trW - 6;
			for (int s = 0; s < segs; s++) {
				float f = (s + 0.5F) / segs;
				g.rect(trX + 3 + inner * s / segs, cy - 3, inner / segs + 0.3F, 6, channelColor(v, c, f));
			}
			float kx = trX + trW * v[c];
			g.softCircle(kx, cy + 0.5F, 5F, 0x40000000);
			g.circle(kx, cy, 4.6F, 0xFFFFFFFF);
			g.circle(kx, cy, 3.1F, channelColor(v, c, v[c]) | 0xFF000000);
			String val = c == 0 ? Math.round(v[0] * 360) + "°" : Math.round(v[c] * 100) + "%";
			g.text(val, x + w - g.width(val), rowY, t.textDim);
			hit(trX - 4, cy - 6, trW + 8, 12, co, CHANNEL, c);
		}
		float rowY = y + 6 + channels * 15 + 1;
		boolean ed = editing == co;
		float hw = 62;
		g.round(x, rowY, hw, 13, 4, ed ? Theme.alpha(t.accent & 0xFFFFFF, 0.2F) : Theme.alpha(t.text & 0xFFFFFF, 0.10F));
		g.outline(x, rowY, hw, 13, 4, ed ? t.accent : Theme.alpha(t.text & 0xFFFFFF, 0.12F));
		if (ed) drawFieldText(g, t, edit, "", x, rowY + 6.5F, hw, false);
		else g.text("#" + co.hex(), x + 5, rowY + 2.5F, t.text);
		hit(x, rowY, hw, 13, co, HEX, 0);
		float px = x + hw + 8;
		for (int p : PRESETS) {
			if (px + 11 > x + w) break;
			int col = co.hasAlpha() ? (p & 0xFFFFFF) | (co.get() & 0xFF000000) : p;
			g.round(px, rowY + 1, 11, 11, 3, col | 0xFF000000);
			g.outline(px, rowY + 1, 11, 11, 3, Theme.alpha(0xFFFFFF, 0.3F));
			hit(px, rowY + 1, 11, 11, co, PRESET, col);
			px += 14;
		}
	}

	private static int channelColor(float[] v, int channel, float f) {
		return switch (channel) {
			case 0 -> Colors.fromHsv(f, 1, 1, 1);
			case 1 -> Colors.fromHsv(v[0], f, v[2], 1);
			case 2 -> Colors.fromHsv(v[0], v[1], f, 1);
			default -> Colors.fromHsv(v[0], v[1], v[2], f);
		};
	}

	private Hit hitAt(double mx, double my) {
		for (int i = hits.size() - 1; i >= 0; i--) if (hits.get(i).contains(mx, my)) return hits.get(i);
		return null;
	}

	@Override
	public boolean click(double mx, double my, int button) {
		Hit h = hitAt(mx, my);
		if (listening != null) {
			listening = null;
			if (h == null || h.kind == KEY) return true;
		}
		if (editing != null && (h == null || h.target != editing || (h.kind != VALUE_BOX && h.kind != TEXT && h.kind != HEX))) commit();
		if (h == null) return false;
		Option<?> o = h.target instanceof Option<?> op ? op : null;
		switch (h.kind) {
			case TOGGLE -> { ((BoolOption) o).toggle(); Mc.click(); }
			case SLIDER -> {
				dragTarget = o;
				dragChannel = -1;
				dragX = h.x + 6;
				dragW = h.w - 12;
				((NumberOption) o).setFraction((mx - dragX) / dragW);
			}
			case VALUE_BOX -> startEdit(o, ((NumberOption) o).format().replace(((NumberOption) o).suffix(), ""), 1);
			case SWATCH -> {
				ColorOption co = (ColorOption) o;
				if (!expanded.remove(co)) expanded.add(co);
				Mc.click();
			}
			case CHANNEL -> {
				dragTarget = o;
				dragChannel = h.extra;
				dragX = h.x + 4;
				dragW = h.w - 8;
				applyChannel((ColorOption) o, (float) ((mx - dragX) / dragW));
			}
			case HEX -> startEdit(o, ((ColorOption) o).hex(), 2);
			case PRESET -> { ((ColorOption) o).set(h.extra); Mc.click(); }
			case CHOICE -> { ((ChoiceOption<?>) o).cycle(button == 1 ? -h.extra : h.extra); Mc.click(); }
			case KEY -> { listening = (KeyOption) o; Mc.click(); }
			case TEXT -> startEdit(o, ((TextOption) o).get(), 0);
			case BUTTON -> { Mc.click(); ((ButtonOption) o).press(); }
			case RESET -> { o.reset(); hsv.remove(o); Mc.click(); }
			default -> { return false; }
		}
		return true;
	}

	private void startEdit(Option<?> o, String initial, int kind) {
		if (editing == o) return;
		editing = o;
		editKind = kind;
		int max = kind == 0 ? ((TextOption) o).maxLength() : (kind == 2 ? 8 : 16);
		edit = new TextEdit(initial, max, cp -> switch (kind) {
			case 1 -> (cp >= '0' && cp <= '9') || cp == '.' || cp == '-';
			case 2 -> (cp >= '0' && cp <= '9') || (cp >= 'a' && cp <= 'f') || (cp >= 'A' && cp <= 'F');
			default -> true;
		});
	}

	@SuppressWarnings("unchecked")
	private void commit() {
		if (editing == null) return;
		String v = edit.value().trim();
		try {
			switch (editKind) {
				case 0 -> ((Option<String>) editing).set(edit.value());
				case 1 -> { if (!v.isEmpty()) ((NumberOption) editing).set(Double.parseDouble(v)); }
				case 2 -> {
					ColorOption co = (ColorOption) editing;
					if (v.length() == 6) co.set((int) (Long.parseLong(v, 16) | (co.get() & 0xFF000000L)));
					else if (v.length() == 8) co.set((int) Long.parseLong(v, 16));
					hsv.remove(co);
				}
				default -> {}
			}
		} catch (NumberFormatException ignored) {
		}
		editing = null;
		edit = null;
	}

	private void applyChannel(ColorOption co, float f) {
		float[] v = hsvOf(co).clone();
		v[dragChannel] = Math.max(0, Math.min(dragChannel == 0 ? 0.999F : 1F, f));
		hsv.put(co, v);
		int c = Colors.fromHsv(v[0], v[1], v[2], co.hasAlpha() ? v[3] : 1F);
		co.set(c);
		hsv.put(co, v);
	}

	@Override
	public boolean release(double mx, double my, int button) {
		boolean was = dragTarget != null;
		dragTarget = null;
		dragChannel = -1;
		return was;
	}

	@Override
	public boolean drag(double mx, double my, int button) {
		if (dragTarget instanceof NumberOption n) {
			n.setFraction((mx - dragX) / dragW);
			return true;
		}
		if (dragTarget instanceof ColorOption co && dragChannel >= 0) {
			applyChannel(co, (float) ((mx - dragX) / dragW));
			return true;
		}
		return false;
	}

	@Override
	public boolean key(int key, int mods) {
		if (listening != null) {
			if (key == InputConstants.KEY_ESCAPE) listening = null;
			else if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
				listening.set(KeyOption.NONE);
				listening = null;
			} else {
				listening.set(key);
				listening = null;
			}
			Mc.click();
			return true;
		}
		if (editing != null) {
			if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_TAB) {
				commit();
				return true;
			}
			if (key == InputConstants.KEY_ESCAPE) {
				editing = null;
				edit = null;
				return true;
			}
			edit.key(key, mods);
			return true;
		}
		return false;
	}

	@Override
	public boolean typed(int cp) {
		return editing != null && edit.typed(cp);
	}

	@Override
	public boolean capturing() {
		return editing != null || listening != null;
	}

	@Override
	public void blur() {
		commit();
		listening = null;
		dragTarget = null;
	}
}
