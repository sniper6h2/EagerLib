package dev.eagerlib.core;

import com.mojang.blaze3d.platform.InputConstants;

import dev.eagerlib.api.BoolOption;
import dev.eagerlib.api.ChoiceOption;
import dev.eagerlib.api.ColorOption;
import dev.eagerlib.api.EagerMod;
import dev.eagerlib.api.EagerRegistry;
import dev.eagerlib.api.KeyOption;
import dev.eagerlib.api.NumberOption;
import dev.eagerlib.api.Section;
import dev.eagerlib.api.TextOption;

public final class Settings {
	public enum Preset {
		LIQUID_GLASS("Liquid Glass", 0x3D8BFF, 0x2EFFFFFF, 0xFFFFFF, 30),
		FROST("Frost", 0x0A84FF, 0x8CF2F5FA, 0x15181E, 8),
		MIDNIGHT("Midnight", 0x8E7CFF, 0x8C14141E, 0xFFFFFF, 55),
		AURORA("Aurora", 0x2EE6A6, 0x3388FFDD, 0xFFFFFF, 35),
		SUNSET("Sunset", 0xFF7A59, 0x38FFB08A, 0xFFFFFF, 35),
		CUSTOM("Custom", 0, 0, 0, 0);

		final String label;
		final int accent;
		final int glass;
		final int text;
		final int dim;

		Preset(String label, int accent, int glass, int text, int dim) {
			this.label = label;
			this.accent = accent;
			this.glass = glass;
			this.text = text;
			this.dim = dim;
		}
	}

	public enum Density {
		COMPACT("Compact", 18), COMFORTABLE("Comfortable", 22), SPACIOUS("Spacious", 28);

		final String label;
		public final int rowHeight;

		Density(String label, int rowHeight) {
			this.label = label;
			this.rowHeight = rowHeight;
		}
	}

	public enum MenuScale {
		AUTO("Auto (fit window)", -1), GUI("Match GUI Scale", 0), S1("1x", 1), S2("2x", 2), S3("3x", 3), S4("4x", 4);

		final String label;
		public final int pixels;

		MenuScale(String label, int pixels) {
			this.label = label;
			this.pixels = pixels;
		}
	}

	public static EagerMod MOD;
	public static ChoiceOption<Preset> PRESET;
	public static ColorOption ACCENT;
	public static ColorOption GLASS_TINT;
	public static ColorOption TEXT_COLOR;
	public static NumberOption GLASS_OPACITY;
	public static NumberOption HIGHLIGHT;
	public static NumberOption BORDER;
	public static NumberOption RADIUS;
	public static BoolOption SHADOWS;
	public static NumberOption SHADOW_STRENGTH;
	public static BoolOption BLUR;
	public static NumberOption DIM;
	public static ChoiceOption<MenuScale> MENU_SCALE;
	public static NumberOption SIDEBAR_WIDTH;
	public static ChoiceOption<Density> DENSITY;
	public static BoolOption DESCRIPTIONS;
	public static BoolOption MOD_ICONS;
	public static BoolOption REMEMBER_PAGE;
	public static BoolOption TEXT_SHADOW;
	public static BoolOption ANIMATIONS;
	public static NumberOption ANIM_SPEED;
	public static BoolOption SMOOTH_SCROLL;
	public static NumberOption SCROLL_SPEED;
	public static BoolOption CLICK_SOUNDS;
	public static BoolOption PAUSE_GAME;
	public static BoolOption HUD_BACKGROUND;
	public static ColorOption HUD_BG_COLOR;
	public static BoolOption HUD_SHADOW;
	public static NumberOption HUD_PADDING;
	public static BoolOption TEAM_COLORS;
	public static KeyOption OPEN_MENU;
	public static TextOption LAST_PAGE;

	private static boolean applying;

	private Settings() {}

	static void register(EagerRegistry registry) {
		EagerMod.Builder b = registry.mod(EagerLib.ID)
			.name("EagerLib")
			.author("Eager")
			.description("The shared menu, key binds, HUD editor and theme for every Eager mod.")
			.icon("assets/eagerlib/icon.png")
			.unlisted()
			.changelog("1.0.0", "2026-11-26",
				"Liquid-glass settings menu shared by every Eager mod",
				"Every key bind from every mod in one place",
				"Drag-and-drop HUD editor with snapping and per-element scale",
				"Teams, changelogs, mod overview and a theme engine with presets");

		Section theme = b.section("Theme");
		PRESET = theme.choice("preset", "Preset", Preset.LIQUID_GLASS, p -> p.label)
			.describe("Pick a starting look. Changing any colour below switches this to Custom.");
		ACCENT = theme.color("accent", "Accent Colour", 0x3D8BFF).describe("Toggles, sliders, selections and highlights.");
		GLASS_TINT = theme.colorAlpha("glass_tint", "Glass Tint", 0x2EFFFFFF).describe("Colour and see-through amount of every glass panel.");
		TEXT_COLOR = theme.color("text_color", "Text Colour", 0xFFFFFF);
		PRESET.onChange(Settings::applyPreset);
		ACCENT.onChange(v -> markCustom());
		GLASS_TINT.onChange(v -> markCustom());
		TEXT_COLOR.onChange(v -> markCustom());

		Section glass = b.section("Glass");
		GLASS_OPACITY = glass.slider("glass_opacity", "Glass Strength", 100, 0, 200, 5).suffix("%");
		HIGHLIGHT = glass.slider("highlight", "Specular Highlight", 70, 0, 100, 5).suffix("%")
			.describe("The soft light reflection across the top of each panel.");
		BORDER = glass.slider("border", "Edge Glow", 60, 0, 100, 5).suffix("%");
		RADIUS = glass.slider("radius", "Corner Radius", 8, 0, 16, 1).suffix("px");
		SHADOWS = glass.toggle("shadows", "Drop Shadows", true);
		SHADOW_STRENGTH = glass.slider("shadow_strength", "Shadow Strength", 60, 0, 100, 5).suffix("%");
		SHADOW_STRENGTH.visibleWhen(() -> SHADOWS.get());
		BLUR = glass.toggle("blur", "Background Blur", true).describe("Blur the game behind the menu (uses Minecraft's menu blur setting).");
		DIM = glass.slider("dim", "Background Dim", 30, 0, 90, 5).suffix("%");

		Section layout = b.section("Layout");
		MENU_SCALE = layout.choice("menu_scale", "Menu Scale", MenuScale.AUTO, s -> s.label)
			.describe("Size of this menu independent of your GUI scale. Always pixel-sharp.");
		SIDEBAR_WIDTH = layout.slider("sidebar_width", "Sidebar Width", 132, 96, 200, 2).suffix("px");
		DENSITY = layout.choice("density", "Row Density", Density.COMFORTABLE, d -> d.label);
		DESCRIPTIONS = layout.toggle("descriptions", "Show Descriptions", true);
		MOD_ICONS = layout.toggle("mod_icons", "Show Mod Icons", true);
		TEXT_SHADOW = layout.toggle("text_shadow", "Text Shadow", false);
		REMEMBER_PAGE = layout.toggle("remember_page", "Reopen Last Page", true);
		LAST_PAGE = layout.text("last_page", "Last Page", "", 64);
		LAST_PAGE.visibleWhen(() -> false);

		Section motion = b.section("Motion");
		ANIMATIONS = motion.toggle("animations", "Animations", true);
		ANIM_SPEED = motion.slider("anim_speed", "Animation Speed", 1.0, 0.25, 3.0, 0.25).suffix("x");
		ANIM_SPEED.visibleWhen(() -> ANIMATIONS.get());
		SMOOTH_SCROLL = motion.toggle("smooth_scroll", "Smooth Scrolling", true);
		SCROLL_SPEED = motion.slider("scroll_speed", "Scroll Speed", 1.0, 0.5, 5.0, 0.25).suffix("x");

		Section behaviour = b.section("Behaviour");
		CLICK_SOUNDS = behaviour.toggle("click_sounds", "Click Sounds", true);
		PAUSE_GAME = behaviour.toggle("pause_game", "Pause Singleplayer", false).describe("Pause the game while this menu is open in singleplayer.");

		Section hud = b.section("HUD");
		HUD_BACKGROUND = hud.toggle("hud_background", "Glass Background", true).describe("Draw HUD elements on a small glass pill.");
		HUD_BG_COLOR = hud.colorAlpha("hud_bg_color", "Background Colour", 0x66101018);
		HUD_BG_COLOR.visibleWhen(() -> HUD_BACKGROUND.get());
		HUD_PADDING = hud.slider("hud_padding", "Padding", 3, 0, 8, 1).suffix("px");
		HUD_SHADOW = hud.toggle("hud_shadow", "Text Shadow", true);

		Section teams = b.section("Teams");
		TEAM_COLORS = teams.toggle("team_colors", "Team Colours", true).describe("Let mods colour players by the teams you set up.");
		TEAM_COLORS.visibleWhen(() -> false);

		Section controls = b.section("Controls");
		OPEN_MENU = controls.keybind("open_menu", "Open EagerLib Menu", InputConstants.KEY_RSHIFT);
		OPEN_MENU.onPress(() -> EagerLib.open(null));

		MOD = b.build();
	}

	private static void markCustom() {
		if (!applying && PRESET.get() != Preset.CUSTOM) {
			applying = true;
			PRESET.set(Preset.CUSTOM);
			applying = false;
		}
	}

	private static void applyPreset(Preset p) {
		if (applying || p == Preset.CUSTOM) return;
		applying = true;
		ACCENT.set(p.accent | 0xFF000000);
		GLASS_TINT.set(p.glass);
		TEXT_COLOR.set(p.text | 0xFF000000);
		DIM.set((double) p.dim);
		applying = false;
	}
}
