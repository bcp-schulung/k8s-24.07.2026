# Joke Platform - Makefile
# Convenient commands for building, testing, and deploying

.PHONY: help build test clean docker-build docker-push local-up local-down k8s-deploy-dev k8s-deploy-prod

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-20s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Build targets
build: ## Build all services with Maven
	./mvnw clean package -DskipTests

test: ## Run all tests
	./mvnw clean verify

clean: ## Clean build artifacts
	./mvnw clean

# Docker targets
docker-build: build ## Build Docker images for all services
	./scripts/build-images.sh

docker-push: ## Push Docker images to registry
	./scripts/build-images.sh

# Local development (Docker Compose)
local-up: ## Start local environment with Docker Compose
	./scripts/deploy-local.sh

local-down: ## Stop local Docker Compose environment
	./scripts/local-down.sh

local-logs: ## Show logs from local Docker Compose
	docker compose -f infrastructure/docker/compose.yaml logs -f

local-restart: local-down local-up ## Restart local environment

# Kubernetes deployment
k8s-deploy-dev: ## Deploy to Kubernetes with 'local' overlay (dev cluster)
	./scripts/deploy-k8s.sh local

k8s-deploy-prod: ## Deploy to Kubernetes with 'cluster' overlay (production)
	./scripts/deploy-k8s.sh cluster

k8s-delete: ## Delete joke-platform from Kubernetes
	kubectl delete namespace joke-platform --ignore-not-found=true

k8s-status: ## Show Kubernetes pod status
	kubectl get pods -n joke-platform

k8s-logs: ## Show logs from gateway service in Kubernetes
	kubectl logs -n joke-platform -l app.kubernetes.io/name=joke-gateway -f

# Helm targets
helm-install: ## Install using Helm chart
	helm install joke-platform infrastructure/helm/joke-platform \
		--create-namespace --namespace joke-platform

helm-upgrade: ## Upgrade Helm release
	helm upgrade joke-platform infrastructure/helm/joke-platform \
		--namespace joke-platform

helm-uninstall: ## Uninstall Helm release
	helm uninstall joke-platform --namespace joke-platform

# Development helpers
dev-gateway: ## Port-forward to gateway service (Docker)
	@echo "Access gateway at http://localhost:8085"
	docker compose -f infrastructure/docker/compose.yaml port joke-gateway 8080

k8s-port-forward: ## Port-forward to gateway in Kubernetes
	@echo "Access gateway at http://localhost:8080"
	kubectl port-forward -n joke-platform svc/joke-gateway 8080:8080
