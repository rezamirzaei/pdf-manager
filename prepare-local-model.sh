#!/usr/bin/env bash
set -eo pipefail

DEFAULT_MODEL_FILE="qwen2.5-0.5b-instruct-q4_0.gguf"
DEFAULT_MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/${DEFAULT_MODEL_FILE}?download=1"

log() {
  printf '%s\n' "$*" >&2
}

MODEL_URL="${PDF_MANAGER_LOCAL_MODEL_URL:-$DEFAULT_MODEL_URL}"

if [[ -n "${PDF_MANAGER_LOCAL_MODEL_PATH:-}" ]]; then
  MODEL_PATH="${PDF_MANAGER_LOCAL_MODEL_PATH}"
else
  MODEL_DIR="${PDF_MANAGER_LOCAL_MODEL_DIR:-$HOME/.pdf-manager/models}"
  MODEL_PATH="${MODEL_DIR}/${DEFAULT_MODEL_FILE}"
fi

MODEL_DIR="$(dirname "$MODEL_PATH")"
mkdir -p "$MODEL_DIR"
MODEL_DIR="$(cd "$MODEL_DIR" && pwd)"
MODEL_PATH="${MODEL_DIR}/$(basename "$MODEL_PATH")"

if [[ -f "$MODEL_PATH" ]]; then
  log "Embedded local model already present at $MODEL_PATH"
  printf '%s\n' "$MODEL_PATH"
  exit 0
fi

TMP_PATH="${MODEL_PATH}.part"

log "Downloading embedded local model to $MODEL_PATH"
curl \
  --fail \
  --location \
  --retry 3 \
  --output "$TMP_PATH" \
  "$MODEL_URL"

mv "$TMP_PATH" "$MODEL_PATH"
log "Embedded local model ready at $MODEL_PATH"
printf '%s\n' "$MODEL_PATH"
