package dev.eagerlib.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;

import dev.eagerlib.api.BoolOption;
import dev.eagerlib.api.ButtonOption;
import dev.eagerlib.api.ColorOption;
import dev.eagerlib.api.EagerMod;
import dev.eagerlib.api.KeyOption;
import dev.eagerlib.api.Option;
import dev.eagerlib.api.Section;
import dev.eagerlib.api.TextOption;
import dev.eagerlib.core.EagerLib;
import dev.eagerlib.core.Mc;
import dev.eagerlib.core.Settings;
import dev.eagerlib.core.Teams;
import dev.eagerlib.platform.Platform;
import dev.eagerlib.render.Canvas;
import dev.eagerlib.render.Gfx;
import dev.eagerlib.render.Glyphs;
import dev.eagerlib.render.Icons;
import dev.eagerlib.render.Theme;

public final class EagerUi implements UiRoot {
	private static final class Page {
		final String key;
		final String title;
		final String subtitle;
		final String description;
		final EagerMod mod;
		final Glyphs.Glyph glyph;
		final Supplier<View> factory;
		final Runnable action;
		final boolean resettable;

		Page(String key, String title, String subtitle, String description, EagerMod mod, Glyphs.Glyph glyph, Supplier<View> factory, Runnable action, boolean resettable) {
			this.key = key;
			this.title = title;
			this.subtitle = subtitle;
			this.description = description;
			this.mod = mod;
			this.glyph = glyph;
			this.factory = factory;
			this.action = action;
			this.resettable = resettable;
		}
	}

	private static final int NAV = 1, SEARCH = 2, RESET = 3, LINK = 4, SCROLLBAR = 5, CLEAR_SEARCH = 6;

	private final Anim anim = new Anim();
	private final List<Object> nav = new ArrayList<>();
	private final Map<String, View> views = new HashMap<>();
	private final List<Hit> hits = new ArrayList<>();
	private Page page;
	private View view;
	private float uiScale = 1F;
	private float scroll;
	private float scrollTarget;
	private float contentH;
	private float bodyX, bodyY, bodyW, bodyH;
	private float sideX, sideY, sideW, sideH;
	private float sideScroll;
	private float sideTarget;
	private float sideContent;
	private TextEdit searchEdit;
	private String query = "";
	private View searchView;
	private long resetArmedUntil;
	private float pageAnim = 1F;
	private float openAnim;
	private boolean draggingScroll;
	private float viewShift;
	private float scrollGrabOffset;

	public EagerUi(String modId) {
		this(modId, null);
	}

	public EagerUi(String modId, String pageKey) {
		buildNav();
		String want = pageKey;
		if (want == null) {
			EagerMod target = modId == null ? null : EagerLib.find(modId);
			if (target != null && target.listed()) want = "mod:" + modId;
			else if (modId == null && Settings.REMEMBER_PAGE.get() && !Settings.LAST_PAGE.get().isEmpty()) want = Settings.LAST_PAGE.get();
		}
		Page first = null;
		Page fallback = null;
		for (Object o : nav) {
			if (o instanceof Page p && p.factory != null) {
				if (fallback == null) fallback = p;
				if (p.key.equals(want)) first = p;
			}
		}
		select(first != null ? first : fallback, false);
		if (!Anim.enabled()) openAnim = 1F;
	}

	private void buildNav() {
		nav.add("Mods");
		for (EagerMod m : EagerLib.listed()) {
			nav.add(new Page("mod:" + m.id(), m.name(), m.author().isEmpty() ? "" : "By " + m.author(), m.description(), m, null,
				() -> new OptionListView(groupsOf(m.sections()), anim, "This mod has no settings."), null, true));
		}
		nav.add("EagerLib");
		EagerMod self = Settings.MOD;
		nav.add(new Page("teams", "Teams", "EagerLib", "Group players into teams so mods can colour them for you.", null, Glyphs.TEAMS, this::teamsView, null, false));
		nav.add(new Page("appearance", "Appearance", "EagerLib", "Shape the liquid-glass look of this menu and the HUD.", self, Glyphs.APPEARANCE, () -> {
			List<Section> secs = new ArrayList<>();
			for (Section s : self.sections()) if (!s.title().equals("Controls") && !s.title().equals("Teams")) secs.add(s);
			return new OptionListView(groupsOf(secs), anim, "");
		}, null, true));
		nav.add(new Page("keybinds", "Key Binds", "EagerLib", "Every key bind from every mod in one place. Click a bind, then press a key. Backspace clears it.", null, Glyphs.KEYS, this::keysView, null, false));
		nav.add(new Page("changelog", "Changelog", "EagerLib", "What changed in each mod.", null, Glyphs.CHANGELOG, () -> {
			List<EagerMod> list = new ArrayList<>();
			list.add(self);
			list.addAll(EagerLib.listed());
			return new ChangelogView(list);
		}, null, false));
		nav.add(new Page("allmods", "All Mods", "EagerLib", "Every mod built on EagerLib. Click one to open its settings.", null, Glyphs.ALL_MODS,
			() -> new ModsGridView(EagerLib.listed(), this::openMod, anim), null, false));
		nav.add(new Page("hud", "Edit HUD", "EagerLib", "", null, Glyphs.HUD, null, () -> EagerLib.openHudEditor(Platform.screen()), false));
	}

	private static List<OptionListView.Group> groupsOf(List<Section> sections) {
		List<OptionListView.Group> out = new ArrayList<>();
		for (Section s : sections) out.add(new OptionListView.Group(s::title, new ArrayList<>(s.options())));
		return out;
	}

	private View keysView() {
		List<OptionListView.Group> out = new ArrayList<>();
		List<EagerMod> mods = new ArrayList<>(EagerLib.all());
		mods.remove(Settings.MOD);
		mods.add(0, Settings.MOD);
		for (EagerMod m : mods) {
			List<Option<?>> keys = new ArrayList<>();
			for (Option<?> o : m.options().values()) if (o instanceof KeyOption) keys.add(o);
			if (!keys.isEmpty()) out.add(new OptionListView.Group(m::name, keys));
		}
		return new OptionListView(out, anim, "No key binds registered.");
	}

	private View teamsView() {
		List<OptionListView.Group> out = new ArrayList<>();
		BoolOption enabled = new BoolOption("team_colors", "Team Colours", Settings.TEAM_COLORS.get());
		enabled.describe("Let mods colour players by the teams below.");
		enabled.onChange(v -> Settings.TEAM_COLORS.set(v));
		ButtonOption add = new ButtonOption("add", "New Team", "+ Add Team", false, () -> {
			Teams.add();
			rebuild("teams");
		});
		out.add(new OptionListView.Group(() -> "Teams", List.of(enabled, add)));
		for (Teams.Team team : new ArrayList<>(Teams.all())) {
			TextOption name = new TextOption("name", "Name", team.name, 32);
			name.onChange(v -> { team.name = v; Teams.save(); });
			ColorOption color = new ColorOption("color", "Colour", team.color, false);
			color.onChange(v -> { team.color = v; Teams.save(); });
			TextOption members = new TextOption("members", "Members", team.membersText(), 512).placeholder("Player names, separated by commas");
			members.onChange(v -> { team.setMembers(v); Teams.save(); });
			ButtonOption remove = new ButtonOption("remove", "Remove Team", "Remove", true, () -> {
				Teams.remove(team);
				rebuild("teams");
			});
			out.add(new OptionListView.Group(() -> team.name.isBlank() ? "Unnamed Team" : team.name, List.of(name, color, members, remove)));
		}
		return new OptionListView(out, anim, "");
	}

	private void rebuild(String key) {
		views.remove(key);
		if (page != null && page.key.equals(key)) view = viewFor(page);
	}

	private View viewFor(Page p) {
		return views.computeIfAbsent(p.key, k -> p.factory.get());
	}

	private void openMod(EagerMod m) {
		for (Object o : nav) if (o instanceof Page p && p.mod == m && p.key.startsWith("mod:")) select(p, true);
	}

	private void select(Page p, boolean animate) {
		if (p == null) return;
		if (p.action != null) {
			p.action.run();
			return;
		}
		if (p == page && query.isEmpty()) return;
		if (view != null) view.blur();
		clearSearch();
		page = p;
		view = viewFor(p);
		scroll = scrollTarget = 0;
		pageAnim = animate && Anim.enabled() ? 0F : 1F;
		resetArmedUntil = 0;
		Settings.LAST_PAGE.set(p.key);
	}

	private void clearSearch() {
		query = "";
		searchView = null;
		searchEdit = null;
	}

	private void updateQuery(String q) {
		if (q.equals(query)) return;
		query = q;
		scroll = scrollTarget = 0;
		if (q.isBlank()) {
			searchView = null;
			return;
		}
		List<OptionListView.Group> out = new ArrayList<>();
		List<EagerMod> mods = new ArrayList<>(EagerLib.listed());
		mods.add(Settings.MOD);
		for (EagerMod m : mods) {
			for (Section s : m.sections()) {
				List<Option<?>> match = new ArrayList<>();
				for (Option<?> o : s.options()) if (o.isVisible() && o.matches(q.trim())) match.add(o);
				if (!match.isEmpty()) {
					String title = (m == Settings.MOD ? "Appearance" : m.name()) + "  ›  " + s.title();
					out.add(new OptionListView.Group(() -> title, match));
				}
			}
		}
		searchView = new OptionListView(out, anim, "Nothing matches \"" + q.trim() + "\"");
	}

	private View activeView() { return searchView != null ? searchView : view; }

	private float menuScale(int gui, int guiW, int guiH) {
		int want = Settings.MENU_SCALE.get().pixels;
		if (want == 0) return 1F;
		if (want < 0) {
			int fbW = guiW * gui, fbH = guiH * gui;
			want = Math.max(1, Math.min(gui, (int) Math.floor(Math.min(fbW / 640F, fbH / 360F))));
		}
		return want / (float) gui;
	}

	@Override
	public void render(Canvas canvas, int mouseX, int mouseY, float delta) {
		Gfx g = new Gfx(canvas);
		Theme t = Theme.current();
		g.textShadow(t.textShadow);
		float dt = anim.frame();
		uiScale = menuScale(g.guiScale(), canvas.guiWidth(), canvas.guiHeight());
		float W = canvas.guiWidth() / uiScale, H = canvas.guiHeight() / uiScale;
		int mx = Math.round(mouseX / uiScale), my = Math.round(mouseY / uiScale);
		openAnim = anim.step(openAnim, 1F, 9F);
		pageAnim = anim.step(pageAnim, 1F, 12F);
		hits.clear();

		g.pushScale(uiScale);
		g.rect(0, 0, W, H, Theme.alpha(0x000000, t.dim * 0.85F));

		float oa = Anim.ease(openAnim);
		g.alpha(oa);
		float margin = W < 520 ? 6 : 14;
		float ww = Math.min(W - margin * 2, 880), wh = Math.min(H - margin * 2, 540);
		float wx = (W - ww) / 2, wy = (H - wh) / 2 + (1 - oa) * 12;
		float R = Math.max(3, t.radius * 1.4F);
		g.glass(wx, wy, ww, wh, R, t, 1F);

		sideW = Math.min(Settings.SIDEBAR_WIDTH.getFloat(), ww * 0.36F);
		sideX = wx + 6;
		sideY = wy + 6;
		sideH = wh - 12;
		g.round(sideX, sideY, sideW, sideH, Math.max(2, R - 3), Theme.scaleAlpha(t.glass, 0.75F * t.glassOpacity));
		if (t.border > 0.01F) g.outline(sideX, sideY, sideW, sideH, Math.max(2, R - 3), Theme.alpha(0xFFFFFF, 0.14F * t.border));
		renderSidebar(g, t, mx, my);

		float cx = sideX + sideW + 12, cy = wy + 10, cw = wx + ww - 10 - cx;
		renderHeader(g, t, cx, cy, cw, mx, my);

		bodyX = cx;
		bodyY = cy + 42;
		bodyW = cw - 8;
		bodyH = wy + wh - 8 - bodyY;
		g.rect(cx, bodyY - 6, cw, 0.5F, Theme.alpha(t.text & 0xFFFFFF, 0.10F));
		View v = activeView();
		contentH = v.layout(g, t, bodyW);
		float maxScroll = Math.max(0, contentH - bodyH);
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
		scroll = Settings.SMOOTH_SCROLL.get() ? anim.step(scroll, scrollTarget, 28F) : scrollTarget;
		scroll = Math.max(0, Math.min(maxScroll, scroll));
		float pa = Anim.ease(pageAnim);
		g.alpha(oa * pa);
		boolean inBody = mx >= bodyX && mx < bodyX + bodyW && my >= bodyY && my < bodyY + bodyH;
		viewShift = g.snap(-scroll + (1 - pa) * 10);
		g.scissor(bodyX - 4, bodyY, bodyW + 8, bodyH);
		g.pushTranslate(0, viewShift);
		v.clip(bodyY - viewShift, bodyY + bodyH - viewShift);
		v.render(g, t, bodyX, bodyY, bodyW, mx, Math.round(my - viewShift), inBody && !draggingScroll);
		g.popTranslate(0, viewShift);
		g.noScissor();
		g.alpha(oa);
		if (maxScroll > 0) {
			float th = Math.max(18, bodyH * bodyH / contentH);
			float ty = bodyY + (bodyH - th) * (scroll / maxScroll);
			float sx = cx + cw - 4;
			boolean sh = draggingScroll || (mx >= sx - 3 && mx < sx + 6 && my >= bodyY && my < bodyY + bodyH);
			g.round(sx, bodyY, 3, bodyH, 1.5F, Theme.alpha(t.text & 0xFFFFFF, 0.06F));
			g.round(sx, ty, 3, th, 1.5F, sh ? t.accent : Theme.alpha(t.text & 0xFFFFFF, 0.35F));
			hits.add(new Hit(sx - 3, bodyY, 9, bodyH, null, SCROLLBAR, 0));
		}
		g.alpha(1F);
		g.popScale();
		Gfx.endFrame();
	}

	private void renderSidebar(Gfx g, Theme t, int mx, int my) {
		sideScroll = Settings.SMOOTH_SCROLL.get() ? anim.step(sideScroll, sideTarget, 28F) : sideTarget;
		g.scissor(sideX, sideY + 2, sideW, sideH - 18);
		float shift = g.snap(-sideScroll);
		g.pushTranslate(0, shift);
		float y = sideY + 8;
		float start = y;
		boolean icons = Settings.MOD_ICONS.get();
		for (Object o : nav) {
			if (o instanceof String header) {
				if (y > start) y += 6;
				g.text(header, sideX + 10, y, t.textDim);
				y += 13;
				continue;
			}
			Page p = (Page) o;
			float ix = sideX + 4, iw = sideW - 8, ih = 18;
			boolean selected = p == page && searchView == null;
			float sy = y + shift;
			boolean hover = mx >= ix && mx < ix + iw && my >= sy && my < sy + ih && my >= sideY && my < sideY + sideH - 16;
			float sa = anim.to(p, selected ? 1F : 0F, 14F);
			float ha = anim.to(p.key + "#h", hover ? 1F : 0F, 18F);
			if (ha > 0.01F) g.round(ix, y, iw, ih, 5, Theme.alpha(t.text & 0xFFFFFF, 0.07F * ha));
			if (sa > 0.01F) {
				g.round(ix, y, iw, ih, 5, Theme.alpha(t.accent & 0xFFFFFF, 0.30F * sa));
				g.outline(ix, y, iw, ih, 5, Theme.alpha(t.accent & 0xFFFFFF, 0.55F * sa));
			}
			float tx = sideX + 10;
			if (icons) {
				if (p.mod != null && p.key.startsWith("mod:")) {
					Icons.Icon icon = Icons.forMod(p.mod.id(), p.mod.iconPath());
					if (icon != null) g.icon(icon, tx, y + 3, 12);
					else {
						g.round(tx, y + 3, 12, 12, 3, t.accent);
						g.textCentered(p.title.substring(0, 1), tx + 6.5F, y + 5, t.onAccent());
					}
				} else if (p.glyph != null) {
					g.glyph(p.glyph, tx, y + 3, 1F, selected ? t.text : Theme.mix(t.textDim, t.text, ha));
				}
				tx += 17;
			}
			g.text(g.ellipsize(p.title, sideX + sideW - 8 - tx), tx, y + 5, selected ? t.text : Theme.mix(t.textDim, t.text, Math.max(ha, 0.55F)));
			if (my >= sideY && my < sideY + sideH - 16) hits.add(new Hit(ix, sy, iw, ih, p, NAV, 0));
			y += ih + 2;
		}
		sideContent = y - sideY;
		g.popTranslate(0, shift);
		g.noScissor();
		float maxSide = Math.max(0, sideContent - (sideH - 18));
		sideTarget = Math.max(0, Math.min(maxSide, sideTarget));
		String version = Settings.MOD.version();
		int plus = version.indexOf('+');
		String footer = "EagerLib " + (plus > 0 ? version.substring(0, plus) : version);
		g.text(g.ellipsize(footer, sideW - 16), sideX + 10, sideY + sideH - 13, t.textFaint);
	}

	private void renderHeader(Gfx g, Theme t, float cx, float cy, float cw, int mx, int my) {
		boolean searching = searchView != null;
		String title = searching ? "Search" : page.title;
		String sub = searching ? "" : page.subtitle;
		String desc = searching ? "Results from every mod" : page.description;

		float searchW = Math.min(132, cw * 0.30F), sh = 16;
		float sx = cx + cw - searchW, sy = cy + 2;
		boolean focused = searchEdit != null;
		float fa = anim.to("search#f", focused ? 1F : 0F, 16F);
		g.round(sx, sy, searchW, sh, sh / 2, Theme.mix(Theme.alpha(t.text & 0xFFFFFF, 0.08F), Theme.alpha(t.accent & 0xFFFFFF, 0.18F), fa));
		g.outline(sx, sy, searchW, sh, sh / 2, Theme.mix(Theme.alpha(0xFFFFFF, 0.16F), t.accent, fa));
		g.glyph(Glyphs.SEARCH, sx + 6, sy + 3, 1F, t.textDim);
		g.scissor(sx + 18, sy, searchW - 30, sh);
		if (focused) {
			String v = searchEdit.value();
			float over = Math.max(0, g.width(v) - (searchW - 34));
			g.text(v, sx + 20 - over, sy + 4, t.text);
			if (searchEdit.caretVisible()) g.rect(sx + 20 - over + g.width(v.substring(0, searchEdit.caret)), sy + 3, 0.75F, 10, t.accent);
		} else {
			g.text(query.isEmpty() ? "Search" : query, sx + 20, sy + 4, query.isEmpty() ? t.textFaint : t.text);
		}
		g.noScissor();
		hits.add(new Hit(sx, sy, searchW, sh, null, SEARCH, 0));
		if (!query.isEmpty()) {
			g.text("x", sx + searchW - 11, sy + 4, t.textDim);
			hits.add(new Hit(sx + searchW - 14, sy, 14, sh, null, CLEAR_SEARCH, 0));
		}

		float rightEdge = sx - 8;
		if (!searching && page.resettable) {
			boolean armed = System.currentTimeMillis() < resetArmedUntil;
			String label = armed ? "Confirm?" : "Reset";
			float rw = g.width(label) + 24, rx = sx - 8 - rw;
			boolean h = mx >= rx && mx < rx + rw && my >= sy && my < sy + sh;
			float ha = anim.to("reset#h", h ? 1F : 0F, 18F);
			int base = armed ? 0xFFE5484D : Theme.alpha(t.text & 0xFFFFFF, 0.08F + 0.08F * ha);
			g.round(rx, sy, rw, sh, sh / 2, base);
			g.outline(rx, sy, rw, sh, sh / 2, armed ? 0xFFFF8A8E : Theme.alpha(0xFFFFFF, 0.16F));
			g.glyph(Glyphs.RESET, rx + 6, sy + 3, 1F, armed ? 0xFFFFFFFF : t.textDim);
			g.text(label, rx + 19, sy + 4, armed ? 0xFFFFFFFF : t.text);
			hits.add(new Hit(rx, sy, rw, sh, null, RESET, 0));
			rightEdge = rx - 8;
		}

		float x = cx;
		if (!searching && page.mod != null && page.key.startsWith("mod:")) {
			Icons.Icon icon = Icons.forMod(page.mod.id(), page.mod.iconPath());
			if (icon != null) {
				g.icon(icon, x, cy, 18);
				x += 24;
			}
		} else if (!searching && page.glyph != null) {
			g.glyph(page.glyph, x + 1, cy + 3, 1F, t.accent);
			x += 18;
		}
		float titleScale = g.width(title) * 2 + 40 < rightEdge - x ? 2F : 1F;
		String shownTitle = g.ellipsize(titleScale > 1 ? title : "\u00a7l" + title, Math.max(20, (rightEdge - x) / titleScale - 4));
		g.textScaled(shownTitle, x, titleScale > 1 ? cy : cy + 5, titleScale, t.text);
		float after = x + g.width(shownTitle) * titleScale + 6;
		if (!sub.isEmpty() && after + g.width(sub) < rightEdge) {
			g.text(sub, after, cy + 7, t.textDim);
			after += g.width(sub) + 8;
		}
		if (!searching && page.mod != null && page.key.startsWith("mod:")) {
			after = link(g, t, page.mod.sourceUrl(), Glyphs.CODE, after, cy + 3, rightEdge, mx, my);
			link(g, t, page.mod.websiteUrl(), Glyphs.GLOBE, after, cy + 3, rightEdge, mx, my);
		}
		if (desc != null && !desc.isEmpty()) {
			g.text(g.ellipsize(desc, cw), cx, cy + 22, t.textDim);
		}
	}

	private float link(Gfx g, Theme t, String url, Glyphs.Glyph glyph, float x, float y, float limit, int mx, int my) {
		if (url == null || url.isBlank() || x + 14 > limit) return x;
		boolean h = mx >= x && mx < x + 14 && my >= y && my < y + 14;
		float ha = anim.to(url, h ? 1F : 0F, 18F);
		g.circle(x + 7, y + 7, 7, Theme.mix(Theme.alpha(t.text & 0xFFFFFF, 0.10F), Theme.alpha(t.accent & 0xFFFFFF, 0.5F), ha));
		g.outline(x, y, 14, 14, 7, Theme.alpha(0xFFFFFF, 0.18F));
		g.glyph(glyph, x + 2, y + 2, 1F, t.text);
		hits.add(new Hit(x, y, 14, 14, url, LINK, 0));
		return x + 18;
	}

	@Override
	public boolean mouseClicked(double rx, double ry, int button) {
		double mx = rx / uiScale, my = ry / uiScale;
		Hit hit = null;
		for (int i = hits.size() - 1; i >= 0; i--) {
			if (hits.get(i).contains(mx, my)) {
				hit = hits.get(i);
				break;
			}
		}
		if (searchEdit != null && (hit == null || hit.kind != SEARCH)) searchEdit = null;
		if (hit != null) {
			switch (hit.kind) {
				case NAV -> {
					activeView().blur();
					Mc.click();
					select((Page) hit.target, true);
					return true;
				}
				case SEARCH -> {
					activeView().blur();
					if (searchEdit == null) searchEdit = new TextEdit(query, 48, cp -> true);
					return true;
				}
				case CLEAR_SEARCH -> {
					clearSearch();
					Mc.click();
					return true;
				}
				case RESET -> {
					if (System.currentTimeMillis() < resetArmedUntil) {
						resetArmedUntil = 0;
						if (page.mod != null) {
							if (page.key.equals("appearance")) {
								for (Section s : page.mod.sections()) {
									if (s.title().equals("Controls")) continue;
									for (Option<?> o : s.options()) if (o.persistent() && o.isVisible()) o.reset();
								}
							} else {
								page.mod.resetAll();
							}
						}
					} else {
						resetArmedUntil = System.currentTimeMillis() + 3000;
					}
					Mc.click();
					return true;
				}
				case LINK -> {
					Mc.click();
					Mc.openLink((String) hit.target);
					return true;
				}
				case SCROLLBAR -> {
					draggingScroll = true;
					dragScroll(my);
					return true;
				}
				default -> {}
			}
		}
		if (mx >= bodyX - 4 && mx < bodyX + bodyW + 4 && my >= bodyY && my < bodyY + bodyH) {
			return activeView().click(mx, my - viewShift, button);
		}
		activeView().blur();
		return false;
	}

	private void dragScroll(double my) {
		float maxScroll = Math.max(0, contentH - bodyH);
		float th = Math.max(18, bodyH * bodyH / Math.max(1, contentH));
		float f = (float) ((my - bodyY - th / 2) / Math.max(1, bodyH - th));
		scrollTarget = scroll = Math.max(0, Math.min(1, f)) * maxScroll;
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		boolean was = draggingScroll;
		draggingScroll = false;
		return activeView().release(x / uiScale, y / uiScale - viewShift, button) || was;
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
		if (draggingScroll) {
			dragScroll(y / uiScale);
			return true;
		}
		return activeView().drag(x / uiScale, y / uiScale - viewShift, button);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double h, double v) {
		double mx = x / uiScale, my = y / uiScale;
		float amount = (float) v * 48F * Settings.SCROLL_SPEED.getFloat();
		if (mx >= sideX && mx < sideX + sideW && my >= sideY && my < sideY + sideH) {
			sideTarget -= amount;
			return true;
		}
		scrollTarget -= amount;
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		if (searchEdit != null) {
			if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
				searchEdit = null;
				return true;
			}
			searchEdit.key(key, modifiers);
			updateQuery(searchEdit.value());
			return true;
		}
		View v = activeView();
		if (v.capturing()) return v.key(key, modifiers);
		boolean shortcut = (modifiers & dev.eagerlib.platform.Platform.SHORTCUT_MODS) != 0;
		if (shortcut && key == InputConstants.KEY_F) {
			searchEdit = new TextEdit(query, 48, cp -> true);
			return true;
		}
		if (key == InputConstants.KEY_ESCAPE && !query.isEmpty()) {
			clearSearch();
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(int codepoint, int modifiers) {
		if (searchEdit != null) {
			searchEdit.typed(codepoint);
			updateQuery(searchEdit.value());
			return true;
		}
		return activeView().typed(codepoint);
	}

	@Override
	public void onClose() {
		activeView().blur();
		EagerLib.saveAll();
	}

	@Override
	public boolean pausesGame() { return Settings.PAUSE_GAME.get(); }

	@Override
	public boolean wantsBlur() { return Settings.BLUR.get(); }
}
