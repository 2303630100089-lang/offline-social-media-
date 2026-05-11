#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

gradle --no-daemon :app:assembleDebug

echo "Debug APK (if build dependencies are available):"
find app/build/outputs/apk -name "*.apk" -print
