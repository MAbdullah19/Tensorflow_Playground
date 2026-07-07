#!/usr/bin/env bash
# Compiles all Java sources to backend/out.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
SOURCES=$(find src/main/java -name "*.java")
javac -d out --release 11 $SOURCES
echo "Built $(find out -name '*.class' | wc -l) classes."
