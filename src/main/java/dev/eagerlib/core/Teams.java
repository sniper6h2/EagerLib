package dev.eagerlib.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class Teams {
	public static final class Team {
		public String name;
		public int color;
		public final List<String> members = new ArrayList<>();

		public Team(String name, int color) {
			this.name = name;
			this.color = color;
		}

		public String membersText() { return String.join(", ", members); }

		public void setMembers(String text) {
			members.clear();
			for (String s : text.split("[,\\s]+")) {
				if (!s.isBlank()) members.add(s.trim());
			}
		}
	}

	private static final List<Team> TEAMS = new ArrayList<>();
	private static final int[] PALETTE = {0xFF5AC8FA, 0xFFFF6B6B, 0xFF51E08A, 0xFFFFC145, 0xFFB38CFF, 0xFFFF8FD1};

	private Teams() {}

	public static List<Team> all() { return TEAMS; }

	public static Team add() {
		Team t = new Team("Team " + (TEAMS.size() + 1), PALETTE[TEAMS.size() % PALETTE.length]);
		TEAMS.add(t);
		save();
		return t;
	}

	public static void remove(Team t) {
		TEAMS.remove(t);
		save();
	}

	public static Team teamOf(String player) {
		if (player == null || Settings.TEAM_COLORS == null || !Settings.TEAM_COLORS.get()) return null;
		String p = player.toLowerCase(Locale.ROOT);
		for (Team t : TEAMS) {
			for (String m : t.members) {
				if (m.toLowerCase(Locale.ROOT).equals(p)) return t;
			}
		}
		return null;
	}

	public static Integer colorOf(String player) {
		Team t = teamOf(player);
		return t == null ? null : t.color;
	}

	static void load() {
		TEAMS.clear();
		JsonObject root = ConfigIO.read("teams.json");
		if (!root.has("teams") || !root.get("teams").isJsonArray()) return;
		for (JsonElement e : root.getAsJsonArray("teams")) {
			if (!e.isJsonObject()) continue;
			JsonObject o = e.getAsJsonObject();
			Team t = new Team(o.has("name") ? o.get("name").getAsString() : "Team", 0xFFFFFFFF);
			try {
				t.color = (int) Long.parseLong(o.get("color").getAsString(), 16) | 0xFF000000;
			} catch (Exception ignored) {
			}
			if (o.has("members") && o.get("members").isJsonArray()) {
				for (JsonElement m : o.getAsJsonArray("members")) t.members.add(m.getAsString());
			}
			TEAMS.add(t);
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		JsonArray arr = new JsonArray();
		for (Team t : TEAMS) {
			JsonObject o = new JsonObject();
			o.addProperty("name", t.name);
			o.addProperty("color", String.format("%06X", t.color & 0xFFFFFF));
			JsonArray m = new JsonArray();
			t.members.forEach(m::add);
			o.add("members", m);
			arr.add(o);
		}
		root.add("teams", arr);
		ConfigIO.write("teams.json", root);
	}
}
