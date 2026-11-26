package dev.eagerlib.api;

public interface EagerRegistry {
	EagerMod.Builder mod(String modId);

	void hud(EagerMod mod, HudElement element);
}
