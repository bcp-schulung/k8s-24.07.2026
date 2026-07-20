#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Harbor Deploy — Container Seminar
# ─────────────────────────────────────────────────────────────────────────────
#
# Prerequisites (must be run AFTER terraform apply in the terraform/ directory):
#   brew install kubectl helm
#   pip install cloudflare python-dotenv requests
#
# Usage:
#   cd /path/to/container-15.07.2026
#   bash harbor/deploy.sh
#
# Steps:
#   1. Generate / load Harbor admin password  → .harbor-passwords.json
#   2. Create the cert-manager Cloudflare secret
#   3. Apply cert-manager ClusterIssuer (DNS-01 via Cloudflare)
#   4. Install Harbor via Helm
#   5. Apply Gateway + Certificate + HTTPRoutes
#   6. Wait for the Cilium Gateway to get a LoadBalancer IP
#   7. Create harbor.container.it-scholar.com A record in Cloudflare
#   8. Wait for TLS certificate issuance (DNS-01, usually < 2 min)
#   9. Print summary; prompt to run harbor-setup.py
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
# shellcheck source=/dev/null
set -o allexport
source "$ROOT_DIR/.env"
set +o allexport

# ── Verify kubeconfig ─────────────────────────────────────────────────────────
export KUBECONFIG="$TF_DIR/kubeconfig"
if [[ ! -f "$KUBECONFIG" ]]; then
  echo "ERROR: $KUBECONFIG not found."
  echo "       Run 'terraform init && terraform apply' inside the terraform/ directory first."
  exit 1
fi

echo "Using KUBECONFIG: $KUBECONFIG"
kubectl cluster-info --request-timeout=10s >/dev/null

# ── Step 1: Harbor admin password ─────────────────────────────────────────────
echo ""
echo "=== Step 1: Harbor admin password ==="
HARBOR_PWDS_FILE="$ROOT_DIR/.harbor-passwords.json"

if [[ -f "$HARBOR_PWDS_FILE" ]]; then
  HARBOR_ADMIN_PASSWORD="$(python3 -c "
import json, sys
d = json.load(open('$HARBOR_PWDS_FILE'))
print(d['admin'])
")"
  echo "  Loaded from $HARBOR_PWDS_FILE"
else
  HARBOR_ADMIN_PASSWORD="$(python3 -c "
import secrets, string
alphabet = string.ascii_letters + string.digits
print(''.join(secrets.choice(alphabet) for _ in range(24)))
")"
  python3 -c "
import json
json.dump({'admin': '$HARBOR_ADMIN_PASSWORD'}, open('$HARBOR_PWDS_FILE', 'w'), indent=2)
"
  echo "  Generated and saved to $HARBOR_PWDS_FILE"
fi

# ── Step 2: Namespace + cert-manager Cloudflare secret ────────────────────────
echo ""
echo "=== Step 2: Namespace + Cloudflare secret ==="
kubectl apply -f "$SCRIPT_DIR/00-namespace.yaml"

# Create (or update) the Cloudflare API token secret in the cert-manager namespace.
# --dry-run=client + apply is idempotent and avoids "already exists" errors.
kubectl create secret generic cloudflare-api-token \
  --namespace cert-manager \
  --from-literal=api-token="$CF_API_TOKEN" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "  Cloudflare secret ready in cert-manager namespace."

# ── Step 3: ClusterIssuer ─────────────────────────────────────────────────────
echo ""
echo "=== Step 3: ClusterIssuer (Let's Encrypt DNS-01) ==="
kubectl apply -f "$SCRIPT_DIR/01-clusterissuer.yaml"

# ── Step 4: Install Harbor via Helm ───────────────────────────────────────────
echo ""
echo "=== Step 4: Installing Harbor via Helm (this takes ~5 min) ==="
helm repo add harbor https://helm.goharbor.io 2>/dev/null || true
helm repo update harbor

# Pin the chart version for reproducibility.
# Check https://github.com/goharbor/harbor-helm/releases for the latest version.
HARBOR_CHART_VERSION="${HARBOR_CHART_VERSION:-1.16.0}"

helm upgrade --install harbor harbor/harbor \
  --namespace harbor \
  --version "$HARBOR_CHART_VERSION" \
  --set harborAdminPassword="$HARBOR_ADMIN_PASSWORD" \
  -f "$SCRIPT_DIR/harbor-values.yaml" \
  --wait \
  --timeout 10m

echo "  Harbor installed."

# ── Step 5: Gateway + Certificate + HTTPRoutes ────────────────────────────────
echo ""
echo "=== Step 5: Applying Gateway, Certificate, HTTPRoutes ==="
kubectl apply -f "$SCRIPT_DIR/02-gateway.yaml"
kubectl apply -f "$SCRIPT_DIR/03-certificate.yaml"
kubectl apply -f "$SCRIPT_DIR/04-httproute.yaml"

# ── Step 6: Wait for Gateway LoadBalancer IP ──────────────────────────────────
echo ""
echo "=== Step 6: Waiting for Gateway LoadBalancer IP ==="
LB_IP=""
for i in $(seq 1 72); do   # up to 12 minutes
  LB_IP="$(kubectl get gateway harbor-gateway -n harbor \
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
  echo "  Debug: kubectl get gateway harbor-gateway -n harbor -o yaml"
  echo "  Debug: kubectl get events -n harbor --sort-by=.lastTimestamp"
  exit 1
fi

# ── Step 7: Create harbor DNS A record ────────────────────────────────────────
echo ""
echo "=== Step 7: Creating harbor.container.it-scholar.com → $LB_IP ==="
python3 - <<PYEOF
import sys
sys.path.insert(0, '$ROOT_DIR')
import cloudflare as cf_module

cf       = cf_module.Cloudflare(api_token='$CF_API_TOKEN')
zone_id  = '$CF_ZONE_ID'
dns_name = 'harbor.container.it-scholar.com'
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
if kubectl wait certificate/harbor-tls \
    --namespace harbor \
    --for=condition=Ready \
    --timeout=300s; then
  echo "  Certificate issued."
else
  echo "  Certificate not ready within 5 min — it may still be issuing."
  echo "  Monitor: kubectl describe certificate harbor-tls -n harbor"
  echo "  Monitor: kubectl describe certificaterequest -n harbor"
  echo "  (Continuing anyway — Harbor will work once the cert is ready.)"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════════════════════════"
echo "  Harbor deployment complete"
echo "════════════════════════════════════════════════════════════════════════"
echo "  URL:            https://harbor.container.it-scholar.com"
echo "  Admin password: $HARBOR_ADMIN_PASSWORD  (also in .harbor-passwords.json)"
echo "  LB IP:          $LB_IP"
echo ""
echo "  NOTE: Cloudflare DNS is set grey-cloud (proxy OFF) as required for"
echo "        the Cilium Gateway. DNS propagation may take up to 60 s."
echo ""
echo "  Next: create student users and projects:"
echo "    pip install requests"
echo "    python3 harbor/harbor-setup.py"
echo "════════════════════════════════════════════════════════════════════════"
