package dev.eagerlib.ui;

import dev.eagerlib.render.Canvas;

public interface UiRoot {
	default void init(int width, int height) {}

	void render(Canvas canvas, int mouseX, int mouseY, float delta);

	default boolean mouseClicked(double x, double y, int button) { return false; }

	default boolean mouseReleased(double x, double y, int button) { return false; }

	default boolean mouseDragged(double x, double y, int button, double dx, double dy) { return false; }

	default boolean mouseScrolled(double x, double y, double h, double v) { return false; }

	default boolean keyPressed(int key, int scancode, int modifiers) { return false; }

	default boolean charTyped(int codepoint, int modifiers) { return false; }

	default void onClose() {}

	default boolean pausesGame() { return false; }

	default boolean wantsBlur() { return true; }
}
