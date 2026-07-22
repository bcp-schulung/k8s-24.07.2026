#!/bin/bash
set -euo pipefail

# Stop local Docker Compose environment
# Usage: ./scripts/local-down.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_DIR="$PROJECT_ROOT/infrastructure/docker"

echo "🛑 Stopping Joke Platform Docker Compose services..."
echo ""

cd "$COMPOSE_DIR"

# Stop and remove containers
docker compose down

echo ""
echo "✅ Services stopped successfully!"
echo ""
echo "To remove volumes as well, run:"
echo "   docker compose -f $COMPOSE_DIR/compose.yaml down -v"
