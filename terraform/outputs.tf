output "staging_kubeconfig_path" {
  description = "Absolute path to the generated staging kubeconfig file"
  value       = abspath("${path.module}/kubeconfig-staging")
}

output "staging_talosconfig_path" {
  description = "Absolute path to the generated staging talosconfig file"
  value       = abspath("${path.module}/talosconfig-staging")
}

output "prod_kubeconfig_path" {
  description = "Absolute path to the generated prod kubeconfig file"
  value       = abspath("${path.module}/kubeconfig-prod")
}

output "prod_talosconfig_path" {
  description = "Absolute path to the generated prod talosconfig file"
  value       = abspath("${path.module}/talosconfig-prod")
}

output "staging_control_plane_public_ipv4" {
  description = "Public IPv4 addresses of the staging control-plane node(s), for DNS records"
  value       = module.staging.control_plane_public_ipv4_list
}

output "staging_worker_public_ipv4" {
  description = "Public IPv4 addresses of the staging worker node(s)"
  value       = module.staging.worker_public_ipv4_list
}

output "prod_control_plane_public_ipv4" {
  description = "Public IPv4 addresses of the prod control-plane node(s), for DNS records"
  value       = module.prod.control_plane_public_ipv4_list
}

output "prod_worker_public_ipv4" {
  description = "Public IPv4 addresses of the prod worker node(s)"
  value       = module.prod.worker_public_ipv4_list
}
