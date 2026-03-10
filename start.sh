#!/usr/bin/env bash
# Convenience wrapper: rebuild quickly and run the app.
set -eo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

exec bash "$ROOT_DIR/run.sh" --fast "$@"
