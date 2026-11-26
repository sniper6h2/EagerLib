package dev.eagerlib.core;

import java.net.URI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import dev.eagerlib.platform.Platform;

public final class Mc {
	private Mc() {}

	public static Minecraft mc() { return Minecraft.getInstance(); }

	public static int guiScale() { return (int) Math.round(mc().getWindow().getGuiScale()); }

	public static int fps() { return mc().getFps(); }

	public static void click() {
		if (Settings.CLICK_SOUNDS == null || !Settings.CLICK_SOUNDS.get()) return;
		try {
			mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		} catch (Throwable ignored) {
		}
	}

	public static void openLink(String url) {
		try {
			Screen current = Platform.screen();
			ConfirmLinkScreen.confirmLinkNow(current, URI.create(url));
		} catch (Throwable t) {
			EagerLib.LOG.warn("Could not open link {}", url, t);
		}
	}
}
