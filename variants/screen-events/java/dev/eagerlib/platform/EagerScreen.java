package dev.eagerlib.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import dev.eagerlib.ui.UiRoot;

public final class EagerScreen extends Screen {
	private final UiRoot root;
	private final Screen parent;

	private static int button(int b) {
		if (b == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return 0;
		if (b == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) return 1;
		if (b == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE) return 2;
		return b;
	}

	public EagerScreen(UiRoot root, Screen parent) {
		super(Component.literal("EagerLib"));
		this.root = root;
		this.parent = parent;
	}

	@Override protected void init() { root.init(width, height); }

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		root.render(new GraphicsCanvas(g), mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (root.wantsBlur() || Minecraft.getInstance().level == null) super.renderBackground(g, mouseX, mouseY, delta);
	}

	@Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) { return root.mouseClicked(e.x(), e.y(), button(e.button())); }
	@Override public boolean mouseReleased(MouseButtonEvent e) { return root.mouseReleased(e.x(), e.y(), button(e.button())); }
	@Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { return root.mouseDragged(e.x(), e.y(), button(e.button()), dx, dy); }
	@Override public boolean mouseScrolled(double x, double y, double h, double v) { return root.mouseScrolled(x, y, h, v); }

	@Override
	public boolean keyPressed(KeyEvent e) {
		return root.keyPressed(e.key(), 0, e.modifiers()) || super.keyPressed(e);
	}

	@Override public boolean charTyped(CharacterEvent e) { return root.charTyped(e.codepoint(), 0); }
	@Override public boolean isPauseScreen() { return root.pausesGame(); }

	@Override
	public void onClose() {
		root.onClose();
		Platform.setScreen(parent);
	}
}
