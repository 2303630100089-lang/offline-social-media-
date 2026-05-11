#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

gradle --no-daemon :app:testDebugUnitTest || true
gradle --no-daemon :app:lintDebug || true
