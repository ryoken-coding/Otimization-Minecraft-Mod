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
mkdir -p build/core-test
mapfile -t sources < <(find src/main/java -name '*.java' ! -path '*/server/*' | sort)
if [[ -n "${ECJ_JAR:-}" ]]; then
    "$JAVA" -jar "$ECJ_JAR" --release 25 -failOnWarning -d build/core-test "${sources[@]}" \
        src/test/java/io/github/ryokencoding/omperf/CoreTests.java
else
    "$JAVAC" --release 25 -Xlint:all -Werror -d build/core-test "${sources[@]}" \
        src/test/java/io/github/ryokencoding/omperf/CoreTests.java
fi
"$JAVA" -ea -Xms128m -Xmx128m -cp build/core-test io.github.ryokencoding.omperf.CoreTests
