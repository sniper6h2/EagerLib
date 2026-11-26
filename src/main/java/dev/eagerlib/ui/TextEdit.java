package dev.eagerlib.ui;

import java.util.function.IntPredicate;

import com.mojang.blaze3d.platform.InputConstants;


final class TextEdit {
	final StringBuilder text = new StringBuilder();
	int caret;
	private final int max;
	private final IntPredicate allowed;
	private final long born = System.currentTimeMillis();

	TextEdit(String initial, int max, IntPredicate allowed) {
		this.max = max;
		this.allowed = allowed;
		text.append(initial == null ? "" : initial);
		caret = text.length();
	}

	String value() { return text.toString(); }

	boolean caretVisible() { return ((System.currentTimeMillis() - born) / 500) % 2 == 0; }

	boolean typed(int cp) {
		if (cp < 32 || cp == 127 || !allowed.test(cp)) return false;
		if (text.length() >= max) return true;
		text.insert(caret, Character.toChars(cp));
		caret += Character.charCount(cp);
		return true;
	}

	boolean key(int key, int mods) {
		boolean shortcut = (mods & dev.eagerlib.platform.Platform.SHORTCUT_MODS) != 0;
		switch (key) {
			case InputConstants.KEY_BACKSPACE -> {
				if (shortcut) {
					text.delete(0, caret);
					caret = 0;
				} else if (caret > 0) {
					text.deleteCharAt(--caret);
				}
				return true;
			}
			case InputConstants.KEY_DELETE -> {
				if (caret < text.length()) text.deleteCharAt(caret);
				return true;
			}
			case InputConstants.KEY_LEFT -> { caret = Math.max(0, caret - 1); return true; }
			case InputConstants.KEY_RIGHT -> { caret = Math.min(text.length(), caret + 1); return true; }
			case InputConstants.KEY_HOME -> { caret = 0; return true; }
			case InputConstants.KEY_END -> { caret = text.length(); return true; }
			case InputConstants.KEY_A -> {
				if (shortcut) {
					caret = text.length();
					return true;
				}
				return false;
			}
			case InputConstants.KEY_V -> {
				if (shortcut) {
					String clip = net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard();
					if (clip != null) clip.codePoints().forEach(this::typed);
					return true;
				}
				return false;
			}
			case InputConstants.KEY_C -> {
				if (shortcut) {
					net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(value());
					return true;
				}
				return false;
			}
			default -> { return false; }
		}
	}
}
