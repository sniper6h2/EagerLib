package dev.eagerlib.ui;

import java.util.ArrayList;
import java.util.List;

import dev.eagerlib.api.Changelog;
import dev.eagerlib.api.EagerMod;
import dev.eagerlib.render.Gfx;
import dev.eagerlib.render.Icons;
import dev.eagerlib.render.Theme;

final class ChangelogView implements View {
	private record Line(int kind, String text, EagerMod mod, Changelog entry, float y) {}

	private final List<EagerMod> mods;
	private final List<Line> lines = new ArrayList<>();
	private final List<float[]> cards = new ArrayList<>();

	private float clipTop = -1e9F;
	private float clipBottom = 1e9F;

	@Override
	public void clip(float top, float bottom) {
		clipTop = top;
		clipBottom = bottom;
	}

	ChangelogView(List<EagerMod> mods) {
		this.mods = mods;
	}

	@Override
	public float layout(Gfx g, Theme t, float w) {
		lines.clear();
		cards.clear();
		float y = 2;
		for (EagerMod m : mods) {
			float start = y;
			lines.add(new Line(0, m.name(), m, null, y + 8));
			y += 32;
			if (m.changelog().isEmpty()) {
				lines.add(new Line(3, "No changelog published yet.", m, null, y));
				y += 14;
			}
			for (Changelog c : m.changelog()) {
				lines.add(new Line(1, c.version(), m, c, y));
				y += 16;
				for (String change : c.changes()) {
					List<String> wrapped = g.wrap(change, w - 44);
					for (int i = 0; i < wrapped.size(); i++) {
						lines.add(new Line(i == 0 ? 2 : 4, wrapped.get(i), m, c, y));
						y += 10;
					}
					y += 2;
				}
				y += 6;
			}
			y += 4;
			cards.add(new float[] {start, y - start});
			y += 8;
		}
		return y;
	}

	@Override
	public void render(Gfx g, Theme t, float x, float y, float w, int mx, int my, boolean hover) {
		float r = Math.max(2, t.radius * 0.8F);
		for (float[] c : cards) {
			if (y + c[0] > clipBottom || y + c[0] + c[1] < clipTop) continue;
			g.round(x, y + c[0], w, c[1], r, Theme.scaleAlpha(t.glass, 0.55F * t.glassOpacity));
			if (t.border > 0.01F) g.outline(x, y + c[0], w, c[1], r, Theme.alpha(0xFFFFFF, 0.10F * t.border));
		}
		for (Line l : lines) {
			float ly = y + l.y();
			if (ly > clipBottom || ly + 20 < clipTop) continue;
			switch (l.kind()) {
				case 0 -> {
					Icons.Icon icon = Icons.forMod(l.mod().id(), l.mod().iconPath());
					if (icon != null) g.icon(icon, x + 10, ly - 2, 18);
					g.text("§l" + l.text(), x + 34, ly + 3, t.text);
				}
				case 1 -> {
					String v = "v" + l.text();
					float pw = g.width(v) + 12;
					g.round(x + 12, ly, pw, 12, 6, Theme.alpha(t.accent & 0xFFFFFF, 0.85F));
					g.text(v, x + 18, ly + 2, t.onAccent());
					if (l.entry().date() != null && !l.entry().date().isEmpty()) g.text(l.entry().date(), x + 18 + pw, ly + 2, t.textDim);
				}
				case 2 -> {
					g.circle(x + 18, ly + 3.5F, 1.6F, t.accent);
					g.text(l.text(), x + 26, ly, t.text);
				}
				case 4 -> g.text(l.text(), x + 26, ly, t.text);
				default -> g.text(l.text(), x + 14, ly, t.textDim);
			}
		}
	}
}
