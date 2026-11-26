package dev.eagerlib.core;

import java.util.stream.Collectors;

import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

import dev.eagerlib.api.EagerMod;

final class EagerModAccess {
	private EagerModAccess() {}

	static void fill(EagerMod mod, ModMetadata meta) {
		if (mod.version().isEmpty()) setField(mod, "version", meta.getVersion().getFriendlyString());
		if (mod.name().equals(mod.id())) setField(mod, "name", meta.getName());
		if (mod.author().isEmpty()) setField(mod, "author", meta.getAuthors().stream().map(Person::getName).collect(Collectors.joining(", ")));
		if (mod.description().isEmpty()) setField(mod, "description", meta.getDescription());
		if (mod.sourceUrl() == null) meta.getContact().get("sources").ifPresent(u -> setField(mod, "sourceUrl", u));
		if (mod.websiteUrl() == null) meta.getContact().get("homepage").ifPresent(u -> setField(mod, "websiteUrl", u));
	}

	private static void setField(EagerMod mod, String name, String value) {
		mod.fillMeta(name, value);
	}
}
