package dev.eagerlib.ui;

import java.util.HashMap;
import java.util.Map;

import dev.eagerlib.core.Settings;

public final class Anim {
	private final Map<Object, Float> values = new HashMap<>();
	private long last = System.nanoTime();
	private float dt;

	public float frame() {
		long now = System.nanoTime();
		dt = Math.min(0.1F, (now - last) / 1_000_000_000F);
		last = now;
		return dt;
	}

	public float dt() { return dt; }

	public static boolean enabled() { return Settings.ANIMATIONS == null || Settings.ANIMATIONS.get(); }

	public static float speed() { return Settings.ANIM_SPEED == null ? 1F : Settings.ANIM_SPEED.getFloat(); }

	public float step(float current, float target, float rate) {
		if (!enabled()) return target;
		float k = 1F - (float) Math.exp(-rate * speed() * dt);
		float v = current + (target - current) * k;
		return Math.abs(target - v) < 0.002F ? target : v;
	}

	public float to(Object key, float target, float rate) {
		float cur = values.getOrDefault(key, target);
		float v = step(cur, target, rate);
		values.put(key, v);
		return v;
	}

	public float get(Object key, float def) { return values.getOrDefault(key, def); }

	public void set(Object key, float v) { values.put(key, v); }

	public static float ease(float t) {
		t = Math.max(0, Math.min(1, t));
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}
}
