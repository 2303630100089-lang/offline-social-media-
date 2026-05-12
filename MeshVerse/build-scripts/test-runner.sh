#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

LOG_DIR="$ROOT_DIR/build/reports/ci"
mkdir -p "$LOG_DIR"

if [ -x ./gradlew ]; then
  GRADLE_CMD="./gradlew"
else
  GRADLE_CMD="gradle"
fi

run_and_capture() {
  local task="$1"
  local log_file="$2"
  echo "Running $task..."
  set +e
  $GRADLE_CMD --no-daemon "$task" >"$log_file" 2>&1
  local status=$?
  set -e
  if [ $status -ne 0 ]; then
    echo "Task failed: $task (see $log_file)"
    return $status
  fi
  echo "Task passed: $task"
}

run_and_capture ":app:testDebugUnitTest" "$LOG_DIR/testDebugUnitTest.log"
run_and_capture ":app:lintDebug" "$LOG_DIR/lintDebug.log"
