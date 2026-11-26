package dev.eagerlib.render;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import dev.eagerlib.core.Mc;

public final class Gfx {
	private static int fills;
	private static int lastFills;

	public static int lastFrameFills() { return lastFills; }

	public static void endFrame() {
		lastFills = fills;
		fills = 0;
	}

	public final Canvas c;
	public final Font font;
	private final int gui;
	private float scale = 1F;
	private boolean antialias = true;
	private final Deque<Float> scales = new ArrayDeque<>();
	private float offX;
	private float offY;
	private float alpha = 1F;
	private boolean textShadow = true;

	public Gfx(Canvas canvas) {
		this.c = canvas;
		this.font = Minecraft.getInstance().font;
		this.gui = Math.max(1, Mc.guiScale());
	}

	private void fill(int x1, int y1, int x2, int y2, int argb) {
		fills++;
		c.fill(x1, y1, x2, y2, argb);
	}

	private void gradientFill(int x1, int y1, int x2, int y2, int top, int bottom) {
		fills++;
		c.gradient(x1, y1, x2, y2, top, bottom);
	}

	public float pixelRatio() { return gui * scale; }

	public float scale() { return scale; }

	public int guiScale() { return gui; }

	public void textShadow(boolean on) { this.textShadow = on; }

	public float alpha() { return alpha; }

	public void alpha(float a) { this.alpha = Math.max(0, Math.min(1, a)); }

	public void pushScale(float s) {
		c.push();
		c.scale(s);
		scales.push(scale);
		scale *= s;
	}

	public float snap(float v) {
		float pr = pixelRatio();
		return Math.round(v * pr) / pr;
	}

	public void pushTranslate(float dx, float dy) {
		c.push();
		c.translate(dx, dy);
		offX += dx;
		offY += dy;
	}

	public void popTranslate(float dx, float dy) {
		c.pop();
		offX -= dx;
		offY -= dy;
	}

	public void popScale() {
		c.pop();
		scale = scales.isEmpty() ? 1F : scales.pop();
	}

	public int col(int argb) {
		if (alpha >= 0.999F) return argb;
		int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
		return (a << 24) | (argb & 0xFFFFFF);
	}

	private int px(float v) { return Math.round(v * pixelRatio()); }

	private void beginPx() {
		c.push();
		c.scale(1F / pixelRatio());
	}

	private void endPx() { c.pop(); }

	public void rect(float x, float y, float w, float h, int argb) {
		if (w <= 0 || h <= 0 || ((argb >>> 24) == 0)) return;
		int color = col(argb);
		beginPx();
		int x1 = px(x), y1 = px(y);
		fill(x1, y1, px(x + w), px(y + h), color);
		endPx();
	}

	public void round(float x, float y, float w, float h, float r, int argb) {
		roundGradient(x, y, w, h, r, argb, argb);
	}

	public void roundGradient(float x, float y, float w, float h, float r, int top, int bottom) {
		if (w <= 0 || h <= 0) return;
		int ct = col(top), cb = col(bottom);
		if ((ct >>> 24) == 0 && (cb >>> 24) == 0) return;
		beginPx();
		int x1 = px(x), y1 = px(y);
		int pw = px(x + w) - x1, ph = px(y + h) - y1;
		roundPx(x1, y1, pw, ph, r * pixelRatio(), ct, cb);
		endPx();
	}

	private static int lerpColor(int a, int b, float t) {
		if (a == b) return a;
		int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}

	private static int withCov(int argb, float cov) {
		return (Math.round((argb >>> 24) * cov) << 24) | (argb & 0xFFFFFF);
	}

	private void roundPx(int x, int y, int w, int h, float r, int top, int bottom) {
		if (w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, (float) Math.floor(Math.min(w, h) / 2F)));
		if (r < 0.75F) {
			if (top == bottom) fill(x, y, x + w, y + h, top);
			else gradientFill(x, y, x + w, y + h, top, bottom);
			return;
		}
		Corners.Mask m = Corners.fill(r);
		int n = m.size;
		if (h - 2 * n > 0) {
			int ca = lerpColor(top, bottom, n / (float) h);
			int cbm = lerpColor(top, bottom, (h - n) / (float) h);
			if (ca == cbm) fill(x, y + n, x + w, y + h - n, ca);
			else gradientFill(x, y + n, x + w, y + h - n, ca, cbm);
		}
		for (int j = 0; j < n; j++) {
			int rowTop = y + j;
			int rowBot = y + h - 1 - j;
			cornerRow(m, j, x, w, rowTop, lerpColor(top, bottom, (j + 0.5F) / h));
			if (rowBot != rowTop) cornerRow(m, j, x, w, rowBot, lerpColor(top, bottom, (h - j - 0.5F) / h));
		}
	}

	private void cornerRow(Corners.Mask m, int j, int x, int w, int py, int color) {
		int fs = m.fullStart[j];
		if (w - 2 * fs > 0) fill(x + fs, py, x + w - fs, py + 1, color);
		int es = m.edgeStart[j];
		if (!antialias || es >= fs || m.edgeCov[j] <= 0.02F) return;
		int pc = withCov(color, m.edgeCov[j]);
		fill(x + es, py, x + fs, py + 1, pc);
		fill(x + w - fs, py, x + w - es, py + 1, pc);
	}

	public void outline(float x, float y, float w, float h, float r, int argb) {
		if (w <= 0 || h <= 0) return;
		int color = col(argb);
		if ((color >>> 24) == 0) return;
		beginPx();
		int x1 = px(x), y1 = px(y);
		int pw = px(x + w) - x1, ph = px(y + h) - y1;
		int t = Math.max(1, Math.round(pixelRatio() / 2F));
		float pr = Math.max(0, Math.min(r * pixelRatio(), (float) Math.floor(Math.min(pw, ph) / 2F)));
		if (pr < 0.75F) {
			fill(x1, y1, x1 + pw, y1 + t, color);
			fill(x1, y1 + ph - t, x1 + pw, y1 + ph, color);
			fill(x1, y1 + t, x1 + t, y1 + ph - t, color);
			fill(x1 + pw - t, y1 + t, x1 + pw, y1 + ph - t, color);
		} else {
			Corners.Mask m = Corners.ring(pr, t);
			int n = m.size;
			fill(x1 + n, y1, x1 + pw - n, y1 + t, color);
			fill(x1 + n, y1 + ph - t, x1 + pw - n, y1 + ph, color);
			fill(x1, y1 + n, x1 + t, y1 + ph - n, color);
			fill(x1 + pw - t, y1 + n, x1 + pw, y1 + ph - n, color);
			for (int j = 0; j < n; j++) {
				int es = m.edgeStart[j], ee = m.fullStart[j];
				if (es >= ee || m.edgeCov[j] <= 0.02F) continue;
				int pc = withCov(color, m.edgeCov[j]);
				fill(x1 + es, y1 + j, x1 + ee, y1 + j + 1, pc);
				fill(x1 + pw - ee, y1 + j, x1 + pw - es, y1 + j + 1, pc);
				fill(x1 + es, y1 + ph - 1 - j, x1 + ee, y1 + ph - j, pc);
				fill(x1 + pw - ee, y1 + ph - 1 - j, x1 + pw - es, y1 + ph - j, pc);
			}
		}
		endPx();
	}

	public void circle(float cx, float cy, float radius, int argb) {
		round(cx - radius, cy - radius, radius * 2, radius * 2, radius, argb);
	}

	public void softCircle(float cx, float cy, float radius, int argb) {
		antialias = false;
		circle(cx, cy, radius, argb);
		antialias = true;
	}

	public void shadow(float x, float y, float w, float h, float r, float strength) {
		if (strength <= 0.01F) return;
		antialias = false;
		int a1 = Math.round(strength * 14F);
		int a2 = Math.round(strength * 20F);
		round(x - 4F, y - 2.5F, w + 8F, h + 8F, r + 4F, a1 << 24);
		round(x - 1.5F, y, w + 3F, h + 3.5F, r + 1.5F, a2 << 24);
		antialias = true;
	}

	public void glass(float x, float y, float w, float h, float r, Theme t, float weight) {
		if (t.shadows) shadow(x, y, w, h, r, t.shadowStrength * weight);
		int tint = Theme.scaleAlpha(t.glass, t.glassOpacity * weight);
		roundGradient(x, y, w, h, r, Theme.brighten(tint, 0.10F * t.highlight), tint);
		if (t.highlight > 0.01F) {
			float hh = Math.min(h * 0.45F, 40F);
			roundGradient(x, y, w, hh, r, Theme.alpha(0xFFFFFF, 0.10F * t.highlight * weight), Theme.alpha(0xFFFFFF, 0));
		}
		if (t.border > 0.01F) outline(x, y, w, h, r, Theme.alpha(0xFFFFFF, 0.28F * t.border * weight));
	}

	public int width(String s) { return font.width(s); }

	public int lineHeight() { return font.lineHeight; }

	public void text(String s, float x, float y, int argb) {
		text(s, x, y, argb, textShadow);
	}

	public void text(String s, float x, float y, int argb, boolean shadow) {
		if (s == null || s.isEmpty()) return;
		int color = col(argb);
		int a = color >>> 24;
		if (a < 8) return;
		c.text(s, Math.round(x), Math.round(y), color, shadow);
	}

	public void textCentered(String s, float cx, float y, int argb) {
		text(s, cx - width(s) / 2F, y, argb);
	}

	public void textScaled(String s, float x, float y, float s2, int argb) {
		pushScale(s2);
		text(s, x / s2, y / s2, argb);
		popScale();
	}

	public String ellipsize(String s, float maxWidth) {
		if (width(s) <= maxWidth) return s;
		String e = "...";
		int n = s.length();
		while (n > 0 && width(s.substring(0, n) + e) > maxWidth) n--;
		return s.substring(0, n) + e;
	}

	public java.util.List<String> wrap(String s, float maxWidth) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		if (s == null || s.isEmpty()) return lines;
		for (String para : s.split("\n")) {
			StringBuilder line = new StringBuilder();
			for (String word : para.split(" ")) {
				String trial = line.length() == 0 ? word : line + " " + word;
				if (width(trial) <= maxWidth || line.length() == 0) {
					line.setLength(0);
					line.append(trial);
				} else {
					lines.add(line.toString());
					line.setLength(0);
					line.append(word);
				}
			}
			lines.add(line.toString());
		}
		return lines;
	}

	public void scissor(float x, float y, float w, float h) {
		float ax = x + offX, ay = y + offY;
		c.scissor((int) Math.floor(ax * scale), (int) Math.floor(ay * scale), (int) Math.ceil((ax + w) * scale), (int) Math.ceil((ay + h) * scale));
	}

	public void noScissor() { c.noScissor(); }

	public void layer() { c.layer(); }

	public void glyph(Glyphs.Glyph glyph, float x, float y, float unit, int argb) {
		int color = col(argb);
		if ((color >>> 24) == 0) return;
		beginPx();
		float p = pixelRatio() * unit;
		for (int row = 0; row < glyph.rows.length; row++) {
			String line = glyph.rows[row];
			int k = 0;
			while (k < line.length()) {
				if (line.charAt(k) != '#') {
					k++;
					continue;
				}
				int start = k;
				while (k < line.length() && line.charAt(k) == '#') k++;
				int gx1 = Math.round(x * pixelRatio() + start * p);
				int gx2 = Math.round(x * pixelRatio() + k * p);
				int gy1 = Math.round(y * pixelRatio() + row * p);
				int gy2 = Math.round(y * pixelRatio() + (row + 1) * p);
				fill(gx1, gy1, gx2, gy2, color);
			}
		}
		endPx();
	}

	public void icon(Icons.Icon icon, float x, float y, float size) {
		if (icon == null) return;
		int sizePx = Math.max(1, Math.round(size * pixelRatio()));
		Icons.Scaled sc = icon.scaled(sizePx);
		float k = sizePx / (float) sc.res;
		beginPx();
		int ox = px(x), oy = px(y);
		for (int i = 0; i < sc.runs.length; i += 4) {
			int color = col(sc.runs[i + 3]);
			if ((color >>> 24) == 0) continue;
			int row = sc.runs[i + 1];
			fill(ox + Math.round(sc.runs[i] * k), oy + Math.round(row * k), ox + Math.round(sc.runs[i + 2] * k), oy + Math.round((row + 1) * k), color);
		}
		endPx();
	}
}
