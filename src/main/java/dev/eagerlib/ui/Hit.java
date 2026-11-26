package dev.eagerlib.ui;

final class Hit {
	final float x;
	final float y;
	final float w;
	final float h;
	final Object target;
	final int kind;
	final int extra;

	Hit(float x, float y, float w, float h, Object target, int kind, int extra) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.target = target;
		this.kind = kind;
		this.extra = extra;
	}

	boolean contains(double mx, double my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
