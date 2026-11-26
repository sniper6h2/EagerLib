package dev.eagerlib.modmenu;

import java.util.HashMap;
import java.util.Map;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import dev.eagerlib.api.EagerMod;
import dev.eagerlib.core.EagerLib;

public final class EagerModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> EagerLib.createScreen(parent, null);
	}

	@Override
	public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		EagerLib.init();
		Map<String, ConfigScreenFactory<?>> map = new HashMap<>();
		for (EagerMod m : EagerLib.listed()) {
			String id = m.id();
			map.put(id, parent -> EagerLib.createScreen(parent, id));
		}
		return map;
	}
}
