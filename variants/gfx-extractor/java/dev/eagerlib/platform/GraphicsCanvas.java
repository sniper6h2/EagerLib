package dev.eagerlib.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import dev.eagerlib.render.Canvas;

public final class GraphicsCanvas implements Canvas {
	private final GuiGraphicsExtractor g;
	private final Font font = Minecraft.getInstance().font;

	public GraphicsCanvas(GuiGraphicsExtractor g) { this.g = g; }

	@Override public void fill(int x1, int y1, int x2, int y2, int argb) { g.fill(x1, y1, x2, y2, argb); }
	@Override public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) { g.fillGradient(x1, y1, x2, y2, top, bottom); }
	@Override public void text(String s, int x, int y, int argb, boolean shadow) { g.text(font, s, x, y, argb, shadow); }
	@Override public void push() { g.pose().pushMatrix(); }
	@Override public void pop() { g.pose().popMatrix(); }
	@Override public void translate(float x, float y) { g.pose().translate(x, y); }
	@Override public void scale(float s) { g.pose().scale(s, s); }

	@Override public void scissor(int x1, int y1, int x2, int y2) {
		g.pose().pushMatrix();
		g.pose().identity();
		g.enableScissor(x1, y1, x2, y2);
		g.pose().popMatrix();
	}

	@Override public void noScissor() { g.disableScissor(); }
	@Override public void layer() { g.nextStratum(); }
	@Override public int guiWidth() { return g.guiWidth(); }
	@Override public int guiHeight() { return g.guiHeight(); }
}
