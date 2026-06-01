#!/usr/bin/env bash
# Run a dumper main class headless under xvfb-run.
#
# Usage:
#   scripts/run.sh <main-class> [args...]
#
# Env overrides:
#   SOS_GAME_DIR       — Steam install (default: ~/.local/share/Steam/...)
#   SOS_XVFB_SCREEN    — Xvfb screen geometry (default: 1920x1080x24)
#   SOS_RUN_TIMEOUT    — seconds; wraps java in `timeout` if set
set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "usage: scripts/run.sh <main-class> [args...]" >&2
    exit 2
fi

main_class="$1"
shift

# Resolve any arg that points to an existing path to absolute, since we cd into
# the game dir below. FileGetter wraps FileNotFoundException as "file is corrupt".
args=()
for arg in "$@"; do
    if [[ -e "$arg" ]]; then
        args+=("$(realpath "$arg")")
    else
        args+=("$arg")
    fi
done
set -- "${args[@]}"

dumper_root="$(cd "$(dirname "$0")/.." && pwd)"
game_dir="${SOS_GAME_DIR:-$HOME/.local/share/Steam/steamapps/common/Songs of Syx}"
xvfb_screen="${SOS_XVFB_SCREEN:-1920x1080x24}"
jar="$dumper_root/build/libs/annals-of-syx-all.jar"
test_classes="$dumper_root/build/classes/java/test"
gradle_cache="$HOME/.gradle/caches/modules-2/files-2.1"

if [[ ! -f "$jar" ]]; then
    echo "missing $jar — run scripts/build.sh first" >&2
    exit 2
fi

if [[ ! -d "$game_dir" ]]; then
    echo "missing game dir $game_dir — set SOS_GAME_DIR" >&2
    exit 2
fi

# JUnit jars exist on classpath for @Test annotation reference; smoke tests use main().
junit_api=$(find "$gradle_cache" -name "junit-jupiter-api-*.jar" 2>/dev/null | head -1)
opentest=$(find "$gradle_cache" -name "opentest4j-*.jar" 2>/dev/null | head -1)
apiguard=$(find "$gradle_cache" -name "apiguardian-api-*.jar" 2>/dev/null | head -1)

# Script jars must be on the system classloader (ScriptLoad uses getSystemClassLoader).
script_jars="$game_dir/base/script/000_Tutorial.jar:$game_dir/base/script/001_Tutorial.jar"

cp="$jar:$test_classes:$junit_api:$opentest:$apiguard:$script_jars"

cd "$game_dir"

if [[ -n "${SOS_RUN_TIMEOUT:-}" ]]; then
    exec timeout "$SOS_RUN_TIMEOUT" xvfb-run -a -s "-screen 0 $xvfb_screen" \
        java -cp "$cp" "$main_class" "$@"
else
    exec xvfb-run -a -s "-screen 0 $xvfb_screen" \
        java -cp "$cp" "$main_class" "$@"
fi
