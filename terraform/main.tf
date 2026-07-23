# ─────────────────────────────────────────────────────────────────────────────
# Container Seminar — Kubernetes Clusters (Talos on Hetzner Cloud)
# ─────────────────────────────────────────────────────────────────────────────
#
# Two small, independent clusters for students: "staging" and "prod".
# Each is a single control-plane + single worker node (etcd HA is not needed
# for a seminar). Both are defined from the same module with per-environment
# overrides so they can diverge later (e.g. giving prod more capacity)
# without restructuring.
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
#   export KUBECONFIG="$(pwd)/kubeconfig-staging"   # or kubeconfig-prod
#   kubectl get nodes
#
# Teardown:
#   Each cluster has cluster_delete_protection = true by default. To destroy
#   a cluster, set the corresponding *_delete_protection variable to false,
#   apply that change, then run `terraform destroy -target=module.staging`
#   (or `module.prod`).
#
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.5.0"
}

locals {
  # Shared "small cluster" sizing for both environments.
  # cpx32: 4 vCPU / 8 GB RAM, hel1 — same spec as cx33 but different hardware pool.
  # cx33/cpx31 are exhausted in hel1; cpx32 runs on a separate pool and is available.
  node_location = "hel1"
  node_type     = "cpx32"

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
}

# ── Staging cluster ─────────────────────────────────────────────────────────
module "staging" {
  source  = "hcloud-k8s/kubernetes/hcloud"
  version = "5.3.0"

  cluster_name              = "container-seminar-staging"
  hcloud_token              = var.hcloud_token
  cluster_delete_protection = var.staging_delete_protection

  # Write kubeconfig and talosconfig next to this file after apply
  cluster_kubeconfig_path  = "${path.module}/kubeconfig-staging"
  cluster_talosconfig_path = "${path.module}/talosconfig-staging"

  # ── Networking ────────────────────────────────────────────────────────────
  # Cilium Gateway API is required for Harbor TLS termination and routing.
  # Cert Manager issues the Let's Encrypt certificate via Cloudflare DNS-01.
  cilium_gateway_api_enabled = true
  cert_manager_enabled       = true

  firewall_kube_api_source = local.firewall_kube_api_source
  firewall_extra_rules     = local.firewall_extra_rules

  # ── Node pools ────────────────────────────────────────────────────────────
  control_plane_nodepools = [
    { name = "control", type = local.node_type, location = local.node_location, count = 1 }
  ]
  worker_nodepools = [
    { name = "worker", type = local.node_type, location = local.node_location, count = 1, placement_group = false }
  ]
}

# ── Prod cluster ────────────────────────────────────────────────────────────
module "prod" {
  source  = "hcloud-k8s/kubernetes/hcloud"
  version = "5.3.0"

  cluster_name              = "container-seminar-prod"
  hcloud_token              = var.hcloud_token
  cluster_delete_protection = var.prod_delete_protection

  # Write kubeconfig and talosconfig next to this file after apply
  cluster_kubeconfig_path  = "${path.module}/kubeconfig-prod"
  cluster_talosconfig_path = "${path.module}/talosconfig-prod"

  # ── Networking ────────────────────────────────────────────────────────────
  # Cilium Gateway API is required for Harbor TLS termination and routing.
  # Cert Manager issues the Let's Encrypt certificate via Cloudflare DNS-01.
  cilium_gateway_api_enabled = true
  cert_manager_enabled       = true

  firewall_kube_api_source = local.firewall_kube_api_source
  firewall_extra_rules     = local.firewall_extra_rules

  # ── Node pools ────────────────────────────────────────────────────────────
  control_plane_nodepools = [
    { name = "control", type = local.node_type, location = local.node_location, count = 1 }
  ]
  worker_nodepools = [
    { name = "worker", type = local.node_type, location = local.node_location, count = 1, placement_group = false }
  ]
}
