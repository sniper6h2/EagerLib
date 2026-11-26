#!/usr/bin/env bash
set -euo pipefail
MC="${1:?usage: smoke-test.sh <minecraft version>}"
ROOT="${EAGER_MOD_ROOT:?run this through the mod repository's scripts/smoke-test.sh}"
RUN="$ROOT/versions/$MC/run"
WORLD="$RUN/saves/test"

if [[ ! -f "$WORLD/level.dat" ]]; then
	SERVER_JAR="$HOME/.gradle/caches/fabric-loom/$MC/minecraft-server.jar"
	[[ -f "$SERVER_JAR" ]] || (cd "$ROOT" && ./gradlew ":$MC:compileJava" -q)
	GEN="$(mktemp -d)"
	echo "eula=true" > "$GEN/eula.txt"
	cat > "$GEN/server.properties" <<PROPS
level-name=test
level-type=minecraft\:flat
generate-structures=false
online-mode=false
spawn-monsters=false
difficulty=peaceful
server-port=25599
PROPS
	echo "Generating test world with the $MC server..."
	(cd "$GEN" && { sleep 1; while ! grep -q 'Done (' logs/latest.log 2>/dev/null; do sleep 1; done; echo stop; } \
		| java -Xmx2G -jar "$SERVER_JAR" --nogui > server.out 2>&1) || true
	mkdir -p "$RUN/saves"
	cp -R "$GEN/test" "$WORLD"
	rm -f "$WORLD/session.lock"
fi

mkdir -p "$RUN"
grep -q '^tutorialStep' "$RUN/options.txt" 2>/dev/null || printf 'onboardAccessibility:false\nnarrator:0\npauseOnLostFocus:false\ntutorialStep:none\n' >> "$RUN/options.txt"

rm -rf "$RUN/screenshots"
cd "$ROOT" && ./gradlew ":$MC:runTestClient" --console=plain
echo "Screenshots:"; ls -1 "$RUN/screenshots" 2>/dev/null || echo "(none)"
