#!/bin/bash
set -euo pipefail

# Start joke-platform locally with Docker Compose
# Usage: ./scripts/deploy-local.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_DIR="$PROJECT_ROOT/infrastructure/docker"

echo "🚀 Starting Joke Platform locally with Docker Compose..."
echo ""

cd "$COMPOSE_DIR"

# Check if .env file exists
if [[ ! -f .env ]]; then
  echo "⚙️  Creating .env file from .env.example..."
  if [[ -f .env.example ]]; then
    cp .env.example .env
  else
    echo "⚠️  Warning: .env.example not found"
  fi
fi

# Start services
echo "📦 Starting services..."
docker compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 5

# Check health
echo ""
docker compose ps

echo ""
echo "✅ Services started successfully!"
echo ""
echo "🌐 Access points:"
echo "   Gateway:          http://localhost:8085"
echo "   Joke Generator:   http://localhost:8081"
echo "   Punchline:        http://localhost:8082"
echo "   Audience:         http://localhost:8083"
echo "   Chaos Comedian:   http://localhost:8084"
echo "   RabbitMQ UI:      http://localhost:15672 (guest/guest)"
echo ""
echo "📊 View logs:"
echo "   docker compose -f $COMPOSE_DIR/compose.yaml logs -f"
echo ""
echo "🛑 Stop services:"
echo "   docker compose -f $COMPOSE_DIR/compose.yaml down"
