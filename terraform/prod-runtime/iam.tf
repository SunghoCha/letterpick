locals {
  secret_name_prefix          = coalesce(var.secret_name_prefix, "${var.project}/${var.environment}")
  ecs_execution_secret_prefix = "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:${local.secret_name_prefix}/*"
}

data "aws_iam_policy_document" "ecs_tasks_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_execution" {
  name               = "${local.name_prefix}-ecs-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-ecs-execution-role"
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution_managed" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "ecs_execution_secrets" {
  statement {
    actions = [
      "secretsmanager:GetSecretValue",
    ]

    resources = [
      local.ecs_execution_secret_prefix,
      local.persistence.rds_master_user_secret_arn,
    ]
  }

  statement {
    actions = [
      "kms:Decrypt",
    ]

    resources = [
      local.persistence.rds_master_user_secret_kms_key_id,
    ]
  }
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name   = "${local.name_prefix}-ecs-execution-secrets"
  role   = aws_iam_role.ecs_execution.id
  policy = data.aws_iam_policy_document.ecs_execution_secrets.json
}

resource "aws_iam_role" "api_task" {
  name               = "${local.name_prefix}-api-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-api-task-role"
  })
}

data "aws_iam_policy_document" "trending_event_publish" {
  statement {
    actions = [
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
      "sqs:SendMessage",
    ]

    resources = [
      local.persistence.trending_lifecycle_events_queue_arn,
      local.persistence.trending_score_events_queue_arn,
    ]
  }
}

resource "aws_iam_role_policy" "api_trending_event_publish" {
  name   = "${local.name_prefix}-api-trending-event-publish"
  role   = aws_iam_role.api_task.id
  policy = data.aws_iam_policy_document.trending_event_publish.json
}

data "aws_iam_policy_document" "api_mail_queue_status_access" {
  statement {
    actions = [
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
    ]

    resources = [
      local.persistence.mail_receive_queue_arn,
      local.persistence.mail_receive_dlq_arn,
    ]
  }
}

resource "aws_iam_role_policy" "api_mail_queue_status_access" {
  name   = "${local.name_prefix}-api-mail-queue-status-access"
  role   = aws_iam_role.api_task.id
  policy = data.aws_iam_policy_document.api_mail_queue_status_access.json
}

resource "aws_iam_role" "worker_task" {
  name               = "${local.name_prefix}-worker-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-worker-task-role"
  })
}

data "aws_iam_policy_document" "worker_mail_access" {
  statement {
    actions = [
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
    ]

    resources = [
      local.persistence.mail_receive_queue_arn,
    ]
  }

  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      local.persistence.raw_mail_object_arn,
    ]
  }
}

resource "aws_iam_role_policy" "worker_mail_access" {
  name   = "${local.name_prefix}-worker-mail-access"
  role   = aws_iam_role.worker_task.id
  policy = data.aws_iam_policy_document.worker_mail_access.json
}

resource "aws_iam_role_policy" "worker_trending_event_publish" {
  name   = "${local.name_prefix}-worker-trending-event-publish"
  role   = aws_iam_role.worker_task.id
  policy = data.aws_iam_policy_document.trending_event_publish.json
}

resource "aws_iam_role" "trending_service_task" {
  name               = "${local.name_prefix}-trending-service-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-trending-service-task-role"
  })
}

data "aws_iam_policy_document" "trending_event_consume" {
  statement {
    actions = [
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
    ]

    resources = [
      local.persistence.trending_lifecycle_events_queue_arn,
      local.persistence.trending_score_events_queue_arn,
    ]
  }
}

resource "aws_iam_role_policy" "trending_service_event_consume" {
  name   = "${local.name_prefix}-trending-service-event-consume"
  role   = aws_iam_role.trending_service_task.id
  policy = data.aws_iam_policy_document.trending_event_consume.json
}

data "aws_iam_policy_document" "ecs_infrastructure_assume_role" {
  statement {
    sid     = "AllowAccessToECSForInfrastructureManagement"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_load_balancer_infrastructure" {
  name               = "${local.name_prefix}-ecs-lb-infra-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_infrastructure_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-ecs-lb-infra-role"
  })
}

resource "aws_iam_role_policy_attachment" "ecs_load_balancer_infrastructure" {
  role       = aws_iam_role.ecs_load_balancer_infrastructure.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonECSInfrastructureRolePolicyForLoadBalancers"
}
