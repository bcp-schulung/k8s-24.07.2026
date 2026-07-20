variable "hcloud_token" {
  description = "Hetzner Cloud API token — same token used in provision.py (HCLOUD_TOKEN in .env)"
  type        = string
  sensitive   = true
}
