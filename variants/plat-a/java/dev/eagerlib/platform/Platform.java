package dev.eagerlib.platform;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class Platform {
	public static final int SHORTCUT_MODS = 2 | 8;
	public static final int SHIFT_MOD = 1;

	private Platform() {}

	public static Screen screen() { return Minecraft.getInstance().screen; }

	public static void setScreen(Screen s) { Minecraft.getInstance().setScreen(s); }

	public static boolean hudHidden() { return Minecraft.getInstance().options.hideGui; }

	public static boolean isKeyDown(int key) { return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key); }

	public static InputConstants.Key keyboardKey(int key) { return InputConstants.Type.KEYSYM.getOrCreate(key); }

	public static void closeCurrent() {
		Screen s = screen();
		if (s != null) s.onClose();
	}
}
