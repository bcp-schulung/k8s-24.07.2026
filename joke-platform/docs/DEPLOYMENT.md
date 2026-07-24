# Joke Platform - Deployment Guide

This guide covers deploying the Joke Platform in different environments.

## Quick Start

### Local Development (Docker Compose)

Start all services locally with Docker Compose:

```bash
# Option 1: Using script
./scripts/deploy-local.sh

# Option 2: Using Makefile
make local-up

# Option 3: Direct docker compose
cd infrastructure/docker
docker compose up -d
```

Access the platform:
- **Gateway Dashboard**: http://localhost:8085
- **Joke Generator API**: http://localhost:8081
- **Punchline Service API**: http://localhost:8082
- **Audience Service API**: http://localhost:8083
- **Chaos Comedian API**: http://localhost:8084
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)

Stop local environment:

```bash
# Using script
./scripts/local-down.sh

# Using Makefile
make local-down

# Direct docker compose
docker compose -f infrastructure/docker/compose.yaml down
```

---

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (kind, minikube, or production cluster)
- kubectl configured and connected to cluster
- Container images built and pushed to registry
- CloudNativePG operator installed in the cluster (required for PostgreSQL)

  ```bash
  # Install the CloudNativePG operator (example using Helm)
  helm repo add cnpg https://cloudnative-pg.github.io/charts
  helm repo update
  helm upgrade --install cnpg \
    --namespace cnpg-system \
    --create-namespace \
    cnpg/cloudnative-pg
  ```

### Deploy to Development Cluster

Uses the `local` Kustomize overlay with reduced resources:

```bash
# Option 1: Using script
./scripts/deploy-k8s.sh local

# Option 2: Using Makefile
make k8s-deploy-dev

# Option 3: Direct kubectl
kubectl apply -k infrastructure/kubernetes/overlays/local
```

Access the platform:

```bash
# Port-forward to gateway
kubectl port-forward -n joke-platform svc/local-joke-gateway 8080:8080

# Or using Makefile
make k8s-port-forward
```

### Deploy to Production Cluster

Uses the `cluster` Kustomize overlay with HPA, ingress, and production settings:

```bash
# Option 1: Using script
./scripts/deploy-k8s.sh cluster

# Option 2: Using Makefile
make k8s-deploy-prod

# Option 3: Direct kubectl
kubectl apply -k infrastructure/kubernetes/overlays/cluster
```

Access via ingress: https://jokes.container.it-scholar.com

### Helm Deployment

Create the PostgreSQL bootstrap secret before installing the chart (the default values expect a secret named `postgres-credentials`):

```bash
kubectl create secret generic postgres-credentials \
  -n joke-platform \
  --from-literal=username=jokes \
  --from-literal=password=<strong-password>
```

Deploy using Helm chart:

```bash
# Install
helm install joke-platform infrastructure/helm/joke-platform \
  --create-namespace \
  --namespace joke-platform \
  --values custom-values.yaml

# Or using Makefile
make helm-install

# Upgrade
helm upgrade joke-platform infrastructure/helm/joke-platform \
  --namespace joke-platform

# Uninstall
helm uninstall joke-platform --namespace joke-platform
```

---

## Building and Pushing Images

### Build Maven Artifacts

```bash
# Build all services
./scripts/build-all.sh

# Build with tests
./mvnw clean verify

# Build without tests
./mvnw clean package -DskipTests
```

### Build and Push Docker Images

```bash
# Build and push with 'latest' tag
./scripts/build-images.sh

# Build and push with specific tag
./scripts/build-images.sh v1.0.0

# Using Makefile
make docker-build
```

Set custom registry:

```bash
export DOCKER_REGISTRY=my-registry.example.com
./scripts/build-images.sh
```

---

## Configuration

### Secrets Management

For Kubernetes deployments, update secrets before deploying:

```bash
# Edit secrets file
vim infrastructure/kubernetes/base/secrets.yaml

# Or create secrets from command line
kubectl create secret generic rabbitmq-credentials \
  -n joke-platform \
  --from-literal=username=jokes \
  --from-literal=password=<strong-password>

kubectl create secret generic redis-credentials \
  -n joke-platform \
  --from-literal=password=<strong-password>

kubectl create secret generic postgres-credentials \
  -n joke-platform \
  --from-literal=username=jokes \
  --from-literal=password=<strong-password>
```

### PostgreSQL

The platform uses PostgreSQL 16 as the durable data store for:

- `audience-service` — persisted audience reactions and statistics
- `joke-generator` — joke setups
- `punchline-service` — punchlines

Schema versioning is handled by Flyway. Migrations are located in:

- `services/audience-service/src/main/resources/db/migration/`
- `services/joke-generator/src/main/resources/db/migration/`
- `services/punchline-service/src/main/resources/db/migration/`

**Local Docker Compose:** PostgreSQL starts automatically with the other infrastructure services. Data is persisted in the `postgres-data` volume.

**Kubernetes:** PostgreSQL is deployed as a [CloudNativePG](https://cloudnative-pg.io/) `Cluster` (`base/cnpg-cluster.yaml`). The cluster bootstraps a `joke-platform` database owned by `jokes` and exposes a read-write service at `joke-platform-db-rw`. Services connect using the `postgres-credentials` secret.

- The `local` overlay runs a single-instance cluster for development.
- The `cluster` overlay scales the cluster to 3 instances with pod anti-affinity for high availability.

To override the cluster name or scale in an overlay, patch the `Cluster` resource:

```bash
kubectl patch cluster -n joke-platform prod-joke-platform-db --type merge -p '{"spec":{"instances":3}}'
```

**Helm:** The Helm chart includes a CloudNativePG `Cluster` template (`templates/cnpg-cluster.yaml`) and configuration in `values.yaml` under the `cnpg` key. You can override the cluster name, instance count, persistence, and credentials via custom values.

### Environment-Specific Configuration

**Local overlay** (`overlays/local`):
- Single replica per service
- Reduced resource limits
- Debug logging enabled
- Development image tags

**Cluster overlay** (`overlays/cluster`):
- 3 replicas per service
- HPA enabled (scale 3-10 pods)
- Production resource limits
- Ingress with TLS
- Production image tags

Customize by editing kustomization files:
```bash
# Local environment
vim infrastructure/kubernetes/overlays/local/kustomization.yaml

# Production environment
vim infrastructure/kubernetes/overlays/cluster/kustomization.yaml
```

---

## Monitoring

### View Logs

**Docker Compose:**
```bash
# All services
docker compose -f infrastructure/docker/compose.yaml logs -f

# Specific service
docker compose -f infrastructure/docker/compose.yaml logs -f joke-gateway
```

**Kubernetes:**
```bash
# Gateway logs
kubectl logs -n joke-platform -l app.kubernetes.io/name=joke-gateway -f

# All pods
kubectl logs -n joke-platform -l app.kubernetes.io/part-of=joke-platform -f

# Specific pod
kubectl logs -n joke-platform <pod-name> -f
```

### Check Status

**Docker Compose:**
```bash
docker compose -f infrastructure/docker/compose.yaml ps
```

**Kubernetes:**
```bash
# Pod status
kubectl get pods -n joke-platform

# All resources
kubectl get all -n joke-platform

# Using Makefile
make k8s-status
```

### Metrics

All services expose Prometheus metrics at `/actuator/prometheus`:

```bash
# Docker Compose
curl http://localhost:8085/actuator/prometheus

# Kubernetes (with port-forward)
kubectl port-forward -n joke-platform svc/joke-gateway 8080:8080
curl http://localhost:8080/actuator/prometheus
```

---

## Troubleshooting

### Services not starting in Docker Compose

Check logs:
```bash
docker compose -f infrastructure/docker/compose.yaml logs
```

Common issues:
- Port already in use: Change ports in compose.yaml or stop conflicting services
- Insufficient memory: Increase Docker Desktop memory limit
- Missing .env file: Copy from .env.example

### Pods not ready in Kubernetes

Check pod status:
```bash
kubectl get pods -n joke-platform
kubectl describe pod <pod-name> -n joke-platform
kubectl logs <pod-name> -n joke-platform
```

Common issues:
- Image pull errors: Check registry credentials and image tags
- Resource limits: Check if cluster has sufficient resources
- Liveness/readiness probes failing: Check application startup time
- ConfigMap/Secret missing: Ensure base manifests are applied

### Cannot access services

**Docker Compose:**
- Verify services are running: `docker compose ps`
- Check port mappings: `docker compose port joke-gateway 8080`
- Verify firewall settings
- **Seminar VMs**: the friendly HTTPS domain (`https://<slug>.container.it-scholar.com`) is
  reverse-proxied to `code-server` on that VM, *not* to Docker Compose. Access the
  gateway directly on its published port instead, e.g.
  `http://<slug>.container.it-scholar.com:8085/` (plain `http://`, no TLS on that
  port). If `GATEWAY_PORT` was overridden in `.env`, use that port instead of 8085.

**Kubernetes:**
- Check service exists: `kubectl get svc -n joke-platform`
- Verify port-forward: `kubectl port-forward -n joke-platform svc/joke-gateway 8080:8080`
- Check ingress configuration: `kubectl get httproute -n joke-platform`

---

## Cleanup

### Remove Docker Compose Environment

```bash
# Stop services
./scripts/local-down.sh

# Remove volumes
docker compose -f infrastructure/docker/compose.yaml down -v

# Remove images
docker compose -f infrastructure/docker/compose.yaml down --rmi all
```

### Remove Kubernetes Deployment

```bash
# Delete namespace (removes all resources)
kubectl delete namespace joke-platform

# Or using Makefile
make k8s-delete

# Delete with Helm
helm uninstall joke-platform --namespace joke-platform
```

---

## CI/CD Integration

See `.github/workflows/` for automated build and deployment pipelines (coming soon).

For ArgoCD deployment:
1. Create ArgoCD Application pointing to `infrastructure/kubernetes/overlays/cluster`
2. Configure auto-sync policy
3. Set up image updater for automated deployments

---

## Next Steps

- Configure distributed tracing (Jaeger)
- Add structured logging (JSON format)
- Implement API authentication (JWT)
- Set up monitoring dashboards (Grafana)
- Configure alerting (Prometheus Alertmanager)
