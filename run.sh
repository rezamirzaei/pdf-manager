#!/usr/bin/env bash
# PDF Manager - Run Script
#
# Usage:
#   ./run.sh              # Build with tests and run (auto: LLM if available)
#   ./run.sh --fast       # Build without tests and run (default mode)
#   ./run.sh --metadata   # Run with metadata-only mode
#   ./run.sh --llm        # Run with LLM mode (requires Ollama + llama model)
#   ./run.sh --composite  # Run with composite mode (LLM + metadata fallback)
#   ./run.sh --fast --llm # Build fast and run with LLM mode
#
set -eo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

build_and_run() {
  local build_goal=verify
  local app_args=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --fast)
        build_goal=package
        shift
        ;;
      --metadata|--llm|--composite)
        app_args="$1"
        shift
        ;;
      *)
        shift
        ;;
    esac
  done

  if [[ ! -x "./mvnw" ]]; then
    echo "Error: ./mvnw not found or not executable. Run from the project root." >&2
    exit 1
  fi

  echo "Building PDF Manager..."
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

  echo "Starting PDF Manager ${app_args:-default mode}..."
  if [[ -n "$app_args" ]]; then
    exec java -jar "$jar" "$app_args"
  else
    exec java -jar "$jar"
  fi
}

build_and_run "$@"
