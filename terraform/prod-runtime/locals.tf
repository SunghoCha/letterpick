locals {
  name_prefix = "${var.project}-${var.environment}"

  service_connect_namespace_name    = "${local.name_prefix}.local"
  service_connect_port_name         = "http"
  trending_service_connect_dns_name = "trending-service"
  trending_service_connect_base_url = "http://${local.trending_service_connect_dns_name}:${var.container_port}"

  common_tags = merge(var.tags, {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  })

  persistence = data.terraform_remote_state.persistence.outputs
}
