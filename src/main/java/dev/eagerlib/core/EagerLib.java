package dev.eagerlib.core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;

import dev.eagerlib.api.EagerLibPlugin;
import dev.eagerlib.api.EagerMod;
import dev.eagerlib.api.EagerRegistry;
import dev.eagerlib.api.HudElement;
import dev.eagerlib.api.KeyOption;
import dev.eagerlib.api.Option;
import dev.eagerlib.api.TextHudElement;
import dev.eagerlib.platform.EagerScreen;
import dev.eagerlib.platform.Platform;
import dev.eagerlib.ui.EagerUi;

public final class EagerLib implements ClientModInitializer {
	public static final String ID = "eagerlib";
	public static final Logger LOG = LoggerFactory.getLogger("EagerLib");

	private static final List<EagerMod> MODS = new ArrayList<>();
	private static boolean initialized;
	private static int ticks;

	public static final EagerRegistry REGISTRY = new EagerRegistry() {
		@Override public EagerMod.Builder mod(String modId) {
			return new EagerMod.Builder(modId, EagerLib::onBuilt);
		}

		@Override public void hud(EagerMod mod, HudElement element) {
			mod.addHud(element);
			HudManager.register(mod, element);
		}
	};

	@Override
	public void onInitializeClient() {
		init();
	}

	public static synchronized void init() {
		if (initialized) return;
		initialized = true;
		HudManager.load();
		Settings.register(REGISTRY);
		registerBuiltinHud();
		for (EntrypointContainer<EagerLibPlugin> c : FabricLoader.getInstance().getEntrypointContainers(ID, EagerLibPlugin.class)) {
			try {
				c.getEntrypoint().register(REGISTRY);
			} catch (Throwable t) {
				LOG.error("EagerLib plugin from {} failed to register", c.getProvider().getMetadata().getId(), t);
			}
		}
		Teams.load();
		LOG.info("EagerLib ready with {} mod(s)", MODS.size() - 1);
	}

	private static void registerBuiltinHud() {
		DateTimeFormatter clock = DateTimeFormatter.ofPattern("HH:mm");
		REGISTRY.hud(Settings.MOD, new TextHudElement("fps", "FPS", () -> Mc.fps() + " FPS", "144 FPS").at(0.01F, 0.01F));
		REGISTRY.hud(Settings.MOD, new TextHudElement("coords", "Coordinates", () -> {
			Player p = Mc.mc().player;
			return p == null ? "" : "XYZ " + p.getBlockX() + " / " + p.getBlockY() + " / " + p.getBlockZ();
		}, "XYZ 128 / 64 / -42").at(0.01F, 0.06F));
		REGISTRY.hud(Settings.MOD, new TextHudElement("clock", "Clock", () -> LocalTime.now().format(clock), "12:00").at(0.01F, 0.11F));
	}

	private static void onBuilt(EagerMod mod) {
		FabricLoader.getInstance().getModContainer(mod.id()).map(ModContainer::getMetadata).ifPresent(meta -> {
			EagerModAccess.fill(mod, meta);
		});
		ConfigIO.load(mod);
		mod.setSaver(ConfigIO::save);
		MODS.add(mod);
		for (HudElement e : mod.hudElements()) HudManager.register(mod, e);
	}

	public static List<EagerMod> all() { return Collections.unmodifiableList(MODS); }

	public static List<EagerMod> listed() {
		List<EagerMod> out = new ArrayList<>();
		for (EagerMod m : MODS) if (m.listed()) out.add(m);
		out.sort(java.util.Comparator.comparing(m -> m.name().toLowerCase(java.util.Locale.ROOT)));
		return out;
	}

	public static EagerMod find(String id) {
		for (EagerMod m : MODS) if (m.id().equals(id)) return m;
		return null;
	}

	public static Screen createScreen(Screen parent, String modId) {
		init();
		return new EagerScreen(new EagerUi(modId), parent);
	}

	public static void open(String modId) {
		Platform.setScreen(createScreen(Platform.screen(), modId));
	}

	public static void openPage(String pageKey) {
		init();
		Platform.setScreen(new EagerScreen(new EagerUi(null, pageKey), Platform.screen()));
	}

	public static void openHudEditor(Screen parent) {
		Platform.setScreen(new EagerScreen(new dev.eagerlib.ui.HudEditorUi(), parent));
	}

	public static void tick() {
		if (!initialized) return;
		boolean allowed = Platform.screen() == null && Mc.mc().player != null;
		for (EagerMod m : MODS) {
			for (Option<?> o : m.options().values()) {
				if (o instanceof KeyOption k) k.poll(allowed);
			}
		}
		if (++ticks % 100 == 0) saveDirty();
	}

	public static void saveDirty() {
		for (EagerMod m : MODS) if (m.isDirty()) m.save();
	}

	public static void saveAll() {
		saveDirty();
		HudManager.save();
	}
}
