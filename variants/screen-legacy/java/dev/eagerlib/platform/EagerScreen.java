package dev.eagerlib.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.eagerlib.ui.UiRoot;

public final class EagerScreen extends Screen {
	private final UiRoot root;
	private final Screen parent;

	public EagerScreen(UiRoot root, Screen parent) {
		super(Component.literal("EagerLib"));
		this.root = root;
		this.parent = parent;
	}

	@Override protected void init() { root.init(width, height); }

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		GraphicsCanvas canvas = new GraphicsCanvas(g);
		canvas.batch(() -> root.render(canvas, mouseX, mouseY, delta));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (root.wantsBlur() || Minecraft.getInstance().level == null) super.renderBackground(g, mouseX, mouseY, delta);
	}

	@Override public boolean mouseClicked(double x, double y, int button) { return root.mouseClicked(x, y, button); }
	@Override public boolean mouseReleased(double x, double y, int button) { return root.mouseReleased(x, y, button); }
	@Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return root.mouseDragged(x, y, button, dx, dy); }
	@Override public boolean mouseScrolled(double x, double y, double h, double v) { return root.mouseScrolled(x, y, h, v); }

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		return root.keyPressed(key, scancode, modifiers) || super.keyPressed(key, scancode, modifiers);
	}

	@Override public boolean charTyped(char c, int modifiers) { return root.charTyped(c, modifiers); }
	@Override public boolean isPauseScreen() { return root.pausesGame(); }

	@Override
	public void onClose() {
		root.onClose();
		Platform.setScreen(parent);
	}
}
