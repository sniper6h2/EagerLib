# Modrinth listing: EagerLib

## Title

EagerLib

## Summary

One beautiful liquid-glass settings menu for all Eager mods, with global key binds, a drag-and-drop HUD editor, teams, changelogs and a fully customisable theme.

## Icon

`eagerlib/modrinth/icon.png` (512 × 512)

## Suggested project settings

- **Categories:** Library, Utility, Cosmetic
- **Loaders:** Fabric
- **Game versions:** 1.21, 1.21.1, 1.21.4, 1.21.6, 1.21.8, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, 26.3
- **Client side:** Required · **Server side:** Unsupported
- **License:** MIT

## Description

---

# EagerLib

**One menu for every Eager mod.**

EagerLib is the shared library behind Eager mods such as **Crossbow Tags** and **Knockback Display**. Instead of each mod shipping its own settings screen, they all plug into one smooth, liquid-glass menu. Press **Right Shift** in game, or click the config button of any Eager mod in Mod Menu. It's the same menu either way, opened on that mod's page.

> EagerLib is required by Eager mods. On its own it adds the menu, the HUD editor and a few HUD elements, and changes nothing else about the game.

---

## ✨ The menu

- **Liquid-glass design:** frosted panels float over a blurred view of your game, with soft shadows, a light sheen across the top and smooth animations everywhere.
- **Every mod in one sidebar:** each installed Eager mod gets its own page with its icon, author, links and description.
- **Proper controls:** animated toggles, sliders with editable values, a full colour picker (hue, saturation, brightness, alpha, hex and quick swatches), choice pills, key binds and text fields.
- **Search everything:** find any setting in any mod from the search bar (or **Ctrl/Cmd + F**).
- **Safe resets:** a small reset icon on every changed setting, and a page-wide **Reset** that asks you to click twice.

## 🧩 Built-in pages

| Page | What it does |
|---|---|
| **Teams** | Group players into teams with names and colours that mods can use. |
| **Appearance** | Customise the look of the menu and the HUD (see below). |
| **Key Binds** | Every key bind from every Eager mod in one list. Click, press a key, done. Backspace clears it. |
| **Changelog** | See what changed in each mod and when. |
| **All Mods** | A card for every Eager mod you have installed. Click one to jump to its settings. |
| **Edit HUD** | Drag HUD elements anywhere, with snapping to edges, the centre and other elements. Scroll to resize, right-click to show or hide, arrow keys to nudge. |

EagerLib comes with **FPS**, **Coordinates** and **Clock** HUD elements, and mods can add their own (Knockback Display adds *Held Knockback*). All of them start hidden; turn them on in **Edit HUD**.

## 🎨 Make it yours

Start from a preset (**Liquid Glass**, **Frost**, **Midnight**, **Aurora** or **Sunset**), then fine-tune it:

- **Colours:** accent, glass tint (with transparency) and text
- **Glass:** strength, sheen, edge glow, corner radius, drop shadows and shadow strength
- **Background:** blur on or off, and how much to dim the game behind the menu
- **Layout:** menu scale (always pixel-sharp), sidebar width, row density, descriptions and mod icons
- **Motion:** animations, animation speed, smooth scrolling and scroll speed
- **Behaviour:** click sounds, and whether the menu pauses singleplayer
- **HUD style:** glass background and its colour, padding and text shadow

## ⌨️ Controls

| Action | Default |
|---|---|
| Open the menu | **Right Shift** (rebind it under Key Binds) |
| Search | **Ctrl/Cmd + F** |
| Close / go back | **Esc** |

## ✅ Compatibility

- **Fabric** for Minecraft **1.21–1.21.1, 1.21.4, 1.21.6, 1.21.8, 1.21.11, 26.1–26.1.2, 26.2 and 26.3**. Download the file that matches your version.
- **Client-side only:** it works on any server, singleplayer or multiplayer, and server owners don't need to install anything.
- No Fabric API needed. [Mod Menu](https://modrinth.com/mod/modmenu) is optional.
- Settings are saved in `config/eagerlib/`.

## 🛠️ For mod developers

Add one entrypoint and a few lines of code, and your mod gets a full settings page, key binds, HUD elements and a changelog in the shared menu. Mods already using EagerLib don't need any changes when another one is added. See the README in the source repository for the API.
