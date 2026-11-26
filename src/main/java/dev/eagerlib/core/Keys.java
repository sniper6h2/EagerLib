package dev.eagerlib.core;

import com.mojang.blaze3d.platform.InputConstants;

import dev.eagerlib.api.KeyOption;
import dev.eagerlib.platform.Platform;

public final class Keys {
	private Keys() {}

	public static boolean isDown(int key) {
		if (key == KeyOption.NONE || key >= KeyOption.MOUSE_OFFSET) return false;
		try {
			return Platform.isKeyDown(key);
		} catch (Throwable t) {
			return false;
		}
	}

	public static String name(int key) {
		if (key == KeyOption.NONE) return "None";
		try {
			String s = Platform.keyboardKey(key).getDisplayName().getString();
			return s.length() == 1 ? s.toUpperCase(java.util.Locale.ROOT) : s;
		} catch (Throwable t) {
			return "Key " + key;
		}
	}

	public static String id(int key) {
		if (key == KeyOption.NONE) return "none";
		try {
			return Platform.keyboardKey(key).getName();
		} catch (Throwable t) {
			return Integer.toString(key);
		}
	}

	public static int fromId(String id) {
		if (id == null || id.isEmpty() || id.equals("none")) return KeyOption.NONE;
		try {
			return Integer.parseInt(id);
		} catch (NumberFormatException ignored) {
		}
		try {
			InputConstants.Key k = InputConstants.getKey(id);
			int v = k.getValue();
			return v < 0 ? KeyOption.NONE : v;
		} catch (Throwable t) {
			return KeyOption.NONE;
		}
	}
}
