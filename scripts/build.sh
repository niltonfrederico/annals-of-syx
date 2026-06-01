#!/usr/bin/env bash
# Build the dumper fat jar and test classes.
set -euo pipefail

cd "$(dirname "$0")/.."
./gradlew shadowJar compileTestJava --no-daemon "$@"
