variable "hcloud_token" {
  description = "Hetzner Cloud API token — same token used in provision.py (HCLOUD_TOKEN in .env)"
  type        = string
  sensitive   = true
}

variable "staging_delete_protection" {
  description = "Whether to enable Hetzner delete protection on the staging cluster's resources. Set to false before destroying the staging cluster."
  type        = bool
  default     = true
}

variable "prod_delete_protection" {
  description = "Whether to enable Hetzner delete protection on the prod cluster's resources. Set to false before destroying the prod cluster."
  type        = bool
  default     = true
}
