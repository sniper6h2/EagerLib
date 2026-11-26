package dev.eagerlib.render;

public interface Canvas {
	void fill(int x1, int y1, int x2, int y2, int argb);

	void gradient(int x1, int y1, int x2, int y2, int top, int bottom);

	void text(String text, int x, int y, int argb, boolean shadow);

	void push();

	void pop();

	void translate(float x, float y);

	void scale(float s);

	void scissor(int x1, int y1, int x2, int y2);

	void noScissor();

	void layer();

	int guiWidth();

	int guiHeight();

	default void batch(Runnable draw) {
		draw.run();
	}
}
