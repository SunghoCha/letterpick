// 이 파일은 메일 수신의 내부 파이프라인을 만든다.
// SES receipt rule은 ses.tf에서 만들고, 이 파일은 raw MIME 저장소(S3),
// 수신 알림(SNS), Worker가 소비할 queue(SQS/DLQ)를 담당한다.

data "aws_caller_identity" "current" {}

locals {
  raw_mail_object_prefix_normalized = trimsuffix(var.raw_mail_object_prefix, "/")
  raw_mail_object_prefix_slash      = local.raw_mail_object_prefix_normalized == "" ? "" : "${local.raw_mail_object_prefix_normalized}/"

  raw_mail_bucket_name     = coalesce(var.raw_mail_bucket_name, "${local.name_prefix}-raw-mail-${data.aws_caller_identity.current.account_id}-${var.aws_region}")
  raw_mail_object_arn      = local.raw_mail_object_prefix_normalized == "" ? "${aws_s3_bucket.raw_mail.arn}/*" : "${aws_s3_bucket.raw_mail.arn}/${local.raw_mail_object_prefix_normalized}/*"
  mail_received_topic_name = coalesce(var.mail_received_topic_name, "${local.name_prefix}-mail-received")
  mail_receive_queue_name  = coalesce(var.mail_receive_queue_name, "${local.name_prefix}-mail-receive")
  mail_receive_dlq_name    = coalesce(var.mail_receive_dlq_name, "${local.name_prefix}-mail-receive-dlq")
}

resource "aws_s3_bucket" "raw_mail" {
  bucket        = local.raw_mail_bucket_name
  force_destroy = true

  tags = merge(local.common_tags, {
    Name = local.raw_mail_bucket_name
  })
}

resource "aws_s3_bucket_public_access_block" "raw_mail" {
  bucket = aws_s3_bucket.raw_mail.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "raw_mail" {
  bucket = aws_s3_bucket.raw_mail.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_sns_topic" "mail_received" {
  name = local.mail_received_topic_name

  tags = merge(local.common_tags, {
    Name = local.mail_received_topic_name
  })
}

resource "aws_sqs_queue" "mail_receive_dlq" {
  name                      = local.mail_receive_dlq_name
  message_retention_seconds = var.mail_receive_message_retention_seconds

  tags = merge(local.common_tags, {
    Name = local.mail_receive_dlq_name
  })
}

resource "aws_sqs_queue" "mail_receive" {
  name                       = local.mail_receive_queue_name
  visibility_timeout_seconds = var.mail_receive_visibility_timeout_seconds
  message_retention_seconds  = var.mail_receive_message_retention_seconds

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.mail_receive_dlq.arn
    maxReceiveCount     = var.mail_receive_max_receive_count
  })

  tags = merge(local.common_tags, {
    Name = local.mail_receive_queue_name
  })
}

data "aws_iam_policy_document" "mail_receive_queue" {
  statement {
    sid = "AllowMailReceivedTopicToSendMessage"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    resources = [
      aws_sqs_queue.mail_receive.arn,
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.mail_received.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "mail_receive" {
  queue_url = aws_sqs_queue.mail_receive.id
  policy    = data.aws_iam_policy_document.mail_receive_queue.json
}

resource "aws_sns_topic_subscription" "mail_receive" {
  topic_arn            = aws_sns_topic.mail_received.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.mail_receive.arn
  raw_message_delivery = true

  depends_on = [
    aws_sqs_queue_policy.mail_receive,
  ]
}
