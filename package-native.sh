#!/usr/bin/env bash
set -eo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

APP_NAME="PDF Manager"
RAW_VERSION="$(sed -n 's/.*<version>\(.*\)<\/version>.*/\1/p' pom.xml | head -n 1)"
APP_VERSION="${PACKAGE_VERSION:-${RAW_VERSION%%-*}}"

if [[ "$APP_VERSION" =~ ^0\. ]]; then
  APP_VERSION="1.${APP_VERSION#*.}"
fi

case "$(uname -s)" in
  Darwin)
    PACKAGE_TYPE="${PACKAGE_TYPE:-dmg}"
    ;;
  Linux)
    PACKAGE_TYPE="${PACKAGE_TYPE:-app-image}"
    ;;
  *)
    PACKAGE_TYPE="${PACKAGE_TYPE:-app-image}"
    ;;
esac

echo "Building application jar..."
./mvnw -q -DskipTests package

JAR="$(ls -1t target/*-all.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "$JAR" ]]; then
  echo "Error: shaded jar not found in target/." >&2
  exit 1
fi

mkdir -p dist

echo "Creating native package (${PACKAGE_TYPE})..."
jpackage \
  --type "$PACKAGE_TYPE" \
  --dest dist \
  --input target \
  --name "$APP_NAME" \
  --main-jar "$(basename "$JAR")" \
  --main-class com.rezami.pdfmanager.app.PdfManagerMain \
  --app-version "$APP_VERSION" \
  --vendor "rezami" \
  --description "Rename PDF files using built-in local title inference or optional Ollama." \
  --java-options "-Dapple.laf.useScreenMenuBar=true"

echo "Native package created in dist/."
