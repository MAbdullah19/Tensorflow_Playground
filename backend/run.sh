#!/usr/bin/env bash
# Builds (if needed) and runs the playground backend.
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -d out ]] || [[ -z "$(find out -name '*.class' -print -quit 2>/dev/null)" ]]; then
  bash build.sh
fi
exec java -cp out com.playground.Main "$@"
