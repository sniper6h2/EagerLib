package dev.eagerlib.core;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

import dev.eagerlib.api.EagerMod;
import dev.eagerlib.api.Option;

public final class ConfigIO {
	static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigIO() {}

	public static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve("eagerlib");
	}

	public static JsonObject read(String name) {
		Path file = dir().resolve(name);
		if (!Files.exists(file)) return new JsonObject();
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement e = JsonParser.parseReader(r);
			return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
		} catch (Exception e) {
			EagerLib.LOG.warn("Could not read {}: {}", file, e.toString());
			return new JsonObject();
		}
	}

	public static void write(String name, JsonObject json) {
		Path file = dir().resolve(name);
		try {
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling(name + ".tmp");
			try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(json, w);
			}
			Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			EagerLib.LOG.warn("Could not write {}: {}", file, e.toString());
		}
	}

	public static void load(EagerMod mod) {
		JsonObject root = read(mod.id() + ".json");
		JsonObject opts = root.has("options") && root.get("options").isJsonObject() ? root.getAsJsonObject("options") : new JsonObject();
		for (Option<?> o : mod.options().values()) {
			if (o.persistent() && opts.has(o.id())) {
				try {
					o.load(opts.get(o.id()));
				} catch (Exception e) {
					EagerLib.LOG.warn("Bad value for {}.{}", mod.id(), o.id());
				}
			}
		}
		mod.clearDirty();
	}

	public static void save(EagerMod mod) {
		JsonObject root = new JsonObject();
		root.addProperty("mod", mod.id());
		JsonObject opts = new JsonObject();
		for (Option<?> o : mod.options().values()) {
			if (o.persistent()) opts.add(o.id(), o.save());
		}
		root.add("options", opts);
		write(mod.id() + ".json", root);
	}
}
