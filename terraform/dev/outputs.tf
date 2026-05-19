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

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "api_task_family" {
  value = aws_ecs_task_definition.api.family
}

output "worker_task_family" {
  value = aws_ecs_task_definition.worker.family
}

output "api_task_definition_arn" {
  value = aws_ecs_task_definition.api.arn
}

output "worker_task_definition_arn" {
  value = aws_ecs_task_definition.worker.arn
}

output "api_service_name" {
  value = aws_ecs_service.api.name
}

output "worker_service_name" {
  value = aws_ecs_service.worker.name
}

output "github_actions_dev_deploy_role_arn" {
  value = aws_iam_role.github_actions_dev_deploy.arn
}

output "api_alb_dns_name" {
  value = aws_lb.api.dns_name
}

output "api_alb_url" {
  value = "http://${aws_lb.api.dns_name}"
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
