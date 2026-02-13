#!/bin/bash
# Integration test script for PDF Manager with LLM

set -e

echo "=== PDF Manager LLM Integration Test ==="
echo ""

# Check Ollama is running
echo "1. Checking Ollama availability..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "   ✓ Ollama is running"
else
    echo "   ✗ Ollama is not running. Starting it..."
    ollama serve &
    sleep 5
fi

# Check model is available
echo ""
echo "2. Checking llama3.2:3b model..."
if ollama list | grep -q "llama3.2:3b"; then
    echo "   ✓ llama3.2:3b model is installed"
else
    echo "   Pulling llama3.2:3b model..."
    ollama pull llama3.2:3b
fi

# Build the project
echo ""
echo "3. Building project..."
cd "$(dirname "$0")"
./mvnw clean package -DskipTests -q
echo "   ✓ Build complete"

# Run tests
echo ""
echo "4. Running tests..."
./mvnw test -q
echo "   ✓ All tests passed"

# Show usage
echo ""
echo "=== Setup Complete ==="
echo ""
echo "Run the application with:"
echo "  # Default (metadata-based):"
echo "  java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar"
echo ""
echo "  # LLM mode (uses Ollama):"
echo "  java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --llm"
echo ""
echo "  # Composite mode (metadata first, LLM fallback):"
echo "  java -jar target/pdf-manager-0.1.0-SNAPSHOT-all.jar --composite"

