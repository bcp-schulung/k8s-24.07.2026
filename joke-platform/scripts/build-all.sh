#!/bin/bash
set -euo pipefail

# Build all services in the joke-platform
# Usage: ./scripts/build-all.sh [--skip-tests]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

SKIP_TESTS=false
if [[ "${1:-}" == "--skip-tests" ]]; then
  SKIP_TESTS=true
fi

echo "🏗️  Building Joke Platform Services..."
echo "Project root: $PROJECT_ROOT"
echo ""

cd "$PROJECT_ROOT"

if $SKIP_TESTS; then
  echo "⏭️  Skipping tests..."
  ./mvnw clean package -DskipTests
else
  echo "🧪 Running tests..."
  ./mvnw clean verify
fi

echo ""
echo "✅ Build completed successfully!"
echo ""
echo "Built artifacts:"
for service in joke-gateway joke-generator punchline-service audience-service chaos-comedian; do
  jar_file="services/$service/target/$service-0.0.1-SNAPSHOT.jar"
  if [[ -f "$jar_file" ]]; then
    size=$(du -h "$jar_file" | cut -f1)
    echo "  ✓ $service ($size)"
  fi
done
