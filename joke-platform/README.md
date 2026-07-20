cat > README.md <<'EOF'
# Joke Platform

A five-service Spring Boot application designed to demonstrate Kubernetes
capabilities such as:

- service discovery and load balancing
- horizontal scaling
- self-healing
- readiness and liveness probes
- rolling deployments
- configuration and secrets
- asynchronous messaging
- distributed caching
- metrics, logs and tracing
- controlled failure and latency injection

## Services

| Service | Purpose |
|---|---|
| joke-gateway | Interactive web dashboard and application entry point |
| joke-generator | Generates joke setups |
| punchline-service | Generates punchlines |
| audience-service | Rates jokes and generates audience reactions |
| chaos-comedian | Produces configurable failures, latency and unusual responses |

## Build

```shell
./mvnw clean verify