output "kubeconfig_path" {
  description = "Absolute path to the generated kubeconfig file"
  value       = abspath("${path.module}/kubeconfig")
}

output "talosconfig_path" {
  description = "Absolute path to the generated talosconfig file"
  value       = abspath("${path.module}/talosconfig")
}
