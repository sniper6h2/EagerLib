package dev.eagerlib.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dev.eagerlib.api.EagerMod;
import dev.eagerlib.core.Mc;
import dev.eagerlib.render.Gfx;
import dev.eagerlib.render.Icons;
import dev.eagerlib.render.Theme;

final class ModsGridView implements View {
	private final List<EagerMod> mods;
	private final Consumer<EagerMod> open;
	private final Anim anim;
	private final List<Hit> hits = new ArrayList<>();
	private int columns = 1;
	private float cardW;
	private static final float CARD_H = 66;

	ModsGridView(List<EagerMod> mods, Consumer<EagerMod> open, Anim anim) {
		this.mods = mods;
		this.open = open;
		this.anim = anim;
	}

	@Override
	public float layout(Gfx g, Theme t, float w) {
		columns = Math.max(1, (int) (w / 190));
		cardW = (w - (columns - 1) * 8) / columns;
		int rows = (mods.size() + columns - 1) / columns;
		return Math.max(40, rows * (CARD_H + 8) + 4);
	}

	@Override
	public void render(Gfx g, Theme t, float x, float y, float w, int mx, int my, boolean hover) {
		hits.clear();
		if (mods.isEmpty()) {
			g.textCentered("No mods use EagerLib yet.", x + w / 2, y + 30, t.textDim);
			return;
		}
		float r = Math.max(2, t.radius * 0.8F);
		for (int i = 0; i < mods.size(); i++) {
			EagerMod m = mods.get(i);
			float cx = x + (i % columns) * (cardW + 8);
			float cy = y + 2 + (i / columns) * (CARD_H + 8);
			boolean h = hover && mx >= cx && mx < cx + cardW && my >= cy && my < cy + CARD_H;
			float ha = anim.to(m, h ? 1F : 0F, 16F);
			g.round(cx, cy, cardW, CARD_H, r, Theme.scaleAlpha(t.glass, (0.55F + 0.35F * ha) * t.glassOpacity));
			g.outline(cx, cy, cardW, CARD_H, r, Theme.mix(Theme.alpha(0xFFFFFF, 0.10F * t.border), t.accent, ha * 0.8F));
			Icons.Icon icon = Icons.forMod(m.id(), m.iconPath());
			if (icon != null) g.icon(icon, cx + 8, cy + 8, 28);
			else {
				g.round(cx + 8, cy + 8, 28, 28, 6, t.accent);
				g.textCentered(m.name().substring(0, 1), cx + 22, cy + 18, t.onAccent());
			}
			g.text("§l" + g.ellipsize(m.name(), cardW - 56), cx + 42, cy + 10, t.text);
			String meta = (m.version().isEmpty() ? "" : "v" + m.version()) + (m.author().isEmpty() ? "" : "  ·  " + m.author());
			g.text(g.ellipsize(meta, cardW - 50), cx + 42, cy + 22, t.textDim);
			List<String> desc = g.wrap(m.description(), cardW - 16);
			for (int l = 0; l < Math.min(2, desc.size()); l++) {
				String line = desc.get(l);
				if (l == 1 && desc.size() > 2) line = g.ellipsize(line + " ...", cardW - 16);
				g.text(line, cx + 8, cy + 42 + l * 10, t.textFaint);
			}
			hits.add(new Hit(cx, cy, cardW, CARD_H, m, 0, 0));
		}
	}

	@Override
	public boolean click(double mx, double my, int button) {
		for (Hit h : hits) {
			if (h.contains(mx, my)) {
				Mc.click();
				open.accept((EagerMod) h.target);
				return true;
			}
		}
		return false;
	}
}
