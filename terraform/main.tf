# ─────────────────────────────────────────────────────────────────────────────
# Container Seminar — Kubernetes Cluster (Talos on Hetzner Cloud)
# ─────────────────────────────────────────────────────────────────────────────
#
# Prerequisites (install via brew):
#   brew install terraform packer talosctl kubectl helm
#
# The module uses Packer (local-exec) to upload the Talos OS image to Hetzner
# before creating the VMs. packer must be in PATH when you run terraform apply.
#
# Usage:
#   export TF_VAR_hcloud_token="<your-token>"   # or add to terraform.tfvars
#   terraform init
#   terraform apply
#
# After apply:
#   export KUBECONFIG="$(pwd)/kubeconfig"
#   kubectl get nodes
#
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.5.0"
}

module "kubernetes" {
  source  = "hcloud-k8s/kubernetes/hcloud"
  version = "5.3.0"

  cluster_name = "container-seminar"
  hcloud_token = var.hcloud_token

  # Write kubeconfig and talosconfig next to this file after apply
  cluster_kubeconfig_path  = "${path.module}/kubeconfig"
  cluster_talosconfig_path = "${path.module}/talosconfig"

  # ── Networking ──────────────────────────────────────────────────────────────
  # Cilium Gateway API is required for Harbor TLS termination and routing.
  # Cert Manager issues the Let's Encrypt certificate via Cloudflare DNS-01.
  cilium_gateway_api_enabled = true
  cert_manager_enabled       = true

  # Allow the Kubernetes API (port 6443) from anywhere for seminar access.
  firewall_kube_api_source = ["0.0.0.0/0", "::/0"]

  # Allow NodePort traffic on the default range from external clients.
  firewall_extra_rules = [
    {
      description = "Allow NodePort TCP traffic"
      direction   = "in"
      source_ips  = ["0.0.0.0/0", "::/0"]
      protocol    = "tcp"
      port        = "30000-32767"
    },
    {
      description = "Allow NodePort UDP traffic"
      direction   = "in"
      source_ips  = ["0.0.0.0/0", "::/0"]
      protocol    = "udp"
      port        = "30000-32767"
    }
  ]

  # ── Node pools ──────────────────────────────────────────────────────────────
  # cpx32: 4 vCPU / 8 GB RAM, hel1 — same spec as cx33 but different hardware pool.
  # cx33/cpx31 are exhausted in hel1; cpx32 runs on a separate pool and is available.
  # 1 control-plane (etcd works fine without HA for a seminar) + 2 workers
  # gives enough capacity for Harbor (registry, core, portal, trivy, redis, postgres).
  # placement_group=false on workers avoids the spread PG capacity constraint.
  control_plane_nodepools = [
    { name = "control", type = "cpx32", location = "hel1", count = 1 }
  ]
  worker_nodepools = [
    { name = "worker", type = "cpx32", location = "hel1", count = 2, placement_group = false }
  ]
}
