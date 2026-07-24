#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# ArgoCD Expose — Container Seminar
# ─────────────────────────────────────────────────────────────────────────────
#
# Exposes the already-installed ArgoCD via Cloudflare DNS + ingress-nginx +
# Let's Encrypt TLS (DNS-01 via Cloudflare).
#
# Prerequisites (run AFTER terraform apply):
#   brew install kubectl helm
#   pip install cloudflare python-dotenv
#
# Usage:
#   cd /path/to/k8s-24.07.2026
#   bash argocd/deploy.sh
#
# Steps:
#   1. Configure ArgoCD server for insecure mode (TLS termination at ingress)
#   2. Create the cert-manager Cloudflare secret
#   3. Apply cert-manager ClusterIssuer (DNS-01 via Cloudflare)
#   4. Install ingress-nginx via Helm (if not already installed)
#   5. Apply Namespace + Ingress + Certificate
#   6. Wait for ingress-nginx LoadBalancer IP
#   7. Create argocd.container.it-scholar.com A record in Cloudflare
#   8. Wait for TLS certificate issuance
#   9. Print access URL and initial admin password
#
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
TF_DIR="$ROOT_DIR/terraform"

# ── Load .env ─────────────────────────────────────────────────────────────────
if [[ ! -f "$ROOT_DIR/.env" ]]; then
  echo "ERROR: $ROOT_DIR/.env not found — copy .env.example and fill in credentials."
  exit 1
fi
set -o allexport
# shellcheck source=/dev/null
source "$ROOT_DIR/.env"
set +o allexport

# ── Verify kubeconfig ─────────────────────────────────────────────────────────
export KUBECONFIG="$TF_DIR/kubeconfig-staging"
if [[ ! -f "$KUBECONFIG" ]]; then
  echo "ERROR: $KUBECONFIG not found."
  exit 1
fi

echo "Using KUBECONFIG: $KUBECONFIG"
kubectl cluster-info --request-timeout=10s >/dev/null

# ── Step 1: ArgoCD insecure mode ──────────────────────────────────────────────
echo ""
echo "=== Step 1: Configuring ArgoCD server for insecure mode ==="
kubectl patch configmap argocd-cmd-params-cm \
  --namespace argocd \
  --type merge \
  -p '{"data":{"server.insecure":"true"}}'

echo "  Restarting argocd-server..."
kubectl rollout restart deployment/argocd-server -n argocd
kubectl rollout status deployment/argocd-server -n argocd --timeout=120s
echo "  argocd-server is running in insecure mode."

# ── Step 2: cert-manager Cloudflare secret ────────────────────────────────────
echo ""
echo "=== Step 2: Cloudflare API token secret in cert-manager ==="
kubectl create secret generic cloudflare-api-token \
  --namespace cert-manager \
  --from-literal=api-token="$CF_API_TOKEN" \
  --dry-run=client -o yaml | kubectl apply -f -
echo "  Cloudflare secret ready."

# ── Step 3: ClusterIssuer ─────────────────────────────────────────────────────
echo ""
echo "=== Step 3: ClusterIssuer (Let's Encrypt DNS-01) ==="
kubectl apply -f "$SCRIPT_DIR/../harbor/01-clusterissuer.yaml"
echo "  ClusterIssuer letsencrypt-prod applied."

# ── Step 4: ingress-nginx ─────────────────────────────────────────────────────
echo ""
echo "=== Step 4: ingress-nginx ==="
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx 2>/dev/null || true
helm repo update ingress-nginx
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.type=LoadBalancer \
  --wait --timeout 5m
echo "  ingress-nginx installed."

# ── Step 5: Namespace + Ingress + Certificate ─────────────────────────────────
echo ""
echo "=== Step 5: Applying ArgoCD Ingress manifests ==="
kubectl apply -f "$SCRIPT_DIR/00-namespace.yaml"
kubectl apply -f "$SCRIPT_DIR/01-gateway.yaml"   # contains the Ingress resource
kubectl apply -f "$SCRIPT_DIR/02-certificate.yaml"
echo "  Ingress and Certificate applied."

# ── Step 6: Wait for ingress-nginx LoadBalancer IP ───────────────────────────
echo ""
echo "=== Step 6: Waiting for ingress-nginx LoadBalancer IP ==="
LB_IP=""
for i in $(seq 1 72); do
  LB_IP="$(kubectl get svc ingress-nginx-controller -n ingress-nginx \
    -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)"
  if [[ -n "$LB_IP" ]]; then
    echo "  LoadBalancer IP: $LB_IP"
    break
  fi
  echo "  Waiting... attempt $i/72 (10 s intervals)"
  sleep 10
done

if [[ -z "$LB_IP" ]]; then
  echo "ERROR: ingress-nginx LoadBalancer IP not assigned after 12 minutes."
  exit 1
fi

# ── Step 7: Create argocd DNS A record ────────────────────────────────────────
echo ""
echo "=== Step 7: Creating argocd.container.it-scholar.com → $LB_IP ==="
python3 - <<PYEOF
import sys
sys.path.insert(0, '$ROOT_DIR')
import cloudflare as cf_module

cf       = cf_module.Cloudflare(api_token='$CF_API_TOKEN')
zone_id  = '$CF_ZONE_ID'
dns_name = 'argocd.container.it-scholar.com'
ip       = '$LB_IP'

all_records = list(cf.dns.records.list(zone_id=zone_id))
existing = [
    r for r in all_records
    if getattr(r, 'name', '') == dns_name and getattr(r, 'type', '') == 'A'
]

if existing:
    rec = existing[0]
    if getattr(rec, 'content', '') == ip:
        print(f'  DNS {dns_name} -> {ip} already correct, skipping.')
    else:
        cf.dns.records.update(
            dns_record_id=rec.id, zone_id=zone_id,
            name=dns_name, type='A', content=ip, proxied=False, ttl=300,
        )
        print(f'  Updated DNS {dns_name} -> {ip}')
else:
    cf.dns.records.create(
        zone_id=zone_id,
        name=dns_name, type='A', content=ip, proxied=False, ttl=300,
    )
    print(f'  Created DNS {dns_name} -> {ip}')
PYEOF

# ── Step 8: Wait for TLS certificate ─────────────────────────────────────────
echo ""
echo "=== Step 8: Waiting for TLS certificate (DNS-01, usually < 2 min) ==="
if kubectl wait certificate/argocd-tls \
    --namespace argocd \
    --for=condition=Ready \
    --timeout=300s; then
  echo "  Certificate issued."
else
  echo "  Certificate not ready within 5 min — monitor with:"
  echo "  kubectl describe certificate argocd-tls -n argocd"
fi

# ── Step 9: Initial admin password ───────────────────────────────────────────
echo ""
echo "=== Step 9: Retrieving initial admin password ==="
ARGOCD_PASSWORD="$(kubectl get secret argocd-initial-admin-secret \
  -n argocd -o jsonpath='{.data.password}' 2>/dev/null \
  | base64 -d 2>/dev/null || echo '(already changed)')"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════════════════════════"
echo "  ArgoCD deployment complete"
echo "════════════════════════════════════════════════════════════════════════"
echo "  URL:      https://argocd.container.it-scholar.com"
echo "  Username: admin"
echo "  Password: $ARGOCD_PASSWORD"
echo "  LB IP:    $LB_IP"
echo ""
echo "  DNS TTL is 300 s — allow up to 5 min for propagation."
echo "════════════════════════════════════════════════════════════════════════"
#
# Prerequisites (run AFTER terraform apply):
#   brew install kubectl
#   pip install cloudflare python-dotenv
#
# Usage:
#   cd /path/to/k8s-24.07.2026
#   bash argocd/deploy.sh
#
# Steps:
#   1. Configure ArgoCD server for insecure mode (TLS termination at Gateway)
#   2. Create the cert-manager Cloudflare secret
#   3. Apply cert-manager ClusterIssuer (DNS-01 via Cloudflare)
#   4. Apply Namespace + Gateway + Certificate + HTTPRoutes
#   5. Wait for the Cilium Gateway to get a LoadBalancer IP
#   6. Create argocd.container.it-scholar.com A record in Cloudflare
#   7. Wait for TLS certificate issuance (DNS-01, usually < 2 min)
#   8. Print access URL
#
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
TF_DIR="$ROOT_DIR/terraform"

# ── Load .env ─────────────────────────────────────────────────────────────────
if [[ ! -f "$ROOT_DIR/.env" ]]; then
  echo "ERROR: $ROOT_DIR/.env not found — copy .env.example and fill in credentials."
  exit 1
fi
set -o allexport
# shellcheck source=/dev/null
source "$ROOT_DIR/.env"
set +o allexport

# ── Verify kubeconfig ─────────────────────────────────────────────────────────
export KUBECONFIG="$TF_DIR/kubeconfig-staging"
if [[ ! -f "$KUBECONFIG" ]]; then
  echo "ERROR: $KUBECONFIG not found."
  echo "       Run 'terraform init && terraform apply' inside the terraform/ directory first."
  exit 1
fi

echo "Using KUBECONFIG: $KUBECONFIG"
kubectl cluster-info --request-timeout=10s >/dev/null

# ── Step 1: ArgoCD insecure mode ──────────────────────────────────────────────
echo ""
echo "=== Step 1: Configuring ArgoCD server for insecure mode ==="
# TLS is terminated at the Cilium Gateway; argocd-server must not do its own TLS.
kubectl patch configmap argocd-cmd-params-cm \
  --namespace argocd \
  --type merge \
  -p '{"data":{"server.insecure":"true"}}'

echo "  Restarting argocd-server to pick up the change..."
kubectl rollout restart deployment/argocd-server -n argocd
kubectl rollout status deployment/argocd-server -n argocd --timeout=120s
echo "  argocd-server is running in insecure mode."

# ── Step 2: cert-manager Cloudflare secret ────────────────────────────────────
echo ""
echo "=== Step 2: Cloudflare API token secret in cert-manager ==="
kubectl create secret generic cloudflare-api-token \
  --namespace cert-manager \
  --from-literal=api-token="$CF_API_TOKEN" \
  --dry-run=client -o yaml | kubectl apply -f -
echo "  Cloudflare secret ready in cert-manager namespace."

# ── Step 3: ClusterIssuer ─────────────────────────────────────────────────────
echo ""
echo "=== Step 3: ClusterIssuer (Let's Encrypt DNS-01) ==="
kubectl apply -f "$SCRIPT_DIR/../harbor/01-clusterissuer.yaml"
echo "  ClusterIssuer letsencrypt-prod applied."

# ── Step 4: Namespace + Gateway + Certificate + HTTPRoutes ────────────────────
echo ""
echo "=== Step 4: Applying ArgoCD Gateway manifests ==="
kubectl apply -f "$SCRIPT_DIR/00-namespace.yaml"
kubectl apply -f "$SCRIPT_DIR/01-gateway.yaml"
kubectl apply -f "$SCRIPT_DIR/02-certificate.yaml"
kubectl apply -f "$SCRIPT_DIR/03-httproute.yaml"
echo "  Gateway, Certificate, and HTTPRoutes applied."

# ── Step 5: Wait for Gateway LoadBalancer IP ──────────────────────────────────
echo ""
echo "=== Step 5: Waiting for Gateway LoadBalancer IP ==="
LB_IP=""
for i in $(seq 1 72); do   # up to 12 minutes
  LB_IP="$(kubectl get gateway argocd-gateway -n argocd \
    -o jsonpath='{.status.addresses[0].value}' 2>/dev/null || true)"
  if [[ -n "$LB_IP" ]]; then
    echo "  LoadBalancer IP: $LB_IP"
    break
  fi
  echo "  Waiting... attempt $i/72 (10 s intervals)"
  sleep 10
done

if [[ -z "$LB_IP" ]]; then
  echo "ERROR: Gateway LoadBalancer IP was not assigned after 12 minutes."
  echo "  Debug: kubectl get gateway argocd-gateway -n argocd -o yaml"
  echo "  Debug: kubectl get events -n argocd --sort-by=.lastTimestamp"
  exit 1
fi

# ── Step 6: Create argocd DNS A record ────────────────────────────────────────
echo ""
echo "=== Step 6: Creating argocd.container.it-scholar.com → $LB_IP ==="
python3 - <<PYEOF
import sys
sys.path.insert(0, '$ROOT_DIR')
import cloudflare as cf_module

cf       = cf_module.Cloudflare(api_token='$CF_API_TOKEN')
zone_id  = '$CF_ZONE_ID'
dns_name = 'argocd.container.it-scholar.com'
ip       = '$LB_IP'

all_records = list(cf.dns.records.list(zone_id=zone_id))
existing = [
    r for r in all_records
    if getattr(r, 'name', '') == dns_name and getattr(r, 'type', '') == 'A'
]

if existing:
    rec = existing[0]
    if getattr(rec, 'content', '') == ip:
        print(f'  DNS {dns_name} -> {ip} already correct, skipping.')
    else:
        cf.dns.records.update(
            dns_record_id=rec.id, zone_id=zone_id,
            name=dns_name, type='A', content=ip, proxied=False, ttl=300,
        )
        print(f'  Updated DNS {dns_name} -> {ip}')
else:
    cf.dns.records.create(
        zone_id=zone_id,
        name=dns_name, type='A', content=ip, proxied=False, ttl=300,
    )
    print(f'  Created DNS {dns_name} -> {ip}')
PYEOF

# ── Step 7: Wait for TLS certificate ─────────────────────────────────────────
echo ""
echo "=== Step 7: Waiting for TLS certificate (DNS-01, usually < 2 min) ==="
if kubectl wait certificate/argocd-tls \
    --namespace argocd \
    --for=condition=Ready \
    --timeout=300s; then
  echo "  Certificate issued."
else
  echo "  Certificate not ready within 5 min — it may still be issuing."
  echo "  Monitor: kubectl describe certificate argocd-tls -n argocd"
  echo "  Monitor: kubectl describe certificaterequest -n argocd"
fi

# ── Step 8: Get initial admin password ────────────────────────────────────────
echo ""
echo "=== Step 8: Retrieving initial admin password ==="
ARGOCD_PASSWORD="$(kubectl get secret argocd-initial-admin-secret \
  -n argocd -o jsonpath='{.data.password}' 2>/dev/null \
  | base64 -d 2>/dev/null || echo '(secret not found — may have been changed already)')"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════════════════════════"
echo "  ArgoCD deployment complete"
echo "════════════════════════════════════════════════════════════════════════"
echo "  URL:            https://argocd.container.it-scholar.com"
echo "  Username:       admin"
echo "  Password:       $ARGOCD_PASSWORD"
echo "  LB IP:          $LB_IP"
echo ""
echo "  NOTE: Cloudflare DNS is set grey-cloud (proxy OFF) as required for"
echo "        the Cilium Gateway. DNS propagation may take up to 60 s."
echo "════════════════════════════════════════════════════════════════════════"
