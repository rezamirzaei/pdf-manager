#!/bin/bash
# Simple run script - just runs the app in default (fast) mode
cd "$(dirname "$0")"
echo "Starting PDF Manager..."
java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar
