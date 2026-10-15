#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "${JAVA_HOME:-}" ]]; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAVA="$JAVA_HOME/bin/java"
else
    JAVAC=javac
    JAVA=java
fi
mkdir -p build/core-benchmark
mapfile -t sources < <(find src/main/java -name '*.java' ! -path '*/server/*' | sort)
if [[ -n "${ECJ_JAR:-}" ]]; then
    "$JAVA" -jar "$ECJ_JAR" --release 25 -failOnWarning -d build/core-benchmark "${sources[@]}" \
        src/benchmark/java/io/github/ryokencoding/omperf/TelemetryBenchmark.java
else
    "$JAVAC" --release 25 -Xlint:all -Werror -d build/core-benchmark "${sources[@]}" \
        src/benchmark/java/io/github/ryokencoding/omperf/TelemetryBenchmark.java
fi
if [[ $# -gt 0 ]]; then
    "$JAVA" -Xms128m -Xmx128m -cp build/core-benchmark io.github.ryokencoding.omperf.TelemetryBenchmark > "$1"
else
    "$JAVA" -Xms128m -Xmx128m -cp build/core-benchmark io.github.ryokencoding.omperf.TelemetryBenchmark
fi
