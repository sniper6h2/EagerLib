package dev.eagerlib.api;

import java.util.List;

public record Changelog(String version, String date, List<String> changes) {
}
