// 이 파일은 다음 Terraform 단계나 수동 확인에 필요한 핵심 값을 출력한다.
// dev root 안에서는 리소스를 직접 참조하지만, plan/apply 후 사람이 상태를 확인할 때 유용하다.

output "vpc_id" {
  value = aws_vpc.main.id
}

output "vpc_cidr" {
  value = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  value = aws_subnet.private[*].id
}

output "public_route_table_id" {
  value = aws_route_table.public.id
}

output "private_route_table_ids" {
  value = aws_route_table.private[*].id
}

output "nat_gateway_id" {
  value = aws_nat_gateway.main.id
}

output "nat_eip_public_ip" {
  value = aws_eip.nat.public_ip
}

output "rds_endpoint" {
  value = aws_db_instance.main.endpoint
}

output "rds_address" {
  value = aws_db_instance.main.address
}

output "rds_port" {
  value = aws_db_instance.main.port
}

output "rds_database_name" {
  value = aws_db_instance.main.db_name
}

output "db_access_instance_id" {
  value = var.enable_db_access_host ? aws_instance.db_access_host[0].id : null
}

output "db_access_instance_private_ip" {
  value = var.enable_db_access_host ? aws_instance.db_access_host[0].private_ip : null
}

output "k6_runner_instance_id" {
  value = var.enable_k6_runner ? aws_instance.k6_runner[0].id : null
}

output "k6_runner_private_ip" {
  value = var.enable_k6_runner ? aws_instance.k6_runner[0].private_ip : null
}

output "performance_dashboard_name" {
  value = var.enable_perf_observability ? aws_cloudwatch_dashboard.performance[0].dashboard_name : null
}

output "raw_mail_bucket_name" {
  value = aws_s3_bucket.raw_mail.bucket
}

output "raw_mail_object_prefix" {
  value = local.raw_mail_object_prefix_slash
}

output "mail_received_topic_arn" {
  value = aws_sns_topic.mail_received.arn
}

output "mail_receive_queue_arn" {
  value = aws_sqs_queue.mail_receive.arn
}

output "mail_receive_queue_name" {
  value = aws_sqs_queue.mail_receive.name
}

output "mail_receive_dlq_arn" {
  value = aws_sqs_queue.mail_receive_dlq.arn
}

output "trending_lifecycle_events_queue_arn" {
  value = aws_sqs_queue.trending_lifecycle_events.arn
}

output "trending_lifecycle_events_queue_name" {
  value = aws_sqs_queue.trending_lifecycle_events.name
}

output "trending_lifecycle_events_dlq_arn" {
  value = aws_sqs_queue.trending_lifecycle_events_dlq.arn
}

output "trending_lifecycle_events_dlq_name" {
  value = aws_sqs_queue.trending_lifecycle_events_dlq.name
}

output "trending_score_events_queue_arn" {
  value = aws_sqs_queue.trending_score_events.arn
}

output "trending_score_events_queue_name" {
  value = aws_sqs_queue.trending_score_events.name
}

output "trending_score_events_dlq_arn" {
  value = aws_sqs_queue.trending_score_events_dlq.arn
}

output "trending_score_events_dlq_name" {
  value = aws_sqs_queue.trending_score_events_dlq.name
}

output "ses_receive_domain_name" {
  value = var.ses_receive_domain_name
}

output "ses_receipt_rule_set_name" {
  value = aws_ses_receipt_rule_set.mail_receive.rule_set_name
}

output "ses_receipt_rule_name" {
  value = aws_ses_receipt_rule.store_and_notify.name
}

output "ses_mx_record" {
  value = local.ses_inbound_mx_record
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

output "api_alb_dns_name" {
  value = aws_lb.api.dns_name
}

output "api_alb_url" {
  value = "http://${aws_lb.api.dns_name}"
}

output "api_domain_name" {
  value = var.api_domain_name
}

output "api_url" {
  value = "https://${var.api_domain_name}"
}

output "frontend_domain_name" {
  value = var.frontend_domain_name
}

output "frontend_url" {
  value = "https://${var.frontend_domain_name}"
}

output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}

output "frontend_cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.frontend.id
}

output "frontend_cloudfront_domain_name" {
  value = aws_cloudfront_distribution.frontend.domain_name
}

output "api_target_group_arn" {
  value = aws_lb_target_group.api.arn
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
