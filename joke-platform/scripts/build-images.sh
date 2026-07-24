#!/bin/bash
set -euo pipefail

# Build and push Docker images for all services
# Usage: ./scripts/build-images.sh [tag]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

REGISTRY="${DOCKER_REGISTRY:-harbor.container.it-scholar.com}"
TAG="${1:-latest}"

SERVICES=("joke-gateway" "joke-generator" "punchline-service" "audience-service" "chaos-comedian")

echo "🐳 Building and pushing Docker images..."
echo "Registry: $REGISTRY"
echo "Tag: $TAG"
echo ""

# Build all services first
echo "📦 Building Maven artifacts..."
cd "$PROJECT_ROOT"
./mvnw clean package -DskipTests

echo ""
echo "🏗️  Building Docker images..."

for service in "${SERVICES[@]}"; do
  echo ""
  echo "Building $service..."
  
  IMAGE_NAME="$REGISTRY/joke-platform/$service:$TAG"
  
  docker build \
    -f infrastructure/docker/Dockerfile.spring-service \
    --build-arg MODULE="$service" \
    -t "$IMAGE_NAME" \
    .
  
  echo "Pushing $IMAGE_NAME..."
  docker push "$IMAGE_NAME"
  
  echo "✅ $service pushed successfully"
done

echo ""
echo "✅ All images built and pushed successfully!"
echo ""
echo "Images:"
for service in "${SERVICES[@]}"; do
  echo "  - $REGISTRY/joke-platform/$service:$TAG"
done
