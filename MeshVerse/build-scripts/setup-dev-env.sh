#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[MeshVerse] Checking Java and Gradle"
java -version || true
gradle -v || true

echo "[MeshVerse] NOTE: This repository currently uses system Gradle in CI sandbox."
echo "[MeshVerse] Run: gradle --no-daemon :app:assembleDebug"
