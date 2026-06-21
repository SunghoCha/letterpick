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

output "route53_zone_name" {
  value = var.route53_zone_name
}

output "route53_zone_id" {
  value = data.aws_route53_zone.api.zone_id
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

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}

output "rds_master_user_secret_arn" {
  value = aws_db_instance.main.master_user_secret[0].secret_arn
}

output "rds_master_user_secret_kms_key_id" {
  value = aws_db_instance.main.master_user_secret[0].kms_key_id
}

output "raw_mail_bucket_name" {
  value = aws_s3_bucket.raw_mail.bucket
}

output "raw_mail_bucket_arn" {
  value = aws_s3_bucket.raw_mail.arn
}

output "raw_mail_object_prefix" {
  value = local.raw_mail_object_prefix_normalized
}

output "raw_mail_object_arn" {
  value = local.raw_mail_object_arn
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

output "mail_receive_dlq_name" {
  value = aws_sqs_queue.mail_receive_dlq.name
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

output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}

output "frontend_bucket_arn" {
  value = aws_s3_bucket.frontend.arn
}

output "frontend_bucket_regional_domain_name" {
  value = aws_s3_bucket.frontend.bucket_regional_domain_name
}
