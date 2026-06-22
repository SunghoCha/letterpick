output "nat_gateway_id" {
  value = aws_nat_gateway.main.id
}

output "nat_eip_public_ip" {
  value = aws_eip.nat.public_ip
}

output "db_access_instance_id" {
  value = var.enable_db_access_host ? aws_instance.db_access_host[0].id : null
}

output "db_access_instance_private_ip" {
  value = var.enable_db_access_host ? aws_instance.db_access_host[0].private_ip : null
}

output "observability_instance_id" {
  value = var.enable_observability_stack ? aws_instance.observability[0].id : null
}

output "observability_private_ip" {
  value = var.enable_observability_stack ? aws_instance.observability[0].private_ip : null
}

output "observability_grafana_port_forward_command" {
  value = var.enable_observability_stack ? "aws ssm start-session --target ${aws_instance.observability[0].id} --document-name AWS-StartPortForwardingSession --parameters '{\"portNumber\":[\"${var.observability_grafana_port}\"],\"localPortNumber\":[\"${var.observability_grafana_port}\"]}'" : null
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "api_task_family" {
  value = aws_ecs_task_definition.api.family
}

output "worker_task_family" {
  value = aws_ecs_task_definition.worker.family
}

output "trending_service_task_family" {
  value = aws_ecs_task_definition.trending_service.family
}

output "api_task_definition_arn" {
  value = aws_ecs_task_definition.api.arn
}

output "worker_task_definition_arn" {
  value = aws_ecs_task_definition.worker.arn
}

output "trending_service_task_definition_arn" {
  value = aws_ecs_task_definition.trending_service.arn
}

output "api_service_name" {
  value = aws_ecs_service.api.name
}

output "worker_service_name" {
  value = aws_ecs_service.worker.name
}

output "trending_service_name" {
  value = aws_ecs_service.trending_service.name
}

output "service_connect_namespace_arn" {
  value = aws_service_discovery_http_namespace.service_connect.arn
}

output "redis_primary_endpoint_address" {
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "redis_port" {
  value = aws_elasticache_replication_group.redis.port
}

output "api_alb_dns_name" {
  value = aws_lb.api.dns_name
}

output "frontend_domain_name" {
  value = var.frontend_domain_name
}

output "frontend_url" {
  value = "https://${var.frontend_domain_name}"
}

output "frontend_cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.frontend.id
}

output "frontend_cloudfront_domain_name" {
  value = aws_cloudfront_distribution.frontend.domain_name
}

output "api_blue_target_group_arn" {
  value = aws_lb_target_group.api_blue.arn
}

output "api_green_target_group_arn" {
  value = aws_lb_target_group.api_green.arn
}

output "api_production_listener_rule_arn" {
  value = aws_lb_listener_rule.https_api.arn
}

output "api_log_group_name" {
  value = aws_cloudwatch_log_group.api.name
}

output "worker_log_group_name" {
  value = aws_cloudwatch_log_group.worker.name
}

output "trending_service_log_group_name" {
  value = aws_cloudwatch_log_group.trending_service.name
}
