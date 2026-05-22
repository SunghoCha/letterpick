// 이 파일은 ECS task 실행과 worker AWS SDK 호출에 필요한 IAM 역할을 만든다.
// execution role은 ECR pull, CloudWatch Logs, Secrets Manager 주입에 쓰이고,
// worker task role은 애플리케이션 코드가 SQS/S3를 호출할 때 쓰인다.
// prod API Blue/Green에서는 ECS가 ALB listener rule과 target group을 조정해야 하므로
// load balancer infrastructure role도 별도로 둔다.

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
      aws_db_instance.main.master_user_secret[0].secret_arn,
    ]
  }

  statement {
    actions = [
      "kms:Decrypt",
    ]

    resources = [
      aws_db_instance.main.master_user_secret[0].kms_key_id,
    ]
  }
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name   = "${local.name_prefix}-ecs-execution-secrets"
  role   = aws_iam_role.ecs_execution.id
  policy = data.aws_iam_policy_document.ecs_execution_secrets.json
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
      aws_sqs_queue.mail_receive.arn,
    ]
  }

  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      local.raw_mail_object_arn,
    ]
  }
}

resource "aws_iam_role_policy" "worker_mail_access" {
  name   = "${local.name_prefix}-worker-mail-access"
  role   = aws_iam_role.worker_task.id
  policy = data.aws_iam_policy_document.worker_mail_access.json
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
