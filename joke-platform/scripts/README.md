# Scripts Directory

Automation scripts for building, testing, and deploying the Joke Platform.

## Scripts Overview

| Script | Purpose | Environment |
|--------|---------|-------------|
| `build-all.sh` | Build all Maven services | Development |
| `build-images.sh` | Build and push Docker images | Development/CI |
| `deploy-local.sh` | **Start local Docker Compose environment** | **Local (Docker)** |
| `local-down.sh` | Stop local Docker Compose environment | Local (Docker) |
| `deploy-k8s.sh` | Deploy to Kubernetes cluster | Kubernetes |

## Local Development (Docker Compose)

**Start all services locally:**
```bash
./scripts/deploy-local.sh
```

Services will be available at:
- Gateway: http://localhost:8085
- Other services: ports 8081-8084
- RabbitMQ UI: http://localhost:15672

**Stop services:**
```bash
./scripts/local-down.sh
```

## Kubernetes Deployment

**Deploy to development Kubernetes cluster:**
```bash
./scripts/deploy-k8s.sh local
```

**Deploy to production Kubernetes cluster:**
```bash
./scripts/deploy-k8s.sh cluster
```

## Building

**Build all services:**
```bash
./scripts/build-all.sh
```

**Skip tests:**
```bash
./scripts/build-all.sh --skip-tests
```

**Build and push Docker images:**
```bash
# Default tag (latest)
./scripts/build-images.sh

# Specific tag
./scripts/build-images.sh v1.0.0

# Custom registry
export DOCKER_REGISTRY=my-registry.com
./scripts/build-images.sh
```

## Using Makefile

For convenience, use the Makefile in the project root:

```bash
# Show all available commands
make help

# Local Docker development
make local-up      # Start Docker Compose
make local-down    # Stop Docker Compose
make local-logs    # View logs

# Kubernetes
make k8s-deploy-dev   # Deploy to dev cluster
make k8s-deploy-prod  # Deploy to production
make k8s-status       # Check pod status
make k8s-logs         # View logs

# Building
make build         # Build with Maven
make test          # Run tests
make docker-build  # Build Docker images
```

## Important Notes

- **"Local" always means Docker Compose** - not Kubernetes
- **"k8s" commands are for Kubernetes clusters** - kind, minikube, or production
- All scripts must be run from the project root or scripts directory
- Scripts require execute permissions (already set)
