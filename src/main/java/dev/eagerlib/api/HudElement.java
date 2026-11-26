package dev.eagerlib.api;

public interface HudElement {
	String id();

	String label();

	String text(boolean preview);

	default int color() { return 0xFFFFFFFF; }

	default boolean enabledByDefault() { return false; }

	default float defaultX() { return 0.02F; }

	default float defaultY() { return 0.02F; }
}
