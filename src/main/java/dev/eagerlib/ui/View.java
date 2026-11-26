package dev.eagerlib.ui;

import dev.eagerlib.render.Gfx;
import dev.eagerlib.render.Theme;

interface View {
	float layout(Gfx g, Theme t, float width);

	void render(Gfx g, Theme t, float x, float y, float width, int mx, int my, boolean hover);

	default void clip(float top, float bottom) {}

	default boolean click(double mx, double my, int button) { return false; }

	default boolean release(double mx, double my, int button) { return false; }

	default boolean drag(double mx, double my, int button) { return false; }

	default boolean key(int key, int mods) { return false; }

	default boolean typed(int cp) { return false; }

	default boolean capturing() { return false; }

	default void blur() {}
}
