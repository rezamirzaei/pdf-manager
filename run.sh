#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

build_and_run() {
  local build_goal=verify
  if [[ "${1-}" == "--fast" ]]; then
    build_goal=package
    shift
  fi

  if [[ ! -x "./mvnw" ]]; then
    echo "Error: ./mvnw not found or not executable. Run from the project root." >&2
    exit 1
  fi

  if [[ "$build_goal" == "verify" ]]; then
    ./mvnw -q verify
  else
    ./mvnw -q -DskipTests package
  fi

  local jar
  jar="$(ls -1t target/*-all.jar 2>/dev/null | head -n 1 || true)"
  if [[ -z "$jar" ]]; then
    echo "Error: shaded jar not found in target/. Build may have failed." >&2
    exit 1
  fi

  exec java -jar "$jar" "$@"
}

build_and_run "$@"

