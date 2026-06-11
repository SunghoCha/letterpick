// 이 파일은 LetterPick API/Worker가 Trending Service로 보낼 이벤트 queue를 만든다.
// lifecycle event와 score event는 처리 특성이 다르므로 queue와 DLQ를 분리한다.

locals {
  trending_lifecycle_events_queue_name = coalesce(var.trending_lifecycle_events_queue_name, "${local.name_prefix}-trending-lifecycle-events")
  trending_lifecycle_events_dlq_name   = coalesce(var.trending_lifecycle_events_dlq_name, "${local.name_prefix}-trending-lifecycle-events-dlq")
  trending_score_events_queue_name     = coalesce(var.trending_score_events_queue_name, "${local.name_prefix}-trending-score-events")
  trending_score_events_dlq_name       = coalesce(var.trending_score_events_dlq_name, "${local.name_prefix}-trending-score-events-dlq")
}

resource "aws_sqs_queue" "trending_lifecycle_events_dlq" {
  name                      = local.trending_lifecycle_events_dlq_name
  message_retention_seconds = var.trending_events_message_retention_seconds

  tags = merge(local.common_tags, {
    Name = local.trending_lifecycle_events_dlq_name
  })
}

resource "aws_sqs_queue" "trending_lifecycle_events" {
  name                       = local.trending_lifecycle_events_queue_name
  visibility_timeout_seconds = var.trending_events_visibility_timeout_seconds
  message_retention_seconds  = var.trending_events_message_retention_seconds

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.trending_lifecycle_events_dlq.arn
    maxReceiveCount     = var.trending_events_max_receive_count
  })

  tags = merge(local.common_tags, {
    Name = local.trending_lifecycle_events_queue_name
  })
}

resource "aws_sqs_queue" "trending_score_events_dlq" {
  name                      = local.trending_score_events_dlq_name
  message_retention_seconds = var.trending_events_message_retention_seconds

  tags = merge(local.common_tags, {
    Name = local.trending_score_events_dlq_name
  })
}

resource "aws_sqs_queue" "trending_score_events" {
  name                       = local.trending_score_events_queue_name
  visibility_timeout_seconds = var.trending_events_visibility_timeout_seconds
  message_retention_seconds  = var.trending_events_message_retention_seconds

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.trending_score_events_dlq.arn
    maxReceiveCount     = var.trending_events_max_receive_count
  })

  tags = merge(local.common_tags, {
    Name = local.trending_score_events_queue_name
  })
}
