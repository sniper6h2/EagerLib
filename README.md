# EagerLib

A shared library for Fabric mods: one liquid-glass settings menu for every mod built on it, plus global key binds, a drag-and-drop HUD editor, teams, changelogs and a theme engine.

Supports Minecraft **1.21–1.21.1, 1.21.4, 1.21.6, 1.21.8, 1.21.11, 26.1–26.1.2, 26.2 and 26.3** on Fabric. It's client-side only and needs no Fabric API.

Mods using it:
- [Crossbow Tags](https://github.com/sniper6h2/crossbow-tags)
- [Knockback Display](https://github.com/sniper6h2/knockback-display)

## For players

1. Install Fabric Loader. Then put **EagerLib** and the mods that use it into `.minecraft/mods/`, picking the jar that matches your Minecraft version.
2. In game, press **Right Shift** to open the EagerLib menu. With [Mod Menu](https://modrinth.com/mod/modmenu) installed, the config button of **any** Eager mod opens the same menu, already on that mod's page.

What the menu has:

- **Mods**: every mod's settings, with toggles, sliders, colour pickers, choice pills, key binds and text fields.
- **Teams**: name players and give them colours that mods can use.
- **Appearance**: lets you customise the menu itself:
  - presets: Liquid Glass, Frost, Midnight, Aurora, Sunset
  - accent, glass tint and text colours
  - glass strength, highlight, edge glow, corner radius and shadows
  - background blur and dim
  - menu scale, sidebar width, row density, descriptions and icons
  - animations and their speed, smooth scrolling and scroll speed
  - click sounds, pausing the game, and the HUD's style
- **Key Binds**: every mod's binds in one list. Click one, then press a key. Backspace clears it.
- **Changelog**: shows what changed in each mod.
- **All Mods**: a card for every installed mod. Click one to open its settings.
- **Edit HUD**: drag HUD elements around, with snapping to edges, the centre and other elements. Scroll to resize, right-click to show or hide, arrow keys to nudge. Built-in elements: FPS, coordinates and a clock. Mods can add more, such as Knockback Display's "Held Knockback".
- **Search** (top right, or Ctrl/Cmd+F) finds any setting in any mod. **Reset** resets the current page; you click it twice to confirm.

Settings are saved in `config/eagerlib/` (`<modid>.json`, `hud.json`, `teams.json`).

## For mod developers: using EagerLib

The easiest way is to do what [Crossbow Tags](https://github.com/sniper6h2/crossbow-tags) and [Knockback Display](https://github.com/sniper6h2/knockback-display) do. Add this repository as a git submodule called `eagerlib`, include its version projects in your `settings.gradle`, and depend on them:

```bash
git submodule add https://github.com/sniper6h2/EagerLib.git eagerlib
```

```groovy
// settings.gradle
include ":eagerlib:1.21.4"
project(":eagerlib:1.21.4").projectDir = file("eagerlib/versions/1.21.4")

// build.gradle (1.21.x uses the remapping Loom plugin; 26.x uses plain `implementation project(":eagerlib:26.3")`)
dependencies {
	implementation project(path: ":eagerlib:1.21.4", configuration: "namedElements")
}
```

Or run `./gradlew publishAll` here to publish every version to `~/.m2`, then depend on `dev.eagerlib:eagerlib:1.0.0+mc1.21.4`. Use `modImplementation` on 1.21.x and `implementation` on 26.x.

Declare an `eagerlib` entrypoint and the dependency in `fabric.mod.json`:

```json
"entrypoints": { "eagerlib": ["com.example.MySettings"] },
"depends": { "eagerlib": ">=1.0.0" }
```

Then register your page:

```java
public final class MySettings implements EagerLibPlugin {
	public static BoolOption ENABLED;
	public static ColorOption COLOUR;

	@Override
	public void register(EagerRegistry registry) {
		EagerMod.Builder mod = registry.mod("mymod")
			.description("What it does")
			.icon("assets/mymod/icon.png")
			.changelog("1.0.0", "2026-11-26", "First release");
		Section general = mod.section("General");
		ENABLED = general.toggle("enabled", "Mod Enabled", true);
		COLOUR = general.color("colour", "Colour", 0xFF8C8C).describe("Shown under the label.");
		general.keybind("toggle", "Toggle", KeyOption.NONE).onPress(() -> ENABLED.toggle());
		mod.hud(new TextHudElement("thing", "My HUD Thing", () -> "Hello", "Hello"));
		mod.build();
	}
}
```

Option types: `toggle`, `slider` (with `.suffix()` and `.decimals()`), `color` and `colorAlpha`, `choice` (enum or list), `keybind`, `text`, `button` and `dangerButton`. Every option supports `.describe(...)`, `.visibleWhen(...)` and `.onChange(...)`. Values save automatically.

## Building

You need JDK 25, which also builds the Java 21 jars.

```bash
./gradlew buildAll
```

The jars land in `dist/`, one per Minecraft version.

Version-specific code is kept small and lives in `*/variants/`.

| Variant | Versions | Why |
|---|---|---|
| `gfx-pose-managed` | 1.21 | `GuiGraphics` with a depth-tested `PoseStack`; batches draws with `drawManaged` |
| `gfx-pose` | 1.21.4 | `GuiGraphics` with a depth-tested `PoseStack` |
| `gfx-matrix` | 1.21.6 – 1.21.11 | `GuiGraphics` with 2D matrices |
| `gfx-extractor` | 26.x | `GuiGraphics` renamed `GuiGraphicsExtractor` |
| `screen-legacy` | 1.21 – 1.21.8 | `(x, y, button)` mouse and key methods |
| `screen-events` | 1.21.11 | `MouseButtonEvent` / `KeyEvent` input |
| `screen-extract` | 26.x | `extractRenderState` + event input |
| `hud-gui` / `hud-gui-extract` / `hud-hud` | ≤1.21.11 / 26.1 / 26.2+ | where the HUD is drawn |
| `plat-a` … `plat-d` | ≤1.21.8 / 1.21.11–26.1 / 26.2 / 26.3 | current screen, HUD hidden, key state (26.3 uses SDL) |

## Testing helper

`testing/` holds a dev-only in-game smoke test that mod repositories reuse:
- `testclient.gradle` adds a `runTestClient` Gradle task.
- `testmod/` is the helper mod, which screenshots every menu page.
- `smoke-test.sh` generates a flat test world and launches the client.

It's never packaged into a release jar.

## License

MIT
