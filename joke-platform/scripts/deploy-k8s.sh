#!/bin/bash
set -euo pipefail

# Deploy joke-platform to Kubernetes cluster using Kustomize
# Usage: ./scripts/deploy-k8s.sh [overlay]
#   overlay: local, cluster (default: local)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

NAMESPACE="joke-platform"
OVERLAY="${1:-local}"

if [[ "$OVERLAY" != "local" && "$OVERLAY" != "cluster" ]]; then
  echo "❌ Invalid overlay: $OVERLAY"
  echo "Usage: $0 [local|cluster]"
  exit 1
fi

echo "🚀 Deploying Joke Platform to Kubernetes..."
echo "   Overlay: $OVERLAY"
echo ""

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
  echo "❌ kubectl not found. Please install kubectl first."
  exit 1
fi

# Check if cluster is accessible
if ! kubectl cluster-info &> /dev/null; then
  echo "❌ Cannot connect to Kubernetes cluster."
  echo "   For local development: start kind or minikube first"
  exit 1
fi

echo "📦 Applying Kustomize overlay: $OVERLAY"
cd "$PROJECT_ROOT/infrastructure/kubernetes/overlays/$OVERLAY"

# Apply the kustomization
kubectl apply -k .

echo ""
echo "⏳ Waiting for pods to be ready..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/part-of=joke-platform \
  -n $NAMESPACE \
  --timeout=300s || true

echo ""
echo "✅ Deployment completed!"
echo ""
echo "📊 Pod status:"
kubectl get pods -n $NAMESPACE

echo ""
echo "🌐 To access the platform:"
if [[ "$OVERLAY" == "local" ]]; then
  echo "   kubectl port-forward -n $NAMESPACE svc/local-joke-gateway 8080:8080"
  echo "   Then open: http://localhost:8080"
else
  echo "   Access via ingress: https://jokes.container.it-scholar.com"
fi
