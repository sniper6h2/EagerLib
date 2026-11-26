package dev.eagerlib.ui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import dev.eagerlib.core.HudManager;
import dev.eagerlib.core.Mc;
import dev.eagerlib.render.Canvas;
import dev.eagerlib.render.Gfx;
import dev.eagerlib.render.Theme;

public final class HudEditorUi implements UiRoot {
	private static final int DONE = 1, GRID = 2, SNAP = 3, RESET = 4;

	private final Anim anim = new Anim();
	private final List<Hit> toolbar = new ArrayList<>();
	private final List<float[]> boxes = new ArrayList<>();
	private final List<float[]> guides = new ArrayList<>();
	private HudManager.Entry dragging;
	private HudManager.Entry selected;
	private float grabX;
	private float grabY;
	private boolean grid = true;
	private boolean snap = true;
	private int screenW = 1;
	private int screenH = 1;
	private Gfx lastGfx;

	public HudEditorUi() {
		HudManager.setEditing(true);
	}

	@Override
	public void render(Canvas canvas, int mx, int my, float delta) {
		Gfx g = new Gfx(canvas);
		lastGfx = g;
		Theme t = Theme.current();
		anim.frame();
		screenW = canvas.guiWidth();
		screenH = canvas.guiHeight();
		toolbar.clear();
		boxes.clear();
		g.rect(0, 0, screenW, screenH, 0x30000000);
		if (grid) {
			int faint = Theme.alpha(0xFFFFFF, 0.05F);
			for (int x = 16; x < screenW; x += 16) g.rect(x, 0, 0.5F, screenH, faint);
			for (int y = 16; y < screenH; y += 16) g.rect(0, y, screenW, 0.5F, faint);
		}
		g.rect(screenW / 2F - 0.25F, 0, 0.5F, screenH, Theme.alpha(t.accent & 0xFFFFFF, 0.22F));
		g.rect(0, screenH / 2F - 0.25F, screenW, 0.5F, Theme.alpha(t.accent & 0xFFFFFF, 0.22F));

		HudManager.Entry hovered = null;
		List<HudManager.Entry> entries = HudManager.entries();
		for (HudManager.Entry e : entries) {
			String text = e.element.text(true);
			if (text == null || text.isEmpty()) text = e.element.label();
			float[] p = HudManager.position(g, e, text, screenW, screenH);
			boxes.add(p);
			if (mx >= p[0] && mx < p[0] + p[2] && my >= p[1] && my < p[1] + p[3]) hovered = e;
		}
		if (dragging != null) hovered = dragging;
		for (int i = 0; i < entries.size(); i++) {
			HudManager.Entry e = entries.get(i);
			float[] p = boxes.get(i);
			String text = e.element.text(true);
			if (text == null || text.isEmpty()) text = e.element.label();
			g.alpha(e.enabled ? 1F : 0.38F);
			HudManager.draw(g, t, e, text, p[0], p[1]);
			g.alpha(1F);
			boolean hot = e == hovered || e == selected;
			float ha = anim.to(e, hot ? 1F : 0F, 18F);
			g.outline(p[0] - 1.5F, p[1] - 1.5F, p[2] + 3, p[3] + 3, 3, Theme.mix(Theme.alpha(0xFFFFFF, 0.30F), t.accent, ha));
		}
		for (float[] gd : guides) {
			if (gd[0] == 0) g.rect(gd[1] - 0.5F, 0, 1, screenH, t.accent);
			else g.rect(0, gd[1] - 0.5F, screenW, 1, t.accent);
		}
		if (hovered != null) {
			int idx = entries.indexOf(hovered);
			float[] p = boxes.get(idx);
			String tag = hovered.element.label() + "  ·  " + hovered.mod.name() + "  ·  " + String.format("%.0f%%", hovered.scale * 100) + (hovered.enabled ? "" : "  ·  hidden");
			float tw = g.width(tag) + 10;
			float tx = Math.max(2, Math.min(screenW - tw - 2, p[0]));
			float ty = p[1] - 16 < 2 ? p[1] + p[3] + 4 : p[1] - 16;
			g.round(tx, ty, tw, 12, 6, 0xD0101016);
			g.text(tag, tx + 5, ty + 2, 0xFFFFFFFF, false);
		}

		String[] labels = {"Done", "Grid: " + (grid ? "On" : "Off"), "Snap: " + (snap ? "On" : "Off"), "Reset All"};
		int[] kinds = {DONE, GRID, SNAP, RESET};
		float total = 8;
		for (String l : labels) total += g.width(l) + 20 + 4;
		float bx = (screenW - total) / 2F, by = screenH / 2F - 60 < 40 ? 8 : 34;
		g.glass(bx, by, total, 22, 11, t, 1F);
		float x = bx + 6;
		for (int i = 0; i < labels.length; i++) {
			float w = g.width(labels[i]) + 20;
			boolean h = mx >= x && mx < x + w && my >= by + 3 && my < by + 19;
			float ha = anim.to(labels[i].substring(0, 3), h ? 1F : 0F, 18F);
			int fill = i == 0 ? Theme.scaleAlpha(t.accent, 0.85F + 0.15F * ha) : Theme.alpha(t.text & 0xFFFFFF, 0.06F + 0.10F * ha);
			g.round(x, by + 3, w, 16, 8, fill);
			g.textCentered(labels[i], x + w / 2, by + 7, i == 0 ? t.onAccent() : t.text);
			toolbar.add(new Hit(x, by + 3, w, 16, null, kinds[i], 0));
			x += w + 4;
		}
		String hint = "Drag to move  \u00b7  Scroll to resize  \u00b7  Right-click to show or hide  \u00b7  Arrow keys nudge";
		if (g.width(hint) + 24 > screenW) hint = "Drag \u00b7 Scroll to resize \u00b7 Right-click to hide";
		float hw = g.width(hint) + 16;
		g.round((screenW - hw) / 2, screenH - 22, hw, 14, 7, 0xB0101016);
		g.textCentered(hint, screenW / 2F, screenH - 19, 0xFFDDDDDD);
	}

	private HudManager.Entry entryAt(double mx, double my) {
		List<HudManager.Entry> entries = HudManager.entries();
		for (int i = Math.min(entries.size(), boxes.size()) - 1; i >= 0; i--) {
			float[] p = boxes.get(i);
			if (mx >= p[0] && mx < p[0] + p[2] && my >= p[1] && my < p[1] + p[3]) return entries.get(i);
		}
		return null;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		for (Hit h : toolbar) {
			if (!h.contains(mx, my)) continue;
			Mc.click();
			switch (h.kind) {
				case DONE -> dev.eagerlib.platform.Platform.closeCurrent();
				case GRID -> grid = !grid;
				case SNAP -> snap = !snap;
				case RESET -> {
					for (HudManager.Entry e : HudManager.entries()) e.reset();
					HudManager.save();
				}
				default -> {}
			}
			return true;
		}
		HudManager.Entry e = entryAt(mx, my);
		if (e == null) {
			selected = null;
			return false;
		}
		selected = e;
		if (button == 1) {
			e.enabled = !e.enabled;
			Mc.click();
			HudManager.save();
			return true;
		}
		int idx = HudManager.entries().indexOf(e);
		float[] p = boxes.get(idx);
		grabX = (float) mx - p[0];
		grabY = (float) my - p[1];
		dragging = e;
		return true;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (dragging == null) return false;
		int idx = HudManager.entries().indexOf(dragging);
		float[] p = boxes.get(idx);
		float w = p[2], h = p[3];
		float x = (float) mx - grabX, y = (float) my - grabY;
		guides.clear();
		if (snap) {
			float th = 4;
			float[] xs = {0, screenW / 2F - w / 2, screenW - w};
			float[] xg = {0, screenW / 2F, screenW};
			for (int i = 0; i < xs.length; i++) {
				if (Math.abs(x - xs[i]) < th) {
					x = xs[i];
					guides.add(new float[] {0, xg[i]});
					break;
				}
			}
			float[] ys = {0, screenH / 2F - h / 2, screenH - h};
			float[] yg = {0, screenH / 2F, screenH};
			for (int i = 0; i < ys.length; i++) {
				if (Math.abs(y - ys[i]) < th) {
					y = ys[i];
					guides.add(new float[] {1, yg[i]});
					break;
				}
			}
			List<HudManager.Entry> entries = HudManager.entries();
			for (int i = 0; i < boxes.size(); i++) {
				if (entries.get(i) == dragging || !entries.get(i).enabled) continue;
				float[] o = boxes.get(i);
				if (Math.abs(x - o[0]) < th) { x = o[0]; guides.add(new float[] {0, o[0]}); }
				else if (Math.abs(x + w - (o[0] + o[2])) < th) { x = o[0] + o[2] - w; guides.add(new float[] {0, o[0] + o[2]}); }
				if (Math.abs(y - (o[1] + o[3] + 2)) < th) { y = o[1] + o[3] + 2; guides.add(new float[] {1, o[1] + o[3] + 1}); }
				else if (Math.abs(y + h + 2 - o[1]) < th) { y = o[1] - h - 2; guides.add(new float[] {1, o[1] - 1}); }
			}
		}
		x = Math.max(0, Math.min(screenW - w, x));
		y = Math.max(0, Math.min(screenH - h, y));
		dragging.x = x / screenW;
		dragging.y = y / screenH;
		return true;
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (dragging == null) return false;
		dragging = null;
		guides.clear();
		HudManager.save();
		return true;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		HudManager.Entry e = entryAt(mx, my);
		if (e == null) return false;
		e.scale = Math.max(0.5F, Math.min(3F, Math.round((e.scale + (float) v * 0.1F) * 20) / 20F));
		HudManager.save();
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		if (selected == null) return false;
		float step = (modifiers & dev.eagerlib.platform.Platform.SHIFT_MOD) != 0 ? 10 : 1;
		float dx = 0, dy = 0;
		switch (key) {
			case InputConstants.KEY_LEFT -> dx = -step;
			case InputConstants.KEY_RIGHT -> dx = step;
			case InputConstants.KEY_UP -> dy = -step;
			case InputConstants.KEY_DOWN -> dy = step;
			default -> { return false; }
		}
		selected.x = Math.max(0, Math.min(1, selected.x + dx / screenW));
		selected.y = Math.max(0, Math.min(1, selected.y + dy / screenH));
		HudManager.save();
		return true;
	}

	@Override
	public void onClose() {
		HudManager.setEditing(false);
		HudManager.save();
	}

	@Override
	public boolean wantsBlur() { return false; }
}
