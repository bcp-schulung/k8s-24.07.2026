#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Headlamp Deploy — Container Seminar
# ─────────────────────────────────────────────────────────────────────────────
#
# Deploys Headlamp (Kubernetes web UI / monitoring tool) to the staging
# cluster, exposed exactly like argocd/harbor: ingress-nginx (shared
# LoadBalancer) + cert-manager (DNS-01 via Cloudflare) + Cloudflare A record.
#
# Prerequisites (run AFTER terraform apply):
#   brew install kubectl helm
#   pip install cloudflare python-dotenv
#
# Usage:
#   cd /path/to/k8s-24.07.2026
#   bash headlamp/deploy.sh
#
# Steps:
#   1. Create the cert-manager Cloudflare secret (idempotent, shared w/ argocd/harbor)
#   2. Apply ClusterIssuer (idempotent, reuses letsencrypt-prod)
#   3. Install Headlamp via Helm (creates namespace, ServiceAccount, ClusterRoleBinding, Ingress)
#   4. Apply the explicit Certificate manifest
#   5. Wait for the shared ingress-nginx LoadBalancer IP
#   6. Create headlamp.container.it-scholar.com A record in Cloudflare
#   7. Wait for TLS certificate issuance
#   8. Mint a long-lived login token for the headlamp ServiceAccount and print it
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

# ── Step 1: cert-manager Cloudflare secret ────────────────────────────────────
echo ""
echo "=== Step 1: Cloudflare API token secret in cert-manager ==="
kubectl create secret generic cloudflare-api-token \
  --namespace cert-manager \
  --from-literal=api-token="$CF_API_TOKEN" \
  --dry-run=client -o yaml | kubectl apply -f -
echo "  Cloudflare secret ready."

# ── Step 2: ClusterIssuer ─────────────────────────────────────────────────────
echo ""
echo "=== Step 2: ClusterIssuer (Let's Encrypt DNS-01) ==="
kubectl apply -f "$SCRIPT_DIR/../harbor/01-clusterissuer.yaml"
echo "  ClusterIssuer letsencrypt-prod applied."

# ── Step 3: Namespace + Install Headlamp via Helm ─────────────────────────────
echo ""
echo "=== Step 3: Installing Headlamp via Helm ==="
kubectl apply -f "$SCRIPT_DIR/00-namespace.yaml"

helm repo add headlamp https://kubernetes-sigs.github.io/headlamp/ 2>/dev/null || true
helm repo update headlamp

helm upgrade --install headlamp headlamp/headlamp \
  --namespace headlamp \
  -f "$SCRIPT_DIR/headlamp-values.yaml" \
  --wait \
  --timeout 5m

echo "  Headlamp installed."

# ── Step 4: Certificate ───────────────────────────────────────────────────────
echo ""
echo "=== Step 4: Applying Certificate ==="
kubectl apply -f "$SCRIPT_DIR/02-certificate.yaml"

# ── Step 5: Wait for the shared ingress-nginx LoadBalancer IP ─────────────────
echo ""
echo "=== Step 5: Waiting for ingress-nginx LoadBalancer IP ==="
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

# ── Step 6: Create headlamp DNS A record ──────────────────────────────────────
echo ""
echo "=== Step 6: Creating headlamp.container.it-scholar.com → $LB_IP ==="
python3 - <<PYEOF
import sys
sys.path.insert(0, '$ROOT_DIR')
import cloudflare as cf_module

cf       = cf_module.Cloudflare(api_token='$CF_API_TOKEN')
zone_id  = '$CF_ZONE_ID'
dns_name = 'headlamp.container.it-scholar.com'
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
if kubectl wait certificate/headlamp-tls \
    --namespace headlamp \
    --for=condition=Ready \
    --timeout=300s; then
  echo "  Certificate issued."
else
  echo "  Certificate not ready within 5 min — monitor with:"
  echo "  kubectl describe certificate headlamp-tls -n headlamp"
fi

# ── Step 8: Mint a login token ────────────────────────────────────────────────
echo ""
echo "=== Step 8: Minting a login token (ServiceAccount: headlamp, cluster-admin) ==="
HEADLAMP_TOKEN="$(kubectl create token headlamp -n headlamp --duration=87600h)"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════════════════════════"
echo "  Headlamp deployment complete"
echo "════════════════════════════════════════════════════════════════════════"
echo "  URL:   https://headlamp.container.it-scholar.com"
echo "  Login: paste this token at the login screen (cluster-admin access):"
echo ""
echo "  $HEADLAMP_TOKEN"
echo ""
echo "  Regenerate anytime with:"
echo "    kubectl create token headlamp -n headlamp --duration=87600h"
echo ""
echo "  DNS TTL is 300 s — allow up to 5 min for propagation."
echo "════════════════════════════════════════════════════════════════════════"
