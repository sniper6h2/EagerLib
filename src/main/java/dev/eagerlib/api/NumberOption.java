package dev.eagerlib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberOption extends Option<Double> {
	private final double min;
	private final double max;
	private final double step;
	private int decimals;
	private String suffix = "";

	public NumberOption(String id, String label, double def, double min, double max, double step) {
		super(id, label, def);
		this.min = min;
		this.max = max;
		this.step = step;
		this.decimals = step >= 1 ? 0 : (step >= 0.1 ? 1 : 2);
	}

	public double min() { return min; }
	public double max() { return max; }
	public double step() { return step; }
	public int decimals() { return decimals; }
	public String suffix() { return suffix; }

	public NumberOption decimals(int d) { this.decimals = d; return this; }

	public NumberOption suffix(String s) { this.suffix = s == null ? "" : s; return this; }

	public int getInt() { return (int) Math.round(get()); }

	public float getFloat() { return get().floatValue(); }

	public double fraction() { return max == min ? 0 : (get() - min) / (max - min); }

	public void setFraction(double f) { set(min + Math.max(0, Math.min(1, f)) * (max - min)); }

	public String format() {
		double v = get();
		String s = decimals == 0 ? Long.toString(Math.round(v)) : String.format(java.util.Locale.ROOT, "%." + decimals + "f", v);
		return s + suffix;
	}

	@Override protected Double sanitize(Double v) {
		double x = Math.max(min, Math.min(max, v));
		if (step > 0) x = min + Math.round((x - min) / step) * step;
		x = Math.max(min, Math.min(max, x));
		double p = Math.pow(10, Math.max(decimals, 0) + 2);
		return Math.round(x * p) / p;
	}

	@Override public JsonElement save() { return new JsonPrimitive(get()); }

	@Override public void load(JsonElement json) {
		if (json != null && json.isJsonPrimitive()) loadValue(json.getAsDouble());
	}
}
